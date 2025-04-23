package projetalgo;

public class Connection {
    private int id;
    private Stop p_dep; // point de départ
    private Stop p_arr; // point d'arrivée
    private int t_dep;// temps de départ
    private int t_arr;// temps d'arrivée

    public Connection(int id, Stop p_dep, Stop p_arr, int t_dep, int t_arr) {
        this.id = id;
        this.p_dep = p_dep;
        this.p_arr = p_arr;
        this.t_dep = t_dep;
        this.t_arr = t_arr;
    }

    public Stop get_p_dep() {
        return p_dep;
    }

    public Stop get_p_arr() {
        return p_arr;
    }

    public int get_t_dep() {
        return t_dep;
    }

    public int get_t_arr() {
        return t_arr;
    }

    public String toString() {
        return String.format("id: %d, (%s <-> %s), (%d -> %d)", id, p_dep, p_arr, t_dep, t_arr);
    }

}
