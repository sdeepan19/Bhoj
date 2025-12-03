package com.bhojpurri;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * DatabaseManager handles all MySQL database operations for storing
 * audio recordings, transcriptions, and translations.
 */
public class DatabaseManager {
    private static final Logger logger = LoggerFactory.getLogger(DatabaseManager.class);
    
    // Database connection details
    private static final String DB_URL = "jdbc:mysql://localhost:3306/";
    private static final String DB_NAME = "bhojpuri_billa";
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "kali";
    // Connection parameters for proper UTF-8 support
    private static final String CONNECTION_PARAMS = "?useUnicode=true&characterEncoding=utf8&useSSL=false&allowPublicKeyRetrieval=true";
    
    private Connection connection;

    /**
     * Constructor - initializes database connection
     */
    public DatabaseManager() {
        try {
            initializeDatabase();
            logger.info("✅ Database connection established successfully");
        } catch (SQLException e) {
            logger.error("❌ Failed to initialize database", e);
        }
    }

    /**
     * Initialize database: create DB if not exists, then create tables
     */
    private void initializeDatabase() throws SQLException {
        // First connect without specifying database to create it
        try (Connection tempConn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             Statement stmt = tempConn.createStatement()) {
            
            // Create database if not exists
            String createDB = "CREATE DATABASE IF NOT EXISTS " + DB_NAME + 
                            " CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci";
            stmt.executeUpdate(createDB);
            logger.info("📦 Database '{}' created/verified", DB_NAME);
        }

        // Now connect to the specific database with UTF-8 parameters
        connection = DriverManager.getConnection(DB_URL + DB_NAME + CONNECTION_PARAMS, DB_USER, DB_PASSWORD);
        
        // Set connection character set to utf8mb4
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("SET NAMES 'utf8mb4'");
            stmt.execute("SET CHARACTER SET utf8mb4");
            stmt.execute("SET character_set_connection=utf8mb4");
        }
        
