package projetalgo;

public interface CriteriaTracker {

    CriteriaTracker copy();

    /**
     * Returns true if the the "this" CriteriaTracker dominates the given criteria
     * tracker.
     */
    boolean dominates(CriteriaTracker criteriaTracker);

    /**
     * This method mut be implemented using the attribute values instead of the
     * address.
     */
    @Override
    boolean equals(Object obj);

    /**
     * Returns a new CriteriaTracker updated according to the given movement. For
     * example, if m is a bus connection and this tracker counts the number of buses
     * taken, then the overridden method should increment the bus count.
     *
     * @param m the movement to apply
     * @return a new CriteriaTracker with the movement applied
     */
    CriteriaTracker addMovement(Movement m);

    // -----------------------------------------------------
    // FootpathsCount
    // -----------------------------------------------------

    default int getFootpathsCount() {
        return 0;
    }

    default void decFootpathsCount() {
    }

    // -----------------------------------------------------
    // TramsCount
    // -----------------------------------------------------

    default int getTramsCount() {
        return 0;
    }

    default void decTramsCount() {
    }

    // -----------------------------------------------------
    // BusesCount
    // -----------------------------------------------------

    default int getBusesCount() {
        return 0;
    }

    default void decBusesCount() {
    }

    // -----------------------------------------------------
    // TrainsCount
    // -----------------------------------------------------

    default int getTrainsCount() {
        return 0;
    }

    default void decTrainsCount() {
    }

    // -----------------------------------------------------
    // MetrosCount
    // -----------------------------------------------------

    default int getMetrosCount() {
        return 0;
    }

    default void decMetrosCount() {
    }

}
