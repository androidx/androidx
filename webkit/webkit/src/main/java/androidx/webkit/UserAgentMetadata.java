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

import androidx.annotation.RequiresFeature;
import androidx.annotation.RestrictTo;
import androidx.annotation.StringDef;
import androidx.webkit.internal.ApiFeature;
import androidx.webkit.internal.WebViewFeatureInternal;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Holds user-agent metadata information and uses to generate user-agent client
 * hints.
 * <p>
 * This class is functionally equivalent to
 * <a href="https://wicg.github.io/ua-client-hints/#interface">UADataValues</a>.
 */
public final class UserAgentMetadata {
    /**
     * Use this value for bitness to use the platform's default bitness value, which is an empty
     * string for Android WebView.
     */
    public static final int BITNESS_DEFAULT = 0;

    /**
     * Form factor option: {@code Desktop}, to be used with {@link Builder#setFormFactors}
     * and {@link Builder#getFormFactors}.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public static final String FORM_FACTOR_DESKTOP = "Desktop";

    /**
     * Form factor option: {@code Automotive}, to be used with {@link Builder#setFormFactors}
     * and {@link Builder#getFormFactors}.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public static final String FORM_FACTOR_AUTOMOTIVE = "Automotive";

    /**
     * Form factor option: {@code Mobile}, to be used with {@link Builder#setFormFactors}
     * and {@link Builder#getFormFactors}.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public static final String FORM_FACTOR_MOBILE = "Mobile";

    /**
     * Form factor option: {@code Tablet}, to be used with {@link Builder#setFormFactors}
     * and {@link Builder#getFormFactors}.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public static final String FORM_FACTOR_TABLET = "Tablet";

    /**
     * Form factor option: {@code XR}, to be used with {@link Builder#setFormFactors}
     * and {@link Builder#getFormFactors}.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public static final String FORM_FACTOR_XR = "XR";

    /**
     * Form factor option: {@code EInk}, to be used with {@link Builder#setFormFactors}
     * and {@link Builder#getFormFactors}.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public static final String FORM_FACTOR_EINK = "EInk";

    /**
     * Form factor option: {@code Watch}, to be used with {@link Builder#setFormFactors}
     * and {@link Builder#getFormFactors}.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public static final String FORM_FACTOR_WATCH = "Watch";

    /**
     * Values for the Sec-CH-UA-Form-Factors header.
     * https://wicg.github.io/ua-client-hints/#sec-ch-ua-form-factors
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @StringDef({
        FORM_FACTOR_DESKTOP,
        FORM_FACTOR_AUTOMOTIVE,
        FORM_FACTOR_MOBILE,
        FORM_FACTOR_TABLET,
        FORM_FACTOR_XR,
        FORM_FACTOR_EINK,
        FORM_FACTOR_WATCH
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface FormFactors {};

    private static final Set<String> VALID_FORM_FACTORS =
            Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
                FORM_FACTOR_DESKTOP, FORM_FACTOR_AUTOMOTIVE, FORM_FACTOR_MOBILE,
                FORM_FACTOR_TABLET, FORM_FACTOR_XR, FORM_FACTOR_EINK, FORM_FACTOR_WATCH
            )));

    private final List<BrandVersion> mBrandVersionList;

    private final String mFullVersion;
    private final String mPlatform;
    private final String mPlatformVersion;
    private final String mArchitecture;
    private final String mModel;
    private boolean mMobile = true;
    private int mBitness = BITNESS_DEFAULT;
    private boolean mWow64 = false;
    private final @FormFactors List<String> mFormFactors;

    @RestrictTo(RestrictTo.Scope.LIBRARY)
    private UserAgentMetadata(@NonNull List<BrandVersion> brandVersionList,
            @Nullable String fullVersion, @Nullable String platform,
            @Nullable String platformVersion, @Nullable String architecture,
            @Nullable String model,
            boolean mobile,
            int bitness, boolean wow64,
            @NonNull @FormFactors List<String> formFactors) {
        mBrandVersionList = brandVersionList;
        mFullVersion = fullVersion;
        mPlatform = platform;
        mPlatformVersion = platformVersion;
        mArchitecture = architecture;
        mModel = model;
        mMobile = mobile;
        mBitness = bitness;
        mWow64 = wow64;
        mFormFactors = formFactors;
    }

    /**
     * Returns the current list of user-agent brand versions which are used to populate
     * user-agent client hints {@code sec-ch-ua} and {@code sec-ch-ua-full-version-list}. Each
     * {@link BrandVersion} object holds the brand name, brand major version and brand
     * full version.
     * <p>
     * @see Builder#setBrandVersionList
     *
     */
    public @NonNull List<BrandVersion> getBrandVersionList() {
        return mBrandVersionList;
    }

