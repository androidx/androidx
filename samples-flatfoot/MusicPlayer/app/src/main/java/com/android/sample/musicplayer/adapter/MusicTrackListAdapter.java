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
package com.android.sample.musicplayer.adapter;

import android.databinding.DataBindingUtil;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.Adapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.android.sample.musicplayer.MusicRepository;
import com.android.sample.musicplayer.MusicRepository.TrackMetadata;
import com.android.sample.musicplayer.R;
import com.android.sample.musicplayer.databinding.MainRowBinding;

import java.util.List;

/**
 * Adapter for the list of music tracks.
 */
public class MusicTrackListAdapter extends Adapter<MusicTrackListAdapter.TrackBindingHolder> {
    private List<TrackMetadata> mTracks;
    private int mActiveTrackIndex;

    /**
     * Holder for the track row.
     */
    public static class TrackBindingHolder extends RecyclerView.ViewHolder {
        private MainRowBinding mViewDataBinding;

        public TrackBindingHolder(MainRowBinding viewDataBinding) {
            super(viewDataBinding.getRoot());
            mViewDataBinding = viewDataBinding;
        }

        public MainRowBinding getBinding() {
            return mViewDataBinding;
        }
    }

    public MusicTrackListAdapter(List<TrackMetadata> tracks) {
        mTracks = tracks;
    }

    @Override
    public TrackBindingHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        MainRowBinding binding = DataBindingUtil.inflate(LayoutInflater.from(parent.getContext()),
                R.layout.main_row, parent, false);
        return new TrackBindingHolder(binding);
    }

    @Override
    public void onBindViewHolder(TrackBindingHolder holder, final int position) {
        MainRowBinding binding = holder.getBinding();
        binding.setTrack(mTracks.get(position));
        binding.setHandler(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Update the LiveData-wrapped current track index directly on the repository.
                // Our service observes those changes and will start the flow of preparing and
                // playing back this track.
                MusicRepository.getInstance().setTrack(position);
            }
        });
        binding.getRoot().setSelected(position == mActiveTrackIndex);
        binding.executePendingBindings();
    }

    @Override
    public int getItemCount() {
        return mTracks.size();
    }

    public void setActiveTrackIndex(int activeTrackIndex) {
        if (mActiveTrackIndex != activeTrackIndex) {
            int previousActiveTrackIndex = mActiveTrackIndex;
            mActiveTrackIndex = activeTrackIndex;
            notifyItemChanged(previousActiveTrackIndex);
            notifyItemChanged(mActiveTrackIndex);
        }
    }
}
