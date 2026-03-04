/*
 * Copyright 2023 The Android Open Source Project
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

import android.os.CancellationSignal;
import android.webkit.CookieManager;
import android.webkit.GeolocationPermissions;
import android.webkit.ServiceWorkerController;
import android.webkit.WebResourceRequest;
import android.webkit.WebStorage;
import android.webkit.WebView;

import androidx.annotation.AnyThread;
import androidx.annotation.RequiresFeature;
import androidx.annotation.RequiresOptIn;
import androidx.annotation.UiThread;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;

/**
 * A Profile represents one browsing session for WebView.
 * <p> You can have multiple profiles and each profile holds its own set of data. The creation
 * and deletion of the Profile is being managed by {@link ProfileStore}.
 */
public interface Profile {

    /**
     * Represents the name of the default profile which can't be deleted.
     */
    String DEFAULT_PROFILE_NAME = "Default";

    /**
     * @return the name of this Profile which was used to create the Profile from
     * ProfileStore create methods.
     */
    @AnyThread
    @RequiresFeature(name = WebViewFeature.MULTI_PROFILE,
            enforcement = "androidx.webkit.WebViewFeature#isFeatureSupported")
    @NonNull
    String getName();

    /**
     * Returns the profile's cookie manager.
     * <p>
     * Can be called from any thread.
     *
     * @throws IllegalStateException if the profile has been deleted by
     *                               {@link ProfileStore#deleteProfile(String)}}.
     */
    @AnyThread
    @RequiresFeature(name = WebViewFeature.MULTI_PROFILE,
            enforcement = "androidx.webkit.WebViewFeature#isFeatureSupported")
    @NonNull
    CookieManager getCookieManager();

    /**
     * Returns the profile's web storage.
     * <p>
     * Can be called from any thread.
     *
     * @throws IllegalStateException if the profile has been deleted by
     *                               {@link ProfileStore#deleteProfile(String)}}.
     */
    @AnyThread
    @RequiresFeature(name = WebViewFeature.MULTI_PROFILE,
            enforcement = "androidx.webkit.WebViewFeature#isFeatureSupported")
    @NonNull
    WebStorage getWebStorage();

    /**
     * Returns the geolocation permissions of the profile.
     * <p>
     * Can be called from any thread.
     *
     * @throws IllegalStateException if the profile has been deleted by
     *                               {@link ProfileStore#deleteProfile(String)}}.
     */
    @AnyThread
    @RequiresFeature(name = WebViewFeature.MULTI_PROFILE,
            enforcement = "androidx.webkit.WebViewFeature#isFeatureSupported")
    @NonNull
    GeolocationPermissions getGeolocationPermissions();

    /**
     * Returns the service worker controller of the profile.
     * <p>
     * Can be called from any thread.
     *
     * @throws IllegalStateException if the profile has been deleted by
     *                               {@link ProfileStore#deleteProfile(String)}}.
     */
    @AnyThread
    @RequiresFeature(name = WebViewFeature.MULTI_PROFILE,
            enforcement = "androidx.webkit.WebViewFeature#isFeatureSupported")
    @NonNull
    ServiceWorkerController getServiceWorkerController();

    /**
     * Denotes that the UrlPrefetch API surface is experimental.
     * It may change without warning.
     */
    @Retention(RetentionPolicy.CLASS)
    @Target({ElementType.METHOD, ElementType.TYPE, ElementType.FIELD})
    @RequiresOptIn(level = RequiresOptIn.Level.ERROR)
    @interface ExperimentalUrlPrefetch {
    }

