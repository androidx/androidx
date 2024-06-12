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

package androidx.appsearch.localstorage;

import static androidx.appsearch.localstorage.util.PrefixUtil.createPrefix;

import static com.google.common.truth.Truth.assertThat;

import com.google.android.icing.proto.SchemaTypeConfigProto;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

import org.junit.Test;

import java.util.Map;

public class SchemaCacheTest {

    @Test
    public void testGetSchemaTypesWithDescendants() throws Exception {
        String prefix = createPrefix("package", "database");
        SchemaTypeConfigProto personSchema =
                SchemaTypeConfigProto.newBuilder()
                        .setSchemaType("package$database/Person")
                        .build();
        SchemaTypeConfigProto artistSchema =
                SchemaTypeConfigProto.newBuilder()
                        .setSchemaType("package$database/Artist")
                        .addParentTypes("package$database/Person")
                        .build();
        SchemaTypeConfigProto otherSchema =
                SchemaTypeConfigProto.newBuilder()
                        .setSchemaType("package$database/Other")
                        .build();
        Map<String, Map<String, SchemaTypeConfigProto>> schemaMap = ImmutableMap.of(
                prefix, ImmutableMap.of(
                        "package$database/Person", personSchema,
                        "package$database/Artist", artistSchema,
                        "package$database/Other", otherSchema));
        SchemaCache schemaCache = new SchemaCache(schemaMap);

        assertThat(schemaCache.getSchemaTypesWithDescendants(prefix,
                ImmutableSet.of("package$database/Person"))).containsExactly(
                "package$database/Person", "package$database/Artist");
        assertThat(schemaCache.getSchemaTypesWithDescendants(prefix,
                ImmutableSet.of("package$database/Artist"))).containsExactly(
                "package$database/Artist");
        assertThat(schemaCache.getSchemaTypesWithDescendants(prefix,
                ImmutableSet.of("package$database/Other"))).containsExactly(
                "package$database/Other");
    }

    @Test
    public void testGetSchemaTypesWithDescendants_multipleLevel() throws Exception {
        String prefix = createPrefix("package", "database");
        SchemaTypeConfigProto schemaA =
                SchemaTypeConfigProto.newBuilder()
                        .setSchemaType("package$database/A")
                        .build();
        SchemaTypeConfigProto schemaB =
                SchemaTypeConfigProto.newBuilder()
                        .setSchemaType("package$database/B")
                        .build();
        SchemaTypeConfigProto schemaC =
                SchemaTypeConfigProto.newBuilder()
                        .setSchemaType("package$database/C")
                        .addParentTypes("package$database/A")
                        .build();
        SchemaTypeConfigProto schemaD =
                SchemaTypeConfigProto.newBuilder()
                        .setSchemaType("package$database/D")
                        .addParentTypes("package$database/C")
                        .build();
        SchemaTypeConfigProto schemaE =
                SchemaTypeConfigProto.newBuilder()
                        .setSchemaType("package$database/E")
                        .addParentTypes("package$database/B")
                        .addParentTypes("package$database/C")
                        .build();
        Map<String, Map<String, SchemaTypeConfigProto>> schemaMap = ImmutableMap.of(
                prefix, ImmutableMap.of(
                        "package$database/A", schemaA,
                        "package$database/B", schemaB,
                        "package$database/C", schemaC,
                        "package$database/D", schemaD,
                        "package$database/E", schemaE));
        SchemaCache schemaCache = new SchemaCache(schemaMap);

        assertThat(schemaCache.getSchemaTypesWithDescendants(prefix,
                ImmutableSet.of("package$database/A"))).containsExactly(
                "package$database/A", "package$database/C", "package$database/D",
                "package$database/E");
        assertThat(schemaCache.getSchemaTypesWithDescendants(prefix,
                ImmutableSet.of("package$database/B"))).containsExactly(
                "package$database/B", "package$database/E");
        assertThat(schemaCache.getSchemaTypesWithDescendants(prefix,
                ImmutableSet.of("package$database/A", "package$database/B"))).containsExactly(
                "package$database/A", "package$database/B", "package$database/C",
                "package$database/D", "package$database/E");
    }
}
