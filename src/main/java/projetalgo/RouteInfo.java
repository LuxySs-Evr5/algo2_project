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

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else if (o == null || getClass() != o.getClass()) {
            return false;
        }
        RouteInfo that = (RouteInfo) o;
        return routeId.equals(that.routeId)
            && routeName.equals(that.routeName)
            && transportType == that.transportType
            && transportOperator.equals(that.transportOperator);
    }

    @Override
    public int hashCode() {
        int result = routeId.hashCode();
        result = 31 * result + routeName.hashCode();
        result = 31 * result + transportType.hashCode();
        result = 31 * result + transportOperator.hashCode();
        return result;
    }
}
