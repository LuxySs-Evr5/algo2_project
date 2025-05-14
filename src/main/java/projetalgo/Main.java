package projetalgo;

import java.io.IOException;

import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.UserInterruptException;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

import com.opencsv.exceptions.CsvValidationException;


public class Main {

    /**
     * @brief Get the input from the user.
     * @param solver the solver object to check if the stop exists (if stop is true)
     * @param reader the line reader object to read the input
     * @param textToShow the text to show to the user when asking for input
     * @return the Stop input from the user
     */
    private static Stop getStopInput(Solver solver, LineReader reader, final String textToShow) {
        String instruction = textToShow;
        try {
            while (true) {
                String input = reader.readLine(instruction).stripTrailing();
    
                if (input.equalsIgnoreCase("q") || input.equalsIgnoreCase("quit")) {
                    System.out.println("Exiting the program ...");
                    System.exit(0);
                }

                if (input.isEmpty()) {
                    instruction = "Invalid input. " + textToShow;
                    continue;
                }

                System.out.println("input: " + input);

                StopExistResult stopExistResult = solver.stopExists(input);

                System.out.println("stopExistResult: " + stopExistResult);
                
                if (stopExistResult.equals(StopExistResult.EXISTS)) {
                    return solver.getStop(input);
                }
    
                instruction = "The '" + input + "' stop was not found in the data. " + textToShow;
            }
        } catch (UserInterruptException e) {
            System.out.println("\nProgram interrupted by user.");
            System.exit(0);
        }
        return null;
    }   
    
    /**
     * @brief Get the input from the user.
     * @param solver the solver object to check if the stop exists (if stop is true)
     * @param reader the line reader object to read the input
     * @param textToShow the text to show to the user when asking for input
     * @return the time input from the user (int)
     */
    private static int getTimeInput(LineReader reader, final String textToShow) {
        String instruction = textToShow;
        try {
            while (true) {
                String input = reader.readLine(instruction).stripTrailing();
    
                if (input.equalsIgnoreCase("q") || input.equalsIgnoreCase("quit")) {
                    System.out.println("Exiting the program ...");
                    System.exit(0);
                }

                if (input.isEmpty()) {
                    instruction = "Invalid time format, expected HH:MM:SS" + textToShow;
                    continue;
                }

                int time = TimeConversion.toSeconds(input);
                if (time == -1) {
                    instruction = "Invalid time format, expected HH:MM:SS" + textToShow;
                    continue;
                }
                return time;
            }
        } catch (UserInterruptException e) {
            System.out.println("\nProgram interrupted by user.");
            System.exit(0);
        }
        return -1;
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
                System.out.println(AinsiCode.BOLD + "\n=== Create a New Trip ===" + AinsiCode.RESET);

                Stop pDeprr = getStopInput(solver, reader, "Enter the departure stop: ");
                if (pDeprr == null) {
                    System.out.println("Invalid stop. Please try again.");
                    continue;
                }
                Stop pArr = getStopInput(solver, reader, "Enter the arrival stop: ");
                if (pArr == null) {
                    System.out.println("Invalid stop. Please try again.");
                    continue;
                }
                int tDep = getTimeInput(reader, "Enter the departure time: ");
                if (tDep == -1) {
                    System.out.println("Invalid time format. Please try again.");
                    continue;
                }

                // -------------- Solve the shortest path --------------

                solver.solve(pDeprr, pArr, tDep);
            }
        } catch (IOException | CsvValidationException e) {
            System.err.println("Sata file not found or invalid csv");
        }
    }

}
