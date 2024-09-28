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
import androidx.appsearch.app.SearchSpec;
import androidx.appsearch.ast.FunctionNode;
import androidx.appsearch.flags.FlaggedApi;
import androidx.appsearch.flags.Flags;
import androidx.core.util.Preconditions;

/**
 * {@link FunctionNode} that represents the getSearchStringParameter function.
 *
 * <p>The getSearchStringParameter function retrieves the String parameter stored at the index in
 * the list provided by {@link SearchSpec#getSearchStringParameters()}.
 *
 * <p>The String parameter can be used in a query and is treated as plain text. It will be
 * segmented, normalized, and stripped of punctuation. Operators such as {@code AND} will be treated
 * as plain text while operators such as negation ("-") and property restricts (":") will be treated
 * as punctuation and removed.
 *
 * <p>So for the query `foo OR getSearchStringParameter(0)`, where getSearchStringParameter(0)
 * contains "bar AND sender:recipient", the string will be segmented into
 * <ul>
 *     <li> bar
 *     <li> AND
 *     <li> sender
 *     <li> :
 *     <li> recipient
 * </ul>
 * Then the punctuation will be removed and the remaining tokens ANDed together. This means the
 * resulting query is equivalent to the query `foo OR (bar AND and AND sender AND recipient)`.
 */
@ExperimentalAppSearchApi
@FlaggedApi(Flags.FLAG_ENABLE_ABSTRACT_SYNTAX_TREES)
public final class GetSearchStringParameterNode implements FunctionNode {
    private int mSearchStringIndex;

    /**
     * Constructor for {@link GetSearchStringParameterNode} that takes in the
     * index of the SearchString parameter provided in {@link SearchSpec#getSearchStringParameters}.
     */
    public GetSearchStringParameterNode(int searchStringIndex) {
        Preconditions.checkArgument(searchStringIndex >= 0,
                "SearchStringIndex must be non-negative.");
        mSearchStringIndex = searchStringIndex;
    }

    /**
     * Returns the name of the function represented by {@link GetSearchStringParameterNode}.
     */
    @NonNull
    @FunctionName
    @Override
    public String getFunctionName() {
        return FUNCTION_NAME_GET_SEARCH_STRING_PARAMETER;
    }

    /**
     * Returns the index of the SearchString parameter to be retrieved from
     * {@link SearchSpec#getSearchStringParameters}.
     */
    public int getSearchStringIndex() {
        return mSearchStringIndex;
    }

    /**
     * Sets the index of the SearchString parameter provided in
     * {@link SearchSpec#getSearchStringParameters} to be represented by
     * {@link GetSearchStringParameterNode}.
     */
    public void setSearchStringIndex(int searchStringIndex) {
        Preconditions.checkArgument(searchStringIndex >= 0,
                "SearchStringIndex must be non-negative.");
        mSearchStringIndex = searchStringIndex;
    }
}
