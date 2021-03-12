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
import androidx.appsearch.app.AppSearchMigrationHelper;
import androidx.appsearch.app.AppSearchSchema;
import androidx.appsearch.app.AppSearchSession;
import androidx.appsearch.app.GenericDocument;
import androidx.appsearch.app.PutDocumentsRequest;
import androidx.appsearch.app.SetSchemaRequest;
import androidx.appsearch.app.SetSchemaResponse;
import androidx.appsearch.localstorage.LocalStorage;

import com.google.common.util.concurrent.ListenableFuture;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutionException;

/*
 * For schema migration, we have 4 factors
 * A. is ForceOverride set to true?
 * B. is the schema change backwards compatible?
 * C. did any versions change?
 * D. is there a migration triggered for each incompatible type and no deleted types?
 * If B is true then D could never be false, so that will give us 12 combinations.
 *
 *                                Trigger       Delete      first            second
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
 * FALSE  FALSE   FALSE   FALSE                             fail             throw error
 * FALSE  FALSE   FALSE   FALSE                             fail             throw error
 */
//TODO(b/178060626) add a platform version of this test
public abstract class AppSearchSchemaMigrationCtsTestBase {

    private static final String DB_NAME = LocalStorage.DEFAULT_DATABASE_NAME;
    private static final AppSearchSchema.Migrator NO_OP_MIGRATOR = new AppSearchSchema.Migrator() {
        @Override
        public void onUpgrade(int currentVersion,
                int targetVersion,
                @NonNull AppSearchMigrationHelper helper)
                throws Exception {
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
    }

    @After
    public void tearDown() throws Exception {
        // Cleanup whatever documents may still exist in these databases.
        mDb.setSchema(new SetSchemaRequest.Builder().setForceOverride(true).build()).get();
    }

    @Test
    public void testSetSchema_migration_A_B_C_D() throws Exception {
        // create a backwards compatible schema and update the version
        AppSearchSchema B_C_Schema = new AppSearchSchema.Builder("testSchema")
                .setVersion(1)     // upgrade version
                .addProperty(new AppSearchSchema.StringPropertyConfig.Builder("subject")
                        .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_OPTIONAL)
                        .setIndexingType(
                                AppSearchSchema.StringPropertyConfig.INDEXING_TYPE_PREFIXES)
                        .setTokenizerType(AppSearchSchema.StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                        .build())
                .build();

        mDb.setSchema(
                new SetSchemaRequest.Builder().addSchemas(B_C_Schema)
                        .setMigrator("testSchema", NO_OP_MIGRATOR)
                        .setForceOverride(true)
                        .build()).get();
    }

    @Test
    public void testSetSchema_migration_A_B_NC_D() throws Exception {
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
                        .setMigrator("testSchema", NO_OP_MIGRATOR)
                        .setForceOverride(true)
                        .build()).get();
    }

    @Test
    public void testSetSchema_migration_A_NB_C_D() throws Exception {
        // create a backwards incompatible schema and update the version
        AppSearchSchema NB_C_Schema = new AppSearchSchema.Builder("testSchema")
                .setVersion(1)     // upgrade version
                .build();

        mDb.setSchema(
                new SetSchemaRequest.Builder().addSchemas(NB_C_Schema)
                        .setMigrator("testSchema", NO_OP_MIGRATOR)
                        .setForceOverride(true)
                        .build()).get();
    }

    @Test
    public void testSetSchema_migration_A_NB_C_ND() throws Exception {
        // create a backwards incompatible schema and update the version
        AppSearchSchema NB_C_Schema = new AppSearchSchema.Builder("testSchema")
                .setVersion(1)     // upgrade version
                .build();

        mDb.setSchema(
                new SetSchemaRequest.Builder().addSchemas(NB_C_Schema)
                        .setMigrator("nonexistSchema", NO_OP_MIGRATOR)  //ND
                        .setForceOverride(true)
                        .build()).get();
    }

    @Test
    public void testSetSchema_migration_A_NB_NC_D() throws Exception {
        // create a backwards incompatible schema but don't update the version
        AppSearchSchema NB_NC_Schema = new AppSearchSchema.Builder("testSchema")
                .build();

        mDb.setSchema(
                new SetSchemaRequest.Builder().addSchemas(NB_NC_Schema)
                        .setMigrator("testSchema", NO_OP_MIGRATOR)
                        .setForceOverride(true)
                        .build()).get();
    }

    @Test
    public void testSetSchema_migration_A_NB_NC_ND() throws Exception {
        // create a backwards incompatible schema but don't update the version
        AppSearchSchema $B_$C_Schema = new AppSearchSchema.Builder("testSchema")
                .build();

        mDb.setSchema(
                new SetSchemaRequest.Builder().addSchemas($B_$C_Schema)
                        .setMigrator("nonexistSchema", NO_OP_MIGRATOR)   //ND
                        .setForceOverride(true)
                        .build()).get();
    }

    @Test
    public void testSetSchema_migration_NA_B_C_D() throws Exception {
        // create a backwards compatible schema and update the version
        AppSearchSchema B_C_Schema = new AppSearchSchema.Builder("testSchema")
                .setVersion(1)     // upgrade version
                .addProperty(new AppSearchSchema.StringPropertyConfig.Builder("subject")
                        .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_OPTIONAL)
                        .setIndexingType(
                                AppSearchSchema.StringPropertyConfig.INDEXING_TYPE_PREFIXES)
                        .setTokenizerType(AppSearchSchema.StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                        .build())
                .build();

        mDb.setSchema(
                new SetSchemaRequest.Builder().addSchemas(B_C_Schema)
                        .setMigrator("testSchema", NO_OP_MIGRATOR)
                        .build()).get();
    }

