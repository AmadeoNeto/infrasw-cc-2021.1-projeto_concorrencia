import ui.*;

import javax.swing.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.concurrent.locks.Condition;
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
    private boolean isHoldingScrubber = false;
    private int currentTime = 0; // The time that the current song played in seconds
    private int numberSongs = 0;

    ArrayList<String[]> queue = new ArrayList<>();

    Thread windowsUpdater; // Thread used to update the window values "in parallel" with the rest of the program
    Thread timerUpdater;   // Thread used to update the current time "in parallel" with the rest program
    Thread songFinishedChecker;   // Thread used to check if the next music should be played

    long killer = -1;  // Used to kill threads with the id stored in this
    long playedSongs =  -1;

    Lock lock = new ReentrantLock(); // Lock used to evict race conditions in the use of class attributes
    Lock timerLock = new ReentrantLock(); //Lock used to keep the consistence in the time measurement
    Condition songFinishedCondition = timerLock.newCondition();
    Condition scrubberReleasedCondition = timerLock.newCondition();

    Runnable increaseTimer = () ->{
        currentTime = 0;
        long timer = 0;      // Counts how much time the current music played
        long currTimeMilis; // Time from the current check
        long prvTime;       // Time from the last check
        long elapsedTime;   // Time elapsed from the current to the last check
        int musicLength = Integer.parseInt(currentSong[5]);

        long id = playedSongs;
        prvTime = System.currentTimeMillis(); // First check

        while(isActive && killer != id) {
            timerLock.lock(); // lock used to prevent inconsistencies with the time measured
            try {
                while (isHoldingScrubber){
                    try {
                        scrubberReleasedCondition.await();
                        timer = (long) currentTime * 1000;
                        prvTime = System.currentTimeMillis();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                currTimeMilis = System.currentTimeMillis();
                elapsedTime = currTimeMilis - prvTime;

                // If the music is not paused, add the elapsed time to the timer
                if (isPlaying) {
                    timer += elapsedTime;
                    currentTime = (int) timer / 1000; // Update the shown time in seconds
                }
                prvTime = currTimeMilis; // The current time is the previous one for the next iteration

                if (currentTime >= musicLength) {
                    songFinishedCondition.signal();
                    return;
                }
            } finally{
                timerLock.unlock();
            }
        }
    };

    Runnable updateWindow = () -> {
        long id = playedSongs;
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

    Runnable finishChecker = () ->{
        int musicLength = Integer.parseInt(currentSong[5]);
        try {
            timerLock.lock();
            while(currentTime < musicLength) {
                songFinishedCondition.await();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            timerLock.unlock();
        }
        next();
    };

    public Player() {
        SwingUtilities.invokeLater(() -> {
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
        });
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

    public void playNewSong(String[] song){
        // A thread is necessary to run "in parallel" with EDT to prevent it to freeze
        Thread playNewThread = new Thread(() -> {
            try {
                lock.lock(); // Evicts race condition in the above class variables
                isPlaying = true;
                isActive = true;
                window.updatePlayPauseButton(true);
            }finally {
                currentSong = song;
                lock.unlock();
            }

            window.updateMiniplayer(
                    true,
                    true,
                    isRepeat,
                    0,
                    Integer.parseInt(currentSong[5]),
                    Integer.parseInt(currentSong[6]),
                    queue.size());

            window.updatePlayingSongInfo(currentSong[0], currentSong[1], currentSong[2]);
            window.enableScrubberArea();

            // Kill the previous song updater threads
            try {
                lock.lock(); // Evicts inconsistencies with the killer variable
                playedSongs++;
                killer = playedSongs-1;
            } finally {
                lock.unlock();
            }

            // Initialize the threads that will update the info of the current playing song
            windowsUpdater = new Thread(updateWindow);
            timerUpdater = new Thread(increaseTimer);
            songFinishedChecker = new Thread(finishChecker);

            windowsUpdater.start();
            timerUpdater.start();
            songFinishedChecker.start();
        });
        playNewThread.start();
    }

    public void stopPlaying(){
            // A thread is necessary to run "in parallel" with EDT to prevent it to freeze
            Thread stopThread = new Thread(() ->{
                try {
                    lock.lock(); // Lock to evict inconsistencies with class attributes
                    isPlaying = false;
                    isActive = false;
                    window.resetMiniPlayer();
                    currentTime = 0;
                } finally {
                    lock.unlock();
                }
            });
            stopThread.start();
        }

    public void next(){
        int nextSongIndex = queue.indexOf(currentSong) + 1;

        if(nextSongIndex < queue.size()){
            playNewSong(queue.get(nextSongIndex));
        } else{
            stopPlaying();
        }
    }

    public void back(){
        int prvSongIndex = queue.indexOf(currentSong) - 1;

        if(prvSongIndex >= 0){
            playNewSong(queue.get(prvSongIndex));
        }
    }

    // Listeners
    ActionListener addSongOk = new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
            try {
                lock.lock(); // Lock used to evict race condition with the var numberSongs and the queue
                numberSongs++;
                queue.add(addWindow.getSong());
            } catch (InterruptedException ex) {
                ex.printStackTrace();
            }finally {
                lock.unlock();
            }
            window.updateQueueList(getQueueArray());
        }
    };

    ActionListener addSongButton = new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
            SwingUtilities.invokeLater(() -> {
                String songId = Integer.toString(numberSongs);
                addWindow = new AddSongWindow(songId, addSongOk, windowListener);
            });
        }
    };

    ActionListener buttonListenerRemove = new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
            // Due to the remove operation been relatively intense, a thread is necessary to not freeze the EDT
            Thread removerThread = new Thread(() -> {
                try {
                    // Lock used to keep the consistence in the numberSongs decrement and the control variables of the class
                    lock.lock();
                    numberSongs--;
                    int songID = window.getSelectedSongID();

                    // If the current song will be removed, stop playing it
                    if (currentSong != null && currentSong[6].equals("" + songID)) {
                        isPlaying = false;
                        isActive = false;
                        window.resetMiniPlayer();
                        currentTime = 0;
                    }
                    // Seek the music that will be removed in the queue by id and r e

                    for (int i = 0; i < queue.size(); i++) {
                        if (queue.get(i)[6].equals("" + songID))
                            queue.remove(i);
                    }
                } finally {
                    lock.unlock();
                }
                window.updateQueueList(getQueueArray());
            });
            removerThread.start();
        }
    };

    ActionListener playNowListener = new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
            int songID = window.getSelectedSongID();
            String[] selectedSong = getMusicByID(songID);
            playNewSong(selectedSong);
        }
    };

    ActionListener buttonListenerPlayPause = new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
            try {
                lock.lock(); // Lock to evict inconsistencies with the isPlaying and in the pause GUI button
                isPlaying = !isPlaying;
                window.updatePlayPauseButton(isPlaying);
            } finally {
                lock.unlock();
            }
        }
    };

    ActionListener buttonListenerStop = new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
            stopPlaying();
        }
    };

    ActionListener buttonListenerNext = new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
            next();
        }
    };

    ActionListener buttonListenerPrevious = new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
            back();
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
            try {
                timerLock.lock();
                isHoldingScrubber = true;
            } finally {
                timerLock.unlock();
            }
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            try {
                timerLock.lock();
                isHoldingScrubber = false;
                scrubberReleasedCondition.signalAll();
            } finally {
                timerLock.unlock();
            }
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
            try{
                lock.lock();
                System.out.println(window.getScrubberValue());
                currentTime = window.getScrubberValue();
            } finally {
                lock.unlock();
            }
        }

        @Override
        public void mouseMoved(MouseEvent e) {

        }
    };
}