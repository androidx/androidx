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

package androidx.pdf.viewer;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.pdf.data.DisplayData;
import androidx.pdf.fetcher.Fetcher;
import androidx.pdf.util.Preconditions;

import com.google.errorprone.annotations.CanIgnoreReturnValue;

/**
 * This base class offers logic to save the {@link DisplayData} it receives (without any heavy
 * contents) and restore it after a destroy/create cycle, including re-fetching (on a separate
 * thread) the actual contents from the saved data. In all cases, the actual contents are
 * delivered to the sub-class via {@link #onContentsAvailable} after its view hierarchy has been
 * built (after {@link #onCreateView}).
 * <p>
 * This class introduces one extra life-cycle callback:
 * <ul>
 * <li>{@link #onContentsAvailable}, as explained above, will run after {@link #onCreateView} and
 *     deliver the full file contents.
 * </ul>
 * In order to work as expected, it requires the parent frame of the viewer to be created at the
 * time it's being restored (which means during Activity#onCreate).
 * <p>
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public abstract class LoadingViewer extends Viewer {

    /** Constant used by subclasses to turn {@link #mSelfManagedContents} on. */
    protected static final boolean SELF_MANAGED_CONTENTS = true;

    /**
     * True if the subclass keeps references to the contents when the view hierarchy is
     * destroyed.
     */
    private final boolean mSelfManagedContents;

    /** A saved {@link #onContentsAvailable} runnable to be run after {@link #onCreateView}. */
    private Runnable mDelayedContentsAvailable;

    private boolean mHasContents;

    @NonNull
    protected Fetcher mFetcher;

    protected LoadingViewer() {
        this(false);
    }

    /**
     * Constructor allowing a subclass to prevent the default behaviour of re-fetching the contents
     * when just the view needs to be re-created. Useful for viewers that keep references to the
     * contents when their view hierarchy is destroyed .
     *
     * @param selfManagedContents Use {@link #SELF_MANAGED_CONTENTS} for the described setting.
     */
    protected LoadingViewer(boolean selfManagedContents) {
        this.mSelfManagedContents = selfManagedContents;
    }

    /** Feed this Viewer with contents to be displayed. */
    @NonNull
    @CanIgnoreReturnValue
    public Viewer feed(@NonNull DisplayData contents) {
        saveToArguments(contents);
        postContentsAvailable(contents, null);
        return this;
    }

    @NonNull
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedState) {
        View result = super.onCreateView(inflater, container, savedState);
        if (mFetcher == null) {
            // When changing device languages the system creates a new ImageViewer and the old
            // instance with a null fetcher stays around, even though it has been detached from the
            // view hierarchy, which is when this case happens. Logging it is sufficient as the
            // second instance will have the fetcher injected as expected.
            mViewState.set(ViewState.ERROR);
            return result;
        }

        if (!mHasContents && mDelayedContentsAvailable == null) {
            restoreContents(savedState);
        }
        return result;
    }

    @Override
    public void onStart() {
        if (mDelayedContentsAvailable != null) {
            mDelayedContentsAvailable.run();
        }
        super.onStart();
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (!mSelfManagedContents) {
            mHasContents = false;
        }
    }

    /**
     * Callback called when the full contents is re-loaded. This method should always run after
     * {@link #onCreateView} and only on the UI thread.
     *
     * @param contents   The fully-loaded contents.
     * @param savedState If this instance is reborn, the saved state that was given to onCreate.
     */
    @MainThread
    protected abstract void onContentsAvailable(
            @NonNull DisplayData contents, @Nullable Bundle savedState);

    /**
     * Posts a {@link #onContentsAvailable} method to be run as soon as permitted (when this Viewer
     * has its view hierarchy built up and {@link #onCreateView} has finished). It might run right
     * now if the Viewer is currently started.
     */
    protected void postContentsAvailable(final @NonNull DisplayData contents,
            @Nullable final Bundle savedState) {
        Preconditions.checkState(mDelayedContentsAvailable == null, "Already waits for contents");

        if (isStarted()) {
            onContentsAvailable(contents, savedState);
            mHasContents = true;
        } else {
            mDelayedContentsAvailable =
                    () -> {
                        Preconditions.checkState(
                                !mHasContents, "Received contents while restoring another copy");
                        onContentsAvailable(contents, savedState);
                        mDelayedContentsAvailable = null;
                        mHasContents = true;
                    };
        }
    }

    /** Sets the fetcher to be used by the viewer and returns the viewer. */
    @NonNull
    public LoadingViewer setFetcher(@NonNull Fetcher fetcher) {
        this.mFetcher = Preconditions.checkNotNull(fetcher);
        return this;
    }

    /**
     * Restores the contents of this Viewer when it is automatically restored by android.
     */
    protected void restoreContents(@Nullable Bundle savedState) {
        Preconditions.checkState(mFetcher != null, "must run after ViewerManager#inject");
        Bundle dataBundle = getArguments().getBundle(KEY_DATA);
        if (dataBundle != null) {
            try {
                DisplayData restoredData = DisplayData.fromBundle(dataBundle);
                postContentsAvailable(restoredData, savedState);
            } catch (Exception e) {
                // This can happen if the data is an instance of StreamOpenable, and the client
                // app that owns it has been killed by the system. We will still recover,
                // but log this.
                mViewState.set(ViewState.ERROR);
            }
        }
    }
}
