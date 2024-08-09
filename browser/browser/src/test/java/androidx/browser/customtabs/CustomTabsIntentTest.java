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

import static com.google.common.net.HttpHeaders.ACCEPT_LANGUAGE;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.Network;
import android.os.Build;
import android.os.Bundle;
import android.os.LocaleList;
import android.provider.Browser;

import androidx.annotation.ColorRes;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.filters.SdkSuppress;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.internal.DoNotInstrument;

import java.util.Locale;

/**
 * Tests for CustomTabsIntent.
 */
@SuppressWarnings("deprecation")
@RunWith(RobolectricTestRunner.class)
@DoNotInstrument
public class CustomTabsIntentTest {

    @Config(maxSdk = 33) // maxSdk due to b/308686817
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

    @Test
    public void testActivityInitialFixedHeightResizeBehavior() {
        int heightFixedResizeBehavior = CustomTabsIntent.ACTIVITY_HEIGHT_FIXED;
        int initialActivityHeight = 200;

        Intent intent = new CustomTabsIntent.Builder()
                .setInitialActivityHeightPx(initialActivityHeight, heightFixedResizeBehavior)
                .build()
                .intent;

        assertEquals("The value of EXTRA_ACTIVITY_HEIGHT_RESIZE_BEHAVIOR should be "
                        + "ACTIVITY_HEIGHT_FIXED.",
                heightFixedResizeBehavior,
                intent.getIntExtra(CustomTabsIntent.EXTRA_ACTIVITY_HEIGHT_RESIZE_BEHAVIOR,
                        CustomTabsIntent.ACTIVITY_HEIGHT_DEFAULT));
        assertEquals("The height should be the same as the one that was set.",
                initialActivityHeight,
                intent.getIntExtra(CustomTabsIntent.EXTRA_INITIAL_ACTIVITY_HEIGHT_PX, 0));
        assertEquals("The value returned by the getter should be the same.",
                heightFixedResizeBehavior,
                CustomTabsIntent.getActivityResizeBehavior(intent));
        assertEquals("The height returned by the getter should be the same.",
                initialActivityHeight,
                CustomTabsIntent.getInitialActivityHeightPx(intent));
    }

    @Test
    public void testActivityInitialAdjustableHeightResizeBehavior() {
        int heightAdjustableResizeBehavior = CustomTabsIntent.ACTIVITY_HEIGHT_ADJUSTABLE;
        int initialActivityHeight = 200;

        Intent intent = new CustomTabsIntent.Builder()
                .setInitialActivityHeightPx(initialActivityHeight, heightAdjustableResizeBehavior)
                .build()
                .intent;

        assertEquals("The value of EXTRA_ACTIVITY_HEIGHT_RESIZE_BEHAVIOR should be "
                        + "ACTIVITY_HEIGHT_ADJUSTABLE.",
                heightAdjustableResizeBehavior,
                intent.getIntExtra(CustomTabsIntent.EXTRA_ACTIVITY_HEIGHT_RESIZE_BEHAVIOR,
                        CustomTabsIntent.ACTIVITY_HEIGHT_DEFAULT));
        assertEquals("The height should be the same as the one that was set.",
                initialActivityHeight,
                intent.getIntExtra(CustomTabsIntent.EXTRA_INITIAL_ACTIVITY_HEIGHT_PX, 0));
        assertEquals("The value returned by the getter should be the same.",
                heightAdjustableResizeBehavior,
                CustomTabsIntent.getActivityResizeBehavior(intent));
        assertEquals("The height returned by the getter should be the same.",
                initialActivityHeight,
                CustomTabsIntent.getInitialActivityHeightPx(intent));
    }

