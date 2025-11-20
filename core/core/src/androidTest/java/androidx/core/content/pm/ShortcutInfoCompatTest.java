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
import android.net.Uri;
import android.os.PersistableBundle;

import androidx.core.app.Person;
import androidx.core.app.TestActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.LocusIdCompat;
import androidx.core.graphics.drawable.IconCompat;
import androidx.core.test.R;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.FlakyTest;
import androidx.test.filters.MediumTest;
import androidx.test.filters.SdkSuppress;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
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
    public void testBuilder_setIsConversation() {
        final ShortcutInfoCompat compat = mBuilder.setIsConversation().build();
        final LocusIdCompat locusId = compat.getLocusId();
        assertNotNull(locusId);
        assertEquals(TEST_SHORTCUT_ID, locusId.getId());
        assertTrue(compat.mIsLongLived);
    }

    @Test
    public void testBuilder_setCapabilities() {
        final String capability1 = "START_EXERCISE";
        final String capability1Param1 = "exerciseName";
        final String capability1Param2 = "duration";
        final String capability1Param1Value1 = "jogging;running";
        final String capability1Param1Value2 = "sleeping";
        final String capability2 = "STOP_EXERCISE";
        final String capability2Param1 = "exerciseName";
        final String capability2Param2Value1 = "sleeping";
        final String sliceUri = "slice-content://com.myfitnessapp/exercise{?start,end}";

        /*
         * Setup capability 1
         * {
         *     "START_EXERCISE": {
         *         "exerciseName": ["jogging;running","sleeping"],
         *         "duration": []
         *     }
         * }
         */
        final List<String> capability1Params1Values = new ArrayList<>();
        capability1Params1Values.add(capability1Param1Value1);
        capability1Params1Values.add(capability1Param1Value2);

        /*
         * Setup capability 2
         * {
         *     "STOP_EXERCISE": {
         *         "exerciseName": ["sleeping"],
         *     }
         * }
         */
        final List<String> capability2Param2Values = new ArrayList<>();
        capability2Param2Values.add(capability2Param2Value1);

        final ShortcutInfoCompat compat = mBuilder
                .addCapabilityBinding(capability1, capability1Param1, capability1Params1Values)
                .addCapabilityBinding(capability1, capability1Param2, new ArrayList<String>())
                .addCapabilityBinding(capability2, capability2Param1, capability2Param2Values)
                .setSliceUri(Uri.parse(sliceUri))
                .build();

        /*
         * Verify the extras contains mapping of capability to their parameter names.
         * {
         *     "START_EXERCISE": ["exerciseName","duration"],
         *     "STOP_EXERCISE": ["exerciseName"],
         * }
         */
        final Set<String> categories = compat.mCategories;
        assertNotNull(categories);
        assertTrue(categories.contains(capability1));
        assertTrue(categories.contains(capability2));
        final PersistableBundle extra = compat.getExtras();
        assertNotNull(extra);
        assertTrue(extra.containsKey(capability1));
        assertTrue(extra.containsKey(capability2));
        final String[] paramNamesForCapability1 = extra.getStringArray(capability1);
        final String[] paramNamesForCapability2 = extra.getStringArray(capability2);
        assertNotNull(paramNamesForCapability1);
        assertNotNull(paramNamesForCapability2);
        assertEquals(1, paramNamesForCapability1.length);
        assertEquals(1, paramNamesForCapability2.length);
        final List<String> parameterListForCapability1 = Arrays.asList(paramNamesForCapability1);
        assertTrue(parameterListForCapability1.contains(capability1Param1));
        assertFalse(parameterListForCapability1.contains(capability1Param2));
        assertEquals(capability2Param1, paramNamesForCapability2[0]);

        /*
         * Verify the extras contains mapping of capability params to their values.
         * {
         *     "START_EXERCISE/exerciseName": ["jogging;running","sleeping"],
         *     "START_EXERCISE/duration": [],
         *     "STOP_EXERCISE/exerciseName": ["sleeping"],
         * }
         */
        final String capability1Param1Key = capability1 + "/" + capability1Param1;
        final String capability1Param2Key = capability1 + "/" + capability1Param2;
        final String capability2Param1Key = capability2 + "/" + capability2Param1;
        assertTrue(extra.containsKey(capability1Param1Key));
        assertFalse(extra.containsKey(capability1Param2Key));
        assertTrue(extra.containsKey(capability2Param1Key));
        final String[] actualCapability1Params1 = extra.getStringArray(capability1Param1Key);
        final String[] actualCapability2Params1 = extra.getStringArray(capability2Param1Key);
        assertNotNull(actualCapability1Params1);
        assertEquals(2, actualCapability1Params1.length);
        assertEquals(capability1Param1Value1, actualCapability1Params1[0]);
        assertEquals(capability1Param1Value2, actualCapability1Params1[1]);
        assertNotNull(actualCapability2Params1);
        assertEquals(1, actualCapability2Params1.length);
        assertEquals(capability2Param2Value1, actualCapability2Params1[0]);
        assertTrue(extra.containsKey("extraSliceUri"));
        assertEquals(sliceUri, extra.getString("extraSliceUri"));
    }

    @Test
    public void testBuilder_setCapabilities_noParameters() {
        final String capability = "actions.intent.TWEET";

        final ShortcutInfoCompat compat = mBuilder
                .addCapabilityBinding(capability)
                .build();

        /*
         * Verify the extras contains mapping of capability to their parameter names.
         * {
         *     "actions.intent.TWEET": null
         * }
         */
        final Set<String> categories = compat.mCategories;
        assertNotNull(categories);
        assertTrue(categories.contains(capability));
        final PersistableBundle extra = compat.getExtras();
        assertNull(extra);
    }

    @Test
    public void testBuilder_setCapabilityWithParameters() {
        final String capability = "actions.intent.START_EXERCISE";
        final String capabilityParam1 = "exercise.name";
        final String capabilityParam2 = "duration";
        final String capabilityParam3 = "difficulty";
        final String capabilityParam1Value1 = "running";
        final String capabilityParam1Value2 = "jogging";
        final String capabilityParam2Value1 = "60 minutes";
        final String capabilityParam2Value2 = "1 hour";

        final ShortcutInfoCompat compat = mBuilder
                .addCapabilityBinding(capability, capabilityParam1,
                        Arrays.asList(capabilityParam1Value1, capabilityParam1Value2))
                .addCapabilityBinding(capability, capabilityParam2,
                        Arrays.asList(capabilityParam2Value1, capabilityParam2Value2))
                // This one should be ignored since its values are empty.
                .addCapabilityBinding(capability, capabilityParam3, new ArrayList<String>())
                .build();

        /*
         * Verify the extras contains mapping of capability to their parameter names.
         * {
         *     "actions.intent.START_EXERCISE": ["exercise.name", "duration"],
         * }
         */
        final Set<String> categories = compat.mCategories;
        assertNotNull(categories);
        assertTrue(categories.contains(capability));
        final PersistableBundle extra = compat.getExtras();
        assertNotNull(extra);
        assertTrue(extra.containsKey(capability));
        final String[] paramNamesForCapability = extra.getStringArray(capability);
        assertNotNull(paramNamesForCapability);
        assertEquals(2, paramNamesForCapability.length);
        final List<String> parameterListForCapability = Arrays.asList(paramNamesForCapability);
        assertTrue(parameterListForCapability.contains(capabilityParam1));
        assertTrue(parameterListForCapability.contains(capabilityParam2));
        assertFalse(parameterListForCapability.contains(capabilityParam3));

        /*
         * Verify the extras contains mapping of capability params to their values.
         * {
         *     "START_EXERCISE/exercise.name": ["running","jogging"],
         *     "START_EXERCISE/duration": ["60 minutes", "1 hour"],
         * }
         */
        final String capabilityParam1Key = capability + "/" + capabilityParam1;
        final String capabilityParam2Key = capability + "/" + capabilityParam2;
        assertTrue(extra.containsKey(capabilityParam1Key));
        assertTrue(extra.containsKey(capabilityParam2Key));
        final String[] actualCapabilityParams1 = extra.getStringArray(capabilityParam1Key);
        final String[] actualCapabilityParams2 = extra.getStringArray(capabilityParam2Key);
        assertNotNull(actualCapabilityParams1);
        assertEquals(2, actualCapabilityParams1.length);
        assertEquals(capabilityParam1Value1, actualCapabilityParams1[0]);
        assertEquals(capabilityParam1Value2, actualCapabilityParams1[1]);
        assertNotNull(actualCapabilityParams2);
        assertEquals(2, actualCapabilityParams2.length);
        assertEquals(capabilityParam2Value1, actualCapabilityParams2[0]);
        assertEquals(capabilityParam2Value2, actualCapabilityParams2[1]);
    }

    @Test
    @SdkSuppress(minSdkVersion = 25)
    public void testBuilder_copyConstructor() {
        String longLabel = "Test long label";
        ComponentName activity = new ComponentName("Package name", "Class name");
        String disabledMessage = "Test disabled message";
        Set<String> categories = new HashSet<>();
        categories.add("cat1");
        categories.add("cat2");
        LocusIdCompat locusId = new LocusIdCompat("Chat_A_B");
        int rank = 3;
        PersistableBundle persistableBundle = new PersistableBundle();
        ShortcutInfoCompat compat = mBuilder
                .setActivity(activity)
                .setCategories(categories)
                .setDisabledMessage(disabledMessage)
                .setLongLabel(longLabel)
                .setLocusId(locusId)
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
        assertEquals(locusId, copyCompat.getLocusId());
        assertEquals(rank, copyCompat.getRank());
        assertEquals(persistableBundle, copyCompat.getExtras());
    }

    @Test
    @FlakyTest
    @SdkSuppress(minSdkVersion = 26)
    public void testBuilder_fromShortcutInfo() throws Exception {
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
        assertNotNull(compat.getUserHandle());
        assertNotNull(compat.getPackage());
        assertFalse(compat.isCached());
        assertFalse(compat.isDeclaredInManifest());
        assertFalse(compat.isDynamic());
        assertTrue(compat.isEnabled());
        assertFalse(compat.isImmutable());
        assertFalse(compat.isPinned());
        assertFalse(compat.hasKeyFieldsOnly());
    }

    @Test
    @SdkSuppress(minSdkVersion = 25)
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
        LocusIdCompat locusId = new LocusIdCompat("Chat_A_B");
        ShortcutInfoCompat compat = mBuilder
                .setPersons(persons)
                .setLocusId(locusId)
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

        assertEquals(locusId, ShortcutInfoCompat.getLocusId(shortcut));
    }

    @Test
    @SdkSuppress(minSdkVersion = 33)
    public void testToShortcutInfo_ExcludedFromSurfaces() {
        final ShortcutInfoCompat compat = mBuilder.setExcludedFromSurfaces(
                ShortcutInfoCompat.SURFACE_LAUNCHER).build();
        final ShortcutInfo shortcut = compat.toShortcutInfo();
        assertTrue(shortcut.isExcludedFromSurfaces(ShortcutInfo.SURFACE_LAUNCHER));
    }

    @Test
    public void testSetCategoriesAndAddCapabilityBinding() {
        HashSet<String> categories = new HashSet<>();
        categories.add("a");
        mBuilder.setActivity(new ComponentName(mContext, TestActivity.class))
                .setCategories(categories)
                .addCapabilityBinding("b")
                .build();
        assertEquals(1, categories.size());
    }
}
