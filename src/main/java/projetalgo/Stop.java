package projetalgo;

import java.util.Objects;

public class Stop {
    private String id;
    private Coord coord;

    public Stop(String id, Coord coord) {
        this.id = id;
        this.coord = coord;
    }

    public String getId() {
        return id;
    }

    public Coord getCoord() {
        return coord;
    }

    @Override
    public String toString() {
        return String.valueOf(id);
    }

}
