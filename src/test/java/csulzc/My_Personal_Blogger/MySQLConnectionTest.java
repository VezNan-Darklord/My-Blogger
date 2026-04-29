package csulzc.My_Personal_Blogger;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * MySQL 数据库连接测试
 * 独立测试数据库配置和连接是否正常
 */
@SpringBootTest(properties = {
        "spring.profiles.active=dev"
})
public class MySQLConnectionTest {

    @Autowired
    private DataSource dataSource;

    @Autowired(required = false)
    private JdbcTemplate jdbcTemplate;

    @Test
    void testDataSourceConfiguration() {
        assertNotNull(dataSource, "DataSource 不应为 null");
        System.out.println("✓ DataSource 配置成功");
    }

    @Test
    void testDatabaseConnection() throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            assertNotNull(connection, "数据库连接不应为 null");
            assertFalse(connection.isClosed(), "数据库连接应该是打开的");

            // 获取数据库元信息
            DatabaseMetaData metaData = connection.getMetaData();
            System.out.println("========================================");
            System.out.println("  MySQL 数据库连接信息");
            System.out.println("========================================");
            System.out.println("数据库产品: " + metaData.getDatabaseProductName());
            System.out.println("数据库版本: " + metaData.getDatabaseProductVersion());
            System.out.println("JDBC 驱动: " + metaData.getDriverName());
            System.out.println("JDBC 版本: " + metaData.getDriverVersion());
            System.out.println("URL: " + metaData.getURL());
            System.out.println("用户: " + metaData.getUserName());
            System.out.println("========================================");

            assertTrue(metaData.getDatabaseProductName().contains("MySQL"),
                    "应该是 MySQL 数据库");
        }
    }

    @Test
    void testJdbcTemplateExecution() {
        if (jdbcTemplate != null) {
            // 执行一个简单的查询测试
            Integer result = jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            assertEquals(1, result, "简单查询应该返回 1");
            System.out.println("✓ JdbcTemplate 执行成功");
        } else {
            System.out.println("⚠ JdbcTemplate 未配置，跳过此测试");
        }
    }

    @Test
    void testCreateAndDropTable() throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            // 测试创建表
            connection.createStatement().executeUpdate(
                    "CREATE TABLE IF NOT EXISTS test_mysql_connection (" +
                            "id INT AUTO_INCREMENT PRIMARY KEY, " +
                            "message VARCHAR(100), " +
                            "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)"
            );

            // 测试插入数据
            connection.createStatement().executeUpdate(
                    "INSERT INTO test_mysql_connection (message) VALUES ('MySQL connection test successful')"
            );

            // 测试查询数据
            var resultSet = connection.createStatement().executeQuery(
                    "SELECT message FROM test_mysql_connection ORDER BY id DESC LIMIT 1"
            );

            assertTrue(resultSet.next(), "应该能查询到测试数据");
            assertEquals("MySQL connection test successful", resultSet.getString("message"));

            // 清理测试表
            connection.createStatement().executeUpdate(
                    "DROP TABLE IF EXISTS test_mysql_connection"
            );

            System.out.println("✓ 数据库 CRUD 操作测试成功");
        }
    }
}
