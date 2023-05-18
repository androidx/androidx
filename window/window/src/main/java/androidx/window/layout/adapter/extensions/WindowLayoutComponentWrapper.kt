/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.window.layout.adapter.extensions

import android.app.Activity
import android.content.Context
import androidx.annotation.UiContext
import androidx.window.extensions.core.util.function.Consumer as JavaConsumer
import androidx.window.extensions.layout.WindowLayoutComponent
import androidx.window.extensions.layout.WindowLayoutInfo
import java.util.function.Consumer

/**
 * A thin wrapper interface around [WindowLayoutComponent] to support testing and easily changing
 * the extensions dependency.
 */
internal interface WindowLayoutComponentWrapper {

    fun addWindowLayoutInfoListener(activity: Activity, consumer: Consumer<WindowLayoutInfo>)

    fun removeWindowLayoutInfoListener(consumer: Consumer<WindowLayoutInfo>)

    fun addWindowLayoutInfoListener(
        @UiContext context: Context,
        consumer: JavaConsumer<WindowLayoutInfo>
    )

    fun removeWindowLayoutInfoListener(consumer: JavaConsumer<WindowLayoutInfo>)

    companion object {
        fun getInstance(component: WindowLayoutComponent): WindowLayoutComponentWrapper {
            return WindowLayoutComponentWrapperImpl(component)
        }
    }
}

private class WindowLayoutComponentWrapperImpl(
    private val component: WindowLayoutComponent
) : WindowLayoutComponentWrapper {

    override fun addWindowLayoutInfoListener(
        activity: Activity,
        consumer: Consumer<WindowLayoutInfo>
    ) {
        @Suppress("DEPRECATION") // maintain for compatibility
        component.addWindowLayoutInfoListener(activity, consumer)
    }

    override fun removeWindowLayoutInfoListener(consumer: Consumer<WindowLayoutInfo>) {
        @Suppress("DEPRECATION") // maintain for compatibility
        component.removeWindowLayoutInfoListener(consumer)
    }

    override fun addWindowLayoutInfoListener(
        context: Context,
        consumer: JavaConsumer<WindowLayoutInfo>
    ) {
        component.addWindowLayoutInfoListener(context, consumer)
    }

    override fun removeWindowLayoutInfoListener(consumer: JavaConsumer<WindowLayoutInfo>) {
        component.removeWindowLayoutInfoListener(consumer)
    }
}