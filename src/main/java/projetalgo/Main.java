package projetalgo;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.jline.reader.EndOfFileException;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.UserInterruptException;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

import com.opencsv.exceptions.CsvValidationException;


public class Main {
    private enum SolverType {
        MULTI_CRIT,
        EARLIEST_ARRIVAL
    }

    /**
     * @brief Get the stops with the same name but not duplicate. (duplicate means that the stop is in the same route but in different direction)
     * @param stopResult the list of stops to check
     * @return List of stops with the same name but not duplicate
     */
    private static List<Stop> getSameStopsNameNoDuplicate(List<Stop> stopResult) {
        List<Stop> stopExistResult = new ArrayList<>();
        for (Stop stop : stopResult) {
            RouteInfo routeInfo = stop.getRouteInfo();
            if (routeInfo != null && stopExistResult.isEmpty()) {
                stopExistResult.add(stop);
                continue;
            }
            List<Stop> savedStops = new ArrayList<>();
            for (Stop stop1 : stopExistResult) {
                if (!stop1.getRouteInfo().equals(routeInfo)) {
                    savedStops.add(stop1);
                }
            }
            stopExistResult.addAll(savedStops);
        }
        return stopExistResult;
    }

    /**
     * @brief Get a single stop input from the user.
     * @param solver the solver object to check if the stop exists (if stop is true)
     * @param reader the line reader object to read the input
     * @param textToShow the text to show to the user when asking for input
     * @param uniqueStop if true, we want only one stop (if false, we can have all the stops with the same name or not (depending on the user choice))
     * @return List of stops input from the user (only one stop)
     */
    private static List<String> getStopInput(Solver solver, LineReader reader, final String textToShow, final boolean uniqueStop) {
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

                List<Stop> stopResult = solver.stopsWithName(input, Optional.empty());
                List<Stop> differentStopsSameNameResult = getSameStopsNameNoDuplicate(stopResult);
                
                if (differentStopsSameNameResult.size() == 1) {
                    return stopResult.stream()
                            .map(Stop::getId)
                            .collect(Collectors.toList());
                } else if (differentStopsSameNameResult.size() > 1) {
                    String instruct = "Several stops found with the name '" + input + "'. Please enter the full name of the route who passes by this stop or enter 'all' to use all the stops with this name: ";
                    while (true) {
                        String routeName = reader.readLine(instruct).stripTrailing();
                        if (routeName.equalsIgnoreCase("q") || routeName.equalsIgnoreCase("quit")) {
                            System.out.println("Exiting the program ...");
                            System.exit(0);
                        }
                        if (routeName.isEmpty()) {
                            instruct = "Invalid input. Please enter the full name of the route who passes by this stop or enter 'all' to use all the stops with this name: ";
                            continue;
                        }
                        if (!uniqueStop && routeName.equalsIgnoreCase("all")) { // if uniqueStop is false, we can have all the stops with the same name or not (depending on the user choice)
                            return stopResult.stream()
                                    .map(Stop::getId)
                                    .collect(Collectors.toList());
                        }

                        List<Stop> stopResultWithRouteName = solver.stopsWithName(input, Optional.of(routeName));
                        List<Stop> differentStopsSameNameWithRouteNameResult = getSameStopsNameNoDuplicate(stopResultWithRouteName);

                        if (differentStopsSameNameWithRouteNameResult.size() == 1) {
                            return differentStopsSameNameWithRouteNameResult.stream()
                                    .map(Stop::getId)
                                    .collect(Collectors.toList());
                        }
                        if (differentStopsSameNameWithRouteNameResult.isEmpty()) {
                            instruct = "The stop '" + input + "' with the route '" + routeName + "' was not found. Please try again: ";
                        }
                        else if (differentStopsSameNameWithRouteNameResult.size() > 1) {
                            System.err.println("Several stops found with the name '" + input + "' and the route '" + routeName + "'. We cannot determine which one you want.");
                            return null;
                        }
                    }
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


            Data AllData = Data.loadFromCSVs(sncbSet, stibSet, delijnSet, tecSet);
            Data sncbData = Data.loadFromCSVs(sncbSet);
            
            Solver solver = new Solver(AllData);
            MultiCritSolver multiCritSolver = new MultiCritSolver(sncbData);

            long endTime = System.nanoTime();
            double durationInSeconds = (endTime - startTime) /  1_000_000_000.0;

            System.out.printf("Data loaded successfully in %.2f seconds!\n", durationInSeconds);

            System.out.println("\nYou can press 'q' or enter 'quit' at any time to stop the program.");
            System.out.println("For the Departure Time, use 24-hour time format, e.g., 08:00:30 or 17:30:45\n");

            // -------------- While the user won't quit --------------

            String instruction = "Please choose whether you want optimize a second criterion before entering your journey (enter '0' for earliest arrival or '1' for multicriteria): ";
            while (true) {
                String solverTypeStr = reader.readLine(instruction).stripTrailing();
                if (solverTypeStr.equalsIgnoreCase("q") || solverTypeStr.equalsIgnoreCase("quit")) {
                    System.out.println("Exiting the program ...");
                    System.exit(0);
                }
                SolverType solverType;
                if (solverTypeStr.equals("0")) {
                    solverType = SolverType.EARLIEST_ARRIVAL;
                } else if (solverTypeStr.equals("1")) {
                    solverType = SolverType.MULTI_CRIT;
                } else {
                    instruction = "Invalid input. Please enter '0' or '1': ";
                    continue;
                }

                boolean running = true;

                if (solverType == SolverType.EARLIEST_ARRIVAL) {
                    while (running) {
                        System.out.println(AinsiCode.BOLD + "\n=== Create a New Trip ===" + AinsiCode.RESET);

                        List<String> pDepIds = getStopInput(solver, reader, "Enter the departure stop: ", false);
                        if (pDepIds == null) {
                            System.out.println("Invalid stop. Please try again.");
                            continue;
                        }
                        List<String> pArrIds = getStopInput(solver, reader, "Enter the arrival stop: ", false);
                        if (pArrIds == null) {
                            System.out.println("Invalid stop. Please try again.");
                            continue;
                        }
                        int tDep = getTimeInput(reader, "Enter the departure time: ");
                        if (tDep == -1) {
                            System.out.println("Invalid time format. Please try again.");
                            continue;
                        }

                        // -------------- Solve the shortest path --------------

                        solver.solve(pDepIds, pArrIds, tDep);
                        running = false;
                    }
                }

                else {
                    while (running) {
                        System.out.println(AinsiCode.BOLD + "\n=== Create a New Trip ===" + AinsiCode.RESET);

                        List<String> pDepIds = getStopInput(solver, reader, "Enter the departure stop: ", true);
                        if (pDepIds == null || pDepIds.size() != 1) {
                            System.out.println("Invalid stop. Please try again.");
                            continue;
                        }
                        List<String> pArrIds = getStopInput(solver, reader, "Enter the arrival stop: ", true);
                        if (pArrIds == null || pArrIds.size() != 1) {
                            System.out.println("Invalid stop. Please try again.");
                            continue;
                        }
                        int tDep = getTimeInput(reader, "Enter the departure time: ");
                        if (tDep == -1) {
                            System.out.println("Invalid time format. Please try again.");
                            continue;
                        }

                        // -------------- Solve the shortest path --------------

                        System.out.println("Searching ...");
                        multiCritSolver.solve(TramsCountCriteriaTracker::new, pDepIds.get(0), pArrIds.get(0), tDep);

                        running = false;
                    }
                }
            }
        } catch (IOException | CsvValidationException e) {
            System.err.println("Data file not found or invalid csv");
        }
        catch (UserInterruptException | EndOfFileException e) {
            System.out.println("\nProgram interrupted by user.");
        }
    }
}
