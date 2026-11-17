package com.corp.ai.agents.service;

import com.unboundid.ldap.sdk.BindResult;
import com.unboundid.ldap.sdk.LDAPConnection;
import com.unboundid.ldap.sdk.LDAPConnectionOptions;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldap.sdk.ResultCode;
import com.unboundid.ldap.sdk.SearchResult;
import com.unboundid.ldap.sdk.SearchResultEntry;
import com.unboundid.ldap.sdk.SearchScope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class LdapExecutorService {

    private static final Logger log = LoggerFactory.getLogger(LdapExecutorService.class);

    public record LdapBindResult(boolean success, long latencyMs, String message) {}

    public LdapBindResult testBind(String host, int port, String bindDn, String bindPassword, int timeoutMs) {
        log.info("[LDAP] Testando bind em {}:{} como {}", host, port, bindDn);
        long start = System.currentTimeMillis();

        LDAPConnectionOptions options = new LDAPConnectionOptions();
        options.setConnectTimeoutMillis(timeoutMs);
        options.setResponseTimeoutMillis(timeoutMs);

        try (LDAPConnection connection = new LDAPConnection(options, host, port)) {
            BindResult bindResult = connection.bind(bindDn, bindPassword);
            long latency = System.currentTimeMillis() - start;

            if (bindResult.getResultCode() == ResultCode.SUCCESS) {
                return new LdapBindResult(true, latency, "Bind autenticado com sucesso.");
            } else {
                return new LdapBindResult(false, latency, "Falha de bind: " + bindResult.getDiagnosticMessage());
            }
        } catch (LDAPException e) {
            long latency = System.currentTimeMillis() - start;
            log.warn("[LDAP Error] Erro ao conectar em {}:{} -> {}", host, port, e.getMessage());
            return new LdapBindResult(false, latency, "Falha de conexão LDAP (VPN ou host inacessível): " + e.getMessage());
        }
    }

    public Map<String, Object> checkContextCsn(String host, int port, String bindDn, String bindPassword, String baseDn, int timeoutMs) {
        Map<String, Object> result = new HashMap<>();
        LDAPConnectionOptions options = new LDAPConnectionOptions();
        options.setConnectTimeoutMillis(timeoutMs);
        options.setResponseTimeoutMillis(timeoutMs);

        try (LDAPConnection connection = new LDAPConnection(options, host, port)) {
            if (bindDn != null && !bindDn.isBlank()) {
                connection.bind(bindDn, bindPassword);
            }
            SearchResult searchResult = connection.search(baseDn, SearchScope.BASE, "(objectClass=*)", "contextCSN");
            if (searchResult.getEntryCount() > 0) {
                SearchResultEntry entry = searchResult.getSearchEntries().get(0);
                String csn = entry.getAttributeValue("contextCSN");
                result.put("success", true);
                result.put("contextCsn", csn != null ? csn : "PRESENTE_SEM_CSN");
                result.put("entryDn", entry.getDN());
            } else {
                result.put("success", false);
                result.put("message", "Base DN não retornou entradas.");
            }
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "Falha ao consultar LDAP: " + e.getMessage());
        }

        return result;
    }
}
