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
import android.net.Uri;
import android.util.Log;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.annotation.VisibleForTesting;
import androidx.webkit.internal.AssetHelper;

import java.io.InputStream;
import java.net.URLConnection;

/**
 * Helper class meant to be used with the android.webkit.WebView class to enable hosting assets,
 * resources and other data on 'virtual' http(s):// URL.
 * Hosting assets and resources on http(s):// URLs is desirable as it is compatible with the
 * Same-Origin policy.
 *
 * This class is intended to be used from within the
 * {@link android.webkit.WebViewClient#shouldInterceptRequest(android.webkit.WebView,
 * android.webkit.WebResourceRequest)}
 * methods.
 * <pre>
 *     WebViewAssetLoader assetLoader = new WebViewAssetLoader(this);
 *     // For security WebViewAssetLoader uses a unique subdomain by default.
 *     assetLoader.hostAssets();
 *     webView.setWebViewClient(new WebViewClient() {
 *         @Override
 *         public WebResourceResponse shouldInterceptRequest(WebView view,
 *                                          WebResourceRequest request) {
 *             return assetLoader.shouldInterceptRequest(request);
 *         }
 *     });
 *     // If your application's assets are in the "main/assets" folder this will read the file
 *     // from "main/assets/www/index.html" and load it as if it were hosted on:
 *     // https://appassets.androidplatform.net/assets/www/index.html
 *     assetLoader.hostAssets();
 *     webview.loadUrl(assetLoader.getAssetsHttpsPrefix().buildUpon().appendPath("www/index.html")
 *                              .build().toString());
 *
 * </pre>
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public class WebViewAssetLoader {
    private static final String TAG = "WebViewAssetLoader";

    /**
     * Using http(s):// URL to access local resources may conflict with a real website. This means
     * that local resources should only be hosted on domains that the user has control of or which
     * have been dedicated for this purpose.
     *
     * The androidplatform.net domain currently belongs to Google and has been reserved for the
     * purpose of Android applications intercepting navigations/requests directed there. It'll be
     * used by default unless the user specified a different domain.
     *
     * A subdomain "appassets" will be used to even make sure no such collisons would happen.
     */
    public static final String KNOWN_UNUSED_AUTHORITY = "appassets.androidplatform.net";

    private static final String HTTP_SCHEME = "http";
    private static final String HTTPS_SCHEME = "https";

    @NonNull final AssetHelper mAssetHelper;
    @Nullable @VisibleForTesting PathHandler mAssetsHandler;
    @Nullable @VisibleForTesting PathHandler mResourcesHandler;

    /**
     * A handler that produces responses for the registered paths.
     *
     * Methods of this handler will be invoked on a background thread and care must be taken to
     * correctly synchronize access to any shared state.
     *
     * On Android KitKat and above these methods may be called on more than one thread. This thread
     * may be different than the thread on which the shouldInterceptRequest method was invoked.
     * This means that on Android KitKat and above it is possible to block in this method without
     * blocking other resources from loading. The number of threads used to parallelize loading
     * is an internal implementation detail of the WebView and may change between updates which
     * means that the amount of time spent blocking in this method should be kept to an absolute
     * minimum.
     */
    @VisibleForTesting
    /*package*/ abstract static class PathHandler {
        @Nullable private String mMimeType;
        @Nullable private String mEncoding;

        final boolean mHttpEnabled;
        @NonNull final String mAuthority;
        @NonNull final String mPath;

        /**
         * Add a URI to match, and the handler to return when this URI is
         * matched. Matches URIs on the form: "scheme://authority/path/**"
         *
         * @param authority the authority to match (For example example.com)
         * @param path the prefix path to match. Should start and end with a slash "/".
         * @param httpEnabled whether to enable hosting using the http scheme.
         */
        PathHandler(@NonNull final String authority, @NonNull final String path,
                            boolean httpEnabled) {
            if (path.isEmpty() || path.charAt(0) != '/') {
                throw new IllegalArgumentException("Path should start with a slash '/'.");
            }
            if (!path.endsWith("/")) {
                throw new IllegalArgumentException("Path should end with a slash '/'");
            }
            this.mMimeType = null;
            this.mEncoding = null;
            this.mAuthority = authority;
            this.mPath = path;
            this.mHttpEnabled = httpEnabled;
        }

        @Nullable
        public abstract InputStream handle(@NonNull Uri url);

        /**
         * Match happens when:
         *      - Scheme is "https" or the scheme is "http" and http is enabled.
         *      - AND authority exact matches the given URI's authority.
         *      - AND path is a prefix of the given URI's path.
         * @param uri The URI whose path we will match against.
         *
         * @return  true if match happens, false otherwise.
         */
        @Nullable
        public boolean match(@NonNull Uri uri) {
            // Only match HTTP_SCHEME if caller enabled HTTP matches.
            if (uri.getScheme().equals(HTTP_SCHEME) && !mHttpEnabled) {
                return false;
            }
            // Don't match non-HTTP(S) schemes.
            if (!uri.getScheme().equals(HTTP_SCHEME) && !uri.getScheme().equals(HTTPS_SCHEME)) {
                return false;
            }
            if (!uri.getAuthority().equals(mAuthority)) {
                return false;
            }
            return uri.getPath().startsWith(mPath);
        }

        @Nullable
        public String getMimeType() {
            return mMimeType;
        }

        @Nullable
        public String getEncoding() {
            return mEncoding;
        }

        void setMimeType(@Nullable String mimeType) {
            mMimeType = mimeType;
        }

        void setEncoding(@Nullable String encoding) {
            mEncoding = encoding;
        }
    }

    @VisibleForTesting
    /*package*/ WebViewAssetLoader(@NonNull AssetHelper assetHelper) {
        this.mAssetHelper = assetHelper;
    }

    /**
     * Creates a new instance of the WebView asset loader.
     * Will use a default domain on the form of: appassets.androidplatform.net
     *
     * @param context context used to resolve resources/assets.
     */
    public WebViewAssetLoader(@NonNull Context context) {
        this(new AssetHelper(context.getApplicationContext()));
    }

    @Nullable
    private static Uri parseAndVerifyUrl(@Nullable String url) {
        if (url == null) {
            return null;
        }
        Uri uri = Uri.parse(url);
        if (uri == null) {
            Log.e(TAG, "Malformed URL: " + url);
            return null;
        }
        String path = uri.getPath();
        if (path == null || path.length() == 0) {
            Log.e(TAG, "URL does not have a path: " + url);
            return null;
        }
        return uri;
    }

    /**
     * Attempt to retrieve the WebResourceResponse associated with the given <code>request</code>.
     * This method should be invoked from within
     * {@link android.webkit.WebViewClient#shouldInterceptRequest(android.webkit.WebView,
     * android.webkit.WebResourceRequest)}.
     *
     * @param request the request to process.
     * @return a response if the request URL had a matching registered url, null otherwise.
     */
    @RequiresApi(21)
    @Nullable
    public WebResourceResponse shouldInterceptRequest(WebResourceRequest request) {
        PathHandler handler;

        if (mAssetsHandler != null && mAssetsHandler.match(request.getUrl())) {
            handler = mAssetsHandler;
        } else if (mResourcesHandler != null && mResourcesHandler.match(request.getUrl())) {
            handler = mResourcesHandler;
        } else {
            return null;
        }

        InputStream is = handler.handle(request.getUrl());
        return new WebResourceResponse(handler.getMimeType(), handler.getEncoding(), is);
    }

    /**
     * Attempt to retrieve the WebResourceResponse associated with the given <code>url</code>.
     * This method should be invoked from within
     * {@link android.webkit.WebViewClient#shouldInterceptRequest(android.webkit.WebView, String)}.
     *
     * @param url the url to process.
     * @return a response if the request URL had a matching registered url, null otherwise.
     */
    @Nullable
    public WebResourceResponse shouldInterceptRequest(@Nullable String url) {
        PathHandler handler = null;
        Uri uri = parseAndVerifyUrl(url);
        if (uri == null) {
            return null;
        }

        if (mAssetsHandler != null && mAssetsHandler.match(uri)) {
            handler = mAssetsHandler;
        } else if (mResourcesHandler != null && mResourcesHandler.match(uri)) {
            handler = mResourcesHandler;
        } else {
            return null;
        }

        InputStream is = handler.handle(uri);
        return new WebResourceResponse(handler.getMimeType(), handler.getEncoding(), is);
    }

    /**
     * Hosts the application's assets on an http(s):// URL. It will be available under
     * <code>http(s)://appassets.androidplatform.net/assets/...</code>.
     */
    @NonNull
    public void hostAssets() {
        hostAssets(KNOWN_UNUSED_AUTHORITY, "/assets/", true);
    }

    /**
     * Hosts the application's assets on an http(s):// URL. It will be available under
     * <code>http(s)://appassets.androidplatform.net/{virtualAssetPath}/...</code>.
     *
     * @param virtualAssetPath the virtual path under which the assets should be hosted. Should
     *                         have a leading and trailing slash (for example "/assets/www/").
     * @param enableHttp whether to enable hosting using the http scheme.
     */
    @NonNull
    public void hostAssets(@NonNull final String virtualAssetPath, boolean enableHttp) {
        hostAssets(KNOWN_UNUSED_AUTHORITY, virtualAssetPath, enableHttp);
    }

    /**
     * Hosts the application's assets on an http(s):// URL. It will be available under
     * <code>http(s)://{domain}/{virtualAssetPath}/...</code>.
     *
     * @param domain custom domain on which the assets should be hosted (for example "example.com").
     * * @param virtualAssetPath the virtual path under which the assets should be hosted. Should
     *                         have a leading and trailing slash (for example "/assets/www/").
     * @param enableHttp whether to enable hosting using the http scheme.
     */
    @NonNull
    public void hostAssets(@NonNull final String domain, @NonNull final String virtualAssetPath,
                                    boolean enableHttp) {
        final Uri uriPrefix = createUriPrefix(domain, virtualAssetPath);

        mAssetsHandler = new PathHandler(uriPrefix.getAuthority(), uriPrefix.getPath(),
                                            enableHttp) {
            @Override
            public InputStream handle(Uri url) {
                String path = url.getPath().replaceFirst(this.mPath, "");
                Uri.Builder assetUriBuilder = new Uri.Builder();
                assetUriBuilder.path(path);
                Uri assetUri = assetUriBuilder.build();

                InputStream stream = mAssetHelper.openAsset(assetUri);
                this.setMimeType(URLConnection.guessContentTypeFromName(assetUri.getPath()));

                return stream;
            }
        };
    }

    /**
     * Hosts the application's resources on an http(s):// URL. Resources
     * <code>http(s)://appassets.androidplatform.net/res/{resource_type}/{resource_name}</code>.
     */
    @NonNull
    public void hostResources() {
        hostResources(KNOWN_UNUSED_AUTHORITY, "/res/", true);
    }

    /**
     * Hosts the application's resources on an http(s):// URL. Resources
     * <code>http(s)://appassets.androidplatform.net/{virtualResourcesPath}/
     * {resource_type}/{resource_name}</code>.
     *
     * @param virtualResourcesPath the virtual path under which the assets should be hosted. Should
     *                         have a leading and trailing slash (for example "/resources/").
     * @param enableHttp whether to enable hosting using the http scheme.
     */
    @NonNull
    public void hostResources(@NonNull final String virtualResourcesPath, boolean enableHttp) {
        hostResources(KNOWN_UNUSED_AUTHORITY, virtualResourcesPath, enableHttp);
    }

    /**
     * Hosts the application's resources on an http(s):// URL. Resources
     * <code>http(s)://{domain}/{virtualResourcesPath}/{resource_type}/{resource_name}</code>.
     *
     * @param domain custom domain on which the assets should be hosted (for example "example.com").
     * @param virtualResourcesPath the virtual path under which the assets should be hosted. Should
     *                         have a leading and trailing slash (for example "/resources/").
     * @param enableHttp whether to enable hosting using the http scheme.
     */
    @NonNull
    public void hostResources(@NonNull final String domain,
                                    @NonNull final String virtualResourcesPath,
                                    boolean enableHttp) {
        final Uri uriPrefix = createUriPrefix(domain, virtualResourcesPath);

        mResourcesHandler = new PathHandler(uriPrefix.getAuthority(), uriPrefix.getPath(),
                                            enableHttp) {
            @Override
            public InputStream handle(Uri url) {
                String path = url.getPath().replaceFirst(uriPrefix.getPath(), "");
                Uri.Builder resourceUriBuilder = new Uri.Builder();
                resourceUriBuilder.path(path);
                Uri resourceUri = resourceUriBuilder.build();

                InputStream stream  = mAssetHelper.openResource(resourceUri);
                this.setMimeType(URLConnection.guessContentTypeFromName(resourceUri.getPath()));

                return stream;
            }
        };
    }

    @NonNull
    private static Uri createUriPrefix(@NonNull String domain, @NonNull String virtualPath) {
        if (virtualPath.indexOf('*') != -1) {
            throw new IllegalArgumentException(
                    "virtualPath cannot contain the '*' character.");
        }
        if (virtualPath.isEmpty() || virtualPath.charAt(0) != '/') {
            throw new IllegalArgumentException(
                    "virtualPath should start with a slash '/'.");
        }
        if (!virtualPath.endsWith("/")) {
            throw new IllegalArgumentException(
                    "virtualPath should end with a slash '/'.");
        }

        Uri.Builder uriBuilder = new Uri.Builder();
        uriBuilder.authority(domain);
        uriBuilder.path(virtualPath);

        return uriBuilder.build();
    }

    /**
     * Gets the http: scheme prefix at which assets are hosted.
     * @return  the http: scheme prefix at which assets are hosted. Can return null.
     */
    @Nullable
    public Uri getAssetsHttpPrefix() {
        if (mAssetsHandler == null || !mAssetsHandler.mHttpEnabled) {
            return null;
        }

        Uri.Builder uriBuilder = new Uri.Builder();
        uriBuilder.authority(mAssetsHandler.mAuthority);
        uriBuilder.path(mAssetsHandler.mPath);
        uriBuilder.scheme(HTTP_SCHEME);

        return uriBuilder.build();
    }

    /**
     * Gets the https: scheme prefix at which assets are hosted.
     * @return  the https: scheme prefix at which assets are hosted. Can return null.
     */
    @Nullable
    public Uri getAssetsHttpsPrefix() {
        if (mAssetsHandler == null) {
            return null;
        }

        Uri.Builder uriBuilder = new Uri.Builder();
        uriBuilder.authority(mAssetsHandler.mAuthority);
        uriBuilder.path(mAssetsHandler.mPath);
        uriBuilder.scheme(HTTPS_SCHEME);

        return uriBuilder.build();
    }

    /**
     * Gets the http: scheme prefix at which resources are hosted.
     * @return  the http: scheme prefix at which resources are hosted. Can return null.
     */
    @Nullable
    public Uri getResourcesHttpPrefix() {
        if (mResourcesHandler == null || !mResourcesHandler.mHttpEnabled) {
            return null;
        }

        Uri.Builder uriBuilder = new Uri.Builder();
        uriBuilder.authority(mResourcesHandler.mAuthority);
        uriBuilder.path(mResourcesHandler.mPath);
        uriBuilder.scheme(HTTP_SCHEME);

        return uriBuilder.build();
    }

    /**
     * Gets the https: scheme prefix at which resources are hosted.
     * @return  the https: scheme prefix at which resources are hosted. Can return null.
     */
    @Nullable
    public Uri getResourcesHttpsPrefix() {
        if (mResourcesHandler == null) {
            return null;
        }

        Uri.Builder uriBuilder = new Uri.Builder();
        uriBuilder.authority(mResourcesHandler.mAuthority);
        uriBuilder.path(mResourcesHandler.mPath);
        uriBuilder.scheme(HTTPS_SCHEME);

        return uriBuilder.build();
    }
}
