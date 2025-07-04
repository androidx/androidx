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

package androidx.privacysandbox.ui.client

import android.annotation.SuppressLint
import android.content.res.Configuration
import android.os.Bundle
import android.view.View
import androidx.privacysandbox.ui.core.SandboxedUiAdapter

/**
 * Create [SandboxedUiAdapter.Session] that proxies to [origSession] Version check on provider
 * version should be made before invoking functions on Session.
 */
@SuppressLint("BanUncheckedReflection") // using reflection on library classes
internal class SessionProxy(private val uiProviderVersion: Int, private val origSession: Any) :
    SandboxedUiAdapter.Session {

    private val targetClass =
        Class.forName(
                "androidx.privacysandbox.ui.core.SandboxedUiAdapter\$Session",
                /* initialize = */ false,
                origSession.javaClass.classLoader,
            )
            .also { it.cast(origSession) }

    private val getViewMethod = targetClass.getMethod("getView")
    private val notifyResizedMethod =
        targetClass.getMethod("notifyResized", Int::class.java, Int::class.java)
    private val getSignalOptionsMethod = targetClass.getMethod("getSignalOptions")
    private val notifyZOrderChangedMethod =
        targetClass.getMethod("notifyZOrderChanged", Boolean::class.java)
    private val notifyConfigurationChangedMethod =
        targetClass.getMethod("notifyConfigurationChanged", Configuration::class.java)
    private val notifyUiChangedMethod = targetClass.getMethod("notifyUiChanged", Bundle::class.java)
    private val notifySessionRenderedMethod =
        targetClass.getMethod("notifySessionRendered", Set::class.java)
    private val closeMethod = targetClass.getMethod("close")

    override val view: View
        get() = getViewMethod.invoke(origSession) as View

    override val signalOptions: Set<String>
        @Suppress("UNCHECKED_CAST") // using reflection on library classes
        get() = getSignalOptionsMethod.invoke(origSession) as Set<String>

    override fun notifyResized(width: Int, height: Int) {
        val parentView = view.parent as View
        view.layout(
            parentView.paddingLeft,
            parentView.paddingTop,
            parentView.paddingLeft + width,
            parentView.paddingTop + height,
        )
        notifyResizedMethod.invoke(origSession, width, height)
    }

    override fun notifyZOrderChanged(isZOrderOnTop: Boolean) {
        notifyZOrderChangedMethod.invoke(origSession, isZOrderOnTop)
    }

    override fun notifyConfigurationChanged(configuration: Configuration) {
        notifyConfigurationChangedMethod.invoke(origSession, configuration)
    }

    override fun notifyUiChanged(uiContainerInfo: Bundle) {
        notifyUiChangedMethod.invoke(origSession, uiContainerInfo)
    }

    override fun notifySessionRendered(supportedSignalOptions: Set<String>) {
        notifySessionRenderedMethod.invoke(origSession, supportedSignalOptions)
    }

    override fun close() {
        closeMethod.invoke(origSession)
    }
}
