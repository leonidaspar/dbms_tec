import java.util.ArrayList;

public class LinearRadiusRangeQuery extends LinearQuery {
//Class used for executing a range query withing a specific circle without the use of an index
// Searches for records within that circle

    private ArrayList<Long> qualifyingRecordIds; // Record ids used for queries
    private ArrayList<Double> searchPoint; // coordinates of point used for radius queries
    private double searchPointRadius; // The radius of the point which will be used for the query

    LinearRadiusRangeQuery(ArrayList<Double> searchPoint, Double searchPointRadius) {
        this.searchPoint = searchPoint;
        this.searchPointRadius = searchPointRadius;
    }

    // Returns the ids of the query's records
    @Override
    ArrayList<Long> getQueryRecordIds() {
        qualifyingRecordIds = new ArrayList<>();
        search();
        return qualifyingRecordIds;
    }

    // Search for Records within searchPoint's radius
    private void search(){
        int blockId = 1;
        while(blockId < DataHandler.getTotalBlocksInDataFile()){
            ArrayList<Record> recordsInBlock;
            recordsInBlock = DataHandler.readDataFileBlock(blockId);
            ArrayList<LeafEntry> entries = new ArrayList<>();
            if(recordsInBlock != null){
                for (Record record : recordsInBlock) {
                    ArrayList<Bounds> boundsForEachDimension = new ArrayList<>();
                    // Since we have to do with points as records we set low and upper to be same
                    for (int d = 0; d < DataHandler.getDataDimensions(); d++)
                        boundsForEachDimension.add(new Bounds(record.getCoordinateInDimension(d), record.getCoordinateInDimension(d)));

                    entries.add(new LeafEntry(record.getId(), blockId, boundsForEachDimension));

                }

                for(LeafEntry entry : entries)
                {
                    if(entry.getBoundingBox().checkOverlapWithPoint(searchPoint, searchPointRadius))
                        qualifyingRecordIds.add(entry.getRecordId());
                }
            }else
                throw new IllegalStateException("Could not read records properly from the datafile");
            blockId++;


        }
    }
}

