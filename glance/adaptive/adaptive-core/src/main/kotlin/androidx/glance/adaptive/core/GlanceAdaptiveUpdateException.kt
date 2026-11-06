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

package androidx.glance.adaptive.core

import androidx.annotation.RestrictTo

/**
 * Thrown by [GlanceAdaptiveWidgetManager.pushUpdate] once it has attempted every targeted widget
 * placement, if some of them could not be updated. Every other placement received the update.
 *
 * `pushUpdate` is idempotent, so callers can retry only the failed widgets via `pushUpdate(...,
 * widgetIds = e.failedWidgetIds)` when [allFailedWidgetIdsKnown] is `true`, or retry with the
 * original arguments when [allFailedWidgetIdsKnown] is `false`.
 *
 * The first failure is the [cause], and any others are attached as
 * [suppressed][Throwable.getSuppressed] exceptions. The message summarizes the failures per host
 * surface and size.
 *
 * @property failedWidgetIds The widget instance String identifiers known not to have received the
 *   update. An identifier placed more than once is included if any of its placements failed.
 * @property allFailedWidgetIdsKnown `true` if every widget that failed to update had a readable
 *   `widgetId` and is included in [failedWidgetIds]. It is `false` if at least one widget failed
 *   before its `widgetId` could be read (for example, when reading its options bundle threw an
 *   exception), meaning [failedWidgetIds] is incomplete and retrying only [failedWidgetIds] may
 *   miss that widget.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class GlanceAdaptiveUpdateException
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
constructor(
    message: String,
    public val failedWidgetIds: Set<String>,
    public val allFailedWidgetIdsKnown: Boolean,
    cause: Throwable,
) : RuntimeException(message, cause)