    @Test
    public void testActivityInitialHeightCorrectValue() {
        int initialActivityHeight = 200;
        int defaultResizeBehavior = CustomTabsIntent.ACTIVITY_HEIGHT_DEFAULT;

        Intent intent = new CustomTabsIntent.Builder()
                .setInitialActivityHeightPx(initialActivityHeight)
                .build()
                .intent;

        assertEquals("The height should be the same as the one that was set.",
                initialActivityHeight,
                intent.getIntExtra(CustomTabsIntent.EXTRA_INITIAL_ACTIVITY_HEIGHT_PX, 0));
        assertEquals("The value of EXTRA_ACTIVITY_HEIGHT_RESIZE_BEHAVIOR should be "
                        + "ACTIVITY_HEIGHT_DEFAULT.",
                defaultResizeBehavior,
                intent.getIntExtra(CustomTabsIntent.EXTRA_ACTIVITY_HEIGHT_RESIZE_BEHAVIOR,
                        CustomTabsIntent.ACTIVITY_HEIGHT_FIXED));
        assertEquals("The height returned by the getter should be the same.",
                initialActivityHeight,
                CustomTabsIntent.getInitialActivityHeightPx(intent));
        assertEquals("The value returned by the getter should be the same.",
                defaultResizeBehavior,
                CustomTabsIntent.getActivityResizeBehavior(intent));
    }

    @Test
    public void testActivityInitialFixedHeightExtraNotSet() {
        int defaultInitialActivityHeight = 0;
        int defaultResizeBehavior = CustomTabsIntent.ACTIVITY_HEIGHT_DEFAULT;

        Intent intent = new CustomTabsIntent.Builder().build().intent;

        assertFalse("The EXTRA_INITIAL_ACTIVITY_HEIGHT_PX should not be set.",
                intent.hasExtra(CustomTabsIntent.EXTRA_INITIAL_ACTIVITY_HEIGHT_PX));
        assertFalse("The EXTRA_ACTIVITY_HEIGHT_RESIZE_BEHAVIOR should not be set.",
                intent.hasExtra(CustomTabsIntent.EXTRA_ACTIVITY_HEIGHT_RESIZE_BEHAVIOR));
        assertEquals("The getter should return the default value.",
                defaultInitialActivityHeight,
                CustomTabsIntent.getInitialActivityHeightPx(intent));
        assertEquals("The getter should return the default value.",
                defaultResizeBehavior,
                CustomTabsIntent.getActivityResizeBehavior(intent));
    }

    @Test
    public void testActivityInitialHeightInvalidValuesThrow() {
        try {
            new CustomTabsIntent.Builder().setInitialActivityHeightPx(-1);
            fail("The height of the activity should be higher than 0.");
        } catch (IllegalArgumentException exception) {
        }

        try {
            new CustomTabsIntent.Builder().setInitialActivityHeightPx(100, -1);
            fail("Underflow arguments are expected to throw an exception");
        } catch (IllegalArgumentException exception) {
        }

        try {
            new CustomTabsIntent.Builder().setInitialActivityHeightPx(100,
                    CustomTabsIntent.ACTIVITY_HEIGHT_FIXED + 1);
            fail("Overflow arguments are expected to throw an exception");
        } catch (IllegalArgumentException exception) {
        }
    }

    @Test
    public void testToolbarCornerRadiusDpCorrectValue() {
        int cornerRadiusDp = 16;

        Intent intent = new CustomTabsIntent.Builder()
                .setToolbarCornerRadiusDp(cornerRadiusDp)
                .build()
                .intent;

        assertEquals("The toolbar corner radius should be the same as the one that was set.",
                cornerRadiusDp,
                intent.getIntExtra(CustomTabsIntent.EXTRA_TOOLBAR_CORNER_RADIUS_DP, 0));
        assertEquals("The toolbar corner radius returned by the getter should be the same.",
                cornerRadiusDp,
                CustomTabsIntent.getToolbarCornerRadiusDp(intent));
    }

    @Test
    public void testToolbarCornerRadiusDpExtraNotSet() {
        int defaultCornerRadiusDp = 16;

        Intent intent = new CustomTabsIntent.Builder().build().intent;

        assertFalse("The EXTRA_TOOLBAR_CORNER_RADIUS_DP should not be set.",
                intent.hasExtra(CustomTabsIntent.EXTRA_TOOLBAR_CORNER_RADIUS_DP));
        assertEquals("The getter should return the default value.",
                defaultCornerRadiusDp,
                CustomTabsIntent.getToolbarCornerRadiusDp(intent));
    }

