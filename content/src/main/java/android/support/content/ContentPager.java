/*
 * Copyright (C) 2017 The Android Open Source Project
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

package android.support.content;

import static android.support.v4.util.Preconditions.checkArgument;
import static android.support.v4.util.Preconditions.checkState;

import android.content.ContentResolver;
import android.database.CrossProcessCursor;
import android.database.Cursor;
import android.database.CursorWindow;
import android.database.CursorWrapper;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.OperationCanceledException;
import android.support.annotation.GuardedBy;
import android.support.annotation.IntDef;
import android.support.annotation.MainThread;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresPermission;
import android.support.annotation.VisibleForTesting;
import android.support.annotation.WorkerThread;
import android.support.v4.util.LruCache;
import android.util.Log;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.HashSet;
import java.util.Set;

/**
 * {@link ContentPager} provides support for loading "paged" data on a background thread
 * using the {@link ContentResolver} framework. This provides an effective compatibility
 * layer for the ContentResolver "paging" support added in Android O. Those Android O changes,
 * like this class, help reduce or eliminate the occurrence of expensive inter-process
 * shared memory operations (aka "CursorWindow swaps") happening on the UI thread when
 * working with remote providers.
 *
 * <p>The list of terms used in this document:
 *
 * <ol>"The provider" is a {@link android.content.ContentProvider} supplying data identified
 * by a specific content {@link Uri}. A provider is the source of data, and for the sake of
 * this documents, the provider resides in a remote process.

 * <ol>"supports paging" A provider supports paging when it returns a pre-paged {@link Cursor}
 * that honors the paging contract. See @link ContentResolver#QUERY_ARG_OFFSET} and
 * {@link ContentResolver#QUERY_ARG_LIMIT} for details on the contract.

 * <ol>"CursorWindow swaps" The process by which new data is loaded into a shared memory
 * via a CursorWindow instance. This is a prominent contributor to UI jank in applications
 * that use Cursor as backing data for UI elements like {@code RecyclerView}.
 *
 * <p><b>Details</b>
 *
 * <p>Data will be loaded from a content uri in one of two ways, depending on the runtime
 * environment and if the provider supports paging.
 *
 * <li>If the system is Android O and greater and the provider supports paging, the Cursor
 * will be returned, effectively unmodified, to a {@link ContentCallback} supplied by
 * your application.
 *
 * <li>If the system is less than Android O or the provider does not support paging, the
 * loader will fetch an unpaged Cursor from the provider. The unpaged Cursor will be held
 * by the ContentPager, and data will be copied into a new cursor in a background thread.
 * The new cursor will be returned to a {@link ContentCallback} supplied by your application.
 *
 * <p>In either cases, when an application employs this library it can generally assume
 * that there will be no CursorWindow swap. But picking the right limit for records can
 * help reduce or even eliminate some heavy lifting done to guard against swaps.
 *
 * <p>How do we avoid that entirely?
 *
 * <p><b>Picking a reasonable item limit</b>
 *
 * <p>Authors are encouraged to experiment with limits using real data and the widest column
 * projection they'll use in their app. The total number of records that will fit into shared
 * memory varies depending on multiple factors.
 *
 * <li>The number of columns being requested in the cursor projection. Limit the number
 * of columns, to reduce the size of each row.
 * <li>The size of the data in each column.
 * <li>the Cursor type.
 *
 * <p>If the cursor is running in-process, there may be no need for paging. Depending on
 * the Cursor implementation chosen there may be no shared memory/CursorWindow in use.
 * NOTE: If the provider is running in your process, you should implement paging support
 * inorder to make your app run fast and to consume the fewest resources possible.
 *
 * <p>In common cases where there is a low volume (in the hundreds) of records in the dataset
 * being queried, all of the data should easily fit in shared memory. A debugger can be handy
 * to understand with greater accuracy how many results can fit in shared memory. Inspect
 * the Cursor object returned from a call to
 * {@link ContentResolver#query(Uri, String[], String, String[], String)}. If the underlying
 * type is a {@link android.database.CrossProcessCursor} or
 * {@link android.database.AbstractWindowedCursor} it'll have a {@link CursorWindow} field.
 * Check {@link CursorWindow#getNumRows()}. If getNumRows returns less than
 * {@link Cursor#getCount}, then you've found something close to the max rows that'll
 * fit in a page. If the data in row is expected to be relatively stable in size, reduce
 * row count by 15-20% to get a reasonable max page size.
 *
 * <p><b>What if the limit I guessed was wrong?</b>

 * <p>The library includes safeguards that protect against situations where an author
 * specifies a record limit that exceeds the number of rows accessible without a CursorWindow swap.
 * In such a circumstance, the Cursor will be adapted to report a count ({Cursor#getCount})
 * that reflects only records available without CursorWindow swap. But this involves
 * extra work that can be eliminated with a correct limit.
 *
 * <p>In addition to adjusted coujnt, {@link #EXTRA_SUGGESTED_LIMIT} will be included
 * in cursor extras. When EXTRA_SUGGESTED_LIMIT is present in extras, the client should
 * strongly consider using this value as the limit for subsequent queries as doing so should
 * help avoid the ned to wrap pre-paged cursors.
 *
 * <p><b>Lifecycle and cleanup</b>
 *
 * <p>Cursors resulting from queries are owned by the requesting client. So they must be closed
 * by the client at the appropriate time.
 *
 * <p>However, the library retains an internal cache of content that needs to be cleaned up.
 * In order to cleanup, call {@link #reset()}.
 *
 * <p><b>Projections</b>
 *
 * <p>Note that projection is ignored when determining the identity of a query. When
 * adding or removing projection, clients should call {@link #reset()} to clear
 * cached data.
 */
