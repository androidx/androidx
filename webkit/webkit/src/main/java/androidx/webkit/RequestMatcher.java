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

import androidx.annotation.AnyThread;
import androidx.annotation.RestrictTo;

import org.chromium.support_lib_boundary.RequestMatcherConfig;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;

/**
 * Matches network requests made by WebView.
 *
 * <p>Allows defining request filters for APIs such as
 * {@link Profile#addHeaderInterceptor(RequestMatcher, HeaderInterceptor)} and
 * {@link WebViewCompat#addHeaderInterceptor(android.webkit.WebView, RequestMatcher, HeaderInterceptor)}.
 *
 * @see #forUrlPatterns(Set)
 * @see #forAnyUrl()
 */
@AnyThread
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public final class RequestMatcher {
    private static final String ANY_URL_PATTERN = "*://*:*";

    private final @NonNull List<@NonNull String> mUrlPatterns;

    private RequestMatcher(@NonNull Collection<@NonNull String> urlPatterns) {
        mUrlPatterns = Collections.unmodifiableList(new ArrayList<>(urlPatterns));
    }

    /**
     * Creates a {@link RequestMatcher} that matches requests whose URL matches at least one of the
     * provided URL patterns, as defined by
     * <a href="https://urlpattern.spec.whatwg.org/">the URL Pattern specification</a>.
     *
     * <p>Each pattern follows the same syntax rules as the single-argument
     * {@code URLPattern(input)} constructor (without a base URL). See
     * <a href="https://developer.mozilla.org/en-US/docs/Web/API/URL_Pattern_API">the URL Pattern
     * API documentation</a> for full syntax details. In particular:
     * <ul>
     * <li>Patterns must be absolute (they must include a protocol component such as
     * {@code https://}, {@code http://}, or {@code *://}).</li>
     * <li>Matching is case-sensitive for path and query components.</li>
     * <li>Trailing components omitted from a pattern (such as path, query, or fragment) default to
     * wildcards, except that specifying a hostname without an explicit port matches only the
     * scheme's default port (e.g. {@code 443} for {@code https} and {@code 80} for {@code http};
     * append {@code :*} to match any port).</li>
     * <li>Custom regular expression groups (such as {@code (\d+)}) are not supported.</li>
     * </ul>
     *
     * <p>Common URL pattern examples:
     * <ul>
     * <li><b>Match any path on an exact domain:</b> {@code "https://example.com"} — matches any
     * path and query on {@code https://example.com} (for example, {@code "https://example.com/"}
     * and {@code "https://example.com/foo/bar"}), but not subdomains such as
     * {@code "https://sub.example.com/"}.</li>
     * <li><b>Match any path on a domain or its subdomains:</b>
     * {@code "https://{*.}?example.com"} — matches any path and query on {@code example.com} as
     * well as subdomains of any depth (for example, {@code "https://example.com/page"},
     * {@code "https://sub.example.com/page"}, and {@code "https://a.b.example.com/page"}).</li>
     * <li><b>Match an exact path:</b> {@code "https://www.example.com/path"} — matches requests to
     * the exact path {@code /path} with any query string (for example,
     * {@code "https://www.example.com/path"} and {@code "https://www.example.com/path?foo=bar"}),
     * but not subpaths such as {@code "https://www.example.com/path/subpath"}.</li>
     * <li><b>Match a path or subpath:</b> {@code "https://www.example.com/api{/*}?"} — matches both
     * {@code /api} and any subpath under {@code /api/} (for example,
     * {@code "https://www.example.com/api"}, {@code "https://www.example.com/api/"}, and
     * {@code "https://www.example.com/api/v1/items"}), without matching sibling paths such as
     * {@code "https://www.example.com/apiv2"}.</li>
     * <li><b>Match a query parameter:</b>
     * {@code "https://www.example.com/path?{*&}?foo=bar{&*}?"} — matches {@code /path} requests
     * whose query string contains the exact parameter {@code foo=bar} at any position (for example,
     * {@code "?foo=bar"} or {@code "?a=1&foo=bar&b=2"}), without matching partial parameter names
     * or values such as {@code "?myfoo=bar2"}.</li>
     * </ul>
     *
     * <p>Caveats to keep in mind when writing URL patterns:
     * <ul>
     * <li><b>Trailing slashes in paths:</b> Omitting a pathname ({@code "https://example.com"})
     * matches any path, whereas {@code "https://example.com/"} matches only the root path
     * ({@code "https://example.com"} and {@code "https://example.com/"}, not
     * {@code "https://example.com/foo"}). Similarly, {@code "https://example.com/foo"} does not
     * match {@code "https://example.com/foo/"}, and {@code "https://example.com/foo/*"} matches
     * {@code "https://example.com/foo/"} and {@code "https://example.com/foo/bar"} but does
     * <em>not</em> match {@code "https://example.com/foo"} (use
     * {@code "https://example.com/foo{/*}?"} to match both).</li>
     * <li><b>Subdomains:</b> {@code "https://*.example.com"} matches subdomains (such as
     * {@code "https://sub.example.com"} and {@code "https://a.b.example.com"}), but does
     * <em>not</em> match the bare domain {@code "https://example.com"} (use
     * {@code "https://{*.}?example.com"} to match both).</li>
     * <li><b>Non-default ports:</b> Specifying a hostname (including {@code *}) without an explicit
     * port matches only the scheme's default port. For example, {@code "https://example.com"} and
     * {@code "*://*"} do not match {@code "https://example.com:8443"}; append {@code :*} (such as
     * {@code "https://example.com:*"} or {@code "*://*:*"}) to match any port.</li>
     * <li><b>Wildcards and protocols:</b> A standalone {@code "*"} is invalid because a protocol
     * must be specified (use {@link #forAnyUrl()} or {@code "*://*:*"} instead). Patterns without
     * {@code //*} (such as {@code "*:"} or {@code "https:"}) match only an empty hostname and will
     * not match URLs with a host such as {@code "https://example.com"}.</li>
     * <li><b>Query string wildcards:</b> Query strings do not treat {@code &} as a segment
     * delimiter, so {@code "https://www.example.com/path?*foo=bar*"} also matches partial parameter
     * names or values such as {@code "?myfoo=bar2"}. Anchor on {@code &} using
     * {@code "?{*&}?foo=bar{&*}?"} to match an exact query parameter.</li>
     * </ul>
     *
     * <p>Note that syntax validation of the pattern strings is performed by WebView when the
     * {@link RequestMatcher} is registered (for example, in
     * {@link Profile#addHeaderInterceptor(RequestMatcher, HeaderInterceptor)}).
     *
     * @param urlPatterns A non-empty set of URL pattern strings. A request matches if it matches
     *                    any of the provided patterns.
     * @return A {@link RequestMatcher} matching any of the given URL patterns.
     * @throws IllegalArgumentException if {@code urlPatterns} is empty.
     */
    public static @NonNull RequestMatcher forUrlPatterns(
            @NonNull Set<@NonNull String> urlPatterns) {
        if (urlPatterns.isEmpty()) {
            throw new IllegalArgumentException("At least one URL pattern must be provided");
        }
        return new RequestMatcher(urlPatterns);
    }

    /**
     * Creates a {@link RequestMatcher} that matches any URL regardless of scheme, host, port, path,
     * or query parameters.
     *
     * <p>For performance and security reasons, prefer {@link #forUrlPatterns(Set)} with specific
     * URL patterns whenever possible so WebView only invokes interceptors on relevant, trusted
     * requests.
     *
     * @return A {@link RequestMatcher} that matches all URLs.
     */
    public static @NonNull RequestMatcher forAnyUrl() {
        return new RequestMatcher(Collections.singletonList(ANY_URL_PATTERN));
    }

    /**
     * Serializes the {@link RequestMatcher} configuration to a {@link BiConsumer} for use with the
     * support library glue layer.
     *
     * <p>This method is for internal library use only.
     *
     * @param chromiumConfig The {@link BiConsumer} to serialize the config to.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public void serializeToBoundaryLayer(
            @NonNull BiConsumer<@RequestMatcherConfig Integer, Object> chromiumConfig) {
        chromiumConfig.accept(RequestMatcherConfig.URL_PATTERNS, mUrlPatterns);
    }
}
