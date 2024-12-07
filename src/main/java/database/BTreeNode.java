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

    public boolean insertNode(int key, int location) {
        int i = keys.size() - 1;

        if (keys.contains(key)) {
            return false;
        }

        // If the node is a leaf, insert the key directly
        if (childrenIDs.isEmpty()) {
            i = keys.size() - 1;
            keys.add(0); // Placeholder to shift elements
            locations.add(0);

            while (i >= 0 && keys.get(i) > key) {
                keys.set(i + 1, keys.get(i));
                locations.set(i + 1, locations.get(i));
                i--;
            }
            keys.set(i + 1, key);
            locations.set(i + 1, location);

            // check for overflow
            if (keys.size() > maxSizeOfKeys) {
                if (!compensate()) split();
            }

            tree.writeNodeToMap(this);
            tree.addModifiedNode(this);
            return true;
        } else {
            // Find the child that will have the new key
            while (i >= 0 && keys.get(i) > key) {
                i--;
            }
            i++;

            BTreeNode child = tree.loadNodeByID(childrenIDs.get(i));
            // Recursive insertion into the appropriate child
            child.insertNode(key, location);
            tree.writeNodeToMap(child);
            tree.addModifiedNode(child);
        }
        return true;
    }

    public void split() {
        // If the node is the root, create a new root
        if (getParentID() == -1) {
            BTreeNode newRoot = new BTreeNode(tree);
            newRoot.assignNodeID();

            int mid = t;

            newRoot.getKeys().add(keys.get(mid));
            newRoot.getLocations().add(locations.get(mid));

            BTreeNode newNode = new BTreeNode(tree);
            newNode.assignNodeID();
            newNode.setParentID(newRoot.getNodeID());

            // Find the middle index (t) for splitting
            int keysSize = keys.size();

            List<Integer> newNodeKeys = new ArrayList<>(getKeys().subList(mid + 1, keysSize));
            List<Integer> newNodeLocations = new ArrayList<>(getLocations().subList(mid + 1, keysSize));

            newNode.getKeys().addAll(newNodeKeys);
            newNode.getLocations().addAll(newNodeLocations);

            getKeys().subList(mid, keysSize).clear();
            getLocations().subList(mid, keysSize).clear();
            if (!childrenIDs.isEmpty()){
                List<Integer> movedChildren = new ArrayList<>(childrenIDs.subList(mid + 1, childrenIDs.size()));
                newNode.getChildrenIDs().addAll(childrenIDs.subList(mid + 1, childrenIDs.size()));
                getChildrenIDs().subList(mid + 1, childrenIDs.size()).clear();

                for (int childID : movedChildren) {
                    BTreeNode child = tree.loadNodeByID(childID);
                    child.setParentID(newNode.getNodeID());
                    tree.writeNodeToMap(child);
                    tree.addModifiedNode(child);
                }
            }

            newRoot.getChildrenIDs().add(nodeID);
            newRoot.getChildrenIDs().add(newNode.getNodeID());

            tree.writeNodeToMap(newRoot);
            tree.writeNodeToMap(newNode);
            tree.writeNodeToMap(this);

            tree.addModifiedNode(newRoot);
            tree.addModifiedNode(newNode);
            tree.addModifiedNode(this);

            setParentID(newRoot.getNodeID());
            tree.setRootID(newRoot.getNodeID());
            return;
        }

        BTreeNode parent = tree.loadNodeByID(parentID);
        int i = parent.getChildrenIDs().indexOf(nodeID);

        // Create a new node to store the right half of the keys
        BTreeNode newNode = new BTreeNode(tree);
        newNode.assignNodeID();
        newNode.setParentID(parentID);

        // Find the middle index (t) for splitting
        int mid = t;
        int keysSize = getKeys().size();

        // Move the keys, locations and children to the new node
        List<Integer> newNodeKeys = new ArrayList<>(getKeys().subList(mid + 1, keysSize));
        List<Integer> newNodeLocations = new ArrayList<>(getLocations().subList(mid + 1, keysSize));
        newNode.getKeys().addAll(newNodeKeys);
        newNode.getLocations().addAll(newNodeLocations);

        // Clear the keys, locations, and childrenIDs from the current node
        getKeys().subList(mid + 1, keysSize).clear();
        getLocations().subList(mid + 1, keysSize).clear();

        // if the current node has children, move the children to the new node
        if (!childrenIDs.isEmpty()) {
            List<Integer> movedChildren = new ArrayList<>(childrenIDs.subList(mid + 1, childrenIDs.size()));
            newNode.getChildrenIDs().addAll(childrenIDs.subList(mid + 1, childrenIDs.size()));
            getChildrenIDs().subList(mid + 1, childrenIDs.size()).clear();

            for (int childID : movedChildren) {
                BTreeNode child = tree.loadNodeByID(childID);
                child.setParentID(newNode.getNodeID());
                tree.writeNodeToMap(child);
                tree.addModifiedNode(child);
            }
        }

        // Insert the middle key from the child node into the parent node
        parent.getKeys().add(i, getKeys().remove(mid));
        parent.getLocations().add(i, getLocations().remove(mid));
        parent.getChildrenIDs().add(i + 1, newNode.getNodeID());

        // Save both the child and newNode to the disk
        tree.writeNodeToMap(parent);
        tree.writeNodeToMap(newNode);
        tree.writeNodeToMap(this);

        // Mark the nodes as modified
        tree.addModifiedNode(parent);
        tree.addModifiedNode(newNode);
        tree.addModifiedNode(this);

        // If the parent node exceeds its maximum size after the split, we need to handle it recursively
        if (parent.getKeys().size() > maxSizeOfKeys) {
            parent.split();
        }
    }

    public boolean compensate() {
        if (parentID == -1) return false;
        BTreeNode parent = tree.loadNodeByID(parentID);
        int childIndex = parent.getChildrenIDs().indexOf(nodeID);

        BTreeNode leftSibling = childIndex > 0 ? tree.loadNodeByID(parent.getChildrenIDs().get(childIndex - 1)) : null;
        BTreeNode rightSibling = childIndex < parent.getChildrenIDs().size() - 1 ? tree.loadNodeByID(parent.getChildrenIDs().get(childIndex + 1)) : null;

        // Try to compensate with left sibling
        if (leftSibling != null && leftSibling.getKeys().size() < maxSizeOfKeys) {
            List<Integer> allKeys = new ArrayList<>(leftSibling.getKeys());
            List<Integer> allLocations = new ArrayList<>(leftSibling.getLocations());

            allKeys.add(parent.getKeys().get(childIndex - 1));  // Add the parent key
            allLocations.add(parent.getLocations().get(childIndex - 1));

            allKeys.addAll(keys); // Add the keys of the current node
            allLocations.addAll(locations);

            int mid = allKeys.size() / 2;

            leftSibling.getKeys().clear();
            leftSibling.getLocations().clear();
            leftSibling.getKeys().addAll(allKeys.subList(0, mid));
            leftSibling.getLocations().addAll(allLocations.subList(0, mid));

            keys.clear();
            locations.clear();
            keys.addAll(allKeys.subList(mid + 1, allKeys.size()));
            locations.addAll(allLocations.subList(mid + 1, allLocations.size()));

            // The parent gets the middle key
            parent.getKeys().set(childIndex - 1, allKeys.get(mid));
            parent.getLocations().set(childIndex - 1, allLocations.get(mid));

            tree.writeNodeToMap(leftSibling);
            tree.writeNodeToMap(parent);
            tree.writeNodeToMap(this);
            tree.addModifiedNode(leftSibling);
            tree.addModifiedNode(parent);
            tree.addModifiedNode(this);
            return true;
        }

        // Try to compensate with right sibling
        else if (rightSibling != null && rightSibling.getKeys().size() < maxSizeOfKeys) {
            List<Integer> allKeys = new ArrayList<>(keys);
            List<Integer> allLocations = new ArrayList<>(locations);
            allKeys.add(parent.getKeys().get(childIndex));  // Add the parent key
            allLocations.add(parent.getLocations().get(childIndex));
            allKeys.addAll(rightSibling.getKeys());
            allLocations.addAll(rightSibling.getLocations());

            int mid = allKeys.size() / 2;

            keys.clear();
            locations.clear();
            keys.addAll(allKeys.subList(0, mid));
            locations.addAll(allLocations.subList(0, mid));

            rightSibling.getKeys().clear();
            rightSibling.getLocations().clear();
            rightSibling.getKeys().addAll(allKeys.subList(mid + 1, allKeys.size()));
            rightSibling.getLocations().addAll(allLocations.subList(mid + 1, allLocations.size()));

            // The parent gets the middle key
            parent.getKeys().set(childIndex, allKeys.get(mid));
            parent.getLocations().set(childIndex, allLocations.get(mid));

            tree.writeNodeToMap(rightSibling);
            tree.writeNodeToMap(parent);
            tree.writeNodeToMap(this);
            tree.addModifiedNode(rightSibling);
            tree.addModifiedNode(parent);
            tree.addModifiedNode(this);
            return true;
        }

        return false;
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