    @Test
    public void testToolbarCornerRadiusDpInvalidValueThrows() {
        try {
            new CustomTabsIntent.Builder().setToolbarCornerRadiusDp(-1);
            fail("Underflow arguments are expected to throw an exception");
        } catch (IllegalArgumentException exception) {
        }

        try {
            new CustomTabsIntent.Builder().setToolbarCornerRadiusDp(17);
            fail("Overflow arguments are expected to throw an exception");
        } catch (IllegalArgumentException exception) {
        }
    }
    @Test
    public void testCloseButtonPositionCorrectValue() {
        int closeButtonPosition = CustomTabsIntent.CLOSE_BUTTON_POSITION_START;

        Intent intent = new CustomTabsIntent.Builder()
                .setCloseButtonPosition(closeButtonPosition)
                .build()
                .intent;

        assertEquals("The close button position should be the same as the one that was set.",
                closeButtonPosition,
                intent.getIntExtra(CustomTabsIntent.EXTRA_CLOSE_BUTTON_POSITION,
                        CustomTabsIntent.CLOSE_BUTTON_POSITION_END));
        assertEquals("The close button position returned by the getter should be the same.",
                closeButtonPosition,
                CustomTabsIntent.getCloseButtonPosition(intent));
    }

    @Test
    public void testCloseButtonPositionExtraNotSet() {
        int defaultPosition = CustomTabsIntent.CLOSE_BUTTON_POSITION_DEFAULT;

        Intent intent = new CustomTabsIntent.Builder().build().intent;

        assertFalse("The EXTRA_CLOSE_BUTTON_POSITION should not be set.",
                intent.hasExtra(CustomTabsIntent.EXTRA_CLOSE_BUTTON_POSITION));
        assertEquals("The getter should return the default value.",
                defaultPosition,
                CustomTabsIntent.getCloseButtonPosition(intent));
    }

