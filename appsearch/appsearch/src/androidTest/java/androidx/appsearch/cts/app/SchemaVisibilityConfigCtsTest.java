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

package androidx.appsearch.cts.app;

import static com.google.common.truth.Truth.assertThat;

import androidx.appsearch.app.PackageIdentifier;
import androidx.appsearch.app.SchemaVisibilityConfig;

import com.google.common.collect.ImmutableSet;

import org.junit.Test;

import java.util.Arrays;

public class SchemaVisibilityConfigCtsTest {

    @Test
    public void testBuildVisibilityConfig() {
        byte[] cert1 = new byte[32];
        Arrays.fill(cert1, (byte) 1);
        byte[] cert2 = new byte[32];
        Arrays.fill(cert2, (byte) 2);
        SchemaVisibilityConfig schemaVisibilityConfig = new SchemaVisibilityConfig.Builder()
                .addAllowedPackage(new PackageIdentifier("pkg1", cert1))
                .setPubliclyVisibleTargetPackage(new PackageIdentifier("pkg2", cert2))
                .addRequiredPermissions(ImmutableSet.of(1, 2))
                .build();

        assertThat(schemaVisibilityConfig.getRequiredPermissions())
                .containsExactly(ImmutableSet.of(1, 2));
        assertThat(schemaVisibilityConfig.getAllowedPackages())
                .containsExactly(new PackageIdentifier("pkg1", cert1));
        assertThat(schemaVisibilityConfig.getPubliclyVisibleTargetPackage())
                .isEqualTo(new PackageIdentifier("pkg2", cert2));
    }

    @Test
    public void testVisibilityConfigEquals() {
        // Create two VisibilityConfig instances with the same properties
        SchemaVisibilityConfig visibilityConfig1 = new SchemaVisibilityConfig.Builder()
                .addAllowedPackage(new PackageIdentifier("pkg1", new byte[32]))
                .setPubliclyVisibleTargetPackage(new PackageIdentifier("pkg2", new byte[32]))
                .addRequiredPermissions(ImmutableSet.of(1, 2))
                .build();

        SchemaVisibilityConfig visibilityConfig2 = new SchemaVisibilityConfig.Builder()
                .addAllowedPackage(new PackageIdentifier("pkg1", new byte[32]))
                .setPubliclyVisibleTargetPackage(new PackageIdentifier("pkg2", new byte[32]))
                .addRequiredPermissions(ImmutableSet.of(1, 2))
                .build();

        // Test equals method
        assertThat(visibilityConfig1).isEqualTo(visibilityConfig2);
        assertThat(visibilityConfig2).isEqualTo(visibilityConfig1);
    }

    @Test
    public void testVisibilityConfig_rebuild() {
        String visibleToPackage = "com.example.package";
        byte[] visibleToPackageCert = new byte[32];

        String publiclyVisibleTarget = "com.example.test";
        byte[] publiclyVisibleTargetCert = new byte[32];

        SchemaVisibilityConfig.Builder builder = new SchemaVisibilityConfig.Builder()
                .addAllowedPackage(new PackageIdentifier(visibleToPackage, visibleToPackageCert))
                .setPubliclyVisibleTargetPackage(new PackageIdentifier(
                        publiclyVisibleTarget, publiclyVisibleTargetCert))
                .addRequiredPermissions(ImmutableSet.of(1, 2));

        // Create a VisibilityConfig using the Builder
        SchemaVisibilityConfig original = builder.build();

        SchemaVisibilityConfig rebuild = builder.clearAllowedPackages()
                .setPubliclyVisibleTargetPackage(null)
                .clearRequiredPermissions().build();

        // Check if the properties are set correctly
        assertThat(original.getAllowedPackages()).containsExactly(
                new PackageIdentifier(visibleToPackage, visibleToPackageCert));
        assertThat(original.getPubliclyVisibleTargetPackage()).isEqualTo(
                new PackageIdentifier(publiclyVisibleTarget, publiclyVisibleTargetCert));
        assertThat(original.getRequiredPermissions()).containsExactly(ImmutableSet.of(1, 2));

        assertThat(rebuild.getAllowedPackages()).isEmpty();
        assertThat(rebuild.getPubliclyVisibleTargetPackage()).isNull();
        assertThat(rebuild.getRequiredPermissions()).isEmpty();
    }
}
