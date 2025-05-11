package projetalgo;

public class TransfersCountCriteriaTracker implements CriteriaTracker {
    private int transfersCount;

    TransfersCountCriteriaTracker() {
        this.transfersCount = 0;
    }

    @Override
    public int getTransfersCount() {
        return transfersCount;
    }

    @Override
    public void setTransfersCount(int transfersCount) {
        this.transfersCount = transfersCount;
    }

    @Override
    public void decFootpathsCount() {
        transfersCount--;
    }

    @Override
    public boolean dominates(CriteriaTracker other) {
        // TODO: the type checking is very bad and handled poorly

        if (other instanceof TransfersCountCriteriaTracker otherCritTracker) {
            return getTransfersCount() < otherCritTracker.getTransfersCount();
        } else {
            throw new IllegalArgumentException("dominates: argument 'other' must be a TransfersCountCriteriaTracker");
        }

    }

    @Override
    public String toString() {
        return String.format("transfersCount: %d", transfersCount);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!(obj instanceof TransfersCountCriteriaTracker other))
            return false;
        return this.transfersCount == other.transfersCount;
    }

    // ensure that it will only use the transfersCount in our profile function map
    @Override
    public int hashCode() {
        return Integer.hashCode(transfersCount);
    }

}
