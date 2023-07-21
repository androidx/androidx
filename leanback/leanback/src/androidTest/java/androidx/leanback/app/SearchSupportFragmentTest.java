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

import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.speech.SpeechRecognizer;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import org.junit.Ignore;

import androidx.fragment.app.Fragment;
import androidx.leanback.test.R;
import androidx.leanback.testutils.LeakDetector;
import androidx.leanback.testutils.PollingCheck;
import androidx.leanback.widget.ArrayObjectAdapter;
import androidx.leanback.widget.HeaderItem;
import androidx.leanback.widget.ListRow;
import androidx.leanback.widget.ListRowPresenter;
import androidx.leanback.widget.ObjectAdapter;
import androidx.leanback.widget.VerticalGridView;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.filters.SdkSuppress;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.testutils.AnimationTest;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Objects;

@LargeTest
@AnimationTest
@RunWith(AndroidJUnit4.class)
public class SearchSupportFragmentTest extends SingleSupportFragmentTestBase {

    static final StringPresenter CARD_PRESENTER = new StringPresenter();

    static void loadData(ArrayObjectAdapter adapter, int numRows, int repeatPerRow) {
        for (int i = 0; i < numRows; ++i) {
            ArrayObjectAdapter listRowAdapter = new ArrayObjectAdapter(CARD_PRESENTER);
            int index = 0;
            for (int j = 0; j < repeatPerRow; ++j) {
                listRowAdapter.add("Hello world-" + (index++));
                listRowAdapter.add("This is a test-" + (index++));
                listRowAdapter.add("Android TV-" + (index++));
                listRowAdapter.add("Leanback-" + (index++));
                listRowAdapter.add("Hello world-" + (index++));
                listRowAdapter.add("Android TV-" + (index++));
                listRowAdapter.add("Leanback-" + (index++));
                listRowAdapter.add("GuidedStepSupportFragment-" + (index++));
            }
            HeaderItem header = new HeaderItem(i, "Row " + i);
            adapter.add(new ListRow(header, listRowAdapter));
        }
    }

    public static final class F_LeakFragment extends SearchSupportFragment
            implements SearchSupportFragment.SearchResultProvider {
        ArrayObjectAdapter mRowsAdapter = new ArrayObjectAdapter(new ListRowPresenter());

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            loadData(mRowsAdapter, 10, 1);
        }

        @Override
        public ObjectAdapter getResultsAdapter() {
            return mRowsAdapter;
        }

        @Override
        public boolean onQueryTextChange(String newQuery) {
            return true;
        }

        @Override
        public boolean onQueryTextSubmit(String query) {
            return true;
        }
    }

    public static final class EmptyFragment extends Fragment {
        EditText mEditText;

        @Override
        public View onCreateView(
                final LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
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

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.LOLLIPOP) // API 17 retains local Variable
    @Test
    public void viewLeakTest() throws Throwable {
        SingleSupportFragmentTestActivity activity = launchAndWaitActivity(F_LeakFragment.class,
                1000);

        VerticalGridView gridView = ((SearchSupportFragment) activity.getTestFragment())
                .getRowsSupportFragment().getVerticalGridView();
        LeakDetector leakDetector = new LeakDetector();
        leakDetector.observeObject(gridView);
        leakDetector.observeObject(gridView.getRecycledViewPool());
        for (int i = 0; i < gridView.getChildCount(); i++) {
            leakDetector.observeObject(gridView.getChildAt(i));
        }
        gridView = null;
        EmptyFragment emptyFragment = new EmptyFragment();
        activity.getSupportFragmentManager().beginTransaction()
                .replace(R.id.main_frame, emptyFragment)
                .addToBackStack("BK")
                .commit();

        PollingCheck.waitFor(1000, new PollingCheck.PollingCheckCondition() {
            @Override
            public boolean canProceed() {
                return emptyFragment.isResumed();
            }
        });
        leakDetector.assertNoLeak();
    }

    @Test
    public void testFocusWithSpeechRecognizerDisabled() {
        SingleSupportFragmentTestActivity activity = launchAndWaitActivity(
                SpeechRecognizerDisabledFragment.class, 1000);

        assertTrue(activity.findViewById(R.id.lb_search_text_editor).hasFocus());

        sendKeys(KeyEvent.KEYCODE_A);
        sendKeys(KeyEvent.KEYCODE_ENTER);

        PollingCheck.waitFor(new PollingCheck.PollingCheckCondition() {
            @Override
            public boolean canProceed() {
                return ((SearchSupportFragment) activity.getTestFragment())
                        .getRowsSupportFragment().getVerticalGridView().hasFocus();
            }
        });

        sendKeys(KeyEvent.KEYCODE_DPAD_UP);
        PollingCheck.waitFor(new PollingCheck.PollingCheckCondition() {
            @Override
            public boolean canProceed() {
                return activity.findViewById(R.id.lb_search_text_editor).hasFocus();
            }
        });
    }

    @Test
    @Ignore("b/281082608")
    public void testFocusWithSpeechRecognizerEnabled() throws Exception {

        // Skip the test for devices which do not have SpeechRecognizer
        if (!SpeechRecognizer.isRecognitionAvailable(
                InstrumentationRegistry.getInstrumentation().getContext())) {
            return;
        }

        SingleSupportFragmentTestActivity activity = launchAndWaitActivity(
                SpeechRecognizerEnabledFragment.class, 1000);

        assertTrue(activity.findViewById(R.id.lb_search_bar_speech_orb).hasFocus());

        sendKeys(KeyEvent.KEYCODE_DPAD_RIGHT);

        assertTrue(activity.findViewById(R.id.lb_search_text_editor).hasFocus());

        sendKeys(KeyEvent.KEYCODE_A);
        sendKeys(KeyEvent.KEYCODE_ENTER);

        PollingCheck.waitFor(new PollingCheck.PollingCheckCondition() {
            @Override
            public boolean canProceed() {
                return ((SearchSupportFragment) activity.getTestFragment())
                        .getRowsSupportFragment().getVerticalGridView().hasFocus();
            }
        });

        Thread.sleep(1000);

        sendKeys(KeyEvent.KEYCODE_DPAD_UP);
        assertTrue(activity.findViewById(R.id.lb_search_bar_speech_orb).hasFocus());
    }

    static class SearchSupportTestFragment extends SearchSupportFragment
            implements SearchSupportFragment.SearchResultProvider {
        ArrayObjectAdapter mRowsAdapter = new ArrayObjectAdapter(new ListRowPresenter());
        String mPreviousQuery;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setSearchResultProvider(this);
        }

        @Override
        public ObjectAdapter getResultsAdapter() {
            return mRowsAdapter;
        }

        @Override
        public boolean onQueryTextChange(String newQuery) {
            if (!Objects.equals(mPreviousQuery, newQuery)) {
                mRowsAdapter.clear();
                loadData(mRowsAdapter, 10, 1);
                mPreviousQuery = newQuery;
                return true;
            }
            return false;
        }

        @Override
        public boolean onQueryTextSubmit(String query) {
            if (!Objects.equals(mPreviousQuery, query)) {
                mRowsAdapter.clear();
                loadData(mRowsAdapter, 10, 1);
                mPreviousQuery = query;
                return true;
            }
            return false;
        }
    }

    public static final class SpeechRecognizerDisabledFragment extends SearchSupportTestFragment {
        @Override
        boolean isSpeechRecognizerAvailable() {
            return false;
        }
    }

    public static final class SpeechRecognizerEnabledFragment extends SearchSupportTestFragment {
        @Override
        boolean isSpeechRecognizerAvailable() {
            return true;
        }
    }
}
