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

package androidx.appsearch.app.cts;

import static androidx.appsearch.app.util.AppSearchTestUtils.checkIsBatchResultSuccess;
import static androidx.appsearch.app.util.AppSearchTestUtils.doGet;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertThrows;

import androidx.annotation.NonNull;
import androidx.appsearch.app.AppSearchBatchResult;
import androidx.appsearch.app.AppSearchSchema;
import androidx.appsearch.app.AppSearchSession;
import androidx.appsearch.app.GenericDocument;
import androidx.appsearch.app.Migrator;
import androidx.appsearch.app.PutDocumentsRequest;
import androidx.appsearch.app.SetSchemaRequest;
import androidx.appsearch.app.SetSchemaResponse;

import com.google.common.util.concurrent.ListenableFuture;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.ExecutionException;

/*
 * For schema migration, we have 4 factors
 * A. is ForceOverride set to true?
 * B. is the schema change backwards compatible?
 * C. is shouldTrigger return true?
 * D. is there a migration triggered for each incompatible type and no deleted types?
 * If B is true then D could never be false, so that will give us 12 combinations.
 *
 *                                Trigger       Delete      First            Second
 * A      B       C       D       Migration     Types       SetSchema        SetSchema
 * TRUE   TRUE    TRUE    TRUE    Yes                       succeeds         succeeds(noop)
 * TRUE   TRUE    FALSE   TRUE                              succeeds         succeeds(noop)
 * TRUE   FALSE   TRUE    TRUE    Yes                       fail             succeeds
 * TRUE   FALSE   TRUE    FALSE   Yes           Yes         fail             succeeds
 * TRUE   FALSE   FALSE   TRUE                  Yes         fail             succeeds
 * TRUE   FALSE   FALSE   FALSE                 Yes         fail             succeeds
 * FALSE  TRUE    TRUE    TRUE    Yes                       succeeds         succeeds(noop)
 * FALSE  TRUE    FALSE   TRUE                              succeeds         succeeds(noop)
 * FALSE  FALSE   TRUE    TRUE    Yes                       fail             succeeds
 * FALSE  FALSE   TRUE    FALSE   Yes                       fail             throw error
 * FALSE  FALSE   FALSE   TRUE    Impossible case, migrators are inactivity
 * FALSE  FALSE   FALSE   FALSE                             fail             throw error
 */
//TODO(b/178060626) add a platform version of this test
public abstract class AppSearchSchemaMigrationCtsTestBase {

    private static final String DB_NAME = "";
    private static final long DOCUMENT_CREATION_TIME = 12345L;
    private static final Migrator ACTIVE_NOOP_MIGRATOR = new Migrator() {
        @Override
        public boolean shouldMigrate(int currentVersion, int finalVersion) {
            return true;
        }

        @NonNull
        @Override
        public GenericDocument onUpgrade(int currentVersion, int finalVersion,
                @NonNull GenericDocument document) {
            return document;
        }

        @NonNull
        @Override
        public GenericDocument onDowngrade(int currentVersion, int finalVersion,
                @NonNull GenericDocument document) {
            return document;
        }
    };
    private static final Migrator INACTIVE_MIGRATOR = new Migrator() {
        @Override
        public boolean shouldMigrate(int currentVersion, int finalVersion) {
            return false;
        }

        @NonNull
        @Override
        public GenericDocument onUpgrade(int currentVersion, int finalVersion,
                @NonNull GenericDocument document) {
            return document;
        }

        @NonNull
        @Override
        public GenericDocument onDowngrade(int currentVersion, int finalVersion,
                @NonNull GenericDocument document) {
            return document;
        }
    };

    private AppSearchSession mDb;

    protected abstract ListenableFuture<AppSearchSession> createSearchSession(
            @NonNull String dbName);

    @Before
    public void setUp() throws Exception {
        mDb = createSearchSession(DB_NAME).get();

        // Cleanup whatever documents may still exist in these databases. This is needed in
        // addition to tearDown in case a test exited without completing properly.
        AppSearchSchema schema = new AppSearchSchema.Builder("testSchema")
                .addProperty(new AppSearchSchema.StringPropertyConfig.Builder("subject")
                        .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_REQUIRED)
                        .setIndexingType(
                                AppSearchSchema.StringPropertyConfig.INDEXING_TYPE_PREFIXES)
                        .setTokenizerType(AppSearchSchema.StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                        .build())
                .build();
        mDb.setSchema(new SetSchemaRequest.Builder()
                .addSchemas(schema).setForceOverride(true).build()).get();
        GenericDocument doc = new GenericDocument.Builder<>(
                "namespace", "uri0", "testSchema")
                .setPropertyString("subject", "testPut example1")
                .setCreationTimestampMillis(DOCUMENT_CREATION_TIME).build();
        AppSearchBatchResult<String, Void> result = checkIsBatchResultSuccess(mDb.put(
                new PutDocumentsRequest.Builder().addGenericDocuments(doc).build()));
        assertThat(result.getSuccesses()).containsExactly("uri0", null);
        assertThat(result.getFailures()).isEmpty();
    }

