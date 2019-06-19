/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.media2.session;

import android.view.Surface;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.Pair;
import androidx.media.AudioAttributesCompat;
import androidx.media2.common.MediaItem;
import androidx.media2.common.MediaMetadata;
import androidx.media2.common.SessionPlayer;
import androidx.media2.common.SubtitleData;
import androidx.media2.common.VideoSize;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;

/**
 * A mock implementation of {@link SessionPlayer} for testing.
 */
public class MockPlayer extends SessionPlayer {
    private static final int ITEM_NONE = -1;

    public final CountDownLatch mCountDownLatch;
    public final boolean mChangePlayerStateWithTransportControl;

    public boolean mPlayCalled;
    public boolean mPauseCalled;
    public boolean mPrepareCalled;
    public boolean mSeekToCalled;
    public boolean mSetPlaybackSpeedCalled;
    public long mSeekPosition;
    public long mCurrentPosition;
    public long mBufferedPosition;
    public float mPlaybackSpeed = 1.0f;
    @PlayerState
    public int mLastPlayerState;
    @BuffState
    public int mLastBufferingState;
    public long mDuration;

    public List<MediaItem> mPlaylist;
    public MediaMetadata mMetadata;
    public MediaItem mCurrentMediaItem;
    public MediaItem mItem;
    public int mIndex = -1;
    public int mPrevMediaItemIndex;
    public int mNextMediaItemIndex;
    @RepeatMode
    public int mRepeatMode = -1;
    @ShuffleMode
    public int mShuffleMode = -1;
    public VideoSize mVideoSize = new VideoSize(0, 0);
    public Surface mSurface;
    public TrackInfo mSelectedVideoTrack;
    public TrackInfo mSelectedAudioTrack;
    public TrackInfo mSelectedSubtitleTrack;
    public TrackInfo mSelectedMetadataTrack;

    public boolean mSetPlaylistCalled;
    public boolean mUpdatePlaylistMetadataCalled;
    public boolean mAddPlaylistItemCalled;
    public boolean mRemovePlaylistItemCalled;
    public boolean mReplacePlaylistItemCalled;
    public boolean mSkipToPlaylistItemCalled;
    public boolean mSkipToPreviousItemCalled;
    public boolean mSkipToNextItemCalled;
    public boolean mSetRepeatModeCalled;
    public boolean mSetShuffleModeCalled;

    private AudioAttributesCompat mAudioAttributes;

    public MockPlayer(int count) {
        this(count, false);
    }

    public MockPlayer(boolean changePlayerStateWithTransportControl) {
        this(0, changePlayerStateWithTransportControl);
    }

    private MockPlayer(int count, boolean changePlayerStateWithTransportControl) {
        mCountDownLatch = (count > 0) ? new CountDownLatch(count) : null;
        mChangePlayerStateWithTransportControl = changePlayerStateWithTransportControl;
        // This prevents MS2#play() from triggering SessionPlayer#prepare().
        mLastPlayerState = PLAYER_STATE_PAUSED;

        // Sets default audio attributes to prevent setVolume() from being called with the play().
        mAudioAttributes = new AudioAttributesCompat.Builder()
                .setUsage(AudioAttributesCompat.USAGE_MEDIA).build();
    }

    @Override
    public void close() {
        // no-op
    }

    @Override
    @NonNull
    public ListenableFuture<PlayerResult> play() {
        mPlayCalled = true;
        if (mCountDownLatch != null) {
            mCountDownLatch.countDown();
        }
        if (mChangePlayerStateWithTransportControl) {
            notifyPlayerStateChanged(PLAYER_STATE_PLAYING);
        }
        return new SyncListenableFuture(mCurrentMediaItem);
    }

    @Override
    @NonNull
    public ListenableFuture<PlayerResult> pause() {
        mPauseCalled = true;
        if (mCountDownLatch != null) {
            mCountDownLatch.countDown();
        }
        if (mChangePlayerStateWithTransportControl) {
            notifyPlayerStateChanged(PLAYER_STATE_PAUSED);
        }
        return new SyncListenableFuture(mCurrentMediaItem);
    }

