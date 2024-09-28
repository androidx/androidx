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
import androidx.appsearch.flags.FlaggedApi;
import androidx.appsearch.flags.Flags;
import androidx.core.util.Preconditions;

/**
 * {@link FunctionNode} representing the `hasProperty` query function.
 *
 * <p>The `hasProperty` query function will return all documents that contain the given property
 * and have values in the given property.
 */
@ExperimentalAppSearchApi
@FlaggedApi(Flags.FLAG_ENABLE_ABSTRACT_SYNTAX_TREES)
public final class HasPropertyNode implements FunctionNode {
    private PropertyPath mProperty;

    /**
     * Constructor for a {@link HasPropertyNode} representing the query function `hasProperty`.
     *
     * @param property A {@link PropertyPath} representing the property to check whether or not
     *                 it contains a value in the document.
     */
    public HasPropertyNode(@NonNull PropertyPath property) {
        mProperty = Preconditions.checkNotNull(property);
    }

    /**
     * Returns the name of the function represented by {@link HasPropertyNode},
     * stored in the enum {@link FunctionNode#FUNCTION_NAME_HAS_PROPERTY}.
     */
    @NonNull
    @Override
    @FunctionName
    public String getFunctionName() {
        return FunctionNode.FUNCTION_NAME_HAS_PROPERTY;
    }

    /**
     * Gets the {@link PropertyPath} representing the property being checked for some value in the
     * document.
     */
    @NonNull
    public PropertyPath getProperty() {
        return mProperty;
    }

    /**
     * Sets the {@link PropertyPath} representing the property being checked for some value in the
     * document.
     */
    public void setProperty(@NonNull PropertyPath property) {
        mProperty = Preconditions.checkNotNull(property);
    }
}
