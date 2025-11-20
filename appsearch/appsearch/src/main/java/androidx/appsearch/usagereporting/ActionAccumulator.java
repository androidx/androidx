/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.appsearch.usagereporting;

import android.content.Context;
import android.os.Parcel;
import android.util.Log;

import androidx.annotation.GuardedBy;
import androidx.annotation.VisibleForTesting;
import androidx.appsearch.app.AppSearchBatchResult;
import androidx.appsearch.app.AppSearchEnvironmentFactory;
import androidx.appsearch.app.AppSearchResult;
import androidx.appsearch.app.AppSearchSchema;
import androidx.appsearch.app.AppSearchSession;
import androidx.appsearch.app.DocumentClassFactory;
import androidx.appsearch.app.DocumentClassFactoryRegistry;
import androidx.appsearch.app.ExperimentalAppSearchApi;
import androidx.appsearch.app.GenericDocument;
import androidx.appsearch.app.PutDocumentsRequest;
import androidx.appsearch.exceptions.AppSearchException;
import androidx.appsearch.safeparcel.GenericDocumentParcel;
import androidx.appsearch.util.LogUtil;
import androidx.concurrent.futures.ResolvableFuture;
import androidx.core.util.Preconditions;

import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import org.jspecify.annotations.NonNull;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;

/**
 * Base class that accumulates {@link SearchAction} and {@link ClickAction} documents and stores
 * them into AppSearch, either immediately or after a timeout.
 *
 * @see ClickAction
 * @see SearchAction
 * @see TakenAction
 */
@ExperimentalAppSearchApi
// TODO(b/395157195): Use FutureUtil once it is in the appsearch package
public class ActionAccumulator {
    private static final String TAG = "AppSearchActionAccumul";

    private static final String CACHE_FILE_NAME = "accumulator_cache.pb";

    /** A timeout after which events will be sent to AppSearch. */
    private static final int TIMEOUT_MILLIS = 5000;

    /** A limit of how many documents are held in the cache before sending to AppSearch. */
    private static final int ACTION_CACHE_COUNT_LIMIT = 100;

    // Lock to protect access to mCache and to disk IO operations
    private final Object mLock = new Object();

    // List of TakenActions used to accumulate documents in memory before flushing. These are saved
    // to disk after each reported action, in case of app close prior to flush.
    @GuardedBy("mLock")
    private List<TakenAction> mCache = new ArrayList<>();

    // List of GenericDocuments loaded from the disk, as a result of TakenActions that were not
    // flushed to AppSearch
    @GuardedBy("mLock")
    private List<GenericDocument> mPriorDiskCache = new ArrayList<>();

    @GuardedBy("mLock")
    private Timer mTimer = new Timer();
    private File mCacheFile;
    private final AppSearchSession mAppSearchSession;
    private final Executor mExecutor;
    private final Context mContext;

    /**
     * Factory method to create an ActionAccumulator.
     *
     * <p>Initializing and using the ActionAccumulator requires that the provided {@link
     * AppSearchSession} has a database where {@link SearchAction} and {@link ClickAction} are set.
     *
     * @throws ExecutionException if either {@link SearchAction} or {@link ClickAction} are not set
     * in the provided {@link AppSearchSession}.
     */
    @NonNull
    public static ListenableFuture<ActionAccumulator> createAsync(@NonNull Context context,
            @NonNull AppSearchSession appSearchSession, @NonNull Executor executor) {
        Preconditions.checkNotNull(appSearchSession);
        Preconditions.checkNotNull(context);
        Preconditions.checkNotNull(executor);
        ActionAccumulator accumulator = new ActionAccumulator(appSearchSession, executor, context);
        return accumulator.initialize();
    }

    /** Private constructor (use {@link #createAsync}). */
    private ActionAccumulator(
            @NonNull AppSearchSession appSearchSession,
            @NonNull Executor executor,
            @NonNull Context context) {
        mAppSearchSession = Preconditions.checkNotNull(appSearchSession);
        mExecutor = Preconditions.checkNotNull(executor);
        mContext = Preconditions.checkNotNull(context);
    }

    /** Assert schemas are set and flushes previous cache to disk. */
    private ListenableFuture<ActionAccumulator> initialize() {
        // Transform the getSchema Future to a saveDocuments Future
        ListenableFuture<AppSearchBatchResult<String, Void>> priorCacheFuture =
                Futures.transformAsync(assertSchemasSetAsync(),
                        unused -> handlePriorCacheAsync(), mExecutor);

        // Transform the saveDocuments Future to an ActionAccumulator Future
        ListenableFuture<ActionAccumulator> accumulatorFuture =
                Futures.transform(priorCacheFuture, unused -> this, mExecutor);

        return accumulatorFuture;
    }