    /**
     * Starts a URL prefetch request.
     * <p>
     * All WebViews associated with this Profile will use a URL request
     * matching algorithm during execution of all variants of
     * {@link android.webkit.WebView#loadUrl(String)} for determining if there
     * was already a prefetch request executed for the provided URL. This
     * includes prefetches that are "in progress". If a prefetch is matched,
     * WebView will leverage that for handling the URL, otherwise the URL
     * will be handled normally (i.e. through a network request).
     * <p>
     * Applications will still be responsible for calling
     * {@link android.webkit.WebView#loadUrl(String)} to display web contents
     * in a WebView.
     * <p>
     * NOTE: Additional headers passed to
     * {@link android.webkit.WebView#loadUrl(String, Map)} are not considered
     * in the matching algorithm for determining whether or not to serve a
     * prefetched response to a navigation.
     * <p>
     * For max latency saving benefits, it is recommended to call this method
     * as early as possible (i.e. before any WebView associated with this
     * profile is created).
     * <p>
     * Only supports HTTPS scheme.
     *
     * @param url                the url associated with the prefetch request.
     * @param cancellationSignal will make the best effort to cancel an
     *                           in-flight prefetch request, However cancellation is not
     *                           guaranteed.
     * @param callbackExecutor   the executor to resolve the callback with.
     * @param operationCallback  callbacks for reporting result back to application.
     * @throws IllegalArgumentException if the url or callback is null.
     */
    @RequiresFeature(name = WebViewFeature.PROFILE_URL_PREFETCH,
            enforcement = "androidx.webkit.WebViewFeature#isFeatureSupported")
    @AnyThread
    @ExperimentalUrlPrefetch
    void prefetchUrlAsync(@NonNull String url,
            @Nullable CancellationSignal cancellationSignal,
            @NonNull Executor callbackExecutor,
            @NonNull WebViewOutcomeReceiver<Void, PrefetchException> operationCallback);

    /**
     * Starts a URL prefetch request.
     * <p>
     * All WebViews associated with this Profile will use a URL request
     * matching algorithm during execution of all variants of
     * {@link android.webkit.WebView#loadUrl(String)} for determining if there
     * was already a prefetch request executed for the provided URL. This
     * includes prefetches that are "in progress". If a prefetch is matched,
     * WebView will leverage that for handling the URL, otherwise the URL
     * will be handled normally (i.e. through a network request).
     * <p>
     * Applications will still be responsible for calling
     * {@link android.webkit.WebView#loadUrl(String)} to display web contents
     * in a WebView.
     * <p>
     * NOTE: Additional headers passed to
     * {@link android.webkit.WebView#loadUrl(String, Map)} are not considered
     * in the matching algorithm for determining whether or not to serve a
     * prefetched response to a navigation.
     * <p>
     * For max latency saving benefits, it is recommended to call this method
     * as early as possible (i.e. before any WebView associated with this
     * profile is created).
     * <p>
     * Only supports HTTPS scheme.
     *
     * @param url                          the url associated with the prefetch request.
     * @param cancellationSignal           will make the best effort to cancel an
     *                                     in-flight prefetch request, However cancellation is not
     *                                     guaranteed.
     * @param callbackExecutor             the executor to resolve the callback with.
     * @param speculativeLoadingParameters parameters to customize the prefetch request.
     * @param operationCallback            callbacks for reporting result back to application.
     * @throws IllegalArgumentException if the url or callback is null.
     */
    @RequiresFeature(name = WebViewFeature.PROFILE_URL_PREFETCH,
            enforcement = "androidx.webkit.WebViewFeature#isFeatureSupported")
    @AnyThread
    @ExperimentalUrlPrefetch
    void prefetchUrlAsync(@NonNull String url,
            @Nullable CancellationSignal cancellationSignal,
            @NonNull Executor callbackExecutor,
            @NonNull SpeculativeLoadingParameters speculativeLoadingParameters,
            @NonNull WebViewOutcomeReceiver<Void, PrefetchException> operationCallback);


