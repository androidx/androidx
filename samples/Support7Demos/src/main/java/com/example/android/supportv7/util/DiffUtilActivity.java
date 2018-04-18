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
package com.example.android.supportv7.util;

import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.util.Pair;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.android.supportv7.Cheeses;
import com.example.android.supportv7.widget.adapter.SimpleStringAdapter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A sample activity that demonstrates usage if {@link android.support.v7.util.DiffUtil} with
 * a RecyclerView.
 */
public class DiffUtilActivity extends AppCompatActivity {
    private Random mRandom = new Random(System.nanoTime());

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LinearLayout ll = new LinearLayout(this);
        RecyclerView rv = new RecyclerView(this);
        Button shuffle = new Button(this);
        shuffle.setText("Shuffle");
        ll.addView(shuffle);
        ll.addView(rv);
        rv.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        rv.setLayoutManager(new LinearLayoutManager(this));
        List<String> cheeseList = createRandomCheeseList(Collections.<String>emptyList(), 50);
        final SimpleStringAdapter adapter =
                new SimpleStringAdapter(this, cheeseList.toArray(new String[cheeseList.size()]));
        rv.setAdapter(adapter);
        final AtomicBoolean refreshingList = new AtomicBoolean(false);
        shuffle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (refreshingList.getAndSet(true)) {
                    // already refreshing, do not allow modifications
                    return;
                }
                //noinspection unchecked
                new AsyncTask<List<String>, Void, Pair<List<String>, DiffUtil.DiffResult>>() {

                    @Override
                    protected Pair<List<String>, DiffUtil.DiffResult> doInBackground(
                            List<String>... lists) {
                        List<String> oldList = lists[0];
                        List<String> newList = createRandomCheeseList(oldList, 5);
                        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(
                                new MyCallback(oldList, newList));
                        //noinspection unchecked
                        return new Pair(newList, diffResult);
                    }

                    @Override
                    protected void onPostExecute(
                            Pair<List<String>, DiffUtil.DiffResult> resultPair) {
                        refreshingList.set(false);
                        adapter.setValues(resultPair.first);
                        resultPair.second.dispatchUpdatesTo(adapter);
                        Toast.makeText(DiffUtilActivity.this, "new list size "
                                + resultPair.first.size(), Toast.LENGTH_SHORT).show();
                    }
                }.execute(adapter.getValues());

            }
        });
        setContentView(ll);
    }

    private static class MyCallback extends DiffUtil.Callback {
        private final List<String> mOld;
        private final List<String> mNew;

        public MyCallback(List<String> old, List<String> aNew) {
            mOld = old;
            mNew = aNew;
        }

        @Override
        public int getOldListSize() {
            return mOld.size();
        }

        @Override
        public int getNewListSize() {
            return mNew.size();
        }

        @Override
        public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
            // for strings, content equality is the same as identitiy equality since we don't have
            // duplicates in this sample.
            return mOld.get(oldItemPosition).equals(mNew.get(newItemPosition));
        }

        @Override
        public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
            return mOld.get(oldItemPosition).equals(mNew.get(newItemPosition));
        }
    }

    private List<String> createRandomCheeseList(List<String> seed, int iterations) {
        List<String> output = new ArrayList<>();
        output.addAll(seed);
        for (int i = 0; i < iterations; i++) {
            switch (mRandom.nextInt(3)) {
                case 0: //add
                    output.add(mRandom.nextInt(1 + output.size()), getRandomCheese(output));
                    break;
                case 1: // remove
                    if (output.size() > 0) {
                        output.remove(mRandom.nextInt(output.size()));
                    }
                    break;
                case 2: // move
                    if (output.size() > 0) {
                        int from = mRandom.nextInt(output.size());
                        int to = mRandom.nextInt(output.size());
                        output.add(to, output.remove(from));
                    }
                    break;
            }
        }
        return output;
    }

    private String getRandomCheese(List<String> excludes) {
        String chosen = Cheeses.sCheeseStrings[mRandom.nextInt(Cheeses.sCheeseStrings.length)];
        while (excludes.contains(chosen)) {
            chosen = Cheeses.sCheeseStrings[mRandom.nextInt(Cheeses.sCheeseStrings.length)];
        }
        return chosen;
    }
}
