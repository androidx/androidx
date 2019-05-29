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
import androidx.annotation.VisibleForTesting;
import androidx.annotation.WorkerThread;
import androidx.webkit.internal.AssetHelper;

import java.io.InputStream;
import java.net.URLConnection;

/**
 * Helper class to enable accessing the application's static assets and resources under an
 * http(s):// URL to be loaded by {@link android.webkit.WebView} class.
 * Hosting assets and resources this way is desirable as it is compatible with the Same-Origin
 * policy.
 *
 * <p>
 * For more context about application's assets and resources and how to normally access them please
 * refer to <a href="https://developer.android.com/guide/topics/resources/providing-resources">
 * Android Developer Docs: App resources overview</a>.
 *
 * <p class='note'>
 * This class is expected to be used within
 * {@link android.webkit.WebViewClient#shouldInterceptRequest}, which is not invoked on the
 * application's main thread. Although instances are themselves thread-safe (and may be safely
 * constructed on the application's main thread), exercise caution when accessing private data or
 * the view system.
 *
 * <p>
 * Using http(s):// URLs to access local resources may conflict with a real website. This means
 * that local resources should only be hosted on domains your organization owns (at paths reserved
 * for this purpose) or the default domain reserved for this: {@code appassets.androidplatform.net}.
 *
 * <p>
 * A typical usage would be like:
 * <pre class="prettyprint">
 *     WebViewAssetLoader.Builder assetLoaderBuilder = new WebViewAssetLoader.Builder(this);
 *     final WebViewAssetLoader assetLoader = assetLoaderBuilder.build();
 *     webView.setWebViewClient(new WebViewClient() {
 *         {@literal @}Override
 *         public WebResourceResponse shouldInterceptRequest(WebView view,
 *                                          WebResourceRequest request) {
 *             return assetLoader.shouldInterceptRequest(request);
 *         }
 *     });
 *     // Assets are hosted under http(s)://appassets.androidplatform.net/assets/... by default.
 *     // If the application's assets are in the "main/assets" folder this will read the file
 *     // from "main/assets/www/index.html" and load it as if it were hosted on:
 *     // https://appassets.androidplatform.net/assets/www/index.html
 *     webview.loadUrl(assetLoader.getAssetsHttpsPrefix().buildUpon()
 *                                      .appendPath("www")
 *                                      .appendPath("index.html")
 *                                      .build().toString());
 *
 * </pre>
 */
public final class WebViewAssetLoader {
    private static final String TAG = "WebViewAssetLoader";

    /**
     * An unused domain reserved for Android applications to intercept requests for app assets.
     * <p>
     * It'll be used by default unless the user specified a different domain.
     */
    public static final String KNOWN_UNUSED_AUTHORITY = "appassets.androidplatform.net";

    private static final String HTTP_SCHEME = "http";
    private static final String HTTPS_SCHEME = "https";

    @NonNull private final PathHandler mAssetsHandler;
    @NonNull private final PathHandler mResourcesHandler;

    /**
     * A handler that produces responses for the registered paths.
     *
     * Matches URIs on the form: {@code "http(s)://authority/path/**"}, HTTPS is always enabled.
     *
     * <p>
     * Methods of this handler will be invoked on a background thread and care must be taken to
     * correctly synchronize access to any shared state.
     * <p>
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
         * @param authority the authority to match (For instance {@code "example.com"})
         * @param path the prefix path to match, it should start and end with a {@code "/"}.
         * @param httpEnabled enable hosting under the HTTP scheme, HTTPS is always enabled.
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

        /**
         * Open an {@link InputStream} for the requested URL.
         * <p>
         * This method should be called if {@code match(Uri)} returns true in order to
         * open the file requested by this URL.
         *
         * @param url path that has been matched.
         * @return {@link InputStream} for the requested URL, {@code null} if an error happens
         *         while opening the file or file doesn't exist.
         */
        @Nullable
        public abstract InputStream handle(@NonNull Uri url);

        /**
         * Match against registered scheme, authority and path prefix.
         *
         * Match happens when:
         * <ul>
         *      <li>Scheme is "https" <b>or</b> the scheme is "http" and http is enabled.</li>
         *      <li>Authority exact matches the given URI's authority.</li>
         *      <li>Path is a prefix of the given URI's path.</li>
         * </ul>
         *
         * @param uri the URI whose path we will match against.
         *
         * @return {@code true} if a match happens, {@code false} otherwise.
         */
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
     * A builder class for constructing {@link WebViewAssetLoader} objects.
     */
    public static final class Builder {
        private final Context mContext;

        boolean mAllowHttp;
        @NonNull Uri mAssetsUri;
        @NonNull Uri mResourcesUri;

        /**
         * @param context {@link Context} used to resolve resources/assets.
         */
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
         * @param domain the domain on which app assets should be hosted.
         * @return {@link Builder} object.
         */
        @NonNull
        public Builder setDomain(@NonNull String domain) {
            mAssetsUri = createUriPrefix(domain, mAssetsUri.getPath());
            mResourcesUri = createUriPrefix(domain, mResourcesUri.getPath());
            return this;
        }

        /**
         * Set the prefix path under which app assets should be hosted.
         * The default path for assets is {@code "/assets/"}. The path must start and end with
         * {@code "/"}.
         * <p>
         * A custom prefix path can be used in conjunction with a custom domain, to
         * avoid conflicts with real paths which may be hosted at that domain.
         *
         * @param path the path under which app assets should be hosted.
         * @return {@link Builder} object.
         * @throws IllegalArgumentException if the path is invalid.
         */
        @NonNull
        public Builder setAssetsHostingPath(@NonNull String path) {
            mAssetsUri = createUriPrefix(mAssetsUri.getAuthority(), path);
            return this;
        }

