import java.awt.*;
import java.io.*;
import javax.swing.*;

import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.VBox;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.geometry.Pos;
import javafx.util.Duration;

/**
 * AURA SIMULATOR  Video Splash Screen
 * Extracts the bundled splash_video.mp4 from inside the JAR to a temp file,
 * plays it via JavaFX MediaPlayer, then launches the main Assembler window.
 */
public class SplashScreen extends JWindow {

    private volatile boolean launched = false;
    private File tempVideoFile = null;

    //  Entry point 
    public static void main(String[] args) {
        try {
            com.formdev.flatlaf.FlatDarkLaf.setup();
        } catch (Exception ex) {
            System.err.println("FlatDarkLaf init failed: " + ex);
        }

        // Force JavaFX toolkit init before any JFXPanel is shown
        new JFXPanel();

        SwingUtilities.invokeLater(SplashScreen::new);
    }

    //  Constructor 
    public SplashScreen() {
        showVideoSplash();
    }

    //  Main splash logic 
    private void showVideoSplash() {
        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
        int w = (int) (screen.width  * 0.75);
        int h = (int) (screen.height * 0.75);
        setSize(w, h);
        setLocation((screen.width - w) / 2, (screen.height - h) / 2);

        JPanel bg = new JPanel(new BorderLayout());
        bg.setBackground(java.awt.Color.BLACK);
        setContentPane(bg);

        // Step 1  extract the video from inside the JAR to a temp file
        tempVideoFile = extractVideoFromJar();

        if (tempVideoFile != null && tempVideoFile.exists()) {
            // Step 2  show the JavaFX video panel
            JFXPanel fxPanel = new JFXPanel();
            fxPanel.setBackground(java.awt.Color.BLACK);
            bg.add(fxPanel, BorderLayout.CENTER);
            setVisible(true);

            final File videoFile = tempVideoFile;
            Platform.runLater(() -> {
                try {
                    Media media = new Media(videoFile.toURI().toString());
                    MediaPlayer player = new MediaPlayer(media);
                    player.setAutoPlay(true);

                    MediaView mediaView = new MediaView(player);
                    mediaView.setPreserveRatio(true);

                    VBox overlay = buildOverlay();
                    overlay.setMouseTransparent(true);

                    StackPane root = new StackPane(mediaView, overlay);
                    root.setStyle("-fx-background-color: black;");

                    // Stretch video to fill window
                    mediaView.fitWidthProperty().bind(root.widthProperty());
                    mediaView.fitHeightProperty().bind(root.heightProperty());

                    Scene scene = new Scene(root, w, h, Color.BLACK);
                    fxPanel.setScene(scene);

                    // Auto-advance when video ends or encounters error
                    player.setOnEndOfMedia(() -> launchMainApp(player));
                    player.setOnError(() -> launchMainApp(player));

                    // Safety cap at 3.5 seconds max for video splash
                    player.currentTimeProperty().addListener((obs, oldT, newT) -> {
                        if (newT.greaterThanOrEqualTo(Duration.seconds(3.5))) {
                            launchMainApp(player);
                        }
                    });

                    // Click or key press to skip immediately
                    scene.setOnMouseClicked(e -> launchMainApp(player));
                    scene.setOnKeyPressed(e  -> launchMainApp(player));

                    // Fail-safe Swing Timer: launch main app after 4 seconds guaranteed
                    Timer failSafeTimer = new Timer(4000, e -> launchMainApp(player));
                    failSafeTimer.setRepeats(false);
                    failSafeTimer.start();

                } catch (Exception ex) {
                    System.err.println("Video playback failed: " + ex.getMessage());
                    launchMainApp(null);
                }
            });

        } else {
            // No video resource found - fallback text splash
            System.err.println("splash_video.mp4 not found in JAR resources.");
            launchMainApp(null);
        }
    }

    //  Extract video from JAR to a temp file 
    private File extractVideoFromJar() {
        try (InputStream in = SplashScreen.class.getResourceAsStream("/splash_video.mp4")) {
            if (in == null) {
                System.err.println("Resource /splash_video.mp4 not found inside JAR.");
                return null;
            }
            File tmp = File.createTempFile("aura_splash_", ".mp4");
            tmp.deleteOnExit(); // auto-clean when JVM exits

            try (FileOutputStream out = new FileOutputStream(tmp)) {
                byte[] buf = new byte[65536];
                int read;
                while ((read = in.read(buf)) != -1) {
                    out.write(buf, 0, read);
                }
            }
            System.out.println("Video extracted to: " + tmp.getAbsolutePath());
            return tmp;
        } catch (Exception ex) {
            System.err.println("Failed to extract video: " + ex.getMessage());
            return null;
        }
    }

    //  No overlay  video plays clean 
    private VBox buildOverlay() {
        VBox box = new VBox();
        box.setMouseTransparent(true);
        return box; // empty  nothing on top of the video
    }

    //  Fallback: pure Swing text splash (no video) 
    private void showFallbackSplash() {
        JPanel p = (JPanel) getContentPane();
        p.removeAll();
        p.setBackground(java.awt.Color.BLACK);
        JLabel label = new JLabel("AURA SIMULATOR", JLabel.CENTER);
        label.setForeground(new java.awt.Color(255, 130, 0));
        label.setFont(new java.awt.Font("Tahoma", java.awt.Font.BOLD, 28));
        JProgressBar bar = new JProgressBar();
        bar.setIndeterminate(true);
        bar.setForeground(new java.awt.Color(255, 130, 0));
        bar.setBackground(java.awt.Color.DARK_GRAY);
        p.add(label, BorderLayout.CENTER);
        p.add(bar,   BorderLayout.SOUTH);
        p.revalidate();
        p.repaint();
        setVisible(true);
    }

    //  Transition: stop video  delete temp  open main window 
    private void launchMainApp(MediaPlayer player) {
        if (launched) return;
        launched = true;

        if (player != null) {
            Platform.runLater(player::stop);
        }

        // Delete temp video file now (don't wait for JVM exit)
        if (tempVideoFile != null && tempVideoFile.exists()) {
            tempVideoFile.delete();
        }

        SwingUtilities.invokeLater(() -> {
            setVisible(false);
            dispose();
            Assembler.main(new String[]{});
        });
    }
}
