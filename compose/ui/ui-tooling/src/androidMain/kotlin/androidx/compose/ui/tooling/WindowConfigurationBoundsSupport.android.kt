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

package androidx.compose.ui.tooling

import android.content.res.Configuration
import android.graphics.Rect
import android.view.View
import java.lang.reflect.Field
import java.lang.reflect.Method

/**
 * Reflective handles used by [updateWindowConfigurationBounds] to update the bounds of
 * `Configuration.windowConfiguration` and `ViewRootImpl`'s merged configurations when running under
 * LayoutLib.
 *
 * The `ViewRootImpl` fields (`mLastReportedMergedConfiguration`, `mPendingMergedConfiguration`, and
 * `mLastConfigurationFromResources`) have existed since API 26 (Android 8.0) and were verified
 * through API 36 (LayoutLib). Each `ViewRootImpl` member is resolved independently so that if a
 * future Android version renames or removes one internal field, `Configuration.windowConfiguration`
 * and the remaining `ViewRootImpl` fields continue to be updated.
 */
@Suppress("BanUncheckedReflection")
internal class WindowConfigurationBoundsSupport
private constructor(
    private val windowConfigField: Field,
    private val setBoundsMethod: Method,
    private val setMaxBoundsMethod: Method,
    private val getViewRootImplMethod: Method?,
    private val mergedConfigFields: List<Field>,
    private val mergedConfigGetters: List<Method>,
    private val lastConfigField: Field?,
) {
    fun updateBounds(view: View, resourcesConfiguration: Configuration, width: Int, height: Int) {
        try {
            val bounds = Rect(0, 0, width, height)

            fun applyBounds(cfg: Any?) {
                val winCfg = cfg?.let { windowConfigField.get(it) } ?: return
                setBoundsMethod.invoke(winCfg, bounds)
                setMaxBoundsMethod.invoke(winCfg, bounds)
            }

            applyBounds(resourcesConfiguration)

            val viewRootImpl = getViewRootImplMethod?.invoke(view) ?: return
            for (field in mergedConfigFields) {
                val mergedConfig = field.get(viewRootImpl) ?: continue
                for (getter in mergedConfigGetters) {
                    applyBounds(getter.invoke(mergedConfig))
                }
            }
            if (lastConfigField != null) {
                applyBounds(lastConfigField.get(viewRootImpl))
            }
        } catch (_: Throwable) {
            // windowConfiguration / ViewRootImpl internals are hidden; ignore reflection or
            // security exceptions outside LayoutLib.
        }
    }

    companion object {
        val instance: WindowConfigurationBoundsSupport? by lazy {
            try {
                val windowConfigField = Configuration::class.java.getField("windowConfiguration")
                val windowConfigClass = windowConfigField.type
                val setBoundsMethod = windowConfigClass.getMethod("setBounds", Rect::class.java)
                val setMaxBoundsMethod =
                    windowConfigClass.getMethod("setMaxBounds", Rect::class.java)
                val getViewRootImplMethod = runCatching {
                    View::class.java.getMethod("getViewRootImpl")
                }
                    .getOrNull()
                val viewRootImplClass = getViewRootImplMethod?.returnType
                val mergedConfigFields =
                    if (viewRootImplClass != null) {
                        listOfNotNull(
                            runCatching {
                                viewRootImplClass
                                    .getDeclaredField("mLastReportedMergedConfiguration")
                                    .apply { isAccessible = true }
                            }
                                .getOrNull(),
                            runCatching {
                                viewRootImplClass
                                    .getDeclaredField("mPendingMergedConfiguration")
                                    .apply { isAccessible = true }
                            }
                                .getOrNull(),
                        )
                    } else {
                        emptyList()
                    }
                val mergedConfigClass = mergedConfigFields.firstOrNull()?.type
                val mergedConfigGetters =
                    if (mergedConfigClass != null) {
                        listOfNotNull(
                            runCatching { mergedConfigClass.getMethod("getGlobalConfiguration") }
                                .getOrNull(),
                            runCatching { mergedConfigClass.getMethod("getOverrideConfiguration") }
                                .getOrNull(),
                            runCatching { mergedConfigClass.getMethod("getMergedConfiguration") }
                                .getOrNull(),
                        )
                    } else {
                        emptyList()
                    }
                val lastConfigField = viewRootImplClass?.let { cls ->
                    runCatching {
                        cls.getDeclaredField("mLastConfigurationFromResources").apply {
                            isAccessible = true
                        }
                    }
                        .getOrNull()
                }
                WindowConfigurationBoundsSupport(
                    windowConfigField = windowConfigField,
                    setBoundsMethod = setBoundsMethod,
                    setMaxBoundsMethod = setMaxBoundsMethod,
                    getViewRootImplMethod = getViewRootImplMethod,
                    mergedConfigFields = mergedConfigFields,
                    mergedConfigGetters = mergedConfigGetters,
                    lastConfigField = lastConfigField,
                )
            } catch (_: Throwable) {
                // windowConfiguration / ViewRootImpl internals are hidden; ignore reflection or
                // security exceptions outside LayoutLib.
                null
            }
        }
    }
}
