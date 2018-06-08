/*
 * Copyright (C) 2015 The Android Open Source Project
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

package androidx.appcompat.app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.os.Build;
import android.os.SystemClock;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.SmallTest;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;

import androidx.appcompat.testutils.BaseTestActivity;
import androidx.appcompat.widget.Toolbar;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class KeyboardShortcutsTestCaseWithToolbar {
    @Rule
    public final ActivityTestRule<ToolbarAppCompatActivity> mActivityTestRule =
            new ActivityTestRule<>(ToolbarAppCompatActivity.class);

    @Test
    @SmallTest
    public void testAccessActionBar() throws Throwable {
        // Since O, we rely on keyboard navigation clusters for jumping to actionbar
        if (Build.VERSION.SDK_INT <= 25) {
            return;
        }
        final BaseTestActivity activity = mActivityTestRule.getActivity();

        final View editText = activity.findViewById(androidx.appcompat.test.R.id.editText);
        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                editText.requestFocus();
            }
        });

        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
        sendMetaKey(KeyEvent.KEYCODE_TAB);
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();

        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                assertFalse(editText.hasFocus());
                final View toolbar = activity.findViewById(androidx.appcompat.test.R.id.toolbar);
                assertTrue(toolbar.hasFocus());
            }
        });
        // We rely on keyboard navigation clusters for jumping out of actionbar since normal
        // navigation won't leaves it.
        sendMetaKey(KeyEvent.KEYCODE_TAB);

        InstrumentationRegistry.getInstrumentation().waitForIdleSync();

        // Should jump to the first view again.
        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                assertTrue(editText.hasFocus());
            }
        });
    }

    @Test
    @SmallTest
    public void testKeyShortcuts() throws Throwable {
        final ToolbarAppCompatActivity activity = mActivityTestRule.getActivity();

        final Toolbar toolbar =
                activity.findViewById(androidx.appcompat.test.R.id.toolbar);

        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                toolbar.inflateMenu(androidx.appcompat.test.R.menu.sample_actions);
            }
        });

        final Boolean[] shareItemClicked = new Boolean[]{false};
        toolbar.getMenu().findItem(androidx.appcompat.test.R.id.action_alpha_shortcut)
                .setOnMenuItemClickListener(
                new MenuItem.OnMenuItemClickListener() {
                        @Override
                        public boolean onMenuItemClick(MenuItem item) {
                            return shareItemClicked[0] = true;
                        }
                    });

        final Window.Callback cb = activity.getWindow().getCallback();

        // Make sure valid menu shortcuts get handled by toolbar menu
        long now = SystemClock.uptimeMillis();
        final KeyEvent handledShortcutKey = new KeyEvent(now, now, KeyEvent.ACTION_DOWN,
                KeyEvent.KEYCODE_A, 0, KeyEvent.META_CTRL_ON);
        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                assertTrue(cb.dispatchKeyShortcutEvent(handledShortcutKey));
            }
        });
        assertTrue(shareItemClicked[0]);

        final KeyEvent unhandledShortcutKey = new KeyEvent(now, now, KeyEvent.ACTION_DOWN,
                KeyEvent.KEYCODE_D, 0, KeyEvent.META_CTRL_ON);

        // Make sure we aren't eating unused shortcuts.
        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                assertFalse(cb.dispatchKeyShortcutEvent(unhandledShortcutKey));
            }
        });

        activity.resetCounters();

        // Make sure that unhandled shortcuts don't prepare menus (since toolbar is handling that).
        InstrumentationRegistry.getInstrumentation().sendKeySync(unhandledShortcutKey);
        assertEquals(1, activity.mKeyShortcutCount);
        assertEquals(0, activity.mPrepareMenuCount);
        assertEquals(0, activity.mCreateMenuCount);
    }

    private void sendMetaKey(int keyCode) throws Throwable {
        long time = SystemClock.uptimeMillis();
        KeyEvent keyDown = new KeyEvent(time, time, KeyEvent.ACTION_DOWN, keyCode,
                0, KeyEvent.META_META_ON);
        InstrumentationRegistry.getInstrumentation().sendKeySync(keyDown);
        time = SystemClock.uptimeMillis();
        KeyEvent keyUp = new KeyEvent(time, time, KeyEvent.ACTION_UP, keyCode,
                0, KeyEvent.META_META_ON);
        InstrumentationRegistry.getInstrumentation().sendKeySync(keyUp);
    }
}