    /** Loads prior cache from disk and saves it to AppSearch if necessary. */
    private ListenableFuture<AppSearchBatchResult<String, Void>> handlePriorCacheAsync()
            throws AppSearchException {

        synchronized (mLock) {
            mCacheFile = getCacheFile();

            // Load any previously cached data from the file if needed
            mPriorDiskCache = readActionsFromDiskLocked();

            if (!mPriorDiskCache.isEmpty()) {
                if (LogUtil.DEBUG) {
                    Log.d(TAG, "Found " + mPriorDiskCache.size() + " documents not flushed from "
                            + "previous use of ActionAccumulator, flushing now.");
                }
                return saveDocumentsToAppSearchAsync();
            }
            return Futures.immediateFuture(
                    new AppSearchBatchResult.Builder<String, Void>().build());
        }
    }

    /**
     * Cancels timers and so we can test initialization/shutdown. This simulates closing the app
     * using the ActionAccumulator
     */
    @VisibleForTesting
    public void cancelTimer() {
        synchronized (mLock) {
            mTimer.cancel();
            mTimer.purge();
        }
    }

    /**
     * Reports a {@link TakenAction} and restarts the flush timer.
     */
    @SuppressWarnings("FutureReturnValueIgnored") // saveDocumentsToAppSearchAsync()
    @NonNull
    public ListenableFuture<AppSearchBatchResult<String, Void>> reportActionAsync(
            @NonNull TakenAction takenAction) {
        Preconditions.checkNotNull(takenAction);
        ResolvableFuture<AppSearchBatchResult<String, Void>> future = ResolvableFuture.create();

        boolean search = takenAction.getActionType() == ActionConstants.ACTION_TYPE_SEARCH;
        boolean click = takenAction.getActionType() == ActionConstants.ACTION_TYPE_CLICK;

        if (!search && !click) {
            // TODO(b/395157195): Handle additional action types if necessary
            return Futures.immediateFailedFuture(new IllegalArgumentException(
                    "Reported actions must be ClickActions or SearchActions"));
        }

        // We just received a search.
        // We need to iterate back through the cache and check if the last search has the same
        // query. If so, it's likely that fetchedResultCount changed due to the user loading more
        // results.
        synchronized (mLock) {
            mTimer.cancel();
            mTimer = new Timer();
            mTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    try {
                        saveDocumentsToAppSearchAsync();
                    } catch (AppSearchException e) {
                        Log.e(TAG, e.getMessage());
                    }
                }
            }, TIMEOUT_MILLIS);

            updateLastClickTimeOnResultLocked(takenAction);
            if (search) {
                SearchAction searchAction = (SearchAction) takenAction;
                // Possibly update prior actions
                boolean sameQueryFound = updateFetchedResultCountLocked(searchAction);

                // Only add if the last search wasn't updated by updateFetchedResultCount
                if (!sameQueryFound) {
                    mCache.add(takenAction);
                }
            } else if (click) {
                mCache.add(takenAction);
            }

            // Run on separate thread
            mExecutor.execute(() -> {
                try {
                    List<TakenAction> localList = null;
                    List<GenericDocument> localPriorCache = null;

                    // This will be executed in a new executor thread, so we have to acquire mLock
                    // here and it won't conflict with the acquisition in reportActionAsync.
                    // TODO(b/395157195): Combine logic with saveDocumentsToAppSearchAsync
                    synchronized (mLock) {
                        if (mCache.size() >= ACTION_CACHE_COUNT_LIMIT) {
                            mTimer.cancel();
                            // Delete what's on disk so it doesn't get saved to AppSearch again next
                            // time we initialize
                            boolean deleted = mCacheFile.delete();
                            if (!deleted) {
                                Log.e(TAG, "Failed to remove cache file for ActionAccumulator");
                            }

                            localList = mCache;
                            localPriorCache = mPriorDiskCache;
                            // Clear the cache afterwards
                            mCache = new ArrayList<>();
                            mPriorDiskCache = new ArrayList<>();
                        } else {
                            writeActionsToDiskLocked();
                        }
                    }

                    if ((localList == null || localList.isEmpty())
                            && (localPriorCache == null || localPriorCache.isEmpty())) {
                        future.set(new AppSearchBatchResult.Builder<String, Void>().build());
                        return;
                    }

                    // Send them to appsearch
                    PutDocumentsRequest.Builder putDocumentsRequestBuilder =
                            new PutDocumentsRequest.Builder();
                    if (localList != null) {
                        putDocumentsRequestBuilder.addTakenActions(localList);
                    }
                    if (localPriorCache != null) {
                        putDocumentsRequestBuilder.addTakenActionGenericDocuments(localPriorCache);
                    }
                    // Attempt to push everything in mCache and possible priorDiskCache to AppSearch
                    future.setFuture(
                            mAppSearchSession.putAsync(putDocumentsRequestBuilder.build()));
                } catch (Throwable t) {
                    Log.e(TAG, t.getMessage());
                    future.setException(t);
                }
            });
        }

        return future;
    }

    /**
     * Find the last added search action and update its fetchResultCount only if its query matches
     * the new search action's query. For example, if we have searches "app", "application", "app",
     * then receive a new search "app", we only want to replace the last one at index 2.
     *
     * @return true if we updated a previous action with the same query, otherwise false.
     */
    @GuardedBy("mLock")
    private boolean updateFetchedResultCountLocked(@NonNull SearchAction searchAction) {
        Preconditions.checkNotNull(searchAction);
        for (int i = mCache.size() - 1; i >= 0; i--) {
            TakenAction action = mCache.get(i);
            if (action.getActionType() == ActionConstants.ACTION_TYPE_SEARCH) {
                SearchAction searchCast = (SearchAction) action;
                if (searchCast.getQuery().equals(searchAction.getQuery())) {
                    // Same query, and recent. Update fetched result count for the prior search
                    mCache.set(i, new SearchAction.Builder(searchCast).setFetchedResultCount(
                            searchAction.getFetchedResultCount()).build());
                    return true;
                }
                break;
            }
        }
        return false;
    }

    /**
     * Update the time stay on result timestamp if the last reported action was a click.
     */
    @GuardedBy("mLock")
    private void updateLastClickTimeOnResultLocked(@NonNull TakenAction takenAction) {
        Preconditions.checkNotNull(takenAction);
        if (!mCache.isEmpty()) {
            TakenAction action = mCache.get(mCache.size() - 1);
            if (action.getActionType() == ActionConstants.ACTION_TYPE_CLICK) {
                // We found a click. If timeStayOnResult hasn't been set, it should be now
                // because we have gone back to the app and have done another search
                ClickAction clickCast = (ClickAction) action;
                if (clickCast.getTimeStayOnResultMillis() == -1) {
                    long timeStayOnResultMillis = takenAction.getActionTimestampMillis()
                            - clickCast.getActionTimestampMillis();
                    mCache.set(mCache.size() - 1, new ClickAction.Builder(clickCast)
                            .setTimeStayOnResultMillis(timeStayOnResultMillis).build());
                }
            }
        }
    }

    /** Saves actions to disk cache. */
    @GuardedBy("mLock")
    private void writeActionsToDiskLocked() throws AppSearchException {
        // TODO(b/395157195): Add mHasFlushedToDisk to avoid saving the same documents repeatedly
        // Convert each SearchAction to a parcelable (GenericDocumentParcel)
        List<GenericDocumentParcel> parcelList = new ArrayList<>(mCache.size());
        for (TakenAction takenAction : mCache) {
            parcelList.add(GenericDocument.fromDocumentClass(takenAction).getDocumentParcel());
        }

        Parcel parcel = Parcel.obtain();
        try {
            // 1. Write the entire list
            parcel.writeTypedList(parcelList);

            // 2. Convert to byte array
            byte[] data = parcel.marshall();

            // 3. Write to file
            try (FileOutputStream fos = new FileOutputStream(mCacheFile, /*append=*/false)) {
                fos.write(data);
                fos.flush();
            } catch (IOException e) {
                Log.e(TAG, "Error saving actions to disk", e);
            }
        } finally {
            parcel.recycle();
        }
    }

    /** Asserts that schemas are set for {@link SearchAction} and {@link ClickAction}. */
    private ListenableFuture<Void> assertSchemasSetAsync() {
        return Futures.transformAsync(
                mAppSearchSession.getSchemaAsync(),
                schemaResponse -> {
                    DocumentClassFactoryRegistry registry =
                            DocumentClassFactoryRegistry.getInstance();
                    DocumentClassFactory<?> searchFactory =
                            registry.getOrCreateFactory(SearchAction.class);
                    AppSearchSchema searchActionSchema = searchFactory.getSchema();

                    DocumentClassFactory<?> clickFactory =
                            registry.getOrCreateFactory(ClickAction.class);
                    AppSearchSchema clickActionSchema = clickFactory.getSchema();

                    // Check for SearchAction and ClickAction schemas. If either is not set, we need
                    // to set schema again. If either exists but the schema aren't equal (the schema
                    // was updated), we need to set schema again. If both exist, we can skip calling
                    // setSchema.
                    boolean searchActionSchemaUpToDate = false;
                    boolean clickActionSchemaUpToDate = false;
                    for (AppSearchSchema schema : schemaResponse.getSchemas()) {
                        if (schema.equals(searchActionSchema)) {
                            searchActionSchemaUpToDate = true;
                        } else if (schema.equals(clickActionSchema)) {
                            clickActionSchemaUpToDate = true;
                        }
                    }

                    // If either schema is missing or out of date, set schema
                    if (!searchActionSchemaUpToDate || !clickActionSchemaUpToDate) {
                        // TODO(b/395157195): Add just the necessary types without modifying other
                        //  types once possible
                        return Futures.immediateFailedFuture(new AppSearchException(
                                AppSearchResult.RESULT_INVALID_SCHEMA,
                                "ActionAccumulator must be used with an AppSearch database where "
                                        + "builtin:SearchAction and builtin:ClickAction are set."));
                    }

                    return Futures.immediateVoidFuture();

                }, mExecutor);
    }

    /** Returns the cache file to use for on-disk caching. */
    @VisibleForTesting
    @NonNull
    public File getCacheFile() {
        File cacheDir = AppSearchEnvironmentFactory.getEnvironmentInstance().getCacheDir(mContext);
        return new File(cacheDir, CACHE_FILE_NAME);
    }

    /** Reads the cache written on disk and converts it to {@link GenericDocument}s. */
    @GuardedBy("mLock")
    @SuppressWarnings("MixedMutabilityReturnType")
    @NonNull
    private List<GenericDocument> readActionsFromDiskLocked() {
        if (!mCacheFile.exists()) {
            return Collections.emptyList();
        }

        // Read the typed list of GenericDocumentParcel. If it fails to read due to some upgrade
        // with GenericDocumentParcel, ignore the file and continue.
        List<GenericDocumentParcel> parcelList = new ArrayList<>();
        try (FileInputStream fis = new FileInputStream(mCacheFile)) {
            // Use a version compatible with older Android versions than Tiramisu
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            int nRead;
            byte[] tempArray = new byte[1024];
            while ((nRead = fis.read(tempArray, 0, tempArray.length)) != -1) {
                buffer.write(tempArray, 0, nRead);
            }

            byte[] data = buffer.toByteArray();

            if (data.length == 0) {
                return Collections.emptyList();
            }
            parcelList = unmarshallTypedList(data);
        } catch (IOException e) {
            Log.e(TAG, "Error reading actions from disk", e);
        }

        List<GenericDocument> actions = new ArrayList<>();
        for (GenericDocumentParcel gdp : parcelList) {
            actions.add(new GenericDocument(gdp));
        }
        return actions;
    }

    /** Helper method that unmarshalls a typed list of GenericDocumentParcel from raw bytes. */
    @SuppressWarnings("MixedMutabilityReturnType")
    @NonNull
    private List<GenericDocumentParcel> unmarshallTypedList(byte[] data) {
        Parcel parcel = Parcel.obtain();
        try {
            parcel.unmarshall(data, 0, data.length);
            parcel.setDataPosition(0);

            List<GenericDocumentParcel> list = new ArrayList<>();
            parcel.readTypedList(list, GenericDocumentParcel.CREATOR);
            return list;
        } catch (IllegalStateException e) {
            // Can't parse the cache file. Clear the cache file to prevent this from happening again
            Log.d(TAG, "Failed to parse cached actions, clearing cache file.", e);
            boolean deleted = mCacheFile.delete();
            if (!deleted) {
                Log.e(TAG, "Failed to remove cache file for ActionAccumulator");
            }

            return ImmutableList.of();
        } finally {
            parcel.recycle();
        }
    }

    /**
     * Saves the documents in the cache to AppSearch and deletes the cache file.
     */
    @NonNull
    public ListenableFuture<AppSearchBatchResult<String, Void>> saveDocumentsToAppSearchAsync()
            throws AppSearchException {
        List<TakenAction> localList;
        List<GenericDocument> localPriorCache;
        synchronized (mLock) {
            mTimer.cancel();
            // Delete what's on disk so it doesn't get saved to AppSearch again next time we
            // initialize
            boolean deleted = mCacheFile.delete();
            if (!deleted) {
                Log.e(TAG, "Failed to remove cache file for ActionAccumulator");
            }

            localList = mCache;
            localPriorCache = mPriorDiskCache;
            // Clear the cache afterwards
            mCache = new ArrayList<>();
            mPriorDiskCache = new ArrayList<>();
        }

        if (localList.isEmpty() && localPriorCache.isEmpty()) {
            return Futures.immediateFuture(
                    new AppSearchBatchResult.Builder<String, Void>().build());
        }

        // Send them to appsearch
        PutDocumentsRequest.Builder putDocumentsRequestBuilder =
                new PutDocumentsRequest.Builder()
                        .addTakenActions(localList)
                        .addTakenActionGenericDocuments(localPriorCache);

        // Attempt to push everything in mCache and possible priorDiskCache to AppSearch
        return mAppSearchSession.putAsync(putDocumentsRequestBuilder.build());
    }
}
