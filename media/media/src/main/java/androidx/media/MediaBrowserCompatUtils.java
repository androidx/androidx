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

package androidx.media;

import static androidx.annotation.RestrictTo.Scope.LIBRARY;

import android.os.Bundle;
import android.support.v4.media.MediaBrowserCompat;

import androidx.annotation.RestrictTo;

/**
 */
@RestrictTo(LIBRARY)
public class MediaBrowserCompatUtils {
    public static boolean areSameOptions(Bundle options1, Bundle options2) {
        if (options1 == options2) {
            return true;
        } else if (options1 == null) {
            return options2.getInt(MediaBrowserCompat.EXTRA_PAGE, -1) == -1
                    && options2.getInt(MediaBrowserCompat.EXTRA_PAGE_SIZE, -1) == -1;
        } else if (options2 == null) {
            return options1.getInt(MediaBrowserCompat.EXTRA_PAGE, -1) == -1
                    && options1.getInt(MediaBrowserCompat.EXTRA_PAGE_SIZE, -1) == -1;
        } else {
            return options1.getInt(MediaBrowserCompat.EXTRA_PAGE, -1)
                    == options2.getInt(MediaBrowserCompat.EXTRA_PAGE, -1)
                    && options1.getInt(MediaBrowserCompat.EXTRA_PAGE_SIZE, -1)
                    == options2.getInt(MediaBrowserCompat.EXTRA_PAGE_SIZE, -1);
        }
    }

    public static boolean hasDuplicatedItems(Bundle options1, Bundle options2) {
        int page1 = options1 == null ? -1 : options1.getInt(MediaBrowserCompat.EXTRA_PAGE, -1);
        int page2 = options2 == null ? -1 :options2.getInt(MediaBrowserCompat.EXTRA_PAGE, -1);
        int pageSize1 = options1 == null
                ? -1 :options1.getInt(MediaBrowserCompat.EXTRA_PAGE_SIZE, -1);
        int pageSize2 = options2 == null
                ? -1 :options2.getInt(MediaBrowserCompat.EXTRA_PAGE_SIZE, -1);

        int startIndex1, startIndex2, endIndex1, endIndex2;
        if (page1 == -1 || pageSize1 == -1) {
            startIndex1 = 0;
            endIndex1 = Integer.MAX_VALUE;
        } else {
            startIndex1 = pageSize1 * page1;
            endIndex1 = startIndex1 + pageSize1 - 1;
        }

        if (page2 == -1 || pageSize2 == -1) {
            startIndex2 = 0;
            endIndex2 = Integer.MAX_VALUE;
        } else {
            startIndex2 = pageSize2 * page2;
            endIndex2 = startIndex2 + pageSize2 - 1;
        }

        // For better readability, leaving the exclamation mark here.
        return !(endIndex1 < startIndex2 || endIndex2 < startIndex1);
    }

    private MediaBrowserCompatUtils() {
    }
}
