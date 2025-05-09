import java.util.ArrayList;
import java.util.Collections;
import java.util.PriorityQueue;
//Class used for k-NN query using a point and RStarTree
public class NearestNeighboursQuery extends Query {
    private ArrayList<Double> point; // coordinates of point
    private double pointRadius; // radius that will be used as bound
    private int howMany; // the number of neighbours to be found
    private PriorityQueue<HoldPairId> nNeighbours; // max heap for the nearest neighbours

    public NearestNeighboursQuery (ArrayList<Double> point, int howMany) {
        if (howMany < 0)
            throw new IllegalArgumentException("Can't find negative neighbours");
        this.point = point;
        this.howMany = howMany;
        this.pointRadius = Double.MAX_VALUE;
        this.nNeighbours = new PriorityQueue<>(howMany,(distancePairA,distancePairB) -> {
            return Double.compare(distancePairA.getDistance(), distancePairB.getDistance()); //Making max heap
        });
    }

    //returns ids of query's records
    @Override
    ArrayList<Long> getQueryRecordIDs (Node node) {
        ArrayList<Long> qualifiers = new ArrayList<>();
        getNeighbours(node);
        while (nNeighbours.size() > 0) {
            HoldPairId pair = nNeighbours.poll();
            qualifiers.add(pair.getRecordID());
        }
        Collections.reverse(qualifiers); //reverse because we want to return closest first
        return qualifiers;
    }
    //finds NN with RStarTree
    private void getNeighbours(Node node) {
        node.getEntries().sort(new ComparatorsForEntries.CompareOnDistanceFromPoint(node.getEntries(),point));
        int i=0;
        if (node.getLevel() != RStarTree.getLeafLevel()) {
            while (i < node.getEntries().size() && (nNeighbours.size() < howMany || node.getEntries().get(i).getBoundingBox().findMinDistance(point) <= pointRadius)) {
                getNeighbours(DataHandler.readIndexFileBlock(node.getEntries().get(i).getBlockIdOfChildNode()));
                i++;
            }
        } else {
            while (i < node.getEntries().size() && (nNeighbours.size() < howMany || node.getEntries().get(i).getBoundingBox().findMinDistance(point) <= pointRadius)) {
                if (nNeighbours.size() >= howMany)
                    nNeighbours.poll();
                LeafEntry leafEntry = (LeafEntry) node.getEntries().get(i);
                double minDistance = leafEntry.getBoundingBox().findMinDistance(point);
                nNeighbours.add(new HoldPairId(leafEntry.getRecordId(),minDistance));
                pointRadius = nNeighbours.peek().getDistance();
                i++;
            }
        }
    }
}
