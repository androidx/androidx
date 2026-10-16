/*
 * Copyright 2026 The Android Open Source Project
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.robolectric.Shadows.shadowOf;

import android.content.ComponentName;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.os.Looper;
import android.support.customtabs.ICustomTabsCallback;
import android.support.customtabs.ICustomTabsService;

import androidx.test.core.app.ApplicationProvider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.internal.DoNotInstrument;

/**
 * Unit tests for {@link CustomTabsClient}.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = {Config.TARGET_SDK})
@DoNotInstrument
public class CustomTabsClientUnitTest {
    private CustomTabsCallback mCallback;
    private ICustomTabsCallback mCallbackWrapper;

    @Before
    public void setup() throws Exception {
        Context context = ApplicationProvider.getApplicationContext();
        ICustomTabsService service = mock(ICustomTabsService.class);
        when(service.newSession(any())).thenReturn(true);

        CustomTabsClient client = new CustomTabsClient(
                service, new ComponentName("com.example", "Service"), context);
        mCallback = mock(CustomTabsCallback.class);
        CustomTabsSession session = client.newSession(mCallback);
        assertNotNull(session);

        ArgumentCaptor<ICustomTabsCallback> captor =
                ArgumentCaptor.forClass(ICustomTabsCallback.class);
        verify(service).newSession(captor.capture());
        mCallbackWrapper = captor.getValue();
    }

    @Test
    public void testExtraCallback_nullCallbackName_isIgnored() throws Exception {
        mCallbackWrapper.extraCallback(null, Bundle.EMPTY);
        shadowOf(Looper.getMainLooper()).idle();
        verify(mCallback, never()).extraCallback(any(), any());

        mCallbackWrapper.extraCallback("callback", Bundle.EMPTY);
        shadowOf(Looper.getMainLooper()).idle();
        verify(mCallback).extraCallback("callback", Bundle.EMPTY);
    }

    @Test
    public void testExtraCallbackWithResult_nullCallbackName_returnsNull() throws Exception {
        Bundle expected = new Bundle();
        when(mCallback.extraCallbackWithResult("callback", Bundle.EMPTY)).thenReturn(expected);

        assertNull(mCallbackWrapper.extraCallbackWithResult(null, Bundle.EMPTY));
        verify(mCallback, never()).extraCallbackWithResult(any(), any());

        assertEquals(expected, mCallbackWrapper.extraCallbackWithResult("callback", Bundle.EMPTY));
        verify(mCallback).extraCallbackWithResult("callback", Bundle.EMPTY);
    }

    @Test
    public void testOnPostMessage_nullMessage_isIgnored() throws Exception {
        mCallbackWrapper.onPostMessage(null, Bundle.EMPTY);
        shadowOf(Looper.getMainLooper()).idle();
        verify(mCallback, never()).onPostMessage(any(), any());

        mCallbackWrapper.onPostMessage("hello", Bundle.EMPTY);
        shadowOf(Looper.getMainLooper()).idle();
        verify(mCallback).onPostMessage("hello", Bundle.EMPTY);
    }

    @Test
    public void testOnRelationshipValidationResult_nullOrigin_isIgnored() throws Exception {
        mCallbackWrapper.onRelationshipValidationResult(
                CustomTabsService.RELATION_USE_AS_ORIGIN, null, true, Bundle.EMPTY);
        shadowOf(Looper.getMainLooper()).idle();
        verify(mCallback, never()).onRelationshipValidationResult(
                anyInt(), any(), anyBoolean(), any());

        Uri origin = Uri.parse("https://example.com");
        mCallbackWrapper.onRelationshipValidationResult(
                CustomTabsService.RELATION_USE_AS_ORIGIN, origin, true, Bundle.EMPTY);
        shadowOf(Looper.getMainLooper()).idle();
        verify(mCallback).onRelationshipValidationResult(
                CustomTabsService.RELATION_USE_AS_ORIGIN, origin, true, Bundle.EMPTY);
    }

    @Test
    public void testOnActivityResized_nullExtras_isIgnored() throws Exception {
        mCallbackWrapper.onActivityResized(100, 200, null);
        shadowOf(Looper.getMainLooper()).idle();
        verify(mCallback, never()).onActivityResized(anyInt(), anyInt(), any());

        mCallbackWrapper.onActivityResized(100, 200, Bundle.EMPTY);
        shadowOf(Looper.getMainLooper()).idle();
        verify(mCallback).onActivityResized(100, 200, Bundle.EMPTY);
    }

    @Test
    public void testOnWarmupCompleted_nullExtras_isIgnored() throws Exception {
        mCallbackWrapper.onWarmupCompleted(null);
        shadowOf(Looper.getMainLooper()).idle();
        verify(mCallback, never()).onWarmupCompleted(any());

        mCallbackWrapper.onWarmupCompleted(Bundle.EMPTY);
        shadowOf(Looper.getMainLooper()).idle();
        verify(mCallback).onWarmupCompleted(Bundle.EMPTY);
    }

    @Test
    public void testOnActivityLayout_nullExtras_isIgnored() throws Exception {
        mCallbackWrapper.onActivityLayout(0, 0, 100, 100,
                CustomTabsCallback.ACTIVITY_LAYOUT_STATE_BOTTOM_SHEET, null);
        shadowOf(Looper.getMainLooper()).idle();
        verify(mCallback, never()).onActivityLayout(
                anyInt(), anyInt(), anyInt(), anyInt(), anyInt(), any());

        mCallbackWrapper.onActivityLayout(0, 0, 100, 100,
                CustomTabsCallback.ACTIVITY_LAYOUT_STATE_BOTTOM_SHEET, Bundle.EMPTY);
        shadowOf(Looper.getMainLooper()).idle();
        verify(mCallback).onActivityLayout(0, 0, 100, 100,
                CustomTabsCallback.ACTIVITY_LAYOUT_STATE_BOTTOM_SHEET, Bundle.EMPTY);
    }

    @Test
    public void testOnMinimized_nullExtras_isIgnored() throws Exception {
        mCallbackWrapper.onMinimized(null);
        shadowOf(Looper.getMainLooper()).idle();
        verify(mCallback, never()).onMinimized(any());

        mCallbackWrapper.onMinimized(Bundle.EMPTY);
        shadowOf(Looper.getMainLooper()).idle();
        verify(mCallback).onMinimized(Bundle.EMPTY);
    }

    @Test
    public void testOnUnminimized_nullExtras_isIgnored() throws Exception {
        mCallbackWrapper.onUnminimized(null);
        shadowOf(Looper.getMainLooper()).idle();
        verify(mCallback, never()).onUnminimized(any());

        mCallbackWrapper.onUnminimized(Bundle.EMPTY);
        shadowOf(Looper.getMainLooper()).idle();
        verify(mCallback).onUnminimized(Bundle.EMPTY);
    }
}
