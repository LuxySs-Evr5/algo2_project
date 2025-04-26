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

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;

public class Solver {

    private HashMap<String, Stop> stopIdToStop;
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
    public void solve(String pDepName, String pArrName, int tDep) {
        List<Connection> filteredConnections = getFilteredConnections(tDep);

        // NOTE: could use a List<Stops> to iterate and get the id, no need for a
        // hashmap

        // will be set in the for loop below
        String pArrId = null;

        // Not in hashmap means infinity
        Map<String, Integer> bestKnown = new HashMap<>();
        for (Map.Entry<String, Stop> entry : stopIdToStop.entrySet()) {
            String keyStopId = entry.getKey();
            Stop valueStop = entry.getValue();

            if (valueStop.getName().equals(pArrName)) {
                pArrId = keyStopId;
            }

            if (valueStop.getName().equals(pDepName)) {
                System.out.printf("found a pDep: %s : %s\n", valueStop.getName(), keyStopId);

                // The time to get to pDep is tDep because we are already there
                bestKnown.put(keyStopId, tDep);

                // Footpaths initial setup
                List<Footpath> footpathsFromPDep = stopIdToFootpaths.get(keyStopId);
                if (footpathsFromPDep != null) {
                    for (Footpath f : stopIdToFootpaths.get(keyStopId)) {
                        bestKnown.put(f.getOtherStop(keyStopId).getId(), tDep + f.getTravelTime());
                    }
                }
            }
        }

        if (pArrId == null) { // pArr not found
            System.out.println("invalid destination");
        }

        for (Connection c : filteredConnections) {
            // τ (pdep(c)) ≤ τdep(c).
            boolean cIsReachable = bestKnown.getOrDefault(c.getPDep().getId(), Integer.MAX_VALUE) <= c.getTDep();

            // τarr(c) < τ (parr(c))
            boolean cIsFaster = c.getTArr() < bestKnown.getOrDefault(c.getPArr().getId(), Integer.MAX_VALUE);

            if (cIsReachable && cIsFaster) {
                bestKnown.put(c.getPArr().getId(), c.getTArr());

                List<Footpath> footpathsFromCPArr = stopIdToFootpaths.get(c.getPArr().getId());
                if (footpathsFromCPArr != null) {
                    Stop footpathPDep = c.getPArr();

                    for (Footpath f : footpathsFromCPArr) {
                        Stop footpathPArr = f.getOtherStop(footpathPDep.getId());

                        int footpathTArr = bestKnown.get(footpathPDep.getId()) + f.getTravelTime();
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
            for (int j = i + 1; j < stops.size(); j++) {
                // TODO: Change Footpath ids
                Footpath footpath = new Footpath(i + j - 1, stops.get(i), stops.get(j));

                stopIdToFootpaths
                        .computeIfAbsent(stops.get(i).getId(), k -> new ArrayList<>())
                        .add(footpath);

                stopIdToFootpaths
                        .computeIfAbsent(stops.get(j).getId(), k -> new ArrayList<>())
                        .add(footpath);
            }
        }

        // ----------------- stop_times.csv -----------------

        List<List<String>> stopTimes = csvToMatrix(stopTimesCSV);

        for (int i = 1; i < stopTimes.size() - 1; i++) {
            List<String> row0 = stopTimes.get(i);
            List<String> row1 = stopTimes.get(i + 1);

            Connection connection = new Connection(i,
                    stopIdToStop.get(row0.get(2)),
                    stopIdToStop.get(row1.get(2)),
                    TimeConversion.toSeconds(row0.get(1)),
                    TimeConversion.toSeconds(row1.get(1)));

            connections.add(connection);
        }

        // TODO: will have to move this elsewhere
        connections.sort(Comparator.comparingInt(Connection::getTDep));
    }

}
