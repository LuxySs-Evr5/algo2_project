package projetalgo;

import java.io.IOException;

public class Main {

    public static void main(String[] args) {
        // Paris - Bruxelles
        // Stop stop0 = new Stop("0", new Coord(50.840780, 4.452554));
        // Stop stop1 = new Stop("1", new Coord(48.8534100, 2.3488000));

        // SNCB-8896008,Courtrai,50.8245,3.264548
        // SNCB-8896388,Bissegem,50.82585,3.224208
        // Stop stop0 = new Stop("0", new Coord(50.8245, 3.264548));
        // Stop stop1 = new Stop("1", new Coord(50.82585, 3.224208));
        //
        // Footpath f = new Footpath(0, stop0, stop1);
        //
        // System.out.println(f.getDistance());
        // System.out.println(f.getTravelTime());

        Solver solver = new Solver();
        try {
            solver.loadData("./GTFS/SNCB/routes.csv", "./GTFS/SNCB/stop_times.csv",
                    "./GTFS/SNCB/stops.csv", "./GTFS/SNCB/trips.csv");

            // Test data
            // String pDepId = "A";
            // String pArrId = "E";
            // String strTDep = "00:00:00";

            String pDepId = "SNCB-8896008";
            String pArrId = "SNCB-8896388";
            String strTDep = "00:00:00";

            int tDep = TimeConversion.toSeconds(strTDep);
            System.out.printf("tDep: %s = %d\n", strTDep, tDep);

            solver.solve(pDepId, pArrId, tDep);
        } catch (IOException e) {
            System.err.println("data file not found");
        }
    }

}
