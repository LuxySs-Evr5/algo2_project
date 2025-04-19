package projetalgo;

import javafx.util.Pair;

public class Node {

    private Pair<Double, Double> coords;
    private String name;
    private int id;


    public Node(int id, String name, Pair<Double, Double> coords) {
        coords = coords; 
        name = name; 
        id = id; 
    }

}
