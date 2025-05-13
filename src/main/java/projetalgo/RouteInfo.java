package projetalgo;

public class RouteInfo {
    private final String routeId;
    private final String routeName;
    private final TransportType transportType;
    private final String transportOperator;

    public RouteInfo(String routeId, String routeName, TransportType transportType, String transportOperator) {
        this.routeId = routeId;
        this.routeName = routeName;
        this.transportType = transportType;
        this.transportOperator = transportOperator;
    }

    public String getRouteId() {
        return routeId;
    }

    public String getRouteName() {
        return routeName;
    }

    public TransportType getTransportType() {
        return transportType;
    }

    public String getTransportOperator() {
        return transportOperator;
    }

    @Override
    public String toString() {
        return transportOperator + " " + transportType + " line " + routeId + " (" + routeName + ")";
    }
}
