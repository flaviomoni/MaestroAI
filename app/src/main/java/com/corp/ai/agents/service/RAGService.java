package com.corp.ai.agents.service;

import com.corp.ai.agents.config.AppProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * RAG Local-First: consulta primeiro a base de conhecimento em Markdown versionada no repositório
 * (pasta {@code knowledge/}) antes de qualquer chamada a LLM ou MCP externo (custo = 0 tokens)
 * e grava automaticamente novos aprendizados dos agentes.
 */
@Service
public class RAGService {

    private static final Logger log = LoggerFactory.getLogger(RAGService.class);
    private static final DateTimeFormatter TS_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    static final String NOTE_INDEX = "00_Indice_Memoria_Local_First.md";
    static final String NOTE_CYBERSEC = "cybersec/Aprendizado_CyberSec_Ataques.md";
    static final String NOTE_CYBERSEC_CATALOG = "cybersec/Catalogo_Skills_SecOps.md";
    static final String NOTE_WEBCENTER = "webcenter/Aprendizado_WebCenter_Layouts_e_Portais.md";
    static final String NOTE_INFRA = "infra/Aprendizado_Infra_WebLogic_Oracle_LDAP.md";
    static final String NOTE_RUNBOOK_504 = "runbooks/Runbook_Erro_504_Portal.md";
    static final String NOTE_RUNBOOK_STUCK = "runbooks/Runbook_Thread_Stuck_WLS.md";
    static final String NOTE_RUNBOOK_LDAP = "runbooks/Runbook_Falha_LDAP_Auth.md";
    static final String NOTE_RUNBOOK_DBA = "runbooks/Runbook_Oracle_DBA_Provisionamento.md";

    private final AppProperties properties;

    public RAGService(AppProperties properties) {
        this.properties = properties;
    }

    /**
     * Busca determinística na base local (knowledge/). Retorna trechos compactados das notas relevantes.
     */
    public Optional<String> lookupRunbookSnippet(String queryOrError) {
        String lower = queryOrError.toLowerCase();
        Set<String> candidateFiles = new LinkedHashSet<>();

        if (containsAny(lower, "ataque", "segurança", "seguranca", "cyber", "sqli", "xss", "brute",
                "captcha", "waf", "invasão", "invasao", "bloquear")) {
            candidateFiles.add(NOTE_CYBERSEC);
            candidateFiles.add(NOTE_CYBERSEC_CATALOG);
        }
        if (containsAny(lower, "portal", "webcenter", "layout", "aar", "jspx", "template",
                "cliente", "provisionar", "criar portal")) {
            candidateFiles.add(NOTE_WEBCENTER);
        }
        if (containsAny(lower, "504", "502", "gateway timeout")) {
            candidateFiles.add(NOTE_RUNBOOK_504);
        }
        if (containsAny(lower, "bea-000337", "stuck", "thread", "weblogic", "managed server", "datasource")) {
            candidateFiles.add(NOTE_RUNBOOK_STUCK);
            candidateFiles.add(NOTE_INFRA);
        }
        if (containsAny(lower, "ldap", "bind", "auth", "login")) {
            candidateFiles.add(NOTE_RUNBOOK_LDAP);
            candidateFiles.add(NOTE_INFRA);
        }
        if (containsAny(lower, "lock", "oracle", "deadlock", "schema", "linux", "log")) {
            candidateFiles.add(NOTE_INFRA);
            candidateFiles.add(NOTE_RUNBOOK_DBA);
        }
        if (candidateFiles.isEmpty()) {
            candidateFiles.add(NOTE_INDEX);
        }

        StringBuilder combined = new StringBuilder();
        for (String relPath : candidateFiles) {
            Optional<String> snippet = readSnippetFromFile(relPath, 900);
            if (snippet.isPresent()) {
                combined.append("### [Fonte local: ").append(relPath).append("]\n")
                        .append(snippet.get()).append("\n\n");
            }
        }
        return combined.length() == 0 ? Optional.empty() : Optional.of(combined.toString().trim());
    }

    /**
     * Grava o aprendizado de um agente na nota Markdown do domínio correspondente.
     */
    public Map<String, Object> recordAgentLearning(String domain, String agentName,
                                                   String contextSummary, String actionResolution) {
        Map<String, Object> status = new LinkedHashMap<>();
        String targetFile = switch (domain.toUpperCase()) {
            case "CYBERSEC", "SECURITY" -> NOTE_CYBERSEC;
            case "WEBCENTER", "PORTAL", "LAYOUT" -> NOTE_WEBCENTER;
            default -> NOTE_INFRA;
        };

        status.put("targetNote", targetFile);
        status.put("agentName", agentName);

        try {
            Path resolved = resolveKnowledgeFile(targetFile);
            if (Files.exists(resolved)) {
                String entry = String.format(
                        "%n- **[%s | %s]** **Caso:** %s → **Solução/Defesa Aplicada:** %s",
                        LocalDateTime.now().format(TS_FMT),
                        agentName,
                        contextSummary.replace("\n", " "),
                        actionResolution.replace("\n", " "));
                Files.writeString(resolved, entry, StandardCharsets.UTF_8, StandardOpenOption.APPEND);
                log.info("[Knowledge Learning] Novo aprendizado documentado em {} por {}",
                        resolved.getFileName(), agentName);
                status.put("recorded", true);
                return status;
            }
        } catch (IOException e) {
            log.warn("[Knowledge Learning] Não foi possível gravar aprendizado em {}: {}", targetFile, e.getMessage());
        }
        status.put("recorded", false);
        return status;
    }

    private static boolean containsAny(String text, String... terms) {
        for (String term : terms) {
            if (text.contains(term)) {
                return true;
            }
        }
        return false;
    }

    private Path resolveKnowledgeFile(String relativePath) {
        Path base = Path.of(properties.getRag().getKnowledgePath());
        Path candidate = base.resolve(relativePath).normalize();
        if (Files.exists(candidate)) {
            return candidate;
        }
        // Fallback: execução a partir da raiz do repositório
        List<Path> fallbacks = new ArrayList<>();
        fallbacks.add(Path.of("knowledge").resolve(relativePath).normalize());
        fallbacks.add(Path.of("..", "knowledge").resolve(relativePath).normalize());
        for (Path fallback : fallbacks) {
            if (Files.exists(fallback)) {
                return fallback;
            }
        }
        return candidate;
    }

    private Optional<String> readSnippetFromFile(String relativePath, int maxChars) {
        try {
            Path fullPath = resolveKnowledgeFile(relativePath);
            if (Files.exists(fullPath)) {
                String content = Files.readString(fullPath, StandardCharsets.UTF_8);
                String snippet = content.length() > maxChars
                        ? content.substring(0, maxChars) + "\n...[snippet local compactado - 0 tokens]..."
                        : content;
                log.info("[RAG Local-First] Nota carregada: {} ({} chars)", fullPath.getFileName(), snippet.length());
                return Optional.of(snippet);
            }
        } catch (IOException e) {
            log.warn("[RAG Local] Erro ao ler nota: {}", e.getMessage());
        }
        return Optional.empty();
    }
}