    /**
     * Sets the {@link SpeculativeLoadingConfig} for the current profile session.
     * These configurations will be applied to any Prefetch requests made after they are set;
     * they will not be applied to in-flight requests.
     * <p>
     * These configurations will be applied to any prefetch requests initiated by
     * a prerender request. This applies specifically to WebViews that are
     * associated with this Profile.
     * <p>
     * @param speculativeLoadingConfig the config to set for this profile session.
     */
    @RequiresFeature(name = WebViewFeature.SPECULATIVE_LOADING_CONFIG,
            enforcement = "androidx.webkit.WebViewFeature#isFeatureSupported")
    @UiThread
    @ExperimentalUrlPrefetch
    void setSpeculativeLoadingConfig(@NonNull SpeculativeLoadingConfig
            speculativeLoadingConfig);

    /**
     * Denotes that the WarmUpRendererProcess API surface is experimental.
     * It may change without warning.
     */
    @Retention(RetentionPolicy.CLASS)
    @Target({ElementType.METHOD, ElementType.TYPE, ElementType.FIELD})
    @RequiresOptIn(level = RequiresOptIn.Level.ERROR)
    @interface ExperimentalWarmUpRendererProcess {
    }

    /**
     * Initiates warm-up of the renderer process associated with this Profile.
     * <p>
     * If no renderer currently exists for the profile, this will kick off the process of
     * starting one in the background. This call does not block or guarantee that the
     * renderer will be fully started by the time it returns.
     * <p>
     * This can be used to reduce perceived latency when a renderer is needed shortly after.
     */
    @RequiresFeature(name = WebViewFeature.WARM_UP_RENDERER_PROCESS,
            enforcement = "androidx.webkit.WebViewFeature#isFeatureSupported")
    @UiThread
    @ExperimentalWarmUpRendererProcess
    void warmUpRendererProcess();

    /**
     * Denotes that the OriginMatchedHeader API surface is experimental.
     * It may change without warning.
     */
    @Retention(RetentionPolicy.CLASS)
    @Target({ElementType.METHOD, ElementType.TYPE, ElementType.FIELD})
    @RequiresOptIn(level = RequiresOptIn.Level.ERROR)
    @interface ExperimentalOriginMatchedHeader {
    }

    /**
     * Set a custom header to be applied to HTTP requests to the specified origins.
     * <p>
     * It applies to all requests that are initiated after this method is called, including
     * prefetch requests and requests sent from service workers.
     * It does <em>not</em> apply the header to WebSocket requests.
     *
     * <p>Headers added through this API will be present in the set returned by
     * {@link WebResourceRequest#getRequestHeaders()} provided in
     * {@link android.webkit.WebViewClient#shouldInterceptRequest(WebView, WebResourceRequest)}
     * and {@link android.webkit.ServiceWorkerClient#shouldInterceptRequest(WebResourceRequest)}.
     * <p>
     * Calling this method again with the same {@code headerName} parameter will overwrite any
     * previously set mapping.
     *
     * @param headerName  A
     *                    <a href="https://datatracker.ietf.org/doc/html/rfc7230#section-3.2">valid HTTP header name string</a>
     * @param headerValue  A
     *                          <a href="https://datatracker.ietf.org/doc/html/rfc7230#section-3.2">valid HTTP value name string</a>
     * @param originRules a set of origin rules following the same format as
     *                    {@link WebViewCompat#addWebMessageListener}
     */
    @RequiresFeature(name = WebViewFeature.ORIGIN_MATCHED_HEADERS,
            enforcement = "androidx.webkit.WebViewFeature#isFeatureSupported")
    @UiThread
    @ExperimentalOriginMatchedHeader
    void setOriginMatchedHeader(@NonNull String headerName,
            @NonNull String headerValue, @NonNull Set<String> originRules);

    /**
     * Returns true if the profile has a value set for the given header name.
     *
     * <p>This method is case sensitive.
     *
     * @param headerName A
     *                   <a href="https://datatracker.ietf.org/doc/html/rfc7230#section-3.2">valid HTTP header name string</a>
     * @return {@code true} if there is a value mapped for the provided {@code
     * headerName}, {code false} otherwise.
     * @see #setOriginMatchedHeader(String, String, Set)
     */
    @RequiresFeature(name = WebViewFeature.ORIGIN_MATCHED_HEADERS,
            enforcement = "androidx.webkit.WebViewFeature#isFeatureSupported")
    @UiThread
    @ExperimentalOriginMatchedHeader
    boolean hasOriginMatchedHeader(@NonNull String headerName);

