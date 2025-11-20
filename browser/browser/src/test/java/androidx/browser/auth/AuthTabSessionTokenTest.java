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

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.customtabs.IAuthTabCallback;

import androidx.test.core.app.ApplicationProvider;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.internal.DoNotInstrument;

/**
 * Tests for {@link AuthTabSessionToken}.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = {Config.TARGET_SDK})
@DoNotInstrument
public class AuthTabSessionTokenTest {
    private Context mContext;

    @Before
    public void setup() {
        mContext = ApplicationProvider.getApplicationContext();
    }

    @Test
    public void testEquality_withId() {
        AuthTabSessionToken token1 = new AuthTabSessionToken(new AuthTabSessionToken.MockCallback(),
                createSessionId(27));

        AuthTabSessionToken token2 = new AuthTabSessionToken(new AuthTabSessionToken.MockCallback(),
                createSessionId(27));

        assertEquals(token1, token2);
    }

    @Test
    public void testNonEquality_withId() {
        // Using the same binder to ensure only the id matters.
        IAuthTabCallback.Stub binder = new AuthTabSessionToken.MockCallback();

        AuthTabSessionToken token1 = new AuthTabSessionToken(binder, createSessionId(10));
        AuthTabSessionToken token2 = new AuthTabSessionToken(binder, createSessionId(20));

        assertNotEquals(token1, token2);
    }

    @Test
    public void testEquality_withBinder() {
        IAuthTabCallback.Stub binder = new AuthTabSessionToken.MockCallback();

        AuthTabSessionToken token1 = new AuthTabSessionToken(binder, null);
        AuthTabSessionToken token2 = new AuthTabSessionToken(binder, null);

        assertEquals(token1, token2);
    }

    @Test
    public void testNonEquality_withBinder() {
        IAuthTabCallback.Stub binder1 = new AuthTabSessionToken.MockCallback();
        IAuthTabCallback.Stub binder2 = new AuthTabSessionToken.MockCallback();

        AuthTabSessionToken token1 = new AuthTabSessionToken(binder1, null);
        AuthTabSessionToken token2 = new AuthTabSessionToken(binder2, null);

        assertNotEquals(token1, token2);
    }

    @Test
    public void testNonEquality_mixedIdAndBinder() {
        // Using the same binder to ensure only the id matters.
        IAuthTabCallback.Stub binder = new AuthTabSessionToken.MockCallback();

        AuthTabSessionToken token1 = new AuthTabSessionToken(binder, createSessionId(10));
        // Tokens cannot be mixed if only one has an id even if the binder is the same.
        AuthTabSessionToken token2 = new AuthTabSessionToken(binder, null);

        assertNotEquals(token1, token2);
    }

    // This code does the same as CustomTabsClient#createSessionId but that is not necessary for the
    // test, we just need to create a PendingIntent that uses sessionId as the requestCode.
    private PendingIntent createSessionId(int sessionId) {
        return PendingIntent.getActivity(mContext, sessionId, new Intent(), 0);
    }

    private void assertEquals(AuthTabSessionToken token1, AuthTabSessionToken token2) {
        Assert.assertEquals(token1, token2);
        Assert.assertEquals(token2, token1);

        Assert.assertEquals(token1.hashCode(), token2.hashCode());
    }

    private void assertNotEquals(AuthTabSessionToken token1, AuthTabSessionToken token2) {
        Assert.assertNotEquals(token1, token2);
        Assert.assertNotEquals(token2, token1);

        // I guess technically this could be flaky, but let's hope not...
        Assert.assertNotEquals(token1.hashCode(), token2.hashCode());
    }
}
