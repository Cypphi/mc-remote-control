## Updated Remote View Plan (LAN/Tailscale-friendly MJPEG)

**What’s already in place**
- Config UX: Remote View toggles, FPS slider, bind address/port, and public URL entry.
- Discord integration: `/remoteview` command generates one-minute invite links with session/auth tokens and embeds the `signal` parameter.
- Session/auth core: `RemoteViewSessionManager`, lifecycle hooks, access control, and new signaling server skeleton.
- Capture scaffolding: `RemoteViewCaptureService` (placeholder implementation), coordinator, and stream publisher interface.
- Static site: polished UI, social links, countdowns, and query-parameter parsing.

**New direction**
We’re moving away from WebRTC to an MJPEG-over-HTTPS stream that works on LAN or via user-managed VPNs (e.g., Tailscale). This keeps all video data local, avoids bundling native WebRTC libraries, and only requires users to ensure the streaming endpoint is reachable (LAN or VPN).

**Remaining work**
1. **Real Frame Capture + JPEG Encoding**
   - Implement actual framebuffer readback (OpenGL/FrameBuffer) at the configured FPS.
   - Encode frames to JPEG (e.g., via `BufferedImage` + `ImageIO`) with minimal impact on render performance.

2. **MJPEG Streaming Endpoint**
   - Replace the placeholder signaling offer/answer flow with an HTTP endpoint that validates `session`/`auth` and streams multipart JPEG (`Content-Type: multipart/x-mixed-replace`).
   - Tie lifecycle events so sessions cancel when the HTTP connection closes or expires.

3. **Static Site Adjustments**
   - Swap WebRTC logic for an MJPEG client (either `<img>` source or `fetch` + `<canvas>`).
   - Keep the countdown/auth UI but show clear messaging about LAN/VPN requirements.

4. **Documentation & Guidance**
   - Clearly state “Remote View works on your LAN; for remote access, install a VPN overlay (Tailscale/ZeroTier/etc.).”
   - Provide a quick-start guide for Tailscale so non-technical players can bring their own secure tunnel without port forwarding.

5. **Security & Cleanup**
   - Maintain per-session auth, rate limiting, and sanitized logging.
   - Remove leftover WebRTC stubs once MJPEG streaming is in place.