    @After
    public void tearDown() throws Exception {
        // Cleanup whatever documents may still exist in these databases.
        mDb.setSchema(new SetSchemaRequest.Builder().setForceOverride(true).build()).get();
    }

    @Test
    public void testSchemaMigration_A_B_C_D() throws Exception {
        // create a backwards compatible schema and update the version
        AppSearchSchema B_C_Schema = new AppSearchSchema.Builder("testSchema")
                .addProperty(new AppSearchSchema.StringPropertyConfig.Builder("subject")
                        .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_OPTIONAL)
                        .setIndexingType(
                                AppSearchSchema.StringPropertyConfig.INDEXING_TYPE_PREFIXES)
                        .setTokenizerType(AppSearchSchema.StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                        .build())
                .build();

        mDb.setSchema(
                new SetSchemaRequest.Builder().addSchemas(B_C_Schema)
                        .setMigrator("testSchema", ACTIVE_NOOP_MIGRATOR)
                        .setForceOverride(true)
                        .setVersion(2)     // upgrade version
                        .build()).get();
    }

    @Test
    public void testSchemaMigration_A_B_NC_D() throws Exception {
        // create a backwards compatible schema but don't update the version
        AppSearchSchema B_NC_Schema = new AppSearchSchema.Builder("testSchema")
                .addProperty(new AppSearchSchema.StringPropertyConfig.Builder("subject")
                        .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_OPTIONAL)
                        .setIndexingType(
                                AppSearchSchema.StringPropertyConfig.INDEXING_TYPE_PREFIXES)
                        .setTokenizerType(AppSearchSchema.StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                        .build())
                .build();

        mDb.setSchema(
                new SetSchemaRequest.Builder().addSchemas(B_NC_Schema)
                        .setMigrator("testSchema", ACTIVE_NOOP_MIGRATOR)
                        .setForceOverride(true)
                        .build()).get();
    }

    @Test
    public void testSchemaMigration_A_NB_C_D() throws Exception {
        // create a backwards incompatible schema and update the version
        AppSearchSchema NB_C_Schema = new AppSearchSchema.Builder("testSchema")
                .build();

        mDb.setSchema(
                new SetSchemaRequest.Builder().addSchemas(NB_C_Schema)
                        .setMigrator("testSchema", ACTIVE_NOOP_MIGRATOR)
                        .setForceOverride(true)
                        .setVersion(2)     // upgrade version
                        .build()).get();
    }

    @Test
    public void testSchemaMigration_A_NB_C_ND() throws Exception {
        // create a backwards incompatible schema and update the version
        AppSearchSchema NB_C_Schema = new AppSearchSchema.Builder("testSchema")
                .build();

        mDb.setSchema(
                new SetSchemaRequest.Builder().addSchemas(NB_C_Schema)
                        .setMigrator("testSchema", INACTIVE_MIGRATOR)  //ND
                        .setForceOverride(true)
                        .setVersion(2)     // upgrade version
                        .build()).get();
    }

    @Test
    public void testSchemaMigration_A_NB_NC_D() throws Exception {
        // create a backwards incompatible schema but don't update the version
        AppSearchSchema NB_NC_Schema = new AppSearchSchema.Builder("testSchema")
                .build();

        mDb.setSchema(
                new SetSchemaRequest.Builder().addSchemas(NB_NC_Schema)
                        .setMigrator("testSchema", ACTIVE_NOOP_MIGRATOR)
                        .setForceOverride(true)
                        .build()).get();
    }

    @Test
    public void testSchemaMigration_A_NB_NC_ND() throws Exception {
        // create a backwards incompatible schema but don't update the version
        AppSearchSchema $B_$C_Schema = new AppSearchSchema.Builder("testSchema")
                .build();

        mDb.setSchema(
                new SetSchemaRequest.Builder().addSchemas($B_$C_Schema)
                        .setMigrator("testSchema", INACTIVE_MIGRATOR)  //ND
                        .setForceOverride(true)
                        .build()).get();
    }

    @Test
    public void testSchemaMigration_NA_B_C_D() throws Exception {
        // create a backwards compatible schema and update the version
        AppSearchSchema B_C_Schema = new AppSearchSchema.Builder("testSchema")
                .addProperty(new AppSearchSchema.StringPropertyConfig.Builder("subject")
                        .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_OPTIONAL)
                        .setIndexingType(
                                AppSearchSchema.StringPropertyConfig.INDEXING_TYPE_PREFIXES)
                        .setTokenizerType(AppSearchSchema.StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                        .build())
                .build();

        mDb.setSchema(
                new SetSchemaRequest.Builder().addSchemas(B_C_Schema)
                        .setMigrator("testSchema", ACTIVE_NOOP_MIGRATOR)
                        .setVersion(2)     // upgrade version
                        .build()).get();
    }

