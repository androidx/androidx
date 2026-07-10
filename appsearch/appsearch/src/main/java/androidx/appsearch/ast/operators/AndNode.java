/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.appsearch.ast.operators;

import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.appsearch.app.ExperimentalAppSearchApi;
import androidx.appsearch.ast.Node;
import androidx.appsearch.flags.FlaggedApi;
import androidx.appsearch.flags.Flags;
import androidx.core.util.Preconditions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * {@link Node} that represents logical AND of nodes.
 */
@ExperimentalAppSearchApi
@FlaggedApi(Flags.FLAG_ENABLE_ABSTRACT_SYNTAX_TREES)
// TODO(b/384721898): Switching to JSpecify annotations changes APIs once synced to platform.
//  Do not switch unless you've checked that no APIs are affected.
@SuppressWarnings("JSpecifyNullness")
public final class AndNode implements Node {
    private List<Node> mChildren;

    /**
     * Constructor for {@link AndNode} that represents logical AND over all its child nodes.
     *
     * @param childNodes The list of {@link Node} of at least size two representing queries to be
     *                   logically ANDed over.
     */
    public AndNode(@NonNull List<Node> childNodes) {
        Preconditions.checkNotNull(childNodes);
        Preconditions.checkArgument(childNodes.size() >= 2,
                /*errorMessage=*/ "Number of nodes must be at least two.");
        mChildren = new ArrayList<>(childNodes);
    }

    /**
     * Convenience constructor for {@link AndNode} that represents logical AND over all its
     * child nodes and takes in a varargs of nodes.
     *
     * @param firstChild The first node to be ANDed over, which is required.
     * @param secondChild The second node to be ANDed over, which is required.
     * @param additionalChildren Additional nodes to be ANDed over, which are optional.
     */
    public AndNode(@NonNull Node firstChild, @NonNull Node secondChild,
            @NonNull Node... additionalChildren) {
        ArrayList<Node> childNodes = new ArrayList<>();
        childNodes.add(Preconditions.checkNotNull(firstChild));
        childNodes.add(Preconditions.checkNotNull(secondChild));
        childNodes.addAll(List.of(Preconditions.checkNotNull(additionalChildren)));
        mChildren = childNodes;
    }

    /**
     * Get the list of nodes being logically ANDed over by this node.
     */
    @Override
    public @NonNull List<Node> getChildren() {
        return Collections.unmodifiableList(mChildren);
    }

    /**
     * Returns the index of the first instance of the node, or -1 if the node does not exist.
     */
    public int getIndexOfChild(@NonNull Node node) {
        Preconditions.checkNotNull(node);
        return mChildren.indexOf(node);
    }

    /**
     * Set the nodes being logically ANDed over by this node.
     *
     * @param childNodes A list of {@link Node} of at least size two representing the nodes to be
     *                   logically ANDed over in this node.
     */
    public void setChildren(@NonNull List<Node> childNodes) {
        Preconditions.checkNotNull(childNodes);
        Preconditions.checkArgument(childNodes.size() >= 2,
                /*errorMessage=*/ "Number of nodes must be at least two.");
        mChildren = new ArrayList<>(childNodes);
    }

    /**
     * Add a child node to the end of the current list of child nodes {@link #mChildren}.
     *
     * @param childNode A {@link Node} to add to the end of the list of child nodes.
     */
    public void addChild(@NonNull Node childNode) {
        mChildren.add(Preconditions.checkNotNull(childNode));
    }

    /**
     * Replace the child node at the provided index with the provided {@link Node}.
     *
     * @param index The index at which to replace the child node in the list of child nodes. Must be
     *              in range of the size of {@link #mChildren}.
     * @param childNode The {@link Node} that is replacing the childNode at the provided index.
     */
    public void setChild(int index, @NonNull Node childNode) {
        Preconditions.checkArgumentInRange(index, /*lower=*/ 0, /*upper=*/ mChildren.size() - 1,
                /*valueName=*/ "Index");
        mChildren.set(index, Preconditions.checkNotNull(childNode));
    }

    /**
     * Removes the given {@link Node} from the list of child nodes. If multiple copies of the node
     * exist, then the first {@link Node} that matches the provided {@link Node} will be removed.
     * If the node does not exist, the list will be unchanged.
     *
     * <p>The list of child nodes must contain at least 3 nodes to perform this operation.
     *
     * @return {@code true} if the node was removed, {@code false} if the node was not removed i.e.
     * the node was not found.
     */
    public boolean removeChild(@NonNull Node node) {
        Preconditions.checkState(mChildren.size() > 2, "List of child nodes must "
                + "contain at least 3 nodes in order to remove.");
        Preconditions.checkNotNull(node);
        return mChildren.remove(node);
    }

    /**
     * Gets the string representation of {@link AndNode}.
     *
     * <p>The string representation of {@link AndNode} is the string representation of
     * {@link AndNode}'s child nodes joined with "AND", all surrounded by parentheses.
     */
    @Override
    public @NonNull String toString() {
        return "(" + TextUtils.join(" AND ", mChildren) + ")";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AndNode andNode = (AndNode) o;
        return Objects.equals(mChildren, andNode.mChildren);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(mChildren);
    }
}
