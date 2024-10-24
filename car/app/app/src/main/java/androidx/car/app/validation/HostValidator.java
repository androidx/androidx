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

package androidx.car.app.validation;

import static androidx.car.app.utils.LogTags.TAG_HOST_VALIDATION;

import static java.util.Objects.requireNonNull;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.content.res.Resources;
import android.os.Build;
import android.os.Process;
import android.util.Log;
import android.util.Pair;

import androidx.annotation.ArrayRes;
import androidx.annotation.RequiresApi;
import androidx.car.app.CarAppService;
import androidx.car.app.HostInfo;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Validates that the calling package is authorized to connect to a {@link CarAppService}.
 *
 * <p>Host are expected to either hold <code>android.car.permission.TEMPLATE_RENDERER</code>
 * privileged permission, or be included in the allow-listed set of hosts, identified by their
 * signatures.
 *
 * @see HostValidator.Builder
 */
public final class HostValidator {
    /**
     * System permission used to identify valid hosts (only used by hosts running on Android API
     * level 31 or later). Other hosts must be allow-listed using
     * {@link HostValidator.Builder#addAllowedHost(String, String)} or
     * {@link HostValidator.Builder#addAllowedHosts(int)}
     */
    public static final String TEMPLATE_RENDERER_PERMISSION = "android.car.permission"
            + ".TEMPLATE_RENDERER";

    private final Map<String, List<String>> mAllowedHosts;
    private final boolean mAllowAllHosts;
    private final Map<String, Pair<Integer, Boolean>> mCallerChecked = new HashMap<>();
    private final @Nullable PackageManager mPackageManager;

    HostValidator(@Nullable PackageManager packageManager,
            @NonNull Map<String, List<String>> allowedHosts, boolean allowAllHosts) {
        mPackageManager = packageManager;
        mAllowedHosts = allowedHosts;
        mAllowAllHosts = allowAllHosts;
    }

    /**
     * A host validator that doesn't block any hosts.
     *
     * <p>This is intended to be used only during development.
     *
     * @see CarAppService#createHostValidator()
     */
    public static final @NonNull HostValidator ALLOW_ALL_HOSTS_VALIDATOR = new HostValidator(null,
            new HashMap<>(), true);

    /**
     * Returns whether the given host is allowed to bind to this client.
     */
    public boolean isValidHost(@NonNull HostInfo hostInfo) {
        requireNonNull(hostInfo);
        if (Log.isLoggable(TAG_HOST_VALIDATION, Log.DEBUG)) {
            Log.d(TAG_HOST_VALIDATION, "Evaluating " + hostInfo);
        }

        if (mAllowAllHosts) {
            if (Log.isLoggable(TAG_HOST_VALIDATION, Log.DEBUG)) {
                Log.d(TAG_HOST_VALIDATION, "Accepted - Validator disabled, all hosts allowed");
            }
            return true;
        }

        // Do not evaluate the same host twice
        Boolean previousResult = checkCache(hostInfo);
        if (previousResult != null) {
            return previousResult;
        }

        // Validate
        boolean isValid = validateHost(hostInfo);

        // Update cache and return
        updateCache(hostInfo, isValid);
        return isValid;
    }

    /**
     * Returns a map from package name to signature digests of each of the allowed hosts.
     */
    public @NonNull Map<String, List<String>> getAllowedHosts() {
        return Collections.unmodifiableMap(mAllowedHosts);
    }

    @SuppressWarnings("deprecation")
    private @Nullable PackageInfo getPackageInfo(String packageName) {
        try {
            if (mPackageManager == null) {
                Log.d(TAG_HOST_VALIDATION,
                        "PackageManager is null. Package info cannot be found for package "
                                + packageName);
                return null;
            }

            if (Build.VERSION.SDK_INT >= 28) {
                return Api28Impl.getPackageInfo(mPackageManager, packageName);
            } else {
                return mPackageManager.getPackageInfo(packageName,
                        PackageManager.GET_SIGNATURES | PackageManager.GET_PERMISSIONS);
            }
        } catch (PackageManager.NameNotFoundException e) {
            Log.w(TAG_HOST_VALIDATION, "Package " + packageName + " not found", e);
            return null;
        }
    }

