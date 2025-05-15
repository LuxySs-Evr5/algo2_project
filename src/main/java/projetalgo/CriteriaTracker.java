package projetalgo;

public interface CriteriaTracker {

    CriteriaTracker copy();

    boolean dominates(CriteriaTracker criteriaTracker);

    /**
     * This method mut be implemented using the attribute values instead of the
     * address.
     */
    @Override
    boolean equals(Object obj);

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

}
