/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.wear.protolayout.renderer.dynamicdata;

import static androidx.core.util.Preconditions.checkNotNull;
import static androidx.wear.protolayout.renderer.common.ProtoLayoutDiffer.FIRST_CHILD_INDEX;
import static androidx.wear.protolayout.renderer.common.ProtoLayoutDiffer.createNodePosId;
import static androidx.wear.protolayout.renderer.common.ProtoLayoutDiffer.getParentNodePosId;

import android.util.ArrayMap;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.wear.protolayout.renderer.dynamicdata.PositionIdTree.TreeNode;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * A pseudo-tree structure for Layout nodes with Position Id. Note that the relation of each two
 * nodes can be discovered through their position id.
 *
 * <p>NOTE: This class relies on strict ordering of the posIds. It's up to the caller to make sure
 * there is never a missing posId between two sibling nodes.
 *
 * <p>This class is not thread-safe.
 */
final class PositionIdTree<T extends TreeNode> {

    /** Interface for nodes stored in this tree. */
    interface TreeNode {
        /** Will be called after a node is removed from the tree. */
        void destroy();
    }

    @NonNull private final Map<String, T> mPosIdToTreeNode = new ArrayMap<>();

    /** Calls {@code action} on all of the tree nodes. */
    void forEach(Consumer<T> action) {
        mPosIdToTreeNode.values().forEach(action);
    }

    /** Removes all of the nodes in the tree and calls their {@link TreeNode#destroy()}. */
    void clear() {
        mPosIdToTreeNode.values().forEach(TreeNode::destroy);
        mPosIdToTreeNode.clear();
    }

    /**
     * Removes all of the nodes in a subtree under the node with {@code posId}. This also calls the
     * {@link TreeNode#destroy()} on all of the removed node. Note that the {@code posId} node won't
     * be removed.
     */
    void removeChildNodesFor(@NonNull String posId) {
        removeChildNodesFor(posId, /* removeRoot= */ false);
    }

    private void removeChildNodesFor(@NonNull String posId, boolean removeRoot) {
        for (int childIndex = FIRST_CHILD_INDEX; ; childIndex++) {
            String possibleChildPosId = createNodePosId(posId, childIndex);
            if (!mPosIdToTreeNode.containsKey(possibleChildPosId)) {
                break;
            }
            removeChildNodesFor(possibleChildPosId, /* removeRoot= */ true);
        }
        if (removeRoot) {
            checkNotNull(mPosIdToTreeNode.remove(posId)).destroy();
        }
    }

    /**
     * Adds the {@code newNode} to the tree. If the tree already contains a node at that position,
     * the old node will be removed and will be destroyed.
     */
    void addOrReplace(@NonNull String posId, @NonNull T newNode) {
        T oldNode = mPosIdToTreeNode.put(posId, newNode);
        if (oldNode != null) {
            oldNode.destroy();
        }
    }

    /** Returns the node matching the {@code predicate} or an null if there is no match. */
    @Nullable
    T findFirst(@NonNull Predicate<? super T> predicate) {
        return mPosIdToTreeNode.values().stream().filter(predicate).findFirst().orElse(null);
    }

    /** Returns the node with {@code posId} or null if it doesn't exist. */
    @Nullable
    T get(String posId) {
        return mPosIdToTreeNode.get(posId);
    }

    /**
     * Returns all of the ancestors of the node with {@code posId} matching the {@code predicate}.
     */
    @NonNull
    List<T> findAncestorsFor(@NonNull String posId, @NonNull Predicate<? super T> predicate) {
        List<T> result = new ArrayList<>();
        while (true) {
            String parentPosId = getParentNodePosId(posId);
            if (parentPosId == null) {
                break;
            }
            T value = mPosIdToTreeNode.get(parentPosId);
            if (value != null && predicate.test(value)) {
                result.add(value);
            }
            posId = parentPosId;
        }
        return result;
    }

    /** Returns all of the nodes in a subtree under the node with {@code posId}. */
    @NonNull
    List<T> findChildrenFor(@NonNull String posId) {
        return findChildrenFor(posId, node -> true);
    }

    /**
     * Returns all of the nodes in a subtree under the node with {@code posId} matching the {@code
     * predicate}.
     */
    @NonNull
    List<T> findChildrenFor(@NonNull String posId, @NonNull Predicate<? super T> predicate) {
        List<T> result = new ArrayList<>();
        addChildrenFor(posId, predicate, result);
        return result;
    }

    private void addChildrenFor(
            @NonNull String posId,
            @NonNull Predicate<? super T> predicate,
            @NonNull List<T> result) {
        for (int childIndex = FIRST_CHILD_INDEX; ; childIndex++) {
            String possibleChildPosId = createNodePosId(posId, childIndex);
            if (!mPosIdToTreeNode.containsKey(possibleChildPosId)) {
                break;
            }
            T value = mPosIdToTreeNode.get(possibleChildPosId);
            if (value != null && predicate.test(value)) {
                result.add(value);
            }
            addChildrenFor(possibleChildPosId, predicate, result);
        }
    }

    /** Returns all of the current tree nodes. This is intended to be used only in tests. */
    @VisibleForTesting
    @NonNull
    Collection<T> getAllNodes() {
        return Collections.unmodifiableCollection(mPosIdToTreeNode.values());
    }
}
