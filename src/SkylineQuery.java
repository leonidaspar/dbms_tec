import java.util.ArrayList;

/** 
 * Class used for skyline query  
 * It collects all record id's whose points are not dominated by any other point.
 */
public class SkylineQuery extends Query {
    private ArrayList<Long> skylineRecordIds;    // Record IDs of skyline points
    private ArrayList<LeafEntry> skylineEntries; // Current skyline entries (with coordinates for dominance checks)

    public SkylineQuery() {
        this.skylineRecordIds = new ArrayList<>();
        this.skylineEntries = new ArrayList<>();
    }

    @Override
    ArrayList<Long> getQueryRecordIDs(Node node) {
        findSkyline(node);
        return skylineRecordIds;
    }

    // Recursive method to traverse the R*-tree and find skyline points
    private void findSkyline(Node node) {
        if (node.getLevel() != RStarTree.getLeafLevel()) {
            // Internal node
            for (Entry entry : node.getEntries()) {
                BoundingBox box = entry.getBoundingBox();
                if (!isDominatedBySkyline(box)) {
                    // Only explore this subtree if its bounding box is not dominated by any known skyline point
                    Node childNode = DataHandler.readIndexFileBlock(entry.getBlockIdOfChildNode());
                    findSkyline(childNode);
                }
            }
        } else {
            // Leaf node
            for (Entry entry : node.getEntries()) {
                LeafEntry leafEntry = (LeafEntry) entry;
                BoundingBox pointBox = leafEntry.getBoundingBox();
                if (!isDominatedBySkyline(pointBox)) {
                    
                    // Remove  existing skyline points that are dominated by this new point
                    for (int j = skylineEntries.size() - 1; j >= 0; j--) {
                        LeafEntry currentSky = skylineEntries.get(j);
                        if (dominates(pointBox, currentSky.getBoundingBox())) {
                            // New point dominates an existing skyline point: remove the dominated point
                            skylineEntries.remove(j);
                            skylineRecordIds.remove(j);
                        }
                    }
                    // add the new skyline point
                    skylineEntries.add(leafEntry);
                    skylineRecordIds.add(leafEntry.getRecordId());
                }
            }
        }
    }

    /** 
     * Check if the given bounding box is made by any point in the current skyline.
        * Returns true if the box is dominated by any existing skyline point.
     */
    private boolean isDominatedBySkyline(BoundingBox box) {
        for (LeafEntry skyEntry : skylineEntries) {
            if (dominates(skyEntry.getBoundingBox(), box)) {
                return true;  
            }
        }
        return false;
    }

    // Check if boxA dominates boxB
    private boolean dominates(BoundingBox boxA, BoundingBox boxB) {
        boolean strictlyLessInOne = false;
        int d = DataHandler.getDataDimensions();  // number of dimensions from metadata
        for (int i = 0; i < d; i++) {
            double aVal = boxA.getBounds().get(i).getLower();  // A's coordinate (or min bound in this dimension)
            double bVal = boxB.getBounds().get(i).getLower();  // B's coordinate (or min bound)
            if (aVal > bVal) { 
                // A is greater in this dimension, so it cannot dominate B
                return false;
            }
            if (aVal < bVal) {
                strictlyLessInOne = true;
            }
        }
        return strictlyLessInOne;
    }
}
