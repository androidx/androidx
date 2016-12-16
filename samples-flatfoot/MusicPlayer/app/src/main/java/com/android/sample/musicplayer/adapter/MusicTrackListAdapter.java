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

import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.Adapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.android.sample.musicplayer.MusicRepository.TrackMetadata;
import com.android.sample.musicplayer.R;

import java.util.List;

/**
 * Adapter for the list of music tracks.
 */
public class MusicTrackListAdapter extends Adapter<MusicTrackListAdapter.CustomViewHolder> {
    private final LayoutInflater mInflater;
    private List<TrackMetadata> mTracks;
    private int mActiveTrackIndex;

    class CustomViewHolder extends RecyclerView.ViewHolder {
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

    public MusicTrackListAdapter(LayoutInflater inflater, List<TrackMetadata> tracks) {
        mInflater = inflater;
        mTracks = tracks;
    }

    @Override
    public CustomViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View inflated = mInflater.inflate(R.layout.main_row, parent, false);
        return new CustomViewHolder(inflated);
    }

    @Override
    public void onBindViewHolder(CustomViewHolder holder, final int position) {
        holder.mContainerView.setSelected(position == mActiveTrackIndex);
        holder.mIndexView.setText(Integer.toString(position + 1));
        holder.mTextView.setText(mTracks.get(position).getTitle());
        holder.mArtistView.setText(mTracks.get(position).getArtist());
    }

    @Override
    public int getItemCount() {
        return mTracks.size();
    }

    public void setActiveTrackIndex(int activeTrackIndex) {
        if (mActiveTrackIndex != activeTrackIndex) {
            mActiveTrackIndex = activeTrackIndex;
            notifyDataSetChanged();
        }
    }
}
