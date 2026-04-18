package cmar;

import java.util.HashMap;
import java.util.Map;

/**
 * FP-Tree node - optimized with HashMap children for fast lookup.
 */
public class FPNode {
    int item;
    int count;
    FPNode parent;
    Map<Integer, FPNode> children;
    FPNode link; // next node with same item in header table

    public FPNode(int item, int count, FPNode parent) {
        this.item = item;
        this.count = count;
        this.parent = parent;
        this.children = new HashMap<>(4); // small initial capacity
    }

    public FPNode getChild(int item) {
        return children.get(item);
    }

    public FPNode addChild(int item) {
        FPNode child = children.get(item);
        if (child != null) {
            child.count++;
            return child;
        }
        child = new FPNode(item, 1, this);
        children.put(item, child);
        return child;
    }

    public boolean isRoot() {
        return item == -1;
    }
}
