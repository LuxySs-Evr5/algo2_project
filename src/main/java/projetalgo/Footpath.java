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
        this.stops = new Stop[] { stop0, stop1 };
    }

    public int getDur() {
        return dur;
    }

    public Stop[] getStops() {
        return stops;
    }

    public boolean contains(String stopId) {
        return stopId.equals(stops[0].getId()) || stopId.equals(stops[1].getId());
    }

    public Stop getOtherStop(String stopId) {
        if (stopId.equals(stops[0].getId())) {
            return stops[1];
        } else if (stopId.equals(stops[1].getId())) {
            return stops[0];
        } else {
            throw new IllegalArgumentException("Stop ID not part of this footpath.");
        }
    }

    @Override
    public String toString() {
        return String.format("id: %d, (%s <-> %s), %d", id, stops[0], stops[1], dur);
    }

}
