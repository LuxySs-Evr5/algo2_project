package projetalgo;

public class FootpathsCountCriteriaTracker implements CriteriaTracker {
    private int footpathsCount;

    FootpathsCountCriteriaTracker() {
        this.footpathsCount = 0;
    }

    @Override
    public int getFootpathsCount() {
        return footpathsCount;
    }

    @Override
    public void setFootpathsCount(int footpathsCount) {
        this.footpathsCount = footpathsCount;
    }

    @Override
    public void decFootpathsCount() {
        footpathsCount--;
    }

    @Override
    public boolean dominates(CriteriaTracker criteriaTracker) {
        if (criteriaTracker instanceof FootpathsCountCriteriaTracker otherCritTracker) {
            return getFootpathsCount() < otherCritTracker.getFootpathsCount();
        } else {
            throw new IllegalArgumentException("dominates: argument 'other' must be a FootpathsCountCriteriaTracker");
        }
    }

    @Override
    public String toString() {
        return String.format("footpathsCount: %d", footpathsCount);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!(obj instanceof FootpathsCountCriteriaTracker other))
            return false;
        return this.footpathsCount == other.footpathsCount;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(footpathsCount);
    }

}
