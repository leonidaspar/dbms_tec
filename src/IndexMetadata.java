/* this file shows the global info about the whole tree, it will probaly be stored in block 0
    Root node block ID,
    Tree height: to handle queries,
    max and min Node capacity,
    Dimension count 
*/
// neo - added more info to the class
/* ta nea:
-       rootBlockId:  where the root node is on disk 
        maxEntries:   maximum entries allowed in any node (for insertion/splitting)
   -    minEntries:   minimum entries  for nodes 
        dimension:    how many dimensions each point has 
 
  to ekana gia na leitourgei to indexmanager
 
*/
public class IndexMetadata {
    private long rootBlockId;
    private int treeHeight;
    private int maxEntries;
    private int minEntries;
    private int dimension;

    private long totalIndexfileBlocks;  // from the old one

    // neo - new constructor to be used by IndexManager
    public IndexMetadata(long rootBlockId, int treeHeight, int maxEntries, int minEntries, int dimension) {
        this.rootBlockId = rootBlockId;
        this.treeHeight = treeHeight;
        this.maxEntries = maxEntries;
        this.minEntries = minEntries;
        this.dimension = dimension;
    }

    // the old constructor as it was
    public IndexMetadata(long treeHeight, long totalIndexfileBlocks) {
        this.treeHeight = (int) treeHeight;
        this.totalIndexfileBlocks = totalIndexfileBlocks;

        // without these values, it gets initialized to 0
        this.rootBlockId = -1;
        this.maxEntries = 40; 
        this.minEntries = 16; // default ola
        this.dimension = 2;   
    }

    
    public long getRootBlockId() {
        return rootBlockId;
    }

    public int getTreeHeight() {
        return treeHeight;
    }

    public int getMaxEntries() {
        return maxEntries;
    }

    public int getMinEntries() {
        return minEntries;
    }

    public int getDimension() {
        return dimension;
    }

    public long getTotalIndexfileBlocks() {
        return totalIndexfileBlocks;
    }

    
    public void setRootBlockId(long rootBlockId) {
        this.rootBlockId = rootBlockId;
    }

    public void setTreeHeight(int treeHeight) {
        this.treeHeight = treeHeight;
    }

    public void setMaxEntries(int maxEntries) {
        this.maxEntries = maxEntries;
    }

    public void setMinEntries(int minEntries) {
        this.minEntries = minEntries;
    }

    public void setDimension(int dimension) {
        this.dimension = dimension;
    }

    public void setTotalIndexfileBlocks(long totalIndexfileBlocks) {
        this.totalIndexfileBlocks = totalIndexfileBlocks;
    }

    @Override // smame as before
    public String toString() {
        return "IndexMetadata{" +
                "rootBlockId=" + rootBlockId +
                ", treeHeight=" + treeHeight +
                ", maxEntries=" + maxEntries +
                ", minEntries=" + minEntries +
                ", dimension=" + dimension +
                ", totalIndexfileBlocks=" + totalIndexfileBlocks +
                '}';
    }
}
