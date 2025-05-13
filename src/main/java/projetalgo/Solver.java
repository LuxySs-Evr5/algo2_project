package projetalgo;

import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;

public class Solver {

    private HashMap<String, Stop> stopIdToStop;
    private HashMap<String, List<Footpath>> stopIdToOutgoingFootpaths;
    private List<Connection> connections;

    public Solver() {
        this.connections = new ArrayList<>();
        this.stopIdToStop = new HashMap<>();
        this.stopIdToOutgoingFootpaths = new HashMap<>();
    }

    /**
     * Returns true if the stop with the given name exists in the data.
     */
    boolean stopExists(final String name) {
        for (Stop stop : stopIdToStop.values()) {
            if (stop.getName().equalsIgnoreCase(name)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns string representation of the duration in minutes and seconds.
     */
    private String formatDuration(final int seconds) {
        int minutes = seconds / 60;
        int remainingSeconds = seconds % 60;
        if (minutes > 0) {
            return minutes + " min" + (remainingSeconds > 0 ? " " + remainingSeconds + " sec" : "");
        } else {
            return remainingSeconds + " sec";
        }
    }
    
    /**
     * Returns the index of the first connection departing at or after our departure
     * time.
     */
    int getEarliestReachableConnectionIdx(int tDep) {

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

    /**
     * Returns the list of connections departing at or after our depature time.
     */
    List<Connection> getFilteredConnections(int tDep) {
        return connections.subList(getEarliestReachableConnectionIdx(tDep),
                connections.size());
    }

    /**
     * Returns the earliest known arrival time at stopId, or Integer.MAX_VALUE if
     * unknown.
     */
    private static int getBestKnownArrivalTime(Map<String, BestKnownEntry> bestKnown, String stopId) {
        BestKnownEntry entry = bestKnown.get(stopId);
        return (entry != null) ? entry.getTArr() : Integer.MAX_VALUE;
    }

    /**
     * Returns the stopId of the arrival stop we arrive at earliest.
     */
    private static String findPArrIdEarliest(Map<String, BestKnownEntry> bestKnown, List<String> pArrIds) {
        int tArrEarliest = Integer.MAX_VALUE;
        String pArrIdEarliest = null;

        for (String pArrId : pArrIds) {
            int tArr = getBestKnownArrivalTime(bestKnown, pArrId);

            if (tArr < tArrEarliest) {
                pArrIdEarliest = pArrId;
                tArrEarliest = tArr;
            }
        }

        return pArrIdEarliest;
    }

    /**
     * Reconstructs and prints the path from the earliest arrival stop back to a
     * departure stop.
     *
     * Traverses the path backwards using the best known entries, storing movements
     * in a stack to reverse the order. Then replays the path forward from the
     * departure.
     */
    Stack<BestKnownEntry> reconstructSolution(Map<String, BestKnownEntry> bestKnown, List<String> pDepIds,
            String pArrIdEarliest) {
        // Reconstruct the solution backwards (from pArr to one of pDeps)
        // TODO: path isn't a good name because (could be confused with footpath)
        Stack<BestKnownEntry> finalPath = new Stack<>();
        String currentStopId = pArrIdEarliest;
        while (!pDepIds.contains(currentStopId)) {
            BestKnownEntry currentEntry = bestKnown.get(currentStopId);
            if (currentEntry == null) {
                throw new IllegalStateException("No path found to a departure stop from: " + currentStopId);
            }

            finalPath.push(currentEntry);

            currentStopId = currentEntry.getMovement()
                    .getPDep().getId();
        }

        return finalPath;
    }

    public void printInstructions(Stack<BestKnownEntry> finalPath) {
        String currentTripId = null;
        RouteInfo currentRouteInfo = null;
        Stop tripStartStop = null;
        Stop previousStop = null;
        int departureTime = -1;

        while (!finalPath.isEmpty()) {
            BestKnownEntry entry = finalPath.pop();
            Movement movement = entry.getMovement();
            Stop pDep = movement.getPDep();
            Stop pArr = movement.getPArr();

            if (movement instanceof Footpath footpath) {
                if (currentTripId != null) {
                    if (tripStartStop != null && previousStop != null && currentRouteInfo != null) {
                        String depTimeStr = TimeConversion.fromSeconds(departureTime);
                        System.out.println("Take " + currentRouteInfo.toString() + " from " 
                            + tripStartStop.getName() + " at " + depTimeStr + " to " + previousStop.getName());
                    }
                    currentTripId = null;
                    currentRouteInfo = null;
                    tripStartStop = null;
                    departureTime = -1;
                }
                int travelTime = footpath.getTravelTime();
                String duration = formatDuration(travelTime);
                System.out.println("Walk " + duration + " from " + pDep.getName() + " to " + pArr.getName());
            } else if (movement instanceof Connection connection) {
                String tripId = connection.getTripId();
                RouteInfo routeInfo = connection.getRouteInfo();
                if (currentTripId == null) {
                    currentTripId = tripId;
                    currentRouteInfo = routeInfo;
                    tripStartStop = pDep;
                    departureTime = connection.getTDep();
                } else if (!tripId.equals(currentTripId)) {
                    if (tripStartStop != null && previousStop != null && currentRouteInfo != null) {
                        String depTimeStr = TimeConversion.fromSeconds(departureTime);
                        System.out.println("Take " + currentRouteInfo.toString() + " from " 
                            + tripStartStop.getName() + " at " + depTimeStr + " to " + previousStop.getName());
                    }
                    currentTripId = tripId;
                    currentRouteInfo = routeInfo;
                    tripStartStop = pDep;
                    departureTime = connection.getTDep();
                }
                // Same trip -> continue
            }

            previousStop = pArr;
        }

        if (currentTripId != null) {
            if (tripStartStop != null && previousStop != null && currentRouteInfo != null) {
                String depTimeStr = TimeConversion.fromSeconds(departureTime);
                System.out.println("Take " + currentRouteInfo.toString() + " from " 
                    + tripStartStop.getName() + " at " + depTimeStr + " to " + previousStop.getName());
            }            
        }
    }

    /**
     * Returns true if the given connection's arrival time is after the
     * earliest arrival time to one of pArrIds.
     *
     * NOTE: in the paper "Intriguingly Simple and Fast Transit Routing?" by
     * Julian Dibbelt, Thomas Pajor, Ben Strasser, and Dorothea Wagner,
     * it is stated:
     * "if we are only interested in one-to-one queries, the algorithm may stop as
     * soon as it scans a connection whose departure time exceeds the target stop’s
     * earliest arrival time."
     */
    boolean checkConnectionTdepAfterEarliestTArr(Map<String, BestKnownEntry> bestKnown, Connection c, List<String> pArrIds) {
        for (String pArrId : pArrIds) {
            if (c.getTDep() >= getBestKnownArrivalTime(bestKnown, pArrId)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Connections must be sorted by their departure time.
     */
    public void solve(String pDepName, String pArrName, int tDep) {
        List<Connection> filteredConnections = getFilteredConnections(tDep);

        // NOTE: could use a List<Stops> to iterate and get the id, no need for a
        // hashmap

        // All stopIds that match pDepName
        List<String> pDepIds = new ArrayList<>();

        // All stopIds that match pArrName
        List<String> pArrIds = new ArrayList<>();

        // Not in hashmap means infinity
        Map<String, BestKnownEntry> bestKnown = new HashMap<>();

        for (Map.Entry<String, Stop> entry : stopIdToStop.entrySet()) {
            String keyStopId = entry.getKey();
            Stop valueStop = entry.getValue();

            if (valueStop.getName().equals(pArrName)) {
                pArrIds.add(keyStopId);
                System.out.printf("found a pArr: %s : %s\n", pArrName, keyStopId);
            }

            if (valueStop.getName().equals(pDepName)) {
                System.out.printf("found a pDep: %s : %s\n", valueStop.getName(), keyStopId);

                pDepIds.add(keyStopId);

                // The time to get to pDep is tDep because we are already there
                bestKnown.put(keyStopId, new BestKnownEntry(tDep, null));

                // Footpaths initial setup
                List<Footpath> footpathsFromPDep = stopIdToOutgoingFootpaths.get(keyStopId);
                if (footpathsFromPDep != null) {
                    for (Footpath f : stopIdToOutgoingFootpaths.get(keyStopId)) {
                        bestKnown.put(f.getPArr().getId(),
                                new BestKnownEntry(tDep + f.getTravelTime(), f));
                    }
                }
            }
        }

        if (pArrIds.isEmpty()) { // no pArrId not found
            System.out.println("invalid destination");
        }

        for (Connection c : filteredConnections) {
            if (checkConnectionTdepAfterEarliestTArr(bestKnown, c, pArrIds)) {
                break;
            }

            // τ (pdep(c)) ≤ τdep(c).
            boolean cIsReachable = getBestKnownArrivalTime(bestKnown, c.getPDep().getId()) <= c.getTDep();

            // τarr(c) < τ (parr(c))
            boolean cIsFaster = c.getTArr() < getBestKnownArrivalTime(bestKnown, c.getPArr().getId());

            if (cIsReachable && cIsFaster) {
                bestKnown.put(c.getPArr().getId(), new BestKnownEntry(c.getTArr(), c));

                List<Footpath> footpathsFromCPArr = stopIdToOutgoingFootpaths.get(c.getPArr().getId());
                if (footpathsFromCPArr != null) {
                    Stop footpathPDep = c.getPArr();

                    for (Footpath f : footpathsFromCPArr) {
                        Stop footpathPArr = f.getPArr();

                        int footpathTArr = getBestKnownArrivalTime(bestKnown, footpathPDep.getId()) + f.getTravelTime();
                        boolean fpIsFaster = footpathTArr < getBestKnownArrivalTime(bestKnown, footpathPArr.getId());
                        if (fpIsFaster)
                            bestKnown.put(footpathPArr.getId(), new BestKnownEntry(footpathTArr, f));
                    }
                }
            }
        }

        String pArrIdEarliest = findPArrIdEarliest(bestKnown, pArrIds);
        if (pArrIdEarliest == null) {
            System.out.println("unreachable target");
            return;
        }

        int tArrEarliest = bestKnown.get(pArrIdEarliest).getTArr();

        Stack<BestKnownEntry> finalPath = reconstructSolution(bestKnown, pDepIds, pArrIdEarliest);
        printInstructions(finalPath);

        System.out.println(AinsiCode.BOLD + AinsiCode.RED + "You will arrive at " + stopIdToStop.get(pArrIdEarliest).getName() + " at " + TimeConversion.fromSeconds(tArrEarliest) + AinsiCode.RESET);
    }

    public void loadData(CsvSet... csvSets) throws IOException, CsvValidationException {
        // TODO: check that we reinitialize every member
        stopIdToStop = new HashMap<>();
        stopIdToOutgoingFootpaths = new HashMap<>();
        connections = new ArrayList<>();

        for (CsvSet csvSet : csvSets) {
            loadOneCsvSet(csvSet);
        }

        // Final sort by departure time
        connections.sort(Comparator.comparingInt(Connection::getTDep));

        // generate all paths
        BallTree ballTree = new BallTree(new ArrayList<>(stopIdToStop.values()));
        double maxDistanceKm = 0.5;
        for (Stop sourceStop : stopIdToStop.values()) {
            List<Stop> nearbyStops = ballTree.findStopsWithinRadius(sourceStop, maxDistanceKm);

            for (Stop arrStop : nearbyStops) {
                if (!sourceStop.equals(arrStop)) {
                    Footpath footpath = new Footpath(sourceStop, arrStop);

                    stopIdToOutgoingFootpaths
                        .computeIfAbsent(sourceStop.getId(), k -> new ArrayList<>())
                        .add(footpath);
                }
            }
        }
    }

    private void loadOneCsvSet(CsvSet csvSet) throws IOException, CsvValidationException {
        // ------------------- stops.csv -------------------

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
                String stopName = line[headerMap.get("stop_name")].toLowerCase();
                Coord coord = new Coord(Double.parseDouble(line[headerMap.get("stop_lat")]), Double.parseDouble(line[headerMap.get("stop_lon")]));
                stopIdToStop.put(stopId, new Stop(stopId, stopName, coord));
            }
        }

        // ------------------- trips.csv -------------------

        final Map<String, String> tripIdToRouteId = new HashMap<>();

        try (CSVReader reader = new CSVReader(new FileReader(csvSet.tripsCSV))) {
            String[] headers = reader.readNext();
            if (headers == null) {
                throw new IllegalArgumentException("trips.csv is empty or missing headers.");
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
                tripIdToRouteId.put(tripId, routeId);
            }
        }

        // ------------------- routes.csv -------------------

        final Map<String, RouteInfo> routeIdToRouteInfo = new HashMap<>();

        try (CSVReader reader = new CSVReader(new FileReader(csvSet.routesCSV))) {
            String[] headers = reader.readNext();
            if (headers == null) {
                throw new IllegalArgumentException("routes.csv is empty or missing headers.");
            }

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
                String routeLongName  = line[headerMap.get("route_long_name")];
                TransportType transportType = TransportType.valueOf(line[headerMap.get("route_type")]);

                RouteInfo routeInfo = new RouteInfo(routeShortName, routeLongName, transportType);
                routeIdToRouteInfo.put(routeId, routeInfo);
            }
        }

        // ----------------- stop_times.csv -----------------

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

                    RouteInfo routeInfo = routeIdToRouteInfo.get(tripIdToRouteId.get(from.tripId));
                    if (routeInfo == null) {
                        System.err.println("Missing route info for trip_id: " + from.tripId);
                        continue;
                    }

                    Connection connection = new Connection(
                            from.tripId,
                            routeInfo,
                            stopIdToStop.get(from.stopId),
                            stopIdToStop.get(to.stopId),
                            from.departureTime,
                            to.departureTime);
                    connections.add(connection);
                }
            }

        }
    }

}
