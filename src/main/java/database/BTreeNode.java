package database;

import java.util.ArrayList;
import java.util.List;

public class BTreeNode {
    private final int t; // Minimum degree
    private final int maxSizeOfKeys;
    private int nodeID;
    private int parentID;
    private List<Integer> keys;       // Keys in the node
    private List<Integer> locations; // Corresponding data locations
    private List<Integer> childrenIDs; // Indices of child nodes
    private BTree tree;

    public BTreeNode(BTree tree) {
        this.tree = tree;
        this.t = tree.getT();
        this.maxSizeOfKeys = 2 * t;
        this.nodeID = -1; // Assigned later
        this.parentID = -1;
        this.keys = new ArrayList<>();
        this.locations = new ArrayList<>();
        this.childrenIDs = new ArrayList<>();
    }

    public BTreeNode(BTree tree, int nodeID, int parentID, List<Integer> keys,
                     List<Integer> locations, List<Integer> childrenIDs) {
        this.tree = tree;
        this.t = tree.getT();
        this.maxSizeOfKeys = 2 * t;
        this.nodeID = nodeID;
        this.parentID = parentID;
        this.keys = keys;
        this.locations = locations;
        this.childrenIDs = childrenIDs;
    }

    public void assignNodeID() {
        this.nodeID = tree.getNextNodeID();
    }

    public void insertNonFull(int key, int location) {
        int i = keys.size() - 1;

        // If the node is a leaf, insert the key directly
        if (childrenIDs.isEmpty()) {
            keys.add(0); // Placeholder to shift elements
            locations.add(0);

            while (i >= 0 && keys.get(i) > key) {
                keys.set(i + 1, keys.get(i));
                locations.set(i + 1, locations.get(i));
                i--;
            }
            keys.set(i + 1, key);
            locations.set(i + 1, location);
            tree.addModifiedNode(this);
        } else {
            // Find the child that will have the new key
            while (i >= 0 && keys.get(i) > key) {
                i--;
            }
            i++;

            // Load the child node by its ID
            BTreeNode child = tree.loadNodeByID(childrenIDs.get(i));
            if (child.getKeys().size() == maxSizeOfKeys) {
                splitChild(i, child);
                if (keys.get(i) < key) {
                    i++;
                }
            }

            // Recursive insertion into the appropriate child
            child.insertNonFull(key, location);
            tree.writeNodeToMap(child); // Save changes to the child
            tree.addModifiedNode(child);
        }
    }

    public void splitChild(int i, BTreeNode child) {
        BTreeNode newNode = new BTreeNode(tree);
        newNode.assignNodeID();
        newNode.setParentID(nodeID);

        // Move t-1 keys and locations from the child to the new node
        for (int j = 0; j < t - 1; j++) {
            newNode.getKeys().add(child.getKeys().remove(t));
            newNode.getLocations().add(child.getLocations().remove(t));
        }

        // Move t children if the child is not a leaf
        if (!child.getChildrenIDs().isEmpty()) {
            for (int j = 0; j < t; j++) {
                newNode.getChildrenIDs().add(child.getChildrenIDs().remove(t));
            }
        }

        // Insert the middle key into this node
        keys.add(i, child.getKeys().remove(t - 1));
        locations.add(i, child.getLocations().remove(t - 1));
        childrenIDs.add(i + 1, newNode.getNodeID());

        // Save both nodes to disk
        tree.writeNodeToMap(child);
        tree.writeNodeToMap(newNode);

        tree.addModifiedNode(child);
        tree.addModifiedNode(newNode);
        tree.addModifiedNode(this);
    }

    public Integer search(int key) {
        int i = 0;

        // Find the key or determine the child to search
        while (i < keys.size() && key > keys.get(i)) {
            i++;
        }

        // If the key is found, return its location
        if (i < keys.size() && keys.get(i) == key) {
            return locations.get(i);
        }

        // If this is a leaf, the key isn't present
        if (childrenIDs.isEmpty()) {
            return -1;
        }

        // Otherwise, search in the appropriate child
        BTreeNode child = tree.loadNodeByID(childrenIDs.get(i));
        return child.search(key);
    }

    public int getNodeID() {
        return nodeID;
    }

    public int getParentID() {
        return parentID;
    }

    public void setParentID(int parentID) {
        this.parentID = parentID;
    }

    public List<Integer> getKeys() {
        return keys;
    }

    public List<Integer> getLocations() {
        return locations;
    }

    public List<Integer> getChildrenIDs() {
        return childrenIDs;
    }
}
