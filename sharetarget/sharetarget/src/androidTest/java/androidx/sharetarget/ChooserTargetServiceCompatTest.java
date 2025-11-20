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

package androidx.sharetarget;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.content.ComponentName;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;

import androidx.core.content.pm.ShortcutInfoCompat;
import androidx.core.content.pm.ShortcutManagerCompat;
import androidx.core.graphics.drawable.IconCompat;
import androidx.sharetarget.ChooserTargetServiceCompat.ShortcutHolder;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.MediumTest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.List;

@MediumTest
@RunWith(AndroidJUnit4.class)
public class ChooserTargetServiceCompatTest {
    private Context mContext;

    private IconCompat mTestIcon;
    private Intent mTestIntent;
    private ShortcutInfoCompatSaverImpl mShortcutSaver;

    @Before
    public void setup() throws Exception {
        mContext = spy(new ContextWrapper(ApplicationProvider.getApplicationContext()));
        mTestIntent = new Intent("TestIntent");
        mTestIcon = IconCompat.createWithResource(mContext,
                androidx.sharetarget.test.R.drawable.bmp_test);
        mShortcutSaver = mock(ShortcutInfoCompatSaverImpl.class);
        when(mShortcutSaver.getShortcutIcon(any())).thenReturn(mTestIcon);
    }

    @SuppressWarnings({"deprecation", "unchecked"})
    @Test
    public void testConvertShortcutstoChooserTargets() {
        ArrayList<ShortcutHolder> testShortcuts = new ArrayList<>();
        testShortcuts.add(new ShortcutHolder(
                new ShortcutInfoCompat.Builder(mContext, "shortcut1")
                        .setIntent(mTestIntent).setShortLabel("label1").setRank(3).build(),
                new ComponentName("package1", "class1")));
        testShortcuts.add(new ShortcutHolder(
                new ShortcutInfoCompat.Builder(mContext, "shortcut2")
                        .setIntent(mTestIntent).setShortLabel("label2").setRank(7).build(),
                new ComponentName("package2", "class2")));
        testShortcuts.add(new ShortcutHolder(
                new ShortcutInfoCompat.Builder(mContext, "shortcut3")
                        .setIntent(mTestIntent).setShortLabel("label3").setRank(1).build(),
                new ComponentName("package3", "class3")));
        testShortcuts.add(new ShortcutHolder(
                new ShortcutInfoCompat.Builder(mContext, "shortcut4")
                        .setIntent(mTestIntent).setShortLabel("label4").setRank(3).build(),
                new ComponentName("package4", "class4")));

        // Need to clone to keep the original order for testing.
        ArrayList<ShortcutHolder> clonedList = (ArrayList<ShortcutHolder>) testShortcuts.clone();
        List<android.service.chooser.ChooserTarget> chooserTargets =
                ChooserTargetServiceCompat.convertShortcutsToChooserTargets(
                        mShortcutSaver, clonedList);

        int[] expectedOrder = {2, 0, 3, 1};
        float[] expectedScores = {1.0f, 1.0f - 0.01f, 1.0f - 0.01f, 1.0f - 0.02f};

        assertEquals(testShortcuts.size(), chooserTargets.size());
        for (int i = 0; i < chooserTargets.size(); i++) {
            android.service.chooser.ChooserTarget ct = chooserTargets.get(i);
            ShortcutInfoCompat si = testShortcuts.get(expectedOrder[i]).getShortcut();
            ComponentName cn = testShortcuts.get(expectedOrder[i]).getTargetClass();

            assertEquals(si.getId(), ct.getIntentExtras().getString(
                    ShortcutManagerCompat.EXTRA_SHORTCUT_ID));
            assertEquals(si.getShortLabel(), ct.getTitle());
            assertTrue(Math.abs(expectedScores[i] - ct.getScore()) < 0.000001);
            assertEquals(cn.flattenToString(), ct.getComponentName().flattenToString());
        }
    }
}