    @Override
    @NonNull
    public ListenableFuture<PlayerResult> prepare() {
        mPrepareCalled = true;
        if (mCountDownLatch != null) {
            mCountDownLatch.countDown();
        }
        if (mChangePlayerStateWithTransportControl) {
            notifyPlayerStateChanged(PLAYER_STATE_PAUSED);
        }
        return new SyncListenableFuture(mCurrentMediaItem);
    }

    @Override
    @NonNull
    public ListenableFuture<PlayerResult> seekTo(long pos) {
        mSeekToCalled = true;
        mSeekPosition = pos;
        if (mCountDownLatch != null) {
            mCountDownLatch.countDown();
        }
        return new SyncListenableFuture(mCurrentMediaItem);
    }

    @Override
    public int getPlayerState() {
        return mLastPlayerState;
    }

    @Override
    public long getCurrentPosition() {
        return mCurrentPosition;
    }

    @Override
    public long getBufferedPosition() {
        return mBufferedPosition;
    }

    @Override
    public float getPlaybackSpeed() {
        return mPlaybackSpeed;
    }

    @Override
    public int getBufferingState() {
        return mLastBufferingState;
    }

    @Override
    public long getDuration() {
        return mDuration;
    }

    public void notifyPlayerStateChanged(final int state) {
        mLastPlayerState = state;

        List<Pair<PlayerCallback, Executor>> callbacks = getCallbacks();
        for (Pair<PlayerCallback, Executor> pair : callbacks) {
            final PlayerCallback callback = pair.first;
            pair.second.execute(new Runnable() {
                @Override
                public void run() {
                    callback.onPlayerStateChanged(MockPlayer.this, state);
                }
            });
        }
    }

    public void notifyCurrentMediaItemChanged(final MediaItem item) {
        List<Pair<PlayerCallback, Executor>> callbacks = getCallbacks();
        for (Pair<PlayerCallback, Executor> pair : callbacks) {
            final PlayerCallback callback = pair.first;
            pair.second.execute(new Runnable() {
                @Override
                public void run() {
                    callback.onCurrentMediaItemChanged(MockPlayer.this, item);
                }
            });
        }
    }

    public void notifyBufferingStateChanged(final MediaItem item,
            final @BuffState int buffState) {
        List<Pair<PlayerCallback, Executor>> callbacks = getCallbacks();
        for (Pair<PlayerCallback, Executor> pair : callbacks) {
            final PlayerCallback callback = pair.first;
            pair.second.execute(new Runnable() {
                @Override
                public void run() {
                    callback.onBufferingStateChanged(MockPlayer.this, item, buffState);
                }
            });
        }
    }

    public void notifyPlaybackSpeedChanged(final float speed) {
        List<Pair<PlayerCallback, Executor>> callbacks = getCallbacks();
        for (Pair<PlayerCallback, Executor> pair : callbacks) {
            final PlayerCallback callback = pair.first;
            pair.second.execute(new Runnable() {
                @Override
                public void run() {
                    callback.onPlaybackSpeedChanged(MockPlayer.this, speed);
                }
            });
        }
    }

    public void notifySeekCompleted(final long position) {
        List<Pair<PlayerCallback, Executor>> callbacks = getCallbacks();
        for (Pair<PlayerCallback, Executor> pair : callbacks) {
            final PlayerCallback callback = pair.first;
            pair.second.execute(new Runnable() {
                @Override
                public void run() {
                    callback.onSeekCompleted(MockPlayer.this, position);
                }
            });
        }
    }

    public void notifyTrackInfoChanged(final List<TrackInfo> trackInfos) {
        List<Pair<PlayerCallback, Executor>> callbacks = getCallbacks();
        for (Pair<PlayerCallback, Executor> pair : callbacks) {
            final PlayerCallback callback = pair.first;
            pair.second.execute(new Runnable() {
                @Override
                public void run() {
                    callback.onTrackInfoChanged(MockPlayer.this, trackInfos);
                }
            });
        }
    }

