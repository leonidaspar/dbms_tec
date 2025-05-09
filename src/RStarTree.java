import java.util.ArrayList;
import java.util.Collections;

public class RStarTree {
    private static int levels; //Total levels of the tree incrementing from root. Root always has the highest level
    private static boolean[] levelsInserted;
    private static final int ROOT_BLOCKID = 1; // Root always has 1 as its id as it is used to easily identify which block has the root
    private static final int LEAF_LEVEL = 1;
    //Instead of scanning all possible entries in an internal node to find the best one to descend into
    //the R*-Tree limits the selection to P best candidate entries based on minimum overlap enlargement or
    //minimum area enlargement. When choosing a subtree to descend into, evaluate only the top P promising entries, not all of them.
    private static final int CHOOSE_SUBTREE_P_ENTRIES = 32;

    //Overflow treatment. When a node overflows some entries are removed and added elsewhere in the tree to reduce overlap
    //REINSERT_P_ENTRIES defines how many entries to reinsert.
    private static final int REINSERT_P_ENTRIES = (int) (0.3*Node.getMaxEntriesInNode());
    
    public RStarTree (boolean insertFromDataFile) {
        this.levels = (int) DataHandler.getTotalLevelsOfTreeIndex();
        if (insertFromDataFile) {
            DataHandler.writeIndexFileBlock(new Node(1));

            for (int i=1; i<DataHandler.getTotalBlocksInDataFile(); i++) {
                ArrayList<Record> records = DataHandler.readDataFileBlock(i);
                if (records != null)
                    for (Record record : records)
                        addRecord(record,i);
                else
                    throw new IllegalStateException("Error in reading data file");
            }
        }
    }
    public Node getRoot() {
        return DataHandler.readIndexFileBlock(ROOT_BLOCKID);
    }
    public static int getRootBlockId() {
        return ROOT_BLOCKID;
    }
    public static int getLeafLevel() {
        return LEAF_LEVEL;
    }

    public ArrayList<Long> getNearestNeighbours(ArrayList<Double> point, int howMany) {
        Query query = new NearestNeighboursQuery(point,howMany);
        return query.getQueryRecordIDs(DataHandler.readIndexFileBlock(ROOT_BLOCKID));
    }

    public ArrayList<Long> getDataInBB(BoundingBox boundingBox) {
        Query query = new BoundingBoxRangeQuery(boundingBox);
        return query.getQueryRecordIDs(DataHandler.readIndexFileBlock(ROOT_BLOCKID));
    }

    public ArrayList<Long> getDataInCircle(ArrayList<Double> point, double radius) {
        Query query = new PointRadiusQuery(point,radius);
        return query.getQueryRecordIDs(DataHandler.readIndexFileBlock(ROOT_BLOCKID));
    }


    public void addRecord(Record record, long dataFileBlockId) {
        ArrayList<Bounds> boundsForEachDimension = new ArrayList<>();

        // Since we have to deal with points as records we set low and upper to be same
        for (int i = 0; i < DataHandler.getDataDimensions(); i++)
            boundsForEachDimension.add(new Bounds(record.getCoordinateInDimension(i), dataFileBlockId));

        levelsInserted = new boolean[levels];
        insert(null,null, new LeafEntry(record.getId(), dataFileBlockId, boundsForEachDimension), LEAF_LEVEL);

    }

