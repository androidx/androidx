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

import androidx.appsearch.app.GenericDocument;
import androidx.appsearch.app.JoinSpec;
import androidx.core.util.Preconditions;

import org.jspecify.annotations.NonNull;

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
     * Replacement for the namespace delimiter.
     *
     * <p>We are using literal replace, so this is the literal replacement String, not a regex. We
     * want to replace "#" with "\\#".
     */
    private static final String NAMESPACE_DELIMITER_REPLACEMENT = "\\#";

    /**
     * Replacement for backslash.
     *
     * <p>We are using literal replace, so this is the literal replacement String, not a regex. We
     * want to replace "\" with "\\".
     */
    private static final String BACKSLASH_REPLACEMENT = "\\\\";

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
    public static @NonNull String createQualifiedId(@NonNull String packageName,
            @NonNull String databaseName, @NonNull GenericDocument document) {
        return createQualifiedId(packageName, databaseName, document.getNamespace(),
                document.getId());
    }

    /**
     * Generates a qualified id based on package, database, namespace, and doc id.
     *
     * <p> A qualified id is a String referring to the combined package name, database name,
     * namespace, and id of the document. It is useful for linking one document to another in order
     * to perform a join operation with {@link JoinSpec}.
     *
     * @param packageName The package the document belongs to.
     * @param databaseName The database containing the document.
     * @param namespace The namespace of the document.
     * @param id The id of the document.
     * @return the qualified id of a document
     *
     * @see JoinSpec
     * @see JoinSpec#QUALIFIED_ID
     */
    public static @NonNull String createQualifiedId(@NonNull String packageName,
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
        StringBuilder escapedString = null;
        for (int i = 0; i < original.length(); i++) {
            char currentChar = original.charAt(i);
            if (currentChar == '\\') {
                if (escapedString == null) {
                    escapedString = new StringBuilder(original.substring(0, i));
                }
                escapedString.append(BACKSLASH_REPLACEMENT);
            } else if (currentChar == '#') {
                if (escapedString == null) {
                    escapedString = new StringBuilder(original.substring(0, i));
                }
                escapedString.append(NAMESPACE_DELIMITER_REPLACEMENT);
            } else if (escapedString != null) {
                escapedString.append(currentChar);
            }
        }
        return escapedString == null ? original : escapedString.toString();
    }
}
