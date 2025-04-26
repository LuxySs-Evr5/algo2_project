package projetalgo;

public class Stop {
    private final String id;
    private final Coord coord;
    private final String name;

    public Stop(String id, String name, Coord coord) {
        this.id = id;
        this.name = name;
        this.coord = coord;
    }

    public String getId() {
        return id;
    }

    public Coord getCoord() {
        return coord;
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return String.format("Name: %s, id: %s, coord: %s", name, id, coord);
    }

}