    @Test
    public void testSetSchema_migration_NA_B_NC_D() throws Exception {
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
                        .setMigrator("testSchema", NO_OP_MIGRATOR)
                        .setForceOverride(true)
                        .build()).get();
    }

    @Test
    public void testSetSchema_migration_NA_NB_C_D() throws Exception {
        // create a backwards incompatible schema and update the version
        AppSearchSchema NB_C_Schema = new AppSearchSchema.Builder("testSchema")
                .setVersion(1)     // upgrade version
                .build();

        mDb.setSchema(
                new SetSchemaRequest.Builder().addSchemas(NB_C_Schema)
                        .setMigrator("testSchema", NO_OP_MIGRATOR)
                        .build()).get();
    }

    @Test
    public void testSetSchema_migration_NA_NB_C_ND() throws Exception {
        // create a backwards incompatible schema and update the version
        AppSearchSchema $B_C_Schema = new AppSearchSchema.Builder("testSchema")
                .setVersion(1)     // upgrade version
                .build();

        ExecutionException exception = assertThrows(ExecutionException.class,
                () -> mDb.setSchema(
                        new SetSchemaRequest.Builder().addSchemas($B_C_Schema)
                                .setMigrator("nonexistSchema", NO_OP_MIGRATOR)  //ND
                                .build()).get());
        assertThat(exception).hasMessageThat().contains("Schema is incompatible.");
    }

    @Test
    public void testSetSchema_migration_NA_NB_NC_D() throws Exception {
        // create a backwards incompatible schema but don't update the version
        AppSearchSchema $B_$C_Schema = new AppSearchSchema.Builder("testSchema")
                .build();

        ExecutionException exception = assertThrows(ExecutionException.class,
                () -> mDb.setSchema(
                        new SetSchemaRequest.Builder().addSchemas($B_$C_Schema)
                                .setMigrator("testSchema", NO_OP_MIGRATOR)
                                .build()).get());
        assertThat(exception).hasMessageThat().contains("Schema is incompatible.");
    }

    @Test
    public void testSetSchema_migration_NA_NB_NC_ND() throws Exception {
        // create a backwards incompatible schema but don't update the version
        AppSearchSchema $B_$C_Schema = new AppSearchSchema.Builder("testSchema")
                .build();

        ExecutionException exception = assertThrows(ExecutionException.class,
                () -> mDb.setSchema(
                        new SetSchemaRequest.Builder().addSchemas($B_$C_Schema)
                                .setMigrator("nonexistSchema", NO_OP_MIGRATOR)  //ND
                                .build()).get());
        assertThat(exception).hasMessageThat().contains("Schema is incompatible.");
    }

    @Test
    public void testSetSchema_migrate() throws Exception {
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
                .setVersion(1)     // upgrade version
                .addProperty(new AppSearchSchema.StringPropertyConfig.Builder("subject")
                        .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_OPTIONAL)
                        .setIndexingType(
                                AppSearchSchema.StringPropertyConfig.INDEXING_TYPE_PREFIXES)
                        .setTokenizerType(AppSearchSchema.StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                        .build())
                .build();

        // set the new schema to AppSearch, the first document will be migrated successfully but the
        // second one will be failed.
        AppSearchSchema.Migrator migrator = new AppSearchSchema.Migrator() {
            @Override
            public void onUpgrade(int currentVersion, int targetVersion,
                    @NonNull AppSearchMigrationHelper helper)
                    throws Exception {
                helper.queryAndTransform("testSchema",
                        (currentVersion1, finalVersion1, document) -> {
                            if (document.getUri().equals("uri2")) {
                                return new GenericDocument.Builder<>(document.getNamespace(),
                                        document.getUri(),
                                        document.getSchemaType())
                                        .setPropertyString("subject", "testPut example2")
                                        .setPropertyString("to",
                                                "Except to fail, property not in the schema")
                                        .build();
                            }
                            return new GenericDocument.Builder<>(document.getNamespace(),
                                    document.getUri(),
                                    document.getSchemaType())
                                    .setPropertyString("subject", "testPut example1 migrated")
                                    .setCreationTimestampMillis(12345L)
                                    .build();
                        });
            }
        };
        SetSchemaResponse setSchemaResponse =
                mDb.setSchema(new SetSchemaRequest.Builder().addSchemas(newSchema)
                        .setMigrator("testSchema", migrator).build()).get();

        // Check the schema has been saved
        Set<AppSearchSchema> actualSchema = new HashSet<>();
        actualSchema.add(newSchema);
        assertThat(actualSchema).isEqualTo(mDb.getSchema().get());

        assertThat(setSchemaResponse.getDeletedTypes()).isEmpty();
        assertThat(setSchemaResponse.getIncompatibleTypes())
                .containsExactly("testSchema");
        assertThat(setSchemaResponse.getMigratedTypes())
                .containsExactly("testSchema");

        // Check migrate the first document is success
        GenericDocument expected = new GenericDocument.Builder<>("namespace", "uri1", "testSchema")
                .setPropertyString("subject", "testPut example1 migrated")
                .setCreationTimestampMillis(12345L)
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
}
