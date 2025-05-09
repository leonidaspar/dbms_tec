import java.util.ArrayList;
import java.util.Objects;

// Class used for executing a range query withing a specific circle with the use of the RStarTree
public class PointRadiusQuery extends Query {
    private ArrayList<Long> recordIdQualifiers;
    private ArrayList<Double> searchPoint;
    private double searchRadius;

    public PointRadiusQuery(ArrayList<Double> searchPoint, Double searchRadius) {
        this.searchPoint = searchPoint;
        this.searchRadius = searchRadius;
    }

    @Override
    ArrayList<Long> getQueryRecordIDs (Node node) {
        recordIdQualifiers = new ArrayList<>();
        search(node);
        return recordIdQualifiers;
    }

    private void search(Node node) {
        //Search subtrees. If node does not point to leaves check each entry E to determine whether E.R overlaps with the searchPoint.
        if (node.getLevel() != RStarTree.getLeafLevel())
            for (Entry entry : node.getEntries()) {
                // For all overlapping entries, invoke Search on the tree whose root is pointed to by E.childPTR.
                if (entry.getBoundingBox().checkOverlapWithPoint(searchPoint, searchRadius))
                    search(Objects.requireNonNull(DataHandler.readIndexFileBlock(entry.getBlockIdOfChildNode())));
            }
            //Search leaf node. If point to leaves, check all entries E to determine whether E.r overlaps S.
            //If so, E is a qualifying record
        else
            for (Entry entry : node.getEntries())
                //For all overlapping entries, invoke Search on the tree whose root is pointed to by E.childPTR.
                if (entry.getBoundingBox().checkOverlapWithPoint(searchPoint, searchRadius)) {
                    LeafEntry leafEntry = (LeafEntry) entry;
                    recordIdQualifiers.add(leafEntry.getRecordId());
                }
    }
}
