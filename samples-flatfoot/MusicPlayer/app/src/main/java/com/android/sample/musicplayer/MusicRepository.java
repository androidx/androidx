/*
 * Copyright (C) 2016 The Android Open Source Project
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
package com.android.sample.musicplayer;

import android.support.annotation.RawRes;

import com.android.support.lifecycle.LiveData;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Music track / state repository.
 */
public class MusicRepository {
    public static final int STATE_INITIAL = 0;
    public static final int STATE_PLAYING = 1;
    public static final int STATE_PAUSED = 2;
    public static final int STATE_PREPARING = 3;
    public static final int STATE_STOPPED = 4;

    private static MusicRepository sInstance;

    /**
     * Metadata for a single track.
     */
    public static final class TrackMetadata {
        private final int mIndex;
        private final String mTitle;
        private final String mArtist;
        @RawRes private final int mTrackRes;

        public TrackMetadata(int index, String title, String artist, @RawRes int trackRes) {
            mIndex = index;
            mTitle = title;
            mArtist = artist;
            mTrackRes = trackRes;
        }

        public int getIndex() {
            return mIndex;
        }

        public String getTitle() {
            return mTitle;
        }

        public String getArtist() {
            return mArtist;
        }

        @RawRes
        public int getTrackRes() {
            return mTrackRes;
        }
    }

    private List<TrackMetadata> mTracks;

    private LiveData<Integer> mCurrentlyActiveTrackData;
    private LiveData<Integer> mStateData;

    /**
     * Gets the repository instance.
     */
    public static synchronized MusicRepository getInstance() {
        if (sInstance == null) {
            sInstance = new MusicRepository();
        }
        return sInstance;
    }

    private MusicRepository() {
        mTracks = new ArrayList<>(9);
        mTracks.add(new TrackMetadata(1, "Tilt You Better", "Dawn Lentil", R.raw.track1));
        mTracks.add(new TrackMetadata(2, "Moongirl", "The Weekdy", R.raw.track2));
        mTracks.add(new TrackMetadata(3, "Further", "The Linkdrinkers", R.raw.track3));
        mTracks.add(new TrackMetadata(4, "Back and Forth", "Marina Venti", R.raw.track4));
        mTracks.add(new TrackMetadata(5, "Let Me Hate You", "Juji Beans", R.raw.track5));
        mTracks.add(new TrackMetadata(6, "Thirsty", "Smiley Leftfield", R.raw.track6));
        mTracks.add(new TrackMetadata(7, "Cheap Deals", "Skia", R.raw.track7));
        mTracks.add(new TrackMetadata(8, "Don't Stop the Drilling", "Raw Oilfield", R.raw.track8));
        mTracks.add(new TrackMetadata(9, "Million Regressions", "Lady BreakBuild", R.raw.track9));

        mCurrentlyActiveTrackData = new LiveData<>();
        mCurrentlyActiveTrackData.setValue(-1);

        mStateData = new LiveData<>();
        mStateData.setValue(STATE_INITIAL);
    }

    /**
     * Returns the unmodifiable list of tracks in this repository.
     */
    public List<TrackMetadata> getTracks() {
        return Collections.unmodifiableList(mTracks);
    }

    /**
     * Goes to the specific track.
     */
    public void setTrack(int trackIndex) {
        mCurrentlyActiveTrackData.setValue(trackIndex);
    }

    /**
     * Goes to the next track.
     */
    public void goToNextTrack() {
        int nextSourceIndex = mCurrentlyActiveTrackData.getValue() + 1;
        if (nextSourceIndex == mTracks.size()) {
            nextSourceIndex = 0;
        }
        setTrack(nextSourceIndex);
    }

    /**
     * Goes to the previous track.
     */
    public void goToPreviousTrack() {
        int prevSourceIndex = mCurrentlyActiveTrackData.getValue() - 1;
        if (prevSourceIndex == -1) {
            prevSourceIndex = mTracks.size() - 1;
        }
        setTrack(prevSourceIndex);
    }

    /**
     * Sets the new value for the playback state.
     */
    public void setState(int state) {
        mStateData.setValue(state);
    }

    /**
     * Returns the {@link LiveData} object that wraps the currently active track index.
     */
    public LiveData<Integer> getCurrentlyActiveTrackData() {
        return this.mCurrentlyActiveTrackData;
    }

    /**
     * Returns the {@link LiveData} object that wraps the playback state.
     */
    public LiveData<Integer> getStateData() {
        return mStateData;
    }
}
