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

package androidx.appsearch.localstorage.visibilitystore;

import static com.google.common.truth.Truth.assertThat;

import androidx.appsearch.app.AppSearchSchema;
import androidx.appsearch.app.GenericDocument;
import androidx.appsearch.app.InternalVisibilityConfig;
import androidx.appsearch.app.PackageIdentifier;
import androidx.appsearch.app.SchemaVisibilityConfig;
import androidx.appsearch.app.SetSchemaRequest;
import androidx.appsearch.flags.Flags;
import androidx.appsearch.testutil.AppSearchTestUtils;
import androidx.appsearch.testutil.flags.RequiresFlagsDisabled;
import androidx.appsearch.testutil.flags.RequiresFlagsEnabled;

import com.google.android.appsearch.proto.AndroidVOverlayProto;
import com.google.android.appsearch.proto.PackageIdentifierProto;
import com.google.android.appsearch.proto.VisibilityConfigProto;
import com.google.android.appsearch.proto.VisibleToPackageWithTagsProto;
import com.google.android.appsearch.proto.VisibleToPermissionProto;
import com.google.android.icing.protobuf.ByteString;
import com.google.common.collect.ImmutableSet;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;

import java.util.Arrays;
import java.util.List;

public class VisibilityToDocumentConverterTest {

    @Rule
    public final RuleChain mRuleChain = AppSearchTestUtils.createCommonTestRules();

    @Test
    public void testToGenericDocuments() throws Exception {
        // Create a SetSchemaRequest for testing
        byte[] cert1 = new byte[32];
        byte[] cert2 = new byte[32];
        byte[] cert3 = new byte[32];
        byte[] cert4 = new byte[32];
        Arrays.fill(cert1, (byte) 1);
        Arrays.fill(cert2, (byte) 2);
        Arrays.fill(cert3, (byte) 3);
        Arrays.fill(cert4, (byte) 4);

        SchemaVisibilityConfig visibleToConfig = new SchemaVisibilityConfig.Builder()
                .addAllowedPackage(new PackageIdentifier("com.example.test1", cert1))
                .setPubliclyVisibleTargetPackage(
                        new PackageIdentifier("com.example.test2", cert2))
                .addRequiredPermissions(ImmutableSet.of(1, 2))
                .build();
        SetSchemaRequest setSchemaRequest = new SetSchemaRequest.Builder()
                .addSchemas(new AppSearchSchema.Builder("someSchema").build())
                .setSchemaTypeDisplayedBySystem("someSchema", false)
                .setSchemaTypeVisibilityForPackage("someSchema", true,
                        new PackageIdentifier("com.example.test3", cert3))
                .addRequiredPermissionsForSchemaTypeVisibility(
                        "someSchema", ImmutableSet.of(3, 4))
                .setPubliclyVisibleSchema("someSchema",
                        new PackageIdentifier("com.example.test4", cert4))
                .addSchemaTypeVisibleToConfig("someSchema", visibleToConfig)
                .build();

        // Create android V overlay proto
        VisibilityConfigProto visibleToConfigProto = VisibilityConfigProto.newBuilder()
                .addVisibleToPackages(PackageIdentifierProto.newBuilder()
                        .setPackageName("com.example.test1")
                        .setPackageSha256Cert(ByteString.copyFrom(cert1)).build())
                .setPubliclyVisibleTargetPackage(PackageIdentifierProto.newBuilder()
                                .setPackageName("com.example.test2")
                                .setPackageSha256Cert(ByteString.copyFrom(cert2)).build())
                .addVisibleToPermissions(VisibleToPermissionProto.newBuilder()
                        .addAllPermissions(ImmutableSet.of(1, 2)).build())
                .build();
        VisibilityConfigProto visibilityConfigProto = VisibilityConfigProto.newBuilder()
                .setPubliclyVisibleTargetPackage(PackageIdentifierProto.newBuilder()
                        .setPackageName("com.example.test4")
                        .setPackageSha256Cert(ByteString.copyFrom(cert4)).build())
                .build();
        AndroidVOverlayProto overlayProto = AndroidVOverlayProto.newBuilder()
                .setVisibilityConfig(visibilityConfigProto)
                .addVisibleToConfigs(visibleToConfigProto)
                .build();

        // Create the expected AndroidVOverlay document
        GenericDocument expectedAndroidVOverlay =
                new GenericDocument.Builder<GenericDocument.Builder<?>>("androidVOverlay",
                        "someSchema", "AndroidVOverlayType")
                        .setCreationTimestampMillis(0)
                        .setPropertyBytes("visibilityProtoSerializeProperty",
                                overlayProto.toByteArray())
                        .build();

        // Create the expected visibility document
        GenericDocument permissionDoc34 =
                new GenericDocument.Builder<GenericDocument.Builder<?>>("", "",
                        "VisibilityPermissionType")
                        .setCreationTimestampMillis(0)
                        .setPropertyLong("allRequiredPermissions", 3, 4).build();
        GenericDocument expectedVisibilityDocument =
                new GenericDocument.Builder<GenericDocument.Builder<?>>("", "someSchema",
                        "VisibilityType")
                        .setCreationTimestampMillis(0)
                        .setPropertyBoolean("notPlatformSurfaceable", true)
                        .setPropertyString("packageName", "com.example.test3")
                        .setPropertyBytes("sha256Cert", cert3)
                        .setPropertyDocument("permission", permissionDoc34)
                        .build();

        // Convert the SetSchemaRequest to a list of VisibilityConfig
        List<InternalVisibilityConfig> visibilityConfigs =
                InternalVisibilityConfig.toInternalVisibilityConfigs(setSchemaRequest);

        // Check if the conversion is correct
        assertThat(visibilityConfigs).hasSize(1);
        InternalVisibilityConfig visibilityConfig = visibilityConfigs.get(0);

        assertThat(expectedVisibilityDocument).isEqualTo(
                VisibilityToDocumentConverter.createVisibilityDocument(visibilityConfig));
        assertThat(expectedAndroidVOverlay).isEqualTo(
                VisibilityToDocumentConverter.createAndroidVOverlay(visibilityConfig));
    }

