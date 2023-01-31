/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.appsearch.util;

import androidx.annotation.NonNull;
import androidx.appsearch.app.GenericDocument;
import androidx.core.util.Preconditions;

/**
 * A util class with methods for working with document ids.
 */
public class DocumentIdUtil {
    private DocumentIdUtil() {}

    /**
     * A delimiter between the namespace and the document id.
     */
    private static final String NAMESPACE_DELIMITER = "#";

    /**
     * In regex, 4 backslashes in Java represent a single backslash in Regex. This will escape
     * the namespace delimiter.
     */
    private static final String NAMESPACE_DELIMITER_REPLACEMENT_REGEX = "\\\\#";

    /**
     * Generates a qualified id based on package, database, and a {@link GenericDocument}.
     *
     * @param packageName The package the document belongs to.
     * @param databaseName The database containing the document.
     * @param document The document to generate a qualified id for.
     * @return the qualified id of a document.
     *
     * @see #createQualifiedId(String, String, String, String)
     */
    @NonNull
    public static String createQualifiedId(@NonNull String packageName,
            @NonNull String databaseName, @NonNull GenericDocument document) {
        return createQualifiedId(packageName, databaseName, document.getNamespace(),
                document.getId());
    }

    /**
     * Generates a qualified id based on package, database, namespace, and doc id.
     *
     * <p> A qualified id is a String referring to the combined package name, database name,
     * namespace, and id of the document. It is useful for linking one document to another in order
     * to perform a join operation.
     *
     * @param packageName The package the document belongs to.
     * @param databaseName The database containing the document.
     * @param namespace The namespace of the document.
     * @param id The id of the document.
     * @return the qualified id of a document
     */
     // TODO(b/256022027): Add @link to QUALIFIED_ID and JoinSpec
    @NonNull
    public static String createQualifiedId(@NonNull String packageName,
            @NonNull String databaseName, @NonNull String namespace, @NonNull String id) {
        Preconditions.checkNotNull(packageName);
        Preconditions.checkNotNull(databaseName);
        Preconditions.checkNotNull(namespace);
        Preconditions.checkNotNull(id);

        StringBuilder qualifiedId = new StringBuilder(escapeNsDelimiters(packageName));

        qualifiedId.append('$')
                .append(escapeNsDelimiters(databaseName))
                .append('/')
                .append(escapeNsDelimiters(namespace))
                .append(NAMESPACE_DELIMITER)
                .append(escapeNsDelimiters(id));
        return qualifiedId.toString();
    }

    /**
     * Escapes both the namespace delimiter and backslashes.
     *
     * <p> For example, say the raw namespace contains ...\#... . if we only escape the namespace
     * delimiter, we would get ...\\#..., which would appear to be a delimiter, and split the
     * namespace in two. We need to escape the backslash as well, resulting in ...\\\#..., which is
     * not a delimiter, keeping the namespace together.
     *
     * @param original The String to escape
     * @return An escaped string
     */
    private static String escapeNsDelimiters(@NonNull String original) {
        // Four backslashes represent a single backslash in regex.
        return original
                .replaceAll("\\\\", "\\\\\\\\")
                .replaceAll(NAMESPACE_DELIMITER, NAMESPACE_DELIMITER_REPLACEMENT_REGEX);
    }
}
