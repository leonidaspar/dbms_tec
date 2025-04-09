public class IndexMetadata {
    private long treeHeight;
    private long totalIndexfileBlocks;

    public IndexMetadata(long treeHeight, long totalIndexfileBlocks) {
        this.treeHeight = treeHeight;
        this.totalIndexfileBlocks = totalIndexfileBlocks;
    }
    public long getTreeHeight() {
        return treeHeight;
    }
    public long getTotalIndexfileBlocks() {
        return totalIndexfileBlocks;
    }

    public void setTreeHeight(long treeHeight) {
        this.treeHeight = treeHeight;
    }

    public void setTotalIndexfileBlocks(long totalIndexfileBlocks) {
        this.totalIndexfileBlocks = totalIndexfileBlocks;
    }
}
