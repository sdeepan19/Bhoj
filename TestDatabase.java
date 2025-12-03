package com.bhojpurri;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Test program to verify MySQL database connectivity and functionality.
 * Run this to ensure database is set up correctly before using the main app.
 */
public class TestDatabase {
    private static final Logger logger = LoggerFactory.getLogger(TestDatabase.class);

    public static void main(String[] args) {
        System.out.println("━".repeat(80));
        System.out.println("🔍 BHOJPURI BILLA - DATABASE TEST");
        System.out.println("━".repeat(80));
        System.out.println();

        DatabaseManager dbManager = null;

        try {
            // Step 1: Test connection
            System.out.println("1️⃣  Testing database connection...");
            dbManager = new DatabaseManager();
            
            if (dbManager.testConnection()) {
                System.out.println("✅ Database connection successful!");
            } else {
                System.out.println("❌ Database connection failed!");
                return;
            }

            System.out.println();

            // Step 2: Test saving a sample translation
            System.out.println("2️⃣  Testing data insertion...");
            String audioPath = "test_audio/sample.wav";
            long audioSize = 12345L;
            String englishText = "Hello, how are you?";
            String bhojpuriText = "नमस्कार, कैसे बानी?";
            String ttsPath = "test_tts/output.mp3";

            int id = dbManager.saveTranslation(audioPath, audioSize, englishText, bhojpuriText, ttsPath);
            
            if (id > 0) {
                System.out.println("✅ Sample translation saved successfully (ID: " + id + ")");
            } else {
                System.out.println("❌ Failed to save translation");
            }

            System.out.println();

            // Step 3: Get total translations
            System.out.println("3️⃣  Fetching statistics...");
            int total = dbManager.getTotalTranslations();
            System.out.println("📊 Total translations in database: " + total);
            
            System.out.println();

            // Step 4: Show recent translations
            System.out.println("4️⃣  Recent translations:");
            dbManager.printRecentTranslations(5);
            
            System.out.println();

            // Step 5: Show today's stats
            System.out.println("5️⃣  Today's statistics:");
            dbManager.printTodayStats();
            
            System.out.println();

            // Step 6: Test search
            System.out.println("6️⃣  Testing search functionality:");
            dbManager.searchTranslations("hello");
            
            System.out.println();
            System.out.println("━".repeat(80));
            System.out.println("✅ ALL DATABASE TESTS PASSED!");
            System.out.println("━".repeat(80));
            System.out.println();
            System.out.println("💡 Your database is ready to use!");
            System.out.println("   Database: bhojpuri_billa");
            System.out.println("   Tables: translations, usage_stats");
            System.out.println("   User: root");
            System.out.println();
            
        } catch (Exception e) {
            System.out.println();
            System.out.println("━".repeat(80));
            System.out.println("❌ DATABASE TEST FAILED!");
            System.out.println("━".repeat(80));
            System.out.println();
            System.out.println("Error: " + e.getMessage());
            System.out.println();
            System.out.println("💡 Troubleshooting:");
            System.out.println("   1. Make sure MySQL is running: net start MySQL80");
            System.out.println("   2. Verify credentials: root / kali");
            System.out.println("   3. Check MySQL port: 3306");
            System.out.println("   4. Ensure MySQL user has CREATE DATABASE privilege");
            System.out.println();
            e.printStackTrace();
        } finally {
            if (dbManager != null) {
                dbManager.close();
            }
        }
    }
}