    private boolean validateHost(HostInfo hostInfo) {
        String hostPackageName = hostInfo.getPackageName();
        PackageInfo packageInfo = getPackageInfo(hostPackageName);
        if (packageInfo == null) {
            Log.w(TAG_HOST_VALIDATION, "Rejected - package name " + hostPackageName + " not found");
            return false;
        }

        Signature[] signatures = getSignatures(packageInfo);
        if (signatures == null || signatures.length == 0) {
            Log.w(TAG_HOST_VALIDATION, "Package " + hostPackageName + " is not signed or "
                    + "it has more than one signature");
            return false;
        }

        // Verify that we got things right (uid from package info should match uid reported by
        // binder)
        int uid = packageInfo.applicationInfo.uid;
        if (uid != hostInfo.getUid()) {
            throw new IllegalStateException("Host " + hostInfo + " doesn't match caller's actual "
                    + "UID " + uid);
        }

        boolean hasPermission = hasPermissionGranted(packageInfo, TEMPLATE_RENDERER_PERMISSION);
        boolean isAllowListed = isAllowListed(hostPackageName, signatures);

        // Validate
        if (uid == Process.myUid()) {
            // If it's the same app making the call, allow it.
            if (Log.isLoggable(TAG_HOST_VALIDATION, Log.DEBUG)) {
                Log.d(TAG_HOST_VALIDATION, "Accepted - Local service call");
            }
            return true;
        }

        if (isAllowListed) {
            // If it's one of the apps in the allow list, allow it.
            if (Log.isLoggable(TAG_HOST_VALIDATION, Log.DEBUG)) {
                Log.d(TAG_HOST_VALIDATION, "Accepted - Host in allow-list");
            }
            return true;
        }

        if (uid == Process.SYSTEM_UID) {
            // If the system is making the call, allow it.
            if (Log.isLoggable(TAG_HOST_VALIDATION, Log.DEBUG)) {
                Log.d(TAG_HOST_VALIDATION, "Accepted - System binding");
            }
            return true;
        }

        if (hasPermission) {
            if (Log.isLoggable(TAG_HOST_VALIDATION, Log.DEBUG)) {
                Log.d(TAG_HOST_VALIDATION, "Accepted - Host has " + TEMPLATE_RENDERER_PERMISSION);
            }
            return true;
        }

        Log.e(TAG_HOST_VALIDATION, String.format("Unrecognized host.\n"
                        + "If this is a valid caller, please add the following to your "
                        + "CarAppService#createHostValidator() implementation:\n"
                        + "return new HostValidator.Builder(context)\n"
                        + "\t.addAllowedHost(\"%s\", \"%s\");\n"
                        + "\t.build()",
                hostPackageName, getDigest(signatures[0])));
        return false;
    }

