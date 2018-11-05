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

package androidx.core.content.pm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.spy;

import android.content.ComponentName;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.Icon;

import androidx.core.app.Person;
import androidx.core.graphics.drawable.IconCompat;
import androidx.core.test.R;
import androidx.test.InstrumentationRegistry;
import androidx.test.filters.MediumTest;
import androidx.test.runner.AndroidJUnit4;

import com.google.common.util.concurrent.ListenableFuture;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;

@MediumTest
@RunWith(AndroidJUnit4.class)
public class ShortcutInfoCompatSaverTest {

    private static final String ID_SHORTCUT_RESOURCE_ICON = "shortcut-resource-icon";
    private static final String ID_SHORTCUT_BITMAP_ICON = "shortcut-bitmap-icon";
    private static final String ID_SHORTCUT_ADAPTIVE_BITMAP_ICON = "shortcut-adaptive-bitmap-icon";
    private static final String ID_SHORTCUT_NO_ICON = "shortcut-no-icon";

    private Context mContext;
    private ShortcutInfoCompatSaver mShortcutInfoSaver;
    private ExecutorService mCacheUpdateService;
    private ExecutorService mDiskIoService;

    private List<ShortcutInfoCompat> mTestShortcuts = new ArrayList<>();

    private IconCompat mTestResourceIcon;
    private IconCompat mTestBitmapIcon;
    private IconCompat mTestAdaptiveBitmapIcon;

    @Before
    public void setup() {
        mContext = spy(new ContextWrapper(InstrumentationRegistry.getContext()));
        mCacheUpdateService = ShortcutInfoCompatSaver.createExecutorService();
        mDiskIoService = ShortcutInfoCompatSaver.createExecutorService();
        mShortcutInfoSaver = new ShortcutInfoCompatSaver(mContext, mCacheUpdateService,
                mDiskIoService);
        catchAsyncExceptions(mShortcutInfoSaver.removeAllShortcuts());

        HashSet<String> testCategories = new HashSet<>();
        testCategories.add("TestCategory1");
        testCategories.add("TestCategory2");

        Person[] testPersons = {
                new Person.Builder().setName("test person 1").build(),
                new Person.Builder().setName("test person 2").build()};

        Intent[] testIntents = {new Intent("TestAction1"), new Intent("TestAction2")};
        testIntents[1].setClassName("test package", "test class");

        Bitmap redBitmap = Bitmap.createBitmap(200, 150, Bitmap.Config.ARGB_8888);
        redBitmap.eraseColor(Color.RED);
        Bitmap blueBitmap = Bitmap.createBitmap(150, 200, Bitmap.Config.ARGB_8888);
        blueBitmap.eraseColor(Color.BLUE);

        mTestResourceIcon = IconCompat.createWithResource(mContext, R.drawable.bmp_test);
        mTestBitmapIcon = IconCompat.createWithBitmap(redBitmap);
        mTestAdaptiveBitmapIcon = IconCompat.createWithAdaptiveBitmap(blueBitmap);

        mTestShortcuts.add(new ShortcutInfoCompat.Builder(mContext, ID_SHORTCUT_RESOURCE_ICON)
                .setShortLabel("test short label 1")
                .setLongLabel("test long label 1")
                .setDisabledMessage("test disabled message 1")
                .setLongLived()
                .setPersons(testPersons)
                .setCategories(testCategories)
                .setActivity(new ComponentName("test package", "test class"))
                .setAlwaysBadged()
                .setIcon(mTestResourceIcon)
                .setIntents(testIntents)
                .build());
        mTestShortcuts.add(new ShortcutInfoCompat.Builder(mContext, ID_SHORTCUT_BITMAP_ICON)
                .setShortLabel("test short label 2")
                .setCategories(testCategories)
                .setActivity(new ComponentName("test package", "test class"))
                .setIcon(mTestBitmapIcon)
                .setIntent(testIntents[0])
                .build());
        mTestShortcuts.add(new ShortcutInfoCompat.Builder(mContext,
                ID_SHORTCUT_ADAPTIVE_BITMAP_ICON)
                .setShortLabel("test short label 3")
                .setLongLabel("test long label 3")
                .setDisabledMessage("test disabled message 3")
                .setCategories(testCategories)
                .setActivity(new ComponentName("test package", "test class"))
                .setAlwaysBadged()
                .setIcon(mTestAdaptiveBitmapIcon)
                .setIntent(testIntents[1])
                .build());
        mTestShortcuts.add(new ShortcutInfoCompat.Builder(mContext, ID_SHORTCUT_NO_ICON)
                .setShortLabel("test short label 4")
                .setLongLabel("test long label 4")
                .setPerson(testPersons[0])
                .setCategories(testCategories)
                .setIntents(testIntents)
                .build());
        mTestShortcuts.add(new ShortcutInfoCompat.Builder(mContext, "shortcut-no-category")
                .setShortLabel("test short label 5")
                .setActivity(new ComponentName("test package", "test class"))
                .setIcon(mTestResourceIcon)
                .setIntents(testIntents)
                .build());
    }

