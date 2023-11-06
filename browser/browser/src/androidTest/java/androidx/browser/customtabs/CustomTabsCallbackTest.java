/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.browser.customtabs;

import static androidx.browser.customtabs.CustomTabsCallback.ACTIVITY_LAYOUT_STATE_BOTTOM_SHEET;

import static org.junit.Assert.assertTrue;

import android.os.Bundle;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests for {@link CustomTabsCallback} with no {@link CustomTabsService} component.
 * Check the flow from {@link CustomTabsSession} to {@link CustomTabsCallback}.
 */
@RunWith(AndroidJUnit4.class)
@SmallTest
public class CustomTabsCallbackTest {
    private TestCustomTabsCallback mCallback;
    private CustomTabsSessionToken mToken;

    @Before
    public void setup() {
        mCallback = new TestCustomTabsCallback();
        mToken = new CustomTabsSessionToken(mCallback.getStub(), null);
    }

    @Test
    public void testOnActivityResized() throws Throwable {
        mToken.getCallback().onActivityResized(75239, 1200, new Bundle());
        assertTrue(mCallback.hasActivityBeenResized());
    }

    @Test
    public void testOnWarmupCompleted() throws Throwable {
        mToken.getCallback().onWarmupCompleted(null);
        assertTrue(mCallback.wasWarmupCompleted());
    }

    @Test
    public void testOnActivityLayout() throws Throwable {
        mToken.getCallback().onActivityLayout(0, 100, 1200, 1200,
                ACTIVITY_LAYOUT_STATE_BOTTOM_SHEET, Bundle.EMPTY);
        assertTrue(mCallback.hasActivityBeenLaidOut());
    }

    @Test
    public void testOnMinimized() throws Throwable {
        mToken.getCallback().onMinimized(Bundle.EMPTY);
        assertTrue(mCallback.wasMinimized());
    }

    @Test
    public void testOnUnminimized() throws Throwable {
        mToken.getCallback().onUnminimized(Bundle.EMPTY);
        assertTrue(mCallback.wasUnminimized());
    }
}
