import ui.AddSongWindow;
import ui.PlayerWindow;

import java.awt.event.*;

public class Player {

    WindowListener windowListener;

    public Player() {

        PlayerWindow window = new PlayerWindow(
                playButton,
                placeholderListener,
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
                new String[5][4]
        );

        windowListener = window.getAddSongWindowListener();
    }

    // Listeners
    ActionListener addSongOk = new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
            System.out.println("Musica adionada");
        }
    };

    ActionListener addSongButton = new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
            AddSongWindow addWindow = new AddSongWindow("465465", addSongOk, windowListener);
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
