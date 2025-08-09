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

package androidx.core.google.shortcuts.utils;

import static androidx.core.google.shortcuts.utils.ShortcutUtils.SHORTCUT_LISTENER_INTENT_FILTER_ACTION;
import static androidx.core.google.shortcuts.utils.ShortcutUtils.SHORTCUT_TAG_KEY;
import static androidx.core.google.shortcuts.utils.ShortcutUtils.SHORTCUT_URL_KEY;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.Intent;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;

import com.google.crypto.tink.KeysetHandle;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class ShortcutUtilsTest {
    private static final String TEST_PACKAGE = "com.test.package";

    @Test
    @SmallTest
    public void testGetIndexableUrl_returnsTrampolineActivityIntentUriWithIdExtra() {
        Context context = mock(Context.class);
        when(context.getPackageName()).thenReturn(TEST_PACKAGE);

        String id = "intentId";

        String url = ShortcutUtils.getIndexableUrl(context, id);

        String expectedUri = String.format("intent:#Intent;action=%s;component=%s/androidx.core"
                        + ".google.shortcuts.TrampolineActivity;S.id=%s;end",
                SHORTCUT_LISTENER_INTENT_FILTER_ACTION, TEST_PACKAGE, id);
        assertThat(url).isEqualTo(expectedUri);
    }

    @Test
    @SmallTest
    public void testGetIndexableShortcutUrl_noKeysetHandle_returnsShortcutUrl() throws Exception {
        Context context = mock(Context.class);
        Intent intent = Intent.parseUri("http://www.google.com", 0);

        String shortcutUrl = ShortcutUtils.getIndexableShortcutUrl(context, intent, null);
        String expectedShortcutUrl = "intent://www.google.com#Intent;scheme=http;end";

        assertThat(shortcutUrl).isEqualTo(expectedShortcutUrl);
    }

    @Test
    @SmallTest
    public void testGetIndexableShortcutUrl_withKeyset_returnsTrampolineIntent() throws Exception {
        Context context = ApplicationProvider.getApplicationContext();
        Intent intent = Intent.parseUri("http://www.google.com", 0);

        KeysetHandle keysetHandle = ShortcutUtils.getOrCreateShortcutKeysetHandle(context);
        assertThat(keysetHandle).isNotNull();

        String trampolineUrl = ShortcutUtils.getIndexableShortcutUrl(context, intent, keysetHandle);
        Intent trampolineIntent = Intent.parseUri(trampolineUrl, Intent.URI_INTENT_SCHEME);

        assertThat(trampolineIntent.getPackage()).isEqualTo(context.getPackageName());
        assertThat(trampolineIntent.getAction()).isEqualTo(SHORTCUT_LISTENER_INTENT_FILTER_ACTION);
        assertThat(trampolineIntent.getStringExtra(SHORTCUT_TAG_KEY)).isNotEmpty();
        String expectedShortcutUrl = "intent://www.google.com#Intent;scheme=http;end";

        assertThat(trampolineIntent.getStringExtra(SHORTCUT_URL_KEY))
                .isEqualTo(expectedShortcutUrl);
    }

    @Test
    @SmallTest
    public void testIsAppActionCapability_returnsTrue() {
        assertThat(ShortcutUtils.isAppActionCapability("actions.intent.ORDER_MENU_ITEM")).isTrue();
    }

    @Test
    @SmallTest
    public void testIsAppActionCapability_returnsFalse() {
        assertThat(ShortcutUtils.isAppActionCapability("ORDER_MENU_ITEM")).isFalse();
    }

    @Test
    @SmallTest
    public void testGetOrCreateShortcutKeysetHandle_returnsKeysetHandle() {
        assertThat(ShortcutUtils.getOrCreateShortcutKeysetHandle(
                ApplicationProvider.getApplicationContext())).isNotNull();
    }

    @Test
    @SmallTest
    public void testGetOrCreateShortcutKeysetHandle_multipleCalls_returnsSameKeyset() {
        KeysetHandle keyset1 = ShortcutUtils.getOrCreateShortcutKeysetHandle(
                ApplicationProvider.getApplicationContext());
        KeysetHandle keyset2 = ShortcutUtils.getOrCreateShortcutKeysetHandle(
                ApplicationProvider.getApplicationContext());

        assertThat(keyset1).isNotNull();
        assertThat(keyset2).isNotNull();
        assertThat(keyset1.toString()).isEqualTo(keyset2.toString());
    }
}
