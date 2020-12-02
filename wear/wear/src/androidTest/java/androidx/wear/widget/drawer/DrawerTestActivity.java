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

package androidx.wear.widget.drawer;

import android.app.Activity;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.ArrayMap;
import android.view.Gravity;

import androidx.annotation.IntDef;
import androidx.wear.test.R;
import androidx.wear.widget.drawer.WearableDrawerLayout.DrawerStateCallback;
import androidx.wear.widget.drawer.WearableNavigationDrawerView.WearableNavigationDrawerAdapter;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Map;

/**
 * Test {@link Activity} for {@link WearableDrawerLayout} and implementations of {@link
 * androidx.wear.widget.drawer.WearableDrawerView}.
 */
public class DrawerTestActivity extends Activity {

    private static final int DRAWER_SIZE = 5;
    private static final String STYLE_EXTRA = "style";
    private static final String OPEN_TOP_IN_ONCREATE_EXTRA = "openTopInOnCreate";
    private static final String OPEN_BOTTOM_IN_ONCREATE_EXTRA = "openBottomInOnCreate";
    private static final String CLOSE_FIRST_DRAWER_OPENED = "closeFirstDrawerOpened";
    private static final Map<Integer, Integer> STYLE_TO_RES_ID = new ArrayMap<>();

    static {
        STYLE_TO_RES_ID.put(
                DrawerStyle.BOTH_DRAWER_NAV_MULTI_PAGE,
                R.layout.test_multi_page_nav_drawer_layout);
        STYLE_TO_RES_ID.put(
                DrawerStyle.BOTH_DRAWER_NAV_SINGLE_PAGE,
                R.layout.test_single_page_nav_drawer_layout);
        STYLE_TO_RES_ID.put(
                DrawerStyle.ONLY_ACTION_DRAWER_WITH_TITLE,
                R.layout.test_only_action_drawer_with_title_layout);

    }

    private final Handler mMainThreadHandler = new Handler(Looper.getMainLooper());
    private final WearableNavigationDrawerAdapter mDrawerAdapter =
            new WearableNavigationDrawerAdapter() {
                @Override
                public String getItemText(int pos) {
                    return Integer.toString(pos);
                }

                @Override
                public Drawable getItemDrawable(int pos) {
                    return getDrawable(android.R.drawable.star_on);
                }

                @Override
                public int getCount() {
                    return DRAWER_SIZE;
                }
            };
    private WearableActionDrawerView mActionDrawer;
    private WearableDrawerLayout mDrawerLayout;
    private WearableNavigationDrawerView mNavigationDrawer;
    private final Runnable mCloseTopDrawerRunnable =
            new Runnable() {
                @Override
                public void run() {
                    mNavigationDrawer.getController().closeDrawer();
                }
            };
    private final DrawerStateCallback mCloseFirstDrawerOpenedCallback =
            new DrawerStateCallback() {
                @Override
                public void onDrawerOpened(WearableDrawerLayout layout,
                        WearableDrawerView drawerView) {
                    mMainThreadHandler.postDelayed(mCloseTopDrawerRunnable, 1000);
                }
            };
    @DrawerStyle private int mNavigationStyle;
    private boolean mOpenTopDrawerInOnCreate;
    private boolean mOpenBottomDrawerInOnCreate;
    private boolean mCloseFirstDrawerOpened;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        parseIntent(getIntent());

        setContentView(STYLE_TO_RES_ID.get(mNavigationStyle));

        mDrawerLayout = (WearableDrawerLayout) findViewById(R.id.drawer_layout);
        mNavigationDrawer = (WearableNavigationDrawerView) findViewById(R.id.navigation_drawer);
        mActionDrawer = (WearableActionDrawerView) findViewById(R.id.action_drawer);

        if (mCloseFirstDrawerOpened) {
            mDrawerLayout.setDrawerStateCallback(mCloseFirstDrawerOpenedCallback);
        }

        if (mNavigationDrawer != null) {
            mNavigationDrawer.setAdapter(mDrawerAdapter);
            if (mOpenTopDrawerInOnCreate) {
                mDrawerLayout.openDrawer(Gravity.TOP);
            } else {
                mDrawerLayout.peekDrawer(Gravity.TOP);
            }
        }

        if (mActionDrawer != null) {
            if (mOpenBottomDrawerInOnCreate) {
                mDrawerLayout.openDrawer(Gravity.BOTTOM);
            } else {
                mDrawerLayout.peekDrawer(Gravity.BOTTOM);
            }
        }
    }

    private void parseIntent(Intent intent) {
        //noinspection WrongConstant - Linter doesn't know intent contains a NavigationStyle
        mNavigationStyle = intent.getIntExtra(STYLE_EXTRA, DrawerStyle.BOTH_DRAWER_NAV_SINGLE_PAGE);
        mOpenTopDrawerInOnCreate = intent.getBooleanExtra(OPEN_TOP_IN_ONCREATE_EXTRA, false);
        mOpenBottomDrawerInOnCreate = intent.getBooleanExtra(OPEN_BOTTOM_IN_ONCREATE_EXTRA, false);
        mCloseFirstDrawerOpened = intent.getBooleanExtra(CLOSE_FIRST_DRAWER_OPENED, false);
    }

    /**
     * Which configuration of drawers should be used.
     */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef({
            DrawerStyle.BOTH_DRAWER_NAV_SINGLE_PAGE,
            DrawerStyle.BOTH_DRAWER_NAV_MULTI_PAGE,
            DrawerStyle.ONLY_ACTION_DRAWER_WITH_TITLE
    })
    public @interface DrawerStyle {
        int BOTH_DRAWER_NAV_SINGLE_PAGE = 0;
        int BOTH_DRAWER_NAV_MULTI_PAGE = 1;
        int ONLY_ACTION_DRAWER_WITH_TITLE = 2;
    }

    /**
     * Builds an {@link Intent} to start this {@link Activity} with the appropriate extras.
     */
    public static class Builder {

        @DrawerStyle private int mStyle = DrawerStyle.BOTH_DRAWER_NAV_SINGLE_PAGE;
        private boolean mOpenTopDrawerInOnCreate = false;
        private boolean mOpenBottomDrawerInOnCreate = false;
        private boolean mCloseFirstDrawerOpened = false;

        public Builder setStyle(@DrawerStyle int style) {
            mStyle = style;
            return this;
        }

        public Builder openTopDrawerInOnCreate() {
            mOpenTopDrawerInOnCreate = true;
            return this;
        }

        public Builder openBottomDrawerInOnCreate() {
            mOpenBottomDrawerInOnCreate = true;
            return this;
        }

        public Builder closeFirstDrawerOpened() {
            mCloseFirstDrawerOpened = true;
            return this;
        }

        public Intent build() {
            return new Intent()
                    .putExtra(STYLE_EXTRA, mStyle)
                    .putExtra(OPEN_TOP_IN_ONCREATE_EXTRA, mOpenTopDrawerInOnCreate)
                    .putExtra(OPEN_BOTTOM_IN_ONCREATE_EXTRA, mOpenBottomDrawerInOnCreate)
                    .putExtra(CLOSE_FIRST_DRAWER_OPENED, mCloseFirstDrawerOpened);
        }
    }
}
