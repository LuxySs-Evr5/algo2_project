package projetalgo;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Solver {

    private HashMap<String, List<Footpath>> stopIdToFootpaths;
    private List<Connection> connections;
    private List<Footpath> footpaths; // TODO: is that useful ? probably won't be.

    public static final String COMMA_DELIMITER = ",";

    public Solver() {
        this.connections = new ArrayList<>();
        this.footpaths = new ArrayList<>();
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
     * Connections must be sorted by their departure time.
     */
    public void solve(String pDepId, String pArrId, int tDep) {
        List<Connection> filteredConnections = getFilteredConnections(tDep);

        // Not in hashmap means infinity
        Map<String, Integer> bestKnown = new HashMap<>();

        // The time to get to pDep is tDep because we are already there
        bestKnown.put(pDepId, tDep);

        // Footpaths initial setup
        List<Footpath> footpathsFromPDep = stopIdToFootpaths.get(pDepId);
        if (footpathsFromPDep != null) {
            for (Footpath f : stopIdToFootpaths.get(pDepId)) {
                bestKnown.put(f.getOtherStop(pDepId).getId(), f.getDur());
            }
        }

        for (Connection c : filteredConnections) {
            // τ (pdep(c)) ≤ τdep(c).
            boolean cIsReachable = bestKnown.getOrDefault(c.getPDep().getId(), Integer.MAX_VALUE) <= c.getTDep();

            // τarr(c) < τ (parr(c))
            boolean cIsFaster = c.getTArr() < bestKnown.getOrDefault(c.getPArr().getId(), Integer.MAX_VALUE);

            if (cIsReachable && cIsFaster) {
                bestKnown.put(c.getPArr().getId(), c.getTArr());

                List<Footpath> footpathsFromPArr = stopIdToFootpaths.get(c.getPArr().getId());
                if (footpathsFromPArr != null) {
                    Stop footpathPDep = c.getPArr();

                    for (Footpath f : footpathsFromPArr) {
                        Stop footpathPArr = f.getOtherStop(footpathPDep.getId());

                        int footpathTArr = bestKnown.get(footpathPDep.getId()) + f.getDur();
                        boolean fpIsFaster = footpathTArr < bestKnown.get(footpathPArr.getId());
                        if (fpIsFaster)
                            bestKnown.put(footpathPArr.getId(), footpathTArr);
                    }
                }
            }

        }

        System.out.printf("bestKnown at the end: %s\n\n", bestKnown);

        Integer totalSec = bestKnown.get(pArrId);
        if (totalSec == null) {
            System.out.println("unreachable target");
        } else {
            System.out.printf("sec = %d (%s)\n", totalSec, TimeConversion.fromSeconds(totalSec));
        }
    }

    public List<List<String>> csvToMatrix(String csvPath) throws IOException {
        List<List<String>> mat = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(csvPath))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] values = line.split(",");
                mat.add(Arrays.asList(values));
            }
        }

        return mat;
    }

    public void loadData(String routesCSV, String stopTimesCSV, String stopsCSV, String tripsCSV) throws IOException {
        List<List<String>> stopTimes = csvToMatrix(stopTimesCSV);

        for (int i = 1; i < stopTimes.size() - 1; i++) {
            List<String> row0 = stopTimes.get(i);
            List<String> row1 = stopTimes.get(i + 1);

            Stop[] stops = {
                    // WARN: very bad, creating stops twice (see below when parsing stops.csv)
                    new Stop(row0.get(2)),
                    new Stop(row1.get(2))
            };

            Connection connection = new Connection(i, stops[0], stops[1],
                    TimeConversion.toSeconds(row0.get(1)),
                    TimeConversion.toSeconds(row1.get(1)));

            connections.add(connection);
        }

        // TODO: will have to move this elsewhere
        connections.sort(Comparator.comparingInt(Connection::getTDep));

        // ------------------- Stops.csv -------------------

        List<List<String>> parsedStopsCSV = csvToMatrix(stopsCSV);
        List<Stop> stops = new ArrayList<>();

        for (List<String> stopRow : parsedStopsCSV) {
            String stopId = stopRow.get(0);
            stops.add(new Stop(stopId));
        }

        // TODO: change this
        int FOOTPATH_DURATION = 3;

        int pathCount = 0;

        // generate all paths
        for (int i = 0; i < stops.size(); i++) {
            for (int j = i + 1; j < stops.size(); j++) {

                Footpath footpath = new Footpath(i + j - 1, stops.get(i), stops.get(j), FOOTPATH_DURATION);

                stopIdToFootpaths
                        .computeIfAbsent(stops.get(i).getId(), k -> new ArrayList<>())
                        .add(footpath);

                stopIdToFootpaths
                        .computeIfAbsent(stops.get(j).getId(), k -> new ArrayList<>())
                        .add(footpath);

                pathCount++;
            }
        }

        int stopCount = stops.size();

        System.out.printf("pathCount: %d, stopCount: %d\n", pathCount, stopCount);

    }

}
