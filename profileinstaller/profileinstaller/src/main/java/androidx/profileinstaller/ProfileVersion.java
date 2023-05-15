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

package androidx.profileinstaller;

import android.os.Build;

import androidx.annotation.RestrictTo;

import java.util.Arrays;

@RestrictTo(RestrictTo.Scope.LIBRARY)
public class ProfileVersion {
    private ProfileVersion() {}
    static final byte[] V015_S = new byte[]{'0', '1', '5', '\0'};
    static final byte[] V010_P = new byte[]{'0', '1', '0', '\0'};
    static final byte[] V009_O_MR1 = new byte[]{'0', '0', '9', '\0'};
    static final byte[] V005_O = new byte[]{'0', '0', '5', '\0'};
    static final byte[] V001_N = new byte[]{'0', '0', '1', '\0'};
    static final byte[] METADATA_V001_N = new byte[]{'0', '0', '1', '\0'};
    static final byte[] METADATA_V002 = new byte[]{'0', '0', '2', '\0'};
    public static final int MIN_SUPPORTED_SDK = Build.VERSION_CODES.N;
    public static final int MAX_SUPPORTED_SDK = 34;

    static String dexKeySeparator(byte[] version) {
        if (Arrays.equals(version, V001_N)) {
            return ":";
        } else if (Arrays.equals(version, V005_O)) {
            return ":";
        } else {
            return "!";
        }
    }
}