    @Test
    public void testSchemaMigration_NA_B_NC_D() throws Exception {
        // create a backwards compatible schema but don't update the version
        AppSearchSchema B_NC_Schema = new AppSearchSchema.Builder("testSchema")
                .addProperty(new AppSearchSchema.StringPropertyConfig.Builder("subject")
                        .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_OPTIONAL)
                        .setIndexingType(
                                AppSearchSchema.StringPropertyConfig.INDEXING_TYPE_PREFIXES)
                        .setTokenizerType(AppSearchSchema.StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                        .build())
                .build();

        mDb.setSchema(
                new SetSchemaRequest.Builder().addSchemas(B_NC_Schema)
                        .setMigrator("testSchema", ACTIVE_NOOP_MIGRATOR)
                        .setForceOverride(true)
                        .build()).get();
    }

    @Test
    public void testSchemaMigration_NA_NB_C_D() throws Exception {
        // create a backwards incompatible schema and update the version
        AppSearchSchema NB_C_Schema = new AppSearchSchema.Builder("testSchema")
                .build();

        mDb.setSchema(
                new SetSchemaRequest.Builder().addSchemas(NB_C_Schema)
                        .setMigrator("testSchema", ACTIVE_NOOP_MIGRATOR)
                        .setVersion(2)     // upgrade version
                        .build()).get();
    }

    @Test
    public void testSchemaMigration_NA_NB_C_ND() throws Exception {
        // create a backwards incompatible schema and update the version
        AppSearchSchema $B_C_Schema = new AppSearchSchema.Builder("testSchema")
                .build();

        ExecutionException exception = assertThrows(ExecutionException.class,
                () -> mDb.setSchema(
                        new SetSchemaRequest.Builder().addSchemas($B_C_Schema)
                                .setMigrator("testSchema", INACTIVE_MIGRATOR)  //ND
                                .setVersion(2)     // upgrade version
                                .build()).get());
        assertThat(exception).hasMessageThat().contains("Schema is incompatible.");
    }

    @Test
    public void testSchemaMigration_NA_NB_NC_ND() throws Exception {
        // create a backwards incompatible schema but don't update the version
        AppSearchSchema $B_$C_Schema = new AppSearchSchema.Builder("testSchema")
                .build();

        ExecutionException exception = assertThrows(ExecutionException.class,
                () -> mDb.setSchema(
                        new SetSchemaRequest.Builder().addSchemas($B_$C_Schema)
                                .setMigrator("testSchema", INACTIVE_MIGRATOR)  //ND
                                .build()).get());
        assertThat(exception).hasMessageThat().contains("Schema is incompatible.");
    }

    @Test
    public void testSchemaMigration() throws Exception {
        AppSearchSchema schema = new AppSearchSchema.Builder("testSchema")
                .addProperty(new AppSearchSchema.StringPropertyConfig.Builder("subject")
                        .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_REQUIRED)
                        .setIndexingType(
                                AppSearchSchema.StringPropertyConfig.INDEXING_TYPE_PREFIXES)
                        .setTokenizerType(AppSearchSchema.StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                        .build())
                .addProperty(new AppSearchSchema.StringPropertyConfig.Builder("To")
                        .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_REQUIRED)
                        .setIndexingType(
                                AppSearchSchema.StringPropertyConfig.INDEXING_TYPE_PREFIXES)
                        .setTokenizerType(AppSearchSchema.StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                        .build())
                .build();
        mDb.setSchema(new SetSchemaRequest.Builder()
                .addSchemas(schema).setForceOverride(true).build()).get();

        GenericDocument doc1 = new GenericDocument.Builder<>("namespace", "uri1", "testSchema")
                .setPropertyString("subject", "testPut example1")
                .setPropertyString("To", "testTo example1")
                .build();
        GenericDocument doc2 = new GenericDocument.Builder<>("namespace", "uri2", "testSchema")
                .setPropertyString("subject", "testPut example2")
                .setPropertyString("To", "testTo example2")
                .build();

        AppSearchBatchResult<String, Void> result = checkIsBatchResultSuccess(mDb.put(
                new PutDocumentsRequest.Builder().addGenericDocuments(doc1, doc2).build()));
        assertThat(result.getSuccesses()).containsExactly("uri1", null, "uri2", null);
        assertThat(result.getFailures()).isEmpty();

        // create new schema type and upgrade the version number
        AppSearchSchema newSchema = new AppSearchSchema.Builder("testSchema")
                .addProperty(new AppSearchSchema.StringPropertyConfig.Builder("subject")
                        .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_OPTIONAL)
                        .setIndexingType(
                                AppSearchSchema.StringPropertyConfig.INDEXING_TYPE_PREFIXES)
                        .setTokenizerType(AppSearchSchema.StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                        .build())
                .build();

        // set the new schema to AppSearch, the first document will be migrated successfully but the
        // second one will be failed.

        Migrator migrator = new Migrator() {
            @Override
            public boolean shouldMigrate(int currentVersion, int finalVersion) {
                return currentVersion != finalVersion;
            }

            @NonNull
            @Override
            public GenericDocument onUpgrade(int currentVersion, int finalVersion,
                    @NonNull GenericDocument document) {
                if (document.getUri().equals("uri2")) {
                    return new GenericDocument.Builder<>(document.getNamespace(), document.getUri(),
                            document.getSchemaType())
                            .setPropertyString("subject", "testPut example2")
                            .setPropertyString("to",
                                    "Expect to fail, property not in the schema")
                            .build();
                }
                return new GenericDocument.Builder<>(document.getNamespace(), document.getUri(),
                        document.getSchemaType())
                        .setPropertyString("subject", "testPut example1 migrated")
                        .setCreationTimestampMillis(DOCUMENT_CREATION_TIME)
                        .build();
            }

            @NonNull
            @Override
            public GenericDocument onDowngrade(int currentVersion, int finalVersion,
                    @NonNull GenericDocument document) {
                throw new IllegalStateException("Downgrade should not be triggered for this test");
            }
        };

        SetSchemaResponse setSchemaResponse =
                mDb.setSchema(new SetSchemaRequest.Builder().addSchemas(newSchema)
                        .setMigrator("testSchema", migrator)
                        .setVersion(2)     // upgrade version
                        .build()).get();

        // Check the schema has been saved
        assertThat(mDb.getSchema().get().getSchemas()).containsExactly(newSchema);

        assertThat(setSchemaResponse.getDeletedTypes()).isEmpty();
        assertThat(setSchemaResponse.getIncompatibleTypes())
                .containsExactly("testSchema");
        assertThat(setSchemaResponse.getMigratedTypes())
                .containsExactly("testSchema");

        // Check migrate the first document is success
        GenericDocument expected = new GenericDocument.Builder<>("namespace", "uri1", "testSchema")
                .setPropertyString("subject", "testPut example1 migrated")
                .setCreationTimestampMillis(DOCUMENT_CREATION_TIME)
                .build();
        assertThat(doGet(mDb, "namespace", "uri1")).containsExactly(expected);

        // Check migrate the second document is fail.
        assertThat(setSchemaResponse.getMigrationFailures()).hasSize(1);
        SetSchemaResponse.MigrationFailure migrationFailure =
                setSchemaResponse.getMigrationFailures().get(0);
        assertThat(migrationFailure.getNamespace()).isEqualTo("namespace");
        assertThat(migrationFailure.getSchemaType()).isEqualTo("testSchema");
        assertThat(migrationFailure.getUri()).isEqualTo("uri2");
    }