    /**
     * Returns the value for the {@code sec-ch-ua-full-version} client hint.
     * <p>
     * @see Builder#setFullVersion
     *
     */
    public @Nullable String getFullVersion() {
        return mFullVersion;
    }

    /**
     * Returns the value for the {@code sec-ch-ua-platform} client hint.
     * <p>
     * @see Builder#setPlatform
     *
     */
    public @Nullable String getPlatform() {
        return mPlatform;
    }

    /**
     * Returns the value for the {@code sec-ch-ua-platform-version} client hint.
     * <p>
     * @see Builder#setPlatformVersion
     *
     * @return Platform version string.
     */
    public @Nullable String getPlatformVersion() {
        return mPlatformVersion;
    }

    /**
     * Returns the value for the {@code sec-ch-ua-arch} client hint.
     * <p>
     * @see Builder#setArchitecture
     *
     */
    public @Nullable String getArchitecture() {
        return mArchitecture;
    }

    /**
     * Returns the value for the {@code sec-ch-ua-model} client hint.
     * <p>
     * @see Builder#setModel
     *
     */
    public @Nullable String getModel() {
        return mModel;
    }

    /**
     * Returns the value for the {@code sec-ch-ua-mobile} client hint.
     * <p>
     * @see Builder#setMobile
     *
     * @return A boolean indicates user-agent's device mobileness.
     */
    public boolean isMobile() {
        return mMobile;
    }

    /**
     * Returns the value for the {@code sec-ch-ua-bitness} client hint.
     * <p>
     * @see Builder#setBitness
     *
     * @return An integer indicates the CPU bitness, the integer value will convert to string
     * when generating the user-agent client hint, and {@link UserAgentMetadata#BITNESS_DEFAULT}
     * means an empty string.
     */
    public int getBitness() {
        return mBitness;
    }

    /**
     * Returns the value for the {@code sec-ch-ua-wow64} client hint.
     * <p>
     * @see Builder#setWow64
     *
     * @return A boolean to indicate whether user-agent's binary is running in 32-bit mode on
     * 64-bit Windows.
     */
    public boolean isWow64() {
        return mWow64;
    }

    /**
     * Returns the value for the {@code sec-ch-ua-form-factors} client hint.
     * Value should be one or more of {@link #FORM_FACTOR_DESKTOP},
     * {@link #FORM_FACTOR_AUTOMOTIVE}, {@link #FORM_FACTOR_MOBILE},
     * {@link #FORM_FACTOR_TABLET}, {@link #FORM_FACTOR_XR},
     * {@link #FORM_FACTOR_EINK}, {@link #FORM_FACTOR_WATCH}. See the
     * <a href="https://wicg.github.io/ua-client-hints/#sec-ch-ua-form-factors">spec</a>
     * for more details.
     * <p>
     * @see Builder#setFormFactors
     *
     * @return A list of strings to indicate the form factors of the user-agent.
     *
     */
    @RequiresFeature(name = WebViewFeature.USER_AGENT_METADATA_FORM_FACTORS,
            enforcement = "androidx.webkit.WebViewFeature#isFeatureSupported")
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public @NonNull @FormFactors List<String> getFormFactors() {
        return mFormFactors;
    }

    /**
     * Two UserAgentMetadata objects are equal only if all the metadata values are equal.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof UserAgentMetadata)) return false;
        UserAgentMetadata that = (UserAgentMetadata) o;
        return mMobile == that.mMobile && mBitness == that.mBitness && mWow64 == that.mWow64
                && Objects.equals(mBrandVersionList, that.mBrandVersionList)
                && Objects.equals(mFullVersion, that.mFullVersion)
                && Objects.equals(mPlatform, that.mPlatform) && Objects.equals(
                mPlatformVersion, that.mPlatformVersion) && Objects.equals(mArchitecture,
                that.mArchitecture) && Objects.equals(mModel, that.mModel)
                && Objects.equals(mFormFactors, that.mFormFactors);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mBrandVersionList, mFullVersion, mPlatform, mPlatformVersion,
                mArchitecture, mModel, mMobile, mBitness, mWow64, mFormFactors);
    }

    /**
     * Class that holds brand name, major version and full version. Brand name and major version
     * used to generated user-agent client hint {@code sec-cu-ua}. Brand name and full version
     * used to generated user-agent client hint {@code sec-ch-ua-full-version-list}.
     * <p>
     * This class is functionally equivalent to
     * <a href="https://wicg.github.io/ua-client-hints/#interface">NavigatorUABrandVersion</a>.
     *
     */
    public static final class BrandVersion {
        private final String mBrand;
        private final String mMajorVersion;
        private final String mFullVersion;