    @After
    public void tearDown() {
        try {
            mShortcutInfoSaver.removeAllShortcuts().get();
        } catch (Exception e) {
            /* Ignore */
        }
    }

    private List<ShortcutInfoCompat> testShortcutsWithCategories() {
        List<ShortcutInfoCompat> shortcuts = new ArrayList<>();
        for (ShortcutInfoCompat item : mTestShortcuts) {
            Set<String> categories = item.getCategories();
            if (categories != null && !categories.isEmpty()) {
                shortcuts.add(item);
            }
        }
        return shortcuts;
    }

    private void assertShortcutsListEquals(List<ShortcutInfoCompat> expected,
            List<ShortcutInfoCompat> actual) {
        assertNotNull(expected);
        assertNotNull(actual);
        assertEquals(expected.size(), actual.size());

        // The order in the lists is not important
        for (ShortcutInfoCompat expectedShortcut : expected) {
            boolean exists = false;
            for (ShortcutInfoCompat actualShortcut : actual) {
                if (expectedShortcut.getId().equals(actualShortcut.getId())) {
                    assertShortcutEquals(expectedShortcut, actualShortcut);
                    exists = true;
                }
            }
            assertTrue(exists);
        }
    }

    private void assertShortcutEquals(ShortcutInfoCompat expected, ShortcutInfoCompat actual) {
        assertNotNull(expected);
        assertNotNull(actual);

        assertEquals(expected.getId(), actual.getId());
        assertEquals(expected.getDisabledMessage(), actual.getDisabledMessage());
        assertEquals(expected.getLongLabel(), actual.getLongLabel());
        assertEquals(expected.getShortLabel(), actual.getShortLabel());

        if (expected.getActivity() == null) {
            assertNull(actual.getActivity());
        } else {
            assertNotNull(actual.getActivity());
            assertEquals(expected.getActivity().getPackageName(),
                    actual.getActivity().getPackageName());
            assertEquals(expected.getActivity().getClassName(),
                    actual.getActivity().getClassName());
        }

        if (expected.getCategories() == null) {
            assertNull(actual.getCategories());
        } else {
            assertNotNull(actual.getCategories());
            Set<String> expectedCats = expected.getCategories();
            Set<String> actualCats = actual.getCategories();
            assertEquals(expectedCats.size(), actualCats.size());
            assertTrue(actualCats.containsAll(expectedCats));
        }

        Intent[] expIntents = expected.getIntents();
        Intent[] actIntents = actual.getIntents();
        assertEquals(expIntents.length, actIntents.length);
        for (int i = 0; i < expIntents.length; i++) {
            assertEquals(expIntents[i].getAction(), actIntents[i].getAction());
            if (expIntents[i].getComponent() == null) {
                assertNull(actIntents[i].getComponent());
            } else {
                assertEquals(expIntents[i].getComponent().flattenToString(),
                        actIntents[i].getComponent().flattenToString());
            }
        }
    }

