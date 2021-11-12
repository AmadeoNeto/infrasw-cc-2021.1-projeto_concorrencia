import ui.*;

import java.awt.event.*;
import java.util.ArrayList;
import java.util.Arrays;

public class Player {

    PlayerWindow window;
    WindowListener windowListener;
    AddSongWindow addWindow;
    private int numberSongs = 0;

    ArrayList<String[]> queue = new ArrayList<>();
    //String[][] queue = new String[10][7];

    public Player() {

        window = new PlayerWindow(
                playButton,
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
                //queue
                getQueueArray()
        );

        windowListener = window.getAddSongWindowListener();

    }

    public String[][] getQueueArray() {
        String[][] array = new String[queue.size()][7];
        array = (String[][]) queue.toArray(array);
        return array;
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

    ActionListener playButton = new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
            System.out.println("hello world");
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
