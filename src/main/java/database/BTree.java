package database;

import java.util.HashMap;
import java.util.Map;

public class BTree {
    private int rootID;
    private final int t;
    private final int maxSizeOfKeys;
    private int nodeIDCounter = 0;
    private final Map<Integer, BTreeNode> nodes;
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
            } else {
                root.insertNonFull(key, location);
                writeNodeToMap(root);
            }
        }
    }

    public Integer search(int key) {
        if (rootID == -1) {
            return null;
        }
        BTreeNode root = loadNodeByID(rootID);
        return root.search(key);
    }

}
