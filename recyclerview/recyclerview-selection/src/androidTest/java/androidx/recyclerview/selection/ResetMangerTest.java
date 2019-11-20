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
import androidx.recyclerview.selection.testing.TestResettable;
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
    private TestResettable mResettable1;
    private TestResettable mResettable2;

    @Before
    public void setUp() {
        mManager = new ResetManager();
        mResettable1 = new TestResettable(true);
        mResettable2 = new TestResettable(true);
        mManager.addResetHandler(mResettable1);
        mManager.addResetHandler(mResettable2);
    }

    @Test
    public void notifiesListenersOnCancelEvent() {
        mManager.getInputListener().onInterceptTouchEvent(null, TestEvents.Unknown.CANCEL);
        mResettable1.assertReset();
        mResettable2.assertReset();
    }

    @Test
    public void notifiesListenersOnSelectionCleared() {
        mManager.getSelectionObserver().onSelectionCleared();
        mResettable1.assertReset();
        mResettable2.assertReset();
    }

    @Test
    public void doesNotNotifyListenersOnSelectionRefreshed() {
        mManager.getSelectionObserver().onSelectionRefresh();
        mResettable1.assertNotReset();
        mResettable2.assertNotReset();
    }

    @Test
    public void doesNotNotifyListenersOnSelectionRestored() {
        mManager.getSelectionObserver().onSelectionRestored();
        mResettable1.assertNotReset();
        mResettable2.assertNotReset();
    }

    @Test
    public void ignoresNonCancelEvents() {
        mManager.getInputListener().onInterceptTouchEvent(null, TestEvents.Mouse.CLICK);
        mManager.getInputListener().onInterceptTouchEvent(null, TestEvents.Touch.TAP);
        mResettable1.assertNotReset();
        mResettable2.assertNotReset();
    }

    @Test
    public void ignoresWhenResetNotRequired() {
        mResettable1.setResetRequired(false);
        mResettable2.setResetRequired(false);

        mManager.getInputListener().onInterceptTouchEvent(null, TestEvents.Unknown.CANCEL);
        mResettable1.assertNotReset();
        mResettable2.assertNotReset();

        mManager.getSelectionObserver().onSelectionCleared();
        mResettable1.assertNotReset();
        mResettable2.assertNotReset();

        mManager.getSelectionObserver().onSelectionRefresh();
        mResettable1.assertNotReset();
        mResettable2.assertNotReset();

        mManager.getSelectionObserver().onSelectionRestored();
        mResettable1.assertNotReset();
        mResettable2.assertNotReset();
    }
}
