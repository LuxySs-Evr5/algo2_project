package projetalgo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import javafx.util.Pair;

public class MultiCritSolver extends AbstractSolver {
    private final HashMap<String, List<Footpath>> stopIdToIncomingFootpaths;
    // not necessary since we don't use tau2
    // private final List<String> tripIds;

    private static final List<Footpath> EMPTY_FOOTPATH_LIST = List.of();

    public MultiCritSolver(Data data) {
        super(data);

        // not necessary since we don't use tau2
        // this.tripIds = new ArrayList<>();

        double maxFootpathDistKm = 0.5;
        this.stopIdToIncomingFootpaths = new HashMap<>();
        this.genFootpaths(maxFootpathDistKm);
    }

    /**
     * Displays instructions for completing the journey.
     */
    private void displayJourney(Map<String, ProfileFunction> S, String pDepId,
            String pArrId, int tDep,
            CriteriaTracker criteriaTracker) {

        String stopId = pDepId;
        String tripId = null;

        while (!stopId.equals(pArrId)) {
            Movement movement = S.get(stopId).getFirstMatch(tDep, criteriaTracker);

            System.out.printf("taking %s\n", movement);

            stopId = movement.getPArr().getId();

            if (movement instanceof Footpath footpath) {
                criteriaTracker.decFootpathsCount();
                tDep += footpath.getTravelTime();
                tripId = null; // means footpath
            } else if (movement instanceof Connection connection) {
                tDep = connection.getTArr();
                tripId = connection.getTripId();

                switch (connection.getTransportType()) {
                    case BUS:
                        criteriaTracker.decBusesCount();
                        break;
                    case METRO:
                        criteriaTracker.decMetrosCount();
                        break;
                    case TRAM:
                        criteriaTracker.decTramsCount();
                        break;
                    case TRAIN:
                        criteriaTracker.decTrainsCount();
                        break;
                }
            }
        }
    }

    /**
     * Displays the characteristics of the journeys in S's profile and prompts the
     * user to select one if at least one journey exists. Returns the
     * CriteriaTracker corresponding to the select journey.
     * If no journey exists, an empty optional is returned.
     */
    Optional<CriteriaTracker> promptJourney(Map<String, ProfileFunction> S, String pDepId, int tDep) {
        Map<CriteriaTracker, Pair<Integer, Movement>> results = S.get(pDepId).evaluateAt(tDep);

        if (results.isEmpty()) {
            return Optional.empty();
        }

        // find journeys dominated by other journeys that we can take
        // NOTE: Until now, there could be journeys that were dominated by other
        // journeys leaving earlier, but we could not remove the ones leaving later
        // because we didn't know whether we would be able to arrive on time to catch
        // the ones leaving earlier.
        // But since now, we know that we can catch those leaving at tdep, we can remove
        // all the journeys dominated by other journeys leaving at/after tdep.
        ArrayList<CriteriaTracker> dominatedResults = new ArrayList<>();
        for (Map.Entry<CriteriaTracker, Pair<Integer, Movement>> entry0 : results.entrySet()) {
            for (Map.Entry<CriteriaTracker, Pair<Integer, Movement>> entry1 : results.entrySet()) {

                CriteriaTracker entry0Criteria = entry0.getKey();
                int entry0TArr = entry0.getValue().getKey();
                CriteriaTracker entry1Criteria = entry1.getKey();
                int entry1TArr = entry1.getValue().getKey();

                if ((entry0Criteria.dominates(entry1Criteria)
                        && entry0TArr <= entry1TArr) ||
                        (entry0Criteria.equals(entry1Criteria)
                                && entry0TArr < entry1TArr)) {
                    dominatedResults.add(entry1Criteria);
                }
            }
        }

        // remove the journeys we found above from results as we have better
        // alternatives
        dominatedResults.forEach((dominatedJourneyCriteria) -> results.remove(dominatedJourneyCriteria));

        List<CriteriaTracker> options = new ArrayList<>(results.keySet());

        System.out.println("Possible journeys:");
        for (int i = 0; i < options.size(); i++) {
            CriteriaTracker tracker = options.get(i);
            int tArr = results.get(tracker).getKey();
            System.out.printf(" [%d] arrives at %s\n", i,
                    TimeConversion.fromSeconds(tArr));
            System.out.printf(" %s\n\n", tracker);
        }

        int choice = -1;
        while (true) {
            try {
                choice = Integer
                        .parseInt(InteractiveConsole.ask("Enter the number of the journey you want to choose: "));
                if (choice >= 0 && choice < options.size()) {
                    break;
                } else {
                    System.out.println("Invalid input, please enter a number from the list.");
                }
            } catch (NumberFormatException e) {
                System.out.println("Invalid input, please enter a number.");
            }
        }

        return Optional.of(options.get(choice));
    }

