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

package androidx.appsearch.app;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertThrows;

import com.google.common.collect.ImmutableSet;

import org.junit.Test;

public class SetSchemaRequestInternalTest {

    @Test
    public void testSetSchemaTypeVisibleForConfigs_unsupportedNestedConfig() {
        AppSearchSchema schema = new AppSearchSchema.Builder("Schema").build();

        PackageIdentifier packageIdentifier1 = new PackageIdentifier("com.package.foo",
                new byte[]{100});
        PackageIdentifier packageIdentifier2 = new PackageIdentifier("com.package.bar",
                new byte[]{100});

        VisibilityConfig nestedConfig = new VisibilityConfig.Builder()
                .addVisibleToPackage(packageIdentifier1)
                .addVisibleToPermissions(
                        ImmutableSet.of(SetSchemaRequest.READ_HOME_APP_SEARCH_DATA))
                .build();
        VisibilityConfig config = new VisibilityConfig.Builder()
                .addVisibleToPackage(packageIdentifier2)
                .addVisibleToPermissions(ImmutableSet.of(
                        SetSchemaRequest.READ_HOME_APP_SEARCH_DATA,
                        SetSchemaRequest.READ_CALENDAR))
                .addVisibleToConfig(nestedConfig)
                .build();

        UnsupportedOperationException exception = assertThrows(UnsupportedOperationException.class,
                () -> new SetSchemaRequest.Builder()
                        .addSchemas(schema)
                        .addSchemaTypeVisibleToConfig("Schema", config)
                        .build());
        assertThat(exception).hasMessageThat().contains(
                "Cannot set a VisibilityConfig with nested VisibilityConfig");
    }

    @Test
    public void testSetSchemaTypeVisibleForConfigs_unsupportedWithSchemaType() {
        AppSearchSchema schema = new AppSearchSchema.Builder("Schema").build();

        VisibilityConfig config = new VisibilityConfig.Builder("Schema")
                .addVisibleToPermissions(ImmutableSet.of(
                        SetSchemaRequest.READ_HOME_APP_SEARCH_DATA,
                        SetSchemaRequest.READ_CALENDAR))
                .build();

        UnsupportedOperationException exception = assertThrows(UnsupportedOperationException.class,
                () -> new SetSchemaRequest.Builder()
                        .addSchemas(schema)
                        .addSchemaTypeVisibleToConfig("Schema", config)
                        .build());
        assertThat(exception).hasMessageThat().contains(
                "Cannot set a VisibilityConfig with a schema type");
    }
}