        // Create tables
        createTables();
    }

    /**
     * Create necessary tables if they don't exist
     */
    private void createTables() throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            
            // Main translations table
            String createTranslationsTable = 
                "CREATE TABLE IF NOT EXISTS translations (" +
                "  id INT AUTO_INCREMENT PRIMARY KEY," +
                "  audio_file_path VARCHAR(500) NOT NULL," +
                "  audio_file_size BIGINT," +
                "  english_text TEXT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci," +
                "  translated_text TEXT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci," +
                "  target_language VARCHAR(10) DEFAULT 'bho'," +
                "  tts_file_path VARCHAR(500)," +
                "  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                "  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP," +
                "  INDEX idx_created_at (created_at)," +
                "  INDEX idx_audio_path (audio_file_path(255))," +
                "  INDEX idx_language (target_language)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci";
            
            stmt.executeUpdate(createTranslationsTable);
            logger.info("📋 Table 'translations' created/verified");
            
            // Stats table for tracking usage
            String createStatsTable = 
                "CREATE TABLE IF NOT EXISTS usage_stats (" +
                "  id INT AUTO_INCREMENT PRIMARY KEY," +
                "  date DATE NOT NULL," +
                "  total_recordings INT DEFAULT 0," +
                "  total_translations INT DEFAULT 0," +
                "  total_audio_size BIGINT DEFAULT 0," +
                "  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                "  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP," +
                "  UNIQUE KEY unique_date (date)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci";
            
            stmt.executeUpdate(createStatsTable);
            logger.info("📊 Table 'usage_stats' created/verified");
        }
    }

    /**
     * Save a complete translation record to database
     * 
     * @param audioPath Path to the recorded audio file
     * @param audioSize Size of audio file in bytes
     * @param englishText Transcribed English text
     * @param translatedText Translated text in target language
     * @param targetLanguage Target language code (e.g., "bho", "hi", "es")
     * @param ttsPath Path to generated TTS audio file
     * @return The ID of the inserted record, or -1 if failed
     */
    public int saveTranslation(String audioPath, long audioSize, String englishText, 
                               String translatedText, String targetLanguage, String ttsPath) {
        String sql = "INSERT INTO translations (audio_file_path, audio_file_size, " +
                    "english_text, translated_text, target_language, tts_file_path) VALUES (?, ?, ?, ?, ?, ?)";
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, audioPath);
            pstmt.setLong(2, audioSize);
            pstmt.setString(3, englishText);
            pstmt.setString(4, translatedText);
            pstmt.setString(5, targetLanguage);
            pstmt.setString(6, ttsPath);
            
            int affectedRows = pstmt.executeUpdate();
            
            if (affectedRows > 0) {
                try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        int id = generatedKeys.getInt(1);
                        logger.info("💾 Saved translation to database (ID: {}, Language: {})", id, targetLanguage);
                        updateDailyStats(audioSize);
                        return id;
                    }
                }
            }
        } catch (SQLException e) {
            logger.error("❌ Failed to save translation to database", e);
        }
        return -1;
    }

    /**
     * Save a complete translation record to database (backward compatibility - defaults to Bhojpuri)
     * 
     * @param audioPath Path to the recorded audio file
     * @param audioSize Size of audio file in bytes
     * @param englishText Transcribed English text
     * @param translatedText Translated text
     * @param ttsPath Path to generated TTS audio file
     * @return The ID of the inserted record, or -1 if failed
     */
    public int saveTranslation(String audioPath, long audioSize, String englishText, 
                               String translatedText, String ttsPath) {
        return saveTranslation(audioPath, audioSize, englishText, translatedText, "bho", ttsPath);
    }

    /**
     * Update daily usage statistics
     */
    private void updateDailyStats(long audioSize) {
        String sql = "INSERT INTO usage_stats (date, total_recordings, total_translations, total_audio_size) " +
                    "VALUES (CURDATE(), 1, 1, ?) " +
                    "ON DUPLICATE KEY UPDATE " +
                    "total_recordings = total_recordings + 1, " +
                    "total_translations = total_translations + 1, " +
                    "total_audio_size = total_audio_size + ?";
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setLong(1, audioSize);
            pstmt.setLong(2, audioSize);
            pstmt.executeUpdate();
            logger.debug("📈 Daily stats updated");
        } catch (SQLException e) {
            logger.error("⚠️ Failed to update daily stats", e);
        }
    }

    /**
     * Get total number of translations stored
     */
    public int getTotalTranslations() {
        String sql = "SELECT COUNT(*) FROM translations";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            logger.error("Failed to get total translations", e);
        }
        return 0;
    }

    /**
     * Get recent translations (last N records)
     */
    public void printRecentTranslations(int limit) {
        String sql = "SELECT id, english_text, bhojpuri_text, created_at " +
                    "FROM translations ORDER BY created_at DESC LIMIT ?";
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, limit);
            try (ResultSet rs = pstmt.executeQuery()) {
                System.out.println("\n📜 Recent Translations:");
                System.out.println("━".repeat(80));
                
                while (rs.next()) {
                    int id = rs.getInt("id");
                    String english = rs.getString("english_text");
                    String bhojpuri = rs.getString("bhojpuri_text");
                    Timestamp created = rs.getTimestamp("created_at");
                    
                    System.out.println("ID: " + id + " | " + created);
                    System.out.println("English: " + (english != null ? english : "N/A"));
                    System.out.println("Bhojpuri: " + (bhojpuri != null ? bhojpuri : "N/A"));
                    System.out.println("─".repeat(80));
                }
            }
        } catch (SQLException e) {
            logger.error("Failed to fetch recent translations", e);
        }
    }

    /**
     * Get today's statistics
     */
    public void printTodayStats() {
        String sql = "SELECT total_recordings, total_translations, total_audio_size " +
                    "FROM usage_stats WHERE date = CURDATE()";
        
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            if (rs.next()) {
                int recordings = rs.getInt("total_recordings");
                int translations = rs.getInt("total_translations");
                long audioSize = rs.getLong("total_audio_size");
                
                System.out.println("\n📊 Today's Stats:");
                System.out.println("━".repeat(50));
                System.out.println("🎙️  Recordings: " + recordings);
                System.out.println("🔄 Translations: " + translations);
                System.out.println("💾 Audio Data: " + formatBytes(audioSize));
                System.out.println("━".repeat(50));
            } else {
                System.out.println("\n📊 No activity today yet.");
            }
        } catch (SQLException e) {
            logger.error("Failed to fetch today's stats", e);
        }
    }

    /**
     * Format bytes to human-readable format
     */
    private String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        String pre = "KMGTPE".charAt(exp - 1) + "";
        return String.format("%.2f %sB", bytes / Math.pow(1024, exp), pre);
    }

    /**
     * Search translations by English or Bhojpuri text
     */
    public void searchTranslations(String searchTerm) {
        String sql = "SELECT id, english_text, bhojpuri_text, created_at " +
                    "FROM translations " +
                    "WHERE english_text LIKE ? OR bhojpuri_text LIKE ? " +
                    "ORDER BY created_at DESC LIMIT 10";
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            String searchPattern = "%" + searchTerm + "%";
            pstmt.setString(1, searchPattern);
            pstmt.setString(2, searchPattern);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                System.out.println("\n🔍 Search Results for: '" + searchTerm + "'");
                System.out.println("━".repeat(80));
                
                boolean found = false;
                while (rs.next()) {
                    found = true;
                    int id = rs.getInt("id");
                    String english = rs.getString("english_text");
                    String bhojpuri = rs.getString("bhojpuri_text");
                    Timestamp created = rs.getTimestamp("created_at");
                    
                    System.out.println("ID: " + id + " | " + created);
                    System.out.println("English: " + english);
                    System.out.println("Bhojpuri: " + bhojpuri);
                    System.out.println("─".repeat(80));
                }
                
                if (!found) {
                    System.out.println("No results found.");
                }
            }
        } catch (SQLException e) {
            logger.error("Failed to search translations", e);
        }
    }

    /**
     * Close database connection
     */
    public void close() {
        if (connection != null) {
            try {
                connection.close();
                logger.info("🔌 Database connection closed");
            } catch (SQLException e) {
                logger.error("Failed to close database connection", e);
            }
        }
    }

    /**
     * Test method to verify database connectivity
     */
    public boolean testConnection() {
        try {
            return connection != null && !connection.isClosed();
        } catch (SQLException e) {
            return false;
        }
    }
}
