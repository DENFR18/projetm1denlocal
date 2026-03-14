package com.example;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import java.util.Map;
import java.util.LinkedHashMap;
import java.time.Instant;
import java.io.*;
import java.nio.file.*;
import java.util.concurrent.TimeUnit;

@SpringBootApplication
@RestController
@CrossOrigin(origins = "*")
public class App {

    public static void main(String[] args) {
        SpringApplication.run(App.class, args);
    }

    @GetMapping("/api/hello")
    public ResponseEntity<Map<String, Object>> hello(
            @RequestParam(defaultValue = "World") String name) {
        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("message", "Hello, " + name + " !");
        resp.put("from", "Spring Boot sur AWS EKS");
        resp.put("timestamp", Instant.now().toString());
        return ResponseEntity.ok(resp);
    }

    @PostMapping("/api/echo")
    public ResponseEntity<Map<String, Object>> echo(@RequestBody Map<String, Object> body) {
        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("echo", body);
        resp.put("receivedAt", Instant.now().toString());
        return ResponseEntity.ok(resp);
    }

    @GetMapping("/api/status")
    public ResponseEntity<Map<String, Object>> status() {
        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("status", "UP");
        resp.put("service", "ProjetM1 API");
        resp.put("version", "2.0.0");
        resp.put("runtime", "Java " + System.getProperty("java.version"));
        resp.put("uptime_ms", ProcessHandle.current().info().startInstant()
                .map(s -> Instant.now().toEpochMilli() - s.toEpochMilli()).orElse(-1L));
        resp.put("timestamp", Instant.now().toString());
        return ResponseEntity.ok(resp);
    }

    @PostMapping("/api/deployments/deploy")
    public ResponseEntity<Map<String, Object>> deploy(@RequestBody Map<String, Object> body) {
        String language = (String) body.getOrDefault("language", "python");
        String code     = (String) body.getOrDefault("code", "");

        Map<String, Object> resp = new LinkedHashMap<>();

        if (code.isBlank()) {
            resp.put("output", "❌ Aucun code fourni.");
            return ResponseEntity.badRequest().body(resp);
        }

        try {
            // Écriture du code dans un fichier temporaire
            String ext      = language.equals("javascript") ? ".js" : ".py";
            Path   tmpFile  = Files.createTempFile("pod-exec-", ext);
            Files.writeString(tmpFile, code);

            // Commande d'exécution selon le langage
            String[] cmd = language.equals("javascript")
                    ? new String[]{"node", tmpFile.toString()}
                    : new String[]{"python3", tmpFile.toString()};

            long start   = System.currentTimeMillis();
            Process proc = new ProcessBuilder(cmd)
                    .redirectErrorStream(true)
                    .start();

            boolean finished = proc.waitFor(10, TimeUnit.SECONDS);
            long    elapsed  = System.currentTimeMillis() - start;

            String output = new String(proc.getInputStream().readAllBytes());
            Files.deleteIfExists(tmpFile);

            if (!finished) {
                proc.destroyForcibly();
                resp.put("output", "⏱️ Timeout : le code a dépassé 10 secondes d'exécution.");
            } else {
                String header = String.format(
                    "✅ Pod démarré · %s · exit(%d) · %dms%n─────────────────────────────%n",
                    language.equals("javascript") ? "Node.js 18" : "Python 3", proc.exitValue(), elapsed
                );
                resp.put("output", header + (output.isBlank() ? "(aucune sortie)" : output));
            }

        } catch (Exception e) {
            resp.put("output", "❌ Erreur d'exécution : " + e.getMessage());
        }

        return ResponseEntity.ok(resp);
    }
}
