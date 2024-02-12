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

import com.google.common.collect.ImmutableSet;

import org.junit.Test;

import java.util.List;

public class InternalVisibilityConfigTest {

    @Test
    public void testVisibilityConfig_setNotDisplayBySystem() {
        InternalVisibilityConfig visibilityConfig = new InternalVisibilityConfig.Builder("schema")
                .setNotDisplayedBySystem(true).build();

        assertThat(visibilityConfig.isNotDisplayedBySystem()).isTrue();
    }

    @Test
    public void testVisibilityConfig_setVisibilityConfig() {
        String visibleToPackage1 = "com.example.package";
        byte[] visibleToPackageCert1 = new byte[32];
        String visibleToPackage2 = "com.example.package2";
        byte[] visibleToPackageCert2 = new byte[32];

        SchemaVisibilityConfig innerConfig1 = new SchemaVisibilityConfig.Builder()
                .addAllowedPackage(
                        new PackageIdentifier(visibleToPackage1, visibleToPackageCert1))
                .addRequiredPermissions(ImmutableSet.of(1, 2))
                .build();
        SchemaVisibilityConfig innerConfig2 = new SchemaVisibilityConfig.Builder()
                .addAllowedPackage(
                        new PackageIdentifier(visibleToPackage2, visibleToPackageCert2))
                .addRequiredPermissions(ImmutableSet.of(3, 4))
                .build();

        InternalVisibilityConfig visibilityConfig = new InternalVisibilityConfig.Builder("schema")
                .addVisibleToConfig(innerConfig1)
                .addVisibleToConfig(innerConfig2)
                .build();

        assertThat(visibilityConfig.getVisibleToConfigs())
                .containsExactly(innerConfig1, innerConfig2);
    }

    @Test
    public void testToInternalVisibilityConfig() {
        byte[] packageSha256Cert = new byte[32];
        packageSha256Cert[0] = 24;
        packageSha256Cert[8] = 23;
        packageSha256Cert[16] = 22;
        packageSha256Cert[24] = 21;

        // Create a SetSchemaRequest for testing
        SetSchemaRequest setSchemaRequest = new SetSchemaRequest.Builder()
                .addSchemas(new AppSearchSchema.Builder("testSchema").build())
                .setSchemaTypeDisplayedBySystem("testSchema", false)
                .setSchemaTypeVisibilityForPackage("testSchema", /*visible=*/true,
                        new PackageIdentifier("com.example.test", packageSha256Cert))
                .setPubliclyVisibleSchema("testSchema",
                        new PackageIdentifier("com.example.test1", packageSha256Cert))
                .build();

        // Convert the SetSchemaRequest to GenericDocument map
        List<InternalVisibilityConfig> visibilityConfigs =
                InternalVisibilityConfig.toInternalVisibilityConfigs(setSchemaRequest);

        // Check if the conversion is correct
        assertThat(visibilityConfigs).hasSize(1);
        InternalVisibilityConfig visibilityConfig = visibilityConfigs.get(0);
        assertThat(visibilityConfig.isNotDisplayedBySystem()).isTrue();
        assertThat(visibilityConfig.getVisibilityConfig().getAllowedPackages())
                .containsExactly(new PackageIdentifier("com.example.test", packageSha256Cert));
        assertThat(visibilityConfig.getVisibilityConfig().getPubliclyVisibleTargetPackage())
                .isNotNull();
        assertThat(
                visibilityConfig.getVisibilityConfig().getPubliclyVisibleTargetPackage()
                        .getPackageName())
                .isEqualTo("com.example.test1");
        assertThat(
                visibilityConfig.getVisibilityConfig().getPubliclyVisibleTargetPackage()
                        .getSha256Certificate())
                .isEqualTo(packageSha256Cert);
    }
}