    // Inserts nodes recursively. As an optimization, the algorithm steps are
    // in a different order. If this returns a non null Entry, then
    // that Entry should be added to the caller's Node of the R-tree
    private Entry insert(Node parentNode,Entry parentEntry,Entry dataEntry,int levelToAdd) {
        Node childNode;
        long idToRead;

        if (parentEntry == null)
            idToRead = ROOT_BLOCKID;
        else {
            // Updating-Adjusting the bounding box of the Entry that points to the Updated Node
            parentEntry.setBoundingBoxToFitEntries(dataEntry);
            DataHandler.updateIndexFileBlock(parentNode, levels);
            idToRead = parentEntry.getBlockIdOfChildNode();
        }
        childNode = DataHandler.readIndexFileBlock(idToRead);
        if (childNode == null)
            throw new IllegalStateException("The node block read was null");

        // CS2: If we're at a leaf (or the level we wanted to insert the dataEntry), then use that level
        // I2: If N has less than M items, accommodate E in N
        if (childNode.getLevel() == levelToAdd) {
            childNode.addEntry(dataEntry);
            DataHandler.updateIndexFileBlock(childNode, levels);
        }
        else {
            // I1: Invoke ChooseSubtree. with the level as a parameter,
            // to find an appropriate node N, m which to place the new leaf E

            // Recurse to get the node that the new data entry will fit better
            Entry bestEntry = chooseSubTree(childNode,dataEntry.getBoundingBox(),levelToAdd);
            // Receiving a new Entry if the recursion caused the next level's Node to split
            Entry newEntry = insert(childNode,bestEntry,dataEntry,levelToAdd);

            childNode = DataHandler.readIndexFileBlock(idToRead);
            if (childNode == null)
                throw new IllegalStateException("The node block read was null");
            //If split was called on children, the new entry that the split caused gets joined to the list of items at this level
            if (newEntry != null) {
                childNode.addEntry(newEntry);
                DataHandler.updateIndexFileBlock(childNode, levels);
            } else {
                //no split was called on children, returning null upwards
                DataHandler.updateIndexFileBlock(childNode, levels);
                return null;
            }
        }
        // If N has M+1 items, call overflowHandler with the level of N as a parameter [for reinsertion or split]
        if (childNode.getEntries().size() > Node.getMaxEntriesInNode()) {
            // I3: If overfowHandler was called and a split was performed, propagate overflowHandler upwards if necessary
            return overflowHandler(parentNode,parentEntry,childNode);
        }
        return null;
    }

    // Returns the best Entry of the sub tree to place the new index entry
    // The loop portion of this algorithm is taken out, so it only picks a subtree at that particular level
    private Entry chooseSubTree(Node node, BoundingBox boundingBoxToAdd, int levelToAdd) {
        Entry bestEntry;

        // If the child pointers in N point to leaves
        if (node.getLevel() == levelToAdd+1) {
            // Alternative for large node sizes, determine the nearly minimum overlap cost
            if (Node.getMaxEntriesInNode() > (CHOOSE_SUBTREE_P_ENTRIES *2)/3 && node.getEntries().size() > CHOOSE_SUBTREE_P_ENTRIES) {
                // Sorting the entries in the node in increasing order of
                // their volume enlargement needed to include the new data rectangle
                ArrayList<EntryAreaEnlargementPair> entryAreaEnlargementPairs = new ArrayList<>();
                for (Entry entry: node.getEntries()) {
                    BoundingBox bbA = new BoundingBox(Bounds.findMinBounds(entry.getBoundingBox(),boundingBoxToAdd));
                    double areaEnlargementA = bbA.getVolume() - entry.getBoundingBox().getVolume();
                    entryAreaEnlargementPairs.add(new EntryAreaEnlargementPair(entry,areaEnlargementA));
                }
                entryAreaEnlargementPairs.sort(EntryAreaEnlargementPair::compareTo);
                // Let sortedByEnlargementEntries be the group of the sorted entries
                ArrayList<Entry> sortedByEnlargementEntries = new ArrayList<>();
                for (EntryAreaEnlargementPair pair: entryAreaEnlargementPairs)
                    sortedByEnlargementEntries.add(pair.getEntry());

                // From the items in sortedByEnlargementEntries, let A be the group of the first p entries,
                // considering all items in the node, choosing the entry whose rectangle needs least overlap enlargement
                bestEntry = Collections.min(sortedByEnlargementEntries.subList(0, CHOOSE_SUBTREE_P_ENTRIES), new ComparatorsForEntries.CompareOnOverlapIncrease(sortedByEnlargementEntries.subList(0, CHOOSE_SUBTREE_P_ENTRIES),boundingBoxToAdd,node.getEntries()));

                return bestEntry;
            }

            // Choose the entry in the node whose rectangle needs least overlap enlargement to include the new data rectangle
            // Resolve ties by choosing the entry whose rectangle needs least area enlargement,
            // then the entry with the rectangle of smallest area
            bestEntry = Collections.min(node.getEntries(), new ComparatorsForEntries.CompareOnOverlapIncrease(node.getEntries(),boundingBoxToAdd,node.getEntries()));
            return bestEntry;
        }

        // If the child pointers in N do not point to leaves: determine the minimum area cost],
        // choose the leaf in N whose rectangle needs least area enlargement to include the new data
        // rectangle. Resolve ties by choosing the leaf with the rectangle of smallest area
        ArrayList<EntryAreaEnlargementPair> entryAreaEnlargementPairs = new ArrayList<>();
        for (Entry entry: node.getEntries())
        {
            BoundingBox bbA = new BoundingBox(Bounds.findMinBounds(entry.getBoundingBox(),boundingBoxToAdd));
            double areaEnlargementA = bbA.getVolume() - entry.getBoundingBox().getVolume();
            entryAreaEnlargementPairs.add(new EntryAreaEnlargementPair(entry,areaEnlargementA));
        }

        bestEntry = Collections.min(entryAreaEnlargementPairs,EntryAreaEnlargementPair::compareTo).getEntry();
        return bestEntry;
    }

