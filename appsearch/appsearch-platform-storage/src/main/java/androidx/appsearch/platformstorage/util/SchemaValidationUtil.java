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

package androidx.appsearch.platformstorage.util;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.appsearch.app.AppSearchSchema;
import androidx.appsearch.app.AppSearchSchema.DocumentPropertyConfig;
import androidx.appsearch.app.AppSearchSchema.LongPropertyConfig;
import androidx.appsearch.app.AppSearchSchema.PropertyConfig;
import androidx.appsearch.app.AppSearchSchema.StringPropertyConfig;
import androidx.appsearch.exceptions.IllegalSchemaException;
import androidx.collection.ArrayMap;
import androidx.collection.ArraySet;

import java.util.Map;
import java.util.Set;

/**
 * Utilities for schema validation.
 *
 * @exportToFramework:hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class SchemaValidationUtil {
    private SchemaValidationUtil() {
    }

    /**
     * Checks that the set of AppSearch schemas form valid schema-type definitions, and do not
     * exceed the maximum number of indexed properties allowed.
     *
     * @param appSearchSchemas   Set of AppSearch Schemas for a
     *                           {@link androidx.appsearch.app.SetSchemaRequest}.
     * @param maxSectionsAllowed The maximum number of sections allowed per AppSearch schema.
     * @throws IllegalSchemaException if any schema in the set is invalid. A schema is invalid if
     *                                it contains an invalid cycle, has an undefined document
     *                                property config, or exceeds the
     *                                maximum number of sections allowed.
     */
    public static void checkSchemasAreValidOrThrow(@NonNull Set<AppSearchSchema> appSearchSchemas,
            int maxSectionsAllowed) throws IllegalSchemaException {
        Map<String, AppSearchSchema> knownSchemas = new ArrayMap<>();
        for (AppSearchSchema schema : appSearchSchemas) {
            knownSchemas.put(schema.getSchemaType(), schema);
        }
        Map<String, Integer> cachedNumSectionsMap = new ArrayMap<>();
        for (AppSearchSchema schema : appSearchSchemas) {
            // Check that the number of sections is below the max allowed
            int numSections = getNumSectionsInSchemaOrThrow(schema, knownSchemas,
                    cachedNumSectionsMap, new ArraySet<>());
            if (numSections > maxSectionsAllowed) {
                throw new IllegalSchemaException(
                        "Too many properties to be indexed, max " + "number of properties allowed: "
                                + maxSectionsAllowed);
            }
        }
    }

    /**
     * Returns the number of indexes sections in a given AppSearch schema.
     *
     * @param schema                    The AppSearch schema to get the number of sections for.
     * @param knownSchemas              Map of known schema-type strings to their corresponding
     *                                  schemas.
     * @param cachedNumSectionsInSchema Map of the cached number of sections in schemas which have
     *                                  already been expanded.
     * @param visitedSchemaTypes        Set of schemas that have already been expanded as parents
     *                                  of the current schema.
     * @throws IllegalSchemaException if the schema contains an invalid cycle, or contains a
     *                                DocumentPropertyConfig where the config's schema type is
     *                                unknown.
     */
    private static int getNumSectionsInSchemaOrThrow(@NonNull AppSearchSchema schema,
            @NonNull Map<String, AppSearchSchema> knownSchemas,
            @NonNull Map<String, Integer> cachedNumSectionsInSchema,
            @NonNull Set<String> visitedSchemaTypes)
            throws IllegalSchemaException {
        String schemaType = schema.getSchemaType();
        if (visitedSchemaTypes.contains(schemaType)) {
            // We've hit an illegal cycle where all DocumentPropertyConfigs set
            // shouldIndexNestedProperties = true.
            throw new IllegalSchemaException(
                    "Invalid cycle detected in schema type configs. '" + schemaType
                            + "' references itself.");
        }
        if (cachedNumSectionsInSchema.containsKey(schemaType)) {
            // We've already calculated and cached the number of sections in this AppSearch schema,
            // just return this value.
            return cachedNumSectionsInSchema.get(schemaType);
        }

        visitedSchemaTypes.add(schemaType);
        int numSections = 0;
        for (PropertyConfig property : schema.getProperties()) {
            if (property.getDataType() == PropertyConfig.DATA_TYPE_DOCUMENT) {
                DocumentPropertyConfig documentProperty = (DocumentPropertyConfig) property;
                String docPropertySchemaType = documentProperty.getSchemaType();
                if (!knownSchemas.containsKey(docPropertySchemaType)) {
                    // The schema type that this document property config is referring to
                    // does not exist in the provided schemas
                    throw new IllegalSchemaException(
                            "Undefined schema type: " + docPropertySchemaType);
                }
                if (!documentProperty.shouldIndexNestedProperties()) {
                    numSections += documentProperty.getIndexableNestedProperties().size();
                } else {
                    numSections += getNumSectionsInSchemaOrThrow(
                            knownSchemas.get(docPropertySchemaType), knownSchemas,
                            cachedNumSectionsInSchema, visitedSchemaTypes);
                }
            } else {
                numSections += isPropertyIndexable(property) ? 1 : 0;
            }
        }
        visitedSchemaTypes.remove(schemaType);
        cachedNumSectionsInSchema.put(schemaType, numSections);
        return numSections;
    }

    private static boolean isPropertyIndexable(PropertyConfig propertyConfig) {
        switch (propertyConfig.getDataType()) {
            case PropertyConfig.DATA_TYPE_STRING:
                return ((StringPropertyConfig) propertyConfig).getIndexingType()
                        != StringPropertyConfig.INDEXING_TYPE_NONE;
            case PropertyConfig.DATA_TYPE_LONG:
                return ((LongPropertyConfig) propertyConfig).getIndexingType()
                        != LongPropertyConfig.INDEXING_TYPE_NONE;
            case PropertyConfig.DATA_TYPE_DOCUMENT:
                DocumentPropertyConfig documentProperty = (DocumentPropertyConfig) propertyConfig;
                return documentProperty.shouldIndexNestedProperties()
                        || !documentProperty.getIndexableNestedProperties().isEmpty();
            case PropertyConfig.DATA_TYPE_DOUBLE:
                // fallthrough
            case PropertyConfig.DATA_TYPE_BOOLEAN:
                // fallthrough
            case PropertyConfig.DATA_TYPE_BYTES:
                // fallthrough
            default:
                return false;
        }
    }
}
