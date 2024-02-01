/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.loader.content;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.CancellationSignal;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContentResolverCompat;
import androidx.core.os.OperationCanceledException;
import androidx.loader.app.LoaderManager;

import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.Arrays;

/**
 * Static library support version of the framework's {@link android.content.CursorLoader}.
 * Used to write apps that run on platforms prior to Android 3.0.  When running
 * on Android 3.0 or above, this implementation is still used; it does not try
 * to switch to the framework's implementation.  See the framework SDK
 * documentation for a class overview.
 */
public class CursorLoader extends AsyncTaskLoader<Cursor> {
    private final ForceLoadContentObserver mObserver;

    private Uri mUri;
    private String[] mProjection;
    private String mSelection;
    private String[] mSelectionArgs;
    private String mSortOrder;

    private Cursor mCursor;
    private CancellationSignal mCancellationSignal;

    /* Runs on a worker thread */
    @Nullable
    @Override
    public Cursor loadInBackground() {
        synchronized (this) {
            if (isLoadInBackgroundCanceled()) {
                throw new OperationCanceledException();
            }
            mCancellationSignal = new CancellationSignal();
        }
        try {
            Cursor cursor = ContentResolverCompat.query(getContext().getContentResolver(),
                    mUri, mProjection, mSelection, mSelectionArgs, mSortOrder,
                    mCancellationSignal);
            if (cursor != null) {
                try {
                    // Ensure the cursor window is filled.
                    cursor.getCount();
                    cursor.registerContentObserver(mObserver);
                } catch (RuntimeException ex) {
                    cursor.close();
                    throw ex;
                }
            }
            return cursor;
        } finally {
            synchronized (this) {
                mCancellationSignal = null;
            }
        }
    }

    @Override
    public void cancelLoadInBackground() {
        super.cancelLoadInBackground();

        synchronized (this) {
            if (mCancellationSignal != null) {
                mCancellationSignal.cancel();
            }
        }
    }

    /* Runs on the UI thread */
    @Override
    public void deliverResult(@Nullable Cursor data) {
        if (isReset()) {
            // An async query came in while the loader is stopped
            if (data != null) {
                data.close();
            }
            return;
        }
        Cursor oldCursor = mCursor;
        mCursor = data;

        if (isStarted()) {
            super.deliverResult(data);
        }

        if (oldCursor != null && oldCursor != data && !oldCursor.isClosed()) {
            oldCursor.close();
        }
    }

    /**
     * Creates an empty unspecified CursorLoader.  You must follow this with
     * calls to {@link #setUri(Uri)}, {@link #setSelection(String)}, etc
     * to specify the query to perform.
     */
    public CursorLoader(@NonNull Context context) {
        super(context);
        mObserver = new ForceLoadContentObserver();
    }

    /**
     * Creates a fully-specified CursorLoader.  See
     * {@link ContentResolver#query(Uri, String[], String, String[], String)
     * ContentResolver.query()} for documentation on the meaning of the
     * parameters.  These will be passed as-is to that call.
     */
    public CursorLoader(@NonNull Context context, @NonNull Uri uri, @Nullable String[] projection,
            @Nullable String selection, @Nullable String[] selectionArgs,
            @Nullable String sortOrder) {
        super(context);
        mObserver = new ForceLoadContentObserver();
        mUri = uri;
        mProjection = projection;
        mSelection = selection;
        mSelectionArgs = selectionArgs;
        mSortOrder = sortOrder;
    }

    /**
     * Starts an asynchronous load of the contacts list data. When the result is ready the callbacks
     * will be called on the UI thread. If a previous load has been completed and is still valid
     * the result may be passed to the callbacks immediately.
     *
     * Must be called from the UI thread
     */
    @Override
    protected void onStartLoading() {
        if (mCursor != null) {
            deliverResult(mCursor);
        }
        if (takeContentChanged() || mCursor == null) {
            forceLoad();
        }
    }

    /**
     * Must be called from the UI thread
     */
    @Override
    protected void onStopLoading() {
        // Attempt to cancel the current load task if possible.
        cancelLoad();
    }

    @Override
    public void onCanceled(@Nullable Cursor data) {
        if (data != null && !data.isClosed()) {
            data.close();
        }
    }

    @Override
    protected void onReset() {
        super.onReset();

        // Ensure the loader is stopped
        onStopLoading();

        if (mCursor != null && !mCursor.isClosed()) {
            mCursor.close();
        }
        mCursor = null;
    }

    @NonNull
    public Uri getUri() {
        return mUri;
    }

    public void setUri(@NonNull Uri uri) {
        mUri = uri;
    }

    @Nullable
    public String[] getProjection() {
        return mProjection;
    }

    public void setProjection(@Nullable String[] projection) {
        mProjection = projection;
    }

    @Nullable
    public String getSelection() {
        return mSelection;
    }

    public void setSelection(@Nullable String selection) {
        mSelection = selection;
    }

    @Nullable
    public String[] getSelectionArgs() {
        return mSelectionArgs;
    }

    public void setSelectionArgs(@Nullable String[] selectionArgs) {
        mSelectionArgs = selectionArgs;
    }

    @Nullable
    public String getSortOrder() {
        return mSortOrder;
    }

    public void setSortOrder(@Nullable String sortOrder) {
        mSortOrder = sortOrder;
    }

    /**
     * @deprecated Consider using {@link LoaderManager#enableDebugLogging(boolean)} to understand
     * the series of operations performed by LoaderManager.
     */
    @SuppressWarnings("deprecation")
    @Override
    @Deprecated
    public void dump(@NonNull String prefix, @Nullable FileDescriptor fd,
            @NonNull PrintWriter writer, @Nullable String[] args) {
        super.dump(prefix, fd, writer, args);
        writer.print(prefix); writer.print("mUri="); writer.println(mUri);
        writer.print(prefix); writer.print("mProjection=");
                writer.println(Arrays.toString(mProjection));
        writer.print(prefix); writer.print("mSelection="); writer.println(mSelection);
        writer.print(prefix); writer.print("mSelectionArgs=");
                writer.println(Arrays.toString(mSelectionArgs));
        writer.print(prefix); writer.print("mSortOrder="); writer.println(mSortOrder);
        writer.print(prefix); writer.print("mCursor="); writer.println(mCursor);
    }
}
