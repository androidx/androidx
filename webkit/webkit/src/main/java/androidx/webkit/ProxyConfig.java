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
import androidx.annotation.RequiresFeature;
import androidx.annotation.RestrictTo;
import androidx.annotation.StringDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.Collections;
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
 *                                                    .addDirect()
 *                                                    .build();
 * </pre>
 */
public final class ProxyConfig {
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
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @StringDef({MATCH_HTTP, MATCH_HTTPS, MATCH_ALL_SCHEMES})
    @Retention(RetentionPolicy.SOURCE)
    public @interface ProxyScheme {}
    private static final String DIRECT = "direct://";
    private static final String BYPASS_RULE_SIMPLE_NAMES = "<local>";
    private static final String BYPASS_RULE_REMOVE_IMPLICIT = "<-loopback>";

    private List<ProxyRule> mProxyRules;
    private List<String> mBypassRules;
    private boolean mReverseBypass;

    /**
     * @hide Internal use only
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public ProxyConfig(@NonNull List<ProxyRule> proxyRules, @NonNull List<String> bypassRules,
            boolean reverseBypass) {
        mProxyRules = proxyRules;
        mBypassRules = bypassRules;
        mReverseBypass = reverseBypass;
    }

    /**
     * Returns the current list of proxy rules. Each {@link ProxyRule} object
     * holds the proxy URL and the URL schemes for which this proxy is used (one of
     * {@code MATCH_HTTP}, {@code MATCH_HTTPS}, {@code MATCH_ALL_SCHEMES}).
     *
     * <p>To add new rules use {@link Builder#addProxyRule(String)} or
     * {@link Builder#addProxyRule(String, String)}.
     *
     * @return List of proxy rules
     */
    @NonNull
    public List<ProxyRule> getProxyRules() {
        return Collections.unmodifiableList(mProxyRules);
    }

    /**
     * Returns the current list that holds the bypass rules represented by this object.
     *
     * <p>To add new rules use {@link Builder#addBypassRule(String)}.
     *
     * @return List of bypass rules
     */
    @NonNull
    public List<String> getBypassRules() {
        return Collections.unmodifiableList(mBypassRules);
    }

    /**
     * Returns {@code true} if reverse bypass is enabled. Reverse bypass means that only URLs in the
     * bypass list will use these proxy settings. {@link #getBypassRules()} returns the URL list.
     *
     * <p>See {@link Builder#setReverseBypassEnabled(boolean)} for a more detailed description.
     *
     * @return reverseBypass
     *
     */
    public boolean isReverseBypassEnabled() {
        return mReverseBypass;
    }

    /**
     * Class that holds a scheme filter and a proxy URL.
     */
    public static final class ProxyRule {
        private String mSchemeFilter;
        private String mUrl;

        /**
         * @hide Internal use only
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        public ProxyRule(@NonNull String schemeFilter, @NonNull String url) {
            mSchemeFilter = schemeFilter;
            mUrl = url;
        }

        /**
         * @hide Internal use only
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        public ProxyRule(@NonNull String url) {
            this(ProxyConfig.MATCH_ALL_SCHEMES, url);
        }

        /**
         * Returns the {@link String} that represents the scheme filter for this object.
         *
         * @return Scheme filter
         */
        @NonNull
        public String getSchemeFilter() {
            return mSchemeFilter;
        }

        /**
         * Returns the {@link String} that represents the proxy URL for this object.
         *
         * @return Proxy URL
         */
        @NonNull
        public String getUrl() {
            return mUrl;
        }
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
        private List<ProxyRule> mProxyRules;
        private List<String> mBypassRules;
        private boolean mReverseBypass = false;

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
            mProxyRules = proxyConfig.getProxyRules();
            mBypassRules = proxyConfig.getBypassRules();
            mReverseBypass = proxyConfig.isReverseBypassEnabled();
        }

        /**
         * Builds the current rules into a ProxyConfig object.
         *
         * @return The ProxyConfig object represented by this Builder
         */
        @NonNull
        public ProxyConfig build() {
            return new ProxyConfig(proxyRules(), bypassRules(), reverseBypass());
        }

