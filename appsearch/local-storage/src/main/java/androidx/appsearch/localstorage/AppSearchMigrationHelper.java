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
// @exportToFramework:skipFile()
package androidx.appsearch.localstorage;

import static androidx.appsearch.app.AppSearchResult.RESULT_INVALID_SCHEMA;
import static androidx.appsearch.app.AppSearchResult.throwableToFailedResult;

import android.os.Bundle;
import android.os.Parcel;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;
import androidx.appsearch.app.AppSearchSchema;
import androidx.appsearch.app.GenericDocument;
import androidx.appsearch.app.Migrator;
import androidx.appsearch.app.SearchResultPage;
import androidx.appsearch.app.SearchSpec;
import androidx.appsearch.app.SetSchemaResponse;
import androidx.appsearch.exceptions.AppSearchException;
import androidx.appsearch.localstorage.stats.SchemaMigrationStats;
import androidx.collection.ArraySet;
import androidx.core.util.Preconditions;

import com.google.android.icing.proto.PersistType;
import com.google.android.icing.protobuf.CodedInputStream;
import com.google.android.icing.protobuf.CodedOutputStream;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Set;

/**
 * The helper class for {@link AppSearchSchema} migration.
 *
 * <p>It will query and migrate {@link GenericDocument} in given type to a new version.
 */
class AppSearchMigrationHelper implements Closeable {
    private final AppSearchImpl mAppSearchImpl;
    private final String mPackageName;
    private final String mDatabaseName;
    private final File mFile;
    private final Set<String> mDestinationTypes;
    private boolean mAreDocumentsMigrated = false;

    AppSearchMigrationHelper(@NonNull AppSearchImpl appSearchImpl,
            @NonNull String packageName,
            @NonNull String databaseName,
            @NonNull Set<AppSearchSchema> newSchemas) throws IOException {
        mAppSearchImpl = Preconditions.checkNotNull(appSearchImpl);
        mPackageName = Preconditions.checkNotNull(packageName);
        mDatabaseName = Preconditions.checkNotNull(databaseName);
        Preconditions.checkNotNull(newSchemas);
        mFile = File.createTempFile(/*prefix=*/"appsearch", /*suffix=*/null);
        mDestinationTypes = new ArraySet<>(newSchemas.size());
        for (AppSearchSchema newSchema : newSchemas) {
            mDestinationTypes.add(newSchema.getSchemaType());
        }
    }

    /**
     * Queries all documents that need to be migrated to new version, and transform documents to
     * new version by passing them to the provided Transformer.
     *
     * <p>This method will be invoked on the background worker thread.
     *
     * @param migrators      The map of active {@link Migrator}s that will upgrade or downgrade a
     *                       {@link GenericDocument} to new version. The key is the schema type that
     *                       {@link Migrator} applies to.
     * @param currentVersion The current version of the document's schema.
     * @param finalVersion   The final version that documents need to be migrated to.
     *
     * @throws IOException        on i/o problem
     * @throws AppSearchException on AppSearch problem
     */
    @WorkerThread
    public void queryAndTransform(@NonNull Map<String, Migrator> migrators, int currentVersion,
            int finalVersion, @Nullable SchemaMigrationStats.Builder schemaMigrationStatsBuilder)
            throws IOException, AppSearchException {
        Preconditions.checkState(mFile.exists(), "Internal temp file does not exist.");
        int migratedDocsCount = 0;
        try (FileOutputStream outputStream = new FileOutputStream(mFile, /*append=*/ true)) {
            CodedOutputStream codedOutputStream = CodedOutputStream.newInstance(outputStream);
            SearchResultPage searchResultPage = mAppSearchImpl.query(mPackageName, mDatabaseName,
                    /*queryExpression=*/"",
                    new SearchSpec.Builder()
                            .addFilterSchemas(migrators.keySet())
                            .setTermMatch(SearchSpec.TERM_MATCH_EXACT_ONLY)
                            .build(),
                    /*logger=*/ null);
            while (!searchResultPage.getResults().isEmpty()) {
                for (int i = 0; i < searchResultPage.getResults().size(); i++) {
                    GenericDocument document =
                            searchResultPage.getResults().get(i).getGenericDocument();
                    Migrator migrator = migrators.get(document.getSchemaType());
                    GenericDocument newDocument;
                    if (currentVersion < finalVersion) {
                        newDocument = migrator.onUpgrade(currentVersion, finalVersion, document);
                    } else {
                        // if current version = final version. we will return empty active
                        // migrators at SchemaMigrationUtils.getActivityMigrators and won't reach
                        // here.
                        newDocument = migrator.onDowngrade(currentVersion, finalVersion, document);
                    }
                    if (!mDestinationTypes.contains(newDocument.getSchemaType())) {
                        // we exit before the new schema has been set to AppSearch. So no
                        // observable changes will be applied to stored schemas and documents.
                        // And the temp file will be deleted at close(), which will be triggered at
                        // the end of try-with-resources when using AppSearchMigrationHelper.
                        throw new AppSearchException(RESULT_INVALID_SCHEMA,
                                "Receive a migrated document with schema type: "
                                        + newDocument.getSchemaType()
                                        + ". But the schema types doesn't exist in the request");
                    }
                    Bundle bundle = newDocument.getBundle();
                    byte[] serializedMessage;
                    Parcel parcel = Parcel.obtain();
                    try {
                        parcel.writeBundle(bundle);
                        serializedMessage = parcel.marshall();
                    } finally {
                        parcel.recycle();
                    }
                    codedOutputStream.writeByteArrayNoTag(serializedMessage);
                }
                codedOutputStream.flush();
                migratedDocsCount += searchResultPage.getResults().size();
                searchResultPage = mAppSearchImpl.getNextPage(mPackageName,
                        searchResultPage.getNextPageToken());
                outputStream.flush();
            }
        }
        mAreDocumentsMigrated = true;
        if (schemaMigrationStatsBuilder != null) {
            schemaMigrationStatsBuilder.setMigratedDocumentCount(migratedDocsCount);
        }
    }

