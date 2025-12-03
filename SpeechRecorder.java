package com.bhojpurri;

import java.io.File;
import java.io.IOException;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.TargetDataLine;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles audio recording from the microphone.
 * Records audio in WAV format for processing.
 */
public class SpeechRecorder {
    private static final Logger logger = LoggerFactory.getLogger(SpeechRecorder.class);
    
    private TargetDataLine line;
    private Thread recordingThread;
    private File outputFile;
    private volatile boolean isRecording = false;

    // Audio format settings - 44.1kHz is more standard than 16kHz
    private static final float SAMPLE_RATE = 44100.0F;
    private static final int SAMPLE_SIZE_BITS = 16;
    private static final int CHANNELS = 1; // Mono
    private static final boolean SIGNED = true;
    private static final boolean BIG_ENDIAN = false; // Little-endian for better compatibility

    public SpeechRecorder() {
        // Create output directory if it doesn't exist
        File outputDir = new File("audio_recordings");
        if (!outputDir.exists()) {
            outputDir.mkdirs();
        }
        
        // Generate unique filename with timestamp
        String timestamp = String.valueOf(System.currentTimeMillis());
        outputFile = new File(outputDir, "recording_" + timestamp + ".wav");
    }

    /**
     * Starts recording audio from the microphone.
     */
    public void startRecording() {
        if (isRecording) {
            logger.warn("Recording already in progress");
            return;
        }

        try {
            // Define audio format
            AudioFormat format = new AudioFormat(
                SAMPLE_RATE, 
                SAMPLE_SIZE_BITS, 
                CHANNELS, 
                SIGNED, 
                BIG_ENDIAN
            );

            // Get and open the line
            DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
            
            if (!AudioSystem.isLineSupported(info)) {
                throw new LineUnavailableException("Microphone not supported");
            }

            line = (TargetDataLine) AudioSystem.getLine(info);
            line.open(format);
            line.start();
            
            isRecording = true;
            logger.info("Recording started to file: {}", outputFile.getAbsolutePath());

            // Start recording in a separate thread
            recordingThread = new Thread(() -> {
                try (AudioInputStream audioInputStream = new AudioInputStream(line)) {
                    AudioSystem.write(
                        audioInputStream, 
                        AudioFileFormat.Type.WAVE, 
                        outputFile
                    );
                } catch (IOException e) {
                    if (isRecording) {
                        logger.error("Error during recording", e);
                    }
                }
            });
            recordingThread.start();

        } catch (LineUnavailableException e) {
            logger.error("Microphone line unavailable", e);
            throw new RuntimeException("Cannot access microphone: " + e.getMessage(), e);
        }
    }

    /**
     * Stops recording and returns the path to the recorded file.
     * @return Absolute path to the recorded WAV file
     */
    public String stopRecording() {
        if (!isRecording) {
            logger.warn("No recording in progress to stop");
            return null;
        }

        try {
            isRecording = false;
            
            if (line != null) {
                line.stop();
                line.close();
                logger.info("Recording stopped");
            }

            if (recordingThread != null) {
                recordingThread.join(2000); // Wait up to 2 seconds for thread to finish
            }

            // Verify file was created
            if (outputFile.exists() && outputFile.length() > 0) {
                logger.info("Recording saved successfully: {} ({} bytes)", 
                    outputFile.getAbsolutePath(), outputFile.length());
                
                // Print for manual testing
                System.out.println("🎤 Recording saved at: " + outputFile.getAbsolutePath());
                System.out.println("   File size: " + outputFile.length() + " bytes");
                System.out.println("   Play it with: start " + outputFile.getAbsolutePath());
                
                return outputFile.getAbsolutePath();
            } else {
                throw new IOException("Recording file was not created or is empty");
            }

        } catch (InterruptedException e) {
            logger.error("Recording thread interrupted", e);
            Thread.currentThread().interrupt();
            return null;
        } catch (IOException e) {
            logger.error("Error saving recording", e);
            return null;
        } finally {
            // Generate new filename for next recording
            String timestamp = String.valueOf(System.currentTimeMillis());
            outputFile = new File("audio_recordings", "recording_" + timestamp + ".wav");
        }
    }

    /**
     * Checks if recording is currently in progress.
     * @return true if recording, false otherwise
     */
    public boolean isRecording() {
        return isRecording;
    }
}
