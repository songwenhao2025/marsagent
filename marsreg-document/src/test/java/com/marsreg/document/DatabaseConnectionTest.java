package com.marsreg.document;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class DatabaseConnectionTest {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void testDatabaseConnection() {
        try (Connection connection = dataSource.getConnection()) {
            assertNotNull(connection);
            assertTrue(connection.isValid(5));
            System.out.println("数据库连接成功！");
            System.out.println("数据库URL: " + connection.getMetaData().getURL());
            System.out.println("数据库用户名: " + connection.getMetaData().getUserName());
            System.out.println("数据库产品名称: " + connection.getMetaData().getDatabaseProductName());
            System.out.println("数据库产品版本: " + connection.getMetaData().getDatabaseProductVersion());
        } catch (SQLException e) {
            fail("数据库连接失败: " + e.getMessage());
        }
    }

    @Test
    void testDatabaseQuery() {
        String result = jdbcTemplate.queryForObject("SELECT @@version", String.class);
        assertNotNull(result);
        System.out.println("数据库版本: " + result);
    }

    @Test
    void testDatabaseTables() {
        List<Map<String, Object>> tables = jdbcTemplate.queryForList(
            "SELECT TABLE_NAME, TABLE_COMMENT " +
            "FROM information_schema.TABLES " +
            "WHERE TABLE_SCHEMA = 'mars_db' " +
            "ORDER BY TABLE_NAME"
        );
        
        System.out.println("\n数据库表结构：");
        System.out.println("----------------------------------------");
        for (Map<String, Object> table : tables) {
            String tableName = (String) table.get("TABLE_NAME");
            String tableComment = (String) table.get("TABLE_COMMENT");
            System.out.println("表名: " + tableName);
            System.out.println("说明: " + (tableComment != null ? tableComment : "无"));
            
            // 获取表的列信息
            List<Map<String, Object>> columns = jdbcTemplate.queryForList(
                "SELECT COLUMN_NAME, COLUMN_TYPE, IS_NULLABLE, COLUMN_COMMENT " +
                "FROM information_schema.COLUMNS " +
                "WHERE TABLE_SCHEMA = 'mars_db' AND TABLE_NAME = ? " +
                "ORDER BY ORDINAL_POSITION",
                tableName
            );
            
            System.out.println("列信息：");
            for (Map<String, Object> column : columns) {
                System.out.println("  - " + column.get("COLUMN_NAME") + 
                                 " (" + column.get("COLUMN_TYPE") + ")" +
                                 " " + (column.get("IS_NULLABLE").equals("YES") ? "可空" : "非空") +
                                 " - " + column.get("COLUMN_COMMENT"));
            }
            System.out.println("----------------------------------------");
        }
    }

    @Test
    void testDatabaseIndexes() {
        List<Map<String, Object>> tables = jdbcTemplate.queryForList(
            "SELECT TABLE_NAME " +
            "FROM information_schema.TABLES " +
            "WHERE TABLE_SCHEMA = 'mars_db' " +
            "ORDER BY TABLE_NAME"
        );
        
        System.out.println("\n数据库索引信息：");
        System.out.println("----------------------------------------");
        for (Map<String, Object> table : tables) {
            String tableName = (String) table.get("TABLE_NAME");
            System.out.println("\n表名: " + tableName);
            
            // 获取表的索引信息
            List<Map<String, Object>> indexes = jdbcTemplate.queryForList(
                "SELECT INDEX_NAME, COLUMN_NAME, NON_UNIQUE, SEQ_IN_INDEX, COLLATION " +
                "FROM information_schema.STATISTICS " +
                "WHERE TABLE_SCHEMA = 'mars_db' AND TABLE_NAME = ? " +
                "ORDER BY INDEX_NAME, SEQ_IN_INDEX",
                tableName
            );
            
            if (indexes.isEmpty()) {
                System.out.println("  无索引");
            } else {
                String currentIndex = null;
                for (Map<String, Object> index : indexes) {
                    String indexName = (String) index.get("INDEX_NAME");
                    if (!indexName.equals(currentIndex)) {
                        currentIndex = indexName;
                        System.out.println("\n  索引名: " + indexName);
                        System.out.println("  类型: " + (index.get("NON_UNIQUE").equals(1) ? "非唯一索引" : "唯一索引"));
                    }
                    System.out.println("    - 列: " + index.get("COLUMN_NAME") + 
                                     " (排序: " + index.get("COLLATION") + ")");
                }
            }
            System.out.println("----------------------------------------");
        }
    }

    @Test
    void testCreateIndexes() {
        // documents 表的索引
        jdbcTemplate.execute("CREATE INDEX idx_documents_name ON documents(name)");
        jdbcTemplate.execute("CREATE INDEX idx_documents_create_time ON documents(create_time)");
        jdbcTemplate.execute("CREATE INDEX idx_documents_update_time ON documents(update_time)");
        jdbcTemplate.execute("CREATE INDEX idx_documents_status ON documents(status)");

        // document_versions 表的索引
        jdbcTemplate.execute("CREATE INDEX idx_document_versions_document_id ON document_versions(document_id)");
        jdbcTemplate.execute("CREATE INDEX idx_document_versions_version ON document_versions(version)");

        // document_chunks 表的索引
        jdbcTemplate.execute("CREATE INDEX idx_document_chunks_document_id ON document_chunks(document_id)");
        jdbcTemplate.execute("CREATE INDEX idx_document_chunks_chunk_index ON document_chunks(chunk_index)");

        // document_permissions 表的索引
        jdbcTemplate.execute("CREATE INDEX idx_document_permissions_document_user ON document_permissions(document_id, user_id)");

        // document_tags 表的索引
        jdbcTemplate.execute("CREATE INDEX idx_document_tags_document_id ON document_tags(document_id)");

        System.out.println("所有索引创建完成！");
    }
} 