    /**
     * Reads {@link GenericDocument} from the temperate file and saves them to AppSearch.
     *
     * <p> This method should be only called once.
     *
     * @param responseBuilder a SetSchemaResponse builder whose result will be returned by this
     *                        function with any
     *                        {@link androidx.appsearch.app.SetSchemaResponse.MigrationFailure}
     *                        added in.
     * @return  the {@link SetSchemaResponse} for this
     *          {@link androidx.appsearch.app.AppSearchSession#setSchema} call.
     *
     * @throws IOException        on i/o problem
     * @throws AppSearchException on AppSearch problem
     */
    @NonNull
    @WorkerThread
    public SetSchemaResponse readAndPutDocuments(@NonNull SetSchemaResponse.Builder responseBuilder,
            SchemaMigrationStats.Builder schemaMigrationStatsBuilder)
            throws IOException, AppSearchException {
        Preconditions.checkState(mFile.exists(), "Internal temp file does not exist.");
        if (!mAreDocumentsMigrated) {
            return responseBuilder.build();
        }
        try (InputStream inputStream = new FileInputStream(mFile)) {
            CodedInputStream codedInputStream = CodedInputStream.newInstance(inputStream);
            int savedDocsCount = 0;
            while (!codedInputStream.isAtEnd()) {
                GenericDocument document = readDocumentFromInputStream(codedInputStream);
                try {
                    mAppSearchImpl.putDocument(mPackageName, mDatabaseName, document,
                            /*logger=*/ null);
                    savedDocsCount++;
                } catch (Throwable t) {
                    responseBuilder.addMigrationFailure(
                            new SetSchemaResponse.MigrationFailure(
                                    document.getNamespace(),
                                    document.getId(),
                                    document.getSchemaType(),
                                    throwableToFailedResult(t)));
                }
            }
            mAppSearchImpl.persistToDisk(PersistType.Code.FULL);
            if (schemaMigrationStatsBuilder != null) {
                schemaMigrationStatsBuilder.setSavedDocumentCount(savedDocsCount);
            }
        }
        return responseBuilder.build();
    }

    /**
     * Reads {@link GenericDocument} from given {@link CodedInputStream}.
     *
     * @param codedInputStream The codedInputStream to read from
     *
     * @throws IOException        on File operation error.
     */
    @NonNull
    private static GenericDocument readDocumentFromInputStream(
            @NonNull CodedInputStream codedInputStream) throws IOException {
        byte[] serializedMessage = codedInputStream.readByteArray();

        Bundle bundle;
        Parcel parcel = Parcel.obtain();
        try {
            parcel.unmarshall(serializedMessage, 0, serializedMessage.length);
            parcel.setDataPosition(0);
            bundle = parcel.readBundle();
        } finally {
            parcel.recycle();
        }

        return new GenericDocument(bundle);
    }

    @Override
    public void close() {
        mFile.delete();
    }
}
