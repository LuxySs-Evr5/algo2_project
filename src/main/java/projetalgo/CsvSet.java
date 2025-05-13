package projetalgo;

import java.io.File;

public class CsvSet {
    public final String routesCSV;
    public final String stopTimesCSV;
    public final String stopsCSV;
    public final String tripsCSV;
    public final String transportOperator;

    public CsvSet(String routesCSV, String stopTimesCSV, String stopsCSV, String tripsCSV) {
        // Get the transport operator from the routesCSV file path (in the parent directory with the name of the transport operator)
        File file = new File(routesCSV);
        this.transportOperator = file.getParentFile().getName();

        this.routesCSV = routesCSV;
        this.stopTimesCSV = stopTimesCSV;
        this.stopsCSV = stopsCSV;
        this.tripsCSV = tripsCSV;
    }
}
