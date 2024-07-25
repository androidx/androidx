/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.browser.auth;

import static androidx.browser.auth.AuthTabIntent.EXTRA_LAUNCH_AUTH_TAB;
import static androidx.browser.auth.AuthTabIntent.EXTRA_REDIRECT_SCHEME;
import static androidx.browser.customtabs.CustomTabsIntent.EXTRA_SESSION;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.verify;

import android.content.Intent;
import android.net.Uri;

import androidx.activity.result.ActivityResultLauncher;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.internal.DoNotInstrument;

/** Tests for {@link AuthTabIntent}. */
@RunWith(RobolectricTestRunner.class)
@DoNotInstrument
public class AuthTabIntentTest {
    @Rule
    public final MockitoRule mMockitoRule = MockitoJUnit.rule();

    private static final Uri URI = Uri.parse("https://www.google.com");

    @Captor
    private ArgumentCaptor<Intent> mIntentCaptor;
    @Mock
    private ActivityResultLauncher<Intent> mLauncher;

    @Test
    public void testIntentHasNecessaryData() {
        AuthTabIntent intent = new AuthTabIntent.Builder().build();
        intent.launch(mLauncher, URI, "myscheme");

        verify(mLauncher).launch(mIntentCaptor.capture());
        Intent launchIntent = mIntentCaptor.getValue();

        assertTrue(launchIntent.getBooleanExtra(EXTRA_LAUNCH_AUTH_TAB, false));
        assertTrue(launchIntent.hasExtra(EXTRA_SESSION));
        assertEquals(URI.toString(), launchIntent.getDataString());
        assertEquals("myscheme", launchIntent.getStringExtra(EXTRA_REDIRECT_SCHEME));
    }
}
