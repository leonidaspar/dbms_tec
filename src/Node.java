import java.io.Serializable;
import java.util.ArrayList;

/**
 * Node class represents a node in the R*-tree.
 * Each node contains a list of entries (which can be either data objects or child nodes).
 * The node supports dynamic splitting using R*-tree heuristics (overlap, area, and margin-based criteria).
 */
public class Node implements Serializable {
    private static final int MAX_ENTRIES = DataHandler.calculateMaxEntriesInNode();
    private static final int MIN_ENTRIES = (int) (0.4 * MAX_ENTRIES);
    private long level; //Level of Node in the tree: 0 = leaf, higher values = internal nodes
    private long blockId; //Unique identifier for disk block
    private ArrayList<Entry> entries; //Entries of this node

    public Node(long level, ArrayList<Entry> entries) {
        this.level = level;
        this.entries = entries;
    }

    public Node (long level) {
        this.level = level;
        this.entries = new ArrayList<>();
        this.blockId = RStarTree.getRootBlockId();
    }
    public long getLevel() {
        return level;
    }
    public long getBlockId() {
        return blockId;
    }
    public ArrayList<Entry> getEntries() {
        return entries;
    }
    public void setBlockId(long blockId) {
        this.blockId = blockId;
    }
    public void setEntries(ArrayList<Entry> entries) {
        this.entries = entries;
    }

    public void addEntry(Entry entry) {
        entries.add(entry);
    }
    static int getMaxEntriesInNode() {
        return MAX_ENTRIES;
    }
    static int getMinEntriesInNode() {
        return MIN_ENTRIES;
    }

    //IMPLEMENT ROOT CONSTRUCTOR

    /**
     * Finds the best axis and split points for node splitting.
     * Follows the R*-tree strategy:
     * 1. For each axis:
     *    - Sort entries based on their lower and upper bounds.
     *    - Generate all valid splits (ensuring each group has ≥ MIN_ENTRIES).
     *    - Calculate total margin (sum of perimeters) for all splits.
     * 2. Choose the axis with the minimum total margin.
     *
     * @return EntryBoundingSplits along the best axis (least margin).
     */
    private ArrayList<EntryBoundingSplit> getSplitAxis () {
        double axisMarginSum = Double.MAX_VALUE;
        ArrayList<EntryBoundingSplit> splitAxisEntryBoundingSplits = new ArrayList<>();
        for (int i = 0; i < DataHandler.getDataDimensions(); i++) {
            ArrayList<Entry> upperSort = new ArrayList<>();
            ArrayList<Entry> lowerSort = new ArrayList<>();

            for (Entry entry : entries) {
                upperSort.add(entry);
                lowerSort.add(entry);
            }

            // Sort entries on axis i by lower and upper bounds
            upperSort.sort(new ComparatorsForEntries.CompareOnBounds(upperSort,i,false));
            lowerSort.sort(new ComparatorsForEntries.CompareOnBounds(lowerSort,i,false));

            ArrayList<ArrayList<Entry>> sortedEntries = new ArrayList<>();
            sortedEntries.add(upperSort);
            sortedEntries.add(lowerSort);

            double sum = 0;
            ArrayList<EntryBoundingSplit> entryBoundingSplits = new ArrayList<>();
            for (ArrayList<Entry> entryList : sortedEntries) {
                // Create splits between MIN_ENTRIES and MAX_ENTRIES - MIN_ENTRIES
                for (int j = 1; j <= MAX_ENTRIES - 2 * MIN_ENTRIES + 2; j++) {
                    ArrayList<Entry> groupA = new ArrayList<>();
                    ArrayList<Entry> groupB = new ArrayList<>();
                    //Fist group contains (m-1)+j entries, second the rest
                    for (int k = 0 ; k < (MIN_ENTRIES - 1)+j; k++)
                        groupA.add(entryList.get(k));
                    for (int k = (MIN_ENTRIES - 1)+j; k < entries.size(); k++)
                        groupB.add(entryList.get(k));

                    BoundingBox bbGroupA = new BoundingBox(Bounds.findMinBounds(groupA));
                    BoundingBox bbGroupB = new BoundingBox(Bounds.findMinBounds(groupB));

                    EntryBoundingSplit entryBoundingSplit = new EntryBoundingSplit(new EntryBoundingGroup(groupA,bbGroupA), new EntryBoundingGroup(groupB,bbGroupB));
                    entryBoundingSplits.add(entryBoundingSplit);
                    // Sum of margins (used to select best axis)
                    sum += bbGroupA.getPerimeter() + bbGroupB.getPerimeter();
                }
                // Keep the axis with minimal total margin
                if (axisMarginSum > sum) {
                    axisMarginSum = sum;
                    splitAxisEntryBoundingSplits = entryBoundingSplits;
                }
            }
        }
        return splitAxisEntryBoundingSplits;
    }

    /**
     * From the provided splits along the best axis, pick the best split pair
     * based on minimum overlap (and area if overlaps are equal).
     *
     * @param splitAxis List of EntryBoundingSplits from the chosen axis.
     * @return Two nodes representing the result of the split.
     */
    public ArrayList<Node> getSplitIndex(ArrayList<EntryBoundingSplit> splitAxis) {
        if (splitAxis.isEmpty())
            throw new IllegalArgumentException("Wrong group size.");
        double minOverlap = Double.MAX_VALUE;
        double minArea = Double.MAX_VALUE;
        int bestIndex = 0;
        //For the chosen split axis, choose the EntryBoundingSplit with the minimum overlap
        for (int i = 0; i < splitAxis.size(); i++) {
            EntryBoundingGroup groupA = splitAxis.get(i).getGroupA();
            EntryBoundingGroup groupB = splitAxis.get(i).getGroupB();

            double overlap = BoundingBox.findOverlap(groupA.getBoundingBox(), groupB.getBoundingBox());
            if (overlap < minOverlap) {
                minOverlap = overlap;
                bestIndex = i;
                minArea = groupA.getBoundingBox().getPerimeter() + groupB.getBoundingBox().getPerimeter();
            } else if (overlap == minOverlap) {
                double area = groupA.getBoundingBox().getPerimeter() + groupB.getBoundingBox().getPerimeter();
                if (area < minArea) {
                    minArea = area;
                    bestIndex = i;
                }
            }
        }
        ArrayList<Node> splitNodes = new ArrayList<>();
        EntryBoundingGroup groupA = splitAxis.get(bestIndex).getGroupA();
        EntryBoundingGroup groupB = splitAxis.get(bestIndex).getGroupB();
        splitNodes.add(new Node(level , groupA.getEntries()));
        splitNodes.add(new Node(level , groupB.getEntries()));
        return splitNodes;
    }

    /**
     * Performs the full node split process by:
     * 1. Finding best axis via getSplitAxis.
     * 2. Choosing best split via getSplitIndex.
     *
     * @return Two nodes after splitting this node.
     */
    public ArrayList<Node> split() {
        ArrayList<EntryBoundingSplit> splitAxis = getSplitAxis();
        return getSplitIndex(splitAxis);
    }
}