    /**
     * Removes the specified header from the set of headers attached to requests.
     * <p>
     * It is safe to call this method even if {@code headerName} has not previously been set via
     * {@link #setOriginMatchedHeader(String, String, Set)}
     *
     * <p>This method is case sensitive.
     *
     * @param headerName Header to remove.
     * @see #setOriginMatchedHeader(String, String, Set)
     */
    @RequiresFeature(name = WebViewFeature.ORIGIN_MATCHED_HEADERS,
            enforcement = "androidx.webkit.WebViewFeature#isFeatureSupported")
    @UiThread
    @ExperimentalOriginMatchedHeader
    void clearOriginMatchedHeader(@NonNull String headerName);

    /**
     * Remove any currently set headers from being applied to network requests.
     * @see #setOriginMatchedHeader(String, String, Set)
     */
    @RequiresFeature(name = WebViewFeature.ORIGIN_MATCHED_HEADERS,
            enforcement = "androidx.webkit.WebViewFeature#isFeatureSupported")
    @UiThread
    @ExperimentalOriginMatchedHeader
    void clearAllOriginMatchedHeaders();

    /**
     * Add a header for outgoing requests that match the given origin rules.
     * <p>
     * It applies to all requests that are initiated after this method is called,
     * including prefetch requests and requests sent from service workers. It does
     * not apply the header to WebSocket requests.
     * <p>
     * Headers added through this API will be present in the set returned by
     * getRequestHeaders provided in shouldInterceptRequest.
     * <p>
     * If this method is called multiple times with headers that have the same name and value,
     * then the sets of origin rules will be merged into a single set.
     * <p>
     * If multiple headers with the same name but different values match a request,
     * then all the values will be sent in a comma-separated list of values
     * following the guidance for <a
     * href="https://www.rfc-editor.org/rfc/rfc7230#section-3.2.2">repeated
     * header fields</a>. This does not take into account whether such merging is safe.
     *
     * <p>Headers are considered the same if their {@code name} matches case-insensitive and
     * their {@code value} matches case-sensitive.
     * This follows
     * <a href="https://www.rfc-editor.org/rfc/rfc9110.html#name-field-names">RFC 9110</a>,
     * which states that "field names are case insensitive".
     * This API will use the casing of the first custom header encountered.
     *
     * @param header The header to add.
     */
    @RequiresFeature(name = WebViewFeature.CUSTOM_REQUEST_HEADERS,
            enforcement = "androidx.webkit.WebViewFeature#isFeatureSupported")
    @UiThread
    default void addCustomHeader(@NonNull CustomHeader header) {
        // default to avoid breaking backwards compatibility.
    }

    /**
     * Returns true if the profile has a value set for the given header name.
     *
     * <p>This method is case insensitive.
     *
     * @param headerName A
     *                   <a href="https://datatracker.ietf.org/doc/html/rfc7230#section-3.2">valid HTTP header name string</a>
     * @return {@code true} if there is a value mapped for the provided {@code
     * headerName}, {code false} otherwise.
     * @see #addCustomHeader(CustomHeader)
     */
    @RequiresFeature(name = WebViewFeature.CUSTOM_REQUEST_HEADERS,
            enforcement = "androidx.webkit.WebViewFeature#isFeatureSupported")
    @UiThread
    default boolean hasCustomHeader(@NonNull String headerName) {
        // default to avoid breaking backwards compatibility.
        return false;
    }

    /**
     * Returns all custom headers set with {@link #addCustomHeader(CustomHeader)} or
     * {@link #setOriginMatchedHeader(String, String, Set)}.
     */
    @RequiresFeature(name = WebViewFeature.CUSTOM_REQUEST_HEADERS,
            enforcement = "androidx.webkit.WebViewFeature#isFeatureSupported")
    @NonNull
    @UiThread
    default Set<CustomHeader> getCustomHeaders() {
        // default to avoid breaking backwards compatibility.
        return Collections.emptySet();
    }

