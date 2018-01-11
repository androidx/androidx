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

package androidx.app.slice.compat;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.SystemClock;
import android.support.annotation.RestrictTo;
import android.support.annotation.VisibleForTesting;
import android.support.v4.util.ArraySet;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import androidx.app.slice.SliceSpec;

/**
 * Tracks the current packages requesting pinning of any given slice. It will clear the
 * list after a reboot since the packages are no longer requesting pinning.
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class CompatPinnedList {

    private static final String LAST_BOOT = "last_boot";
    private static final String PIN_PREFIX = "pinned_";
    private static final String SPEC_NAME_PREFIX = "spec_names_";
    private static final String SPEC_REV_PREFIX = "spec_revs_";

    // Max skew between bootup times that we think its probably rebooted.
    // There could be some difference in our calculated boot up time if the thread
    // sleeps between currentTimeMillis and elapsedRealtime.
    // Its probably safe to assume the device can't boot twice within 2 secs.
    private static final long BOOT_THRESHOLD = 2000;

    private final Context mContext;
    private final String mPrefsName;

    public CompatPinnedList(Context context, String prefsName) {
        mContext = context;
        mPrefsName = prefsName;
    }

    private SharedPreferences getPrefs() {
        SharedPreferences prefs = mContext.getSharedPreferences(mPrefsName, Context.MODE_PRIVATE);
        long lastBootTime = prefs.getLong(LAST_BOOT, 0);
        long currentBootTime = getBootTime();
        if (Math.abs(lastBootTime - currentBootTime) > BOOT_THRESHOLD) {
            prefs.edit()
                    .clear()
                    .putLong(LAST_BOOT, currentBootTime)
                    .commit();
        }
        return prefs;
    }

    private Set<String> getPins(Uri uri) {
        return getPrefs().getStringSet(PIN_PREFIX + uri.toString(), new ArraySet<String>());
    }

    /**
     * Get the list of specs for a pinned Uri.
     */
    public synchronized List<SliceSpec> getSpecs(Uri uri) {
        List<SliceSpec> specs = new ArrayList<>();
        SharedPreferences prefs = getPrefs();
        String specNamesStr = prefs.getString(SPEC_NAME_PREFIX + uri.toString(), null);
        String specRevsStr = prefs.getString(SPEC_REV_PREFIX + uri.toString(), null);
        if (TextUtils.isEmpty(specNamesStr) || TextUtils.isEmpty(specRevsStr)) {
            return Collections.emptyList();
        }
        String[] specNames = specNamesStr.split(",");
        String[] specRevs = specRevsStr.split(",");
        if (specNames.length != specRevs.length) {
            return Collections.emptyList();
        }
        for (int i = 0; i < specNames.length; i++) {
            specs.add(new SliceSpec(specNames[i], Integer.parseInt(specRevs[i])));
        }
        return specs;
    }

    private void setPins(Uri uri, Set<String> pins) {
        getPrefs().edit()
                .putStringSet(PIN_PREFIX + uri.toString(), pins)
                .commit();
    }

    private void setSpecs(Uri uri, List<SliceSpec> specs) {
        String[] specNames = new String[specs.size()];
        String[] specRevs = new String[specs.size()];
        for (int i = 0; i < specs.size(); i++) {
            specNames[i] = specs.get(i).getType();
            specRevs[i] = String.valueOf(specs.get(i).getRevision());
        }
        getPrefs().edit()
                .putString(SPEC_NAME_PREFIX + uri.toString(), TextUtils.join(",", specNames))
                .putString(SPEC_REV_PREFIX + uri.toString(), TextUtils.join(",", specRevs))
                .commit();
    }

    @VisibleForTesting
    protected long getBootTime() {
        return System.currentTimeMillis() - SystemClock.elapsedRealtime();
    }

    /**
     * Adds a pin for a specific uri/pkg pair and returns true if the
     * uri was not previously pinned.
     */
    public synchronized boolean addPin(Uri uri, String pkg, List<SliceSpec> specs) {
        Set<String> pins = getPins(uri);
        boolean wasNotPinned = pins.isEmpty();
        pins.add(pkg);
        setPins(uri, pins);
        if (wasNotPinned) {
            setSpecs(uri, specs);
        } else {
            setSpecs(uri, mergeSpecs(getSpecs(uri), specs));
        }
        return wasNotPinned;
    }

    /**
     * Removes a pin for a specific uri/pkg pair and returns true if the
     * uri is no longer pinned (but was).
     */
    public synchronized boolean removePin(Uri uri, String pkg) {
        Set<String> pins = getPins(uri);
        if (pins.isEmpty() || !pins.contains(pkg)) {
            return false;
        }
        pins.remove(pkg);
        setPins(uri, pins);
        return pins.size() == 0;
    }

    private static List<SliceSpec> mergeSpecs(List<SliceSpec> specs,
            List<SliceSpec> supportedSpecs) {
        for (int i = 0; i < specs.size(); i++) {
            SliceSpec s = specs.get(i);
            SliceSpec other = findSpec(supportedSpecs, s.getType());
            if (other == null) {
                specs.remove(i--);
            } else if (other.getRevision() < s.getRevision()) {
                specs.set(i, other);
            }
        }
        return specs;
    }

    private static SliceSpec findSpec(List<SliceSpec> specs, String type) {
        for (SliceSpec spec : specs) {
            if (Objects.equals(spec.getType(), type)) {
                return spec;
            }
        }
        return null;
    }
}
