package com.clinic.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseManager {

    private static final String URL = 
        "jdbc:postgresql://aws-1-ap-northeast-1.pooler.supabase.com:6543/postgres?sslmode=require";
    
    private static final String USER     = "postgres.gqntxvkurtebbidkxxzm";
    private static final String PASSWORD = "clineanicallyTested";

    private static Connection connection = null;

    public static Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            try {
                Class.forName("org.postgresql.Driver");
                connection = DriverManager.getConnection(URL, USER, PASSWORD);
                System.out.println("Supabase connected successfully!");
            } catch (ClassNotFoundException e) {
                throw new SQLException("PostgreSQL Driver not found!", e);
            }
        }
        return connection;
    }

    public static void closeConnection() {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException e) {
                System.err.println("Error closing: " + e.getMessage());
            }
        }
    }
}