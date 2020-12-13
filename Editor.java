import java.util.ArrayList;
import java.util.List;
import java.awt.*;
import java.awt.event.*;
import java.util.TreeMap;

import javax.swing.*;

/**
 * Client-server graphical editor
 *
 * @author Chris Bailey-Kellogg, Dartmouth CS 10, Fall 2012; loosely based on CS 5 code by Tom Cormen
 * @author CBK, winter 2014, overall structure substantially revised
 * @author Travis Peters, Dartmouth CS 10, Winter 2015; remove EditorCommunicatorStandalone (use echo server for testing)
 * @author CBK, spring 2016 and Fall 2016, restructured Shape and some of the GUI
 */

public class Editor extends JFrame {
    private static String serverIP = "localhost";            // IP address of sketch server
    // "localhost" for your own machine;
    // or ask a friend for their IP address

    private static final int width = 800, height = 800;        // canvas size

    // Current settings on GUI
    public enum Mode {
        DRAW, MOVE, RECOLOR, DELETE
    }

    private Mode mode = Mode.DRAW;                // drawing/moving/recoloring/deleting objects
    private String shapeType = "ellipse";        // type of object to add
    private Color color = Color.black;            // current drawing color

    // Drawing state
    // these are remnants of my implementation; take them as possible suggestions or ignore them
    private Shape curr = null;                    // current shape (if any) being drawn
    private Sketch sketch;                        // holds and handles all the completed objects
    private int movingId = -1;                    // current shape id (if any; else -1) being moved
    private Point drawFrom = new Point();                // where the drawing started
    private Point moveFrom = new Point();                // where object is as it's being dragged
    private Point releasePoint = new Point();
    private Point temp = new Point();


    // Communication
    private EditorCommunicator comm;            // communication with the sketch server

    public Editor() {
        super("Graphical Editor");

        sketch = new Sketch();

        // Connect to server
        comm = new EditorCommunicator(serverIP, this);
        comm.start();

        // Helpers to create the canvas and GUI (buttons, etc.)
        JComponent canvas = setupCanvas();
        JComponent gui = setupGUI();

        // Put the buttons and canvas together into the window
        Container cp = getContentPane();
        cp.setLayout(new BorderLayout());
        cp.add(canvas, BorderLayout.CENTER);
        cp.add(gui, BorderLayout.NORTH);

        // Usual initialization
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        pack();
        setVisible(true);
    }

    /**
     * Creates a component to draw into
     */
    private JComponent setupCanvas() {
        JComponent canvas = new JComponent() {
            public void paintComponent(Graphics g) {
                super.paintComponent(g);
                drawSketch(g);
            }
        };

        canvas.setPreferredSize(new Dimension(width, height));

        canvas.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent event) {
                handlePress(event.getPoint());
            }

