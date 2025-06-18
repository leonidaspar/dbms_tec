import java.util.ArrayList;

// Class use for queries execution without any use of an index
abstract public class LinearQuery {
    // Returns the ids of the query's records
    abstract ArrayList<Long> getQueryRecordIds();
}
