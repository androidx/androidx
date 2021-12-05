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
import android.widget.RelativeLayout
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.max
import androidx.compose.ui.unit.min
import androidx.core.view.children
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import java.io.FileInputStream
import java.util.concurrent.atomic.AtomicReference
import kotlin.test.assertIs

inline fun <reified T : View> View.findChild(noinline pred: (T) -> Boolean) =
    findChild(pred, T::class.java)

inline fun <reified T : View> View.findChildByType() =
    findChild({ true }, T::class.java)

internal inline fun <reified T> Collection<T>.toArrayList() = ArrayList<T>(this)

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

fun optionsBundleOf(sizes: List<DpSize>): Bundle {
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
fun runShellCommand(command: String): String {
    return InstrumentationRegistry.getInstrumentation()
        .uiAutomation
        .executeShellCommand(command)
        .use { FileInputStream(it.fileDescriptor).reader().readText() }
}

val context: Context
    get() = ApplicationProvider.getApplicationContext()

/** Count the number of children that are not gone. */
val ViewGroup.notGoneChildCount: Int
    get() = children.count { it.visibility != View.GONE }

/** Iterate over children that are not gone. */
val ViewGroup.notGoneChildren: Sequence<View>
    get() = children.filter { it.visibility != View.GONE }

// Extract the target view if it is a complex view in Android R-.
inline fun <reified T : View> View.getTargetView(): T {
    if ((tag as? String) != "glanceComplexLayout") {
        return assertIs(this)
    }
    val layout = assertIs<RelativeLayout>(this)
    return assertIs(
        when (layout.childCount) {
            1 -> layout.getChildAt(0)
            2 -> layout.getChildAt(1)
            else -> throw IllegalStateException("Unknown complex layout with more than 2 elements.")
        }
    )
}

// Get the parent view, even if the current view is in a complex layout.
inline fun <reified T : View> View.getParentView(): T {
    val parent = assertIs<ViewGroup>(this.parent)
    if ((parent.tag as? String) != "glanceComplexLayout") {
        return assertIs(parent)
    }
    return assertIs(parent.parent)
}

// Perform a click on the root layout of a compound button. In our tests we often identify a
// compound button by its TextView, but we should perform the click on the root view of a compound
// button layout (parent of the TextView). On both S+ (standard compound button views) and R-
// (backported views) we tag the root view with "glanceCompoundButton", so we can use that to find
// the right view to click on.
fun View.performCompoundButtonClick() {
    if (tag == "glanceCompoundButton") {
        this
    } else {
        assertIs<View>(this.parent).also {
            assertThat(it.tag).isEqualTo("glanceCompoundButton")
        }
    }.performClick()
}

// Update the value of the AtomicReference using the given updater function. Will throw an error
// if unable to successfully set the value.
fun <T> AtomicReference<T>.update(updater: (T) -> T) {
    repeat(100) {
        get().let {
            if (compareAndSet(it, updater(it))) return
        }
    }
    error("Could not update the AtomicReference")
}
