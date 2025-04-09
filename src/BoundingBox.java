import java.io.Serializable;
import java.util.ArrayList;

import static java.lang.Math.abs;

//Implementation of a box of N-dimensions
public class BoundingBox implements Serializable {
    private ArrayList<Bounds> bounds; //Bounds for each dimension
    private Double volume; //Total area that the box covers
    private Double perimeter; //Perimeter of the box
    private ArrayList<Double> center; //Coordinates for the center of the box

    public BoundingBox(ArrayList<Bounds> bounds, double volume, double perimeter, ArrayList<Double> center) {
        this.bounds = bounds;
        this.volume = volume;
        this.perimeter = perimeter;
        this.center = center;
    }

    public BoundingBox(ArrayList<Bounds> bounds) {
        this.volume = getVolume();
        this.perimeter = getPerimeter();
        this.center = getCenter();
        this.bounds = bounds;
    }

    public ArrayList<Bounds> getBounds() {
        return bounds;
    }

    public double getVolume() {
        if (volume==null) {
            double product = 1;
            for (int i = 0; i < DataHandler.getMetadata().getDataDimensions(); i++) {
                product *= abs(bounds.get(i).getUpper() - bounds.get(i).getLower());
            }
            volume = product;
        }
        return volume;
    }

    public double getPerimeter() {
        if (perimeter==null) {
            double sum = 0;
            for (int i = 0; i < DataHandler.getMetadata().getDataDimensions(); i++) {
                sum += abs(bounds.get(i).getUpper() - bounds.get(i).getLower());
            }
            perimeter = sum;
        }
        return perimeter;
    }

    public ArrayList<Double> getCenter() {
        if (center == null) {
            center = new ArrayList<>();
            for (int i = 0; i < DataHandler.getMetadata().getDataDimensions(); i++) {
                center.add((bounds.get(i).getLower() + bounds.get(i).getUpper()) / 2);
            }
        }
        return center;
    }

    public double findMinDistance(ArrayList<Double> point) {
        double minDistance = 0;
        double tempDist;
        for (int d=0; d<DataHandler.getMetadata().getDataDimensions(); d++) {
            if (getBounds().get(d).getLower() > point.get(d))
                tempDist = getBounds().get(d).getLower();
            else if (getBounds().get(d).getUpper() < point.get(d))
                tempDist = getBounds().get(d).getUpper();
            else
                tempDist = point.get(d);

            minDistance += Math.pow(point.get(d) - tempDist, 2);
        }
        return Math.sqrt(minDistance);
    }

    //True if point's radius overlaps with BoundingBox
    public boolean checkOverlapWithPoint(ArrayList<Double> point, double radius) {
        return findMinDistance(point) <= radius;
    }

    public static double findOverlap(BoundingBox box1, BoundingBox box2) {
        double overlap = 1;
        //For every dimension
        for (int i = 0; i<DataHandler.getMetadata().getDataDimensions(); i++) {
            double overlapInDimensionI;
            overlapInDimensionI = Math.min(box1.getBounds().get(i).getUpper(),box2.getBounds().get(i).getUpper())
                    - Math.max(box1.getBounds().get(i).getLower(),box2.getBounds().get(i).getLower());

            if (overlapInDimensionI<=0)
                return 0;
            else
                overlap *= overlapInDimensionI;
        }
        return overlap;
    }

    public double findDistanceBetweenCenters (BoundingBox box1, BoundingBox box2) {
        double distance = 0;
        for (int i = 0; i<DataHandler.getMetadata().getDataDimensions(); i++) {
            distance += Math.pow(box1.getCenter().get(i) - box2.getCenter().get(i), 2);
        }
        return Math.sqrt(distance);
    }
}
