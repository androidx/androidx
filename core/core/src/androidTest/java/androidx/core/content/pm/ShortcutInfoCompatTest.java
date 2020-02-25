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
import static org.junit.Assert.assertFalse;
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
import android.os.Build;
import android.os.PersistableBundle;

import androidx.core.app.Person;
import androidx.core.app.TestActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.IconCompat;
import androidx.core.test.R;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.MediumTest;
import androidx.test.filters.SdkSuppress;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;

@MediumTest
@RunWith(AndroidJUnit4.class)
public class ShortcutInfoCompatTest {

    private static final String TEST_SHORTCUT_ID = "test-shortcut";
    private static final String TEST_SHORTCUT_SHORT_LABEL = "Test shortcut label";
    private static final String TEST_EXTRAS_ID = "test-extras-id";
    private static final String TEST_EXTRAS_VALUE = "test-extras-id-value";

    private static final int FLAG_DYNAMIC = 1 << 0;
    private static final int FLAG_PINNED = 1 << 1;
    private static final int FLAG_KEY_FIELDS_ONLY = 1 << 4;
    private static final int FLAG_MANIFEST = 1 << 5;
    private static final int FLAG_DISABLED = 1 << 6;
    private static final int FLAG_IMMUTABLE = 1 << 8;
    private static final int FLAG_CACHED = 1 << 14;

    private Intent mAction;

    private Context mContext;
    private ShortcutInfoCompat.Builder mBuilder;

    @Before
    public void setup() {
        mContext = spy(new ContextWrapper(ApplicationProvider.getApplicationContext()));
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
        assertEquals(0, compat.getRank());
        assertNull(compat.getLongLabel());
        assertNull(compat.getDisabledMessage());
        assertNull(compat.getActivity());
        assertNull(compat.getCategories());
        assertNull(compat.getExtras());
    }

    @Test
    public void testBuilder_copyConstructor() {
        String longLabel = "Test long label";
        ComponentName activity = new ComponentName("Package name", "Class name");
        String disabledMessage = "Test disabled message";
        Set<String> categories = new HashSet<>();
        categories.add("cat1");
        categories.add("cat2");
        int rank = 3;
        PersistableBundle persistableBundle = new PersistableBundle();
        ShortcutInfoCompat compat = mBuilder
                .setActivity(activity)
                .setCategories(categories)
                .setDisabledMessage(disabledMessage)
                .setLongLabel(longLabel)
                .setRank(rank)
                .setExtras(persistableBundle)
                .build();

        ShortcutInfoCompat copyCompat = new ShortcutInfoCompat.Builder(compat).build();
        assertEquals(TEST_SHORTCUT_ID, copyCompat.getId());
        assertEquals(TEST_SHORTCUT_SHORT_LABEL, copyCompat.getShortLabel());
        assertEquals(mAction, copyCompat.getIntent());
        assertEquals(longLabel, copyCompat.getLongLabel());
        assertEquals(disabledMessage, copyCompat.getDisabledMessage());
        assertEquals(activity, copyCompat.getActivity());
        assertEquals(categories, copyCompat.getCategories());
        assertEquals(rank, copyCompat.getRank());
        assertEquals(persistableBundle, copyCompat.getExtras());
    }

