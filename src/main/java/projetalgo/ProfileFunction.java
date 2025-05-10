package projetalgo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javafx.util.Pair;

public class ProfileFunction<T extends CriteriaTracker> {
    // List<Pair<depatureTime, Map<CriteriaTracker --> Pair<arrivalTime,
    // movement>>>>, sorted by decreasing depatureTime
    private List<Pair<Integer, Map<T, Pair<Integer, Movement>>>> entries;

    public ProfileFunction() {
        this.entries = new ArrayList<>();
    }

    /**
     * TODO
     */
    public Movement getFirstMatch(int tDep, CriteriaTracker criteriaTracker) {
        int firstReachableEntryIdx = getFirstReachableEntry(tDep);

        // no entry can be reached
        if (firstReachableEntryIdx == -1) {
            return null;
        }

        for (int i = getFirstReachableEntry(tDep); i >= 0; i--) {
            Map<T, Pair<Integer, Movement>> map = entries.get(i).getValue();
            if (map.containsKey(criteriaTracker)) {
                Movement movement = map.get(criteriaTracker).getValue();
                return movement;
            }
        }

        return null;
    }

    /**
     * TODO
     */
    private int getFirstReachableEntry(int tDep) {
        for (int i = entries.size() - 1; i >= 0; i--) {
            if (entries.get(i).getKey() >= tDep) {
                return i;
            }
        }

        // TODO: handle the -1 case when calling this function
        return -1; // no reachable entry found
    }

    /**
     * Inserts new partial journeys departing at tDep.
     *
     * @param tDep               departure time (in seconds)
     * @param newPartialJourneys a map from each CriteriaTracker (T) to a Pair of
     *                           (arrivalTime, movement), where arrival time is the
     *                           arrival time at the destination and movement is the
     *                           next connection/footpath to take if following
     *                           that partial journey.
     */
    public void insert(int tDep, Map<T, Pair<Integer, Movement>> newPartialJourneys) {
        // find the index from which all the entries keys are >=tDep
        int firstReachableEntryIdx = getFirstReachableEntry(tDep);

        // track which new partial journeys were added, so we can later remove any
        // existing ones that are now dominated.
        List<Pair<T, Pair<Integer, Movement>>> addedNewPartialJourneys = new ArrayList<>();

        // 2 cases:

        // 1. there isn't any pair<departureTime, map> such that departureTime >= tDep
        // yet -> we simply add our new entry at the end
        if (firstReachableEntryIdx == entries.size()) {

            // add the new new partial journeys to entries
            entries.add(new Pair<Integer, Map<T, Pair<Integer, Movement>>>(tDep,
                    newPartialJourneys));

            // add the new partial journeys to the tracker so we can then remove the partial
            // journeys that leave at/before tdep and are dominated by the new partial
            // journeys
            for (Map.Entry<T, Pair<Integer, Movement>> newPartialJourneyEntry : newPartialJourneys.entrySet()) {
                addedNewPartialJourneys
                        .add(new Pair<>(newPartialJourneyEntry.getKey(), newPartialJourneyEntry.getValue()));
            }

        } // 2. there is already a pair<departureTime, map> such that departureTime >=
          // tDep
        else {

            // iterate over the new partial journeys that we want to insert
            for (Map.Entry<T, Pair<Integer, Movement>> newPartialJourney : newPartialJourneys.entrySet()) {

                // the first entry whose departure time is >= tDep
                Pair<Integer, Map<T, Pair<Integer, Movement>>> firstReachableEntry = entries
                        .get(firstReachableEntryIdx);

                T newPartialJourneyCriteria = newPartialJourney.getKey();
                int newPartialJourneyTArr = newPartialJourney.getValue().getKey();

                boolean dominated = false;

                // iterate over all the things that depart at/after tdep
                for (Pair<Integer, Map<T, Pair<Integer, Movement>>> currentEntry : entries
                        .subList(firstReachableEntryIdx, entries.size())) {

                    // iterate over their <key, values>
                    for (Map.Entry<T, Pair<Integer, Movement>> partialJourney : currentEntry.getValue().entrySet()) {

                        T partialJourneyCriteria = partialJourney.getKey();
                        int partialJourneyTArr = partialJourney.getValue().getKey();

                        // Check whether we already have a journey that has better criteria- or
                        // identical criteria tracker but arrives at the same time or before the
                        // newPartialJourney.
                        if ((partialJourneyCriteria.dominates(newPartialJourneyCriteria) ||
                                partialJourneyCriteria.equals(newPartialJourneyCriteria)) &&
                                partialJourneyTArr <= newPartialJourneyTArr) {
                            dominated = true;
                        }

                    }

                }

                if (!dominated) {
                    System.out.println("not dominated");

                    // add the newPartialJourney

                    // 2 cases:

                    // 1: There alread exists a pair with tdep
                    if (tDep == firstReachableEntry.getKey()) {
                        System.out.printf("pair with tdep=%d already exists\n", tDep);

                        Map<T, Pair<Integer, Movement>> map = entries.get(firstReachableEntryIdx).getValue();
                        map.put(newPartialJourneyCriteria, newPartialJourney.getValue());
                    }
                    // 2: There isn't any pair with tdep yet
                    else if (tDep < firstReachableEntry.getKey()) {
                        System.out.printf("no pair with tdep=%d yet\n", tDep);

                        // create a new hashmap with the new partial journey
                        Map<T, Pair<Integer, Movement>> newMap = new HashMap<T, Pair<Integer, Movement>>(
                                Map.of(newPartialJourneyCriteria, newPartialJourney.getValue()));

                        // add the new entry with tdep and map in entries
                        entries.add(firstReachableEntryIdx,
                                new Pair<Integer, Map<T, Pair<Integer, Movement>>>(tDep, newMap));
                    }

                    // add the new partial journeys to the tracker so we can then remove the partial
                    // journeys that leave at/before tdep and are dominated by the new partial
                    // journeys
                    addedNewPartialJourneys.add(new Pair<T, Pair<Integer, Movement>>(newPartialJourneyCriteria,
                            newPartialJourney.getValue()));

                } else {
                    System.out.println("dominated");
                }
            }
        }

        // for each newly added partial journey
        for (Pair<T, Pair<Integer, Movement>> addedNewPartialJourney : addedNewPartialJourneys) {

            T addedNewPartialJourneyCriteria = addedNewPartialJourney.getKey();
            int addedNewPartialJourneyTArr = addedNewPartialJourney.getValue().getKey();

            // for each bag entry whose departure time is before/at tdep
            for (Pair<Integer, Map<T, Pair<Integer, Movement>>> currentEntry : entries.subList(0,
                    firstReachableEntryIdx + 1)) { // TODO: check the +1

                List<T> partialJourneysNowDominated = new ArrayList<T>();

                // for each partial journey in that entry
                for (Map.Entry<T, Pair<Integer, Movement>> partialJourney : currentEntry.getValue().entrySet()) {

                    T partialJourneyCriteria = partialJourney.getKey();
                    int partialJourneyTArr = partialJourney.getValue().getKey();

                    // the partial journey being scanned is now dominated by at least one of
                    // addedNewPartialJourneys
                    if ((addedNewPartialJourneyCriteria.dominates(partialJourneyCriteria) &&
                            addedNewPartialJourneyTArr <= partialJourneyTArr) ||
                            (addedNewPartialJourneyCriteria.equals(partialJourneyCriteria) &&
                                    addedNewPartialJourneyTArr < partialJourneyTArr)) {
                        System.out.printf("setting %s to remove because dominated by%s\n", partialJourney,
                                addedNewPartialJourney);

                        partialJourneysNowDominated.add(partialJourneyCriteria);
                    }
                }

                partialJourneysNowDominated
                        .forEach((dominatedJourneyKey) -> entries.get(firstReachableEntryIdx).getValue()
                                .remove(dominatedJourneyKey));
            }
        }
    }

