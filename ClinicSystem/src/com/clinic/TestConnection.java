package com.clinic;

import com.clinic.db.DatabaseManager;
import com.google.gson.JsonArray;

public class TestConnection {

    public static void main(String[] args) {

        System.out.println("=========================================");
        System.out.println("  Barangay Clinic - Connection Test");
        System.out.println("=========================================");
        System.out.println("Connecting to Supabase...");
        System.out.println();

        try {
            JsonArray result = DatabaseManager.getRows("connection_test");

            if (result != null && result.size() > 0) {
                System.out.println(">>> CONNECTION SUCCESSFUL! <<<");
                System.out.println();
                System.out.println("Rows returned: " + result.size());
                System.out.println("Data from database:");

                for (int i = 0; i < result.size(); i++) {
                    System.out.println("  Row " + (i + 1) + ": " + result.get(i));
                }

                System.out.println();
                System.out.println("Your database is online and accessible.");
                System.out.println("You can now build the full clinic system.");

            } else {
                System.out.println("Connected but no data found.");
                System.out.println("Make sure you ran the SQL INSERT in Supabase.");
            }

        } catch (Exception e) {
            System.out.println(">>> CONNECTION FAILED <<<");
            System.out.println("Error: " + e.getMessage());
            System.out.println();
            System.out.println("Check:");
            System.out.println("  1. Your SUPABASE_URL in DatabaseManager.java");
            System.out.println("  2. Your SUPABASE_KEY in DatabaseManager.java");
            System.out.println("  3. That your laptop has internet access");
            System.out.println("  4. That all 5 JARs are in the Build Path");
            e.printStackTrace();
        }
    }
}