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

package androidx.webkit.internal;

import android.content.Context;
import android.content.res.Resources;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.MediumTest;
import androidx.test.filters.SmallTest;
import androidx.webkit.WebkitUtils;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.FileNameMap;
import java.net.URLConnection;

@RunWith(AndroidJUnit4.class)
public class AssetHelperTest {
    private static final String TAG = "AssetHelperTest";

    private static final String TEST_STRING = "Just a test";
    private AssetHelper mAssetHelper;
    private File mInternalStorageTestDir;
    private FileNameMap mDefaultFileNameMap;

    @Before
    public void setup() {
        Context context = ApplicationProvider.getApplicationContext();
        mAssetHelper = new AssetHelper(context);
        mInternalStorageTestDir = new File(context.getFilesDir(), "test_dir");
        mInternalStorageTestDir.mkdirs();
        mDefaultFileNameMap = URLConnection.getFileNameMap();
    }

    @After
    public void tearDown() {
        WebkitUtils.recursivelyDeleteFile(mInternalStorageTestDir);
        URLConnection.setFileNameMap(mDefaultFileNameMap);
    }

    @Test
    @SmallTest
    public void testOpenExistingResourceWithExtension() throws Throwable {
        InputStream stream = mAssetHelper.openResource("raw/test.txt");
        Assert.assertEquals(TEST_STRING, readAsString(stream));
    }

    @Test
    @SmallTest
    public void testOpenExistingResourceWithLeadingSlash() throws Throwable {
        InputStream stream = mAssetHelper.openResource("/raw/test");
        Assert.assertEquals(TEST_STRING, readAsString(stream));
    }

    @Test
    @SmallTest
    public void testOpenExistingResourceWithNoExtension() throws Throwable {
        InputStream stream = mAssetHelper.openResource("raw/test");
        Assert.assertEquals(TEST_STRING, readAsString(stream));
    }

    @Test(expected = Resources.NotFoundException.class)
    @SmallTest
    public void testOpenNonExistentResource() throws Throwable {
        mAssetHelper.openResource("raw/nonexist_file.html");
        Assert.fail("raw/nonexist_file.html doesn't exist - should fail");
    }

    @Test(expected = IllegalArgumentException.class)
    @SmallTest
    public void testOpenInvalidResourcePath_onlyFileName() throws Throwable {
        mAssetHelper.openResource("test.txt");
        Assert.fail("test.txt doesn't have a resource type - should fail");
    }

    // This Test makes sure that AssetHelper#openResource trys to open the given path from resources
    // without dropping any parts of it, unlike dropping the "android_res" prefix in
    // "file://android_res/..." urls when opening a resource file.
    @Test(expected = IllegalArgumentException.class)
    @SmallTest
    public void testOpenInvalidResourcePath_threePathSegements() throws Throwable {
        mAssetHelper.openResource("/android_res/raw/test.txt");
        Assert.fail("resource with \"/android_res\" prefix should fail");
    }

    @Test
    @SmallTest
    public void testOpenExistingAsset() throws Throwable {
        InputStream stream = mAssetHelper.openAsset("text/test.txt");
        Assert.assertEquals(TEST_STRING, readAsString(stream));
    }

    @Test
    @SmallTest
    public void testOpenExistingAssetWithLeadingSlash() throws Throwable {
        InputStream stream = mAssetHelper.openAsset("/text/test.txt");
        Assert.assertEquals(TEST_STRING, readAsString(stream));
    }

    @Test(expected = IOException.class)
    @SmallTest
    public void testOpenInvalidAssets_nonExistentAsset() throws Throwable {
        mAssetHelper.openAsset("nonexist_file.html");
        Assert.fail("nonexist_file.html doesn't exist - should fail");
    }

    // This Test makes sure that AssetHelper#openAsset tries to open the given path from assets
    // without dropping any parts of it, unlike dropping the "android_asset" prefix in
    // "file://android_assets/..." urls when opening an asset file.
    @Test(expected = IOException.class)
    @SmallTest
    public void testOpenExistingAsset_withAndroidAssetPrefix() throws Throwable {
        mAssetHelper.openAsset("/android_asset/text/test.txt");
        Assert.fail("asset with \"/android_asset\" prefix should fail");
    }

    @Test
    @MediumTest
    public void testOpenFileFromInternalStorage() throws Throwable {
        File testFile = new File(mInternalStorageTestDir, "some_file.txt");
        WebkitUtils.writeToFile(testFile, TEST_STRING);

        InputStream stream = AssetHelper.openFile(testFile);
        Assert.assertEquals(TEST_STRING, readAsString(stream));
    }

    @Test
    @MediumTest
    public void testOpenFileNameWhichResemblesUriScheme() throws Throwable {
        File testFile = new File(mInternalStorageTestDir, "obb/obb:11/test/some_file.txt");
        WebkitUtils.writeToFile(testFile, TEST_STRING);

        InputStream stream = AssetHelper.openFile(testFile);
        Assert.assertEquals(TEST_STRING, readAsString(stream));
    }

