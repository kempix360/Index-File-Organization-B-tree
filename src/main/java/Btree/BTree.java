package Btree;

import java.util.ArrayList;
import java.util.List;

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
        if (root == null) {
            return -1;
        }

        return root.getNodeID();
    }

    public int getT() {
        return t;
    }

    public int getNextNodeID() {
        return nodeIDCounter++;
    }

    public void insert(int key, int location) {
        if (root == null) {
            root = new BTreeNode(this);
            root.getKeys().add(key);
            root.getLocations().add(location);
            root.assignNodeID();
        } else {
            if (root.getKeys().size() == maxSizeOfKeys) {
                BTreeNode newRoot = new BTreeNode(this);
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

    public List<BTreeNode> getAllNodes() {
        List<BTreeNode> nodes = new ArrayList<>();
        if (root != null) {
            collectNodes(root, nodes);
        }
        return nodes;
    }

    private void collectNodes(BTreeNode node, List<BTreeNode> nodes) {
        nodes.add(node);
        for (BTreeNode child : node.getChildren()) {
            collectNodes(child, nodes);
        }
    }

    public void printTree() {
        if (root == null) {
            System.out.println("The B-Tree is empty.");
            return;
        }

        System.out.println("B-Tree structure:");
        List<BTreeNode> nodes = getAllNodes();

        for (BTreeNode node : nodes) {
            System.out.println("Node ID: " + node.getNodeID() +
                    ", Parent ID: " + (node.getParentID() == -1 ? "None" : node.getParentID()));
            System.out.println("Keys: " + node.getKeys());
            System.out.println("Locations: " + node.getLocations());

            List<BTreeNode> children = node.getChildren();
            if (children.isEmpty()) {
                System.out.println("No children.");
            } else {
                List<Integer> childIDs = new ArrayList<>();
                for (BTreeNode child : children) {
                    childIDs.add(child.getNodeID());
                }
                System.out.println("Children IDs: " + childIDs);
            }
            System.out.println();
        }
    }


}
