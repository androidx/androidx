/*
 * Copyright 2018 The Android Open Source Project
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

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.annotation.StringDef;

import java.util.ArrayList;
import java.util.concurrent.Executor;

/**
 * Config for {@link ProxyController#setProxyOverride(ProxyConfig, Executor, Runnable)}.
 * <p>
 * Proxy rules should be added using {@code addProxyRule} methods. Multiple rules can be used as
 * fallback if a proxy fails to respond (e.g. the proxy server is down). Bypass rules can be set
 * for URLs that should not use these settings.
 * <p>
 * For instance, the following code means that WebView would first try to use proxy1.com for all
 * URLs, if that fails, proxy2.com, and if that fails, it would make a direct connection.
 * <pre class="prettyprint">
 * ProxyConfig proxyConfig = new ProxyConfig.Builder().addProxyRule("proxy1.com")
 *                                                    .addProxyRule("proxy2.com")
 *                                                    .addProxyRule(ProxyConfig.DIRECT)
 *                                                    .build();
 * </pre>
 * TODO(laisminchillo): unhide this when we're ready to expose this
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class ProxyConfig {
    /**
     * Connect to URLs directly instead of using a proxy server.
     */
    public static final String DIRECT = "direct://";
    /**
     * HTTP scheme.
     */
    public static final String HTTP = "http";
    /**
     * HTTPS scheme.
     */
    public static final String HTTPS = "https";
    /**
     * Matches all schemes.
     */
    public static final String MATCH_ALL_SCHEMES = "*";
    /** @hide */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @StringDef({HTTP, HTTPS, MATCH_ALL_SCHEMES})
    public @interface ProxyScheme {}
    private static final String BYPASS_RULE_LOCAL = "<local>";
    private static final String BYPASS_RULE_LOOPBACK = "<-loopback>";

    private String[][] mProxyRules;
    private String[] mBypassRules;

    /**
     * @hide Internal use only
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public ProxyConfig(String[][] proxyRules, String[] bypassRules) {
        mProxyRules = proxyRules;
        mBypassRules = bypassRules;
    }

    /**
     * @hide Internal use only
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @NonNull
    public String[][] proxyRules() {
        return mProxyRules;
    }

    /**
     * @hide Internal use only
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @NonNull
    public String[] bypassRules() {
        return mBypassRules;
    }

    /**
     * ProxyConfig builder. Use {@link Builder#addProxyRule(String)} or
     * {@link Builder#addProxyRule(String, String)} to add proxy rules. Note that if
     * you don't add any proxy rules, all connections will be made directly. Use
     * {@link Builder#addBypassRule(String)} to add bypass rules. Use
     * {@link Builder#build()} to build this into a {@link ProxyConfig} object.
     */
    public static class Builder {
        private ArrayList<String[]> mProxyRules;
        private ArrayList<String> mBypassRules;

        public Builder() {
            mProxyRules = new ArrayList<>();
            mBypassRules = new ArrayList<>();
        }

        /**
         * Builds the current rules into a ProxyConfig object.
         */
        @NonNull
        public ProxyConfig build() {
            return new ProxyConfig(buildProxyRules(), buildBypassRules());
        }

        /**
         * Adds a proxy to be used for all URLs.
         * <p>Proxy is either {@link ProxyConfig#DIRECT} or a string in the format
         * {@code [scheme://]host[:port]}. Scheme is optional and defaults to HTTP; host is one
         * of an IPv6 literal with brackets, an IPv4 literal or one or more labels separated by
         * a period; port number is optional and defaults to {@code 80} for {@code HTTP},
         * {@code 443} for {@code HTTPS} and {@code 1080} for {@code SOCKS}.
         * <p>
         * The correct syntax for hosts is defined by
         * <a  href="https://tools.ietf.org/html/rfc3986#section-3.2.2">RFC 3986</a>
         * <p>
         * Host examples:
         * <table>
         * <tr><th> Type </th> <th> Example </th></tr>
         * <tr><td> IPv4 literal</td> <td> 192.168.1.1 </td></tr>
         * <tr><td> IPv6 literal with brackets</td> <td> [10:20:30:40:50:60:70:80] </td></tr>
         * <tr><td> Labels </td> <td> example.com </td></tr>
         * </table>
         * <p>
         * Proxy URL examples:
         * <table>
         * <tr><th> Scheme </th> <th> Host </th> <th> Port </th> <th> Proxy URL </th></tr>
         * <tr><td></td> <td>example.com</td> <td></td> <td>example.com</td> </tr>
         * <tr><td>https</td> <td>example.com</td> <td></td> <td>https://example.com</td> </tr>
         * <tr><td></td> <td>example.com</td> <td>1111</td> <td>example.com:1111</td> </tr>
         * <tr><td>https</td> <td>example.com</td> <td>1111</td> <td>https://example.com:1111</td> </tr>
         * <tr><td></td> <td>192.168.1.1</td> <td></td> <td>192.168.1.1</td> </tr>
         * <tr><td></td> <td>192.168.1.1</td> <td>2020</td> <td>192.168.1.1:2020</td> </tr>
         * <tr><td></td> <td>[10:20:30:40:50:60:70:80]</td>
         * <td></td> <td>[10:20:30:40:50:60:70:80]</td> </tr>
         * </table>
         *
         * @param proxyUrl Proxy URL
         */
        @NonNull
        public Builder addProxyRule(@NonNull String proxyUrl) {
            return addProxyRule(proxyUrl, MATCH_ALL_SCHEMES);
        }

        /**
         * This does everything that {@link Builder#addProxyRule(String)} does,
         * but only applies to URLs using {@code schemeFilter}. Scheme filter must be one of
         * {@link ProxyConfig#HTTP}, {@link ProxyConfig#HTTPS} or
         * {@link ProxyConfig#MATCH_ALL_SCHEMES}.
         *
         * @param proxyUrl Proxy URL
         * @param schemeFilter Scheme filter
         */
        @NonNull
        public Builder addProxyRule(@NonNull String proxyUrl,
                @NonNull @ProxyScheme String schemeFilter) {
            String[] rule = {schemeFilter, proxyUrl};
            mProxyRules.add(rule);
            return this;
        }

        /**
         * Adds a new bypass rule that describes URLs that should skip proxy override settings
         * and make a direct connection instead. Wildcards are accepted. For instance, the rule
         * {@code "*example.com"} would mean that requests to {@code "http://example.com"} and
         * {@code "www.example.com"} would not be directed to any proxy, instead, would be made
         * directly to the origin specified by the URL.
         *
         * @param bypassRule Rule to be added to the exclusion list
         */
        @NonNull
        public Builder addBypassRule(@NonNull String bypassRule) {
            mBypassRules.add(bypassRule);
            return this;
        }

        /**
         * Matches hostnames without a period in them (and are not IP literals).
         */
        @NonNull
        public Builder doNotProxyLocalNetworkRequests() {
            return addBypassRule(BYPASS_RULE_LOCAL);
        }

        /**
         * Subtracts the implicit proxy bypass rules (localhost and link local addresses), so they
         * are no longer bypassed.
         */
        @NonNull
        public Builder doProxyLoopbackRequests() {
            return addBypassRule(BYPASS_RULE_LOOPBACK);
        }

        @NonNull
        private String[][] buildProxyRules() {
            return mProxyRules.toArray(new String[0][]);
        }

        @NonNull
        private String[] buildBypassRules() {
            return mBypassRules.toArray(new String[mBypassRules.size()]);
        }
    }
}
