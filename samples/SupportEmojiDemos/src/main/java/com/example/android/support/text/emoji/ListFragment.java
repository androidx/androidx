/*
 * Copyright (C) 2017 The Android Open Source Project
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

package com.example.android.support.text.emoji;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import java.util.List;

/**
 * UI to list all supported emojis.
 */

public class ListFragment extends Fragment {

    private ListView mListView;

    static ListFragment newInstance() {
        ListFragment fragment = new ListFragment();
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_list, container, false);
        mListView = view.findViewById(R.id.list);
        loadList();
        return view;
    }

    private void loadList() {
        final ArrayAdapter<EmojiRepo.EmojiData> adapter = new MyArrayAdapter(getActivity(),
                R.layout.list_item_emoji, R.id.text, EmojiRepo.getEmojis());
        final int index = mListView.getFirstVisiblePosition();
        mListView.setAdapter(adapter);
        mListView.setSelection(index);
    }

    private static class MyArrayAdapter extends ArrayAdapter<EmojiRepo.EmojiData> {
        MyArrayAdapter(Context context, int resource, int textViewResourceId,
                List<EmojiRepo.EmojiData> objects) {
            super(context, resource, textViewResourceId, objects);
        }

        @NonNull
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view = super.getView(position, convertView, parent);
            ViewHolder holder = (ViewHolder) view.getTag();
            if (holder == null) {
                holder = new ViewHolder();
                holder.mEmojiTextView = view.findViewById(R.id.emoji);
                holder.mTextView = view.findViewById(R.id.text);
            }

            EmojiRepo.EmojiData item = getItem(position);
            holder.mEmojiTextView.setText(item.getEmoji());
            holder.mTextView.setText(item.getCodepointString());
            holder.mTextView.setContentDescription(holder.mTextView.getText());
            return view;
        }
    }

    private static class ViewHolder {
        TextView mEmojiTextView;
        TextView mTextView;
    }

}
