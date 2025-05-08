package projetalgo;

import java.io.FileReader;
import java.io.IOException;
import java.sql.Array;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;

import javafx.util.Pair;
import projetalgo.Route.RouteType;

public class MultiCritSolver {

    private HashMap<String, Stop> stopIdToStop;
    private HashMap<String, Route> tripIdToRoute;
    private HashMap<String, List<Footpath>> stopIdToIncomingFootpaths;
    private HashMap<String, List<Footpath>> stopIdToOutgoingFootpaths;
    private List<Connection> connections;

    public MultiCritSolver() {
        this.connections = new ArrayList<>();
        this.stopIdToStop = new HashMap<>();
        this.tripIdToRoute = new HashMap<>();
        this.stopIdToIncomingFootpaths = new HashMap<>();
        this.stopIdToOutgoingFootpaths = new HashMap<>();
    }

    /**
     * Returns the index of the first connection departing at or after our departure
     * time.
     */
    private int getEarliestReachableConnectionIdx(int tDep) {
        // TODO: avoid code duplication by moving this method in an AbstractSolver class
        int i = 0;
        int j = connections.size();

        while (i < j) {
            int mid = (i + j) / 2;
            if (connections.get(mid).getTDep() < tDep) {
                i = mid + 1;
            } else {
                j = mid;
            }
        }

        return i;
    }

    private void diplayJourney(Map<String, ProfileFunction<FootpathsCountCriteriaTracker>> F, String pDepId,
            String pArrId, int tDep,
            FootpathsCountCriteriaTracker footpathsCountCriteriaTracker) {

        int currentTDep = tDep;
        String currentStopId = pDepId;
        FootpathsCountCriteriaTracker currentFootpathsCountCriteriaTracker = footpathsCountCriteriaTracker;

        while (!currentStopId.equals(pArrId)) {
            System.out.printf("currentStopId: %s\n", currentStopId);

            Movement movement = F.get(currentStopId).getFirstMatch(currentTDep, currentFootpathsCountCriteriaTracker);
            System.out.printf("taking %s\n", movement);

            currentStopId = movement.getPArr().getId();
            if (movement instanceof Footpath footpath) {
                footpathsCountCriteriaTracker.decFootpathsCount();
                currentTDep += footpath.getTravelTime();
            } else if (movement instanceof Connection connection) {
                currentTDep = connection.getTArr();
            }
        }
    }

    FootpathsCountCriteriaTracker promptJourney(
            Map<String, ProfileFunction<FootpathsCountCriteriaTracker>> F,
            String pDepId,
            int tDep) {
        Map<FootpathsCountCriteriaTracker, Pair<Integer, Movement>> results = F.get(pDepId).evaluateAt(tDep);

        // find journeys dominated by other journeys that we can take
        // NOTE: Until now, there could be journeys that were dominated by other
        // journeys leaving earlier, but we could not remove the ones leaving later
        // because we didn't know whether we would be able to arrive on time to catch
        // the ones leaving earlier.
        // But since now, we know that we can catch those leaving at tdep, we can remove
        // all the journeys dominated by other journeys leaving at/after tdep.
        ArrayList<FootpathsCountCriteriaTracker> dominatedResults = new ArrayList<>();
        for (Map.Entry<FootpathsCountCriteriaTracker, Pair<Integer, Movement>> entry0 : results.entrySet()) {
            for (Map.Entry<FootpathsCountCriteriaTracker, Pair<Integer, Movement>> entry1 : results.entrySet()) {

                FootpathsCountCriteriaTracker entry0Criteria = entry0.getKey();
                int entry0TArr = entry0.getValue().getKey();
                FootpathsCountCriteriaTracker entry1Criteria = entry1.getKey();
                int entry1TArr = entry1.getValue().getKey();

                if ((entry0Criteria.dominates(entry1Criteria)
                        && entry0TArr <= entry1TArr) ||
                        (entry0Criteria.equals(entry1Criteria)
                                && entry0TArr < entry1TArr)) {
                    dominatedResults.add(entry1Criteria);
                }
            }
        }

        // remove the journeys we found above from results as we have better
        // alternatives
        dominatedResults.forEach((dominatedJourneyCriteria) -> results.remove(dominatedJourneyCriteria));

        List<FootpathsCountCriteriaTracker> options = new ArrayList<>(results.keySet());

        System.out.println("Possible journeys:");
        for (int i = 0; i < options.size(); i++) {
            FootpathsCountCriteriaTracker tracker = options.get(i);
            int tArr = results.get(tracker).getKey();
            System.out.printf(" [%d] arrives at %s\n", i,
                    TimeConversion.fromSeconds(tArr));
            System.out.printf(" %s\n\n", tracker);
        }

        Scanner scanner = new Scanner(System.in);
        int choice = -1;
        while (true) {
            System.out.print("Enter the number of the journey you want to choose: ");
            try {
                choice = Integer.parseInt(scanner.nextLine());
                if (choice >= 0 && choice < options.size()) {
                    break;
                } else {
                    System.out.println("Invalid input, please enter a number from the list.");
                }
            } catch (NumberFormatException e) {
                System.out.println("Invalid input, please enter a number.");
            }
        }

        scanner.close();

        return options.get(choice);
    }