    // Algorithm OverflowTreatment
    private Entry overflowHandler(Node parentNode, Entry parentEntry, Node childNode) {

        // If the level is not the root level and this is the first call of OverflowTreatment
        // in the given level during the insertion of one data rectangle, then reinsert
        if (childNode.getBlockId() != ROOT_BLOCKID && !levelsInserted[(int) (childNode.getLevel()-1)])
        {
            levelsInserted[(int) (childNode.getLevel()-1)] = true; // Mark level as already inserted
            reInsert(parentNode,parentEntry,childNode);
            return null;
        }

        // Else invoke Split
        ArrayList<Node> splitNodes = childNode.split(); // The two nodes occurring after the split
        if (splitNodes.size() != 2)
            throw new IllegalStateException("The resulting Nodes after a split cannot be more or less than two");
        childNode.setEntries(splitNodes.get(0).getEntries()); // Adjusting the previous Node with the new entries
        Node splitNode = splitNodes.get(1); // The new Node that occurred from the split

        // Updating the file with the new changes of the split nodes
        if (childNode.getBlockId() != ROOT_BLOCKID)
        {
            DataHandler.updateIndexFileBlock(childNode,levels);
            splitNode.setBlockId(DataHandler.getTotalBlocksInIndexFile());
            DataHandler.writeIndexFileBlock(splitNode);

            // Propagate the overflow treatment upwards, to fit the entry on the caller's level Node
            parentEntry.setBoundingBoxToFitEntries(childNode.getEntries()); // Adjusting the bounding box of the Entry that points to the updated Node
            DataHandler.updateIndexFileBlock(parentNode,levels); // Write changes to file
            return new Entry(splitNode);
        }

        // Else if OverflowTreatment caused a split of the root, create a new root

        // Creating two Node-blocks for the split
        childNode.setBlockId(DataHandler.getTotalBlocksInIndexFile());
        DataHandler.writeIndexFileBlock(childNode);
        splitNode.setBlockId(DataHandler.getTotalBlocksInIndexFile());
        DataHandler.writeIndexFileBlock(splitNode);

        // Updating the root Node-block with the new root Node
        ArrayList<Entry> newRootEntries = new ArrayList<>();
        newRootEntries.add(new Entry(childNode));
        newRootEntries.add(new Entry(splitNode));
        Node newRoot = new Node(++levels,newRootEntries);
        newRoot.setBlockId(ROOT_BLOCKID);
        DataHandler.updateIndexFileBlock(newRoot,levels);
        return null;
    }

    // Algorithm reinsert
    private void reInsert(Node parentNode, Entry parentEntry, Node childNode) {

        if(childNode.getEntries().size() != Node.getMaxEntriesInNode() + 1)
            throw new IllegalStateException("Cannot throw reinsert for node with total entries fewer than M+1");

        // RI1 For all M+l items of a node N, compute the distance between the centers of their rectangles
        // and the center of the bounding rectangle of N

        // RI2: Sort the items in INCREASING order (since then we use close reinsert)
        // of their distances computed in RI1
        childNode.getEntries().sort(new ComparatorsForEntries.CompareOnDistanceFromCenter(childNode.getEntries(),parentEntry.getBoundingBox()));
        ArrayList<Entry> removedEntries = new ArrayList<>(childNode.getEntries().subList(childNode.getEntries().size()-REINSERT_P_ENTRIES,childNode.getEntries().size()));

        // RI3: Remove the last p items from N (since then we use close reinsert) and adjust the bounding rectangle of N
        for(int i = 0; i < REINSERT_P_ENTRIES; i++)
            childNode.getEntries().remove(childNode.getEntries().size()-1);

        // Updating bounding box of node and to the parent entry
        parentEntry.setBoundingBoxToFitEntries(childNode.getEntries());
        DataHandler.updateIndexFileBlock(parentNode,levels);
        DataHandler.updateIndexFileBlock(childNode,levels);

        // RI4: In the sort, defined in RI2, starting with the minimum distance (= close reinsert),
        // invoke Insert to reinsert the items
        if(removedEntries.size() != REINSERT_P_ENTRIES)
            throw new IllegalStateException("Entries queued for reinsert have different size than the ones that were removed");

        for (Entry entry : removedEntries)
            insert(null,null,entry,(int) childNode.getLevel());
    }


