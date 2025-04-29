package projetalgo;

public interface CriteriaTracker {

    public boolean dominates(CriteriaTracker criteriaTracker);

    /**
     * This method mut be implemented using the attribute values instead of the address.
     */
    @Override
    public boolean equals(Object obj);

}