    /**
     * Returns all custom headers set with {@link #addCustomHeader(CustomHeader)} or
     * {@link #setOriginMatchedHeader(String, String, Set)} which have the specified {@code name}.
     *
     * <p>This method is case insensitive.
     *
     * @param name Name of headers to get. Case sensitive.
     */
    @RequiresFeature(name = WebViewFeature.CUSTOM_REQUEST_HEADERS,
            enforcement = "androidx.webkit.WebViewFeature#isFeatureSupported")
    @NonNull
    @UiThread
    default Set<CustomHeader> getCustomHeaders(@NonNull String name) {
        // default to avoid breaking backwards compatibility.
        return Collections.emptySet();
    }

    /**
     * Returns all custom headers set with {@link #addCustomHeader(CustomHeader)} or
     * {@link #setOriginMatchedHeader(String, String, Set)} which have the specified {@code name}
     * and {@code value}.
     *
     * <p>This method is case insensitive for {@code name} but case-sensitive for {@code value}.
     *
     * @param name  Name of headers to get. Case sensitive.
     * @param value Value of headers to get. Case sensitive.
     */
    @RequiresFeature(name = WebViewFeature.CUSTOM_REQUEST_HEADERS,
            enforcement = "androidx.webkit.WebViewFeature#isFeatureSupported")
    @NonNull
    @UiThread
    default Set<CustomHeader> getCustomHeaders(@NonNull String name, @NonNull String value) {
        // default to avoid breaking backwards compatibility.
        return Collections.emptySet();
    }

    /**
     * Removes the specified headers from the set of headers attached to requests. This will
     * remove all configured headers that match {@code headerName}.
     * <p>
     * It is safe to call this method even if {@code headerName} has not previously been set via
     * {@link #addCustomHeader(CustomHeader)} or
     * {@link #setOriginMatchedHeader(String, String, Set)}.
     *
     * <p>This method is case insensitive.
     *
     * @param headerName Header to remove.
     * @see #addCustomHeader(CustomHeader)
     */
    @RequiresFeature(name = WebViewFeature.CUSTOM_REQUEST_HEADERS,
            enforcement = "androidx.webkit.WebViewFeature#isFeatureSupported")
    @UiThread
    default void clearCustomHeader(@NonNull String headerName) {
        // default to avoid breaking backwards compatibility.
    }

    /**
     * Removes the specified header from the set of headers attached to requests.
     * <p>
     * It is safe to call this method even if {@code (headerName, headerValue)} has not
     * previously been set via
     * {@link #addCustomHeader(CustomHeader)} or
     * {@link #setOriginMatchedHeader(String, String, Set)}.
     *
     * <p>This method is case insensitive for {@code name} but case-sensitive for {@code value}.
     *
     * @param headerName  Header name to remove.
     * @param headerValue Header value to remove.
     * @see #addCustomHeader(CustomHeader)
     */
    @RequiresFeature(name = WebViewFeature.CUSTOM_REQUEST_HEADERS,
            enforcement = "androidx.webkit.WebViewFeature#isFeatureSupported")
    @UiThread
    default void clearCustomHeader(@NonNull String headerName, @NonNull String headerValue) {
        // default to avoid breaking backwards compatibility.
    }

    /**
     * Remove any currently set headers from being applied to network requests.
     *
     * @see #addCustomHeader(CustomHeader)
     * @see #setOriginMatchedHeader(String, String, Set)
     */
    @RequiresFeature(name = WebViewFeature.CUSTOM_REQUEST_HEADERS,
            enforcement = "androidx.webkit.WebViewFeature#isFeatureSupported")
    @UiThread
    default void clearAllCustomHeaders() {
        // default to avoid breaking backwards compatibility.
    }

