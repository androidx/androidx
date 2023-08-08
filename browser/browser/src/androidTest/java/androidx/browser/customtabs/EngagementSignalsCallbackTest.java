/*
 * Copyright 2023 The Android Open Source Project
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import android.os.RemoteException;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests for {@link EngagementSignalsCallback} with no {@link CustomTabsService}.
 */
@RunWith(AndroidJUnit4.class)
@SmallTest
public class EngagementSignalsCallbackTest {
    TestEngagementSignalsCallback mCallback;

    @Before
    public void setup() {
        mCallback = new TestEngagementSignalsCallback();
    }

    @Test
    public void testOnVerticalScrollEvent() throws RemoteException {
        mCallback.getStub().onVerticalScrollEvent(true, null);
        verify(mCallback.getMock()).onVerticalScrollEvent(eq(true), any());
    }

    @Test
    public void testOnGreatestScrollPercentageIncreased() throws RemoteException {
        mCallback.getStub().onGreatestScrollPercentageIncreased(65, null);
        verify(mCallback.getMock()).onGreatestScrollPercentageIncreased(eq(65), any());
    }

    @Test
    public void testOnSessionEnded() throws RemoteException {
        mCallback.getStub().onSessionEnded(true, null);
        verify(mCallback.getMock()).onSessionEnded(eq(true), any());
    }
}
