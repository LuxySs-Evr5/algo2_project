package projetalgo;

import javafx.util.Pair;

public class Node {

    private final Pair<Double, Double> coords;
    private final String name;
    private final int id;


    public Node(int ID, String Name, Pair<Double, Double> Coords) {
        coords = Coords; 
        name = Name; 
        id = ID; 
    }

    public Pair<Double, Double> getCoords() {
        return coords;
    }
    public String getName() {
        return name;
    }
    public int getId() {
        return id;
    }

}
