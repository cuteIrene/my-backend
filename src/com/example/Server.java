package com.example;
import com.sun.net.httpserver.*;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;


public class Server {
    public static void main(String[] args) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(8000), 0);

        // 綁定 API 路由
        server.createContext("/api/hello", new HelloHandler());
        server.createContext("/api/customers", new CustHandler());
        server.createContext("/api/reservation", new ReserveHandler());

        server.createContext("/api/1/customers", new TableDataHandler("select * from memberinfo"));
        server.createContext("/api/1/orders", new TableDataHandler("select * from reservation"));
        server.createContext("/api/1/products", new TableDataHandler("select * from pricelist"));
        server.createContext("/api/1/promote", new TableDataHandler("select * from promotion"));
        server.createContext("/api/1/come", new TableDataHandler("select * from actual"));
        server.createContext("/api/1/beautician", new TableDataHandler("select * from employee"));

        server.createContext("/api/2/insert", new InsertDataHandler());
        server.createContext("/api/2/update", new UpdateDataHandler());

        //server.createContext("/chat/send", new ChatHandler.SendHandler());
        //server.createContext("/chat/messages", new ChatHandler.MessageHandler());

        server.createContext("/postdemo", new postdemo());
        server.createContext("/getdemo", new getdemo());

        server.createContext("/check-member", new VerifyMemberHandler());//確認是否為會員
        server.createContext("/api/2/reservations-tomorrow", new TomorrowReservationHandler());//撈隔天預約內容
        server.createContext("/api/2/check-reservation", new CheckReservationHandler());//確認尚未到來的預約資訊
        server.createContext("/api/2/insert-actual", new InsertActualHandler());//實際來店
        server.createContext("/api/2/check-session", new CheckSessionHandler());//查詢剩餘堂數

        server.start();
        System.out.println("Server started at http://localhost:8000");
    }
    static class getdemo implements HttpHandler{
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String response = "Hello Welcome!";
            //指定回傳格式:text, 開放完全存取 *
            exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=utf-8");
            exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
            //格式化回傳的內容 文字->bytes
            byte[] responseBytes = response.getBytes(StandardCharsets.UTF_8);
            //啟動回傳，通知成功與長度，再回傳內容
            exchange.sendResponseHeaders(200,responseBytes.length);
            OutputStream os = exchange.getResponseBody();
            os.write(responseBytes);
            os.close();
        }
    }

    static class postdemo implements HttpHandler{
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String file = "temppost.txt";
            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            try (BufferedWriter bw = new BufferedWriter(new FileWriter(file))) {
                bw.write(body);
            }
            exchange.sendResponseHeaders(200, -1);
        }
    }

    static class TableDataHandler implements HttpHandler {      

    String sql = "";

    public TableDataHandler(String sql) {
        this.sql = sql;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        Headers headers = exchange.getResponseHeaders();
        headers.set("Access-Control-Allow-Origin", "*");
        headers.set("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        headers.set("Access-Control-Allow-Headers", "Content-Type");

        // Handle preflight OPTIONS request
        if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(204, -1); // No content
            return;
        }

        String response = "HI Orsino";
        StringBuilder json = new StringBuilder();
        json.append("[");

        try {
            ResultSet rs = DBConnect.selectQuery(sql);
            ResultSetMetaData meta = rs.getMetaData();
            int columnCount = meta.getColumnCount();

            boolean firstRow = true;
            while (rs.next()) {
                if (!firstRow) json.append(",");
                firstRow = false;

                json.append("{");
                for (int i = 1; i <= columnCount; i++) {
                    String columnName = meta.getColumnLabel(i);
                    String value = rs.getString(i);
                    if (i > 1) json.append(",");
                    json.append("\"").append(columnName).append("\":");
                    json.append("\"").append(value == null ? "" : escapeJson(value)).append("\"");
                }
                json.append("}");
            }
            rs.getStatement().getConnection().close();
        } catch (Exception e) {
            json = new StringBuilder("{\"error\":\"" + e.getMessage() + "\"}");
        }

        json.append("]");
        response = json.toString();

        byte[] responseBytes = response.getBytes("UTF-8");
        headers.set("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(200, responseBytes.length);
        OutputStream os = exchange.getResponseBody();
        os.write(responseBytes);
        os.close();
    }

    private static String escapeJson(String s) {
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }
}


    //預約資訊回傳
    static class ReserveHandler implements HttpHandler {
        @Override    
        public void handle(HttpExchange exchange) throws IOException {
            String response = "Hi Irene";
            try(ResultSet rs = DBConnect.selectQuery("select * from reservation")){
                while(rs.next()){
                    response +="\n"+(rs.getInt("Customer_ID")+ " | " + rs.getInt("Course_ID")+ " | " + rs.getDate("datetime"));
                }
            }catch(Exception e){
                e.printStackTrace();
            }
            
        StringBuilder json = new StringBuilder();
        json.append("[");
        try {
            ResultSet rs = DBConnect.selectQuery("SELECT * FROM reservation");
            boolean first = true;
            while (rs.next()) {
                if (!first) json.append(",");
                first = false;
                String id = rs.getString("idreservation");
                String name = rs.getString("Customer_ID");
                json.append("{")
                    .append("\"idreservation\":\"").append(id).append("\",")
                    .append("\"Customer_ID\":\"").append(name).append("\"")
                    .append("}");
            }
            rs.getStatement().getConnection().close(); // 手動關閉連線
        } catch (Exception e) {
            json = new StringBuilder("{\"error\":\"" + e.getMessage() + "\"}");
        }

            json.append("]");
            response = json.toString();
            
            exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=utf-8");                  
            exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
            byte[] responseBytes = response.toString().getBytes("UTF-8");
            exchange.sendResponseHeaders(200,responseBytes.length);
            OutputStream os = exchange.getResponseBody();
            os.write(responseBytes);
            os.close();
        }
    }

    //Customer資料回傳
    static class CustHandler implements HttpHandler {
        @Override    
        public void handle(HttpExchange exchange) throws IOException {
            String response = "Hi Irene";
            try(ResultSet rs = DBConnect.selectQuery("select * from memberinfo")){
                while(rs.next()){
                    response +="\n"+(rs.getString("Member_name")+ " | " + rs.getString("Member_gender")+ " | " + rs.getString("Member_phone"));
                }
            }catch(Exception e){
                e.printStackTrace();
            }
            
        StringBuilder json = new StringBuilder();
        json.append("[");
        try {
            ResultSet rs = DBConnect.selectQuery("SELECT * FROM memberinfo");
            boolean first = true;
            while (rs.next()) {
                if (!first) json.append(",");
                first = false;
                String id = rs.getString("idmemberinfo");
                String name = rs.getString("Member_name");
                json.append("{")
                    .append("\"idmemberinfo\":\"").append(id).append("\",")
                    .append("\"Member_name\":\"").append(name).append("\"")
                    .append("}");
            }
            rs.getStatement().getConnection().close(); // 手動關閉連線
        } catch (Exception e) {
            json = new StringBuilder("{\"error\":\"" + e.getMessage() + "\"}");
        }

            json.append("]");
            response = json.toString();
            
            exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=utf-8");                  
            exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
            byte[] responseBytes = response.toString().getBytes("UTF-8");
            exchange.sendResponseHeaders(200,responseBytes.length);
            OutputStream os = exchange.getResponseBody();
            os.write(responseBytes);
            os.close();
        }
    }    

        // 定義 handler
    static class HelloHandler implements HttpHandler {
        @Override    
        public void handle(HttpExchange exchange) throws IOException {
            String response = "Hi Irene";
            try(ResultSet rs = DBConnect.selectQuery("select * from pricelist")){
                while(rs.next()){
                    response +="\n"+(rs.getString("Course_name")+ " | " + rs.getInt("Course_time")+ " | " + rs.getInt("Course_price"));
                }
            }catch(Exception e){
                e.printStackTrace();
            }
            
        StringBuilder json = new StringBuilder();
        json.append("[");
        try {
            ResultSet rs = DBConnect.selectQuery("SELECT * FROM pricelist");
            boolean first = true;
            while (rs.next()) {
                if (!first) json.append(",");
                first = false;
                String id = rs.getString("idPriceList");
                String name = rs.getString("Course_name");
                json.append("{")
                    .append("\"idPriceList\":\"").append(id).append("\",")
                    .append("\"Course_name\":\"").append(name).append("\"")
                    .append("}");
            }
            rs.getStatement().getConnection().close(); // 手動關閉連線
        } catch (Exception e) {
            json = new StringBuilder("{\"error\":\"" + e.getMessage() + "\"}");
        }

            json.append("]");
            response = json.toString();
            
            exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=utf-8");                  
            exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
            byte[] responseBytes = response.toString().getBytes("UTF-8");
            exchange.sendResponseHeaders(200,responseBytes.length);
            OutputStream os = exchange.getResponseBody();
            os.write(responseBytes);
            os.close();
        }
    }

    static class InsertDataHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                handleCORSPreflight(exchange);
                return;
            }
            exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            try {
                String body = new BufferedReader(new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8))
                        .lines().collect(Collectors.joining());
                Map<String, String> map = DBConnect.parseJson(body);
                String targetTable = map.get("table");

                switch (targetTable) {
                    case "memberinfo": {
                        String name = map.get("name");
                        String gender = map.get("gender");
                        String phone = map.get("phone");
                        String birth = map.get("birth");
                        String contact = map.get("contact");
                        String conphone = map.get("conphone");
                        int id = DBConnect.getNextId("memberinfo", "idmemberinfo");
                        DBConnect.executeUpdate(
                                "INSERT INTO memberinfo (idmemberinfo, Member_name, Member_gender, Member_phone, Birthday, Emergency_contact, Emergency_phone) VALUES (?, ?, ?, ?, ?, ?, ?)",
                                id, name, gender, phone, birth, contact, conphone);
                        exchange.sendResponseHeaders(200, -1);
                        break;
                    }
                    case "pricelist": {
                        String pname = map.get("pname");
                        String time = map.get("time");
                        String price = map.get("price");
                        String remark = map.get("remark");
                        int pid = DBConnect.getNextId("pricelist", "idPriceList");
                        DBConnect.executeUpdate(
                                "INSERT INTO pricelist (idPriceList, Course_name, Course_time, Course_price, Course_remark) VALUES (?, ?, ?, ?, ?)",
                                pid, pname, Integer.parseInt(time), Integer.parseInt(price), remark);
                        exchange.sendResponseHeaders(200, -1);
                        break;
                    }
                    case "reservation": {
                        String cname = map.get("Customer_ID");
                        String courseIdStr = map.get("Course_ID");
                        String rtime = map.get("DateTime");
                        String beauticianStr = map.get("Beautician");
                        int rid = DBConnect.getNextId("reservation", "idreservation");

                        Integer beautician = null;
                        if (beauticianStr != null && !beauticianStr.isEmpty() && !beauticianStr.equals("null")) {
                            beautician = Integer.parseInt(beauticianStr);
                        }

                        int state = 0; // 預設狀態

                        DBConnect.executeUpdate(
                            "INSERT INTO reservation (idreservation, Customer_ID, Course_ID, Datetime, Beautician, state) VALUES (?, ?, ?, ?, ?, ?)",
                            rid,
                            Integer.parseInt(cname),
                            Integer.parseInt(courseIdStr),
                            rtime,
                            beautician,
                            state
                        );

                        String ok = "{\"message\":\"success\"}";
                        exchange.getResponseHeaders().set("Content-Type", "application/json");
                        exchange.sendResponseHeaders(200, ok.getBytes().length);
                        exchange.getResponseBody().write(ok.getBytes(StandardCharsets.UTF_8));
                        break;
                    }
                    default:
                        String resp = "{\"error\":\"Unknown table\"}";
                        exchange.getResponseHeaders().set("Content-Type", "application/json");
                        exchange.sendResponseHeaders(400, resp.length());
                        exchange.getResponseBody().write(resp.getBytes(StandardCharsets.UTF_8));
                }
            } catch (Exception e) {
                e.printStackTrace();
                String response = "{\"error\": \"" + e.getMessage().replace("\"", "\\\"") + "\"}";
                exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(500, response.getBytes().length);
                OutputStream os = exchange.getResponseBody();
                os.write(response.getBytes());
                os.close();
            }
        }
    }

    static class UpdateDataHandler implements HttpHandler {
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "POST, GET, OPTIONS");
            exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type");
            exchange.sendResponseHeaders(204, -1);
            return;
        }

        exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");

        try {
            String body = new BufferedReader(
                    new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8))
                    .lines().collect(Collectors.joining());
            Map<String, String> map = DBConnect.parseJson(body);
            String targetTable = map.get("table");

            switch (targetTable) {
                case "memberinfo": {
                    String id = map.get("id");
                    String name = map.get("name");
                    String gender = map.get("gender");
                    String phone = map.get("phone");
                    String birth = map.get("birth");
                    String contact = map.get("contact");
                    String conphone = map.get("conphone");

                    int result = DBConnect.executeUpdate(
                        "UPDATE memberinfo SET Member_name=?, Member_gender=?, Member_phone=?, Birthday=?, Emergency_contact=?, Emergency_phone=? WHERE idmemberinfo=?",
                        name, gender, phone, birth, contact, conphone, id
                    );
                    break;
                }
                case "pricelist": {
                    String id = map.get("id");
                    String pname = map.get("pname");
                    String time = map.get("time");
                    String price = map.get("price");
                    String remark = map.get("remark");

                    int result = DBConnect.executeUpdate(
                        "UPDATE pricelist SET Course_name=?, Course_time=?, Course_price=?, Course_remark=? WHERE idPriceList=?",
                        pname, Integer.parseInt(time), Integer.parseInt(price), remark, id
                    );
                    break;
                }
                case "reservation": {
                    String cname = map.get("Customer_ID");      // 會員ID (字串整數)
                    String courseIdStr = map.get("Course_ID");  // 課程ID (字串整數)
                    String rtime = map.get("DateTime");         // 預約時間字串
                    String beauticianStr = map.get("Beautician"); // 美容師ID字串或 null

                    int rid = DBConnect.getNextId("reservation", "idreservation");

                    Integer beautician = null;
                    if (beauticianStr != null && !beauticianStr.isEmpty()) {
                        beautician = Integer.parseInt(beauticianStr);
                    }

                    int state = 0;  // 新增預約時，狀態固定為 0

                    DBConnect.executeUpdate(
                        "INSERT INTO reservation (idreservation, Customer_ID, Course_ID, Datetime, Beautician, state) VALUES (?, ?, ?, ?, ?, ?)",
                        rid,
                        Integer.parseInt(cname),
                        Integer.parseInt(courseIdStr),
                        rtime,
                        beautician,
                        state
                    );

                    exchange.sendResponseHeaders(200, -1);
                    break;
                }
                default:
                    throw new IllegalArgumentException("Unsupported table: " + targetTable);
            }

            exchange.sendResponseHeaders(200, -1);
        } catch (Exception e) {
            e.printStackTrace();
            String response = "{\"error\": \"" + e.getMessage().replace("\"", "\\\"") + "\"}";
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(500, response.getBytes().length);
            OutputStream os = exchange.getResponseBody();
            os.write(response.getBytes());
            os.close();
        }
    }
}

    // 靜態方法，統一處理 CORS 預檢請求
    public static void handleCORSPreflight(HttpExchange exchange) throws IOException {
        Headers headers = exchange.getResponseHeaders();
        headers.add("Access-Control-Allow-Origin", "*");
        headers.add("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        headers.add("Access-Control-Allow-Headers", "Content-Type");
        headers.add("Access-Control-Max-Age", "86400"); // Optional, 提高效能
        exchange.sendResponseHeaders(204, -1); // No content
        exchange.close();
    }

   public static class VerifyMemberHandler implements HttpHandler {
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
            handleCORSPreflight(exchange);
            return;
        }

        Headers headers = exchange.getResponseHeaders();
        headers.set("Content-Type", "application/json; charset=utf-8");
        headers.set("Access-Control-Allow-Origin", "*");  // ✅ 統一加在最前面

        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            String msg = "{\"error\":\"Method Not Allowed\"}";
            exchange.sendResponseHeaders(405, msg.length());
            exchange.getResponseBody().write(msg.getBytes(StandardCharsets.UTF_8));
            exchange.close();
            return;
        }

        InputStream is = exchange.getRequestBody();
        String body = new String(is.readAllBytes(), StandardCharsets.UTF_8);

        Map<String, String> data = DBConnect.parseJson(body);
        String name = data.get("name");
        String phone = data.get("phone");

        String response;
        int statusCode = 200;

        if (name == null || phone == null) {
            statusCode = 400;
            response = "{\"error\":\"Missing name or phone\"}";
        } else {
            try (ResultSet rs = DBConnect.selectQuery(
                    "SELECT idmemberinfo FROM memberinfo WHERE Member_name = ? AND Member_phone = ?",
                    name, phone)) {
                if (rs.next()) {
                    int id = rs.getInt("idmemberinfo");
                    response = "{\"isMember\": true, \"idmemberinfo\": " + id + "}";
                } else {
                    response = "{\"isMember\": false}";
                }
            } catch (SQLException e) {
                statusCode = 500;
                response = "{\"error\":\"Database error\"}";
                e.printStackTrace();
            }
        }

        byte[] respBytes = response.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(statusCode, respBytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(respBytes);
        }
    }
}

   public static class TomorrowReservationHandler implements HttpHandler {
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
            handleCORSPreflight(exchange);
            return;
        }

        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            String msg = "{\"error\":\"Method Not Allowed\"}";
            exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
            exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
            exchange.sendResponseHeaders(405, msg.length());
            exchange.getResponseBody().write(msg.getBytes(StandardCharsets.UTF_8));
            exchange.close();
            return;
        }

        StringBuilder json = new StringBuilder();
        int statusCode = 200;
        json.append("[");

        try (ResultSet rs = DBConnect.selectQuery(
                """
                SELECT 
                    m.Member_name AS CustomerName, 
                    m.Member_phone AS Phone, 
                    p.Course_name AS CourseName,
                    r.DateTime, 
                    e.employee_name AS BeauticianName
                FROM reservation r
                JOIN memberinfo m ON r.Customer_ID = m.idmemberinfo
                JOIN pricelist p ON r.Course_ID = p.idPriceList
                LEFT JOIN employee e ON r.Beautician = e.idemployee
                WHERE DATE(r.DateTime) = CURDATE() + INTERVAL 1 DAY
                ORDER BY r.DateTime
                """)) {

            boolean first = true;
            while (rs.next()) {
                if (!first) json.append(",");
                json.append("{")
                    .append("\"CustomerName\":\"").append(escapeJson(rs.getString("CustomerName"))).append("\",")
                    .append("\"Phone\":\"").append(escapeJson(rs.getString("Phone"))).append("\",")
                    .append("\"CourseName\":\"").append(escapeJson(rs.getString("CourseName"))).append("\",")
                    .append("\"DateTime\":\"").append(escapeJson(rs.getString("DateTime"))).append("\",")
                    .append("\"Beautician\":\"").append(escapeJson(rs.getString("BeauticianName"))).append("\"")
                    .append("}");
                first = false;
            }

            json.append("]");
        } catch (SQLException e) {
            e.printStackTrace();
            statusCode = 500;
            json.setLength(0);
            json.append("{\"error\":\"Database error\"}");
        }

        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");

        byte[] respBytes = json.toString().getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(statusCode, respBytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(respBytes);
        }
    }

    private static String escapeJson(String value) {
        if (value == null) return "";
        return value.replace("\\", "\\\\")
                    .replace("\"", "\\\"")
                    .replace("\n", "\\n")
                    .replace("\r", "\\r");
    }
}

    public static class CheckReservationHandler implements HttpHandler {
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
            handleCORSPreflight(exchange);
            return;
        }
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            String msg = "{\"error\":\"Method Not Allowed\"}";
            exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
            exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
            exchange.sendResponseHeaders(405, msg.length());
            exchange.getResponseBody().write(msg.getBytes(StandardCharsets.UTF_8));
            exchange.close();
            return;
        }

        InputStream is = exchange.getRequestBody();
        String body = new String(is.readAllBytes(), StandardCharsets.UTF_8);

        Map<String, String> data = DBConnect.parseJson(body);
        String name = data.get("name");
        String phone = data.get("phone");

        String response;
        int statusCode = 200;

        if (name == null || phone == null) {
            statusCode = 400;
            response = "{\"error\":\"Missing name or phone\"}";
        } else {
            StringBuilder json = new StringBuilder();
            json.append("[");
            try (ResultSet rs = DBConnect.selectQuery(
                    "SELECT r.idReservation, r.Course_ID, r.DateTime, r.Beautician, " +
                    "p.Course_name, p.Course_price, e.employee_name " +
                    "FROM reservation r " +
                    "JOIN memberinfo m ON r.Customer_ID = m.idmemberinfo " +
                    "JOIN pricelist p ON r.Course_ID = p.idPriceList " +
                    "LEFT JOIN employee e ON r.Beautician = e.idemployee " +
                    "WHERE m.Member_name = ? AND m.Member_phone = ? AND r.DateTime > NOW() " +
                    "ORDER BY r.DateTime",
                    name, phone)) {

                boolean first = true;
                while (rs.next()) {
                    if (!first) json.append(",");
                    json.append("{")
                        .append("\"idReservation\":").append(rs.getInt("idReservation")).append(",")
                        .append("\"Course_ID\":").append(rs.getInt("Course_ID")).append(",")
                        .append("\"DateTime\":\"").append(rs.getString("DateTime")).append("\",")
                        .append("\"Beautician\":\"").append(rs.getString("employee_name") == null ? "" : rs.getString("employee_name")).append("\",")
                        .append("\"Course_name\":\"").append(rs.getString("Course_name")).append("\",")
                        .append("\"Course_price\":").append(rs.getInt("Course_price"))
                        .append("}");
                    first = false;
                }
                json.append("]");
                response = json.toString();

                if (response.equals("[]")) {
                    statusCode = 204;  // 沒有預約
                    response = "";
                }
            } catch (SQLException e) {
                statusCode = 500;
                response = "{\"error\":\"Database error\"}";
                e.printStackTrace();
            }
        }

        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");

        byte[] respBytes = response.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(statusCode, respBytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(respBytes);
        }
    }
}


    public static class InsertActualHandler implements HttpHandler {
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
            handleCORSPreflight(exchange);
            return;
        }

        Headers headers = exchange.getResponseHeaders();
        headers.set("Content-Type", "application/json; charset=utf-8");
        headers.set("Access-Control-Allow-Origin", "*");

        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            String msg = "{\"error\":\"Method Not Allowed\"}";
            exchange.sendResponseHeaders(405, msg.length());
            exchange.getResponseBody().write(msg.getBytes(StandardCharsets.UTF_8));
            exchange.close();
            return;
        }

        String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        Map<String, String> data = DBConnect.parseJson(body);

        String customerIdStr = data.get("customerId");
        if (customerIdStr == null) customerIdStr = data.get("customerid");

        String courseIdStr = data.get("courseId");
        if (courseIdStr == null) courseIdStr = data.get("courseid");

        String datetime = data.get("datetime");
        String priceStr = data.get("price");

        String response;
        int statusCode = 200;

        if (customerIdStr == null || courseIdStr == null || datetime == null || priceStr == null) {
            statusCode = 400;
            response = "{\"error\":\"Missing required fields\"}";
        } else {
            try {
                int customerId = Integer.valueOf(customerIdStr);
                int courseId = Integer.valueOf(courseIdStr);
                double price = Double.valueOf(priceStr);

                // datetime 要是 "yyyy-MM-dd HH:mm:ss" 格式，前端送來的格式必須對
                java.sql.Timestamp ts = java.sql.Timestamp.valueOf(datetime);

                String sql = "INSERT INTO actual (datetime, customerid, courseid, price) VALUES (?, ?, ?, ?)";
                int rows = DBConnect.executeUpdate(sql, ts, customerId, courseId, price);

                if (rows > 0) {
                    response = "{\"success\": true}";
                } else {
                    statusCode = 500;
                    response = "{\"error\":\"Insert failed\"}";
                }
            } catch (NumberFormatException e) {
                statusCode = 400;
                response = "{\"error\":\"Invalid number format\"}";
            } catch (IllegalArgumentException e) {
                // Timestamp.valueOf 可能丟這錯
                statusCode = 400;
                response = "{\"error\":\"Invalid datetime format\"}";
            } catch (Exception e) {
                statusCode = 500;
                response = "{\"error\":\"Server error\"}";
                e.printStackTrace();
            }
        }

        byte[] respBytes = response.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(statusCode, respBytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(respBytes);
        }
    }
}

