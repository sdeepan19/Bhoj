package com.bhojpurri;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ServiceLoader;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineEvent;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.sound.sampled.spi.AudioFileReader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles audio playback for TTS output.
 * Supports both WAV and MP3 formats.
 */
public class AudioPlayer {
    private static final Logger logger = LoggerFactory.getLogger(AudioPlayer.class);
    
    private Clip currentClip;
    
    static {
        // Check if mp3spi is available
        ServiceLoader<AudioFileReader> readers = ServiceLoader.load(AudioFileReader.class);
        boolean mp3SpiFound = false;
        System.out.println("\n🔍 Checking audio codec providers:");
        for (AudioFileReader reader : readers) {
            System.out.println("   - " + reader.getClass().getName());
            if (reader.getClass().getName().toLowerCase().contains("mp3") || 
                reader.getClass().getName().toLowerCase().contains("mpeg")) {
                mp3SpiFound = true;
            }
        }
        if (!mp3SpiFound) {
            System.out.println("⚠️  WARNING: MP3 codec (mp3spi) not found! MP3 playback will fail.");
            System.out.println("   Run with: mvn exec:java -Dexec.classpathScope=runtime");
        } else {
            System.out.println("✅ MP3 codec loaded successfully");
        }
    }

    /**
     * Plays an audio file through the system speakers.
     * Supports WAV and MP3 formats (requires mp3spi library for MP3).
     * 
     * @param filePath Path to the audio file to play
     * @throws RuntimeException if playback fails
     */
    public void play(String filePath) {
        if (filePath == null || filePath.isEmpty()) {
            logger.error("Invalid file path provided for playback");
            return;
        }

        File audioFile = new File(filePath);
        if (!audioFile.exists()) {
            logger.error("Audio file does not exist: {}", filePath);
            throw new RuntimeException("Audio file not found: " + filePath);
        }

        logger.info("Playing audio file: {} ({} bytes)", filePath, audioFile.length());

        try {
            // Stop any currently playing audio
            stopCurrentPlayback();

            // Get audio input stream
            AudioInputStream audioStream = AudioSystem.getAudioInputStream(audioFile);
            
            // Get audio format
            AudioFormat format = audioStream.getFormat();
            logger.debug("Audio format: {}", format);

            // Handle compressed formats (like MP3)
            AudioFormat decodedFormat = getDecodedFormat(format);
            if (decodedFormat != null) {
                logger.debug("Decoding audio from {} to PCM", format.getEncoding());
                audioStream = AudioSystem.getAudioInputStream(decodedFormat, audioStream);
            }

            // Get a clip resource
            DataLine.Info info = new DataLine.Info(Clip.class, audioStream.getFormat());
            
            if (!AudioSystem.isLineSupported(info)) {
                logger.error("Audio line not supported for format: {}", audioStream.getFormat());
                throw new LineUnavailableException("Audio line not supported");
            }

            currentClip = (Clip) AudioSystem.getLine(info);
            currentClip.open(audioStream);

            // Add listener for completion
            currentClip.addLineListener(event -> {
                if (event.getType() == LineEvent.Type.STOP) {
                    logger.debug("Audio playback completed");
                }
            });

            // Start playback
            currentClip.start();
            logger.info("Audio playback started");

            // Wait for playback to complete
            Thread.sleep(currentClip.getMicrosecondLength() / 1000);

            // Cleanup
            currentClip.drain();
            currentClip.close();
            audioStream.close();
            
            logger.info("Audio playback finished successfully");

        } catch (UnsupportedAudioFileException e) {
            // javax.sound couldn't handle this audio file - try JLayer (pure MP3 playback)
            logger.warn("javax.sound reported unsupported audio format for {} — trying JLayer fallback", filePath);
            try {
                playWithJLayer(audioFile);
                return;
            } catch (Exception jlEx) {
                logger.error("JLayer fallback also failed", jlEx);
                throw new RuntimeException("Unsupported audio format: " + e.getMessage(), e);
            }
        } catch (IOException e) {
            logger.error("I/O error during audio playback", e);
            throw new RuntimeException("Audio playback I/O error: " + e.getMessage(), e);
        } catch (LineUnavailableException e) {
            logger.error("Audio line unavailable", e);
            throw new RuntimeException("Cannot access audio output: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            logger.error("Audio playback interrupted", e);
            Thread.currentThread().interrupt();
            throw new RuntimeException("Audio playback interrupted: " + e.getMessage(), e);
        }
    }

    /**
     * Fallback MP3 playback using JLayer (javazoom.jl.player.Player).
     * This is used when javax.sound.sampled cannot decode the MP3.
     */
    private void playWithJLayer(File audioFile) throws Exception {
        logger.info("Playing MP3 with JLayer fallback: {}", audioFile.getAbsolutePath());
        try (FileInputStream fis = new FileInputStream(audioFile)) {
            javazoom.jl.player.Player player = new javazoom.jl.player.Player(fis);
            // JLayer playback is blocking until completion
            player.play();
            logger.info("JLayer playback finished successfully");
        }
    }

    /**
     * Gets the decoded PCM format for compressed audio formats.
     * 
     * @param baseFormat The original audio format
     * @return Decoded PCM format, or null if already PCM
     */
    private AudioFormat getDecodedFormat(AudioFormat baseFormat) {
        AudioFormat.Encoding encoding = baseFormat.getEncoding();
        
        // Check if decoding is needed
        if (encoding.equals(AudioFormat.Encoding.PCM_SIGNED) || 
            encoding.equals(AudioFormat.Encoding.PCM_UNSIGNED)) {
            return null; // Already PCM
        }

        // Return decoded format for compressed formats
        return new AudioFormat(
            AudioFormat.Encoding.PCM_SIGNED,
            baseFormat.getSampleRate(),
            16, // Sample size in bits
            baseFormat.getChannels(),
            baseFormat.getChannels() * 2, // Frame size
            baseFormat.getSampleRate(),
            false // Little endian
        );
    }

    /**
     * Stops any currently playing audio.
     */
    public void stopCurrentPlayback() {
        if (currentClip != null && currentClip.isRunning()) {
            logger.info("Stopping current audio playback");
            currentClip.stop();
            currentClip.close();
            currentClip = null;
        }
    }

    /**
     * Checks if audio is currently playing.
     * 
     * @return true if audio is playing, false otherwise
     */
    public boolean isPlaying() {
        return currentClip != null && currentClip.isRunning();
    }
}
