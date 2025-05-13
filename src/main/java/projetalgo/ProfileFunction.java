package projetalgo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
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

    // TODO: remove this
    public void displayTDeps() {
        for (Pair<Integer, Map<T, Pair<Integer, Movement>>> entry : entries) {
            System.out.printf("%s ", TimeConversion.fromSeconds(entry.getKey()));
        }

        System.out.println();
    }

    /**
     * TODO
     */
    // public Movement getFirstMatch(boolean isFirstMovement, int tDep, String tripId, CriteriaTracker criteriaTracker) {
    //     int firstReachableEntryIdx = getFirstReachableEntry(tDep);
    //
    //     // no entry can be reached
    //     if (firstReachableEntryIdx == -1) {
    //         return null;
    //     }
    //
    //     for (int i = getFirstReachableEntry(tDep); i >= 0; i--) {
    //         Map<T, Pair<Integer, Movement>> map = entries.get(i).getValue();
    //
    //         for (Map.Entry<T, Pair<Integer, Movement>> entry : map.entrySet()) {
    //             T tracker = entry.getKey();
    //             int tArr = entry.getValue().getKey();
    //             Movement movement = entry.getValue().getValue();
    //
    //             boolean isTransfer = (!isFirstMovement) &&
    //                     (((tripId == null) || (movement instanceof Footpath)
    //                             || (movement instanceof Connection connection
    //                                     && !connection.getTripId().equals(tripId))));
    //
    //             System.out.printf("movement : %s\n", movement);
    //             System.out.printf("isTransfer : %b\n", isTransfer);
    //
    //             if (movement instanceof Connection connection) {
    //                 System.out.printf("tripId : %s\n", connection.getTripId());
    //             } else {
    //                 System.out.printf("tripId : none (footpath)\n");
    //             }
    //
    //             System.out.printf("criteriaTracker.getTransfersCount(): %d\n", criteriaTracker.getTransfersCount());
    //             System.out.printf("tracker.getTransfersCount() + ((isTransfer) ? 1 : 0): %d\n",
    //                     tracker.getTransfersCount() + ((isTransfer) ? 1 : 0));
    //
    //             // If the connection we are looking at requires a transfer, make sure we have
    //             // the right number of transfers (this is why +1 if transfer)
    //             if (criteriaTracker.getTransfersCount() == tracker.getTransfersCount() + ((isTransfer) ? 1 : 0)) {
    //                 if (isTransfer) {
    //                     criteriaTracker.decTransfersCount();
    //                 }
    //
    //                 return movement;
    //             }
    //         }
    //
    //     }
    //
    //     return null;
    // }

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
     *
     * @return True if at least one newPartialJourneys has been inserted (meaning it
     *         wasn't dominated); false otherwise.
     */
    public boolean insert(int tDep, Map<T, Pair<Integer, Movement>> newPartialJourneys) {
        System.out.printf("insert called with %s\n", newPartialJourneys);

        // 1. find the index of the last entry (from the back) whose key is <= tDep
        int firstReachableEntryIdx = getFirstReachableEntry(tDep);

        // 2. filter dominated journeys in newPartialJourneys
        Iterator<Map.Entry<T, Pair<Integer, Movement>>> outerIt = newPartialJourneys.entrySet().iterator();

        while (outerIt.hasNext()) {
            Map.Entry<T, Pair<Integer, Movement>> entryCandidate = outerIt.next();
            T critCand = entryCandidate.getKey();
            int tArrCand = entryCandidate.getValue().getKey();

            boolean dominated = false;

            // TODO: swap 1) and 2) (better performance)

            // 1) check domination by any old journey
            for (int i = 0; i <= firstReachableEntryIdx && !dominated; i++) {
                for (Map.Entry<T, Pair<Integer, Movement>> entryOld : entries.get(i).getValue().entrySet()) {
                    T critOld = entryOld.getKey();
                    int tArrOld = entryOld.getValue().getKey();

                    if ((critOld.dominates(critCand) && tArrOld <= tArrCand) || (critOld.equals(critCand)
                            && tArrOld < tArrCand)) {

                        System.out.printf("%s dominates candidate %s\n", entryOld, entryCandidate);
                        dominated = true;
                        break;
                    }
                }
            }
            if (dominated) {

                outerIt.remove();
                continue;
            }

            // 2) check domination by any other new journey
            for (Map.Entry<T, Pair<Integer, Movement>> entryOther : newPartialJourneys.entrySet()) {
                if (entryOther == entryCandidate) {
                    continue;
                }

                T critOther = entryOther.getKey();
                int tArrOther = entryOther.getValue().getKey();

                if ((critOther.dominates(critCand) && tArrOther <= tArrCand)
                        || (critOther.equals(critCand) && tArrOther < tArrCand)) {

                    System.out.printf("%s dominates (among) %s\n", entryOther, entryCandidate);

                    dominated = true;
                    break;
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
            System.out.printf("create bag, %s\n", newPartialJourneys);

            // create a new map at insertionIdx with remaining newPartialJourneys entries
            entries.add(insertionIdx,
                    new Pair<Integer, Map<T, Pair<Integer, Movement>>>(tDep, newPartialJourneys));
        } else {
            System.out.printf("bag already exists, %s\n", newPartialJourneys);

            // add remaining newPartialJourneys entries to the map at insertionIdx
            entries.get(insertionIdx).getValue().putAll(newPartialJourneys);
        }

        // 4. remove partialJourneys that leave at/after tDep and that are now dominated
        // by the remaining newPartialJourneys
        for (int i = insertionIdx; i < entries.size(); i++) {
            Pair<Integer, Map<T, Pair<Integer, Movement>>> entry = entries.get(i);

            Map<T, Pair<Integer, Movement>> existingJourneys = entry.getValue();
            Iterator<Map.Entry<T, Pair<Integer, Movement>>> it = existingJourneys.entrySet().iterator();

            while (it.hasNext()) {
                Map.Entry<T, Pair<Integer, Movement>> oldEntry = it.next();
                T oldCriteria = oldEntry.getKey();
                int oldTArr = oldEntry.getValue().getKey();

                // check if this old journey is now dominated by any new one
                for (Map.Entry<T, Pair<Integer, Movement>> newEntry : newPartialJourneys.entrySet()) {
                    T newCriteria = newEntry.getKey();
                    int newTArr = newEntry.getValue().getKey();

                    if ((newCriteria.dominates(oldCriteria) && newTArr <= oldTArr)
                            || (newCriteria.equals(oldCriteria) &&
                                    newTArr < oldTArr)) {

                        System.out.printf("%s dominates (old) %s\n", newEntry, oldEntry);

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
    public Map<T, Pair<Integer, Movement>> evaluateAt(int tDep) {
        Map<T, Pair<Integer, Movement>> ret = new HashMap<>();

        // all the entries that leave at/after tdep
        for (int i = 0; i <= getFirstReachableEntry(tDep); i++) {
            for (Map.Entry<T, Pair<Integer, Movement>> entry : entries.get(i).getValue().entrySet()) {
                // if multiple times the same T, put at that T key the pair<arrivalTime,
                // Movement> that has the earliest arrivalTime
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
            Pair<Integer, Map<T, Pair<Integer, Movement>>> entry = entries.get(i);

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
