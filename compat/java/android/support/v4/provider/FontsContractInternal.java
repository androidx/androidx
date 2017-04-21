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

package android.support.v4.provider;

import static android.support.annotation.RestrictTo.Scope.LIBRARY_GROUP;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ProviderInfo;
import android.content.pm.Signature;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.ParcelFileDescriptor;
import android.os.Process;
import android.support.annotation.GuardedBy;
import android.support.annotation.RestrictTo;
import android.support.annotation.VisibleForTesting;
import android.support.v4.content.res.FontResourcesParserCompat;
import android.support.v4.graphics.fonts.FontRequest;
import android.support.v4.graphics.fonts.FontResult;
import android.support.v4.os.ResultReceiver;
import android.support.v4.provider.FontsContractCompat.Columns;
import android.util.Log;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Utility class to deal with Font ContentProviders for internal use. This is kept for compatibility
 * reasons with xml layout font loading.
 * @hide
 */
@RestrictTo(LIBRARY_GROUP)
public class FontsContractInternal {
    private static final String TAG = "FontsContractCompat";

    /**
     * Constant used to identify the List of {@link ParcelFileDescriptor} item in the Bundle
     * returned to the ResultReceiver in getFont.
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    public static final String PARCEL_FONT_RESULTS = "font_results";

    // Error codes internal to the system, which can not come from a provider. To keep the number
    // space open for new provider codes, these should all be negative numbers.
    /** @hide */
    @RestrictTo(LIBRARY_GROUP)
    public static final int RESULT_CODE_PROVIDER_NOT_FOUND = -1;
    /** @hide */
    @RestrictTo(LIBRARY_GROUP)
    public static final int RESULT_CODE_WRONG_CERTIFICATES = -2;
    // Note -3 is used by Typeface to indicate the font failed to load.

    private static final int THREAD_RENEWAL_THRESHOLD_MS = 10000;

    private final Context mContext;
    private final PackageManager mPackageManager;
    private final Object mLock = new Object();
    @GuardedBy("mLock")
    private Handler mHandler;
    @GuardedBy("mLock")
    private HandlerThread mThread;

    /** @hide */
    @RestrictTo(LIBRARY_GROUP)
    public FontsContractInternal(Context context) {
        mContext = context.getApplicationContext();
        mPackageManager = mContext.getPackageManager();
    }

    @VisibleForTesting
    FontsContractInternal(Context context, PackageManager packageManager) {
        mContext = context;
        mPackageManager = packageManager;
    }

    // We use a background thread to post the content resolving work for all requests on. This
    // thread should be quit/stopped after all requests are done.
    private final Runnable mReplaceDispatcherThreadRunnable = new Runnable() {
        @Override
        public void run() {
            synchronized (mLock) {
                if (mThread != null) {
                    mThread.quit();
                    mThread = null;
                    mHandler = null;
                }
            }
        }
    };

