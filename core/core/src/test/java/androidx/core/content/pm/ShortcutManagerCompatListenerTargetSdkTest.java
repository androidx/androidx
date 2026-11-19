/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.core.content.pm;

import static com.google.common.truth.Truth.assertThat;

import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.test.core.app.ApplicationProvider;

import org.jspecify.annotations.NonNull;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Unit tests for {@code targetSdkVersion} gating of {@link ShortcutInfoChangeListener} in {@link
 * ShortcutManagerCompat}.
 */
@SuppressWarnings("deprecation")
@RunWith(RobolectricTestRunner.class)
@Config(sdk = {Config.TARGET_SDK})
public class ShortcutManagerCompatListenerTargetSdkTest {

    private Context mContext;
    private ShortcutInfoCompat mShortcut;
    private RecordingShortcutInfoChangeListener mListener;
    private int mOriginalTargetSdkVersion;

    @Before
    public void setUp() {
        mContext = ApplicationProvider.getApplicationContext();
        mOriginalTargetSdkVersion = mContext.getApplicationInfo().targetSdkVersion;
        mShortcut =
                new ShortcutInfoCompat.Builder(mContext, "shortcut-1")
                        .setShortLabel("Test shortcut")
                        .setIntent(new Intent(Intent.ACTION_VIEW))
                        .build();
        mListener = new RecordingShortcutInfoChangeListener();
        ShortcutManagerCompat.setShortcutInfoChangeListeners(
                Collections.singletonList(mListener));
    }

    @After
    public void tearDown() {
        mContext.getApplicationInfo().targetSdkVersion = mOriginalTargetSdkVersion;
        ShortcutManagerCompat.setShortcutInfoChangeListeners(null);
    }

    @Test
    public void shortcutMutations_targetSdkCinnamonBun_invokesListenerCallbacks() {
        mContext.getApplicationInfo().targetSdkVersion = Build.VERSION_CODES.CINNAMON_BUN;
        List<ShortcutInfoCompat> shortcuts = Collections.singletonList(mShortcut);
        List<String> ids = Collections.singletonList("shortcut-1");

        ShortcutManagerCompat.addDynamicShortcuts(mContext, shortcuts);
        ShortcutManagerCompat.updateShortcuts(mContext, shortcuts);
        ShortcutManagerCompat.reportShortcutUsed(mContext, "shortcut-1");
        ShortcutManagerCompat.removeDynamicShortcuts(mContext, ids);
        ShortcutManagerCompat.removeAllDynamicShortcuts(mContext);

        assertThat(mListener.mAddedShortcuts).containsExactly(mShortcut);
        assertThat(mListener.mUpdatedShortcuts).containsExactly(mShortcut);
        assertThat(mListener.mReportedIds).containsExactly("shortcut-1");
        assertThat(mListener.mRemovedIds).containsExactly("shortcut-1");
        assertThat(mListener.mAllRemovedCount).isEqualTo(1);
    }

    @Test
    public void shortcutMutations_targetSdkAfterCinnamonBun_doesNotInvokeListenerCallbacks() {
        mContext.getApplicationInfo().targetSdkVersion = Build.VERSION_CODES.CINNAMON_BUN + 1;
        List<ShortcutInfoCompat> shortcuts = Collections.singletonList(mShortcut);
        List<String> ids = Collections.singletonList("shortcut-1");

        ShortcutManagerCompat.addDynamicShortcuts(mContext, shortcuts);
        ShortcutManagerCompat.updateShortcuts(mContext, shortcuts);
        ShortcutManagerCompat.setDynamicShortcuts(mContext, shortcuts);
        ShortcutManagerCompat.enableShortcuts(mContext, shortcuts);
        ShortcutManagerCompat.disableShortcuts(mContext, ids, "disabled");
        ShortcutManagerCompat.pushDynamicShortcut(mContext, mShortcut);
        ShortcutManagerCompat.reportShortcutUsed(mContext, "shortcut-1");
        ShortcutManagerCompat.removeDynamicShortcuts(mContext, ids);
        ShortcutManagerCompat.removeLongLivedShortcuts(mContext, ids);
        ShortcutManagerCompat.removeAllDynamicShortcuts(mContext);

        assertThat(mListener.mAddedShortcuts).isEmpty();
        assertThat(mListener.mUpdatedShortcuts).isEmpty();
        assertThat(mListener.mReportedIds).isEmpty();
        assertThat(mListener.mRemovedIds).isEmpty();
        assertThat(mListener.mAllRemovedCount).isEqualTo(0);
    }

    private static class RecordingShortcutInfoChangeListener extends ShortcutInfoChangeListener {
        final List<ShortcutInfoCompat> mAddedShortcuts = new ArrayList<>();
        final List<ShortcutInfoCompat> mUpdatedShortcuts = new ArrayList<>();
        final List<String> mRemovedIds = new ArrayList<>();
        final List<String> mReportedIds = new ArrayList<>();
        int mAllRemovedCount = 0;

        @Override
        public void onShortcutAdded(@NonNull List<ShortcutInfoCompat> shortcuts) {
            mAddedShortcuts.addAll(shortcuts);
        }

        @Override
        public void onShortcutUpdated(@NonNull List<ShortcutInfoCompat> shortcuts) {
            mUpdatedShortcuts.addAll(shortcuts);
        }

        @Override
        public void onShortcutRemoved(@NonNull List<String> shortcutIds) {
            mRemovedIds.addAll(shortcutIds);
        }

        @Override
        public void onAllShortcutsRemoved() {
            mAllRemovedCount++;
        }

        @Override
        public void onShortcutUsageReported(@NonNull List<String> shortcutIds) {
            mReportedIds.addAll(shortcutIds);
        }
    }
}
