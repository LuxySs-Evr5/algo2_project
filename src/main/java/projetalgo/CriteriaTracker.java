package projetalgo;

public interface CriteriaTracker {

    boolean dominates(CriteriaTracker criteriaTracker);


    default int getFootpathsCount() {
        return 0;
    }

    default void setFootpathsCount(int footpathsCount) {}

    default int decFootpathsCount() {
        return 0;
    }

    /**
     * This method mut be implemented using the attribute values instead of the address.
     */
    @Override
    boolean equals(Object obj);

}