public class ContentPager {

    @VisibleForTesting
    static final String CURSOR_DISPOSITION = "android.support.v7.widget.CURSOR_DISPOSITION";

    @IntDef(value = {
            ContentPager.CURSOR_DISPOSITION_COPIED,
            ContentPager.CURSOR_DISPOSITION_PAGED,
            ContentPager.CURSOR_DISPOSITION_REPAGED,
            ContentPager.CURSOR_DISPOSITION_WRAPPED
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface CursorDisposition {}

    /** The cursor size exceeded page size. A new cursor with with page data was created. */
    public static final int CURSOR_DISPOSITION_COPIED = 1;

    /**
     * The cursor was provider paged.
     */
    public static final int CURSOR_DISPOSITION_PAGED = 2;

    /** The cursor was pre-paged, but total size was larger than CursorWindow size. */
    public static final int CURSOR_DISPOSITION_REPAGED = 3;

    /**
     * The cursor was not pre-paged, but total size was smaller than page size.
     * Cursor wrapped to supply data in extras only.
     */
    public static final int CURSOR_DISPOSITION_WRAPPED = 4;

    /** @see ContentResolver#EXTRA_HONORED_ARGS */
    public static final String EXTRA_HONORED_ARGS = ContentResolver.EXTRA_HONORED_ARGS;

    /** @see ContentResolver#EXTRA_TOTAL_COUNT */
    public static final String EXTRA_TOTAL_COUNT = ContentResolver.EXTRA_TOTAL_COUNT;

    /** @see ContentResolver#QUERY_ARG_OFFSET */
    public static final String QUERY_ARG_OFFSET = ContentResolver.QUERY_ARG_OFFSET;

    /** @see ContentResolver#QUERY_ARG_LIMIT */
    public static final String QUERY_ARG_LIMIT = ContentResolver.QUERY_ARG_LIMIT;

    /** Denotes the requested limit, if the limit was not-honored. */
    public static final String EXTRA_REQUESTED_LIMIT = "android-support:extra-ignored-limit";

    /** Specifies a limit likely to fit in CursorWindow limit. */
    public static final String EXTRA_SUGGESTED_LIMIT = "android-support:extra-suggested-limit";

    private static final boolean DEBUG = false;
    private static final String TAG = "ContentPager";
    private static final int DEFAULT_CURSOR_CACHE_SIZE = 1;

    private final QueryRunner mQueryRunner;
    private final QueryRunner.Callback mQueryCallback;
    private final ContentResolver mResolver;
    private final Object mContentLock = new Object();
    private final @GuardedBy("mContentLock") Set<Query> mActiveQueries = new HashSet<>();
    private final @GuardedBy("mContentLock") CursorCache mCursorCache;

    private final Stats mStats = new Stats();

    /**
     * Creates a new ContentPager with a default cursor cache size of 1.
     */
    public ContentPager(ContentResolver resolver, QueryRunner queryRunner) {
        this(resolver, queryRunner, DEFAULT_CURSOR_CACHE_SIZE);
    }

    /**
     * Creates a new ContentPager.
     *
     * @param cursorCacheSize Specifies the size of the unpaged cursor cache. If you will
     *     only be querying a single content Uri, 1 is sufficient. If you wish to use
     *     a single ContentPager for queries against several independent Uris this number
     *     should be increased to reflect that. Remember that adding or modifying a
     *     query argument creates a new Uri.
     * @param resolver The content resolver to use when performing queries.
     * @param queryRunner The query running to use. This provides a means of executing
     *         queries on a background thread.
     */
    public ContentPager(
            @NonNull ContentResolver resolver,
            @NonNull QueryRunner queryRunner,
            int cursorCacheSize) {

        checkArgument(resolver != null, "'resolver' argument cannot be null.");
        checkArgument(queryRunner != null, "'queryRunner' argument cannot be null.");
        checkArgument(cursorCacheSize > 0, "'cursorCacheSize' argument must be greater than 0.");

        mResolver = resolver;
        mQueryRunner = queryRunner;
        mQueryCallback = new QueryRunner.Callback() {

            @WorkerThread
            @Override
            public @Nullable Cursor runQueryInBackground(Query query) {
                return loadContentInBackground(query);
            }

            @MainThread
            @Override
            public void onQueryFinished(Query query, Cursor cursor) {
                ContentPager.this.onCursorReady(query, cursor);
            }
        };

        mCursorCache = new CursorCache(cursorCacheSize);
    }

    /**
     * Initiates loading of content.
     * For details on all params but callback, see
     * {@link ContentResolver#query(Uri, String[], Bundle, CancellationSignal)}.
     *
     * @param uri The URI, using the content:// scheme, for the content to retrieve.
     * @param projection A list of which columns to return. Passing null will return
     *         the default project as determined by the provider. This can be inefficient,
     *         so it is best to supply a projection.
     * @param queryArgs A Bundle containing any arguments to the query.
     * @param cancellationSignal A signal to cancel the operation in progress, or null if none.
     * If the operation is canceled, then {@link OperationCanceledException} will be thrown
     * when the query is executed.
     * @param callback The callback that will receive the query results.
     *
     * @return A Query object describing the query.
     */
    @MainThread
    public @NonNull Query query(
            @NonNull @RequiresPermission.Read Uri uri,
            @Nullable String[] projection,
            @NonNull Bundle queryArgs,
            @Nullable CancellationSignal cancellationSignal,
            @NonNull ContentCallback callback) {

        checkArgument(uri != null, "'uri' argument cannot be null.");
        checkArgument(queryArgs != null, "'queryArgs' argument cannot be null.");
        checkArgument(callback != null, "'callback' argument cannot be null.");

        Query query = new Query(uri, projection, queryArgs, cancellationSignal, callback);

        if (DEBUG) Log.d(TAG, "Handling query: " + query);

        if (!mQueryRunner.isRunning(query)) {
            synchronized (mContentLock) {
                mActiveQueries.add(query);
            }
            mQueryRunner.query(query, mQueryCallback);
        }

        return query;
    }

    /**
     * Clears any cached data. This method must be called in order to cleanup runtime state
     * (like cursors).
     */
    @MainThread
    public void reset() {
        synchronized (mContentLock) {
            if (DEBUG) Log.d(TAG, "Clearing un-paged cursor cache.");
            mCursorCache.evictAll();

            for (Query query : mActiveQueries) {
                if (DEBUG) Log.d(TAG, "Canceling running query: " + query);
                mQueryRunner.cancel(query);
                query.cancel();
            }

            mActiveQueries.clear();
        }
    }

    @WorkerThread
    private Cursor loadContentInBackground(Query query) {
        if (DEBUG) Log.v(TAG, "Loading cursor for query: " + query);
        mStats.increment(Stats.EXTRA_TOTAL_QUERIES);

        synchronized (mContentLock) {
            // We have a existing unpaged-cursor for this query. Instead of running a new query
            // via ContentResolver, we'll just copy results from that.
            // This is the "compat" behavior.
            if (mCursorCache.hasEntry(query.getUri())) {
                if (DEBUG) Log.d(TAG, "Found unpaged results in cache for: " + query);
                return createPagedCursor(query);
            }
        }

        // We don't have an unpaged query, so we run the query using ContentResolver.
        // It may be that no query for this URI has ever been run, so no unpaged
        // results have been saved. Or, it may be the the provider supports paging
        // directly, and is returning a pre-paged result set...so no unpaged
        // cursor will ever be set.
        Cursor cursor = query.run(mResolver);
        mStats.increment(Stats.EXTRA_RESOLVED_QUERIES);

        //       for the window. If so, communicate the overflow back to the client.
        if (cursor == null) {
            Log.e(TAG, "Query resulted in null cursor. " + query);
            return null;
        }

        if (isProviderPaged(cursor)) {
            return processProviderPagedCursor(query, cursor);
        }

        // Cache the unpaged results so we can generate pages from them on subsequent queries.
        synchronized (mContentLock) {
            mCursorCache.put(query.getUri(), cursor);
            return createPagedCursor(query);
        }
    }

    @WorkerThread
    @GuardedBy("mContentLock")
    private Cursor createPagedCursor(Query query) {
        Cursor unpaged = mCursorCache.get(query.getUri());
        checkState(unpaged != null, "No un-paged cursor in cache, or can't retrieve it.");

        mStats.increment(Stats.EXTRA_COMPAT_PAGED);

        if (DEBUG) Log.d(TAG, "Synthesizing cursor for page: " + query);
        int count = Math.min(query.getLimit(), unpaged.getCount());

        // don't wander off the end of the cursor.
        if (query.getOffset() + query.getLimit() > unpaged.getCount()) {
            count = unpaged.getCount() % query.getLimit();
        }

        if (DEBUG) Log.d(TAG, "Cursor count: " + count);

        Cursor result = null;
        // If the cursor isn't advertising support for paging, but is in-fact smaller
        // than the page size requested, we just decorate the cursor with paging data,
        // and wrap it without copy.
        if (query.getOffset() == 0 && unpaged.getCount() < query.getLimit()) {
            result = new CursorView(
                    unpaged, unpaged.getCount(), CURSOR_DISPOSITION_WRAPPED);
        } else {
            // This creates an in-memory copy of the data that fits the requested page.
            // ContentObservers registered on InMemoryCursor are directly registered
            // on the unpaged cursor.
            result = new InMemoryCursor(
                    unpaged, query.getOffset(), count, CURSOR_DISPOSITION_COPIED);
        }

        mStats.includeStats(result.getExtras());
        return result;
    }

    @WorkerThread
    private @Nullable Cursor processProviderPagedCursor(Query query, Cursor cursor) {

        CursorWindow window = getWindow(cursor);
        int windowSize = cursor.getCount();
        if (window != null) {
            if (DEBUG) Log.d(TAG, "Returning provider-paged cursor.");
            windowSize = window.getNumRows();
        }

        // Android O paging APIs are *all* about avoiding CursorWindow swaps,
        // because the swaps need to happen on the UI thread in jank-inducing ways.
        // But, the APIs don't *guarantee* that no window-swapping will happen
        // when traversing a cursor.
        //
        // Here in the support lib, we can guarantee there is no window swapping
        // by detecting mismatches between requested sizes and window sizes.
        // When a mismatch is detected we can return a cursor that reports
        // a size bounded by its CursorWindow size, and includes a suggested
        // size to use for subsequent queries.

        if (DEBUG) Log.d(TAG, "Cursor window overflow detected. Returning re-paged cursor.");

        int disposition = (cursor.getCount() <= windowSize)
                ? CURSOR_DISPOSITION_PAGED
                : CURSOR_DISPOSITION_REPAGED;

        Cursor result = new CursorView(cursor, windowSize, disposition);
        Bundle extras = result.getExtras();

        // If the orig cursor reports a size larger than the window, suggest a better limit.
        if (cursor.getCount() > windowSize) {
            extras.putInt(EXTRA_REQUESTED_LIMIT, query.getLimit());
            extras.putInt(EXTRA_SUGGESTED_LIMIT, (int) (windowSize * .85));
        }

        mStats.increment(Stats.EXTRA_PROVIDER_PAGED);
        mStats.includeStats(extras);
        return result;
    }

    private CursorWindow getWindow(Cursor cursor) {
        if (cursor instanceof CursorWrapper) {
            return getWindow(((CursorWrapper) cursor).getWrappedCursor());
        }
        if (cursor instanceof CrossProcessCursor) {
            return ((CrossProcessCursor) cursor).getWindow();
        }
        // TODO: Any other ways we can find/access windows?
        return null;
    }

    // Called in the foreground when the cursor is ready for the client.
    @MainThread
    private void onCursorReady(Query query, Cursor cursor) {
        synchronized (mContentLock) {
            mActiveQueries.remove(query);
        }

        query.getCallback().onCursorReady(query, cursor);
    }

    /**
     * @return true if the cursor extras contains all of the signs of being paged.
     *     Technically we could also check SDK version since facilities for paging
     *     were added in SDK 26, but if it looks like a duck and talks like a duck
     *     itsa duck (especially if it helps with testing).
     */
    @WorkerThread
    private boolean isProviderPaged(Cursor cursor) {
        Bundle extras = cursor.getExtras();
        extras = extras != null ? extras : Bundle.EMPTY;
        String[] honoredArgs = extras.getStringArray(EXTRA_HONORED_ARGS);

        return (extras.containsKey(EXTRA_TOTAL_COUNT)
                && honoredArgs != null
                && contains(honoredArgs, QUERY_ARG_OFFSET)
                && contains(honoredArgs, QUERY_ARG_LIMIT));
    }

    private static <T> boolean contains(T[] array, T value) {
        for (T element : array) {
            if (value.equals(element)) {
                return true;
            }
        }
        return false;
    }

    /**
     * @return Bundle populated with existing extras (if any) as well as
     * all usefule paging related extras.
     */
    static Bundle buildExtras(
            @Nullable Bundle extras, int recordCount, @CursorDisposition int cursorDisposition) {

        if (extras == null || extras == Bundle.EMPTY) {
            extras = new Bundle();
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            extras = extras.deepCopy();
        }
        // else we modify cursor extras directly, cuz that's our only choice.

        extras.putInt(CURSOR_DISPOSITION, cursorDisposition);
        if (!extras.containsKey(EXTRA_TOTAL_COUNT)) {
            extras.putInt(EXTRA_TOTAL_COUNT, recordCount);
        }

        if (!extras.containsKey(EXTRA_HONORED_ARGS)) {
            extras.putStringArray(EXTRA_HONORED_ARGS, new String[]{
                    ContentPager.QUERY_ARG_OFFSET,
                    ContentPager.QUERY_ARG_LIMIT
            });
        }

        return extras;
    }

    /**
     * Builds a Bundle with offset and limit values suitable for with
     * {@link #query(Uri, String[], Bundle, CancellationSignal, ContentCallback)}.
     *
     * @param offset must be greater than or equal to 0.
     * @param limit can be any value. Only values greater than or equal to 0 are respected.
     *         If any other value results in no upper limit on results. Note that a well
     *         behaved client should probably supply a reasonable limit. See class
     *         documentation on how to select a limit.
     *
     * @return Mutable Bundle pre-populated with offset and limits vales.
     */
    public static @NonNull Bundle createArgs(int offset, int limit) {
        checkArgument(offset >= 0);
        Bundle args = new Bundle();
        args.putInt(ContentPager.QUERY_ARG_OFFSET, offset);
        args.putInt(ContentPager.QUERY_ARG_LIMIT, limit);
        return args;
    }

    /**
     * Callback by which a client receives results of a query.
     */
    public interface ContentCallback {
        /**
         * Called when paged cursor is ready. Null, if query failed.
         * @param query The query having been executed.
         * @param cursor the query results. Null if query couldn't be executed.
         */
        @MainThread
        void onCursorReady(@NonNull Query query, @Nullable Cursor cursor);
    }

    /**
     * Provides support for adding extras to a cursor. This is necessary
     * as a cursor returning an extras Bundle that is either Bundle.EMPTY
     * or null, cannot have information added to the cursor. On SDKs earlier
     * than M, there is no facility to replace the Bundle.
     */
    private static final class CursorView extends CursorWrapper {
        private final Bundle mExtras;
        private final int mSize;

        CursorView(Cursor delegate, int size, @CursorDisposition int disposition) {
            super(delegate);
            mSize = size;

            mExtras = buildExtras(delegate.getExtras(), delegate.getCount(), disposition);
        }

        @Override
        public int getCount() {
            return mSize;
        }

        @Override
        public Bundle getExtras() {
            return mExtras;
        }
    }

    /**
     * LruCache holding at most {@code maxSize} cursors. Once evicted a cursor
     * is immediately closed. The only cursor's held in this cache are
     * unpaged results. For this purpose the cache is keyed by the URI,
     * not the entire query. Cursors that are pre-paged by the provider
     * are never cached.
     */
    private static final class CursorCache extends LruCache<Uri, Cursor> {
        CursorCache(int maxSize) {
            super(maxSize);
        }

        @WorkerThread
        @Override
        protected void entryRemoved(
                boolean evicted, Uri uri, Cursor oldCursor, Cursor newCursor) {
            if (!oldCursor.isClosed()) {
                oldCursor.close();
            }
        }

        /** @return true if an entry is present for the Uri. */
        @WorkerThread
        @GuardedBy("mContentLock")
        boolean hasEntry(Uri uri) {
            return get(uri) != null;
        }
    }

    /**
     * Implementations of this interface provide the mechanism
     * for execution of queries off the UI thread.
     */
    public interface QueryRunner {
        /**
         * Execute a query.
         * @param query The query that will be run. This value should be handed
         *         back to the callback when ready to run in the background.
         * @param callback The callback that should be called to both execute
         *         the query (in the background) and to receive the results
         *         (in the foreground).
         */
        void query(@NonNull Query query, @NonNull Callback callback);

        /**
         * @param query The query in question.
         * @return true if the query is already running.
         */
        boolean isRunning(@NonNull Query query);

        /**
         * Attempt to cancel a (presumably) running query.
         * @param query The query in question.
         */
        void cancel(@NonNull Query query);

        /**
         * Callback that receives a cursor once a query as been executed on the Runner.
         */
        interface Callback {
            /**
             * Method called on background thread where actual query is executed. This is provided
             * by ContentPager.
             * @param query The query to be executed.
             */
            @Nullable Cursor runQueryInBackground(@NonNull Query query);

            /**
             * Called on main thread when query has completed.
             * @param query The completed query.
             * @param cursor The results in Cursor form. Null if not successfully completed.
             */
            void onQueryFinished(@NonNull Query query, @Nullable Cursor cursor);
        }
    }

    static final class Stats {

        /** Identifes the total number of queries handled by ContentPager. */
        static final String EXTRA_TOTAL_QUERIES = "android-support:extra-total-queries";

        /** Identifes the number of queries handled by content resolver. */
        static final String EXTRA_RESOLVED_QUERIES = "android-support:extra-resolved-queries";

        /** Identifes the number of pages produced by way of copying. */
        static final String EXTRA_COMPAT_PAGED = "android-support:extra-compat-paged";

        /** Identifes the number of pages produced directly by a page-supporting provider. */
        static final String EXTRA_PROVIDER_PAGED = "android-support:extra-provider-paged";

        // simple stats objects tracking paged result handling.
        private int mTotalQueries;
        private int mResolvedQueries;
        private int mCompatPaged;
        private int mProviderPaged;

        private void increment(String prop) {
            switch (prop) {
                case EXTRA_TOTAL_QUERIES:
                    ++mTotalQueries;
                    break;

                case EXTRA_RESOLVED_QUERIES:
                    ++mResolvedQueries;
                    break;

                case EXTRA_COMPAT_PAGED:
                    ++mCompatPaged;
                    break;

                case EXTRA_PROVIDER_PAGED:
                    ++mProviderPaged;
                    break;

                default:
                    throw new IllegalArgumentException("Unknown property: " + prop);
            }
        }

        private void reset() {
            mTotalQueries = 0;
            mResolvedQueries = 0;
            mCompatPaged = 0;
            mProviderPaged = 0;
        }

        void includeStats(Bundle bundle) {
            bundle.putInt(EXTRA_TOTAL_QUERIES, mTotalQueries);
            bundle.putInt(EXTRA_RESOLVED_QUERIES, mResolvedQueries);
            bundle.putInt(EXTRA_COMPAT_PAGED, mCompatPaged);
            bundle.putInt(EXTRA_PROVIDER_PAGED, mProviderPaged);
        }
    }
}
