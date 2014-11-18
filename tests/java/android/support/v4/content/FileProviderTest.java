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

package android.support.v4.content;

import static android.provider.OpenableColumns.DISPLAY_NAME;
import static android.provider.OpenableColumns.SIZE;

import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.support.v4.content.FileProvider.SimplePathStrategy;
import android.test.AndroidTestCase;
import android.test.MoreAsserts;
import android.test.suitebuilder.annotation.Suppress;

import libcore.io.IoUtils;
import libcore.io.Streams;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Tests for {@link FileProvider}
 */
@Suppress
public class FileProviderTest extends AndroidTestCase {
    private static final String TEST_AUTHORITY = "moocow";

    private static final String TEST_FILE = "file.test";
    private static final byte[] TEST_DATA = new byte[] { (byte) 0xf0, 0x00, 0x0d };
    private static final byte[] TEST_DATA_ALT = new byte[] { (byte) 0x33, 0x66 };

    private ContentResolver mResolver;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        mResolver = getContext().getContentResolver();
    }

    public void testStrategyUriSimple() throws Exception {
        final SimplePathStrategy strat = new SimplePathStrategy("authority");
        strat.addRoot("tag", mContext.getFilesDir());

        File file = buildPath(mContext.getFilesDir(), "file.test");
        assertEquals("content://authority/tag/file.test",
                strat.getUriForFile(file).toString());

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

    public void testStrategyUriShortestRoot() throws Exception {
        SimplePathStrategy strat = new SimplePathStrategy("authority");
        strat.addRoot("tag1", mContext.getFilesDir());
        strat.addRoot("tag2", new File("/"));

        File file = buildPath(mContext.getFilesDir(), "file.test");
        assertEquals("content://authority/tag1/file.test",
                strat.getUriForFile(file).toString());

        strat = new SimplePathStrategy("authority");
        strat.addRoot("tag1", new File("/"));
        strat.addRoot("tag2", mContext.getFilesDir());

        file = buildPath(mContext.getFilesDir(), "file.test");
        assertEquals("content://authority/tag2/file.test",
                strat.getUriForFile(file).toString());
    }

    public void testStrategyFileSimple() throws Exception {
        final SimplePathStrategy strat = new SimplePathStrategy("authority");
        strat.addRoot("tag", mContext.getFilesDir());

        File file = buildPath(mContext.getFilesDir(), "file.test");
        assertEquals(file.getPath(),
                strat.getFileForUri(Uri.parse("content://authority/tag/file.test")).getPath());

        file = buildPath(mContext.getFilesDir(), "subdir", "file.test");
        assertEquals(file.getPath(), strat.getFileForUri(
                Uri.parse("content://authority/tag/subdir/file.test")).getPath());
    }

    public void testStrategyFileJumpOutside() throws Exception {
        final SimplePathStrategy strat = new SimplePathStrategy("authority");
        strat.addRoot("tag", mContext.getFilesDir());

        try {
            strat.getFileForUri(Uri.parse("content://authority/tag/../file.test"));
            fail("file escaped!");
        } catch (SecurityException e) {
        }
    }

    public void testStrategyEscaping() throws Exception {
        final SimplePathStrategy strat = new SimplePathStrategy("authority");
        strat.addRoot("t/g", mContext.getFilesDir());

        File file = buildPath(mContext.getFilesDir(), "lol\"wat?foo&bar", "wat.txt");
        final String expected = "content://authority/t%2Fg/lol%22wat%3Ffoo%26bar/wat.txt";

        assertEquals(expected,
                strat.getUriForFile(file).toString());
        assertEquals(file.getPath(),
                strat.getFileForUri(Uri.parse(expected)).getPath());
    }

    public void testStrategyExtraParams() throws Exception {
        final SimplePathStrategy strat = new SimplePathStrategy("authority");
        strat.addRoot("tag", mContext.getFilesDir());

        File file = buildPath(mContext.getFilesDir(), "file.txt");
        assertEquals(file.getPath(), strat.getFileForUri(
                Uri.parse("content://authority/tag/file.txt?extra=foo")).getPath());
    }

    public void testStrategyExtraSeparators() throws Exception {
        final SimplePathStrategy strat = new SimplePathStrategy("authority");
        strat.addRoot("tag", mContext.getFilesDir());

        // When canonicalized, the path separators are trimmed
        File inFile = new File(mContext.getFilesDir(), "//foo//bar//");
        File outFile = new File(mContext.getFilesDir(), "/foo/bar");
        final String expected = "content://authority/tag/foo/bar";

        assertEquals(expected,
                strat.getUriForFile(inFile).toString());
        assertEquals(outFile.getPath(),
                strat.getFileForUri(Uri.parse(expected)).getPath());
    }

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

    public void testQueryProjectionOrder() throws Exception {
        final File file = new File(mContext.getFilesDir(), TEST_FILE);
        final Uri uri = stageFileAndGetUri(file, TEST_DATA);

        // Verify that swapped order works
        Cursor cursor = mResolver.query(uri, new String[] {
                SIZE, DISPLAY_NAME }, null, null, null);
        try {
            assertEquals(1, cursor.getCount());
            cursor.moveToFirst();
            assertEquals(TEST_DATA.length, cursor.getLong(0));
            assertEquals(TEST_FILE, cursor.getString(1));
        } finally {
            cursor.close();
        }

        cursor = mResolver.query(uri, new String[] {
                DISPLAY_NAME, SIZE }, null, null, null);
        try {
            assertEquals(1, cursor.getCount());
            cursor.moveToFirst();
            assertEquals(TEST_FILE, cursor.getString(0));
            assertEquals(TEST_DATA.length, cursor.getLong(1));
        } finally {
            cursor.close();
        }
    }

    public void testQueryExtraColumn() throws Exception {
        final File file = new File(mContext.getFilesDir(), TEST_FILE);
        final Uri uri = stageFileAndGetUri(file, TEST_DATA);

        // Verify that extra column doesn't gook things up
        Cursor cursor = mResolver.query(uri, new String[] {
                SIZE, "foobar", DISPLAY_NAME }, null, null, null);
        try {
            assertEquals(1, cursor.getCount());
            cursor.moveToFirst();
            assertEquals(TEST_DATA.length, cursor.getLong(0));
            assertEquals(TEST_FILE, cursor.getString(1));
        } finally {
            cursor.close();
        }
    }

    public void testReadFile() throws Exception {
        final File file = new File(mContext.getFilesDir(), TEST_FILE);
        final Uri uri = stageFileAndGetUri(file, TEST_DATA);

        assertContentsEquals(TEST_DATA, uri);
    }

    public void testWriteFile() throws Exception {
        final File file = new File(mContext.getFilesDir(), TEST_FILE);
        final Uri uri = stageFileAndGetUri(file, TEST_DATA);

        assertContentsEquals(TEST_DATA, uri);

        final OutputStream out = mResolver.openOutputStream(uri);
        try {
            out.write(TEST_DATA_ALT);
        } finally {
            IoUtils.closeQuietly(out);
        }

        assertContentsEquals(TEST_DATA_ALT, uri);
    }

    public void testWriteMissingFile() throws Exception {
        final File file = new File(mContext.getFilesDir(), TEST_FILE);
        final Uri uri = stageFileAndGetUri(file, null);

        try {
            assertContentsEquals(new byte[0], uri);
            fail("Somehow read missing file?");
        } catch(FileNotFoundException e) {
        }

        final OutputStream out = mResolver.openOutputStream(uri);
        try {
            out.write(TEST_DATA_ALT);
        } finally {
            IoUtils.closeQuietly(out);
        }

        assertContentsEquals(TEST_DATA_ALT, uri);
    }

    public void testDelete() throws Exception {
        final File file = new File(mContext.getFilesDir(), TEST_FILE);
        final Uri uri = stageFileAndGetUri(file, TEST_DATA);

        assertContentsEquals(TEST_DATA, uri);

        assertEquals(1, mResolver.delete(uri, null, null));
        assertEquals(0, mResolver.delete(uri, null, null));

        try {
            assertContentsEquals(new byte[0], uri);
            fail("Somehow read missing file?");
        } catch(FileNotFoundException e) {
        }
    }

    public void testMetaDataTargets() {
        Uri actual;

        actual = FileProvider.getUriForFile(mContext, TEST_AUTHORITY,
                new File("/proc/version"));
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
    }

    private void assertContentsEquals(byte[] expected, Uri actual) throws Exception {
        final InputStream in = mResolver.openInputStream(actual);
        try {
            MoreAsserts.assertEquals(expected, Streams.readFully(in));
        } finally {
            IoUtils.closeQuietly(in);
        }
    }

    private Uri stageFileAndGetUri(File file, byte[] data) throws Exception {
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
        return FileProvider.getUriForFile(mContext, TEST_AUTHORITY, file);
    }

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
}
