// CHECKSTYLE:OFF Generated code
/* This file is auto-generated from VerticalGridSupportFragmentTest.java.  DO NOT MODIFY. */

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

package androidx.leanback.app;

import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

import androidx.leanback.test.R;
import androidx.leanback.testutils.LeakDetector;
import androidx.leanback.testutils.PollingCheck;
import androidx.leanback.widget.ArrayObjectAdapter;
import androidx.leanback.widget.VerticalGridPresenter;
import androidx.leanback.widget.VerticalGridView;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;
import org.junit.runner.RunWith;

@LargeTest
@RunWith(AndroidJUnit4.class)
public class VerticalGridFragmentTest extends SingleFragmentTestBase {

    static void loadData(ArrayObjectAdapter adapter, int items) {
        int index = 0;
        for (int j = 0; j < items; ++j) {
            adapter.add("Hello world-" + (index++));
            adapter.add("This is a test-" + (index++));
            adapter.add("Android TV-" + (index++));
            adapter.add("Leanback-" + (index++));
            adapter.add("Hello world-" + (index++));
            adapter.add("Android TV-" + (index++));
            adapter.add("Leanback-" + (index++));
        }
    }

    public static class GridFragment extends VerticalGridFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            if (savedInstanceState == null) {
                prepareEntranceTransition();
            }
            VerticalGridPresenter gridPresenter = new VerticalGridPresenter();
            gridPresenter.setNumberOfColumns(3);
            setGridPresenter(gridPresenter);
            ArrayObjectAdapter adapter = new ArrayObjectAdapter(new StringPresenter());
            setAdapter(adapter);
            loadData(adapter, 10);
        }
    }

    @Test
    public void immediateRemoveFragment() throws Throwable {
        final SingleFragmentTestActivity activity = launchAndWaitActivity(GridFragment.class, 500);

        InstrumentationRegistry.getInstrumentation().runOnMainSync(new Runnable() {
            @Override
            public void run() {
                GridFragment f = new GridFragment();
                activity.getFragmentManager().beginTransaction()
                        .replace(android.R.id.content, f, null).commit();
                f.startEntranceTransition();
                activity.getFragmentManager().beginTransaction()
                        .replace(android.R.id.content, new Fragment(), null).commit();
            }
        });

        Thread.sleep(1000);
        activity.finish();
    }

    public static final class EmptyFragment extends Fragment {
        EditText mEditText;

        @Override
        public View onCreateView(
                LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            return mEditText = new EditText(container.getContext());
        }

        @Override
        public void onStart() {
            super.onStart();
            // focus IME on the new fragment because there is a memory leak that IME remembers
            // last editable view, which will cause a false reporting of leaking View.
            InputMethodManager imm =
                    (InputMethodManager) getActivity()
                            .getSystemService(Context.INPUT_METHOD_SERVICE);
            mEditText.requestFocus();
            imm.showSoftInput(mEditText, 0);
        }

        @Override
        public void onDestroyView() {
            mEditText = null;
            super.onDestroyView();
        }
    }

    @Test
    public void viewLeakTest() throws Throwable {
        SingleFragmentTestActivity activity = launchAndWaitActivity(
                GridFragment.class,
                1000);

        VerticalGridView gridView = ((GridFragment) activity.getTestFragment())
                .mGridViewHolder.getGridView();
        LeakDetector leakDetector = new LeakDetector();
        leakDetector.observeObject(gridView);
        leakDetector.observeObject(gridView.getRecycledViewPool());
        for (int i = 0; i < gridView.getChildCount(); i++) {
            leakDetector.observeObject(gridView.getChildAt(i));
        }
        gridView = null;
        InstrumentationRegistry.getInstrumentation().runOnMainSync(
                new Runnable() {
                    @Override
                    public void run() {
                        activity.getFragmentManager().beginTransaction()
                               .replace(R.id.main_frame, new EmptyFragment())
                               .addToBackStack("BK")
                               .commit();
                    }
                }
        );

        PollingCheck.waitFor(1000, new PollingCheck.PollingCheckCondition() {
            @Override
            public boolean canProceed() {
                return activity.getFragmentManager()
                        .findFragmentById(R.id.main_frame).isResumed();
            }
        });
        leakDetector.assertNoLeak();
    }
}
