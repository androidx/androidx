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

import androidx.annotation.NonNull;

import java.util.TreeMap;

class DexProfileData {
    @NonNull
    final String apkName;
    @NonNull
    final String dexName;
    final long dexChecksum;
    int classSetSize;
    final int hotMethodRegionSize;
    final int numMethodIds;
    @NonNull int[] classes;
    final @NonNull
    TreeMap<Integer, Integer> methods;

    DexProfileData(
            @NonNull String apkName,
            @NonNull String dexName,
            long dexChecksum,
            int classSetSize,
            int hotMethodRegionSize,
            int numMethodIds,
            @NonNull int[] classes,
            @NonNull TreeMap<Integer, Integer> methods
    ) {
        this.apkName = apkName;
        this.dexName = dexName;
        this.dexChecksum = dexChecksum;
        this.classSetSize = classSetSize;
        this.hotMethodRegionSize = hotMethodRegionSize;
        this.numMethodIds = numMethodIds;
        this.classes = classes;
        this.methods = methods;
    }
}