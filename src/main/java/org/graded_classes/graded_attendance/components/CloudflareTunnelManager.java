package org.graded_classes.graded_attendance.components;

import java.io.*;
import java.nio.file.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CloudflareTunnelManager {

    private Process tunnelProcess;
    private String tunnelUrl;

    public void startTunnel() throws Exception {

        ProcessBuilder pb = new ProcessBuilder(
                "cloudflared",
                "tunnel",
                "--url",
                "http://localhost:8080"
        );

        pb.redirectErrorStream(true);

        tunnelProcess = pb.start();

        BufferedReader reader = new BufferedReader(
                new InputStreamReader(tunnelProcess.getInputStream())
        );

        Pattern pattern = Pattern.compile(
                "https://[\\w-]+\\.trycloudflare\\.com"
        );

        String line;

        while ((line = reader.readLine()) != null) {

            System.out.println(line);

            Matcher matcher = pattern.matcher(line);

            if (matcher.find()) {

                tunnelUrl = matcher.group();

                saveUrlToJson(tunnelUrl);

                System.out.println("Tunnel URL: " + tunnelUrl);

                break;
            }
        }

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {

            System.out.println("Stopping Cloudflare Tunnel...");

            try {

                if (tunnelProcess != null && tunnelProcess.isAlive()) {

                    tunnelProcess.destroy();

                    if (!tunnelProcess.waitFor(5, TimeUnit.SECONDS)) {
                        tunnelProcess.destroyForcibly();
                    }
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }));
    }

    private void saveUrlToJson(String url) throws IOException {

        String json = String.format("""
                {
                    "url": "%s"
                }
                """, url);

        Files.writeString(
                Paths.get("tunnel.json"),
                json,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING
        );
    }

    public String getTunnelUrl() {
        return tunnelUrl;
    }

    public void stopTunnel() {

        try {

            if (tunnelProcess != null && tunnelProcess.isAlive()) {

                tunnelProcess.destroy();

                if (!tunnelProcess.waitFor(5, TimeUnit.SECONDS)) {
                    tunnelProcess.destroyForcibly();
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    void main(String[] args) {

        try {

            CloudflareTunnelManager manager =
                    new CloudflareTunnelManager();

            manager.startTunnel();

            System.out.println(
                    "Application running...\nURL = "
                            + manager.getTunnelUrl()
            );

            Thread.sleep(Long.MAX_VALUE);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}