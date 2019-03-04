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

    @NonNull private final PathHandler mAssetsHandler;
    @NonNull private final PathHandler mResourcesHandler;

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
    }

    static class AssetsPathHandler extends PathHandler {
        private AssetHelper mAssetHelper;

        AssetsPathHandler(@NonNull final String authority, @NonNull final String path,
                                boolean httpEnabled, @NonNull AssetHelper assetHelper) {
            super(authority, path, httpEnabled);
            mAssetHelper = assetHelper;
        }

        @Override
        public InputStream handle(Uri url) {
            String path = url.getPath().replaceFirst(this.mPath, "");
            Uri.Builder assetUriBuilder = new Uri.Builder();
            assetUriBuilder.path(path);
            Uri assetUri = assetUriBuilder.build();

            return mAssetHelper.openAsset(assetUri);
        }
    }

    static class ResourcesPathHandler extends PathHandler {
        private AssetHelper mAssetHelper;

        ResourcesPathHandler(@NonNull final String authority, @NonNull final String path,
                                boolean httpEnabled, @NonNull AssetHelper assetHelper) {
            super(authority, path, httpEnabled);
            mAssetHelper = assetHelper;
        }

        @Override
        public InputStream handle(Uri url) {
            String path = url.getPath().replaceFirst(this.mPath, "");
            Uri.Builder resourceUriBuilder = new Uri.Builder();
            resourceUriBuilder.path(path);
            Uri resourceUri = resourceUriBuilder.build();

            return mAssetHelper.openResource(resourceUri);
        }
    }


    /**
     * A builder class for constructing WebViewAssetLoader objects.
     */
    public static final class Builder {
        private final Context mContext;

        boolean mAllowHttp;
        @NonNull Uri mAssetsUri;
        @NonNull Uri mResourcesUri;

        public Builder(@NonNull Context context) {
            mContext = context;
            mAllowHttp = false;
            mAssetsUri = createUriPrefix(KNOWN_UNUSED_AUTHORITY, "/assets/");
            mResourcesUri = createUriPrefix(KNOWN_UNUSED_AUTHORITY, "/res/");
        }

        /**
         * Set the domain under which app assets and resources can be accessed.
         * The default domain is {@code "appassets.androidplatform.net"}
         *
         * @param domain the domain on which app assets are hosted.
         * @return builder object.
         */
        public Builder setDomain(@NonNull String domain) {
            mAssetsUri = createUriPrefix(domain, mAssetsUri.getPath());
            mResourcesUri = createUriPrefix(domain, mResourcesUri.getPath());
            return this;
        }

        /**
         * Set the prefix path under which app assets are hosted.
         * The default path for assets is {@code "/assets/"}
         *
         * @param path the path under which app assets are hosted.
         * @return builder object.
         */
        public Builder setAssetsHostingPath(@NonNull String path) {
            mAssetsUri = createUriPrefix(mAssetsUri.getAuthority(), path);
            return this;
        }

        /**
         * Set the prefix path under which app resources are hosted.
         * the default path for resources is {@code "/res/"}
         *
         * @param path the path under which app resources are hosted.
         * @return builder object.
         */
        public Builder setResourcesHostingPath(@NonNull String path) {
            mResourcesUri = createUriPrefix(mResourcesUri.getAuthority(), path);
            return this;
        }

        /**
         * Allow using the HTTP scheme in addition to HTTPS.
         * The default is to not allow HTTP.
         *
         * @return builder object.
         */
        public Builder allowHttp() {
            this.mAllowHttp = true;
            return this;
        }

        /**
         * Build and return WebViewAssetLoader object.
         *
         * @return immutable WebViewAssetLoader object.
         */
        @NonNull
        public WebViewAssetLoader build() {
            AssetHelper assetHelper = new AssetHelper(mContext);
            PathHandler assetHandler = new AssetsPathHandler(mAssetsUri.getAuthority(),
                                                mAssetsUri.getPath(), mAllowHttp, assetHelper);

            PathHandler resourceHandler = new ResourcesPathHandler(mResourcesUri.getAuthority(),
                                                    mResourcesUri.getPath(), mAllowHttp,
                                                    assetHelper);

            return new WebViewAssetLoader(assetHandler, resourceHandler);
        }

        @VisibleForTesting
        @NonNull
        /*package*/ WebViewAssetLoader buildForTest(@NonNull AssetHelper assetHelper) {
            PathHandler assetHandler = new AssetsPathHandler(mAssetsUri.getAuthority(),
                                                mAssetsUri.getPath(), mAllowHttp, assetHelper);

            PathHandler resourceHandler = new ResourcesPathHandler(mResourcesUri.getAuthority(),
                                                    mResourcesUri.getPath(), mAllowHttp,
                                                    assetHelper);

            return new WebViewAssetLoader(assetHandler, resourceHandler);
        }

        @VisibleForTesting
        @NonNull
        /*package*/ WebViewAssetLoader buildForTest(@NonNull PathHandler assetHandler,
                                                        @NonNull PathHandler resourceHandler) {
            return new WebViewAssetLoader(assetHandler, resourceHandler);
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
    }

    /**
     * Creates a new instance of the WebView asset loader.
     * Will use a default domain on the form of: appassets.androidplatform.net
     *
     * @param context context used to resolve resources/assets.
     */
    /*package*/ WebViewAssetLoader(@NonNull PathHandler assetHandler,
                                        @NonNull PathHandler resourceHandler) {
        this.mAssetsHandler = assetHandler;
        this.mResourcesHandler = resourceHandler;
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
        return shouldInterceptRequestImpl(request.getUrl());
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
        return shouldInterceptRequestImpl(uri);
    }

    @Nullable
    private WebResourceResponse shouldInterceptRequestImpl(@NonNull Uri url) {
        PathHandler handler;
        if (mAssetsHandler.match(url)) {
            handler = mAssetsHandler;
        } else if (mResourcesHandler.match(url)) {
            handler = mResourcesHandler;
        } else {
            return null;
        }

        InputStream is = handler.handle(url);
        String mimeType = URLConnection.guessContentTypeFromName(url.getPath());

        return new WebResourceResponse(mimeType, null, is);
    }

    /**
     * Gets the http: scheme prefix at which assets are hosted.
     * @return  the http: scheme prefix at which assets are hosted. Can return null.
     */
    @Nullable
    public Uri getAssetsHttpPrefix() {
        if (!mAssetsHandler.mHttpEnabled) {
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
    @NonNull
    public Uri getAssetsHttpsPrefix() {
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
        if (!mResourcesHandler.mHttpEnabled) {
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
    @NonNull
    public Uri getResourcesHttpsPrefix() {
        Uri.Builder uriBuilder = new Uri.Builder();
        uriBuilder.authority(mResourcesHandler.mAuthority);
        uriBuilder.path(mResourcesHandler.mPath);
        uriBuilder.scheme(HTTPS_SCHEME);

        return uriBuilder.build();
    }
}
