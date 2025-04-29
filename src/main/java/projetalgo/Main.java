package projetalgo;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.opencsv.exceptions.CsvValidationException;

import javafx.util.Pair;

public class Main {

    public static void main(String[] args) {
        MultiCritSolver solver = new MultiCritSolver();
        try {
            CsvSet sncbDataSet = new CsvSet("./GTFS/SNCB/routes.csv", "./GTFS/SNCB/stop_times.csv",
                    "./GTFS/SNCB/stops.csv", "./GTFS/SNCB/trips.csv");

            solver.loadData(sncbDataSet);

            String pDepId = "SNCB-8200518";
            String pArrId = "SNCB-8728686";
            String strTDep = "00:00:00";

            int tDep = TimeConversion.toSeconds(strTDep);
            System.out.printf("tDep: %s = %d\n", strTDep, tDep);

            solver.solve(TramsCountCriteriaTracker::new, pDepId, pArrId, tDep);
        } catch (IOException | CsvValidationException e) {
            System.err.println("data file not found or invalid csv");
        }
    }

}
