import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * 执行 docs/sql/migration_plaza.sql（不依赖 mysql 命令行）
 * 用法: java -cp ".;path/to/mysql-connector-j.jar" RunPlazaMigration [sql路径]
 * 默认连接: localhost:3306/study_helper root/123456
 */
public class RunPlazaMigration {
    public static void main(String[] args) throws Exception {
        Path root = Path.of(System.getProperty("user.dir"));
        Path sqlPath = args.length > 0 ? Path.of(args[0]) : root.resolve("docs/sql/migration_plaza.sql");
        if (!Files.isRegularFile(sqlPath)) {
            System.err.println("找不到 SQL 文件: " + sqlPath.toAbsolutePath());
            System.exit(1);
        }
        String raw = Files.readString(sqlPath, StandardCharsets.UTF_8);
        StringBuilder sb = new StringBuilder();
        for (String line : raw.split("\r?\n")) {
            String t = line.trim();
            if (t.startsWith("--")) continue;
            sb.append(line).append('\n');
        }
        String cleaned = sb.toString().replaceAll("(?is)/\\*.*?\\*/", "");
        List<String> stmts = new ArrayList<>();
        for (String part : cleaned.split(";")) {
            String s = part.trim();
            if (s.isEmpty()) continue;
            String upper = s.toUpperCase().replaceFirst("^\\s+", "");
            if (upper.startsWith("USE ")) continue;
            stmts.add(s);
        }
        String url = System.getenv().getOrDefault("MYSQL_URL",
                "jdbc:mysql://localhost:3306/study_helper?useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true");
        String user = System.getenv().getOrDefault("MYSQL_USER", "root");
        String pass = System.getenv().getOrDefault("MYSQL_PASSWORD", "123456");
        Class.forName("com.mysql.cj.jdbc.Driver");
        try (Connection conn = DriverManager.getConnection(url, user, pass);
             Statement st = conn.createStatement()) {
            for (String sql : stmts) {
                System.out.println("执行: " + sql.substring(0, Math.min(60, sql.length())) + "...");
                st.execute(sql);
            }
        }
        System.out.println("迁移完成，共 " + stmts.size() + " 条语句。");
    }
}
