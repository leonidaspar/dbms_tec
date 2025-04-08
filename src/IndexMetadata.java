 /* this file shows the global info about the whole tree, it will probaly be stored in block 0
    Root node block ID,
    Tree height: to handle queries,
    max and min Node capacity,
    Dimension count 
*/

public class IndexMetadata{
    public long rootBlockId;
    public int treeHeight;
    public int maxCapacity;
    public int minCapacity;
    public int dimension;

    public IndexMetadata(long rootBlockId, int treeHeight, int maxCapacity, int minCapacity, int dimension) {
        this.rootBlockId = rootBlockId;
        this.treeHeight = treeHeight;
        this.maxCapacity = maxCapacity;
        this.minCapacity = minCapacity;
        this.dimension = dimension;
    }

    public long getRootBlockId() { return rootBlockId; }
    public int getTreeHeight() { return treeHeight; }
    public int getMaxCapacity() { return maxCapacity; }
    public int getMinCapacity() { return minCapacity; }
    public int getDimension() { return dimension; }


    @Override // representation of the node
    public String toString() {
        return "IndexMetadata{" +
                "rootBlockId=" + rootBlockId +
                ", treeHeight=" + treeHeight +
                ", maxCapacity=" + maxCapacity +
                ", minCapacity=" + minCapacity +
                ", Dimension=" + dimension +
                '}';
    }
}