    @Test
    public void testGetInstance() {
        ShortcutInfoCompatSaver saver = ShortcutInfoCompatSaver.getInstance(mContext);
        assertNotNull(saver);
        assertEquals(saver, ShortcutInfoCompatSaver.getInstance(mContext));
    }

    @Test
    public void testGetShortcuts_noShortcuts() throws Exception {
        List<ShortcutInfoCompat> shortcuts = mShortcutInfoSaver.getShortcuts();

        assertNotNull(shortcuts);
        assertTrue(shortcuts.isEmpty());
    }

    @Test
    public void testAddShortcuts_skipShortcutsWithNoCategories() throws Exception {
        catchAsyncExceptions(mShortcutInfoSaver.addShortcuts(mTestShortcuts));
        assertShortcutsListEquals(testShortcutsWithCategories(), mShortcutInfoSaver.getShortcuts());
    }

    @Test
    public void testAddShortcuts_forceReload() throws Exception {
        ListenableFuture<?> future = mShortcutInfoSaver.addShortcuts(mTestShortcuts);
        catchAsyncExceptions(future);
        forceReloadFromDisk(future);

        assertShortcutsListEquals(testShortcutsWithCategories(), mShortcutInfoSaver.getShortcuts());
    }

    @Test
    public void testAddShortcuts_incrementalAddWithIdOverlap() throws Exception {
        List<ShortcutInfoCompat> firstBatch = new ArrayList<>();
        firstBatch.add(mTestShortcuts.get(0));
        firstBatch.add(mTestShortcuts.get(1));
        List<ShortcutInfoCompat> secondBatch = new ArrayList<>();
        secondBatch.add(mTestShortcuts.get(1));
        secondBatch.add(mTestShortcuts.get(2));
        List<ShortcutInfoCompat> allShortcuts = new ArrayList<>();
        allShortcuts.add(mTestShortcuts.get(0));
        allShortcuts.add(mTestShortcuts.get(1));
        allShortcuts.add(mTestShortcuts.get(2));

        catchAsyncExceptions(mShortcutInfoSaver.addShortcuts(firstBatch));
        ListenableFuture<Void> future = mShortcutInfoSaver.addShortcuts(secondBatch);
        catchAsyncExceptions(future);
        assertShortcutsListEquals(allShortcuts, mShortcutInfoSaver.getShortcuts());

        forceReloadFromDisk(future);
        assertShortcutsListEquals(allShortcuts, mShortcutInfoSaver.getShortcuts());
    }

    @Test
    public void testRemoveShortcuts() throws Exception {
        catchAsyncExceptions(mShortcutInfoSaver.addShortcuts(mTestShortcuts));
        ArrayList<String> removeIds = new ArrayList<>();
        removeIds.add(mTestShortcuts.get(1).getId());
        removeIds.add(mTestShortcuts.get(3).getId());

        ListenableFuture<?> future = mShortcutInfoSaver.removeShortcuts(removeIds);
        catchAsyncExceptions(future);

        mTestShortcuts.remove(3);
        mTestShortcuts.remove(1);
        assertShortcutsListEquals(testShortcutsWithCategories(), mShortcutInfoSaver.getShortcuts());

        forceReloadFromDisk(future);
        assertShortcutsListEquals(testShortcutsWithCategories(), mShortcutInfoSaver.getShortcuts());
    }

    @Test
    public void testRemoveAllShortcuts() throws Exception {
        catchAsyncExceptions(mShortcutInfoSaver.addShortcuts(mTestShortcuts));
        assertShortcutsListEquals(testShortcutsWithCategories(), mShortcutInfoSaver.getShortcuts());

        ListenableFuture<?> future = mShortcutInfoSaver.removeAllShortcuts();
        catchAsyncExceptions(future);
        assertTrue(mShortcutInfoSaver.getShortcuts().isEmpty());

        forceReloadFromDisk(future);
        assertTrue(mShortcutInfoSaver.getShortcuts().isEmpty());
    }

