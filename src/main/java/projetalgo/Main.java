package projetalgo;

import java.io.IOException;

import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.UserInterruptException;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

import com.opencsv.exceptions.CsvValidationException;


public class Main {

    public static final String ANSI_RESET = "\u001B[0m";
    public static final String ANSI_BOLD = "\u001B[1m";
    public static final String ANSI_UNDERLINE = "\u001B[4m";
    public static final String ANSI_RED = "\u001B[31m";

    private static String getInput(LineReader reader, String textToShow) {
        try {
            while (true) {
                String input = reader.readLine(textToShow).stripTrailing().toLowerCase();
    
                if (input.equals("q") || input.equals("quit")) {
                    System.out.println("Exiting the program ...");
                    System.exit(0);
                }
    
                if (!input.isEmpty()) {
                    return input;
                }
    
                textToShow = "Invalid input. " + textToShow;
            }
        } catch (UserInterruptException e) {
            System.out.println("\nProgram interrupted by user.");
            System.exit(0);
        }
        return "";
    }    

    public static void main(String[] args) {
        Solver solver = new Solver();
        try {
            Terminal terminal = TerminalBuilder.terminal();
            LineReader reader = LineReaderBuilder.builder().terminal(terminal).build();

            // -------------- Load the GTFS data --------------

            System.out.println("Loading the data of the GTFS files. Please wait ...");

            long startTime = System.nanoTime();

            CsvSet sncbSet = new CsvSet("./GTFS/SNCB/routes.csv", "./GTFS/SNCB/stop_times.csv",
                    "./GTFS/SNCB/stops.csv", "./GTFS/SNCB/trips.csv");

            CsvSet stibSet = new CsvSet("./GTFS/STIB/routes.csv", "./GTFS/STIB/stop_times.csv",
                    "./GTFS/STIB/stops.csv", "./GTFS/STIB/trips.csv");

            CsvSet delijnSet = new CsvSet("./GTFS/DELIJN/routes.csv", "./GTFS/DELIJN/stop_times.csv",
                    "./GTFS/DELIJN/stops.csv", "./GTFS/DELIJN/trips.csv");

            CsvSet tecSet = new CsvSet("./GTFS/TEC/routes.csv", "./GTFS/TEC/stop_times.csv",
                    "./GTFS/TEC/stops.csv", "./GTFS/TEC/trips.csv");

            solver.loadData(sncbSet, stibSet, delijnSet, tecSet);

            long endTime = System.nanoTime();
            double durationInSeconds = (endTime - startTime) /  1_000_000_000.0;

            System.out.printf("Data loaded successfully in %.2f seconds!\n", durationInSeconds);

            System.out.println("\nPress 'q' or enter 'quit' to stop the program.");
            System.out.println("For the Departure Time, use 24-hour time format, e.g., 08:00:30 or 17:30:45");

            // -------------- While the user won't quit --------------

            while (true) {
                System.out.println(ANSI_BOLD + "\n=== Create a New Trip ===" + ANSI_RESET);

                String pDepName = getInput(reader, "Enter the departure stop: ");
                String pArrName = getInput(reader, "Enter the arrival stop: ");
                String strTDep = getInput(reader, "Enter the departure time: ");

                int tDep = TimeConversion.toSeconds(strTDep);
                if (tDep == -1) {
                    continue;
                }

                // -------------- Solve the shortest path --------------

                System.out.println(
                    ANSI_BOLD + "\nThe shortest path for " +
                    ANSI_RED + ANSI_UNDERLINE + pDepName + ANSI_RESET + ANSI_BOLD +
                    " to " + ANSI_RED + ANSI_UNDERLINE + pArrName + ANSI_RESET + ANSI_BOLD +
                    " at " + ANSI_RED + ANSI_UNDERLINE + strTDep + ANSI_RESET + ANSI_BOLD +
                    " is:" + ANSI_RESET
                );

                solver.solve(pDepName, pArrName, tDep);
            }
        } catch (IOException | CsvValidationException e) {
            System.err.println("data file not found or invalid csv");
        }
    }

}
