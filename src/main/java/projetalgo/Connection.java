package projetalgo;

public class Connection implements Movement {
    private final String tripId;
    RouteInfo routeInfo;
    private final Stop pDep; // departure point
    private final Stop pArr; // arrival point
    private final int tDep;  // departure time
    private final int tArr;  // arrival time

    public Connection(String tripId, RouteInfo routeInfo, Stop pDep, Stop pArr, int tDep, int tArr) {
        this.tripId = tripId;
        this.routeInfo = routeInfo;
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

    public RouteInfo gRouteInfo() {
        return routeInfo;
    }

    @Override
    public String toString() { 
        return String.format("tripId: %s, (%s -> %s), (%d -> %d)", tripId, pDep, pArr, tDep, tArr);
    }

}
