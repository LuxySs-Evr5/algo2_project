package projetalgo;

import java.io.IOException;

import com.opencsv.exceptions.CsvValidationException;

public class Main {

    public static final String ANSI_RESET = "\u001B[0m";
    public static final String ANSI_BOLD = "\u001B[1m";
    public static final String ANSI_UNDERLINE = "\u001B[4m";

    public static void main(String[] args) {
        Solver solver = new Solver();
        try {
            // -------------- Load the GTFS data --------------

            System.out.println("Loading the data of the GTFS files. Please wait...");

            CsvSet sncbSet = new CsvSet("./GTFS/SNCB/routes.csv", "./GTFS/SNCB/stop_times.csv",
                    "./GTFS/SNCB/stops.csv", "./GTFS/SNCB/trips.csv");

            CsvSet stibSet = new CsvSet("./GTFS/STIB/routes.csv", "./GTFS/STIB/stop_times.csv",
                    "./GTFS/STIB/stops.csv", "./GTFS/STIB/trips.csv");

            CsvSet delijnSet = new CsvSet("./GTFS/DELIJN/routes.csv", "./GTFS/DELIJN/stop_times.csv",
                    "./GTFS/DELIJN/stops.csv", "./GTFS/DELIJN/trips.csv");

            CsvSet tecSet = new CsvSet("./GTFS/TEC/routes.csv", "./GTFS/TEC/stop_times.csv",
                    "./GTFS/TEC/stops.csv", "./GTFS/TEC/trips.csv");

            solver.loadData(sncbSet, stibSet, delijnSet, tecSet);

            System.out.println("Data loaded successfully!");

            System.out.println("\nPress 'q' or enter 'quit' to stop the program.");
            System.out.println("Enter '<Departure Stop>' '<Arrival Stop>' '<Departure Time>' to find the shortest path.");
            System.out.println("Example: Namur Brussels 12:00:00.");
            System.out.println("(Use 24-hour time format, e.g., 08:00:30 or 17:30:45)");

            // -------------- While the user won't quit --------------

            boolean quit = false;

            while (!quit) {
                System.out.print("\nEnter your request: ");
                String input = System.console().readLine();

                if (input.equals("q") || input.equals("quit")) {
                    quit = true;
                } else {
                    // -------------- Solve the shortest path --------------
                    String[] inputs = input.split(" ");

                    if (inputs.length == 3) {
                        String pDepName = inputs[0].toLowerCase();
                        String pArrName = inputs[1].toLowerCase();
                        String strTDep = inputs[2];

                        if (pDepName.equals("") || pArrName.equals("") || strTDep.equals("")) {
                            System.out.println("Invalid input. Please enter '<Departure Stop>' '<Arrival Stop>' '<Departure Time>'.");
                            continue;
                        }

                        int tDep = TimeConversion.toSeconds(strTDep);
                        if (tDep == -1) {
                            continue;
                        }

                        System.out.println(ANSI_BOLD + ANSI_UNDERLINE + "\nThe shortest path is:" + ANSI_RESET);
                        solver.solve(pDepName, pArrName, tDep);
                    } else {
                        System.out.println("Invalid input. Please enter '<Departure Stop>' '<Arrival Stop>' '<Departure Time>'.");
                    }
                }
            }
        } catch (IOException | CsvValidationException e) {
            System.err.println("data file not found or invalid csv");
        }
    }

}
