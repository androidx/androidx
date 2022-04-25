/*
 * Copyright 2018 The Android Open Source Project
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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import android.content.Intent;
import android.graphics.Color;
import android.os.Build;

import androidx.annotation.ColorRes;
import androidx.annotation.RequiresApi;
import androidx.test.core.app.ApplicationProvider;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.internal.DoNotInstrument;

/**
 * Tests for CustomTabsIntent.
 */
@SuppressWarnings("deprecation")
@RunWith(RobolectricTestRunner.class)
@DoNotInstrument
// minSdk For Bundle#getBinder
@RequiresApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
@Config(minSdk = Build.VERSION_CODES.JELLY_BEAN_MR2)
public class CustomTabsIntentTest {

    @Test
    public void testBareboneCustomTabIntent() {
        CustomTabsIntent customTabsIntent = new CustomTabsIntent.Builder().build();
        Intent intent = customTabsIntent.intent;
        assertNotNull(intent);
        assertNull(customTabsIntent.startAnimationBundle);

        assertEquals(Intent.ACTION_VIEW, intent.getAction());
        assertNull(intent.getComponent());
    }

    @Test
    public void testToolbarColor() {
        int color = Color.RED;
        Intent intent = new CustomTabsIntent.Builder().setToolbarColor(color).build().intent;
        assertTrue(intent.hasExtra(CustomTabsIntent.EXTRA_TOOLBAR_COLOR));
        assertEquals(color, intent.getIntExtra(CustomTabsIntent.EXTRA_TOOLBAR_COLOR, 0));
    }

    @Test
    public void testNavBarDividerColor() {
        int color = Color.RED;
        Intent intent = new CustomTabsIntent.Builder()
                .setNavigationBarDividerColor(color).build().intent;
        assertTrue(intent.hasExtra(CustomTabsIntent.EXTRA_NAVIGATION_BAR_DIVIDER_COLOR));
        assertEquals(color, intent.getIntExtra(CustomTabsIntent.EXTRA_NAVIGATION_BAR_DIVIDER_COLOR,
                0));
    }

    @Test
    public void testToolbarColorIsNotAResource() {
        @ColorRes int colorId = android.R.color.background_dark;
        int color = ApplicationProvider.getApplicationContext().getResources().getColor(colorId);
        Intent intent = new CustomTabsIntent.Builder().setToolbarColor(colorId).build().intent;
        assertFalse("The color should not be a resource ID",
                color == intent.getIntExtra(CustomTabsIntent.EXTRA_TOOLBAR_COLOR, 0));
        intent = new CustomTabsIntent.Builder().setToolbarColor(color).build().intent;
        assertEquals(color, intent.getIntExtra(CustomTabsIntent.EXTRA_TOOLBAR_COLOR, 0));
    }

    @Test
    public void testSecondaryToolbarColor() {
        int color = Color.RED;
        Intent intent = new CustomTabsIntent.Builder()
                .setSecondaryToolbarColor(color)
                .build()
                .intent;
        assertEquals(color, intent.getIntExtra(CustomTabsIntent.EXTRA_SECONDARY_TOOLBAR_COLOR, 0));
    }

    @Test
    public void testColorScheme() {
        try {
            new CustomTabsIntent.Builder().setColorScheme(-1);
            fail("Underflow arguments are expected to throw an exception");
        } catch (IllegalArgumentException exception) {
        }

        try {
            new CustomTabsIntent.Builder().setColorScheme(42);
            fail("Overflow arguments are expected to throw an exception");
        } catch (IllegalArgumentException exception) {
        }

        // None of the valid parameters should throw.
        final int[] colorSchemeValues = new int[] {
            CustomTabsIntent.COLOR_SCHEME_SYSTEM,
            CustomTabsIntent.COLOR_SCHEME_LIGHT,
            CustomTabsIntent.COLOR_SCHEME_DARK
        };

        for (int value : colorSchemeValues) {
            Intent intent =
                    new CustomTabsIntent.Builder().setColorScheme(value).build().intent;
            assertEquals(value, intent.getIntExtra(CustomTabsIntent.EXTRA_COLOR_SCHEME, -1));
        }
    }

