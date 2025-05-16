package projetalgo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

public class Solver extends AbstractSolver {
    private final HashMap<String, List<Footpath>> stopIdToOutgoingFootpaths;

    public Solver(Data data) {
        super(data);

        double maxFootpathDistKm = 0.5;
        this.stopIdToOutgoingFootpaths = new HashMap<>();
        genFootpaths(maxFootpathDistKm);
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

    /**
     * Displays instructions for completing the journey.
     */
    private void printInstructions(Stack<BestKnownEntry> finalPath) {
        String currentTripId = null;
        RouteInfo currentRouteInfo = null;
        Stop tripStartStop = null;
        Stop previousStop = null;
        int departureTime = -1;
        int arrivalTime = -1;

        while (!finalPath.isEmpty()) {
            BestKnownEntry entry = finalPath.pop();
            Movement movement = entry.getMovement();
            Stop pDep = movement.getPDep();
            Stop pArr = movement.getPArr();

            switch (movement) {
                case Footpath footpath -> {
                    if (currentTripId != null) {
                        if (tripStartStop != null && previousStop != null && currentRouteInfo != null) {
                            String depTimeStr = TimeConversion.fromSeconds(departureTime);
                            String arrTimeStr = TimeConversion.fromSeconds(arrivalTime);
                            System.out.println(
                                "Take " + AinsiCode.BOLD + AinsiCode.RED + currentRouteInfo.toString() + AinsiCode.RESET + " from "
                                + AinsiCode.BOLD + AinsiCode.RED + tripStartStop.getName() + AinsiCode.RESET + " at " + AinsiCode.BOLD + 
                                AinsiCode.RED + depTimeStr + AinsiCode.RESET + " to " + AinsiCode.BOLD + AinsiCode.RED + 
                                previousStop.getName() + AinsiCode.RESET + " (" + arrTimeStr + ")."
                            );
                        }
                        currentTripId = null;
                        currentRouteInfo = null;
                        tripStartStop = null;
                        departureTime = -1;
                        arrivalTime = -1;
                    }
                    int travelTime = footpath.getTravelTime();
                    String depTimeStr = TimeConversion.fromSeconds(entry.getTArr() - travelTime);
                    String arrTimeStr = TimeConversion.fromSeconds(entry.getTArr());
                    String duration = TimeConversion.formatDuration(travelTime);
                    System.out.println(
                        "From " + AinsiCode.BOLD + AinsiCode.RED + pDep.getName() + AinsiCode.RESET + " (" + pDep.getTransportOperatorStop() + 
                        "), leave on foot at " + AinsiCode.BOLD + AinsiCode.RED + depTimeStr + AinsiCode.RESET + " to reach " + AinsiCode.BOLD + 
                        AinsiCode.RED + pArr.getName() + AinsiCode.RESET + " (" + pArr.getTransportOperatorStop() + ") by " + AinsiCode.BOLD + 
                        AinsiCode.RED + arrTimeStr + AinsiCode.RESET + ". Expect a walk of around " + AinsiCode.BOLD + AinsiCode.RED + duration + AinsiCode.RESET + "."
                    );

                    break;
                }

                case Connection connection -> {
                    String tripId = connection.getTripId();
                    RouteInfo routeInfo = connection.getRouteInfo();
                    if (currentTripId == null) {
                        currentTripId = tripId;
                        currentRouteInfo = routeInfo;
                        tripStartStop = pDep;
                        departureTime = connection.getTDep();
                        arrivalTime = connection.getTArr();
                    } else if (!tripId.equals(currentTripId)) {
                        if (tripStartStop != null && previousStop != null && currentRouteInfo != null) {
                            String depTimeStr = TimeConversion.fromSeconds(departureTime);
                            String arrTimeStr = TimeConversion.fromSeconds(arrivalTime);
                            System.out.println(
                                "Take " + AinsiCode.BOLD + AinsiCode.RED + currentRouteInfo.toString() + AinsiCode.RESET + " from "
                                + AinsiCode.BOLD + AinsiCode.RED + tripStartStop.getName() + AinsiCode.RESET + " at " + AinsiCode.BOLD + 
                                AinsiCode.RED + depTimeStr + AinsiCode.RESET + " to " + AinsiCode.BOLD + AinsiCode.RED + 
                                previousStop.getName() + AinsiCode.RESET + " (" + arrTimeStr + ")."
                            );
                        }
                        currentTripId = tripId;
                        currentRouteInfo = routeInfo;
                        tripStartStop = pDep;
                        departureTime = connection.getTDep();
                        arrivalTime = connection.getTArr();
                    }
                    break;
                }

                default -> {
                    // Same trip -> continue
                }
            }

            previousStop = pArr;
        }

        if (currentTripId != null) {
            if (tripStartStop != null && previousStop != null && currentRouteInfo != null) {
                String depTimeStr = TimeConversion.fromSeconds(departureTime);
                String arrTimeStr = TimeConversion.fromSeconds(arrivalTime);
                System.out.println(
                    "Take " + AinsiCode.BOLD + AinsiCode.RED + currentRouteInfo.toString() + AinsiCode.RESET + " from "
                    + AinsiCode.BOLD + AinsiCode.RED + tripStartStop.getName() + AinsiCode.RESET + " at " + AinsiCode.BOLD + 
                    AinsiCode.RED + depTimeStr + AinsiCode.RESET + " to " + AinsiCode.BOLD + AinsiCode.RED + 
                    previousStop.getName() + AinsiCode.RESET + " (" + arrTimeStr + ")."
                );
            }
        }
    }

    /**
     * Returns true if the given connection's departure time is after the
     * earliest arrival time to one of pArrIds.
     *
     * NOTE: in the paper "Intriguingly Simple and Fast Transit Routing?" by
     * Julian Dibbelt, Thomas Pajor, Ben Strasser, and Dorothea Wagner,
     * it is stated:
     * "if we are only interested in one-to-one queries, the algorithm may stop as
     * soon as it scans a connection whose departure time exceeds the target stop’s
     * earliest arrival time."
     */
    boolean checkConnectionTdepAfterEarliestTArr(Map<String, BestKnownEntry> bestKnown, Connection c,
            List<String> pArrIds) {
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
    public void solve(List<String> pDepIds, List<String> pArrIds, int tDep) {
        for (String pDepId : pDepIds) {
            for (String pArrId : pArrIds) {
                if (pDepId.equals(pArrId)) {
                    System.out.println("You are already at your destination");
                    return;
                }
            }
        }

        List<Connection> filteredConnections = getFilteredConnections(tDep);

        // Not in hashmap means infinity
        Map<String, BestKnownEntry> bestKnown = new HashMap<>();

        for (String pDepId : pDepIds) {
            // The time to get to pDep is tDep because we are already there
            bestKnown.put(pDepId, new BestKnownEntry(tDep, null));

            // Footpaths initial setup
            List<Footpath> footpathsFromPDep = stopIdToOutgoingFootpaths.get(pDepId);
            if (footpathsFromPDep != null) {
                for (Footpath f : stopIdToOutgoingFootpaths.get(pDepId)) {
                    bestKnown.put(f.getPArr().getId(),
                            new BestKnownEntry(tDep + f.getTravelTime(), f));
                }
            }
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

        String pDepName = finalPath.peek().getMovement().getPDep().getName();
        String pArrName = finalPath.firstElement().getMovement().getPArr().getName();

        System.out.println(
                AinsiCode.BOLD + "\nHere are the directions for the shortest route from " +
                        AinsiCode.RED + pDepName + AinsiCode.RESET + AinsiCode.BOLD +
                        " to " + AinsiCode.RED + pArrName + AinsiCode.RESET + AinsiCode.BOLD
                        +
                        ", departing at " + AinsiCode.RED + TimeConversion.fromSeconds(tDep) +
                        AinsiCode.RESET + AinsiCode.BOLD + " :\n" + AinsiCode.RESET);

        printInstructions(finalPath);

        System.out.println(
                AinsiCode.BOLD + AinsiCode.UNDERLINE + AinsiCode.RED + "You will arrive at " + stopIdToStop.get(pArrIdEarliest).getName()
                        + " at " + TimeConversion.fromSeconds(tArrEarliest) + AinsiCode.RESET);
    }

    /**
     * Generates all the footpaths.
     */
    private void genFootpaths(double maxDistKm) {
        BallTree ballTree = new BallTree(new ArrayList<>(stopIdToStop.values()));
        double maxDistanceKm = maxDistKm;
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

}