    /**
     * Updates TauC by adding the given entry in it. If this entry conflicts with
     * one already in TauC, i.e. there is already another CriteriaTracker with the
     * same characteristics in TauC, only the one with the best arrival time is
     * kept in TauC.
     */
    private void updateTauC(Map<CriteriaTracker, Pair<Integer, Movement>> tauC, CriteriaTracker criteriaTracker,
            Pair<Integer, Movement> tArrMovement) {

        Pair<Integer, Movement> pairCurrentlyAtKey = tauC.get(criteriaTracker);
        if (pairCurrentlyAtKey == null) {
            tauC.put(criteriaTracker, tArrMovement);
        } else { // there is already sth for that CriteriaTracker -> update only if it improves
                 // TArr
            int entryTArr = tArrMovement.getKey();
            int pairCurrentlyAtKeyTArr = pairCurrentlyAtKey.getKey();

            if (entryTArr < pairCurrentlyAtKeyTArr) {
                tauC.put(criteriaTracker, tArrMovement);
            }
        }
    }

    /**
     * Solves the multicriteria Connection Scan problem (mcCSA variant), computing
     * Pareto-optimal journeys from a departure stop to an arrival stop, given a
     * departure time and a CriteriaTracker's derivative (template argument).
     *
     * This implementation is based on the two pseudocodes from the article:
     * Julian Dibbelt, Thomas Pajor, Ben Strasser, Dorothea Wagner.
     * "Connection Scan Algorithm" (March 2017):
     * 1. "Pareto Connection Scan profile algorithm without interstop footpaths"
     * (Figure 11) (for the pareto optimisation)
     * 2. "Earliest arrival Connection Scan profile algorithm with interstop
     * footpaths and the limited walking optimization" (Figure 9) (for the interstop
     * footpaths)
     *
     *
     * === Key Difference from the Original Pareto optimization pseudocode ===
     * 1. The second optimization criterion differs:
     * The original pseudocode optimizes the number of legs whereas our
     * implementation optimizes the number of movements using certain
     * transport modes such as bus, tram, train, metro, footpath, using a
     * `CriteriaTracker` derivative and its overridden `dominates()` logic.
     * This allows the user to write a `CriteriaTracker` derivative and override
     * `dominates()` so that it maximizes or minimizes the number of bus-connections
     * for example.
     *
     * 2. To reconstruct the journey at the end, not only do we store the
     * arrival time for each partial journey but also the last Movement taken for
     * this journey (both are stored in a Pair). Pay attention to the fact that we
     * build the journeys backwards from pArr, meaning that "last movement taken"
     * means "the next movement to take if we are travelling the journey forward".
     * Thus, a Pair<Integer, Movement> (tArr, m) can be read as:
     * "There exists a partial journey arriving at tArr, and to continue it from
     * here, you must take the movement m."
     *
     *
     * Similarities & differences to the original pseudocodes are noted with
     * comments. Due to differences in data structures and optimization goals, some
     * lines differ slightly and some have been removed as they are irrelevant for
     * our optimization goals (see τ2 below).
     *
     * @param criteriaTrackerFactory the criteriaTracker factory (depends on the
     *                               criteria that the caller wants to use)
     * @param pDepId                 the departure stop ID
     * @param pArrId                 the arrival stop ID
     * @param tDep                   the departure time in seconds
     */
    public <T extends CriteriaTracker> void solve(Supplier<T> criteriaTrackerFactory, String pDepId, String pArrId,
            int tDep) {

        if (pDepId.equals(pArrId)) {
            System.out.println("You are already at your destination");
            return;
        }

        // ### init data structure

        // stopId -> stop's ProfileFunction
        Map<String, ProfileFunction> S = new HashMap<>();

        // tripId -> Map<CriteriaTracker -> (tArr for this journey + last Movement
        // taken)>
        // Map<String, Map<CriteriaTracker, Pair<Integer, Movement>>> T = new
        // HashMap<>();

        // stopId -> footpath to pArr (dest)
        // In the original pseudocode (figure 11), D only stores the footpath's
        // travel times but we store the Footpath objects directly.
        Map<String, Footpath> D = new HashMap<>();

        // We don't initalize the default values for all stops/trips with
        // infinities as this is not necessary because the absence of a key in the
        // hashmap already represents the default value.

        // for all footpaths f with farr stop = target do D[x] ← fdur;
        // We store the footpaths directly instead of the footpath's travel time.
        stopIdToIncomingFootpaths.getOrDefault(pArrId, new ArrayList<>()).forEach(footpath -> {
            D.put(footpath.getPDep().getId(), footpath);
        });

        // for all stops x do S[x] ← {(∞, ∞)}
        stopIdToStop.forEach((stopId, stop) -> {
            S.put(stopId, new ProfileFunction());
        });

        // for all trips x do T [x] ← ∞;
        // tripIds.forEach(tripId -> {
        // T.put(tripId, new HashMap<>());
        // });

        // ### Actual algorithm

        for (Connection c : getFilteredConnections(tDep).reversed()) {
            if (c.getPDep().getId().equals(pArrId)) {
                // avoid stupid loops, e.g. if our dest is A and the algorithm scans a
                // connection c from A to B, without this "continue", it will consider the
                // journeys that take the connection c and then come back to A.
                continue;
            }

            // τc ← min{τ1, τ2, τ3};
            // In the original pseudocode (figure 11) τ1, τ2, τ3 are caculated separately
            // and then merged to create τc. Our implementation directly updates tauC
            // without creating tau1/2/3 temporarily.
            Map<CriteriaTracker, Pair<Integer, Movement>> tauC = new HashMap<>();

            ProfileFunction sCPArr = S.get(c.getPArr().getId());

            // τ1 : corresponds to "take c and then walk to pArr"
            // Since D doesn't store travel times but footpaths, and there is no footpath
            // from pArr to pArr, we have to handle separately the case where the connection
            // directly arrives at pArr. (In the original pseudocode, since the travel time
            // from pArr to pArr is 0, this was done without splitting it in two cases).
            if (c.getPArr().getId().equals(pArrId)) { // no need to walk if we arrive directly at pArrId
                CriteriaTracker newTracker = criteriaTrackerFactory.get().addMovement(c);
                int tArr = c.getTArr();

                updateTauC(tauC, newTracker, new Pair<>(tArr, c));
            } else { // doesn't arrive directly at target -> must walk to target

                // In practice, the path that leads to pArr may not exist if it is too long to
                // travel.
                Footpath finalFootpath = D.get(c.getPArr().getId());

                if (finalFootpath != null) {
                    // from pseudocode figure 9: τ1 ← carr time + D[carr stop]
                    // (The arrival time is the arrival time of the connection + the time to walk to
                    // the destination).
                    int tArrWithfootpath = c.getTArr() + finalFootpath.getTravelTime();

                    CriteriaTracker newTracker = criteriaTrackerFactory.get().addMovement(finalFootpath).addMovement(c);

                    updateTauC(tauC, newTracker, new Pair<>(tArrWithfootpath, c));

                    int foopathTDep = c.getTArr();

                    CriteriaTracker finalFootpathNewTracker = criteriaTrackerFactory.get().addMovement(finalFootpath);

                    // insert the footpath in c.pArr's profile
                    sCPArr.insert(foopathTDep,
                            new HashMap<>(Map.of(finalFootpathNewTracker,
                                    new Pair<>(tArrWithfootpath, finalFootpath))));
                }
            }

            // τ2 ← T [ctrip];
            // This corresponds to continuing on the same trip without transferring.
            //
            // In the original algorithm, we don't increase the number of legs in τ2 because
            // τ2 is for "staying on the same trip", the number of legs is
            // increased in τ3 as τ3 is for transferring to other trips.
            //
            // But since our algorithm only optimizes criteria on the movement-level instead
            // of trip-level (meaning ours doesn't depend on trip-related data (e.g.
            // transfers), but only the connection being scanned), this case doesn't need to
            // be handled separately from tau3 in our algorithm. Here is the tau2 code
            // in case we ever want to add a trip-level criterion such as number of
            // legs/transfers.
            //
            // T.get(c.getTripId())
            // .entrySet()
            // .forEach(entry -> {
            // int tArr = entry.getValue().getKey();
            // CriteriaTracker prevTracker = entry.getKey();
            // CriteriaTracker newTracker = prevTracker.addMovement(c);
            // updateTauC(tauC, newTracker, new Pair<>(tArr, c));
            // });

            // τ3 ← evaluate S[carr stop] at carr time;
            //
            // τ3 corresponds to the "change trip" case in the original
            // pseudocode (figure 11).
            //
            // As explained for τ2 our algorithm doesn't take number of transfers and other
            // trip-related data into account.
            // Therefore, in our algorithm, c is just like any other connection (no matter
            // which trip it belongs to) that should be considered for CriteriaTracker.
            sCPArr.evaluateAt(c.getTArr())
                    .entrySet()
                    .forEach(entry -> {
                        int tArr = entry.getValue().getKey();
                        CriteriaTracker prevTracker = entry.getKey();

                        // For example, if c is a bus-connection, and our criteriaTracker takes the
                        // number of buses connections into account, the busesCount would be increased
                        // in addMovement.
                        CriteriaTracker newTracker = prevTracker.addMovement(c);
                        updateTauC(tauC, newTracker, new Pair<>(tArr, c));
                    });

            // insert a copy of tauC into T[ctrip] (not necessary since we don't use tau2)
            // T.put(c.getTripId(),
            // tauC.entrySet().stream()
            // .collect(Collectors.toMap(
            // e -> e.getKey().copy(),
            // e -> new Pair<>(e.getValue().getKey(), e.getValue().getValue()))));

            ProfileFunction sCPDep = S.get(c.getPDep().getId());
            boolean atLeastOneNotDominated = sCPDep.insert(c.getTDep(), tauC);

            // Propagate into incoming footpaths only if at least one entry from tauC was
            // actually inserted (not dominated) in c.pDep. (A partial journey being
            // dominated in c.pDep implies that it is also dominated in incoming footpaths
            // of c.pDep).
            if (atLeastOneNotDominated) {
                Map<CriteriaTracker, Pair<Integer, Movement>> sCPDepEvaluatedAtCTDep = sCPDep.evaluateAt(c.getTDep());

                for (Footpath f : stopIdToIncomingFootpaths.getOrDefault(c.getPDep().getId(), EMPTY_FOOTPATH_LIST)) {
                    int fTDep = c.getTDep() - f.getTravelTime();
                    if (fTDep > tDep) {

                        Map<CriteriaTracker, Pair<Integer, Movement>> map = sCPDepEvaluatedAtCTDep
                                .entrySet()
                                .stream()
                                .collect(Collectors.toMap(
                                        e -> {
                                            CriteriaTracker prevTracker = e.getKey();
                                            CriteriaTracker newTracker = prevTracker.addMovement(f);
                                            return newTracker;
                                        },
                                        e -> new Pair<>(e.getValue().getKey(), f)));

                        S.get(f.getPDep().getId()).insert(fTDep, map);
                    }
                }
            }
        }

        System.out.println("prompting journey");
        Optional<CriteriaTracker> optCriteriaTracker = promptJourney(S, pDepId, tDep);

        optCriteriaTracker.ifPresentOrElse(
                criteriaTracker -> displayJourney(S, pDepId, pArrId, tDep, criteriaTracker),
                () -> System.out.println("no journey found"));
    }

    /**
     * Generates all the footpaths.
     */
    private void genFootpaths(double maxDistKm) {
        BallTree ballTree = new BallTree(new ArrayList<>(stopIdToStop.values()));
        for (Stop sourceStop : stopIdToStop.values()) {

            List<Stop> nearbyStops = ballTree.findStopsWithinRadius(sourceStop, maxDistKm);

            for (Stop arrStop : nearbyStops) {
                if (!sourceStop.equals(arrStop)) {
                    Footpath footpath = new Footpath(sourceStop, arrStop);

                    stopIdToIncomingFootpaths
                            .computeIfAbsent(arrStop.getId(), k -> new ArrayList<>())
                            .add(footpath);
                }
            }
        }

    }

}
