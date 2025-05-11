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
    // TransfersCount
    // -----------------------------------------------------

    default int getTransfersCount() {
        return 0;
    }

    default void setTransfersCount(int transfersCount) {
    }

    default void decTransfersCount() {
    }

}
