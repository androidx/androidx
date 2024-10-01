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
 * {@link FunctionNode} representing the `propertyDefined` query function.
 *
 * <p>The `propertyDefined` query function will return all documents of types that define the given
 * property. This will include documents that do not have the property itself, so long as that
 * property is a part of the document's schema.
 *
 * <p>If you need to restrict to documents that have >=1 value(s) populated for that property, see
 * {@link HasPropertyNode}.
 */
@ExperimentalAppSearchApi
@FlaggedApi(Flags.FLAG_ENABLE_ABSTRACT_SYNTAX_TREES)
public final class PropertyDefinedNode implements FunctionNode {
    private PropertyPath mProperty;

    /**
     * Constructor for a {@link PropertyDefinedNode} representing the query function
     * `propertyDefined` that takes in a {@link PropertyPath}.
     */
    public PropertyDefinedNode(@NonNull PropertyPath property) {
        mProperty = Preconditions.checkNotNull(property);
    }

    /**
     * Returns the name of the function represented by {@link PropertyDefinedNode}.
     */
    @NonNull
    @Override
    @FunctionName
    public String getFunctionName() {
        return FUNCTION_NAME_PROPERTY_DEFINED;
    }

    /**
     * Returns the {@link PropertyDefinedNode} representing the property being checked for in the
     * document.
     */
    @NonNull
    public PropertyPath getProperty() {
        return mProperty;
    }

    /**
     * Sets the property being checked for in the document, as represented by
     * {@link PropertyDefinedNode}.
     */
    public void setProperty(@NonNull PropertyPath property) {
        mProperty = Preconditions.checkNotNull(property);
    }
}
