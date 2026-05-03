package com.clinic;

import java.sql.Connection;
import com.clinic.db.DatabaseManager;

public class TestConnection {
    public static void main(String[] args) {
        System.out.println("Testing connection to Supabase...");
        
        try {
            Connection conn = DatabaseManager.getConnection();
            
            if (conn != null) {
                System.out.println("SUCCESS! Database is connected.");
                System.out.println("You can now build the full clinic system.");
            }
            
        } catch (Exception e) {
            System.out.println("FAILED: " + e.getMessage());
            e.printStackTrace();
        }
    }
}