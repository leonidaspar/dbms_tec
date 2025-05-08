import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Main {

    public static void main(String[] args) {

        DataHandler dataHandler = new DataHandler();

        try {
            int dataDimensions = 2;                    // adjust if needed
            dataHandler.initializeDataFile(dataDimensions);
            System.out.println("Process completed.");
        } catch (IOException e) {
            e.printStackTrace();
        }

        /* -------------- original append-records example (kept, but inactive) -------------
        List<Record> records = new ArrayList<>();
        ArrayList<Double> testList = new ArrayList<>();
        testList.add(32.3);
        testList.add(43.1);
        records.add(new Record(
                dataHandler.getMetadata().getTotalRecords() + 1,
                testList));

        try {
            dataHandler.appendRecords(records);
            System.out.println("Records appended successfully!");
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println(dataHandler.getMetadata());
        ------------------------------------------------------------------------------- */

        // ------------ R*-tree insertion and deletion -test --------------------------
        try {
            RStarTree tree = new RStarTree(false);          // empty root

            tree.addRecord(new Record(1, new ArrayList<>(List.of(1.0, 2.0))), 1);
            tree.addRecord(new Record(2, new ArrayList<>(List.of(3.0, 4.0))), 1);

            System.out.println("delete id 1  → " + tree.delete(1));   // true expected
            System.out.println("delete id 99 → " + tree.delete(99));  // false expected
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}

