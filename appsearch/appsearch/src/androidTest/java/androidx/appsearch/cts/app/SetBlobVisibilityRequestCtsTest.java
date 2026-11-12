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

import static org.junit.Assert.assertThrows;

import androidx.appsearch.app.PackageIdentifier;
import androidx.appsearch.app.SchemaVisibilityConfig;
import androidx.appsearch.app.SetBlobVisibilityRequest;
import androidx.appsearch.app.SetSchemaRequest;
import androidx.appsearch.flags.Flags;
import androidx.appsearch.testutil.AppSearchTestUtils;
import androidx.appsearch.testutil.flags.RequiresFlagsEnabled;

import com.google.common.collect.ImmutableSet;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;

public class SetBlobVisibilityRequestCtsTest {

    @Rule
    public final RuleChain mRuleChain = AppSearchTestUtils.createCommonTestRules();

    @Test
    public void testBuildAndGet() {
        PackageIdentifier packageIdentifier1 =
                new PackageIdentifier("com.package.foo", new byte[] {100});
        PackageIdentifier packageIdentifier2 =
                new PackageIdentifier("com.package.bar", new byte[] {100});

        SchemaVisibilityConfig config1 =
                new SchemaVisibilityConfig.Builder()
                        .addAllowedPackage(packageIdentifier1)
                        .addRequiredPermissions(
                                ImmutableSet.of(SetSchemaRequest.READ_HOME_APP_SEARCH_DATA))
                        .build();
        SchemaVisibilityConfig config2 =
                new SchemaVisibilityConfig.Builder()
                        .addAllowedPackage(packageIdentifier2)
                        .addRequiredPermissions(
                                ImmutableSet.of(
                                        SetSchemaRequest.READ_HOME_APP_SEARCH_DATA,
                                        SetSchemaRequest.READ_CALENDAR))
                        .build();

        SetBlobVisibilityRequest request = new SetBlobVisibilityRequest.Builder()
                .setNamespaceDisplayedBySystem("namespace1", /* displayed= */false)
                .addNamespaceVisibleToConfig("namespace2", config1)
                .addNamespaceVisibleToConfig("namespace2", config2)
                .build();

        assertThat(request.getNamespacesNotDisplayedBySystem())
                .containsExactly("namespace1");
        assertThat(request.getNamespacesVisibleToConfigs())
                .containsExactly("namespace2", ImmutableSet.of(config1, config2));
    }

    @Test
    public void testRebuild() {
        PackageIdentifier packageIdentifier1 =
                new PackageIdentifier("com.package.foo", new byte[] {100});
        PackageIdentifier packageIdentifier2 =
                new PackageIdentifier("com.package.bar", new byte[] {100});

        SchemaVisibilityConfig config1 =
                new SchemaVisibilityConfig.Builder()
                        .addAllowedPackage(packageIdentifier1)
                        .addRequiredPermissions(
                                ImmutableSet.of(SetSchemaRequest.READ_HOME_APP_SEARCH_DATA))
                        .build();
        SchemaVisibilityConfig config2 =
                new SchemaVisibilityConfig.Builder()
                        .addAllowedPackage(packageIdentifier2)
                        .addRequiredPermissions(
                                ImmutableSet.of(
                                        SetSchemaRequest.READ_HOME_APP_SEARCH_DATA,
                                        SetSchemaRequest.READ_CALENDAR))
                        .build();

        SetBlobVisibilityRequest.Builder builder = new SetBlobVisibilityRequest.Builder()
                .setNamespaceDisplayedBySystem("namespace1", /* displayed= */false)
                .addNamespaceVisibleToConfig("namespace2", config1);

        SetBlobVisibilityRequest original = builder.build();

        SetBlobVisibilityRequest rebuild = builder
                .setNamespaceDisplayedBySystem("namespace3", /* displayed= */ false)
                .addNamespaceVisibleToConfig("namespace4", config2)
                .build();

        assertThat(original.getNamespacesNotDisplayedBySystem())
                .containsExactly("namespace1");
        assertThat(original.getNamespacesVisibleToConfigs())
                .containsExactly("namespace2", ImmutableSet.of(config1));

        assertThat(rebuild.getNamespacesNotDisplayedBySystem())
                .containsExactly("namespace1", "namespace3");
        assertThat(rebuild.getNamespacesVisibleToConfigs())
                .containsExactly("namespace2", ImmutableSet.of(config1),
                        "namespace4", ImmutableSet.of(config2));
    }

