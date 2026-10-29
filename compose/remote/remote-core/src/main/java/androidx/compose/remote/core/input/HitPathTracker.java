/*
 * Copyright 2026 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.compose.remote.core.input;

import androidx.annotation.RestrictTo;
import androidx.compose.remote.core.Operation;
import androidx.compose.remote.core.RemoteContext;
import androidx.compose.remote.core.operations.layout.Component;
import androidx.compose.remote.core.operations.layout.LayoutComponent;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * Tracks the components that are under the pointers that are down.
 *
 * <p>These components form a tree: the root component, the child of the root that is under a
 * pointer, the child of that child, and so on. Two pointers share the nodes from the root down to
 * the last component that is under both of them.
 *
 * <p>The tree is stored in arrays, so that the tracker does not allocate objects during a
 * gesture. The nodes are kept in tree order: a node comes before its children, and everything
 * below a node follows it directly. Each node stores its depth, which is enough to tell where the
 * nodes below it end. For example, this tree
 *
 * <pre>
 *   root
 *     column
 *       button A   (pointer 0)
 *       button B   (pointer 1)
 * </pre>
 *
 * is stored as
 *
 * <pre>
 *   index   component   depth   pointers
 *   0       root        0       0, 1
 *   1       column      1       0, 1
 *   2       button B    2       1
 *   3       button A    2       0
 * </pre>
 *
 * <p>Going through the nodes from the first to the last visits every node before its children.
 * Going from the last to the first visits every node after its children. With one pointer, the
 * nodes are simply the path from the root to the deepest component under the pointer.
 *
 * <p>The root is always node 0, also when no pointer is down.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class HitPathTracker {

    /**
     * The largest pointer id. The pointers of a node are stored as the bits of an int, one bit for
     * each pointer id.
     */
    static final int MAX_POINTER_ID = 31;

    /** The number of nodes that fit before the arrays have to grow. */
    private static final int INITIAL_CAPACITY = 16;

    private static final int DEPTH_SHIFT = 32;
    private static final long POINTERS_MASK = 0xFFFFFFFFL;

    /**
     * The component of each node.
     *
     * <p>This holds references to the components and not their ids. Looking up a component by its
     * id boxes the id, because the document keeps its components in a {@code HashMap<Integer,
     * Component>}.
     */
    // TODO(b/586817696): consider an IntHashMap that does not box the id during the lookup, and
    //  store the component ids here.
    private Component @NonNull [] mComponents = new Component[INITIAL_CAPACITY];

    /**
     * The depth and the pointers of each node, packed into a long. The low 32 bits have one bit
     * for each pointer that is on the node: bit n is set for pointer id n. The high 32 bits are
     * the depth, which is 0 for the root.
     */
    private long @NonNull [] mNodes = new long[INITIAL_CAPACITY];

    /** The number of nodes. */
    private int mSize;

    /**
     * Creates a hit path tracker rooted at the given component.
     *
     * @param root root component of the layout tree
     */
    HitPathTracker(@NonNull Component root) {
        mComponents[0] = root;
        mNodes[0] = node(0, 0);
        mSize = 1;
    }

    /**
     * Returns the root component.
     *
     * @return root component of the layout tree
     */
    @NonNull Component getRoot() {
        return mComponents[0];
    }

    /**
     * Returns the number of nodes. There is always at least one: the root.
     *
     * @return number of nodes
     */
    int size() {
        return mSize;
    }

    /**
     * Returns the component of a node. The nodes are in tree order: a node comes before its
     * children.
     *
     * @param index index of the node, from 0 to {@link #size()} - 1
     * @return component of the node
     */
    @NonNull Component getComponent(int index) {
        return mComponents[index];
    }

    /**
     * Returns whether a pointer is on a node.
     *
     * @param index index of the node, from 0 to {@link #size()} - 1
     * @param pointerId identifier of the pointer, from 0 to {@link #MAX_POINTER_ID}
     * @return true if the pointer went down on the component of the node or on a component below
     *     it, and is not up yet
     */
    boolean hasPointer(int index, int pointerId) {
        return (pointers(mNodes[index]) & (1 << pointerId)) != 0;
    }

    /**
     * Adds a pointer hit path starting from root to the deepest hit component.
     *
     * <p>The position of the pointer must be in window coordinates: relative to the root component,
     * which is also the coordinate space of the player view. The same position is tested against
     * every component on the way down, and {@link Component#contains} compares it to the location
     * of the component in the window ({@link Component#getLocationInWindow}), which includes the
     * scroll of its parents. A position local to a component would hit the wrong children.
     *
     * @param context current remote context
     * @param pointerId identifier of the active pointer, from 0 to {@link #MAX_POINTER_ID}
     * @param x x coordinate of the pointer in window coordinates
     * @param y y coordinate of the pointer in window coordinates
     */
    public void addHitPath(@NonNull RemoteContext context, int pointerId, float x, float y) {
        if (!mComponents[0].contains(context, x, y)) {
            return;
        }
        int pointer = 1 << pointerId;
        int node = 0;
        addPointer(node, pointer);
        Component child = childAt(context, mComponents[node], x, y);
        while (child != null) {
            node = findOrAddChild(node, child);
            addPointer(node, pointer);
            child = childAt(context, mComponents[node], x, y);
        }
    }

    /**
     * Removes a pointer from all tracked hit paths, and removes the nodes that are left without a
     * pointer.
     *
     * @param pointerId identifier of the pointer to remove, from 0 to {@link #MAX_POINTER_ID}
     */
    public void removeHitPath(int pointerId) {
        int pointer = 1 << pointerId;
        // The root stays, also without a pointer.
        mNodes[0] = node(0, pointers(mNodes[0]) & ~pointer);
        // Keep the nodes that still have a pointer, in the same order. A node has every pointer
        // of the nodes below it, so a node that is kept never loses its parent.
        int size = 1;
        for (int i = 1; i < mSize; i++) {
            int pointers = pointers(mNodes[i]) & ~pointer;
            if (pointers != 0) {
                mComponents[size] = mComponents[i];
                mNodes[size] = node(depth(mNodes[i]), pointers);
                size++;
            }
        }
        // Let go of the components of the removed nodes.
        Arrays.fill(mComponents, size, mSize, null);
        mSize = size;
    }

    /**
     * Returns the child of a component that is under a point. When several children are under the
     * point, this is the last one, which is the one drawn on top.
     *
     * @param context current remote context
     * @param component component whose children are tested
     * @param x x coordinate of the point in window coordinates
     * @param y y coordinate of the point in window coordinates
     * @return child under the point, or null if there is none
     */
    private static @Nullable Component childAt(
            @NonNull RemoteContext context, @NonNull Component component, float x, float y) {
        if (component instanceof LayoutComponent) {
            ArrayList<Component> children = ((LayoutComponent) component).getChildrenComponents();
            for (int i = children.size() - 1; i >= 0; i--) {
                Component child = children.get(i);
                if (isHit(context, child, x, y)) {
                    return child;
                }
            }
        } else {
            ArrayList<Operation> operations = component.mList;
            for (int i = operations.size() - 1; i >= 0; i--) {
                Operation operation = operations.get(i);
                if (operation instanceof Component
                        && isHit(context, (Component) operation, x, y)) {
                    return (Component) operation;
                }
            }
        }
        return null;
    }

    /**
     * Returns whether a point hits a component.
     *
     * @param context current remote context
     * @param component component to test
     * @param x x coordinate of the point in window coordinates
     * @param y y coordinate of the point in window coordinates
     * @return true if the component is drawn and contains the point
     */
    private static boolean isHit(
            @NonNull RemoteContext context, @NonNull Component component, float x, float y) {
        // Same rule as Component.paint(): components that are not drawn cannot be hit. A GONE
        // component (e.g. a non-selected StateLayout pane) keeps its last layout bounds, so
        // the bounds check alone is not enough.
        return !component.isGone() && !component.isInvisible() && component.contains(context, x, y);
    }

    /**
     * Returns the child node of a node for a component. Adds the child node if the node does not
     * have one for the component.
     *
     * @param parent index of the parent node
     * @param component component of the child node
     * @return index of the child node
     */
    private int findOrAddChild(int parent, @NonNull Component component) {
        int childDepth = depth(mNodes[parent]) + 1;
        // The nodes below the parent follow it directly, and they are deeper than the parent.
        for (int i = parent + 1; i < mSize && depth(mNodes[i]) >= childDepth; i++) {
            if (depth(mNodes[i]) == childDepth && mComponents[i] == component) {
                return i;
            }
        }
        int child = parent + 1;
        insertNode(child, component, childDepth);
        return child;
    }

    /**
     * Inserts a node without pointers, and moves the nodes from that index on one place up.
     *
     * @param index index of the new node
     * @param component component of the new node
     * @param depth depth of the new node
     */
    private void insertNode(int index, @NonNull Component component, int depth) {
        if (mSize == mNodes.length) {
            // There are more nodes than ever before: make room for them.
            mComponents = Arrays.copyOf(mComponents, mSize * 2);
            mNodes = Arrays.copyOf(mNodes, mSize * 2);
        }
        System.arraycopy(mComponents, index, mComponents, index + 1, mSize - index);
        System.arraycopy(mNodes, index, mNodes, index + 1, mSize - index);
        mComponents[index] = component;
        mNodes[index] = node(depth, 0);
        mSize++;
    }

    /**
     * Adds a pointer to a node.
     *
     * @param index index of the node
     * @param pointer bit of the pointer
     */
    private void addPointer(int index, int pointer) {
        mNodes[index] = node(depth(mNodes[index]), pointers(mNodes[index]) | pointer);
    }

    /**
     * Packs the depth and the pointers of a node into a long.
     *
     * @param depth depth of the node
     * @param pointers bits of the pointers that are on the node
     * @return packed node
     */
    private static long node(int depth, int pointers) {
        return ((long) depth << DEPTH_SHIFT) | (pointers & POINTERS_MASK);
    }

    /**
     * Returns the depth of a packed node.
     *
     * @param node packed node
     * @return depth of the node
     */
    private static int depth(long node) {
        return (int) (node >>> DEPTH_SHIFT);
    }

    /**
     * Returns the pointers of a packed node.
     *
     * @param node packed node
     * @return bits of the pointers that are on the node
     */
    private static int pointers(long node) {
        return (int) node;
    }
}
