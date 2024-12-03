import java.io.File;
import java.io.IOException;
import java.util.List;

import Btree.BTree;
import Btree.BTreeNode;
import memory.*;


public class DatabaseManager {
    private DiskFile dataFile;
    private BlockOfMemory dataBlock;
    private RAM ram;
    private BTree bTree;
    private int rootID;
    private File directory = new File("src\\disk_files\\Btree_files");

    public DatabaseManager(DiskFile _dataFile) throws IOException {
        this.dataFile = _dataFile;
        ram = new RAM();
        bTree = new BTree(3);
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

        List<BTreeNode> nodes = bTree.getAllNodes();
        for (BTreeNode node : nodes) {
            BlockOfMemory block = new BlockOfMemory();
            File nodeFile = new File(directory.getPath() + "\\page_" + node.getNodeID() + ".txt");

            if (!nodeFile.exists()) {
                try {
                    boolean created = nodeFile.createNewFile();
                    if (!created) {
                        System.out.println("Failed to create file: " + nodeFile.getPath());
                    }
                } catch (IOException e) {
                    System.out.println("Error while creating a file: " + e.getMessage());
                    e.printStackTrace();
                }
            }
            DiskFile file = new DiskFile(nodeFile.getPath());
            file.resetFileOutputStream();
            ram.writeNodeToBlock(block, node);
            ram.writeBtreeBlockToDisk(file, block);
        }
        rootID = bTree.getRootID();
        ram.resetStats();

        System.out.println("End of serialization.");
    }

    public Record search(int key) {
        final String RESET = "\u001B[0m";
        final String YELLOW = "\u001B[33m";
        final String CYAN = "\u001B[36m";
        final String RED = "\u001B[31m";
        int currentNodeID = rootID;

        while (true) {
            // Load the current node from disk
            BTreeNode currentNode = loadNodeFromDisk(currentNodeID);
            if (currentNode == null) {
                System.out.println("Error: Unable to load node with ID: " + currentNodeID);
                return new Record(-1, -1, -1, -1);
            }

            List<Integer> keys = currentNode.getKeys();
            List<Integer> locations = currentNode.getLocations();
            List<Integer> children = currentNode.getChildrenIDs();

            for (int i = 0; i < keys.size(); i++) {
                if (key == keys.get(i)) {
                    int lineNumber = locations.get(i);

                    int b = BlockOfMemory.BUFFER_SIZE / Record.RECORD_SIZE;
                    int blockNumber = lineNumber / b;

                    BlockOfMemory block = ram.loadBlockFromData(dataFile, blockNumber);

                    int index = (lineNumber % b) * Record.RECORD_SIZE;
                    block.setIndex(index);

                    Record record = ram.readRecordFromBlock(block);
                    lineNumber++;
                    System.out.println(YELLOW + "Record found on line " + lineNumber + ": " + record.toString() + RESET);
                    printStats();
                    return record;
                }

                if (key < keys.get(i)) {
                    if (children.isEmpty()) {
                        System.out.println(RED + "Record with key " + key + " not found." + RESET);
                        printStats();
                        return new Record(-1, -1, -1, -1);
                    }
                    currentNodeID = children.get(i);
                    break;
                }

                if (i == keys.size() - 1) {
                    currentNodeID = children.get(children.size() - 1);
                }
            }
        }
    }


    private BTreeNode loadNodeFromDisk(int nodeID) {
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

        return ram.readNodeFromBlock(block);
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
        final String YELLOW = "\u001B[33m";
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
