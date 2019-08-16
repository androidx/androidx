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
import android.util.Log;

import androidx.test.InstrumentationRegistry;
import androidx.test.filters.MediumTest;
import androidx.test.filters.SmallTest;
import androidx.test.runner.AndroidJUnit4;
import androidx.webkit.WebkitUtils;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

@RunWith(AndroidJUnit4.class)
public class AssetHelperTest {
    private static final String TAG = "AssetHelperTest";

    private static final String TEST_STRING = "Just a test";
    private AssetHelper mAssetHelper;
    private File mInternalStorageTestDir;

    @Before
    public void setup() {
        Context context = InstrumentationRegistry.getContext();
        mAssetHelper = new AssetHelper(context);
        mInternalStorageTestDir = new File(context.getFilesDir(), "test_dir");
        mInternalStorageTestDir.mkdirs();
    }

    @After
    public void tearDown() {
        WebkitUtils.recursivelyDeleteFile(mInternalStorageTestDir);
    }

    @Test
    @SmallTest
    public void testOpenExistingResource() {
        InputStream stream = mAssetHelper.openResource("raw/test.txt");

        Assert.assertNotNull("failed to open resource raw/test.txt", stream);
        Assert.assertEquals(readAsString(stream), TEST_STRING);
    }

    @Test
    @SmallTest
    public void testOpenExistingResourceWithLeadingSlash() {
        InputStream stream = mAssetHelper.openResource("/raw/test");

        Assert.assertNotNull("failed to open resource /raw/test.txt with leading slash", stream);
        Assert.assertEquals(readAsString(stream), TEST_STRING);
    }

    @Test
    @SmallTest
    public void testOpenExistingResourceWithNoExtension() {
        InputStream stream = mAssetHelper.openResource("raw/test");

        Assert.assertNotNull("failed to open resource raw/test with no extension", stream);
        Assert.assertEquals(readAsString(stream), TEST_STRING);
    }

    @Test
    @SmallTest
    public void testOpenInvalidResources() {
        Assert.assertNull("raw/nonexist_file.html doesn't exist - should fail",
                          mAssetHelper.openResource("raw/nonexist_file.html"));

        Assert.assertNull("test.txt doesn't have a resource type - should fail",
                          mAssetHelper.openResource("test.txt"));

        Assert.assertNull("resource with \"/android_res\" prefix should fail",
                          mAssetHelper.openResource("/android_res/raw/test.txt"));
    }

    @Test
    @SmallTest
    public void testOpenExistingAsset() {
        InputStream stream = mAssetHelper.openAsset("text/test.txt");

        Assert.assertNotNull("failed to open asset text/test.txt", stream);
        Assert.assertEquals(readAsString(stream), TEST_STRING);
    }

    @Test
    @SmallTest
    public void testOpenExistingAssetWithLeadingSlash() {
        InputStream stream = mAssetHelper.openAsset("/text/test.txt");

        Assert.assertNotNull("failed to open asset /text/test.txt with leading slash", stream);
        Assert.assertEquals(readAsString(stream), TEST_STRING);
    }

    @Test
    @SmallTest
    public void testOpenInvalidAssets() {
        Assert.assertNull("nonexist_file.html doesn't exist - should fail",
                          mAssetHelper.openAsset("nonexist_file.html"));

        Assert.assertNull("asset with \"/android_asset\" prefix should fail",
                          mAssetHelper.openAsset("/android_asset/test.txt"));
    }

    @Test
    @MediumTest
    public void testOpenFileFromInternalStorage() throws Throwable {
        File testFile = new File(mInternalStorageTestDir, "some_file.txt");
        WebkitUtils.writeToFile(testFile, TEST_STRING);

        InputStream stream = AssetHelper.openFile(testFile);
        Assert.assertNotNull("Should be able to open \"" + testFile + "\" from internal storage",
                stream);
        Assert.assertEquals(readAsString(stream), TEST_STRING);
    }

    @Test
    @MediumTest
    public void testOpenFileNameWhichResemblesUriScheme() throws Throwable {
        File testFile = new File(mInternalStorageTestDir, "obb/obb:11/test/some_file.txt");
        WebkitUtils.writeToFile(testFile, TEST_STRING);

        InputStream stream = AssetHelper.openFile(testFile);
        Assert.assertNotNull("Should be able to open \"" + testFile + "\" from internal storage",
                stream);
        Assert.assertEquals(readAsString(stream), TEST_STRING);
    }

    @Test
    @MediumTest
    public void testOpenNonExistingFileInInternalStorage() throws Throwable {
        File testFile = new File(mInternalStorageTestDir, "some/path/to/non_exist_file.txt");
        InputStream stream = AssetHelper.openFile(testFile);
        Assert.assertNull("Should not be able to open a non existing file from internal storage",
                stream);
    }

    @Test
    @SmallTest
    public void testIsCanonicalChildOf() throws Throwable {
        // Two files are used for testing :
        // "/some/path/to/file_1.txt" and "/some/path/file_2.txt"

        File parent = new File(mInternalStorageTestDir, "/some/path/");
        File child = new File(parent, "/to/./file_1.txt");
        boolean res = AssetHelper.isCanonicalChildOf(parent, child);
        Assert.assertTrue(
                "/to/./\"file_1.txt\" is in a subdirectory of \"/some/path/\"", res);

        parent = new File(mInternalStorageTestDir, "/some/path/");
        child = new File(parent, "/to/../file_2.txt");
        res = AssetHelper.isCanonicalChildOf(parent, child);
        Assert.assertTrue(
                "/to/../\"file_2.txt\" is in a subdirectory of \"/some/path/\"", res);

        parent = new File(mInternalStorageTestDir, "/some/path/to");
        child = new File(parent, "/../file_2.txt");
        res = AssetHelper.isCanonicalChildOf(parent, child);
        Assert.assertFalse(
                "/../\"file_2.txt\" is not in a subdirectory of \"/some/path/to/\"", res);
    }

    private static String readAsString(InputStream is) {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        byte[] buffer = new byte[512];
        int len = 0;
        try {
            while ((len = is.read(buffer)) != -1) {
                os.write(buffer, 0, len);
            }
            return new String(os.toByteArray(), "utf-8");
        } catch (IOException e) {
            Log.e(TAG, "exception when reading the string", e);
            return "";
        }
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

    private InputStream assertOpen(String path) {
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
