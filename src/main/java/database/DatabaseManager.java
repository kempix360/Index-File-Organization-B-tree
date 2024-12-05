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
    private int numberOfNodes;
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
        numberOfNodes = allNodes.size();
        bTree.clearAllNodes();
        ram.resetStats();

        System.out.println("End of serialization.");
    }

//    public void search(int key) {
//        final String RESET = "\u001B[0m";
//        final String YELLOW = "\u001B[33m";
//        final String RED = "\u001B[31m";
//        int currentNodeID = rootID;
//
//        while (true) {
//            // Load the current node from disk
//            BTreeNode currentNode = loadNodeFromDisk(currentNodeID);
//            if (currentNode == null) {
//                System.out.println("Error: Unable to load node with ID: " + currentNodeID);
//                return;
//            }
//
//            List<Integer> keys = currentNode.getKeys();
//            List<Integer> locations = currentNode.getLocations();
//            List<Integer> children = currentNode.getChildrenIDs();
//
//            for (int i = 0; i < keys.size(); i++) {
//                if (key == keys.get(i)) {
//                    int lineNumber = locations.get(i);
//
//                    int b = BlockOfMemory.BUFFER_SIZE / Record.RECORD_SIZE;
//                    int blockNumber = lineNumber / b;
//
//                    BlockOfMemory block = ram.loadBlockFromData(dataFile, blockNumber);
//
//                    int index = (lineNumber % b) * Record.RECORD_SIZE;
//                    block.setIndex(index);
//
//                    Record record = ram.readRecordFromBlock(block);
//                    lineNumber++;
//                    System.out.println(YELLOW + "Record found on line " + lineNumber + ": " + record.toString() + RESET);
//                    printStats();
//                    return;
//                }
//
//                if (key < keys.get(i)) {
//                    if (children.isEmpty()) {
//                        System.out.println(RED + "Record with key " + key + " not found." + RESET);
//                        printStats();
//                        return;
//                    }
//                    currentNodeID = children.get(i);
//                    break;
//                }
//
//                if (i == keys.size() - 1) {
//                    currentNodeID = children.get(children.size() - 1);
//                }
//            }
//        }
//    }

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

    public void insert(Record record) throws IOException {
        int location = appendRecordToFile(record);

        if (location == -1) {
            System.out.println("Error: Failed to append record to file.");
            return;
        }

        int key = record.getKey();

        if (rootID == -1) {
            BTreeNode rootNode = new BTreeNode(bTree);
            rootNode.getKeys().add(key);
            rootNode.getLocations().add(location);
            rootNode.assignNodeID();

            rootID = rootNode.getNodeID();
            bTree.writeNodeToMap(rootNode);
            numberOfNodes++;
            return;
        }

        BTreeNode rootNode = bTree.loadNodeByID(rootID);

        if (rootNode.getKeys().size() == 2 * bTree.getT() - 1) {
            BTreeNode newRoot = new BTreeNode(bTree);
            newRoot.assignNodeID();
            newRoot.getChildrenIDs().add(rootID);
            rootNode.setParentID(newRoot.getNodeID());

            newRoot.splitChild(0, rootNode);

            int i = 0;
            if (newRoot.getKeys().get(0) < key) {
                i++;
            }
            BTreeNode childNode = bTree.loadNodeByID(newRoot.getChildrenIDs().get(i));
            childNode.insertNonFull(key, location);
            bTree.writeNodeToMap(childNode);

            rootID = newRoot.getNodeID();
            bTree.writeNodeToMap(newRoot);
            numberOfNodes++;
        } else {
            rootNode.insertNonFull(key, location);
            bTree.writeNodeToMap(rootNode);
        }

        System.out.println("Record inserted successfully. Key: " + key + ", Location: " + location);
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

    public void printTree() {
        System.out.println("B-Tree structure:");
        printTreeRecursively(rootID, 0);
        printStats();
    }

    private void printTreeRecursively(int nodeID, int level) {
        BTreeNode node = loadNodeFromDisk(nodeID);
        if (node == null) {
            System.out.println("Error: Unable to load node with ID: " + nodeID);
            return;
        }

        // indent to show the level of the node
        String indent = "    ".repeat(level);

        System.out.println(indent + "\u001B[33m" + "- NodeID: " + "\u001B[0m" + node.getNodeID());
        System.out.println(indent + "  Keys: " + node.getKeys());
        System.out.println(indent + "  Locations: " + node.getLocations());

        List<Integer> children = node.getChildrenIDs();
        if (children.isEmpty()) {
            System.out.println(indent + "  (Leaf node)");
        } else {
            System.out.println(indent + "  Children:");
            for (int childID : children) {
                printTreeRecursively(childID, level + 1);
            }
        }
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
