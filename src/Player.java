import ui.*;

import java.awt.event.*;
import java.util.ArrayList;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class Player {

    private PlayerWindow window;
    private WindowListener windowListener;
    private AddSongWindow addWindow;

    private String[] currentSong = null;
    private Boolean isPlaying = false;
    private boolean isActive = false;
    private boolean isRepeat = false;
    private int currentTime = 0;
    private int numberSongs = 0;

    ArrayList<String[]> queue = new ArrayList<>();

    Thread windowsUpdater;
    Thread timerUpdater;

    Lock timerLock = new ReentrantLock();

    Runnable increaseTimer = () ->{
        long currTime;    // Time from the current check
        long prvTime;     // Time from the last check
        long elapsedTime; // Time elapsed from the current to the last check
        long timer;       // Counts how much time the music played

        while(true) {
            currentTime = 0;
            timer = 0;

            prvTime = System.currentTimeMillis(); // First check

            while(isActive) {
                timerLock.lock(); // Prevents inconsistencies with the time measured
                currTime = System.currentTimeMillis();
                elapsedTime = currTime - prvTime;

                // If the music is not paused, add the elapsed time to the timer
                if(isPlaying) {
                    timer += elapsedTime;
                    currentTime = (int) timer / 1000; // Update the shown time in seconds
                }

                prvTime = currTime; // The current time is the previous one for the next iteration
                timerLock.unlock();
            }
        }
    };

    Runnable updateWindow = () -> {
        int songID = Integer.parseInt(currentSong[6]);
        int totalTime = Integer.parseInt(currentSong[5]);

        while(currentTime <= totalTime) {
            window.updateMiniplayer(
                    isActive,
                    isPlaying,
                    isRepeat,
                    currentTime,
                    totalTime,
                    songID,
                    queue.size()
            );
        }
    };

    public Player() {

        window = new PlayerWindow(
                playNowListener,
                buttonListenerRemove,
                addSongButton,
                placeholderListener,
                placeholderListener,
                placeholderListener,
                placeholderListener,
                placeholderListener,
                placeholderListener,
                mouseListener,
                motionListener,
                "CIn Media Player",
                getQueueArray()
        );

        windowListener = window.getAddSongWindowListener();
    }

    /**
     * @return the queue as a array of songs (String[])
     */
    public String[][] getQueueArray() {
        String[][] array = new String[queue.size()][7];
        array = (String[][]) queue.toArray(array);
        return array;
    }

    public String[] getMusicByID(int songID){

        for (String[] song : queue) {
            if (song[6].equals("" + songID)) {
                return song;
            }
        }
        return null;
    }

    // Listeners
    ActionListener addSongOk = new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
            numberSongs++;
            queue.add(addWindow.getSong());
            window.updateQueueList(getQueueArray());
            System.out.println("Nova musica Id: " + Integer.toString(numberSongs));
        }
    };

    ActionListener addSongButton = new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
            String songId = Integer.toString(numberSongs);
            addWindow = new AddSongWindow(songId, addSongOk, windowListener);
        }
    };

    ActionListener buttonListenerRemove = new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
            numberSongs--;
            int songID = window.getSelectedSongID();
            System.out.println(songID);
            for (int i = 0; i < queue.size(); i++){
                 if(queue.get(i)[6].equals(""+songID))
                     queue.remove(i);
            }

            window.updateQueueList(getQueueArray());
        }
    };


    ActionListener placeholderListener = new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {

        }
    };

    ActionListener playNowListener = new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
            isPlaying = true;
            isActive = true;
            window.updatePlayPauseButton(true);

            int songID = window.getSelectedSongID();
            currentSong = getMusicByID(songID);
            currentTime = 0;

            window.updateMiniplayer(
                    true,
                    true,
                    isRepeat,
                    currentTime,
                    Integer.parseInt(currentSong[5]),
                    songID,
                    queue.size());

            window.enableScrubberArea();

            windowsUpdater = new Thread(updateWindow);
            timerUpdater = new Thread(increaseTimer);

            //timerBarrier = new CyclicBarrier(1);
            windowsUpdater.start();
            timerUpdater.start();
        }
    };

    MouseListener mouseListener = new MouseListener() {
        @Override
        public void mouseClicked(MouseEvent e) {

        }

        @Override
        public void mousePressed(MouseEvent e) {

        }

        @Override
        public void mouseReleased(MouseEvent e) {

        }

        @Override
        public void mouseEntered(MouseEvent e) {

        }

        @Override
        public void mouseExited(MouseEvent e) {

        }
    };

    MouseMotionListener motionListener = new MouseMotionListener() {
        @Override
        public void mouseDragged(MouseEvent e) {

        }

        @Override
        public void mouseMoved(MouseEvent e) {

        }
    };
}
