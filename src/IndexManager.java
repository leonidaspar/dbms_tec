/*
  Handles reading and writing to indexfile.dat.
 * 
  Block 0: IndexMetadata (root block, tree height, max/min entries, dimensions)
  Block N: RStarNode (a single node with bounding boxes + child pointers)
  
  what it does:
  - Saves tree nodes to disk and loads them back when needed
 
   will be used by:
  - insert() to save new nodes
  - query() to load subtrees
  - init/startup to read metadata
 */


import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;

public class IndexManager {
    private static final int BLOCK_SIZE = 32 * 1024; // 32 KB fixed-size blocks
    private final String indexFilename;

    public IndexManager(String indexFilename) {
        this.indexFilename = indexFilename;
    }

    /* Write IndexMetadata TO BLOCK 0 */
    public void writeIndexMetadata(IndexMetadata meta) throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate(BLOCK_SIZE);
        buffer.putLong(meta.getRootBlockId());
        buffer.putInt(meta.getTreeHeight());
        buffer.putInt(meta.getMaxEntries());
        buffer.putInt(meta.getMinEntries());
        buffer.putInt(meta.getDimension());
        buffer.flip();

        try (RandomAccessFile raf = new RandomAccessFile(indexFilename, "rw");
             FileChannel channel = raf.getChannel()) {
            channel.position(0);
            channel.write(buffer);
        }
    }

    /* Read indexMetadata from BLOCK 0 */
    public IndexMetadata readIndexMetadata() throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate(BLOCK_SIZE);
        try (RandomAccessFile raf = new RandomAccessFile(indexFilename, "r");
             FileChannel channel = raf.getChannel()) {
            channel.position(0);
            channel.read(buffer);
        }
        buffer.flip();

        long rootBlockId = buffer.getLong();
        int treeHeight = buffer.getInt();
        int maxEntries = buffer.getInt();
        int minEntries = buffer.getInt();
        int dimension = buffer.getInt();

        return new IndexMetadata(rootBlockId, treeHeight, maxEntries, minEntries, dimension);
    }

    // /* Write RStarNode TO BLOCK N */
    public void writeNode(RStarNode node, int blockId, int dimension) throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate(BLOCK_SIZE);

        buffer.put((byte) (node.isLeaf ? 1 : 0));
        buffer.putInt(node.mbrs.size());

        for (int i = 0; i < node.mbrs.size(); i++) {
            BoundingBox box = node.mbrs.get(i);
            for (int d = 0; d < dimension; d++) {
                buffer.putDouble(box.getBounds().get(d).getLower());
                buffer.putDouble(box.getBounds().get(d).getUpper());
            }
            buffer.putLong(node.childrenPointers.get(i));
        }

        buffer.flip();

        try (RandomAccessFile raf = new RandomAccessFile(indexFilename, "rw");
             FileChannel channel = raf.getChannel()) {
            long offset = (long) blockId * BLOCK_SIZE;
            channel.position(offset);
            channel.write(buffer);
        }
    }

    // /* Read RStarNode from BLOCK N */
    public RStarNode readNode(int blockId, int dimension) throws IOException { 
        ByteBuffer buffer = ByteBuffer.allocate(BLOCK_SIZE);
        try (RandomAccessFile raf = new RandomAccessFile(indexFilename, "r");
             FileChannel channel = raf.getChannel()) {
            long offset = (long) blockId * BLOCK_SIZE;
            channel.position(offset);
            channel.read(buffer);
        }
        buffer.flip();

        boolean isLeaf = (buffer.get() == 1); // 1 for leaf, 0 for internal node
        int entryCount = buffer.getInt();
        RStarNode node = new RStarNode(isLeaf);

        for (int i = 0; i < entryCount; i++) {
            ArrayList<Bounds> boundsList = new ArrayList<>();
            for (int d = 0; d < dimension; d++) {
                double lower = buffer.getDouble();
                double upper = buffer.getDouble();
                boundsList.add(new Bounds(upper, lower));  
            }
            BoundingBox box = new BoundingBox(boundsList);
            long pointer = buffer.getLong();
            node.addEntry(box, pointer);
        }

        return node;
    }
}
