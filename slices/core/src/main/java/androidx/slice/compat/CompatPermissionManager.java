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

package androidx.slice.compat;

import static androidx.core.content.PermissionChecker.PERMISSION_DENIED;
import static androidx.core.content.PermissionChecker.PERMISSION_GRANTED;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.RestrictTo;
import androidx.collection.ArraySet;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class CompatPermissionManager {

    private static final String TAG = "CompatPermissionManager";
    public static final String ALL_SUFFIX = "_all";

    private final Context mContext;
    private final String mPrefsName;
    private final int mMyUid;
    private final String[] mAutoGrantPermissions;

    public CompatPermissionManager(Context context, String prefsName, int myUid,
            String[] autoGrantPermissions) {
        mContext = context;
        mPrefsName = prefsName;
        mMyUid = myUid;
        mAutoGrantPermissions = autoGrantPermissions;
    }

    private SharedPreferences getPrefs() {
        return mContext.getSharedPreferences(mPrefsName, Context.MODE_PRIVATE);
    }

    public int checkSlicePermission(Uri uri, int pid, int uid) {
        if (uid == mMyUid) {
            return PERMISSION_GRANTED;
        }
        String[] pkgs = mContext.getPackageManager().getPackagesForUid(uid);
        for (String pkg : pkgs) {
            if (checkSlicePermission(uri, pkg) == PERMISSION_GRANTED) {
                return PERMISSION_GRANTED;
            }
        }
        for (String autoGrantPermission : mAutoGrantPermissions) {
            if (mContext.checkPermission(autoGrantPermission, pid, uid) == PERMISSION_GRANTED) {
                for (String pkg : pkgs) {
                    grantSlicePermission(uri, pkg);
                }
                return PERMISSION_GRANTED;
            }
        }
        // Fall back to allowing uri permissions through.
        return mContext.checkUriPermission(uri, pid, uid, Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
    }

    private int checkSlicePermission(Uri uri, String pkg) {
        PermissionState state = getPermissionState(pkg, uri.getAuthority());
        return state.hasAccess(uri.getPathSegments()) ? PERMISSION_GRANTED : PERMISSION_DENIED;
    }

    public void grantSlicePermission(Uri uri, String toPkg) {
        PermissionState state = getPermissionState(toPkg, uri.getAuthority());
        if (state.addPath(uri.getPathSegments())) {
            persist(state);
        }
    }

    public void revokeSlicePermission(Uri uri, String toPkg) {
        PermissionState state = getPermissionState(toPkg, uri.getAuthority());
        if (state.removePath(uri.getPathSegments())) {
            persist(state);
        }
    }

    private synchronized void persist(PermissionState state) {
        if (!getPrefs().edit()
                .putStringSet(state.getKey(), state.toPersistable())
                .putBoolean(state.getKey() + ALL_SUFFIX, state.hasAllPermissions())
                .commit()) {
            Log.e(TAG, "Unable to persist permissions");
        }
    }

    private PermissionState getPermissionState(String pkg, String authority) {
        String key = pkg + "_" + authority;
        Set<String> grant = getPrefs().getStringSet(key, Collections.<String>emptySet());
        boolean hasAllPermissions = getPrefs().getBoolean(key + ALL_SUFFIX, false);
        return new PermissionState(grant, key, hasAllPermissions);
    }

    public static class PermissionState {

        private final ArraySet<String[]> mPaths = new ArraySet<>();
        private final String mKey;

        PermissionState(Set<String> grant, String key, boolean hasAllPermissions) {
            if (hasAllPermissions) {
                mPaths.add(new String[0]);
            } else {
                for (String g : grant) {
                    mPaths.add(decodeSegments(g));
                }
            }
            mKey = key;
        }

        public boolean hasAllPermissions() {
            return hasAccess(Collections.<String>emptyList());
        }

        public String getKey() {
            return mKey;
        }

        public Set<String> toPersistable() {
            ArraySet<String> ret = new ArraySet<>();
            for (String[] path : mPaths) {
                ret.add(encodeSegments(path));
            }
            return ret;
        }

        public boolean hasAccess(List<String> path) {
            String[] inPath = path.toArray(new String[path.size()]);
            for (String[] p : mPaths) {
                if (isPathPrefixMatch(p, inPath)) {
                    return true;
                }
            }
            return false;
        }

        boolean addPath(List<String> path) {
            String[] pathSegs = path.toArray(new String[path.size()]);
            for (int i = mPaths.size() - 1; i >= 0; i--) {
                String[] existing = mPaths.valueAt(i);
                if (isPathPrefixMatch(existing, pathSegs)) {
                    // Nothing to add here.
                    return false;
                }
                if (isPathPrefixMatch(pathSegs, existing)) {
                    mPaths.removeAt(i);
                }
            }
            mPaths.add(pathSegs);
            return true;
        }

        boolean removePath(List<String> path) {
            boolean changed = false;
            String[] pathSegs = path.toArray(new String[path.size()]);
            for (int i = mPaths.size() - 1; i >= 0; i--) {
                String[] existing = mPaths.valueAt(i);
                if (isPathPrefixMatch(pathSegs, existing)) {
                    changed = true;
                    mPaths.removeAt(i);
                }
            }
            return changed;
        }

        private boolean isPathPrefixMatch(String[] prefix, String[] path) {
            final int prefixSize = prefix.length;
            if (path.length < prefixSize) return false;

            for (int i = 0; i < prefixSize; i++) {
                if (!Objects.equals(path[i], prefix[i])) {
                    return false;
                }
            }

            return true;
        }

        private String encodeSegments(String[] s) {
            String[] out = new String[s.length];
            for (int i = 0; i < s.length; i++) {
                out[i] = Uri.encode(s[i]);
            }
            return TextUtils.join("/", out);
        }

        private String[] decodeSegments(String s) {
            String[] sets = s.split("/", -1);
            for (int i = 0; i < sets.length; i++) {
                sets[i] = Uri.decode(sets[i]);
            }
            return sets;
        }
    }
}
