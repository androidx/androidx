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

package androidx.ui.rendering.proxybox

/** How to behave during hit tests. */
enum class HitTestBehavior {
    /**
     * Targets that defer to their children receive events within their bounds
     * only if one of their children is hit by the hit test.
     */
    DEFER_TO_CHILD,

    /**
     * Opaque targets can be hit by hit tests, causing them to both receive
     * events within their bounds and prevent targets visually behind them from
     * also receiving events.
     */
    OPAQUE,

    /**
     * Translucent targets both receive events within their bounds and permit
     * targets visually behind them to also receive events.
     */
    TRANSLUCENT
}