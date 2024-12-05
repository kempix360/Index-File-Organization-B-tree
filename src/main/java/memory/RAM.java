package memory;

import database.BTree;
import database.BTreeNode;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class RAM {

    private int readOperationsData;
    private int writeOperationsData;
    private int readOperationsBtree;
    private int writeOperationsBtree;

    public RAM() {
        readOperationsData = 0;
        writeOperationsData = 0;
        readOperationsBtree = 0;
        writeOperationsBtree = 0;
    }

    // DATA
    // --------------------------------------------------------------------------------------------

    public BlockOfMemory loadBlockFromData(DiskFile file, int blockNumber) {
        try (BufferedReader reader = new BufferedReader(new FileReader(file.getFilename()))) {
            int b = BlockOfMemory.BUFFER_SIZE / Record.RECORD_SIZE;
            int recordsToSkip = blockNumber * b;
            int currentLine = 0;

            String line;
            while (currentLine < recordsToSkip && reader.readLine() != null) {
                currentLine++;
            }

            byte[] buffer = new byte[BlockOfMemory.BUFFER_SIZE];
            int bufferIndex = 0;

            while ((line = reader.readLine()) != null && bufferIndex < BlockOfMemory.BUFFER_SIZE) {
                String[] tokens = line.split("\\s+");
                for (String token : tokens) {
                    try {
                        int number = Integer.parseInt(token);

                        buffer[bufferIndex] = (byte) (number >> 24);
                        buffer[bufferIndex + 1] = (byte) (number >> 16);
                        buffer[bufferIndex + 2] = (byte) (number >> 8);
                        buffer[bufferIndex + 3] = (byte) number;
                        bufferIndex += 4;
                    } catch (NumberFormatException e) {
                        e.printStackTrace();
                    }

                    if (bufferIndex >= BlockOfMemory.BUFFER_SIZE) {
                        break;
                    }
                }
            }

            if (bufferIndex > 0) {
                readOperationsData++;
                return new BlockOfMemory(buffer, bufferIndex);
            } else {
                return null;
            }
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public void writeDataBlockToDisk(DiskFile file, BlockOfMemory _blockOfMemory) {
        if (_blockOfMemory == null) {
            return;
        }

        try {
            DataOutputStream outputStream = new DataOutputStream(file.getFileOutputStream());
            byte[] data = _blockOfMemory.getBuffer();
            int size = _blockOfMemory.getSize();
            int index = _blockOfMemory.getIndex();

            for (int i = index; i < size; i += 4) {
                if (i + 3 < size) {
                    int number = ((data[i] & 0xFF) << 24) |
                            ((data[i + 1] & 0xFF) << 16) |
                            ((data[i + 2] & 0xFF) << 8) |
                            (data[i + 3] & 0xFF);
                    outputStream.writeBytes(number + " ");

                    // After every third integer, write a newline
                    if ((i / 4 + 1) % 3 == 0) {
                        outputStream.writeBytes("\n");
                    }
                }
            }

            writeOperationsData++;

        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public Record readRecordFromBlock(BlockOfMemory blockOfMemory) {
        if (blockOfMemory == null) {
            return new Record(-1, -1, -1, -1);
        }

        byte[] data = blockOfMemory.getBuffer();
        int size = blockOfMemory.getSize();
        int index = blockOfMemory.getIndex();
        int recordSize = Record.RECORD_SIZE;

        int[] record_values = new int[4];
        int values_index = 0;

        if (index < 0 || (index + recordSize) > BlockOfMemory.BUFFER_SIZE) {
            return new Record(-1, -1, -1, -1);
        }

        for (int i = index; i < index + recordSize; i += 4) {

            if (i + 4 <= size) {
                int number = ((data[i] & 0xFF) << 24) |
                        ((data[i + 1] & 0xFF) << 16) |
                        ((data[i + 2] & 0xFF) << 8) |
                        (data[i + 3] & 0xFF);
                record_values[values_index] = number;
                values_index++;
            }
        }

        return new Record(record_values[0], record_values[1], record_values[2], record_values[3]);
    }

    public void writeRecordToBlock(BlockOfMemory blockOfMemory, Record record) {
        if (blockOfMemory == null) {
            return;
        }

        if (record == null || record.getFirst() == -1 || record.getSecond() == -1 || record.getThird() == -1) {
            return;
        }

        byte[] data = blockOfMemory.getBuffer();
        int size = blockOfMemory.getSize();
        int recordSize = Record.RECORD_SIZE;

        if ((size + recordSize) > BlockOfMemory.BUFFER_SIZE) {
            return;
        }

        int[] record_values = {record.getFirst(), record.getSecond(), record.getThird(), record.getKey()};
        int values_index = 0;

        for (int i = size; i < size + recordSize; i += 4) {
            int number = record_values[values_index];
            data[i] = (byte) (number >> 24);
            data[i + 1] = (byte) (number >> 16);
            data[i + 2] = (byte) (number >> 8);
            data[i + 3] = (byte) number;
            values_index++;
        }

        blockOfMemory.setSize(size + recordSize);

    }

    // BTREE
    // --------------------------------------------------------------------------------------------

    public BlockOfMemory loadBlockFromBTree(DiskFile file) {
        String filename = file.getFilename();

        try (Scanner scanner = new Scanner(new File(filename))) {
            byte[] buffer = new byte[BlockOfMemory.BUFFER_SIZE];
            int bufferIndex = 0;

            while (scanner.hasNext()) {
                String token = scanner.next();

                if (token.endsWith("=") || token.endsWith("[") || token.equals("]")) {
                    continue;
                }

                try {
                    int number = Integer.parseInt(token);

                    buffer[bufferIndex] = (byte) (number >> 24);
                    buffer[bufferIndex + 1] = (byte) (number >> 16);
                    buffer[bufferIndex + 2] = (byte) (number >> 8);
                    buffer[bufferIndex + 3] = (byte) number;
                    bufferIndex += 4;

                    if (bufferIndex >= BlockOfMemory.BUFFER_SIZE) {
                        break;
                    }
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                }
            }

            readOperationsBtree++;
            return new BlockOfMemory(buffer, bufferIndex);

        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }



    public void writeBtreeBlockToDisk(DiskFile file, BlockOfMemory block) {
        if (block == null) {
            return;
        }

        try (DataOutputStream outputStream = new DataOutputStream(new FileOutputStream(file.getFilename()))) {
            StringBuilder sb = new StringBuilder();
            block.setIndex(0);

            int nodeID = readIntFromBuffer(block);
            sb.append("nodeID= ").append(nodeID).append(" ");
            int parentID = readIntFromBuffer(block);
            sb.append("parentID= ").append(parentID).append(" ");
            int keysSize = readIntFromBuffer(block);
            sb.append("keysSize= ").append(keysSize).append(" ");
            int childrenSize = readIntFromBuffer(block);
            sb.append("childrenSize= ").append(childrenSize).append(" ");

            sb.append("keys=[ ");
            for (int i = 0; i < keysSize; i++) {
                int key = readIntFromBuffer(block);
                sb.append(key).append(" ");
            }

            sb.append("] locations=[ ");
            for (int i = 0; i < keysSize; i++) {
                int location = readIntFromBuffer(block);
                sb.append(location).append(" ");
            }

            sb.append("] children=[ ");
            for (int i = 0; i < childrenSize; i++) {
                int child = readIntFromBuffer(block);
                sb.append(child).append(" ");
            }
            sb.append("]");

            outputStream.writeBytes(sb.toString());
            writeOperationsBtree++;

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public BTreeNode readNodeFromBlock(BlockOfMemory block, BTree tree) {
        if (block == null) {
            return null;
        }

        block.setIndex(0);

        int nodeID = readIntFromBuffer(block);
        int parentID = readIntFromBuffer(block);
        int keysSize = readIntFromBuffer(block);
        int childrenSize = readIntFromBuffer(block);

        List<Integer> keys = new ArrayList<>();
        for (int i = 0; i < keysSize; i++) {
            keys.add(readIntFromBuffer(block));
        }

        List<Integer> locations = new ArrayList<>();
        for (int i = 0; i < keysSize; i++) {
            locations.add(readIntFromBuffer(block));
        }

        List<Integer> children = new ArrayList<>();
        for (int i = 0; i < childrenSize; i++) {
            children.add(readIntFromBuffer(block));
        }

        return new BTreeNode(tree, nodeID, parentID, keys, locations, children);
    }

    public void writeNodeToBlock(BlockOfMemory block, BTreeNode node) {
        if (block == null || node == null) {
            return;
        }

        writeIntToBuffer(block, node.getNodeID());
        writeIntToBuffer(block, node.getParentID());
        writeIntToBuffer(block, node.getKeys().size());
        writeIntToBuffer(block, node.getChildrenIDs().size());

        for (int key : node.getKeys()) {
            writeIntToBuffer(block, key);
        }

        for (int location : node.getLocations()) {
            writeIntToBuffer(block, location);
        }

        for (int childID : node.getChildrenIDs()) {
            writeIntToBuffer(block, childID);
        }
    }

    private int readIntFromBuffer(BlockOfMemory block) {
        byte[] buffer = block.getBuffer();
        int offset = block.getIndex();
        block.setIndex(offset + 4);

        return ((buffer[offset] & 0xFF) << 24) |
                ((buffer[offset + 1] & 0xFF) << 16) |
                ((buffer[offset + 2] & 0xFF) << 8) |
                (buffer[offset + 3] & 0xFF);
    }

    private void writeIntToBuffer(BlockOfMemory block, int value) {
        int offset = block.getSize();
        block.getBuffer()[offset] = (byte) (value >> 24);
        block.getBuffer()[offset + 1] = (byte) (value >> 16);
        block.getBuffer()[offset + 2] = (byte) (value >> 8);
        block.getBuffer()[offset + 3] = (byte) value;
        block.setSize(offset + 4);
    }

    public int getReadOperationsData() {
        return readOperationsData;
    }

    public int getWriteOperationsData() {
        return writeOperationsData;
    }

    public int getReadOperationsBTree() {
        return readOperationsBtree;
    }

    public int getWriteOperationsBTree() {
        return writeOperationsBtree;
    }

    public void resetStats() {
        readOperationsData = 0;
        writeOperationsData = 0;
        readOperationsBtree = 0;
        writeOperationsBtree = 0;
    }

}