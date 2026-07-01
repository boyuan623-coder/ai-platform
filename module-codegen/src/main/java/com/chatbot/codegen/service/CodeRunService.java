package com.chatbot.codegen.service;

import com.chatbot.codegen.dto.CodeRunResult;
import com.chatbot.common.exception.BusinessException;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class CodeRunService {

    private static final int TIMEOUT_SECONDS = 15;
    private static final Pattern CLASS_NAME = Pattern.compile("public\\s+class\\s+(\\w+)");
    private static final Pattern FENCE = Pattern.compile("```(?:\\w+)?\\s*([\\s\\S]*?)```", Pattern.MULTILINE);

    public CodeRunResult run(String rawCode, String language) {
        String code = extractCode(rawCode);
        if (code.isBlank()) {
            throw new BusinessException("没有可运行的代码");
        }

        String lang = (language == null || language.isBlank() || "auto".equalsIgnoreCase(language))
                ? detectLanguage(code)
                : language.toLowerCase();

        if ("java".equals(lang) || "python".equals(lang) || "py".equals(lang) || "javascript".equals(lang) || "js".equals(lang)) {
            throw new BusinessException("出于安全考虑，Java/Python/JS 请在本地 IDE 或浏览器中运行，服务端仅支持 HTML 预览提示");
        }

        long start = System.currentTimeMillis();
        CodeRunResult result = switch (lang) {
            case "html" -> htmlPreviewHint(code);
            default -> throw new BusinessException("暂不支持运行该语言: " + lang + "，请使用 HTML 预览或本地运行");
        };
        result.setLanguage(lang);
        result.setDurationMs(System.currentTimeMillis() - start);
        return result;
    }

    private String extractCode(String raw) {
        if (raw == null) {
            return "";
        }
        String trimmed = raw.trim();

        Matcher htmlFence = Pattern.compile("```html\\s*([\\s\\S]*?)```", Pattern.CASE_INSENSITIVE).matcher(trimmed);
        if (htmlFence.find()) {
            return htmlFence.group(1).trim();
        }

        Matcher jsFence = Pattern.compile("```(?:javascript|js)\\s*([\\s\\S]*?)```", Pattern.CASE_INSENSITIVE).matcher(trimmed);
        if (jsFence.find()) {
            return jsFence.group(1).trim();
        }

        Matcher matcher = FENCE.matcher(trimmed);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return trimmed;
    }

    private String detectLanguage(String code) {
        if (isHtml(code)) {
            return "html";
        }
        if (code.contains("public class") || code.contains("public static void main")
                || code.contains("import java.")) {
            return "java";
        }
        if (code.contains("def ") && (code.contains("print(") || code.contains("import "))) {
            return "python";
        }
        if (code.contains("console.log") || code.contains("function ") || code.contains("const ")
                || code.contains("let ")) {
            return "javascript";
        }
        return "java";
    }

    private boolean isHtml(String code) {
        String lower = code.toLowerCase();
        return lower.contains("<!doctype html")
                || lower.contains("<html")
                || (lower.contains("<head") && lower.contains("<body"))
                || (lower.contains("<div") && lower.contains("<script"));
    }

    private CodeRunResult htmlPreviewHint(String code) {
        return CodeRunResult.builder()
                .success(true)
                .exitCode(0)
                .stdout("检测到 HTML 页面，请在浏览器预览区域查看效果。")
                .message("HTML 预览")
                .build();
    }

    private CodeRunResult runJavaScript(String code) {
        if (isHtml(code)) {
            return htmlPreviewHint(code);
        }
        return runScript(code, "node", ".js");
    }

    private CodeRunResult runJava(String code) {
        try {
            code = ensureJavaRunnable(code);
            String className = resolveJavaClassName(code);
            Path workDir = Files.createTempDirectory("code-run-");
            try {
                Path sourceFile = workDir.resolve(className + ".java");
                Files.writeString(sourceFile, code, StandardCharsets.UTF_8);

                ProcessResult compile = exec(workDir, TIMEOUT_SECONDS, javaCommand("javac"), sourceFile.toString());
                if (compile.exitCode != 0) {
                    return CodeRunResult.builder()
                            .success(false)
                            .exitCode(compile.exitCode)
                            .stdout(compile.stdout)
                            .stderr(compile.stderr)
                            .message("编译失败")
                            .build();
                }

                ProcessResult run = exec(workDir, TIMEOUT_SECONDS, javaCommand("java"), className);
                return CodeRunResult.builder()
                        .success(run.exitCode == 0)
                        .exitCode(run.exitCode)
                        .stdout(run.stdout)
                        .stderr(run.stderr)
                        .message(run.exitCode == 0 ? "运行成功" : "运行失败")
                        .build();
            } finally {
                deleteRecursively(workDir);
            }
        } catch (IOException e) {
            throw new BusinessException("运行环境异常: " + e.getMessage());
        }
    }

    private String ensureJavaRunnable(String code) {
        if (code.contains("public class")) {
            return code;
        }
        if (code.contains("public static void main")) {
            return "public class GeneratedMain {\n" + code + "\n}";
        }
        return """
                public class GeneratedMain {
                    public static void main(String[] args) throws Exception {
                %s
                    }
                }
                """.formatted(indent(code, "        "));
    }

    private String indent(String code, String prefix) {
        return code.lines().map(line -> prefix + line).collect(Collectors.joining("\n"));
    }

    private String resolveJavaClassName(String code) {
        Matcher matcher = CLASS_NAME.matcher(code);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return "GeneratedMain";
    }

    private CodeRunResult runScript(String code, String command, String suffix) {
        try {
            Path workDir = Files.createTempDirectory("code-run-");
            try {
                Path scriptFile = workDir.resolve("main" + suffix);
                Files.writeString(scriptFile, code, StandardCharsets.UTF_8);

                ProcessResult run = exec(workDir, TIMEOUT_SECONDS, command, scriptFile.toString());
                return CodeRunResult.builder()
                        .success(run.exitCode == 0)
                        .exitCode(run.exitCode)
                        .stdout(run.stdout)
                        .stderr(run.stderr)
                        .message(run.exitCode == 0 ? "运行成功" : "运行失败")
                        .build();
            } finally {
                deleteRecursively(workDir);
            }
        } catch (IOException e) {
            throw new BusinessException("运行环境异常: " + e.getMessage());
        }
    }

    private String javaCommand(String tool) {
        String javaHome = System.getenv("JAVA_HOME");
        if (javaHome != null && !javaHome.isBlank()) {
            Path bin = Path.of(javaHome, "bin", tool + (isWindows() ? ".exe" : ""));
            if (Files.exists(bin)) {
                return bin.toString();
            }
        }
        return tool;
    }

    private boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase().contains("win");
    }

    private ProcessResult exec(Path workDir, int timeoutSeconds, String command, String... args) throws IOException {
        List<String> cmd = new ArrayList<>();
        cmd.add(command);
        cmd.addAll(List.of(args));

        ProcessBuilder builder = new ProcessBuilder(cmd);
        builder.directory(workDir.toFile());
        builder.redirectErrorStream(false);

        Process process = builder.start();
        String stdout = readStream(process.getInputStream());
        String stderr = readStream(process.getErrorStream());

        try {
            boolean finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                return new ProcessResult(-1, stdout, stderr + "\n运行超时（>" + timeoutSeconds + "s），已强制终止");
            }
            return new ProcessResult(process.exitValue(), stdout, stderr);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            process.destroyForcibly();
            return new ProcessResult(-1, stdout, stderr + "\n运行被中断");
        }
    }

    private String readStream(java.io.InputStream inputStream) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append('\n');
            }
            return sb.toString().trim();
        }
    }

    private void deleteRecursively(Path path) {
        try {
            if (Files.isDirectory(path)) {
                try (var stream = Files.list(path)) {
                    stream.forEach(this::deleteRecursively);
                }
            }
            Files.deleteIfExists(path);
        } catch (IOException ignored) {
            // best effort cleanup
        }
    }

    private record ProcessResult(int exitCode, String stdout, String stderr) {}
}