            public void mouseReleased(MouseEvent event) {
                handleRelease();
            }
        });

        canvas.addMouseMotionListener(new MouseAdapter() {
            public void mouseDragged(MouseEvent event) {
                handleDrag(event.getPoint());
            }
        });

        return canvas;
    }

    /**
     * Creates a panel with all the buttons
     */
    private JComponent setupGUI() {
        // Select type of shape
        String[] shapes = {"ellipse", "freehand", "rectangle", "segment"};
        JComboBox<String> shapeB = new JComboBox<String>(shapes);
        shapeB.addActionListener(e -> shapeType = (String) ((JComboBox<String>) e.getSource()).getSelectedItem());

        // Select drawing/recoloring color
        // Following Oracle example
        JButton chooseColorB = new JButton("choose color");
        JColorChooser colorChooser = new JColorChooser();
        JLabel colorL = new JLabel();
        colorL.setBackground(Color.black);
        colorL.setOpaque(true);
        colorL.setBorder(BorderFactory.createLineBorder(Color.black));
        colorL.setPreferredSize(new Dimension(25, 25));
        JDialog colorDialog = JColorChooser.createDialog(chooseColorB,
                "Pick a Color",
                true,  //modal
                colorChooser,
                e -> {
                    color = colorChooser.getColor();
                    colorL.setBackground(color);
                },  // OK button
                null); // no CANCEL button handler
        chooseColorB.addActionListener(e -> colorDialog.setVisible(true));

        // Mode: draw, move, recolor, or delete
        JRadioButton drawB = new JRadioButton("draw");
        drawB.addActionListener(e -> mode = Mode.DRAW);
        drawB.setSelected(true);
        JRadioButton moveB = new JRadioButton("move");
        moveB.addActionListener(e -> mode = Mode.MOVE);
        JRadioButton recolorB = new JRadioButton("recolor");
        recolorB.addActionListener(e -> mode = Mode.RECOLOR);
        JRadioButton deleteB = new JRadioButton("delete");
        deleteB.addActionListener(e -> mode = Mode.DELETE);
        ButtonGroup modes = new ButtonGroup(); // make them act as radios -- only one selected
        modes.add(drawB);
        modes.add(moveB);
        modes.add(recolorB);
        modes.add(deleteB);
        JPanel modesP = new JPanel(new GridLayout(1, 0)); // group them on the GUI
        modesP.add(drawB);
        modesP.add(moveB);
        modesP.add(recolorB);
        modesP.add(deleteB);

        // Put all the stuff into a panel
        JComponent gui = new JPanel();
        gui.setLayout(new FlowLayout());
        gui.add(shapeB);
        gui.add(chooseColorB);
        gui.add(colorL);
        gui.add(modesP);
        return gui;
    }

    /**
     * Getter for the sketch instance variable
     */
    public Sketch getSketch() {
        return sketch;
    }

    /**
     * Draws all the shapes in the sketch,
     * along with the object currently being drawn in this editor (not yet part of the sketch)
     */
    public void drawSketch(Graphics g) {
        for (Shape x : sketch.getShapes()) {
            x.draw(g);
        }
        if(curr!= null)
             curr.draw(g);

    }

    // Helpers for event handlers

    /**
     * Helper method for press at point
     * In drawing mode, start a new object;
     * in moving mode, (request to) start dragging if clicked in a shape;
     * in recoloring mode, (request to) change clicked shape's color
     * in deleting mode, (request to) delete clicked shape
     */

    //instantiates the current shape, as well as set the drawFrom location and the moveFrom location. This also resets the releasePoint point,
    //which causes issues with moving shapes if it is not reset
    private void handlePress(Point p) {
        releasePoint = p;
        System.out.println(p);
        System.out.println(mode);
        System.out.println();
        if (mode.equals(Mode.DRAW)) {
            drawFrom.setLocation(p);
            if(shapeType.equals("ellipse")){
                curr = new Ellipse((int)drawFrom.getX(), (int)drawFrom.getY(), (int)releasePoint.getX(), (int)releasePoint.getY(), color);
            }
            else if(shapeType.equals("segment")){
                curr = new Segment((int)drawFrom.getX(), (int)drawFrom.getY(), (int)releasePoint.getX(), (int)releasePoint.getY(), color);
            }
            else if(shapeType.equals("rectangle")){
                curr = new Rectangle((int)drawFrom.getX(), (int)drawFrom.getY(), (int)releasePoint.getX(), (int)releasePoint.getY(), color);
            }
        }

        else if (mode.equals((Mode.MOVE))) {
            moveFrom.setLocation(p);
        }

        else if (mode.equals(Mode.DELETE)) {

        }
    }

    /**
     * Helper method for drag to new point
     * In drawing mode, update the other corner of the object;
     * in moving mode, (request to) drag the object
     */

    //this is largely just to deal with the movement of objects and also showing a shape thats currently being drawn
    private void handleDrag(Point p) {
        temp = new Point((int)releasePoint.getX(),(int) releasePoint.getY());
        releasePoint.setLocation(p);
        if (mode.equals(Mode.MOVE)) {
            String send;
            send = "move " + (int) temp.getX() + " " + (int) temp.getY() + " " + (int) releasePoint.getX() + " " + (int) releasePoint.getY();
            comm.send(send);
        }
        try {
            if (curr != null)
                curr.setCorners((int) drawFrom.getX(), (int) drawFrom.getY(), (int) releasePoint.getX(), (int) releasePoint.getY());
            repaint();
        }
        catch(Exception e){}
    }

//this is the method that unpacks the string representing the sketch that i sent over from the server.
    // I represented the sketch as a string, with components of shapes separated by spaces and shapes separated by semi-colons
    //In addition, i repaint at the end in order to always have the most updated version on the screen
public synchronized void updateSketch(String s){
        String[] temp = s.split(";");
        Sketch now = new Sketch();
        for(int x =0; x<temp.length; x++){
            String[] shape = temp[x].split(" ");
            if(shape.length==6)
                 now.addShape(shape[0], Integer.parseInt(shape[1]), Integer.parseInt(shape[2]), Integer.parseInt(shape[3]), Integer.parseInt(shape[4]), new Color(Integer.parseInt(shape[5])));
        }
        sketch = now;
        repaint();
}

    /**
     * Helper method for release
     * In drawing mode, pass the add new object request on to the server;
     * in moving mode, release it
     */

    //this has a lot of the functionality in it, as i opted for having things to occur on release rather than on press
    //its pretty much all the creation and sending of command strings to the server.
    private void handleRelease() {
        if (mode.equals(Mode.DRAW)) {
            String send = " l";
            if (shapeType.equals("ellipse")) {
                send = "draw ellipse " + (int) drawFrom.getX() + " " + (int) drawFrom.getY() + " " + (int) releasePoint.getX() + " " + (int) releasePoint.getY() + " " + color.getRGB();
            } else if (shapeType.equals("rectangle")) {
                send = "draw rectangle " + (int) drawFrom.getX() + " " + (int) drawFrom.getY() + " " + (int) releasePoint.getX() + " " + (int) releasePoint.getY() + " " + color.getRGB();
            } else if (shapeType.equals("segment")) {
                send = "draw segment " + (int) drawFrom.getX() + " " + (int) drawFrom.getY() + " " + (int) releasePoint.getX() + " " + (int) releasePoint.getY() + " " + color.getRGB();
            }
            comm.send(send);
        } else if (mode.equals(Mode.DELETE)) {
            String send;
            send = "delete " + (int) releasePoint.getX() + " " + (int) releasePoint.getY();
            comm.send(send);
        }
        else if(mode.equals(Mode.RECOLOR)){
            String send ;
            send = "recolor "+ (int) releasePoint.getX() + " " + (int) releasePoint.getY() + " "+ color.getRGB();
            comm.send(send);
        }
        curr = null;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                new Editor();
            }
        });
    }
}
