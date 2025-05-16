package projetalgo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javafx.util.Pair;

public class ProfileFunction {
    // List<Pair<depatureTime, Map<CriteriaTracker --> Pair<arrivalTime,
    // movement>>>>, sorted by decreasing depatureTime
    private List<Pair<Integer, Map<CriteriaTracker, Pair<Integer, Movement>>>> entries;

    public ProfileFunction() {
        this.entries = new ArrayList<>();
    }

    /**
     * Get the movement whose corresponding CriteriaTracker matches the given
     * CriteriaTracker
     * and whose departure time is as early as possible while being >= tDep.
     */
    public Movement getFirstMatch(int tDep, CriteriaTracker criteriaTracker) {
        int firstReachableEntryIdx = getFirstReachableEntry(tDep);

        // no entry can be reached
        if (firstReachableEntryIdx == -1) {
            return null;
        }

        for (int i = firstReachableEntryIdx; i >= 0; i--) {
            Map<CriteriaTracker, Pair<Integer, Movement>> map = entries.get(i).getValue();
            if (map.containsKey(criteriaTracker)) {
                Movement movement = map.get(criteriaTracker).getValue();
                return movement;
            }
        }

        return null;
    }

    /**
     * Returns the index of the first entry whose departure time is greater
     * than or equal to tDep, entries.size() if no such entry.
     *
     * NOTE: This could be done with a binary search, but in practice it would
     * probably slow down the algorithm as we are always inserting near the
     * front of the entries array. (Because entries are sorted by increasing
     * departure time and we are scanning connections by decreasing departure
     * time.) The only reason why it is not always exactly in the first bag is
     * because of interstop footpaths:
     *
     * Let X, Y, Z be three stops. Let c be a connection leaving Y at t1. Let d
     * be a connection leaving Z at t2. Let f be a footpath from Y to Z that can
     * be travelled in fTravelTime.
     *
     * Assume t1 is before t2, therefore c is scanned first. Since f is an
     * incoming footpath of Y, the profile function of Z (f's departure stop)
     * will be updated (pushing new partial journeys leaving at time t1 -
     * fTravelTime).
     *
     * The next connection to be scanned is d, Z's profile will be updated
     * (again): adding partial journeys taking connection d (at t2).
     *
     * If t2 > t1 - fTravelTime, the journeys added in the previous step won't
     * be added at the front of Z's profile entries but right after the bag that
     * stores the journeys previously added when scanning f (near the front).
     */
    private int getFirstReachableEntry(int tDep) {
        for (int i = entries.size() - 1; i >= 0; i--) {
            if (entries.get(i).getKey() >= tDep) {
                return i;
            }
        }

        return -1; // no reachable entry found
    }

