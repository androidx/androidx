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

package com.example.android.support.content.demos;

import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.contentpager.content.ContentPager;
import androidx.contentpager.content.ContentPager.ContentCallback;
import androidx.contentpager.content.LoaderQueryRunner;
import androidx.contentpager.content.Query;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.BaseTransientBottomBar;
import com.google.android.material.snackbar.Snackbar;

import java.util.Locale;

/**
 * ContentPager demo activity.
 */
public class ContentPagerDemoActivity extends AppCompatActivity {

    private static final int PAGE_SIZE = 20;

    private RecyclerView mRecycler;
    private ContentPager mPager;
    private Adapter mAdapter;
    private FloatingActionButton mFab;
    private Menu mMenu;
    private int mCurrentPage = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_demo);

        ContentPager.QueryRunner runner = new LoaderQueryRunner(this, getLoaderManager());
        mPager = new ContentPager(getContentResolver(), runner);
        mAdapter = new Adapter(mPager, PAGE_SIZE);

        mRecycler = (RecyclerView) findViewById(R.id.list);
        mRecycler.setLayoutManager(new LinearLayoutManager(this));
        mRecycler.setAdapter(mAdapter);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mPager.reset();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_demo, menu);
        mMenu = menu;
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case R.id.action_load:
                onLoadContent();
                break;
            case R.id.action_previous:
                onLoadPreviousPage();
                break;
            case R.id.action_next:
                onLoadNextPage();
                break;
        }
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_load) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void onLoadContent() {
        mAdapter.reset(UnpagedDemoDataProvider.URI);
        mCurrentPage = 0;
        mAdapter.loadPage(mCurrentPage);

        updateOptionsMenu();
    }

    private void onLoadNextPage() {
        mAdapter.loadPage(mCurrentPage + 1);

        updateOptionsMenu();
    }

    private void onLoadPreviousPage() {
        mAdapter.loadPage(mCurrentPage - 1);

        updateOptionsMenu();
    }

    private void updateOptionsMenu() {
        MenuItem prev = mMenu.findItem(R.id.action_previous);
        MenuItem next = mMenu.findItem(R.id.action_next);

        int lastPage = (UnpagedDemoDataProvider.TOTAL_SIZE / PAGE_SIZE) - 1;
        prev.setEnabled(mCurrentPage > 0);
        next.setEnabled(mCurrentPage < lastPage);
    }

    private void msg(String msg) {
        Snackbar.make(
                mRecycler,
                msg, BaseTransientBottomBar.LENGTH_LONG)
                .setAction("Action", null).show();
    }

    private final class Adapter extends RecyclerView.Adapter<Holder> implements ContentCallback {

        private final ContentPager mPager;
        private final int mPageSize;

        private Uri mUri;
        private Cursor mCursor;

        private Adapter(ContentPager pager, int pageSize) {
            mPager = pager;
            mPageSize = pageSize;
        }

        private void reset(Uri uri) {
            mUri = uri;
            mCursor = null;
        }

        void loadPage(int page) {
            if (page < 0 || page >= (UnpagedDemoDataProvider.TOTAL_SIZE / PAGE_SIZE)) {
                throw new IndexOutOfBoundsException();
            }

            mCurrentPage = page;
            int offset = mCurrentPage * mPageSize;
            mPager.query(mUri, null, ContentPager.createArgs(offset, mPageSize), null, this);
        }

        @Override
        public Holder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new Holder(new TextView(ContentPagerDemoActivity.this));
        }

        @Override
        public void onBindViewHolder(Holder holder, int position) {
            if (!mCursor.moveToPosition(position)) {
                holder.view.setText("Nope, couldn't position cursor to: " + position);
                return;
            }

            holder.view.setText(String.format(Locale.US,
                    "%d.%d (%d): %s",
                    mCurrentPage,
                    mCursor.getInt(0),
                    mCursor.getLong(2),
                    mCursor.getString(1)));
        }

        @Override
        public int getItemCount() {
            return mCursor == null ? 0 : mCursor.getCount();
        }

        @Override
        public void onCursorReady(@NonNull Query query, Cursor cursor) {
            if (cursor == null) {
                msg("Content query returned a null cursor: " + query.getUri());
            }

            mCurrentPage = query.getOffset() / mPageSize;
            mCursor = cursor;
            notifyDataSetChanged();
        }

        private int getCorpusSize(Bundle extras) {
            return extras.getInt(ContentPager.EXTRA_TOTAL_COUNT, -1);
        }
    }

    private class Holder extends RecyclerView.ViewHolder {

        public final TextView view;

        private Holder(TextView view) {
            super(view);
            this.view = view;
        }
    }
}
