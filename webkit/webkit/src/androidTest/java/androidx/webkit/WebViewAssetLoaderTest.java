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

package androidx.webkit;

import android.content.Context;
import android.content.ContextWrapper;
import android.net.Uri;
import android.webkit.WebResourceResponse;

import static androidx.webkit.WebViewAssetLoader.AssetsPathHandler;
import static androidx.webkit.WebViewAssetLoader.InternalStoragePathHandler;
import static androidx.webkit.WebViewAssetLoader.PathHandler;
import static androidx.webkit.WebViewAssetLoader.ResourcesPathHandler;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;
import androidx.webkit.internal.AssetHelper;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;

@RunWith(AndroidJUnit4.class)
public class WebViewAssetLoaderTest {
    private static final String TAG = "WebViewAssetLoaderTest";

    private static final String CONTENTS = "Some content for testing";
    private static final String ENCODING = "utf-8";

    private static String readAsString(InputStream is, String encoding) throws IOException {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        byte[] buffer = new byte[512];
        int len = 0;
        while ((len = is.read(buffer)) != -1) {
            os.write(buffer, 0, len);
        }
        return new String(os.toByteArray(), encoding);
    }

    private static class MockAssetHelper extends AssetHelper {
        MockAssetHelper() {
            super(null);
        }

        @Override
        public InputStream openAsset(String path) {
            return null;
        }

        @Override
        public InputStream openResource(String path) {
            return null;
        }
    }

    private static class MockContext extends ContextWrapper {
        MockContext() {
            super(ApplicationProvider.getApplicationContext());
        }
    }

