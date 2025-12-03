package com.bhojpurri;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manages Text-to-Speech conversion with ElevenLabs API (high quality) and Google TTS fallback.
 */
public class TTSManager {
    private static final Logger logger = LoggerFactory.getLogger(TTSManager.class);
    
    // ElevenLabs API Configuration (High Quality TTS)
    // Get your FREE API key from: https://elevenlabs.io/
    // Free tier: 10,000 characters/month with premium voices
    private static final String ELEVENLABS_API_KEY = EnvLoader.get("ELEVENLABS_API_KEY",
        "sk_db200f30e240c3696692703c78ee3c8b72296657d0f376ab"); // Replace with your key
    
    private static final String ELEVENLABS_ENDPOINT = "https://api.elevenlabs.io/v1/text-to-speech/";
    
    // Voice IDs for different languages/styles
    // Multilingual voices work great for Hindi/Bhojpuri
    private static final String VOICE_ID_ADAM = "pNInz6obpgDQGcFmaJgB"; // Deep, confident male
    private static final String VOICE_ID_BELLA = "EXAVITQu4vr4xnSDxMaL"; // Soft, clear female
    private static final String VOICE_ID_RACHEL = "21m00Tcm4TlvDq8ikWAM"; // Calm, natural female
    private static final String VOICE_ID_ANTONI = "ErXwobaYiN019PkySvjV"; // Well-rounded male
    
    // Using a multilingual voice for better Bhojpuri/Hindi pronunciation
    private static final String DEFAULT_VOICE_ID = VOICE_ID_BELLA;
    
    private final HttpClient httpClient;
    private final AudioPlayer audioPlayer;
    private final Path outputDirectory;
    
    /**
     * Maps language codes to Google TTS supported codes
     * Some languages aren't directly supported, so we map to closest alternative
     */
    private String mapLanguageCodeForGoogleTTS(String langCode) {
        // Map unsupported languages to closest supported alternatives
        switch (langCode) {
            case "bho": return "hi";  // Bhojpuri → Hindi (closest)
            case "pa": return "pa";   // Punjabi is supported
            case "gu": return "gu";   // Gujarati is supported
            case "kn": return "kn";   // Kannada is supported
            case "ml": return "ml";   // Malayalam is supported
            case "te": return "te";   // Telugu is supported
            case "ta": return "ta";   // Tamil is supported
            case "mr": return "mr";   // Marathi is supported
            case "bn": return "bn";   // Bengali is supported
            case "ur": return "ur";   // Urdu is supported
            default: return langCode; // Use as-is for all others
        }
    }

    public TTSManager() {
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(java.time.Duration.ofSeconds(30))
            .build();
        this.audioPlayer = new AudioPlayer();
        
        // Create output directory for TTS audio files
        this.outputDirectory = Paths.get("tts_output");
        try {
            if (!Files.exists(outputDirectory)) {
                Files.createDirectories(outputDirectory);
                System.out.println("📁 Created directory: " + outputDirectory.toAbsolutePath());
            }
        } catch (IOException e) {
            logger.error("Failed to create TTS output directory", e);
        }
    }

    /**
     * Converts text to speech - tries ElevenLabs first (premium quality), then falls back to Google TTS (FREE).
     * @param text The text to convert to speech
     * @return Path to the generated TTS audio file, or null if failed
     */
    public String speak(String text) throws IOException, InterruptedException {
        return speak(text, "hi"); // Default to Hindi for backward compatibility
    }

