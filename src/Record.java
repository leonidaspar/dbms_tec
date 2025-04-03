import java.util.ArrayList;

public class Record {
    private long id;
    private ArrayList<Double> coordinates;

    public Record(long id, ArrayList<Double> coordinates) {
        this.id = id;
        this.coordinates = coordinates;
    }

    public long getId() {
        return id;
    }
    public ArrayList<Double> getCoordinates() {
        return coordinates;
    }
    @Override
    public String toString() {
        StringBuilder recordToString = new StringBuilder(id + "," + coordinates.get(0));
        for(int i = 1; i < coordinates.size(); i++)
            recordToString.append(",").append(coordinates.get(i));
        return String.valueOf(recordToString);
    }
}