    private static class TestPathHandler implements PathHandler {
        @Override
        public WebResourceResponse handle(@NonNull String path) {
            try {
                InputStream stream = new ByteArrayInputStream(CONTENTS.getBytes(ENCODING));
                return new WebResourceResponse(null, null, stream);
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Test
    @SmallTest
    public void testCustomPathHandler() throws Throwable {
        PathHandler pathHandler = new TestPathHandler();
        WebViewAssetLoader assetLoader = new WebViewAssetLoader.Builder()
                                                .addPathHandler("/test/", pathHandler)
                                                .build();

        WebResourceResponse response = assetLoader.shouldInterceptRequest(
                Uri.parse("https://appassets.androidplatform.net/test/"));
        assertResponse(response, CONTENTS);

        Assert.assertNull("non-registered URL should return null response",
                assetLoader.shouldInterceptRequest(Uri.parse("https://foo.bar/")));
    }

    @Test
    @SmallTest
    public void testCustomDomain() throws Throwable {
        PathHandler pathHandler = new TestPathHandler();
        WebViewAssetLoader assetLoader = new WebViewAssetLoader.Builder()
                                                .setDomain("test.myDomain.net")
                                                .addPathHandler("/test/", pathHandler)
                                                .build();

        WebResourceResponse response = assetLoader.shouldInterceptRequest(
                Uri.parse("https://test.myDomain.net/test/"));
        assertResponse(response, CONTENTS);

        Assert.assertNull("non-addPathHandlered URL should return null response",
                assetLoader.shouldInterceptRequest(
                        Uri.parse("https://appassets.androidplatform.net/test/")));
    }

    @Test
    @SmallTest
    public void testAllowingHttp() throws Throwable {
        PathHandler pathHandler = new TestPathHandler();
        WebViewAssetLoader assetLoader = new WebViewAssetLoader.Builder()
                                                .setHttpAllowed(true)
                                                .addPathHandler("/test/", pathHandler)
                                                .build();

        WebResourceResponse response = assetLoader.shouldInterceptRequest(
                Uri.parse("https://appassets.androidplatform.net/test/"));
        assertResponse(response, CONTENTS);

        Assert.assertNotNull("didn't match HTTP URL despite allowing HTTP",
                assetLoader.shouldInterceptRequest(
                        Uri.parse("http://appassets.androidplatform.net/test/")));
    }

    @Test
    @SmallTest
    public void testDisallowingHttp() throws Throwable {
        PathHandler pathHandler = new TestPathHandler();
        WebViewAssetLoader assetLoader = new WebViewAssetLoader.Builder()
                                                .addPathHandler("/test/", pathHandler)
                                                .build();

        WebResourceResponse response = assetLoader.shouldInterceptRequest(
                Uri.parse("https://appassets.androidplatform.net/test/"));
        assertResponse(response, CONTENTS);

        Assert.assertNull("matched HTTP URL despite disallowing HTTP",
                assetLoader.shouldInterceptRequest(
                        Uri.parse("http://appassets.androidplatform.net/test/")));
    }

    @Test
    @SmallTest
    public void testHostAssets() throws Throwable {
        final String testHtmlContents = "<body><div>test</div></body>";

        PathHandler assetsPathHandler = new AssetsPathHandler(new MockAssetHelper() {
            @Override
            public InputStream openAsset(String path) {
                if (path.equals("www/test.html")) {
                    try {
                        return new ByteArrayInputStream(testHtmlContents.getBytes(ENCODING));
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
                return null;
            }
        });
        WebViewAssetLoader assetLoader = new WebViewAssetLoader.Builder()
                                                      .addPathHandler("/assets/", assetsPathHandler)
                                                      .build();

        WebResourceResponse response = assetLoader.shouldInterceptRequest(
                Uri.parse("https://appassets.androidplatform.net/assets/www/test.html"));
        assertResponse(response, testHtmlContents);
    }

    @Test
    @SmallTest
    public void testHostResources() throws Throwable {
        final String testHtmlContents = "<body><div>test</div></body>";

        PathHandler resourcesPathHandler = new ResourcesPathHandler(new MockAssetHelper() {
            @Override
            public InputStream openResource(String path) {
                if (path.equals("raw/test.html")) {
                    try {
                        return new ByteArrayInputStream(testHtmlContents.getBytes(ENCODING));
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
                return null;
            }
        });
        WebViewAssetLoader assetLoader = new WebViewAssetLoader.Builder()
                                                      .addPathHandler("/res/", resourcesPathHandler)
                                                      .build();

        WebResourceResponse response = assetLoader.shouldInterceptRequest(
                Uri.parse("https://appassets.androidplatform.net/res/raw/test.html"));
        assertResponse(response, testHtmlContents);
    }

    @SmallTest
    @Test(expected = IllegalArgumentException.class)
    public void testCreateInternalStorageHandler_entireDataDir() throws Throwable {
        Context context = ApplicationProvider.getApplicationContext();
        File testDir = AssetHelper.getDataDir(context);
        PathHandler handler =
                new InternalStoragePathHandler(context, testDir);
    }

    @SmallTest
    @Test(expected = IllegalArgumentException.class)
    public void testCreateInternalStorageHandler_entireCacheDir() throws Throwable {
        Context context = ApplicationProvider.getApplicationContext();
        File testDir = context.getCacheDir();
        PathHandler handler =
                new InternalStoragePathHandler(context, testDir);
    }

    @SmallTest
    @Test(expected = IllegalArgumentException.class)
    public void testCreateInternalStorageHandler_databasesDir() throws Throwable {
        Context context = ApplicationProvider.getApplicationContext();
        File testDir = new File(AssetHelper.getDataDir(context), "databases/");
        PathHandler handler =
                new InternalStoragePathHandler(context, testDir);
    }

    @SmallTest
    @Test(expected = IllegalArgumentException.class)
    public void testCreateInternalStorageHandler_libDir() throws Throwable {
        Context context = ApplicationProvider.getApplicationContext();
        File testDir = new File(AssetHelper.getDataDir(context), "lib/");
        PathHandler handler =
                new InternalStoragePathHandler(context, testDir);
    }

    @SmallTest
    @Test(expected = IllegalArgumentException.class)
    public void testCreateInternalStorageHandler_webViewDir() throws Throwable {
        Context context = ApplicationProvider.getApplicationContext();
        File testDir = new File(AssetHelper.getDataDir(context), "app_webview");
        PathHandler handler =
                new InternalStoragePathHandler(context, testDir);
    }

    @SmallTest
    @Test(expected = IllegalArgumentException.class)
    public void testCreateInternalStorageHandler_sharedPrefsDir() throws Throwable {
        Context context = ApplicationProvider.getApplicationContext();
        File testDir = new File(AssetHelper.getDataDir(context), "/shared_prefs/");
        PathHandler handler =
                new InternalStoragePathHandler(context, testDir);
    }

    @Test
    @SmallTest
    public void testHostInternalStorageHandler_invalidAccess() throws Throwable {
        Context context = ApplicationProvider.getApplicationContext();
        File testDir = new File(AssetHelper.getDataDir(context), "/public/");
        PathHandler handler =
                new InternalStoragePathHandler(context, testDir);
        WebViewAssetLoader assetLoader = new WebViewAssetLoader.Builder()
                                                      .addPathHandler("/public-data/", handler)
                                                      .build();

        WebResourceResponse response = assetLoader.shouldInterceptRequest(
                Uri.parse("https://appassets.androidplatform.net/public-data/../test.html"));
        Assert.assertNull(
                "should be null since it tries to access a file outside the mounted directory",
                response.getData());

        response = assetLoader.shouldInterceptRequest(
                Uri.parse("https://appassets.androidplatform.net/public-data/html/test.html"));
        Assert.assertNull(
                "should be null as it accesses a non-existent file under the mounted directory",
                response.getData());
    }

    @Test
    @SmallTest
    public void testMultiplePathHandlers() throws Throwable {
        WebViewAssetLoader.Builder builder = new WebViewAssetLoader.Builder();
        for (int i = 1; i <= 5; ++i) {
            final String testContent = CONTENTS + Integer.toString(i);
            builder.addPathHandler("/test_path_" + Integer.toString(i) + "/", path -> {
                try {
                    InputStream is = new ByteArrayInputStream(testContent.getBytes(ENCODING));
                    return new WebResourceResponse(null, null, is);
                } catch (UnsupportedEncodingException e) {
                    throw new RuntimeException(e);
                }
            });
        }
        WebViewAssetLoader assetLoader = builder.build();

        for (int i = 5; i >= 1; --i) {
            WebResourceResponse response = assetLoader.shouldInterceptRequest(
                    Uri.parse("https://appassets.androidplatform.net/test_path_"
                            + Integer.toString(i) + "/"));
            assertResponse(response, CONTENTS + Integer.toString(i));
        }
    }

    // Fake PathHandler for files ending with .zip
    static class FakeZipPathHandler implements PathHandler {
        static final String CONTENTS = "This is zip";

        @Override
        public WebResourceResponse handle(@NonNull String path) {
            try {
                if (path.endsWith(".zip")) {
                    InputStream is = new ByteArrayInputStream(CONTENTS.getBytes(ENCODING));
                    return new WebResourceResponse(null, null, is);
                }
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException(e);
            }
            return null;
        }
    }

    // Fake PathHandler for files ending with .txt
    static class FakeTextPathHandler implements PathHandler {
        static final String CONTENTS = "This is txt";
        private @NonNull String mContents;

        FakeTextPathHandler() {
            this(CONTENTS);
        }

        FakeTextPathHandler(@NonNull String contents) {
            mContents = contents;
        }

        @Override
        public WebResourceResponse handle(@NonNull String path) {
            try {
                if (path.endsWith(".txt")) {
                    InputStream is = new ByteArrayInputStream(mContents.getBytes(ENCODING));
                    return new WebResourceResponse(null, null, is);
                }
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException(e);
            }
            return null;
        }
    }

    @Test
    @SmallTest
    public void testMultiplePathHandlersOnTheSamePath() throws Throwable {
        WebViewAssetLoader assetLoader = new WebViewAssetLoader.Builder()
                .addPathHandler("/test_path/", new FakeZipPathHandler())
                .addPathHandler("/test_path/", new FakeTextPathHandler())
                .build();

        WebResourceResponse response = assetLoader.shouldInterceptRequest(
                Uri.parse("https://appassets.androidplatform.net/test_path/file.zip"));
        assertResponse(response, FakeZipPathHandler.CONTENTS);

        response = assetLoader.shouldInterceptRequest(
                Uri.parse("https://appassets.androidplatform.net/test_path/file.txt"));
        assertResponse(response, FakeTextPathHandler.CONTENTS);

        response = assetLoader.shouldInterceptRequest(
                Uri.parse("https://appassets.androidplatform.net/test_path/file.jpg"));
        Assert.assertNull("handled .jpg file, should return a null response", response);

        // Register in reverse order to make sure it works regardless of order.
        assetLoader = new WebViewAssetLoader.Builder()
                .addPathHandler("/test_path/", new FakeTextPathHandler())
                .addPathHandler("/test_path/", new FakeZipPathHandler())
                .build();

        response = assetLoader.shouldInterceptRequest(
                Uri.parse("https://appassets.androidplatform.net/test_path/file.zip"));
        assertResponse(response, FakeZipPathHandler.CONTENTS);

        response = assetLoader.shouldInterceptRequest(
                Uri.parse("https://appassets.androidplatform.net/test_path/file.txt"));
        assertResponse(response, FakeTextPathHandler.CONTENTS);
    }

    @Test
    @SmallTest
    public void testMultiplePathHandlersOnTheSamePath_orderMatters() throws Throwable {
        WebViewAssetLoader assetLoader = new WebViewAssetLoader.Builder()
                .addPathHandler("/test_path/", new FakeTextPathHandler("contents1"))
                .addPathHandler("/test_path/", new FakeTextPathHandler("contents2"))
                .build();

        // This should prefer the first matching PathHandler
        WebResourceResponse response = assetLoader.shouldInterceptRequest(
                Uri.parse("https://appassets.androidplatform.net/test_path/file.txt"));
        assertResponse(response, "contents1");

        // Register in reverse order and make sure we get "contents2" now
        assetLoader = new WebViewAssetLoader.Builder()
                .addPathHandler("/test_path/", new FakeTextPathHandler("contents2"))
                .addPathHandler("/test_path/", new FakeTextPathHandler("contents1"))
                .build();

        response = assetLoader.shouldInterceptRequest(
                Uri.parse("https://appassets.androidplatform.net/test_path/file.txt"));
        assertResponse(response, "contents2");
    }

    @Test
    @SmallTest
    public void testMimeTypeInPathHandlers() throws Throwable {
        final String testHtmlContents = "<body><div>test</div></body>";

        AssetHelper mockAssetHelper = new MockAssetHelper() {
            @Override
            public InputStream openResource(String path) {
                try {
                    return new ByteArrayInputStream(testHtmlContents.getBytes(ENCODING));
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        };

        WebViewAssetLoader assetLoader = new WebViewAssetLoader.Builder()
                .addPathHandler("/assets/", new AssetsPathHandler(mockAssetHelper))
                .addPathHandler("/res/", new ResourcesPathHandler(mockAssetHelper))
                .build();

        WebResourceResponse response = assetLoader.shouldInterceptRequest(
                Uri.parse("https://appassets.androidplatform.net/res/raw/test"));
        Assert.assertEquals("File doesn't have an extension, MIME type should be text/plain",
                AssetHelper.DEFAULT_MIME_TYPE, response.getMimeType());

        response = assetLoader.shouldInterceptRequest(
                Uri.parse("https://appassets.androidplatform.net/assets/other/test"));
        Assert.assertEquals("File doesn't have an extension, MIME type should be text/plain",
                AssetHelper.DEFAULT_MIME_TYPE, response.getMimeType());

        response = assetLoader.shouldInterceptRequest(
                Uri.parse("https://appassets.androidplatform.net/res/drawable/test.png"));
        Assert.assertEquals(".png file should have mime type image/png regardless of its content",
                "image/png", response.getMimeType());

        response = assetLoader.shouldInterceptRequest(
                Uri.parse("https://appassets.androidplatform.net/assets/images/test.png"));
        Assert.assertEquals(".png file should have mime type image/png regardless of its content",
                "image/png", response.getMimeType());
    }

    @Test
    @SmallTest
    public void testOrderDoesNotMatter_domain() throws Throwable {
        PathHandler pathHandler = new TestPathHandler();
        WebViewAssetLoader assetLoader1 = new WebViewAssetLoader.Builder()
                                                .setDomain("test.myDomain.net")
                                                .addPathHandler("/test/", pathHandler)
                                                .build();

        WebViewAssetLoader assetLoader2 = new WebViewAssetLoader.Builder()
                                                .addPathHandler("/test/", pathHandler)
                                                .setDomain("test.myDomain.net")
                                                .build();

        WebResourceResponse response1 = assetLoader1.shouldInterceptRequest(
                Uri.parse("https://test.myDomain.net/test/"));
        WebResourceResponse response2 = assetLoader2.shouldInterceptRequest(
                Uri.parse("https://test.myDomain.net/test/"));
        assertResponse(response1, CONTENTS);
        assertResponse(response2, CONTENTS);
    }

    @Test
    @SmallTest
    public void testOrderDoesNotMatter_httpAllowed() throws Throwable {
        PathHandler pathHandler = new TestPathHandler();
        WebViewAssetLoader assetLoader1 = new WebViewAssetLoader.Builder()
                                                .setHttpAllowed(true)
                                                .addPathHandler("/test/", pathHandler)
                                                .build();

        WebViewAssetLoader assetLoader2 = new WebViewAssetLoader.Builder()
                                                .addPathHandler("/test/", pathHandler)
                                                .setHttpAllowed(true)
                                                .build();

        WebResourceResponse response1 = assetLoader1.shouldInterceptRequest(
                Uri.parse("http://appassets.androidplatform.net/test/"));
        WebResourceResponse response2 = assetLoader2.shouldInterceptRequest(
                Uri.parse("http://appassets.androidplatform.net/test/"));
        assertResponse(response1, CONTENTS);
        assertResponse(response2, CONTENTS);
    }

    private static void assertResponse(@Nullable WebResourceResponse response,
              @NonNull String expectedContent) throws IOException {
        Assert.assertNotNull("failed to match the URL and returned null response", response);
        Assert.assertNotNull("matched the URL but returned a null InputStream",
                response.getData());
        Assert.assertEquals(expectedContent, readAsString(response.getData(), ENCODING));
    }
}
