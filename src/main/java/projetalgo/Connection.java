package projetalgo;

public class Connection implements Movement {
    private final String tripId;
    private final Stop pDep; // point de départ
    private final Stop pArr; // point d'arrivée
    private final int tDep;// temps de départ
    private final int tArr;// temps d'arrivée

    public Connection(String tripId, Stop pDep, Stop pArr, int tDep, int tArr) {
        this.tripId = tripId;
        this.pDep = pDep;
        this.pArr = pArr;
        this.tDep = tDep;
        this.tArr = tArr;
    }

    @Override
    public Stop getPDep() {
        return pDep;
    }

    @Override
    public Stop getPArr() {
        return pArr;
    }

    public int getTDep() {
        return tDep;
    }

    public int getTArr() {
        return tArr;
    }

    public String getTripId() {
        return tripId;
    }

    @Override
    public String toString() { 
        return String.format("tripId: %d, (%s -> %s), (%d -> %d)", tripId, pDep, pArr, tDep, tArr);
    }

}
