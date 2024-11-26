import java.util.ArrayList;
import java.util.List;

class BTreeNode {
    int t; // Minimum degree (defines the range for number of keys)
    List<Integer> keys;
    List<BTreeNode> children;
    boolean isLeaf;

    public BTreeNode(int t, boolean isLeaf) {
        this.t = t;
        this.isLeaf = isLeaf;
        this.keys = new ArrayList<>();
        this.children = new ArrayList<>();
    }

    public void insertNonFull(int k) {
        int i = keys.size() - 1;
        if (isLeaf) {
            keys.add(0);
            while (i >= 0 && keys.get(i) > k) {
                keys.set(i + 1, keys.get(i));
                i--;
            }
            keys.set(i + 1, k);
        } else {
            while (i >= 0 && keys.get(i) > k) {
                i--;
            }
            i++;
            if (children.get(i).keys.size() == 2 * t - 1) {
                splitChild(i, children.get(i));
                if (keys.get(i) < k) {
                    i++;
                }
            }
            children.get(i).insertNonFull(k);
        }
    }

    public void splitChild(int i, BTreeNode y) {
        BTreeNode z = new BTreeNode(y.t, y.isLeaf);
        for (int j = 0; j < t - 1; j++) {
            z.keys.add(y.keys.remove(t));
        }
        if (!y.isLeaf) {
            for (int j = 0; j < t; j++) {
                z.children.add(y.children.remove(t));
            }
        }
        children.add(i + 1, z);
        keys.add(i, y.keys.remove(t - 1));
    }

    public void traverse() {
        int i;
        for (i = 0; i < keys.size(); i++) {
            if (!isLeaf) {
                children.get(i).traverse();
            }
            System.out.print(keys.get(i) + " ");
        }
        if (!isLeaf) {
            children.get(i).traverse();
        }
    }

    public BTreeNode search(int k) {
        int i = 0;
        while (i < keys.size() && k > keys.get(i)) {
            i++;
        }
        if (i < keys.size() && keys.get(i) == k) {
            return this;
        }
        if (isLeaf) {
            return null;
        }
        return children.get(i).search(k);
    }

    public void deleteFromNode(int k) {
        int idx = findKey(k);

        if (idx < keys.size() && keys.get(idx) == k) {
            // Key is in this node
            if (isLeaf) {
                // Case 1: Key is in a leaf node
                keys.remove(idx);
            } else {
                // Case 2: Key is in an internal node
                deleteFromInternalNode(idx);
            }
        } else {
            // Key is not in this node
            if (isLeaf) {
                System.out.println("The key " + k + " is not present in the tree.");
                return;
            }

            boolean lastChild = (idx == keys.size());
            if (children.get(idx).keys.size() < t) {
                fill(idx);
            }

            if (lastChild && idx > keys.size()) {
                children.get(idx - 1).deleteFromNode(k);
            } else {
                children.get(idx).deleteFromNode(k);
            }
        }
    }

    private void deleteFromInternalNode(int idx) {
        int k = keys.get(idx);

        if (children.get(idx).keys.size() >= t) {
            // Case 2a: Predecessor is large enough
            int pred = getPredecessor(idx);
            keys.set(idx, pred);
            children.get(idx).deleteFromNode(pred);
        } else if (children.get(idx + 1).keys.size() >= t) {
            // Case 2b: Successor is large enough
            int succ = getSuccessor(idx);
            keys.set(idx, succ);
            children.get(idx + 1).deleteFromNode(succ);
        } else {
            // Case 2c: Merge children
            merge(idx);
            children.get(idx).deleteFromNode(k);
        }
    }

    private int getPredecessor(int idx) {
        BTreeNode current = children.get(idx);
        while (!current.isLeaf) {
            current = current.children.get(current.keys.size());
        }
        return current.keys.get(current.keys.size() - 1);
    }

    private int getSuccessor(int idx) {
        BTreeNode current = children.get(idx + 1);
        while (!current.isLeaf) {
            current = current.children.get(0);
        }
        return current.keys.get(0);
    }

    private void fill(int idx) {
        if (idx != 0 && children.get(idx - 1).keys.size() >= t) {
            borrowFromPrev(idx);
        } else if (idx != keys.size() && children.get(idx + 1).keys.size() >= t) {
            borrowFromNext(idx);
        } else {
            if (idx != keys.size()) {
                merge(idx);
            } else {
                merge(idx - 1);
            }
        }
    }

    private void borrowFromPrev(int idx) {
        BTreeNode child = children.get(idx);
        BTreeNode sibling = children.get(idx - 1);

        child.keys.add(0, keys.get(idx - 1));
        if (!child.isLeaf) {
            child.children.add(0, sibling.children.remove(sibling.keys.size()));
        }
        keys.set(idx - 1, sibling.keys.remove(sibling.keys.size() - 1));
    }

    private void borrowFromNext(int idx) {
        BTreeNode child = children.get(idx);
        BTreeNode sibling = children.get(idx + 1);

        child.keys.add(keys.get(idx));
        if (!child.isLeaf) {
            child.children.add(sibling.children.remove(0));
        }
        keys.set(idx, sibling.keys.remove(0));
    }

    private void merge(int idx) {
        BTreeNode child = children.get(idx);
        BTreeNode sibling = children.get(idx + 1);

        child.keys.add(keys.remove(idx));
        child.keys.addAll(sibling.keys);
        if (!child.isLeaf) {
            child.children.addAll(sibling.children);
        }
        children.remove(idx + 1);
    }

    private int findKey(int k) {
        int idx = 0;
        while (idx < keys.size() && keys.get(idx) < k) {
            idx++;
        }
        return idx;
    }
}


public class BTree {
    private BTreeNode root;
    private final int t;

    private BTree(int t) {
        this.root = null;
        this.t = t;
    }

    private void traverse() {
        if (root != null) {
            root.traverse();
        }
    }

    private BTreeNode search(int k) {
        return root == null ? null : root.search(k);
    }

    private void insert(int k) {
        // 1. case
        if (root == null) {
            root = new BTreeNode(t, true);
            root.keys.add(k);
        } else {
            // 2. case
            if (root.keys.size() == 2 * t - 1) {
                BTreeNode s = new BTreeNode(t, false);
                s.children.add(root);
                s.splitChild(0, root);
                int i = 0;
                if (s.keys.get(0) < k) {
                    i++;
                }
                s.children.get(i).insertNonFull(k);
                root = s;
            } // 3. case
            else {
                root.insertNonFull(k);
            }
        }
    }

    private void delete(int k) {
        if (root == null) {
            System.out.println("The tree is empty.");
            return;
        }

        root.deleteFromNode(k);

        if (root.keys.size() == 0) {
            // Shrink the height of the tree
            root = root.isLeaf ? null : root.children.get(0);
        }
    }

    private void reorganize() {
        System.out.println("Reorganizing the tree...");

    }
}
