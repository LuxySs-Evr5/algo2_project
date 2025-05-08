package projetalgo;

public class Footpath implements Movement {
    private final Stop pDep; // departure point
    private final Stop pArr; // arrival point

    public static double WALKING_SPEED = 5; // km/h

    public Footpath(Stop pDep, Stop pArr) {
        this.pDep = pDep;
        this.pArr = pArr;
    }

    /**
     * Uses haversine formula to compute the distance between the two stops that
     * it links.
     */
    public double getDistance() {
        return Coord.distance(pDep.getCoord(), pArr.getCoord());
    }

    /**
     * Returns the travel time in seconds assuming constant walking speed.
     */
    public int getTravelTime() {
        double distanceInKm = getDistance();
        double speedInKmPerSecond = WALKING_SPEED / 3600.0; // km/h -> km/s

        // Avoid travelTime = 0 second as this can make the algorithm take additional
        // footpaths that aren't necessary to get to an arrival stop.
        return Math.max(1, (int) Math.round(distanceInKm / speedInKmPerSecond));
    }

    @Override
    public Stop getPDep() {
        return pDep;
    }

    @Override
    public Stop getPArr() {
        return pArr;
    }

    public boolean contains(String stopId) {
        return stopId.equals(pDep.getId()) || stopId.equals(pArr.getId());
    }

    @Override
    public String toString() {
        return String.format("(%s <-> %s), %d", pDep, pArr, getTravelTime());
    }

}
