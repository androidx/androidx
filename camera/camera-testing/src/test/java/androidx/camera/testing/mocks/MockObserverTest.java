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

package androidx.camera.testing.mocks;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import android.os.Build;

import androidx.camera.testing.impl.mocks.MockObserver;
import androidx.camera.testing.impl.mocks.helpers.CallTimes;

import junit.framework.AssertionFailedError;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.internal.DoNotInstrument;

@RunWith(RobolectricTestRunner.class)
@DoNotInstrument
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
public class MockObserverTest {
    private static final Object DUMMY_NEW_DATA = new Object();
    private static final Object DUMMY_NEW_DATA_2 = new Object();

    private MockObserver<Object> mMockObserver;

    @Before
    public void setUp() {
        mMockObserver = new MockObserver<>();
    }

    @Test
    public void callTimesMatches_verifyOnNewDataCallPasses() {
        mMockObserver.onNewData(DUMMY_NEW_DATA);
        mMockObserver.onNewData(DUMMY_NEW_DATA);
        mMockObserver.onNewData(DUMMY_NEW_DATA_2);

        mMockObserver.verifyOnNewDataCall(DUMMY_NEW_DATA, 100, new CallTimes(2));
    }

    @Test
    public void verifiedWithinTimeout_verifyAcceptCallPasses() {
        new Thread(() -> {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }

            mMockObserver.onNewData(DUMMY_NEW_DATA);
        }, "test thread").start();

        mMockObserver.verifyOnNewDataCall(DUMMY_NEW_DATA, 500);
    }


    @Test
    public void notVerifiedWithinTimeout_verifyOnNewDataCallFailsWithProperMessage() {
        mMockObserver.onNewData(DUMMY_NEW_DATA);

        AssertionFailedError error = assertThrows(AssertionFailedError.class,
                () -> mMockObserver.verifyOnNewDataCall(DUMMY_NEW_DATA, 100, new CallTimes(2)));
        assertEquals("Test failed for a timeout of 100 ms",
                error.getMessage());
    }
}
