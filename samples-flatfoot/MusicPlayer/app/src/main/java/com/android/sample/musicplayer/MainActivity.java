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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.Adapter;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.android.sample.musicplayer.MusicRepository.TrackMetadata;

import java.util.List;

/**
 * Our main activity.
 */
public class MainActivity extends AppCompatActivity {
    private MusicRepository mMusicRepository;

    private RecyclerView mRecyclerView;

    static class CustomViewHolder extends RecyclerView.ViewHolder {
        private ViewGroup mContainerView;
        private TextView mIndexView;
        private TextView mTextView;
        private TextView mArtistView;

        CustomViewHolder(View view) {
            super(view);
            this.mContainerView = (ViewGroup) view;
            this.mIndexView = (TextView) view.findViewById(R.id.index);
            this.mTextView = (TextView) view.findViewById(R.id.title);
            this.mArtistView = (TextView) view.findViewById(R.id.artist);
        }
    }

    private class ProgressReceiver extends BroadcastReceiver {
        public void onReceive(Context context, Intent intent) {
            updateFab();
            if (mRecyclerView != null) {
                mRecyclerView.getAdapter().notifyDataSetChanged();
            }
        }
    }

    private void updateFab() {
        final FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        switch (mMusicRepository.getState()) {
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

        mMusicRepository = MusicRepository.getInstance();

        startService(new Intent(MusicService.ACTION_INITIALIZE)
                .setPackage("com.android.sample.musicplayer"));

        final FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);

        updateFab();

        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                switch (mMusicRepository.getState()) {
                    case MusicRepository.STATE_STOPPED:
                        startService(new Intent(MusicService.ACTION_PLAY).setPackage(
                                "com.android.sample.musicplayer"));
                        break;
                    case MusicRepository.STATE_PLAYING:
                        startService(new Intent(MusicService.ACTION_PAUSE).setPackage(
                                "com.android.sample.musicplayer"));
                        break;
                    case MusicRepository.STATE_PAUSED:
                        startService(new Intent(MusicService.ACTION_PLAY).setPackage(
                                "com.android.sample.musicplayer"));
                        break;

                }
            }
        });

        // The filter's action is BROADCAST_ACTION
        IntentFilter statusIntentFilter = new IntentFilter(MusicService.BROADCAST_ACTION);
        // Instantiates a new progress receiver
        ProgressReceiver progressReceiver = new ProgressReceiver();
        // Registers the receiver and its intent filters
        LocalBroadcastManager.getInstance(this).registerReceiver(
                progressReceiver, statusIntentFilter);

        final LayoutInflater inflater = LayoutInflater.from(this);

        mRecyclerView = (RecyclerView) findViewById(R.id.recycler);
        final List<TrackMetadata> tracks = MusicRepository.getInstance().getTracks();
        mRecyclerView.setAdapter(new Adapter<CustomViewHolder>() {

            @Override
            public CustomViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
                View inflated = inflater.inflate(R.layout.main_row, parent, false);
                return new CustomViewHolder(inflated);
            }

            @Override
            public void onBindViewHolder(CustomViewHolder holder, final int position) {
                holder.mContainerView.setSelected(
                        position == mMusicRepository.getCurrentlyActiveTrack());
                holder.mIndexView.setText(Integer.toString(position + 1));
                holder.mTextView.setText(tracks.get(position).getTitle());
                holder.mArtistView.setText(tracks.get(position).getArtist());
            }

            @Override
            public int getItemCount() {
                return tracks.size();
            }
        });
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
    }
}
