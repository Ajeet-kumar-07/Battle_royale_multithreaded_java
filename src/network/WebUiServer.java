package network;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import engine.GameEngine;
import model.Item;
import model.Player;
import model.Zone;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.logging.Logger;

public class WebUiServer {
    private static final Logger logger = Logger.getLogger(WebUiServer.class.getName());
    private static final int WEB_PORT = 8090;
    private static final int MAX_EVENTS = 100;
    private static final Path REACT_DIST_DIR = Path.of("frontend", "dist");
    private static final Path LEGACY_WEB_DIR = Path.of("src", "web");

    private final GameEngine engine;
    private final Deque<String> eventFeed = new ArrayDeque<>();
    private HttpServer server;

    public WebUiServer(GameEngine engine) {
        this.engine = engine;

        this.engine.registerEventListener(event -> {
            synchronized (eventFeed) {
                eventFeed.addLast(event.getMessage());
                while (eventFeed.size() > MAX_EVENTS) {
                    eventFeed.removeFirst();
                }
            }
        });
    }

    public void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress(WEB_PORT), 0);
        server.createContext("/api/join", this::handleJoin);
        server.createContext("/api/command", this::handleCommand);
        server.createContext("/api/state", this::handleState);
        server.createContext("/", this::handleFrontend);
        server.setExecutor(null);
        server.start();

        logger.info("🌐 Web UI available at http://localhost:" + WEB_PORT);
        System.out.println("🌐 Web UI available at http://localhost:" + WEB_PORT);
    }

    private void handleFrontend(HttpExchange exchange) throws IOException {
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendText(exchange, 405, "Method Not Allowed");
            return;
        }

        String rawPath = exchange.getRequestURI().getPath();
        String requestPath = (rawPath == null || rawPath.isBlank()) ? "/" : rawPath;

        // React production build location (preferred)
        Path staticRoot = Files.exists(REACT_DIST_DIR) ? REACT_DIST_DIR : LEGACY_WEB_DIR;

        if (!Files.exists(staticRoot)) {
            sendText(exchange, 500, "Frontend build not found. Run: cd frontend && npm run build");
            return;
        }

        // Prevent path traversal, normalize to relative path under static root
        String relative = requestPath.startsWith("/") ? requestPath.substring(1) : requestPath;
        if (relative.isEmpty()) {
            relative = "index.html";
        }

        Path requestedFile = staticRoot.resolve(relative).normalize();

        // If not inside root, reject
        if (!requestedFile.startsWith(staticRoot.normalize())) {
            sendText(exchange, 403, "Forbidden");
            return;
        }

        // Serve static file if present, else fallback to SPA entry
        if (Files.exists(requestedFile) && !Files.isDirectory(requestedFile)) {
            sendFile(exchange, requestedFile, contentTypeFor(requestedFile));
            return;
        }

        // Fallback for client-side routes
        Path indexFile = staticRoot.resolve("index.html");
        if (Files.exists(indexFile)) {
            sendFile(exchange, indexFile, "text/html; charset=utf-8");
            return;
        }

        sendText(exchange, 404, "Not Found");
    }

    private void handleJoin(HttpExchange exchange) throws IOException {
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendText(exchange, 405, "Method Not Allowed");
            return;
        }

        Map<String, String> form = parseForm(exchange);
        String name = form.getOrDefault("name", "").trim();
        if (name.isEmpty()) {
            sendJson(exchange, 400, "{\"ok\":false,\"message\":\"Name is required\"}");
            return;
        }

        engine.addPlayerIfAbsent(name);

        if (!engine.isGameRunning()) {
            engine.startGame();
        }

        String body = "{\"ok\":true,\"player\":\"" + jsonEscape(name) + "\"}";
        sendJson(exchange, 200, body);
    }

    private void handleCommand(HttpExchange exchange) throws IOException {
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendText(exchange, 405, "Method Not Allowed");
            return;
        }

        Map<String, String> form = parseForm(exchange);
        String name = form.getOrDefault("name", "").trim();
        String command = form.getOrDefault("command", "").trim();

        if (name.isEmpty() || command.isEmpty()) {
            sendJson(exchange, 400, "{\"ok\":false,\"message\":\"name and command required\"}");
            return;
        }

        String result = engine.executeCommand(name, command);
        String body = "{\"ok\":true,\"result\":\"" + jsonEscape(result) + "\"}";
        sendJson(exchange, 200, body);
    }

    private void handleState(HttpExchange exchange) throws IOException {
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendText(exchange, 405, "Method Not Allowed");
            return;
        }

        parseQuery(exchange.getRequestURI().getRawQuery());
        
        List<String> events;
        synchronized (eventFeed) {
            events = new ArrayList<>(eventFeed);
        }

        StringBuilder json = new StringBuilder();
        json.append("{");
        json.append("\"running\":").append(engine.isGameRunning()).append(",");
        json.append("\"round\":").append(engine.getGameState().getCurrentRound()).append(",");
        
        // Add zone information
        Zone zone = engine.getGameState().getSafeZone();
        json.append("\"zone\":{");
        json.append("\"centerX\":").append(zone.getCenterX()).append(",");
        json.append("\"centerY\":").append(zone.getCenterY()).append(",");
        json.append("\"radius\":").append(zone.getRadius());
        json.append("},");
        
        // Add zone shrink timer
        long timeUntilShrink = engine.getGameState().getTimeUntilZoneShrink();
        json.append("\"timeUntilZoneShrink\":").append(timeUntilShrink).append(",");

        // Add players with full details
        json.append("\"players\":[");
        Collection<Player> allPlayers = engine.getGameState().getAllPlayers();
        int playerCount = 0;
        for (Player p : allPlayers) {
            if (playerCount > 0) json.append(",");
            json.append(playerToJson(p));
            playerCount++;
        }
        json.append("],");

        // Add items (uncollected loot on the ground)
        json.append("\"items\":[");
        int itemCount = 0;
        for (Item item : engine.getGameState().getItems()) {
            if (item.isCollected()) continue;
            if (itemCount > 0) json.append(",");
            json.append("{\"type\":\"").append(item.getType().getDisplayName())
                .append("\",\"x\":").append(item.getX())
                .append(",\"y\":").append(item.getY()).append("}");
            itemCount++;
        }
        json.append("],");

        // Add events
        json.append("\"events\":[");
        for (int i = 0; i < events.size(); i++) {
            if (i > 0) json.append(",");
            json.append("\"").append(jsonEscape(events.get(i))).append("\"");
        }
        json.append("]");
        json.append("}");

        sendJson(exchange, 200, json.toString());
    }

    private String playerToJson(Player player) {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"id\":\"").append(jsonEscape(player.getId())).append("\",");
        sb.append("\"x\":").append(player.getX()).append(",");
        sb.append("\"y\":").append(player.getY()).append(",");
        sb.append("\"health\":").append(player.getHealth()).append(",");
        sb.append("\"armor\":").append(player.getArmor()).append(",");
        sb.append("\"alive\":").append(player.isAlive()).append(",");
        sb.append("\"kills\":").append(player.getKills()).append(",");
        sb.append("\"deaths\":").append(player.getDeaths()).append(",");
        
        // Add weapon details
        if (player.getWeapon() != null) {
            sb.append("\"weapon\":{");
            sb.append("\"name\":\"").append(jsonEscape(player.getWeapon().getName())).append("\",");
            sb.append("\"damage\":").append(player.getWeapon().getAverageDamage());
            sb.append("}");
        } else {
            sb.append("\"weapon\":null");
        }
        
        sb.append("}");
        return sb.toString();
    }

    private Map<String, String> parseForm(HttpExchange exchange) throws IOException {
        String body;
        try (InputStream in = exchange.getRequestBody()) {
            body = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
        return parseQuery(body);
    }

    private Map<String, String> parseQuery(String raw) {
        Map<String, String> map = new HashMap<>();
        if (raw == null || raw.isEmpty()) {
            return map;
        }

        String[] pairs = raw.split("&");
        for (String pair : pairs) {
            String[] kv = pair.split("=", 2);
            String key = urlDecode(kv[0]);
            String value = kv.length > 1 ? urlDecode(kv[1]) : "";
            map.put(key, value);
        }
        return map;
    }

    private String urlDecode(String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }

    private void sendFile(HttpExchange exchange, Path path, String contentType) throws IOException {
        if (!Files.exists(path)) {
            sendText(exchange, 404, "Not Found");
            return;
        }

        byte[] bytes = Files.readAllBytes(path);
        exchange.getResponseHeaders().set("Content-Type", contentType);
        applyCacheHeaders(exchange, path);
        exchange.sendResponseHeaders(200, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private void applyCacheHeaders(HttpExchange exchange, Path path) {
        String name = path.getFileName().toString().toLowerCase(Locale.ROOT);

        // HTML should not be cached so new deploys are picked up immediately.
        if (name.endsWith(".html")) {
            exchange.getResponseHeaders().set("Cache-Control", "no-cache, no-store, must-revalidate");
            exchange.getResponseHeaders().set("Pragma", "no-cache");
            exchange.getResponseHeaders().set("Expires", "0");
            return;
        }

        // Vite build assets are content-hashed (e.g. index-abc123.js), safe for long immutable cache.
        if (isHashedAsset(path)) {
            exchange.getResponseHeaders().set("Cache-Control", "public, max-age=31536000, immutable");
            return;
        }

        // Default cache for other static files.
        exchange.getResponseHeaders().set("Cache-Control", "public, max-age=3600");
    }

    private boolean isHashedAsset(Path path) {
        String normalized = path.toString().replace('\\', '/').toLowerCase(Locale.ROOT);
        String name = path.getFileName().toString();

        // Vite typically outputs hashed bundles under dist/assets
        if (normalized.contains("/assets/")) {
            return true;
        }

        int dot = name.lastIndexOf('.');
        if (dot <= 0) return false;
        String base = name.substring(0, dot);
        int dash = base.lastIndexOf('-');
        if (dash < 0 || dash == base.length() - 1) return false;
        String suffix = base.substring(dash + 1);

        // Accept alphanumeric hash-like suffixes (6+ chars)
        if (suffix.length() < 6) return false;
        for (int i = 0; i < suffix.length(); i++) {
            if (!Character.isLetterOrDigit(suffix.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    private String contentTypeFor(Path path) {
        String name = path.getFileName().toString().toLowerCase(Locale.ROOT);
        if (name.endsWith(".html")) return "text/html; charset=utf-8";
        if (name.endsWith(".js") || name.endsWith(".mjs")) return "application/javascript; charset=utf-8";
        if (name.endsWith(".css")) return "text/css; charset=utf-8";
        if (name.endsWith(".json")) return "application/json; charset=utf-8";
        if (name.endsWith(".svg")) return "image/svg+xml";
        if (name.endsWith(".png")) return "image/png";
        if (name.endsWith(".jpg") || name.endsWith(".jpeg")) return "image/jpeg";
        if (name.endsWith(".gif")) return "image/gif";
        if (name.endsWith(".ico")) return "image/x-icon";
        if (name.endsWith(".woff")) return "font/woff";
        if (name.endsWith(".woff2")) return "font/woff2";
        if (name.endsWith(".ttf")) return "font/ttf";
        return "application/octet-stream";
    }

    private void sendJson(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private void sendText(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=utf-8");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private String jsonEscape(String value) {
        if (value == null) return "";
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "");
    }
}
