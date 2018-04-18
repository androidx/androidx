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
package androidx.wear.ambient;

import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;

import androidx.fragment.app.FragmentActivity;

import com.google.android.wearable.compat.WearableActivityController;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

/**
 * Tests for {@link AmbientDelegate}.
 */
@SmallTest
@RunWith(AndroidJUnit4.class)
public class AmbientDelegateTest {

    @Mock
    AmbientDelegate.AmbientCallback mMockAmbientCallback;
    @Mock
    WearableControllerProvider mMockWearableControllerProvider;
    @Mock
    WearableActivityController mMockWearableController;
    @Mock
    FragmentActivity mMockActivity;

    private AmbientDelegate mAmbientDelegateUnderTest;

    @Before
    public void setUp() {
        mMockAmbientCallback = mock(AmbientDelegate.AmbientCallback.class);
        mMockWearableControllerProvider = mock(WearableControllerProvider.class);
        mMockWearableController = mock(WearableActivityController.class);
        mMockActivity = mock(FragmentActivity.class);
        when(mMockWearableControllerProvider
                .getWearableController(mMockActivity, mMockAmbientCallback))
                .thenReturn(mMockWearableController);
    }

    @Test
    public void testNullActivity() {
        mAmbientDelegateUnderTest = new AmbientDelegate(null,
                mMockWearableControllerProvider, mMockAmbientCallback);
        verifyZeroInteractions(mMockWearableControllerProvider);

        assertFalse(mAmbientDelegateUnderTest.isAmbient());

    }

    @Test
    public void testActivityPresent() {
        mAmbientDelegateUnderTest = new AmbientDelegate(mMockActivity,
                mMockWearableControllerProvider, mMockAmbientCallback);

        mAmbientDelegateUnderTest.onCreate();
        verify(mMockWearableController).onCreate();

        mAmbientDelegateUnderTest.onResume();
        verify(mMockWearableController).onResume();

        mAmbientDelegateUnderTest.onPause();
        verify(mMockWearableController).onPause();

        mAmbientDelegateUnderTest.onStop();
        verify(mMockWearableController).onStop();

        mAmbientDelegateUnderTest.onDestroy();
        verify(mMockWearableController).onDestroy();
    }
}
