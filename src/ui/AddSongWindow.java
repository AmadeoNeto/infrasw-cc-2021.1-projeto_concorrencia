package ui;

import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.text.MaskFormatter;
import java.awt.*;
import java.awt.event.*;
import java.text.ParseException;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class AddSongWindow extends Thread {

    private boolean titleFirstFocus = false;
    private boolean albumFirstFocus = false;
    private boolean artistFirstFocus = false;
    private boolean yearFirstFocus = false;
    private boolean addAlreadyPressed = false;
    private String[] song;

    ReentrantLock lock = new ReentrantLock();
    Condition songAdded = lock.newCondition();

    /**
     * Window to input information from a song to be added to the queue. The object with that information is returned
     * by the method getSong().
     *
     * @param songID                  Integer unique to that song. Used as an identifier.
     * @param buttonListenerAddSongOK ActionListener for the "OK" button. Should be created in the Player class.
     * @param windowListener          WindowListener for the "OK" button for internal use.
     *                                Should be acquired from PlayerWindow.getAddSongWindowListener().
     */
    public AddSongWindow(String songID, ActionListener buttonListenerAddSongOK, WindowListener windowListener) {

        JFrame window = new JFrame();
        window.addWindowListener(windowListener);
        JPanel mainPanel = new JPanel();
        JPanel songInfoPanel = new JPanel();
        JPanel buttonsPanel = new JPanel();

        JPanel songTitlePanel = new JPanel();
        JPanel songAlbumPanel = new JPanel();
        JPanel songArtistPanel = new JPanel();
        JPanel songYearPanel = new JPanel();
        JPanel songLengthPanel = new JPanel();

        JLabel songTitleLabel = new JLabel("Title:");
        JLabel songAlbumLabel = new JLabel("Album:");
        JLabel songArtistLabel = new JLabel("Artist");
        JLabel songYearLabel = new JLabel("Year:");
        JLabel songLengthLabel = new JLabel("Length:");

        JFormattedTextField songTitleField = new JFormattedTextField(createTitleFormatter());
        JFormattedTextField songAlbumField = new JFormattedTextField(createAlbumFormatter());
        JFormattedTextField songArtistField = new JFormattedTextField(createArtistFormatter());
        JFormattedTextField songYearField = new JFormattedTextField(createYearFormatter());
        JFormattedTextField songLengthField = new JFormattedTextField(createLengthFormatter());

        JButton addButton = new JButton("Add");
        JButton cancelButton = new JButton("Cancel");

        window.setLayout(new BoxLayout(window.getContentPane(), BoxLayout.PAGE_AXIS));
        window.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        window.setTitle("Add song");
        window.setResizable(false);
        window.setSize(300, 350);
        window.setLocationRelativeTo(null);
        window.add(mainPanel);

        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.PAGE_AXIS));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        mainPanel.add(songInfoPanel);
        mainPanel.add(buttonsPanel);

        songInfoPanel.setLayout(new GridLayout(5, 1));
        songInfoPanel.add(songTitlePanel);
        songInfoPanel.add(songAlbumPanel);
        songInfoPanel.add(songArtistPanel);
        songInfoPanel.add(songYearPanel);
        songInfoPanel.add(songLengthPanel);

        songTitlePanel.setLayout(new GridLayout(2, 1));
        songTitlePanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 3, 0));
        songTitlePanel.add(songTitleLabel);
        songTitlePanel.add(songTitleField);

        songAlbumPanel.setLayout(new GridLayout(2, 1));
        songAlbumPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 3, 0));
        songAlbumPanel.add(songAlbumLabel);
        songAlbumPanel.add(songAlbumField);

        songArtistPanel.setLayout(new GridLayout(2, 1));
        songArtistPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 3, 0));
        songArtistPanel.add(songArtistField);
        songArtistPanel.add(songArtistLabel);
        songArtistPanel.add(songArtistField);

        songYearPanel.setLayout(new GridLayout(2, 1));
        songYearPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 3, 0));
        songYearPanel.add(songYearField);
        songYearPanel.add(songYearLabel);
        songYearPanel.add(songYearField);

        songLengthPanel.setLayout(new GridLayout(2, 1));
        songLengthPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        songLengthPanel.add(songLengthField);
        songLengthPanel.add(songLengthLabel);
        songLengthPanel.add(songLengthField);

        buttonsPanel.setLayout(new BoxLayout(buttonsPanel, BoxLayout.LINE_AXIS));
        buttonsPanel.setBorder(BorderFactory.createEmptyBorder(5, 0, 0, 0));
        buttonsPanel.add(Box.createHorizontalGlue());
        buttonsPanel.add(addButton);
        buttonsPanel.add(Box.createRigidArea(new Dimension(5, 0)));
        buttonsPanel.add(cancelButton);

        Timer lengthWarningTimer = new Timer(4000, e -> songLengthLabel.setText("Length:"));
        lengthWarningTimer.setRepeats(false);

        songTitleField.addFocusListener(new FocusListener() {
            @Override
            public void focusGained(FocusEvent e) {
                if (!titleFirstFocus) songTitleField.setText("");
                titleFirstFocus = true;
            }

            @Override
            public void focusLost(FocusEvent e) {
            }
        });
        songAlbumField.addFocusListener(new FocusListener() {
            @Override
            public void focusGained(FocusEvent e) {
                if (!albumFirstFocus) songAlbumField.setText("");
                albumFirstFocus = true;
            }

            @Override
            public void focusLost(FocusEvent e) {
            }
        });
        songArtistField.addFocusListener(new FocusListener() {
            @Override
            public void focusGained(FocusEvent e) {
                if (!artistFirstFocus) songArtistField.setText("");
                artistFirstFocus = true;
            }

            @Override
            public void focusLost(FocusEvent e) {
            }
        });
        songYearField.addFocusListener(new FocusListener() {
            @Override
            public void focusGained(FocusEvent e) {
                if (!yearFirstFocus) songYearField.setText("");
                yearFirstFocus = true;
            }

            @Override
            public void focusLost(FocusEvent e) {
            }
        });

        addButton.addActionListener(e -> {
            if (!addAlreadyPressed) {
                Object songTitleObject = songTitleField.getValue();
                Object songAlbumObject = songAlbumField.getValue();
                Object songArtistObject = songArtistField.getValue();
                Object songYearObject = songYearField.getValue();
                Object songLengthObject = songLengthField.getValue();

                String songTitle = "Untitled";
                String songAlbum = "Untitled";
                String songArtist = "Unknown";
                String songYear = "Unknown";
                String songLengthSeconds;
                String songLength;

                if (songTitleObject != null) {
                    if (!songTitleObject.toString().strip().isEmpty()) {
                        songTitle = songTitleObject.toString().strip();
                    }
                }
                if (songAlbumObject != null) {
                    if (!songAlbumObject.toString().strip().isEmpty()) {
                        songAlbum = songAlbumObject.toString().strip();
                    }
                }
                if (songArtistObject != null) {
                    if (!songArtistObject.toString().strip().isEmpty()) {
                        songArtist = songArtistObject.toString().strip();
                    }
                }
                if (songYearObject != null) {
                    if (!songYearObject.toString().isEmpty()) {
                        String string = songYearObject.toString().strip();
                        if (!string.isEmpty())
                            songYear = String.valueOf(Integer.parseInt(string));
                    }
                }
                if (songLengthObject != null) {
                    String string = songLengthObject.toString();
                    if (!string.equals("00:00:00")) {
                        String hoursString = string.substring(0, 2);
                        String minutesString = string.substring(3, 5);
                        String secondsString = string.substring(6, 8);
                        int hours = Integer.parseInt(hoursString);
                        int minutes = Integer.parseInt(minutesString);
                        int seconds = Integer.parseInt(secondsString);
                        if (minutes < 60 && seconds < 60) {
                            addAlreadyPressed = true;
                            songLength = string;
                            songLengthSeconds = String.valueOf(((hours * 60 * 60) + (minutes * 60) + seconds));

                            song = new String[]{
                                    songTitle,
                                    songAlbum,
                                    songArtist,
                                    songYear,
                                    songLength,
                                    songLengthSeconds,
                                    songID};

                            lock.lock();
                            songAdded.signalAll();
                            lock.unlock();

                            buttonListenerAddSongOK.actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, null));
                            window.dispatchEvent(new WindowEvent(window, WindowEvent.WINDOW_CLOSING));
                        } else {
                            if (lengthWarningTimer.isRunning()) lengthWarningTimer.stop();
                            songLengthLabel.setText("Minutes or seconds can't be greater than 59.");
                            songLengthField.putClientProperty("JComponent.outline", "error");
                            songLengthField.revalidate();
                            songLengthField.repaint();
                            songLengthField.requestFocus();
                            lengthWarningTimer.start();
                        }
                    } else {
                        if (lengthWarningTimer.isRunning()) lengthWarningTimer.stop();
                        songLengthLabel.setText("Length can't be zero.");
                        songLengthField.putClientProperty("JComponent.outline", "error");
                        songLengthField.revalidate();
                        songLengthField.repaint();
                        songLengthField.requestFocus();
                        lengthWarningTimer.start();
                    }
                } else {
                    if (lengthWarningTimer.isRunning()) lengthWarningTimer.stop();
                    songLengthLabel.setText("Length can't be zero.");
                    songLengthField.putClientProperty("JComponent.outline", "error");
                    songLengthField.revalidate();
                    songLengthField.repaint();
                    songLengthField.requestFocus();
                    lengthWarningTimer.start();
                }

            }
        });
        cancelButton.addActionListener(e -> window.dispatchEvent(new WindowEvent(window, WindowEvent.WINDOW_CLOSING)));

        window.setVisible(true);
    }

    // TODO Sincronizar

    /**
     * @return String array with the following information:<br>
     * [0] Song Title<br>
     * [1] Song Album<br>
     * [2] Song Artist<br>
     * [3] Song Year<br>
     * [4] Song Length in the format 00:00<br>
     * [5] Song Length in seconds<br>
     * [6] Song ID
     */
    public String[] getSong() throws InterruptedException {
        String[] newSong = new String[song.length];

        while (song.length == 0){
            songAdded.await();
        }
        lock.lock();
        System.arraycopy(song, 0, newSong, 0, song.length);
        lock.unlock();
        return newSong;
    }

    private @Nullable
    MaskFormatter createTitleFormatter() {
        try {
            MaskFormatter formatter = new MaskFormatter("********************");
            formatter.setPlaceholder("Untitled");
            formatter.setPlaceholderCharacter(' ');
            return formatter;
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return null;
    }

    private @Nullable
    MaskFormatter createAlbumFormatter() {
        try {
            MaskFormatter formatter = new MaskFormatter("********************");
            formatter.setPlaceholder("Untitled");
            formatter.setPlaceholderCharacter(' ');
            return formatter;
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return null;
    }

    private @Nullable
    MaskFormatter createArtistFormatter() {
        try {
            MaskFormatter formatter = new MaskFormatter("********************");
            formatter.setPlaceholder("Unknown");
            formatter.setPlaceholderCharacter(' ');
            return formatter;
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return null;
    }

    private @Nullable
    MaskFormatter createYearFormatter() {
        try {
            MaskFormatter formatter = new MaskFormatter("****");
            formatter.setPlaceholder("2021");
            formatter.setPlaceholderCharacter(' ');
            formatter.setValidCharacters("0123456789 ");
            return formatter;
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return null;
    }

    private @Nullable
    MaskFormatter createLengthFormatter() {
        try {
            MaskFormatter formatter = new MaskFormatter("##:##:##");
            formatter.setPlaceholderCharacter('0');
            return formatter;
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return null;
    }
}