    @Test
    public void testToVisibilityConfig() throws Exception {
        byte[] cert1 = new byte[32];
        byte[] cert2 = new byte[32];
        byte[] cert3 = new byte[32];
        byte[] cert4 = new byte[32];
        Arrays.fill(cert1, (byte) 1);
        Arrays.fill(cert2, (byte) 2);
        Arrays.fill(cert3, (byte) 3);
        Arrays.fill(cert4, (byte) 4);

        // Create visibility proto property
        VisibilityConfigProto visibleToConfigProto = VisibilityConfigProto.newBuilder()
                .addVisibleToPackages(PackageIdentifierProto.newBuilder()
                        .setPackageName("com.example.test1")
                        .setPackageSha256Cert(ByteString.copyFrom(cert1)).build())
                .setPubliclyVisibleTargetPackage(PackageIdentifierProto.newBuilder()
                        .setPackageName("com.example.test2")
                        .setPackageSha256Cert(ByteString.copyFrom(cert2)).build())
                .addVisibleToPermissions(VisibleToPermissionProto.newBuilder()
                        .addAllPermissions(ImmutableSet.of(1, 2)).build())
                .build();
        VisibilityConfigProto visibilityConfigProto = VisibilityConfigProto.newBuilder()
                .setPubliclyVisibleTargetPackage(PackageIdentifierProto.newBuilder()
                        .setPackageName("com.example.test4")
                        .setPackageSha256Cert(ByteString.copyFrom(cert4)).build())
                .build();
        AndroidVOverlayProto overlayProto = AndroidVOverlayProto.newBuilder()
                .setVisibilityConfig(visibilityConfigProto)
                .addVisibleToConfigs(visibleToConfigProto)
                .build();

        // Create a visible config overlay for testing
        GenericDocument androidVOverlay =
                new GenericDocument.Builder<GenericDocument.Builder<?>>("androidVOverlay",
                        "someSchema", "AndroidVOverlayType")
                        .setCreationTimestampMillis(0)
                        .setPropertyBytes("visibilityProtoSerializeProperty",
                                overlayProto.toByteArray())
                        .build();

        // Create a VisibilityDocument for testing
        GenericDocument permissionDoc34 =
                new GenericDocument.Builder<GenericDocument.Builder<?>>("", "",
                        "VisibilityPermissionType")
                        .setCreationTimestampMillis(0)
                        .setPropertyLong("allRequiredPermissions", 3, 4).build();
        GenericDocument visibilityDoc =
                new GenericDocument.Builder<GenericDocument.Builder<?>>("", "someSchema",
                        "VisibilityType")
                        .setCreationTimestampMillis(0)
                        .setPropertyBoolean("notPlatformSurfaceable", true)
                        .setPropertyString("packageName", "com.example.test3")
                        .setPropertyBytes("sha256Cert", cert3)
                        .setPropertyDocument("permission", permissionDoc34)
                        .build();

        // Create a VisibilityConfig using the Builder
        InternalVisibilityConfig visibilityConfig =
                VisibilityToDocumentConverter.createInternalVisibilityConfig(
                        visibilityDoc, androidVOverlay);

        // Check if the properties are set correctly
        assertThat(visibilityDoc).isEqualTo(
                VisibilityToDocumentConverter.createVisibilityDocument(visibilityConfig));
        GenericDocument actualOverlayDoc =
                VisibilityToDocumentConverter.createAndroidVOverlay(visibilityConfig);
        AndroidVOverlayProto actualOverlayProto = AndroidVOverlayProto.parseFrom(
                actualOverlayDoc.getPropertyBytes("visibilityProtoSerializeProperty"));
        assertThat(actualOverlayProto).isEqualTo(overlayProto);
        assertThat(androidVOverlay).isEqualTo(actualOverlayDoc);

        // Verify rebuild from InternalVisibilityConfig remains the same.
        InternalVisibilityConfig.Builder builder =
                new InternalVisibilityConfig.Builder(visibilityConfig);
        InternalVisibilityConfig rebuild = builder.build();
        assertThat(visibilityConfig).isEqualTo(rebuild);

        InternalVisibilityConfig modifiedConfig = builder
                .setSchemaType("prefixedSchema")
                .setNotDisplayedBySystem(false)
                .addVisibleToPermissions(ImmutableSet.of(SetSchemaRequest.READ_SMS,
                        SetSchemaRequest.READ_CALENDAR))
                .clearVisibleToPackages()
                .addVisibleToPackage(
                        new PackageIdentifier("com.example.other", new byte[32]))
                .setPubliclyVisibleTargetPackage(
                        new PackageIdentifier("com.example.other", new byte[32]))
                .clearVisibleToConfig()
                .build();
        assertThat(modifiedConfig.getSchemaType()).isEqualTo("prefixedSchema");

        // Check that the rebuild stayed the same
        assertThat(rebuild.getSchemaType()).isEqualTo("someSchema");
        assertThat(rebuild.isNotDisplayedBySystem()).isTrue();
        assertThat(rebuild.getVisibilityConfig().getRequiredPermissions())
                .containsExactly(ImmutableSet.of(3, 4));
        assertThat(rebuild.getVisibilityConfig().getAllowedPackages())
                .containsExactly(new PackageIdentifier("com.example.test3", cert3));
        assertThat(
                rebuild.getVisibilityConfig().getPubliclyVisibleTargetPackage()).isEqualTo(
                new PackageIdentifier("com.example.test4", cert4));

        SchemaVisibilityConfig expectedVisibleToConfig = new SchemaVisibilityConfig.Builder()
                .addRequiredPermissions(ImmutableSet.of(1, 2))
                .addAllowedPackage(new PackageIdentifier("com.example.test1", cert1))
                .setPubliclyVisibleTargetPackage(new PackageIdentifier("com.example.test2", cert2))
                .build();
        assertThat(rebuild.getVisibleToConfigs()).containsExactly(expectedVisibleToConfig);
    }

