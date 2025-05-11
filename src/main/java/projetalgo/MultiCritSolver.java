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
import java.util.function.Supplier;
import java.util.stream.Collectors;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;

import javafx.util.Pair;
import projetalgo.Route.RouteType;

public class MultiCritSolver<T extends CriteriaTracker> {
    private final Supplier<T> factory;
    private HashMap<String, Stop> stopIdToStop;
    private HashMap<String, Route> tripIdToRoute;
    private HashMap<String, List<Footpath>> stopIdToIncomingFootpaths;
    private HashMap<String, List<Footpath>> stopIdToOutgoingFootpaths;
    private List<Connection> connections;

    private static final List<Footpath> EMPTY_FOOTPATH_LIST = List.of();

    public MultiCritSolver(Supplier<T> factory) {
        this.factory = factory;
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

    private void diplayJourney(Map<String, ProfileFunction<T>> F, String pDepId, String pArrId, int tDep,
            T criteriaTracker) {

        String stopId = pDepId;
        String prevTripId = null;

        while (!stopId.equals(pArrId)) {
            Movement movement = F.get(stopId).getFirstMatch(tDep, criteriaTracker);
            System.out.printf("taking %s\n", movement);

            stopId = movement.getPArr().getId();

            if (movement instanceof Footpath footpath) {
                criteriaTracker.decFootpathsCount();
                criteriaTracker.decTransfersCount();

                tDep += footpath.getTravelTime();

                prevTripId = null; // means footpath
            } else if (movement instanceof Connection connection) {
                tDep = connection.getTArr();

                // previously was on a footpath or changed trip
                if (prevTripId == null || !connection.getTripId().equals(prevTripId)) {
                    criteriaTracker.decTransfersCount();
                }

                prevTripId = connection.getTripId();
            }
        }
    }

    T promptJourney(Map<String, ProfileFunction<T>> F, String pDepId, int tDep) {
        Map<T, Pair<Integer, Movement>> results = F.get(pDepId).evaluateAt(tDep);

        // find journeys dominated by other journeys that we can take
        // NOTE: Until now, there could be journeys that were dominated by other
        // journeys leaving earlier, but we could not remove the ones leaving later
        // because we didn't know whether we would be able to arrive on time to catch
        // the ones leaving earlier.
        // But since now, we know that we can catch those leaving at tdep, we can remove
        // all the journeys dominated by other journeys leaving at/after tdep.
        ArrayList<T> dominatedResults = new ArrayList<>();
        for (Map.Entry<T, Pair<Integer, Movement>> entry0 : results.entrySet()) {
            for (Map.Entry<T, Pair<Integer, Movement>> entry1 : results.entrySet()) {

                T entry0Criteria = entry0.getKey();
                int entry0TArr = entry0.getValue().getKey();
                T entry1Criteria = entry1.getKey();
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

        List<T> options = new ArrayList<>(results.keySet());

        System.out.println("Possible journeys:");
        for (int i = 0; i < options.size(); i++) {
            T tracker = options.get(i);
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

    private void updateTauC(Map<T, Pair<Integer, Movement>> tauC, T criteriaTracker,
            Pair<Integer, Movement> tArrMovement) {

        Pair<Integer, Movement> pairCurrentlyAtKey = tauC.get(criteriaTracker);
        if (pairCurrentlyAtKey == null) {
            tauC.put(criteriaTracker, tArrMovement);
        } else { // there is already sth for that CriteriaTracker -> update only if it improves
                 // TArr
            int entryTArr = tArrMovement.getKey();
            int pairCurrentlyAtKeyTArr = pairCurrentlyAtKey.getKey();

            if (entryTArr < pairCurrentlyAtKeyTArr) {
                tauC.put(criteriaTracker, tArrMovement);
            }
        }
    }

    /**
     * Connections must be sorted by their departure time.
     */
    public void solve(String pDepId, String pArrId, int tDep) {
        // stopId -> ProfileFunction
        Map<String, ProfileFunction<T>> S = new HashMap<>();

        // tripId -> Map<CriteriaTracker -> tArr for this journey>
        Map<String, Map<T, Pair<Integer, Movement>>> T = new HashMap<>();

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
            S.put(stopId, new ProfileFunction<T>());
        });

        // for all trips x do T [x] ← ∞;
        tripIdToRoute.forEach((tripId, route) -> {
            T.put(tripId, new HashMap<T, Pair<Integer, Movement>>());
        });

        // ### Actual algorithm

        for (Connection c : connections.subList(getEarliestReachableConnectionIdx(tDep), connections.size())) {
            System.out.printf("__________________scanning %s__________________\n", c);

            if (c.getPDep().getId().equals(pArrId)) {
                // avoid stupid loops, e.g. if our dest is D and the algorithm scans a
                // connection from S to D, without this "continue", it will consider the journey
                // that takes the connection and then walks back to D.
                continue;
            }

            // τc ← min{τ1, τ2, τ3};
            Map<T, Pair<Integer, Movement>> tauC = new HashMap<>();

            ProfileFunction<T> sCPArr = S.get(c.getPArr().getId());

            // τ1
            if (c.getPArr().getId().equals(pArrId)) { // no need to walk if we arrive directly at pArrId
                T newTracker = factory.get();
                int tArr = c.getTArr();

                System.out.printf("newtracker tau1: %s -> tarr: %s\n", newTracker, tArr);

                updateTauC(tauC, newTracker, new Pair<Integer, Movement>(tArr, c));
            } else {
                Footpath finalFootpath = D.get(c.getPArr().getId());
                if (finalFootpath != null) {
                    int tArrWithfootpath = c.getTArr() + finalFootpath.getTravelTime();

                    T newTracker = factory.get();
                    newTracker.setFootpathsCount(1);
                    newTracker.setTransfersCount(1);

                    System.out.printf("newtracker tau1: %s -> tarr: %s\n", newTracker, tArrWithfootpath);

                    updateTauC(tauC, newTracker, new Pair<Integer, Movement>(tArrWithfootpath, c));

                    int foopathTDep = c.getTArr();

                    T finalFootpathNewTracker = factory.get();
                    finalFootpathNewTracker.setFootpathsCount(1);
                    finalFootpathNewTracker.setTransfersCount(0);

                    // insert the footpath in c.parr
                    sCPArr.insert(foopathTDep,
                            new HashMap<>(Map.of(finalFootpathNewTracker,
                                    new Pair<Integer, Movement>(tArrWithfootpath, finalFootpath))));
                }
            }

            // τ2 ← T [ctrip];
            for (Map.Entry<T, Pair<Integer, Movement>> entry : T.get(c.getTripId())
                    .entrySet()) {

                System.out.printf("tau2 based on %s\n", entry);

                int tArr = entry.getValue().getKey();

                int footpathsCount = entry.getKey().getFootpathsCount();
                int transfersCount = entry.getKey().getTransfersCount();
                T newTracker = factory.get();
                newTracker.setFootpathsCount(footpathsCount);
                newTracker.setFootpathsCount(transfersCount);

                System.out.printf("newtracker tau2: %s -> tarr: %s\n", newTracker, tArr);

                updateTauC(tauC, newTracker, new Pair<>(tArr, c));
            }

            // τ3 ← evaluate S[carr stop] at carr time;
            // TODO: consider a potential change of vehicle ?
            for (Map.Entry<T, Pair<Integer, Movement>> entry : sCPArr
                    .evaluateAt(c.getTArr()).entrySet()) {
                int tArr = entry.getValue().getKey();

                int footpathsCount = entry.getKey().getFootpathsCount();
                int transfersCount = entry.getKey().getTransfersCount() + 1;
                T newTracker = factory.get();
                newTracker.setFootpathsCount(footpathsCount);
                newTracker.setTransfersCount(transfersCount);

                System.out.printf("newtracker tau3: %s -> tarr: %s\n", newTracker, tArr);

                updateTauC(tauC, newTracker, new Pair<>(tArr, c));
            }

            System.out.printf("inserted tauC in %s: %s\n", c.getTripId(), tauC);

            // insert a copy of tauC into T[ctrip]
            Map<T, Pair<Integer, Movement>> copyOfTauC = new HashMap<>();
            tauC.forEach((tracker, pairtArrMovement) -> {
                T newTracker = factory.get();
                newTracker.setFootpathsCount(tracker.getFootpathsCount());
                newTracker.setTransfersCount(tracker.getTransfersCount());

                int tArr = pairtArrMovement.getKey();
                Movement movement = pairtArrMovement.getValue();

                copyOfTauC.put(newTracker, new Pair<Integer, Movement>(tArr, movement));
            });
            T.put(c.getTripId(), copyOfTauC);

            boolean atLeastOneNotDominated = S.get(c.getPDep().getId()).insert(c.getTDep(), tauC);

            // Propagate into incoming footpaths only if at least one entry from tauC was
            // inserted actually inserted (not dominated) in c.pDep.
            // (A partial journey being dominated in c.pDep implies it is also dominated in
            // incoming footpaths of c.pDep).
            if (atLeastOneNotDominated) {
                // in c.PDep as they would also be dominated in incomin footpaths.
                Map<T, Pair<Integer, Movement>> sCPDepEvaluatedAtCTDep = S
                        .get(c.getPDep().getId())
                        .evaluateAt(c.getTDep());

                for (Footpath f : stopIdToIncomingFootpaths.getOrDefault(c.getPDep().getId(), EMPTY_FOOTPATH_LIST)) {
                    int fTDep = c.getTDep() - f.getTravelTime();
                    if (fTDep > tDep) {

                        // TODO: Choose between this and code below (does the same, but
                        // functional/imperative)
                        //
                        // Map<FootpathsCountCriteriaTracker, Pair<Integer, Movement>> map = new
                        // HashMap<>();
                        // for (Map.Entry<FootpathsCountCriteriaTracker, Pair<Integer, Movement>> entry
                        // : sCPDepEvaluatedAtCTDep
                        // .entrySet()) {
                        //
                        // int tArr = entry.getValue().getKey();
                        //
                        // // one more footpath -> +1
                        // map.put(new FootpathsCountCriteriaTracker(entry.getKey().getFootpathsCount()
                        // + 1),
                        // new Pair<Integer, Movement>(tArr, f));
                        // }

                        Map<T, Pair<Integer, Movement>> map = sCPDepEvaluatedAtCTDep
                                .entrySet()
                                .stream()
                                .collect(Collectors.toMap(
                                        e -> {
                                            T newTracker = factory.get();
                                            newTracker.setFootpathsCount(e.getKey().getFootpathsCount() + 1);
                                            newTracker.setTransfersCount(e.getKey().getTransfersCount() + 1);
                                            return newTracker;
                                        },
                                        e -> new Pair<>(e.getValue().getKey(), f)));

                        S.get(f.getPDep().getId()).insert(fTDep, map);
                    }
                }
            }

            stopIdToStop.forEach((stopId, stop) -> {
                System.out.printf("%s profile: %s\n", stopId, S.get(stopId));
            });

            System.out.printf("T: %s\n", T);

        }

        System.out.println("prompting journey");
        T footpathsCountCriteriaTracker = promptJourney(S, pDepId, tDep);
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

                    System.out.printf("adding footpath: %s distance: \n", footpath, footpath.getDistance());

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
