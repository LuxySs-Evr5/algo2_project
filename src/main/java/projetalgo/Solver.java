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
        Stop tripStartStop = null;
        Stop previousStop = null;

        while (!finalPath.isEmpty()) {
            BestKnownEntry entry = finalPath.pop();
            Movement movement = entry.getMovement();
            Stop pDep = movement.getPDep();
            Stop pArr = movement.getPArr();

            if (movement instanceof Footpath) {
                if (currentTripId != null) {
                    System.out.println("Take trip " + currentTripId + " from " +
                            tripStartStop.getName() + " to "
                            + previousStop.getName());
                    currentTripId = null;
                    tripStartStop = null;
                }
                System.out.println("Walk from " + pDep.getName() + " to " + pArr.getName());
            } else if (movement instanceof Connection connection) {
                String tripId = connection.getTripId();
                if (currentTripId == null) {
                    currentTripId = tripId;
                    tripStartStop = pDep;
                } else if (!tripId.equals(currentTripId)) {
                    System.out.println("Take trip " + currentTripId + " from " +
                            tripStartStop.getName() + " to "
                            + previousStop.getName());
                    currentTripId = tripId;
                    tripStartStop = pDep;
                }
                // Same trip -> continue
            }

            previousStop = pArr;
        }

        if (currentTripId != null) {
            System.out.println("Take trip " + currentTripId + " from " +
                    tripStartStop.getName() + " to "
                    + previousStop.getName());
        }
    }

    /**
     * Returns true if the given connection's arrival time is after the
     * earliest arrival time to one of pArrIds.
     *
     * TODO: check this again
     *
     * NOTE: in the paper "Intriguingly Simple and Fast Transit Routing?" by
     * Julian Dibbelt, Thomas Pajor, Ben Strasser, and Dorothea Wagner,
     * it is stated:
     *
     * "if we are only interested in one-to-one queries, the algorithm may stop as
     * soon as it scans a connection whose departure time exceeds the target stop’s
     * earliest arrival time."
     *
     * Although this is true, we can actually stop as soon as the connection's
     * arrival time exceeds the best known arrival time to the destination (which
     * occurs a tiny bit sooner than checking the departure time).
     */
    boolean checkConnectionTArrAfterEarliestTArr(Map<String, BestKnownEntry> bestKnown, Connection c,
            List<String> pArrIds) {
        for (String pArrId : pArrIds) {
            if (c.getTArr() >= getBestKnownArrivalTime(bestKnown, pArrId)) {
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

        if (pArrIds.size() == 0) { // no pArrId not found
            System.out.println("invalid destination");
        }

        for (Connection c : filteredConnections) {
            if (checkConnectionTArrAfterEarliestTArr(bestKnown, c, pArrIds)) {
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
                        boolean fpIsFaster = footpathTArr < getBestKnownArrivalTime(bestKnown, footpathPDep.getId());
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

        System.out.printf("pArr: %s, sec = %d (%s)\n", stopIdToStop.get(pArrIdEarliest).getName(), tArrEarliest,
                TimeConversion.fromSeconds(tArrEarliest));

        Stack<BestKnownEntry> finalPath = reconstructSolution(bestKnown, pDepIds, pArrIdEarliest);
        printInstructions(finalPath);
    }

    public void loadData(CsvSet... csvSets) throws IOException, CsvValidationException {
        stopIdToStop = new HashMap<>();
        stopIdToOutgoingFootpaths = new HashMap<>();
        connections = new ArrayList<>();

        for (CsvSet csvSet : csvSets) {
            loadOneCsvSet(csvSet);
        }


        // generate all paths
        // TODO: Use Ball Tree here
        // for (Stop stop0 : stopIdToStop.values()) {
        // for (Stop stop1 : stopIdToStop.values()) {
        // if (stop0 != stop1) {
        // Footpath footpath = new Footpath(stop0, stop1);
        //
        // // TODO: remove magic number 5
        // if (footpath.getDistance() <= 5) {
        // stopIdToFootpaths
        // .computeIfAbsent(stop0.getId(), k -> new ArrayList<>())
        // .add(footpath);
        // }
        //
        // }
        // }
        // }
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
                String stopName = line[headerMap.get("stop_name")];
                Double lat = Double.parseDouble(line[headerMap.get("stop_lat")]);
                Double lon = Double.parseDouble(line[headerMap.get("stop_lon")]);
                Coord coord = new Coord(lat, lon);
                stopIdToStop.put(stopId, new Stop(stopId, stopName, coord));
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

        // Final sort by departure time
        connections.sort(Comparator.comparingInt(Connection::getTDep));
    }

}