    @Test(expected = IOException.class)
    @MediumTest
    public void testOpenNonExistingFileInInternalStorage() throws Throwable {
        File testFile = new File(mInternalStorageTestDir, "some/path/to/non_exist_file.txt");
        // Calling AssetHelper#openFile should throw IOException
        InputStream stream = AssetHelper.openFile(testFile);
        Assert.fail("Should not be able to open a non existing file from internal storage");
    }

    @Test
    @SmallTest
    public void testGetCanonicalFile() throws Throwable {
        // Two files are used for testing :
        // "/some/path/to/file_1.txt" and "/some/path/file_2.txt"

        File parent = new File(mInternalStorageTestDir, "some/path/");
        File child = AssetHelper.getCanonicalFileIfChild(parent, "to/./file_1.txt");
        File expectedFile = new File(parent, "to/file_1.txt");
        Assert.assertNotNull(
                "to/./\"file_1.txt\" is in a subdirectory of \"some/path/\"", child);
        Assert.assertEquals(expectedFile.getCanonicalPath(), child.getCanonicalPath());

        parent = new File(mInternalStorageTestDir, "some/path/");
        child = AssetHelper.getCanonicalFileIfChild(parent, "to/../file_2.txt");
        expectedFile = new File(parent, "file_2.txt");
        Assert.assertNotNull(
                "to/../\"file_2.txt\" is in a subdirectory of \"some/path/\"", child);
        Assert.assertEquals(expectedFile.getCanonicalPath(), child.getCanonicalPath());

        parent = new File(mInternalStorageTestDir, "some/path/to");
        child = AssetHelper.getCanonicalFileIfChild(parent, "../file_2.txt");
        Assert.assertNull(
                "../\"file_2.txt\" is not in a subdirectory of \"some/path/to/\"", child);
    }

    private static String readAsString(InputStream is) throws IOException {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        byte[] buffer = new byte[512];
        int len = 0;
        while ((len = is.read(buffer)) != -1) {
            os.write(buffer, 0, len);
        }
        return new String(os.toByteArray(), "utf-8");
    }

    // star.svg and star.svgz contain the same data. AssetHelper should decompress the
    // svgz automatically. Load both from assets and assert that they're equal.
    @Test
    @SmallTest
    public void testSvgzAsset() throws IOException {
        InputStream svgStream = null;
        InputStream svgzStream = null;
        try {
            svgStream = assertOpen("star.svg");
            byte[] expectedData = readFully(svgStream);

            svgzStream = assertOpen("star.svgz");
            byte[] actualData = readFully(svgzStream);

            Assert.assertArrayEquals(
                    "Decompressed star.svgz doesn't match star.svg", expectedData, actualData);
        } finally {
            if (svgStream != null) svgStream.close();
            if (svgzStream != null) svgzStream.close();
        }
    }

    @Test
    @SmallTest
    public void testGuessMimeType() {
        // First check the OS
        Assert.assertEquals("text/plain", AssetHelper.guessMimeType("aFile.txt"));

        // Then check the first item in our list
        Assert.assertEquals("video/webm", AssetHelper.guessMimeType("AVideoFile.webm"));
        // A random list item
        Assert.assertEquals("application/xhtml+xml", AssetHelper.guessMimeType("TestMimeFile.xht"));
        // A file path doesn't cause issues
        Assert.assertEquals(
                "application/xhtml+xml", AssetHelper.guessMimeType("a/path/to/TestMimeFile.xht"));
        // A file path doesn't cause issues
        Assert.assertEquals(
                "application/xhtml+xml", AssetHelper.guessMimeType("a/path/to/TestMimeFile.xht"));

        // Check case insensitive
        Assert.assertEquals("video/mpeg", AssetHelper.guessMimeType("aVideo.mPG"));

        // Check a few error conditions fallback to default
        Assert.assertEquals("text/plain", AssetHelper.guessMimeType(null));
        Assert.assertEquals("text/plain", AssetHelper.guessMimeType("No full stop!"));
        Assert.assertEquals("text/plain", AssetHelper.guessMimeType("file."));
        Assert.assertEquals("text/plain", AssetHelper.guessMimeType("A.myownfiletype"));

        // We added this because javascript mime types weren't being handled
        // correctly so also adding a test for that to be safe.
        // Depending on the Android version, this could be either text/javascript (newer),
        // or application/javascript (older) so checking for both.
        MatcherAssert.assertThat(
                AssetHelper.guessMimeType("a js file.js"),
                Matchers.isOneOf("text/javascript", "application/javascript"));

        // Check that overridden mime map is prioritized
        final String expectedMime = "test/mime";

        URLConnection.setFileNameMap(new FileNameMap() {
            @Override
            public String getContentTypeFor(String fileName) {
                return expectedMime;
            }
        });
        Assert.assertEquals(expectedMime, AssetHelper.guessMimeType("aFile.txt"));
    }

    private InputStream assertOpen(String path) throws IOException {
        InputStream stream = mAssetHelper.openAsset(path);
        Assert.assertNotNull("Failed to open \"" + path + "\"", stream);
        return stream;
    }

    private byte[] readFully(InputStream stream) throws IOException {
        ByteArrayOutputStream data = new ByteArrayOutputStream();
        byte[] buf = new byte[4096];
        for (;;) {
            int len = stream.read(buf);
            if (len < 1) break;
            data.write(buf, 0, len);
        }
        return data.toByteArray();
    }
}