    @Test
    public void testToGenericDocumentAndBack() {
        // Create a SetSchemaRequest for testing
        byte[] cert1 = new byte[32];
        byte[] cert2 = new byte[32];
        byte[] cert3 = new byte[32];
        byte[] cert4 = new byte[32];
        byte[] cert5 = new byte[32];
        byte[] cert6 = new byte[32];
        byte[] cert7 = new byte[32];
        Arrays.fill(cert1, (byte) 1);
        Arrays.fill(cert2, (byte) 2);
        Arrays.fill(cert3, (byte) 3);
        Arrays.fill(cert4, (byte) 4);
        Arrays.fill(cert5, (byte) 5);
        Arrays.fill(cert6, (byte) 6);
        Arrays.fill(cert7, (byte) 7);

        SchemaVisibilityConfig config1 = new SchemaVisibilityConfig.Builder()
                .addAllowedPackage(new PackageIdentifier("com.example.test1", cert1))
                .setPubliclyVisibleTargetPackage(
                        new PackageIdentifier("com.example.test2", cert2))
                .addRequiredPermissions(ImmutableSet.of(1, 2))
                .build();
        SchemaVisibilityConfig config2 = new SchemaVisibilityConfig.Builder()
                .addAllowedPackage(new PackageIdentifier("com.example.test3", cert3))
                .addRequiredPermissions(ImmutableSet.of(3, 4))
                .build();
        SchemaVisibilityConfig config3 = new SchemaVisibilityConfig.Builder()
                .addAllowedPackage(new PackageIdentifier("com.example.test4", cert4))
                .setPubliclyVisibleTargetPackage(
                        new PackageIdentifier("com.example.test5", cert5))
                .build();
        SetSchemaRequest setSchemaRequest = new SetSchemaRequest.Builder()
                .addSchemas(new AppSearchSchema.Builder("someSchema").build())
                .setSchemaTypeDisplayedBySystem("someSchema", /*displayed=*/ true)
                .setSchemaTypeVisibilityForPackage("someSchema", /*visible=*/ true,
                        new PackageIdentifier("com.example.test6", cert6))
                .addRequiredPermissionsForSchemaTypeVisibility("someSchema",
                        ImmutableSet.of(1, 2))
                .setPubliclyVisibleSchema("someSchema",
                        new PackageIdentifier("com.example.test7", cert7))
                .addSchemaTypeVisibleToConfig("someSchema", config1)
                .addSchemaTypeVisibleToConfig("someSchema", config2)
                .addSchemaTypeVisibleToConfig("someSchema", config3)
                .build();

        // Convert the SetSchemaRequest to a list of VisibilityConfig
        List<InternalVisibilityConfig> visibilityConfigs =
                InternalVisibilityConfig.toInternalVisibilityConfigs(setSchemaRequest);
        InternalVisibilityConfig visibilityConfig = visibilityConfigs.get(0);

        GenericDocument visibilityDoc =
                VisibilityToDocumentConverter.createVisibilityDocument(visibilityConfig);
        GenericDocument androidVOverlay =
                VisibilityToDocumentConverter.createAndroidVOverlay(visibilityConfig);

        InternalVisibilityConfig rebuild =
                VisibilityToDocumentConverter.createInternalVisibilityConfig(
                        visibilityDoc, androidVOverlay);

        assertThat(rebuild).isEqualTo(visibilityConfig);
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_ENABLE_APPS_INDEXER_PWA_MULTI_CERT)
    public void testToGenericDocuments_multiCert() throws Exception {
        byte[] cert1 = new byte[32];
        byte[] cert2 = new byte[32];
        Arrays.fill(cert1, (byte) 1);
        Arrays.fill(cert2, (byte) 2);

        PackageIdentifier multiCertPkg =
                new PackageIdentifier("com.example.multicert", List.of(cert1, cert2));

        SchemaVisibilityConfig config =
                new SchemaVisibilityConfig.Builder()
                        .addAllowedPackage(multiCertPkg)
                        .setPubliclyVisibleTargetPackage(multiCertPkg)
                        .build();

        SetSchemaRequest setSchemaRequest =
                new SetSchemaRequest.Builder()
                        .addSchemas(new AppSearchSchema.Builder("multiCertSchema").build())
                        .setPubliclyVisibleSchema("multiCertSchema", multiCertPkg)
                        .addSchemaTypeVisibleToConfig("multiCertSchema", config)
                        .build();

        List<InternalVisibilityConfig> visibilityConfigs =
                InternalVisibilityConfig.toInternalVisibilityConfigs(setSchemaRequest);
        InternalVisibilityConfig visibilityConfig = visibilityConfigs.get(0);

        GenericDocument visibilityDoc =
                VisibilityToDocumentConverter.createVisibilityDocument(visibilityConfig);
        GenericDocument androidVOverlay =
                VisibilityToDocumentConverter.createAndroidVOverlay(visibilityConfig);

        InternalVisibilityConfig rebuild =
                VisibilityToDocumentConverter.createInternalVisibilityConfig(
                        visibilityDoc, androidVOverlay);

        assertThat(rebuild).isEqualTo(visibilityConfig);
        List<byte[]> rebuildPublicCerts =
                rebuild.getVisibilityConfig()
                        .getPubliclyVisibleTargetPackage()
                        .getMultiSignerSha256Certificates();
        assertThat(rebuildPublicCerts).hasSize(2);
        assertThat(rebuildPublicCerts.get(0)).isEqualTo(cert1);
        assertThat(rebuildPublicCerts.get(1)).isEqualTo(cert2);
        SchemaVisibilityConfig rebuildVisibleToConfig =
                rebuild.getVisibleToConfigs().iterator().next();
        List<byte[]> rebuildVisibleToCerts =
                rebuildVisibleToConfig
                        .getPubliclyVisibleTargetPackage()
                        .getMultiSignerSha256Certificates();
        assertThat(rebuildVisibleToCerts).hasSize(2);
        assertThat(rebuildVisibleToCerts.get(0)).isEqualTo(cert1);
        assertThat(rebuildVisibleToCerts.get(1)).isEqualTo(cert2);
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_ENABLE_APPS_INDEXER_PWA_MULTI_CERT)
    public void testToGenericDocuments_singleCert_rebuildsAsSingleSigner() throws Exception {
        byte[] cert = new byte[32];
        Arrays.fill(cert, (byte) 1);

        PackageIdentifier singleCertPkg =
                new PackageIdentifier("com.example.singlecert", cert);

        SchemaVisibilityConfig config =
                new SchemaVisibilityConfig.Builder()
                        .addAllowedPackage(singleCertPkg)
                        .setPubliclyVisibleTargetPackage(singleCertPkg)
                        .build();

        SetSchemaRequest setSchemaRequest =
                new SetSchemaRequest.Builder()
                        .addSchemas(new AppSearchSchema.Builder("singleCertSchema").build())
                        .setPubliclyVisibleSchema("singleCertSchema", singleCertPkg)
                        .addSchemaTypeVisibleToConfig("singleCertSchema", config)
                        .build();

        List<InternalVisibilityConfig> visibilityConfigs =
                InternalVisibilityConfig.toInternalVisibilityConfigs(setSchemaRequest);
        InternalVisibilityConfig visibilityConfig = visibilityConfigs.get(0);

        GenericDocument visibilityDoc =
                VisibilityToDocumentConverter.createVisibilityDocument(visibilityConfig);
        GenericDocument androidVOverlay =
                VisibilityToDocumentConverter.createAndroidVOverlay(visibilityConfig);

        InternalVisibilityConfig rebuild =
                VisibilityToDocumentConverter.createInternalVisibilityConfig(
                        visibilityDoc, androidVOverlay);

        assertThat(rebuild).isEqualTo(visibilityConfig);
        PackageIdentifier rebuildPublicPkg =
                rebuild.getVisibilityConfig().getPubliclyVisibleTargetPackage();
        assertThat(rebuildPublicPkg.getSha256Certificate()).isEqualTo(cert);
        assertThat(rebuildPublicPkg.getMultiSignerSha256Certificates()).isEmpty();
    }