    @Test
    public void testSchemaMigration_downgrade() throws Exception {
        AppSearchSchema schema = new AppSearchSchema.Builder("testSchema")
                .addProperty(new AppSearchSchema.StringPropertyConfig.Builder("subject")
                        .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_REQUIRED)
                        .setIndexingType(
                                AppSearchSchema.StringPropertyConfig.INDEXING_TYPE_PREFIXES)
                        .setTokenizerType(AppSearchSchema.StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                        .build())
                .addProperty(new AppSearchSchema.StringPropertyConfig.Builder("To")
                        .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_REQUIRED)
                        .setIndexingType(
                                AppSearchSchema.StringPropertyConfig.INDEXING_TYPE_PREFIXES)
                        .setTokenizerType(AppSearchSchema.StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                        .build())
                .build();
        mDb.setSchema(new SetSchemaRequest.Builder()
                .addSchemas(schema).setForceOverride(true).setVersion(3).build()).get();

        GenericDocument doc1 = new GenericDocument.Builder<>("namespace", "uri1", "testSchema")
                .setPropertyString("subject", "testPut example1")
                .setPropertyString("To", "testTo example1")
                .build();

        AppSearchBatchResult<String, Void> result = checkIsBatchResultSuccess(mDb.put(
                new PutDocumentsRequest.Builder().addGenericDocuments(doc1).build()));
        assertThat(result.getSuccesses()).containsExactly("uri1", null);
        assertThat(result.getFailures()).isEmpty();

        // create new schema type and upgrade the version number
        AppSearchSchema newSchema = new AppSearchSchema.Builder("testSchema")
                .addProperty(new AppSearchSchema.StringPropertyConfig.Builder("subject")
                        .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_OPTIONAL)
                        .setIndexingType(
                                AppSearchSchema.StringPropertyConfig.INDEXING_TYPE_PREFIXES)
                        .setTokenizerType(AppSearchSchema.StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                        .build())
                .build();

        // set the new schema to AppSearch
        Migrator migrator = new Migrator() {
            @Override
            public boolean shouldMigrate(int currentVersion, int finalVersion) {
                return currentVersion != finalVersion;
            }

            @NonNull
            @Override
            public GenericDocument onUpgrade(int currentVersion, int finalVersion,
                    @NonNull GenericDocument document) {
                throw new IllegalStateException("Upgrade should not be triggered for this test");
            }

            @NonNull
            @Override
            public GenericDocument onDowngrade(int currentVersion, int finalVersion,
                    @NonNull GenericDocument document) {
                return new GenericDocument.Builder<>(document.getNamespace(), document.getUri(),
                        document.getSchemaType())
                        .setPropertyString("subject", "testPut example1 migrated")
                        .setCreationTimestampMillis(DOCUMENT_CREATION_TIME)
                        .build();
            }
        };

        SetSchemaResponse setSchemaResponse =
                mDb.setSchema(new SetSchemaRequest.Builder().addSchemas(newSchema)
                        .setMigrator("testSchema", migrator)
                        .setVersion(1)     // downgrade version
                        .build()).get();

        // Check the schema has been saved
        assertThat(mDb.getSchema().get().getSchemas()).containsExactly(newSchema);

        assertThat(setSchemaResponse.getDeletedTypes()).isEmpty();
        assertThat(setSchemaResponse.getIncompatibleTypes())
                .containsExactly("testSchema");
        assertThat(setSchemaResponse.getMigratedTypes())
                .containsExactly("testSchema");

        // Check migrate is success
        GenericDocument expected = new GenericDocument.Builder<>("namespace", "uri1", "testSchema")
                .setPropertyString("subject", "testPut example1 migrated")
                .setCreationTimestampMillis(DOCUMENT_CREATION_TIME)
                .build();
        assertThat(doGet(mDb, "namespace", "uri1")).containsExactly(expected);
    }

