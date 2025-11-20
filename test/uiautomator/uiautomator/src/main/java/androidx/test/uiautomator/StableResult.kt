/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.test.uiautomator

import android.graphics.Bitmap
import android.view.accessibility.AccessibilityNodeInfo

/**
 * Represents a node that is considered stable and it's returned by
 * [androidx.test.uiautomator.waitForStable].
 *
 * Note that if [isTimeout] is true, it means that the node was not stable by the end of the
 * timeout.
 */
public class StableResult
internal constructor(

    /** The latest node acquired, that is stable if [isTimeout] is false. */
    public val node: AccessibilityNodeInfo,

    /** A screenshot of the node if `requireStableScreenshot` was set to true. */
    public val screenshot: Bitmap?,

    /**
     * Whether the [waitForStable] request timed out. If this value is true, [node] may not be
     * stable.
     */
    public val isTimeout: Boolean,
)
