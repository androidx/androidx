/*
 * Copyright (C) 2013 The Android Open Source Project
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

package androidx.core.content;

import static android.provider.OpenableColumns.DISPLAY_NAME;
import static android.provider.OpenableColumns.SIZE;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.fail;

import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.ProviderInfo;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;

import androidx.core.content.FileProvider.SimplePathStrategy;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.test.R;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Tests for {@link FileProvider}
 */
@SmallTest
@RunWith(AndroidJUnit4.class)
public class FileProviderTest {
    private static final String TEST_AUTHORITY = "moocow";
    private static final String ADDITIONAL_AUTHORITY = "additional.authority";

    private static final String TEST_FILE = "file.test";
    private static final byte[] TEST_DATA = new byte[]{(byte) 0xf0, 0x00, 0x0d};
    private static final byte[] TEST_DATA_ALT = new byte[]{(byte) 0x33, 0x66};
    private static final String TEST_FILE_DISPLAY_NAME = "Test Display Name";

    private ContentResolver mResolver;
    private Context mContext;

    private static File buildPath(File base, String... segments) {
        File cur = base;
        for (String segment : segments) {
            if (cur == null) {
                cur = new File(segment);
            } else {
                cur = new File(cur, segment);
            }
        }
        return cur;
    }

    /**
     * Closes 'closeable', ignoring any checked exceptions. Does nothing if 'closeable' is null.
     */
    private static void closeQuietly(AutoCloseable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (RuntimeException rethrown) {
                throw rethrown;
            } catch (Exception ignored) {
            }
        }
    }

    /**
     * Returns a byte[] containing the remainder of 'in', closing it when done.
     */
    private static byte[] readFully(InputStream in) throws IOException {
        try {
            return readFullyNoClose(in);
        } finally {
            in.close();
        }
    }

    /**
     * Returns a byte[] containing the remainder of 'in'.
     */
    private static byte[] readFullyNoClose(InputStream in) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int count;
        while ((count = in.read(buffer)) != -1) {
            bytes.write(buffer, 0, count);
        }
        return bytes.toByteArray();
    }

    @Before
    public void setup() throws Exception {
        mContext = ApplicationProvider.getApplicationContext();
        mResolver = mContext.getContentResolver();
    }

    @Test
    public void testStrategyUriSimple() throws Exception {
        final SimplePathStrategy strat = new SimplePathStrategy("authority");
        strat.addRoot("tag", mContext.getFilesDir());

        File file = buildPath(mContext.getFilesDir(), "file.test");
        assertEquals("content://authority/tag/file.test", strat.getUriForFile(file).toString());

        file = buildPath(mContext.getFilesDir(), "subdir", "file.test");
        assertEquals("content://authority/tag/subdir/file.test",
                strat.getUriForFile(file).toString());

        file = buildPath(Environment.getExternalStorageDirectory(), "file.test");
        try {
            strat.getUriForFile(file);
            fail("somehow got uri for file outside roots?");
        } catch (IllegalArgumentException e) {
        }
    }

    @Test
    public void testStrategyUriJumpOutside() throws Exception {
        final SimplePathStrategy strat = new SimplePathStrategy("authority");
        strat.addRoot("tag", mContext.getFilesDir());

        File file = buildPath(mContext.getFilesDir(), "..", "file.test");
        try {
            strat.getUriForFile(file);
            fail("file escaped!");
        } catch (IllegalArgumentException e) {
        }
    }

    @Test
    public void testStrategyUriRootPathShouldNotBeReturned() throws Exception {
        final SimplePathStrategy strat = new SimplePathStrategy("authority");
        strat.addRoot("tag", buildPath(mContext.getFilesDir(), "tag"));

        File file = buildPath(mContext.getFilesDir(), "tag");
        try {
            strat.getUriForFile(file);
            fail("root path returned");
        } catch (IllegalArgumentException e) {
        }
    }

    @Test
    public void testStrategyUriRootPathShouldNotBeReturnedUsingPathTraversal() throws Exception {
        final SimplePathStrategy strat = new SimplePathStrategy("authority");
        strat.addRoot("my_tag", buildPath(mContext.getFilesDir(), "tag"));

        File file = buildPath(mContext.getFilesDir(), "my_tag", "..", "tag");
        try {
            strat.getUriForFile(file);
            fail("root path returned");
        } catch (IllegalArgumentException e) {
        }
    }

    @Test
    public void testStrategyUriShortestRoot() throws Exception {
        SimplePathStrategy strat = new SimplePathStrategy("authority");
        strat.addRoot("tag1", mContext.getFilesDir());
        strat.addRoot("tag2", new File("/"));

        File file = buildPath(mContext.getFilesDir(), "file.test");
        assertEquals("content://authority/tag1/file.test", strat.getUriForFile(file).toString());

        strat = new SimplePathStrategy("authority");
        strat.addRoot("tag1", new File("/"));
        strat.addRoot("tag2", mContext.getFilesDir());

        file = buildPath(mContext.getFilesDir(), "file.test");
        assertEquals("content://authority/tag2/file.test", strat.getUriForFile(file).toString());
    }

    @Test
    public void testStrategyFileSimple() throws Exception {
        final SimplePathStrategy strat = new SimplePathStrategy("authority");
        strat.addRoot("tag", mContext.getFilesDir());

        File expectedRoot = mContext.getFilesDir().getCanonicalFile();
        File file = buildPath(expectedRoot, "file.test");
        assertEquals(file.getPath(),
                strat.getFileForUri(Uri.parse("content://authority/tag/file.test")).getPath());

        file = buildPath(expectedRoot, "subdir", "file.test");
        assertEquals(file.getPath(), strat.getFileForUri(
                Uri.parse("content://authority/tag/subdir/file.test")).getPath());
    }

    @Test
    public void testStrategyFileJumpOutside() throws Exception {
        final SimplePathStrategy strat = new SimplePathStrategy("authority");
        strat.addRoot("tag", mContext.getFilesDir());

        try {
            strat.getFileForUri(Uri.parse("content://authority/tag/../file.test"));
            fail("file escaped!");
        } catch (SecurityException e) {
        }
    }

    @Test
    public void testStrategyRootFolderShouldNotBeReturnedAsFileDirectly() throws Exception {
        final SimplePathStrategy strat = new SimplePathStrategy("authority");
        strat.addRoot("tag", buildPath(mContext.getFilesDir(), "tag"));

        try {
            strat.getFileForUri(Uri.parse("content://authority/tag"));
            fail("root folder returned");
        } catch (IllegalArgumentException e) {
        }
    }

    @Test
    public void testStrategyRootFolderShouldNotBeReturnedAsFileUsingPathTraversal()
            throws Exception {
        final SimplePathStrategy strat = new SimplePathStrategy("authority");
        strat.addRoot("my_tag", buildPath(mContext.getFilesDir(), "tag"));

        try {
            strat.getFileForUri(Uri.parse("content://authority/my_tag/../tag"));
            fail("root folder returned");
        } catch (SecurityException e) {
        }
    }

    @Test
    public void testStrategyEscaping() throws Exception {
        final SimplePathStrategy strat = new SimplePathStrategy("authority");
        strat.addRoot("t/g", mContext.getFilesDir());

        File expectedRoot = mContext.getFilesDir().getCanonicalFile();
        File file = buildPath(expectedRoot, "lol\"wat?foo&bar", "wat.txt");
        final String expected = "content://authority/t%2Fg/lol%22wat%3Ffoo%26bar/wat.txt";

        assertEquals(expected, strat.getUriForFile(file).toString());
        assertEquals(file.getPath(), strat.getFileForUri(Uri.parse(expected)).getPath());
    }

    @Test
    public void testStrategyExtraParams() throws Exception {
        final SimplePathStrategy strat = new SimplePathStrategy("authority");
        strat.addRoot("tag", mContext.getFilesDir());

        File expectedRoot = mContext.getFilesDir().getCanonicalFile();
        File file = buildPath(expectedRoot, "file.txt");
        assertEquals(file.getPath(), strat.getFileForUri(
                Uri.parse("content://authority/tag/file.txt?extra=foo")).getPath());
    }

    @Test
    public void testStrategyExtraSeparators() throws Exception {
        final SimplePathStrategy strat = new SimplePathStrategy("authority");
        strat.addRoot("tag", mContext.getFilesDir());

        // When canonicalized, the path separators are trimmed
        File inFile = new File(mContext.getFilesDir(), "//foo//bar//");
        File expectedRoot = mContext.getFilesDir().getCanonicalFile();
        File outFile = new File(expectedRoot, "/foo/bar");
        final String expected = "content://authority/tag/foo/bar";

        assertEquals(expected, strat.getUriForFile(inFile).toString());
        assertEquals(outFile.getPath(), strat.getFileForUri(Uri.parse(expected)).getPath());
    }

    @Test
    public void testQueryProjectionNull() throws Exception {
        final File file = new File(mContext.getFilesDir(), TEST_FILE);
        final Uri uri = stageFileAndGetUri(file, TEST_DATA);

        // Verify that null brings out default columns
        Cursor cursor = mResolver.query(uri, null, null, null, null);
        try {
            assertEquals(1, cursor.getCount());
            cursor.moveToFirst();
            assertEquals(TEST_FILE, cursor.getString(cursor.getColumnIndex(DISPLAY_NAME)));
            assertEquals(TEST_DATA.length, cursor.getLong(cursor.getColumnIndex(SIZE)));
        } finally {
            cursor.close();
        }
    }

    @Test
    public void testQueryProjectionOrder() throws Exception {
        final File file = new File(mContext.getFilesDir(), TEST_FILE);
        final Uri uri = stageFileAndGetUri(file, TEST_DATA);

        // Verify that swapped order works
        Cursor cursor = mResolver.query(uri, new String[]{SIZE, DISPLAY_NAME}, null, null, null);
        try {
            assertEquals(1, cursor.getCount());
            cursor.moveToFirst();
            assertEquals(TEST_DATA.length, cursor.getLong(0));
            assertEquals(TEST_FILE, cursor.getString(1));
        } finally {
            cursor.close();
        }

        cursor = mResolver.query(uri, new String[]{DISPLAY_NAME, SIZE}, null, null, null);
        try {
            assertEquals(1, cursor.getCount());
            cursor.moveToFirst();
            assertEquals(TEST_FILE, cursor.getString(0));
            assertEquals(TEST_DATA.length, cursor.getLong(1));
        } finally {
            cursor.close();
        }
    }

    @Test
    public void testQueryExtraColumn() throws Exception {
        final File file = new File(mContext.getFilesDir(), TEST_FILE);
        final Uri uri = stageFileAndGetUri(file, TEST_DATA);

        // Verify that extra column doesn't gook things up
        Cursor cursor = mResolver.query(uri, new String[]{SIZE, "foobar", DISPLAY_NAME}, null, null,
                null);
        try {
            assertEquals(1, cursor.getCount());
            cursor.moveToFirst();
            assertEquals(TEST_DATA.length, cursor.getLong(0));
            assertEquals(TEST_FILE, cursor.getString(1));
        } finally {
            cursor.close();
        }
    }

    @Test
    public void testQueryDisplayName() throws Exception {
        final File file = new File(mContext.getFilesDir(), TEST_FILE);
        final Uri uri = stageFileAndGetUri(file, TEST_DATA, TEST_AUTHORITY, TEST_FILE_DISPLAY_NAME);

        Cursor cursor = mResolver.query(uri, null, null, null, null);
        try {
            cursor.moveToFirst();
            assertEquals(TEST_FILE_DISPLAY_NAME,
                    cursor.getString(cursor.getColumnIndex(DISPLAY_NAME)));
        } finally {
            cursor.close();
        }
    }

    @Test
    public void testReadFile() throws Exception {
        final File file = new File(mContext.getFilesDir(), TEST_FILE);
        final Uri uri = stageFileAndGetUri(file, TEST_DATA);

        assertContentsEquals(TEST_DATA, uri);
    }

    @Test
    public void testReadFileWithCustomDisplayName() throws Exception {
        final File file = new File(mContext.getFilesDir(), TEST_FILE);
        final Uri uri = stageFileAndGetUri(file, TEST_DATA, TEST_AUTHORITY, TEST_FILE_DISPLAY_NAME);

        assertContentsEquals(TEST_DATA, uri);
    }

    @Test
    public void testWriteFile() throws Exception {
        final File file = new File(mContext.getFilesDir(), TEST_FILE);
        final Uri uri = stageFileAndGetUri(file, TEST_DATA);

        assertContentsEquals(TEST_DATA, uri);

        final OutputStream out = mResolver.openOutputStream(uri);
        try {
            out.write(TEST_DATA_ALT);
        } finally {
            closeQuietly(out);
        }

        assertContentsEquals(TEST_DATA_ALT, uri);
    }

    @Test
    public void testWriteMissingFile() throws Exception {
        final File file = new File(mContext.getFilesDir(), TEST_FILE);
        final Uri uri = stageFileAndGetUri(file, null);

        try {
            assertContentsEquals(new byte[0], uri);
            fail("Somehow read missing file?");
        } catch (FileNotFoundException e) {
        }

        final OutputStream out = mResolver.openOutputStream(uri);
        try {
            out.write(TEST_DATA_ALT);
        } finally {
            closeQuietly(out);
        }

        assertContentsEquals(TEST_DATA_ALT, uri);
    }

    @Test
    public void testDelete() throws Exception {
        final File file = new File(mContext.getFilesDir(), TEST_FILE);
        final Uri uri = stageFileAndGetUri(file, TEST_DATA);

        assertContentsEquals(TEST_DATA, uri);

        assertEquals(1, mResolver.delete(uri, null, null));
        assertEquals(0, mResolver.delete(uri, null, null));

        try {
            assertContentsEquals(new byte[0], uri);
            fail("Somehow read missing file?");
        } catch (FileNotFoundException e) {
        }
    }

    @Test
    public void testMetaDataTargets() {
        Uri actual;

        actual = FileProvider.getUriForFile(mContext, TEST_AUTHORITY, new File("/proc/version"));
        assertEquals("content://moocow/test_root/proc/version", actual.toString());

        actual = FileProvider.getUriForFile(mContext, TEST_AUTHORITY,
                new File("/proc/1/mountinfo"));
        assertEquals("content://moocow/test_init/mountinfo", actual.toString());

        actual = FileProvider.getUriForFile(mContext, TEST_AUTHORITY,
                buildPath(mContext.getFilesDir(), "meow"));
        assertEquals("content://moocow/test_files/meow", actual.toString());

        actual = FileProvider.getUriForFile(mContext, TEST_AUTHORITY,
                buildPath(mContext.getFilesDir(), "thumbs", "rawr"));
        assertEquals("content://moocow/test_thumbs/rawr", actual.toString());

        actual = FileProvider.getUriForFile(mContext, TEST_AUTHORITY,
                buildPath(mContext.getCacheDir(), "up", "down"));
        assertEquals("content://moocow/test_cache/up/down", actual.toString());

        actual = FileProvider.getUriForFile(mContext, TEST_AUTHORITY,
                buildPath(Environment.getExternalStorageDirectory(), "Android", "obb", "foobar"));
        assertEquals("content://moocow/test_external/Android/obb/foobar", actual.toString());

        File[] externalFilesDirs = ContextCompat.getExternalFilesDirs(mContext, null);
        actual = FileProvider.getUriForFile(mContext, TEST_AUTHORITY,
                buildPath(externalFilesDirs[0], "foo", "bar"));
        assertEquals("content://moocow/test_external_files/foo/bar", actual.toString());

        File[] externalCacheDirs = ContextCompat.getExternalCacheDirs(mContext);
        actual = FileProvider.getUriForFile(mContext, TEST_AUTHORITY,
                buildPath(externalCacheDirs[0], "foo", "bar"));
        assertEquals("content://moocow/test_external_cache/foo/bar", actual.toString());

        File[] externalMediaDirs = mContext.getExternalMediaDirs();
        actual = FileProvider.getUriForFile(mContext, TEST_AUTHORITY,
                buildPath(externalMediaDirs[0], "foo", "bar"));
        assertEquals("content://moocow/test_external_media/foo/bar", actual.toString());
    }

    @Test
    public void testAdditionalAuthority() throws Exception {
        final File file = new File(mContext.getFilesDir(), TEST_FILE);
        final Uri uri = stageFileAndGetUri(file, TEST_DATA, ADDITIONAL_AUTHORITY);
        assertEquals("content://additional.authority/test_files/file.test", uri.toString());
        assertContentsEquals(TEST_DATA, uri);
    }

    @Test
    public void testNonExistentAuthority() {
        File file = buildPath(mContext.getFilesDir(), "file.test");
        try {
            FileProvider.getUriForFile(mContext, "example.nonexistent", file);
            fail("Expected exception for non-existent authority");
        } catch (IllegalArgumentException e) {
            assertEquals("Couldn't find meta-data for provider with authority example.nonexistent",
                    e.getMessage());
        }
    }

    @Test
    public void testExplicitResourceId() {
        ProviderInfo providerInfo = new ProviderInfo();
        providerInfo.grantUriPermissions = true;
        providerInfo.authority = TEST_AUTHORITY;

        ProviderInfo resolvedProviderInfo = mContext.getPackageManager().resolveContentProvider(
                providerInfo.authority, PackageManager.GET_META_DATA);
        resolvedProviderInfo.metaData = null;

        // Should throw since there is no metadata.
        assertThrows(IllegalArgumentException.class,
                () -> FileProvider.getFileProviderPathsMetaData(mContext, TEST_AUTHORITY,
                        resolvedProviderInfo, ResourcesCompat.ID_NULL));

        // This should not throw even though there is no metadata, as we've explicitly provided it.
        FileProvider.getFileProviderPathsMetaData(mContext, TEST_AUTHORITY, resolvedProviderInfo,
                R.xml.paths);
    }

    private void assertContentsEquals(byte[] expected, Uri actual) throws Exception {
        final InputStream in = mResolver.openInputStream(actual);
        try {
            assertArrayEquals(expected, readFully(in));
        } finally {
            closeQuietly(in);
        }
    }

    private Uri stageFileAndGetUri(File file, byte[] data) throws Exception {
        return stageFileAndGetUri(file, data, TEST_AUTHORITY, null);
    }

    private Uri stageFileAndGetUri(File file, byte[] data, String authority) throws Exception {
        return stageFileAndGetUri(file, data, authority, null);
    }

    private Uri stageFileAndGetUri(File file, byte[] data, String authority, String displayName)
            throws Exception {
        if (data != null) {
            final FileOutputStream out = new FileOutputStream(file);
            try {
                out.write(data);
            } finally {
                out.close();
            }
        } else {
            file.delete();
        }

        if (displayName != null) {
            return FileProvider.getUriForFile(mContext, authority, file, displayName);
        }
        return FileProvider.getUriForFile(mContext, authority, file);
    }
}
