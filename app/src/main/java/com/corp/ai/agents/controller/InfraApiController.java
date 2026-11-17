package com.corp.ai.agents.controller;

import com.corp.ai.agents.config.AppProperties;
import com.corp.ai.agents.model.InfraNodeStatus;
import com.corp.ai.agents.service.InfraStatusService;
import com.corp.ai.agents.skills.ApacheSkill;
import com.corp.ai.agents.skills.JavaSpringSkill;
import com.corp.ai.agents.skills.OpenLdapSkill;
import com.corp.ai.agents.skills.OracleDbaSkill;
import com.corp.ai.agents.skills.WebLogicWcpSkill;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/infra")
public class InfraApiController {

    private final AppProperties properties;
    private final InfraStatusService infraStatusService;
    private final OpenLdapSkill openLdapSkill;
    private final OracleDbaSkill oracleDbaSkill;
    private final WebLogicWcpSkill webLogicWcpSkill;
    private final ApacheSkill apacheSkill;
    private final JavaSpringSkill javaSpringSkill;

    public InfraApiController(AppProperties properties,
                              InfraStatusService infraStatusService,
                              OpenLdapSkill openLdapSkill,
                              OracleDbaSkill oracleDbaSkill,
                              WebLogicWcpSkill webLogicWcpSkill,
                              ApacheSkill apacheSkill,
                              JavaSpringSkill javaSpringSkill) {
        this.properties = properties;
        this.infraStatusService = infraStatusService;
        this.openLdapSkill = openLdapSkill;
        this.oracleDbaSkill = oracleDbaSkill;
        this.webLogicWcpSkill = webLogicWcpSkill;
        this.apacheSkill = apacheSkill;
        this.javaSpringSkill = javaSpringSkill;
    }

    @GetMapping("/status")
    public ResponseEntity<List<InfraNodeStatus>> getStatus() {
        return ResponseEntity.ok(infraStatusService.getClusterSnapshot());
    }

    @GetMapping("/vpn")
    public ResponseEntity<Map<String, Object>> getVpnStatus() {
        return ResponseEntity.ok(infraStatusService.checkVpnStatus());
    }

    @GetMapping("/diagnostics")
    public ResponseEntity<Map<String, Object>> runLiveDiagnostics() {
        Map<String, Object> report = new LinkedHashMap<>();
        report.put("timestamp", Instant.now().toString());

        // 1. OpenLDAP
        Map<String, Object> ldapReport = new LinkedHashMap<>();
        ldapReport.put("bindTest", openLdapSkill.testBind("admin"));
        ldapReport.put("replicationCheck", openLdapSkill.checkReplicationLag());
        report.put("openldap", ldapReport);

        // 2. Oracle Apps DB
        Map<String, Object> appsDbReport = new LinkedHashMap<>();
        appsDbReport.put("ping", oracleDbaSkill.testPing("APPSDB"));
        appsDbReport.put("invalidObjects", oracleDbaSkill.checkAppLocksAndBlockers("APPSDB"));
        report.put("oracleAppsDb", appsDbReport);

        // 3. Oracle Portal DB
        Map<String, Object> wcpDbReport = new LinkedHashMap<>();
        wcpDbReport.put("ping", oracleDbaSkill.testPing("WCPDB"));
        wcpDbReport.put("invalidObjects", oracleDbaSkill.checkAppLocksAndBlockers("WCPDB"));
        report.put("oracleWcpDb", wcpDbReport);

        // 4. WebLogic 12c Cluster & ThreadPool Runtimes
        Map<String, Object> wlsReport = new LinkedHashMap<>();
        wlsReport.put("serversTopology", webLogicWcpSkill.getClusterTopologyOverview());
        wlsReport.put("portalNode1Runtime", webLogicWcpSkill.queryServerStatus("WC_Portal_server1"));
        wlsReport.put("portalNode2Runtime", webLogicWcpSkill.queryServerStatus("WC_Portal_server2"));
        wlsReport.put("appServer1Runtime", webLogicWcpSkill.queryServerStatus("APP_server1"));
        wlsReport.put("threadDumpAnalysis", webLogicWcpSkill.captureThreadDumpSummary("APP_server1"));
        wlsReport.put("dataSources", webLogicWcpSkill.checkDataSourcePools("mds-SpacesDS"));
        report.put("weblogic", wlsReport);

        // 5. Apache DMZ
        String apacheHost = properties.getInfra().getApache().getHost();
        Map<String, Object> apacheReport = new LinkedHashMap<>();
        apacheReport.put("perimeterHealth", apacheSkill.checkUrlHealth("https://" + apacheHost));
        apacheReport.put("sslCertificate", apacheSkill.checkSslCertExpiry(apacheHost));
        apacheReport.put("errorLogStatus", apacheSkill.parseErrorLogs(20));
        report.put("apache", apacheReport);

        // 6. Java / Spring JVM & Actuator
        Map<String, Object> javaSpringReport = new LinkedHashMap<>();
        javaSpringReport.put("actuatorHealth", javaSpringSkill.queryActuatorHealth());
        javaSpringReport.put("jvmAppServer1", javaSpringSkill.checkJvmGcMemory("APP_server1"));
        report.put("javaSpring", javaSpringReport);

        return ResponseEntity.ok(report);
    }
}
