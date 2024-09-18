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

package androidx.appsearch.ast;

import androidx.annotation.NonNull;
import androidx.appsearch.app.ExperimentalAppSearchApi;
import androidx.appsearch.flags.FlaggedApi;
import androidx.appsearch.flags.Flags;
import androidx.core.util.Preconditions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * {@link Node} that stores a child node to be logically negated with a negative sign ("-")
 * or 'NOT'.
 *
 * <p>The child node stored in this node will be negated in a query search, which means search will
 * return all documents that do not match the sub-expression represented by the child node.
 * For example, if the child node is a {@link TextNode} containing "foo", the resulting node will
 * be treated as the query `-foo` or alternatively `NOT foo`.
 *
 * <p>This node should correspond to `(NOT WS | MINUS) simple` in
 * <a href="https://google.aip.dev/assets/misc/ebnf-filtering.txt">
 * Google AIP EBNF Filtering Definition</a>.
 *
 * <p>This API may change in response to feedback and additional changes.
 */
@ExperimentalAppSearchApi
@FlaggedApi(Flags.FLAG_ENABLE_ABSTRACT_SYNTAX_TREES)
public final class NegationNode implements Node{
    private final List<Node> mChildren = new ArrayList<>(1);

    /**
     * Constructor for a {@link NegationNode} that takes in a child node of any {@link Node} type.
     *
     * <p>The resulting NegationNode represents the logical negation of its child node. For example
     * if the child node represents `foo AND bar` then the resulting NegationNode represents
     * `-(foo AND bar)` or alternatively `NOT (foo AND bar)`
     *
     * <p>This constructor is NOT a copy constructor. Passing in a {@link NegationNode} will make
     * that {@link NegationNode} a child of another {@link NegationNode},
     * NOT a new {@link NegationNode} with the same child as the original {@link NegationNode}.
     *
     * @param childNode The {@link Node} representing some query to be logically negated.
     */
    public NegationNode(@NonNull Node childNode) {
        mChildren.add(Preconditions.checkNotNull(childNode));
    }

    /**
     * Retrieve the child node of this Node as a list containing the only child node.
     *
     * <p>This method will return the child node as a List of size one containing a {@link Node}
     * but could be of any type that implements {@link Node}. The caller should check what type
     * the child node is and cast it to that type accordingly.
     *
     * @return A list of size one containing a child {@link Node} representing a query that is being
     * logically negated that could be cast to a type that implements {@link Node}
     */
    @Override
    @NonNull
    public List<Node> getChildren() {
        return Collections.unmodifiableList(mChildren);
    }

    /**
     * Retrieve the child node of this Node.
     *
     * <p>This method will return the child node as a {@link Node} but could be of any type that
     * implements {@link Node}. The caller should check what type the child node is and cast
     * it to that type accordingly.
     *
     * @return The child {@link Node} representing a query that is being logically negated
     * that could be cast to a type that implements {@link Node}
     */
    @NonNull
    public Node getChild() {
        return mChildren.get(0);
    }


    /**
     * Set the child node that the {@link NegationNode} holds.
     *
     * <p>The node will be treated such that search will return everything not
     * matching the term contained in the child.
     *
     * @param child The child node that {@link NegationNode} will hold.
     */
    public void setChild(@NonNull Node child) {
        mChildren.set(0, Preconditions.checkNotNull(child));
    }
}
