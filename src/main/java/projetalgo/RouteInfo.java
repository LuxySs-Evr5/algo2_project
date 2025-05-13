package projetalgo;

public class RouteInfo {
    private final String routeId;
    private final String routeName;
    private final String transportType;

    public RouteInfo(String routeId, String routeName, String transportType) {
        this.routeId = routeId;
        this.routeName = routeName;
        this.transportType = transportType;
    }

    public String getRouteId() {
        return routeId;
    }

    public String getRouteName() {
        return routeName;
    }

    public String getTransportType() {
        return transportType;
    }

    @Override
    public String toString() {
        return transportType + " line " + routeId + " (" + routeName + ")";
    }
}
