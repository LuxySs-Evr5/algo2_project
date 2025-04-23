package projetalgo;

public class Connection {
    private int id;
    private Stop pDep; // point de départ
    private Stop pArr; // point d'arrivée
    private int tDep;// temps de départ
    private int tArr;// temps d'arrivée

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

    public String toString() {
        return String.format("id: %d, (%s -> %s), (%d -> %d)", id, pDep, pArr, tDep, tArr);
    }

}
