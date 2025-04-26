package projetalgo;

public class BestKnownEntry {
    private int tArr;
    private Movement movement; // the connection/footpath that made us arrive at the stop corresponding to this entry

    public BestKnownEntry(int tArr, Movement movement) {
        this.tArr = tArr;
        this.movement = movement;
    }

    public int getTArr() {
        return tArr;
    }

    public void setTArr(int tArr) {
        this.tArr = tArr;
    }

    public Movement getMovement() {
        return movement;
    }

    public void setMovement(Movement movement) {
        this.movement = movement;
    }

}
