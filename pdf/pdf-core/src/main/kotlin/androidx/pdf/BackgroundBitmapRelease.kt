/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.pdf

import androidx.annotation.IntDef
import androidx.annotation.RestrictTo

/** Controls whether a PDF viewer retains or releases rendered page bitmaps when hidden. */
@ExperimentalPdfApi
public object BackgroundBitmapRelease {
    /**
     * Retains rendered bitmaps when the window is hidden, showing content immediately on return.
     * This is the default.
     */
    public const val NONE: Int = 0

    /**
     * Releases all rendered bitmaps when the window is hidden and re-renders visible pages on
     * return.
     */
    public const val ALL: Int = 1
}

/** Defines valid [BackgroundBitmapRelease] constants. */
@OptIn(ExperimentalPdfApi::class)
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Retention(AnnotationRetention.SOURCE)
@IntDef(BackgroundBitmapRelease.NONE, BackgroundBitmapRelease.ALL)
public annotation class BackgroundBitmapReleasePolicy
