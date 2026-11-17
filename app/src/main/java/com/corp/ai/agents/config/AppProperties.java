package com.corp.ai.agents.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "corp.ai")
public class AppProperties {

    private GeminiProperties gemini = new GeminiProperties();
    private RagProperties rag = new RagProperties();
    private InfraProperties infra = new InfraProperties();
    private boolean simulationMode = false;

    public static class GeminiProperties {
        private String apiKey = "";
        private String model = "gemini-2.0-flash";
        private double temperature = 0.2;
        private int maxOutputTokens = 2048;

        public String getApiKey() { return apiKey; }
        public void setApiKey(String apiKey) { this.apiKey = apiKey; }
        public String getModel() { return model; }
        public void setModel(String model) { this.model = model; }
        public double getTemperature() { return temperature; }
        public void setTemperature(double temperature) { this.temperature = temperature; }
        public int getMaxOutputTokens() { return maxOutputTokens; }
        public void setMaxOutputTokens(int maxOutputTokens) { this.maxOutputTokens = maxOutputTokens; }
    }

    public static class RagProperties {
        // Base de conhecimento Local-First (Markdown versionado junto ao projeto)
        private String knowledgePath = "../knowledge";
        // Projeto de layout/page template padrão dos portais (pode apontar para um layout externo local)
        private String layoutProjectPath = "../layout/portal-universal-template";

        public String getKnowledgePath() { return knowledgePath; }
        public void setKnowledgePath(String knowledgePath) { this.knowledgePath = knowledgePath; }
        public String getLayoutProjectPath() { return layoutProjectPath; }
        public void setLayoutProjectPath(String layoutProjectPath) { this.layoutProjectPath = layoutProjectPath; }
    }

    public static class InfraProperties {
        private SshProperties apache = new SshProperties();
        private WlsProperties weblogic = new WlsProperties();
        private LdapProperties ldap = new LdapProperties();
        private DbProperties oracleWcp = new DbProperties();
        private DbProperties oracleApps = new DbProperties();
        private String springActuatorUrl = "http://10.10.2.20:8080/actuator/health";

        public SshProperties getApache() { return apache; }
        public void setApache(SshProperties apache) { this.apache = apache; }
        public WlsProperties getWeblogic() { return weblogic; }
        public void setWeblogic(WlsProperties weblogic) { this.weblogic = weblogic; }
        public LdapProperties getLdap() { return ldap; }
        public void setLdap(LdapProperties ldap) { this.ldap = ldap; }
        public DbProperties getOracleWcp() { return oracleWcp; }
        public void setOracleWcp(DbProperties oracleWcp) { this.oracleWcp = oracleWcp; }
        public DbProperties getOracleApps() { return oracleApps; }
        public void setOracleApps(DbProperties oracleApps) { this.oracleApps = oracleApps; }
        public String getSpringActuatorUrl() { return springActuatorUrl; }
        public void setSpringActuatorUrl(String springActuatorUrl) { this.springActuatorUrl = springActuatorUrl; }
    }

    public static class SshProperties {
        private String host = "10.10.1.10";
        private int port = 22;
        private String user = "appuser";
        private String password = "";
        private String privateKeyPath = "";
        private int timeoutMs = 3000;

        public String getHost() { return host; }
        public void setHost(String host) { this.host = host; }
        public int getPort() { return port; }
        public void setPort(int port) { this.port = port; }
        public String getUser() { return user; }
        public void setUser(String user) { this.user = user; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
        public String getPrivateKeyPath() { return privateKeyPath; }
        public void setPrivateKeyPath(String privateKeyPath) { this.privateKeyPath = privateKeyPath; }
        public int getTimeoutMs() { return timeoutMs; }
        public void setTimeoutMs(int timeoutMs) { this.timeoutMs = timeoutMs; }
    }

    public static class WlsProperties {
        private String adminHost = "10.10.2.20";
        private int adminPort = 17001;
        private String wcp1Host = "10.10.2.20";
        private int wcp1Port = 19500;
        private String wcp2Host = "10.10.2.21";
        private int wcp2Port = 19500;
        private String username = "weblogic";
        private String password = "";
        private int timeoutMs = 3000;

        public String getAdminHost() { return adminHost; }
        public void setAdminHost(String adminHost) { this.adminHost = adminHost; }
        public int getAdminPort() { return adminPort; }
        public void setAdminPort(int adminPort) { this.adminPort = adminPort; }
        public String getWcp1Host() { return wcp1Host; }
        public void setWcp1Host(String wcp1Host) { this.wcp1Host = wcp1Host; }
        public int getWcp1Port() { return wcp1Port; }
        public void setWcp1Port(int wcp1Port) { this.wcp1Port = wcp1Port; }
        public String getWcp2Host() { return wcp2Host; }
        public void setWcp2Host(String wcp2Host) { this.wcp2Host = wcp2Host; }
        public int getWcp2Port() { return wcp2Port; }
        public void setWcp2Port(int wcp2Port) { this.wcp2Port = wcp2Port; }
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
        public int getTimeoutMs() { return timeoutMs; }
        public void setTimeoutMs(int timeoutMs) { this.timeoutMs = timeoutMs; }
    }

    public static class LdapProperties {
        private String host = "10.10.2.30";
        private int port = 389;
        private boolean useSsl = false;
        private String baseDn = "dc=empresa,dc=com,dc=br";
        private String bindDn = "cn=admin,dc=empresa,dc=com,dc=br";
        private String bindPassword = "";
        private int timeoutMs = 3000;

        public String getHost() { return host; }
        public void setHost(String host) { this.host = host; }
        public int getPort() { return port; }
        public void setPort(int port) { this.port = port; }
        public boolean isUseSsl() { return useSsl; }
        public void setUseSsl(boolean useSsl) { this.useSsl = useSsl; }
        public String getBaseDn() { return baseDn; }
        public void setBaseDn(String baseDn) { this.baseDn = baseDn; }
        public String getBindDn() { return bindDn; }
        public void setBindDn(String bindDn) { this.bindDn = bindDn; }
        public String getBindPassword() { return bindPassword; }
        public void setBindPassword(String bindPassword) { this.bindPassword = bindPassword; }
        public int getTimeoutMs() { return timeoutMs; }
        public void setTimeoutMs(int timeoutMs) { this.timeoutMs = timeoutMs; }
    }

    public static class DbProperties {
        private String url = "jdbc:oracle:thin:@10.10.3.40:1521/WCPDB";
        private String user = "app_user";
        private String password = "";
        private int timeoutSeconds = 3;

        public String getUrl() { return url; }
        public void setUrl(String url) { this.url = url; }
        public String getUser() { return user; }
        public void setUser(String user) { this.user = user; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
        public int getTimeoutSeconds() { return timeoutSeconds; }
        public void setTimeoutSeconds(int timeoutSeconds) { this.timeoutSeconds = timeoutSeconds; }
    }

    public GeminiProperties getGemini() { return gemini; }
    public void setGemini(GeminiProperties gemini) { this.gemini = gemini; }
    public RagProperties getRag() { return rag; }
    public void setRag(RagProperties rag) { this.rag = rag; }
    public InfraProperties getInfra() { return infra; }
    public void setInfra(InfraProperties infra) { this.infra = infra; }
    public boolean isSimulationMode() { return simulationMode; }
    public void setSimulationMode(boolean simulationMode) { this.simulationMode = simulationMode; }
}
