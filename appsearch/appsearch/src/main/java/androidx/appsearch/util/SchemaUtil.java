/*
 * Copyright 2025 The Android Open Source Project
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

import androidx.annotation.RestrictTo;
import androidx.appsearch.app.AppSearchSchema;
import androidx.appsearch.app.PropertyPath;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A util class with methods for working with {@link AppSearchSchema}s.
 *
 * @exportToFramework:hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class SchemaUtil {
    private SchemaUtil() {}

    /**
     * The recursive logic to validate each segment of the given property path is exists and match
     * the given target schema type.
     *
     * @param schemas          The full schema map to look up nested config.
     * @param currentSchema    The schema currently being inspected (changes in recursion).
     * @param targetSchemaType The schema type of the target nested document that this property path
     *                         should be.
     * @param propertyPath     The full path object.
     * @param segmentIndex     The index of the current path segment to check.
     * @return true if the path is valid from this segment onwards.
     */
    public static boolean checkPathRecursive(@NonNull Map<String, AppSearchSchema> schemas,
            @Nullable AppSearchSchema currentSchema,
            @NonNull String targetSchemaType,
            @NonNull PropertyPath propertyPath,
            int segmentIndex) {

        // The required schema is not defined or cannot be found.
        if (currentSchema == null) {
            return false;
        }

        // All segments have been checked.
        if (segmentIndex == propertyPath.size()) {
            return currentSchema.getSchemaType().equals(targetSchemaType);
        }

        PropertyPath.PathSegment currentSegment = propertyPath.get(segmentIndex);
        // The PropertyPath class conveniently gives us the clean name, ignoring any array index.
        String propertyName = currentSegment.getPropertyName();

        // 1. Look up the property config by name in the current schema.
        AppSearchSchema.PropertyConfig matchingConfig = null;
        List<AppSearchSchema.PropertyConfig> properties = currentSchema.getProperties();
        for (int i = 0; i < properties.size(); i++) {
            AppSearchSchema.PropertyConfig config = properties.get(i);
            if (config.getName().equals(propertyName)) {
                matchingConfig = config;
                break;
            }
        }

        // 2. Check if the property segment exists.
        if (matchingConfig == null) {
            return false; // Segment not found in the current schema.
        }

        // 3. If the property MUST be a nested DOCUMENT.
        if (matchingConfig.getDataType() != AppSearchSchema.PropertyConfig.DATA_TYPE_DOCUMENT) {
            // Path continues, but current property is a leaf type (e.g., STRING), so the path is
            // invalid (too long).
            return false;
        }

        // 4. Recursively check the child schema.
        AppSearchSchema.DocumentPropertyConfig docConfig =
                (AppSearchSchema.DocumentPropertyConfig) matchingConfig;
        String childSchemaType = docConfig.getSchemaType();

        AppSearchSchema childSchema = schemas.get(childSchemaType);

        // Proceed to the next segment with the child schema.
        return checkPathRecursive(schemas, childSchema, targetSchemaType, propertyPath,
                segmentIndex + 1);
    }

    /**
     * Verifies that all specified property paths for a set of schemas point to a property
     * of the required schema type.
     *
     * <p>This method iterates through a map of schema names and their corresponding required
     * property paths. For each property path, it recursively checks if the terminal
     * property type matches {@code expectedSchemaType}.
     *
     * @param schemas                      A map of schema type names to the {@link AppSearchSchema}
     *                                     objects being verified. This map must contain all schemas
     *                                     referenced in {@code expectedSchemasPropertyPaths}.
     * @param expectedSchemasPropertyPaths A map where the key is the schema type, and the value is
     *                                     a Set of property paths within that schema that must
     *                                     adhere to the {@code expectedSchemaType}.
     * @param expectedSchemaType           The required schema type that the properties pointed to
     *                                     by the paths must match.
     * @throws IllegalArgumentException    If any property path does not exist or does not resolve
     *                                     to a property of the required {@code expectedSchemaType}.
     */
    public static void verifyPropertyPathSchemaTypes(
            @NonNull Map<String, AppSearchSchema> schemas,
            @NonNull Map<String, Set<String>> expectedSchemasPropertyPaths,
            @NonNull String expectedSchemaType)
            throws IllegalArgumentException {
        for (Map.Entry<String, Set<String>> entry : expectedSchemasPropertyPaths.entrySet()) {
            AppSearchSchema currentSchema = schemas.get(entry.getKey());
            for (String propertyPath : entry.getValue()) {
                if (!checkPathRecursive(schemas, currentSchema, expectedSchemaType,
                        new PropertyPath(propertyPath), /*segmentIndex=*/0)) {
                    throw new IllegalArgumentException("The property path of: " + propertyPath
                            + " is not the required property type: " + expectedSchemaType);
                }
            }
        }
    }
}
