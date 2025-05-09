import java.util.ArrayList;

// Class used for executing a range query withing a specific bounding box with the use of the RStarTree
public class BoundingBoxRangeQuery extends Query {
    private ArrayList<Long> recordIdQualifiers; // Record ids used for queries
    private BoundingBox searchBox; // BoundingBox used for range queries

    public BoundingBoxRangeQuery(BoundingBox searchBox) {
        this.searchBox = searchBox;
    }
    @Override
    ArrayList<Long> getQueryRecordIDs (Node node) {
        recordIdQualifiers = new ArrayList<>();
        search(node);
        return recordIdQualifiers;
    }
    // Search for records within searchBoundingBox
    private void search(Node node) {
        //Search subtrees. If T is not a leaf check each entry E to determine whether E.R overlaps searchBoundingBox.
        if (node.getLevel()!= RStarTree.getLeafLevel())
            for (Entry entry : node.getEntries()) {
                // For all overlapping entries, invoke Search on the tree whose root is pointed to by E.childPTR.
                if (BoundingBox.checkOverlap(entry.getBoundingBox(),searchBox))
                    search(DataHandler.readIndexFileBlock(entry.getBlockIdOfChildNode()));
            }
            //Search leaf node
            //If T is a leaf, check all entries E to determine whether E.r overlaps S. If so, E is a qualifying record
        else
            for (Entry entry : node.getEntries())
                // For all overlapping entries, invoke Search on the tree whose root is pointed to by E.childPTR
                if (BoundingBox.checkOverlap(entry.getBoundingBox(),searchBox)) {
                    LeafEntry leafEntry = (LeafEntry) entry;
                    recordIdQualifiers.add(leafEntry.getRecordId());
                }
    }
}