    // Algorithm for deleting a record based on Beckmann et al. (R*-tree paper)
    
    public boolean delete(long recordId) {
        // ❶ Search path stacks (node + entry that led to it)
        ArrayList<Node> pathNodes = new ArrayList<>();
        ArrayList<Entry> pathEntries = new ArrayList<>();

        // step down to the leaf that *may* contain the target
        Node n = getRoot();
        pathNodes.add(n);
        while (!n.isLeaf()) {
            // pick the child whose bbox contains the point id (quick & dirty)
            Entry next = null;
            for (Entry e : n.getEntries()) {
                if (recordIdInside(e, recordId)) {   // helper below
                    next = e; break;
                }
            }
            if (next == null) return false;          // not found
            pathEntries.add(next);
            n = DataHandler.readIndexFileBlock(next.getBlockIdOfChildNode());
            if (n == null) return false;
            pathNodes.add(n);
        }

        /* ❷ Locate the LeafEntry inside that leaf node                    */
        LeafEntry target = null;
        for (Entry e : n.getEntries()) {
            if (e instanceof LeafEntry le && le.getRecordId() == recordId) {
                target = le; break;
            }
        }
        if (target == null) return false;            // id absent

        n.removeEntry(target);                       // physical removal
        DataHandler.updateIndexFileBlock(n, levels); // persist leaf

        /* ❸ Condense-Tree upward                                         */
        condenseTree(pathNodes, pathEntries);

        /* ❹ Root adjustment (if root shrank and is not a leaf)           */
        Node root = getRoot();
        if (!root.isLeaf() && root.getEntries().size() == 1) {
            Entry only = root.getEntries().getFirst();
            Node newRoot = DataHandler.readIndexFileBlock(only.getBlockIdOfChildNode());
            newRoot.setBlockId(ROOT_BLOCKID);
            levels--;                                 // tree got shorter
            DataHandler.updateIndexFileBlock(newRoot, levels);
        }
        return true;
    }

    /* ------------------------------------------------------------------
       Condense-Tree – Algorithm from Beckmann et al. (R*-tree paper)
       ------------------------------------------------------------------ */
    private void condenseTree(ArrayList<Node> nodePath,
                              ArrayList<Entry> entryPath) {

        ArrayList<Entry> orphaned = new ArrayList<>();   // re-insert later

        // walk back from leaf toward root
        for (int i = nodePath.size() - 1; i >= 0; i--) {
            Node n = nodePath.get(i);

            // root handled separately in delete()
            if (n.getBlockId() == ROOT_BLOCKID) {
                DataHandler.updateIndexFileBlock(n, levels);
                break;
            }

            if (n.getEntries().size() < Node.getMinEntriesInNode()) {
                // under-full: remove entry that points to n from its parent
                Node parent = nodePath.get(i - 1);
                Entry parentPtr = entryPath.get(i - 1);
                parent.removeEntry(parentPtr);
                DataHandler.updateIndexFileBlock(parent, levels);

                // collect all entries of n for reinsertion (if not leaf)
                orphaned.addAll(n.getEntries());
            } else {
                // just shrink MBR in parent entry
                if (i - 1 >= 0) {
                    Node parent = nodePath.get(i - 1);
                    Entry parentPtr = entryPath.get(i - 1);
                    parentPtr.setBoundingBoxToFitEntries(n.getEntries());
                    DataHandler.updateIndexFileBlock(parent, levels);
                }
                DataHandler.updateIndexFileBlock(n, levels);
            }
        }

        /* Re-insert orphaned entries (depth first)                        */
        for (Entry e : orphaned) {
            if (e instanceof LeafEntry le)
                insert(null, null, le, LEAF_LEVEL);
            else
                insert(null, null, e, (int) (nodeLevelFromEntry(e))); // helper
        }
    }

    /* ------------------------------------------------------------------ */
    private boolean recordIdInside(Entry e, long recordId) {
        // super-cheap check: compare lower≤id≤upper along pseudo-“id” axis
        BoundingBox b = e.getBoundingBox();
        Bounds idDim = b.getBounds().getFirst();
        return idDim.getLower() <= recordId && recordId <= idDim.getUpper();
    }
    private long nodeLevelFromEntry(Entry e) {
        // called only for internal entries during reinsertion
        Node n = DataHandler.readIndexFileBlock(e.getBlockIdOfChildNode());
        return n == null ? LEAF_LEVEL : n.getLevel();
    }
    

    
    // to use deletion
    /*
    boolean ok = tree.delete(42);
    System.out.println("deleted? " + ok);
     


     */
}