    private static Map<FootpathsCountCriteriaTracker, Pair<Integer, Movement>> mergeMaps(
            List<Map<FootpathsCountCriteriaTracker, Pair<Integer, Movement>>> maps) {
        Map<FootpathsCountCriteriaTracker, Pair<Integer, Movement>> ret = new HashMap<>();

        // TODO: try to remove code duplication with ProfileFunction evaluateAt
        for (Map<FootpathsCountCriteriaTracker, Pair<Integer, Movement>> map : maps) {
            for (Map.Entry<FootpathsCountCriteriaTracker, Pair<Integer, Movement>> entry : map.entrySet()) {
                Pair<Integer, Movement> pairCurrentlyAtKey = ret.get(entry.getKey());
                if (pairCurrentlyAtKey == null) {
                    ret.put(entry.getKey(), entry.getValue());
                } else { // there is already sth for that CriteriaTracker -> update only if it improves
                         // TArr
                    int entryTArr = entry.getValue().getKey();
                    int pairCurrentlyAtKeyTArr = pairCurrentlyAtKey.getKey();

                    if (entryTArr < pairCurrentlyAtKeyTArr) {
                        ret.put(entry.getKey(), entry.getValue());
                    }
                }
            }
        }

        return ret;
    }

    /**
     * Connections must be sorted by their departure time.
     */
    public void solve(String pDepId, String pArrId, int tDep) {
        // stopId -> ProfileFunction
        Map<String, ProfileFunction<FootpathsCountCriteriaTracker>> S = new HashMap<>();

        // tripId -> Map<CriteriaTracker -> tArr for this journey>
        Map<String, Map<FootpathsCountCriteriaTracker, Pair<Integer, Movement>>> T = new HashMap<>();

        // stopId -> footpath to dest
        Map<String, Footpath> D = new HashMap<>();

        // ### init data structure

        // NOTE: We don't initalize the default values for all stops/trips with
        // infinities as this is not necessary because the absence of a key in the
        // hashmap already represents the default value.

        // for all footpaths f with farr stop = target do D[x] ← fdur;
        stopIdToIncomingFootpaths.getOrDefault(pArrId, new ArrayList<>()).forEach(footpath -> {
            D.put(footpath.getPDep().getId(), footpath);
        });

        // for all stops x do S[x] ← {(∞, ∞)}
        stopIdToStop.forEach((stopId, stop) -> {
            S.put(stopId, new ProfileFunction<FootpathsCountCriteriaTracker>());
        });

        // for all trips x do T [x] ← ∞;
        tripIdToRoute.forEach((tripId, route) -> {
            T.put(tripId, new HashMap<FootpathsCountCriteriaTracker, Pair<Integer, Movement>>());
        });

        // ### Actual algorithm

        for (Connection c : connections.subList(getEarliestReachableConnectionIdx(tDep), connections.size())) {
            if (c.getPDep().getId().equals(pArrId)) {
                // avoid stupid loops, e.g. if our dest is D and the algorithm scans a
                // connection from S to D, without this "continue", it will consider the journey
                // that takes the connection and then walks back to D.
                System.out.println("continuing");
                continue;
            }

            System.out.println("------------------------------------------------------------------------");
            System.out.printf("scanning %s\n", c);
            Map<FootpathsCountCriteriaTracker, Pair<Integer, Movement>> tau1 = new HashMap<>();

            if (c.getPArr().getId().equals(pArrId)) { // no need to walk if we arrive directly at pArrId
                System.out.println("arrives at target directly");
                tau1 = Map.of(new FootpathsCountCriteriaTracker(0), new Pair<Integer, Movement>(c.getTArr(), c));
            } else {
                Footpath finalFootpath = D.get(c.getPArr().getId());
                if (finalFootpath != null) {
                    System.out.println("arrives at target with a final footpath");
                    int tArrWithfootpath = c.getTArr() + finalFootpath.getTravelTime();

                    tau1 = Map.of(new FootpathsCountCriteriaTracker(1),
                            new Pair<Integer, Movement>(tArrWithfootpath, c));

                    int foopathTDep = c.getTArr();

                    System.out.println("inserting final footpath in C.parr");

                    System.out.printf("C.parr 's profile before: %s\n", S.get(c.getPArr().getId()));

                    // insert the footpath in c.parr
                    S.get(c.getPArr().getId()).insert(foopathTDep,
                            new HashMap<>(Map.of(new FootpathsCountCriteriaTracker(1),
                                    new Pair<Integer, Movement>(tArrWithfootpath, finalFootpath))));

                    System.out.printf("C.parr 's profile after: %s\n", S.get(c.getPArr().getId()));
                }
            }

            System.out.printf("tau1: %s\n", tau1);

            // τ2 ← T [ctrip];
            Map<FootpathsCountCriteriaTracker, Pair<Integer, Movement>> tau2 = new HashMap<>();
            for (Map.Entry<FootpathsCountCriteriaTracker, Pair<Integer, Movement>> entry : T.get(c.getTripId())
                    .entrySet()) {
                int tArr = entry.getValue().getKey();
                tau2.put(entry.getKey(), new Pair<>(tArr, c)); // update the movement
            }

            System.out.printf("tau2: %s\n", tau2);

            // τ3 ← evaluate S[carr stop] at carr time;
            // TODO: consider a potential change of vehicle ?
            Map<FootpathsCountCriteriaTracker, Pair<Integer, Movement>> tau3 = new HashMap<>();
            for (Map.Entry<FootpathsCountCriteriaTracker, Pair<Integer, Movement>> entry : S.get(c.getPArr().getId())
                    .evaluateAt(c.getTArr())
                    .entrySet()) {
                int tArr = entry.getValue().getKey();
                int transfersCount = entry.getKey().getFootpathsCount();
                FootpathsCountCriteriaTracker transfersCountCriteriaTracker = new FootpathsCountCriteriaTracker(
                        transfersCount);
                tau3.put(transfersCountCriteriaTracker, new Pair<>(tArr, c)); // update the movement
            }

            System.out.printf("tau3: %s\n", tau3);

            // τc ← min{τ1, τ2, τ3};
            Map<FootpathsCountCriteriaTracker, Pair<Integer, Movement>> tauC = mergeMaps(List.of(tau1, tau2, tau3));

            System.out.printf("tauC: %s\n", tauC);

            T.put(c.getTripId(), tauC);

            System.out.printf("profile func before updating : %s\n", S.get(c.getPDep().getId()));

            S.get(c.getPDep().getId()).insert(c.getTDep(), tauC);

            System.out.printf("profile func after updating : %s\n", S.get(c.getPDep().getId()));

            for (Footpath f : stopIdToIncomingFootpaths.getOrDefault(c.getPDep().getId(), new ArrayList<>())) {
                int fTDep = c.getTDep() - f.getTravelTime();
                if (fTDep > tDep) {

                    Map<FootpathsCountCriteriaTracker, Pair<Integer, Movement>> map = new HashMap<>();
                    for (Map.Entry<FootpathsCountCriteriaTracker, Pair<Integer, Movement>> entry : S
                            .get(c.getPDep().getId())
                            .evaluateAt(c.getTDep()).entrySet()) {

                        int tArr = entry.getValue().getKey();

                        // one more footpath -> +1
                        map.put(new FootpathsCountCriteriaTracker(entry.getKey().getFootpathsCount() + 1),
                                new Pair<Integer, Movement>(tArr, f));

                    }

                    S.get(f.getPDep().getId()).insert(fTDep, map);
                } else {
                    System.out.printf("footpath skipped, fTDep=%s\n", TimeConversion.fromSeconds(fTDep));
                }
            }

            System.out.println();
        }

        System.out.println("prompting journey");
        FootpathsCountCriteriaTracker footpathsCountCriteriaTracker = promptJourney(S, pDepId, tDep);
        System.out.println("printing journey");
        diplayJourney(S, pDepId, pArrId, tDep, footpathsCountCriteriaTracker);
    }