    public void notifyTrackSelected(final TrackInfo trackInfo) {
        switch (trackInfo.getTrackType()) {
            case TrackInfo.MEDIA_TRACK_TYPE_VIDEO:
                mSelectedVideoTrack = trackInfo;
                break;
            case TrackInfo.MEDIA_TRACK_TYPE_AUDIO:
                mSelectedAudioTrack = trackInfo;
                break;
            case TrackInfo.MEDIA_TRACK_TYPE_SUBTITLE:
                mSelectedSubtitleTrack = trackInfo;
                break;
            case TrackInfo.MEDIA_TRACK_TYPE_METADATA:
                mSelectedMetadataTrack = trackInfo;
                break;
        }

        List<Pair<PlayerCallback, Executor>> callbacks = getCallbacks();
        for (Pair<PlayerCallback, Executor> pair : callbacks) {
            final PlayerCallback callback = pair.first;
            pair.second.execute(new Runnable() {
                @Override
                public void run() {
                    callback.onTrackSelected(MockPlayer.this, trackInfo);
                }
            });
        }
    }

    public void notifyTrackDeselected(final TrackInfo trackInfo) {
        switch (trackInfo.getTrackType()) {
            case TrackInfo.MEDIA_TRACK_TYPE_VIDEO:
                mSelectedVideoTrack = null;
                break;
            case TrackInfo.MEDIA_TRACK_TYPE_AUDIO:
                mSelectedAudioTrack = null;
                break;
            case TrackInfo.MEDIA_TRACK_TYPE_SUBTITLE:
                mSelectedSubtitleTrack = null;
                break;
            case TrackInfo.MEDIA_TRACK_TYPE_METADATA:
                mSelectedMetadataTrack = null;
                break;
        }

        List<Pair<PlayerCallback, Executor>> callbacks = getCallbacks();
        for (Pair<PlayerCallback, Executor> pair : callbacks) {
            final PlayerCallback callback = pair.first;
            pair.second.execute(new Runnable() {
                @Override
                public void run() {
                    callback.onTrackDeselected(MockPlayer.this, trackInfo);
                }
            });
        }
    }

    @Override
    @NonNull
    public ListenableFuture<PlayerResult> setAudioAttributes(
            @NonNull AudioAttributesCompat attributes) {
        mAudioAttributes = attributes;
        return new SyncListenableFuture(mCurrentMediaItem);
    }

    @Override
    public AudioAttributesCompat getAudioAttributes() {
        return mAudioAttributes;
    }

    @Override
    @NonNull
    public ListenableFuture<PlayerResult> setPlaybackSpeed(float speed) {
        mSetPlaybackSpeedCalled = true;
        mPlaybackSpeed = speed;
        if (mCountDownLatch != null) {
            mCountDownLatch.countDown();
        }
        return new SyncListenableFuture(mCurrentMediaItem);
    }

    /////////////////////////////////////////////////////////////////////////////////
    // Playlist APIs
    /////////////////////////////////////////////////////////////////////////////////

    @Override
    public List<MediaItem> getPlaylist() {
        return mPlaylist;
    }

    @Override
    @NonNull
    public ListenableFuture<PlayerResult> setMediaItem(@NonNull MediaItem item) {
        mItem = item;
        mCurrentMediaItem = item;
        mCountDownLatch.countDown();
        return new SyncListenableFuture(mCurrentMediaItem);
    }

    @Override
    @NonNull
    public ListenableFuture<PlayerResult> setPlaylist(
            @NonNull List<MediaItem> list, MediaMetadata metadata) {
        mSetPlaylistCalled = true;
        mPlaylist = list;
        mMetadata = metadata;
        mCountDownLatch.countDown();
        return new SyncListenableFuture(mCurrentMediaItem);
    }

    @Override
    public MediaMetadata getPlaylistMetadata() {
        return mMetadata;
    }

