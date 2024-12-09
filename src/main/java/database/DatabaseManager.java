package database;

import java.io.*;
import java.util.Map;

import memory.*;


public class DatabaseManager {
    private final RAM ram;
    private final BTree bTree;
    private final File dataDirectory;
    private final File BTreeDirectory;
    private int location = 0;

    public DatabaseManager(String dataDirectory, String BTreeDirectory) throws IOException {
        ram = new RAM();
        bTree = new BTree(2, this);
        this.dataDirectory = new File(dataDirectory);
        this.BTreeDirectory = new File(BTreeDirectory);
    }

    public void loadRecordsAndSerializeIndex() throws IOException {
        int blockNumber = 0;
        BlockOfMemory dataBlock;
        while ((dataBlock = loadDataBlockFromDisk(blockNumber)) != null) {
            int index = 0;

            while (index < dataBlock.getSize()) {
                Record record = ram.readRecordFromBlock(dataBlock);
                if (record.getFirst() != -1) {
                    int key = record.getKey();
                    int location = getNextLocation();
                    bTree.insert(key, location);
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
        int locationNumber = bTree.search(key);

        if (locationNumber == -1) {
            System.out.println(ColorCode.RED + "Record with key " + key + " not found." + ColorCode.RESET);
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

        System.out.println(ColorCode.GREEN + "Record found on line " + lineNumber +
                " in block " + blockNumber +  ": " + record.toString() + ColorCode.RESET);
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

    public void deleteNodeFromDisk(BTreeNode node) {
        try {
            File nodeFile = new File(BTreeDirectory.getPath() + "\\block_" + node.getNodeID() + ".txt");
            if (nodeFile.exists()) {
                if (nodeFile.delete()) {
                    System.out.println("Node file " + node.getNodeID() + " deleted successfully.");
                } else {
                    System.out.println("Failed to delete node file " + node.getNodeID() + ".");
                }
            }
        } catch (Exception e) {
            System.out.println("Error while deleting node file: " + e.getMessage());
            e.printStackTrace();
        }
    }


    public void writeModifiedNodes(BTree tree) {
        for (BTreeNode node : tree.getModifiedNodes()) {
            writeNodeToDisk(node);
        }
        tree.clearModifiedNodes();
    }

    public void deleteNodes(BTree tree) {
        for (BTreeNode node : tree.getDeletedNodes()) {
            deleteNodeFromDisk(node);
        }
        tree.clearDeletedNodes();
    }



    public void insert(Record record) {
        int location = getNextLocation();

        int key = record.getKey();
        boolean isInsertSuccessful = bTree.insert(key, location);
        if (!isInsertSuccessful) {
            System.out.println(ColorCode.RED + "Record with key " + record.getKey() +
                    " already exists in the database." + ColorCode.RESET);
            bTree.clearAllNodes();
            ram.resetStats();
            return;
        }

        writeModifiedNodes(bTree);
        appendRecordToFile(record, location);

        bTree.clearAllNodes();
        printStats();
    }


    private void appendRecordToFile(Record record, int location) {
        int blockNumber = location / (BlockOfMemory.BUFFER_SIZE / Record.RECORD_SIZE);
        String blockPath = dataDirectory.getPath() + "\\block_" + blockNumber + ".txt";
        File blockFile = new File(blockPath);
        BlockOfMemory block;

        if (!blockFile.exists()) {
            block = new BlockOfMemory();
            try {
                blockFile.createNewFile();
            } catch (IOException e) {
                System.out.println("Error while creating file: " + e.getMessage());
                e.printStackTrace();
                return;
            }
        } else {
            block = loadDataBlockFromDisk(blockNumber);
        }

        DiskFile dataFile = new DiskFile(blockPath);
        ram.writeRecordToBlock(block, record);
        ram.writeDataBlockToDisk(dataFile, block);

        int b = BlockOfMemory.BUFFER_SIZE / Record.RECORD_SIZE;
        int lineNumber = location % b + 1;
        System.out.println(ColorCode.GREEN + "Record inserted successfully to line " + lineNumber +
                " in block " + blockNumber +". Key: " + record.getKey() + ", Location: " + location + ColorCode.RESET);
    }

    public void updateRecord(int key, Record updatedRecord) {
        // Search for the record in the B-Tree
        int locationNumber = bTree.search(key);

        if (locationNumber == -1) {
            System.out.println(ColorCode.RED + "Record with key " + key + " not found. Update failed." + ColorCode.RESET);
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
            System.out.println(ColorCode.RED + "Failed to load block " + blockNumber + ". Update failed." + ColorCode.RESET);
            bTree.clearAllNodes();
            return;
        }

        int size = block.getSize();
        // Read the current record
        block.setIndex(indexInBlock);
        Record existingRecord = ram.readRecordFromBlock(block);

        if (existingRecord.getKey() != key) {
            System.out.println(ColorCode.RED + "Key mismatch in block. Update aborted." + ColorCode.RESET);
            bTree.clearAllNodes();
            return;
        }

        block.setSize(indexInBlock);
        ram.writeRecordToBlock(block, updatedRecord);
        block.setIndex(0);
        block.setSize(size);
        ram.writeDataBlockToDisk(dataFile, block);

        System.out.println(ColorCode.GREEN + "Record with key " + key + " successfully updated." + ColorCode.RESET);

        bTree.clearAllNodes();
        printStats();
    }

    public void delete(int key){
        int location = bTree.delete(key);

        if (location == -1) {
            System.out.println(ColorCode.RED + "Record with key " + key + " not found." + ColorCode.RESET);
            bTree.clearAllNodes();
            bTree.clearModifiedNodes();
            printStats();
            return;
        }

        int b = BlockOfMemory.BUFFER_SIZE / Record.RECORD_SIZE;
        int blockNumber = location / b;

        DiskFile dataFile = new DiskFile(dataDirectory.getPath() + "\\block_" + blockNumber + ".txt");
        BlockOfMemory block = ram.loadBlockFromData(dataFile);

        int index = (location % b) * Record.RECORD_SIZE;
        block.setIndex(index);

        Record record = ram.readRecordFromBlock(block);
        ram.deleteRecordFromBlock(index, block);
        int lineNumber = location % b + 1;
        ram.writeDataBlockToDisk(dataFile, block);

        writeModifiedNodes(bTree);
        deleteNodes(bTree);
        bTree.clearAllNodes();
        System.out.println(ColorCode.GREEN + "Record with key " + key + " deleted from line " + lineNumber +
                " in block " + blockNumber + ": " + record.toString() + ColorCode.RESET);
        printStats();
    }

    public void printDataBlock(int blockNumber) {
        BlockOfMemory block = loadDataBlockFromDisk(blockNumber);
        if (block == null) {
            System.out.println(ColorCode.RED + "Block " + blockNumber + " not found." + ColorCode.RESET);
            return;
        }

        System.out.println(ColorCode.CYAN + "Block " + blockNumber + ":" + ColorCode.RESET);
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

    public int getNextLocation() {
        return location++;
    }

    public void printStats() {
        System.out.println(ColorCode.CYAN + "Statistics:" + ColorCode.RESET);
        System.out.println("Data read operations: " + ram.getReadOperationsData());
        System.out.println("Data write operations: " + ram.getWriteOperationsData());
        System.out.println("B-Tree read operations: " + ram.getReadOperationsBTree());
        System.out.println("B-Tree write operations: " + ram.getWriteOperationsBTree());

        ram.resetStats();
    }

    public String getDataDirectory() {
        return dataDirectory.getPath();
    }

    public String getBTreeDirectory() {
        return BTreeDirectory.getPath();
    }
}
