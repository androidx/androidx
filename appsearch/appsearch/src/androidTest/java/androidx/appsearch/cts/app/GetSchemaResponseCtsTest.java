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
import androidx.appsearch.app.SchemaVisibilityConfig;
import androidx.appsearch.app.SetSchemaRequest;
import androidx.appsearch.flags.CheckFlagsRule;
import androidx.appsearch.flags.DeviceFlagsValueProvider;
import androidx.appsearch.flags.Flags;
import androidx.appsearch.flags.RequiresFlagsEnabled;

import com.google.common.collect.ImmutableSet;

import org.junit.Rule;
import org.junit.Test;

import java.util.Arrays;
import java.util.Map;

public class GetSchemaResponseCtsTest {
    @Rule
    public final CheckFlagsRule mCheckFlagsRule = DeviceFlagsValueProvider.createCheckFlagsRule();

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
                        .setRequiredPermissionsForSchemaTypeVisibility("Email1",
                                ImmutableSet.of(
                                        ImmutableSet.of(SetSchemaRequest.READ_SMS,
                                                SetSchemaRequest.READ_CALENDAR),
                                        ImmutableSet.of(SetSchemaRequest.READ_HOME_APP_SEARCH_DATA))
                        );

        GetSchemaResponse original = builder.build();
        GetSchemaResponse rebuild = builder.setVersion(37).addSchema(schema2)
                .addSchemaTypeNotDisplayedBySystem("Email2")
                .setSchemaTypeVisibleToPackages("Email2",
                        ImmutableSet.of(packageIdentifier2))
                .setRequiredPermissionsForSchemaTypeVisibility("Email2",
                        ImmutableSet.of(
                                ImmutableSet.of(SetSchemaRequest.READ_CONTACTS,
                                        SetSchemaRequest.READ_EXTERNAL_STORAGE),
                                ImmutableSet.of(SetSchemaRequest
                                        .READ_ASSISTANT_APP_SEARCH_DATA))
                ).build();

        // rebuild won't effect the original object
        assertThat(original.getVersion()).isEqualTo(42);
        assertThat(original.getSchemas()).containsExactly(schema1);
        assertThat(original.getSchemaTypesNotDisplayedBySystem())
                .containsExactly("Email1");
        assertThat(original.getSchemaTypesVisibleToPackages()).hasSize(1);
        assertThat(original.getSchemaTypesVisibleToPackages().get("Email1"))
                .containsExactly(packageIdentifier1);
        assertThat(original.getRequiredPermissionsForSchemaTypeVisibility()).containsExactly(
                "Email1",
                ImmutableSet.of(
                        ImmutableSet.of(SetSchemaRequest.READ_SMS,
                                SetSchemaRequest.READ_CALENDAR),
                        ImmutableSet.of(SetSchemaRequest.READ_HOME_APP_SEARCH_DATA)));

        assertThat(rebuild.getVersion()).isEqualTo(37);
        assertThat(rebuild.getSchemas()).containsExactly(schema1, schema2);
        assertThat(rebuild.getSchemaTypesNotDisplayedBySystem())
                .containsExactly("Email1", "Email2");
        assertThat(rebuild.getSchemaTypesVisibleToPackages()).hasSize(2);
        assertThat(rebuild.getSchemaTypesVisibleToPackages().get("Email1"))
                .containsExactly(packageIdentifier1);
        assertThat(rebuild.getSchemaTypesVisibleToPackages().get("Email2"))
                .containsExactly(packageIdentifier2);
        assertThat(rebuild.getRequiredPermissionsForSchemaTypeVisibility()).containsExactly(
                "Email1",
                ImmutableSet.of(
                        ImmutableSet.of(SetSchemaRequest.READ_SMS,
                                SetSchemaRequest.READ_CALENDAR),
                        ImmutableSet.of(SetSchemaRequest.READ_HOME_APP_SEARCH_DATA)),
                "Email2",
                ImmutableSet.of(
                        ImmutableSet.of(SetSchemaRequest.READ_CONTACTS,
                                SetSchemaRequest.READ_EXTERNAL_STORAGE),
                        ImmutableSet.of(SetSchemaRequest
                                .READ_ASSISTANT_APP_SEARCH_DATA)));
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
                        .setRequiredPermissionsForSchemaTypeVisibility("Email",
                                ImmutableSet.of(
                                        ImmutableSet.of(SetSchemaRequest.READ_CONTACTS,
                                                SetSchemaRequest.READ_EXTERNAL_STORAGE),
                                        ImmutableSet.of(SetSchemaRequest
                                                .READ_ASSISTANT_APP_SEARCH_DATA)))
                        .build();

