/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.recyclerview.selection;

import androidx.recyclerview.selection.testing.TestData;
import androidx.recyclerview.selection.testing.TestEvents;
import androidx.recyclerview.selection.testing.TestRunnable;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;

/**
 * MouseInputDelegate / SelectHelper integration test covering the shared
 * responsibility of range selection.
 */
@RunWith(AndroidJUnit4.class)
@SmallTest
public final class ResetMangerTest {

    private static final List<String> ITEMS = TestData.createStringData(100);

    private ResetManager<String> mManager;
    private TestRunnable mListern1;
    private TestRunnable mListern2;

    @Before
    public void setUp() {
        mManager = new ResetManager();
        mListern1 = new TestRunnable();
        mListern2 = new TestRunnable();
        mManager.addResetListener(mListern1);
        mManager.addResetListener(mListern2);
    }

    @Test
    public void notifiesListenersOnCancelEvent() {
        mManager.getInputListener().onInterceptTouchEvent(null, TestEvents.Unknown.CANCEL);
        mListern1.assertRun();
        mListern2.assertRun();
    }

    @Test
    public void ignoresNonCancelEvents() {
        mManager.getInputListener().onInterceptTouchEvent(null, TestEvents.Mouse.CLICK);
        mManager.getInputListener().onInterceptTouchEvent(null, TestEvents.Touch.TAP);
        mListern1.assertNotRun();
        mListern2.assertNotRun();
    }

    @Test
    public void notifiesListenersOnSelectionCleared() {
        mManager.getSelectionObserver().onSelectionCleared();
        mListern1.assertRun();
        mListern2.assertRun();
    }

    @Test
    public void notifiesListenersOnSelectionRefreshed() {
        mManager.getSelectionObserver().onSelectionRefresh();
        mListern1.assertRun();
        mListern2.assertRun();
    }

    @Test
    public void notifiesListenersOnSelectionRestored() {
        mManager.getSelectionObserver().onSelectionRestored();
        mListern1.assertRun();
        mListern2.assertRun();
    }
}
