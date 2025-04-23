package projetalgo;

public class Footpath {
    private int id;
    private Stop[] stops;
    private int dur;

    public Footpath(int id, Stop stop0, Stop stop1, int dur) {
        this.id = id;
        this.dur = dur;

        if (stop0.equals(stop1)) {
            throw new IllegalArgumentException("A Footpath must connect two distinct stops.");
        }
        this.stops = new Stop[2];
        this.stops[0] = stop0;
        this.stops[1] = stop1;
    }

    public int getDur() {
        return dur;
    }

    public Stop[] getStops() {
        return stops;
    }

    public boolean contains(Stop stop) {
        return stop.equals(stops[0]) || stop.equals(stops[1]);
    }

    public Stop getOtherStop(Stop stop) {
        return stop.equals(stops[0]) ? stops[1] : stops[0];
    }

    @Override
    public String toString() {
        return String.format("id: %d, (%s <-> %s), %d", id, stops[0], stops[1], dur);
    }

}
