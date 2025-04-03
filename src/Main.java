import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        DataHandler dataHandler = new DataHandler();
        try {
            int dataDimensions = 2; // Change based on your dataset
            dataHandler.initializeDataFile(dataDimensions);

            System.out.println("Process completed.");
        } catch (IOException e) {
            e.printStackTrace();
        }

       /* // Example usage: Creating a list of records.
        List<Record> records = new ArrayList<>();
        ArrayList<Double> testList = new ArrayList();
        testList.add(32.3);
        testList.add(43.1);
        records.add(new Record(dataHandler.getMetadata().getTotalRecords()+1,testList));
        try {
            // Append the records to the datafile.dat file.
            dataHandler.appendRecords(records);
            System.out.println("Records appended successfully!");
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println(dataHandler.getMetadata()); */
    }

}
