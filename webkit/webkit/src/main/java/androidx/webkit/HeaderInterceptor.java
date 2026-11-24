/*
 * Copyright 2026 The Android Open Source Project
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

import android.webkit.WebResourceRequest;

import androidx.annotation.RestrictTo;
import androidx.annotation.WorkerThread;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Intercepts WebView requests to dynamically set or remove HTTP request headers.
 *
 * <p>A {@link HeaderInterceptor} can be registered for an entire {@link Profile} via
 * {@link Profile#addHeaderInterceptor(RequestMatcher, HeaderInterceptor)} or for an individual
 * {@link android.webkit.WebView} via
 * {@link WebViewCompat#addHeaderInterceptor(android.webkit.WebView, RequestMatcher, HeaderInterceptor)}.
 *
 * <h3>Intercepted Requests</h3>
 * <p>Interceptors are invoked for requests matching the associated {@link RequestMatcher} across
 * all HTTP methods (such as {@code GET} and {@code POST}), including:
 * <ul>
 * <li>Main-frame document navigations.</li>
 * <li>Subframe ({@code iframe}) document navigations.</li>
 * <li>Subresource requests (such as scripts, stylesheets, images, {@code fetch()}, and
 * {@code XMLHttpRequest}).</li>
 * <li>Page-initiated speculative loading requests (such as via the Speculation Rules API).</li>
 * <li>Intermediate HTTP redirects: each redirect hop is evaluated independently against the
 * {@link RequestMatcher}. Headers added by an interceptor on a previous hop are <em>not</em>
 * carried over to subsequent redirect hops.</li>
 * </ul>
 * <p>Interceptors do not apply to WebSocket requests.
 * <p>The scope of intercepted requests depends on where the interceptor is registered:
 * <ul>
 * <li><b>{@link Profile}-level ({@link Profile#addHeaderInterceptor}):</b> Invoked for matching
 * requests across all {@link android.webkit.WebView} instances using that {@link Profile}, as well
 * as {@link Profile}-scoped requests not tied to a specific {@link android.webkit.WebView} (such as
 * Service Worker network requests and application-initiated prefetch requests via
 * {@link Profile#getPrefetchCache()}).</li>
 * <li><b>{@link android.webkit.WebView}-level ({@link WebViewCompat#addHeaderInterceptor}):</b>
 * Invoked only for matching requests originating from that specific {@link android.webkit.WebView}
 * instance. Requests not tied to a {@link android.webkit.WebView} (such as Service Worker requests
 * and {@link Profile#getPrefetchCache()} requests) are not intercepted.</li>
 * </ul>
 *
 * <h3>Threading</h3>
 * <p>{@link #interceptRequest(WebResourceRequest)} is invoked by WebView on background (non-UI)
 * worker threads. Calls are not confined to a single thread and may be invoked in parallel from
 * multiple worker threads for different requests, so implementations must be thread-safe.
 * Implementations may block the calling thread while computing header updates without blocking the
 * application's UI thread; however, the intercepted request itself will remain blocked until
 * {@link #interceptRequest} returns.
 *
 * <h3>Error Handling</h3>
 * <p>Implementations of {@link HeaderInterceptor} must handle exceptions gracefully. Uncaught
 * exceptions thrown from {@link #interceptRequest(WebResourceRequest)} are not caught by WebView
 * and will crash the application.
 *
 * <h3>Caching Semantics</h3>
 * <p>Requests are intercepted <em>before</em> HTTP cache eligibility is evaluated. Consequently:
 * <ul>
 * <li>Headers added or removed by a {@link HeaderInterceptor} are taken into account when
 * evaluating cache reuse (for example, against a cached response's {@code Vary} header). If an
 * interceptor changes the value of a header listed in {@code Vary}, WebView will bypass the cached
 * entry and issue a network request with the updated headers.</li>
 * <li>Because interception occurs prior to cache lookup, {@link #interceptRequest} is invoked even
 * for requests that are ultimately served from the HTTP cache without hitting the network.</li>
 * </ul>
 *
 * <h3>Allowed Header Modifications</h3>
 * <p>Applications may set or remove custom HTTP request headers (for example,
 * {@code X-My-Header}) using valid HTTP header field names and values. Attempts to set or remove
 * <a href="https://fetch.spec.whatwg.org/#forbidden-request-header">forbidden request headers</a>
 * (such as {@code Cookie}, {@code Host}, {@code Origin}, {@code Connection}, {@code Sec-*}, or
 * {@code Proxy-*}) or headers with invalid names or values are ignored by WebView.
 * <p>In addition, certain standard headers managed by WebView are populated or updated later in the
 * network pipeline and may overwrite modifications returned by an interceptor. Because the exact
 * set of WebView-managed headers is unspecified and may change across WebView versions, this API
 * should only be used to manage custom headers rather than overriding standard headers set by
 * WebView.
 *
 * <h3>Call Order</h3>
 * <p>When multiple mechanisms modify headers on the same request, updates are applied in the
 * following order, where later stages observe and can override or remove custom headers set by
 * earlier stages:
 * <ol>
 * <li>Headers provided with the request itself (such as via
 * {@link android.webkit.WebView#loadUrl(String, java.util.Map)}, {@link WebViewCompat#navigate},
 * or JavaScript {@code fetch()}/{@code XMLHttpRequest}) and declarative headers configured via
 * {@link Profile#addCustomHeader(CustomHeader)}.</li>
 * <li>{@link Profile}-level {@link HeaderInterceptor}s registered via
 * {@link Profile#addHeaderInterceptor}, in the order they were registered.</li>
 * <li>{@link android.webkit.WebView}-level {@link HeaderInterceptor}s registered via
 * {@link WebViewCompat#addHeaderInterceptor}, in the order they were registered.</li>
 * </ol>
 * <p>{@link Profile}-level interceptors always run before {@link android.webkit.WebView}-level
 * interceptors, even if a {@link Profile}-level interceptor was registered after a
 * {@link android.webkit.WebView}-level interceptor.
 * <p>All {@link HeaderInterceptor}s run before
 * {@link android.webkit.WebViewClient#shouldInterceptRequest(android.webkit.WebView, WebResourceRequest)}
 * and {@link android.webkit.ServiceWorkerClient#shouldInterceptRequest(WebResourceRequest)}. Header
 * additions, overrides, and removals made through this API will be reflected in the map returned
 * by {@link WebResourceRequest#getRequestHeaders()} in {@code shouldInterceptRequest}.
 *
 * <h3>Headers Visible in {@link WebResourceRequest}</h3>
 * <p>Calling {@link WebResourceRequest#getRequestHeaders()} on the request passed to
 * {@link #interceptRequest} returns headers attached prior to the current interceptor, including:
 * <ul>
 * <li>Headers supplied via {@link android.webkit.WebView#loadUrl(String, java.util.Map)} or
 * {@link WebViewCompat#navigate}.</li>
 * <li>Headers set by web content (such as via {@code fetch()} or {@code XMLHttpRequest}).</li>
 * <li>Declarative headers added via {@link Profile#addCustomHeader(CustomHeader)}.</li>
 * <li>Header updates applied by earlier {@link HeaderInterceptor}s invoked for the same
 * request.</li>
 * </ul>
 * <p>Headers populated later by the network stack (such as {@code Cookie}), as well as headers
 * attached by an interceptor on a prior redirect hop or a prior cached load, are not included.
 *
 * <h3>History Navigations and State Restoration</h3>
 * <p>Header modifications returned by a {@link HeaderInterceptor} are <em>not</em> persisted in
 * navigation history entries or in state bundles saved via {@link android.webkit.WebView#saveState}
 * or {@link WebViewCompat#saveState}. On back/forward navigations or state restoration, previous
 * interceptor updates are not pre-populated on the request; instead, registered
 * {@link HeaderInterceptor}s are invoked again so the application can compute headers for the new
 * request.
 *
 * <h3>Comparison with {@link Profile#addCustomHeader(CustomHeader)}</h3>
 * <p>{@link Profile#addCustomHeader(CustomHeader)} registers static name-value headers directly
 * inside WebView's native network layer without invoking Java callbacks per request, and currently
 * matches on origin rather than on the full request URL. By contrast, {@link HeaderInterceptor}
 * invokes application code for every matching request, allowing dynamic per-request values (such as
 * timestamps or request IDs), header removals, matching on the full request URL (including path and
 * query parameters) via {@link RequestMatcher}, and per-WebView scoping.
 *
 * <h3>Caveats</h3>
 * <ul>
 * <li><b>When registration and unregistration take effect:</b> Registering a
 * {@link HeaderInterceptor} or unregistering one via {@link HeaderInterceptorHandle#unregister()}
 * takes effect on document load. For document-initiated requests (such as subresources,
 * {@code fetch()}, and {@code XMLHttpRequest}), changes only apply to documents that begin loading
 * after the method is called; they do not affect requests initiated by documents that are already
 * loaded. Similarly, for requests sent from Service Workers (when registered on a {@link Profile}),
 * changes only apply to Service Workers that are started after the method is called; they do not
 * affect requests sent from Service Workers that are already running. In particular, this means a
 * {@link HeaderInterceptor} may continue to be called for requests from already-loaded documents
 * (or already-running Service Workers) even after {@link HeaderInterceptorHandle#unregister()} has
 * been called.</li>
 * <li><b>Shared HTTP cache across a {@link Profile}:</b> HTTP caching is shared across all
 * {@link android.webkit.WebView} instances belonging to the same {@link Profile}. If a
 * {@link android.webkit.WebView}-specific interceptor (registered via
 * {@link WebViewCompat#addHeaderInterceptor}) attaches headers that influence a cached response,
 * another {@link android.webkit.WebView} in the same {@link Profile} may receive that cached
 * response unless prevented by response caching headers (such as {@code Vary} or
 * {@code Cache-Control}) or other mechanisms that clear or invalidate the cache.</li>
 * <li><b>Service Workers and {@link Profile} prefetching:</b> Interceptors registered via
 * {@link WebViewCompat#addHeaderInterceptor} are not called for Service Worker requests or
 * application-initiated prefetch requests via {@link Profile#getPrefetchCache()}, because those
 * requests are not bound to any {@link android.webkit.WebView}.</li>
 * <li><b>Request latency:</b> Because matching requests wait for {@link #interceptRequest} to
 * complete before cache lookup and network transmission, expensive work or overly broad
 * {@link RequestMatcher} patterns can increase navigation and subresource latency.</li>
 * </ul>
 *
 * <h3>Recommendations</h3>
 * <ul>
 * <li><b>Prefer {@link Profile#addCustomHeader(CustomHeader)} whenever possible:</b> For static
 * headers that do not vary per request and only need origin-level scoping, prefer
 * {@link Profile#addCustomHeader(CustomHeader)} for better performance, as it avoids per-request
 * cross-thread callbacks into application code.</li>
 * <li><b>Prefer {@link Profile#addHeaderInterceptor} over
 * {@link WebViewCompat#addHeaderInterceptor}:</b> Registering interceptors on {@link Profile}
 * ensures consistent behavior across HTTP caching, Service Workers, and
 * {@link Profile#getPrefetchCache()}. Only use {@link WebViewCompat#addHeaderInterceptor} when
 * multiple {@link android.webkit.WebView} instances must share a single {@link Profile} while
 * sending different dynamic headers, and the shared HTTP cache caveats do not apply to your use
 * case.</li>
 * <li><b>Register on initialization and avoid unregistering:</b> Due to the caveat of when
 * registration and unregistration take effect, prefer registering {@link HeaderInterceptor}s when
 * the {@link Profile} or {@link android.webkit.WebView} is initialized and avoid unregistering
 * them.</li>
 * <li><b>Keep {@link RequestMatcher} patterns specific:</b> Scope interceptors to the narrowest
 * URL patterns needed via {@link RequestMatcher#forUrlPatterns(java.util.Set)} rather than
 * {@link RequestMatcher#forAnyUrl()}.</li>
 * </ul>
 *
 * @see Profile#addHeaderInterceptor(RequestMatcher, HeaderInterceptor)
 * @see WebViewCompat#addHeaderInterceptor(
 *         android.webkit.WebView, RequestMatcher, HeaderInterceptor)
 * @see Profile#addCustomHeader(CustomHeader)
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public interface HeaderInterceptor {
    /**
     * Intercepts the provided {@link WebResourceRequest} and returns header modifications to apply.
     *
     * <p>This method is called by WebView on a background thread; see the top-level
     * {@link HeaderInterceptor} documentation for more information about the semantics.
     *
     * <p>If this method returns {@code null}, the headers of the request will be left unmodified.
     *
     * @param request The {@link WebResourceRequest} being intercepted.
     * @return The {@link HeaderUpdates} to apply to the request, or {@code null} to leave the
     *     headers of the request unmodified.
     */
    @WorkerThread
    @Nullable HeaderUpdates interceptRequest(@NonNull WebResourceRequest request);
}
