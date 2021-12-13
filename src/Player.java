import ui.*;

import javax.swing.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.Random;

public class Player {

    private PlayerWindow window;
    private WindowListener windowListener;
    private AddSongWindow addWindow;

    private String[] currentSong = null;
    private Boolean isPlaying = false;
    private boolean isActive = false;
    private boolean isRepeat = false;
    private boolean isShuffling = false;
    private boolean isHoldingScrubber = false;
    private int currentTime = 0; // The time that the current song played in seconds
    private int numberSongs = 0;

    ArrayList<String[]> queue = new ArrayList<>();
    ArrayList<String[]> bkp_queue = new ArrayList<>();

    Map<Integer,Integer> shuffledIndex;

    Thread windowsUpdater;       // Thread used to update the window values "in parallel" with the rest of the program
    Thread timerUpdater;         // Thread used to update the current time "in parallel" with the rest program
    Thread songFinishedChecker;  // Thread used to check if the next music should be played

    long killer = -1;       // Used to kill threads with the id stored in this
    long playedSongs = -1;  // Counts how many songs were played. Is used to kill threads

    Lock lock = new ReentrantLock(); // Lock used to evict race conditions in the use of class attributes
    Condition songFinishedCondition = lock.newCondition();
    Condition scrubberReleasedCondition = lock.newCondition();

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
            lock.lock(); // lock used to prevent inconsistencies with the time measured
            try {
                // If it is holding the scrubber, wait until it is released
                while (isHoldingScrubber){
                    try {
                        scrubberReleasedCondition.await();
                        timer = (long) currentTime * 1000;    // Updates the timer using the scrubber time
                        prvTime = System.currentTimeMillis(); // First time checking after the scrubber was released
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

                // If the music finished, call the finish checker and kill this thread
                if (currentTime >= musicLength) {
                    songFinishedCondition.signalAll(); // Condition used to indicate that the song ended
                    return;
                }
            } finally{
                lock.unlock();
            }
        }
    };

    Runnable updateWindow = () -> {
        long id = playedSongs;
        int songID = Integer.parseInt(currentSong[6]);
        int totalTime = Integer.parseInt(currentSong[5]);

        // Local variables that will receive values from the critical region
        boolean _isActive = isActive;
        int   _currentTime;
        boolean _isPlaying;
        boolean _isRepeat;

        while(_isActive && killer != id) {
            try {
                lock.lock(); // Lock used to read values from the critical region
                _currentTime = currentTime;
                _isActive = isActive;
                _isPlaying = isPlaying;
                _isRepeat = isRepeat;
            } finally {
                lock.unlock();
            }
            if(!_isActive){
                window.resetMiniPlayer();
                break;
            }

            window.updateMiniplayer(
                    _isActive,
                    _isPlaying,
                    _isRepeat,
                    _currentTime,
                    totalTime,
                    songID,
                    queue.size()
            );
        }
    };

    Runnable finishChecker = () ->{
        int musicLength = Integer.parseInt(currentSong[5]);
        long id = playedSongs;
        String[] _currentSong = null;

        try {
            lock.lock();
            // Await a song finish and then try to play the next one.
            while(currentTime < musicLength) {
                songFinishedCondition.await();
            }
            _currentSong = currentSong;
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            lock.unlock();
        }

        // Prevents a previous execution from this thread mess with the system
        if(killer < id) {
            next(); // Play next song
        }
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
                lock.lock(); // Evicts inconsistencies with class variables
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
        try {
            lock.lock(); // Used to get the right value of currentSong and of the queue
            int nextSongIndex = queue.indexOf(currentSong) + 1;

            if (nextSongIndex < queue.size()) {
                playNewSong(queue.get(nextSongIndex));
            } else {
                if(isRepeat ){
                    playNewSong(queue.get(0));
                } else{
                    stopPlaying();
                }
            }
        } finally {
            lock.unlock();
        }
    }

