package database;

import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;

public class BTree {
    private int rootID;
    private final int t;
    private final int maxSizeOfKeys;
    private int nodeIDCounter = 0;
    private final Map<Integer, BTreeNode> nodes;
    private final List<BTreeNode> modifiedNodes = new ArrayList<>();
    private final DatabaseManager manager;

    public BTree(int t, DatabaseManager manager) {
        this.rootID = -1;
        this.t = t;
        this.maxSizeOfKeys = 2 * t - 1;
        this.nodes = new HashMap<>();
        this.manager = manager;
    }

    public BTree(int t) {
        this.rootID = -1;
        this.t = t;
        this.maxSizeOfKeys = 2 * t - 1;
        this.nodes = new HashMap<>();
        this.manager = null;
    }

    public int getT() {
        return t;
    }

    public int getNextNodeID() {
        return nodeIDCounter++;
    }

    public int getRootID() {
        return rootID;
    }

    public void setRootID(int rootID) {
        this.rootID = rootID;
    }

    public Map<Integer, BTreeNode> getAllNodes() {
        return nodes;
    }

    public void clearAllNodes() {
        nodes.clear();
    }

    public BTreeNode loadNodeByID(int nodeID) {
        if (!nodes.containsKey(nodeID)) {
            BTreeNode node = manager.loadNodeFromDisk(nodeID);
            if (node == null) {
                return null;
            }
            nodes.put(nodeID, node);
        }

        return nodes.get(nodeID);
    }

    public void addModifiedNode(BTreeNode node) {
        if (!modifiedNodes.contains(node)) {
            modifiedNodes.add(node);
        }
    }

    public List<BTreeNode> getModifiedNodes() {
        return modifiedNodes;
    }

    public void clearModifiedNodes() {
        modifiedNodes.clear();
    }

    public void writeNodeToMap(BTreeNode node) {
        nodes.put(node.getNodeID(), node);
    }

    public void insert(int key, int location) {

        if (rootID == -1) {
            BTreeNode root = new BTreeNode(this);
            root.assignNodeID();
            root.getKeys().add(key);
            root.getLocations().add(location);
            rootID = root.getNodeID();
            writeNodeToMap(root);
            addModifiedNode(root);
        } else {
            BTreeNode root = loadNodeByID(rootID);
            if (root.getKeys().size() == maxSizeOfKeys) {
                BTreeNode newRoot = new BTreeNode(this);
                newRoot.assignNodeID();
                newRoot.getChildrenIDs().add(rootID);
                root.setParentID(newRoot.getNodeID());
                newRoot.splitChild(0, root);
                int i = 0;
                if (newRoot.getKeys().get(0) < key) {
                    i++;
                }
                BTreeNode child = loadNodeByID(newRoot.getChildrenIDs().get(i));
                child.insertNonFull(key, location);
                writeNodeToMap(child);
                rootID = newRoot.getNodeID();
                writeNodeToMap(newRoot);
                addModifiedNode(root);
                addModifiedNode(newRoot);
                addModifiedNode(child);
            } else {
                root.insertNonFull(key, location);
                writeNodeToMap(root);
            }
        }
    }

    public Integer search(int key) {
        if (rootID == -1) {
            return -1;
        }
        BTreeNode root = loadNodeByID(rootID);
        return root.search(key);
    }

    public void printTree() {
        System.out.println("B-Tree structure:");
        printTreeRecursively(rootID, 0);
    }

    private void printTreeRecursively(int nodeID, int level) {
        BTreeNode node = loadNodeByID(nodeID);
        if (node == null) {
            System.out.println("Error: Unable to load node with ID: " + nodeID);
            return;
        }

        // Indentation to represent tree levels visually
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
}
