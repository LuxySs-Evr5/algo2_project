package projetalgo;

import java.util.Objects;

public class Stop {
    private int id;

    public Stop(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return String.valueOf(id);
    }

}
