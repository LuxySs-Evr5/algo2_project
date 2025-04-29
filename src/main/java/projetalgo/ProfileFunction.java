package projetalgo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javafx.util.Pair;

public class ProfileFunction<T extends CriteriaTracker> {
    // List<Pair<depatureTime, Map<CriteriaTracker --> Pair<arrivalTime,
    // movement>>>>, sorted by depatureTime
    private List<Pair<Integer, Map<T, Pair<Integer, Movement>>>> entries;

    public ProfileFunction() {
        this.entries = new ArrayList<>();
    }

    /**
     * Returns the Movement that matches firt with the given CriteriaTracker and
     * depart at/after the given tDep.
     * (Scanning by increasing tDep).
     */
    public Movement getFirstMatch(int tDep, CriteriaTracker criteriaTracker) {
        for (Pair<Integer, Map<T, Pair<Integer, Movement>>> entry : entries.subList(getFirstReachableEntry(tDep),
                entries.size())) {
            Map<T, Pair<Integer, Movement>> map = entry.getValue();

            if (map.containsKey(criteriaTracker)) {
                Movement movement = map.get(criteriaTracker).getValue();
                return movement;
            }
        }

        return null;
    }

    // TODO: make this use a binary search ?
    // not ure it's such a good idea since mot of the time we add at the front (or
    // near the front) of the list,
    // so scanning the whole list could be worse than linearly scanning from the
    // start
    private int getFirstReachableEntry(int tDep) {
        int firstReachableEntryIdx = 0;
        while (firstReachableEntryIdx < entries.size() && entries.get(firstReachableEntryIdx).getKey() < tDep) {
            firstReachableEntryIdx++;
        }

        return firstReachableEntryIdx;
    }

    /**
     * @param tDep               is the departureTime
     * @param newPartialJourneys a map that associates each T to a
     *                           Pair<arrivalTime, Movement> where T is a
     *                           CriteriaTracker that keeps track of the stats for
     *                           each criteria we are interested in for the partial
     *                           journey.
     */
    public void insert(int tDep, Map<T, Pair<Integer, Movement>> newPartialJourneys) { // NOTE: newPartialJourneys is
                                                                                       // article
        // find the index from which all the entries keys are >=tDep
        int firstReachableEntryIdx = getFirstReachableEntry(tDep);

        // keep track of which partial journeys get added so we can then remove the ones
        // that are dominated by those
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

            // 2. there is already a pair<departureTime, map> such that departureTime >=
            // tDep
        } else {

            // iterate over the new partial journeys that we want to insert
            for (Map.Entry<T, Pair<Integer, Movement>> newPartialJourney : newPartialJourneys.entrySet()) {

                // the first entry whose departure time is >= tDep
                Pair<Integer, Map<T, Pair<Integer, Movement>>> firstReachableEntry = entries
                        .get(firstReachableEntryIdx);

                T newPartialJourneyCriteria = newPartialJourney.getKey();
                int newPartialJourneyTArr = newPartialJourney.getValue().getKey();

                boolean dominated = false;

                // iterate over all the things that depart at/after tdep
                for (int i = firstReachableEntryIdx; i < entries.size(); i++) {
                    Pair<Integer, Map<T, Pair<Integer, Movement>>> currentEntry = entries.get(i);

                    // iterate over their <key, values>
                    for (Map.Entry<T, Pair<Integer, Movement>> partialJourney : currentEntry.getValue().entrySet()) {

                        T partialJourneyCriteria = partialJourney.getKey();
                        int partialJourneyTArr = partialJourney.getValue().getKey();

                        // check dominated with the arrival time included
                        if ((partialJourneyCriteria.dominates(newPartialJourneyCriteria)
                                && partialJourneyTArr <= newPartialJourneyTArr) ||
                                (partialJourneyCriteria.equals(newPartialJourneyCriteria)
                                        && partialJourneyTArr < newPartialJourneyTArr)) {
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

                        // add the new map in entries
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

            // for each partial journey in the bags that leave before/at tdep
            for (Pair<Integer, Map<T, Pair<Integer, Movement>>> currentEntry : entries.subList(0,
                    firstReachableEntryIdx)) {

                List<T> partialJourneysNowDominated = new ArrayList<T>();

                // for each journey in that bag
                for (Map.Entry<T, Pair<Integer, Movement>> partialJourney : currentEntry.getValue().entrySet()) {

                    T partialJourneyCriteria = partialJourney.getKey();
                    int partialJourneyTArr = partialJourney.getValue().getKey();

                    // a partial journey (that leaves at/before tdep) is now dominated by one of
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
