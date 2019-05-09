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
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;

import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests for CustomTabsIntent.
 */
@RunWith(AndroidJUnit4.class)
@SmallTest
public class CustomTabsIntentTest {

    @Test
    public void testBareboneCustomTabIntent() {
        CustomTabsIntent customTabsIntent = new CustomTabsIntent.Builder().build();
        Intent intent = customTabsIntent.intent;
        assertNotNull(intent);
        assertNull(customTabsIntent.startAnimationBundle);

        assertEquals(Intent.ACTION_VIEW, intent.getAction());
        assertTrue(intent.hasExtra(CustomTabsIntent.EXTRA_SESSION));
        if (Build.VERSION.SDK_INT >= 18) {
            assertNull(intent.getExtras().getBinder(CustomTabsIntent.EXTRA_SESSION));
        }
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
}