        /**
         * Set the prefix path under which app resources should be hosted.
         * The default path for resources is {@code "/res/"}. The path must start and end with
         * {@code "/"}. A custom prefix path can be used in conjunction with a custom domain, to
         * avoid conflicts with real paths which may be hosted at that domain.
         *
         * @param path the path under which app resources should be hosted.
         * @return {@link Builder} object.
         * @throws IllegalArgumentException if the path is invalid.
         */
        @NonNull
        public Builder setResourcesHostingPath(@NonNull String path) {
            mResourcesUri = createUriPrefix(mResourcesUri.getAuthority(), path);
            return this;
        }

        /**
         * Allow using the HTTP scheme in addition to HTTPS.
         * The default is to not allow HTTP.
         *
         * @return {@link Builder} object.
         */
        @NonNull
        public Builder allowHttp() {
            this.mAllowHttp = true;
            return this;
        }

        /**
         * Build and return a {@link WebViewAssetLoader} object.
         *
         * @return immutable {@link WebViewAssetLoader} object.
         * @throws IllegalArgumentException if the {@code Builder} received conflicting inputs.
         */
        @NonNull
        public WebViewAssetLoader build() {
            String assetsPath = mAssetsUri.getPath();
            String resourcesPath = mResourcesUri.getPath();
            if (assetsPath.startsWith(resourcesPath)) {
                throw new
                    IllegalArgumentException("Resources path cannot be prefix of assets path");
            }
            if (resourcesPath.startsWith(assetsPath)) {
                throw new
                    IllegalArgumentException("Assets path cannot be prefix of resources path");
            }

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
     * Attempt to resolve the {@link WebResourceRequest} to an application resource or
     * asset, and return a {@link WebResourceResponse} for the content.
     * <p>
     * The prefix path used shouldn't be a prefix of a real web path. Thus, in case of having a URL
     * that matches a registered prefix path but the requested asset cannot be found or opened a
     * {@link WebResourceResponse} object with a {@code null} {@link InputStream} will be returned
     * instead of {@code null}. This saves the time of falling back to network and trying to
     * resolve a path that doesn't exist. A {@link WebResourceResponse} with {@code null}
     * {@link InputStream} will be received as an HTTP response with status code {@code 404} and
     * no body.
     * <p>
     * This method should be invoked from within
     * {@link android.webkit.WebViewClient#shouldInterceptRequest(android.webkit.WebView, WebResourceRequest)}.
     *
     * @param request the {@link WebResourceRequest} to process.
     * @return {@link WebResourceResponse} if the request URL matches a registered url,
     *         {@code null} otherwise.
     */
    @RequiresApi(21)
    @WorkerThread
    @Nullable
    public WebResourceResponse shouldInterceptRequest(@NonNull WebResourceRequest request) {
        return shouldInterceptRequestImpl(request.getUrl());
    }

    /**
     * Attempt to resolve the {@code url} to an application resource or asset, and return
     * a {@link WebResourceResponse} for the content.
     * <p>
     * The prefix path used shouldn't be a prefix of a real web path. Thus, in case of having a URL
     * that matches a registered prefix path but the requested asset cannot be found or opened a
     * {@link WebResourceResponse} object with a {@code null} {@link InputStream} will be returned
     * instead of {@code null}. This saves the time of falling back to network and trying to
     * resolve a path that doesn't exist. A {@link WebResourceResponse} with {@code null}
     * {@link InputStream} will be received as an HTTP response with status code {@code 404} and
     * no body.
     * <p>
     * This method should be invoked from within
     * {@link android.webkit.WebViewClient#shouldInterceptRequest(android.webkit.WebView, String)}.
     *
     * @param url the URL string to process.
     * @return {@link WebResourceResponse} if the request URL matches a registered URL,
     *         {@code null} otherwise.
     */
    @WorkerThread
    @Nullable
    public WebResourceResponse shouldInterceptRequest(@NonNull String url) {
        PathHandler handler = null;
        Uri uri = parseAndVerifyUrl(url);
        if (uri == null) {
            return null;
        }
        return shouldInterceptRequestImpl(uri);
    }

    @WorkerThread
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
     * Get the HTTP URL prefix under which assets are hosted.
     * <p>
     * If HTTP is allowed, the prefix will be on the format:
     * {@code "http://<domain>/<prefix-path>/"}, for example:
     * {@code "http://appassets.androidplatform.net/assets/"}.
     *
     * @return the HTTP URL prefix under which assets are hosted, or {@code null} if HTTP is not
     *         enabled.
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
     * Get the HTTPS URL prefix under which assets are hosted.
     * <p>
     * The prefix will be on the format: {@code "https://<domain>/<prefix-path>/"}, if the default
     * values are used then it will be: {@code "https://appassets.androidplatform.net/assets/"}.
     *
     * @return the HTTPS URL prefix under which assets are hosted.
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
     * Get the HTTP URL prefix under which resources are hosted.
     * <p>
     * If HTTP is allowed, the prefix will be on the format:
     * {@code "http://<domain>/<prefix-path>/"}, for example
     * {@code "http://appassets.androidplatform.net/res/"}.
     *
     * @return the HTTP URL prefix under which resources are hosted, or {@code null} if HTTP is not
     *         enabled.
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
     * Get the HTTPS URL prefix under which resources are hosted.
     * <p>
     * The prefix will be on the format: {@code "https://<domain>/<prefix-path>/"}, if the default
     * values are used then it will be: {@code "https://appassets.androidplatform.net/res/"}.
     *
     * @return the HTTPs URL prefix under which resources are hosted.
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
