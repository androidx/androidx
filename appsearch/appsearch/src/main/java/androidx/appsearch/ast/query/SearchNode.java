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

package androidx.appsearch.ast.query;

import androidx.annotation.NonNull;
import androidx.appsearch.app.ExperimentalAppSearchApi;
import androidx.appsearch.app.PropertyPath;
import androidx.appsearch.ast.FunctionNode;
import androidx.appsearch.ast.Node;
import androidx.appsearch.flags.FlaggedApi;
import androidx.appsearch.flags.Flags;
import androidx.core.util.Preconditions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * {@link FunctionNode} that represents the search function.
 *
 * <p>The search function is a convenience function that takes a query string and parses it
 * according to the supported query language, and can optionally take a list of property paths to
 * serve as property restricts. This means that the query
 * `search("foo bar", createList("subject", body")` is equivalent to
 * the query `(subject:foo OR body:foo) (subject:bar OR body:bar)`.
 */
@ExperimentalAppSearchApi
@FlaggedApi(Flags.FLAG_ENABLE_ABSTRACT_SYNTAX_TREES)
public final class SearchNode implements FunctionNode {
    private final List<Node> mChildren = new ArrayList<>(1);
    private final List<PropertyPath> mProperties;

    /**
     * Create a {@link SearchNode} representing the query function
     * `search(queryString, createList(listOfProperties)`.
     *
     * @param childNode The query to search for represented as a {@link Node}.
     * @param properties A list of property paths to restrict results from the query. If the list is
     *                   empty, all results from the query will be returned.
     */
    public SearchNode(@NonNull Node childNode, @NonNull List<PropertyPath> properties) {
        Preconditions.checkNotNull(childNode);
        Preconditions.checkNotNull(properties);
        for (int i = 0; i < properties.size(); i++) {
            Preconditions.checkNotNull(properties.get(i));
        }
        mChildren.add(childNode);
        mProperties = new ArrayList<>(properties);
    }

    /**
     * Create a {@link SearchNode} representing the query function `search(queryString)`.
     *
     * <p>By default, the query function search will have an empty list of restricts. The search
     * function will return all results from the query.
     *
     * @param childNode The query to search for represented as a {@link Node}.
     */
    public SearchNode(@NonNull Node childNode) {
        this(childNode, Collections.emptyList());
    }

    /**
     * Returns the name of the function represented by {@link SearchNode}.
     */
    @NonNull
    @Override
    @FunctionName
    public String getFunctionName() {
        return FUNCTION_NAME_SEARCH;
    }

    /**
     * Returns the child {@link Node} of {@link SearchNode} as a list containing
     * the only child {@link Node}.
     */
    @NonNull
    @Override
    public List<Node> getChildren() {
        return Collections.unmodifiableList(mChildren);
    }

    /**
     * Returns the child query searched for in the function.
     */
    @NonNull
    public Node getChild() {
        return mChildren.get(0);
    }

    /**
     * Returns the list of property restricts applied to the query. If the list is empty, there are
     * no property restricts, which means that `search` will return all results from the query.
     */
    @NonNull
    public List<PropertyPath> getProperties() {
        return Collections.unmodifiableList(mProperties);
    }

    /**
     * Sets the query searched for in the function.
     */
    public void setChild(@NonNull Node childNode) {
        mChildren.set(0, Preconditions.checkNotNull(childNode));
    }

    /**
     * Sets what property restricts will be applied to the query.
     */
    public void setProperties(@NonNull List<PropertyPath> properties) {
        Preconditions.checkNotNull(properties);
        for (int i = 0; i < properties.size(); i++) {
            Preconditions.checkNotNull(properties.get(i));
        }
        mProperties.clear();
        mProperties.addAll(properties);
    }

    /**
     * Add a restrict to the end of the current list of restricts {@link #mProperties}.
     */
    public void addProperty(@NonNull PropertyPath propertyPath) {
        mProperties.add(Preconditions.checkNotNull(propertyPath));
    }
}