    @Test
    public void testSchemaMigration_sameVersion() throws Exception {
        AppSearchSchema schema = new AppSearchSchema.Builder("testSchema")
                .addProperty(new AppSearchSchema.StringPropertyConfig.Builder("subject")
                        .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_REQUIRED)
                        .setIndexingType(
                                AppSearchSchema.StringPropertyConfig.INDEXING_TYPE_PREFIXES)
                        .setTokenizerType(AppSearchSchema.StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                        .build())
                .addProperty(new AppSearchSchema.StringPropertyConfig.Builder("To")
                        .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_REQUIRED)
                        .setIndexingType(
                                AppSearchSchema.StringPropertyConfig.INDEXING_TYPE_PREFIXES)
                        .setTokenizerType(AppSearchSchema.StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                        .build())
                .build();
        mDb.setSchema(new SetSchemaRequest.Builder()
                .addSchemas(schema).setForceOverride(true).setVersion(3).build()).get();

        GenericDocument doc1 = new GenericDocument.Builder<>("namespace", "uri1", "testSchema")
                .setPropertyString("subject", "testPut example1")
                .setPropertyString("To", "testTo example1")
                .build();

        AppSearchBatchResult<String, Void> result = checkIsBatchResultSuccess(mDb.put(
                new PutDocumentsRequest.Builder().addGenericDocuments(doc1).build()));
        assertThat(result.getSuccesses()).containsExactly("uri1", null);
        assertThat(result.getFailures()).isEmpty();

        // create new schema type with the same version number
        AppSearchSchema newSchema = new AppSearchSchema.Builder("testSchema")
                .addProperty(new AppSearchSchema.StringPropertyConfig.Builder("subject")
                        .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_OPTIONAL)
                        .setIndexingType(
                                AppSearchSchema.StringPropertyConfig.INDEXING_TYPE_PREFIXES)
                        .setTokenizerType(AppSearchSchema.StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                        .build())
                .build();

        // set the new schema to AppSearch
        Migrator migrator = new Migrator() {

            @Override
            public boolean shouldMigrate(int currentVersion, int finalVersion) {
                return currentVersion != finalVersion;
            }

            @NonNull
            @Override
            public GenericDocument onUpgrade(int currentVersion, int finalVersion,
                    @NonNull GenericDocument document) {
                throw new IllegalStateException("Upgrade should not be triggered for this test");
            }

            @NonNull
            @Override
            public GenericDocument onDowngrade(int currentVersion, int finalVersion,
                    @NonNull GenericDocument document) {
                throw new IllegalStateException("Downgrade should not be triggered for this test");
            }
        };

        // SetSchema with forceOverride=false
        ExecutionException exception = assertThrows(ExecutionException.class,
                () -> mDb.setSchema(new SetSchemaRequest.Builder().addSchemas(newSchema)
                        .setMigrator("testSchema", migrator)
                        .setVersion(3)     // same version
                        .build()).get());
        assertThat(exception).hasMessageThat().contains("Schema is incompatible.");

        // SetSchema with forceOverride=true
        SetSchemaResponse setSchemaResponse =
                mDb.setSchema(new SetSchemaRequest.Builder().addSchemas(newSchema)
                        .setMigrator("testSchema", migrator)
                        .setVersion(3)     // same version
                        .setForceOverride(true).build()).get();

        assertThat(mDb.getSchema().get().getSchemas()).containsExactly(newSchema);

        assertThat(setSchemaResponse.getDeletedTypes()).isEmpty();
        assertThat(setSchemaResponse.getIncompatibleTypes())
                .containsExactly("testSchema");
        assertThat(setSchemaResponse.getMigratedTypes()).isEmpty();

    }