        @RestrictTo(RestrictTo.Scope.LIBRARY)
        private BrandVersion(@NonNull String brand, @NonNull String majorVersion,
                @NonNull String fullVersion) {
            mBrand = brand;
            mMajorVersion = majorVersion;
            mFullVersion = fullVersion;
        }

        /**
         * Returns the brand of user-agent brand version tuple.
         *
         */
        public @NonNull String getBrand() {
            return mBrand;
        }

        /**
         * Returns the major version of user-agent brand version tuple.
         *
         */
        public @NonNull String getMajorVersion() {
            return mMajorVersion;
        }

        /**
         * Returns the full version of user-agent brand version tuple.
         *
         */
        public @NonNull String getFullVersion() {
            return mFullVersion;
        }

        @Override
        public @NonNull String toString() {
            return mBrand + "," + mMajorVersion + "," + mFullVersion;
        }

        /**
         * Two BrandVersion objects are equal only if brand name, major version and full version
         * are equal.
         */
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof BrandVersion)) return false;
            BrandVersion that = (BrandVersion) o;
            return Objects.equals(mBrand, that.mBrand) && Objects.equals(mMajorVersion,
                    that.mMajorVersion) && Objects.equals(mFullVersion, that.mFullVersion);
        }

        @Override
        public int hashCode() {
            return Objects.hash(mBrand, mMajorVersion, mFullVersion);
        }

        /**
         * Builder used to create {@link BrandVersion} objects.
         * <p>
         * Examples:
         * <pre class="prettyprint">
         *  // Create a setting with a brand version contains brand name: myBrand,
         *  // major version: 100, full version: 100.1.1.1.
         *  new BrandVersion.Builder().setBrand("myBrand")
         *                            .setMajorVersion("100")
         *                            .setFullVersion("100.1.1.1")
         *                            .build();
         * </pre>
         */
        public static final class Builder {
            private String mBrand;
            private String mMajorVersion;
            private String mFullVersion;

            /**
             * Create an empty BrandVersion Builder.
             */
            public Builder() {
            }

            /**
             * Create a BrandVersion Builder from an existing BrandVersion object.
             */
            public Builder(@NonNull BrandVersion brandVersion) {
                mBrand = brandVersion.getBrand();
                mMajorVersion = brandVersion.getMajorVersion();
                mFullVersion = brandVersion.getFullVersion();
            }

            /**
             * Builds the current brand, majorVersion and fullVersion into a BrandVersion object.
             *
             * @return The BrandVersion object represented by this Builder.
             * @throws IllegalStateException If any of the value in brand, majorVersion and
             *                               fullVersion is null or blank.
             */
            public @NonNull BrandVersion build() {
                if (mBrand == null || mBrand.trim().isEmpty()
                        || mMajorVersion == null || mMajorVersion.trim().isEmpty()
                        || mFullVersion == null || mFullVersion.trim().isEmpty()) {
                    throw new IllegalStateException("Brand name, major version and full version "
                            + "should not be null or blank.");
                }
                return new BrandVersion(mBrand, mMajorVersion, mFullVersion);
            }

            /**
             * Sets the BrandVersion's brand. The brand should not be blank.
             *
             * @param brand The brand is used to generate user-agent client hint
             *              {@code sec-ch-ua} and {@code sec-ch-ua-full-version-list}.
             *
             */
            public BrandVersion.@NonNull Builder setBrand(@NonNull String brand) {
                if (brand.trim().isEmpty()) {
                    throw new IllegalArgumentException("Brand should not be blank.");
                }
                mBrand = brand;
                return this;
            }

            /**
             * Sets the BrandVersion's majorVersion. The majorVersion should not be blank.
             *
             * @param majorVersion The majorVersion is used to generate user-agent client hint
             *                     {@code sec-ch-ua}.
             *
             */
            public BrandVersion.@NonNull Builder setMajorVersion(@NonNull String majorVersion) {
                if (majorVersion.trim().isEmpty()) {
                    throw new IllegalArgumentException("MajorVersion should not be blank.");
                }
                mMajorVersion = majorVersion;
                return this;
            }

            /**
             * Sets the BrandVersion's fullVersion. The fullVersion should not be blank.
             *
             * @param fullVersion The brand is used to generate user-agent client hint
             *                    {@code sec-ch-ua-full-version-list}.
             *
             */
            public BrandVersion.@NonNull Builder setFullVersion(@NonNull String fullVersion) {
                if (fullVersion.trim().isEmpty()) {
                    throw new IllegalArgumentException("FullVersion should not be blank.");
                }
                mFullVersion = fullVersion;
                return this;
            }
        }
    }

    /**
     * Builder used to create {@link UserAgentMetadata} objects.
     * <p>
     * Examples:
     * <pre class="prettyprint">
     *  // Create a setting with default options.
     *  new UserAgentMetadata.Builder().build();
     * <p>
     *  // Create a setting with a brand version contains brand name: myBrand, major version: 100,
     *  // full version: 100.1.1.1.
     *  BrandVersion brandVersion = new BrandVersion.Builder().setBrand("myBrand")
     *                                                        .setMajorVersion("100")
     *                                                        .setFullVersion("100.1.1.1")
     *                                                        .build();
     *  new UserAgentMetadata.Builder().setBrandVersionList(Collections.singletonList(brandVersion))
     *                                 .build();
     * <p>
     *  // Create a setting brand version, platform, platform version and bitness.
     *  new UserAgentMetadata.Builder().setBrandVersionList(Collections.singletonList(brandVersion))
     *                                 .setPlatform("myPlatform")
     *                                 .setPlatform("1.1.1.1")
     *                                 .setBitness(BITNESS_64)
     *                                 .build();
     * </pre>
     */
    public static final class Builder {
        private List<BrandVersion> mBrandVersionList = new ArrayList<>();
        private String mFullVersion;
        private String mPlatform;
        private String mPlatformVersion;
        private String mArchitecture;
        private String mModel;
        private boolean mMobile = true;
        private int mBitness = BITNESS_DEFAULT;
        private boolean mWow64 = false;
        private List<String> mFormFactors = new ArrayList<>();

        /**
         * Create an empty UserAgentMetadata Builder.
         */
        public Builder() {
        }

        /**
         * Create a UserAgentMetadata Builder from an existing UserAgentMetadata object.
         */
        public Builder(@NonNull UserAgentMetadata uaMetadata) {
            mBrandVersionList = uaMetadata.getBrandVersionList();
            mFullVersion = uaMetadata.getFullVersion();
            mPlatform = uaMetadata.getPlatform();
            mPlatformVersion = uaMetadata.getPlatformVersion();
            mArchitecture = uaMetadata.getArchitecture();
            mModel = uaMetadata.getModel();
            mMobile = uaMetadata.isMobile();
            mBitness = uaMetadata.getBitness();
            mWow64 = uaMetadata.isWow64();
            mFormFactors = uaMetadata.getFormFactors();
        }

        /**
         * Builds the current settings into a UserAgentMetadata object.
         *
         * @return The UserAgentMetadata object represented by this Builder
         */
        public @NonNull UserAgentMetadata build() {
            return new UserAgentMetadata(mBrandVersionList, mFullVersion, mPlatform,
                    mPlatformVersion, mArchitecture, mModel, mMobile, mBitness, mWow64,
                    mFormFactors);
        }

        /**
         * Sets user-agent metadata brands and their versions. The brand name, major version and
         * full version should not be blank. The default value is an empty list which means the
         * system default user-agent metadata brands and versions will be used to generate the
         * user-agent client hints.
         *
         * @param brandVersions a list of {@link BrandVersion} used to generated user-agent client
         *                     hints {@code sec-cu-ua} and {@code sec-ch-ua-full-version-list}.
         *
         */
        public @NonNull Builder setBrandVersionList(@NonNull List<BrandVersion> brandVersions) {
            mBrandVersionList = brandVersions;
            return this;
        }

        /**
         * Sets the user-agent metadata full version. The full version should not be blank, even
         * though the <a href="https://wicg.github.io/ua-client-hints">spec</a> about brand full
         * version could be empty. A blank full version could cause inconsistent brands when
         * generating brand version related user-agent client hints. It also provides a bad
         * experience for developers when processing the brand full version. If null is provided,
         * the system default value will be used to generate the client hint.
         *
         * @param fullVersion The full version is used to generate user-agent client hint
         *                    {@code sec-ch-ua-full-version}.
         *
         */
        public @NonNull Builder setFullVersion(@Nullable String fullVersion) {
            if (fullVersion == null) {
                mFullVersion = null;
                return this;
            }
            if (fullVersion.trim().isEmpty()) {
                throw new IllegalArgumentException("Full version should not be blank.");
            }
            mFullVersion = fullVersion;
            return this;
        }

        /**
         * Sets the user-agent metadata platform. The platform should not be blank. If null is
         * provided, the system default value will be used to generate the client hint.
         *
         * @param platform The platform is used to generate user-agent client hint
         *                 {@code sec-ch-ua-platform}.
         *
         */
        public @NonNull Builder setPlatform(@Nullable String platform) {
            if (platform == null) {
                mPlatform = null;
                return this;
            }
            if (platform.trim().isEmpty()) {
                throw new IllegalArgumentException("Platform should not be blank.");
            }
            mPlatform = platform;
            return this;
        }

        /**
         * Sets the user-agent metadata platform version. If null is provided, the system default
         * value will be used to generate the client hint.
         *
         * @param platformVersion The platform version is used to generate user-agent client
         *                        hint {@code sec-ch-ua-platform-version}.
         *
         */
        public @NonNull Builder setPlatformVersion(@Nullable String platformVersion) {
            mPlatformVersion = platformVersion;
            return this;
        }

        /**
         * Sets the user-agent metadata architecture. If null is provided, the system default
         * value will be used to generate the client hint.
         *
         * @param architecture The architecture is used to generate user-agent client hint
         *                     {@code sec-ch-ua-arch}.
         *
         */
        public @NonNull Builder setArchitecture(@Nullable String architecture) {
            mArchitecture = architecture;
            return this;
        }

        /**
         * Sets the user-agent metadata model. If null is provided, the system default value will
         * be used to generate the client hint.
         *
         * @param model The model is used to generate user-agent client hint
         *              {@code sec-ch-ua-model}.
         *
         */
        public @NonNull Builder setModel(@Nullable String model) {
            mModel = model;
            return this;
        }

        /**
         * Sets the user-agent metadata mobile, the default value is true.
         *
         * @param mobile The mobile is used to generate user-agent client hint
         *               {@code sec-ch-ua-mobile}.
         *
         */
        public @NonNull Builder setMobile(boolean mobile) {
            mMobile = mobile;
            return this;
        }

        /**
         * Sets the user-agent metadata bitness, the default value is
         * {@link UserAgentMetadata#BITNESS_DEFAULT}, which indicates an empty string for
         * {@code sec-ch-ua-bitness}.
         *
         * @param bitness The bitness is used to generate user-agent client hint
         *                {@code sec-ch-ua-bitness}.
         *
         */
        public @NonNull Builder setBitness(int bitness) {
            mBitness = bitness;
            return this;
        }

        /**
         * Sets the user-agent metadata wow64, the default is false.
         *
         * @param wow64 The wow64 is used to generate user-agent client hint
         *              {@code sec-ch-ua-wow64}.
         *
         */
        public @NonNull Builder setWow64(boolean wow64) {
            mWow64 = wow64;
            return this;
        }

        /**
         * Sets the user-agent metadata form factors. The default value is an empty list
         * which means the system default user-agent metadata form factor will be used to
         * generate the user-agent client hints.
         *
         * Form factor value should be one or more of {@link #FORM_FACTOR_DESKTOP},
         * {@link #FORM_FACTOR_AUTOMOTIVE}, {@link #FORM_FACTOR_MOBILE},
         * {@link #FORM_FACTOR_TABLET}, {@link #FORM_FACTOR_XR},
         * {@link #FORM_FACTOR_EINK}, {@link #FORM_FACTOR_WATCH}. See the
         * <a href="https://wicg.github.io/ua-client-hints/#sec-ch-ua-form-factors">spec</a>
         * for more details.
         *
         * @param formFactors The form factors is used to generate user-agent client hint
         *                    {@code sec-ch-ua-form-factors}.
         * @throws IllegalArgumentException if the list contains an invalid form factor string.
         * @throws UnsupportedOperationException if this feature is not supported by the current
         *         WebView.
         *
         */
        @RequiresFeature(name = WebViewFeature.USER_AGENT_METADATA_FORM_FACTORS,
                enforcement = "androidx.webkit.WebViewFeature#isFeatureSupported")
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        public @NonNull Builder setFormFactors(@NonNull @FormFactors List<String> formFactors) {
            ApiFeature.NoFramework feature =
                    WebViewFeatureInternal.USER_AGENT_METADATA_FORM_FACTORS;
            if (!feature.isSupportedByWebView()) {
                throw WebViewFeatureInternal.getUnsupportedOperationException();
            }

            for (String factor : formFactors) {
                if (!VALID_FORM_FACTORS.contains(factor)) {
                    throw new IllegalArgumentException("Invalid form factor: " + factor);
                }
            }
            mFormFactors = formFactors;
            return this;
        }
    }
}
