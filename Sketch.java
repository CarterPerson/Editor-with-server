import java.awt.*;
import java.util.ArrayList;
import java.util.TreeMap;

public class Sketch {
    private  TreeMap<Integer, Shape> shapes;
    private  int shapecount;

    public Sketch() {
        shapes = new TreeMap<>();
        shapecount = 0;
    }
    //adds a shape to the TreeMap. This is designed to be passed the information straight from a String Command
    public synchronized void addShape(String type, int x1, int y1, int x2, int y2, Color color) {
        Shape shape;
        if (type.equals("ellipse")) {
            shape = new Ellipse(x1, y1, x2, y2, color);
            shapes.put(shapecount, shape);
            shapecount++;
        } else if (type.equals("rectangle")) {
            shape = new Rectangle(x1, y1, x2, y2, color);
            shapes.put(shapecount, shape);
            shapecount++;
        } else if (type.equals("segment")) {
            shape = new Segment(x1, y1, x2, y2, color);
            shapes.put(shapecount, shape);
            shapecount++;
        }

    }

    //This is a long method, as its designed to read any string sent from a client and determine what they want to change.
    //as the string is of different lengths for different requests, I needed to repeat myself a little bit at times.
    public  synchronized void inputString(String s) {
        System.out.println(s);
        String[] parts = s.split(" ");
        if (parts[0].equals("move")) {
            for (int x = shapecount-1;  x>=0; x-- ) {
                if(shapes.get(x) != null) {
                    System.out.println(shapecount);
                    if (shapes.get(x).contains(Integer.parseInt(parts[1]), Integer.parseInt(parts[2]))) {
                        shapes.get(x).moveBy(Integer.parseInt(parts[3]) - Integer.parseInt(parts[1]), Integer.parseInt(parts[4]) - Integer.parseInt(parts[2]));
                        Shape temp = shapes.get(x);
                        Shape temp2 = shapes.get(shapecount - 1);
                        shapes.put(shapecount - 1, temp);
                        shapes.put(x, temp2);

                        break;

                    }
                }
            }
        }
        if (parts[0].equals("draw")) {
            Shape theory;
            if (parts[1].equals("rectangle")) {
                theory = new Rectangle(Integer.parseInt(parts[2]), Integer.parseInt(parts[3]), Integer.parseInt(parts[4]), Integer.parseInt(parts[5]), new Color(Integer.parseInt(parts[6])));
            } else if (parts[1].equals("ellipse")) {
                theory = new Ellipse(Integer.parseInt(parts[2]), Integer.parseInt(parts[3]), Integer.parseInt(parts[4]), Integer.parseInt(parts[5]), new Color(Integer.parseInt(parts[6])));
            } else if (parts[1].equals("segment")) {
                theory = new Segment(Integer.parseInt(parts[2]), Integer.parseInt(parts[3]), Integer.parseInt(parts[4]), Integer.parseInt(parts[5]), new Color(Integer.parseInt(parts[6])));
            } else {
                return;
            }
            shapes.put(shapecount, theory);
            shapecount++;
        }
        if (parts[0].equals("delete")) {
            Point p;
            p = new Point(Integer.parseInt(parts[1]), Integer.parseInt(parts[2]));

            ArrayList<Integer> tracker = new ArrayList<>();
            for (int x : shapes.descendingKeySet()) {
                if (shapes.get(x).contains((int) p.getX(), (int) p.getY())) {
                    tracker.add(x);
                }
            }
            for(int x = tracker.size()-1; x>=0; x--){
                shapes.remove(tracker.get(x));

            }
        }
        if (parts[0].equals("recolor")) {
            Color color = new Color(Integer.parseInt(parts[3]));
            for (int x : shapes.descendingKeySet()) {
                if (shapes.get(x).contains(Integer.parseInt(parts[1]), Integer.parseInt(parts[2]))) {
                    shapes.get(x).setColor(color);
                    break;
                }
            }
        }
    }


    public synchronized ArrayList<Shape> getShapes() {
        ArrayList<Shape> returner = new ArrayList<>();
        for (int x : shapes.keySet()) {
            returner.add(shapes.get(x));
        }
        return returner;
    }

    //this models the sketch as a string, as described in editor
    public String toString(){
        String returner="";
        for(int x: shapes.keySet()){
            if(shapes.get(x) != null)
                 returner+=shapes.get(x).toString()+";";
        }
        return returner;
    }


}
