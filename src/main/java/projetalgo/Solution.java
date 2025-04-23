package projetalgo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

public class Solution {

    private HashMap<Stop, List<Footpath>> stops_to_footpaths;
    private List<Connection> connections;
    private List<Footpath> footpaths;
    private int num_stops;
    private Stop p_dep;
    private Stop p_arr;
    private int t_dep;

    public Solution() {
    }

    /**
     * Returns the index of the first connection in this.connections that
     * leaves as early as possible but doesn't leave before t_dep.
     */
    int get_earliest_reachable_connection() {

        int i = 0;
        int j = connections.size();

        while (i < j) {
            int mid = (i + j) / 2;
            if (connections.get(mid).get_t_dep() < t_dep) {
                i = mid + 1;
            } else {
                j = mid;
            }
        }

        return i;
    }

    List<Connection> get_filtered_connections() {
        return connections.subList(
                get_earliest_reachable_connection(),
                connections.size());
    }

    /**
     * Connections must be sorted by their t_dep.
     */
    public void solve() {
        List<Connection> filtered_connections = get_filtered_connections();

        int[] best_known = new int[num_stops];
        Arrays.fill(best_known, Integer.MAX_VALUE);

        best_known[p_dep.getId()] = 0;

        for (Footpath f : footpaths) {
            boolean can_start_with = f.contains(p_dep);
            if (can_start_with)
                best_known[f.getOtherStop(p_dep).getId()] = f.get_dur();
        }

        for (Connection c : filtered_connections) {
            boolean c_is_reachable = best_known[c.get_p_dep().getId()] <= c.get_t_dep(); // τ (pdep(c)) ≤ τdep(c).
            boolean c_is_faster = c.get_t_arr() < best_known[c.get_p_arr().getId()]; // τarr(c) < τ (parr(c))

            if (c_is_reachable && c_is_faster) {
                best_known[c.get_p_arr().getId()] = c.get_t_arr();

                List<Footpath> footpaths = stops_to_footpaths.get(c.get_p_arr());
                if (footpaths != null) {
                    Stop footpath_p_dep = c.get_p_arr();

                    for (Footpath f : footpaths) {
                        Stop footpath_p_arr = f.getOtherStop(footpath_p_dep);

                        int footpath_t_arr = best_known[footpath_p_dep.getId()] + f.get_dur();
                        boolean fp_is_faster = footpath_t_arr < best_known[footpath_p_arr.getId()];
                        if (fp_is_faster)
                            best_known[footpath_p_arr.getId()] = footpath_t_arr;

                        System.out.printf("fp: %s, faster: %b%n", f, fp_is_faster);
                    }
                }
            }

            System.out.printf("c: %s, reachable: %b, faster: %b%n", c.toString(), c_is_reachable, c_is_faster);
        }

        System.out.println(Arrays.toString(best_known));
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

        connections.sort(Comparator.comparingInt(Connection::get_t_dep));

        stops_to_footpaths = new HashMap<>();
        for (Footpath p : footpaths) {
            for (Stop stop : p.get_stops())
                stops_to_footpaths
                        .computeIfAbsent(stop, k -> new ArrayList<>())
                        .add(p);
        }

        num_stops = stops.size();
        p_dep = stops.get(0);
        p_arr = stops.get(4);
        t_dep = 0;
    }

}
