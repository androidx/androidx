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

import android.appwidget.AppWidgetProviderInfo
import android.content.Context
import android.content.res.Configuration
import android.os.Parcel
import android.text.TextUtils
import android.util.DisplayMetrics
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.FrameLayout
import android.widget.RemoteViews
import androidx.compose.runtime.BroadcastFrameClock
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Composition
import androidx.compose.runtime.Recomposer
import androidx.compose.ui.unit.TextUnit
import androidx.core.view.children
import androidx.glance.Applier
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.launch
import java.util.Locale

internal suspend fun runTestingComposition(content: @Composable () -> Unit): RemoteViewsRoot =
    coroutineScope {
        val root = RemoteViewsRoot(10)
        val applier = Applier(root)
        val recomposer = Recomposer(currentCoroutineContext())
        val composition = Composition(applier, recomposer)
        val frameClock = BroadcastFrameClock()

        composition.setContent { content() }

        launch(frameClock) { recomposer.runRecomposeAndApplyChanges() }

        recomposer.close()
        recomposer.join()

        root
    }

/** Create the view out of a RemoteViews. */
internal fun Context.applyRemoteViews(rv: RemoteViews): View {
    val p = Parcel.obtain()
    return try {
        rv.writeToParcel(p, 0)
        p.setDataPosition(0)
        val parceled = RemoteViews(p)
        val parent = FrameLayout(this)
        parent.layoutParams = LayoutParams(WRAP_CONTENT, WRAP_CONTENT)
        parceled.apply(this, parent)
    } finally {
        p.recycle()
    }
}

internal suspend fun Context.runAndTranslate(
    appWidgetId: Int = 0,
    content: @Composable () -> Unit
): RemoteViews {
    val root = runTestingComposition(content)
    return translateComposition(this, appWidgetId, TestWidget::class.java, root)
}

internal suspend fun Context.runAndTranslateInRtl(
    appWidgetId: Int = 0,
    content: @Composable () -> Unit
): RemoteViews {
    val rtlLocale = Locale.getAvailableLocales().first {
        TextUtils.getLayoutDirectionFromLocale(it) == View.LAYOUT_DIRECTION_RTL
    }
    val rtlContext = createConfigurationContext(
        Configuration(resources.configuration).also {
            it.setLayoutDirection(rtlLocale)
        }
    )
    return rtlContext.runAndTranslate(appWidgetId, content = content)
}

internal fun appWidgetProviderInfo(
    builder: AppWidgetProviderInfo.() -> Unit
): AppWidgetProviderInfo =
    AppWidgetProviderInfo().apply(builder)

internal fun TextUnit.toPixels(displayMetrics: DisplayMetrics) =
    TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, value, displayMetrics).toInt()

internal inline fun <reified T> Collection<T>.toArrayList() = ArrayList<T>(this)

inline fun <reified T : View> View.findView(noinline pred: (T) -> Boolean) =
    findView(pred, T::class.java)

inline fun <reified T : View> View.findViewByType() =
    findView({ true }, T::class.java)

fun <T : View> View.findView(predicate: (T) -> Boolean, klass: Class<T>): T? {
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
    return children.mapNotNull { it.findView(predicate, klass) }.firstOrNull()
}

internal class TestWidget : GlanceAppWidget() {
    @Composable
    override fun Content() {}
}
