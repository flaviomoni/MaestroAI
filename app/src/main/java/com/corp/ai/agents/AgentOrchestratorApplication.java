package com.corp.ai.agents;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.io.File;
import java.nio.file.Files;
import java.util.List;

@SpringBootApplication
@EnableAsync
@EnableScheduling
public class AgentOrchestratorApplication {

    public static void main(String[] args) {
        // Carregador automático de arquivo .env local (permite usar o mesmo .env no Java e no Podman)
        loadDotEnvIfExists();
        SpringApplication.run(AgentOrchestratorApplication.class, args);
    }

    private static void loadDotEnvIfExists() {
        File envFile = new File(".env");
        if (!envFile.exists()) {
            envFile = new File("agent-orchestrator/.env");
        }

        if (envFile.exists()) {
            try {
                List<String> lines = Files.readAllLines(envFile.toPath());
                for (String line : lines) {
                    line = line.trim();
                    if (line.isEmpty() || line.startsWith("#") || !line.contains("=")) {
                        continue;
                    }
                    int idx = line.indexOf('=');
                    String key = line.substring(0, idx).trim();
                    String value = line.substring(idx + 1).trim();
                    // Define como System Property se ainda não estiver definida no ambiente
                    if (System.getProperty(key) == null && System.getenv(key) == null) {
                        System.setProperty(key, value);
                    }
                }
                System.out.println("[AgentOrchestrator] Arquivo .env carregado com sucesso de: " + envFile.getAbsolutePath());
            } catch (Exception e) {
                System.err.println("[AgentOrchestrator] Aviso: Não foi possível ler .env: " + e.getMessage());
            }
        }
    }
}