    /**
     * Inserts new partial journeys departing at tDep.
     *
     * @param tDep               departure time (in seconds)
     * @param newPartialJourneys a map from each CriteriaTracker to a Pair of
     *                           (arrivalTime, movement), where arrival time is the
     *                           arrival time at the destination and movement is the
     *                           next connection/footpath to take if following
     *                           that partial journey.
     *
     * @return True if at least one newPartialJourneys has been inserted (meaning it
     *         wasn't dominated); false otherwise.
     */
    public boolean insert(int tDep, Map<CriteriaTracker, Pair<Integer, Movement>> newPartialJourneys) {
        // 1. find the index of the last entry (from the back) whose key is <= tDep
        int firstReachableEntryIdx = getFirstReachableEntry(tDep);

        // 2. filter dominated journeys in newPartialJourneys
        Iterator<Map.Entry<CriteriaTracker, Pair<Integer, Movement>>> outerIt = newPartialJourneys.entrySet().iterator();

        while (outerIt.hasNext()) {
            Map.Entry<CriteriaTracker, Pair<Integer, Movement>> entryCandidate = outerIt.next();
            CriteriaTracker critCand = entryCandidate.getKey();
            int tArrCand = entryCandidate.getValue().getKey();

            boolean dominated = false;

            // 1) check domination by any other new journey
            for (Map.Entry<CriteriaTracker, Pair<Integer, Movement>> entryOther : newPartialJourneys.entrySet()) {
                if (entryOther == entryCandidate) {
                    continue;
                }

                CriteriaTracker critOther = entryOther.getKey();
                int tArrOther = entryOther.getValue().getKey();

                if ((critOther.dominates(critCand) && tArrOther <= tArrCand)
                        || (critOther.equals(critCand) && tArrOther < tArrCand)) {
                    dominated = true;
                    break;
                }
            }
            if (dominated) {
                outerIt.remove();
                continue;
            }

            // 2) check domination by any old journey
            for (int i = 0; i <= firstReachableEntryIdx && !dominated; i++) {
                for (Map.Entry<CriteriaTracker, Pair<Integer, Movement>> entryOld : entries.get(i).getValue().entrySet()) {
                    CriteriaTracker critOld = entryOld.getKey();
                    int tArrOld = entryOld.getValue().getKey();

                    if ((critOld.dominates(critCand) && tArrOld <= tArrCand) || (critOld.equals(critCand)
                            && tArrOld < tArrCand)) {
                        dominated = true;
                        break;
                    }
                }
            }
            if (dominated) {
                outerIt.remove();
            }
        }

        // 3. insert remaining non-dominated newPartialJourneys
        int insertionIdx;
        boolean createNewBag;
        if (firstReachableEntryIdx == -1) {
            // create a new bag with tdep at the index 0
            insertionIdx = 0;
            createNewBag = true;
        } else {
            if (entries.get(firstReachableEntryIdx).getKey() == tDep) {
                insertionIdx = firstReachableEntryIdx;
                createNewBag = false;
            } else {
                insertionIdx = firstReachableEntryIdx + 1;
                createNewBag = true;
            }
        }

        if (createNewBag) {
            // create a new map at insertionIdx with remaining newPartialJourneys entries
            entries.add(insertionIdx,
                    new Pair<Integer, Map<CriteriaTracker, Pair<Integer, Movement>>>(tDep, newPartialJourneys));
        } else {
            // add remaining newPartialJourneys entries to the map at insertionIdx
            entries.get(insertionIdx).getValue().putAll(newPartialJourneys);
        }

        // 4. remove partialJourneys that leave at/after tDep and that are now dominated
        // by the remaining newPartialJourneys
        for (int i = insertionIdx; i < entries.size(); i++) {
            Pair<Integer, Map<CriteriaTracker, Pair<Integer, Movement>>> entry = entries.get(i);

            Map<CriteriaTracker, Pair<Integer, Movement>> existingJourneys = entry.getValue();
            Iterator<Map.Entry<CriteriaTracker, Pair<Integer, Movement>>> it = existingJourneys.entrySet().iterator();

            while (it.hasNext()) {
                Map.Entry<CriteriaTracker, Pair<Integer, Movement>> oldEntry = it.next();
                CriteriaTracker oldCriteria = oldEntry.getKey();
                int oldTArr = oldEntry.getValue().getKey();

                // check if this old journey is now dominated by any new one
                for (Map.Entry<CriteriaTracker, Pair<Integer, Movement>> newEntry : newPartialJourneys.entrySet()) {
                    CriteriaTracker newCriteria = newEntry.getKey();
                    int newTArr = newEntry.getValue().getKey();

                    if ((newCriteria.dominates(oldCriteria) && newTArr <= oldTArr)
                            || (newCriteria.equals(oldCriteria) &&
                                    newTArr < oldTArr)) {
                        it.remove();
                        break;
                    }
                }
            }
        }

        return newPartialJourneys.size() > 0;
    }

    /**
     * Evaluates the profile function at a given time.
     *
     * For a given departure time tDep, scans all entries leaving at or after tDep.
     * For each CriteriaTracker, keeps the movement with the best (earliest) arrival
     * time. Returns a map from CriteriaTracker to its best (arrivalTime, movement)
     * pair.
     */
    public Map<CriteriaTracker, Pair<Integer, Movement>> evaluateAt(int tDep) {
        Map<CriteriaTracker, Pair<Integer, Movement>> ret = new HashMap<>();

        // all the entries that leave at/after tdep
        for (int i = 0; i <= getFirstReachableEntry(tDep); i++) {
            for (Map.Entry<CriteriaTracker, Pair<Integer, Movement>> entry : entries.get(i).getValue().entrySet()) {
                // if multiple times the same CriteriaTracker, put at that CriteriaTracker key
                // the pair<arrivalTime, Movement> that has the earliest arrivalTime
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

        for (int i = entries.size() - 1; i >= 0; i--) {
            Pair<Integer, Map<CriteriaTracker, Pair<Integer, Movement>>> entry = entries.get(i);

            int departureTime = entry.getKey();
            Map<CriteriaTracker, Pair<Integer, Movement>> criteriaMap = entry.getValue();

            sb.append("  Departure Time: ").append(TimeConversion.fromSeconds(departureTime)).append("\n");

            for (Map.Entry<CriteriaTracker, Pair<Integer, Movement>> mapEntry : criteriaMap.entrySet()) {
                CriteriaTracker criteria = mapEntry.getKey();
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
