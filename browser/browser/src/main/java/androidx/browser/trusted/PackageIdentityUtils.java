/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.browser.trusted;

import android.annotation.SuppressLint;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.content.pm.SigningInfo;
import android.os.Build;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

/**
 * Abstracts away the different ways of fetching a package's fingerprints and checking that the
 * given package matches a previously generated Token on different Android versions.
 */
class PackageIdentityUtils {
    private static final String TAG = "PackageIdentity";
    private PackageIdentityUtils() {}

    @Nullable
    static List<byte[]> getFingerprintsForPackage(String name, PackageManager pm) {
        try {
            return getImpl().getFingerprintsForPackage(name, pm);
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "Could not get fingerprint for package.", e);
            return null;
        }
    }

    static boolean packageMatchesToken(String name, PackageManager pm, TokenContents token) {
        try {
            return getImpl().packageMatchesToken(name, pm, token);
        } catch (IOException | PackageManager.NameNotFoundException e) {
            Log.e(TAG, "Could not check if package matches token.", e);
            return false;
        }
    }

    private static SignaturesCompat getImpl() {
        if (Build.VERSION.SDK_INT >= 28) {
            return new Api28Implementation();
        } else {
            return new Pre28Implementation();
        }
    }

    interface SignaturesCompat {
        @Nullable
        List<byte[]> getFingerprintsForPackage(String name, PackageManager pm)
                throws PackageManager.NameNotFoundException;
        boolean packageMatchesToken(String name, PackageManager pm, TokenContents token)
                throws IOException, PackageManager.NameNotFoundException;
    }

    @RequiresApi(28)
    @SuppressWarnings("deprecation")
    static class Api28Implementation implements SignaturesCompat {
        @Override
        @Nullable
        public List<byte[]> getFingerprintsForPackage(String name, PackageManager pm)
                throws PackageManager.NameNotFoundException {
            PackageInfo packageInfo = pm.getPackageInfo(name,
                    PackageManager.GET_SIGNING_CERTIFICATES);

            List<byte[]> fingerprints = new ArrayList<>();
            SigningInfo signingInfo = packageInfo.signingInfo;

            if (signingInfo.hasMultipleSigners()) {
                // If the app has multiple signers, we can't use the new
                // PackageManager#hasSigningCertificate method and we have to use all the
                // fingerprints (as we do on Android pre-28).
                for (Signature signature : signingInfo.getApkContentsSigners()) {
                    fingerprints.add(getCertificateSHA256Fingerprint(signature));
                }
            } else {
                fingerprints.add(getCertificateSHA256Fingerprint(
                        signingInfo.getSigningCertificateHistory()[0]));
            }
            return fingerprints;
        }

        @Override
        public boolean packageMatchesToken(String name, PackageManager pm, TokenContents token)
                throws PackageManager.NameNotFoundException, IOException {
            // Exit early if we can avoid the PackageManager call.
            if (!token.getPackageName().equals(name)) return false;

            List<byte[]> fingerprints = getFingerprintsForPackage(name, pm);
            if (fingerprints == null) return false;

            if (fingerprints.size() == 1) {
                return pm.hasSigningCertificate(name, token.getFingerprint(0),
                        PackageManager.CERT_INPUT_SHA256);
            } else {
                TokenContents contents = TokenContents.create(name, fingerprints);
                return token.equals(contents);
            }
        }
    }

    static class Pre28Implementation implements SignaturesCompat {
        @SuppressWarnings("deprecation")  // For GET_SIGNATURES and PackageInfo#signatures.
        @SuppressLint("PackageManagerGetSignatures")  // We deal with multiple signatures.
        @Override
        @Nullable
        public List<byte[]> getFingerprintsForPackage(String name, PackageManager pm)
                throws PackageManager.NameNotFoundException {
            PackageInfo packageInfo = pm.getPackageInfo(name, PackageManager.GET_SIGNATURES);

            List<byte[]> fingerprints = new ArrayList<>(packageInfo.signatures.length);
            for (Signature signature : packageInfo.signatures) {
                byte[] fingerprint = getCertificateSHA256Fingerprint(signature);
                if (fingerprint == null) return null;
                fingerprints.add(fingerprint);
            }

            return fingerprints;
        }

        @Override
        public boolean packageMatchesToken(String name, PackageManager pm, TokenContents token)
                throws IOException, PackageManager.NameNotFoundException {
            // Exit early if we can avoid the PackageManager call.
            if (!name.equals(token.getPackageName())) return false;

            // On Android pre-28 we just check that the Tokens are equal - this takes into account
            // the package name and all of the fingerprints.
            List<byte[]> fingerprints = getFingerprintsForPackage(name, pm);
            if (fingerprints == null) return false;

            TokenContents contents = TokenContents.create(name, fingerprints);
            return token.equals(contents);
        }
    }

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    static @Nullable byte[] getCertificateSHA256Fingerprint(Signature signature) {
        try {
            return MessageDigest.getInstance("SHA256").digest(signature.toByteArray());
        } catch (NoSuchAlgorithmException e) {
            // This shouldn't happen.
            return null;
        }
    }
}
