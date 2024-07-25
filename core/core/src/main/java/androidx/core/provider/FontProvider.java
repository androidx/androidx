/*
 * Copyright 2021 The Android Open Source Project
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

import android.annotation.SuppressLint;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ProviderInfo;
import android.content.pm.Signature;
import android.content.res.Resources;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.CancellationSignal;
import android.os.RemoteException;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.VisibleForTesting;
import androidx.collection.LruCache;
import androidx.core.content.res.FontResourcesParserCompat;
import androidx.core.graphics.TypefaceCompat;
import androidx.core.provider.FontsContractCompat.FontFamilyResult;
import androidx.core.provider.FontsContractCompat.FontInfo;
import androidx.tracing.Trace;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

class FontProvider {
    private FontProvider() {}

    @NonNull
    static FontFamilyResult getFontFamilyResult(@NonNull Context context,
            @NonNull List<FontRequest> requests, @Nullable CancellationSignal cancellationSignal)
            throws PackageManager.NameNotFoundException {
        if (TypefaceCompat.DOWNLOADABLE_FONT_TRACING) {
            Trace.beginSection("FontProvider.getFontFamilyResult");
        }
        try {
            ArrayList<FontInfo[]> queryResults = new ArrayList<>();
            for (int i = 0; i < requests.size(); i++) {
                FontRequest request = requests.get(i);
                ProviderInfo providerInfo = getProvider(
                        context.getPackageManager(), request, context.getResources());
                if (providerInfo == null) {
                    return FontFamilyResult.create(FontFamilyResult.STATUS_WRONG_CERTIFICATES,
                            (FontInfo[]) null);

                }
                FontInfo[] fonts = query(
                        context, request, providerInfo.authority, cancellationSignal);
                queryResults.add(fonts);
            }

            return FontFamilyResult.create(FontFamilyResult.STATUS_OK, queryResults);
        } finally {
            if (TypefaceCompat.DOWNLOADABLE_FONT_TRACING) {
                Trace.endSection();
            }
        }
    }

    private static class ProviderCacheKey {
        String mAuthority;
        String mPackageName;
        List<List<byte[]>> mCertificates;

        ProviderCacheKey(String authority, String packageName,
                List<List<byte[]>> certificates) {
            this.mAuthority = authority;
            this.mPackageName = packageName;
            this.mCertificates = certificates;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof ProviderCacheKey)) return false;
            ProviderCacheKey that = (ProviderCacheKey) o;
            return Objects.equals(mAuthority, that.mAuthority) && Objects.equals(
                    mPackageName, that.mPackageName) && Objects.equals(mCertificates,
                    that.mCertificates);
        }

        @Override
        public int hashCode() {
            return Objects.hash(mAuthority, mPackageName, mCertificates);
        }
    }

    /**
     * Cache of font providers.
     * The realistic number of providers is 1, so we'll be generous and store up to 2.
     */
    private static final LruCache<ProviderCacheKey, ProviderInfo> sProviderCache =
            new LruCache<>(2);

    /**
     * Clear font provider cache so that tests reset to a blank slate
     */
    @VisibleForTesting
    static void clearProviderCache() {
        sProviderCache.evictAll();
    }

    /**
     * Do not access directly, visible for testing only.
     */
    @VisibleForTesting
    @Nullable
    static ProviderInfo getProvider(
            @NonNull PackageManager packageManager,
            @NonNull FontRequest request,
            @Nullable Resources resources
    )
            throws PackageManager.NameNotFoundException {
        if (TypefaceCompat.DOWNLOADABLE_FONT_TRACING) {
            Trace.beginSection("FontProvider.getProvider");
        }
        try {
            List<List<byte[]>> requestCertificatesList = getCertificates(request, resources);
            ProviderCacheKey cacheKey = new ProviderCacheKey(request.getProviderAuthority(),
                    request.getProviderPackage(), requestCertificatesList);
            ProviderInfo cachedPackageInfo = sProviderCache.get(cacheKey);
            if (cachedPackageInfo != null) {
                return cachedPackageInfo;
            }
            String providerAuthority = request.getProviderAuthority();
            ProviderInfo info = packageManager.resolveContentProvider(providerAuthority, 0);
            if (info == null) {
                throw new PackageManager.NameNotFoundException("No package found for authority: "
                        + providerAuthority);
            }

            if (!info.packageName.equals(request.getProviderPackage())) {
                throw new PackageManager.NameNotFoundException("Found content provider "
                        + providerAuthority
                        + ", but package was not " + request.getProviderPackage());
            }

            List<byte[]> signatures;
            // We correctly check all signatures returned, as advised in the lint error.
            @SuppressLint("PackageManagerGetSignatures")
            PackageInfo packageInfo = packageManager.getPackageInfo(info.packageName,
                    PackageManager.GET_SIGNATURES);
            signatures = convertToByteArrayList(packageInfo.signatures);
            Collections.sort(signatures, sByteArrayComparator);
            for (int i = 0; i < requestCertificatesList.size(); ++i) {
                // Make a copy so we can sort it without modifying the incoming data.
                List<byte[]> requestSignatures = new ArrayList<>(requestCertificatesList.get(i));
                Collections.sort(requestSignatures, sByteArrayComparator);
                if (equalsByteArrayList(signatures, requestSignatures)) {
                    sProviderCache.put(cacheKey, info);
                    return info;
                }
            }
            return null;
        } finally {
            if (TypefaceCompat.DOWNLOADABLE_FONT_TRACING) {
                Trace.endSection();
            }
        }
    }

    /**
     * Do not access directly, visible for testing only.
     */
    @VisibleForTesting
    @NonNull
    static FontInfo[] query(
            Context context,
            FontRequest request,
            String authority,
            CancellationSignal cancellationSignal
    ) {
        if (TypefaceCompat.DOWNLOADABLE_FONT_TRACING) {
            Trace.beginSection("FontProvider.query");
        }
        try {
            ArrayList<FontInfo> result = new ArrayList<>();
            final Uri uri = new Uri.Builder().scheme(ContentResolver.SCHEME_CONTENT)
                    .authority(authority)
                    .build();
            final Uri fileBaseUri = new Uri.Builder().scheme(ContentResolver.SCHEME_CONTENT)
                    .authority(authority)
                    .appendPath("file")
                    .build();
            Cursor cursor = null;
            ContentQueryWrapper queryWrapper = ContentQueryWrapper.make(context, uri);
            try {
                String[] projection = {
                        FontsContractCompat.Columns._ID, FontsContractCompat.Columns.FILE_ID,
                        FontsContractCompat.Columns.TTC_INDEX,
                        FontsContractCompat.Columns.VARIATION_SETTINGS,
                        FontsContractCompat.Columns.WEIGHT, FontsContractCompat.Columns.ITALIC,
                        FontsContractCompat.Columns.RESULT_CODE};
                if (TypefaceCompat.DOWNLOADABLE_FONT_TRACING) {
                    Trace.beginSection("ContentQueryWrapper.query");
                }
                try {
                    cursor = queryWrapper.query(uri, projection, "query = ?",
                            new String[]{request.getQuery()}, null, cancellationSignal);
                } finally {
                    if (TypefaceCompat.DOWNLOADABLE_FONT_TRACING) {
                        Trace.endSection();
                    }
                }

                if (cursor != null && cursor.getCount() > 0) {
                    final int resultCodeColumnIndex = cursor.getColumnIndex(
                            FontsContractCompat.Columns.RESULT_CODE);
                    result = new ArrayList<>();
                    final int idColumnIndex = cursor.getColumnIndex(
                            FontsContractCompat.Columns._ID);
                    final int fileIdColumnIndex = cursor.getColumnIndex(
                            FontsContractCompat.Columns.FILE_ID);
                    final int ttcIndexColumnIndex = cursor.getColumnIndex(
                            FontsContractCompat.Columns.TTC_INDEX);
                    final int weightColumnIndex = cursor.getColumnIndex(
                            FontsContractCompat.Columns.WEIGHT);
                    final int italicColumnIndex = cursor.getColumnIndex(
                            FontsContractCompat.Columns.ITALIC);
                    while (cursor.moveToNext()) {
                        int resultCode = resultCodeColumnIndex != -1
                                ? cursor.getInt(resultCodeColumnIndex)
                                : FontsContractCompat.Columns.RESULT_CODE_OK;
                        final int ttcIndex = ttcIndexColumnIndex != -1
                                ? cursor.getInt(ttcIndexColumnIndex) : 0;
                        Uri fileUri;
                        if (fileIdColumnIndex == -1) {
                            long id = cursor.getLong(idColumnIndex);
                            fileUri = ContentUris.withAppendedId(uri, id);
                        } else {
                            long id = cursor.getLong(fileIdColumnIndex);
                            fileUri = ContentUris.withAppendedId(fileBaseUri, id);
                        }

                        int weight = weightColumnIndex != -1 ? cursor.getInt(weightColumnIndex)
                                : 400;
                        boolean italic = italicColumnIndex != -1 && cursor.getInt(italicColumnIndex)
                                == 1;
                        result.add(FontInfo.create(fileUri, ttcIndex, weight, italic, resultCode));
                    }
                }
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
                queryWrapper.close();
            }
            return result.toArray(new FontInfo[0]);
        } finally {
            if (TypefaceCompat.DOWNLOADABLE_FONT_TRACING) {
                Trace.endSection();
            }
        }
    }

    private static List<List<byte[]>> getCertificates(FontRequest request, Resources resources) {
        if (request.getCertificates() != null) {
            return request.getCertificates();
        }
        int resourceId = request.getCertificatesArrayResId();
        return FontResourcesParserCompat.readCerts(resources, resourceId);
    }

    private static final Comparator<byte[]> sByteArrayComparator = (l, r) -> {
        if (l.length != r.length) {
            return l.length - r.length;
        }
        for (int i = 0; i < l.length; ++i) {
            if (l[i] != r[i]) {
                return l[i] - r[i];
            }
        }
        return 0;
    };

    private static boolean equalsByteArrayList(List<byte[]> signatures,
            List<byte[]> requestSignatures) {
        if (signatures.size() != requestSignatures.size()) {
            return false;
        }
        for (int i = 0; i < signatures.size(); ++i) {
            if (!Arrays.equals(signatures.get(i), requestSignatures.get(i))) {
                return false;
            }
        }
        return true;
    }

    private static List<byte[]> convertToByteArrayList(Signature[] signatures) {
        List<byte[]> shaList = new ArrayList<>();
        for (Signature signature : signatures) {
            shaList.add(signature.toByteArray());
        }
        return shaList;
    }

    /**
     * Interface for absorbing querying ContentProvider API dependencies.
     */
    private interface ContentQueryWrapper {
        Cursor query(
                Uri uri,
                String[] projection,
                String selection,
                String[] selectionArgs,
                String sortOrder,
                CancellationSignal cancellationSignal);
        void close();

        static ContentQueryWrapper make(Context context, Uri uri) {
            if (Build.VERSION.SDK_INT < 24) {
                return new ContentQueryWrapperApi16Impl(context, uri);
            } else {
                return new ContentQueryWrapperApi24Impl(context, uri);
            }
        }
    }

    private static class ContentQueryWrapperApi16Impl implements ContentQueryWrapper {
        private final ContentProviderClient mClient;
        ContentQueryWrapperApi16Impl(Context context, Uri uri) {
            mClient = context.getContentResolver().acquireUnstableContentProviderClient(uri);
        }

        @Override
        public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
                String sortOrder, CancellationSignal cancellationSignal) {
            if (mClient == null) {
                return null;
            }
            try {
                return mClient.query(uri, projection, selection, selectionArgs, sortOrder,
                        cancellationSignal);
            } catch (RemoteException e) {
                Log.w("FontsProvider", "Unable to query the content provider", e);
                return null;
            }
        }

        @Override
        public void close() {
            if (mClient != null) {
                mClient.release();
            }
        }
    }

    @RequiresApi(24)
    private static class ContentQueryWrapperApi24Impl implements ContentQueryWrapper {
        private final ContentProviderClient mClient;
        ContentQueryWrapperApi24Impl(Context context, Uri uri) {
            mClient = context.getContentResolver().acquireUnstableContentProviderClient(uri);
        }

        @Override
        public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
                String sortOrder, CancellationSignal cancellationSignal) {
            if (mClient == null) {
                return null;
            }
            try {
                return mClient.query(uri, projection, selection, selectionArgs, sortOrder,
                        cancellationSignal);
            } catch (RemoteException e) {
                Log.w("FontsProvider", "Unable to query the content provider", e);
                return null;
            }
        }

        @Override
        public void close() {
            if (mClient != null) {
                mClient.close();
            }
        }
    }
}
