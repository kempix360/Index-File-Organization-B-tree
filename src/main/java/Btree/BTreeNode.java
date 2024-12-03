package Btree;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class BTreeNode {
    private final int t; // Minimum degree (defines the range for number of keys)
    private int nodeID;
    private int parentID;
    private List<Integer> keys; // Keys in this node
    private List<Integer> locations; // Corresponding data locations
    private List<Integer> childrenIDs;
    private List<BTreeNode> children; // Children of this node
    private BTree tree;

    public BTreeNode(BTree tree) {
        this.tree = tree;
        this.t = tree.getT();
        this.nodeID = -1; // assigned later
        this.parentID = -1;
        this.keys = new ArrayList<>();
        this.locations = new ArrayList<>();
        this.children = new ArrayList<>();
    }

    public BTreeNode (BTree tree, int nodeID, int parentID, List<Integer> keys,
                      List<Integer> locations, List<Integer> childrenIDs) {
        this.tree = tree;
        this.t = tree.getT();
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
        if (children.isEmpty()) {
            keys.add(0);
            locations.add(0);

            while (i >= 0 && keys.get(i) > key) {
                keys.set(i + 1, keys.get(i));
                locations.set(i + 1, locations.get(i));
                i--;
            }
            keys.set(i + 1, key);
            locations.set(i + 1, location);
        } else {
            // Find the child that will have the new key
            while (i >= 0 && keys.get(i) > key) {
                i--;
            }
            i++;

            // If the child is full, split it
            if (children.get(i).keys.size() == 2 * t - 1) {
                splitChild(i);
                if (keys.get(i) < key) {
                    i++;
                }
            }
            children.get(i).insertNonFull(key, location);
        }
    }

    public void splitChild(int i) {
        BTreeNode y = children.get(i);
        BTreeNode z = new BTreeNode(tree);
        z.assignNodeID();
        z.setParentID(nodeID);

        // Move t-1 keys and locations to the new node
        for (int j = 0; j < t - 1; j++) {
            z.keys.add(y.keys.remove(t));
            z.locations.add(y.locations.remove(t));
        }

        // If not a leaf, move t children to the new node
        if (!y.children.isEmpty()) {
            for (int j = 0; j < t; j++) {
                z.children.add(y.children.remove(t));
            }
        }

        // Insert the middle key into this node
        keys.add(i, y.keys.remove(t - 1));
        locations.add(i, y.locations.remove(t - 1));
        children.add(i + 1, z);
    }

    public void traverse() {
        int i;
        for (i = 0; i < keys.size(); i++) {
            if (!children.isEmpty()) {
                children.get(i).traverse();
            }
            System.out.println("Key: " + keys.get(i) + ", Location: " + locations.get(i));
        }

        if (!children.isEmpty()) {
            children.get(i).traverse();
        }
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
        if (children.isEmpty()) {
            return null;
        }

        // Otherwise, search in the appropriate child
        return children.get(i).search(key);
    }

    public int getNodeID() {
        return nodeID;
    }

    public void setNodeID(int nodeID) {
        this.nodeID = nodeID;
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

    public List<BTreeNode> getChildren() {
        return children;
    }
}
