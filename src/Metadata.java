public class Metadata {
    private long totalRecords;
    private long totalBlocks;
    private static long dataDimensions;

    public Metadata(long totalRecords, long totalBlocks, long dataDimensions) {
        this.totalRecords = totalRecords;
        this.totalBlocks = totalBlocks;
        this.dataDimensions = dataDimensions;
    }
    public long getTotalRecords() {
        return totalRecords;
    }
    public long getTotalBlocks() {
        return totalBlocks;
    }
    public static long getDataDimensions() {
        return dataDimensions;
    }
    @Override
    public String toString() {
        return "Metadata{" +
                "totalRecords=" + totalRecords +
                ", totalBlocks=" + totalBlocks +
                ", dataDimensions=" + dataDimensions +
                '}';
    }
}
