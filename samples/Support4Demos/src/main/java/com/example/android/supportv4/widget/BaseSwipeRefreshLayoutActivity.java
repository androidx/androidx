/*
 * Copyright (C) 2013 The Android Open Source Project
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

package com.example.android.supportv4.widget;

import android.os.Bundle;
import android.os.Handler;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.LayoutRes;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout.OnRefreshListener;

import com.example.android.supportv4.R;

/**
 * Example of using the SwipeRefreshLayout.
 */
abstract class BaseSwipeRefreshLayoutActivity extends FragmentActivity
        implements OnRefreshListener {

    public static final String[] TITLES = {
            "Henry IV (1)",
            "Henry V",
            "Henry VIII",
            "Richard II",
            "Richard III",
            "Merchant of Venice",
            "Othello",
            "King Lear",
            "Henry IV (1)",
            "Henry V",
            "Henry VIII",
            "Richard II",
            "Richard III",
            "Merchant of Venice",
            "Othello",
            "King Lear",
            "Henry IV (1)",
            "Henry V",
            "Henry VIII",
            "Richard II",
            "Richard III",
            "Merchant of Venice",
            "Othello",
            "King Lear",
            "Henry IV (1)",
            "Henry V",
            "Henry VIII",
            "Richard II",
            "Richard III",
            "Merchant of Venice",
            "Othello",
            "King Lear"
    };

    // Try a SUPER quick refresh to make sure we don't get extra refreshes
    // while the user's finger is still down.
    private static final boolean SUPER_QUICK_REFRESH = false;

    private SwipeRefreshLayout mSwipeRefreshWidget;

    private final Handler mHandler = new Handler();
    private MyViewModel mViewModel;

    public static class MyViewModel extends ViewModel {
        final LiveData<Event<Object>> refreshDone = new MutableLiveData<>();

        final Runnable mRefreshDone = new Runnable() {
            @Override
            public void run() {
                ((MutableLiveData) refreshDone).setValue(new Event<>(new Object()));
            }
        };
    }
    public static class Event<T> {
        boolean mHasBeenHandled = false;
        T mContent;

        public void setContent(T content) {
            this.mContent = content;
        }

        Event(T content) {
            this.mContent = content;
        }

        /**
         * Returns the mContent and prevents its use again.
         */
        T getContentIfNotHandled() {
            if (mHasBeenHandled) {
                return null;
            } else {
                mHasBeenHandled = true;
                return mContent;
            }
        }

        /**
         * Returns the mContent, even if it's already been handled.
         */
        T peekContent() {
            return mContent;
        }
    }

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setContentView(getLayoutId());

        mViewModel = new ViewModelProvider(this).get(MyViewModel.class);
        mViewModel.refreshDone.observe(this, event -> {
            if (event.getContentIfNotHandled() != null) {
                mSwipeRefreshWidget.setRefreshing(false);
                Toast.makeText(BaseSwipeRefreshLayoutActivity.this,
                        "Refreshing is completed. Long text is added here just to be better "
                                + "visible on screen",
                        Toast.LENGTH_LONG).show();
            }
        });

        mSwipeRefreshWidget = findViewById(R.id.swipe_refresh_widget);
        mSwipeRefreshWidget.setColorSchemeResources(R.color.color1, R.color.color2, R.color.color3,
                R.color.color4);
        mSwipeRefreshWidget.setOnRefreshListener(this);
    }

    @LayoutRes
    protected abstract int getLayoutId();

    @Override
    public void onRefresh() {
        refresh();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.swipe_refresh_menu, menu);
        return true;
    }

    /**
     * Click handler for the menu item to force a refresh.
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        final int id = item.getItemId();
        switch(id) {
            case R.id.force_refresh:
                mSwipeRefreshWidget.setRefreshing(true);
                refresh();
                return true;
        }
        return false;
    }

    private void refresh() {
        mHandler.removeCallbacks(mViewModel.mRefreshDone);
        mHandler.postDelayed(mViewModel.mRefreshDone, 5000);
    }
}