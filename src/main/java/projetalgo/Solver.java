package projetalgo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

public class Solver {

    private HashMap<Stop, List<Footpath>> stopsToFootpaths;
    private List<Connection> connections;
    private List<Footpath> footpaths; // is that useful ? probably won't be.
    private int numStops;
    private Stop pDep;
    private Stop pArr;
    private int tDep;

    public Solver() {
    }

    /**
     * Returns the index of the first connection departing at or after our departure
     * time.
     */
    int getEarliestReachableConnectionIdx() {

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
    List<Connection> getFilteredConnections() {
        return connections.subList(
                getEarliestReachableConnectionIdx(),
                connections.size());
    }

    /**
     * Connections must be sorted by their departure time.
     */
    public void solve() {
        List<Connection> filteredConnections = getFilteredConnections();

        int[] bestKnown = new int[numStops];
        Arrays.fill(bestKnown, Integer.MAX_VALUE);

        bestKnown[pDep.getId()] = 0;

        for (Footpath f : footpaths) {
            boolean canStartWith = f.contains(pDep);
            if (canStartWith)
                bestKnown[f.getOtherStop(pDep).getId()] = f.getDur();
        }

        for (Connection c : filteredConnections) {
            boolean cIsReachable = bestKnown[c.getPDep().getId()] <= c.getTDep(); // τ (pdep(c)) ≤ τdep(c).
            boolean cIsFaster = c.getTArr() < bestKnown[c.getPArr().getId()]; // τarr(c) < τ (parr(c))

            if (cIsReachable && cIsFaster) {
                bestKnown[c.getPArr().getId()] = c.getTArr();

                List<Footpath> footpaths = stopsToFootpaths.get(c.getPArr());
                if (footpaths != null) {
                    Stop footpathPDep = c.getPArr();

                    for (Footpath f : footpaths) {
                        Stop footpathPArr = f.getOtherStop(footpathPDep);

                        int footpathTArr = bestKnown[footpathPDep.getId()] + f.getDur();
                        boolean fpIsFaster = footpathTArr < bestKnown[footpathPArr.getId()];
                        if (fpIsFaster)
                            bestKnown[footpathPArr.getId()] = footpathTArr;

                        System.out.printf("fp: %s, faster: %b%n", f, fpIsFaster);
                    }
                }
            }

            System.out.printf("c: %s, reachable: %b, faster: %b%n", c.toString(), cIsReachable, cIsFaster);
        }

        System.out.println(Arrays.toString(bestKnown));
    }

    public void setup() {
        List<Stop> stops = Arrays.asList(
                new Stop(0),
                new Stop(1),
                new Stop(2),
                new Stop(3),
                new Stop(4));

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

        stopsToFootpaths = new HashMap<>();
        for (Footpath p : footpaths) {
            for (Stop stop : p.getStops())
                stopsToFootpaths
                        .computeIfAbsent(stop, k -> new ArrayList<>())
                        .add(p);
        }

        numStops = stops.size();
        pDep = stops.get(0);
        pArr = stops.get(4);
        tDep = 0;
    }

}