    /** @hide */
    @RestrictTo(LIBRARY_GROUP)
    public void getFont(final FontRequest request, final ResultReceiver receiver) {
        synchronized (mLock) {
            if (mHandler == null) {
                mThread = new HandlerThread("fonts", Process.THREAD_PRIORITY_BACKGROUND);
                mThread.start();
                mHandler = new Handler(mThread.getLooper());
            }
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    ProviderInfo providerInfo = getProvider(request, receiver);
                    if (providerInfo == null) {
                        return;
                    }
                    getFontFromProvider(request, receiver, providerInfo.authority);
                }
            });
            mHandler.removeCallbacks(mReplaceDispatcherThreadRunnable);
            mHandler.postDelayed(mReplaceDispatcherThreadRunnable, THREAD_RENEWAL_THRESHOLD_MS);
        }
    }

    @VisibleForTesting
    ProviderInfo getProvider(FontRequest request, ResultReceiver receiver) {
        String providerAuthority = request.getProviderAuthority();
        ProviderInfo info = mPackageManager.resolveContentProvider(providerAuthority, 0);
        if (info == null) {
            Log.e(TAG, "Can't find content provider " + providerAuthority);
            receiver.send(RESULT_CODE_PROVIDER_NOT_FOUND, null);
            return null;
        }

        if (!info.packageName.equals(request.getProviderPackage())) {
            Log.e(TAG, "Found content provider " + providerAuthority + ", but package was not "
                    + request.getProviderPackage());
            receiver.send(RESULT_CODE_PROVIDER_NOT_FOUND, null);
            return null;
        }

        List<byte[]> signatures;
        try {
            PackageInfo packageInfo = mPackageManager.getPackageInfo(info.packageName,
                    PackageManager.GET_SIGNATURES);
            signatures = convertToByteArrayList(packageInfo.signatures);
            Collections.sort(signatures, sByteArrayComparator);
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "Can't find content provider " + providerAuthority, e);
            receiver.send(RESULT_CODE_PROVIDER_NOT_FOUND, null);
            return null;
        }
        List<List<byte[]>> requestCertificatesList = getCertificates(request);
        for (int i = 0; i < requestCertificatesList.size(); ++i) {
            // Make a copy so we can sort it without modifying the incoming data.
            List<byte[]> requestSignatures = new ArrayList<>(requestCertificatesList.get(i));
            Collections.sort(requestSignatures, sByteArrayComparator);
            if (equalsByteArrayList(signatures, requestSignatures)) {
                return info;
            }
        }
        Log.e(TAG, "Certificates don't match for given provider " + providerAuthority);
        receiver.send(RESULT_CODE_WRONG_CERTIFICATES, null);
        return null;
    }

    private List<List<byte[]>> getCertificates(FontRequest request) {
        if (request.getCertificates() != null) {
            return request.getCertificates();
        }
        int resourceId = request.getCertificatesArrayResId();
        return FontResourcesParserCompat.readCerts(mContext.getResources(), resourceId);
    }

    private static final Comparator<byte[]> sByteArrayComparator = new Comparator<byte[]>() {
        @Override
        public int compare(byte[] l, byte[] r) {
            if (l.length != r.length) {
                return l.length - r.length;
            }
            for (int i = 0; i < l.length; ++i) {
                if (l[i] != r[i]) {
                    return l[i] - r[i];
                }
            }
            return 0;
        }
    };

    private boolean equalsByteArrayList(List<byte[]> signatures, List<byte[]> requestSignatures) {
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

    private List<byte[]> convertToByteArrayList(Signature[] signatures) {
        List<byte[]> shas = new ArrayList<>();
        for (int i = 0; i < signatures.length; ++i) {
            shas.add(signatures[i].toByteArray());
        }
        return shas;
    }

    @VisibleForTesting
    void getFontFromProvider(FontRequest request, ResultReceiver receiver,
            String authority) {
        ArrayList<FontResult> result = new ArrayList<>();
        final Uri uri = new Uri.Builder().scheme(ContentResolver.SCHEME_CONTENT)
                .authority(authority)
                .build();
        final Uri fileBaseUri = new Uri.Builder().scheme(ContentResolver.SCHEME_CONTENT)
                .authority(authority)
                .appendPath("file")
                .build();
        Cursor cursor = null;
        try {
            cursor = mContext.getContentResolver().query(uri, new String[] {
                    Columns._ID, Columns.FILE_ID, Columns.TTC_INDEX,
                    Columns.VARIATION_SETTINGS, Columns.WEIGHT, Columns.ITALIC,
                    Columns.RESULT_CODE },
                    "query = ?", new String[] { request.getQuery() }, null);
            if (cursor != null && cursor.getCount() > 0) {
                final int resultCodeColumnIndex = cursor.getColumnIndex(Columns.RESULT_CODE);
                final int idColumnIndex = cursor.getColumnIndex(Columns._ID);
                final int fileIdColumnIndex = cursor.getColumnIndex(Columns.FILE_ID);
                final int ttcIndexColumnIndex = cursor.getColumnIndex(Columns.TTC_INDEX);
                final int vsColumnIndex = cursor.getColumnIndex(Columns.VARIATION_SETTINGS);
                final int weightColumnIndex = cursor.getColumnIndex(Columns.WEIGHT);
                final int italicColumnIndex = cursor.getColumnIndex(Columns.ITALIC);
                while (cursor.moveToNext()) {
                    int resultCode = resultCodeColumnIndex != -1
                            ? cursor.getInt(resultCodeColumnIndex) : Columns.RESULT_CODE_OK;
                    if (resultCode != Columns.RESULT_CODE_OK) {
                        if (resultCode < 0) {
                            // Negative values are reserved for the internal errors.
                            resultCode = Columns.RESULT_CODE_FONT_NOT_FOUND;
                        }
                        for (int i = 0; i < result.size(); ++i) {
                            try {
                                result.get(i).getFileDescriptor().close();
                            } catch (IOException e) {
                                // Ignore, as we are closing fds for cleanup.
                            }
                        }
                        receiver.send(resultCode, null);
                        return;
                    }
                    Uri fileUri;
                    if (fileIdColumnIndex == -1) {
                        long id = cursor.getLong(idColumnIndex);
                        fileUri = ContentUris.withAppendedId(uri, id);
                    } else {
                        long id = cursor.getLong(fileIdColumnIndex);
                        fileUri = ContentUris.withAppendedId(fileBaseUri, id);
                    }
                    try {
                        ParcelFileDescriptor pfd =
                                mContext.getContentResolver().openFileDescriptor(fileUri, "r");
                        if (pfd != null) {
                            final int ttcIndex = ttcIndexColumnIndex != -1
                                    ? cursor.getInt(ttcIndexColumnIndex) : 0;
                            final String variationSettings = vsColumnIndex != -1
                                    ? cursor.getString(vsColumnIndex) : null;
                            int weight;
                            boolean italic;
                            if (weightColumnIndex != -1 && italicColumnIndex != -1) {
                                weight = cursor.getInt(weightColumnIndex);
                                italic = cursor.getInt(italicColumnIndex) == 1;
                            } else {
                                weight = 400;
                                italic = false;
                            }
                            result.add(new FontResult(
                                    pfd, ttcIndex, variationSettings, weight, italic));
                        }
                    } catch (FileNotFoundException e) {
                        Log.e(TAG, "FileNotFoundException raised when interacting with content "
                                + "provider " + authority, e);
                    }
                }
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        if (result != null && !result.isEmpty()) {
            Bundle bundle = new Bundle();
            bundle.putParcelableArrayList(PARCEL_FONT_RESULTS, result);
            receiver.send(Columns.RESULT_CODE_OK, bundle);
            return;
        }
        receiver.send(Columns.RESULT_CODE_FONT_NOT_FOUND, null);
    }
}
