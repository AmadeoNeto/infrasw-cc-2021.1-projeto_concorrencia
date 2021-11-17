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
    private int currentTime = 0; // The time that the current song played
    private int numberSongs = 0;

    ArrayList<String[]> queue = new ArrayList<>();

    Thread windowsUpdater; // Thread used to update the window values "in parallel" with the rest of the program
    Thread timerUpdater;   // Thread used to update the current time "in parallel" with the rest program

    int playNowTimes = 0; // How many times the Play Now button was pressed
    long killer = -1;  // Used to kill threads with the id stored in this

    Lock timerLock = new ReentrantLock(); // Lock used to keep the consistence during the time measurement

    Runnable increaseTimer = () ->{
        currentTime = 0;
        long timer = 0;   // Counts how much time the current music played
        long currTime;    // Time from the current check
        long prvTime;     // Time from the last check
        long elapsedTime; // Time elapsed from the current to the last check

        int id = playNowTimes;
        prvTime = System.currentTimeMillis(); // First check

        while(isActive && killer != id) {
            try {
                timerLock.lock(); // lock used to prevent inconsistencies with the time measured
                currTime = System.currentTimeMillis();
                elapsedTime = currTime - prvTime;

                // If the music is not paused, add the elapsed time to the timer
                if (isPlaying) {
                    timer += elapsedTime;
                    currentTime = (int) timer / 1000; // Update the shown time in seconds
                }

                prvTime = currTime; // The current time is the previous one for the next iteration
            } finally{
                timerLock.unlock();
            }
        }
    };

    Runnable updateWindow = () -> {
        long id = playNowTimes;
        int songID = Integer.parseInt(currentSong[6]);
        int totalTime = Integer.parseInt(currentSong[5]);

        while(isActive && killer != id) {
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
                buttonListenerPlayPause,
                buttonListenerStop,
                buttonListenerNext,
                buttonListenerPrevious,
                buttonListenerShuffle,
                buttonListenerRepeat,
                scrubberListenerClick,
                scrubberListenerMotion,
                "CIn Media Player",
                getQueueArray()
        );
        windowListener = window.getAddSongWindowListener();
        numberSongs = queue.size();
    }

    /**
     * @return the queue as an array of songs (String[])
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
            try {
                queue.add(addWindow.getSong());
            } catch (InterruptedException ex) {
                ex.printStackTrace();
            }
            window.updateQueueList(getQueueArray());
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

            // If the current song will be removed, stop playing it
            if(currentSong != null && currentSong[6].equals(""+songID)){
                isPlaying = false;
                isActive = false;
                window.resetMiniPlayer();
                currentTime = 0;
            }
            // Seek the music that will be removed in the queue by id and remove it
            for (int i = 0; i < queue.size(); i++){
                if(queue.get(i)[6].equals(""+songID))
                    queue.remove(i);
            }

            window.updateQueueList(getQueueArray());
        }
    };

    ActionListener playNowListener = new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
            // A thread is necessary to run "in parallel" with EDT to prevent it to freeze
            Thread playNowThread = new Thread(() -> {
                playNowTimes++;
                isPlaying = true;
                isActive = true;
                window.updatePlayPauseButton(true);

                int songID = window.getSelectedSongID();
                currentSong = getMusicByID(songID);

                window.updateMiniplayer(
                        true,
                        true,
                        isRepeat,
                        0,
                        Integer.parseInt(currentSong[5]),
                        songID,
                        queue.size());

                window.updatePlayingSongInfo(currentSong[0], currentSong[1], currentSong[2]);

                window.enableScrubberArea();

                // Kill the previous song updater threads
                killer = playNowTimes-1;

                // Initialize the threads that will update the info of the current playing song
                windowsUpdater = new Thread(updateWindow);
                timerUpdater = new Thread(increaseTimer);

                windowsUpdater.start();
                timerUpdater.start();
            });
            playNowThread.start();

            // Makes the curr thread wait this one to end to prevent scheduling problems
            try {
                playNowThread.join();
            } catch (InterruptedException ex) {
                ex.printStackTrace();
            }
        }
    };

    ActionListener buttonListenerPlayPause = new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
            isPlaying = !isPlaying;
            window.updatePlayPauseButton(isPlaying);
        }
    };

    ActionListener buttonListenerStop = new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
            // A thread is necessary to run "in parallel" with EDT to prevent it to freeze
            Thread stopThread = new Thread(() ->{
                isPlaying = false;
                isActive = false;
                window.resetMiniPlayer();
                currentTime = 0;
            });
            stopThread.start();
        };
    };

    ActionListener buttonListenerNext = new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {

        }
    };

    ActionListener buttonListenerPrevious = new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {

        }
    };

    ActionListener buttonListenerShuffle = new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {

        }
    };

    ActionListener buttonListenerRepeat = new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {

        }
    };

    MouseListener scrubberListenerClick = new MouseListener() {
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

    MouseMotionListener scrubberListenerMotion = new MouseMotionListener() {
        @Override
        public void mouseDragged(MouseEvent e) {

        }

        @Override
        public void mouseMoved(MouseEvent e) {

        }
    };
}