    private boolean isAllowListed(String hostPackageName, Signature[] signatures) {
        List<String> allowedDigests = mAllowedHosts.get(hostPackageName);
        if (allowedDigests == null) {
            return false;
        }
        for (Signature signature : signatures) {
            String digest = getDigest(signature);
            if (allowedDigests.contains(digest)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns {@code true} if the host was already approved, {@code false} if it was previously
     * rejected, and {@code null} if this is the first time this host is evaluated.
     */
    private @Nullable Boolean checkCache(HostInfo hostInfo) {
        Pair<Integer, Boolean> entry = mCallerChecked.get(hostInfo.getPackageName());
        if (entry == null) {
            return null;
        }
        // Host UID might change when it is re-installed/updated. In that case, we force the host
        // to be evaluated again.
        if (entry.first != hostInfo.getUid()) {
            return null;
        }
        return entry.second;
    }

    private void updateCache(HostInfo hostInfo, boolean isValid) {
        mCallerChecked.put(hostInfo.getPackageName(), Pair.create(hostInfo.getUid(), isValid));
    }

    private static @Nullable MessageDigest getMessageDigest() {
        try {
            return MessageDigest.getInstance("SHA256");
        } catch (NoSuchAlgorithmException e) {
            Log.e(TAG_HOST_VALIDATION, "Could not find SHA256 hash algorithm", e);
            return null;
        }
    }

    @SuppressWarnings("deprecation")
    private Signature @Nullable [] getSignatures(PackageInfo packageInfo) {
        if (Build.VERSION.SDK_INT >= 28) {
            // Implementation extracted to inner class to improve runtime performance.
            return Api28Impl.getSignatures(packageInfo);
        } else {
            if (packageInfo.signatures == null || packageInfo.signatures.length != 1) {
                // Security best practices dictate that an app should be signed with exactly one (1)
                // signature. Because of this, if there are multiple signatures, reject it.
                return null;
            }
            return packageInfo.signatures;
        }
    }

    private @Nullable String getDigest(Signature signature) {
        byte[] data = signature.toByteArray();
        MessageDigest messageDigest = getMessageDigest();
        if (messageDigest == null) {
            // Error has been already logged in getMessageDigest()
            return null;
        }
        messageDigest.update(data);
        byte[] digest = messageDigest.digest();
        StringBuilder sb = new StringBuilder(digest.length * 3 - 1);
        for (byte b : digest) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    private static boolean hasPermissionGranted(PackageInfo packageInfo,
            String permission) {
        if (packageInfo.requestedPermissionsFlags == null
                || packageInfo.requestedPermissions == null) {
            return false;
        }
        for (int i = 0; i < packageInfo.requestedPermissionsFlags.length; i++) {
            if (((packageInfo.requestedPermissionsFlags[i]
                          & PackageInfo.REQUESTED_PERMISSION_GRANTED) != 0)
                        && i < packageInfo.requestedPermissions.length
                        && permission.equals(packageInfo.requestedPermissions[i])) {
                return true;
            }
        }
        return false;
    }

    /**
     * Version-specific static inner classes to avoid verification errors that negatively affect
     * run-time performance.
     */
    @RequiresApi(28)
    private static final class Api28Impl {
        private Api28Impl() {
        }

        static Signature @Nullable [] getSignatures(@NonNull PackageInfo packageInfo) {
            if (packageInfo.signingInfo == null) {
                return null;
            }
            return packageInfo.signingInfo.getSigningCertificateHistory();
        }

        @SuppressWarnings("deprecation")
        static @NonNull PackageInfo getPackageInfo(@NonNull PackageManager packageManager,
                @NonNull String packageName) throws PackageManager.NameNotFoundException {
            return packageManager.getPackageInfo(packageName,
                    PackageManager.GET_SIGNING_CERTIFICATES | PackageManager.GET_PERMISSIONS);
        }
    }

    /**
     * Builder of {@link HostValidator}.
     *
     * <p>Allows applications to customize the {@link HostValidator} that will be used to verify
     * whether a caller is a valid templates host.
     */
    public static final class Builder {
        private final Map<String, List<String>> mAllowedHosts = new HashMap<>();
        private final Context mContext;

        /** Returns an empty {@link Builder} instance. */
        public Builder(@NonNull Context context) {
            mContext = context;
        }

        /**
         * Adds a host to the allow list.
         *
         * @param packageName host package name (as reported by {@link PackageManager})
         * @param digest      SHA256 digest of the DER encoding of the allow-listed host
         *                    certificate, formatted as 32 lowercase 2 digits  hexadecimal values
         *                    separated by colon (e.g.:"000102030405060708090a0b0c0d0e0f101112131415
         *                    161718191a1b1c1d1e1f"). When using
         *                    <a href="https://developer.android.com/about/versions/pie/android-9.0#apk-key-rotation">signature
         *                    rotation</a>, this digest should correspond to the initial signing
         *                    certificate
         */
        public @NonNull Builder addAllowedHost(@NonNull String packageName,
                @NonNull String digest) {
            requireNonNull(packageName);
            requireNonNull(digest);
            List<String> digests = mAllowedHosts.get(packageName);
            if (digests == null) {
                digests = new ArrayList<>();
                mAllowedHosts.put(packageName, digests);
            }
            digests.add(digest);
            return this;
        }

        /**
         * Adds a hosts to the allow list.
         *
         * <p>Allow-listed hosts are retrieved from a string-array resource, encoded as
         * [digest,package-name] pairs separated by comma. See
         * {@link #addAllowedHost(String, String)} for details on signature digest and
         * package-name formatting.
         *
         * @param allowListedHostsRes string-array resource identifier
         * @throws IllegalArgumentException if the provided resource doesn't exist or if the entries
         *                                  in the given resource are not formatted as expected
         */
        @SuppressLint("MissingGetterMatchingBuilder")
        public @NonNull Builder addAllowedHosts(@ArrayRes int allowListedHostsRes) {
            Resources resources = mContext.getResources();
            String[] entries = resources.getStringArray(allowListedHostsRes);
            if (entries == null) {
                throw new IllegalArgumentException("Invalid allowlist res id: "
                        + allowListedHostsRes);
            }
            for (String entry : entries) {
                // Using limit -1 as suggested by https://errorprone.info/bugpattern/StringSplitter
                String[] keyValue = entry.split(",", -1);
                if (keyValue.length != 2) {
                    throw new IllegalArgumentException("Invalid allowed host entry: '" + entry
                            + "'");
                }
                addAllowedHost(cleanUp(keyValue[1]), cleanUp(keyValue[0]));
            }
            return this;
        }

        /** Returns a new {@link HostValidator} */
        public @NonNull HostValidator build() {
            return new HostValidator(mContext.getPackageManager(), mAllowedHosts, false);
        }

        private String cleanUp(String value) {
            return value.toLowerCase(Locale.US).replace(" ", "");
        }
    }
}
