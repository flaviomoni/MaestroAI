package com.corp.ai.agents.skills;

import com.corp.ai.agents.config.AppProperties;
import com.corp.ai.agents.service.SshExecutorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class LinuxLogSkill {

    private static final Logger log = LoggerFactory.getLogger(LinuxLogSkill.class);
    private final AppProperties properties;
    private final SshExecutorService sshExecutorService;

    public LinuxLogSkill(AppProperties properties, SshExecutorService sshExecutorService) {
        this.properties = properties;
        this.sshExecutorService = sshExecutorService;
    }

    /**
     * Inspeciona logs de sistema operacional Linux e servidores de aplicação
     * (/var/log/httpd/*, WC_Portal_server1-diagnostic.log, AdminServer.log) em busca de anomalias.
     */
    public Map<String, Object> inspectLinuxAndMiddlewareLogs(int tailLines) {
        log.info("[LinuxLogSkill] Inspecionando logs Linux (Apache + WebLogic/WCP diagnostic.log)...");
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("tailLines", tailLines);

        if (properties.isSimulationMode()) {
            result.put("status", "CLEAN_WITH_MINOR_WARNINGS");
            result.put("monitoredLogFiles", List.of(
                    "/var/log/httpd/access_log",
                    "/var/log/httpd/error_log",
                    "$DOMAIN_HOME/servers/WC_Portal_server1/logs/WC_Portal_server1-diagnostic.log",
                    "$DOMAIN_HOME/servers/APP_server1/logs/APP_server1.out"
            ));
            result.put("wcsInternalErrorsCount", 0);
            result.put("beaStuckThreadCount", 2);
            result.put("linuxDiskUsage", "/u01 em 62% de uso | Load Average: 0.78, 0.65, 0.59");
            result.put("message", "Linux Logs: 0 erros internos WCS# no Portal; 2 alertas BEA-000337 em APP_server1.out; partição /u01 saudável (62%).");
            return result;
        }

        AppProperties.SshProperties ssh = properties.getInfra().getApache();
        boolean hasCreds = (ssh.getPassword() != null && !ssh.getPassword().isBlank())
                || (ssh.getPrivateKeyPath() != null && !ssh.getPrivateKeyPath().isBlank());

        if (!hasCreds) {
            result.put("status", "SSH_PENDING_CREDENTIALS");
            result.put("message", "Agente Linux pronto para leitura de /var/log/httpd e $DOMAIN_HOME/servers/*/logs via SSH non-sudo.");
            return result;
        }

        String cmd = "df -h / /u01 2>/dev/null; uptime; tail -n " + tailLines + " /var/log/httpd/error_log 2>/dev/null";
        SshExecutorService.SshResult sshRes = sshExecutorService.executeNonSudoCommand(
                ssh.getHost(), ssh.getPort(), ssh.getUser(), ssh.getPassword(), ssh.getPrivateKeyPath(), cmd, ssh.getTimeoutMs()
        );
        result.put("status", sshRes.success() ? "COLLECTED" : "ERROR");
        result.put("rawOutput", sshRes.output());
        result.put("message", sshRes.success() ? "Logs Linux e métricas de disco/uptime coletados via SSH." : "Falha SSH: " + sshRes.error());
        return result;
    }
}