    @Test
    public void verifyIconsAreNotKeptInMemory() throws Exception {
        ListenableFuture<?> future = mShortcutInfoSaver.addShortcuts(mTestShortcuts);
        catchAsyncExceptions(future);
        forceReloadFromDisk(future);

        List<ShortcutInfoCompat> shortcuts = mShortcutInfoSaver.getShortcuts();
        for (ShortcutInfoCompat item : shortcuts) {
            assertNull(item.mIcon);
        }
    }

    @Test
    public void testGetShortcutIcon() throws Exception {
        catchAsyncExceptions(mShortcutInfoSaver.addShortcuts(mTestShortcuts));

        List<ShortcutInfoCompat> shortcuts = mShortcutInfoSaver.getShortcuts();
        for (ShortcutInfoCompat item : shortcuts) {
            verifyCorrectIconLoaded(item.getId(), mShortcutInfoSaver.getShortcutIcon(item.getId()));
        }
    }

    @Test
    public void testGetShortcutIcon_forceReload() throws Exception {
        ListenableFuture<?> future = mShortcutInfoSaver.addShortcuts(mTestShortcuts);
        catchAsyncExceptions(future);
        forceReloadFromDisk(future);

        List<ShortcutInfoCompat> shortcuts = mShortcutInfoSaver.getShortcuts();
        for (ShortcutInfoCompat item : shortcuts) {
            verifyCorrectIconLoaded(item.getId(), mShortcutInfoSaver.getShortcutIcon(item.getId()));
        }
    }

    @Test
    public void testGetShortcutIcon_unknownId() throws Exception {
        catchAsyncExceptions(mShortcutInfoSaver.addShortcuts(mTestShortcuts));

        assertNull(mShortcutInfoSaver.getShortcutIcon("unknown-id"));
    }

    private void verifyCorrectIconLoaded(String id, IconCompat icon) throws Exception {
        switch (id) {
            case ID_SHORTCUT_RESOURCE_ICON:
                assertNotNull(icon);
                assertEquals(Icon.TYPE_RESOURCE, icon.getType());
                assertEquals(mTestResourceIcon.getResId(), icon.getResId());
                break;
            case ID_SHORTCUT_BITMAP_ICON:
                assertNotNull(icon);
                assertEquals(Icon.TYPE_BITMAP, icon.getType());
                assertEquals(getCenterColor(mTestBitmapIcon), getCenterColor(icon));
                break;
            case ID_SHORTCUT_ADAPTIVE_BITMAP_ICON:
                assertNotNull(icon);
                // Adaptive icons are restored from disk as legacy (non-adaptive) icons. If icon is
                // still waiting to be saved, the original Adaptive Icon will be returned.
                assertTrue(icon.getType() == Icon.TYPE_BITMAP
                        || icon.getType() == Icon.TYPE_ADAPTIVE_BITMAP);
                assertEquals(getCenterColor(mTestAdaptiveBitmapIcon), getCenterColor(icon));
                break;
            case ID_SHORTCUT_NO_ICON:
                assertNull(icon);
                break;
            default:
                throw new Exception("Unknown shortcut Id: " + id);
        }
    }

    private int getCenterColor(IconCompat icon) {
        assertNotNull(icon);
        Bitmap bitmap = icon.getBitmap();
        assertNotNull(bitmap);
        return bitmap.getPixel(bitmap.getWidth() / 2, bitmap.getHeight() / 2);
    }

    private void forceReloadFromDisk(ListenableFuture<?> lastFuture) throws Exception {
        // Wait until the last async operation is finished.
        lastFuture.get();

        mCacheUpdateService = ShortcutInfoCompatSaver.createExecutorService();
        mDiskIoService = ShortcutInfoCompatSaver.createExecutorService();
        mShortcutInfoSaver = new ShortcutInfoCompatSaver(mContext, mCacheUpdateService,
                mDiskIoService);
    }

    private void catchAsyncExceptions(final ListenableFuture<?> future) {
        future.addListener(new Runnable() {
            @Override
            public void run() {
                try {
                    future.get();
                } catch (Exception e) {
                    throw new RuntimeException("Async operation failed", e);
                }
            }
        }, new Executor() {
            @Override
            public void execute(Runnable command) {
                // Run in the current thread
                command.run();
            }
        });
    }
}