    @Override
    @NonNull
    public ListenableFuture<PlayerResult> updatePlaylistMetadata(MediaMetadata metadata) {
        mUpdatePlaylistMetadataCalled = true;
        mMetadata = metadata;
        mCountDownLatch.countDown();
        return new SyncListenableFuture(mCurrentMediaItem);
    }

    @Override
    public MediaItem getCurrentMediaItem() {
        return mCurrentMediaItem;
    }

    @Override
    public int getCurrentMediaItemIndex() {
        if (mPlaylist == null) {
            return ITEM_NONE;
        }
        return mPlaylist.indexOf(mCurrentMediaItem);
    }

    @Override
    public int getPreviousMediaItemIndex() {
        return mPrevMediaItemIndex;
    }

    @Override
    public int getNextMediaItemIndex() {
        return mNextMediaItemIndex;
    }

    @Override
    @NonNull
    public ListenableFuture<PlayerResult> addPlaylistItem(int index, @NonNull MediaItem item) {
        // TODO: check for invalid index
        mAddPlaylistItemCalled = true;
        mIndex = index;
        mItem = item;
        mCountDownLatch.countDown();
        return new SyncListenableFuture(mCurrentMediaItem);
    }

    @Override
    @NonNull
    public ListenableFuture<PlayerResult> removePlaylistItem(int index) {
        // TODO: check for invalid index
        mRemovePlaylistItemCalled = true;
        mIndex = index;
        mCountDownLatch.countDown();
        return new SyncListenableFuture(mCurrentMediaItem);
    }

    @Override
    @NonNull
    public ListenableFuture<PlayerResult> replacePlaylistItem(int index, @NonNull MediaItem item) {
        // TODO: check for invalid index
        mReplacePlaylistItemCalled = true;
        mIndex = index;
        mItem = item;
        mCountDownLatch.countDown();
        return new SyncListenableFuture(mCurrentMediaItem);
    }

    @Override
    @NonNull
    public ListenableFuture<PlayerResult> skipToPlaylistItem(int index) {
        // TODO: check for invalid index
        mSkipToPlaylistItemCalled = true;
        mIndex = index;
        if (mPlaylist != null && index >= 0 && index < mPlaylist.size()) {
            mItem = mPlaylist.get(index);
            mCurrentMediaItem = mItem;
        }
        mCountDownLatch.countDown();
        return new SyncListenableFuture(mCurrentMediaItem);
    }

    @Override
    @NonNull
    public ListenableFuture<PlayerResult> skipToPreviousPlaylistItem() {
        mSkipToPreviousItemCalled = true;
        mCountDownLatch.countDown();
        return new SyncListenableFuture(mCurrentMediaItem);
    }

    @Override
    @NonNull
    public ListenableFuture<PlayerResult> skipToNextPlaylistItem() {
        mSkipToNextItemCalled = true;
        mCountDownLatch.countDown();
        return new SyncListenableFuture(mCurrentMediaItem);
    }

    @Override
    public int getRepeatMode() {
        return mRepeatMode;
    }

    @Override
    @NonNull
    public ListenableFuture<PlayerResult> setRepeatMode(int repeatMode) {
        mSetRepeatModeCalled = true;
        mRepeatMode = repeatMode;
        mCountDownLatch.countDown();
        return new SyncListenableFuture(mCurrentMediaItem);
    }

    @Override
    public int getShuffleMode() {
        return mShuffleMode;
    }

    @Override
    @NonNull
    public ListenableFuture<PlayerResult> setShuffleMode(int shuffleMode) {
        mSetShuffleModeCalled = true;
        mShuffleMode = shuffleMode;
        mCountDownLatch.countDown();
        return new SyncListenableFuture(mCurrentMediaItem);
    }

