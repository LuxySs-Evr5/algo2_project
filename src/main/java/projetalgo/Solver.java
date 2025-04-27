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
    private HashMap<String, List<Footpath>> stopIdToFootpaths;
    private List<Connection> connections;

    public Solver() {
        this.connections = new ArrayList<>();
        this.stopIdToFootpaths = new HashMap<>();
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
        return connections.subList(
                getEarliestReachableConnectionIdx(tDep),
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
    void reconstructSolution(Map<String, BestKnownEntry> bestKnown, List<String> pDepIds, String pArrIdEarliest) {
        // Reconstruct the solution backwards (from pArr to one of pDeps)
        // TODO: path isn't a good name because (could be confused with footpath)
        Stack<BestKnownEntry> finalPath = new Stack<>();
        String currentStopId = pArrIdEarliest;
        while (!pDepIds.contains(currentStopId)) {
            BestKnownEntry currentEntry = bestKnown.get(currentStopId);
            finalPath.push(currentEntry);

            String otherStopId = currentEntry.getMovement()
                    .getPDep().getId();

            System.out.printf("otherStopId: %s \n", otherStopId);

            currentStopId = otherStopId;
        }

        // Pop the stack to replay the path forward.
        // because currentStopId = pDepId here (see above)
        System.out.printf("dep stop: %s\n", currentStopId);
        while (!finalPath.isEmpty()) {
            BestKnownEntry entry = finalPath.pop();
            currentStopId = entry.getMovement().getPArr().getId();
            System.out.printf("next stop: %s\n", currentStopId);
        }
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
                List<Footpath> footpathsFromPDep = stopIdToFootpaths.get(keyStopId);
                if (footpathsFromPDep != null) {
                    for (Footpath f : stopIdToFootpaths.get(keyStopId)) {
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
            // τ (pdep(c)) ≤ τdep(c).
            boolean cIsReachable = getBestKnownArrivalTime(bestKnown, c.getPDep().getId()) <= c.getTDep();

            // τarr(c) < τ (parr(c))
            boolean cIsFaster = c.getTArr() < getBestKnownArrivalTime(bestKnown, c.getPArr().getId());

            if (cIsReachable && cIsFaster) {
                bestKnown.put(c.getPArr().getId(), new BestKnownEntry(c.getTArr(), c));

                List<Footpath> footpathsFromCPArr = stopIdToFootpaths.get(c.getPArr().getId());
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

        // TODO: call here
        String pArrIdEarliest = findPArrIdEarliest(bestKnown, pArrIds);
        if (pArrIdEarliest == null) {
            System.out.println("unreachable target");
            return;
        }

        int tArrEarliest = bestKnown.get(pArrIdEarliest).getTArr();

        System.out.printf("pArr: %s, sec = %d (%s)\n", stopIdToStop.get(pArrIdEarliest).getName(), tArrEarliest,
                TimeConversion.fromSeconds(tArrEarliest));

        reconstructSolution(bestKnown, pDepIds, pArrIdEarliest);
    }

    public List<List<String>> csvToMatrix(String csvPath) throws IOException, CsvValidationException {
        List<List<String>> mat = new ArrayList<>();

        try (CSVReader reader = new CSVReader(new FileReader(csvPath))) {
            String[] line;
            while ((line = reader.readNext()) != null) {
                mat.add(List.of(line));
            }
        }

        return mat;
    }

    public void loadData(String routesCSV, String stopTimesCSV, String stopsCSV, String tripsCSV)
            throws IOException, CsvValidationException {

        // ------------------- stops.csv -------------------

        List<List<String>> parsedStopsCSV = csvToMatrix(stopsCSV);
        stopIdToStop = new HashMap<String, Stop>();

        for (int i = 1; i < parsedStopsCSV.size(); i++) {
            List<String> stopRow = parsedStopsCSV.get(i);
            String stopId = stopRow.get(0);
            String stopName = stopRow.get(1);
            Double lat = Double.parseDouble(stopRow.get(2));
            Double lon = Double.parseDouble(stopRow.get(3));
            Coord coord = new Coord(lat, lon);
            stopIdToStop.put(stopId, new Stop(stopId, stopName, coord));
        }

        List<Stop> stops = new ArrayList<>(stopIdToStop.values());

        // generate all paths
        // TODO: Use Ball Tree here
        for (int i = 0; i < stops.size(); i++) {
            for (int j = 0; j < stops.size(); j++) {
                Footpath footpath = new Footpath(stops.get(i), stops.get(j));

                stopIdToFootpaths
                        .computeIfAbsent(stops.get(i).getId(), k -> new ArrayList<>())
                        .add(footpath);

            }
        }

        // ----------------- stop_times.csv -----------------

        List<List<String>> stopTimes = csvToMatrix(stopTimesCSV);

        // Step 1: group by trip_id
        Map<String, List<StopTimeEntry>> tripIdToStopTimes = new HashMap<>();

        for (int i = 1; i < stopTimes.size(); i++) {
            List<String> row = stopTimes.get(i);
            String tripId = row.get(0);
            int departureTime = TimeConversion.toSeconds(row.get(1));
            String stopId = row.get(2);
            int stopSequence = Integer.parseInt(row.get(3));

            StopTimeEntry entry = new StopTimeEntry(tripId, departureTime, stopId, stopSequence);

            tripIdToStopTimes
                    .computeIfAbsent(tripId, k -> new ArrayList<>())
                    .add(entry);
        }

        // Step 2: create Connections by trip
        connections = new ArrayList<>();
        int connectionId = 0;

        for (List<StopTimeEntry> entries : tripIdToStopTimes.values()) {
            entries.sort(Comparator.comparingInt(e -> e.stopSequence)); // sort by stop_sequence

            for (int i = 0; i < entries.size() - 1; i++) {
                StopTimeEntry from = entries.get(i);
                StopTimeEntry to = entries.get(i + 1);

                Connection connection = new Connection(
                        connectionId++,
                        stopIdToStop.get(from.stopId),
                        stopIdToStop.get(to.stopId),
                        from.departureTime,
                        to.departureTime);
                connections.add(connection);
            }
        }

        // Final sort by departure time
        connections.sort(Comparator.comparingInt(Connection::getTDep));
    }

}