    @Test
    public void testSchemaMigration_noMigration() throws Exception {
        AppSearchSchema schema = new AppSearchSchema.Builder("testSchema")
                .addProperty(new AppSearchSchema.StringPropertyConfig.Builder("subject")
                        .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_REQUIRED)
                        .setIndexingType(
                                AppSearchSchema.StringPropertyConfig.INDEXING_TYPE_PREFIXES)
                        .setTokenizerType(AppSearchSchema.StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                        .build())
                .addProperty(new AppSearchSchema.StringPropertyConfig.Builder("To")
                        .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_REQUIRED)
                        .setIndexingType(
                                AppSearchSchema.StringPropertyConfig.INDEXING_TYPE_PREFIXES)
                        .setTokenizerType(AppSearchSchema.StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                        .build())
                .build();
        mDb.setSchema(new SetSchemaRequest.Builder()
                .addSchemas(schema).setForceOverride(true).setVersion(2).build()).get();

        GenericDocument doc1 = new GenericDocument.Builder<>("namespace", "uri1", "testSchema")
                .setPropertyString("subject", "testPut example1")
                .setPropertyString("To", "testTo example1")
                .build();

        AppSearchBatchResult<String, Void> result = checkIsBatchResultSuccess(mDb.put(
                new PutDocumentsRequest.Builder().addGenericDocuments(doc1).build()));
        assertThat(result.getSuccesses()).containsExactly("uri1", null);
        assertThat(result.getFailures()).isEmpty();

        // create new schema type and upgrade the version number
        AppSearchSchema newSchema = new AppSearchSchema.Builder("testSchema")
                .addProperty(new AppSearchSchema.StringPropertyConfig.Builder("subject")
                        .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_OPTIONAL)
                        .setIndexingType(
                                AppSearchSchema.StringPropertyConfig.INDEXING_TYPE_PREFIXES)
                        .setTokenizerType(AppSearchSchema.StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                        .build())
                .build();

        // Set start version to be 3 means we won't trigger migration for 2.
        Migrator migrator = new Migrator() {

            @Override
            public boolean shouldMigrate(int currentVersion, int finalVersion) {
                return currentVersion > 2 && currentVersion != finalVersion;
            }

            @NonNull
            @Override
            public GenericDocument onUpgrade(int currentVersion, int finalVersion,
                    @NonNull GenericDocument document) {
                throw new IllegalStateException("Upgrade should not be triggered for this test");
            }

            @NonNull
            @Override
            public GenericDocument onDowngrade(int currentVersion, int finalVersion,
                    @NonNull GenericDocument document) {
                throw new IllegalStateException("Downgrade should not be triggered for this test");
            }
        };

        // SetSchema with forceOverride=false
        ExecutionException exception = assertThrows(ExecutionException.class,
                () -> mDb.setSchema(new SetSchemaRequest.Builder().addSchemas(newSchema)
                        .setMigrator("testSchema", migrator)
                        .setVersion(4)     // upgrade version
                        .build()).get());
        assertThat(exception).hasMessageThat().contains("Schema is incompatible.");
    }


    @Test
    public void testSchemaMigration_sourceToNowhere() throws Exception {
        // set the source schema to AppSearch
        AppSearchSchema schema = new AppSearchSchema.Builder("sourceSchema")
                .build();
        mDb.setSchema(new SetSchemaRequest.Builder()
                .addSchemas(schema).setForceOverride(true).build()).get();

        // save a doc to the source type
        GenericDocument doc = new GenericDocument.Builder<>(
                "namespace", "uri1", "sourceSchema")
                .setCreationTimestampMillis(DOCUMENT_CREATION_TIME).build();
        AppSearchBatchResult<String, Void> result = checkIsBatchResultSuccess(mDb.put(
                new PutDocumentsRequest.Builder().addGenericDocuments(doc).build()));
        assertThat(result.getSuccesses()).containsExactly("uri1", null);
        assertThat(result.getFailures()).isEmpty();

        Migrator migrator_sourceToNowhere = new Migrator() {
            @Override
            public boolean shouldMigrate(int currentVersion, int finalVersion) {
                return true;
            }

            @NonNull
            @Override
            public GenericDocument onUpgrade(int currentVersion, int finalVersion,
                    @NonNull GenericDocument document) {
                return new GenericDocument.Builder<>(
                        "zombieNamespace", "zombieUri", "nonExistSchema")
                        .build();
            }

            @NonNull
            @Override
            public GenericDocument onDowngrade(int currentVersion, int finalVersion,
                    @NonNull GenericDocument document) {
                return document;
            }
        };

        // SetSchema with forceOverride=false
        // Source type exist, destination type doesn't exist.
        ExecutionException exception = assertThrows(ExecutionException.class,
                () -> mDb.setSchema(new SetSchemaRequest.Builder()
                        .setMigrator("sourceSchema", migrator_sourceToNowhere)
                        .setVersion(2).build())   // upgrade version
                        .get());
        assertThat(exception).hasMessageThat().contains(
                "Receive a migrated document with schema type: nonExistSchema. "
                        + "But the schema types doesn't exist in the request");

        // SetSchema with forceOverride=true
        // Source type exist, destination type doesn't exist.
        exception = assertThrows(ExecutionException.class,
                () -> mDb.setSchema(new SetSchemaRequest.Builder()
                        .setMigrator("sourceSchema", migrator_sourceToNowhere)
                        .setForceOverride(true)
                        .setVersion(2).build())   // upgrade version
                        .get());
        assertThat(exception).hasMessageThat().contains(
                "Receive a migrated document with schema type: nonExistSchema. "
                        + "But the schema types doesn't exist in the request");
    }

