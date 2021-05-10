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

package androidx.appsearch.cts.app;

import static com.google.common.truth.Truth.assertThat;

import androidx.annotation.NonNull;
import androidx.appsearch.app.AppSearchSchema;
import androidx.appsearch.app.GenericDocument;
import androidx.appsearch.app.Migrator;
import androidx.appsearch.app.PackageIdentifier;
import androidx.appsearch.app.SetSchemaRequest;
import androidx.collection.ArrayMap;

import org.junit.Test;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

public class SetSchemaRequestCtsTest {
    @Test
    public void testBuildSetSchemaRequest() {
        AppSearchSchema.StringPropertyConfig prop1 =
                new AppSearchSchema.StringPropertyConfig.Builder("prop1")
                        .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_OPTIONAL)
                        .setIndexingType(
                                AppSearchSchema.StringPropertyConfig.INDEXING_TYPE_PREFIXES)
                        .setTokenizerType(AppSearchSchema.StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                        .build();
        AppSearchSchema schema1 =
                new AppSearchSchema.Builder("type1").addProperty(prop1).build();
        AppSearchSchema schema2 =
                new AppSearchSchema.Builder("type2").addProperty(prop1).build();

        PackageIdentifier packageIdentifier =
                new PackageIdentifier("com.package.foo", new byte[]{100});

        SetSchemaRequest request = new SetSchemaRequest.Builder()
                .addSchemas(schema1, schema2)
                .setSchemaTypeDisplayedBySystem("type2", /*displayed=*/ false)
                .setSchemaTypeVisibilityForPackage("type1", /*visible=*/ true,
                        packageIdentifier).build();


        assertThat(request.getSchemas()).containsExactly(schema1, schema2);
        assertThat(request.getSchemasNotDisplayedBySystem()).containsExactly("type2");

        Map<String, Set<PackageIdentifier>> expectedVisibleToPackagesMap = new ArrayMap<>();
        expectedVisibleToPackagesMap.put("type1", Collections.singleton(packageIdentifier));
        assertThat(request.getSchemasVisibleToPackages()).containsExactlyEntriesIn(
                expectedVisibleToPackagesMap);
    }

    @Test
    public void testSetSchemaRequestTypeChanges() {
        AppSearchSchema.StringPropertyConfig requiredProp =
                new AppSearchSchema.StringPropertyConfig.Builder("prop1")
                        .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_REQUIRED)
                        .setIndexingType(
                                AppSearchSchema.StringPropertyConfig.INDEXING_TYPE_PREFIXES)
                        .setTokenizerType(AppSearchSchema.StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                        .build();
        AppSearchSchema schema1 =
                new AppSearchSchema.Builder("type1").addProperty(requiredProp).build();
        AppSearchSchema schema2 =
                new AppSearchSchema.Builder("type2").addProperty(requiredProp).build();

        Migrator migrator = new Migrator() {
            @Override
            public boolean shouldMigrate(int currentVersion, int finalVersion) {
                return true;
            }

            @NonNull
            @Override
            public GenericDocument onUpgrade(int currentVersion, int finalVersion,
                    @NonNull GenericDocument document) {
                return document;
            }

            @NonNull
            @Override
            public GenericDocument onDowngrade(int currentVersion, int finalVersion,
                    @NonNull GenericDocument document) {
                return document;
            }
        };

        SetSchemaRequest request = new SetSchemaRequest.Builder()
                .addSchemas(schema1, schema2)
                .setForceOverride(/*forceOverride=*/ true)
                .setMigrator("type2", migrator)
                .build();

        assertThat(request.isForceOverride()).isTrue();
        Map<String, Migrator> expectedMigratorMap = new ArrayMap<>();
        expectedMigratorMap.put("type2", migrator);
        assertThat(request.getMigrators()).containsExactlyEntriesIn(expectedMigratorMap);
    }
}