public static class CheckSessionHandler implements HttpHandler {
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
            handleCORSPreflight(exchange);
            return;
        }

        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.getResponseHeaders().add("Content-Type", "application/json; charset=utf-8");
            exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            String msg = "{\"error\":\"Method Not Allowed\"}";
            exchange.sendResponseHeaders(405, msg.length());
            exchange.getResponseBody().write(msg.getBytes(StandardCharsets.UTF_8));
            exchange.close();
            return;
        }

        InputStream is = exchange.getRequestBody();
        String body = new String(is.readAllBytes(), StandardCharsets.UTF_8);
        Map<String, String> data = DBConnect.parseJson(body);

        String name = data.get("name");
        String phone = data.get("phone");

        String json = "[]";
        int status = 200;

        if (name == null || phone == null) {
            status = 400;
            json = "{\"error\":\"Missing name or phone\"}";
        } else {
            try {
                ResultSet rs = DBConnect.selectQuery(
                    "SELECT idmemberinfo FROM memberinfo WHERE Member_name = ? AND Member_phone = ?",
                    name, phone
                );

                if (!rs.next()) {
                    status = 404;
                    json = "{\"error\":\"Member not found\"}";
                } else {
                    String memberId = rs.getString("idmemberinfo");

                    // 預設每種課程對應總堂數
                    Map<String, Integer> totalCount = new HashMap<>();
                    totalCount.put("5", 10);
                    totalCount.put("6", 20);
                    totalCount.put("7", 10);
                    totalCount.put("8", 20);

                    // 查詢每種課程的使用紀錄與堂數
                    ResultSet sessionRs = DBConnect.selectQuery(
                        "SELECT a.courseid, p.Course_name, COUNT(*) AS used_count, GROUP_CONCAT(DATE(a.datetime) ORDER BY a.datetime) AS used_dates " +
                        "FROM actual a " +
                        "JOIN pricelist p ON a.courseid = p.idPriceList " +
                        "WHERE a.customerid = ? AND a.courseid IN (5,6,7,8) " +
                        "GROUP BY a.courseid, p.Course_name",
                        memberId
                    );

                    StringBuilder sb = new StringBuilder("[");
                    boolean first = true;

                    while (sessionRs.next()) {
                        String courseId = sessionRs.getString("courseid");
                        String courseName = sessionRs.getString("Course_name");
                        int used = sessionRs.getInt("used_count");
                        int total = totalCount.getOrDefault(courseId, 0);
                        int remaining = total - used;

                        String dateListRaw = sessionRs.getString("used_dates");
                        String[] usedDates = (dateListRaw != null) ? dateListRaw.split(",") : new String[0];

                        if (!first) sb.append(",");
                        sb.append("{")
                          .append("\"courseid\":\"").append(courseId).append("\",")
                          .append("\"Course_name\":\"").append(courseName).append("\",")
                          .append("\"total_sessions\":").append(total).append(",")
                          .append("\"used_sessions\":").append(used).append(",")
                          .append("\"remaining_sessions\":").append(remaining).append(",");

                        sb.append("\"used_dates\":[");
                        for (int i = 0; i < usedDates.length; i++) {
                            if (i > 0) sb.append(",");
                            sb.append("\"").append(usedDates[i]).append("\"");
                        }
                        sb.append("]}");
                        first = false;
                    }

                    sb.append("]");
                    json = sb.toString();
                }
            } catch (Exception e) {
                e.printStackTrace();
                status = 500;
                json = "{\"error\":\"Server error\"}";
            }
        }

        exchange.getResponseHeaders().add("Content-Type", "application/json; charset=utf-8");
        exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        byte[] respBytes = json.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(status, respBytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(respBytes);
        }
    }
}


    
}

