/*
 * Copyright 2020 The Android Open Source Project
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
// @exportToFramework:skipFile()
package androidx.appsearch.app;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appsearch.exceptions.AppSearchException;

import java.util.List;
import java.util.Map;

/**
 * An interface for factories which can convert between instances of classes annotated with
 * \@{@link androidx.appsearch.annotation.Document} and instances of {@link GenericDocument}.
 *
 * @param <T> The document class type this factory converts to and from {@link GenericDocument}.
 */
public interface DocumentClassFactory<T> {
    /**
     * Returns the name of this schema type, e.g. {@code Email}.
     *
     * <p>This is the name used in queries for type restricts.
     */
    @NonNull
    String getSchemaName();

    /** Returns the schema for this document class. */
    @NonNull
    AppSearchSchema getSchema() throws AppSearchException;

    /**
     * Returns document classes that this document class depends on. This is useful so clients
     * are not required to explicitly set all dependencies.
     */
    @NonNull
    List<Class<?>> getDependencyDocumentClasses() throws AppSearchException;

    /**
     * Converts an instance of the class annotated with
     * \@{@link androidx.appsearch.annotation.Document} into a
     * {@link androidx.appsearch.app.GenericDocument}.
     */
    @NonNull
    GenericDocument toGenericDocument(@NonNull T document) throws AppSearchException;

    /**
     * Converts a {@link androidx.appsearch.app.GenericDocument} into an instance of the document
     * class. For nested document properties, this method should pass {@code documentClassMap} down
     * to the nested calls of {@link GenericDocument#toDocumentClass(Class, Map)}.
     */
    @NonNull
    T fromGenericDocument(@NonNull GenericDocument genericDoc,
            @Nullable Map<String, List<String>> documentClassMap) throws AppSearchException;
}
