/*
 * Copyright (C) 2017 The Android Open Source Project
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

import static androidx.core.graphics.drawable.IconCompatTest.verifyBadgeBitmap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

import android.content.ComponentName;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ShortcutInfo;

import androidx.core.app.Person;
import androidx.core.app.TestActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.IconCompat;
import androidx.core.test.R;
import androidx.test.InstrumentationRegistry;
import androidx.test.filters.SdkSuppress;
import androidx.test.filters.SmallTest;
import androidx.test.runner.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.HashSet;
import java.util.Set;

@SmallTest
@RunWith(AndroidJUnit4.class)
public class ShortcutInfoCompatTest {

    private static final String TEST_SHORTCUT_ID = "test-shortcut";
    private static final String TEST_SHORTCUT_SHORT_LABEL = "Test shortcut label";

    private Intent mAction;

    private Context mContext;
    private ShortcutInfoCompat.Builder mBuilder;

    @Before
    public void setup() {
        mContext = spy(new ContextWrapper(InstrumentationRegistry.getContext()));
        mAction = new Intent(Intent.ACTION_VIEW).setPackage(mContext.getPackageName());

        mBuilder = new ShortcutInfoCompat.Builder(mContext, TEST_SHORTCUT_ID)
                .setIntent(mAction)
                .setShortLabel(TEST_SHORTCUT_SHORT_LABEL)
                .setIcon(IconCompat.createWithResource(mContext, R.drawable.test_drawable_red));
    }

    @Test
    public void testAddToIntent_noBadge() {
        Intent intent = new Intent();
        mBuilder.setActivity(new ComponentName(mContext, TestActivity.class))
                .build()
                .addToIntent(intent);

        assertEquals(mAction, intent.getParcelableExtra(Intent.EXTRA_SHORTCUT_INTENT));
        assertNotNull(intent.getParcelableExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE));
        assertNull(intent.getParcelableExtra(Intent.EXTRA_SHORTCUT_ICON));
    }

    @Test
    public void testAddToIntent_badgeActivity() {
        Intent intent = new Intent();
        mBuilder.setActivity(new ComponentName(mContext, TestActivity.class))
                .setAlwaysBadged()
                .build()
                .addToIntent(intent);

        assertEquals(mAction, intent.getParcelableExtra(Intent.EXTRA_SHORTCUT_INTENT));
        assertNull(intent.getParcelableExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE));

        verifyBadgeBitmap(intent, ContextCompat.getColor(mContext, R.color.test_red),
                ContextCompat.getColor(mContext, R.color.test_blue));
    }

    @Test
    public void testAddToIntent_badgeApplication() {
        ApplicationInfo appInfo = spy(mContext.getApplicationInfo());
        doReturn(ContextCompat.getDrawable(mContext, R.drawable.test_drawable_green))
                .when(appInfo).loadIcon(any(PackageManager.class));
        doReturn(appInfo).when(mContext).getApplicationInfo();

        Intent intent = new Intent();
        mBuilder.setAlwaysBadged()
                .build()
                .addToIntent(intent);

        assertEquals(mAction, intent.getParcelableExtra(Intent.EXTRA_SHORTCUT_INTENT));
        assertNull(intent.getParcelableExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE));

        verifyBadgeBitmap(intent, ContextCompat.getColor(mContext, R.color.test_red),
                ContextCompat.getColor(mContext, R.color.test_green));
    }

    @Test
    public void testBuilder_defaultValues() {
        ShortcutInfoCompat compat = mBuilder.build();
        assertEquals(TEST_SHORTCUT_ID, compat.getId());
        assertEquals(TEST_SHORTCUT_SHORT_LABEL, compat.getShortLabel());
        assertEquals(mAction, compat.getIntent());
        assertNull(compat.getLongLabel());
        assertNull(compat.getDisabledMessage());
        assertNull(compat.getActivity());
        assertNull(compat.getCategories());
    }

    @Test
    public void testBuilder_copyConstructor() {
        String longLabel = "Test long label";
        ComponentName activity = new ComponentName("Package name", "Class name");
        String disabledMessage = "Test disabled message";
        Set<String> categories = new HashSet<>();
        categories.add("cat1");
        categories.add("cat2");
        ShortcutInfoCompat compat = mBuilder
                .setActivity(activity)
                .setCategories(categories)
                .setDisabledMessage(disabledMessage)
                .setLongLabel(longLabel)
                .build();

        ShortcutInfoCompat copyCompat = new ShortcutInfoCompat.Builder(compat).build();
        assertEquals(TEST_SHORTCUT_ID, copyCompat.getId());
        assertEquals(TEST_SHORTCUT_SHORT_LABEL, copyCompat.getShortLabel());
        assertEquals(mAction, copyCompat.getIntent());
        assertEquals(longLabel, copyCompat.getLongLabel());
        assertEquals(disabledMessage, copyCompat.getDisabledMessage());
        assertEquals(activity, copyCompat.getActivity());
        assertEquals(categories, copyCompat.getCategories());
    }

    @Test
    @SdkSuppress(minSdkVersion = 26)
    public void testBuilder_fromShortcutInfo() {
        String longLabel = "Test long label";
        ComponentName activity = new ComponentName("Package name", "Class name");
        String disabledMessage = "Test disabled message";
        Set<String> categories = new HashSet<>();
        categories.add("cat1");
        categories.add("cat2");

        ShortcutInfo.Builder builder = new ShortcutInfo.Builder(mContext, TEST_SHORTCUT_ID);
        ShortcutInfo shortcut = builder.setIntent(mAction)
                .setShortLabel(TEST_SHORTCUT_SHORT_LABEL)
                .setActivity(activity)
                .setCategories(categories)
                .setDisabledMessage(disabledMessage)
                .setLongLabel(longLabel)
                .build();

        ShortcutInfoCompat compat = new ShortcutInfoCompat.Builder(mContext, shortcut).build();
        assertEquals(TEST_SHORTCUT_ID, compat.getId());
        assertEquals(TEST_SHORTCUT_SHORT_LABEL, compat.getShortLabel());
        assertEquals(mAction.getPackage(), compat.getIntent().getPackage());
        assertEquals(mAction.getAction(), compat.getIntent().getAction());
        assertEquals(longLabel, compat.getLongLabel());
        assertEquals(disabledMessage, compat.getDisabledMessage());
        assertEquals(activity, compat.getActivity());
        assertEquals(categories, compat.getCategories());
    }

    @Test
    public void testBuilder_getters() {
        String longLabel = "Test long label";
        ComponentName activity = new ComponentName("Package name", "Class name");
        String disabledMessage = "Test disabled message";
        Set<String> categories = new HashSet<>();
        categories.add("cat1");
        categories.add("cat2");

        ShortcutInfoCompat compat = mBuilder
                .setActivity(activity)
                .setCategories(categories)
                .setDisabledMessage(disabledMessage)
                .setLongLabel(longLabel)
                .build();
        assertEquals(TEST_SHORTCUT_ID, compat.getId());
        assertEquals(TEST_SHORTCUT_SHORT_LABEL, compat.getShortLabel());
        assertEquals(mAction, compat.getIntent());
        assertEquals(longLabel, compat.getLongLabel());
        assertEquals(disabledMessage, compat.getDisabledMessage());
        assertEquals(activity, compat.getActivity());
        assertEquals(categories, compat.getCategories());
    }

    @Test
    @SdkSuppress(minSdkVersion = 26)
    public void testToShortcutInfo_extrasBundle() {
        String longLabel = "Test long label";
        ComponentName activity = new ComponentName("Package name", "Class name");
        String disabledMessage = "Test disabled message";
        Set<String> categories = new HashSet<>();
        categories.add("cat1");
        categories.add("cat2");
        Person[] persons = {
                new Person.Builder().setName("P1").build(),
                new Person.Builder().setName("P2").build()};

        ShortcutInfoCompat compat = mBuilder
                .setActivity(activity)
                .setCategories(categories)
                .setDisabledMessage(disabledMessage)
                .setLongLabel(longLabel)
                .setPersons(persons)
                .setLongLived()
                .build();

        ShortcutInfo shortcut = compat.toShortcutInfo();

        assertEquals(TEST_SHORTCUT_ID, shortcut.getId());
        assertEquals(TEST_SHORTCUT_SHORT_LABEL, shortcut.getShortLabel());
        assertEquals(mAction.getAction(), shortcut.getIntent().getAction());
        assertEquals(mAction.getPackage(), shortcut.getIntent().getPackage());
        assertEquals(longLabel, shortcut.getLongLabel());
        assertEquals(disabledMessage, shortcut.getDisabledMessage());
        assertEquals(activity, shortcut.getActivity());
        assertEquals(categories, shortcut.getCategories());

        assertNotNull(shortcut.getExtras());
        assertTrue(ShortcutInfoCompat.getLongLivedFromExtra(shortcut.getExtras()));
        Person[] retrievedPersons = ShortcutInfoCompat.getPersonsFromExtra(shortcut.getExtras());
        assertNotNull(retrievedPersons);
        assertEquals(persons.length, retrievedPersons.length);
        for (int i = 0; i < persons.length; i++) {
            assertEquals(persons[i].getName(), retrievedPersons[i].getName());
        }
    }
}
