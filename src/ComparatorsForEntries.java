import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

//Comparators of entries using different methods
public class ComparatorsForEntries {

    //Compare based on their bounds
    public static class CompareOnBounds implements Comparator<Entry> {
        //HashMap used to store comparison values for the Entry being used
        //Double is the value of the bound whether it is upper or lower (resolved in CompareBounds constructor)
        private HashMap<Entry,Double> comparisonMap;

        public CompareOnBounds(List<Entry> entries, int dimension, boolean basedOnUpperBound) {
            this.comparisonMap = new HashMap<>();
            if (basedOnUpperBound)
                for (Entry entry : entries)
                    comparisonMap.put(entry, entry.getBoundingBox().getBounds().get(dimension).getUpper());
            else
                for (Entry entry : entries)
                    comparisonMap.put(entry, entry.getBoundingBox().getBounds().get(dimension).getLower());
        }
        @Override
        public int compare(Entry en1, Entry en2) {
            return Double.compare(comparisonMap.get(en1),comparisonMap.get(en2));
        }
    }

    //Compare based on volume enlargement if a new BoundingBox was added
    public static class CompareOnVolumeEnlargement implements Comparator<Entry> {
        //Array list has 2 values, one for volume, one for volume enlargement for entry
        private HashMap<Entry, ArrayList<Double>> comparisonMap;

        public CompareOnVolumeEnlargement(List<Entry> entries, BoundingBox newBox) {
            this.comparisonMap = new HashMap<>();
            for (Entry entry : entries) {
                BoundingBox tempBoundingBox = new BoundingBox(Bounds.findMinBounds(entry.getBoundingBox(),newBox));
                ArrayList<Double> tempList = new ArrayList<>();
                tempList.add(entry.getBoundingBox().getVolume()); //Volume of BB
                double enlargementDiff = tempBoundingBox.getVolume() - entry.getBoundingBox().getVolume();
                tempList.add(enlargementDiff); //Enlargement of entry
                comparisonMap.put(entry,tempList);
            }
        }

        @Override
        public int compare(Entry en1, Entry en2) {
            double enlargement1 = comparisonMap.get(en1).get(1);
            double enlargement2 = comparisonMap.get(en2).get(1);
            if (enlargement1 == enlargement2)
                return Double.compare(comparisonMap.get(en1).get(0),comparisonMap.get(en2).get(0));
            else
                return Double.compare(enlargement1,enlargement2);
        }
    }
    //Compare based on overlap increase if a new BoundingBox was added
    public static class CompareOnOverlapIncrease implements Comparator<Entry> {
        private BoundingBox newBox;
        private ArrayList<Entry> nodeEntries;
        private HashMap<Entry, Double> comparisonMap= new HashMap<>();

        public CompareOnOverlapIncrease(List<Entry> entriesC, BoundingBox addedBox, ArrayList<Entry> nodeEntries) {
            newBox = addedBox;
            this.nodeEntries = nodeEntries;
            for (Entry entry: entriesC) {
                double overlapEntry,overlapNewEntry,overlapIncreaseEntry;
                Entry tempEntry;
                overlapEntry = findEntryOverlap(entry,entry.getBoundingBox());
                tempEntry = new Entry(new BoundingBox(Bounds.findMinBounds(entry.getBoundingBox(),addedBox))); //Entry's box after new box is added
                overlapNewEntry = findEntryOverlap(entry,tempEntry.getBoundingBox());
                overlapIncreaseEntry = overlapNewEntry-overlapEntry;

                comparisonMap.put(entry,overlapIncreaseEntry);
            }
        }

        double findEntryOverlap(Entry entry, BoundingBox box) {
            double overlap = 0;
            for (Entry nodeEntry : nodeEntries)
                if (nodeEntry!=entry)
                    overlap+= BoundingBox.findOverlap(box,nodeEntry.getBoundingBox());
            return overlap;
        }
        @Override
        public int compare(Entry en1, Entry en2) {
            double overlapIncrease1 = comparisonMap.get(en1);
            double overlapIncrease2 = comparisonMap.get(en2);
            if (overlapIncrease1 == overlapIncrease2) {
                ArrayList<Entry> tempList = new ArrayList<>();
                tempList.add(en2);
                tempList.add(en1);
                return new CompareOnVolumeEnlargement(tempList,newBox).compare(en1,en2);
            }
            else
                return Double.compare(overlapIncrease1,overlapIncrease2);
        }
    }

    //Compare based on entry distance from its encapsulating BoundingBox's center
    public static class CompareOnDistanceFromPoint implements Comparator<Entry> {
        //Value (Double) is the distance of the BoundingBox from the center for the given Entry
        private HashMap<Entry, Double> comparisonMap = new HashMap<>();

        public CompareOnDistanceFromPoint(List<Entry> entries, ArrayList<Double> point) {
            for (Entry entry : entries)
                comparisonMap.put(entry,entry.getBoundingBox().findMinDistance(point));
        }

        public int compare(Entry en1, Entry en2) {
            return Double.compare(comparisonMap.get(en1),comparisonMap.get(en2));
        }
    }
    public static class CompareOnDistanceFromCenter implements Comparator<Entry> {
        private HashMap<Entry, Double> comparisonMap;
        public CompareOnDistanceFromCenter(List<Entry> entries, BoundingBox boundingBox) {
            comparisonMap = new HashMap<>();
            for (Entry entry : entries)
                comparisonMap.put(entry,BoundingBox.findDistanceBetweenBoundingBoxes(entry.getBoundingBox(),boundingBox));
        }
        public int compare(Entry en1, Entry en2) {
            return Double.compare(comparisonMap.get(en1),comparisonMap.get(en2));
        }
    }
}

