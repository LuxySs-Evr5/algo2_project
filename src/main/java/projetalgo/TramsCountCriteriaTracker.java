package projetalgo;

public class TramsCountCriteriaTracker implements CriteriaTracker {
    private int tramsCount;

    TramsCountCriteriaTracker() {
        this.tramsCount = 0;
    }

    @Override
    public int getTramsCount() {
        return tramsCount;
    }

    @Override
    public void setTramsCount(int tramsCount) {
        this.tramsCount = tramsCount;
    }

    @Override
    public void decTramsCount() {
        tramsCount--;
    }

    @Override
    public boolean dominates(CriteriaTracker criteriaTracker) {
        if (criteriaTracker instanceof TramsCountCriteriaTracker otherCritTracker) {
            return getTramsCount() < otherCritTracker.getTramsCount();
        } else {
            throw new IllegalArgumentException("dominates: argument 'other' must be a TramsCountCriteriaTracker");
        }
    }

    @Override
    public String toString() {
        return String.format("tramsCount: %d", tramsCount);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!(obj instanceof TramsCountCriteriaTracker other))
            return false;
        return this.tramsCount == other.tramsCount;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(tramsCount);
    }

}
