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

import android.net.Uri;
import android.util.Log;
import android.webkit.WebResourceResponse;

import androidx.test.filters.SmallTest;
import androidx.test.runner.AndroidJUnit4;
import androidx.webkit.internal.AssetHelper;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;

@RunWith(AndroidJUnit4.class)
public class WebViewAssetLoaderTest {
    private static final String TAG = "WebViewAssetLoaderTest";

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
        public InputStream openAsset(Uri uri) {
            return null;
        }

        @Override
        public InputStream openResource(Uri uri) {
            return null;
        }
    }

    @Test
    @SmallTest
    public void testCustomPathHandler() throws Throwable {
        WebViewAssetLoader assetLoader = new WebViewAssetLoader(new MockAssetHelper());
        final String contents = "Some content for testing\n";
        final String encoding = "utf-8";

        assetLoader.mResourcesHandler =
                new WebViewAssetLoader.PathHandler("appassets.androidplatform.net", "/test/",
                        true) {
            @Override
            public String getEncoding() {
                return encoding;
            }

            @Override
            public InputStream handle(Uri url) {
                try {
                    return new ByteArrayInputStream(contents.getBytes(encoding));
                } catch (UnsupportedEncodingException e) {
                    Log.e(TAG, "exception when creating response", e);
                }
                return null;
            }
        };

        WebResourceResponse response =
                assetLoader.shouldInterceptRequest("http://appassets.androidplatform.net/test/");
        Assert.assertNotNull(response);

        Assert.assertEquals(encoding, response.getEncoding());
        Assert.assertEquals(contents, readAsString(response.getData(), encoding));

        Assert.assertNull(assetLoader.shouldInterceptRequest("http://foo.bar/"));
    }

    @Test
    @SmallTest
    public void testHostAssets() throws Throwable {
        final String testHtmlContents = "<body><div>hah</div></body>";

        WebViewAssetLoader assetLoader = new WebViewAssetLoader(new MockAssetHelper() {
            @Override
            public InputStream openAsset(Uri url) {
                if (url.getPath().equals("www/test.html")) {
                    try {
                        return new ByteArrayInputStream(testHtmlContents.getBytes("utf-8"));
                    } catch (IOException e) {
                        Log.e(TAG, "Unable to open asset URL: " + url);
                        return null;
                    }
                }
                return null;
            }
        });

        assetLoader.hostAssets("/assets/", true);
        Assert.assertEquals(assetLoader.getAssetsHttpPrefix(),
                                    Uri.parse("http://appassets.androidplatform.net/assets/"));
        Assert.assertEquals(assetLoader.getAssetsHttpsPrefix(),
                                    Uri.parse("https://appassets.androidplatform.net/assets/"));

        WebResourceResponse response =
                assetLoader.shouldInterceptRequest("http://appassets.androidplatform.net/assets/www/test.html");
        Assert.assertNotNull(response);
        Assert.assertEquals(testHtmlContents, readAsString(response.getData(), "utf-8"));
    }

    @Test
    @SmallTest
    public void testHostResources() throws Throwable {
        final String testHtmlContents = "<body><div>hah</div></body>";

        WebViewAssetLoader assetLoader = new WebViewAssetLoader(new MockAssetHelper() {
            @Override
            public InputStream openResource(Uri uri) {
                try {
                    if (uri.getPath().equals("raw/test.html")) {
                        return new ByteArrayInputStream(testHtmlContents.getBytes("utf-8"));
                    }
                } catch (IOException e) {
                    Log.e(TAG, "exception when creating response", e);
                }
                return null;
            }
        });

        assetLoader.hostResources("/res/", true);
        Assert.assertEquals(assetLoader.getResourcesHttpPrefix(), Uri.parse("http://appassets.androidplatform.net/res/"));
        Assert.assertEquals(assetLoader.getResourcesHttpsPrefix(), Uri.parse("https://appassets.androidplatform.net/res/"));

        WebResourceResponse response =
                 assetLoader.shouldInterceptRequest("http://appassets.androidplatform.net/res/raw/test.html");
        Assert.assertNotNull(response);
        Assert.assertEquals(testHtmlContents, readAsString(response.getData(), "utf-8"));
    }
}
