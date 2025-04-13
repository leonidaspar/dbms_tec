import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class DataHandler {
    private static final String csvFile = "data.csv";
    private static final String dataFilename = "datafile.dat";
    private static final String indexFilename = "indexfile.dat";
    private static final int BLOCK_SIZE = 32*1024;
    private static Metadata metadata;
    private static IndexMetadata indexMetadata;

    public String getDataFilename() {
        return dataFilename;
    }
    public String getIndexFilename() {
        return indexFilename;
    }
    public static Metadata getMetadata() {
        return metadata;
    }
    public static IndexMetadata getIndexMetadata() {
        return indexMetadata;
    }

    public Metadata readBlock0() throws IOException {
        File file = new File(dataFilename);

        // Ensure the file exists and has sufficient data
        if (!file.exists() || file.length() < BLOCK_SIZE) {
            throw new IOException("File " + dataFilename + " does not exist or does not contain enough data.");
        }

        try (RandomAccessFile dataFile = new RandomAccessFile(file, "rw")) {
            dataFile.seek(0);
            byte[] block = new byte[BLOCK_SIZE];
            dataFile.readFully(block); // This is safe since we already checked the file length

            ByteBuffer buffer = ByteBuffer.wrap(block);
            long totalRecords = buffer.getLong();
            long totalBlocks = buffer.getLong();
            long dataDimensions = buffer.getLong();

            metadata = new Metadata(totalRecords, totalBlocks, dataDimensions);

        return metadata;
    } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

    }


    public void writeBlock0(Metadata metadata, String pathToFile) throws IOException {
        RandomAccessFile dataFile = new RandomAccessFile(new File(pathToFile), "rw");
        dataFile.seek(0);
        ByteBuffer buffer = ByteBuffer.allocate(BLOCK_SIZE);
        buffer.putLong(metadata.getTotalRecords());
        buffer.putLong(metadata.getTotalBlocks());
        buffer.putLong(metadata.getDataDimensions());

        byte[] paddedData = new byte[BLOCK_SIZE];
        System.arraycopy(buffer.array(), 0, paddedData, 0, buffer.position());

        dataFile.write(paddedData);
    }

    public long getTotalBlocksInDataFile() {
        File file = new File(dataFilename);
        if (!file.exists()) {
            return 0; // File doesn't exist, no blocks
        }

        long fileSize = file.length(); // Get file size in bytes
        return (long) Math.ceil((double) fileSize / BLOCK_SIZE); // Calculate total blocks
    }
    public void initializeDataFile(int dataDimensions) throws IOException {
        File file = new File(dataFilename);

        // If the file already exists, skip initialization
        if (file.exists()) {
            System.out.println("Data file already exists. Skipping re-initialization.");
            metadata = readBlock0(); // Read Block 0 (metadata)
            return;
        }

        // Delete the file and reinitialize it
        Files.deleteIfExists(file.toPath());
        RandomAccessFile dataFile = new RandomAccessFile(dataFilename, "rw");

        // Parse Records from the CSV file
        List<Record> records = parseCSV(dataDimensions);
        metadata = new Metadata(0,0,dataDimensions);
        int maxRecordsPerBlock = calculateMaxRecordsInBlock();
        int totalBlocks = (int) Math.ceil((double) records.size() / maxRecordsPerBlock);
        metadata = new Metadata(records.size(), totalBlocks, dataDimensions);

        // Write block 0 (metadata)
        writeBlock0(metadata, dataFilename);

        // Write data blocks
        int blockIndex = 1;
        ByteBuffer blockBuffer = ByteBuffer.allocate(BLOCK_SIZE);

        for (int i = 0; i < records.size(); i++) {
            Record record = records.get(i);
            writeRecordToBuffer(blockBuffer, record);

            // If the buffer is full or it's the last record, write the block to the file
            if ((i + 1) % maxRecordsPerBlock == 0 || i == records.size() - 1) {
                // Write the block
                byte[] block = new byte[BLOCK_SIZE];
                System.arraycopy(blockBuffer.array(), 0, block, 0, blockBuffer.position());
                dataFile.seek((long) blockIndex * BLOCK_SIZE);
                dataFile.write(block);

                blockBuffer.clear();
                blockIndex++;
            }
        }

        dataFile.close();
        System.out.println("Data file successfully initialized from CSV.");
    }


    public int calculateMaxRecordsInBlock() {
        if (metadata == null) {
            throw new IllegalStateException("Metadata is not initialized. Cannot calculate max records.");
        }

        // Size of one record in bytes:
        long dataDimensions = metadata.getDataDimensions(); // Number of dimensions in the data
        long idSize = Long.BYTES; // Size of 'id', which is a long (8 bytes)
        long coordinateSize = Double.BYTES * dataDimensions; // Each coordinate is a double (8 bytes)
        long recordSize = idSize + coordinateSize; // Total size of one record

        // Calculate the number of records that can fit in a block:
        return (int) (BLOCK_SIZE / recordSize);
    }

    private List<Record> parseCSV(int dataDimensions) throws IOException {
        List<Record> records = new ArrayList<>();
        BufferedReader br = new BufferedReader(new FileReader(csvFile));

        String line;
        while ((line = br.readLine()) != null) {
            String[] values = line.split(",");
            long id = Long.parseLong(values[0]);

            ArrayList<Double> coordinates = new ArrayList<>();
            for (int i = 1; i < values.length; i++) {
                coordinates.add(Double.parseDouble(values[i]));
            }

            if (coordinates.size() != dataDimensions) {
                throw new IllegalArgumentException("Record does not match specified data dimensions.");
            }

            records.add(new Record(id, coordinates));
        }

        br.close();
        return records;
    }

    private void writeRecordToBuffer(ByteBuffer buffer, Record record) {
        buffer.putLong(record.getId());
        for (double coordinate : record.getCoordinates()) {
            buffer.putDouble(coordinate);
        }
    }

    public void appendRecords(List<Record> records) throws IOException {
        // Open file channel in append mode
        try (FileChannel fileChannel = FileChannel.open(
                new File(dataFilename).toPath(),
                java.nio.file.StandardOpenOption.WRITE,
                java.nio.file.StandardOpenOption.APPEND)) {

            ByteBuffer buffer = ByteBuffer.allocate(BLOCK_SIZE);
            for (Record record : records) {
                writeRecordToBuffer(buffer, record); // Serialize and write each record to the buffer

                if (buffer.remaining() < 100) { // If buffer is nearly full (100 bytes margin, adjust as needed)
                    buffer.flip(); // Prepare buffer for writing
                    fileChannel.write(buffer); // Write the buffer to the file
                    buffer.clear(); // Clear buffer for next batch of records
                }
            }

            // Write remaining data in the buffer
            if (buffer.position() > 0) {
                buffer.flip(); // Prepare remaining data in buffer for writing
                fileChannel.write(buffer);
            }

            // Update metadata using getTotalBlocksInDataFile
            long totalBlocks = getTotalBlocksInDataFile();
            metadata = new Metadata(
                    metadata.getTotalRecords() + records.size(),
                    totalBlocks, // Use dynamically calculated blocks
                    metadata.getDataDimensions()
            );

            // Write updated metadata back to Block 0
            writeBlock0(metadata, dataFilename);
        }
    }
    public ArrayList<Record> readDataFileBlock(int blockId) throws IOException {
        // Validate blockId
        if (blockId < 0) {
            throw new IllegalArgumentException("Block ID cannot be negative.");
        }

        File file = new File(dataFilename);

        // Check if file exists
        if (!file.exists()) {
            throw new FileNotFoundException("Data file not found: " + dataFilename);
        }

        // Get the total number of blocks
        long totalBlocks = getTotalBlocksInDataFile();

        // Ensure the block ID is valid
        if (blockId >= totalBlocks) {
            throw new IllegalArgumentException("Block ID " + blockId + " is out of range. Total Blocks: " + totalBlocks);
        }

        // Calculate the starting byte offset of the block
        long offset = (long) blockId * BLOCK_SIZE;

        // Prepare the byte buffer to read the block
        ByteBuffer buffer = ByteBuffer.allocate(BLOCK_SIZE);

        // Open the file and read the specific block
        try (FileChannel fileChannel = FileChannel.open(file.toPath(), java.nio.file.StandardOpenOption.READ)) {
            fileChannel.position(offset); // Move to the block's starting position
            fileChannel.read(buffer);
        }

        // Prepare the buffer for reading (flip buffer)
        buffer.flip();

        // Deserialize the block into a list of records
        ArrayList<Record> records = new ArrayList<>();
        while (buffer.remaining() > 0) {
            Record record = readRecordFromBuffer(buffer);
            if (record != null) {
                records.add(record);
            }
        }

        return records;
    }
    private Record readRecordFromBuffer(ByteBuffer buffer) {
        try {
            // Read the record ID
            long id = buffer.getLong();

            // Read the coordinates
            ArrayList<Double> coordinates = new ArrayList<>();
            for (int i = 0; i < metadata.getDataDimensions(); i++) {
                if (buffer.remaining() >= Double.BYTES) {
                    coordinates.add(buffer.getDouble());
                } else {
                    // Not enough data for a valid record, stop processing
                    return null;
                }
            }

            // Return a new record
            return new Record(id, coordinates);
        } catch (Exception e) {
            // If an error occurs (e.g., incomplete record), return null
            return null;
        }
    }


    //Index file operations

    //WRITE READ MIGHT NEED WORK
    public void writeIndexBlock0(IndexMetadata indexMetadata) throws IOException {
        try (RandomAccessFile indexFile = new RandomAccessFile(indexFilename, "rw")) {
            indexFile.seek(0);
            ByteBuffer buffer = ByteBuffer.allocate(BLOCK_SIZE);
            buffer.putLong(indexMetadata.getTreeHeight());
            buffer.putLong(indexMetadata.getTotalIndexfileBlocks());

            byte[] paddedData = new byte[BLOCK_SIZE];
            System.arraycopy(buffer.array(), 0, paddedData, 0, buffer.position());

            indexFile.write(paddedData);
        }
    }

    public IndexMetadata readIndexBlock0() throws IOException {
        File file = new File(indexFilename);

        if (!file.exists() || file.length() < BLOCK_SIZE) {
            throw new IOException("Index file does not exist or is too small.");
        }

        try (RandomAccessFile indexFile = new RandomAccessFile(file, "r")) {
            indexFile.seek(0);
            byte[] block = new byte[BLOCK_SIZE];
            indexFile.readFully(block);

            ByteBuffer buffer = ByteBuffer.wrap(block);
            long treeHeight = buffer.getLong();
            long totalBlocks = buffer.getLong();

            indexMetadata = new IndexMetadata(treeHeight, totalBlocks);
            return indexMetadata;
        }
    }

    public long getTotalBlocksInIndexFile() {
        return indexMetadata.getTotalIndexfileBlocks();
    }

    public long getTotalLevelsOfTreeIndex() {
        return indexMetadata.getTreeHeight();
    }

    static int calculateMaxEntriesInNode() {
        ArrayList<Entry> entries = new ArrayList<>();
        int i;
        for (i = 0; i < Integer.MAX_VALUE; i++) {
            ArrayList<Bounds> boundsPerDimension = new ArrayList<>();
            for (int d = 0; d < metadata.getDataDimensions(); d++)
                boundsPerDimension.add(new Bounds(0.0,0.0));
            Entry entry = new LeafEntry (new Random().nextLong(),new Random().nextLong(), boundsPerDimension);
            entry.setBlockIdOfChildNode(new Random().nextLong());
            entries.add(entry);
            try {
                // Serialize Node to byte array
                ByteArrayOutputStream nodeOut = new ByteArrayOutputStream();
                ObjectOutputStream nodeOos = new ObjectOutputStream(nodeOut);
                nodeOos.writeObject(new Node(new Random().nextInt(), entries));
                nodeOos.flush();
                byte[] nodeBytes = nodeOut.toByteArray();

                // Serialize the length (like the original method does)
                ByteArrayOutputStream lenOut = new ByteArrayOutputStream();
                ObjectOutputStream lenOos = new ObjectOutputStream(lenOut);
                lenOos.writeInt(nodeBytes.length);
                lenOos.flush();
                byte[] lenBytes = lenOut.toByteArray();

                // Check total size against block size
                if (nodeBytes.length + lenBytes.length > BLOCK_SIZE) {
                    break;
                }
        } catch (IOException e) {
            e.printStackTrace();}
        }
        return i;
    }



}

