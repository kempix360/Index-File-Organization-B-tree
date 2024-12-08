package database;

import java.io.*;
import java.util.Map;

import memory.*;


public class DatabaseManager {
    private BlockOfMemory dataBlock;
    private final RAM ram;
    private BTree bTree;
    private final File dataDirectory;
    private final File BTreeDirectory;

    public DatabaseManager(String dataDirectory, String BTreeDirectory) throws IOException {
        ram = new RAM();
        bTree = new BTree(2, this);
        this.dataDirectory = new File(dataDirectory);
        this.BTreeDirectory = new File(BTreeDirectory);
    }

    public void loadRecordsAndSerializeIndex() throws IOException {
        int location = 0;
        int blockNumber = 0;
        while ((dataBlock = loadDataBlockFromDisk(blockNumber)) != null) {
            int index = 0;

            while (index < dataBlock.getSize()) {
                Record record = ram.readRecordFromBlock(dataBlock);
                if (record.getFirst() != -1) {
                    int key = record.getKey();
                    bTree.insert(key, location);
                    location++;
                }
                dataBlock.setIndex(index + Record.RECORD_SIZE);
                index = dataBlock.getIndex();
            }
            blockNumber++;
        }

        Map<Integer, BTreeNode> allNodes = bTree.getAllNodes(); // Assuming this now returns a map of nodeID -> BTreeNode
        for (Map.Entry<Integer, BTreeNode> entry : allNodes.entrySet()) {
            writeNodeToDisk(entry.getValue()); // Serialize each node to disk
        }

        bTree.clearAllNodes();
        bTree.clearModifiedNodes();
        ram.resetStats();

        System.out.println("End of serialization.");
    }

    public void search(int key) {
        final String RESET = "\u001B[0m";
        final String GREEN = "\u001B[32m";
        final String RED = "\u001B[31m";

        int locationNumber = bTree.search(key);

        if (locationNumber == -1) {
            System.out.println(RED + "Record with key " + key + " not found." + RESET);
            printStats();
            return;
        }

        int b = BlockOfMemory.BUFFER_SIZE / Record.RECORD_SIZE;
        int blockNumber = locationNumber / b;

        DiskFile dataFile = new DiskFile(dataDirectory.getPath() + "\\block_" + blockNumber + ".txt");
        BlockOfMemory block = ram.loadBlockFromData(dataFile);

        int index = (locationNumber % b) * Record.RECORD_SIZE;
        block.setIndex(index);

        Record record = ram.readRecordFromBlock(block);
        int lineNumber = locationNumber % b + 1;

        System.out.println(GREEN + "Record found on line " + lineNumber +
                " in block " + blockNumber +  ": " + record.toString() + RESET);
        bTree.clearAllNodes();
        printStats();
    }

    public BTreeNode loadNodeFromDisk(int nodeID) {
        File nodeFile = new File(BTreeDirectory.getPath() + "\\block_" + nodeID + ".txt");

        if (!nodeFile.exists()) {
            System.out.println("Error: Node file not found for nodeID: " + nodeID);
            return null;
        }

        DiskFile file;
        file = new DiskFile(nodeFile.getPath());

        BlockOfMemory block = ram.loadBlockFromBTree(file);

        if (block == null) {
            System.out.println("Error: Failed to load block from file for nodeID: " + nodeID);
            return null;
        }

        return ram.readNodeFromBlock(block, bTree);
    }

    public void writeNodeToDisk(BTreeNode node) {
        File nodeFile = new File(BTreeDirectory.getPath() + "\\block_" + node.getNodeID() + ".txt");

        // Check if the file exists, if not, create it
        if (!nodeFile.exists()) {
            try {
                boolean created = nodeFile.createNewFile();
                if (!created) {
                    System.out.println("Failed to create file: " + nodeFile.getPath());
                    return;
                }
            } catch (IOException e) {
                System.out.println("Error while creating file: " + e.getMessage());
                e.printStackTrace();
                return;
            }
        }

        // Create a DiskFile object for the node file
        DiskFile file;
        file = new DiskFile(nodeFile.getPath());

        BlockOfMemory block = new BlockOfMemory();

        ram.writeNodeToBlock(block, node);
        ram.writeBtreeBlockToDisk(file, block);
    }

    public BlockOfMemory loadDataBlockFromDisk(int blockNumber) {
        String path = dataDirectory.getPath() + "\\block_" + blockNumber + ".txt";
        if (!new File(path).exists()) {
            return null;
        }

        DiskFile dataFile = new DiskFile(path);

        BlockOfMemory block = ram.loadBlockFromData(dataFile);
        if (block == null) {
            System.out.println("Error: Failed to load block from file.");
            return null;
        }
        return block;
    }

    public void writeModifiedNodes(BTree tree) {
        for (BTreeNode node : tree.getModifiedNodes()) {
            writeNodeToDisk(node); // Existing method to write a node to disk
        }
        tree.clearModifiedNodes(); // Clear the list of modified nodes after writing
    }


