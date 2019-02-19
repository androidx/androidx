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

package androidx.media2.exoplayer;

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP_PREFIX;

import android.annotation.SuppressLint;

import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.media2.exoplayer.external.C;
import androidx.media2.exoplayer.external.ExoPlayer;
import androidx.media2.exoplayer.external.Timeline;
import androidx.media2.exoplayer.external.source.CompositeMediaSource;
import androidx.media2.exoplayer.external.source.MediaPeriod;
import androidx.media2.exoplayer.external.source.MediaSource;
import androidx.media2.exoplayer.external.upstream.Allocator;
import androidx.media2.exoplayer.external.upstream.TransferListener;

/**
 * Wraps a {@link MediaSource} and exposes its duration.
 *
 * @hide
 */
@RestrictTo(LIBRARY_GROUP_PREFIX)
@SuppressLint("RestrictedApi") // TODO(b/68398926): Remove once RestrictedApi checks are fixed.
/* package */ class DurationProvidingMediaSource extends CompositeMediaSource<Void> {

    private final MediaSource mMediaSource;

    private Timeline mCurrentTimeline;

    DurationProvidingMediaSource(MediaSource mediaSource) {
        mMediaSource = mediaSource;
    }

    /**
     * Returns the duration of the wrapped source in milliseconds if known, or {@link C#TIME_UNSET}
     * otherwise.
     */
    public long getDurationMs() {
        return mCurrentTimeline == null ? C.TIME_UNSET :
                mCurrentTimeline.getWindow(
                        /* windowIndex= */ 0,
                        new Timeline.Window()).getDurationMs();
    }

    @Override
    public void prepareSourceInternal(ExoPlayer player, boolean isTopLevelSource,
            @Nullable TransferListener mediaTransferListener) {
        super.prepareSourceInternal(player, isTopLevelSource, mediaTransferListener);
        prepareChildSource(/* id= */ null, mMediaSource);
    }

    @Override
    public MediaPeriod createPeriod(MediaPeriodId id, Allocator allocator) {
        return mMediaSource.createPeriod(id, allocator);
    }

    @Override
    public void releasePeriod(MediaPeriod mediaPeriod) {
        mMediaSource.releasePeriod(mediaPeriod);
    }

    @Override
    protected void onChildSourceInfoRefreshed(
            Void id, MediaSource mediaSource, Timeline timeline, @Nullable Object manifest) {
        mCurrentTimeline = timeline;
        refreshSourceInfo(timeline, manifest);
    }

}