    @Test
    public void testWriterUid_overlaySerializationAndDeserialization() throws Exception {
        int testUid = 12345;
        InternalVisibilityConfig visibilityConfig =
                new InternalVisibilityConfig.Builder("someSchema")
                        .setWriterUid(testUid)
                        .build();

        GenericDocument visibilityDoc =
                VisibilityToDocumentConverter.createVisibilityDocument(visibilityConfig);
        GenericDocument androidVOverlayDoc =
                VisibilityToDocumentConverter.createAndroidVOverlay(visibilityConfig);

        assertThat(androidVOverlayDoc).isNotNull();

        InternalVisibilityConfig convertedConfig =
                VisibilityToDocumentConverter.createInternalVisibilityConfig(
                        visibilityDoc, androidVOverlayDoc);

        assertThat(convertedConfig.getWriterUid()).isEqualTo(testUid);
        assertThat(convertedConfig).isEqualTo(visibilityConfig);
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_ENABLE_SCHEMA_VISIBLE_TO_PACKAGE_WITH_TAGS)
    public void testToGenericDocuments_packagesWithTags() throws Exception {
        byte[] cert1 = new byte[32];
        Arrays.fill(cert1, (byte) 1);

        // The schema is visible to "tagA" and "tagB" of package1 which also hold permissions 1
        // and 2.
        SchemaVisibilityConfig visibleToConfig = new SchemaVisibilityConfig.Builder()
                .setAllowedPackageWithTags(new PackageIdentifier("com.example.test1", cert1),
                        ImmutableSet.of("tagA", "tagB"))
                .addRequiredPermissions(ImmutableSet.of(1, 2))
                .build();
        InternalVisibilityConfig visibilityConfig =
                new InternalVisibilityConfig.Builder("someSchema")
                        .addVisibleToConfig(visibleToConfig)
                        .build();

        // A package which is restricted by attribution tag must not be stored as a package which
        // has access regardless of the attribution tag.
        assertThat(VisibilityToDocumentConverter.createVisibilityDocument(visibilityConfig))
                .isEqualTo(VisibilityToDocumentConverter.createVisibilityDocument(
                        new InternalVisibilityConfig.Builder("someSchema").build()));

        GenericDocument overlayDoc =
                VisibilityToDocumentConverter.createAndroidVOverlay(visibilityConfig);
        assertThat(overlayDoc).isNotNull();
        AndroidVOverlayProto overlayProto = AndroidVOverlayProto.parseFrom(
                overlayDoc.getPropertyBytes("visibilityProtoSerializeProperty"));

        assertThat(overlayProto.getVisibilityConfig())
                .isEqualTo(VisibilityConfigProto.getDefaultInstance());

        assertThat(overlayProto.getVisibleToConfigsList()).hasSize(1);
        VisibilityConfigProto visibleToConfigProto = overlayProto.getVisibleToConfigs(0);
        // The only package is the placeholder, which no caller can match.
        assertThat(visibleToConfigProto.getVisibleToPackagesList()).containsExactly(
                PackageIdentifierProto.newBuilder()
                        .setPackageName(
                                VisibilityToDocumentConverter
                                        .PACKAGES_WITH_TAGS_PLACEHOLDER_PACKAGE_NAME)
                        .setPackageSha256Cert(ByteString.copyFrom(new byte[32])).build());
        assertThat(visibleToConfigProto.getVisibleToPermissionsList()).containsExactly(
                VisibleToPermissionProto.newBuilder()
                        .addAllPermissions(ImmutableSet.of(1, 2)).build());
        assertThat(visibleToConfigProto.getVisibleToPackagesWithTagsList()).hasSize(1);
        VisibleToPackageWithTagsProto visibleToPackageWithTagsProto =
                visibleToConfigProto.getVisibleToPackagesWithTags(0);
        assertThat(visibleToPackageWithTagsProto.getPackageIdentifier()).isEqualTo(
                PackageIdentifierProto.newBuilder()
                        .setPackageName("com.example.test1")
                        .setPackageSha256Cert(ByteString.copyFrom(cert1)).build());
        assertThat(visibleToPackageWithTagsProto.getAttributionTagsList())
                .containsExactly("tagA", "tagB");
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_ENABLE_SCHEMA_VISIBLE_TO_PACKAGE_WITH_TAGS)
    public void testToGenericDocumentAndBack_packagesWithTags() {
        byte[] cert1 = new byte[32];
        byte[] cert2 = new byte[32];
        byte[] cert3 = new byte[32];
        byte[] cert4 = new byte[32];
        Arrays.fill(cert1, (byte) 1);
        Arrays.fill(cert2, (byte) 2);
        Arrays.fill(cert3, (byte) 3);
        Arrays.fill(cert4, (byte) 4);
        PackageIdentifier package1 = new PackageIdentifier("com.example.test1", cert1);
        PackageIdentifier package2 = new PackageIdentifier("com.example.test2", cert2);
        PackageIdentifier package3 = new PackageIdentifier("com.example.test3", cert3);
        PackageIdentifier package4 = new PackageIdentifier("com.example.test4", cert4);

        SchemaVisibilityConfig packagesOnlyConfig = new SchemaVisibilityConfig.Builder()
                .addAllowedPackage(package3)
                .build();
        SchemaVisibilityConfig tagsOnlyConfig = new SchemaVisibilityConfig.Builder()
                .setAllowedPackageWithTags(package1, ImmutableSet.of("tagA", "tagB"))
                .build();
        SchemaVisibilityConfig tagsAndPermissionsConfig = new SchemaVisibilityConfig.Builder()
                .setAllowedPackageWithTags(package2, ImmutableSet.of("tagC"))
                .addRequiredPermissions(ImmutableSet.of(1, 2))
                .build();
        InternalVisibilityConfig visibilityConfig =
                new InternalVisibilityConfig.Builder("someSchema")
                        .addVisibleToPackage(package4)
                        .setNotDisplayedBySystem(true)
                        .addVisibleToConfig(packagesOnlyConfig)
                        .addVisibleToConfig(tagsOnlyConfig)
                        .addVisibleToConfig(tagsAndPermissionsConfig)
                        .build();

        GenericDocument visibilityDoc =
                VisibilityToDocumentConverter.createVisibilityDocument(visibilityConfig);
        GenericDocument androidVOverlay =
                VisibilityToDocumentConverter.createAndroidVOverlay(visibilityConfig);

        InternalVisibilityConfig rebuild =
                VisibilityToDocumentConverter.createInternalVisibilityConfig(
                        visibilityDoc, androidVOverlay);

        // InternalVisibilityConfig#equals depends on the order of the visible to configs, which
        // may change when they are stored, so compare each setting.
        assertThat(rebuild.getSchemaType()).isEqualTo("someSchema");
        assertThat(rebuild.isNotDisplayedBySystem()).isTrue();
        assertThat(rebuild.getVisibilityConfig()).isEqualTo(visibilityConfig.getVisibilityConfig());
        assertThat(rebuild.getVisibilityConfig().getAllowedPackages()).containsExactly(package4);
        assertThat(rebuild.getVisibilityConfig().getAllowedPackagesWithTags()).isEmpty();
        assertThat(rebuild.getVisibleToConfigs())
                .containsExactly(packagesOnlyConfig, tagsOnlyConfig, tagsAndPermissionsConfig);
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_ENABLE_SCHEMA_VISIBLE_TO_PACKAGE_WITH_TAGS)
    public void testDoesConfigContainAndroidVOverlay_packagesWithTags() {
        InternalVisibilityConfig configWithoutOverlay =
                new InternalVisibilityConfig.Builder("someSchema")
                        .addVisibleToPackage(
                                new PackageIdentifier("com.example.test", new byte[32]))
                        .build();
        assertThat(VisibilityToDocumentConverter
                .doesConfigContainAndroidVOverlay(configWithoutOverlay)).isFalse();
        assertThat(VisibilityToDocumentConverter.createAndroidVOverlay(configWithoutOverlay))
                .isNull();

        SchemaVisibilityConfig packageWithTagsConfig = new SchemaVisibilityConfig.Builder()
                .setAllowedPackageWithTags(
                        new PackageIdentifier("com.example.test", new byte[32]),
                        ImmutableSet.of("tagA"))
                .build();

        // Packages which are restricted by attribution tag are stored in a visible to config.
        InternalVisibilityConfig configWithTags =
                new InternalVisibilityConfig.Builder("someSchema")
                        .addVisibleToConfig(packageWithTagsConfig)
                        .build();
        assertThat(VisibilityToDocumentConverter
                .doesConfigContainAndroidVOverlay(configWithTags)).isTrue();
        assertThat(VisibilityToDocumentConverter.createAndroidVOverlay(configWithTags))
                .isNotNull();

        // They are only supported in a visible to config: the API cannot set them on the
        // visibility config of a schema, where they are not stored.
        InternalVisibilityConfig configWithTagsInVisibilityConfig =
                new InternalVisibilityConfig.Builder("someSchema")
                        .setVisibilityConfig(packageWithTagsConfig)
                        .build();
        assertThat(VisibilityToDocumentConverter
                .doesConfigContainAndroidVOverlay(configWithTagsInVisibilityConfig)).isFalse();
        assertThat(VisibilityToDocumentConverter
                .createAndroidVOverlay(configWithTagsInVisibilityConfig)).isNull();
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_ENABLE_SCHEMA_VISIBLE_TO_PACKAGE_WITH_TAGS)
    public void testToVisibilityConfig_packageWithoutTags() throws Exception {
        byte[] cert1 = new byte[32];
        byte[] cert2 = new byte[32];
        Arrays.fill(cert1, (byte) 1);
        Arrays.fill(cert2, (byte) 2);
        PackageIdentifierProto package1Proto = PackageIdentifierProto.newBuilder()
                .setPackageName("com.example.test1")
                .setPackageSha256Cert(ByteString.copyFrom(cert1)).build();
        PackageIdentifierProto package2Proto = PackageIdentifierProto.newBuilder()
                .setPackageName("com.example.test2")
                .setPackageSha256Cert(ByteString.copyFrom(cert2)).build();
        VisibleToPackageWithTagsProto package1WithTagsProto =
                VisibleToPackageWithTagsProto.newBuilder()
                        .setPackageIdentifier(package1Proto)
                        .addAttributionTags("tagA")
                        .build();
        // A package which is restricted to no attribution tags cannot be created through the API.
        VisibleToPackageWithTagsProto package2WithoutTagsProto =
                VisibleToPackageWithTagsProto.newBuilder()
                        .setPackageIdentifier(package2Proto)
                        .build();

        AndroidVOverlayProto overlayProto = AndroidVOverlayProto.newBuilder()
                .setVisibilityConfig(VisibilityConfigProto.newBuilder()
                        .addVisibleToPackagesWithTags(package1WithTagsProto)
                        .addVisibleToPackagesWithTags(package2WithoutTagsProto)
                        .build())
                // A valid config.
                .addVisibleToConfigs(VisibilityConfigProto.newBuilder()
                        .addVisibleToPackagesWithTags(package1WithTagsProto)
                        .addVisibleToPermissions(VisibleToPermissionProto.newBuilder()
                                .addAllPermissions(ImmutableSet.of(1)).build())
                        .build())
                // A config which would only require permission 2 if the package without
                // attribution tags was ignored on its own.
                .addVisibleToConfigs(VisibilityConfigProto.newBuilder()
                        .addVisibleToPackagesWithTags(package2WithoutTagsProto)
                        .addVisibleToPermissions(VisibleToPermissionProto.newBuilder()
                                .addAllPermissions(ImmutableSet.of(2)).build())
                        .build())
                // A config which has both a valid and an invalid package.
                .addVisibleToConfigs(VisibilityConfigProto.newBuilder()
                        .addVisibleToPackagesWithTags(package1WithTagsProto)
                        .addVisibleToPackagesWithTags(package2WithoutTagsProto)
                        .addVisibleToPermissions(VisibleToPermissionProto.newBuilder()
                                .addAllPermissions(ImmutableSet.of(3)).build())
                        .build())
                // A config whose only package that is restricted by attribution tag is invalid.
                .addVisibleToConfigs(VisibilityConfigProto.newBuilder()
                        .addVisibleToPackages(package1Proto)
                        .addVisibleToPackagesWithTags(package2WithoutTagsProto)
                        .addVisibleToPermissions(VisibleToPermissionProto.newBuilder()
                                .addAllPermissions(ImmutableSet.of(4)).build())
                        .build())
                .build();
        GenericDocument androidVOverlay =
                new GenericDocument.Builder<GenericDocument.Builder<?>>("androidVOverlay",
                        "someSchema", "AndroidVOverlayType")
                        .setCreationTimestampMillis(0)
                        .setPropertyBytes("visibilityProtoSerializeProperty",
                                overlayProto.toByteArray())
                        .build();
        GenericDocument visibilityDoc =
                new GenericDocument.Builder<GenericDocument.Builder<?>>("", "someSchema",
                        "VisibilityType")
                        .setCreationTimestampMillis(0)
                        .build();

        InternalVisibilityConfig visibilityConfig =
                VisibilityToDocumentConverter.createInternalVisibilityConfig(
                        visibilityDoc, androidVOverlay);

        // The package without attribution tags doesn't grant access to anyone.
        PackageIdentifier package1 = new PackageIdentifier("com.example.test1", cert1);
        // Packages restricted by attribution tag are not read for the visibility config of a
        // schema, only for its visible to configs.
        assertThat(visibilityConfig.getVisibilityConfig().getAllowedPackages()).isEmpty();
        assertThat(visibilityConfig.getVisibilityConfig().getAllowedPackagesWithTags()).isEmpty();
        // The invalid package is ignored in the config which also has a valid package. The
        // configs which would have no package restricted by attribution tag left are dropped, so
        // that a config is never widened.
        assertThat(visibilityConfig.getVisibleToConfigs()).containsExactly(
                new SchemaVisibilityConfig.Builder()
                        .setAllowedPackageWithTags(package1, ImmutableSet.of("tagA"))
                        .addRequiredPermissions(ImmutableSet.of(1))
                        .build(),
                new SchemaVisibilityConfig.Builder()
                        .setAllowedPackageWithTags(package1, ImmutableSet.of("tagA"))
                        .addRequiredPermissions(ImmutableSet.of(3))
                        .build());
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_ENABLE_SCHEMA_VISIBLE_TO_PACKAGE_WITH_TAGS)
    public void testToVisibilityConfig_packagesAndPackagesWithTags() throws Exception {
        byte[] cert1 = new byte[32];
        byte[] cert2 = new byte[32];
        byte[] cert3 = new byte[32];
        Arrays.fill(cert1, (byte) 1);
        Arrays.fill(cert2, (byte) 2);
        Arrays.fill(cert3, (byte) 3);
        PackageIdentifierProto package1Proto = PackageIdentifierProto.newBuilder()
                .setPackageName("com.example.test1")
                .setPackageSha256Cert(ByteString.copyFrom(cert1)).build();
        VisibleToPackageWithTagsProto package2WithTagsProto =
                VisibleToPackageWithTagsProto.newBuilder()
                        .setPackageIdentifier(PackageIdentifierProto.newBuilder()
                                .setPackageName("com.example.test2")
                                .setPackageSha256Cert(ByteString.copyFrom(cert2)).build())
                        .addAttributionTags("tagA")
                        .build();

        // A config which has both packages and packages that are restricted by attribution tag
        // cannot be created through the API.
        AndroidVOverlayProto overlayProto = AndroidVOverlayProto.newBuilder()
                .setVisibilityConfig(VisibilityConfigProto.newBuilder()
                        .addVisibleToPackagesWithTags(package2WithTagsProto)
                        .build())
                // A valid config.
                .addVisibleToConfigs(VisibilityConfigProto.newBuilder()
                        .addVisibleToPackagesWithTags(package2WithTagsProto)
                        .addVisibleToPermissions(VisibleToPermissionProto.newBuilder()
                                .addAllPermissions(ImmutableSet.of(1)).build())
                        .build())
                // A config which has both.
                .addVisibleToConfigs(VisibilityConfigProto.newBuilder()
                        .addVisibleToPackages(package1Proto)
                        .addVisibleToPackagesWithTags(package2WithTagsProto)
                        .addVisibleToPermissions(VisibleToPermissionProto.newBuilder()
                                .addAllPermissions(ImmutableSet.of(2)).build())
                        .build())
                .build();
        GenericDocument androidVOverlay =
                new GenericDocument.Builder<GenericDocument.Builder<?>>("androidVOverlay",
                        "someSchema", "AndroidVOverlayType")
                        .setCreationTimestampMillis(0)
                        .setPropertyBytes("visibilityProtoSerializeProperty",
                                overlayProto.toByteArray())
                        .build();
        // The visibility document has a package, while the overlay has a package that is
        // restricted by attribution tag for the same config.
        GenericDocument visibilityDoc =
                new GenericDocument.Builder<GenericDocument.Builder<?>>("", "someSchema",
                        "VisibilityType")
                        .setCreationTimestampMillis(0)
                        .setPropertyString("packageName", "com.example.test3")
                        .setPropertyBytes("sha256Cert", cert3)
                        .build();

        // The stored settings are read without an exception.
        InternalVisibilityConfig visibilityConfig =
                VisibilityToDocumentConverter.createInternalVisibilityConfig(
                        visibilityDoc, androidVOverlay);

        // The package that is restricted by attribution tag is ignored, which can only narrow
        // access.
        assertThat(visibilityConfig.getVisibilityConfig().getAllowedPackages())
                .containsExactly(new PackageIdentifier("com.example.test3", cert3));
        assertThat(visibilityConfig.getVisibilityConfig().getAllowedPackagesWithTags()).isEmpty();
        // Only the valid config is kept.
        assertThat(visibilityConfig.getVisibleToConfigs()).containsExactly(
                new SchemaVisibilityConfig.Builder()
                        .setAllowedPackageWithTags(
                                new PackageIdentifier("com.example.test2", cert2),
                                ImmutableSet.of("tagA"))
                        .addRequiredPermissions(ImmutableSet.of(1))
                        .build());
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_ENABLE_SCHEMA_VISIBLE_TO_PACKAGE_WITH_TAGS)
    public void testPackagesWithTagsPlaceholder() throws Exception {
        byte[] cert1 = new byte[32];
        Arrays.fill(cert1, (byte) 1);
        PackageIdentifier package1 = new PackageIdentifier("com.example.test1", cert1);
        SchemaVisibilityConfig packageWithTagsConfig = new SchemaVisibilityConfig.Builder()
                .setAllowedPackageWithTags(package1, ImmutableSet.of("tagA"))
                .addRequiredPermissions(ImmutableSet.of(1, 2))
                .build();
        SchemaVisibilityConfig packageConfig = new SchemaVisibilityConfig.Builder()
                .addAllowedPackage(package1)
                .addRequiredPermissions(ImmutableSet.of(3))
                .build();

        // A config which has packages restricted by attribution tag is stored with the
        // placeholder package. Other configs are not. A version of AppSearch that predates
        // packages restricted by attribution tag ignores them, and must not be left with only
        // the permissions of the config: the placeholder is then the package that has access,
        // and no caller can match it.
        VisibilityConfigProto packageWithTagsProto =
                VisibilityToDocumentConverter.convertSchemaVisibilityConfigToProto(
                        packageWithTagsConfig);
        assertThat(packageWithTagsProto.getVisibleToPackagesCount()).isEqualTo(1);
        assertThat(packageWithTagsProto.getVisibleToPackages(0).getPackageName()).isEqualTo(
                VisibilityToDocumentConverter.PACKAGES_WITH_TAGS_PLACEHOLDER_PACKAGE_NAME);
        VisibilityConfigProto packageProto =
                VisibilityToDocumentConverter.convertSchemaVisibilityConfigToProto(packageConfig);
        assertThat(packageProto.getVisibleToPackagesCount()).isEqualTo(1);
        assertThat(packageProto.getVisibleToPackages(0).getPackageName())
                .isEqualTo("com.example.test1");

        // The placeholder is removed when the config is read.
        assertThat(VisibilityToDocumentConverter.convertVisibilityConfigFromProto(
                packageWithTagsProto)).isEqualTo(packageWithTagsConfig);
        InternalVisibilityConfig visibilityConfig =
                new InternalVisibilityConfig.Builder("someSchema")
                        .addVisibleToConfig(packageWithTagsConfig)
                        .addVisibleToConfig(packageConfig)
                        .build();
        InternalVisibilityConfig rebuild =
                VisibilityToDocumentConverter.createInternalVisibilityConfig(
                        VisibilityToDocumentConverter.createVisibilityDocument(visibilityConfig),
                        VisibilityToDocumentConverter.createAndroidVOverlay(visibilityConfig));
        assertThat(rebuild.getVisibleToConfigs())
                .containsExactly(packageWithTagsConfig, packageConfig);
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_ENABLE_SCHEMA_VISIBLE_TO_PACKAGE_WITH_TAGS)
    public void testPackagesWithTagsPlaceholder_withoutPackagesWithTags() throws Exception {
        byte[] cert1 = new byte[32];
        Arrays.fill(cert1, (byte) 1);
        PackageIdentifier package1 = new PackageIdentifier("com.example.test1", cert1);
        PackageIdentifierProto package1Proto = PackageIdentifierProto.newBuilder()
                .setPackageName("com.example.test1")
                .setPackageSha256Cert(ByteString.copyFrom(cert1)).build();
        PackageIdentifierProto placeholderProto = PackageIdentifierProto.newBuilder()
                .setPackageName(
                        VisibilityToDocumentConverter.PACKAGES_WITH_TAGS_PLACEHOLDER_PACKAGE_NAME)
                .setPackageSha256Cert(ByteString.copyFrom(new byte[32])).build();
        SchemaVisibilityConfig packageWithTagsConfig = new SchemaVisibilityConfig.Builder()
                .setAllowedPackageWithTags(package1, ImmutableSet.of("tagA"))
                .addRequiredPermissions(ImmutableSet.of(3))
                .build();

        // A version of AppSearch that predates packages restricted by attribution tag returns
        // the placeholder to callers as a package. When a caller sets such a config back, it is
        // stored with the placeholder, but without packages restricted by attribution tag.
        AndroidVOverlayProto overlayProto = AndroidVOverlayProto.newBuilder()
                // A config which would only require permission 1 without the placeholder.
                .addVisibleToConfigs(VisibilityConfigProto.newBuilder()
                        .addVisibleToPackages(placeholderProto)
                        .addVisibleToPermissions(VisibleToPermissionProto.newBuilder()
                                .addAllPermissions(ImmutableSet.of(1)).build())
                        .build())
                // A config to which the caller added a package before it set the config back.
                .addVisibleToConfigs(VisibilityConfigProto.newBuilder()
                        .addVisibleToPackages(placeholderProto)
                        .addVisibleToPackages(package1Proto)
                        .addVisibleToPermissions(VisibleToPermissionProto.newBuilder()
                                .addAllPermissions(ImmutableSet.of(2)).build())
                        .build())
                // A config which was stored by this version.
                .addVisibleToConfigs(
                        VisibilityToDocumentConverter.convertSchemaVisibilityConfigToProto(
                                packageWithTagsConfig))
                .build();
        GenericDocument androidVOverlay =
                new GenericDocument.Builder<GenericDocument.Builder<?>>("androidVOverlay",
                        "someSchema", "AndroidVOverlayType")
                        .setCreationTimestampMillis(0)
                        .setPropertyBytes("visibilityProtoSerializeProperty",
                                overlayProto.toByteArray())
                        .build();
        GenericDocument visibilityDoc =
                new GenericDocument.Builder<GenericDocument.Builder<?>>("", "someSchema",
                        "VisibilityType")
                        .setCreationTimestampMillis(0)
                        .build();

        InternalVisibilityConfig visibilityConfig =
                VisibilityToDocumentConverter.createInternalVisibilityConfig(
                        visibilityDoc, androidVOverlay);

        // The placeholder is never returned. The config whose only package is the placeholder
        // is dropped, so that it is not widened to every caller which holds permission 1. The
        // config which has another package keeps the access that it had with the placeholder.
        assertThat(visibilityConfig.getVisibleToConfigs()).containsExactly(
                new SchemaVisibilityConfig.Builder()
                        .addAllowedPackage(package1)
                        .addRequiredPermissions(ImmutableSet.of(2))
                        .build(),
                packageWithTagsConfig);
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_ENABLE_SCHEMA_VISIBLE_TO_PACKAGE_WITH_TAGS)
    public void testConvertVisibilityConfigFromProto_packagesWithTagsNotConverted() {
        byte[] cert1 = new byte[32];
        Arrays.fill(cert1, (byte) 1);
        PackageIdentifierProto package1Proto = PackageIdentifierProto.newBuilder()
                .setPackageName("com.example.test1")
                .setPackageSha256Cert(ByteString.copyFrom(cert1)).build();
        PackageIdentifierProto placeholderProto = PackageIdentifierProto.newBuilder()
                .setPackageName(
                        VisibilityToDocumentConverter.PACKAGES_WITH_TAGS_PLACEHOLDER_PACKAGE_NAME)
                .setPackageSha256Cert(ByteString.copyFrom(new byte[32])).build();
        PackageIdentifier placeholder = new PackageIdentifier(
                VisibilityToDocumentConverter.PACKAGES_WITH_TAGS_PLACEHOLDER_PACKAGE_NAME,
                new byte[32]);
        // A package which is restricted to no attribution tags cannot be created through the API.
        VisibleToPackageWithTagsProto package1WithoutTagsProto =
                VisibleToPackageWithTagsProto.newBuilder()
                        .setPackageIdentifier(package1Proto)
                        .build();
        VisibleToPermissionProto permissionProto = VisibleToPermissionProto.newBuilder()
                .addAllPermissions(ImmutableSet.of(1)).build();

        // In all of these protos, no package that is restricted by attribution tag can be
        // converted. The config must not be left with the permission only, which would grant
        // access to every caller which holds it.

        // The placeholder is kept as a package, which no caller can match.
        SchemaVisibilityConfig placeholderOnlyConfig =
                VisibilityToDocumentConverter.convertVisibilityConfigFromProto(
                        VisibilityConfigProto.newBuilder()
                                .addVisibleToPackages(placeholderProto)
                                .addVisibleToPermissions(permissionProto)
                                .build());
        assertThat(placeholderOnlyConfig.getAllowedPackages()).containsExactly(placeholder);
        assertThat(placeholderOnlyConfig.getAllowedPackagesWithTags()).isEmpty();
        assertThat(placeholderOnlyConfig.getRequiredPermissions())
                .containsExactly(ImmutableSet.of(1));

        SchemaVisibilityConfig placeholderAndPackageWithoutTagsConfig =
                VisibilityToDocumentConverter.convertVisibilityConfigFromProto(
                        VisibilityConfigProto.newBuilder()
                                .addVisibleToPackages(placeholderProto)
                                .addVisibleToPackagesWithTags(package1WithoutTagsProto)
                                .addVisibleToPermissions(permissionProto)
                                .build());
        assertThat(placeholderAndPackageWithoutTagsConfig.getAllowedPackages())
                .containsExactly(placeholder);
        assertThat(placeholderAndPackageWithoutTagsConfig.getAllowedPackagesWithTags()).isEmpty();
        assertThat(placeholderAndPackageWithoutTagsConfig.getRequiredPermissions())
                .containsExactly(ImmutableSet.of(1));

        // Without the placeholder, the config has no requirement at all, and grants access to
        // no one.
        SchemaVisibilityConfig packageWithoutTagsConfig =
                VisibilityToDocumentConverter.convertVisibilityConfigFromProto(
                        VisibilityConfigProto.newBuilder()
                                .addVisibleToPackagesWithTags(package1WithoutTagsProto)
                                .addVisibleToPermissions(permissionProto)
                                .build());
        assertThat(packageWithoutTagsConfig)
                .isEqualTo(new SchemaVisibilityConfig.Builder().build());
    }

