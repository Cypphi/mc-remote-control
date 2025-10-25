package dev.cypphi.mcrc.remoteview.signaling;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import dev.cypphi.mcrc.remoteview.RemoteViewSession;
import dev.cypphi.mcrc.remoteview.RemoteViewSessionManager;
import dev.cypphi.mcrc.remoteview.stream.MjpegRemoteViewPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RemoteViewSignalingServer {
    private static final Logger LOGGER = LoggerFactory.getLogger("mcrc-remoteview-stream-server");
    private static final Gson GSON = new GsonBuilder().create();

    private final RemoteViewSessionManager sessionManager;
    private final MjpegRemoteViewPublisher streamPublisher;
    private HttpServer server;
    private ExecutorService executor;
    private InetSocketAddress boundAddress;

    public RemoteViewSignalingServer(RemoteViewSessionManager sessionManager,
                                     MjpegRemoteViewPublisher streamPublisher) {
        this.sessionManager = sessionManager;
        this.streamPublisher = streamPublisher;
    }

    public synchronized void start(String bindAddress, int port) throws IOException {
        if (server != null) {
            return;
        }

        InetSocketAddress address = new InetSocketAddress(bindAddress, port);
        server = HttpServer.create(address, 0);
        executor = Executors.newFixedThreadPool(8, r -> {
            Thread t = new Thread(r, "mcrc-remoteview-stream");
            t.setDaemon(true);
            return t;
        });
        server.createContext("/", new StaticSiteHandler());
        server.createContext("/api/remoteview/stream", new StreamHandler());
        server.createContext("/api/remoteview/health", new HealthHandler());
        server.setExecutor(executor);
        server.start();
        boundAddress = address;
        LOGGER.info("Remote View stream server listening on {}:{}", bindAddress, port);
    }

    public synchronized void stop() {
        if (server != null) {
            server.stop(0);
            server = null;
        }
        if (executor != null) {
            executor.shutdownNow();
            executor = null;
        }
        boundAddress = null;
    }

    public Optional<InetSocketAddress> getBoundAddress() {
        return Optional.ofNullable(boundAddress);
    }

    private final class StaticSiteHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            try {
                String method = exchange.getRequestMethod();
                if (!"GET".equalsIgnoreCase(method) && !"HEAD".equalsIgnoreCase(method)) {
                    sendPlain(exchange, 405, "Method not allowed");
                    return;
                }

                StaticAsset asset = resolveAsset(exchange.getRequestURI().getPath());
                if (asset == null) {
                    sendPlain(exchange, 404, "Not found");
                    return;
                }

                Headers headers = exchange.getResponseHeaders();
                headers.set("Content-Type", asset.contentType());
                headers.set("Cache-Control", asset.cacheControl());
                headers.set("X-Content-Type-Options", "nosniff");

                byte[] bytes = asset.bytes();
                exchange.sendResponseHeaders(200, bytes.length);
                if (!"HEAD".equalsIgnoreCase(method)) {
                    try (OutputStream output = exchange.getResponseBody()) {
                        output.write(bytes);
                    }
                }
            } finally {
                exchange.close();
            }
        }
    }

    private final class StreamHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            try {
                addCorsHeaders(exchange.getResponseHeaders());

                if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                    exchange.sendResponseHeaders(204, -1);
                    return;
                }

                if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                    sendJson(exchange, 405, Map.of("error", "Method not allowed"));
                    return;
                }

                Map<String, String> queryParams = parseQuery(exchange.getRequestURI());
                String sessionId = queryParams.get("session");
                String authToken = queryParams.get("auth");

                if (sessionId == null || authToken == null) {
                    sendJson(exchange, 400, Map.of("error", "Missing session or auth parameter"));
                    return;
                }

                boolean valid = sessionManager.validateAuthToken(sessionId, authToken);
                if (!valid) {
                    sendJson(exchange, 401, Map.of("error", "Unauthorized or expired session"));
                    return;
                }

                Optional<RemoteViewSession> sessionOpt = sessionManager.getSessionById(sessionId);
                if (sessionOpt.isEmpty()) {
                    sendJson(exchange, 404, Map.of("error", "Session not active"));
                    return;
                }

                RemoteViewSession session = sessionOpt.get();
                String boundary = "mcrc-" + UUID.randomUUID();

                Optional<MjpegRemoteViewPublisher.ViewerConnection> connectionOpt =
                        streamPublisher.attachViewer(session, boundary,
                                () -> sessionManager.markViewerDisconnected(sessionId));
                if (connectionOpt.isEmpty()) {
                    sendJson(exchange, 409, Map.of("error", "Viewer already connected"));
                    return;
                }

                MjpegRemoteViewPublisher.ViewerConnection connection = connectionOpt.get();

                Headers headers = exchange.getResponseHeaders();
                headers.set("Content-Type", "multipart/x-mixed-replace; boundary=" + boundary);
                headers.set("Cache-Control", "no-cache, no-store, must-revalidate");
                headers.set("Pragma", "no-cache");
                headers.set("Connection", "close");
                headers.set("X-Content-Type-Options", "nosniff");

                exchange.sendResponseHeaders(200, 0);

                sessionManager.markViewerConnected(sessionId);

                try (OutputStream output = exchange.getResponseBody()) {
                    connection.stream(output);
                } catch (IOException e) {
                    if (!connection.isClosed()) {
                        LOGGER.debug("MJPEG stream for session {} closed: {}", sessionId, e.getMessage());
                    }
                    throw e;
                }
            } catch (IOException e) {
                LOGGER.error("MJPEG stream error", e);
                throw e;
            } catch (Exception e) {
                LOGGER.error("MJPEG stream handler error", e);
                if (!exchange.getResponseHeaders().containsKey("Content-Type")) {
                    sendJson(exchange, 500, Map.of("error", "Internal server error"));
                }
            } finally {
                exchange.close();
            }
        }
    }

    private final class HealthHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            try {
                addCorsHeaders(exchange.getResponseHeaders());
                if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                    exchange.sendResponseHeaders(204, -1);
                    return;
                }
                if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                    sendJson(exchange, 405, Map.of("error", "Method not allowed"));
                    return;
                }
                sendJson(exchange, 200, Map.of(
                        "status", "ok",
                        "time", Instant.now().toEpochMilli()
                ));
            } finally {
                exchange.close();
            }
        }
    }

    private StaticAsset resolveAsset(String requestPath) throws IOException {
        String path = normalizePath(requestPath);
        if (path == null) {
            return null;
        }

        String resourcePath = "remoteview/site/" + path;
        try (InputStream stream = RemoteViewSignalingServer.class.getClassLoader().getResourceAsStream(resourcePath)) {
            if (stream == null) {
                return null;
            }
            byte[] bytes = stream.readAllBytes();
            String contentType = contentTypeFor(path);
            String cacheControl = path.equals("index.html")
                    ? "no-cache, no-store, must-revalidate"
                    : "public, max-age=120";
            return new StaticAsset(bytes, contentType, cacheControl);
        }
    }

    private String normalizePath(String requestPath) {
        if (requestPath == null || requestPath.isBlank() || "/".equals(requestPath)) {
            return "index.html";
        }
        String path = requestPath;
        if (path.startsWith("/")) {
            path = path.substring(1);
        }
        if (path.contains("..")) {
            return null;
        }
        if (path.isBlank() || path.equals("/")) {
            return "index.html";
        }
        if (path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
            if (path.isBlank()) {
                return "index.html";
            }
        }
        return path;
    }

    private String contentTypeFor(String path) {
        if (path.endsWith(".html")) {
            return "text/html; charset=utf-8";
        }
        if (path.endsWith(".css")) {
            return "text/css; charset=utf-8";
        }
        if (path.endsWith(".js")) {
            return "application/javascript; charset=utf-8";
        }
        if (path.endsWith(".png")) {
            return "image/png";
        }
        if (path.endsWith(".svg")) {
            return "image/svg+xml";
        }
        return "application/octet-stream";
    }

    private void sendPlain(HttpExchange exchange, int statusCode, String message) throws IOException {
        byte[] bytes = message.getBytes(StandardCharsets.UTF_8);
        Headers headers = exchange.getResponseHeaders();
        headers.set("Content-Type", "text/plain; charset=utf-8");
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream output = exchange.getResponseBody()) {
            output.write(bytes);
        }
    }

    private Map<String, String> parseQuery(URI uri) {
        Map<String, String> params = new HashMap<>();
        if (uri == null || uri.getRawQuery() == null || uri.getRawQuery().isEmpty()) {
            return params;
        }
        String[] pairs = uri.getRawQuery().split("&");
        for (String pair : pairs) {
            int idx = pair.indexOf('=');
            if (idx == -1) {
                continue;
            }
            String key = decode(pair.substring(0, idx));
            String value = decode(pair.substring(idx + 1));
            params.put(key, value);
        }
        return params;
    }

    private String decode(String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }

    private void sendJson(HttpExchange exchange, int statusCode, Object body) throws IOException {
        Headers headers = exchange.getResponseHeaders();
        headers.set("Content-Type", "application/json; charset=utf-8");
        addCorsHeaders(headers);
        byte[] bytes = GSON.toJson(body).getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream output = exchange.getResponseBody()) {
            output.write(bytes);
        }
    }

    private void addCorsHeaders(Headers headers) {
        headers.set("Access-Control-Allow-Origin", "*");
        headers.set("Access-Control-Allow-Headers", "Content-Type");
        headers.set("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
    }

    private record StaticAsset(byte[] bytes, String contentType, String cacheControl) {}
}
