package projetalgo;

public class RouteInfo {
    private final String lineId;
    private final String lineName;
    private final String transportType;

    public RouteInfo(String lineId, String lineName, String transportType) {
        this.lineId = lineId;
        this.lineName = lineName;
        this.transportType = transportType;
    }

    public String getLineId() {
        return lineId;
    }

    public String getLineName() {
        return lineName;
    }

    public String getTransportType() {
        return transportType;
    }

    @Override
    public String toString() {
        return transportType + " line " + lineId + " (" + lineName + ")";
    }
}
