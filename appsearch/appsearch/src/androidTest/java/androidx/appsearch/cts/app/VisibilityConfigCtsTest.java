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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import androidx.appsearch.app.PackageIdentifier;
import androidx.appsearch.app.VisibilityConfig;

import com.google.common.collect.ImmutableSet;

import org.junit.Test;

import java.util.Arrays;

public class VisibilityConfigCtsTest {

    @Test
    public void testBuildVisibilityConfig() {
        byte[] cert1 = new byte[32];
        Arrays.fill(cert1, (byte) 1);
        byte[] cert2 = new byte[32];
        Arrays.fill(cert2, (byte) 2);
        VisibilityConfig visibilityConfig = new VisibilityConfig.Builder()
                .setNotDisplayedBySystem(true)
                .addVisibleToPackage(new PackageIdentifier("pkg1", cert1))
                .setPubliclyVisibleTargetPackage(new PackageIdentifier("pkg2", cert2))
                .addVisibleToPermissions(ImmutableSet.of(1, 2))
                .build();

        assertTrue(visibilityConfig.isNotDisplayedBySystem());
        assertThat(visibilityConfig.getVisibleToPermissions())
                .containsExactly(ImmutableSet.of(1, 2));
        assertThat(visibilityConfig.getVisibleToPackages())
                .containsExactly(new PackageIdentifier("pkg1", cert1));
        assertThat(visibilityConfig.getPubliclyVisibleTargetPackage())
                .isEqualTo(new PackageIdentifier("pkg2", cert2));
    }

    @Test
    public void testVisibilityConfigEquals() {
        // Create two VisibilityConfig instances with the same properties
        VisibilityConfig visibilityConfig1 = new VisibilityConfig.Builder()
                .setNotDisplayedBySystem(true)
                .addVisibleToPackage(new PackageIdentifier("pkg1", new byte[32]))
                .setPubliclyVisibleTargetPackage(new PackageIdentifier("pkg2", new byte[32]))
                .addVisibleToPermissions(ImmutableSet.of(1, 2))
                .build();

        VisibilityConfig visibilityConfig2 = new VisibilityConfig.Builder()
                .setNotDisplayedBySystem(true)
                .addVisibleToPackage(new PackageIdentifier("pkg1", new byte[32]))
                .setPubliclyVisibleTargetPackage(new PackageIdentifier("pkg2", new byte[32]))
                .addVisibleToPermissions(ImmutableSet.of(1, 2))
                .build();

        // Test equals method
        assertEquals(visibilityConfig1, visibilityConfig2);
        assertEquals(visibilityConfig2, visibilityConfig1);
    }

    @Test
    public void testVisibilityConfig_rebuild() {
        String visibleToPackage = "com.example.package";
        byte[] visibleToPackageCert = new byte[32];

        String publiclyVisibleTarget = "com.example.test";
        byte[] publiclyVisibleTargetCert = new byte[32];

        VisibilityConfig.Builder builder = new VisibilityConfig.Builder()
                .setNotDisplayedBySystem(true)
                .addVisibleToPackage(new PackageIdentifier(visibleToPackage, visibleToPackageCert))
                .setPubliclyVisibleTargetPackage(new PackageIdentifier(
                        publiclyVisibleTarget, publiclyVisibleTargetCert))
                .addVisibleToPermissions(ImmutableSet.of(1, 2));

        // Create a VisibilityConfig using the Builder
        VisibilityConfig original = builder.build();

        VisibilityConfig rebuild = builder.clearVisibleToPackages()
                .setNotDisplayedBySystem(false)
                .setPubliclyVisibleTargetPackage(null)
                .clearVisibleToPermissions().build();

        // Check if the properties are set correctly
        assertThat(original.isNotDisplayedBySystem()).isTrue();
        assertThat(original.getVisibleToPackages()).containsExactly(
                new PackageIdentifier(visibleToPackage, visibleToPackageCert));
        assertThat(original.getPubliclyVisibleTargetPackage()).isEqualTo(
                new PackageIdentifier(publiclyVisibleTarget, publiclyVisibleTargetCert));
        assertThat(original.getVisibleToPermissions()).containsExactly(ImmutableSet.of(1, 2));

        assertThat(rebuild.isNotDisplayedBySystem()).isFalse();
        assertThat(rebuild.getVisibleToPackages()).isEmpty();
        assertThat(rebuild.getPubliclyVisibleTargetPackage()).isNull();
        assertThat(rebuild.getVisibleToPermissions()).isEmpty();
    }
}
