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
    public static final int STATE_PLAYING = 0;
    public static final int STATE_PAUSED = 1;
    public static final int STATE_STOPPED = 2;

    private static MusicRepository sInstance;

    /**
     * Metadata for a single track.
     */
    public final class TrackMetadata {
        private final String mTitle;
        private final String mArtist;
        @RawRes private final int mTrackRes;

        public TrackMetadata(String title, String artist, @RawRes int trackRes) {
            this.mTitle = title;
            this.mArtist = artist;
            this.mTrackRes = trackRes;
        }

        public String getTitle() {
            return mTitle;
        }

        public String getArtist() {
            return mArtist;
        }

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
        mTracks.add(new TrackMetadata("Tilt You Better", "Dawn Lentil", R.raw.track_01));
        mTracks.add(new TrackMetadata("Moongirl", "The Weekdy", R.raw.track_02));
        mTracks.add(new TrackMetadata("Further", "The Linkdrinkers", R.raw.track_03));
        mTracks.add(new TrackMetadata("Back and Forth", "Marina Venti", R.raw.track_04));
        mTracks.add(new TrackMetadata("Let Me Hate You", "Juji Beans", R.raw.track_05));
        mTracks.add(new TrackMetadata("Thirsty", "Smiley Leftfield", R.raw.track_06));
        mTracks.add(new TrackMetadata("Cheap Deals", "Skia", R.raw.track_07));
        mTracks.add(new TrackMetadata("Don't Stop the Drilling", "Dusty Oilfield", R.raw.track_08));
        mTracks.add(new TrackMetadata("Million Regressions", "Lady BreakBuild", R.raw.track_09));

        mCurrentlyActiveTrackData = new LiveData<>();
        mCurrentlyActiveTrackData.setValue(-1);

        mStateData = new LiveData<>();
        mStateData.setValue(STATE_STOPPED);
    }

    public List<TrackMetadata> getTracks() {
        return Collections.unmodifiableList(mTracks);
    }

    public int getCurrentlyActiveTrack() {
        return mCurrentlyActiveTrackData.getValue();
    }

    public void setCurrentlyActiveTrack(int currentlyActiveTrack) {
        mCurrentlyActiveTrackData.setValue(currentlyActiveTrack);
    }

    public int getState() {
        return mStateData.getValue();
    }

    public void setState(int state) {
        mStateData.setValue(state);
    }

    public LiveData<Integer> getCurrentlyActiveTrackData() {
        return this.mCurrentlyActiveTrackData;
    }

    public LiveData<Integer> getStateData() {
        return mStateData;
    }
}
