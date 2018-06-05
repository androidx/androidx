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

package androidx.textclassifier.resolver;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Build;
import android.util.Base64;
import android.util.Log;
import android.view.textclassifier.TextClassificationManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.annotation.VisibleForTesting;
import androidx.core.util.Preconditions;
import androidx.textclassifier.TextClassifier;
import androidx.textclassifier.TextClassifierService;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.List;

/**
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class TextClassifierResolver {
    private static final String TAG = TextClassifier.DEFAULT_LOG_TAG;
    @VisibleForTesting
    static final String AOSP_TEXT_CLASSIFIER_CLASSNAME =
            "android.view.textclassifier.TextClassifierImpl";

    @NonNull
    private final PackageManager mPackageManager;
    @NonNull
    private final Context mContext;

    public TextClassifierResolver(@NonNull Context context) {
        mContext = Preconditions.checkNotNull(context);
        mPackageManager = context.getPackageManager();
    }

    /**
     * Finds the best match text classifier from the given candidates. The first match always wins.
     */
    @Nullable
    public TextClassifierEntry findBestMatch(@NonNull List<TextClassifierEntry> candidates) {
        Preconditions.checkNotNull(candidates);

        for (TextClassifierEntry entry : candidates) {
            if (entry.isOem()) {
                if (isAtLeastP() && !isAospTextClassifier()) {
                    return entry;
                }
            } else if (entry.isAosp()) {
                if (isAtLeastP() && isAospTextClassifier()) {
                    return entry;
                }
            } else {
                if (!hasTextClassifierServiceInterface(entry)) {
                    Log.w(TAG, "findBestMatch: skip " + entry.packageName
                            + ", failed to find the text classifier service in the package");
                    continue;
                }
                if (!isPackageSignedProperly(entry)) {
                    Log.w(TAG, "findBestMatch: skip " + entry.packageName + ", signature mismatch");
                    continue;
                }
                return entry;
            }
        }
        return null;
    }

    private boolean hasTextClassifierServiceInterface(@NonNull TextClassifierEntry entry) {
        Intent intent = new Intent(TextClassifierService.SERVICE_INTERFACE);
        intent.setPackage(entry.packageName);
        ResolveInfo resolveInfo = mPackageManager.resolveService(intent, 0);
        return resolveInfo != null;
    }

    @SuppressLint("PackageManagerGetSignatures")
    @SuppressWarnings("deprecation") // Has to use deprecated API before P
    private boolean isPackageSignedProperly(@NonNull TextClassifierEntry textClassifierEntry) {
        byte[] targetSigningCertificate =
                Base64.decode(textClassifierEntry.certificate, Base64.NO_WRAP);
        try {
            if (isAtLeastP()) {
                return mPackageManager.hasSigningCertificate(textClassifierEntry.packageName,
                        targetSigningCertificate,
                        PackageManager.CERT_INPUT_SHA256);
            }
            PackageInfo packageInfo = mPackageManager.getPackageInfo(
                    textClassifierEntry.packageName,
                    PackageManager.GET_SIGNATURES);
            // Handling apps signed with multiple keys are tricky while it is super rare, so simply
            // not support it here. Note that this is consistent with the new P API,
            // hasSigningCertificate.
            if (packageInfo.signatures.length == 1) {
                byte[] digest = computeSha256DigestBytes(packageInfo.signatures[0].toByteArray());
                return Arrays.equals(targetSigningCertificate, digest);
            }
        } catch (PackageManager.NameNotFoundException ex) {
            // Not installed.
        }
        return false;
    }

    @NonNull
    private static byte[] computeSha256DigestBytes(@NonNull byte[] data) {
        MessageDigest messageDigest;
        try {
            messageDigest = MessageDigest.getInstance("SHA256");
        } catch (NoSuchAlgorithmException e) {
            /* can't happen */
            throw new RuntimeException("Not support SHA256!");
        }
        messageDigest.update(data);
        return messageDigest.digest();
    }

    @RequiresApi(28)
    private boolean isAospTextClassifier() {
        TextClassificationManager textClassificationManager =
                (TextClassificationManager)
                        mContext.getSystemService(Context.TEXT_CLASSIFICATION_SERVICE);
        android.view.textclassifier.TextClassifier textClassifier =
                textClassificationManager.getTextClassifier();
        return AOSP_TEXT_CLASSIFIER_CLASSNAME.equals(textClassifier.getClass().getName());
    }

    private static boolean isAtLeastP() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.P;
    }
}
