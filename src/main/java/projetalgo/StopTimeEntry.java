package projetalgo;

public class StopTimeEntry {
    String tripId;
    int departureTime;
    String stopId;
    int stopSequence;

    public StopTimeEntry(String tripId, int departureTime, String stopId, int stopSequence) {
        this.tripId = tripId;
        this.departureTime = departureTime;
        this.stopId = stopId;
        this.stopSequence = stopSequence;
    }
}
