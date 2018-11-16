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

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * Holds tracing configuration information and predefined settings
 * for {@link TracingController}.
 *
 * This class is functionally equivalent to {@link android.webkit.TracingConfig}.
 */
public class TracingConfig {
    /** @hide */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @IntDef(flag = true, value = {CATEGORIES_NONE, CATEGORIES_ALL, CATEGORIES_ANDROID_WEBVIEW,
            CATEGORIES_WEB_DEVELOPER, CATEGORIES_INPUT_LATENCY, CATEGORIES_RENDERING,
            CATEGORIES_JAVASCRIPT_AND_RENDERING, CATEGORIES_FRAME_VIEWER})
    @Retention(RetentionPolicy.SOURCE)
    public @interface PredefinedCategories {}

    /**
     * Indicates that there are no predefined categories.
     */
    public static final int CATEGORIES_NONE = android.webkit.TracingConfig.CATEGORIES_NONE;

    /**
     * Predefined set of categories, includes all categories enabled by default in chromium.
     * Use with caution: this setting may produce large trace output.
     */
    public static final int CATEGORIES_ALL = android.webkit.TracingConfig.CATEGORIES_ALL;

    /**
     * Predefined set of categories typically useful for analyzing WebViews.
     * Typically includes "android_webview" and "Java" categories.
     */
    public static final int CATEGORIES_ANDROID_WEBVIEW =
            android.webkit.TracingConfig.CATEGORIES_ANDROID_WEBVIEW;

    /**
     * Predefined set of categories typically useful for web developers.
     * Typically includes "blink", "compositor", "renderer.scheduler" and "v8" categories.
     */
    public static final int CATEGORIES_WEB_DEVELOPER =
            android.webkit.TracingConfig.CATEGORIES_WEB_DEVELOPER;

    /**
     * Predefined set of categories for analyzing input latency issues.
     * Typically includes "input", "renderer.scheduler" categories..
     */
    public static final int CATEGORIES_INPUT_LATENCY =
            android.webkit.TracingConfig.CATEGORIES_INPUT_LATENCY;

    /**
     * Predefined set of categories for analyzing rendering issues.
     * Typically includes "blink", "compositor" and "gpu" categories.
     */
    public static final int CATEGORIES_RENDERING =
            android.webkit.TracingConfig.CATEGORIES_RENDERING;

    /**
     * Predefined set of categories for analyzing javascript and rendering issues.
     * Typically includes "blink", "compositor", "gpu", "renderer.scheduler" and "v8" categories.
     */
    public static final int CATEGORIES_JAVASCRIPT_AND_RENDERING =
            android.webkit.TracingConfig.CATEGORIES_JAVASCRIPT_AND_RENDERING;

    /**
     * Predefined set of categories for studying difficult rendering performance problems.
     * Typically includes "blink", "compositor", "gpu", "renderer.scheduler", "v8" and
     * some other compositor categories which are disabled by default.
     */
    public static final int CATEGORIES_FRAME_VIEWER =
            android.webkit.TracingConfig.CATEGORIES_FRAME_VIEWER;

    /** @hide */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @IntDef({RECORD_UNTIL_FULL, RECORD_CONTINUOUSLY})
    @Retention(RetentionPolicy.SOURCE)
    public @interface TracingMode {}

    /**
     * Record trace events until the internal tracing buffer is full.
     *
     * Typically the buffer memory usage is larger than {@link #RECORD_CONTINUOUSLY}.
     * Depending on the implementation typically allows up to 256k events to be stored.
     */
    public static final int RECORD_UNTIL_FULL = android.webkit.TracingConfig.RECORD_UNTIL_FULL;

    /**
     * Record trace events continuously using an internal ring buffer. Default tracing mode.
     *
     * Overwrites old events if they exceed buffer capacity. Uses less memory than the
     * {@link #RECORD_UNTIL_FULL} mode. Depending on the implementation typically allows
     * up to 64k events to be stored.
     */
    public static final int RECORD_CONTINUOUSLY = android.webkit.TracingConfig.RECORD_CONTINUOUSLY;

    private @PredefinedCategories int mPredefinedCategories;
    private final List<String> mCustomIncludedCategories = new ArrayList<>();
    private @TracingMode int mTracingMode;

    /** @hide */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public TracingConfig(@PredefinedCategories int predefinedCategories,
                         List<String> customIncludedCategories, @TracingMode int tracingMode) {
        mPredefinedCategories = predefinedCategories;
        mCustomIncludedCategories.addAll(customIncludedCategories);
        mTracingMode = tracingMode;
    }

    /**
     * Returns a bitmask of the predefined category sets of this configuration.
     *
     * @return Bitmask of predefined category sets.
     */
    @PredefinedCategories
    public int getPredefinedCategories() {
        return mPredefinedCategories;
    }

    /**
     * Returns the list of included custom category patterns for this configuration.
     *
     * @return Empty list if no custom category patterns are specified.
     */
    @NonNull
    public List<String> getCustomIncludedCategories() {
        return mCustomIncludedCategories;
    }

    /**
     * Returns the tracing mode of this configuration.
     *
     * @return The tracing mode of this configuration.
     */
    @TracingMode
    public int getTracingMode() {
        return mTracingMode;
    }

