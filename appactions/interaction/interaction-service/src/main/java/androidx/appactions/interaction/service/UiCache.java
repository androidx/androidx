/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.appactions.interaction.service;

import android.util.SizeF;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService.RemoteViewsFactory;

import androidx.annotation.GuardedBy;
import androidx.annotation.NonNull;
import androidx.appactions.interaction.capabilities.core.BaseExecutionSession;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;

/**
 * Cache for different types of supported UI (RemoteViews for general Android and TileLayout for
 * Wear).
 * <p>
 * When developers call our APIs to update UI, we don't immediately respect that and send it over
 * the service. Instead, we cache it here and wait for the GRPC impl to decide on an appropriate
 * time to return UI.
 */
@ThreadSafe
final class UiCache {

    private final Object mLock = new Object();
    @GuardedBy("mLock")
    private final Map<Integer, RemoteViewsFactory> mCachedRemoteViewsFactories = new HashMap<>();

    @GuardedBy("mLock")
    @Nullable
    private RemoteViews mCachedRemoteViews;
    @GuardedBy("mLock")
    @Nullable
    private SizeF mCachedRemoteViewsSize;
    @GuardedBy("mLock")
    @Nullable
    private TileLayoutInternal mCachedTileLayout;
    @GuardedBy("mLock")
    @Nullable
    private Set<Integer> mCachedChangedViewIds = new HashSet<>();
    // Needs to be reset after latest UiResponse has been rendered. That way can know there
    // is new UI that has been sent by app that must now be rendered on this turn.
    @GuardedBy("mLock")
    private boolean mUnreadUiResponse;

    /**
     * Caches a UiResponse for this particular {@link BaseExecutionSession}.
     */
    public void updateUiInternal(@NonNull UiResponse uiResponse) {
        synchronized (mLock) {
            mUnreadUiResponse = true;
            if (uiResponse.getRemoteViewsInternal() != null) {
                updateCachedRemoteViews(uiResponse.getRemoteViewsInternal());
            }
            if (uiResponse.getTileLayoutInternal() != null) {
                mCachedTileLayout = uiResponse.getTileLayoutInternal();
            }
        }
    }

    @Nullable
    RemoteViewsFactory onGetViewFactoryInternal(int viewId) {
        synchronized (mLock) {
            return mCachedRemoteViewsFactories.get(viewId);
        }
    }

    @Nullable
    RemoteViews getCachedRemoteViews() {
        synchronized (mLock) {
            return mCachedRemoteViews;
        }
    }

    @Nullable
    SizeF getCachedRemoteViewsSize() {
        synchronized (mLock) {
            return mCachedRemoteViewsSize;
        }
    }

    @NonNull
    Set<Integer> getCachedChangedViewIds() {
        synchronized (mLock) {
            return mCachedChangedViewIds;
        }
    }

    @Nullable
    TileLayoutInternal getCachedTileLayout() {
        synchronized (mLock) {
            return mCachedTileLayout;
        }
    }

    boolean hasUnreadUiResponse() {
        synchronized (mLock) {
            return mUnreadUiResponse;
        }
    }

    void resetUnreadUiResponse() {
        synchronized (mLock) {
            mUnreadUiResponse = false;
            mCachedRemoteViews = null;
            mCachedRemoteViewsSize = null;
            mCachedTileLayout = null;
            mCachedChangedViewIds = new HashSet<>();
        }
    }

    private void updateCachedRemoteViews(@NonNull RemoteViewsInternal remoteViewsInternal) {
        synchronized (mLock) {
            mCachedRemoteViews = remoteViewsInternal.getRemoteViews();
            mCachedRemoteViewsSize = remoteViewsInternal.getSize();
            mCachedRemoteViewsFactories.putAll(remoteViewsInternal.getRemoteViewsFactories());
            if (!remoteViewsInternal.getChangedViewIds().isEmpty()) {
                mCachedChangedViewIds = remoteViewsInternal.getChangedViewIds();
                // TODO(b/213520133): Here we should call onDataSetChanged() on RemoteViewsFactory.
                // https://developer.android.com/reference/android/widget/RemoteViewsService.RemoteViewsFactory#onDataSetChanged()
                // This call allows developer to update internal references. This is a blocking call
                // so we should probably move it to another thread.
            }
        }
    }
}