    /**
     * Converts text to speech in specified language - tries ElevenLabs first (premium quality), then falls back to Google TTS (FREE).
     * @param text The text to convert to speech
     * @param languageCode The language code (e.g., "hi" for Hindi, "es" for Spanish, "fr" for French)
     * @return Path to the generated TTS audio file, or null if failed
     */
    public String speak(String text, String languageCode) throws IOException, InterruptedException {
        if (text == null || text.trim().isEmpty()) {
            System.out.println("⚠️ Empty text provided for TTS");
            return null;
        }

        System.out.println("\n🔊 Starting TTS for: " + text + " (Language: " + languageCode + ")");
        logger.info("Converting text to speech: {} in language: {}", text, languageCode);

        boolean success = false;
        Exception lastException = null;
        String generatedFilePath = null;

        // Try ElevenLabs TTS first (high quality)
        if (!ELEVENLABS_API_KEY.equals("YOUR_ELEVENLABS_API_KEY_HERE")) {
            System.out.println("🎙️ Trying ElevenLabs TTS (Premium Quality)...");
            logger.info("Trying ElevenLabs TTS");
            
            try {
                generatedFilePath = useElevenLabsTTS(text, languageCode);
                System.out.println("✅ ElevenLabs TTS successful!");
                success = true;
            } catch (Exception e) {
                System.out.println("⚠️ ElevenLabs TTS failed: " + e.getMessage());
                logger.warn("ElevenLabs TTS failed: {}", e.getMessage());
                lastException = e;
            }
        } else {
            System.out.println("⚠️ ElevenLabs API key not set - skipping premium TTS");
            logger.info("ElevenLabs API key not configured, skipping");
        }

        // Fallback to Google TTS if ElevenLabs failed
        if (!success) {
            System.out.println("🔄 Using FREE Google TTS fallback...");
            logger.info("Falling back to Google TTS");
            try {
                generatedFilePath = useGoogleTTS(text, languageCode);
                System.out.println("✅ Google TTS successful!");
                success = true;
            } catch (Exception e) {
                System.out.println("❌ Google TTS also failed: " + e.getMessage());
                logger.error("Google TTS failed", e);
                lastException = e;
            }
        }

        // If everything failed, throw error
        if (!success) {
            System.out.println("❌ All TTS methods failed!");
            logger.error("All TTS methods failed");
            String errorMsg = lastException != null ? lastException.getMessage() : "Unknown error";
            throw new IOException("TTS conversion failed: " + errorMsg, lastException);
        }
        
        return generatedFilePath;
    }

