import java.util.ArrayList;

// Implements the entries at the bottom of a tree
public class LeafEntry extends Entry {
    private long recordId;
    private long dataFileBlockId; //The id of the block in the datafile where the record is saved
    public LeafEntry (long recordId, long dataFileBlockId, ArrayList<Bounds> recordBounds) {
        super (new BoundingBox(recordBounds));
        this.recordId = recordId;
        this.dataFileBlockId = dataFileBlockId;
    }
    public long getRecordId() {
        return recordId;
    }
    public long getDataFileBlockId() {
        return dataFileBlockId;
    }

}
