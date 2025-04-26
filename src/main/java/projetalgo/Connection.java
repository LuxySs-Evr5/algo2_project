package projetalgo;

public class Connection implements Movement {
    private final int id;
    private final Stop pDep; // point de départ
    private final Stop pArr; // point d'arrivée
    private final int tDep;// temps de départ
    private final int tArr;// temps d'arrivée

    public Connection(int id, Stop pDep, Stop pArr, int tDep, int tArr) {
        this.id = id;
        this.pDep = pDep;
        this.pArr = pArr;
        this.tDep = tDep;
        this.tArr = tArr;
    }

    public Stop getPDep() {
        return pDep;
    }

    public Stop getPArr() {
        return pArr;
    }

    public int getTDep() {
        return tDep;
    }

    public int getTArr() {
        return tArr;
    }

    @Override
    public Stop getOtherStop(String stopId) {
        if (stopId.equals(pDep.getId())) {
            return pArr;
        } else if (stopId.equals(pArr.getId())) {
            return pDep;
        } else {
            throw new IllegalArgumentException("Stop ID not part of this footpath.");
        }
    }

    @Override
    public String toString() { 
        return String.format("id: %d, (%s -> %s), (%d -> %d)", id, pDep, pArr, tDep, tArr);
    }

}