    public void back(){
        int prvSongIndex;
        int last_song_index;
        boolean _isRepeat;

        lock.lock();
        try {
            prvSongIndex = queue.indexOf(currentSong) - 1;
            last_song_index = queue.size() - 1;
            _isRepeat = isRepeat;
        } finally {
            lock.unlock();
        }

        if(prvSongIndex >= 0){
            playNewSong(queue.get(prvSongIndex));
        } else if (_isRepeat) {
            playNewSong(queue.get(last_song_index));
        }
    }

    // Listeners
    ActionListener addSongOk = new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
            try {
                lock.lock(); // Lock used to evict race condition with the var numberSongs and the queue
                numberSongs++;
                String[] song = addWindow.getSong();
                queue.add(song);
                bkp_queue.add(song);
                if(isShuffling){
                    shuffledIndex.put(queue.size()-1,queue.size()-1);
                }
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

                    int removedIndex = -1;
                    for (int i = 0; i < queue.size(); i++) {
                        if (queue.get(i)[6].equals("" + songID))
                            queue.remove(i);
                            removedIndex = i;
                    }

                    if (isShuffling){
                        int bkpRemoveIndex = shuffledIndex.get(removedIndex);
                        for (int i = 0; i < bkp_queue.size(); i++) {
                            if(bkp_queue.get(i)[6].equals(bkpRemoveIndex+"")){
                                bkp_queue.remove(i);
                            }
                        }
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
            Thread shuffleThread = new Thread(() -> {
                lock.lock();
                try {
                    isShuffling = !isShuffling;
                    shuffledIndex = new HashMap<Integer,Integer>();
                } finally {
                    lock.unlock();
                }

                if (isShuffling){
                    Random random = new Random();
                    ArrayList<String[]> shufledQueue = new ArrayList<String[]>();
                    ArrayList<Integer> indexes = new ArrayList<>();

                    for (int i = 0; i < queue.size(); i++) {
                        indexes.add(i);
                    }

                    lock.lock();
                    try{
                        int forLoops = queue.size();
                        if(currentSong != null){
                            String[] firstElement = currentSong.clone();
                            firstElement[6] = "0";
                            shuffledIndex.put(0,Integer.parseInt(currentSong[0]));
                            shufledQueue.add(firstElement);
                            indexes.remove((Integer) Integer.parseInt(currentSong[6]));
                            forLoops -= 1;
                        }

                        for (int i = 0; i < forLoops; i++) {
                            int randomNum = random.nextInt(indexes.size());
                            int randomIndex = indexes.get(randomNum);
                            indexes.remove((Integer) randomIndex);

                            String[] randomSong = queue.get(randomIndex).clone();
                            int newId = shufledQueue.size();
                            shuffledIndex.put(newId,Integer.parseInt(randomSong[6]));
                            randomSong[6] = newId +"";
                            shufledQueue.add(randomSong);
                        }
                        bkp_queue = (ArrayList<String[]>) queue.clone();
                        queue = shufledQueue;
                    } finally {
                        lock.unlock();
                    }
                } else {
                    lock.lock();
                    try {
                        queue = (ArrayList<String[]>) bkp_queue.clone();
                    } finally {
                        lock.unlock();
                    }
                }

                window.updateQueueList(getQueueArray());
            });
            shuffleThread.start();
        }
    };

    ActionListener buttonListenerRepeat = new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
            lock.lock();
            try {
                isRepeat = !isRepeat;
            } finally {
                lock.unlock();
            }
        }
    };

    MouseListener scrubberListenerClick = new MouseListener() {
        @Override
        public void mouseClicked(MouseEvent e) {
        }

        @Override
        public void mousePressed(MouseEvent e) {
            // Run if the user does a simple click on the scrubber
            int scrubberValue =  window.getScrubberValue();
            try {
                lock.lock(); // Used to set the above variables without concur with the timer update
                isHoldingScrubber = true;
                currentTime = scrubberValue;
            } finally {
                lock.unlock();
            }
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            try {
                lock.lock(); // Used to set the value of isHoldingScrubber correctly
                isHoldingScrubber = false;
                scrubberReleasedCondition.signalAll(); // Signal threads that are waiting for this condition
            } finally {
                lock.unlock();
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
                lock.lock(); // Evict race condition with the timer thread
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