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

import android.util.Base64;

import androidx.annotation.ArrayRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.core.util.Preconditions;

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
        mProviderAuthority = Preconditions.checkNotNull(providerAuthority);
        mProviderPackage = Preconditions.checkNotNull(providerPackage);
        mQuery = Preconditions.checkNotNull(query);
        mCertificates = Preconditions.checkNotNull(certificates);
        mCertificatesArray = 0;
        mIdentifier = createIdentifier(providerAuthority, providerPackage, query);
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
        mProviderAuthority = Preconditions.checkNotNull(providerAuthority);
        mProviderPackage = Preconditions.checkNotNull(providerPackage);
        mQuery = Preconditions.checkNotNull(query);
        mCertificates = null;
        Preconditions.checkArgument(certificates != 0);
        mCertificatesArray = certificates;
        mIdentifier = createIdentifier(providerAuthority, providerPackage, query);
    }

    private String createIdentifier(
            @NonNull String providerAuthority,
            @NonNull String providerPackage,
            @NonNull String query
    ) {
        return providerAuthority + "-" + providerPackage + "-" + query;
    }

    /**
     * Returns the selected font provider's authority. This tells the system what font provider
     * it should request the font from.
     */
    @NonNull
    public String getProviderAuthority() {
        return mProviderAuthority;
    }

    /**
     * Returns the selected font provider's package. This helps the system verify that the provider
     * identified by the given authority is the one requested.
     */
    @NonNull
    public String getProviderPackage() {
        return mProviderPackage;
    }

    /**
     * Returns the query string. Refer to your font provider's documentation on the format of this
     * string.
     */
    @NonNull
    public String getQuery() {
        return mQuery;
    }

    /**
     * Returns the list of certificate sets given for this provider. This helps the system verify
     * that the provider identified by the given authority is the one requested. Note this might
     * be null if the certificates were provided via a resource id.
     *
     * @see #getCertificatesArrayResId()
     */
    @Nullable
    public List<List<byte[]>> getCertificates() {
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
    @NonNull
    String getId() {
        return mIdentifier;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("FontRequest {"
                + "mProviderAuthority: " + mProviderAuthority
                + ", mProviderPackage: " + mProviderPackage
                + ", mQuery: " + mQuery
                + ", mCertificates:");
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
        builder.append("}");
        builder.append("mCertificatesArray: " + mCertificatesArray);
        return builder.toString();
    }
}
