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

package androidx.browser.customtabs;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.customtabs.ICustomTabsCallback;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.filters.SmallTest;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.internal.DoNotInstrument;

/**
 * Tests for {@link CustomTabsSessionToken}.
 */
@RunWith(RobolectricTestRunner.class)
@DoNotInstrument
@SmallTest
public class CustomTabsSessionTokenTest {
    private Context mContext;

    @Before
    public void setup() {
        mContext = ApplicationProvider.getApplicationContext();
    }

    @Test
    public void testEquality_withId() {
        CustomTabsSessionToken token1 = new CustomTabsSessionToken(
                new CustomTabsSessionToken.MockCallback(),
                createSessionId(27)
        );

        CustomTabsSessionToken token2 = new CustomTabsSessionToken(
                new CustomTabsSessionToken.MockCallback(),
                createSessionId(27)
        );

        assertEquals(token1, token2);
    }

    @Test
    public void testNonEquality_withId() {
        // Using the same binder to ensure only the id matters.
        ICustomTabsCallback.Stub binder = new CustomTabsSessionToken.MockCallback();

        CustomTabsSessionToken token1 = new CustomTabsSessionToken(binder, createSessionId(10));
        CustomTabsSessionToken token2 = new CustomTabsSessionToken(binder, createSessionId(20));

        assertNotEquals(token1, token2);
    }

    @Test
    public void testEquality_withBinder() {
        ICustomTabsCallback.Stub binder = new CustomTabsSessionToken.MockCallback();

        CustomTabsSessionToken token1 = new CustomTabsSessionToken(binder, null);
        CustomTabsSessionToken token2 = new CustomTabsSessionToken(binder, null);

        assertEquals(token1, token2);
    }

    @Test
    public void testNonEquality_withBinder() {
        ICustomTabsCallback.Stub binder1 = new CustomTabsSessionToken.MockCallback();
        ICustomTabsCallback.Stub binder2 = new CustomTabsSessionToken.MockCallback();

        CustomTabsSessionToken token1 = new CustomTabsSessionToken(binder1, null);
        CustomTabsSessionToken token2 = new CustomTabsSessionToken(binder2, null);

        assertNotEquals(token1, token2);
    }

    @Test
    public void testNonEquality_mixedIdAndBinder() {
        // Using the same binder to ensure only the id matters.
        ICustomTabsCallback.Stub binder = new CustomTabsSessionToken.MockCallback();

        CustomTabsSessionToken token1 = new CustomTabsSessionToken(binder, createSessionId(10));
        // Tokens cannot be mixed if only one has an id even if the binder is the same.
        CustomTabsSessionToken token2 = new CustomTabsSessionToken(binder, null);

        assertNotEquals(token1, token2);
    }

    // This code does the same as CustomTabsClient#createSessionId but that is not necessary for the
    // test, we just need to create a PendingIntent that uses sessionId as the requestCode.
    private PendingIntent createSessionId(int sessionId) {
        return PendingIntent.getActivity(mContext, sessionId, new Intent(), 0);
    }

    private void assertEquals(CustomTabsSessionToken token1, CustomTabsSessionToken token2) {
        Assert.assertEquals(token1, token2);
        Assert.assertEquals(token2, token1);

        Assert.assertEquals(token1.hashCode(), token2.hashCode());
    }

    private void assertNotEquals(CustomTabsSessionToken token1, CustomTabsSessionToken token2) {
        Assert.assertNotEquals(token1, token2);
        Assert.assertNotEquals(token2, token1);

        // I guess technically this could be flaky, but let's hope not...
        Assert.assertNotEquals(token1.hashCode(), token2.hashCode());
    }
}
