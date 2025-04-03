import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public class DataHandler {
    private static final String csvFile = "data.csv";
    private static final String dataFilename = "datafile.dat";
    private static final String indexFilename = "indexfile.dat";
    private static final int BLOCK_SIZE = 32*1024;
    private Metadata metadata;

    public String getDataFilename() {
        return dataFilename;
    }

    public String getIndexFilename() {
        return indexFilename;
    }
    public Metadata getMetadata() {
        return metadata;
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
        if (metadata == null) {
            return 0;
        }
        return metadata.getTotalBlocks();
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


    public int calculateMaxRecordsInBlock() { //Maybe wrong
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
        if (records == null || records.isEmpty()) {
            throw new IllegalArgumentException("No records to append");
        }

        try (RandomAccessFile dataFile = new RandomAccessFile(dataFilename, "rw");
             FileChannel fileChannel = dataFile.getChannel()) {

            // Move to the end of the current file to append
            long dataFileSize = fileChannel.size();
            fileChannel.position(dataFileSize);

            // Create a buffer to store records
            ByteBuffer buffer = ByteBuffer.allocate(BLOCK_SIZE);

            int maxRecordsInBlock = calculateMaxRecordsInBlock();
            int recordCountInBlock = 0;

            for (Record record : records) {
                // Write record to buffer
                writeRecordToBuffer(buffer, record);
                recordCountInBlock++;

                // If buffer is full or max records in block is reached, flush it to file
                if (buffer.position() >= BLOCK_SIZE || recordCountInBlock >= maxRecordsInBlock) {
                    buffer.flip();
                    fileChannel.write(buffer);
                    buffer.clear();
                    recordCountInBlock = 0;
                }
            }

            // Flush remaining data in buffer
            if (buffer.position() > 0) {
                buffer.flip();
                fileChannel.write(buffer);
            }
        }

        // Update metadata
        long newTotalRecords = metadata.getTotalRecords() + records.size(); // Add new records to total
        long newTotalBlocks = getTotalBlocksInDataFile(); // Recalculate total blocks
        metadata = new Metadata(newTotalRecords, newTotalBlocks, metadata.getDataDimensions()); // Update metadata object

        // Save the updated metadata to Block 0
        writeBlock0(metadata, dataFilename);
    }


}