    @Test
    @SdkSuppress(minSdkVersion = 26)
    public void testBuilder_fromShortcutInfo() throws Exception {
        final long ts = System.currentTimeMillis();
        String longLabel = "Test long label";
        ComponentName activity = new ComponentName("Package name", "Class name");
        String disabledMessage = "Test disabled message";
        Set<String> categories = new HashSet<>();
        categories.add("cat1");
        categories.add("cat2");
        int rank = 3;
        PersistableBundle persistableBundle = new PersistableBundle();
        ShortcutInfo.Builder builder = new ShortcutInfo.Builder(mContext, TEST_SHORTCUT_ID);
        ShortcutInfo shortcut = builder.setIntent(mAction)
                .setShortLabel(TEST_SHORTCUT_SHORT_LABEL)
                .setActivity(activity)
                .setCategories(categories)
                .setDisabledMessage(disabledMessage)
                .setLongLabel(longLabel)
                .setRank(rank)
                .setExtras(persistableBundle)
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
        assertEquals(rank, compat.getRank());
        assertEquals(persistableBundle, compat.getExtras());
        assertEquals(ShortcutInfo.DISABLED_REASON_NOT_DISABLED, compat.getDisabledReason());
        assertTrue(compat.getLastChangedTimestamp() > ts);
        assertNotNull(compat.getUserHandle());
        assertNotNull(compat.getPackage());
        assertFalse(compat.isCached());
        assertFalse(compat.isDeclaredInManifest());
        assertFalse(compat.isDynamic());
        assertTrue(compat.isEnabled());
        assertFalse(compat.isImmutable());
        assertFalse(compat.isPinned());
        assertFalse(compat.hasKeyFieldsOnly());

        if (Build.VERSION.SDK_INT >= 28) {
            final Method setDisabledReason = ShortcutInfo.class.getDeclaredMethod(
                    "setDisabledReason", int.class);
            setDisabledReason.setAccessible(true);
            setDisabledReason.invoke(shortcut, ShortcutInfo.DISABLED_REASON_BY_APP);
        }

        final int flag = FLAG_PINNED | FLAG_DYNAMIC | FLAG_MANIFEST | FLAG_IMMUTABLE | FLAG_DISABLED
                | FLAG_CACHED | FLAG_KEY_FIELDS_ONLY;
        final Method replaceFlags = ShortcutInfo.class.getDeclaredMethod("replaceFlags", int.class);
        replaceFlags.setAccessible(true);
        replaceFlags.invoke(shortcut, flag);

        compat = new ShortcutInfoCompat.Builder(mContext, shortcut).build();
        assertEquals(Build.VERSION.SDK_INT >= 28 ? ShortcutInfo.DISABLED_REASON_BY_APP :
                ShortcutInfo.DISABLED_REASON_UNKNOWN, compat.getDisabledReason());
        if (Build.VERSION.SDK_INT >= 30) {
            assertTrue(compat.isCached());
        }
        assertTrue(compat.isDeclaredInManifest());
        assertTrue(compat.isDynamic());
        assertFalse(compat.isEnabled());
        assertTrue(compat.isImmutable());
        assertTrue(compat.isPinned());
        assertTrue(compat.hasKeyFieldsOnly());
    }

    @Test
    public void testBuilder_getters() {
        String longLabel = "Test long label";
        ComponentName activity = new ComponentName("Package name", "Class name");
        String disabledMessage = "Test disabled message";
        Set<String> categories = new HashSet<>();
        categories.add("cat1");
        categories.add("cat2");
        int rank = 3;
        PersistableBundle persistableBundle = new PersistableBundle();
        ShortcutInfoCompat compat = mBuilder
                .setActivity(activity)
                .setCategories(categories)
                .setDisabledMessage(disabledMessage)
                .setLongLabel(longLabel)
                .setRank(3)
                .setExtras(persistableBundle)
                .build();
        assertEquals(TEST_SHORTCUT_ID, compat.getId());
        assertEquals(TEST_SHORTCUT_SHORT_LABEL, compat.getShortLabel());
        assertEquals(mAction, compat.getIntent());
        assertEquals(longLabel, compat.getLongLabel());
        assertEquals(disabledMessage, compat.getDisabledMessage());
        assertEquals(activity, compat.getActivity());
        assertEquals(categories, compat.getCategories());
        assertEquals(rank, compat.getRank());
        assertEquals(persistableBundle, compat.getExtras());
    }

    @Test
    @SdkSuppress(minSdkVersion = 26)
    public void testToShortcutInfo() {
        String longLabel = "Test long label";
        ComponentName activity = new ComponentName("Package name", "Class name");
        String disabledMessage = "Test disabled message";
        Set<String> categories = new HashSet<>();
        categories.add("cat1");
        categories.add("cat2");
        int rank = 3;
        ShortcutInfoCompat compat = mBuilder
                .setActivity(activity)
                .setCategories(categories)
                .setDisabledMessage(disabledMessage)
                .setLongLabel(longLabel)
                .setRank(3)
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
        assertEquals(rank, shortcut.getRank());
    }

    @Test
    @SdkSuppress(minSdkVersion = 26, maxSdkVersion = 28)
    public void testToShortcutInfo_extrasBundle() {
        Person[] persons = {
                new Person.Builder().setName("P1").build(),
                new Person.Builder().setName("P2").build()};

        PersistableBundle persistableBundle = new PersistableBundle();
        persistableBundle.putString(TEST_EXTRAS_ID, TEST_EXTRAS_VALUE);

        ShortcutInfoCompat compat = mBuilder
                .setPersons(persons)
                .setLongLived(true)
                .setExtras(persistableBundle)
                .build();

        ShortcutInfo shortcut = compat.toShortcutInfo();

        assertNotNull(shortcut.getExtras());
        assertTrue(ShortcutInfoCompat.getLongLivedFromExtra(shortcut.getExtras()));
        assertEquals(compat.getExtras().getString(TEST_EXTRAS_ID), TEST_EXTRAS_VALUE);
        Person[] retrievedPersons = ShortcutInfoCompat.getPersonsFromExtra(shortcut.getExtras());
        assertNotNull(retrievedPersons);
        assertEquals(persons.length, retrievedPersons.length);
        for (int i = 0; i < persons.length; i++) {
            assertEquals(persons[i].getName(), retrievedPersons[i].getName());
        }
    }
}
