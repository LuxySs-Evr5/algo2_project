package projetalgo;

public class CsvSet {
    public final String routesCSV;
    public final String stopTimesCSV;
    public final String stopsCSV;
    public final String tripsCSV;

    public CsvSet(String routesCSV, String stopTimesCSV, String stopsCSV, String tripsCSV) {
        this.routesCSV = routesCSV;
        this.stopTimesCSV = stopTimesCSV;
        this.stopsCSV = stopsCSV;
        this.tripsCSV = tripsCSV;
    }
}
