package memory;

public class BlockOfMemory {
    public static final int BUFFER_SIZE = 4800;
    private final byte[] buffer;
    private int size; // Actual size of data in the block
    private int index = 0; // Index of the next record to be read

    public BlockOfMemory() {
        this.buffer = new byte[BUFFER_SIZE];
        this.size = 0;
    }

    public BlockOfMemory(byte[] data, int size) {
        this.buffer = new byte[BUFFER_SIZE];
        System.arraycopy(data, 0, this.buffer, 0, BUFFER_SIZE);
        this.size = size;
    }

    public byte[] getBuffer() {
        return buffer;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public void clear() {
        for (int i = 0; i < size; i++) {
            buffer[i] = 0;
        }
        size = 0;
    }

}
