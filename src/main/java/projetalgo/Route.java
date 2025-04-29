package projetalgo;

public class Route {

    public enum RouteType {
        TRAIN,
        BUS,
        METRO,
        TRAM,
    };

    private String id;
    private String shortName;
    private String longName;
    private RouteType routeType;

    public Route(String id, String shortName, String longName, RouteType routeType) {
        this.id = id;
        this.shortName = shortName;
        this.longName = longName;
        this.routeType = routeType;
    }

    public String getId() {
        return id;
    }

    public String getShortName() {
        return shortName;
    }

    public String getLongName() {
        return longName;
    }

    public RouteType getRouteType() {
        return routeType;
    }

}
