package com.bhojpurri;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

/**
 * Simple test to verify Speechify API connectivity.
 * Run this standalone to test the API before running the full app.
 */
public class TestSpeechify {
    
    private static final String SPEECHIFY_API_KEY = "leP8fg7FfN3g6CJEnyLW_lBalB1EUsCriNk9sRThK3M=";
    
    public static void main(String[] args) {
        System.out.println("🧪 Testing Speechify API Connection...\n");
        
        String[] endpoints = {
            "https://api.sws.speechify.com/v1/audio",
            "https://api.sws.speechify.com/v1/audio/speech",
            "https://audio.api.speechify.com/v1/audio"
        };
        
        for (String endpoint : endpoints) {
            System.out.println("🔹 Testing endpoint: " + endpoint);
            testEndpoint(endpoint);
            System.out.println();
        }
    }
    
    private static void testEndpoint(String endpoint) {
        try {
            HttpClient client = HttpClient.newHttpClient();
            
            String jsonBody = "{\"voice_id\":\"hi-IN\",\"text\":\"Namaste duniya\"}";
            System.out.println("   Request body: " + jsonBody);
            
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(endpoint))
                .header("Authorization", "Bearer " + SPEECHIFY_API_KEY)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();
            
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            
            System.out.println("   Status Code: " + response.statusCode());
            System.out.println("   Response: " + response.body());
            
            if (response.statusCode() == 200 || response.statusCode() == 201) {
                System.out.println("   ✅ SUCCESS!");
            } else {
                System.out.println("   ⚠️ Failed");
            }
            
        } catch (Exception e) {
            System.out.println("   ❌ Error: " + e.getMessage());
        }
    }
}