    public void notifyShuffleModeChanged() {
        final int shuffleMode = mShuffleMode;
        List<Pair<PlayerCallback, Executor>> callbacks = getCallbacks();
        for (Pair<PlayerCallback, Executor> pair : callbacks) {
            final PlayerCallback callback = pair.first;
            pair.second.execute(new Runnable() {
                @Override
                public void run() {
                    callback.onShuffleModeChanged(MockPlayer.this, shuffleMode);
                }
            });
        }
    }

    public void notifyRepeatModeChanged() {
        final int repeatMode = mRepeatMode;
        List<Pair<PlayerCallback, Executor>> callbacks = getCallbacks();
        for (Pair<PlayerCallback, Executor> pair : callbacks) {
            final PlayerCallback callback = pair.first;
            pair.second.execute(new Runnable() {
                @Override
                public void run() {
                    callback.onRepeatModeChanged(MockPlayer.this, repeatMode);
                }
            });
        }
    }

    public void notifyPlaylistChanged() {
        final List<MediaItem> list = mPlaylist;
        final MediaMetadata metadata = mMetadata;
        List<Pair<PlayerCallback, Executor>> callbacks = getCallbacks();
        for (Pair<PlayerCallback, Executor> pair : callbacks) {
            final PlayerCallback callback = pair.first;
            pair.second.execute(new Runnable() {
                @Override
                public void run() {
                    callback.onPlaylistChanged(MockPlayer.this, list, metadata);
                }
            });
        }
    }

    public void notifyPlaylistMetadataChanged() {
        final MediaMetadata metadata = mMetadata;
        List<Pair<PlayerCallback, Executor>> callbacks = getCallbacks();
        for (Pair<PlayerCallback, Executor> pair : callbacks) {
            final PlayerCallback callback = pair.first;
            pair.second.execute(new Runnable() {
                @Override
                public void run() {
                    callback.onPlaylistMetadataChanged(MockPlayer.this, metadata);
                }
            });
        }
    }

    @Override
    @NonNull
    public VideoSize getVideoSizeInternal() {
        if (mVideoSize == null) {
            mVideoSize = new VideoSize(0, 0);
        }
        return mVideoSize;
    }

    void notifyVideoSizeChanged(final VideoSize videoSize) {
        mVideoSize = videoSize;
        final MediaItem dummyItem = TestUtils.createMediaItem("onVideoSizeChanged");

        List<Pair<PlayerCallback, Executor>> callbacks = getCallbacks();
        for (Pair<PlayerCallback, Executor> pair : callbacks) {
            final PlayerCallback callback = pair.first;
            pair.second.execute(new Runnable() {
                @Override
                public void run() {
                    callback.onVideoSizeChangedInternal(MockPlayer.this, dummyItem, videoSize);
                }
            });
        }
    }

    @Override
    @Nullable
    public TrackInfo getSelectedTrackInternal(int trackType) {
        switch (trackType) {
            case TrackInfo.MEDIA_TRACK_TYPE_VIDEO:
                return mSelectedVideoTrack;
            case TrackInfo.MEDIA_TRACK_TYPE_AUDIO:
                return mSelectedAudioTrack;
            case TrackInfo.MEDIA_TRACK_TYPE_SUBTITLE:
                return mSelectedSubtitleTrack;
            case TrackInfo.MEDIA_TRACK_TYPE_METADATA:
                return mSelectedMetadataTrack;
        }
        return null;
    }

    @Override
    @NonNull
    public ListenableFuture<PlayerResult> setSurfaceInternal(@Nullable Surface surface) {
        mSurface = surface;
        return new SyncListenableFuture(mCurrentMediaItem);
    }

    void notifySubtitleData(@NonNull final MediaItem item, @NonNull final TrackInfo track,
            @NonNull final SubtitleData data) {
        List<Pair<PlayerCallback, Executor>> callbacks = getCallbacks();
        for (Pair<PlayerCallback, Executor> pair : callbacks) {
            final PlayerCallback callback = pair.first;
            pair.second.execute(new Runnable() {
                @Override
                public void run() {
                    callback.onSubtitleData(MockPlayer.this, item, track, data);
                }
            });
        }
    }
}