    @Test
    public void testClearNamespaceVisibleToConfigs() {
        PackageIdentifier packageIdentifier1 =
                new PackageIdentifier("com.package.foo", new byte[] {100});
        PackageIdentifier packageIdentifier2 =
                new PackageIdentifier("com.package.bar", new byte[] {100});

        SchemaVisibilityConfig config1 =
                new SchemaVisibilityConfig.Builder()
                        .addAllowedPackage(packageIdentifier1)
                        .addRequiredPermissions(
                                ImmutableSet.of(SetSchemaRequest.READ_HOME_APP_SEARCH_DATA))
                        .build();
        SchemaVisibilityConfig config2 =
                new SchemaVisibilityConfig.Builder()
                        .addAllowedPackage(packageIdentifier2)
                        .addRequiredPermissions(
                                ImmutableSet.of(
                                        SetSchemaRequest.READ_HOME_APP_SEARCH_DATA,
                                        SetSchemaRequest.READ_CALENDAR))
                        .build();

        SetBlobVisibilityRequest request = new SetBlobVisibilityRequest.Builder()
                .addNamespaceVisibleToConfig("namespace1", config1)
                .clearNamespaceVisibleToConfigs("namespace1")
                .addNamespaceVisibleToConfig("namespace1", config2).build();

        assertThat(request.getNamespacesVisibleToConfigs())
                .containsExactly("namespace1", ImmutableSet.of(config2));
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_ENABLE_SCHEMA_VISIBLE_TO_PACKAGE_WITH_TAGS)
    public void testBuild_packageVisibleWithAndWithoutTags_throws() {
        PackageIdentifier packageFoo =
                new PackageIdentifier("com.package.foo", new byte[] {100});
        // The same package, identified by another certificate.
        PackageIdentifier packageFooOtherCert =
                new PackageIdentifier("com.package.foo", new byte[] {101});
        SchemaVisibilityConfig packageConfig =
                new SchemaVisibilityConfig.Builder()
                        .addAllowedPackage(packageFooOtherCert)
                        .addRequiredPermissions(ImmutableSet.of(SetSchemaRequest.READ_CALENDAR))
                        .build();
        SchemaVisibilityConfig packageWithTagsConfig =
                new SchemaVisibilityConfig.Builder()
                        .setAllowedPackageWithTags(packageFoo, ImmutableSet.of("tagA"))
                        .build();

        SetBlobVisibilityRequest.Builder builder = new SetBlobVisibilityRequest.Builder()
                .addNamespaceVisibleToConfig("namespace1", packageConfig)
                .addNamespaceVisibleToConfig("namespace1", packageWithTagsConfig);
        IllegalArgumentException e =
                assertThrows(IllegalArgumentException.class, builder::build);
        assertThat(e).hasMessageThat().contains(
                "Namespace namespace1 is visible to package com.package.foo both as a whole and "
                        + "restricted by attribution tags");

        // The request is valid once the namespace only has one of the two.
        SetBlobVisibilityRequest request = builder
                .clearNamespaceVisibleToConfigs("namespace1")
                .addNamespaceVisibleToConfig("namespace1", packageWithTagsConfig)
                .build();
        assertThat(request.getNamespacesVisibleToConfigs())
                .containsExactly("namespace1", ImmutableSet.of(packageWithTagsConfig));
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_ENABLE_SCHEMA_VISIBLE_TO_PACKAGE_WITH_TAGS)
    public void testBuild_differentPackagesVisibleWithAndWithoutTags() {
        PackageIdentifier packageFoo =
                new PackageIdentifier("com.package.foo", new byte[] {100});
        PackageIdentifier packageBar =
                new PackageIdentifier("com.package.bar", new byte[] {100});
        SchemaVisibilityConfig fooConfig =
                new SchemaVisibilityConfig.Builder().addAllowedPackage(packageFoo).build();
        SchemaVisibilityConfig barConfig =
                new SchemaVisibilityConfig.Builder().addAllowedPackage(packageBar).build();
        SchemaVisibilityConfig fooWithTagsConfig =
                new SchemaVisibilityConfig.Builder()
                        .setAllowedPackageWithTags(packageFoo, ImmutableSet.of("tagA"))
                        .build();

        // A namespace can be visible to a package as a whole and to another package by
        // attribution tag, and a package can be visible both ways for different namespaces.
        SetBlobVisibilityRequest request = new SetBlobVisibilityRequest.Builder()
                .addNamespaceVisibleToConfig("namespace1", barConfig)
                .addNamespaceVisibleToConfig("namespace1", fooWithTagsConfig)
                .addNamespaceVisibleToConfig("namespace2", fooConfig)
                .build();

        assertThat(request.getNamespacesVisibleToConfigs()).containsExactly(
                "namespace1", ImmutableSet.of(barConfig, fooWithTagsConfig),
                "namespace2", ImmutableSet.of(fooConfig));
    }
}
