package database;

import java.io.*;
import java.util.List;
import java.util.Map;

import database.BTree;
import database.BTreeNode;
import memory.*;


public class DatabaseManager {
    private DiskFile dataFile;
    private BlockOfMemory dataBlock;
    private final RAM ram;
    private BTree bTree;
    private int rootID;
    private final File directory = new File("src\\disk_files\\Btree_files");

    public DatabaseManager(DiskFile _dataFile) throws IOException {
        this.dataFile = _dataFile;
        ram = new RAM();
        bTree = new BTree(3, this);
        rootID = bTree.getRootID();
    }

    public void loadRecordsAndSerializeIndex() throws IOException {
        int location = 0;
        int blockNumber = 0;
        while ((dataBlock = ram.loadBlockFromData(dataFile, blockNumber)) != null) {
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

        File directory = new File("src\\disk_files\\Btree_files");
        if (!directory.exists()) {
            boolean res = directory.mkdirs();
            if (!res) {
                System.out.println("Error while creating directory for BTree.");
                return;
            }
        }

        Map<Integer, BTreeNode> allNodes = bTree.getAllNodes(); // Assuming this now returns a map of nodeID -> BTreeNode
        for (Map.Entry<Integer, BTreeNode> entry : allNodes.entrySet()) {
            writeNodeToDisk(entry.getValue()); // Serialize each node to disk
        }

        rootID = bTree.getRootID();
        bTree.clearAllNodes();
        bTree.clearModifiedNodes();
        ram.resetStats();

        System.out.println("End of serialization.");
    }

    public void search(int key) {
        final String RESET = "\u001B[0m";
        final String YELLOW = "\u001B[33m";
        final String RED = "\u001B[31m";

        int lineNumber = bTree.search(key);

        if (lineNumber == -1) {
            System.out.println(RED + "Record with key " + key + " not found." + RESET);
            printStats();
            return;
        }

        int b = BlockOfMemory.BUFFER_SIZE / Record.RECORD_SIZE;
        int blockNumber = lineNumber / b;

        BlockOfMemory block = ram.loadBlockFromData(dataFile, blockNumber);

        int index = (lineNumber % b) * Record.RECORD_SIZE;
        block.setIndex(index);

        Record record = ram.readRecordFromBlock(block);
        lineNumber++;
        System.out.println(YELLOW + "Record found on line " + lineNumber + ": " + record.toString() + RESET);
        bTree.clearAllNodes();
        printStats();
    }

    public BTreeNode loadNodeFromDisk(int nodeID) {
        File nodeFile = new File(directory.getPath() + "\\page_" + nodeID + ".txt");

        if (!nodeFile.exists()) {
            System.out.println("Error: Node file not found for nodeID: " + nodeID);
            return null;
        }

        DiskFile file;
        try {
            file = new DiskFile(nodeFile.getPath());
        } catch (IOException e) {
            System.out.println("Error while obtaining the disk file: " + nodeFile.getPath() + " " + e.getMessage());
            e.printStackTrace();
            return null;
        }

        BlockOfMemory block = ram.loadBlockFromBTree(file);

        if (block == null) {
            System.out.println("Error: Failed to load block from file for nodeID: " + nodeID);
            return null;
        }

        return ram.readNodeFromBlock(block, bTree);
    }

    public void writeNodeToDisk(BTreeNode node) {
        File nodeFile = new File(directory.getPath() + "\\page_" + node.getNodeID() + ".txt");

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
        try {
            file = new DiskFile(nodeFile.getPath());
        } catch (IOException e) {
            System.out.println("Error while obtaining DiskFile: " + nodeFile.getPath() + " " + e.getMessage());
            e.printStackTrace();
            return;
        }

        BlockOfMemory block = new BlockOfMemory();

        ram.writeNodeToBlock(block, node);
        ram.writeBtreeBlockToDisk(file, block);
    }

    public void writeModifiedNodes(BTree tree) {
        for (BTreeNode node : tree.getModifiedNodes()) {
            writeNodeToDisk(node); // Existing method to write a node to disk
        }
        tree.clearModifiedNodes(); // Clear the list of modified nodes after writing
    }


    public void insert(Record record) {
        final String RESET = "\u001B[0m";
        final String YELLOW = "\u001B[33m";
        final String RED = "\u001B[31m";

        if (bTree.search(record.getKey()) != -1) {
            System.out.println(RED + "Record with key " + record.getKey() + " already exists in the database." + RESET);
            bTree.clearAllNodes();
            return;
        }

        int location = appendRecordToFile(record);

        if (location == -1) {
            System.out.println("Error: Failed to append record to file.");
            return;
        }

        int key = record.getKey();
        bTree.insert(key, location);
        writeModifiedNodes(bTree);

        System.out.println(YELLOW + "Record inserted successfully. Key: " + key + ", Location: " + location + RESET);
        bTree.clearAllNodes();
        printStats();
    }


    private int appendRecordToFile(Record record) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(dataFile.getFilename(), true))) {
            writer.write(record.getFirst() + " " + record.getSecond() + " " + record.getThird() + " " + record.getKey());
            writer.newLine();

            return getLineNumber();
        }
        catch (IOException e) {
            System.out.println("Error while inserting record to data file: " + e.getMessage());
            e.printStackTrace();
            return -1;
        }
    }

    private int getLineNumber() {
        int lineNumber = 0;
        try (BufferedReader reader = new BufferedReader(new FileReader(dataFile.getFilename()))) {
            while (reader.readLine() != null) {
                lineNumber++;
            }
        } catch (IOException e) {
            System.out.println("Error while reading the file to count lines: " + e.getMessage());
            e.printStackTrace();
            return -1;
        }
        return lineNumber;
    }

    public void print() {
        bTree.printTree();
        bTree.clearAllNodes();
        printStats();
    }

    public void deleteDirectory() {
        if (directory.exists()) {
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    boolean result = file.delete();
                    if (!result) {
                        System.out.println("Error while deleting file: " + file.getPath());
                    }
                }
            }
            boolean result = directory.delete();
            if (!result) {
                System.out.println("Error while deleting directory: " + directory.getPath());
            }
        }
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

    public BTree getBTree() {
        return bTree;
    }

    public DiskFile getDataFile() {
        return dataFile;
    }

    public RAM getRam() {
        return ram;
    }

    public int getRootID() {
        return rootID;
    }

}
