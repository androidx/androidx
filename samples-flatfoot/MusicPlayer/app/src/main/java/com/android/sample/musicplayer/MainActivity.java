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

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.View;

import com.android.sample.musicplayer.MusicRepository.TrackMetadata;
import com.android.sample.musicplayer.adapter.MusicTrackListAdapter;
import com.android.support.lifecycle.LiveData;
import com.android.support.lifecycle.Observer;

import java.util.List;

/**
 * Our main activity.
 */
public class MainActivity extends BaseActivity {
    private RecyclerView mRecyclerView;

    private MusicTrackListAdapter mMusicTrackListAdapter;
    private int mCurrPlaybackState;

    private void updateFab() {
        final FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        switch (mCurrPlaybackState) {
            case MusicRepository.STATE_PLAYING:
                fab.setImageResource(R.drawable.ic_pause_white_36dp);
                break;
            case MusicRepository.STATE_PAUSED:
            case MusicRepository.STATE_STOPPED:
                fab.setImageResource(R.drawable.ic_play_arrow_white_36dp);
                break;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Start the service. From this point on there is no direct communication between the
        // activity and the service. Everything is done by updating LiveData objects in the
        // repository and observing / reacting to those changes.
        startService(new Intent(MusicService.ACTION_START).setPackage(
                "com.android.sample.musicplayer"));

        final MusicRepository musicRepository = MusicRepository.getInstance();
        LiveData<Integer> currentlyActiveTrackData = musicRepository.getCurrentlyActiveTrackData();
        currentlyActiveTrackData.observe(this, new Observer<Integer>() {
            @Override
            public void onChanged(@Nullable Integer integer) {
                if (mMusicTrackListAdapter != null) {
                    mMusicTrackListAdapter.setActiveTrackIndex(integer);
                }
            }
        });
        LiveData<Integer> stateData = musicRepository.getStateData();
        stateData.observe(this, new Observer<Integer>() {
            @Override
            public void onChanged(@Nullable Integer integer) {
                mCurrPlaybackState = integer;
                updateFab();
            }
        });

        final FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        updateFab();

        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mCurrPlaybackState == MusicRepository.STATE_INITIAL) {
                    // If the FAB is clicked in the initial state, start the playback from the
                    // first track
                    musicRepository.setTrack(0);
                } else {
                    // Otherwise we're past the initial state. Set the state to playing or
                    // paused based on the current state
                    musicRepository.setState((mCurrPlaybackState == MusicRepository.STATE_PLAYING)
                            ? MusicRepository.STATE_PAUSED : MusicRepository.STATE_PLAYING);
                }
            }
        });

        mRecyclerView = (RecyclerView) findViewById(R.id.recycler);
        final List<TrackMetadata> tracks = MusicRepository.getInstance().getTracks();
        mMusicTrackListAdapter = new MusicTrackListAdapter(tracks);
        mRecyclerView.setAdapter(mMusicTrackListAdapter);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
    }
}