    public void loadData(CsvSet... csvSets) throws IOException, CsvValidationException {
        // TODO: check that we reinitialize every member
        this.connections = new ArrayList<>();
        this.stopIdToStop = new HashMap<>();
        this.tripIdToRoute = new HashMap<>();
        this.stopIdToIncomingFootpaths = new HashMap<>();
        this.stopIdToOutgoingFootpaths = new HashMap<>();

        for (CsvSet csvSet : csvSets) {
            loadOneCsvSet(csvSet);
        }

        BallTree ballTree = new BallTree(new ArrayList<>(stopIdToStop.values()));
        double maxDistanceKm = Integer.MAX_VALUE; // TODO: replace by the actual value
        for (Stop sourceStop : stopIdToStop.values()) {

            List<Stop> nearbyStops = ballTree.findStopsWithinRadius(sourceStop, maxDistanceKm);

            for (Stop arrStop : nearbyStops) {
                if (!sourceStop.equals(arrStop)) {
                    Footpath footpath = new Footpath(sourceStop, arrStop);

                    stopIdToIncomingFootpaths
                            .computeIfAbsent(arrStop.getId(), k -> new ArrayList<>())
                            .add(footpath);
                }
            }
        }
    }

    private void loadOneCsvSet(CsvSet csvSet) throws IOException, CsvValidationException {
        // ------------------- stops.csv -------------------
        System.out.println("__stops.csv__");

        try (CSVReader reader = new CSVReader(new FileReader(csvSet.stopsCSV))) {
            String[] headers = reader.readNext(); // Read the header row
            if (headers == null) {
                throw new IllegalArgumentException("CSV file is empty or missing headers.");
            }

            // Map header names to their indices
            Map<String, Integer> headerMap = new HashMap<>();
            for (int i = 0; i < headers.length; i++) {
                headerMap.put(headers[i], i);
            }

            // Verify that required headers are present
            String[] requiredHeaders = { "stop_id", "stop_name", "stop_lat", "stop_lon" };
            for (String header : requiredHeaders) {
                if (!headerMap.containsKey(header)) {
                    throw new IllegalArgumentException("Missing required header: " + header);
                }
            }

            String[] line;
            while ((line = reader.readNext()) != null) {
                String stopId = line[headerMap.get("stop_id")];
                String stopName = line[headerMap.get("stop_name")];
                Double lat = Double.parseDouble(line[headerMap.get("stop_lat")]);
                Double lon = Double.parseDouble(line[headerMap.get("stop_lon")]);
                Coord coord = new Coord(lat, lon);
                stopIdToStop.put(stopId, new Stop(stopId, stopName, coord, csvSet.transportOperator));
            }
        }

        // ------------------- routes.csv ------------------
        System.out.println("__routes.csv__");

        HashMap<String, Route> routeIdToRoute = new HashMap<>();

        try (CSVReader reader = new CSVReader(new FileReader(csvSet.routesCSV))) {
            String[] headers = reader.readNext(); // Read the header row
            if (headers == null) {
                throw new IllegalArgumentException("CSV file is empty or missing headers.");
            }

            // Map header names to their indices
            Map<String, Integer> headerMap = new HashMap<>();
            for (int i = 0; i < headers.length; i++) {
                headerMap.put(headers[i], i);
            }

            // Verify that required headers are present
            String[] requiredHeaders = { "route_id", "route_short_name", "route_long_name", "route_type" };
            for (String header : requiredHeaders) {
                if (!headerMap.containsKey(header)) {
                    throw new IllegalArgumentException("Missing required header: " + header);
                }
            }

            String[] line;
            while ((line = reader.readNext()) != null) {
                String routeId = line[headerMap.get("route_id")];
                String routeShortName = line[headerMap.get("route_short_name")];
                String routeLongName = line[headerMap.get("route_long_name")];
                Route.RouteType routeType = RouteType.valueOf(line[headerMap.get("route_type")]);
                routeIdToRoute.put(routeId, new Route(routeId, routeShortName, routeLongName, routeType));
            }
        }

        // ----------------- trips.csv ----------------
        System.out.println("__trips.csv__");

        try (CSVReader reader = new CSVReader(new FileReader(csvSet.tripsCSV))) {
            String[] headers = reader.readNext(); // Read the header row
            if (headers == null) {
                throw new IllegalArgumentException("CSV file is empty or missing headers.");
            }

            // Map header names to their indices
            Map<String, Integer> headerMap = new HashMap<>();
            for (int i = 0; i < headers.length; i++) {
                headerMap.put(headers[i], i);
            }

            // Verify that required headers are present
            String[] requiredHeaders = { "trip_id", "route_id" };
            for (String header : requiredHeaders) {
                if (!headerMap.containsKey(header)) {
                    throw new IllegalArgumentException("Missing required header: " + header);
                }
            }

            String[] line;
            while ((line = reader.readNext()) != null) {
                String tripId = line[headerMap.get("trip_id")];
                String routeId = line[headerMap.get("route_id")];
                Route route = routeIdToRoute.get(routeId);
                tripIdToRoute.put(tripId, route);
            }
        }

        // ----------------- stop_times.csv ----------------
        System.out.println("__stop_times.csv__");

        try (CSVReader reader = new CSVReader(new FileReader(csvSet.stopTimesCSV))) {
            String[] headers = reader.readNext(); // Read the header row
            if (headers == null) {
                throw new IllegalArgumentException("CSV file is empty or missing headers.");
            }

            // Map header names to their indices
            Map<String, Integer> headerMap = new HashMap<>();
            for (int i = 0; i < headers.length; i++) {
                headerMap.put(headers[i], i);
            }

            // Verify that required headers are present
            String[] requiredHeaders = { "trip_id", "departure_time", "stop_id", "stop_sequence" };
            for (String header : requiredHeaders) {
                if (!headerMap.containsKey(header)) {
                    throw new IllegalArgumentException("Missing required header: " + header);
                }
            }

            // Step 1: group by trip_id
            Map<String, List<StopTimeEntry>> tripIdToStopTimes = new HashMap<>();

            String[] line;
            while ((line = reader.readNext()) != null) {
                String tripId = line[headerMap.get("trip_id")];
                int departureTime = TimeConversion.toSeconds(line[headerMap.get("departure_time")]);
                String stopId = line[headerMap.get("stop_id")];
                int stopSequence = Integer.parseInt(line[headerMap.get("stop_sequence")]);

                StopTimeEntry entry = new StopTimeEntry(tripId, departureTime, stopId, stopSequence);

                tripIdToStopTimes
                        .computeIfAbsent(tripId, k -> new ArrayList<>())
                        .add(entry);
            }

            for (List<StopTimeEntry> entries : tripIdToStopTimes.values()) {
                entries.sort(Comparator.comparingInt(e -> e.stopSequence)); // sort by stop_sequence

                for (int i = 0; i < entries.size() - 1; i++) {
                    StopTimeEntry from = entries.get(i);
                    StopTimeEntry to = entries.get(i + 1);

                    Connection connection = new Connection(
                            from.tripId,
                            stopIdToStop.get(from.stopId),
                            stopIdToStop.get(to.stopId),
                            from.departureTime,
                            to.departureTime);
                    connections.add(connection);
                }
            }

        }

        // sort by decreasing departure time
        connections.sort(Comparator.comparingInt(Connection::getTDep).reversed());
    }

}
