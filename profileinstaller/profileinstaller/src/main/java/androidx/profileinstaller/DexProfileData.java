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

import java.util.HashMap;
import java.util.HashSet;

class DexProfileData {
    final @NonNull
    String key;
    final long dexChecksum;
    final int classSetSize;
    final int hotMethodRegionSize;
    final int numMethodIds;
    final @NonNull
    HashSet<Integer> classes;
    final @NonNull
    HashMap<Integer, Integer> methods;

    DexProfileData(
            @NonNull String key,
            long dexChecksum,
            int classSetSize,
            int hotMethodRegionSize,
            int numMethodIds,
            @NonNull HashSet<Integer> classes,
            @NonNull HashMap<Integer, Integer> methods
    ) {
        this.key = key;
        this.dexChecksum = dexChecksum;
        this.classSetSize = classSetSize;
        this.hotMethodRegionSize = hotMethodRegionSize;
        this.numMethodIds = numMethodIds;
        this.classes = classes;
        this.methods = methods;
    }
}