    /**
     * Builder used to create {@link TracingConfig} objects.
     * <p>
     * Examples:
     * <pre class="prettyprint">
     *   // Create a configuration with default options: {@link #CATEGORIES_NONE},
     *   // {@link #RECORD_CONTINUOUSLY}.
     *   <code>new TracingConfig.Builder().build()</code>
     *
     *   // Record trace events from the "web developer" predefined category sets.
     *   // Uses a ring buffer (the default {@link #RECORD_CONTINUOUSLY} mode) for
     *   // internal storage during tracing.
     *   <code>new TracingConfig.Builder().addCategories(CATEGORIES_WEB_DEVELOPER).build()</code>
     *
     *   // Record trace events from the "rendering" and "input latency" predefined
     *   // category sets.
     *   <code>new TracingConfig.Builder().addCategories(CATEGORIES_RENDERING,
     *                                     CATEGORIES_INPUT_LATENCY).build()</code>
     *
     *   // Record only the trace events from the "browser" category.
     *   <code>new TracingConfig.Builder().addCategories("browser").build()</code>
     *
     *   // Record only the trace events matching the "blink*" and "renderer*" patterns
     *   // (e.g. "blink.animations", "renderer_host" and "renderer.scheduler" categories).
     *   <code>new TracingConfig.Builder().addCategories("blink*","renderer*").build()</code>
     *
     *   // Record events from the "web developer" predefined category set and events from
     *   // the "disabled-by-default-v8.gc" category to understand where garbage collection
     *   // is being triggered. Uses a limited size buffer for internal storage during tracing.
     *   <code>new TracingConfig.Builder().addCategories(CATEGORIES_WEB_DEVELOPER)
     *                              .addCategories("disabled-by-default-v8.gc")
     *                              .setTracingMode(RECORD_UNTIL_FULL).build()</code>
     * </pre>
     */
    public static class Builder {
        private @PredefinedCategories int mPredefinedCategories = CATEGORIES_NONE;
        private final List<String> mCustomIncludedCategories = new ArrayList<>();
        private @TracingMode int mTracingMode = RECORD_CONTINUOUSLY;

        /**
         * Default constructor for Builder.
         */
        public Builder() {
        }

        /**
         * Build {@link TracingConfig} using the current settings.
         *
         * @return The {@link TracingConfig} with the current settings.
         */
        @NonNull
        public TracingConfig build() {
            return new TracingConfig(mPredefinedCategories, mCustomIncludedCategories,
                    mTracingMode);
        }

        /**
         * Adds predefined sets of categories to be included in the trace output.
         *
         * A predefined category set can be one of
         * {@link TracingConfig#CATEGORIES_NONE},
         * {@link TracingConfig#CATEGORIES_ALL},
         * {@link TracingConfig#CATEGORIES_ANDROID_WEBVIEW},
         * {@link TracingConfig#CATEGORIES_WEB_DEVELOPER},
         * {@link TracingConfig#CATEGORIES_INPUT_LATENCY},
         * {@link TracingConfig#CATEGORIES_RENDERING},
         * {@link TracingConfig#CATEGORIES_JAVASCRIPT_AND_RENDERING} or
         * {@link TracingConfig#CATEGORIES_FRAME_VIEWER}.
         *
         * @param predefinedCategories A list or bitmask of predefined category sets.
         * @return The builder to facilitate chaining.
         */
        @NonNull
        public Builder addCategories(@NonNull @PredefinedCategories int... predefinedCategories) {
            for (int categorySet : predefinedCategories) {
                mPredefinedCategories |= categorySet;
            }
            return this;
        }

        /**
         * Adds custom categories to be included in trace output.
         *
         * Note that the categories are defined by the currently-in-use version of WebView. They
         * live in chromium code and are not part of the Android API.
         * See <a href="https://www.chromium.org/developers/how-tos/trace-event-profiling-tool">
         * chromium documentation on tracing</a> for more details.
         *
         * @param categories A list of category patterns. A category pattern can contain wildcards,
         *        e.g. "blink*" or full category name e.g. "renderer.scheduler".
         * @return The builder to facilitate chaining.
         */
        @NonNull
        public Builder addCategories(@NonNull String... categories) {
            mCustomIncludedCategories.addAll(Arrays.asList(categories));
            return this;
        }

        /**
         * Adds custom categories to be included in trace output.
         *
         * Same as {@link #addCategories(String...)} but allows to pass a Collection as a parameter.
         *
         * @param categories A list of category patterns.
         * @return The builder to facilitate chaining.
         */
        @NonNull
        public Builder addCategories(@NonNull Collection<String> categories) {
            mCustomIncludedCategories.addAll(categories);
            return this;
        }

        /**
         * Sets the tracing mode for this configuration.
         * When tracingMode is not set explicitly,
         * the default is {@link android.webkit.TracingConfig#RECORD_CONTINUOUSLY}.
         *
         * @param tracingMode The tracing mode to use, one of
         *                    {@link TracingConfig#RECORD_UNTIL_FULL} or
         *                    {@link TracingConfig#RECORD_CONTINUOUSLY}.
         * @return The builder to facilitate chaining.
         */
        @NonNull
        public Builder setTracingMode(@TracingMode int tracingMode) {
            mTracingMode = tracingMode;
            return this;
        }
    }
}