    /**
     * Evaluates the profile function at a given time.
     *
     * For a given departure time tDep, scans all entries leaving at or after tDep.
     * For each CriteriaTracker, keeps the movement with the best (earliest) arrival
     * time. Returns a map from CriteriaTracker to its best (arrivalTime, movement)
     * pair.
     */
    public Map<T, Pair<Integer, Movement>> evaluateAt(int tDep) {
        Map<T, Pair<Integer, Movement>> ret = new HashMap<>();

        for (int i = getFirstReachableEntry(tDep); i < entries.size(); i++) {
            for (Map.Entry<T, Pair<Integer, Movement>> entry : entries.get(i).getValue().entrySet()) {
                // put at entry.getKey() (T) -> min(T, pair that has the best tarr)

                Pair<Integer, Movement> pairCurrentlyAtKey = ret.get(entry.getKey());
                if (pairCurrentlyAtKey == null) {
                    ret.put(entry.getKey(), entry.getValue());
                } else { // there is already sth for that CriteriaTracker -> update only if it improves
                         // TArr
                    int entryTArr = entry.getValue().getKey();
                    int pairCurrentlyAtKeyTArr = pairCurrentlyAtKey.getKey();

                    if (entryTArr < pairCurrentlyAtKeyTArr) {
                        ret.put(entry.getKey(), entry.getValue());
                    }
                }
            }
        }

        return ret;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("ProfileFunction:\n");

        for (Pair<Integer, Map<T, Pair<Integer, Movement>>> entry : entries) {
            int departureTime = entry.getKey();
            Map<T, Pair<Integer, Movement>> criteriaMap = entry.getValue();

            sb.append("  Departure Time: ").append(TimeConversion.fromSeconds(departureTime)).append("\n");

            for (Map.Entry<T, Pair<Integer, Movement>> mapEntry : criteriaMap.entrySet()) {
                T criteria = mapEntry.getKey();
                Pair<Integer, Movement> value = mapEntry.getValue();
                int arrivalTime = value.getKey();
                Movement movement = value.getValue();

                sb.append("    Criteria: ").append(criteria.toString()).append("\n");
                sb.append("      Arrival Time: ").append(TimeConversion.fromSeconds(arrivalTime)).append("\n");
                sb.append("      Movement: ").append((movement != null) ? movement.toString() : "null").append("\n");
            }
        }

        return sb.toString();
    }

}
