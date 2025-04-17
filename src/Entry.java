import java.io.Serializable;
import java.util.ArrayList;

//Represents a reference to a child node in the R*-Tree along with its bounding box.
//Used in both internal and leaf nodes to store spatial coverage and linkage.
public class Entry implements Serializable {
    private BoundingBox boundingBox;
    private Long blockIdOfChildNode;

    public Entry(Node childNode) {
        blockIdOfChildNode = childNode.getBlockId();
        setBoundingBoxToFitEntries(childNode.getEntries());
    }
    public Entry(BoundingBox boundingBox) {
        this.boundingBox = boundingBox;
    }
    public BoundingBox getBoundingBox() {
        return boundingBox;
    }
    public Long getBlockIdOfChildNode() {
        return blockIdOfChildNode;
    }

    public void setBlockIdOfChildNode(Long blockIdOfChildNode) {
        this.blockIdOfChildNode = blockIdOfChildNode;
    }

    //Alter BoundingBox so that it fits the new Entry given by extending the minimum bounds
    void setBoundingBoxToFitEntries(Entry entry) {
        boundingBox = new BoundingBox(Bounds.findMinBounds(boundingBox, entry.getBoundingBox()));
    }

    //Alter BoundingBox so that it fits the new Entry given by extending the minimum bounds
    void setBoundingBoxToFitEntries (ArrayList<Entry> entries) {
        boundingBox = new BoundingBox(Bounds.findMinBounds(entries));
    }
}
