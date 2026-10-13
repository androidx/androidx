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
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ProviderInfo;
import android.content.pm.Signature;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Typeface;
import android.graphics.fonts.Font;
import android.net.Uri;
import android.os.Build;
import android.os.CancellationSignal;
import android.os.RemoteException;
import android.util.Log;

import androidx.annotation.RequiresApi;
import androidx.annotation.VisibleForTesting;
import androidx.collection.LruCache;
import androidx.core.content.res.FontResourcesParserCompat;
import androidx.core.graphics.TypefaceCompat;
import androidx.core.provider.FontsContractCompat.FontFamilyResult;
import androidx.core.provider.FontsContractCompat.FontInfo;
import androidx.tracing.Trace;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

class FontProvider {
    private FontProvider() {}

    private static final String VARIABLE_FONT_QUERY_PARAM = "VF";

    static @NonNull FontFamilyResult getFontFamilyResult(@NonNull Context context,
            @NonNull List<FontRequest> requests, @Nullable CancellationSignal cancellationSignal)
            throws PackageManager.NameNotFoundException {
        if (TypefaceCompat.DOWNLOADABLE_FONT_TRACING) {
            Trace.beginSection("FontProvider.getFontFamilyResult");
        }
        try {
            ArrayList<FontInfo[]> queryResults = new ArrayList<>();
            for (int i = 0; i < requests.size(); i++) {
                FontRequest request = requests.get(i);

                if (Build.VERSION.SDK_INT >= 31) {
                    final String systemFont = request.getSystemFont();
                    final Typeface typeface = TypefaceCompat.getSystemFontFamily(systemFont);
                    if (typeface != null) {
                        Font font = TypefaceCompat.guessPrimaryFont(typeface);
                        if (font != null) {
                            // We cannot store the Font instance directly into FontInfo objects.
                            // Instead, we will re-resolve the font at the time of Typeface
                            // creation. The performance overhead should be minimal because of the
                            // system's layout cache.
                            queryResults.add(new FontInfo[]{
                                    new FontInfo(systemFont, request.getVariationSettings())
                            });
                            continue;
                        }
                    }
                }

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
        private final String mAuthority;
        private final String mPackageName;
        private final int mCertificatesArray;
        private final int mSystemCertificatesArray;
        private final List<List<byte[]>> mCertificates;
        private final List<List<byte[]>> mSystemCertificates;

        ProviderCacheKey(@NonNull String authority, @NonNull String packageName,
                int certificatesArray, int systemCertificatesArray,
                @Nullable List<List<byte[]>> certificates,
                @Nullable List<List<byte[]>> systemCertificates) {
            this.mAuthority = authority;
            this.mPackageName = packageName;
            this.mCertificatesArray = certificatesArray;
            this.mSystemCertificatesArray = systemCertificatesArray;
            this.mCertificates = certificates;
            this.mSystemCertificates = systemCertificates;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof ProviderCacheKey)) return false;
            ProviderCacheKey that = (ProviderCacheKey) o;
            if (!mAuthority.equals(that.mAuthority) || !mPackageName.equals(that.mPackageName)) {
                return false;
            }
            if (mCertificatesArray != that.mCertificatesArray
                    || mSystemCertificatesArray != that.mSystemCertificatesArray) {
                return false;
            }
            if (mCertificatesArray == 0
                    && !equalsByteArrayLists(mCertificates, that.mCertificates)) {
                return false;
            }
            if (mSystemCertificatesArray == 0
                    && !equalsByteArrayLists(mSystemCertificates, that.mSystemCertificates)) {
                return false;
            }
            return true;
        }

        @Override
        public int hashCode() {
            int result = Objects.hash(mAuthority, mPackageName, mCertificatesArray,
                    mSystemCertificatesArray);
            if (mCertificatesArray == 0) {
                result = 31 * result + hashByteArrayLists(mCertificates);
            }
            if (mSystemCertificatesArray == 0) {
                result = 31 * result + hashByteArrayLists(mSystemCertificates);
            }
            return result;
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
    static @Nullable ProviderInfo getProvider(
            @NonNull PackageManager packageManager,
            @NonNull FontRequest request,
            @Nullable Resources resources
    )
            throws PackageManager.NameNotFoundException {
        if (TypefaceCompat.DOWNLOADABLE_FONT_TRACING) {
            Trace.beginSection("FontProvider.getProvider");
        }
        try {
            int certsArray = request.getCertificatesArrayResId();
            int systemCertsArray = request.getSystemCertificatesArrayResId();
            List<List<byte[]>> requestCertificatesList = null;
            List<List<byte[]>> requestSystemCertificatesList = null;

            if (certsArray == 0) {
                requestCertificatesList = request.getCertificates();
            }
            if (systemCertsArray == 0) {
                requestSystemCertificatesList = request.getSystemCertificates();
            }

            ProviderCacheKey cacheKey = new ProviderCacheKey(
                    request.getProviderAuthority(),
                    request.getProviderPackage(),
                    certsArray,
                    systemCertsArray,
                    requestCertificatesList,
                    requestSystemCertificatesList);

            ProviderInfo cachedPackageInfo = sProviderCache.get(cacheKey);
            if (cachedPackageInfo != null) {
                return cachedPackageInfo;
            }

            if (certsArray != 0) {
                requestCertificatesList = getCertificates(request, resources);
            }
            if (systemCertsArray != 0) {
                requestSystemCertificatesList = getSystemCertificates(request, resources);
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
            if (requestCertificatesList != null) {
                for (int i = 0; i < requestCertificatesList.size(); ++i) {
                    // Make a copy so we can sort it without modifying the incoming data.
                    List<byte[]> requestSignatures = new ArrayList<>(
                            requestCertificatesList.get(i));
                    Collections.sort(requestSignatures, sByteArrayComparator);
                    if (equalsByteArrayList(signatures, requestSignatures)) {
                        sProviderCache.put(cacheKey, info);
                        return info;
                    }
                }
            }
            if (requestSystemCertificatesList != null && !requestSystemCertificatesList.isEmpty()) {
                ApplicationInfo appInfo = info.applicationInfo != null
                        ? info.applicationInfo : packageInfo.applicationInfo;
                boolean isSystemApp = appInfo != null
                        && (appInfo.flags & (ApplicationInfo.FLAG_SYSTEM
                        | ApplicationInfo.FLAG_UPDATED_SYSTEM_APP)) != 0;
                if (isSystemApp) {
                    for (int i = 0; i < requestSystemCertificatesList.size(); ++i) {
                        List<byte[]> requestSignatures = new ArrayList<>(
                                requestSystemCertificatesList.get(i));
                        Collections.sort(requestSignatures, sByteArrayComparator);
                        if (equalsByteArrayList(signatures, requestSignatures)) {
                            sProviderCache.put(cacheKey, info);
                            return info;
                        }
                    }
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
    static FontInfo @NonNull [] query(
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
                            getSelectionArgs(request), null, cancellationSignal);
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

                        // Font variation settings can originate from either a font provider or an
                        // XML definition. While merging or prioritizing these sources would be
                        // ideal, settings from font providers have historically been ignored and
                        // are currently unused by any provider.
                        // Therefore, XML-defined settings are used exclusively for now.
                        String fontVariationSettings = request.getVariationSettings();

                        result.add(new FontInfo(fileUri, ttcIndex, weight, italic,
                                fontVariationSettings, resultCode));
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

    protected static String[] getSelectionArgs(FontRequest fontRequest) {
        String variationSettings = fontRequest.getVariationSettings();
        if (variationSettings != null && !variationSettings.isBlank()) {
            // If variation settings are present, send the "VF" in the selectionArgs[1]. This
            // enables font provider to return a variable font.
            return new String[]{fontRequest.getQuery(), VARIABLE_FONT_QUERY_PARAM};
        } else {
            return new String[]{fontRequest.getQuery()};
        }
    }

    private static List<List<byte[]>> getCertificates(FontRequest request, Resources resources) {
        if (request.getCertificates() != null) {
            return request.getCertificates();
        }
        int resourceId = request.getCertificatesArrayResId();
        return FontResourcesParserCompat.readCerts(resources, resourceId);
    }

    private static List<List<byte[]>> getSystemCertificates(FontRequest request,
            Resources resources) {
        if (request.getSystemCertificates() != null) {
            return request.getSystemCertificates();
        }
        int resourceId = request.getSystemCertificatesArrayResId();
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

    private static boolean equalsByteArrayLists(
            @Nullable List<List<byte[]>> list1,
            @Nullable List<List<byte[]>> list2) {
        if (list1 == list2) {
            return true;
        }
        if (list1 == null || list2 == null) {
            return false;
        }
        if (list1.size() != list2.size()) {
            return false;
        }
        for (int i = 0; i < list1.size(); ++i) {
            if (!equalsByteArrayList(list1.get(i), list2.get(i))) {
                return false;
            }
        }
        return true;
    }

    private static int hashByteArrayLists(@Nullable List<List<byte[]>> list) {
        if (list == null) {
            return 0;
        }
        int hash = 1;
        for (int i = 0; i < list.size(); ++i) {
            List<byte[]> inner = list.get(i);
            int innerHash = 1;
            if (inner != null) {
                for (int j = 0; j < inner.size(); ++j) {
                    innerHash = 31 * innerHash + Arrays.hashCode(inner.get(j));
                }
            }
            hash = 31 * hash + innerHash;
        }
        return hash;
    }

    private static boolean equalsByteArrayList(List<byte[]> signatures,
            List<byte[]> requestSignatures) {
        if (signatures == requestSignatures) {
            return true;
        }
        if (signatures == null || requestSignatures == null) {
            return false;
        }
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