    @Test
    public void testSchemaMigration_nowhereToDestination() throws Exception {
        // set the destination schema to AppSearch
        AppSearchSchema destinationSchema =
                new AppSearchSchema.Builder("destinationSchema").build();
        mDb.setSchema(new SetSchemaRequest.Builder()
                .addSchemas(destinationSchema).setForceOverride(true).build()).get();

        Migrator migrator_nowhereToDestination = new Migrator() {
            @Override
            public boolean shouldMigrate(int currentVersion, int finalVersion) {
                return true;
            }

            @NonNull
            @Override
            public GenericDocument onUpgrade(int currentVersion, int finalVersion,
                    @NonNull GenericDocument document) {
                return document;
            }

            @NonNull
            @Override
            public GenericDocument onDowngrade(int currentVersion, int finalVersion,
                    @NonNull GenericDocument document) {
                return document;
            }
        };


        // Source type doesn't exist, destination type exist. Since source type doesn't exist,
        // no matter force override or not, the migrator won't be invoked
        // SetSchema with forceOverride=false
        SetSchemaResponse setSchemaResponse =
                mDb.setSchema(new SetSchemaRequest.Builder().addSchemas(destinationSchema)
                        .setMigrator("nonExistSchema", migrator_nowhereToDestination)
                        .setVersion(2) //  upgrade version
                        .build()).get();
        assertThat(setSchemaResponse.getMigratedTypes()).isEmpty();

        // SetSchema with forceOverride=true
        setSchemaResponse =
                mDb.setSchema(new SetSchemaRequest.Builder().addSchemas(destinationSchema)
                        .setMigrator("nonExistSchema", migrator_nowhereToDestination)
                        .setVersion(2) //  upgrade version
                        .setForceOverride(true).build()).get();
        assertThat(setSchemaResponse.getMigratedTypes()).isEmpty();
    }

    @Test
    public void testSchemaMigration_nowhereToNowhere() throws Exception {
        // set empty schema
        mDb.setSchema(new SetSchemaRequest.Builder()
                .setForceOverride(true).build()).get();
        Migrator migrator_nowhereToNowhere = new Migrator() {
            @Override
            public boolean shouldMigrate(int currentVersion, int finalVersion) {
                return true;
            }

            @NonNull
            @Override
            public GenericDocument onUpgrade(int currentVersion, int finalVersion,
                    @NonNull GenericDocument document) {
                return document;
            }

            @NonNull
            @Override
            public GenericDocument onDowngrade(int currentVersion, int finalVersion,
                    @NonNull GenericDocument document) {
                return document;
            }
        };


        // Source type doesn't exist, destination type exist. Since source type doesn't exist,
        // no matter force override or not, the migrator won't be invoked
        // SetSchema with forceOverride=false
        SetSchemaResponse setSchemaResponse =
                mDb.setSchema(new SetSchemaRequest.Builder()
                        .setMigrator("nonExistSchema", migrator_nowhereToNowhere)
                        .setVersion(2)  //  upgrade version
                        .build()).get();
        assertThat(setSchemaResponse.getMigratedTypes()).isEmpty();

        // SetSchema with forceOverride=true
        setSchemaResponse =
                mDb.setSchema(new SetSchemaRequest.Builder()
                        .setMigrator("nonExistSchema", migrator_nowhereToNowhere)
                        .setVersion(2) //  upgrade version
                        .setForceOverride(true).build()).get();
        assertThat(setSchemaResponse.getMigratedTypes()).isEmpty();
    }

    @Test
    public void testSchemaMigration_toAnotherType() throws Exception {
        // set the source schema to AppSearch
        AppSearchSchema sourceSchema = new AppSearchSchema.Builder("sourceSchema")
                .build();
        mDb.setSchema(new SetSchemaRequest.Builder()
                .addSchemas(sourceSchema).setForceOverride(true).build()).get();

        // save a doc to the source type
        GenericDocument doc = new GenericDocument.Builder<>(
                "namespace", "uri1", "sourceSchema").build();
        AppSearchBatchResult<String, Void> result = checkIsBatchResultSuccess(mDb.put(
                new PutDocumentsRequest.Builder().addGenericDocuments(doc).build()));
        assertThat(result.getSuccesses()).containsExactly("uri1", null);
        assertThat(result.getFailures()).isEmpty();

        // create the destination type and migrator
        AppSearchSchema destinationSchema = new AppSearchSchema.Builder("destinationSchema")
                .build();
        Migrator migrator = new Migrator() {
            @Override
            public boolean shouldMigrate(int currentVersion, int finalVersion) {
                return true;
            }

            @NonNull
            @Override
            public GenericDocument onUpgrade(int currentVersion, int finalVersion,
                    @NonNull GenericDocument document) {
                return new GenericDocument.Builder<>("namespace",
                        document.getUri(),
                        "destinationSchema")
                        .setCreationTimestampMillis(DOCUMENT_CREATION_TIME)
                        .build();
            }

            @NonNull
            @Override
            public GenericDocument onDowngrade(int currentVersion, int finalVersion,
                    @NonNull GenericDocument document) {
                return document;
            }
        };

        // SetSchema with forceOverride=false and increase overall version
        SetSchemaResponse setSchemaResponse = mDb.setSchema(new SetSchemaRequest.Builder()
                .addSchemas(destinationSchema)
                .setMigrator("sourceSchema", migrator)
                .setForceOverride(false)
                .setVersion(2) //  upgrade version
                .build()).get();
        assertThat(setSchemaResponse.getDeletedTypes())
                .containsExactly("sourceSchema");
        assertThat(setSchemaResponse.getIncompatibleTypes()).isEmpty();
        assertThat(setSchemaResponse.getMigratedTypes())
                .containsExactly("sourceSchema");

        // Check successfully migrate the doc to the destination type
        GenericDocument expected = new GenericDocument.Builder<>(
                "namespace", "uri1", "destinationSchema")
                .setCreationTimestampMillis(DOCUMENT_CREATION_TIME)
                .build();
        assertThat(doGet(mDb, "namespace", "uri1")).containsExactly(expected);
    }

