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

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;

/**
 * Config for {@link ProxyController#setProxyOverride(ProxyConfig, Executor, Runnable)}.
 * <p>
 * Proxy rules should be added using {@code addProxyRule} methods. Multiple rules can be used as
 * fallback if a proxy fails to respond (for example, the proxy server is down). Bypass rules can
 * be set for URLs that should not use these settings.
 * <p>
 * For instance, the following code means that WebView would first try to use {@code proxy1.com}
 * for all URLs, if that fails, {@code proxy2.com}, and if that fails, it would make a direct
 * connection.
 * <pre class="prettyprint">
 * ProxyConfig proxyConfig = new ProxyConfig.Builder().addProxyRule("proxy1.com")
 *                                                    .addProxyRule("proxy2.com")
 *                                                    .addProxyRule(ProxyConfig.DIRECT)
 *                                                    .build();
 * </pre>
 */
public class ProxyConfig {
    /**
     * Connect to URLs directly instead of using a proxy server.
     */
    public static final String DIRECT = "direct://";
    /**
     * HTTP scheme.
     */
    public static final String MATCH_HTTP = "http";
    /**
     * HTTPS scheme.
     */
    public static final String MATCH_HTTPS = "https";
    /**
     * Matches all schemes.
     */
    public static final String MATCH_ALL_SCHEMES = "*";
    /** @hide */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    @StringDef({MATCH_HTTP, MATCH_HTTPS, MATCH_ALL_SCHEMES})
    @Retention(RetentionPolicy.SOURCE)
    public @interface ProxyScheme {}
    private static final String BYPASS_RULE_SIMPLE_NAMES = "<local>";
    private static final String BYPASS_RULE_SUBTRACT_IMPLICIT = "<-loopback>";

    private List<String[]> mProxyRules;
    private List<String> mBypassRules;

    /**
     * @hide Internal use only
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    public ProxyConfig(List<String[]> proxyRules, List<String> bypassRules) {
        mProxyRules = proxyRules;
        mBypassRules = bypassRules;
    }

    /**
     * @hide Internal use only
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    @NonNull
    public List<String[]> proxyRules() {
        return mProxyRules;
    }

    /**
     * @hide Internal use only
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    @NonNull
    public List<String> bypassRules() {
        return mBypassRules;
    }

    /**
     * ProxyConfig builder. Use {@link Builder#addProxyRule(String)} or
     * {@link Builder#addProxyRule(String, String)} to add proxy rules. Use
     * {@link Builder#addBypassRule(String)} to add bypass rules. Use {@link Builder#build()} to
     * build this into a {@link ProxyConfig} object.
     *
     * <p class="note"><b>Note:</b> applying a {@code ProxyConfig} with no rules will cause all
     * connections to be made directly.
     */
    public static final class Builder {
        private List<String[]> mProxyRules;
        private List<String> mBypassRules;

        /**
         * Create an empty ProxyConfig Builder.
         */
        public Builder() {
            mProxyRules = new ArrayList<>();
            mBypassRules = new ArrayList<>();
        }

        /**
         * Create a ProxyConfig Builder from an existing ProxyConfig object.
         */
        public Builder(@NonNull ProxyConfig proxyConfig) {
            mProxyRules = proxyConfig.proxyRules();
            mBypassRules = proxyConfig.bypassRules();
        }

        /**
         * Builds the current rules into a ProxyConfig object.
         */
        @NonNull
        public ProxyConfig build() {
            return new ProxyConfig(proxyRules(), bypassRules());
        }

        /**
         * Adds a proxy to be used for all URLs.
         * <p>Proxy is either {@link ProxyConfig#DIRECT} or a string in the format
         * {@code [scheme://]host[:port]}. Scheme is optional, if present must be {@code HTTP},
         * {@code HTTPS} or <a href="https://tools.ietf.org/html/rfc1928">SOCKS</a> and defaults to
         * {@code HTTP}. Host is one of an IPv6 literal with brackets, an IPv4 literal or one or
         * more labels separated by a period. Port number is optional and defaults to {@code 80} for
         * {@code HTTP}, {@code 443} for {@code HTTPS} and {@code 1080} for {@code SOCKS}.
         * <p>
         * The correct syntax for hosts is defined by
         * <a href="https://tools.ietf.org/html/rfc3986#section-3.2.2">RFC 3986</a>
         * <p>
         * Examples:
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
         * {@link ProxyConfig#MATCH_HTTP}, {@link ProxyConfig#MATCH_HTTPS} or
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
         * Hostnames without a period in them (and that are not IP literals) will skip proxy
         * settings and be connected to directly instead. Examples: {@code "abc"}, {@code "local"},
         * {@code "some-domain"}.
         * <p>
         * Hostnames with a trailing dot are not considered simple by this definition.
         */
        @NonNull
        public Builder bypassSimpleHostnames() {
            return addBypassRule(BYPASS_RULE_SIMPLE_NAMES);
        }

        /**
         * By default, certain hostnames implicitly bypass the proxy if they are link-local IPs, or
         * localhost addresses. For instance hostnames matching any of (non-exhaustive list):
         * <ul>
         * <li>localhost</li>
         * <li>*.localhost</li>
         * <li>[::1]</li>
         * <li>127.0.0.1/8</li>
         * <li>169.254/16</li>
         * <li>[FE80::]/10</li>
         * </ul>
         * <p>
         * Call this function to override the default behavior and force localhost and link-local
         * URLs to be sent through the proxy.
         */
        @NonNull
        public Builder subtractImplicitRules() {
            return addBypassRule(BYPASS_RULE_SUBTRACT_IMPLICIT);
        }

        @NonNull
        private List<String[]> proxyRules() {
            return mProxyRules;
        }

        @NonNull
        private List<String> bypassRules() {
            return mBypassRules;
        }
    }
}
