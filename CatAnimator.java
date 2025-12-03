package com.bhojpurri;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.Timer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles cat sprite animation with multiple states.
 * Provides smooth frame-based animation for the UI mascot.
 */
public class CatAnimator {
    private static final Logger logger = LoggerFactory.getLogger(CatAnimator.class);
    
    // Animation states
    public enum AnimationState {
        IDLE,       // Breathing/waiting animation
        LISTENING,  // Cat covering ears (when recording)
        THINKING,   // Processing animation
        SPEAKING    // Mouth moving (during speech)
    }
    
    // Sprite directory paths
    private static final String SPRITE_BASE_PATH = "catndog assit/png/cat/";
    private static final int TARGET_WIDTH = 400;  // Resize cat to this width
    private static final int TARGET_HEIGHT = 400; // Resize cat to this height
    
    // Frame storage
    private Map<AnimationState, List<ImageIcon>> animations;
    private AnimationState currentState;
    private int currentFrame;
    private Timer animationTimer;
    private JLabel displayLabel;
    
    // Animation speed (milliseconds per frame)
    private static final int FRAME_DELAY = 100; // 10 FPS
    
    public CatAnimator(JLabel displayLabel) {
        this.displayLabel = displayLabel;
        this.animations = new HashMap<>();
        this.currentState = AnimationState.IDLE;
        this.currentFrame = 0;
        
        loadAllAnimations();
        setupAnimationTimer();
    }
    
    /**
     * Load all sprite animations from disk
     */
    private void loadAllAnimations() {
        logger.info("Loading cat sprite animations...");
        
        // Map animation states to sprite folder patterns
        Map<AnimationState, String> spritePatterns = new HashMap<>();
        spritePatterns.put(AnimationState.IDLE, "Idle");
        spritePatterns.put(AnimationState.LISTENING, "Hurt");  // Paw over ear!
        spritePatterns.put(AnimationState.THINKING, "Walk");
        spritePatterns.put(AnimationState.SPEAKING, "Jump");
        
        // Load each animation
        for (Map.Entry<AnimationState, String> entry : spritePatterns.entrySet()) {
            AnimationState state = entry.getKey();
            String pattern = entry.getValue();
            List<ImageIcon> frames = loadSpriteFrames(pattern);
            
            if (!frames.isEmpty()) {
                animations.put(state, frames);
                logger.info("Loaded {} frames for {} animation", frames.size(), state);
            } else {
                logger.warn("No frames loaded for {} animation", state);
            }
        }
        
        // Verify we have at least idle animation
        if (!animations.containsKey(AnimationState.IDLE) || animations.get(AnimationState.IDLE).isEmpty()) {
            logger.error("Failed to load IDLE animation - creating fallback");
            createFallbackAnimation();
        }
    }
    
    /**
     * Load sprite frames matching a pattern (e.g., "Idle", "Hurt")
     */
    private List<ImageIcon> loadSpriteFrames(String pattern) {
        List<ImageIcon> frames = new ArrayList<>();
        
        // Try to load numbered frames (1-10)
        for (int i = 1; i <= 10; i++) {
            String filename = String.format("%s (%d).png", pattern, i);
            String fullPath = SPRITE_BASE_PATH + filename;
            
            try {
                File imageFile = new File(fullPath);
                if (imageFile.exists()) {
                    BufferedImage originalImage = ImageIO.read(imageFile);
                    
                    // Resize image to target size
                    Image scaledImage = originalImage.getScaledInstance(
                        TARGET_WIDTH, TARGET_HEIGHT, Image.SCALE_SMOOTH
                    );
                    
                    ImageIcon icon = new ImageIcon(scaledImage);
                    frames.add(icon);
                } else {
                    // Stop loading if file doesn't exist (assuming sequential numbering)
                    if (i == 1) {
                        logger.warn("First frame not found: {}", fullPath);
                    }
                    break;
                }
            } catch (IOException e) {
                logger.error("Failed to load sprite frame: {}", fullPath, e);
            }
        }
        
        return frames;
    }
    
    /**
     * Create a simple fallback animation if sprite loading fails
     */
    private void createFallbackAnimation() {
        List<ImageIcon> fallback = new ArrayList<>();
        
        // Create simple colored rectangle as fallback
        BufferedImage img = new BufferedImage(TARGET_WIDTH, TARGET_HEIGHT, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = img.createGraphics();
        g2d.setColor(new Color(255, 200, 100));
        g2d.fillOval(20, 20, TARGET_WIDTH - 40, TARGET_HEIGHT - 40);
        g2d.setColor(Color.BLACK);
        g2d.setFont(new Font("SansSerif", Font.BOLD, 40));
        g2d.drawString("🐱", TARGET_WIDTH / 2 - 20, TARGET_HEIGHT / 2 + 15);
        g2d.dispose();
        
        fallback.add(new ImageIcon(img));
        animations.put(AnimationState.IDLE, fallback);
    }
    
    /**
     * Setup timer for frame animation
     */
    private void setupAnimationTimer() {
        animationTimer = new Timer(FRAME_DELAY, e -> updateFrame());
    }
    
    /**
     * Update to next animation frame
     */
    private void updateFrame() {
        List<ImageIcon> currentAnimation = animations.get(currentState);
        
        if (currentAnimation == null || currentAnimation.isEmpty()) {
            // Fallback to idle if current animation missing
            currentAnimation = animations.get(AnimationState.IDLE);
            if (currentAnimation == null || currentAnimation.isEmpty()) {
                return;
            }
        }
        
        // Display current frame
        displayLabel.setIcon(currentAnimation.get(currentFrame));
        
        // Advance to next frame (loop)
        currentFrame = (currentFrame + 1) % currentAnimation.size();
    }
    
    /**
     * Change animation state
     */
    public void setState(AnimationState newState) {
        if (this.currentState != newState) {
            logger.debug("Animation state changed: {} -> {}", currentState, newState);
            this.currentState = newState;
            this.currentFrame = 0; // Reset to first frame of new animation
            updateFrame(); // Immediately show first frame
        }
    }
    
    /**
     * Start animation playback
     */
    public void start() {
        if (!animationTimer.isRunning()) {
            logger.info("Starting cat animation");
            animationTimer.start();
            updateFrame(); // Show first frame immediately
        }
    }
    
    /**
     * Stop animation playback
     */
    public void stop() {
        if (animationTimer.isRunning()) {
            logger.info("Stopping cat animation");
            animationTimer.stop();
        }
    }
    
    /**
     * Check if animations are loaded successfully
     */
    public boolean isLoaded() {
        return !animations.isEmpty() && animations.containsKey(AnimationState.IDLE);
    }
    
    /**
     * Get current animation state
     */
    public AnimationState getCurrentState() {
        return currentState;
    }
    
    /**
     * Set animation speed (frames per second)
     */
    public void setFPS(int fps) {
        int newDelay = 1000 / fps;
        animationTimer.setDelay(newDelay);
        logger.info("Animation speed changed to {} FPS ({} ms delay)", fps, newDelay);
    }
}
