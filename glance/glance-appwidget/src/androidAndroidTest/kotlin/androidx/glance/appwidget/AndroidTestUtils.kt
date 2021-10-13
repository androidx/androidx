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

package androidx.glance.appwidget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.core.view.children
import androidx.glance.unit.DpSize
import androidx.glance.unit.max
import androidx.glance.unit.min
import androidx.glance.unit.toSizeF
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import java.io.FileInputStream

inline fun <reified T : View> View.findChild(noinline pred: (T) -> Boolean) =
    findChild(pred, T::class.java)

inline fun <reified T : View> View.findChildByType() =
    findChild({ true }, T::class.java)

fun <T : View> View.findChild(predicate: (T) -> Boolean, klass: Class<T>): T? {
    try {
        val castView = klass.cast(this)!!
        if (predicate(castView)) {
            return castView
        }
    } catch (e: ClassCastException) {
        // Nothing to do
    }
    if (this !is ViewGroup) {
        return null
    }
    return children.mapNotNull { it.findChild(predicate, klass) }.firstOrNull()
}

internal inline fun <reified T> Collection<T>.toArrayList() = ArrayList<T>(this)

internal fun optionsBundleOf(vararg sizes: DpSize) = optionsBundleOf(sizes.toList())

internal fun optionsBundleOf(sizes: List<DpSize>): Bundle {
    require(sizes.isNotEmpty()) { "There must be at least one size" }
    val (minSize, maxSize) = sizes.fold(sizes[0] to sizes[0]) { acc, s ->
        DpSize(min(acc.first.width, s.width), min(acc.first.height, s.height)) to
            DpSize(max(acc.second.width, s.width), max(acc.second.height, s.height))
    }
    return Bundle().apply {
        putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, minSize.width.value.toInt())
        putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, minSize.height.value.toInt())
        putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, maxSize.width.value.toInt())
        putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, maxSize.height.value.toInt())
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val sizeList = sizes.map { it.toSizeF() }.toArrayList()
            putParcelableArrayList(AppWidgetManager.OPTION_APPWIDGET_SIZES, sizeList)
        }
    }
}

/** Run a command and retrieve the output as a string. */
internal fun runShellCommand(command: String): String {
    return InstrumentationRegistry.getInstrumentation()
        .uiAutomation
        .executeShellCommand(command)
        .use { FileInputStream(it.fileDescriptor).reader().readText() }
}

internal val context: Context
    get() = ApplicationProvider.getApplicationContext()
