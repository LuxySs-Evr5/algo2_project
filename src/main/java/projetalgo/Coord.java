package projetalgo;

public class Coord {
    private final double lat;
    private final double lon;

    public static final double EARTH_RADIUS = 6371.0; // km

    public Coord(double lat, double lon) {
        this.lat = lat;
        this.lon = lon;
    }

    public Coord(Coord other) {
        this.lat = other.lat;
        this.lon = other.lon;
    }

    public double lat() {
        return lat;
    }

    public double lon() {
        return lon;
    }

    public double distanceTo(Coord other) {
        double lat1 = Math.toRadians(this.lat());
        double lon1 = Math.toRadians(this.lon());
        double lat2 = Math.toRadians(other.lat());
        double lon2 = Math.toRadians(other.lon());
    
        double dLat = lat2 - lat1;
        double dLon = lon2 - lon1;
    
        double a = Math.pow(Math.sin(dLat / 2), 2)
                + Math.cos(lat1) * Math.cos(lat2) * Math.pow(Math.sin(dLon / 2), 2);

        double c = 2 * Math.asin(Math.sqrt(a));
        
        return EARTH_RADIUS * c;
    }

    public static double distance(Coord coord1, Coord coord2) {
        return coord1.distanceTo(coord2);
    }
}
