/*
 * Copyright (C) 2017 The Android Open Source Project
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

package androidx.core.provider;

import static androidx.annotation.RestrictTo.Scope.LIBRARY;
import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP_PREFIX;

import android.annotation.SuppressLint;
import android.util.Base64;

import androidx.annotation.ArrayRes;
import androidx.annotation.RestrictTo;
import androidx.core.util.Preconditions;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Collections;
import java.util.List;

/**
 * Information about a font request that may be sent to a Font Provider.
 */
public final class FontRequest {
    private final String mProviderAuthority;
    private final String mProviderPackage;
    private final String mQuery;
    private final List<List<byte[]>> mCertificates;
    private final int mCertificatesArray;
    private final List<List<byte[]>> mSystemCertificates;
    private final int mSystemCertificatesArray;
    private final @Nullable String mSystemFont;
    private final @Nullable String mVariationSettings;

    // Used for key of the cache
    private final String mIdentifier;

    /**
     * @param providerAuthority The authority of the Font Provider to be used for the request.
     * @param query The query to be sent over to the provider. Refer to your font provider's
     *         documentation on the format of this string.
     * @param providerPackage The package for the Font Provider to be used for the request. This is
     *         used to verify the identity of the provider.
     * @param certificates The list of sets of hashes for the certificates the provider should be
     *         signed with. This is used to verify the identity of the provider. Each set in the
     *         list represents one collection of signature hashes. Refer to your font provider's
     *         documentation for these values.
     */
    public FontRequest(@NonNull String providerAuthority, @NonNull String providerPackage,
            @NonNull String query, @NonNull List<List<byte[]>> certificates) {
        this(providerAuthority, providerPackage, query, certificates, Collections.emptyList(),
                null, null);
    }

    /**
     * @param providerAuthority The authority of the Font Provider to be used for the request.
     * @param query The query to be sent over to the provider. Refer to your font provider's
     *         documentation on the format of this string.
     * @param providerPackage The package for the Font Provider to be used for the request. This is
     *         used to verify the identity of the provider.
     * @param certificates The list of sets of hashes for the certificates the provider should be
     *         signed with. This is used to verify the identity of the provider. Each set in the
     *         list represents one collection of signature hashes. Refer to your font provider's
     *         documentation for these values.
     * @param variationSettings {@link String} specifying the font variation settings (e.g. standard
     *         axes like weight, width, and slant, or custom axes) to be applied to a variable font
     *         when it is loaded. See
     *         {@link android.graphics.Paint#setFontVariationSettings(String)} for more information
     *         about the format of the string. Supported on API 26 (Android O) and above. Note that
     *         these settings will only be effective if the font provider actually returns a
     *         variable font.
     */
    public FontRequest(
            @NonNull String providerAuthority, @NonNull String providerPackage,
            @NonNull String query, @NonNull List<List<byte[]>> certificates,
            @Nullable String variationSettings) {
        this(providerAuthority, providerPackage, query, certificates, Collections.emptyList(),
                null, variationSettings);
    }

    /**
     * @param providerAuthority The authority of the Font Provider to be used for the request.
     * @param query The query to be sent over to the provider. Refer to your font provider's
     *         documentation on the format of this string.
     * @param providerPackage The package for the Font Provider to be used for the request. This is
     *         used to verify the identity of the provider.
     * @param certificates A resource array with the list of sets of hashes for the certificates the
     *         provider should be signed with. This is used to verify the identity of the provider.
     *         Each set in the list represents one collection of signature hashes. Refer to your
     *         font provider's documentation for these values.
     */
    public FontRequest(@NonNull String providerAuthority, @NonNull String providerPackage,
            @NonNull String query, @ArrayRes int certificates) {
        this(providerAuthority, providerPackage, query, certificates, 0, null, null);
    }

