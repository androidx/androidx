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

public class VisibilityConfigInternalTest {

    @Test
    public void testVisibilityConfig_setVisibilityConfig() {
        String visibleToPackage1 = "com.example.package";
        byte[] visibleToPackageCert1 = new byte[32];
        String visibleToPackage2 = "com.example.package2";
        byte[] visibleToPackageCert2 = new byte[32];

        VisibilityConfig innerConfig1 = new VisibilityConfig.Builder()
                .addVisibleToPackage(
                        new PackageIdentifier(visibleToPackage1, visibleToPackageCert1))
                .addVisibleToPermissions(ImmutableSet.of(1, 2))
                .build();
        VisibilityConfig innerConfig2 = new VisibilityConfig.Builder()
                .addVisibleToPackage(
                        new PackageIdentifier(visibleToPackage2, visibleToPackageCert2))
                .addVisibleToPermissions(ImmutableSet.of(3, 4))
                .build();

        VisibilityConfig visibilityConfig = new VisibilityConfig.Builder()
                .addVisibleToConfig(innerConfig1)
                .addVisibleToConfig(innerConfig2)
                .build();

        assertThat(visibilityConfig.getVisibleToConfigs())
                .containsExactly(innerConfig1, innerConfig2);
    }
}
