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

import static androidx.browser.auth.AuthTabIntent.EXTRA_HTTPS_REDIRECT_HOST;
import static androidx.browser.auth.AuthTabIntent.EXTRA_HTTPS_REDIRECT_PATH;
import static androidx.browser.auth.AuthTabIntent.EXTRA_LAUNCH_AUTH_TAB;
import static androidx.browser.auth.AuthTabIntent.EXTRA_REDIRECT_SCHEME;
import static androidx.browser.auth.AuthTabIntent.RESULT_CANCELED;
import static androidx.browser.auth.AuthTabIntent.RESULT_OK;
import static androidx.browser.auth.AuthTabIntent.RESULT_UNKNOWN_CODE;
import static androidx.browser.auth.AuthTabIntent.RESULT_VERIFICATION_FAILED;
import static androidx.browser.auth.AuthTabIntent.RESULT_VERIFICATION_TIMED_OUT;
import static androidx.browser.customtabs.CustomTabsIntent.EXTRA_SESSION;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
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
    public void testIntentHasNecessaryData_customScheme() {
        AuthTabIntent intent = new AuthTabIntent.Builder().build();
        intent.launch(mLauncher, URI, "myscheme");

        verify(mLauncher).launch(mIntentCaptor.capture());
        Intent launchIntent = mIntentCaptor.getValue();

        assertTrue(launchIntent.getBooleanExtra(EXTRA_LAUNCH_AUTH_TAB, false));
        assertTrue(launchIntent.hasExtra(EXTRA_SESSION));
        assertEquals(URI.toString(), launchIntent.getDataString());
        assertEquals("myscheme", launchIntent.getStringExtra(EXTRA_REDIRECT_SCHEME));
    }

    @Test
    public void testIntentHasNecessaryData_https() {
        AuthTabIntent intent = new AuthTabIntent.Builder().build();
        intent.launch(mLauncher, URI, "example.com", "/auth/code");

        verify(mLauncher).launch(mIntentCaptor.capture());
        Intent launchIntent = mIntentCaptor.getValue();

        assertTrue(launchIntent.getBooleanExtra(EXTRA_LAUNCH_AUTH_TAB, false));
        assertTrue(launchIntent.hasExtra(EXTRA_SESSION));
        assertEquals(URI.toString(), launchIntent.getDataString());
        assertEquals("example.com", launchIntent.getStringExtra(EXTRA_HTTPS_REDIRECT_HOST));
        assertEquals("/auth/code", launchIntent.getStringExtra(EXTRA_HTTPS_REDIRECT_PATH));
    }

    @Test
    public void testParseResult_ok() {
        AuthTabIntent.AuthenticateUserResultContract contract =
                new AuthTabIntent.AuthenticateUserResultContract();
        Uri uri = Uri.parse("myscheme://auth?token=Qw3rty");
        Intent intent = new Intent();
        intent.setData(uri);

        AuthTabIntent.AuthResult result = contract.parseResult(RESULT_OK, intent);
        assertEquals(RESULT_OK, result.resultCode);
        assertEquals(uri, result.resultUri);
    }

    @Test
    public void testParseResult_canceled() {
        AuthTabIntent.AuthenticateUserResultContract contract =
                new AuthTabIntent.AuthenticateUserResultContract();
        Uri uri = Uri.EMPTY;
        Intent intent = new Intent();
        intent.setData(uri);

        AuthTabIntent.AuthResult result = contract.parseResult(RESULT_CANCELED, intent);
        assertEquals(RESULT_CANCELED, result.resultCode);
        assertNull(result.resultUri);
    }

    @Test
    public void testParseResult_verificationFailed() {
        AuthTabIntent.AuthenticateUserResultContract contract =
                new AuthTabIntent.AuthenticateUserResultContract();
        Uri uri = Uri.EMPTY;
        Intent intent = new Intent();
        intent.setData(uri);

        AuthTabIntent.AuthResult result = contract.parseResult(RESULT_VERIFICATION_FAILED, intent);
        assertEquals(RESULT_VERIFICATION_FAILED, result.resultCode);
        assertNull(result.resultUri);
    }

    @Test
    public void testParseResult_verificationTimedOut() {
        AuthTabIntent.AuthenticateUserResultContract contract =
                new AuthTabIntent.AuthenticateUserResultContract();
        Uri uri = Uri.EMPTY;
        Intent intent = new Intent();
        intent.setData(uri);

        AuthTabIntent.AuthResult result = contract.parseResult(RESULT_VERIFICATION_TIMED_OUT,
                intent);
        assertEquals(RESULT_VERIFICATION_TIMED_OUT, result.resultCode);
        assertNull(result.resultUri);
    }

    @Test
    public void testParseResult_other() {
        AuthTabIntent.AuthenticateUserResultContract contract =
                new AuthTabIntent.AuthenticateUserResultContract();
        Uri uri = Uri.EMPTY;
        Intent intent = new Intent();
        intent.setData(uri);

        AuthTabIntent.AuthResult result = contract.parseResult(100, intent);
        assertEquals(RESULT_UNKNOWN_CODE, result.resultCode);
        assertNull(result.resultUri);
    }
}
