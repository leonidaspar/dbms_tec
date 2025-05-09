import java.util.ArrayList;

//Used for query execution
abstract public class Query {
    abstract ArrayList<Long> getQueryRecordIDs(Node node);
}
