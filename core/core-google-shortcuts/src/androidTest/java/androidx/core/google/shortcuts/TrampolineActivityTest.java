/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.core.google.shortcuts;

import static androidx.core.google.shortcuts.utils.ShortcutUtils.SHORTCUT_TAG_KEY;
import static androidx.core.google.shortcuts.utils.ShortcutUtils.SHORTCUT_URL_KEY;
import static androidx.test.espresso.intent.Intents.intended;
import static androidx.test.espresso.intent.Intents.times;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent;

import static com.google.common.truth.Truth.assertThat;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;

import androidx.core.google.shortcuts.test.TestActivity;
import androidx.core.google.shortcuts.utils.ShortcutUtils;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.espresso.intent.Intents;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;

import com.google.crypto.tink.KeysetHandle;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;

@RunWith(AndroidJUnit4.class)
public class TrampolineActivityTest {
    private static final String SHORTCUT_LISTENER_INTENT_FILTER_ACTION = "androidx.core.content.pm"
            + ".SHORTCUT_LISTENER";
    private static final String SHORTCUT_LISTENER_META_DATA_KEY = "androidx.core.content.pm"
            + ".shortcut_listener_impl";
    private static final String SHORTCUT_LISTENER_CLASS_NAME = "androidx.core.google.shortcuts"
            + ".ShortcutInfoChangeListenerImpl";

    private Context mContext;

    @Before
    public void setUp() {
        Intents.init();
        mContext = ApplicationProvider.getApplicationContext();
    }

    @After
    public void tearDown() {
        Intents.release();
    }

    @Test
    @SmallTest
    public void testOnCreate_withTrampolineIntent_launchesShortcut() throws Exception {
        Intent trampolineIntent = createIntentToTestActivity();

        ActivityScenario.launchActivityForResult(trampolineIntent);

        // Verify test activity was launched.
        intended(hasComponent(TestActivity.class.getName()));
    }

    @Test
    @SmallTest
    public void testOnCreate_withMismatchTag_exits() throws Exception {
        Intent trampolineIntent = createIntentToTestActivity();
        trampolineIntent.putExtra(SHORTCUT_TAG_KEY, "bad_tag");

        ActivityScenario<Activity> scenario =
                ActivityScenario.launchActivityForResult(trampolineIntent);

        // Verify test activity was not launched.
        intended(hasComponent(TestActivity.class.getName()), times(0));
        // Verify trampoline activity is finished.
        assertThat(scenario.getResult().getResultCode()).isEqualTo(Activity.RESULT_CANCELED);
    }

    @Test
    @SmallTest
    public void testOnCreate_withNoTag_exits() throws Exception {
        Intent trampolineIntent = createIntentToTestActivity();
        trampolineIntent.removeExtra(SHORTCUT_TAG_KEY);

        ActivityScenario<Activity> scenario =
                ActivityScenario.launchActivityForResult(trampolineIntent);

        // Verify test activity was not launched.
        intended(hasComponent(TestActivity.class.getName()), times(0));
        // Verify trampoline activity is finished.
        assertThat(scenario.getResult().getResultCode()).isEqualTo(Activity.RESULT_CANCELED);
    }

    @Test
    @SmallTest
    public void testOnCreate_withNoShortcutUrl_exits() throws Exception {
        Intent trampolineIntent = createIntentToTestActivity();
        trampolineIntent.removeExtra(SHORTCUT_URL_KEY);

        ActivityScenario<Activity> scenario =
                ActivityScenario.launchActivityForResult(trampolineIntent);

        // Verify test activity was not launched.
        intended(hasComponent(TestActivity.class.getName()), times(0));
        // Verify trampoline activity is finished.
        assertThat(scenario.getResult().getResultCode()).isEqualTo(Activity.RESULT_CANCELED);
    }

    @SuppressWarnings("deprecation") // usage of PackageManager.queryIntentActivities
    @Test
    @SmallTest
    public void testManifest_canDiscoverMetadata() {
        PackageManager packageManager = mContext.getPackageManager();
        Intent activityIntent = new Intent(SHORTCUT_LISTENER_INTENT_FILTER_ACTION);
        activityIntent.setPackage(mContext.getPackageName());

        List<ResolveInfo> resolveInfos = packageManager.queryIntentActivities(
                activityIntent, PackageManager.GET_META_DATA);

        boolean hasMetadata = false;
        for (ResolveInfo resolveInfo : resolveInfos) {
            if (SHORTCUT_LISTENER_CLASS_NAME.equals(
                    resolveInfo.activityInfo.metaData.getString(SHORTCUT_LISTENER_META_DATA_KEY))) {
                hasMetadata = true;
            }
        }
        assertThat(hasMetadata).isTrue();
    }

    private Intent createIntentToTestActivity() throws Exception {
        KeysetHandle keysetHandle = ShortcutUtils.getOrCreateShortcutKeysetHandle(mContext);
        assertThat(keysetHandle).isNotNull();

        Intent shortcutIntent = new Intent(mContext, TestActivity.class);
        String trampolineIntentString = ShortcutUtils.getIndexableShortcutUrl(mContext,
                shortcutIntent, keysetHandle);
        return Intent.parseUri(trampolineIntentString, 0);
    }
}
