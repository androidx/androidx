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

import androidx.annotation.NonNull;
import androidx.appsearch.app.ExperimentalAppSearchApi;
import androidx.appsearch.app.PropertyPath;
import androidx.appsearch.ast.Node;
import androidx.appsearch.flags.FlaggedApi;
import androidx.appsearch.flags.Flags;
import androidx.core.util.Preconditions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * {@link Node} that represents a property restrict.
 *
 * <p>A property restrict is an expression in the query language that allows a querier to restrict
 * the results of a query expression to those contained in a given property path. Written as a query
 * string, this node should be equivalent to the query `property:child`, where `property` is the
 * property path to restrict results to and `child` is the query subexpression.
 *
 * <p>This node is a comparator that should correspond with HAS in the
 * <a href="https://google.aip.dev/assets/misc/ebnf-filtering.txt">
 *      Google AIP EBNF Filtering Definition</a>.
 */
@ExperimentalAppSearchApi
@FlaggedApi(Flags.FLAG_ENABLE_ABSTRACT_SYNTAX_TREES)
public class PropertyRestrictNode implements Node {
    private PropertyPath mProperty;
    private final List<Node> mChildren = new ArrayList<>(1);

    /**
     * Constructor for building a {@link PropertyRestrictNode} that represents a restriction on a
     * query subexpression by some property i.e. the query `property:subexpression`.
     *
     * @param propertyPath The property that will restrict results returned by the subexpression
     *                     in the property restrict
     * @param childNode The subexpression to be restricted in the property restrict
     */
    public PropertyRestrictNode(@NonNull PropertyPath propertyPath, @NonNull Node childNode) {
        mProperty = Preconditions.checkNotNull(propertyPath);
        mChildren.add(Preconditions.checkNotNull(childNode));
    }

    /**
     * Get the property in the property restriction (i.e. the left hand side of the property
     * restrict sign (":")).
     */
    @NonNull
    public PropertyPath getProperty() {
        return mProperty;
    }

    /**
     * Get the child {@link Node} of {@link PropertyRestrictNode} as a list containing the only
     * child {@link Node}.
     */
    @NonNull
    @Override
    public List<Node> getChildren() {
        return Collections.unmodifiableList(mChildren);
    }

    /**
     * Get the subexpression in the property restriction as a {@link Node} (i.e. the right hand side
     * of the property restrict sign (":")).
     */
    @NonNull
    public Node getChild() {
        return mChildren.get(0);
    }

    /**
     * Set the property in the property restriction (i.e. the left hand side of the property
     * restrict sign (":")).
     */
    public void setProperty(@NonNull PropertyPath propertyPath) {
        mProperty = Preconditions.checkNotNull(propertyPath);
    }

    /**
     * Set the query subexpression in the property restriction (i.e. the right hand side of the
     * property restrict sign (":")).
     */
    public void setChild(@NonNull Node childNode) {
        mChildren.set(0, Preconditions.checkNotNull(childNode));
    }
}
