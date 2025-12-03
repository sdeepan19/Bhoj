package com.bhojpurri;

import java.io.File;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.TargetDataLine;

/**
 * Test program to check if microphone recording is working properly.
 * Run this to diagnose recording issues.
 */
public class TestRecording {
    
    public static void main(String[] args) {
        System.out.println("🎤 Recording Test Program");
        System.out.println("========================\n");
        
        // Test 1: Check available mixers (audio devices)
        System.out.println("📋 Step 1: Checking available audio devices...\n");
        Mixer.Info[] mixerInfos = AudioSystem.getMixerInfo();
        System.out.println("Found " + mixerInfos.length + " audio devices:");
        for (int i = 0; i < mixerInfos.length; i++) {
            System.out.println("  [" + i + "] " + mixerInfos[i].getName());
            System.out.println("      " + mixerInfos[i].getDescription());
        }
        System.out.println();
        
        // Test 2: Check if recording format is supported
        System.out.println("📋 Step 2: Checking microphone support...\n");
        AudioFormat format = new AudioFormat(
            44100.0F,  // Sample rate
            16,        // Sample size in bits
            1,         // Channels (mono)
            true,      // Signed
            false      // Little endian
        );
        
        DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
        if (AudioSystem.isLineSupported(info)) {
            System.out.println("✅ Microphone format is supported!");
        } else {
            System.out.println("❌ Microphone format is NOT supported!");
            System.out.println("   This is the problem - trying alternate formats...\n");
            
            // Try common alternate formats
            tryAlternateFormats();
        }
        System.out.println();
        
        // Test 3: Actual recording test
        System.out.println("📋 Step 3: Testing actual recording...\n");
        System.out.println("Recording for 3 seconds... SPEAK NOW!");
        
        try {
            SpeechRecorder recorder = new SpeechRecorder();
            recorder.startRecording();
            
            Thread.sleep(3000); // Record for 3 seconds
            
            String filePath = recorder.stopRecording();
            
            if (filePath != null) {
                File file = new File(filePath);
                System.out.println("\n✅ Recording successful!");
                System.out.println("   File: " + filePath);
                System.out.println("   Size: " + file.length() + " bytes");
                
                if (file.length() < 1000) {
                    System.out.println("   ⚠️ WARNING: File is very small - microphone might not be working!");
                } else if (file.length() < 10000) {
                    System.out.println("   ⚠️ WARNING: File is smaller than expected for 3 seconds");
                } else {
                    System.out.println("   ✅ File size looks good!");
                }
                
                System.out.println("\n🔊 To play the recording, run:");
                System.out.println("   start " + filePath);
            } else {
                System.out.println("\n❌ Recording failed!");
            }
            
        } catch (Exception e) {
            System.out.println("\n❌ Recording failed with error:");
            e.printStackTrace();
        }
        
        System.out.println("\n========================");
        System.out.println("Test complete!");
    }
    
    private static void tryAlternateFormats() {
        float[] sampleRates = {8000.0F, 16000.0F, 44100.0F, 48000.0F};
        int[] sampleSizes = {8, 16};
        int[] channels = {1, 2};
        
        System.out.println("Trying different audio formats:");
        
        for (float rate : sampleRates) {
            for (int size : sampleSizes) {
                for (int channel : channels) {
                    AudioFormat format = new AudioFormat(rate, size, channel, true, false);
                    DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
                    
                    if (AudioSystem.isLineSupported(info)) {
                        System.out.printf("   ✅ SUPPORTED: %.0f Hz, %d-bit, %s\n", 
                            rate, size, channel == 1 ? "Mono" : "Stereo");
                    }
                }
            }
        }
    }
}
