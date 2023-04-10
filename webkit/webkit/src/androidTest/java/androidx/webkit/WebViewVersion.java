/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.webkit;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.test.core.app.ApplicationProvider;

import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Represents a WebView version. Is comparable. This supports version numbers following the
 * scheme outlined at https://www.chromium.org/developers/version-numbers.
 */
@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
class WebViewVersion implements Comparable<WebViewVersion> {
    private static final Pattern CHROMIUM_VERSION_REGEX =
            Pattern.compile("^(\\d+)\\.(\\d+)\\.(\\d+)\\.(\\d+)$");

    private final int[] mComponents;

    WebViewVersion(String versionString) {
        Matcher m;
        if ((m = CHROMIUM_VERSION_REGEX.matcher(versionString)).matches()) {
            mComponents = new int[]{Integer.parseInt(m.group(1)),
                    Integer.parseInt(m.group(2)), Integer.parseInt(m.group(3)),
                    Integer.parseInt(m.group(4))};
        } else {
            throw new IllegalArgumentException("Invalid WebView version string: '"
                    + versionString + "'");
        }
    }

    @Nullable
    static WebViewVersion getInstalledWebViewVersionFromPackage() {
        Context context = ApplicationProvider.getApplicationContext();
        // Before M42, we used the major version number, followed by other text wrapped in
        // parentheses.
        final Pattern oldVersionNameFormat =
                Pattern.compile("^(37|38|39|40|41) \\(.*\\)$");
        PackageInfo currentPackage = WebViewCompat.getCurrentWebViewPackage(context);
        if (currentPackage != null) {
            String installedVersionName = currentPackage.versionName;
            Matcher m = oldVersionNameFormat.matcher(installedVersionName);
            if (m.matches()) {
                // There's no way to get the full version number, so just assume 0s for the later
                // numbers.
                installedVersionName = "" + m.group(1) + ".0.0.0";
            }
            return new WebViewVersion(installedVersionName);
        } else {
            return null;
        }
    }

    public boolean equals(Object obj) {
        if (!(obj instanceof WebViewVersion)) {
            return false;
        }
        WebViewVersion that = (WebViewVersion) obj;
        return Arrays.equals(this.mComponents, that.mComponents);
    }

    public int hashCode() {
        return Arrays.hashCode(mComponents);
    }

    @Override
    public int compareTo(WebViewVersion that) {
        for (int i = 0; i < 4; i++) {
            int diff = this.mComponents[i] - that.mComponents[i];
            if (diff != 0) return diff;
        }
        return 0;
    }

    @NonNull
    @Override
    public String toString() {
        return this.mComponents[0] + "." + this.mComponents[1] + "."
                + this.mComponents[2] + "." + this.mComponents[3];
    }
}
