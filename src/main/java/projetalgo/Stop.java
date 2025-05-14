package projetalgo;

public class Stop {
    private final String id;
    private final Coord coord;
    private final String name;
    private final String transportOperatorStop;
    private RouteInfo routeInfo;

    public Stop(String id, String name, Coord coord, String transportOperatorStop) {
        this.id = id;
        this.name = name;
        this.coord = coord;
        this.transportOperatorStop = transportOperatorStop;
    }

    public String getId() {
        return id;
    }

    public Coord getCoord() {
        return coord;
    }

    public String getName() {
        return name;
    }

    public String getTransportOperatorStop() {
        return transportOperatorStop;
    }

    public void setRouteInfo(RouteInfo route) {
        this.routeInfo = route;
    }

    public RouteInfo getRouteInfo() {
        return routeInfo;
    }

    @Override
    public String toString() {
        return String.format("Name: %s, id: %s, coord: %s", name, id, coord);
    }
    
}