    @Test
    public void testDefaultColorSchemeParams() {
        int toolbarColor = Color.RED;
        int navigationBarColor = Color.GREEN;
        int navigationBarDividerColor = Color.BLUE;
        int secondaryToolbarColor = Color.WHITE;
        CustomTabColorSchemeParams defaultParam = new CustomTabColorSchemeParams.Builder()
                .setToolbarColor(toolbarColor)
                .setNavigationBarColor(navigationBarColor)
                .setNavigationBarDividerColor(navigationBarDividerColor)
                .setSecondaryToolbarColor(secondaryToolbarColor)
                .build();
        Intent intent =
                new CustomTabsIntent.Builder().setDefaultColorSchemeParams(
                        defaultParam).build().intent;

        assertEquals(toolbarColor, intent.getIntExtra(CustomTabsIntent.EXTRA_TOOLBAR_COLOR, 0));
        assertEquals(secondaryToolbarColor, intent.getIntExtra(
                CustomTabsIntent.EXTRA_SECONDARY_TOOLBAR_COLOR, 0));
        assertEquals(navigationBarColor, intent.getIntExtra(
                CustomTabsIntent.EXTRA_NAVIGATION_BAR_COLOR, 0));
        assertEquals(navigationBarDividerColor, intent.getIntExtra(
                CustomTabsIntent.EXTRA_NAVIGATION_BAR_DIVIDER_COLOR, 0));
    }

    public void throwsError_WhenInvalidShareStateSet() {
        try {
            new CustomTabsIntent.Builder().setShareState(-1);
            fail("Underflow arguments are expected to throw an exception");
        } catch (IllegalArgumentException exception) {
        }

        try {
            new CustomTabsIntent.Builder().setShareState(42);
            fail("Overflow arguments are expected to throw an exception");
        } catch (IllegalArgumentException exception) {
        }
    }

    @Test
    public void hasShareState_WhenValidShareStateSet() {
        final int[] shareStateValues = new int[]{
                CustomTabsIntent.SHARE_STATE_DEFAULT,
                CustomTabsIntent.SHARE_STATE_ON,
                CustomTabsIntent.SHARE_STATE_OFF
        };

        for (int value : shareStateValues) {
            Intent intent =
                    new CustomTabsIntent.Builder().setShareState(value).build().intent;
            assertEquals(value, intent.getIntExtra(CustomTabsIntent.EXTRA_SHARE_STATE, -1));
        }
    }

    @Test
    public void hasDefaultShareStateAndNoShareMenuItem_WhenBuiltWithDefaultConstructor() {
        Intent intent = new CustomTabsIntent.Builder().build().intent;

        assertEquals(CustomTabsIntent.SHARE_STATE_DEFAULT,
                intent.getIntExtra(CustomTabsIntent.EXTRA_SHARE_STATE, -1));
        assertFalse(intent.hasExtra(CustomTabsIntent.EXTRA_DEFAULT_SHARE_MENU_ITEM));
    }

    @Test
    public void hasDefaultShareStateAndNoShareMenuItem_WhenShareStateSetToDefault() {
        Intent intent = new CustomTabsIntent.Builder().setShareState(
                CustomTabsIntent.SHARE_STATE_DEFAULT).build().intent;

        assertEquals(CustomTabsIntent.SHARE_STATE_DEFAULT,
                intent.getIntExtra(CustomTabsIntent.EXTRA_SHARE_STATE, -1));
        assertFalse(intent.hasExtra(CustomTabsIntent.EXTRA_DEFAULT_SHARE_MENU_ITEM));
    }

    @Test
    public void hasShareStateOffAndShareMenuItemFalse_WhenShareStateSetToOff() {
        Intent intent = new CustomTabsIntent.Builder().setShareState(
                CustomTabsIntent.SHARE_STATE_OFF).build().intent;

        assertEquals(CustomTabsIntent.SHARE_STATE_OFF,
                intent.getIntExtra(CustomTabsIntent.EXTRA_SHARE_STATE, -1));
        assertFalse(intent.getBooleanExtra(CustomTabsIntent.EXTRA_DEFAULT_SHARE_MENU_ITEM, false));
    }

    @Test
    public void hasShareStateOnAndShareMenuItemTrue_WhenShareStateSetToOn() {
        Intent intent = new CustomTabsIntent.Builder().setShareState(
                CustomTabsIntent.SHARE_STATE_ON).build().intent;

        assertEquals(CustomTabsIntent.SHARE_STATE_ON,
                intent.getIntExtra(CustomTabsIntent.EXTRA_SHARE_STATE, -1));
        assertTrue(intent.getBooleanExtra(CustomTabsIntent.EXTRA_DEFAULT_SHARE_MENU_ITEM, false));
    }

