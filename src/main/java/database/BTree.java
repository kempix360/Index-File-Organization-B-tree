package database;

import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;

public class BTree {
    private int rootID;
    private final int d;
    private int nodeIDCounter = 0;
    private final Map<Integer, BTreeNode> nodes;
    private final List<BTreeNode> modifiedNodes = new ArrayList<>();
    private final List<BTreeNode> deletedNodes = new ArrayList<>();
    private final DatabaseManager manager;

    public BTree(int d, DatabaseManager manager) {
        this.rootID = -1;
        this.d = d;
        this.nodes = new HashMap<>();
        this.manager = manager;
    }

    public int getD() {
        return d;
    }

    public int getNextNodeID() {
        return nodeIDCounter++;
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

    public void addDeletedNode(BTreeNode node) {
        if (!deletedNodes.contains(node)) {
            deletedNodes.add(node);
        }
    }

    public List<BTreeNode> getDeletedNodes() {
        return deletedNodes;
    }

    public void clearDeletedNodes() {
        deletedNodes.clear();
    }

    public void writeNodeToMap(BTreeNode node) {
        nodes.put(node.getNodeID(), node);
    }

    public void deleteNodeFromMap(BTreeNode node) {
        nodes.remove(node.getNodeID());
    }

    public boolean insert(int key, int location) {

        if (rootID == -1) {
            BTreeNode root = new BTreeNode(this);
            root.assignNodeID();
            root.getKeys().add(key);
            root.getLocations().add(location);
            rootID = root.getNodeID();
            writeNodeToMap(root);
            addModifiedNode(root);
            return true;
        } else {
            BTreeNode root = loadNodeByID(rootID);
            return root.insertNode(key, location);
        }
    }

    public int delete(int key) {
        if (rootID == -1) {
            return -1;
        }
        BTreeNode root = loadNodeByID(rootID);
        if (root.getKeys().size() == 1 && root.getChildrenIDs().isEmpty()) {
            addDeletedNode(root);
            setRootID(-1);
            return root.getLocations().remove(0);
        }
        return root.deleteNode(key);
    }

    public Integer search(int key) {
        if (rootID == -1) {
            return -1;
        }
        BTreeNode root = loadNodeByID(rootID);
        return root.search(key);
    }

    public int getHeight() {
        if (rootID == -1) {
            return 0;
        }
        return calculateHeight(rootID);
    }

    private int calculateHeight(int nodeID) {
        BTreeNode node = loadNodeByID(nodeID);
        if (node == null || node.getChildrenIDs().isEmpty()) {
            return 1;
        }
        return 1 + calculateHeight(node.getChildrenIDs().get(0));
    }


    public void printTree() {
        if (rootID == -1) {
            System.out.println("B-Tree is empty.");
            return;
        }

        System.out.println("Structure:");
        printTreeRecursively(rootID, 0);
        System.out.println(ColorCode.YELLOW + "Height of the B-Tree: " + ColorCode.RESET + getHeight());
    }

    private void printTreeRecursively(int nodeID, int level) {
        BTreeNode node = loadNodeByID(nodeID);
        if (node == null) {
            System.out.println("Error: Unable to load node with ID: " + nodeID);
            return;
        }

        // Indentation to represent tree levels visually
        String indent = "    ".repeat(level);

        System.out.println(indent + ColorCode.YELLOW + "- NodeID: " + ColorCode.RESET + node.getNodeID());
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
