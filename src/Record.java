
import java.util.ArrayList;

public class Record {
    private long id;
    private ArrayList<Double> coordinates;

    public Record(long id, ArrayList<Double> coordinates) {
        this.id = id;
        this.coordinates = coordinates;
    }

    Record(String recordInString)
    {
        String[] stringArray;
        stringArray = recordInString.split(","); // given string will be split by the argument delimiter provided

        if (stringArray.length != DataHandler.getDataDimensions() + 1)
            throw new IllegalArgumentException("In order to convert a String to a Record, a Long and a total amount of coordinates for each dimension must be given");

        id = Long.parseLong(stringArray[0]);
        coordinates = new ArrayList<>();
        for (int i = 1; i < stringArray.length ; i++)
            coordinates.add(Double.parseDouble(stringArray[i]));
    }


    public long getId() {
        return id;
    }
    public double getCoordinateInDimension(int dimension) {
        return coordinates.get(dimension);
    }
    @Override
    public String toString() {
        StringBuilder recordToString = new StringBuilder(id + "," + coordinates.get(0));
        for(int i = 1; i < coordinates.size(); i++)
            recordToString.append(",").append(coordinates.get(i));
        return String.valueOf(recordToString);
    }
}
