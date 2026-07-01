package com.chatbot;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * 一键初始化 ai_platform 库、表和示例数据。
 * 在 IDEA 中直接运行此类即可（无需 mysql 命令行）。
 */
public class DbInit {

    private static final String HOST = System.getenv().getOrDefault("MYSQL_HOST", "127.0.0.1");
    private static final String PORT = System.getenv().getOrDefault("MYSQL_PORT", "3306");
    private static final String USER = System.getenv().getOrDefault("MYSQL_USER", "root");
    private static final String[] PASSWORD_CANDIDATES = {
            System.getenv("MYSQL_PASSWORD"),
            ""
    };

    public static void main(String[] args) throws Exception {
        String sql = loadInitSql();
        SQLException lastError = null;

        List<String> hosts = List.of(HOST, "localhost", "127.0.0.1");
        for (String host : hosts.stream().distinct().toList()) {
            for (String password : PASSWORD_CANDIDATES) {
                if (password == null) {
                    continue;
                }
                try {
                    System.out.println("Trying MySQL " + USER + "@" + host + ":" + PORT + " ...");
                    runInit(host, password, sql);
                    System.out.println("Database initialized successfully.");
                    return;
                } catch (SQLException e) {
                    lastError = e;
                    System.out.println("Failed: " + e.getMessage());
                }
            }
        }

        throw new IllegalStateException(
                "无法连接 MySQL，请确认 MySQL80 服务已启动且 root 密码正确。"
                        + "可在环境变量中设置 MYSQL_HOST / MYSQL_PASSWORD。",
                lastError
        );
    }

    private static void runInit(String host, String password, String sql) throws SQLException {
        String baseUrl = "jdbc:mysql://" + host + ":" + PORT
                + "/?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai"
                + "&allowPublicKeyRetrieval=true&useSSL=false";

        Properties props = new Properties();
        props.setProperty("user", USER);
        props.setProperty("password", password);

        try (Connection conn = DriverManager.getConnection(baseUrl, props);
             Statement stmt = conn.createStatement()) {

            for (String statement : splitSql(sql)) {
                stmt.execute(statement);
            }
        }

        String verifyUrl = "jdbc:mysql://" + host + ":" + PORT
                + "/ai_platform?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai"
                + "&allowPublicKeyRetrieval=true&useSSL=false";

        try (Connection conn = DriverManager.getConnection(verifyUrl, props);
             Statement stmt = conn.createStatement();
             var rs = stmt.executeQuery(
                     "SELECT id, user_name, phone, service_type, status FROM appointment_order")) {

            System.out.println("\n--- appointment_order ---");
            while (rs.next()) {
                System.out.printf("id=%d name=%s phone=%s service=%s status=%s%n",
                        rs.getLong("id"),
                        rs.getString("user_name"),
                        rs.getString("phone"),
                        rs.getString("service_type"),
                        rs.getString("status"));
            }
        }
    }

    private static String loadInitSql() throws IOException {
        try (var in = DbInit.class.getClassLoader().getResourceAsStream("db/init.sql")) {
            if (in == null) {
                throw new IllegalStateException("db/init.sql not found on classpath");
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static List<String> splitSql(String sql) {
        List<String> statements = new ArrayList<>();
        StringBuilder current = new StringBuilder();

        for (String line : sql.split("\n")) {
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("--")) {
                continue;
            }
            current.append(line).append('\n');
            if (trimmed.endsWith(";")) {
                statements.add(current.toString().trim());
                current.setLength(0);
            }
        }

        if (!current.isEmpty()) {
            statements.add(current.toString().trim());
        }
        return statements;
    }
}
