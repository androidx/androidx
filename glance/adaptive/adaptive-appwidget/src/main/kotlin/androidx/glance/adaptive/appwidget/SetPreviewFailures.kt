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

package androidx.glance.adaptive.appwidget

/**
 * Collects the picker categories that one [BaseWidgetDelegate.setPreview] call fails to set, so
 * that the call sets every other category first and then throws.
 *
 * The first failure is thrown with its original type, so that it stays meaningful to the caller. A
 * summary naming the widget, category, surface and size of every failure, and the other failures,
 * are attached to it as suppressed exceptions.
 */
internal class SetPreviewFailures(private val widgetName: String) {
    private val errors = ArrayList<Exception>()
    private val summaries = ArrayList<String>()

    /** Records that setting the preview of [previews], all rendered for [target], failed. */
    fun add(error: Exception, target: RenderTarget, previews: List<PreviewTarget>) {
        errors.add(error)
        for (i in previews.indices) {
            summaries.add(
                "category ${previews[i].category} on surface ${target.surface} at " +
                    "${target.dimensions.widthDp}x${target.dimensions.heightDp}dp " +
                    "(${error.javaClass.simpleName})"
            )
        }
    }

    /** Throws the first collected failure, with the others attached, if there are any. */
    fun throwIfAny() {
        if (errors.isEmpty()) return
        val first = errors[0]
        first.addSuppressed(
            IllegalStateException(
                buildString {
                    append("setPreview failed for widgetName ")
                    append(widgetName)
                    append(':')
                    for (i in summaries.indices) {
                        append('\n')
                        append(summaries[i])
                    }
                }
            )
        )
        for (i in 1 until errors.size) {
            // The same instance can be thrown more than once, but cannot suppress itself.
            if (errors[i] !== first) first.addSuppressed(errors[i])
        }
        throw first
    }
}
