package projetalgo;

public class Footpath implements Movement {
    private final int id;
    private Stop[] stops;

    public static double WALKING_SPEED = 5; // km/s
    public static double EARTH_RADIUS = 6371.0; // km

    public Footpath(int id, Stop stop0, Stop stop1) {
        this.id = id;

        if (stop0.equals(stop1)) {
            throw new IllegalArgumentException("A Footpath must connect two distinct stops.");
        }
        this.stops = new Stop[] { stop0, stop1 };
    }

    /**
     * Uses haversine formula to compute the distance between the two stops that
     * it links.
     */
    public double getDistance() {
        Coord coord0 = stops[0].getCoord();
        Coord coord1 = stops[1].getCoord();

        return Coord.distance(coord0, coord1);
    }

    /**
     * Returns the travel time in seconds assuming constant walking speed.
     */
    public int getTravelTime() {
        double distanceInKm = getDistance();
        double speedInKmPerSecond = WALKING_SPEED / 3600.0; // km/h -> km/s
        return (int) Math.round(distanceInKm / speedInKmPerSecond);
    }

    public Stop[] getStops() {
        return stops;
    }

    public boolean contains(String stopId) {
        return stopId.equals(stops[0].getId()) || stopId.equals(stops[1].getId());
    }

    @Override
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
        return String.format("id: %d, (%s <-> %s), %d", id, stops[0], stops[1], getTravelTime());
    }

}