        assertThat(getSchemaResponse.getSchemaTypesNotDisplayedBySystem())
                .containsExactly("Email", "Text");
        assertThat(getSchemaResponse.getSchemaTypesVisibleToPackages()).hasSize(1);
        assertThat(getSchemaResponse.getSchemaTypesVisibleToPackages().get("Email"))
                .containsExactly(packageIdentifier1, packageIdentifier2);
        assertThat(getSchemaResponse.getRequiredPermissionsForSchemaTypeVisibility().get("Email"))
                .containsExactlyElementsIn(ImmutableSet.of(
                        ImmutableSet.of(SetSchemaRequest.READ_CONTACTS,
                                SetSchemaRequest.READ_EXTERNAL_STORAGE),
                        ImmutableSet.of(SetSchemaRequest
                                .READ_ASSISTANT_APP_SEARCH_DATA)));
    }

    @Test
    public void setVisibilityConfig() {
        SchemaVisibilityConfig visibilityConfig1 = new SchemaVisibilityConfig.Builder()
                .addAllowedPackage(new PackageIdentifier("pkg1", new byte[32]))
                .setPubliclyVisibleTargetPackage(new PackageIdentifier("pkg2", new byte[32]))
                .addRequiredPermissions(ImmutableSet.of(1, 2))
                .build();
        SchemaVisibilityConfig visibilityConfig2 = new SchemaVisibilityConfig.Builder()
                .addAllowedPackage(new PackageIdentifier("pkg3", new byte[32]))
                .setPubliclyVisibleTargetPackage(new PackageIdentifier("pkg4", new byte[32]))
                .addRequiredPermissions(ImmutableSet.of(3, 4))
                .build();

        GetSchemaResponse getSchemaResponse =
                new GetSchemaResponse.Builder().setVersion(42)
                        .setSchemaTypeVisibleToConfigs("Email",
                                ImmutableSet.of(visibilityConfig1, visibilityConfig2))
                        .build();

        assertThat(getSchemaResponse.getSchemaTypesVisibleToConfigs()).containsExactly("Email",
                ImmutableSet.of(visibilityConfig1, visibilityConfig2));
    }

    @Test
    public void getEmptyVisibility() {
        GetSchemaResponse getSchemaResponse =
                new GetSchemaResponse.Builder().setVersion(42)
                        .build();
        assertThat(getSchemaResponse.getSchemaTypesNotDisplayedBySystem()).isEmpty();
        assertThat(getSchemaResponse.getSchemaTypesVisibleToPackages()).isEmpty();
        assertThat(getSchemaResponse.getRequiredPermissionsForSchemaTypeVisibility()).isEmpty();
    }

    @Test
    public void getEmptyVisibility_visibilityConfig() {
        GetSchemaResponse getSchemaResponse =
                new GetSchemaResponse.Builder().setVersion(42)
                        .build();
        assertThat(getSchemaResponse.getSchemaTypesVisibleToConfigs()).isEmpty();
    }

    // @exportToFramework:startStrip()
    // Not exported as setVisibilitySettingSupported is hidden in framework
    /**
     * Makes sure an exception is thrown when visibility getters are called after visibility is set
     * to no supported.
     */
    @Test
    public void setVisibility_setFalse() {
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
                        .setRequiredPermissionsForSchemaTypeVisibility("Email",
                                ImmutableSet.of(
                                        ImmutableSet.of(SetSchemaRequest.READ_CONTACTS,
                                                SetSchemaRequest.READ_EXTERNAL_STORAGE),
                                        ImmutableSet.of(SetSchemaRequest
                                                .READ_ASSISTANT_APP_SEARCH_DATA)))
                        // This should clear all visibility settings.
                        .setVisibilitySettingSupported(false)
                        .build();

        Exception e = assertThrows(UnsupportedOperationException.class,
                getSchemaResponse::getSchemaTypesNotDisplayedBySystem);
        assertThat(e.getMessage()).isEqualTo("Get visibility setting is not supported with"
                + " this backend/Android API level combination.");
        e = assertThrows(UnsupportedOperationException.class,
                getSchemaResponse::getSchemaTypesVisibleToPackages);
        assertThat(e.getMessage()).isEqualTo("Get visibility setting is not supported with"
                + " this backend/Android API level combination.");
        e = assertThrows(UnsupportedOperationException.class,
                getSchemaResponse::getRequiredPermissionsForSchemaTypeVisibility);
        assertThat(e.getMessage()).isEqualTo("Get visibility setting is not supported with"
                + " this backend/Android API level combination.");
        e = assertThrows(UnsupportedOperationException.class,
                getSchemaResponse::getPubliclyVisibleSchemas);
        assertThat(e.getMessage()).isEqualTo("Get visibility setting is not supported with"
                + " this backend/Android API level combination.");
        e = assertThrows(UnsupportedOperationException.class,
                getSchemaResponse::getSchemaTypesVisibleToConfigs);
        assertThat(e.getMessage()).isEqualTo("Get visibility setting is not supported with"
                + " this backend/Android API level combination.");
    }

    /**
     * Makes sure an exception is thrown when visibility getters are called after visibility is set
     * to no supported, even on a rebuild.
     */
    @Test
    public void testRebuild_notSupportedException() {
        AppSearchSchema schema1 = new AppSearchSchema.Builder("Email1")
                .addProperty(new AppSearchSchema.StringPropertyConfig.Builder("subject")
                        .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_OPTIONAL)
                        .setIndexingType(
                                AppSearchSchema.StringPropertyConfig.INDEXING_TYPE_PREFIXES)
                        .setTokenizerType(AppSearchSchema.StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                        .build()
                ).build();

        GetSchemaResponse.Builder builder =
                new GetSchemaResponse.Builder().setVisibilitySettingSupported(false)
                        .setVersion(42).addSchema(schema1);

        GetSchemaResponse original = builder.build();
        assertThrows(UnsupportedOperationException.class,
                original::getSchemaTypesNotDisplayedBySystem);
        assertThrows(UnsupportedOperationException.class,
                original::getSchemaTypesVisibleToPackages);
        assertThrows(UnsupportedOperationException.class,
                original::getRequiredPermissionsForSchemaTypeVisibility);

        // rebuild will throw same exception
        GetSchemaResponse rebuild = builder.setVersion(42).build();
        assertThrows(UnsupportedOperationException.class,
                rebuild::getSchemaTypesNotDisplayedBySystem);
        assertThrows(UnsupportedOperationException.class,
                rebuild::getSchemaTypesVisibleToPackages);
        assertThrows(UnsupportedOperationException.class,
                original::getRequiredPermissionsForSchemaTypeVisibility);
    }
    // @exportToFramework:endStrip()

    @Test
    public void testVisibility_publicVisibility() {
        byte[] sha256cert1 = new byte[32];
        byte[] sha256cert2 = new byte[32];
        Arrays.fill(sha256cert1, (byte) 1);
        Arrays.fill(sha256cert2, (byte) 1);
        PackageIdentifier packageIdentifier1 = new PackageIdentifier("Email", sha256cert1);
        PackageIdentifier packageIdentifier2 = new PackageIdentifier("Email", sha256cert2);

        GetSchemaResponse getSchemaResponse = new GetSchemaResponse.Builder()
                .setPubliclyVisibleSchema("Email1", packageIdentifier2)
                .setPubliclyVisibleSchema("Email1", packageIdentifier1)
                .build();
        assertThat(getSchemaResponse.getPubliclyVisibleSchemas().get("Email1"))
                .isEqualTo(packageIdentifier1);
    }

    // @exportToFramework:startStrip()
    // Not exported as setVisibilitySettingSupported is hidden in framework
    @Test
    public void testVisibility_publicVisibility_clearVisibility() {
        byte[] sha256cert1 = new byte[32];
        Arrays.fill(sha256cert1, (byte) 1);
        PackageIdentifier packageIdentifier1 = new PackageIdentifier("Email", sha256cert1);
        GetSchemaResponse getSchemaResponse = new GetSchemaResponse.Builder()
                .setPubliclyVisibleSchema("Email1", packageIdentifier1)
                // This should clear all visibility settings.
                .setVisibilitySettingSupported(true)
                .build();

        Map<String, PackageIdentifier> publiclyVisibleSchemas =
                getSchemaResponse.getPubliclyVisibleSchemas();
        assertThat(publiclyVisibleSchemas).isEmpty();
    }

    @Test
    public void testVisibility_publicVisibility_notSupported() {
        byte[] sha256cert1 = new byte[32];
        Arrays.fill(sha256cert1, (byte) 1);
        PackageIdentifier packageIdentifier1 = new PackageIdentifier("Email", sha256cert1);
        GetSchemaResponse getSchemaResponse = new GetSchemaResponse.Builder()
                .setPubliclyVisibleSchema("Email1", packageIdentifier1)
                .setVisibilitySettingSupported(false)
                .build();

        Exception e = assertThrows(UnsupportedOperationException.class,
                getSchemaResponse::getPubliclyVisibleSchemas);
        assertThat(e.getMessage()).isEqualTo("Get visibility setting is not supported with"
                + " this backend/Android API level combination.");
    }
    // @exportToFramework:endStrip()

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_ENABLE_ADDITIONAL_BUILDER_COPY_CONSTRUCTORS)
    public void testGetSchemaResponseBuilder_copyConstructor() {
        byte[] sha256cert1 = new byte[32];
        byte[] sha256cert2 = new byte[32];
        Arrays.fill(sha256cert1, (byte) 1);
        Arrays.fill(sha256cert2, (byte) 2);
        PackageIdentifier packageIdentifier1 = new PackageIdentifier("Email", sha256cert1);
        PackageIdentifier packageIdentifier2 = new PackageIdentifier("Email", sha256cert2);
        SchemaVisibilityConfig schemaVisibilityConfig =
                new SchemaVisibilityConfig.Builder().build();
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
        GetSchemaResponse response =
                new GetSchemaResponse.Builder().setVersion(42).addSchema(schema1).addSchema(schema2)
                        .addSchemaTypeNotDisplayedBySystem("Email1")
                        .addSchemaTypeNotDisplayedBySystem("Email2")
                        .setSchemaTypeVisibleToPackages("Email1",
                                ImmutableSet.of(packageIdentifier1))
                        .setSchemaTypeVisibleToPackages("Email2",
                                ImmutableSet.of(packageIdentifier2))
                        .setRequiredPermissionsForSchemaTypeVisibility("Email1",
                                ImmutableSet.of(
                                        ImmutableSet.of(SetSchemaRequest.READ_SMS,
                                                SetSchemaRequest.READ_CALENDAR),
                                        ImmutableSet.of(SetSchemaRequest.READ_HOME_APP_SEARCH_DATA))
                        )
                        .setRequiredPermissionsForSchemaTypeVisibility("Email2",
                                ImmutableSet.of(
                                        ImmutableSet.of(SetSchemaRequest.READ_CONTACTS,
                                                SetSchemaRequest.READ_EXTERNAL_STORAGE),
                                        ImmutableSet.of(SetSchemaRequest
                                                .READ_ASSISTANT_APP_SEARCH_DATA)))
                        .setPubliclyVisibleSchema("Email1", packageIdentifier1)
                        .setPubliclyVisibleSchema("Email2", packageIdentifier2)
                        .setSchemaTypeVisibleToConfigs("Email1",
                                ImmutableSet.of(schemaVisibilityConfig))
                        .setSchemaTypeVisibleToConfigs("Email2",
                                ImmutableSet.of(schemaVisibilityConfig))
                        .build();
        GetSchemaResponse responseCopy = new GetSchemaResponse.Builder(response).build();
        assertThat(responseCopy.getVersion()).isEqualTo(response.getVersion());
        assertThat(responseCopy.getSchemas()).isEqualTo(response.getSchemas());
        assertThat(responseCopy.getPubliclyVisibleSchemas()).isEqualTo(
                response.getPubliclyVisibleSchemas());
        assertThat(responseCopy.getRequiredPermissionsForSchemaTypeVisibility()).isEqualTo(
                response.getRequiredPermissionsForSchemaTypeVisibility());
        assertThat(responseCopy.getSchemaTypesVisibleToConfigs()).isEqualTo(
                response.getSchemaTypesVisibleToConfigs());
        assertThat(responseCopy.getSchemaTypesVisibleToPackages()).isEqualTo(
                response.getSchemaTypesVisibleToPackages());
        assertThat(responseCopy.getSchemaTypesNotDisplayedBySystem()).isEqualTo(
                response.getSchemaTypesNotDisplayedBySystem());
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_ENABLE_ADDITIONAL_BUILDER_COPY_CONSTRUCTORS)
    public void testGetSchemaResponseBuilder_clearSchemas() {
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
        GetSchemaResponse response = new GetSchemaResponse.Builder()
                .addSchema(schema1)
                .addSchema(schema2)
                .clearSchemas()
                .build();
        assertThat(response.getSchemas()).isEmpty();
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_ENABLE_ADDITIONAL_BUILDER_COPY_CONSTRUCTORS)
    public void testGetSchemaResponseBuilder_clearSchemaTypeVisibilityConfig() {
        byte[] sha256cert1 = new byte[32];
        byte[] sha256cert2 = new byte[32];
        Arrays.fill(sha256cert1, (byte) 1);
        Arrays.fill(sha256cert2, (byte) 2);
        PackageIdentifier packageIdentifier1 = new PackageIdentifier("Email", sha256cert1);
        PackageIdentifier packageIdentifier2 = new PackageIdentifier("Email", sha256cert2);
        SchemaVisibilityConfig schemaVisibilityConfig =
                new SchemaVisibilityConfig.Builder().build();
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
        GetSchemaResponse response =
                new GetSchemaResponse.Builder().setVersion(42).addSchema(schema1).addSchema(schema2)
                        .addSchemaTypeNotDisplayedBySystem("Email1")
                        .addSchemaTypeNotDisplayedBySystem("Email2")
                        .setSchemaTypeVisibleToPackages("Email1",
                                ImmutableSet.of(packageIdentifier1))
                        .setSchemaTypeVisibleToPackages("Email2",
                                ImmutableSet.of(packageIdentifier2))
                        .setRequiredPermissionsForSchemaTypeVisibility("Email1",
                                ImmutableSet.of(
                                        ImmutableSet.of(SetSchemaRequest.READ_SMS,
                                                SetSchemaRequest.READ_CALENDAR),
                                        ImmutableSet.of(SetSchemaRequest.READ_HOME_APP_SEARCH_DATA))
                        )
                        .setRequiredPermissionsForSchemaTypeVisibility("Email2",
                                ImmutableSet.of(
                                        ImmutableSet.of(SetSchemaRequest.READ_CONTACTS,
                                                SetSchemaRequest.READ_EXTERNAL_STORAGE),
                                        ImmutableSet.of(SetSchemaRequest
                                                .READ_ASSISTANT_APP_SEARCH_DATA)))
                        .setPubliclyVisibleSchema("Email1", packageIdentifier1)
                        .setPubliclyVisibleSchema("Email2", packageIdentifier2)
                        .setSchemaTypeVisibleToConfigs("Email1",
                                ImmutableSet.of(schemaVisibilityConfig))
                        .setSchemaTypeVisibleToConfigs("Email2",
                                ImmutableSet.of(schemaVisibilityConfig))
                        .clearSchemaTypeVisibilityConfig("Email1")
                        .build();
        assertThat(response.getSchemaTypesNotDisplayedBySystem()).containsExactly("Email2");
        assertThat(response.getSchemaTypesVisibleToPackages()).containsExactly("Email2",
                ImmutableSet.of(packageIdentifier2));
        assertThat(response.getRequiredPermissionsForSchemaTypeVisibility()).containsExactly(
                "Email2", ImmutableSet.of(
                        ImmutableSet.of(SetSchemaRequest.READ_CONTACTS,
                                SetSchemaRequest.READ_EXTERNAL_STORAGE),
                        ImmutableSet.of(SetSchemaRequest
                                .READ_ASSISTANT_APP_SEARCH_DATA)));
        assertThat(response.getPubliclyVisibleSchemas()).containsExactly("Email2",
                packageIdentifier2);
        assertThat(response.getSchemaTypesVisibleToConfigs()).containsExactly("Email2",
                ImmutableSet.of(schemaVisibilityConfig));
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_ENABLE_ADDITIONAL_BUILDER_COPY_CONSTRUCTORS)
    public void testGetSchemaResponseBuilder_clearSchemaTypeVisibilityConfigs() {
        byte[] sha256cert1 = new byte[32];
        byte[] sha256cert2 = new byte[32];
        Arrays.fill(sha256cert1, (byte) 1);
        Arrays.fill(sha256cert2, (byte) 2);
        PackageIdentifier packageIdentifier1 = new PackageIdentifier("Email", sha256cert1);
        PackageIdentifier packageIdentifier2 = new PackageIdentifier("Email", sha256cert2);
        SchemaVisibilityConfig schemaVisibilityConfig =
                new SchemaVisibilityConfig.Builder().build();
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
        GetSchemaResponse response =
                new GetSchemaResponse.Builder().setVersion(42).addSchema(schema1).addSchema(schema2)
                        .addSchemaTypeNotDisplayedBySystem("Email1")
                        .addSchemaTypeNotDisplayedBySystem("Email2")
                        .setSchemaTypeVisibleToPackages("Email1",
                                ImmutableSet.of(packageIdentifier1))
                        .setSchemaTypeVisibleToPackages("Email2",
                                ImmutableSet.of(packageIdentifier2))
                        .setRequiredPermissionsForSchemaTypeVisibility("Email1",
                                ImmutableSet.of(
                                        ImmutableSet.of(SetSchemaRequest.READ_SMS,
                                                SetSchemaRequest.READ_CALENDAR),
                                        ImmutableSet.of(SetSchemaRequest.READ_HOME_APP_SEARCH_DATA))
                        )
                        .setRequiredPermissionsForSchemaTypeVisibility("Email2",
                                ImmutableSet.of(
                                        ImmutableSet.of(SetSchemaRequest.READ_CONTACTS,
                                                SetSchemaRequest.READ_EXTERNAL_STORAGE),
                                        ImmutableSet.of(SetSchemaRequest
                                                .READ_ASSISTANT_APP_SEARCH_DATA)))
                        .setPubliclyVisibleSchema("Email1", packageIdentifier1)
                        .setPubliclyVisibleSchema("Email2", packageIdentifier2)
                        .setSchemaTypeVisibleToConfigs("Email1",
                                ImmutableSet.of(schemaVisibilityConfig))
                        .setSchemaTypeVisibleToConfigs("Email2",
                                ImmutableSet.of(schemaVisibilityConfig))
                        .clearSchemaTypeVisibilityConfig("Email1")
                        .clearSchemaTypeVisibilityConfigs()
                        .build();
        assertThat(response.getSchemaTypesNotDisplayedBySystem()).isEmpty();
        assertThat(response.getSchemaTypesVisibleToPackages()).isEmpty();
        assertThat(response.getRequiredPermissionsForSchemaTypeVisibility()).isEmpty();
        assertThat(response.getPubliclyVisibleSchemas()).isEmpty();
        assertThat(response.getSchemaTypesVisibleToConfigs()).isEmpty();
    }
}