    @Test
    public void testSchemaMigration_toMultipleDestinationType() throws Exception {
        // set the source schema to AppSearch
        AppSearchSchema sourceSchema = new AppSearchSchema.Builder("Person")
                .addProperty(new AppSearchSchema.Int64PropertyConfig.Builder("Age")
                        .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_REQUIRED)
                        .build())
                .build();
        mDb.setSchema(new SetSchemaRequest.Builder()
                .addSchemas(sourceSchema).setForceOverride(true).build()).get();

        // save a child and an adult to the Person type
        GenericDocument childDoc = new GenericDocument.Builder<>(
                "namespace", "Person1", "Person")
                .setPropertyLong("Age", 6).build();
        GenericDocument adultDoc = new GenericDocument.Builder<>(
                "namespace", "Person2", "Person")
                .setPropertyLong("Age", 36).build();
        AppSearchBatchResult<String, Void> result = checkIsBatchResultSuccess(mDb.put(
                new PutDocumentsRequest.Builder().addGenericDocuments(childDoc, adultDoc).build()));
        assertThat(result.getSuccesses()).containsExactly("Person1", null, "Person2", null);
        assertThat(result.getFailures()).isEmpty();

        // create the migrator
        Migrator migrator = new Migrator() {
            @Override
            public boolean shouldMigrate(int currentVersion, int finalVersion) {
                return true;
            }

            @NonNull
            @Override
            public GenericDocument onUpgrade(int currentVersion, int finalVersion,
                    @NonNull GenericDocument document) {
                if (document.getPropertyLong("Age") < 21) {
                    return new GenericDocument.Builder<>(
                            "namespace", "child-uri", "Child")
                            .setPropertyLong("Age", document.getPropertyLong("Age"))
                            .setCreationTimestampMillis(DOCUMENT_CREATION_TIME)
                            .build();
                } else {
                    return new GenericDocument.Builder<>(
                            "namespace", "adult-uri", "Adult")
                            .setPropertyLong("Age", document.getPropertyLong("Age"))
                            .setCreationTimestampMillis(DOCUMENT_CREATION_TIME)
                            .build();
                }
            }

            @NonNull
            @Override
            public GenericDocument onDowngrade(int currentVersion, int finalVersion,
                    @NonNull GenericDocument document) {
                return document;
            }
        };

        // create adult and child schema
        AppSearchSchema adultSchema = new AppSearchSchema.Builder("Adult")
                .addProperty(new AppSearchSchema.Int64PropertyConfig.Builder("Age")
                        .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_REQUIRED)
                        .build())
                .build();
        AppSearchSchema childSchema = new AppSearchSchema.Builder("Child")
                .addProperty(new AppSearchSchema.Int64PropertyConfig.Builder("Age")
                        .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_REQUIRED)
                        .build())
                .build();

        // SetSchema with forceOverride=false and increase overall version
        SetSchemaResponse setSchemaResponse = mDb.setSchema(new SetSchemaRequest.Builder()
                .addSchemas(adultSchema, childSchema)
                .setMigrator("Person", migrator)
                .setForceOverride(false)
                .setVersion(2) //  upgrade version
                .build()).get();
        assertThat(setSchemaResponse.getDeletedTypes())
                .containsExactly("Person");
        assertThat(setSchemaResponse.getIncompatibleTypes()).isEmpty();
        assertThat(setSchemaResponse.getMigratedTypes())
                .containsExactly("Person");

        // Check successfully migrate the child doc
        GenericDocument expectedInChild = new GenericDocument.Builder<>(
                "namespace", "child-uri", "Child")
                .setPropertyLong("Age", 6)
                .setCreationTimestampMillis(DOCUMENT_CREATION_TIME)
                .build();
        assertThat(doGet(mDb, "namespace", "child-uri"))
                .containsExactly(expectedInChild);

        // Check successfully migrate the adult doc
        GenericDocument expectedInAdult = new GenericDocument.Builder<>(
                "namespace", "adult-uri", "Adult")
                .setPropertyLong("Age", 36)
                .setCreationTimestampMillis(DOCUMENT_CREATION_TIME)
                .build();
        assertThat(doGet(mDb, "namespace", "adult-uri"))
                .containsExactly(expectedInAdult);
    }
}
