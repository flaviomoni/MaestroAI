package com.corp.ai.agents.service;

import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

@Service
public class SshExecutorService {

    private static final Logger log = LoggerFactory.getLogger(SshExecutorService.class);

    public record SshResult(boolean success, int exitCode, String output, String error) {
    }

    public SshResult executeNonSudoCommand(String host, int port, String user, String password, String privateKeyPath,
            String command, int timeoutMs) {
        log.info("[SSH] Executando comando em {}@{}:{} (non-sudo) -> {}", user, host, port, command);

        Session session = null;
        ChannelExec channel = null;

        try {
            JSch jsch = new JSch();

            if (privateKeyPath != null && !privateKeyPath.isBlank()) {
                jsch.addIdentity(privateKeyPath);
            }

            session = jsch.getSession(user, host, port);
            if (password != null && !password.isBlank()) {
                session.setPassword(password);
            }

            Properties config = new Properties();
            config.put("StrictHostKeyChecking", "no");
            config.put("PreferredAuthentications", "publickey,password,keyboard-interactive");
            config.put("server_host_key",
                    "ssh-ed25519,ecdsa-sha2-nistp256,ecdsa-sha2-nistp384,ecdsa-sha2-nistp521,rsa-sha2-512,rsa-sha2-256,ssh-rsa,ssh-dss");
            config.put("PubkeyAcceptedAlgorithms",
                    "ssh-ed25519,ecdsa-sha2-nistp256,ecdsa-sha2-nistp384,ecdsa-sha2-nistp521,rsa-sha2-512,rsa-sha2-256,ssh-rsa,ssh-dss");
            config.put("kex",
                    "curve25519-sha256,curve25519-sha256@libssh.org,ecdh-sha2-nistp256,ecdh-sha2-nistp384,ecdh-sha2-nistp521,diffie-hellman-group-exchange-sha256,diffie-hellman-group16-sha512,diffie-hellman-group18-sha512,diffie-hellman-group14-sha256,diffie-hellman-group14-sha1,diffie-hellman-group1-sha1");
            session.setConfig(config);
            session.setTimeout(timeoutMs);

            session.connect(timeoutMs);

            channel = (ChannelExec) session.openChannel("exec");
            channel.setCommand(command);

            InputStream in = channel.getInputStream();
            InputStream err = channel.getErrStream();

            channel.connect(timeoutMs);

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            ByteArrayOutputStream errorStream = new ByteArrayOutputStream();

            byte[] buffer = new byte[1024];
            while (true) {
                while (in.available() > 0) {
                    int i = in.read(buffer, 0, 1024);
                    if (i < 0)
                        break;
                    outputStream.write(buffer, 0, i);
                }
                while (err.available() > 0) {
                    int i = err.read(buffer, 0, 1024);
                    if (i < 0)
                        break;
                    errorStream.write(buffer, 0, i);
                }
                if (channel.isClosed()) {
                    if (in.available() > 0 || err.available() > 0)
                        continue;
                    break;
                }
                Thread.sleep(50);
            }

            int exitCode = channel.getExitStatus();
            String stdout = outputStream.toString(StandardCharsets.UTF_8).trim();
            String stderr = errorStream.toString(StandardCharsets.UTF_8).trim();

            log.info("[SSH Result] ExitCode: {}, OutputLength: {}", exitCode, stdout.length());
            return new SshResult(exitCode == 0, exitCode, stdout, stderr);

        } catch (Exception e) {
            log.warn("[SSH Error] Falha ao conectar em {}@{}:{} -> {}", user, host, port, e.getMessage());
            return new SshResult(false, -1, "",
                    "Falha de conexão SSH (VPN desconectada ou host inacessível): " + e.getMessage());
        } finally {
            if (channel != null)
                channel.disconnect();
            if (session != null)
                session.disconnect();
        }
    }
}