    public void insert(Record record) {
        final String RESET = "\u001B[0m";
        final String GREEN = "\u001B[32m";
        final String RED = "\u001B[31m";

        if (bTree.search(record.getKey()) != -1) {
            System.out.println(RED + "Record with key " + record.getKey() + " already exists in the database." + RESET);
            bTree.clearAllNodes();
            ram.resetStats();
            return;
        }

        int blockNumber = getNumberOfBlocksInDirectory(dataDirectory) - 1;
        BlockOfMemory block = loadDataBlockFromDisk(blockNumber);

        int b = BlockOfMemory.BUFFER_SIZE / Record.RECORD_SIZE;
        int location = block.getSize() / Record.RECORD_SIZE * blockNumber * b;
        int lineNumber = location % b + 1;

        int key = record.getKey();
        bTree.insert(key, location);
        writeModifiedNodes(bTree);



        System.out.println(GREEN + "Record inserted successfully to " + lineNumber +
                " in block " + blockNumber +". Key: " + key + ", Location: " + location + RESET);
        bTree.clearAllNodes();
        bTree.clearModifiedNodes();
        printStats();
    }


    private int appendRecordToFile(Record record) {
        int blockNumber = getNumberOfBlocksInDirectory(dataDirectory) - 1;
        int location;

        BlockOfMemory block = loadDataBlockFromDisk(blockNumber);
        if (block == null) {
            System.out.println("Error: Failed to load block from file.");
            return -1;
        }

        int size = block.getSize();
        if (size == BlockOfMemory.BUFFER_SIZE) {
            blockNumber++;
            block = new BlockOfMemory();
            String path = dataDirectory.getPath() + "\\block_" + blockNumber + ".txt";
            File dataFile = new File(path);

            try {
                dataFile.createNewFile();
            }
            catch (IOException e) {
                System.out.println("Error while creating file: " + e.getMessage());
                e.printStackTrace();
                return -1;
            }
            size = 0;
        }

        location = blockNumber * BlockOfMemory.BUFFER_SIZE / Record.RECORD_SIZE + size / Record.RECORD_SIZE;

        DiskFile dataFile = new DiskFile(dataDirectory.getPath() + "\\block_" + blockNumber + ".txt");
        ram.writeRecordToBlock(block, record);
        ram.writeDataBlockToDisk(dataFile, block);

        return location;
    }

    public void updateRecord(int key, Record updatedRecord) {
        final String RESET = "\u001B[0m";
        final String RED = "\u001B[31m";
        final String GREEN = "\u001B[32m";

        // Search for the record in the B-Tree
        int locationNumber = bTree.search(key);

        if (locationNumber == -1) {
            System.out.println(RED + "Record with key " + key + " not found. Update failed." + RESET);
            bTree.clearAllNodes();
            printStats();
            return;
        }

        // Calculate block and line details
        int b = BlockOfMemory.BUFFER_SIZE / Record.RECORD_SIZE;
        int blockNumber = locationNumber / b;
        int indexInBlock = (locationNumber % b) * Record.RECORD_SIZE;

        // Load the block containing the record
        DiskFile dataFile = new DiskFile(dataDirectory.getPath() + "\\block_" + blockNumber + ".txt");
        BlockOfMemory block = ram.loadBlockFromData(dataFile);

        if (block == null) {
            System.out.println(RED + "Failed to load block " + blockNumber + ". Update failed." + RESET);
            bTree.clearAllNodes();
            return;
        }

        int size = block.getSize();
        // Read the current record
        block.setIndex(indexInBlock);
        Record existingRecord = ram.readRecordFromBlock(block);

        if (existingRecord.getKey() != key) {
            System.out.println(RED + "Key mismatch in block. Update aborted." + RESET);
            bTree.clearAllNodes();
            return;
        }

        block.setSize(indexInBlock);
        ram.writeRecordToBlock(block, updatedRecord);
        block.setIndex(0);
        block.setSize(size);
        ram.writeDataBlockToDisk(dataFile, block);

        System.out.println(GREEN + "Record with key " + key + " successfully updated." + RESET);

        bTree.clearAllNodes();
        printStats();
    }


    public void printDataBlock(int blockNumber) {
        final String RESET = "\u001B[0m";
        final String CYAN = "\u001B[36m";
        final String RED = "\u001B[31m";

        BlockOfMemory block = loadDataBlockFromDisk(blockNumber);
        if (block == null) {
            System.out.println(RED + "Block " + blockNumber + " not found." + RESET);
            return;
        }

        System.out.println(CYAN + "Block " + blockNumber + ":" + RESET);
        int size = block.getSize();
        int counter = 0;
        for (int i = 0; i < size; i += Record.RECORD_SIZE) {
            Record record = ram.readRecordFromBlock(block);
            block.setIndex(i + Record.RECORD_SIZE);
            counter++;
            System.out.println(counter + ". record: " + record.toString());
        }
    }

    public void printBTree() {
        bTree.printTree();
        bTree.clearAllNodes();
        printStats();
    }

    public int getNumberOfBlocksInDirectory(File directory) {
        return directory.listFiles().length;
    }

    public void printStats() {
        final String RESET = "\u001B[0m";
        final String CYAN = "\u001B[36m";
        System.out.println(CYAN + "Statistics:" + RESET);
        System.out.println("Data read operations: " + ram.getReadOperationsData());
        System.out.println("Data write operations: " + ram.getWriteOperationsData());
        System.out.println("B-Tree read operations: " + ram.getReadOperationsBTree());
        System.out.println("B-Tree write operations: " + ram.getWriteOperationsBTree());

        ram.resetStats();
    }

}
