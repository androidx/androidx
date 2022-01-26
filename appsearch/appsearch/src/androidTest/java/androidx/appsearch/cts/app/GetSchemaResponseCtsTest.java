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

import static org.junit.Assert.assertThrows;

import androidx.appsearch.app.AppSearchSchema;
import androidx.appsearch.app.GetSchemaResponse;
import androidx.appsearch.app.PackageIdentifier;
import androidx.appsearch.app.SetSchemaRequest;

import com.google.common.collect.ImmutableSet;

import org.junit.Test;

import java.util.Arrays;

public class GetSchemaResponseCtsTest {
    @Test
    public void testRebuild() {
        byte[] sha256cert1 = new byte[32];
        byte[] sha256cert2 = new byte[32];
        Arrays.fill(sha256cert1, (byte) 1);
        Arrays.fill(sha256cert2, (byte) 2);
        PackageIdentifier packageIdentifier1 = new PackageIdentifier("Email", sha256cert1);
        PackageIdentifier packageIdentifier2 = new PackageIdentifier("Email", sha256cert2);
        AppSearchSchema schema1 = new AppSearchSchema.Builder("Email1")
                .addProperty(new AppSearchSchema.StringPropertyConfig.Builder("subject")
                        .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_OPTIONAL)
                        .setIndexingType(
                                AppSearchSchema.StringPropertyConfig.INDEXING_TYPE_PREFIXES)
                        .setTokenizerType(AppSearchSchema.StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                        .build()
                ).build();
        AppSearchSchema schema2 = new AppSearchSchema.Builder("Email2")
                .addProperty(new AppSearchSchema.StringPropertyConfig.Builder("subject")
                        .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_OPTIONAL)
                        .setIndexingType(
                                AppSearchSchema.StringPropertyConfig.INDEXING_TYPE_PREFIXES)
                        .setTokenizerType(AppSearchSchema.StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                        .build()
                ).build();

        GetSchemaResponse.Builder builder =
                new GetSchemaResponse.Builder().setVersion(42).addSchema(schema1)
                        .addSchemaTypeNotDisplayedBySystem("Email1")
                        .setSchemaTypeVisibleToPackages("Email1",
                                ImmutableSet.of(packageIdentifier1))
                        .setAllowedRolesForSchemaTypeVisibility("Email1",
                                ImmutableSet.of(SetSchemaRequest.ROLE_HOME,
                                        SetSchemaRequest.ROLE_ASSISTANT))
                        .setRequiredPermissionsForSchemaTypeVisibility("Email1",
                                ImmutableSet.of(SetSchemaRequest.READ_SMS,
                                        SetSchemaRequest.READ_CALENDAR));

        GetSchemaResponse original = builder.build();
        GetSchemaResponse rebuild = builder.setVersion(37).addSchema(schema2)
                .addSchemaTypeNotDisplayedBySystem("Email2")
                .setSchemaTypeVisibleToPackages("Email2",
                        ImmutableSet.of(packageIdentifier2))
                .setAllowedRolesForSchemaTypeVisibility("Email2",
                        ImmutableSet.of(SetSchemaRequest.ROLE_HOME))
                .setRequiredPermissionsForSchemaTypeVisibility("Email2",
                        ImmutableSet.of(SetSchemaRequest.READ_SMS))
                .build();

        // rebuild won't effect the original object
        assertThat(original.getVersion()).isEqualTo(42);
        assertThat(original.getSchemas()).containsExactly(schema1);
        assertThat(original.getSchemaTypesNotDisplayedBySystem())
                .containsExactly("Email1");
        assertThat(original.getSchemaTypesVisibleToPackages()).hasSize(1);
        assertThat(original.getSchemaTypesVisibleToPackages().get("Email1"))
                .containsExactly(packageIdentifier1);
        assertThat(original.getAllowedRolesForSchemaTypeVisibility()).hasSize(1);
        assertThat(original.getAllowedRolesForSchemaTypeVisibility().get("Email1"))
                .containsExactly(SetSchemaRequest.ROLE_HOME, SetSchemaRequest.ROLE_ASSISTANT);
        assertThat(original.getRequiredPermissionsForSchemaTypeVisibility()).hasSize(1);
        assertThat(original.getRequiredPermissionsForSchemaTypeVisibility().get("Email1"))
                .containsExactly(SetSchemaRequest.READ_SMS, SetSchemaRequest.READ_CALENDAR);

        assertThat(rebuild.getVersion()).isEqualTo(37);
        assertThat(rebuild.getSchemas()).containsExactly(schema1, schema2);
        assertThat(rebuild.getSchemaTypesNotDisplayedBySystem())
                .containsExactly("Email1", "Email2");
        assertThat(rebuild.getSchemaTypesVisibleToPackages()).hasSize(2);
        assertThat(rebuild.getSchemaTypesVisibleToPackages().get("Email1"))
                .containsExactly(packageIdentifier1);
        assertThat(rebuild.getSchemaTypesVisibleToPackages().get("Email2"))
                .containsExactly(packageIdentifier2);
        assertThat(rebuild.getAllowedRolesForSchemaTypeVisibility()).hasSize(2);
        assertThat(rebuild.getAllowedRolesForSchemaTypeVisibility().get("Email1"))
                .containsExactly(SetSchemaRequest.ROLE_HOME, SetSchemaRequest.ROLE_ASSISTANT);
        assertThat(rebuild.getAllowedRolesForSchemaTypeVisibility().get("Email2"))
                .containsExactly(SetSchemaRequest.ROLE_HOME);
        assertThat(rebuild.getRequiredPermissionsForSchemaTypeVisibility()).hasSize(2);
        assertThat(rebuild.getRequiredPermissionsForSchemaTypeVisibility().get("Email1"))
                .containsExactly(SetSchemaRequest.READ_SMS, SetSchemaRequest.READ_CALENDAR);
        assertThat(rebuild.getRequiredPermissionsForSchemaTypeVisibility().get("Email2"))
                .containsExactly(SetSchemaRequest.READ_SMS);
    }

