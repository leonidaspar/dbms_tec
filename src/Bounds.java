import java.io.Serializable;
import java.util.ArrayList;

// Bounds of an interval in a single dimension
public class Bounds implements Serializable {
    private double upper;
    private double lower;

    public Bounds(double upper, double lower) {
        if (lower <= upper) {
            this.upper = upper;
            this.lower = lower;
        }
        else
            throw new IllegalArgumentException("The lower value of the bounds cannot be greater than the upper value.");
    }
    public double getUpper() {
        return upper;
    }
    public double getLower() {
        return lower;
    }

    public ArrayList<Bounds> findMinBounds (BoundingBox box1, BoundingBox box2) {
        ArrayList<Bounds> minBounds = new ArrayList<>();
        for (int i = 0; i < DataHandler.getMetadata().getDataDimensions(); i++) {
            double lowerMin;
            double upperMax;
            lowerMin = Math.min(box1.getBounds().get(i).getLower(), box2.getBounds().get(i).getLower());
            upperMax = Math.max(box1.getBounds().get(i).getUpper(), box2.getBounds().get(i).getUpper());
            minBounds.add(new Bounds(lowerMin, upperMax));
        }
        return minBounds;
    }
}