    @Test
    public void testCloseButtonPositionInvalidValueThrows() {
        try {
            new CustomTabsIntent.Builder().setCloseButtonPosition(-1);
            fail("Underflow arguments are expected to throw an exception");
        } catch (IllegalArgumentException exception) {
        }

        try {
            new CustomTabsIntent.Builder()
                    .setCloseButtonPosition(CustomTabsIntent.CLOSE_BUTTON_POSITION_END + 1);
            fail("Overflow arguments are expected to throw an exception");
        } catch (IllegalArgumentException exception) {
        }
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

    @SuppressWarnings("deprecation")
    @Test
    public void putsSessionBinderAndId_IfSuppliedInConstructor() {
        CustomTabsSession session = TestUtil.makeMockSession();
        Intent intent = new CustomTabsIntent.Builder(session).build().intent;
        assertEquals(session.getBinder(),
                intent.getExtras().getBinder(CustomTabsIntent.EXTRA_SESSION));
        assertEquals(session.getId(), intent.getParcelableExtra(CustomTabsIntent.EXTRA_SESSION_ID));
    }

    @SuppressWarnings("deprecation")
    @Test
    public void putsSessionBinderAndId_IfSuppliedInSetter() {
        CustomTabsSession session = TestUtil.makeMockSession();
        Intent intent = new CustomTabsIntent.Builder().setSession(session).build().intent;
        assertEquals(session.getBinder(),
                intent.getExtras().getBinder(CustomTabsIntent.EXTRA_SESSION));
        assertEquals(session.getId(), intent.getParcelableExtra(CustomTabsIntent.EXTRA_SESSION_ID));
    }

    @SuppressWarnings("deprecation")
    @Test
    public void putsPendingSessionId() {
        CustomTabsSession.PendingSession pendingSession = TestUtil.makeMockPendingSession();
        Intent intent = new CustomTabsIntent.Builder().setPendingSession(pendingSession).build()
                .intent;
        assertEquals(pendingSession.getId(),
                intent.getParcelableExtra(CustomTabsIntent.EXTRA_SESSION_ID));
    }

    @Config(maxSdk = Build.VERSION_CODES.M)
    @Test
    public void putDefaultAcceptLanguage_BeforeSdk24() {
        Intent intent = new CustomTabsIntent.Builder().build().intent;

        Bundle header = intent.getBundleExtra(Browser.EXTRA_HEADERS);
        boolean isEmptyAcceptLanguage = header == null || !header.containsKey(ACCEPT_LANGUAGE);
        assertTrue(isEmptyAcceptLanguage);
    }

    @Config(minSdk = Build.VERSION_CODES.N)
    @Test
    public void putDefaultAcceptLanguage() {
        Intent intent = new CustomTabsIntent.Builder().build().intent;

        assertEquals(LocaleList.getAdjustedDefault().get(0).toLanguageTag(),
                intent.getBundleExtra(Browser.EXTRA_HEADERS).getString(ACCEPT_LANGUAGE));
    }

    @Test
    public void testBookmarksButton() {
        Intent intent = new CustomTabsIntent.Builder().build().intent;
        assertTrue(CustomTabsIntent.isBookmarksButtonEnabled(intent));

        intent = new CustomTabsIntent.Builder().setBookmarksButtonEnabled(true).build().intent;
        assertTrue(CustomTabsIntent.isBookmarksButtonEnabled(intent));

        // Disabled only when explicitly called to disable it.
        intent = new CustomTabsIntent.Builder().setBookmarksButtonEnabled(false).build().intent;
        assertFalse(CustomTabsIntent.isBookmarksButtonEnabled(intent));
    }

    @Test
    public void testDownloadButton() {
        Intent intent = new CustomTabsIntent.Builder().build().intent;
        assertTrue(CustomTabsIntent.isDownloadButtonEnabled(intent));

        intent = new CustomTabsIntent.Builder().setDownloadButtonEnabled(true).build().intent;
        assertTrue(CustomTabsIntent.isDownloadButtonEnabled(intent));

        // Disabled only when explicitly called to disable it.
        intent = new CustomTabsIntent.Builder().setDownloadButtonEnabled(false).build().intent;
        assertFalse(CustomTabsIntent.isDownloadButtonEnabled(intent));
    }

    @Test
    public void testSendToExternalDefaultHandler() {
        Intent intent = new CustomTabsIntent.Builder().build().intent;
        assertFalse(CustomTabsIntent.isSendToExternalDefaultHandlerEnabled(intent));

        intent = new CustomTabsIntent.Builder()
                .setSendToExternalDefaultHandlerEnabled(false).build().intent;
        assertFalse(CustomTabsIntent.isSendToExternalDefaultHandlerEnabled(intent));

        // The extra is set to true only when explicitly called to enable it.
        intent = new CustomTabsIntent.Builder()
                .setSendToExternalDefaultHandlerEnabled(true).build().intent;
        assertTrue(CustomTabsIntent.isSendToExternalDefaultHandlerEnabled(intent));
    }

    @Config(minSdk = Build.VERSION_CODES.N)
    @Test
    public void testBackgroundInteraction() {
        Intent intent = new CustomTabsIntent.Builder().build().intent;
        assertTrue(CustomTabsIntent.isBackgroundInteractionEnabled(intent));

        intent = new CustomTabsIntent.Builder()
                .setBackgroundInteractionEnabled(true).build().intent;
        assertTrue(CustomTabsIntent.isBackgroundInteractionEnabled(intent));

        // The extra (EXTRA_DISABLE_BACKGROUND_INTERACTION) is set to true
        // only when explicitly called to disable it.
        intent = new CustomTabsIntent.Builder()
                .setBackgroundInteractionEnabled(false).build().intent;
        assertFalse(CustomTabsIntent.isBackgroundInteractionEnabled(intent));
    }

    @Config(minSdk = Build.VERSION_CODES.N)
    @Test
    public void testTranslateLocale() {
        Intent intent = new CustomTabsIntent.Builder().build().intent;
        assertNull(CustomTabsIntent.getTranslateLocale(intent));

        intent = new CustomTabsIntent.Builder().setTranslateLocale(Locale.FRANCE).build().intent;
        Locale locale = CustomTabsIntent.getTranslateLocale(intent);
        assertEquals(locale.toLanguageTag(), Locale.FRANCE.toLanguageTag());
    }

    @Config(minSdk = Build.VERSION_CODES.N)
    @Test
    public void testSecondaryToolbarSwipeUpGesture() {
        PendingIntent pendingIntent = TestUtil.makeMockPendingIntent();
        Intent intent = new CustomTabsIntent.Builder()
                .setSecondaryToolbarSwipeUpGesture(pendingIntent)
                .build()
                .intent;
        assertEquals(pendingIntent, CustomTabsIntent.getSecondaryToolbarSwipeUpGesture(intent));
    }

    @Test
    public void testInitialActivityWidthPx() {
        // Intent Extra is set to a correct value.
        int initialActivityWidthPx = 200;
        Intent intent = new CustomTabsIntent.Builder()
                .setInitialActivityWidthPx(initialActivityWidthPx)
                .build()
                .intent;

        assertEquals("The width should be the same as the one that was set.",
                initialActivityWidthPx,
                intent.getIntExtra(CustomTabsIntent.EXTRA_INITIAL_ACTIVITY_WIDTH_PX, 0));
        assertEquals("The width returned by the getter should be the same.",
                initialActivityWidthPx,
                CustomTabsIntent.getInitialActivityWidthPx(intent));


        // Intent Extra is not set, default value should be returned for the getter.
        int defaultInitialActivityWidth = 0;
        intent = new CustomTabsIntent.Builder().build().intent;

        assertFalse("The EXTRA_INITIAL_ACTIVITY_WIDTH_PX should not be set.",
                intent.hasExtra(CustomTabsIntent.EXTRA_INITIAL_ACTIVITY_WIDTH_PX));
        assertEquals("The getter should return the default value.",
                defaultInitialActivityWidth,
                CustomTabsIntent.getInitialActivityWidthPx(intent));

        // Intent Extra is trying to be set to an invalid value.
        try {
            new CustomTabsIntent.Builder().setInitialActivityWidthPx(-1);
            fail("The width of the activity should be higher than 0.");
        } catch (IllegalArgumentException exception) {
        }
    }

    @Test
    public void testActivitySideSheetRoundedCornersPosition() {
        // Intent Extra is set to a correct value.
        int roundedCornersPosition =
                CustomTabsIntent.ACTIVITY_SIDE_SHEET_ROUNDED_CORNERS_POSITION_TOP;
        Intent intent = new CustomTabsIntent.Builder()
                .setActivitySideSheetRoundedCornersPosition(roundedCornersPosition)
                .build()
                .intent;

        assertEquals("The rounded corners position should be the same as the one that was set.",
                roundedCornersPosition,
                intent.getIntExtra(
                        CustomTabsIntent.EXTRA_ACTIVITY_SIDE_SHEET_ROUNDED_CORNERS_POSITION,
                        CustomTabsIntent.ACTIVITY_SIDE_SHEET_ROUNDED_CORNERS_POSITION_DEFAULT));
        assertEquals("The rounded corners position returned by the getter should be the same.",
                roundedCornersPosition,
                CustomTabsIntent.getActivitySideSheetRoundedCornersPosition(intent));

        // Intent Extra is not set, default value should be returned for the getter.
        int defaultPosition = CustomTabsIntent.CLOSE_BUTTON_POSITION_DEFAULT;
        intent = new CustomTabsIntent.Builder().build().intent;

        assertFalse("The EXTRA_ACTIVITY_SIDE_SHEET_ROUNDED_CORNERS_POSITION should not be set.",
                intent.hasExtra(
                        CustomTabsIntent.EXTRA_ACTIVITY_SIDE_SHEET_ROUNDED_CORNERS_POSITION));
        assertEquals("The getter should return the default value.",
                defaultPosition,
                CustomTabsIntent.getActivitySideSheetRoundedCornersPosition(intent));

        // Intent Extra is trying to be set to an invalid value.
        try {
            new CustomTabsIntent.Builder().setActivitySideSheetRoundedCornersPosition(-1);
            fail("Underflow arguments are expected to throw an exception");
        } catch (IllegalArgumentException exception) {
        }

        try {
            new CustomTabsIntent.Builder()
                    .setActivitySideSheetRoundedCornersPosition(
                            CustomTabsIntent.ACTIVITY_SIDE_SHEET_ROUNDED_CORNERS_POSITION_TOP + 1);
            fail("Overflow arguments are expected to throw an exception");
        } catch (IllegalArgumentException exception) {
        }
    }

    @Test
    public void testActivitySideSheetDecorationType() {
        // Intent Extra is set to a correct value.
        int decorationType = CustomTabsIntent.ACTIVITY_SIDE_SHEET_DECORATION_TYPE_SHADOW;
        Intent intent = new CustomTabsIntent.Builder()
                .setActivitySideSheetDecorationType(decorationType)
                .build()
                .intent;

        assertEquals("The decoration type should be the same as the one that was set.",
                decorationType,
                intent.getIntExtra(CustomTabsIntent.EXTRA_ACTIVITY_SIDE_SHEET_DECORATION_TYPE,
                        CustomTabsIntent.ACTIVITY_SIDE_SHEET_DECORATION_TYPE_DEFAULT));
        assertEquals("The decoration type returned by the getter should be the same.",
                decorationType,
                CustomTabsIntent.getActivitySideSheetDecorationType(intent));

        // Intent Extra is not set, default value should be returned for the getter.
        int defaultDecorationType = CustomTabsIntent.ACTIVITY_SIDE_SHEET_DECORATION_TYPE_DEFAULT;
        intent = new CustomTabsIntent.Builder().build().intent;

        assertFalse("The EXTRA_ACTIVITY_SIDE_SHEET_DECORATION_TYPE should not be set.",
                intent.hasExtra(CustomTabsIntent.EXTRA_ACTIVITY_SIDE_SHEET_DECORATION_TYPE));
        assertEquals("The getter should return the default value.",
                defaultDecorationType,
                CustomTabsIntent.getActivitySideSheetDecorationType(intent));

        // Intent Extra is trying to be set to an invalid value.
        try {
            new CustomTabsIntent.Builder().setActivitySideSheetDecorationType(-1);
            fail("Underflow arguments are expected to throw an exception");
        } catch (IllegalArgumentException exception) {
        }

        try {
            new CustomTabsIntent.Builder()
                    .setActivitySideSheetDecorationType(
                            CustomTabsIntent.ACTIVITY_SIDE_SHEET_DECORATION_TYPE_DIVIDER + 1);
            fail("Overflow arguments are expected to throw an exception");
        } catch (IllegalArgumentException exception) {
        }
    }

    @Test
    public void testActivitySideSheetPosition() {
        // Intent Extra is set to a correct value.
        int position = CustomTabsIntent.ACTIVITY_SIDE_SHEET_POSITION_END;
        Intent intent = new CustomTabsIntent.Builder()
                .setActivitySideSheetPosition(position)
                .build()
                .intent;

        assertEquals("The position should be the same as the one that was set.",
                position,
                intent.getIntExtra(CustomTabsIntent.EXTRA_ACTIVITY_SIDE_SHEET_POSITION,
                        CustomTabsIntent.ACTIVITY_SIDE_SHEET_POSITION_DEFAULT));
        assertEquals("The position returned by the getter should be the same.",
                position,
                CustomTabsIntent.getActivitySideSheetPosition(intent));

        // Intent Extra is not set, default value should be returned for the getter.
        int defaultPosition = CustomTabsIntent.ACTIVITY_SIDE_SHEET_POSITION_DEFAULT;
        intent = new CustomTabsIntent.Builder().build().intent;

        assertFalse("The EXTRA_ACTIVITY_SIDE_SHEET_POSITION should not be set.",
                intent.hasExtra(CustomTabsIntent.EXTRA_ACTIVITY_SIDE_SHEET_POSITION));
        assertEquals("The getter should return the default value.",
                defaultPosition,
                CustomTabsIntent.getActivitySideSheetPosition(intent));

        // Intent Extra is trying to be set to an invalid value.
        try {
            new CustomTabsIntent.Builder().setActivitySideSheetPosition(-1);
            fail("Underflow arguments are expected to throw an exception");
        } catch (IllegalArgumentException exception) {
        }

        try {
            new CustomTabsIntent.Builder()
                    .setActivitySideSheetPosition(
                            CustomTabsIntent.ACTIVITY_SIDE_SHEET_POSITION_END + 1);
            fail("Overflow arguments are expected to throw an exception");
        } catch (IllegalArgumentException exception) {
        }
    }

    @Test
    public void testActivitySideSheetBreakpointDp() {
        // Intent Extra is set to a correct value.
        int breakpointDp = 200;
        int defaultBreakpointDp = 0;

        Intent intent = new CustomTabsIntent.Builder()
                .setActivitySideSheetBreakpointDp(breakpointDp)
                .build()
                .intent;

        assertEquals("The breakpoint should be the same as the one that was set.",
                breakpointDp,
                intent.getIntExtra(CustomTabsIntent.EXTRA_ACTIVITY_SIDE_SHEET_BREAKPOINT_DP,
                        defaultBreakpointDp));
        assertEquals("The breakpoint returned by the getter should be the same.",
                breakpointDp,
                CustomTabsIntent.getActivitySideSheetBreakpointDp(intent));

        // Intent Extra is not set, default value should be returned for the getter.
        intent = new CustomTabsIntent.Builder().build().intent;

        assertFalse("The EXTRA_INITIAL_ACTIVITY_WIDTH_PX should not be set.",
                intent.hasExtra(CustomTabsIntent.EXTRA_INITIAL_ACTIVITY_WIDTH_PX));
        assertEquals("The getter should return the default value.",
                defaultBreakpointDp,
                CustomTabsIntent.getActivitySideSheetBreakpointDp(intent));

        // Intent Extra is trying to be set to an invalid value.
        try {
            new CustomTabsIntent.Builder().setActivitySideSheetBreakpointDp(-1);
            fail("The breakpoint of the activity should be higher than 0.");
        } catch (IllegalArgumentException exception) {
        }
    }

    @Test
    public void testActivitySideSheetEnableMaximization() {
        Intent intent = new CustomTabsIntent.Builder().build().intent;
        assertFalse(CustomTabsIntent.isActivitySideSheetMaximizationEnabled(intent));

        intent = new CustomTabsIntent.Builder().setActivitySideSheetMaximizationEnabled(
                true).build().intent;
        assertTrue(CustomTabsIntent.isActivitySideSheetMaximizationEnabled(intent));

        intent = new CustomTabsIntent.Builder().setActivitySideSheetMaximizationEnabled(
                false).build().intent;
        assertFalse(CustomTabsIntent.isActivitySideSheetMaximizationEnabled(intent));
    }

    private void assertNullSessionInExtras(Intent intent) {
        assertTrue(intent.hasExtra(CustomTabsIntent.EXTRA_SESSION));
        assertNull(intent.getExtras().getBinder(CustomTabsIntent.EXTRA_SESSION));
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.M /* getActiveNetwork requires api>=23 */)
    public void testBindNetworkToCustomTabs() {
        ConnectivityManager cm =
                (ConnectivityManager) ApplicationProvider.getApplicationContext()
                        .getSystemService(Context.CONNECTIVITY_SERVICE);
        Network network = cm.getActiveNetwork();
        long expectedNetworkHandle = network.getNetworkHandle();
        Intent intent = new CustomTabsIntent.Builder()
                .setNetwork(network)
                .build()
                .intent;
        assertNotNull(intent);
        assertTrue(intent.hasExtra(CustomTabsIntent.EXTRA_NETWORK));
        assertNotNull(CustomTabsIntent.getNetwork(intent));
        assertEquals(expectedNetworkHandle,
                CustomTabsIntent.getNetwork(intent).getNetworkHandle());
    }
}
