import memory.DiskFile;

import java.util.ArrayList;
import java.util.List;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import memory.PageBtreeFile;

class BTreeNode {
    private final int t; // Minimum degree (defines the range for number of keys)
    private int nodeID;
    private int parentID;
    private List<Integer> keys; // Keys in this node
    private List<Integer> locations; // Corresponding data locations
    private List<BTreeNode> children; // Children of this node
    private BTree tree;

    public BTreeNode(int t, BTree tree) {
        this.t = t;
        this.nodeID = -1; // assigned later
        this.parentID = -1;
        this.keys = new ArrayList<>();
        this.locations = new ArrayList<>();
        this.children = new ArrayList<>();
        this.tree = tree;
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
        BTreeNode z = new BTreeNode(y.t, tree);
        z.assignNodeID();

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

    public List<BTreeNode> getChildren() {
        return children;
    }

    public void serializeNode(BufferedWriter writer) throws IOException {
        List<Integer> childrenIds = new ArrayList<>();
        for (BTreeNode child : children) {
            childrenIds.add(child.nodeID);
        }

        PageBtreeFile page = new PageBtreeFile(
                nodeID,
                parentID,
                new ArrayList<>(keys),
                new ArrayList<>(locations),
                childrenIds
        );

        writer.write(page.serialize());
        writer.newLine();

        // Recursively serialize all children
        for (BTreeNode child : children) {
            child.serializeNode(writer);
        }
    }
}

public class BTree {
    private BTreeNode root;
    private final int t;
    private final int maxSizeOfKeys;
    private int nodeIDCounter = 0;


    public BTree(int t) {
        this.root = null;
        this.t = t;
        this.maxSizeOfKeys = 2 * t - 1;
    }

    public int getRootID() {
        return root.getNodeID();
    }

    public int getNextNodeID() {
        return nodeIDCounter++;
    }

    public void insert(int key, int location) {
        if (root == null) {
            root = new BTreeNode(t, this);
            root.getKeys().add(key);
            root.getLocations().add(location);
            root.assignNodeID();
        } else {
            if (root.getKeys().size() == maxSizeOfKeys) {
                BTreeNode newRoot = new BTreeNode(t, this);
                newRoot.assignNodeID();
                newRoot.getChildren().add(root);
                root.setParentID(newRoot.getNodeID());
                newRoot.splitChild(0);
                int i = 0;
                if (newRoot.getKeys().get(0) < key) {
                    i++;
                }
                newRoot.getChildren().get(i).insertNonFull(key, location);
                root = newRoot;
            } else {
                root.insertNonFull(key, location);
            }
        }
    }

    public Integer search(int key) {
        return root == null ? null : root.search(key);
    }

    public void traverse() {
        if (root != null) {
            root.traverse();
        }
    }

    public void serialize(DiskFile file) {
        String filename = file.getFilename();

        if (root == null) {
            System.out.println("The tree is empty.");
            return;
        }

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filename))) {
            root.serializeNode(writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


}
