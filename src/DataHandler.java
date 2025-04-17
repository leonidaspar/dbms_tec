
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class DataHandler {
    private static final String csvFile = "data.csv";
    private static final String dataFilename = "datafile.dat";
    private static final String indexFilename = "indexfile.dat";
    private static final int BLOCK_SIZE = 32*1024;
    private static int dataDimensions;
    private static int blocksInDataFile;
    private static int blocksInIndexFile;
    private static int levelsOfTreeIndex;
   // private static Metadata metadata;


    public String getDataFilename() {
        return dataFilename;
    }

    public String getIndexFilename() {
        return indexFilename;
    }
    public static int getDataDimensions() {
        return dataDimensions;
    }


    // Used to serializable a serializable Object to byte array
    private static byte[] serialize(Object obj) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ObjectOutputStream os = new ObjectOutputStream(out);
        os.writeObject(obj);
        return out.toByteArray();
    }

    // Used to deserializable a byte array to a serializable Object
    private static Object deserialize(byte[] data) throws IOException, ClassNotFoundException {
        ByteArrayInputStream in = new ByteArrayInputStream(data);
        ObjectInputStream is = new ObjectInputStream(in);
        return is.readObject();
    }

    public ArrayList<Integer> readBlock0(String filePath) throws IOException {
        try {
            RandomAccessFile raf = new RandomAccessFile(new File(filePath), "rw");
            FileInputStream fis = new FileInputStream(raf.getFD());
            BufferedInputStream bis = new BufferedInputStream(fis);
            byte[] block = new byte[BLOCK_SIZE];
            if (bis.read(block,0,BLOCK_SIZE) != BLOCK_SIZE)
                throw new IllegalStateException("Block size read was not of " + BLOCK_SIZE + " bytes");

            byte[] goodPutLengthInBytes = serialize(new Random().nextInt()); // Serializing an integer ir order to get the size of goodPutLength in bytes
            System.arraycopy(block, 0, goodPutLengthInBytes, 0, goodPutLengthInBytes.length);

            byte[] dataInBlock = new byte[(Integer)deserialize(goodPutLengthInBytes)];
            System.arraycopy(block, goodPutLengthInBytes.length, dataInBlock, 0, dataInBlock.length);

            return (ArrayList<Integer>)deserialize(dataInBlock);

        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;

    }


    public static void updateBlock0(String pathToFile) throws IOException {
        try {
            ArrayList<Integer> dataFileMetaData = new ArrayList<>();
            dataFileMetaData.add(dataDimensions);
            dataFileMetaData.add(BLOCK_SIZE);
            if (pathToFile.equals(dataFilename))
                dataFileMetaData.add(++blocksInDataFile);
            else if (pathToFile.equals(indexFilename))
            {
                dataFileMetaData.add(++blocksInIndexFile);
                dataFileMetaData.add(levelsOfTreeIndex);
            }
            byte[] metaDataInBytes = serialize(dataFileMetaData);
            byte[] goodPutLengthInBytes = serialize(metaDataInBytes.length);
            byte[] block = new byte[BLOCK_SIZE];
            System.arraycopy(goodPutLengthInBytes, 0, block, 0, goodPutLengthInBytes.length);
            System.arraycopy(metaDataInBytes, 0, block, goodPutLengthInBytes.length, metaDataInBytes.length);

            RandomAccessFile f = new RandomAccessFile(new File(pathToFile), "rw");
            f.write(block);
            f.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static long getTotalBlocksInDataFile() {
        return blocksInDataFile;
    }

    public void initializeDataFile(int dataDimensions) throws IOException {
        try{
            // Checks if a datafile already exists, initialise the metaData from the metadata block (block 0 of the file)
            // If already exists, initialise the variables with the values of the dimensions, block size and total blocks of the data file
            if (Files.exists(Paths.get(dataFilename)))
            {
                ArrayList<Integer> dataFileMetaData = readBlock0(dataFilename);
                if (dataFileMetaData == null)
                    throw new IllegalStateException("Could not read datafile's Meta Data Block properly");
                DataHandler.dataDimensions = dataFileMetaData.get(0);
                if (DataHandler.dataDimensions  <= 0)
                    throw new IllegalStateException("The number of data dimensions must be a positive integer");
                if (dataFileMetaData.get(1) != BLOCK_SIZE)
                    throw new IllegalStateException("Block size read was not of " + BLOCK_SIZE + " bytes");
                blocksInDataFile = dataFileMetaData.get(2);
                if (blocksInDataFile  < 0)
                    throw new IllegalStateException("The total blocks of the datafile cannot be a negative number");
            }
            // Else initialize a new datafile
            else
            {
                Files.deleteIfExists(Paths.get(dataFilename)); // Resetting/Deleting dataFile data
                DataHandler.dataDimensions = dataDimensions;
                if (DataHandler.dataDimensions  <= 0)
                    throw new IllegalStateException("The number of data dimensions must be a positive integer");
                updateBlock0(dataFilename);
                ArrayList<Record> blockRecords = new ArrayList<>();
                BufferedReader csvReader = (new BufferedReader(new FileReader(csvFile))); // BufferedReader used to read the data from the csv file
                String stringRecord; // String used to read each line (row) of the csv file
                int maxRecordsInBlock = calculateMaxRecordsInBlock();
                while ((stringRecord = csvReader.readLine()) != null)
                {
                    if (blockRecords.size() == maxRecordsInBlock)
                    {
                        writeBlock0(blockRecords);
                        blockRecords =  new ArrayList<>();
                    }
                    blockRecords.add(new Record(stringRecord));
                }
                csvReader.close();
                if (blockRecords.size() > 0)
                    writeBlock0(blockRecords);
            }
        }catch(Exception e){e.printStackTrace();}
    }


    public int calculateMaxRecordsInBlock() {
        ArrayList<Record> blockRecords = new ArrayList<>();
        int i;
        for (i = 0; i < Integer.MAX_VALUE; i++) {
            ArrayList<Double> coordinateForEachDimension = new ArrayList<>();
            for (int d = 0; d < DataHandler.dataDimensions; d++)
                coordinateForEachDimension.add(0.0);
            Record record = new Record(0, coordinateForEachDimension);
            blockRecords.add(record);
            byte[] recordInBytes = new byte[0];
            byte[] goodPutLengthInBytes = new byte[0];
            try {
                recordInBytes = serialize(blockRecords);
                goodPutLengthInBytes = serialize(recordInBytes.length);
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (goodPutLengthInBytes.length + recordInBytes.length > BLOCK_SIZE)
                break;
        }
        return i;
    }

    public void writeBlock0(List<Record> records) {
        try {
            byte[] recordInBytes = serialize(records);
            byte[] goodPutLengthInBytes = serialize(recordInBytes.length);
            byte[] block = new byte[BLOCK_SIZE];
            System.arraycopy(goodPutLengthInBytes, 0, block, 0, goodPutLengthInBytes.length);
            System.arraycopy(recordInBytes, 0, block, goodPutLengthInBytes.length, recordInBytes.length);

            FileOutputStream fos = new FileOutputStream(dataFilename,true);
            BufferedOutputStream bout = new BufferedOutputStream(fos);
            bout.write(block);
            updateBlock0(dataFilename);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public static ArrayList<Record> readDataFileBlock(int blockId) {
        try {
            RandomAccessFile raf = new RandomAccessFile(new File(dataFilename), "rw");
            FileInputStream fis = new FileInputStream(raf.getFD());
            BufferedInputStream bis = new BufferedInputStream(fis);
            //seek to a a different section of the file, so discard the previous buffer
            raf.seek(blockId*BLOCK_SIZE);
            //bis = new BufferedInputStream(fis);
            byte[] block = new byte[BLOCK_SIZE];
            if (bis.read(block,0,BLOCK_SIZE) != BLOCK_SIZE)
                throw new IllegalStateException("Block size read was not of " + BLOCK_SIZE + " bytes");
            byte[] goodPutLengthInBytes = serialize(new Random().nextInt()); // Serializing an integer ir order to get the size of goodPutLength in bytes
            System.arraycopy(block, 0, goodPutLengthInBytes, 0, goodPutLengthInBytes.length);

            byte[] recordsInBlock = new byte[(Integer)deserialize(goodPutLengthInBytes)];
            System.arraycopy(block, goodPutLengthInBytes.length, recordsInBlock, 0, recordsInBlock.length);

            return (ArrayList<Record>)deserialize(recordsInBlock);

        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    //Index file operations

    public static long getTotalBlocksInIndexFile() {
        return blocksInIndexFile;
    }

    public static long getTotalLevelsOfTreeIndex() {
        return levelsOfTreeIndex;
    }

    // Calculates the maximum number of entries that can fit in a node within a block,
    // based on the block size and serialized node structure
    public static int calculateMaxEntriesInNode() {
        ArrayList<Entry> entries = new ArrayList<>();
        int i;
        for (i = 0; i < Integer.MAX_VALUE; i++) {
            ArrayList<Bounds> boundsPerDimension = new ArrayList<>();
            for (int d = 0; d < DataHandler.getDataDimensions(); d++)
                boundsPerDimension.add(new Bounds(0.0,0.0));
            Entry entry = new LeafEntry (new Random().nextLong(),new Random().nextLong(), boundsPerDimension);
            entry.setBlockIdOfChildNode(new Random().nextLong());
            entries.add(entry);
            try {
                // Serialize a node with the current entries list
                ByteArrayOutputStream nodeOut = new ByteArrayOutputStream();
                ObjectOutputStream nodeOos = new ObjectOutputStream(nodeOut);
                nodeOos.writeObject(new Node(new Random().nextInt(), entries));
                nodeOos.flush();
                byte[] nodeBytes = nodeOut.toByteArray();

                // Serialize the length of the node
                ByteArrayOutputStream lenOut = new ByteArrayOutputStream();
                ObjectOutputStream lenOos = new ObjectOutputStream(lenOut);
                lenOos.writeInt(nodeBytes.length);
                lenOos.flush();
                byte[] lenBytes = lenOut.toByteArray();

                // Check if combined size exceeds block size
                if (nodeBytes.length + lenBytes.length > BLOCK_SIZE) {
                    break;
                }
        } catch (IOException e) {
            e.printStackTrace();}
        }
        return i;
    }

    // Updates the metadata in block 0 of the index file when a new level is added to the tree index
    public static void updateLevelsOfTreeIndexFile() {
        try {
            ArrayList<Integer> metadata = new ArrayList<>();
            metadata.add(dataDimensions);
            metadata.add(BLOCK_SIZE);
            metadata.add(blocksInIndexFile);
            metadata.add(++levelsOfTreeIndex);
            byte[] metadataInBytes = serialize(metadata);
            byte[] goodPutLengthInBytes = serialize(metadataInBytes.length);
            byte[] block = new byte[BLOCK_SIZE];
            System.arraycopy(goodPutLengthInBytes, 0, block, 0, goodPutLengthInBytes.length);
            System.arraycopy(metadataInBytes, 0, block, goodPutLengthInBytes.length, metadataInBytes.length);
            RandomAccessFile raf = new RandomAccessFile(new File(indexFilename),"rw");
            raf.write(block);
            raf.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    // Initializes the index file and reads/writes metadata from/to block 0
    public void initializeIndexFile(int dataDimensions) {
        try {
            if (Files.exists(Paths.get(indexFilename))) {
                ArrayList<Integer> metadata = readBlock0(indexFilename);
                if (metadata==null)
                    throw new IllegalStateException("Could not read block 0 from file " + indexFilename);
                DataHandler.dataDimensions = metadata.getFirst();
                if (DataHandler.dataDimensions <= 0)
                    throw new IllegalStateException("Data dimensions must be greater than 0");
                if (metadata.get(1) > BLOCK_SIZE)
                    throw new IllegalStateException("Block size was not of " + BLOCK_SIZE + " bytes");
                blocksInIndexFile = metadata.get(2);
                if (blocksInIndexFile < 0)
                    throw new IllegalStateException("Blocks of index file must be greater than 0");
                levelsOfTreeIndex = metadata.get(3);
                if (levelsOfTreeIndex < 0)
                    throw new IllegalStateException("Levels of tree index must be greater than 0");
            } else {
                // If index file does not exist, initialize it with default values
                DataHandler.dataDimensions = dataDimensions;
                levelsOfTreeIndex = 1;
                if (DataHandler.dataDimensions <= 0)
                    throw new IllegalStateException("Data dimensions must be greater than 0");
                updateBlock0(indexFilename);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Writes a Node to the index file as a block, and updates metadata if the tree level has changed
    public static void updateIndexFileBlock(Node node, int levelsOfTreeIndex) {
        try {
            byte[] block = new byte[BLOCK_SIZE];
            byte[] nodeInBytes = serialize(node);
            byte[] goodPutLengthInBytes = serialize(nodeInBytes.length);

            System.arraycopy(goodPutLengthInBytes, 0, block, 0, goodPutLengthInBytes.length);
            System.arraycopy(nodeInBytes, 0, block, goodPutLengthInBytes.length, nodeInBytes.length);
            RandomAccessFile raf = new RandomAccessFile(new File(indexFilename),"rw");
            raf.write(block);
            raf.close();

            // If this node is the root and the tree level has changed, update the metadata
            if (node.getBlockId() == RStarTree.getRootBlockId() && DataHandler.levelsOfTreeIndex != levelsOfTreeIndex)
                updateLevelsOfTreeIndexFile();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void writeIndexFileBlock(Node node) {
        try {
            byte[] block = new byte[BLOCK_SIZE];
            byte[] nodeInBytes = serialize(node);
            byte[] goodPutLengthInBytes = serialize(nodeInBytes.length);
            System.arraycopy(goodPutLengthInBytes, 0, block, 0, goodPutLengthInBytes.length);
            System.arraycopy(nodeInBytes, 0, block, goodPutLengthInBytes.length, nodeInBytes.length);

            FileOutputStream fos = new FileOutputStream(indexFilename,true);
            BufferedOutputStream bos = new BufferedOutputStream(fos);
            bos.write(block);
            updateBlock0(indexFilename);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Reads a Node from the specified block in the index file
    public static Node readIndexFileBlock(long blockId) {
        try {
            RandomAccessFile raf = new RandomAccessFile(new File(indexFilename),"rw");
            FileInputStream fis = new FileInputStream(raf.getFD());
            BufferedInputStream bis = new BufferedInputStream(fis);

            // Seek to the correct block position
            raf.seek(blockId * BLOCK_SIZE);
            byte[] block = new byte[BLOCK_SIZE];
            if (bis.read(block,0,BLOCK_SIZE)!= BLOCK_SIZE)
                throw new IllegalStateException("Blocks size was not of " + BLOCK_SIZE + " bytes");

            // Estimate the size of the payload (used for deserialization)
            byte[] goodPutLengthInBytes = serialize(new Random().nextInt());
            System.arraycopy(block,0,goodPutLengthInBytes,0,goodPutLengthInBytes.length);

            // Extract and deserialize the node
            byte[] nodeInBytes = new byte[(Integer) deserialize(goodPutLengthInBytes)];
            System.arraycopy(block,goodPutLengthInBytes.length,nodeInBytes,0,nodeInBytes.length);

            return (Node) deserialize(nodeInBytes);

        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }



}

