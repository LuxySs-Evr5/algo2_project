package projetalgo;

import java.util.Optional;
import java.util.HashMap;
import java.util.List;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Stack;

public class AbstractSolver {
    final protected HashMap<String, Stop> stopIdToStop;
    final protected List<Connection> connections;

    public AbstractSolver(final Data data) {
        this.stopIdToStop = data.stopIdToStop;
        this.connections = data.connections;
    }

    /**
     * Returns the result of the search for a stop with the given name.
     */
    List<Stop> stopsWithName(final String name, Optional<String> routeName) {
        List<Stop> matchingStops = new ArrayList<>();
        for (Stop stop : stopIdToStop.values()) {
            if (routeName.isPresent() && stop.getRouteInfo() != null
                    && !stop.getRouteInfo().getRouteName().equals(routeName.get())) {
                continue;
            }
            if (stop.getName().equals(name)) {
                matchingStops.add(stop);
            }
        }
        return matchingStops;
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
    public List<Connection> getFilteredConnections(int tDep) {
        return connections.subList(getEarliestReachableConnectionIdx(tDep),
                connections.size());
    }

}
