package ui.support;

import ui.resources.icons.Icons;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.Objects;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class MiniPlayerPanel extends JPanel {

    private final ImageIcon iconPlay = new ImageIcon(Objects.requireNonNull(Icons.class.getResource("play-24.png")));
    private final ImageIcon iconPause = new ImageIcon(Objects.requireNonNull(Icons.class.getResource("pause-24.png")));
    private final ImageIcon iconStop = new ImageIcon(Objects.requireNonNull(Icons.class.getResource("stop-24.png")));
    private final ImageIcon iconPrevious = new ImageIcon(Objects.requireNonNull(Icons.class.getResource("previous-24.png")));
    private final ImageIcon iconNext = new ImageIcon(Objects.requireNonNull(Icons.class.getResource("next-24.png")));
    private final ImageIcon iconRepeat = new ImageIcon(Objects.requireNonNull(Icons.class.getResource("repeat-24.png")));
    private final ImageIcon iconShuffle = new ImageIcon(Objects.requireNonNull(Icons.class.getResource("shuffle-24.png")));

    private final JButton miniPlayerPlayPauseButton = new JButton(iconPlay);
    private final JButton miniPlayerStopButton = new JButton(iconStop);
    private final JButton miniPlayerPreviousButton = new JButton(iconPrevious);
    private final JButton miniPlayerNextButton = new JButton(iconNext);
    private final JLabel miniPlayerSongInfo = new JLabel();
    private final JLabel miniPlayerCurrentTime = new JLabel("- - : - -");
    private final JSlider miniPlayerScrubber = new JSlider();
    private final JLabel miniPlayerTotalTime = new JLabel("- - : - -");

    // Concurrency Control Vars
    private ReentrantLock lock = new ReentrantLock();
    private Thread update;

    // TODO Sincronizar
    public MiniPlayerPanel(
            ActionListener buttonListenerPlayPause,
            ActionListener buttonListenerStop,
            ActionListener buttonListenerNext,
            ActionListener buttonListenerPrevious,
            ActionListener buttonListenerShuffle,
            ActionListener buttonListenerRepeat,
            MouseListener scrubberListenerClick,
            MouseMotionListener scrubberListenerMotion) {
        JPanel miniPlayerInfoAndScrubber = new JPanel();
        JPanel miniPlayerScrubberPanel = new JPanel();
        JPanel miniPlayerButtons = new JPanel();
        JToggleButton miniPlayerShuffleButton = new JToggleButton(iconShuffle);
        JToggleButton miniPlayerRepeatButton = new JToggleButton(iconRepeat);

        this.setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
        this.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));

        miniPlayerInfoAndScrubber.setLayout(new GridLayout(2, 1, 0, 0));
        GroupLayout groupLayout = new GroupLayout(miniPlayerScrubberPanel);
        miniPlayerScrubberPanel.setLayout(groupLayout);
        miniPlayerScrubber.setMaximum(0);
        miniPlayerScrubber.setEnabled(false);
        groupLayout.setHorizontalGroup(
                groupLayout.createSequentialGroup()
                        .addComponent(miniPlayerCurrentTime)
                        .addComponent(miniPlayerScrubber)
                        .addComponent(miniPlayerTotalTime)
        );
        groupLayout.setVerticalGroup(
                groupLayout.createParallelGroup()
                        .addComponent(miniPlayerCurrentTime)
                        .addComponent(miniPlayerScrubber)
                        .addComponent(miniPlayerTotalTime)
        );
        miniPlayerInfoAndScrubber.add(miniPlayerSongInfo);
        miniPlayerInfoAndScrubber.add(miniPlayerScrubberPanel);

        miniPlayerButtons.setLayout(new BoxLayout(miniPlayerButtons, BoxLayout.X_AXIS));
        miniPlayerButtons.setBorder(BorderFactory.createEmptyBorder(5, 0, 0, 0));
        miniPlayerShuffleButton.setPreferredSize(new Dimension(35, 35));
        miniPlayerPreviousButton.setPreferredSize(new Dimension(35, 35));
        miniPlayerPlayPauseButton.setPreferredSize(new Dimension(35, 35));
        miniPlayerStopButton.setPreferredSize(new Dimension(35, 35));
        miniPlayerNextButton.setPreferredSize(new Dimension(35, 35));
        miniPlayerRepeatButton.setPreferredSize(new Dimension(35, 35));
        miniPlayerShuffleButton.setMaximumSize(new Dimension(35, 35));
        miniPlayerPreviousButton.setMaximumSize(new Dimension(35, 35));
        miniPlayerPlayPauseButton.setMaximumSize(new Dimension(35, 35));
        miniPlayerStopButton.setMaximumSize(new Dimension(35, 35));
        miniPlayerNextButton.setMaximumSize(new Dimension(35, 35));
        miniPlayerRepeatButton.setMaximumSize(new Dimension(35, 35));

        miniPlayerButtons.add(Box.createHorizontalGlue());
        miniPlayerButtons.add(miniPlayerShuffleButton);
        miniPlayerButtons.add(Box.createRigidArea(new Dimension(5, 0)));
        miniPlayerButtons.add(miniPlayerPreviousButton);
        miniPlayerButtons.add(Box.createRigidArea(new Dimension(5, 0)));
        miniPlayerButtons.add(miniPlayerPlayPauseButton);
        miniPlayerButtons.add(Box.createRigidArea(new Dimension(5, 0)));
        miniPlayerButtons.add(miniPlayerStopButton);
        miniPlayerButtons.add(Box.createRigidArea(new Dimension(5, 0)));
        miniPlayerButtons.add(miniPlayerNextButton);
        miniPlayerButtons.add(Box.createRigidArea(new Dimension(5, 0)));
        miniPlayerButtons.add(miniPlayerRepeatButton);
        miniPlayerButtons.add(Box.createRigidArea(new Dimension(5, 0)));
        miniPlayerButtons.add(Box.createHorizontalGlue());

        miniPlayerPreviousButton.setEnabled(false);
        miniPlayerPlayPauseButton.setEnabled(false);
        miniPlayerStopButton.setEnabled(false);
        miniPlayerNextButton.setEnabled(false);

        this.add(miniPlayerInfoAndScrubber);
        this.add(miniPlayerButtons);

        miniPlayerPlayPauseButton.addActionListener(buttonListenerPlayPause);
        miniPlayerStopButton.addActionListener(buttonListenerStop);
        miniPlayerPreviousButton.addActionListener(buttonListenerPrevious);
        miniPlayerNextButton.addActionListener(buttonListenerNext);
        miniPlayerShuffleButton.addActionListener(buttonListenerShuffle);
        miniPlayerRepeatButton.addActionListener(buttonListenerRepeat);
        miniPlayerScrubber.addMouseMotionListener(scrubberListenerMotion);
        miniPlayerScrubber.addMouseListener(scrubberListenerClick);
    }

    // TODO Sincronizar
    public void updatePlayingSongInfo(String songTitle, String songAlbum, String songArtist) {
        miniPlayerSongInfo.setText(songTitle + "     |     " + songAlbum + "     |     " + songArtist);
        miniPlayerSongInfo.repaint();
    }

    // TODO Sincronizar
    public void enableScrubberArea() {
             miniPlayerPreviousButton.setEnabled(true);
             miniPlayerPlayPauseButton.setEnabled(true);
             miniPlayerStopButton.setEnabled(true);
             miniPlayerNextButton.setEnabled(true);
             miniPlayerScrubber.setEnabled(true);
         }

    // TODO Sincronizar
    public void disableScrubberArea() {
        miniPlayerPreviousButton.setEnabled(false);
        miniPlayerPlayPauseButton.setEnabled(false);
        miniPlayerStopButton.setEnabled(false);
        miniPlayerNextButton.setEnabled(false);
        miniPlayerScrubber.setEnabled(false);
    }

    // Sincronizado
    public void updatePlayPauseButton(boolean isPlaying) {

        // Prevent changes in isPlaying while the playPause icon is updating, preventing inconsistencies
        lock.lock();
        try{
            miniPlayerPlayPauseButton.setIcon(isPlaying ? iconPause : iconPlay);
        }finally {
            lock.unlock();
        }
    }

    // Sincronizado
    public void updateMiniplayer(
            boolean isActive,
            boolean isPlaying,
            boolean isRepeat,
            int currentTime,
            int totalTime,
            int songIndex,
            int queueSize) {
        updatePlayPauseButton(isPlaying);

        //Thread used to allow the reset run always after the update change the values of the UI objects
        this.update = new Thread(() -> {
            if (isActive) {
                updatePlayPauseButton(isPlaying);
                miniPlayerCurrentTime.setText(SecondsToString.currentTimeToString(currentTime, totalTime));
                miniPlayerTotalTime.setText(SecondsToString.lengthToString(totalTime));
                miniPlayerScrubber.setMaximum(totalTime);
                miniPlayerScrubber.setValue(currentTime);
                if (!isRepeat) {
                    miniPlayerPreviousButton.setEnabled(songIndex > 0);
                    miniPlayerNextButton.setEnabled(songIndex < queueSize - 1);
                } else {
                    miniPlayerPreviousButton.setEnabled(true);
                    miniPlayerNextButton.setEnabled(true);
                }
            } else {
                resetMiniPlayer();
            }
        });
        this.update.start();
        try {
            this.update.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }

    // Sincronized
    public void resetMiniPlayer() {

        // Ensures that the reset will run after the update, preventing the update to overwrite the reset values
        try {
            this.update.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        miniPlayerCurrentTime.setText("- - : - -");
        miniPlayerTotalTime.setText("- - : - -");
        miniPlayerSongInfo.setText("");
        miniPlayerScrubber.setMaximum(0);
        miniPlayerScrubber.setEnabled(false);
        miniPlayerPlayPauseButton.setIcon(iconPlay);
        disableScrubberArea();
    }

    public int getScrubberValue() {
        return miniPlayerScrubber.getValue();
    }
}
