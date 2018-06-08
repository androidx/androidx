/*
 * Copyright (C) 2015 The Android Open Source Project
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
package com.example.android.supportv7.util;

import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SortedList;
import androidx.recyclerview.widget.SortedListAdapterCallback;

import com.example.android.supportv7.R;

/**
 * A sample activity that uses {@link SortedList} in combination with RecyclerView.
 */
public class SortedListActivity extends AppCompatActivity {
    private RecyclerView mRecyclerView;
    private LinearLayoutManager mLinearLayoutManager;
    private SortedListAdapter mAdapter;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.sorted_list_activity);
        mRecyclerView = findViewById(R.id.recycler_view);
        mRecyclerView.setHasFixedSize(true);
        mLinearLayoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(mLinearLayoutManager);
        mAdapter = new SortedListAdapter(getLayoutInflater(),
                new Item("buy milk"), new Item("wash the car"),
                new Item("wash the dishes"));
        mRecyclerView.setAdapter(mAdapter);
        mRecyclerView.setHasFixedSize(true);
        final EditText newItemTextView = findViewById(R.id.new_item_text_view);
        newItemTextView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
                if (id == EditorInfo.IME_ACTION_DONE &&
                        (keyEvent == null || keyEvent.getAction() == KeyEvent.ACTION_DOWN)) {
                    final String text = textView.getText().toString().trim();
                    if (text.length() > 0) {
                        mAdapter.addItem(new Item(text));
                    }
                    textView.setText("");
                    return true;
                }
                return false;
            }
        });
    }

    private static class SortedListAdapter extends RecyclerView.Adapter<TodoViewHolder> {
        SortedList<Item> mData;
        final LayoutInflater mLayoutInflater;
        public SortedListAdapter(LayoutInflater layoutInflater, Item... items) {
            mLayoutInflater = layoutInflater;
            mData = new SortedList<Item>(Item.class, new SortedListAdapterCallback<Item>(this) {
                @Override
                public int compare(Item t0, Item t1) {
                    if (t0.mIsDone != t1.mIsDone) {
                        return t0.mIsDone ? 1 : -1;
                    }
                    int txtComp = t0.mText.compareTo(t1.mText);
                    if (txtComp != 0) {
                        return txtComp;
                    }
                    if (t0.id < t1.id) {
                        return -1;
                    } else if (t0.id > t1.id) {
                        return 1;
                    }
                    return 0;
                }

                @Override
                public boolean areContentsTheSame(Item oldItem,
                        Item newItem) {
                    return oldItem.mText.equals(newItem.mText);
                }

                @Override
                public boolean areItemsTheSame(Item item1, Item item2) {
                    return item1.id == item2.id;
                }
            });
            for (Item item : items) {
                mData.add(item);
            }
        }

        public void addItem(Item item) {
            mData.add(item);
        }

        @Override
        public TodoViewHolder onCreateViewHolder(final ViewGroup parent, int viewType) {
            return new TodoViewHolder (
                    mLayoutInflater.inflate(R.layout.sorted_list_item_view, parent, false)) {
                @Override
                void onDoneChanged(boolean isDone) {
                    int adapterPosition = getAdapterPosition();
                    if (adapterPosition == RecyclerView.NO_POSITION) {
                        return;
                    }
                    mBoundItem.mIsDone = isDone;
                    mData.recalculatePositionOfItemAt(adapterPosition);
                }
            };
        }

        @Override
        public void onBindViewHolder(TodoViewHolder holder, int position) {
            holder.bindTo(mData.get(position));
        }

        @Override
        public int getItemCount() {
            return mData.size();
        }
    }

    abstract private static class TodoViewHolder extends RecyclerView.ViewHolder {
        final CheckBox mCheckBox;
        Item mBoundItem;
        public TodoViewHolder(View itemView) {
            super(itemView);
            mCheckBox = (CheckBox) itemView;
            mCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    if (mBoundItem != null && isChecked != mBoundItem.mIsDone) {
                        onDoneChanged(isChecked);
                    }
                }
            });
        }

        public void bindTo(Item item) {
            mBoundItem = item;
            mCheckBox.setText(item.mText);
            mCheckBox.setChecked(item.mIsDone);
        }

        abstract void onDoneChanged(boolean isChecked);
    }

    private static class Item {
        String mText;
        boolean mIsDone = false;
        final public int id;
        private static int idCounter = 0;

        public Item(String text) {
            id = idCounter ++;
            this.mText = text;
        }
    }
}
