package projetalgo;

public class Node {

    private Coord coord;
    private String name;
    private int id;

    public Node(int id, String name, Coord coord) {
        this.coord = coord ;
        this.name = name;
        this.id = id;
    }

    public int getId() {
        return this.id;
    }

    public String getName() {
        return this.name;
    }

    public Coord getCoord() {
        return this.coord;
    }

}
