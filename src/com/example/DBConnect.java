package com.example;
import java.sql.*;
import java.util.HashMap;
import java.util.Map;

public class DBConnect {
    public static Connection getConnection() throws SQLException {
        String host = System.getenv().getOrDefault("MYSQLHOST", "localhost");
        String port = System.getenv().getOrDefault("MYSQLPORT", "3306");
        String db   = System.getenv().getOrDefault("MYSQLDATABASE", "beautifullife");
        String user = System.getenv().getOrDefault("MYSQLUSER", "root");
        String pass = System.getenv().getOrDefault("MYSQLPASSWORD", "yourpassword");

        String jdbcUrl = "jdbc:mysql://" + host + ":" + port + "/" + db + "?useSSL=false&serverTimezone=UTC";
        return DriverManager.getConnection(jdbcUrl, user, pass);
    }
    // MySQL 連線資訊
    public static final String URL = "jdbc:mysql://localhost:3306/beautifullife";
    public static final String USER = "root";
    public static final String PASSWORD = "Irene870118";

    static {
        try {
            // 載入 MySQL 驅動
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("⚠️ 無法載入 MySQL 驅動程式", e);
        }
    }

    /**
     * 執行 SELECT 查詢
     *
     * @param query  SQL 查詢語句
     * @param params 可變參數（用於 PreparedStatement）
     * @return ResultSet 查詢結果
     */
    public static ResultSet selectQuery(String query, Object... params) {
    Connection conn = null;
    PreparedStatement stmt = null;
    try {
        // 使用 getConnection()，會用環境變數
        conn = getConnection();
        stmt = conn.prepareStatement(query);

        for (int i = 0; i < params.length; i++) {
            stmt.setObject(i + 1, params[i]);
        }

        return stmt.executeQuery();

    } catch (SQLException e) {
        throw new RuntimeException("⚠️ 查詢失敗：" + e.getMessage(), e);
    }
}

    /*
    public static String selectQuery(String query, Object... params) {
        String str="";
        selectQuery(query);
        return str;
    }*/

    /**
     * 執行 INSERT / UPDATE / DELETE 操作
     *
     * @param query  SQL 語句
     * @param params 可變參數（用於 PreparedStatement）
     * @return 受影響的行數
     */
    public static int executeUpdate(String query, Object... params) {
    Connection conn = null;
    PreparedStatement stmt = null;
    try {
        conn = getConnection();
        stmt = conn.prepareStatement(query);

        for (int i = 0; i < params.length; i++) {
            stmt.setObject(i + 1, params[i]);
        }

        return stmt.executeUpdate();

    } catch (SQLException e) {
        throw new RuntimeException("⚠️ 資料更新失敗：" + e.getMessage(), e);
    } finally {
        if (stmt != null) try { stmt.close(); } catch (SQLException e) { e.printStackTrace(); }
        if (conn != null) try { conn.close(); } catch (SQLException e) { e.printStackTrace(); }
    }
}


    public static int getNextId(String tableName, String columnName) {
        try (ResultSet rs = DBConnect.selectQuery("SELECT MAX(" + columnName + ") FROM " + tableName)) {
            if (rs.next()) {
                return rs.getInt(1) + 1;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 1;
    }

    public static Map<String, String> parseJson(String json) {
        Map<String, String> map = new HashMap<>();
        json = json.trim().replaceAll("[{}\"]", "");
        for (String pair : json.split(",")) {
            String[] kv = pair.split(":", 2);
            if (kv.length == 2)
                map.put(kv[0].trim(), kv[1].trim());
        }
        return map;
    }

    public static void main(String[] args) {
        // 測試 SELECT 查詢
        try (ResultSet rs = selectQuery("SELECT * FROM pricelist")) {
            while (rs.next()) {
                System.out.println("ID: " + rs.getString("idPriceList") + ", Name: " + rs.getString("Course_name"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        // 測試 INSERT
        int insertResult = executeUpdate("INSERT INTO pricelist (idPriceList, Course_name) VALUES (?, ?)", 102,
                "Keyboard");
        System.out.println("🔹 插入成功，影響行數：" + insertResult);

        // 測試 UPDATE
        int updateResult = executeUpdate("UPDATE pricelist SET Course_name = ? WHERE idPriceList = ?", "Gaming Keyboard",
                102);
        System.out.println("🔹 更新成功，影響行數：" + updateResult);

        // 測試 DELETE
        int deleteResult = executeUpdate("DELETE FROM pricelist WHERE idPriceList = ?", 102);
        System.out.println("🔹 刪除成功，影響行數：" + deleteResult);
    }
}