    @Test
    public void hasShareStateOnAndShareMenuItemTrue_WhenAddDefaultShareMenuItemIsCalled() {
        Intent intent = new CustomTabsIntent.Builder().addDefaultShareMenuItem().build().intent;

        assertEquals(CustomTabsIntent.SHARE_STATE_ON,
                intent.getIntExtra(CustomTabsIntent.EXTRA_SHARE_STATE, -1));
        assertTrue(intent.getBooleanExtra(CustomTabsIntent.EXTRA_DEFAULT_SHARE_MENU_ITEM, false));
    }

    @Test
    public void hasShareStateDefaultAndNoShareMenuItem_WhenShareStateChangedToDefault() {
        Intent intent = new CustomTabsIntent.Builder()
                .setShareState(CustomTabsIntent.SHARE_STATE_OFF)
                .setShareState(CustomTabsIntent.SHARE_STATE_DEFAULT)
                .build()
                .intent;

        assertEquals(CustomTabsIntent.SHARE_STATE_DEFAULT,
                intent.getIntExtra(CustomTabsIntent.EXTRA_SHARE_STATE, -1));
        assertFalse(intent.hasExtra(CustomTabsIntent.EXTRA_DEFAULT_SHARE_MENU_ITEM));
    }

    @Test
    public void hasShareStateOnAndShareMenuItem_WhenSetDefaultShareMenuItemIsTrue() {
        Intent intent =
                new CustomTabsIntent.Builder().setDefaultShareMenuItemEnabled(true).build().intent;

        assertEquals(CustomTabsIntent.SHARE_STATE_ON,
                intent.getIntExtra(CustomTabsIntent.EXTRA_SHARE_STATE, -1));
        assertTrue(intent.getBooleanExtra(CustomTabsIntent.EXTRA_DEFAULT_SHARE_MENU_ITEM, false));
    }

    @Test
    public void hasShareStateOffAndShareMenuItemFalse_WhenSetDefaultShareMenuItemIsFalse() {
        Intent intent =
                new CustomTabsIntent.Builder().setDefaultShareMenuItemEnabled(false).build().intent;

        assertEquals(CustomTabsIntent.SHARE_STATE_OFF,
                intent.getIntExtra(CustomTabsIntent.EXTRA_SHARE_STATE, -1));
        assertFalse(intent.getBooleanExtra(CustomTabsIntent.EXTRA_DEFAULT_SHARE_MENU_ITEM, false));
    }

    @Test
    public void hasNullSessionExtra_WhenBuiltWithDefaultConstructor() {
        Intent intent = new CustomTabsIntent.Builder().build().intent;
        assertNullSessionInExtras(intent);
    }

    @Test
    public void hasNullSessionExtra_WhenBuiltWithNullSession() {
        CustomTabsSession session = null;
        Intent intent = new CustomTabsIntent.Builder(session).build().intent;
        assertNullSessionInExtras(intent);
    }

    @Test
    public void putsSessionBinderAndId_IfSuppliedInConstructor() {
        CustomTabsSession session = TestUtil.makeMockSession();
        Intent intent = new CustomTabsIntent.Builder(session).build().intent;
        assertEquals(session.getBinder(),
                intent.getExtras().getBinder(CustomTabsIntent.EXTRA_SESSION));
        assertEquals(session.getId(), intent.getParcelableExtra(CustomTabsIntent.EXTRA_SESSION_ID));
    }

    @Test
    public void putsSessionBinderAndId_IfSuppliedInSetter() {
        CustomTabsSession session = TestUtil.makeMockSession();
        Intent intent = new CustomTabsIntent.Builder().setSession(session).build().intent;
        assertEquals(session.getBinder(),
                intent.getExtras().getBinder(CustomTabsIntent.EXTRA_SESSION));
        assertEquals(session.getId(), intent.getParcelableExtra(CustomTabsIntent.EXTRA_SESSION_ID));
    }

    @Test
    public void putsPendingSessionId() {
        CustomTabsSession.PendingSession pendingSession = TestUtil.makeMockPendingSession();
        Intent intent = new CustomTabsIntent.Builder().setPendingSession(pendingSession).build()
                .intent;
        assertEquals(pendingSession.getId(),
                intent.getParcelableExtra(CustomTabsIntent.EXTRA_SESSION_ID));
    }

    private void assertNullSessionInExtras(Intent intent) {
        assertTrue(intent.hasExtra(CustomTabsIntent.EXTRA_SESSION));
        assertNull(intent.getExtras().getBinder(CustomTabsIntent.EXTRA_SESSION));
    }
}
