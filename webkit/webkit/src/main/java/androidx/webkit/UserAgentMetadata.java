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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;

import java.util.List;
import java.util.Objects;

/**
 * Holds user-agent metadata information and uses to generate user-agent client
 * hints.
 * <p>
 * This class is functionally equivalent to
 * <a href="https://wicg.github.io/ua-client-hints/#interface">UADataValues</a>.
 * <p>
 * TODO(b/294183509): unhide
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class UserAgentMetadata {
    /**
     * Use this value for bitness to use the platform's default bitness value, which is an empty
     * string for Android WebView.
     */
    public static final int BITNESS_DEFAULT = 0;

    private final List<BrandVersion> mBrandVersionList;

    private final String mFullVersion;
    private final String mPlatform;
    private final String mPlatformVersion;
    private final String mArchitecture;
    private final String mModel;
    private boolean mMobile = true;
    private int mBitness = BITNESS_DEFAULT;
    private boolean mWow64 = false;
    private final String mFormFactor;

    @RestrictTo(RestrictTo.Scope.LIBRARY)
    private UserAgentMetadata(@Nullable List<BrandVersion> brandVersionList,
            @Nullable String fullVersion, @Nullable String platform,
            @Nullable String platformVersion, @Nullable String architecture,
            @Nullable String model,
            boolean mobile,
            int bitness, boolean wow64, @Nullable String formFactor) {
        mBrandVersionList = brandVersionList;
        mFullVersion = fullVersion;
        mPlatform = platform;
        mPlatformVersion = platformVersion;
        mArchitecture = architecture;
        mModel = model;
        mMobile = mobile;
        mBitness = bitness;
        mWow64 = wow64;
        mFormFactor = formFactor;
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
    @Nullable
    public List<BrandVersion> getBrandVersionList() {
        return mBrandVersionList;
    }

    /**
     * Returns the value for the {@code sec-ch-ua-full-version} client hint.
     * <p>
     * @see Builder#setFullVersion
     *
     */
    @Nullable
    public String getFullVersion() {
        return mFullVersion;
    }

    /**
     * Returns the value for the {@code sec-ch-ua-platform} client hint.
     * <p>
     * @see Builder#setPlatform
     *
     */
    @Nullable
    public String getPlatform() {
        return mPlatform;
    }

    /**
     * Returns the value for the {@code sec-ch-ua-platform-version} client hint.
     * <p>
     * @see Builder#setPlatformVersion
     *
     * @return Platform version string.
     */
    @Nullable
    public String getPlatformVersion() {
        return mPlatformVersion;
    }

    /**
     * Returns the value for the {@code sec-ch-ua-arch} client hint.
     * <p>
     * @see Builder#setArchitecture
     *
     */
    @Nullable
    public String getArchitecture() {
        return mArchitecture;
    }

    /**
     * Returns the value for the {@code sec-ch-ua-model} client hint.
     * <p>
     * @see Builder#setModel
     *
     */
    @Nullable
    public String getModel() {
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
     * @return A boolean to indicate whether user-agent's binary is running in 64-bit Windows.
     */
    public boolean isWow64() {
        return mWow64;
    }

    /**
     * Returns the value for the {@code sec-ch-ua-form-factor} client hint.
     * <p>
     * @see Builder#setFormFactor
     *
     */
    @Nullable
    public String getFormFactor() {
        return mFormFactor;
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
                && Objects.equals(mFormFactor, that.mFormFactor);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mBrandVersionList, mFullVersion, mPlatform, mPlatformVersion,
                mArchitecture, mModel, mMobile, mBitness, mWow64, mFormFactor);
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
    public static class BrandVersion {
        private final String mBrand;
        private final String mMajorVersion;
        private final String mFullVersion;

        @RestrictTo(RestrictTo.Scope.LIBRARY)
        public BrandVersion(@NonNull String brand, @NonNull String majorVersion,
                @NonNull String fullVersion) {
            if (brand.trim().isEmpty() || majorVersion.trim().isEmpty()
                    || fullVersion.trim().isEmpty()) {
                throw new IllegalArgumentException("Brand name, major version and full version "
                        + "should not be blank.");
            }
            mBrand = brand;
            mMajorVersion = majorVersion;
            mFullVersion = fullVersion;
        }

        /**
         * Returns the brand of user-agent brand version tuple.
         *
         */
        @NonNull
        public String getBrand() {
            return mBrand;
        }

        /**
         * Returns the major version of user-agent brand version tuple.
         *
         */
        @NonNull
        public String getMajorVersion() {
            return mMajorVersion;
        }

        /**
         * Returns the full version of user-agent brand version tuple.
         *
         */
        @NonNull
        public String getFullVersion() {
            return mFullVersion;
        }

        @NonNull
        @Override
        public String toString() {
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
    }

    /**
     * Builder used to create {@link UserAgentMetadata} objects.
     * <p>
     * Examples:
     * <pre class="prettyprint">
     *   // Create a setting with default options.
     *   new UserAgentMetadata.Builder().build();
     *
     *   // Create a setting with a brand version contains brand name: myBrand, major version: 100,
     *   // full version: 100.1.1.1.
     *   new UserAgentMetadata.Builder().setBrandVersionList(
     *   Collections.singletonList(new BrandVersion("myBrand", "100", "100.1.1.1"))).build();
     *
     *   // Create a setting brand version, platform, platform version and bitness.
     *   new UserAgentMetadata.Builder().setBrandVersionList(
     *   Collections.singletonList(new BrandVersion("myBrand", "100", "100.1.1.1")))
     *                                        .setPlatform("myPlatform")
     *                                        .setPlatform("1.1.1.1")
     *                                        .setBitness(BITNESS_64)
     *                                        .build();
     * </pre>
     */
    public static final class Builder {
        private List<BrandVersion> mBrandVersionList;
        private String mFullVersion;
        private String mPlatform;
        private String mPlatformVersion;
        private String mArchitecture;
        private String mModel;
        private boolean mMobile = true;
        private int mBitness = BITNESS_DEFAULT;
        private boolean mWow64 = false;
        private String mFormFactor;

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
            mFormFactor = uaMetadata.getFormFactor();
        }

        /**
         * Builds the current settings into a UserAgentMetadata object.
         *
         * @return The UserAgentMetadata object represented by this Builder
         */
        @NonNull
        public UserAgentMetadata build() {
            return new UserAgentMetadata(mBrandVersionList, mFullVersion, mPlatform,
                    mPlatformVersion, mArchitecture, mModel, mMobile, mBitness, mWow64,
                    mFormFactor);
        }

        /**
         * Sets user-agent metadata brands and their versions. The brand name, major version and
         * full version should not be blank.
         *
         * @param brandVersions a list of {@link BrandVersion} used to generated user-agent client
         *                     hints {@code sec-cu-ua} and {@code sec-ch-ua-full-version-list}.
         *
         */
        @NonNull
        public Builder setBrandVersionList(@NonNull List<BrandVersion> brandVersions) {
            if (brandVersions.isEmpty()) {
                throw new IllegalArgumentException("Brand version list should not be empty.");
            }
            mBrandVersionList = brandVersions;
            return this;
        }

        /**
         * Sets the user-agent metadata full version. The full version should not be blank, even
         * though the <a href="https://wicg.github.io/ua-client-hints">spec<a/> about brand full
         * version could be empty. The blank full version could cause inconsistent brands when
         * generating brand version related user-agent client hints. It also gives bad experience
         * for developers when processing the brand full version.
         *
         * @param fullVersion The full version is used to generate user-agent client hint
         *                    {@code sec-ch-ua-full-version}.
         *
         */
        @NonNull
        public Builder setFullVersion(@NonNull String fullVersion) {
            if (fullVersion.trim().isEmpty()) {
                throw new IllegalArgumentException("Full version should not be blank.");
            }
            mFullVersion = fullVersion;
            return this;
        }

        /**
         * Sets the user-agent metadata platform. The platform should not be blank.
         *
         * @param platform The platform is used to generate user-agent client hint
         *                 {@code sec-ch-ua-platform}.
         *
         */
        @NonNull
        public Builder setPlatform(@NonNull String platform) {
            if (platform.trim().isEmpty()) {
                throw new IllegalArgumentException("Platform should not be blank.");
            }
            mPlatform = platform;
            return this;
        }

        /**
         * Sets the user-agent metadata platform version. The value should not be null but can be
         * empty string.
         *
         * @param platformVersion The platform version is used to generate user-agent client
         *                        hint {@code sec-ch-ua-platform-version}.
         *
         */
        @NonNull
        public Builder setPlatformVersion(@NonNull String platformVersion) {
            mPlatformVersion = platformVersion;
            return this;
        }

        /**
         * Sets the user-agent metadata architecture. The value should not be null but can be
         * empty string.
         *
         * @param architecture The architecture is used to generate user-agent client hint
         *                     {@code sec-ch-ua-arch}.
         *
         */
        @NonNull
        public Builder setArchitecture(@NonNull String architecture) {
            mArchitecture = architecture;
            return this;
        }

        /**
         * Sets the user-agent metadata model. The value should not be null but can be empty string.
         *
         * @param model The model is used to generate user-agent client hint
         *              {@code sec-ch-ua-model}.
         *
         */
        @NonNull
        public Builder setModel(@NonNull String model) {
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
        @NonNull
        public Builder setMobile(boolean mobile) {
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
        @NonNull
        public Builder setBitness(int bitness) {
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
        @NonNull
        public Builder setWow64(boolean wow64) {
            mWow64 = wow64;
            return this;
        }

        /**
         * Sets the user-agent metadata form factor. The value should not be null but can be
         * empty string.
         *
         * @param formFactor The form factor is used to generate user-agent client hint
         *                   {@code sec-ch-ua-form-factor}.
         *
         */
        @NonNull
        public Builder setFormFactor(@NonNull String formFactor) {
            mFormFactor = formFactor;
            return this;
        }
    }
}