    @Test
    @RequiresFlagsDisabled(Flags.FLAG_ENABLE_SCHEMA_VISIBLE_TO_PACKAGE_WITH_TAGS)
    public void testPackagesWithTags_flagDisabled() throws Exception {
        byte[] cert1 = new byte[32];
        Arrays.fill(cert1, (byte) 1);
        PackageIdentifier package1 = new PackageIdentifier("com.example.test1", cert1);
        SchemaVisibilityConfig packageWithTagsConfig = new SchemaVisibilityConfig.Builder()
                .setAllowedPackageWithTags(package1, ImmutableSet.of("tagA"))
                .addRequiredPermissions(ImmutableSet.of(1, 2))
                .build();
        InternalVisibilityConfig visibilityConfig =
                new InternalVisibilityConfig.Builder("someSchema")
                        .addVisibleToConfig(packageWithTagsConfig)
                        .build();

        // Writing still stores the placeholder and the packages restricted by attribution tag,
        // so that the config is not widened to its permissions only.
        GenericDocument visibilityDoc =
                VisibilityToDocumentConverter.createVisibilityDocument(visibilityConfig);
        GenericDocument androidVOverlay =
                VisibilityToDocumentConverter.createAndroidVOverlay(visibilityConfig);
        VisibilityConfigProto storedProto =
                VisibilityToDocumentConverter.convertSchemaVisibilityConfigToProto(
                        packageWithTagsConfig);
        assertThat(storedProto.getVisibleToPackagesCount()).isEqualTo(1);
        assertThat(storedProto.getVisibleToPackages(0).getPackageName()).isEqualTo(
                VisibilityToDocumentConverter.PACKAGES_WITH_TAGS_PLACEHOLDER_PACKAGE_NAME);
        assertThat(storedProto.getVisibleToPackagesWithTagsCount()).isEqualTo(1);

        // With the flag disabled, reading keeps the placeholder as an allowed package and
        // ignores the packages restricted by attribution tag, restoring pre-feature behavior.
        SchemaVisibilityConfig expectedConfig = new SchemaVisibilityConfig.Builder()
                .addAllowedPackage(new PackageIdentifier(
                        VisibilityToDocumentConverter.PACKAGES_WITH_TAGS_PLACEHOLDER_PACKAGE_NAME,
                        new byte[32]))
                .addRequiredPermissions(ImmutableSet.of(1, 2))
                .build();
        assertThat(VisibilityToDocumentConverter.convertVisibilityConfigFromProto(storedProto))
                .isEqualTo(expectedConfig);
        InternalVisibilityConfig rebuild =
                VisibilityToDocumentConverter.createInternalVisibilityConfig(
                        visibilityDoc, androidVOverlay);
        assertThat(rebuild.getVisibleToConfigs()).containsExactly(expectedConfig);
    }
}
