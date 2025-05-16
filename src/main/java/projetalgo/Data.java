package projetalgo;

import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;

public class Data {
    public HashMap<String, Stop> stopIdToStop;
    public List<Connection> connections;

    /**
     * Loads all the data corresponding to all the given csvSets :
     * connections, stopIdToStop
     */
    static public Data loadFromCSVs(CsvSet... csvSets) throws IOException, CsvValidationException {
        Data data = new Data();
        data.stopIdToStop = new HashMap<>();
        data.connections = new ArrayList<>();

        for (CsvSet csvSet : csvSets) {
            data.loadOneCsvSet(csvSet);
        }

        // sort by increasing departure time
        data.connections.sort(Comparator.comparingInt(Connection::getTDep));

        return data;
    }

    void loadOneCsvSet(CsvSet csvSet) throws IOException, CsvValidationException {
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
                Coord coord = new Coord(Double.parseDouble(line[headerMap.get("stop_lat")]),
                        Double.parseDouble(line[headerMap.get("stop_lon")]));
                stopIdToStop.put(stopId, new Stop(stopId, stopName, coord, csvSet.transportOperator));
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
                String routeLongName = line[headerMap.get("route_long_name")];
                TransportType transportType = TransportType.valueOf(line[headerMap.get("route_type")]);

                RouteInfo routeInfo = new RouteInfo(routeShortName, routeLongName, transportType,
                        csvSet.transportOperator);
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

        // --------- Add RouteInfo for each stop that have a connection ---------

        for (Connection c : connections) {
            Stop pDep = c.getPDep();
            Stop pArr = c.getPArr();
            RouteInfo route = c.getRouteInfo();

            if (pDep.getRouteInfo() == null) {
                pDep.setRouteInfo(route);
            }
            if (pArr.getRouteInfo() == null) {
                pArr.setRouteInfo(route);
            }
        }
    }


}
