package projetalgo;

import java.io.IOException;

public class Main {

    public static void main(String[] args) {
        Solver solver = new Solver();
        try {
            solver.loadData("./GTFS/SNCB/routes.csv", "./GTFS/SNCB/stop_times.csv", "./GTFS/SNCB/stops.csv", "./GTFS/SNCB/trips.csv");

            String pDepId = "SNCB-8896008";
            String pArrId = "SNCB-8896388";
            String strTDep = "06:04:00";
            int tDep = TimeConversion.toSeconds(strTDep);
            System.out.printf("tDep: %s = %d\n", strTDep, tDep);

            solver.solve(pDepId, pArrId, tDep);
        } catch (IOException e) {
            System.err.println("data file not found");
        }
    }

}
