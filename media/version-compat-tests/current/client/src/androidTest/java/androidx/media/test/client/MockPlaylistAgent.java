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

package androidx.media.test.client;

import androidx.media.MediaItem2;
import androidx.media.MediaMetadata2;
import androidx.media.MediaPlaylistAgent;

import java.util.List;
import java.util.concurrent.CountDownLatch;

/**
 * A mock implementation of {@link MediaPlaylistAgent} for testing.
 * <p>
 * Do not use mockito for {@link MediaPlaylistAgent}. Instead, use this.
 * Mocks created from mockito should not be shared across different threads.
 */
public class MockPlaylistAgent extends MediaPlaylistAgent {
    public final CountDownLatch mCountDownLatch = new CountDownLatch(1);

    public List<MediaItem2> mPlaylist;
    public MediaMetadata2 mMetadata;
    public MediaItem2 mCurrentMediaItem;
    public MediaItem2 mItem;
    public int mIndex = -1;
    public @RepeatMode int mRepeatMode = -1;
    public @ShuffleMode int mShuffleMode = -1;

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

    @Override
    public List<MediaItem2> getPlaylist() {
        return mPlaylist;
    }

    @Override
    public void setPlaylist(List<MediaItem2> list, MediaMetadata2 metadata) {
        mSetPlaylistCalled = true;
        mPlaylist = list;
        mMetadata = metadata;
        mCountDownLatch.countDown();
    }

    @Override
    public MediaMetadata2 getPlaylistMetadata() {
        return mMetadata;
    }

    @Override
    public void updatePlaylistMetadata(MediaMetadata2 metadata) {
        mUpdatePlaylistMetadataCalled = true;
        mMetadata = metadata;
        mCountDownLatch.countDown();
    }

    @Override
    public MediaItem2 getCurrentMediaItem() {
        return mCurrentMediaItem;
    }

    @Override
    public void addPlaylistItem(int index, MediaItem2 item) {
        mAddPlaylistItemCalled = true;
        mIndex = index;
        mItem = item;
        mCountDownLatch.countDown();
    }

    @Override
    public void removePlaylistItem(MediaItem2 item) {
        mRemovePlaylistItemCalled = true;
        mItem = item;
        mCountDownLatch.countDown();
    }

    @Override
    public void replacePlaylistItem(int index, MediaItem2 item) {
        mReplacePlaylistItemCalled = true;
        mIndex = index;
        mItem = item;
        mCountDownLatch.countDown();
    }

    @Override
    public void skipToPlaylistItem(MediaItem2 item) {
        mSkipToPlaylistItemCalled = true;
        mItem = item;
        mCountDownLatch.countDown();
    }

    @Override
    public void skipToPreviousItem() {
        mSkipToPreviousItemCalled = true;
        mCountDownLatch.countDown();
    }

    @Override
    public void skipToNextItem() {
        mSkipToNextItemCalled = true;
        mCountDownLatch.countDown();
    }

    @Override
    public int getRepeatMode() {
        return mRepeatMode;
    }

    @Override
    public void setRepeatMode(int repeatMode) {
        mSetRepeatModeCalled = true;
        mRepeatMode = repeatMode;
        mCountDownLatch.countDown();
    }

    @Override
    public int getShuffleMode() {
        return mShuffleMode;
    }

    @Override
    public void setShuffleMode(int shuffleMode) {
        mSetShuffleModeCalled = true;
        mShuffleMode = shuffleMode;
        mCountDownLatch.countDown();
    }
}