    @Test
    public void testRebuild_noSupportedException() {
        AppSearchSchema schema1 = new AppSearchSchema.Builder("Email1")
                .addProperty(new AppSearchSchema.StringPropertyConfig.Builder("subject")
                        .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_OPTIONAL)
                        .setIndexingType(
                                AppSearchSchema.StringPropertyConfig.INDEXING_TYPE_PREFIXES)
                        .setTokenizerType(AppSearchSchema.StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                        .build()
                ).build();

        GetSchemaResponse.Builder builder =
                new GetSchemaResponse.Builder(/*getVisibilitySettingSupported=*/false)
                        .setVersion(42).addSchema(schema1);

        GetSchemaResponse original = builder.build();
        assertThrows(
                UnsupportedOperationException.class,
                () -> original.getSchemaTypesNotDisplayedBySystem());
        assertThrows(
                UnsupportedOperationException.class,
                () -> original.getSchemaTypesVisibleToPackages());
        assertThrows(
                UnsupportedOperationException.class,
                () -> original.getAllowedRolesForSchemaTypeVisibility());
        assertThrows(
                UnsupportedOperationException.class,
                () -> original.getRequiredPermissionsForSchemaTypeVisibility());

        // rebuild will throw same exception
        GetSchemaResponse rebuild = builder.setVersion(42).build();
        assertThrows(
                UnsupportedOperationException.class,
                () -> rebuild.getSchemaTypesNotDisplayedBySystem());
        assertThrows(
                UnsupportedOperationException.class,
                () -> rebuild.getSchemaTypesVisibleToPackages());
        assertThrows(
                UnsupportedOperationException.class,
                () -> original.getAllowedRolesForSchemaTypeVisibility());
        assertThrows(
                UnsupportedOperationException.class,
                () -> original.getRequiredPermissionsForSchemaTypeVisibility());
    }

    @Test
    public void setVisibility() {
        byte[] sha256cert1 = new byte[32];
        byte[] sha256cert2 = new byte[32];
        Arrays.fill(sha256cert1, (byte) 1);
        Arrays.fill(sha256cert2, (byte) 2);
        PackageIdentifier packageIdentifier1 = new PackageIdentifier("Email", sha256cert1);
        PackageIdentifier packageIdentifier2 = new PackageIdentifier("Email", sha256cert2);

        GetSchemaResponse getSchemaResponse =
                new GetSchemaResponse.Builder().setVersion(42)
                        .addSchemaTypeNotDisplayedBySystem("Email")
                        .addSchemaTypeNotDisplayedBySystem("Text")
                        .setSchemaTypeVisibleToPackages("Email",
                                ImmutableSet.of(packageIdentifier1, packageIdentifier2))
                        .setAllowedRolesForSchemaTypeVisibility("Email",
                                ImmutableSet.of(SetSchemaRequest.ROLE_HOME,
                                        SetSchemaRequest.ROLE_ASSISTANT))
                        .setRequiredPermissionsForSchemaTypeVisibility("Email",
                                ImmutableSet.of(SetSchemaRequest.READ_SMS,
                                        SetSchemaRequest.READ_CALENDAR))
                        .build();

        assertThat(getSchemaResponse.getSchemaTypesNotDisplayedBySystem())
                .containsExactly("Email", "Text");
        assertThat(getSchemaResponse.getSchemaTypesVisibleToPackages()).hasSize(1);
        assertThat(getSchemaResponse.getSchemaTypesVisibleToPackages().get("Email"))
                .containsExactly(packageIdentifier1, packageIdentifier2);
        assertThat(getSchemaResponse.getAllowedRolesForSchemaTypeVisibility().get("Email"))
                .containsExactly(SetSchemaRequest.ROLE_HOME, SetSchemaRequest.ROLE_ASSISTANT);
        assertThat(getSchemaResponse.getRequiredPermissionsForSchemaTypeVisibility().get("Email"))
                .containsExactly(SetSchemaRequest.READ_SMS, SetSchemaRequest.READ_CALENDAR);
    }
}
