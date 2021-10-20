/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.appsearch.localstorage;

import static androidx.appsearch.localstorage.util.PrefixUtil.createPrefix;

import static com.google.common.truth.Truth.assertThat;

import androidx.annotation.NonNull;
import androidx.appsearch.app.PackageIdentifier;
import androidx.appsearch.exceptions.AppSearchException;
import androidx.appsearch.localstorage.util.PrefixUtil;
import androidx.appsearch.localstorage.visibilitystore.VisibilityStore;
import androidx.collection.ArraySet;

import com.google.android.icing.proto.SchemaTypeConfigProto;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

import org.junit.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class QueryProcessorTest {

    @Test
    public void testGetTargetNamespaceFilters() {
        // Insert schema
        String prefix1 = createPrefix("package", "database1");
        String prefix2 = createPrefix("package", "database2");
        Map<String, Set<String>> namespaceMap = ImmutableMap.of(
                prefix1, ImmutableSet.of("package$database1/namespace1",
                        "package$database1/namespace2"),
                prefix2, ImmutableSet.of("package$database2/namespace3",
                        "package$database2/namespace4")
        );

        // Empty searching filter will get all stored namespaces for target filter
        Set<String> targetNamespaceFilters = QueryProcessor.getPrefixedTargetNamespaceFilters(
                ImmutableSet.of(prefix1, prefix2),
                /*searchingNamespaceFilters=*/ ImmutableList.of(), namespaceMap);
        assertThat(targetNamespaceFilters).containsExactly(
                "package$database1/namespace1", "package$database1/namespace2",
                "package$database2/namespace3", "package$database2/namespace4");

        // Only search prefix1 will return namespace 1 and 2.
        targetNamespaceFilters = QueryProcessor.getPrefixedTargetNamespaceFilters(
                ImmutableSet.of(prefix1),
                /*searchingNamespaceFilters=*/ ImmutableList.of(), namespaceMap);
        assertThat(targetNamespaceFilters).containsExactly(
                "package$database1/namespace1", "package$database1/namespace2");

        // If the searching namespace filter is not empty, the target namespace filter will be the
        // intersection of the searching namespace filters that users want to search over and
        // those candidates which are stored in AppSearch.
        targetNamespaceFilters = QueryProcessor.getPrefixedTargetNamespaceFilters(
                ImmutableSet.of(prefix1),
                /*searchingNamespaceFilters=*/ ImmutableList.of("namespace1", "nonExist"),
                namespaceMap);
        assertThat(targetNamespaceFilters).containsExactly("package$database1/namespace1");

        // If there is no intersection of the namespace filters that user want to search over and
        // those candidates which are stored in AppSearch, return empty.
        targetNamespaceFilters = QueryProcessor.getPrefixedTargetNamespaceFilters(
                ImmutableSet.of(prefix1),
                /*searchingNamespaceFilters=*/ ImmutableList.of("nonExist"),
                namespaceMap);
        assertThat(targetNamespaceFilters).isEmpty();
    }

    @Test
    public void testGetTargetSchemaFilters() {
        // Build a schema map
        SchemaTypeConfigProto schemaTypeConfigProto =
                SchemaTypeConfigProto.newBuilder().getDefaultInstanceForType();
        String prefix1 = createPrefix("package", "database1");
        String prefix2 = createPrefix("package", "database2");
        Map<String, Map<String, SchemaTypeConfigProto>> schemaMap = ImmutableMap.of(
                prefix1, ImmutableMap.of(
                        "package$database1/typeA", schemaTypeConfigProto,
                        "package$database1/typeB", schemaTypeConfigProto),
                prefix2, ImmutableMap.of(
                        "package$database2/typeC", schemaTypeConfigProto,
                        "package$database2/typeD", schemaTypeConfigProto)
        );
        // Empty searching filter will get all types for target filter
        Set<String> targetSchemaFilters = QueryProcessor.getPrefixedTargetSchemaFilters(
                ImmutableSet.of(prefix1, prefix2),
                /*searchingSchemaFilters=*/ ImmutableList.of(), schemaMap);
        assertThat(targetSchemaFilters).containsExactly(
                "package$database1/typeA", "package$database1/typeB",
                "package$database2/typeC", "package$database2/typeD");

        // Only search prefix1 will return namespace 1 and 2.
        targetSchemaFilters = QueryProcessor.getPrefixedTargetSchemaFilters(
                ImmutableSet.of(prefix1),
                /*searchingSchemaFilters=*/ ImmutableList.of(), schemaMap);
        assertThat(targetSchemaFilters).containsExactly(
                "package$database1/typeA", "package$database1/typeB");

        // If the searching schema filter is not empty, the target schema filter will be the
        // intersection of the schema filters that users want to search over and those candidates
        // which are stored in AppSearch.
        targetSchemaFilters = QueryProcessor.getPrefixedTargetSchemaFilters(
                ImmutableSet.of(prefix1),
                /*searchingSchemaFilters=*/ ImmutableList.of("typeA", "nonExist"),
                schemaMap);
        assertThat(targetSchemaFilters).containsExactly("package$database1/typeA");

        // If there is no intersection of the schema filters that user want to search over and
        // those filters which are stored in AppSearch, return empty.
        targetSchemaFilters = QueryProcessor.getPrefixedTargetSchemaFilters(
                ImmutableSet.of(prefix1),
                /*searchingSchemaFilters=*/ ImmutableList.of("nonExist"),
                schemaMap);
        assertThat(targetSchemaFilters).isEmpty();
    }

    @Test
    public void testRemoveInaccessibleSchemaFilter() {
        String prefix = PrefixUtil.createPrefix("packageName", "databaseName");
        // create mutable target schema filters
        Set<String> prefixedTargetSchema = new ArraySet<>();
        prefixedTargetSchema.add(prefix + "schema1");
        prefixedTargetSchema.add(prefix + "schema2");
        prefixedTargetSchema.add(prefix + "schema3");
        QueryProcessor.removeInaccessibleSchemaFilter(
                "otherPackageName",
                new VisibilityStore() {
                    @Override
                    public void setVisibility(@NonNull String packageName,
                            @NonNull String databaseName,
                            @NonNull Set<String> schemasNotDisplayedBySystem,
                            @NonNull Map<String, List<PackageIdentifier>> schemasVisibleToPackages)
                            throws AppSearchException {

                    }

                    @Override
                    public boolean isSchemaSearchableByCaller(@NonNull String packageName,
                            @NonNull String databaseName, @NonNull String prefixedSchema,
                            int callerUid, boolean callerHasSystemAccess) {
                        // filter out schema 2 which is not searchable for user.
                        return !prefixedSchema.equals(prefix + "schema2");
                    }
                },
                /*callerUid=*/-1,
                /*callerHasSystemAccess=*/true,
                prefixedTargetSchema);

        assertThat(prefixedTargetSchema).containsExactly(
                prefix + "schema1", prefix + "schema3");
    }
}
