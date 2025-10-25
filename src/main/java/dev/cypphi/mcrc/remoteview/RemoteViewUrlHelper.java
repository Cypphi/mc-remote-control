package dev.cypphi.mcrc.remoteview;

import dev.cypphi.mcrc.config.MCRCConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Enumeration;
import java.util.Optional;

public final class RemoteViewUrlHelper {
    private static final Logger LOGGER = LoggerFactory.getLogger("mcrc-remoteview-url");
    private static final String API_PATH = "/api/remoteview";

    private RemoteViewUrlHelper() {
    }

    public static Optional<ResolvedRemoteViewUrls> resolveUrls(MCRCConfig config) {
        String configured = config.remoteViewPublicBaseUrl == null ? "" : config.remoteViewPublicBaseUrl.trim();
        if (!configured.isEmpty()) {
            String normalized = ensureApiPath(configured);
            return Optional.of(new ResolvedRemoteViewUrls(normalized, deriveViewerBase(normalized)));
        }

        InetAddress preferred = determinePreferredAddress(config.remoteViewBindAddress);
        if (preferred == null) {
            preferred = findSiteLocalAddress().orElse(null);
        }

        if (preferred == null) {
            LOGGER.warn("Unable to determine LAN address for Remote View. Set a Remote View Public URL in the config.");
            return Optional.empty();
        }

        String host = formatHost(preferred);
        String baseUrl = "http://" + host + ":" + config.remoteViewPort + API_PATH;
        LOGGER.info("Resolved Remote View LAN URL: {}", baseUrl);
        return Optional.of(new ResolvedRemoteViewUrls(baseUrl, deriveViewerBase(baseUrl)));
    }

    private static InetAddress determinePreferredAddress(String bindAddress) {
        if (bindAddress == null || bindAddress.isBlank()) {
            return null;
        }
        String trimmed = bindAddress.trim();
        if ("0.0.0.0".equals(trimmed) || "::".equals(trimmed) || "::0".equals(trimmed)) {
            return null;
        }

        try {
            InetAddress candidate = InetAddress.getByName(trimmed);
            if (!candidate.isLoopbackAddress()) {
                return candidate;
            }
        } catch (UnknownHostException e) {
            LOGGER.warn("Failed to resolve bind address {}: {}", trimmed, e.getMessage());
        }
        return null;
    }

    private static Optional<InetAddress> findSiteLocalAddress() {
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            if (interfaces == null) {
                return Optional.empty();
            }

            while (interfaces.hasMoreElements()) {
                NetworkInterface iface = interfaces.nextElement();
                if (!iface.isUp() || iface.isLoopback() || iface.isVirtual()) {
                    continue;
                }
                var addresses = iface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress address = addresses.nextElement();
                    if (address.isLoopbackAddress() || address.isAnyLocalAddress()) {
                        continue;
                    }
                    if (address instanceof Inet4Address && address.isSiteLocalAddress()) {
                        return Optional.of(address);
                    }
                }
            }
        } catch (SocketException e) {
            LOGGER.warn("Failed to enumerate network interfaces for Remote View LAN URL: {}", e.getMessage());
        }

        return Optional.empty();
    }

    private static String formatHost(InetAddress address) {
        String host = address.getHostAddress();
        if (address instanceof Inet6Address && !host.startsWith("[")) {
            return "[" + host + "]";
        }
        return host;
    }

    private static String ensureApiPath(String base) {
        String trimmed = base.trim();
        while (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        if (!trimmed.endsWith(API_PATH)) {
            trimmed = trimmed + API_PATH;
        }
        return trimmed;
    }

    private static String deriveViewerBase(String signalBase) {
        String withoutPath = signalBase;
        if (withoutPath.endsWith(API_PATH)) {
            withoutPath = withoutPath.substring(0, withoutPath.length() - API_PATH.length());
        }
        if (!withoutPath.endsWith("/")) {
            withoutPath = withoutPath + "/";
        }
        return withoutPath;
    }

    public record ResolvedRemoteViewUrls(String signalBase, String viewerBase) {}
}
