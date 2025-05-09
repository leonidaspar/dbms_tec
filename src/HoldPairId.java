//class used to hold the id of a record and its distance from an item
public class HoldPairId {
    private long recordID;
    private double distance;

    public HoldPairId(long recordID, double distance) {
        this.recordID = recordID;
        this.distance = distance;
    }
    public long getRecordID() {
        return recordID;
    }
    public double getDistance() {
        return distance;
    }
}