        /**
         * Adds a proxy to be used for all URLs. This method can be called multiple times to add
         * multiple rules. Additional rules have decreasing precedence.
         * <p>Proxy is a string in the format {@code [scheme://]host[:port]}. Scheme is optional, if
         * present must be {@code HTTP}, {@code HTTPS} or
         * <a href="https://tools.ietf.org/html/rfc1928">SOCKS</a> and defaults to {@code HTTP}.
         * Host is one of an IPv6 literal with brackets, an IPv4 literal or one or more labels
         * separated by a period. Port number is optional and defaults to {@code 80} for
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
         * @return This Builder object
         */
        @NonNull
        public Builder addProxyRule(@NonNull String proxyUrl) {
            mProxyRules.add(new ProxyRule(proxyUrl));
            return this;
        }

        /**
         * This does everything that {@link Builder#addProxyRule(String)} does,
         * but only applies to URLs using {@code schemeFilter}. Scheme filter must be one of
         * {@link ProxyConfig#MATCH_HTTP}, {@link ProxyConfig#MATCH_HTTPS} or
         * {@link ProxyConfig#MATCH_ALL_SCHEMES}.
         *
         * @param proxyUrl Proxy URL
         * @param schemeFilter Scheme filter
         * @return This Builder object
         */
        @NonNull
        public Builder addProxyRule(@NonNull String proxyUrl,
                @NonNull @ProxyScheme String schemeFilter) {
            mProxyRules.add(new ProxyRule(schemeFilter, proxyUrl));
            return this;
        }

        /**
         * Adds a new bypass rule that describes URLs that should skip proxy override settings
         * and make a direct connection instead. These can be URLs or IP addresses. Wildcards are
         * accepted. For instance, the rule {@code "*example.com"} would mean that requests to
         * {@code "http://example.com"} and {@code "www.example.com"} would not be directed to any
         * proxy, instead, would be made directly to the origin specified by the URL.
         *
         * @param bypassRule Rule to be added to the exclusion list
         * @return This Builder object
         */
        @NonNull
        public Builder addBypassRule(@NonNull String bypassRule) {
            mBypassRules.add(bypassRule);
            return this;
        }

        /**
         * Adds a proxy rule so URLs that match the scheme filter are connected to directly instead
         * of using a proxy server.
         *
         * @param schemeFilter Scheme filter
         * @return This Builder object
         */
        @NonNull
        public Builder addDirect(@NonNull @ProxyScheme String schemeFilter) {
            mProxyRules.add(new ProxyRule(schemeFilter, DIRECT));
            return this;
        }

        /**
         * Adds a proxy rule so URLs are connected to directly instead of using a proxy server.
         *
         * @return This Builder object
         */
        @NonNull
        public Builder addDirect() {
            return addDirect(MATCH_ALL_SCHEMES);
        }

        /**
         * Hostnames without a period in them (and that are not IP literals) will skip proxy
         * settings and be connected to directly instead. Examples: {@code "abc"}, {@code "local"},
         * {@code "some-domain"}.
         * <p>
         * Hostnames with a trailing dot are not considered simple by this definition.
         *
         * @return This Builder object
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
         *
         * @return This Builder object
         */
        @NonNull
        public Builder removeImplicitRules() {
            return addBypassRule(BYPASS_RULE_REMOVE_IMPLICIT);
        }

        /**
         * Reverse the bypass list.
         *
         * <p>The default value is {@code false}, in which case all URLs will use proxy settings
         * except the ones in the bypass list, which will be connected to directly instead.
         *
         * <p>If set to {@code true}, then only URLs in the bypass list will use these proxy
         * settings, and all other URLs will be connected to directly.
         *
         * <p>Use {@link #addBypassRule(String)} to add bypass rules.
         *
         * <p>This method should only be called if
         * {@link WebViewFeature#isFeatureSupported(String)}
         * returns {@code true} for {@link WebViewFeature#PROXY_OVERRIDE_REVERSE_BYPASS}.
         *
         * @return This Builder object
         */
        @RequiresFeature(name = WebViewFeature.PROXY_OVERRIDE_REVERSE_BYPASS,
                enforcement = "androidx.webkit.WebViewFeature#isFeatureSupported")
        @NonNull
        public Builder setReverseBypassEnabled(boolean reverseBypass) {
            mReverseBypass = reverseBypass;
            return this;
        }

        @NonNull
        private List<ProxyRule> proxyRules() {
            return mProxyRules;
        }

        @NonNull
        private List<String> bypassRules() {
            return mBypassRules;
        }

        private boolean reverseBypass() {
            return mReverseBypass;
        }
    }
}
