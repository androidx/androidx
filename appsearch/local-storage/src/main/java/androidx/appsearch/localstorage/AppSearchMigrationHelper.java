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

import static androidx.appsearch.app.AppSearchResult.throwableToFailedResult;

import android.os.Bundle;
import android.os.Parcel;

import androidx.annotation.NonNull;
import androidx.annotation.WorkerThread;
import androidx.appsearch.app.AppSearchSchema;
import androidx.appsearch.app.GenericDocument;
import androidx.appsearch.app.Migrator;
import androidx.appsearch.app.SearchResultPage;
import androidx.appsearch.app.SearchSpec;
import androidx.appsearch.app.SetSchemaResponse;
import androidx.appsearch.exceptions.AppSearchException;
import androidx.core.util.Preconditions;

import com.google.android.icing.protobuf.CodedInputStream;
import com.google.android.icing.protobuf.CodedOutputStream;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

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
    private final Map<String, Integer> mCurrentVersionMap;
    private final Map<String, Integer> mFinalVersionMap;
    private boolean mAreDocumentsMigrated = false;

    AppSearchMigrationHelper(@NonNull AppSearchImpl appSearchImpl,
            @NonNull Map<String, Integer> currentVersionMap,
            @NonNull Map<String, Integer> finalVersionMap,
            @NonNull String packageName,
            @NonNull String databaseName) throws IOException {
        mAppSearchImpl = Preconditions.checkNotNull(appSearchImpl);
        mCurrentVersionMap = Preconditions.checkNotNull(currentVersionMap);
        mFinalVersionMap = Preconditions.checkNotNull(finalVersionMap);
        mPackageName = Preconditions.checkNotNull(packageName);
        mDatabaseName = Preconditions.checkNotNull(databaseName);
        mFile = File.createTempFile(/*prefix=*/"appsearch", /*suffix=*/null);
    }

    /**
     * Queries all documents that need to be migrated to new version, and transform documents to
     * new version by passing them to the provided Transformer.
     *
     * <p>This method will be invoked on the background worker thread.
     *
     * @param schemaType   The schema that need be updated and migrated {@link GenericDocument}
     *                     under this type.
     * @param migrator     The {@link Migrator} that will upgrade or downgrade a
     *                     {@link GenericDocument} to new version.
     * @throws IOException        on i/o problem
     * @throws AppSearchException on AppSearch problem
     */
    @WorkerThread
    public void queryAndTransform(@NonNull String schemaType, @NonNull Migrator migrator)
            throws IOException, AppSearchException {
        Preconditions.checkState(mFile.exists(), "Internal temp file does not exist.");
        int currentVersion = mCurrentVersionMap.get(schemaType);
        int finalVersion = mFinalVersionMap.get(schemaType);
        try (FileOutputStream outputStream = new FileOutputStream(mFile)) {
            // TODO(b/151178558) change the output stream so that we can use it in platform
            CodedOutputStream codedOutputStream = CodedOutputStream.newInstance(outputStream);
            SearchResultPage searchResultPage = mAppSearchImpl.query(mPackageName, mDatabaseName,
                    /*queryExpression=*/"",
                    new SearchSpec.Builder()
                            .addFilterSchemas(schemaType)
                            .setTermMatch(SearchSpec.TERM_MATCH_PREFIX)
                            .build());
            while (!searchResultPage.getResults().isEmpty()) {
                for (int i = 0; i < searchResultPage.getResults().size(); i++) {
                    GenericDocument newDocument;
                    if (currentVersion < finalVersion) {
                        newDocument = migrator.onUpgrade(
                                currentVersion, finalVersion,
                                searchResultPage.getResults().get(i).getGenericDocument());
                    } else {
                        newDocument = migrator.onDowngrade(
                                currentVersion, finalVersion,
                                searchResultPage.getResults().get(i).getGenericDocument());
                    }
                    Bundle bundle = newDocument.getBundle();
                    Parcel parcel = Parcel.obtain();
                    parcel.writeBundle(bundle);
                    byte[] serializedMessage = parcel.marshall();
                    parcel.recycle();
                    codedOutputStream.writeByteArrayNoTag(serializedMessage);
                }
                codedOutputStream.flush();
                searchResultPage = mAppSearchImpl.getNextPage(searchResultPage.getNextPageToken());
                outputStream.flush();
            }
        }
        mAreDocumentsMigrated = true;
    }

    /**
     * Reads {@link GenericDocument} from the temperate file and saves them to AppSearch.
     *
     * <p> This method should be only called once.
     *
     * @return  the {@link SetSchemaResponse} for this
     *          {@link androidx.appsearch.app.AppSearchSession#setSchema} call.
     *
     * @throws IOException        on i/o problem
     * @throws AppSearchException on AppSearch problem
     */
    @NonNull
    public SetSchemaResponse readAndPutDocuments(@NonNull SetSchemaResponse.Builder responseBuilder)
            throws IOException, AppSearchException {
        Preconditions.checkState(mFile.exists(), "Internal temp file does not exist.");
        if (!mAreDocumentsMigrated) {
            return responseBuilder.build();
        }
        try (InputStream inputStream = new FileInputStream(mFile)) {
            CodedInputStream codedInputStream = CodedInputStream.newInstance(inputStream);
            while (!codedInputStream.isAtEnd()) {
                GenericDocument document = readDocumentFromInputStream(codedInputStream);
                try {
                    mAppSearchImpl.putDocument(mPackageName, mDatabaseName, document,
                            /*logger=*/ null);
                } catch (Throwable t) {
                    responseBuilder.addMigrationFailure(
                            new SetSchemaResponse.MigrationFailure.Builder()
                                    .setNamespace(document.getNamespace())
                                    .setSchemaType(document.getSchemaType())
                                    .setUri(document.getUri())
                                    .setAppSearchResult(throwableToFailedResult(t))
                                    .build());
                }
            }
            mAppSearchImpl.persistToDisk();
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

        Parcel parcel = Parcel.obtain();
        parcel.unmarshall(serializedMessage, 0, serializedMessage.length);
        parcel.setDataPosition(0);
        Bundle bundle = parcel.readBundle();
        parcel.recycle();

        return new GenericDocument(bundle);
    }

    @Override
    public void close() {
        mFile.delete();
    }
}
