/*
 * Copyright 2026 The Android Open Source Project
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
import androidx.appsearch.app.InternalVisibilityConfig;
import androidx.appsearch.app.PackageIdentifier;
import androidx.appsearch.flags.Flags;
import androidx.appsearch.localstorage.AppSearchConfig;
import androidx.appsearch.localstorage.AppSearchConfigImpl;
import androidx.appsearch.localstorage.AppSearchImpl;
import androidx.appsearch.localstorage.AppSearchUserPlugins;
import androidx.appsearch.localstorage.LocalStorageIcingOptionsConfig;
import androidx.appsearch.localstorage.OptimizeStrategy;
import androidx.appsearch.localstorage.UnlimitedLimitConfig;
import androidx.appsearch.localstorage.util.PrefixUtil;
import androidx.appsearch.testutil.AppSearchTestUtils;
import androidx.appsearch.testutil.flags.RequiresFlagsEnabled;

import com.google.common.collect.ImmutableList;

import org.jspecify.annotations.NonNull;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

@RequiresFlagsEnabled(Flags.FLAG_ENABLE_PCC_DATA_ENCAPSULATION)
public class PccEncapsulationUtilTest {
    private static final OptimizeStrategy ALWAYS_OPTIMIZE = optimizeInfo -> true;
    private static final int NON_PCC_UID = 1000;
    private static final int PCC_UID = 30000;

    @Rule
    public final RuleChain mRuleChain = AppSearchTestUtils.createCommonTestRules();
    @Rule
    public TemporaryFolder mTemporaryFolder = new TemporaryFolder();

    private final AppSearchConfig mUnlimitedConfig =
            new AppSearchConfigImpl(
                    new UnlimitedLimitConfig(), new LocalStorageIcingOptionsConfig());

    private final VisibilityChecker mVisibilityChecker =
            new VisibilityChecker() {
                @Override
                public boolean isSchemaSearchableByCaller(
                        @NonNull CallerAccess callerAccess,
                        @NonNull String packageName,
                        @NonNull String prefixedSchema,
                        @NonNull VisibilityStore visibilityStore) {
                    return true;
                }

                @Override
                public boolean doesCallerHaveSystemAccess(@NonNull String callerPackageName) {
                    return false;
                }

                @Override
                public boolean isPrivateComputeCoreUid(int uid) {
                    return uid == PCC_UID;
                }
            };

    private final AppSearchUserPlugins mPlugins =
            new AppSearchUserPlugins.Builder()
                    .setVisibilityChecker(mVisibilityChecker)
                    .build();

    private AppSearchImpl mAppSearchImpl;

    @Before
    public void setUp() throws Exception {
        File appSearchDir = mTemporaryFolder.newFolder();
        mAppSearchImpl =
                AppSearchImpl.create(
                        appSearchDir, mUnlimitedConfig, mPlugins, ALWAYS_OPTIMIZE);
    }

    @After
    public void tearDown() {
        mAppSearchImpl.close();
    }

    @Test
    public void testIsPccUid() {
        assertThat(PccEncapsulationUtil.isPccUid(mVisibilityChecker, PCC_UID)).isTrue();
        assertThat(PccEncapsulationUtil.isPccUid(mVisibilityChecker, NON_PCC_UID)).isFalse();
        assertThat(PccEncapsulationUtil.isPccUid(/* visibilityChecker= */ null, PCC_UID)).isFalse();
    }

    @Test
    public void testUpdateSetSchemaVisibilityConfigs_initialAssignment_nonPcc() {
        AppSearchSchema schemaA = new AppSearchSchema.Builder("SchemaA").build();
        AppSearchSchema schemaB = new AppSearchSchema.Builder("SchemaB").build();
        List<InternalVisibilityConfig> visibilityConfigs = new ArrayList<>();
        PccEncapsulationUtil.updateSetSchemaVisibilityConfigs(
                NON_PCC_UID, ImmutableList.of(schemaA, schemaB), visibilityConfigs);

        assertThat(visibilityConfigs).hasSize(2);
        for (InternalVisibilityConfig config : visibilityConfigs) {
            assertThat(config.getWriterUid()).isEqualTo(NON_PCC_UID);
        }
    }

    @Test
    public void testUpdateSetSchemaVisibilityConfigs_initialAssignment_pcc() {
        AppSearchSchema schemaA = new AppSearchSchema.Builder("SchemaA").build();
        AppSearchSchema schemaB = new AppSearchSchema.Builder("SchemaB").build();
        List<InternalVisibilityConfig> visibilityConfigs = new ArrayList<>();
        PccEncapsulationUtil.updateSetSchemaVisibilityConfigs(
                PCC_UID, ImmutableList.of(schemaA, schemaB), visibilityConfigs);

        assertThat(visibilityConfigs).hasSize(2);
        for (InternalVisibilityConfig config : visibilityConfigs) {
            assertThat(config.getWriterUid()).isEqualTo(PCC_UID);
        }
    }

    @Test
    public void testUpdateSetSchemaVisibilityConfigs_preservesCustomVisibilityConfigs() {
        AppSearchSchema schemaA = new AppSearchSchema.Builder("SchemaA").build();
        AppSearchSchema schemaB = new AppSearchSchema.Builder("SchemaB").build();
        InternalVisibilityConfig inputConfigA =
                new InternalVisibilityConfig.Builder("SchemaA")
                        .setNotDisplayedBySystem(true)
                        .addVisibleToPackage(
                                new PackageIdentifier("com.example.pkgA", new byte[32]))
                        .build();
        InternalVisibilityConfig inputConfigB =
                new InternalVisibilityConfig.Builder("SchemaB")
                        .setNotDisplayedBySystem(false)
                        .addVisibleToPackage(
                                new PackageIdentifier("com.example.pkgB", new byte[32]))
                        .build();

        List<InternalVisibilityConfig> visibilityConfigs =
                new ArrayList<>(ImmutableList.of(inputConfigA, inputConfigB));
        PccEncapsulationUtil.updateSetSchemaVisibilityConfigs(
                PCC_UID, ImmutableList.of(schemaA, schemaB), visibilityConfigs);

        assertThat(visibilityConfigs).hasSize(2);
        for (InternalVisibilityConfig outputConfig : visibilityConfigs) {
            assertThat(outputConfig.getWriterUid()).isEqualTo(PCC_UID);
            if (outputConfig.getSchemaType().equals("SchemaA")) {
                assertThat(outputConfig.isNotDisplayedBySystem()).isTrue();
                assertThat(
                        outputConfig
                                .getVisibilityConfig()
                                .getAllowedPackages()
                                .iterator()
                                .next()
                                .getPackageName())
                        .isEqualTo("com.example.pkgA");
            } else if (outputConfig.getSchemaType().equals("SchemaB")) {
                assertThat(outputConfig.isNotDisplayedBySystem()).isFalse();
                assertThat(
                        outputConfig
                                .getVisibilityConfig()
                                .getAllowedPackages()
                                .iterator()
                                .next()
                                .getPackageName())
                        .isEqualTo("com.example.pkgB");
            }
        }
    }

    @Test
    public void testUpdateSetSchemaVisibilityConfigs_partialVisibilityConfigs_createsMissingConfigs() {
        AppSearchSchema schemaA = new AppSearchSchema.Builder("SchemaA").build();
        AppSearchSchema schemaB = new AppSearchSchema.Builder("SchemaB").build();
        InternalVisibilityConfig inputConfigA =
                new InternalVisibilityConfig.Builder("SchemaA")
                        .setNotDisplayedBySystem(true)
                        .addVisibleToPackage(
                                new PackageIdentifier("com.example.pkgA", new byte[32]))
                        .build();

        // Only pass visibility config for SchemaA; SchemaB has no config initially
        List<InternalVisibilityConfig> visibilityConfigs =
                new ArrayList<>(ImmutableList.of(inputConfigA));
        PccEncapsulationUtil.updateSetSchemaVisibilityConfigs(
                PCC_UID, ImmutableList.of(schemaA, schemaB), visibilityConfigs);

        assertThat(visibilityConfigs).hasSize(2);
        for (InternalVisibilityConfig outputConfig : visibilityConfigs) {
            assertThat(outputConfig.getWriterUid()).isEqualTo(PCC_UID);
            if (outputConfig.getSchemaType().equals("SchemaA")) {
                assertThat(outputConfig.isNotDisplayedBySystem()).isTrue();
                assertThat(
                        outputConfig
                                .getVisibilityConfig()
                                .getAllowedPackages()
                                .iterator()
                                .next()
                                .getPackageName())
                        .isEqualTo("com.example.pkgA");
            } else if (outputConfig.getSchemaType().equals("SchemaB")) {
                assertThat(outputConfig.isNotDisplayedBySystem()).isFalse();
                assertThat(outputConfig.getVisibilityConfig().getAllowedPackages()).isEmpty();
            }
        }
    }

    @Test
    public void testUpdateSetSchemaVisibilityConfigs_invalidUid_returnsInputConfigs() {
        AppSearchSchema schemaA = new AppSearchSchema.Builder("SchemaA").build();
        AppSearchSchema schemaB = new AppSearchSchema.Builder("SchemaB").build();
        InternalVisibilityConfig configA =
                new InternalVisibilityConfig.Builder("SchemaA").build();
        InternalVisibilityConfig configB =
                new InternalVisibilityConfig.Builder("SchemaB").build();

        List<InternalVisibilityConfig> visibilityConfigs =
                new ArrayList<>(ImmutableList.of(configA, configB));
        PccEncapsulationUtil.updateSetSchemaVisibilityConfigs(
                InternalVisibilityConfig.INVALID_UID,
                ImmutableList.of(schemaA, schemaB),
                visibilityConfigs);
        assertThat(visibilityConfigs).containsExactly(configA, configB);
    }

    @Test
    public void testCalculatePccSchemasToWipe_newSchemas_noWipe() throws Exception {
        VisibilityStore visibilityStore =
                VisibilityStore.createDocumentVisibilityStore(
                        mAppSearchImpl, /*callStatsBuilder=*/ null);
        AppSearchSchema schemaA = new AppSearchSchema.Builder("SchemaA").build();
        AppSearchSchema schemaB = new AppSearchSchema.Builder("SchemaB").build();

        assertThat(
                        PccEncapsulationUtil.calculatePccSchemasToWipe(
                                visibilityStore,
                                mVisibilityChecker,
                                "pkg",
                                "db",
                                NON_PCC_UID,
                                ImmutableList.of(schemaA, schemaB)))
                .isEmpty();

        assertThat(
                        PccEncapsulationUtil.calculatePccSchemasToWipe(
                                visibilityStore,
                                mVisibilityChecker,
                                "pkg",
                                "db",
                                PCC_UID,
                                ImmutableList.of(schemaA, schemaB)))
                .isEmpty();
    }

    @Test
    public void testCalculatePccSchemasToWipe_upgradeNonPccToPcc_noWipe() throws Exception {
        VisibilityStore visibilityStore =
                VisibilityStore.createDocumentVisibilityStore(
                        mAppSearchImpl, /*callStatsBuilder=*/ null);
        String prefix = PrefixUtil.createPrefix("pkg", "db");
        // Previously written by NON_PCC_UID
        InternalVisibilityConfig oldConfigA =
                new InternalVisibilityConfig.Builder(prefix + "SchemaA")
                        .setWriterUid(NON_PCC_UID)
                        .build();
        InternalVisibilityConfig oldConfigB =
                new InternalVisibilityConfig.Builder(prefix + "SchemaB")
                        .setWriterUid(NON_PCC_UID)
                        .build();
        visibilityStore.setVisibility(
                ImmutableList.of(oldConfigA, oldConfigB), /* callStatsBuilder= */ null);

        AppSearchSchema schemaA = new AppSearchSchema.Builder("SchemaA").build();
        AppSearchSchema schemaB = new AppSearchSchema.Builder("SchemaB").build();
        List<String> schemasToWipe =
                PccEncapsulationUtil.calculatePccSchemasToWipe(
                        visibilityStore,
                        mVisibilityChecker,
                        "pkg",
                        "db",
                        PCC_UID,
                        ImmutableList.of(schemaA, schemaB));

        assertThat(schemasToWipe).isEmpty();
    }

    @Test
    public void testCalculatePccSchemasToWipe_sameRole_noWipe() throws Exception {
        VisibilityStore visibilityStore =
                VisibilityStore.createDocumentVisibilityStore(
                        mAppSearchImpl, /*callStatsBuilder=*/ null);
        String prefix = PrefixUtil.createPrefix("pkg", "db");
        InternalVisibilityConfig nonPccConfigA =
                new InternalVisibilityConfig.Builder(prefix + "SchemaA")
                        .setWriterUid(NON_PCC_UID)
                        .build();
        InternalVisibilityConfig nonPccConfigB =
                new InternalVisibilityConfig.Builder(prefix + "SchemaB")
                        .setWriterUid(NON_PCC_UID)
                        .build();
        InternalVisibilityConfig pccConfigC =
                new InternalVisibilityConfig.Builder(prefix + "SchemaC")
                        .setWriterUid(PCC_UID)
                        .build();
        InternalVisibilityConfig pccConfigD =
                new InternalVisibilityConfig.Builder(prefix + "SchemaD")
                        .setWriterUid(PCC_UID)
                        .build();
        visibilityStore.setVisibility(
                ImmutableList.of(nonPccConfigA, nonPccConfigB, pccConfigC, pccConfigD),
                /* callStatsBuilder= */ null);

        AppSearchSchema schemaA = new AppSearchSchema.Builder("SchemaA").build();
        AppSearchSchema schemaB = new AppSearchSchema.Builder("SchemaB").build();
        AppSearchSchema schemaC = new AppSearchSchema.Builder("SchemaC").build();
        AppSearchSchema schemaD = new AppSearchSchema.Builder("SchemaD").build();

        // Non-PCC caller updating non-PCC schemas: no wipe
        List<String> nonPccSchemasToWipe =
                PccEncapsulationUtil.calculatePccSchemasToWipe(
                        visibilityStore,
                        mVisibilityChecker,
                        "pkg",
                        "db",
                        NON_PCC_UID,
                        ImmutableList.of(schemaA, schemaB));
        assertThat(nonPccSchemasToWipe).isEmpty();

        // PCC caller updating PCC schemas: no wipe
        List<String> pccSchemasToWipe =
                PccEncapsulationUtil.calculatePccSchemasToWipe(
                        visibilityStore,
                        mVisibilityChecker,
                        "pkg",
                        "db",
                        PCC_UID,
                        ImmutableList.of(schemaC, schemaD));
        assertThat(pccSchemasToWipe).isEmpty();
    }

    @Test
    public void testCalculatePccSchemasToWipe_downgradePccToNonPcc_identifiesSchemasToWipe()
            throws Exception {
        VisibilityStore visibilityStore =
                VisibilityStore.createDocumentVisibilityStore(
                        mAppSearchImpl, /*callStatsBuilder=*/ null);
        String prefix = PrefixUtil.createPrefix("pkg", "db");
        // Previously written by PCC_UID
        InternalVisibilityConfig oldConfigA =
                new InternalVisibilityConfig.Builder(prefix + "SchemaA")
                        .setWriterUid(PCC_UID)
                        .build();
        InternalVisibilityConfig oldConfigB =
                new InternalVisibilityConfig.Builder(prefix + "SchemaB")
                        .setWriterUid(PCC_UID)
                        .build();
        visibilityStore.setVisibility(
                ImmutableList.of(oldConfigA, oldConfigB), /* callStatsBuilder= */ null);

        AppSearchSchema schemaA = new AppSearchSchema.Builder("SchemaA").build();
        AppSearchSchema schemaB = new AppSearchSchema.Builder("SchemaB").build();
        List<String> schemasToWipe =
                PccEncapsulationUtil.calculatePccSchemasToWipe(
                        visibilityStore,
                        mVisibilityChecker,
                        "pkg",
                        "db",
                        NON_PCC_UID,
                        ImmutableList.of(schemaA, schemaB));

        assertThat(schemasToWipe).containsExactly("SchemaA", "SchemaB");
    }

    @Test
    public void testCalculatePccSchemasToWipe_mixedSchemas_nonPccCaller() throws Exception {
        VisibilityStore visibilityStore =
                VisibilityStore.createDocumentVisibilityStore(
                        mAppSearchImpl, /*callStatsBuilder=*/ null);
        String prefix = PrefixUtil.createPrefix("pkg", "db");
        // Schemas written by NON_PCC_UID
        InternalVisibilityConfig nonPccConfig1 =
                new InternalVisibilityConfig.Builder(prefix + "SchemaNonPcc1")
                        .setWriterUid(NON_PCC_UID)
                        .build();
        InternalVisibilityConfig nonPccConfig2 =
                new InternalVisibilityConfig.Builder(prefix + "SchemaNonPcc2")
                        .setWriterUid(NON_PCC_UID)
                        .build();
        // Schemas written by PCC_UID
        InternalVisibilityConfig pccConfig1 =
                new InternalVisibilityConfig.Builder(prefix + "SchemaPcc1")
                        .setWriterUid(PCC_UID)
                        .build();
        InternalVisibilityConfig pccConfig2 =
                new InternalVisibilityConfig.Builder(prefix + "SchemaPcc2")
                        .setWriterUid(PCC_UID)
                        .build();
        visibilityStore.setVisibility(
                ImmutableList.of(nonPccConfig1, nonPccConfig2, pccConfig1, pccConfig2),
                /* callStatsBuilder= */ null);

        AppSearchSchema schemaNew1 = new AppSearchSchema.Builder("SchemaNew1").build();
        AppSearchSchema schemaNew2 = new AppSearchSchema.Builder("SchemaNew2").build();
        AppSearchSchema schemaNonPcc1 = new AppSearchSchema.Builder("SchemaNonPcc1").build();
        AppSearchSchema schemaNonPcc2 = new AppSearchSchema.Builder("SchemaNonPcc2").build();
        AppSearchSchema schemaPcc1 = new AppSearchSchema.Builder("SchemaPcc1").build();
        AppSearchSchema schemaPcc2 = new AppSearchSchema.Builder("SchemaPcc2").build();

        List<String> schemasToWipe =
                PccEncapsulationUtil.calculatePccSchemasToWipe(
                        visibilityStore,
                        mVisibilityChecker,
                        "pkg",
                        "db",
                        NON_PCC_UID,
                        ImmutableList.of(
                                schemaNew1,
                                schemaNew2,
                                schemaNonPcc1,
                                schemaNonPcc2,
                                schemaPcc1,
                                schemaPcc2));

        assertThat(schemasToWipe).containsExactly("SchemaPcc1", "SchemaPcc2");
    }

    @Test
    public void testCalculatePccSchemasToWipe_mixedSchemas_pccCaller() throws Exception {
        VisibilityStore visibilityStore =
                VisibilityStore.createDocumentVisibilityStore(
                        mAppSearchImpl, /*callStatsBuilder=*/ null);
        String prefix = PrefixUtil.createPrefix("pkg", "db");
        // Schemas written by NON_PCC_UID (upgrade)
        InternalVisibilityConfig nonPccConfig1 =
                new InternalVisibilityConfig.Builder(prefix + "SchemaNonPcc1")
                        .setWriterUid(NON_PCC_UID)
                        .build();
        InternalVisibilityConfig nonPccConfig2 =
                new InternalVisibilityConfig.Builder(prefix + "SchemaNonPcc2")
                        .setWriterUid(NON_PCC_UID)
                        .build();
        // Schemas written by PCC_UID
        InternalVisibilityConfig pccConfig1 =
                new InternalVisibilityConfig.Builder(prefix + "SchemaPcc1")
                        .setWriterUid(PCC_UID)
                        .build();
        InternalVisibilityConfig pccConfig2 =
                new InternalVisibilityConfig.Builder(prefix + "SchemaPcc2")
                        .setWriterUid(PCC_UID)
                        .build();
        visibilityStore.setVisibility(
                ImmutableList.of(nonPccConfig1, nonPccConfig2, pccConfig1, pccConfig2),
                /* callStatsBuilder= */ null);

        AppSearchSchema schemaNew1 = new AppSearchSchema.Builder("SchemaNew1").build();
        AppSearchSchema schemaNew2 = new AppSearchSchema.Builder("SchemaNew2").build();
        AppSearchSchema schemaNonPcc1 = new AppSearchSchema.Builder("SchemaNonPcc1").build();
        AppSearchSchema schemaNonPcc2 = new AppSearchSchema.Builder("SchemaNonPcc2").build();
        AppSearchSchema schemaPcc1 = new AppSearchSchema.Builder("SchemaPcc1").build();
        AppSearchSchema schemaPcc2 = new AppSearchSchema.Builder("SchemaPcc2").build();

        List<String> schemasToWipe =
                PccEncapsulationUtil.calculatePccSchemasToWipe(
                        visibilityStore,
                        mVisibilityChecker,
                        "pkg",
                        "db",
                        PCC_UID,
                        ImmutableList.of(
                                schemaNew1,
                                schemaNew2,
                                schemaNonPcc1,
                                schemaNonPcc2,
                                schemaPcc1,
                                schemaPcc2));

        assertThat(schemasToWipe).isEmpty();
    }

    @Test
    public void testCalculatePccSchemasToWipe_nullVisibilityStoreOrChecker_returnsEmpty()
            throws Exception {
        VisibilityStore visibilityStore =
                VisibilityStore.createDocumentVisibilityStore(
                        mAppSearchImpl, /*callStatsBuilder=*/ null);
        AppSearchSchema schemaA = new AppSearchSchema.Builder("SchemaA").build();
        AppSearchSchema schemaB = new AppSearchSchema.Builder("SchemaB").build();

        List<String> resultNullStore =
                PccEncapsulationUtil.calculatePccSchemasToWipe(
                        /* visibilityStore= */ null,
                        mVisibilityChecker,
                        "pkg",
                        "db",
                        NON_PCC_UID,
                        ImmutableList.of(schemaA, schemaB));
        assertThat(resultNullStore).isEmpty();

        List<String> resultNullChecker =
                PccEncapsulationUtil.calculatePccSchemasToWipe(
                        visibilityStore,
                        /* visibilityChecker= */ null,
                        "pkg",
                        "db",
                        NON_PCC_UID,
                        ImmutableList.of(schemaA, schemaB));
        assertThat(resultNullChecker).isEmpty();
    }
}