    /**
     * @param providerAuthority The authority of the Font Provider to be used for the request.
     * @param query The query to be sent over to the provider. Refer to your font provider's
     *         documentation on the format of this string.
     * @param providerPackage The package for the Font Provider to be used for the request. This is
     *         used to verify the identity of the provider.
     * @param certificates A resource array with the list of sets of hashes for the certificates the
     *         provider should be signed with. This is used to verify the identity of the provider.
     *         Each set in the list represents one collection of signature hashes. Refer to your
     *         font provider's documentation for these values.
     * @param variationSettings {@link String} specifying the font variation settings (e.g. standard
     *         axes like weight, width, and slant, or custom axes) to be applied to a variable font
     *         when it is loaded. See
     *         {@link android.graphics.Paint#setFontVariationSettings(String)} for more information
     *         about the format of the string. Supported on API 26 (Android O) and above. Note that
     *         these settings will only be effective if the font provider actually returns a
     *         variable font.
     */
    public FontRequest(@NonNull String providerAuthority, @NonNull String providerPackage,
            @NonNull String query, @ArrayRes int certificates, @Nullable String variationSettings) {
        this(providerAuthority, providerPackage, query, certificates, 0, null, variationSettings);
    }

    @RestrictTo(LIBRARY)
    public FontRequest(@NonNull String providerAuthority, @NonNull String providerPackage,
            @NonNull String query, @NonNull List<List<byte[]>> certificates,
            @NonNull List<List<byte[]>> systemCertificates,
            @Nullable String systemFont, @Nullable String variationSettings) {
        mProviderAuthority = Preconditions.checkNotNull(providerAuthority);
        mProviderPackage = Preconditions.checkNotNull(providerPackage);
        mQuery = Preconditions.checkNotNull(query);
        mCertificates = Preconditions.checkNotNull(certificates);
        mCertificatesArray = 0;
        mSystemCertificates = Preconditions.checkNotNull(systemCertificates);
        mSystemCertificatesArray = 0;
        mSystemFont = systemFont;
        mVariationSettings = variationSettings;
        mIdentifier = createIdentifier(providerAuthority, providerPackage, query, systemFont,
                mVariationSettings);
    }

    @RestrictTo(LIBRARY)
    public FontRequest(@NonNull String providerAuthority, @NonNull String providerPackage,
            @NonNull String query, @NonNull List<List<byte[]>> certificates,
            @Nullable String systemFont, @Nullable String variationSettings) {
        this(providerAuthority, providerPackage, query, certificates, Collections.emptyList(),
                systemFont, variationSettings);
    }

    private FontRequest(@NonNull String providerAuthority, @NonNull String providerPackage,
            @NonNull String query, @ArrayRes int certificates, @ArrayRes int systemCertificates,
            @Nullable String systemFont, @Nullable String variationSettings) {
        mProviderAuthority = Preconditions.checkNotNull(providerAuthority);
        mProviderPackage = Preconditions.checkNotNull(providerPackage);
        mQuery = Preconditions.checkNotNull(query);
        mCertificates = null;
        Preconditions.checkArgument(certificates != 0 || systemCertificates != 0);
        mCertificatesArray = certificates;
        mSystemCertificates = null;
        mSystemCertificatesArray = systemCertificates;
        mSystemFont = systemFont;
        mVariationSettings = variationSettings;
        mIdentifier = createIdentifier(providerAuthority, providerPackage, query, mSystemFont,
                mVariationSettings);
    }

    private String createIdentifier(
            @NonNull String providerAuthority,
            @NonNull String providerPackage,
            @NonNull String query,
            @Nullable String systemFont,
            @Nullable String variationSettings
    ) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(providerAuthority)
                .append("-").append(providerPackage)
                .append("-").append(query)
                .append("-").append(systemFont);

        if (variationSettings != null && !variationSettings.isBlank()) {
            stringBuilder.append("-VF");
        }

