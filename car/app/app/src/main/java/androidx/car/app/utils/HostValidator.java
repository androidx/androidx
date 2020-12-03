/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.car.app.utils;

import static androidx.car.app.utils.CommonUtils.TAG_HOST_VALIDATION;

import static java.util.Objects.requireNonNull;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.os.Process;
import android.util.Log;
import android.util.Pair;

import androidx.annotation.ArrayRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.car.app.CarAppService;
import androidx.car.app.HostInfo;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

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
     * {@link HostValidator.Builder#addAllowListedHost(String, String)} or
     * {@link HostValidator.Builder#addAllowListedHosts(int)}
     */
    public static final String TEMPLATE_RENDERER_PERMISSION = "android.car.permission"
            + ".TEMPLATE_RENDERER";

    private final Map<String, String> mAllowListedHosts;
    private final Set<String> mDenyListedHosts;
    private final boolean mAllowUnknownHosts;
    private final Map<String, Pair<Integer, Boolean>> mCallerChecked = new HashMap<>();
    private final PackageManager mPackageManager;
    @Nullable
    private final MessageDigest mMessageDigest;

    HostValidator(@NonNull PackageManager packageManager,
            @NonNull Map<String, String> allowListedHosts,
            @NonNull Set<String> denyListedHosts,
            boolean allowUnknownHosts) {
        mPackageManager = packageManager;
        mAllowListedHosts = allowListedHosts;
        mDenyListedHosts = denyListedHosts;
        mAllowUnknownHosts = allowUnknownHosts;
        mMessageDigest = getMessageDigest();
    }

    /**
     * @return true if the given host is allowed to bind to this client, or false otherwise
     */
    public boolean isValidHost(@NonNull HostInfo hostInfo) {
        requireNonNull(hostInfo);
        Log.d(TAG_HOST_VALIDATION, "Evaluating " + hostInfo);

        if (mDenyListedHosts.contains(hostInfo.getPackageName())) {
            Log.d(TAG_HOST_VALIDATION, "Rejected - Host is in the deny list");
            return false;
        }

        if (mAllowUnknownHosts) {
            Log.d(TAG_HOST_VALIDATION, "Accepted - Unknown hosts allowed");
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

    @SuppressWarnings("deprecation")
    @Nullable
    private PackageInfo getPackageInfo(String packageName) {
        try {
            return mPackageManager.getPackageInfo(packageName,
                    PackageManager.GET_SIGNATURES | PackageManager.GET_PERMISSIONS);
        } catch (PackageManager.NameNotFoundException e) {
            Log.d(TAG_HOST_VALIDATION, "Package " + packageName + " not found.", e);
            return null;
        }
    }

    private boolean validateHost(@NonNull HostInfo hostInfo) {
        String hostPackageName = hostInfo.getPackageName();
        PackageInfo packageInfo = getPackageInfo(hostPackageName);
        if (packageInfo == null) {
            Log.d(TAG_HOST_VALIDATION, "Rejected - package name " + hostPackageName + " not found");
            return false;
        }

        String signature = getSignature(packageInfo);
        if (signature == null) {
            Log.d(TAG_HOST_VALIDATION, "Package " + hostPackageName + " is not signed or "
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
        boolean isAllowListed = hostPackageName.equals(mAllowListedHosts.get(signature));

        // Validate
        if (uid == Process.myUid()) {
            // If it's the same app making the call, allow it.
            Log.d(TAG_HOST_VALIDATION, "Accepted - Local service call");
            return true;
        }

        if (isAllowListed) {
            // If it's one of the apps in the allow list, allow it.
            Log.d(TAG_HOST_VALIDATION, "Accepted - Host in allow-list");
            return true;
        }

        if (uid == Process.SYSTEM_UID) {
            // If the system is making the call, allow it.
            Log.d(TAG_HOST_VALIDATION, "Accepted - System binding");
            return true;
        }

        if (hasPermission) {
            Log.d(TAG_HOST_VALIDATION, "Accepted - Host has " + TEMPLATE_RENDERER_PERMISSION);
            return true;
        }

        Log.i(TAG_HOST_VALIDATION, String.format("Unrecognized host. If this is a valid caller, "
                + "please add the following to your CarAppService#onConfigureHostValidator() "
                + "implementation: hostValidator.allowHost(\"%s\", \"%s\");", signature,
                hostPackageName));
        return false;
    }

    /**
     * @return true if the host was already approved, false if it was previously rejected, and
     * null if this is the first time this host is evaluated.
     */
    @Nullable
    private Boolean checkCache(@NonNull HostInfo hostInfo) {
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

    private void updateCache(@NonNull HostInfo hostInfo, boolean isValid) {
        mCallerChecked.put(hostInfo.getPackageName(), Pair.create(hostInfo.getUid(), isValid));
    }

    @Nullable
    private static MessageDigest getMessageDigest() {
        try {
            return MessageDigest.getInstance("SHA256");
        } catch (NoSuchAlgorithmException e) {
            Log.e(TAG_HOST_VALIDATION, "Could not find SHA256 hash algorithm", e);
            return null;
        }
    }

    @SuppressWarnings("deprecation")
    @Nullable
    private String getSignature(@NonNull PackageInfo packageInfo) {
        // PackageInfo#signatures is deprecated, but the replacement has a min API of 28
        if (packageInfo.signatures == null || packageInfo.signatures.length != 1) {
            // Security best practices dictate that an app should be signed with exactly one (1)
            // signature. Because of this, if there are multiple signatures, reject it.
            return null;
        }
        if (mMessageDigest == null) {
            Log.e(TAG_HOST_VALIDATION, "Unable to retrieve certificate digest.");
            return null;
        }
        byte[] signature = packageInfo.signatures[0].toByteArray();
        mMessageDigest.update(signature);
        byte[] digest = mMessageDigest.digest();
        StringBuilder sb = new StringBuilder(digest.length * 3 - 1);
        for (int i = 0; i < digest.length; i++) {
            sb.append(String.format("%02x", digest[i]));
        }
        return sb.toString();
    }

    private static boolean hasPermissionGranted(@NonNull PackageInfo packageInfo,
            @NonNull String permission) {
        if (packageInfo.requestedPermissionsFlags == null
                || packageInfo.requestedPermissions == null) {
            return false;
        }
        for (int i = 0; i < packageInfo.requestedPermissionsFlags.length; i++) {
            if (packageInfo.requestedPermissionsFlags[i]
                    == PackageInfo.REQUESTED_PERMISSION_GRANTED
                    && i < packageInfo.requestedPermissions.length
                    && permission.equals(packageInfo.requestedPermissions[i])) {
                return true;
            }
        }
        return false;
    }

    @NonNull
    public Map<String, String> getAllowListedHosts() {
        return mAllowListedHosts;
    }

    @NonNull
    public Set<String> getDenyListedHosts() {
        return mDenyListedHosts;
    }

    public boolean isAllowUnknownHostsEnabled() {
        return mAllowUnknownHosts;
    }

    /**
     * Creates a new {@link HostValidator}.
     *
     * <p>Allows applications to customize the {@link HostValidator} that will be used to verify
     * whether a caller is a valid templates host.
     */
    public static final class Builder {
        private final Map<String, String> mAllowListedHosts = new HashMap<>();
        private final Set<String> mDenyListedHosts = new HashSet<>();
        private boolean mAllowUnknownHosts = false;
        private final Context mContext;

        public Builder(@NonNull Context context) {
            mContext = context;
        }

        /**
         * Add a host to the allow list.
         *
         * @param packageName host package name (as reported by {@link PackageManager})
         * @param signature SHA256 digest of the DER encoding of the allow-listed host certificate.
         *                  This must be formatted as 32 lowercase 2 digits hexadecimal values
         *                  separated by colon (e.g.: "000102030405060708090a0b0c0d0e0f101112131415
         *                  161718191a1b1c1d1e1f"). When using
         *                  <a href="https://developer.android.com/about/versions/pie/android-9.0#apk-key-rotation">signature
         *                  rotation</a>, this digest should correspond to the initial signing
         *                  certificate.
         */
        @NonNull
        public Builder addAllowListedHost(@NonNull String packageName,
                @NonNull String signature) {
            requireNonNull(packageName);
            requireNonNull(signature);
            mAllowListedHosts.put(cleanUp(signature), cleanUp(packageName));
            return this;
        }

        private String cleanUp(String value) {
            return value.toLowerCase().replace(" ", "");
        }

        /**
         * Add a list of hosts to the allow list.
         *
         * <p>Allow-listed hosts are retrieved from a string-array resource, encoded as
         * [signature,package-name] pairs separated by comma. See
         * {@link #addAllowListedHost(String, String)} for details on signature and package-name
         * encoding.
         *
         * @param allowListedHostsRes string-array resource identifier
         * @throws IllegalArgumentException if the provided resource doesn't exist or if the entries
         * in the given resource are not formatted as expected.
         */
        @NonNull
        @SuppressLint("MissingGetterMatchingBuilder")
        public Builder addAllowListedHosts(@ArrayRes int allowListedHostsRes) {
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
                addAllowListedHost(keyValue[1], keyValue[0]);
            }
            return this;
        }

        /**
         * Add a host to the deny list.
         *
         * <p>If a host appears in both the allow and deny lists, the deny list will take
         * precedence.
         *
         * @param packageName host package name (as reported by {@link PackageManager})
         */
        @NonNull
        public Builder addDenyListedHost(@NonNull String packageName) {
            requireNonNull(packageName);
            mDenyListedHosts.add(cleanUp(packageName));
            return this;
        }

        /**
         * Configures this validator to accept bindings from unknown hosts. Use this option only for
         * testing or debugging.
         */
        @NonNull
        public Builder setAllowUnknownHostsEnabled(boolean allowUnknownHosts) {
            mAllowUnknownHosts = allowUnknownHosts;
            return this;
        }

        /** @return a new {@link HostValidator} */
        @NonNull
        public HostValidator build() {
            return new HostValidator(mContext.getPackageManager(), mAllowListedHosts,
                    mDenyListedHosts, mAllowUnknownHosts);
        }
    }
}
