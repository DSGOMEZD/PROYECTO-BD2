package com.basesdatos2.backendBD2;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

@SpringBootApplication
public class BackendBd2Application {

    private static final Logger log = LoggerFactory.getLogger(BackendBd2Application.class);
    private static volatile Process procesoFrontend;

    public static void main(String[] args) {
        liberarPuertoPrevio(8080);
        SpringApplication.run(BackendBd2Application.class, args);
    }

    @EventListener(ApplicationReadyEvent.class)
    public void alIniciarBackend() {
        Thread hiloFrontend = new Thread(this::iniciarFrontendAngular, "frontend-launcher");
        hiloFrontend.setDaemon(true);
        hiloFrontend.start();
    }

    private void iniciarFrontendAngular() {
        try {
            log.info("==================================================================");
            log.info("MotoGo Backend activo en:  http://localhost:8080");

            if (puertoEnUso(4200)) {
                log.info("MotoGo Frontend activo en: http://localhost:4200");
                log.info("==================================================================");
                abrirNavegador("http://localhost:4200");
                return;
            }

            File dirFrontend = localizarCarpetaFrontend();
            File ejecutableNode = localizarNodeExe();

            if (dirFrontend == null || ejecutableNode == null) {
                log.info("MotoGo Web disponible directamente en: http://localhost:8080");
                log.info("==================================================================");
                abrirNavegador("http://localhost:8080");
                return;
            }

            File ngCli = new File(dirFrontend, "node_modules/@angular/cli/bin/ng.js");
            if (!ngCli.exists()) {
                log.info("MotoGo Web disponible directamente en: http://localhost:8080");
                log.info("==================================================================");
                abrirNavegador("http://localhost:8080");
                return;
            }

            log.info("Iniciando automáticamente el Frontend Angular en puerto 4200...");
            log.info("==================================================================");

            ProcessBuilder pb = new ProcessBuilder(
                    ejecutableNode.getAbsolutePath(),
                    ngCli.getAbsolutePath(),
                    "serve",
                    "--host", "127.0.0.1",
                    "--port", "4200"
            );
            pb.directory(dirFrontend);
            pb.redirectErrorStream(true);

            Map<String, String> env = pb.environment();
            String nodeFolder = ejecutableNode.getParent();
            String pathActual = env.getOrDefault("Path", env.getOrDefault("PATH", ""));
            env.put("Path", nodeFolder + File.pathSeparator + pathActual);

            procesoFrontend = pb.start();

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                Process proc = procesoFrontend;
                if (proc != null && proc.isAlive()) {
                    proc.descendants().forEach(ProcessHandle::destroyForcibly);
                    proc.destroyForcibly();
                }
            }));

            boolean navegadorAbierto = false;
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(procesoFrontend.getInputStream(), StandardCharsets.UTF_8))) {
                String linea;
                while ((linea = reader.readLine()) != null) {
                    System.out.println("[FRONTEND] " + linea);
                    if (!navegadorAbierto && (linea.contains("localhost:4200") || linea.contains("Application bundle generation complete"))) {
                        navegadorAbierto = true;
                        log.info(">>> Frontend Angular LISTO en: http://localhost:4200 <<<");
                        abrirNavegador("http://localhost:4200");
                    }
                }
            }
        } catch (Exception e) {
            log.warn("No se pudo lanzar ng serve en puerto 4200 ({}). Puedes usar http://localhost:8080", e.getMessage());
            abrirNavegador("http://localhost:8080");
        }
    }

    private static void liberarPuertoPrevio(int puerto) {
        if (!puertoEnUso(puerto)) {
            return;
        }
        long miPid = ProcessHandle.current().pid();
        try {
            Process proc = new ProcessBuilder("cmd.exe", "/c", "netstat -ano | findstr :" + puerto + " | findstr LISTENING")
                    .redirectErrorStream(true)
                    .start();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(proc.getInputStream()))) {
                String linea;
                while ((linea = reader.readLine()) != null) {
                    String[] partes = linea.trim().split("\\s+");
                    if (partes.length >= 5) {
                        long pid = Long.parseLong(partes[partes.length - 1]);
                        if (pid > 0 && pid != miPid) {
                            ProcessHandle.of(pid).ifPresent(ph -> {
                                System.out.println("[BackendBd2Application] Liberando instancia previa en puerto " + puerto + " (PID " + pid + ")...");
                                ph.descendants().forEach(ProcessHandle::destroyForcibly);
                                ph.destroyForcibly();
                            });
                        }
                    }
                }
            }
            for (int i = 0; i < 20 && puertoEnUso(puerto); i++) {
                Thread.sleep(150);
            }
        } catch (Exception ignored) {
        }
    }

    private static boolean puertoEnUso(int puerto) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress("127.0.0.1", puerto), 300);
            return true;
        } catch (Exception ignored) {
        }
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress("::1", puerto), 300);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private static File localizarCarpetaFrontend() {
        Path actual = Paths.get("").toAbsolutePath();
        List<Path> candidatos = List.of(
                actual.resolve("FRONT-BD2/fronted-BD2"),
                actual.resolve("../FRONT-BD2/fronted-BD2"),
                actual.resolve("../../FRONT-BD2/fronted-BD2")
        );
        for (Path candidato : candidatos) {
            Path norm = candidato.normalize();
            if (Files.isDirectory(norm) && Files.exists(norm.resolve("package.json"))) {
                return norm.toFile();
            }
        }
        return null;
    }

    private static File localizarNodeExe() {
        String localAppData = System.getenv("LOCALAPPDATA");
        String programFiles = System.getenv("ProgramFiles");
        List<String> rutas = List.of(
                localAppData != null ? localAppData + "\\Programs\\nodejs\\node.exe" : "",
                programFiles != null ? programFiles + "\\nodejs\\node.exe" : "",
                "C:\\Program Files\\nodejs\\node.exe"
        );
        for (String ruta : rutas) {
            if (!ruta.isEmpty()) {
                File f = new File(ruta);
                if (f.exists() && f.canExecute()) {
                    return f;
                }
            }
        }
        String pathEnv = System.getenv("PATH");
        if (pathEnv != null) {
            for (String dir : pathEnv.split(File.pathSeparator)) {
                File f = new File(dir, "node.exe");
                if (f.exists() && f.canExecute()) {
                    return f;
                }
            }
        }
        return null;
    }

    private static void abrirNavegador(String url) {
        try {
            new ProcessBuilder("rundll32", "url.dll,FileProtocolHandler", url).start();
        } catch (Exception ignored) {
        }
    }
}

