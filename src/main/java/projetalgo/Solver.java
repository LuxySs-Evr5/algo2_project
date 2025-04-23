package projetalgo;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Solver {

    private HashMap<Stop, List<Footpath>> stopToFootpaths;
    private List<Connection> connections;
    private List<Footpath> footpaths; // TODO: is that useful ? probably won't be.

    public static final String COMMA_DELIMITER = ",";

    public Solver() {
        this.connections = new ArrayList<>();
        this.footpaths = new ArrayList<>();
        this.stopToFootpaths = new HashMap<>();
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
        // I don't think this is an effective way to do it
        for (Footpath f : footpaths) {
            boolean canStartWith = f.contains(pDepId);
            if (canStartWith)
                bestKnown.put(f.getOtherStop(pDepId).getId(), f.getDur());
        }

        for (Connection c : filteredConnections) {
            // τ (pdep(c)) ≤ τdep(c).
            boolean cIsReachable = bestKnown.getOrDefault(c.getPDep().getId(), Integer.MAX_VALUE) <= c.getTDep();

            // τarr(c) < τ (parr(c))
            boolean cIsFaster = c.getTArr() < bestKnown.getOrDefault(c.getPArr().getId(), Integer.MAX_VALUE);

            if (cIsReachable && cIsFaster) {
                bestKnown.put(c.getPArr().getId(), c.getTArr());

                List<Footpath> footpaths = stopToFootpaths.get(c.getPArr());
                if (footpaths != null) {
                    Stop footpathPDep = c.getPArr();

                    for (Footpath f : footpaths) {
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

    public void setup() {
        List<Stop> stops = Arrays.asList(
                new Stop("0"),
                new Stop("1"),
                new Stop("2"),
                new Stop("3"),
                new Stop("4"));

        connections = new ArrayList<>(Arrays.asList(
                new Connection(0, stops.get(0), stops.get(1), 0, 1),
                new Connection(1, stops.get(1), stops.get(3), 1, 2),
                new Connection(2, stops.get(0), stops.get(2), 1, 3),
                new Connection(3, stops.get(2), stops.get(3), 3, 4),
                new Connection(4, stops.get(0), stops.get(4), 0, 0),
                new Connection(5, stops.get(4), stops.get(2), 0, 2),
                new Connection(6, stops.get(4), stops.get(1), 0, 1)));

        footpaths = new ArrayList<>(Collections.singletonList(
                new Footpath(0, stops.get(4), stops.get(3), 1)));

        connections.sort(Comparator.comparingInt(Connection::getTDep));

        stopToFootpaths = new HashMap<>();
        for (Footpath p : footpaths) {
            for (Stop stop : p.getStops())
                stopToFootpaths
                        .computeIfAbsent(stop, k -> new ArrayList<>())
                        .add(p);
        }
    }

    public void loadData(String routesCSV, String stopTimesCSV, String stopsCSV, String tripsCSV) throws IOException {
        List<List<String>> stopTimes = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(stopTimesCSV))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] values = line.split(",");
                stopTimes.add(Arrays.asList(values));
            }
        }

        for (int i = 1; i < stopTimes.size() - 1; i++) {
            List<String> row0 = stopTimes.get(i);
            List<String> row1 = stopTimes.get(i + 1);

            Stop[] stops = new Stop[2];
            stops[0] = new Stop(row0.get(2));
            stops[1] = new Stop(row1.get(2));

            Connection connection = new Connection(i, stops[0], stops[1],
                    TimeConversion.toSeconds(row0.get(1)),
                    TimeConversion.toSeconds(row1.get(1)));

            connections.add(connection);
        }

        // TODO: will have to move this elsewhere
        connections.sort(Comparator.comparingInt(Connection::getTDep));
    }

}
