package projetalgo;

import java.io.IOException;

import com.opencsv.exceptions.CsvValidationException;

public class Main {

    public static void main(String[] args) {
        Solver solver = new Solver();
        try {
            CsvSet sncbSet = new CsvSet("./GTFS/SNCB/routes.csv", "./GTFS/SNCB/stop_times.csv",
                    "./GTFS/SNCB/stops.csv", "./GTFS/SNCB/trips.csv");

            CsvSet stibSet = new CsvSet("./GTFS/STIB/routes.csv", "./GTFS/STIB/stop_times.csv",
                    "./GTFS/STIB/stops.csv", "./GTFS/STIB/trips.csv");

            CsvSet delijnSet = new CsvSet("./GTFS/DELIJN/routes.csv", "./GTFS/DELIJN/stop_times.csv",
                    "./GTFS/DELIJN/stops.csv", "./GTFS/DELIJN/trips.csv");

            CsvSet tecSet = new CsvSet("./GTFS/TEC/routes.csv", "./GTFS/TEC/stop_times.csv",
                    "./GTFS/TEC/stops.csv", "./GTFS/TEC/trips.csv");

            solver.loadData(sncbSet, stibSet, delijnSet, tecSet);

            String pDepName = "Courtrai";
            String pArrName = "Waregem";
            String strTDep = "06:03:30";

            int tDep = TimeConversion.toSeconds(strTDep);
            System.out.printf("tDep: %s = %d\n", strTDep, tDep);

            solver.solve(pDepName, pArrName, tDep);
        } catch (IOException | CsvValidationException e) {
            System.err.println("data file not found or invalid csv");
        }
    }

}
