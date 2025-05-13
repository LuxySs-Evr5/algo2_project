package projetalgo;

public interface CriteriaTracker {

    boolean dominates(CriteriaTracker criteriaTracker);

    /**
     * This method mut be implemented using the attribute values instead of the
     * address.
     */
    @Override
    boolean equals(Object obj);

    // -----------------------------------------------------
    // FootpathsCount
    // -----------------------------------------------------

    default int getFootpathsCount() {
        return 0;
    }

    default void setFootpathsCount(int footpathsCount) {
    }

    default void decFootpathsCount() {
    }

    // -----------------------------------------------------
    // TramsCount
    // -----------------------------------------------------

    default int getTramsCount() {
        return 0;
    }

    default void setTramsCount(int tramsCount) {
    }

    default void decTramsCount() {
    }

}
