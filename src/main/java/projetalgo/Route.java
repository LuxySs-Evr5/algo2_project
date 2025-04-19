package projetalgo;

public class Route {

    private int id;
    private String shortName;
    private String longName;
    private final RouteType routeType;

    public Route(int id, String shortName, String longName, RouteType routeType)  {
        this.id = id;
        this.shortName = shortName;
        this.longName = longName;
        this.routeType = routeType;
    }

    public int getId() {
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
