import java.util.ArrayList;

//Group of Entries and their encapsulating BoundingBox
public class EntryBoundingGroup {
    private ArrayList<Entry> entries;
    private BoundingBox boundingBox;

    public EntryBoundingGroup(ArrayList<Entry> entries, BoundingBox boundingBox) {
        this.entries = entries;
        this.boundingBox = boundingBox;
    }
    public ArrayList<Entry> getEntries() {
        return entries;
    }
    public BoundingBox getBoundingBox() {
        return boundingBox;
    }
}