    /**
     * ElevenLabs TTS - Premium quality, natural-sounding voices.
     * Free tier: 10,000 characters/month
     * Supports multilingual voices perfect for Hindi/Bhojpuri
     * @param languageCode Language code (not directly used but logged for tracking)
     */
    private String useElevenLabsTTS(String text, String languageCode) throws IOException, InterruptedException {
        Path outputFile = outputDirectory.resolve("tts_output.mp3");

        // Delete old file first to prevent accumulation
        try {
            if (Files.exists(outputFile)) {
                Files.delete(outputFile);
                System.out.println("   🗑️ Deleted old TTS file to prevent accumulation");
                logger.info("Deleted old TTS file: {}", outputFile);
            }
        } catch (IOException e) {
            logger.warn("Could not delete old TTS file: {}", e.getMessage());
        }

        try {
            String endpoint = ELEVENLABS_ENDPOINT + DEFAULT_VOICE_ID;
            
            // Request body for ElevenLabs
            JSONObject requestBody = new JSONObject();
            requestBody.put("text", text);
            requestBody.put("model_id", "eleven_multilingual_v2"); // Best for non-English languages
            
            // Voice settings for natural speech
            JSONObject voiceSettings = new JSONObject();
            voiceSettings.put("stability", 0.5);      // 0-1, higher = more stable but less expressive
            voiceSettings.put("similarity_boost", 0.75); // 0-1, higher = closer to original voice
            requestBody.put("voice_settings", voiceSettings);

            System.out.println("   ElevenLabs API URL: " + endpoint);
            System.out.println("   Voice: " + DEFAULT_VOICE_ID + " (Bella - Clear Female, Language: " + languageCode + ")");
            logger.info("Using ElevenLabs TTS with voice ID: {} for language: {}", DEFAULT_VOICE_ID, languageCode);

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(endpoint))
                .header("xi-api-key", ELEVENLABS_API_KEY)
                .header("Content-Type", "application/json")
                .header("Accept", "audio/mpeg")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody.toString()))
                .timeout(java.time.Duration.ofSeconds(30))
                .build();

            HttpResponse<Path> response = httpClient.send(
                request, 
                HttpResponse.BodyHandlers.ofFile(outputFile)
            );

            if (response.statusCode() == 200 && Files.exists(outputFile) && Files.size(outputFile) > 0) {
                System.out.println("   File size: " + Files.size(outputFile) + " bytes");
                System.out.println("   Saved to: " + outputFile.toAbsolutePath());
                System.out.println("   Playing audio...");
                audioPlayer.play(outputFile.toString());
                System.out.println("   ✅ Playback completed");
                return outputFile.toAbsolutePath().toString();
            } else {
                String errorMsg = "ElevenLabs TTS failed with status: " + response.statusCode();
                if (Files.exists(outputFile)) {
                    try {
                        String errorBody = Files.readString(outputFile);
                        System.out.println("   Error response: " + errorBody);
                        errorMsg += " - " + errorBody;
                    } catch (Exception ignored) {}
                }
                Files.deleteIfExists(outputFile);
                throw new IOException(errorMsg);
            }

        } catch (Exception e) {
            Files.deleteIfExists(outputFile);
            throw e;
        }
    }

    /**
     * Google TTS fallback - FREE and reliable!
     * Uses Google Translate's text-to-speech API (no key needed).
     * @param text The text to speak
     * @param languageCode The language code (e.g., "hi", "es", "fr", "ja", etc.)
     * @return Path to the generated audio file
     */
    private String useGoogleTTS(String text, String languageCode) throws IOException, InterruptedException {
        Path outputFile = outputDirectory.resolve("tts_output.mp3");

        // ⚠️ IMPORTANT: Delete old file first to prevent audio accumulation
        try {
            if (Files.exists(outputFile)) {
                Files.delete(outputFile);
                System.out.println("   🗑️ Deleted old TTS file to prevent accumulation");
                logger.info("Deleted old TTS file: {}", outputFile);
            }
        } catch (IOException e) {
            logger.warn("Could not delete old TTS file: {}", e.getMessage());
        }

        try {
            String encodedText = URLEncoder.encode(text, "UTF-8");
            // Map language code to Google TTS supported code
            String googleLangCode = mapLanguageCodeForGoogleTTS(languageCode);
            String url = "https://translate.google.com/translate_tts?ie=UTF-8&q=" + encodedText + "&tl=" + googleLangCode + "&client=tw-ob";
            
            System.out.println("   Google TTS URL: " + url);
            System.out.println("   Language: " + languageCode + " (mapped to: " + googleLangCode + ")");
            logger.info("Using Google TTS with language: {} (mapped to: {})", languageCode, googleLangCode);

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .GET()
                .timeout(java.time.Duration.ofSeconds(30))
                .build();

            HttpResponse<Path> response = httpClient.send(
                request, 
                HttpResponse.BodyHandlers.ofFile(outputFile)
            );

            if (response.statusCode() == 200 && Files.exists(outputFile) && Files.size(outputFile) > 0) {
                System.out.println("   File size: " + Files.size(outputFile) + " bytes");
                System.out.println("   Saved to: " + outputFile.toAbsolutePath());
                System.out.println("   Playing audio...");
                audioPlayer.play(outputFile.toString());
                System.out.println("   ✅ Playback completed");
                return outputFile.toAbsolutePath().toString();
            } else {
                Files.deleteIfExists(outputFile);
                throw new IOException("Google TTS failed with status: " + response.statusCode());
            }

        } catch (Exception e) {
            Files.deleteIfExists(outputFile);
            throw e;
        }
    }

    /**
     * Cleans up old TTS audio files.
     */
    public void cleanupOldFiles() {
        try {
            long currentTime = System.currentTimeMillis();
            long oneHourAgo = currentTime - (60 * 60 * 1000);

            Files.list(outputDirectory)
                .filter(path -> path.toString().endsWith(".mp3"))
                .filter(path -> {
                    try {
                        return Files.getLastModifiedTime(path).toMillis() < oneHourAgo;
                    } catch (IOException e) {
                        return false;
                    }
                })
                .forEach(path -> {
                    try {
                        Files.delete(path);
                        logger.debug("Deleted old TTS file: {}", path);
                    } catch (IOException e) {
                        logger.warn("Failed to delete old file: {}", path);
                    }
                });
        } catch (IOException e) {
            logger.warn("Error during cleanup", e);
        }
    }
}
