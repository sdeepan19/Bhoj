package com.bhojpurri;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles transcription and translation using Groq Whisper API and OpenL Translate API.
 * Transcribes English speech to text and translates to Bhojpuri.
 */
public class Translator {
    private static final Logger logger = LoggerFactory.getLogger(Translator.class);
    
    // Groq API Configuration (FREE Whisper API)
    // Prefer environment variable GROQ_API_KEY; fall back to hardcoded value if not set.
    private static final String GROQ_API_KEY = EnvLoader.get("GROQ_API_KEY", 
        "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"); // Get from https://console.groq.com/
    private static final String GROQ_WHISPER_ENDPOINT = "https://api.groq.com/openai/v1/audio/transcriptions";
    private static final String GROQ_MODEL = "whisper-large-v3-turbo"; // TURBO model is 8x faster!
    
    // OpenL Translate API Configuration
    private static final String OPENL_API_KEY = "0877a9d4f9msh08264169aeb9030p1f75d8jsnd78f5a07b348";
    private static final String OPENL_HOST = "openl-translate.p.rapidapi.com";
    private static final String TRANSLATE_ENDPOINT = "https://" + OPENL_HOST + "/translate";
    
    private final HttpClient httpClient;

    public Translator() {
        // Create HTTP client with relaxed SSL verification to fix SSL handshake errors
        HttpClient client;
        try {
            // Create a trust manager that accepts all certificates
            TrustManager[] trustAllCerts = new TrustManager[]{
                new X509TrustManager() {
                    public X509Certificate[] getAcceptedIssuers() { return null; }
                    public void checkClientTrusted(X509Certificate[] certs, String authType) { }
                    public void checkServerTrusted(X509Certificate[] certs, String authType) { }
                }
            };
            
            // Install the all-trusting trust manager
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, trustAllCerts, new SecureRandom());
            
            client = HttpClient.newBuilder()
                .connectTimeout(java.time.Duration.ofSeconds(10))  // Reduced from 30s to 10s
                .sslContext(sslContext)
                .build();
                
            logger.info("HTTP client initialized with SSL workaround for Groq API");
        } catch (NoSuchAlgorithmException | KeyManagementException e) {
            logger.warn("Failed to setup SSL context, using default: {}", e.getMessage());
            // Fallback to default client
            client = HttpClient.newBuilder()
                .connectTimeout(java.time.Duration.ofSeconds(10))  // Reduced from 30s to 10s
                .build();
        }
        this.httpClient = client;
    }

    /**
     * Transcribes audio file to English text using Groq's FREE Whisper API.
     * 
     * @param audioFilePath Path to the recorded WAV file
     * @return Transcribed English text
     * @throws IOException If transcription fails
     */
    public String transcribeToEnglish(String audioFilePath) throws IOException {
        logger.info("Transcribing audio file using Groq Whisper: {}", audioFilePath);
        
        // Check if API key is configured
        if (GROQ_API_KEY.equals("YOUR_GROQ_API_KEY_HERE")) {
            String errorMsg = "Groq API key not configured! Get your FREE key from https://console.groq.com/";
            logger.error(errorMsg);
            System.out.println("\n❌ " + errorMsg);
            throw new IllegalStateException(errorMsg);
        }
        
        File audioFile = new File(audioFilePath);
        if (!audioFile.exists() || audioFile.length() == 0) {
            throw new IOException("Audio file is empty or does not exist");
        }
        
        logger.info("Audio file size: {} bytes", audioFile.length());
        System.out.println("🎤 Transcribing audio with Groq Whisper API...");
        
        try {
            // Create multipart form data request
            String boundary = "----WebKitFormBoundary" + System.currentTimeMillis();
            byte[] audioBytes = Files.readAllBytes(audioFile.toPath());
            
            // Build multipart body
            StringBuilder bodyBuilder = new StringBuilder();
            
            // Add file part
            bodyBuilder.append("--").append(boundary).append("\r\n");
            bodyBuilder.append("Content-Disposition: form-data; name=\"file\"; filename=\"")
                      .append(audioFile.getName()).append("\"\r\n");
            bodyBuilder.append("Content-Type: audio/wav\r\n\r\n");
            
            String bodyPrefix = bodyBuilder.toString();
            
            bodyBuilder = new StringBuilder();
            bodyBuilder.append("\r\n");
            
            // Add model part
            bodyBuilder.append("--").append(boundary).append("\r\n");
            bodyBuilder.append("Content-Disposition: form-data; name=\"model\"\r\n\r\n");
            bodyBuilder.append(GROQ_MODEL).append("\r\n");
            
            // Add response format
            bodyBuilder.append("--").append(boundary).append("\r\n");
            bodyBuilder.append("Content-Disposition: form-data; name=\"response_format\"\r\n\r\n");
            bodyBuilder.append("json").append("\r\n");
            
            bodyBuilder.append("--").append(boundary).append("--\r\n");
            
            String bodySuffix = bodyBuilder.toString();
            
            // Combine all parts
            byte[] prefixBytes = bodyPrefix.getBytes("UTF-8");
            byte[] suffixBytes = bodySuffix.getBytes("UTF-8");
            byte[] fullBody = new byte[prefixBytes.length + audioBytes.length + suffixBytes.length];
            
            System.arraycopy(prefixBytes, 0, fullBody, 0, prefixBytes.length);
            System.arraycopy(audioBytes, 0, fullBody, prefixBytes.length, audioBytes.length);
            System.arraycopy(suffixBytes, 0, fullBody, prefixBytes.length + audioBytes.length, suffixBytes.length);
            
            // Create HTTP request
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(GROQ_WHISPER_ENDPOINT))
                .header("Authorization", "Bearer " + GROQ_API_KEY)
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .POST(HttpRequest.BodyPublishers.ofByteArray(fullBody))
                .timeout(java.time.Duration.ofSeconds(15))  // Reduced from 60s to 15s
                .build();
            
            logger.debug("Sending transcription request to Groq Whisper API");
            
            // Send request
            HttpResponse<String> response = httpClient.send(
                request,
                HttpResponse.BodyHandlers.ofString()
            );
            
            logger.debug("Received response with status code: {}", response.statusCode());
            
            if (response.statusCode() == 200) {
                JSONObject jsonResponse = new JSONObject(response.body());
                String transcribedText = jsonResponse.getString("text");
                
                logger.info("Transcription successful: {}", transcribedText);
                System.out.println("✅ Transcribed: " + transcribedText);
                
                return transcribedText;
                
            } else {
                String errorMsg = "Groq Whisper API error (status " + response.statusCode() + "): " + response.body();
                logger.error(errorMsg);
                System.out.println("❌ " + errorMsg);
                throw new IOException(errorMsg);
            }
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Transcription interrupted: " + e.getMessage(), e);
        } catch (Exception e) {
            logger.error("Error transcribing audio with Groq", e);
            throw new IOException("Transcription failed: " + e.getMessage(), e);
        }
    }

    /**
     * Translates English text to Bhojpuri using OpenL Translate API.
     * 
     * @param englishText The English text to translate
     * @return Translated Bhojpuri text
     * @throws IOException If network error occurs
     * @throws InterruptedException If request is interrupted
     */
    public String translateToBhojpuri(String englishText) throws IOException, InterruptedException {
        return translateTo(englishText, "bho"); // Default to Bhojpuri
    }

    /**
     * Translates English text to any target language using OpenL Translate API.
     * 
     * @param englishText The English text to translate
     * @param targetLangCode The target language code (e.g., "bho", "hi", "es", "fr")
     * @return Translated text
     * @throws IOException If network error occurs
     * @throws InterruptedException If request is interrupted
     */
    public String translateTo(String englishText, String targetLangCode) throws IOException, InterruptedException {
        if (englishText == null || englishText.trim().isEmpty()) {
            logger.warn("Empty text provided for translation");
            return "";
        }

        logger.info("Translating to {}: {}", targetLangCode, englishText);

        try {
            // Prepare request body
            JSONObject requestBody = new JSONObject();
            requestBody.put("source_lang", "en");
            requestBody.put("target_lang", targetLangCode);
            requestBody.put("text", englishText);

            // Build HTTP request
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(TRANSLATE_ENDPOINT))
                .header("Content-Type", "application/json")
                .header("x-rapidapi-key", OPENL_API_KEY)
                .header("x-rapidapi-host", OPENL_HOST)
                .POST(HttpRequest.BodyPublishers.ofString(requestBody.toString()))
                .timeout(java.time.Duration.ofSeconds(10))  // Reduced from 30s to 10s
                .build();

            logger.debug("Sending translation request to OpenL API");

            // Send request
            HttpResponse<String> response = httpClient.send(
                request, 
                HttpResponse.BodyHandlers.ofString()
            );

            logger.debug("Received response with status code: {}", response.statusCode());

            // Handle response
            if (response.statusCode() == 200) {
                JSONObject jsonResponse = new JSONObject(response.body());
                
                // Parse translation from response
                // Expected format: {"translations": [{"text": "translated text"}]}
                if (jsonResponse.has("translatedText")) {
                    String translation = jsonResponse.getString("translatedText");
                    logger.info("Translation successful: {}", translation);
                    return translation;
                } else if (jsonResponse.has("translations")) {
                    JSONArray translations = jsonResponse.getJSONArray("translations");
                    if (translations.length() > 0) {
                        String translation = translations.getJSONObject(0).getString("text");
                        logger.info("Translation successful: {}", translation);
                        return translation;
                    }
                } else if (jsonResponse.has("data")) {
                    // Alternative response format
                    String translation = jsonResponse.getJSONObject("data").getString("translatedText");
                    logger.info("Translation successful: {}", translation);
                    return translation;
                }
                
                logger.error("Unexpected response format: {}", response.body());
                throw new IOException("Unexpected API response format");
                
            } else {
                String errorMessage = "API returned error code: " + response.statusCode();
                logger.error("{}, Response: {}", errorMessage, response.body());
                throw new IOException(errorMessage + " - " + response.body());
            }

        } catch (Exception e) {
            logger.error("Translation failed", e);
            throw new RuntimeException("Translation error: " + e.getMessage(), e);
        }
    }

    /**
     * Test method to verify API connectivity.
     * @return true if API is accessible, false otherwise
     */
    public boolean testConnection() {
        try {
            translateToBhojpuri("test");
            return true;
        } catch (Exception e) {
            logger.error("Connection test failed", e);
            return false;
        }
    }
}
