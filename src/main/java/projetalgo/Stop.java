package projetalgo;

import java.util.Objects;

public class Stop {
    private String id;

    public Stop(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    @Override
    public String toString() {
        return String.valueOf(id);
    }

}
