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

package androidx.recyclerview.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;
import android.view.ContextThemeWrapper;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.test.R;

import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests for {@link DividerItemDecoration}.
 */
@SmallTest
@RunWith(AndroidJUnit4.class)
public class DividerItemDecorationTest {
    private static final String[] STRINGS = {"Foo", "Bar", "Great"};

    @Test
    public void testNullListDivider() {
        final Context context = InstrumentationRegistry.getContext();
        RecyclerView rv = new RecyclerView(context);
        rv.setLayoutManager(new LinearLayoutManager(context));
        rv.setAdapter(new MyAdapter(STRINGS));
        DividerItemDecoration decoration = new DividerItemDecoration(
                new ContextThemeWrapper(context, R.style.nullListDivider),
                DividerItemDecoration.HORIZONTAL);
        rv.addItemDecoration(decoration);
        rv.layout(0, 0, 1000, 1000);
        decoration.onDraw(new Canvas(), rv, null);
    }

    private static class MyAdapter extends RecyclerView.Adapter<MyAdapter.ViewHolder> {
        private String[] mDataset;

        public static class ViewHolder extends RecyclerView.ViewHolder {
            TextView mTextView;
            ViewHolder(TextView v) {
                super(v);
                mTextView = v;
            }
        }

        MyAdapter(String[] myDataset) {
            mDataset = myDataset;
        }

        @Override
        public MyAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new ViewHolder(new TextView(parent.getContext()));
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            holder.mTextView.setText(mDataset[position]);
        }

        @Override
        public int getItemCount() {
            return mDataset.length;
        }
    }
}
