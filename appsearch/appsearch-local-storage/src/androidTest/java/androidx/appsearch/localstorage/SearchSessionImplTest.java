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

package androidx.appsearch.localstorage;

import static com.google.common.truth.Truth.assertThat;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.appsearch.app.AppSearchSchema;
import androidx.appsearch.app.GenericDocument;
import androidx.appsearch.app.Migrator;
import androidx.appsearch.app.PutDocumentsRequest;
import androidx.appsearch.app.SetSchemaRequest;
import androidx.appsearch.localstorage.stats.SchemaMigrationStats;
import androidx.test.core.app.ApplicationProvider;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

// TODO(b/173532925): Move unit tests to cts tests when logger is available in framework.
public class SearchSessionImplTest {
    private static final String PACKAGE_NAME = "packageName";
    private static final String DATABASE_NAME = "dbName";
    private static final long DOCUMENT_CREATION_TIME = 12345L;
    private static final OptimizeStrategy ALWAYS_OPTIMIZE = optimizeInfo -> true;
    @Rule
    public TemporaryFolder mTemporaryFolder = new TemporaryFolder();
    private AppSearchLoggerTest.TestLogger mAppSearchLogger;
    private SearchSessionImpl mSearchSessionImpl;

    @Before
    public void setUp() throws Exception {
        Context context = ApplicationProvider.getApplicationContext();
        LocalStorage.SearchContext searchContext =
                new LocalStorage.SearchContext.Builder(context, DATABASE_NAME).build();
        mAppSearchLogger = new AppSearchLoggerTest.TestLogger();
        AppSearchImpl appSearchImpl = AppSearchImpl.create(
                mTemporaryFolder.newFolder(),
                new UnlimitedLimitConfig(),
                /*initStatsBuilder=*/ null, ALWAYS_OPTIMIZE);
        mSearchSessionImpl = new SearchSessionImpl(appSearchImpl,
                searchContext.mExecutor, PACKAGE_NAME, DATABASE_NAME, mAppSearchLogger);
    }

    @After
    public void tearDown() {
        mSearchSessionImpl.close();
    }

    @Test
    public void testSetSchemaStats_withoutSchemaMigration() throws Exception {
        AppSearchSchema appSearchSchema = new AppSearchSchema.Builder("testSchema")
                .addProperty(new AppSearchSchema.StringPropertyConfig.Builder("subject")
                        .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_REQUIRED)
                        .setIndexingType(
                                AppSearchSchema.StringPropertyConfig.INDEXING_TYPE_PREFIXES)
                        .setTokenizerType(AppSearchSchema.StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                        .build())
                .build();

        mSearchSessionImpl.setSchema(
                new SetSchemaRequest.Builder().addSchemas(appSearchSchema).build()).get();

        assertThat(mAppSearchLogger.mSetSchemaStats).isNotNull();
        assertThat(mAppSearchLogger.mSetSchemaStats.getPackageName()).isEqualTo(PACKAGE_NAME);
        assertThat(mAppSearchLogger.mSetSchemaStats.getDatabase()).isEqualTo(DATABASE_NAME);
        assertThat(mAppSearchLogger.mSetSchemaStats.getSchemaMigrationStats()).isNull();
        assertThat(mAppSearchLogger.mSetSchemaStats.getNewTypeCount()).isEqualTo(1);
        assertThat(mAppSearchLogger.mSetSchemaStats.getDeletedTypeCount()).isEqualTo(0);
        assertThat(mAppSearchLogger.mSetSchemaStats.getIndexIncompatibleTypeChangeCount())
                .isEqualTo(0);
        assertThat(mAppSearchLogger.mSetSchemaStats.getBackwardsIncompatibleTypeChangeCount())
                .isEqualTo(0);
        assertThat(mAppSearchLogger.mSetSchemaStats.getCompatibleTypeChangeCount()).isEqualTo(0);
    }

    @Test
    public void testSetSchemaStats_withSchemaMigration() throws Exception {
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
        AppSearchSchema newSchema = new AppSearchSchema.Builder("testSchema")
                .addProperty(new AppSearchSchema.StringPropertyConfig.Builder("subject")
                        .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_OPTIONAL)
                        .setIndexingType(
                                AppSearchSchema.StringPropertyConfig.INDEXING_TYPE_PREFIXES)
                        .setTokenizerType(AppSearchSchema.StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                        .build())
                .build();
        GenericDocument doc = new GenericDocument.Builder<>("namespace", "id", "testSchema")
                .setPropertyString("subject", "testPut example")
                .setPropertyString("To", "testTo example")
                .build();
        Migrator migrator = new Migrator() {
            @Override
            public boolean shouldMigrate(int currentVersion, int finalVersion) {
                return currentVersion != finalVersion;
            }

            @NonNull
            @Override
            public GenericDocument onUpgrade(int currentVersion, int finalVersion,
                    @NonNull GenericDocument document) {
                return new GenericDocument.Builder<>(document.getNamespace(), document.getId(),
                        document.getSchemaType())
                        .setPropertyString("subject", "testPut example migrated")
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

        mSearchSessionImpl.setSchema(new SetSchemaRequest.Builder().addSchemas(
                schema).setForceOverride(true).build()).get();
        mSearchSessionImpl.put(new PutDocumentsRequest.Builder().addGenericDocuments(doc).build());
        mSearchSessionImpl.setSchema(new SetSchemaRequest.Builder().addSchemas(newSchema)
                .setMigrator("testSchema", migrator)
                .setVersion(2)     // upgrade version
                .build()).get();

        assertThat(mAppSearchLogger.mSetSchemaStats).isNotNull();
        assertThat(mAppSearchLogger.mSetSchemaStats.getSchemaMigrationStats()).isNotNull();
        SchemaMigrationStats schemaMigrationStats =
                mAppSearchLogger.mSetSchemaStats.getSchemaMigrationStats();
        assertThat(schemaMigrationStats.getMigratedDocumentCount()).isEqualTo(1);
        assertThat(schemaMigrationStats.getSavedDocumentCount()).isEqualTo(1);
    }
}