        return stringBuilder.toString();
    }

    /**
     * Returns the selected font provider's authority. This tells the system what font provider
     * it should request the font from.
     */
    public @NonNull String getProviderAuthority() {
        return mProviderAuthority;
    }

    /**
     * Returns the selected font provider's package. This helps the system verify that the provider
     * identified by the given authority is the one requested.
     */
    public @NonNull String getProviderPackage() {
        return mProviderPackage;
    }

    /**
     * Returns the query string. Refer to your font provider's documentation on the format of this
     * string.
     */
    public @NonNull String getQuery() {
        return mQuery;
    }

    /**
     * Returns the list of certificate sets given for this provider. This helps the system verify
     * that the provider identified by the given authority is the one requested. Note this might
     * be null if the certificates were provided via a resource id.
     *
     * @see #getCertificatesArrayResId()
     */
    public @Nullable List<List<byte[]>> getCertificates() {
        return mCertificates;
    }

    /**
     * Returns the array resource id pointing to the certificate sets given for this provider. This
     * helps the system verify that the provider identified by the given authority is the one
     * requested. Note that this may be 0 if the certificates were provided as a list.
     *
     * @see #getCertificates()
     */
    @ArrayRes
    public int getCertificatesArrayResId() {
        return mCertificatesArray;
    }

    /**
     * Returns the list of system certificate sets given for this provider. This helps the system
     * verify that the provider identified by the given authority is the one requested, and is
     * only trusted if the provider is installed as a system application (FLAG_SYSTEM or
     * FLAG_UPDATED_SYSTEM_APP). Note this might be null if the certificates were provided via a
     * resource id.
     *
     * @see #getSystemCertificatesArrayResId()
     */
    @SuppressWarnings("NullableCollection")
    public @Nullable List<List<byte[]>> getSystemCertificates() {
        return mSystemCertificates;
    }

    /**
     * Returns the array resource id pointing to the system certificate sets given for this
     * provider. This helps the system verify that the provider identified by the given authority
     * is the one requested, and is only trusted if the provider is installed as a system
     * application (FLAG_SYSTEM or FLAG_UPDATED_SYSTEM_APP). Note that this may be 0 if the
     * certificates were provided as a list.
     *
     * @see #getSystemCertificates()
     */
    @ArrayRes
    public int getSystemCertificatesArrayResId() {
        return mSystemCertificatesArray;
    }

    /**
     * Returns the array resource id pointing to the debug fallback certificate sets given for this
     * provider, or 0 if not set.
     *
     * @see Builder#setDebugFallbackCertificates(int)
     * @see #getCertificatesArrayResId()
     */
    @ArrayRes
    public int getDebugFallbackCertificatesArrayResId() {
        return mCertificatesArray;
    }

    /**
     * @deprecated Not being used by any cross library, and should not be used, internal
     * implementation detail.
     *
     */
    @Deprecated
    @RestrictTo(LIBRARY_GROUP_PREFIX)
    public String getIdentifier() {
        return mIdentifier;
    }

    @RestrictTo(LIBRARY)
    @NonNull String getId() {
        return mIdentifier;
    }

    @RestrictTo(LIBRARY)
    public @Nullable String getSystemFont() {
        return mSystemFont;
    }

    /**
     * Returns the font variation settings.
     *
     * See {@link android.graphics.Paint#setFontVariationSettings(String)} for more information
     * about the format of the string.
     */
    public @Nullable String getVariationSettings() {
        return mVariationSettings;
    }

    @NonNull
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("FontRequest {"
                + "mProviderAuthority: " + mProviderAuthority
                + ", mProviderPackage: " + mProviderPackage
                + ", mQuery: " + mQuery
                + ", mSystemFont: " + mSystemFont
                + ", mVariationSettings: " + mVariationSettings
                + ", mCertificates:");

        if (mCertificates != null) {
            for (int i = 0; i < mCertificates.size(); i++) {
                builder.append(" [");
                List<byte[]> set = mCertificates.get(i);
                for (int j = 0; j < set.size(); j++) {
                    builder.append(" \"");
                    byte[] array = set.get(j);
                    builder.append(Base64.encodeToString(array, Base64.DEFAULT));
                    builder.append("\"");
                }
                builder.append(" ]");
            }
        }
        builder.append(", mCertificatesArray: " + mCertificatesArray);
        builder.append(", mSystemCertificates:");
        if (mSystemCertificates != null) {
            for (int i = 0; i < mSystemCertificates.size(); i++) {
                builder.append(" [");
                List<byte[]> set = mSystemCertificates.get(i);
                for (int j = 0; j < set.size(); j++) {
                    builder.append(" \"");
                    byte[] array = set.get(j);
                    builder.append(Base64.encodeToString(array, Base64.DEFAULT));
                    builder.append("\"");
                }
                builder.append(" ]");
            }
        }
        builder.append(", mSystemCertificatesArray: " + mSystemCertificatesArray);
        builder.append("}");
        return builder.toString();
    }

    /**
     * Builder for constructing {@link FontRequest} instances.
     */
    public static final class Builder {
        private final @NonNull String mProviderAuthority;
        private final @NonNull String mProviderPackage;
        private final @NonNull String mQuery;
        private final @ArrayRes int mSystemCertificatesArray;
        private @ArrayRes int mDebugFallbackCertificatesArray;
        private @Nullable String mVariationSettings;

        /**
         * Constructs a new Builder for a system font provider.
         *
         * @param providerAuthority The authority of the Font Provider to be used for the request.
         * @param providerPackage The package for the Font Provider to be used for the request.
         * @param query The query to be sent over to the provider.
         * @param systemCertificates A resource array with the list of sets of hashes for the
         *         certificates the system provider should be signed with. The provider package must
         *         be installed on the system image ({@code FLAG_SYSTEM} or
         *         {@code FLAG_UPDATED_SYSTEM_APP}).
         */
        public Builder(
                @NonNull String providerAuthority,
                @NonNull String providerPackage,
                @NonNull String query,
                @ArrayRes int systemCertificates) {
            mProviderAuthority = Preconditions.checkNotNull(providerAuthority);
            mProviderPackage = Preconditions.checkNotNull(providerPackage);
            mQuery = Preconditions.checkNotNull(query);
            Preconditions.checkArgument(
                    systemCertificates != 0, "systemCertificates resource ID must not be 0");
            mSystemCertificatesArray = systemCertificates;
        }

        /**
         * Constructs a new Builder initialized with the properties of an existing
         * {@link FontRequest}.
         */
        public Builder(@NonNull FontRequest request) {
            Preconditions.checkNotNull(request);
            Preconditions.checkArgument(
                    request.mSystemCertificatesArray != 0,
                    "Cannot copy a legacy FontRequest without a systemCertificates resource ID");
            mProviderAuthority = request.mProviderAuthority;
            mProviderPackage = request.mProviderPackage;
            mQuery = request.mQuery;
            mSystemCertificatesArray = request.mSystemCertificatesArray;
            mDebugFallbackCertificatesArray = request.mCertificatesArray;
            mVariationSettings = request.mVariationSettings;
        }

        /**
         * Sets the optional debug fallback certificate resource array ID used to verify a
         * non-system font provider during development or testing (for example, when testing
         * against a local development build of GMSCore or running in Robolectric).
         *
         * <p>Warning: Certificates provided here do not require {@code FLAG_SYSTEM} and bypass
         * system app verification. Only use this method during development or testing.
         */
        @SuppressLint("MissingGetterMatchingBuilder")
        public @NonNull Builder setDebugFallbackCertificates(
                @ArrayRes int debugFallbackCertificates) {
            Preconditions.checkArgument(
                    debugFallbackCertificates != 0,
                    "debugFallbackCertificates resource ID must not be 0");
            mDebugFallbackCertificatesArray = debugFallbackCertificates;
            return this;
        }

        /**
         * Sets the font variation settings to be applied to a variable font when loaded.
         */
        public @NonNull Builder setVariationSettings(@NonNull String variationSettings) {
            mVariationSettings = Preconditions.checkNotNull(variationSettings);
            return this;
        }

        /**
         * Builds and returns the {@link FontRequest} instance.
         */
        public @NonNull FontRequest build() {
            return new FontRequest(
                    mProviderAuthority,
                    mProviderPackage,
                    mQuery,
                    mDebugFallbackCertificatesArray,
                    mSystemCertificatesArray,
                    null,
                    mVariationSettings);
        }
    }
}