    /**
     * Denotes that the Profile#preconnect API surface is experimental.
     * It may change without warning.
     */
    @Retention(RetentionPolicy.CLASS)
    @Target({ElementType.METHOD, ElementType.TYPE, ElementType.FIELD})
    @RequiresOptIn(level = RequiresOptIn.Level.ERROR)
    @interface ExperimentalPreconnect {
    }

    /**
     * Preconnects to the given origin, this can speed up future loads.
     * <p>
     * Opens a connection to the provided origin, performing DNS lookup and TCP/TLS handshakes. This
     * can speed up future loads to the origin which could use the open connection. The connection
     * will remain open (and can be reused by future loads) until it times out (roughly 30s).
     * <p>
     * The main benefit of this API is to preconnect to origins that haven't yet been visited by a
     * WebView - it provides no further performance benefit to origins that have already been
     * loaded.
     * <p>
     * Note: Preconnect operates on origins, but for convenience full URLs can be provided. A call
     * with a full URL (such as {@code https://www.example.com/index.html}) will be treated as a call
     * to the origin ({@code https://www.example.com}).
     * <p>
     * Multiple origins can be connected to by calling this API multiple times.
     * <p>
     * See:
     * <a href="https://developer.mozilla.org/en-US/docs/Web/HTML/Reference/Attributes/rel/preconnect">HTML Preconnect Specification</a>
     *
     * @param url A url containing the origin to open a connection to.
     */
    @RequiresFeature(name = WebViewFeature.PRECONNECT,
            enforcement = "androidx.webkit.WebViewFeature#isFeatureSupported")
    @UiThread
    @ExperimentalPreconnect
    default void preconnect(@NonNull String url) {
        // We provide a default implementation of this method so that embedders extending the
        // Profile (eg, for testing) don't have their build broken by the addition of this
        // method. However, throw a runtime exception if this method is actually called, as
        // that's better than silently no-oping.
        throw new UnsupportedOperationException("Profile#preconnect is not implemented.");
    }

    /**
     * Denotes that the Profile#addQuicHints API surface is experimental.
     * It may change without warning.
     */
    @Retention(RetentionPolicy.CLASS)
    @Target({ElementType.METHOD, ElementType.TYPE, ElementType.FIELD})
    @RequiresOptIn(level = RequiresOptIn.Level.ERROR)
    @interface ExperimentalAddQuicHints {
    }

    /**
     * Advises that the given origins support the QUIC protocol and that WebView should use that to
     * connect to them.
     * <p>
     * By default, when connecting to a new server, WebView attempts both a HTTP3 (QUIC) and HTTP2
     * connection, choosing the one that responds faster. This can leads to cases where HTTP2
     * responds faster, even though HTTP3 is supported and would result in a faster overall load.
     * Calling this API tells WebView to prefer HTTP3 connections for these origins.
     * <p>
     * Note: addQuicHints operates on origins, but for convenience full URLs can be provided. A full
     * URL (such as {@code https://www.example.com/index.html}) will be treated as its origin
     * ({@code https://www.example.com}).
     * <p>
     * This method can be called multiple times and the result is additive - QUIC hints are applied
     * to all of the origins provided to all calls. Providing the same origin multiple times has no
     * further effect.
     *
     * @param urls A set of urls representing origins that support the QUIC protocol.
     */
    @RequiresFeature(name = WebViewFeature.ADD_QUIC_HINTS_V1,
            enforcement = "androidx.webkit.WebViewFeature#isFeatureSupported")
    @UiThread
    @ExperimentalAddQuicHints
    default void addQuicHints(@NonNull Set<@NonNull String> urls) {
        // We provide a default implementation of this method so that embedders extending the
        // Profile (eg, for testing) don't have their build broken by the addition of this
        // method. However, throw a runtime exception if this method is actually called, as
        // that's better than silently no-oping.
        throw new UnsupportedOperationException("Profile#addQuicHints is not implemented.");
    }
}
