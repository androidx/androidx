/*
 * Copyright (C) 2015 The Android Open Source Project
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
package android.support.v7.widget;

/**
 * Helper class for tests to check internals of ViewInfoStore
 */
public class InfoStoreTrojan {
    static int sizeOfPreLayout(ViewInfoStore store) {
        return sizeOf(store, ViewInfoStore.InfoRecord.FLAG_PRE);
    }
    static int sizeOfPostLayout(ViewInfoStore store) {
        return sizeOf(store, ViewInfoStore.InfoRecord.FLAG_POST);
    }
    static int sizeOf(ViewInfoStore store, int flags) {
        int cnt = 0;
        final int size = store.mLayoutHolderMap.size();
        for (int i = 0; i < size; i ++) {
            ViewInfoStore.InfoRecord record = store.mLayoutHolderMap.valueAt(i);
            if ((record.flags & flags) != 0) {
                cnt ++;
            }
        }
        return cnt;
    }
}
