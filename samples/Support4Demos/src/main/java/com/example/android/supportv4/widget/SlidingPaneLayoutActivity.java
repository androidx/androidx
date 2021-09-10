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

import android.app.ActionBar;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import androidx.activity.ComponentActivity;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.slidingpanelayout.widget.SlidingPaneLayout;

import com.example.android.supportv4.LoremIpsum;
import com.example.android.supportv4.R;

/**
 * This example illustrates a common usage of SlidingPaneLayout in the Android support library.
 *
 * <p>A SlidingPaneLayout should be positioned at the top of your view hierarchy, placing it
 * below the action bar but above your content views. It is ideal as a two-pane layout
 * for larger screens, used in place of a horizontal LinearLayout.</p>
 *
 * <p>What separates SlidingPaneLayout from LinearLayout in this usage is that SlidingPaneLayout
 * allows these wide, two-pane layouts to overlap when horizontal space is at a premium. The user
 * can then access both panes by physically sliding the content pane into view or out of the way
 * or implicitly by moving focus between the two panes. This can greatly simplify development
 * of Android apps that support multiple form factors and screen sizes.</p>
 *
 * <p>When it comes to your navigation hierarchy, the left pane of a SlidingPaneLayout is always
 * considered to be one level up from the right content pane. As such, your Action Bar's
 * Up navigation should be enabled if the right pane is obscuring the left pane, and invoking it
 * should open the panes, revealing the left pane for normal interaction. From this open state
 * where the left pane is in primary focus, the Action Bar's Up affordance should act as if
 * both panes were fully visible in the activity window and navigate to the activity one level up
 * in the app's logical hierarchy. If the activity is the root of the application's task, the up
 * affordance should be disabled when the sliding pane is open and showing the left pane.
 * This code example illustrates this root activity case.</p>
 *
 * <p>Note that SlidingPaneLayout differs in usage from DrawerLayout. While DrawerLayout offers
 * sliding utility drawers for extended navigation options and actions, the panes of a
 * SlidingPaneLayout are firmly part of the content itself. If it would not make sense for
 * both panes to be visible all the time on a sufficiently wide screen, DrawerLayout and its
 * associated patterns are likely to be a better choice for your usage.</p>
 */
public class SlidingPaneLayoutActivity extends ComponentActivity {
    SlidingPaneLayout mSlidingLayout;
    TextView mContent;

    ActionBarHelper mActionBar;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.sliding_pane_layout);

        mSlidingLayout = findViewById(R.id.sliding_pane_layout);
        ListView list = findViewById(R.id.left_pane);
        mContent = findViewById(R.id.content_text);

        getOnBackPressedDispatcher().addCallback(this,
                new SliderOnBackPressedCallback(mSlidingLayout));

        mSlidingLayout.addPanelSlideListener(new SliderListener());

        list.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1,
                LoremIpsum.TITLES));
        list.setOnItemClickListener(new ListItemClickListener());

        mActionBar = createActionBarHelper();
        mActionBar.init();

        mSlidingLayout.getViewTreeObserver().addOnGlobalLayoutListener(new FirstLayoutListener());
    }

    @Override
    public boolean onCreateOptionsMenu(@NonNull Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.sliding_pane_layout_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.lock_mode_unlocked:
                mSlidingLayout.setLockMode(SlidingPaneLayout.LOCK_MODE_UNLOCKED);
                return true;
            case R.id.lock_mode_locked_open:
                mSlidingLayout.setLockMode(SlidingPaneLayout.LOCK_MODE_LOCKED_OPEN);
                return true;
            case R.id.lock_mode_locked_closed:
                mSlidingLayout.setLockMode(SlidingPaneLayout.LOCK_MODE_LOCKED_CLOSED);
                return true;
            case R.id.lock_mode_locked:
                mSlidingLayout.setLockMode(SlidingPaneLayout.LOCK_MODE_LOCKED);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * This list item click listener implements very simple view switching by changing
     * the primary content text. The slider is closed when a selection is made to fully
     * reveal the content.
     */
    class ListItemClickListener implements ListView.OnItemClickListener {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            mContent.setText(LoremIpsum.DIALOGUE[position]);
            mActionBar.setTitle(LoremIpsum.TITLES[position]);
            mSlidingLayout.openPane();
        }
    }

    /**
     * A self contained {@link OnBackPressedCallback} that ensures that the system back
     * button will close the {@link SlidingPaneLayout} if it is slideable (i.e., the panes
     * are overlapping) and open (i.e., the detail pane is visible).
     */
    static class SliderOnBackPressedCallback extends OnBackPressedCallback
            implements SlidingPaneLayout.PanelSlideListener {

        private final SlidingPaneLayout mSlidingPaneLayout;

        SliderOnBackPressedCallback(@NonNull SlidingPaneLayout slidingPaneLayout) {
            super(slidingPaneLayout.isSlideable() && slidingPaneLayout.isOpen());
            mSlidingPaneLayout = slidingPaneLayout;
            slidingPaneLayout.addPanelSlideListener(this);
        }

        @Override
        public void handleOnBackPressed() {
            mSlidingPaneLayout.closePane();
        }

        @Override
        public void onPanelSlide(@NonNull View panel, float slideOffset) { }

        @Override
        public void onPanelOpened(@NonNull View panel) {
            setEnabled(true);
        }

        @Override
        public void onPanelClosed(@NonNull View panel) {
            setEnabled(false);
        }
    }

    /**
     * This panel slide listener updates the action bar accordingly for each panel state.
     */
    class SliderListener extends SlidingPaneLayout.SimplePanelSlideListener {
        @Override
        public void onPanelOpened(@NonNull View panel) {
            mActionBar.onPanelOpened();
        }

        @Override
        public void onPanelClosed(@NonNull View panel) {
            mActionBar.onPanelClosed();
        }
    }

    /**
     * This global layout listener is used to fire an event after first layout occurs
     * and then it is removed. This gives us a chance to configure parts of the UI
     * that adapt based on available space after they have had the opportunity to measure
     * and layout.
     */
    class FirstLayoutListener implements ViewTreeObserver.OnGlobalLayoutListener {
        @Override
        public void onGlobalLayout() {
            mActionBar.onFirstLayout();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                mSlidingLayout.getViewTreeObserver().removeOnGlobalLayoutListener(this);
            } else {
                mSlidingLayout.getViewTreeObserver().removeGlobalOnLayoutListener(this);
            }
        }
    }

    /**
     * Create a compatible helper that will manipulate the action bar if available.
     */
    private ActionBarHelper createActionBarHelper() {
        return new ActionBarHelper();
    }

    /**
     * Stub action bar helper; this does nothing.
     */
    private class ActionBarHelper {
        private final ActionBar mActionBar;
        private CharSequence mListTitle;
        private CharSequence mDetailTitle;

        ActionBarHelper() {
            mActionBar = getActionBar();
        }

        public void init() {
            mListTitle = mDetailTitle = getTitle();
        }

        public void onPanelClosed() {
            mActionBar.setTitle(mListTitle);
        }

        public void onPanelOpened() {
            mActionBar.setTitle(mDetailTitle);
        }

        public void onFirstLayout() {
            if (mSlidingLayout.isSlideable() && !mSlidingLayout.isOpen()) {
                onPanelClosed();
            } else {
                onPanelOpened();
            }
        }

        public void setTitle(CharSequence title) {
            mListTitle = title;
        }
    }



}
