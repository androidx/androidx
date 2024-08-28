/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.privacysandbox.tools.integration.testsdk

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import android.os.IBinder
import android.view.View
import android.widget.TextView
import androidx.privacysandbox.tools.PrivacySandboxInterface
import androidx.privacysandbox.tools.PrivacySandboxService
import androidx.privacysandbox.ui.core.SandboxedUiAdapter
import androidx.privacysandbox.ui.core.SessionObserverFactory
import java.util.concurrent.Executor

@PrivacySandboxService
interface MySdk {
    suspend fun doSum(x: Int, y: Int): Int

    suspend fun getTextViewAd(): TextViewAd
}

@PrivacySandboxInterface interface TextViewAd : SandboxedUiAdapter

class MySdkImpl(private val context: Context) : MySdk {
    override suspend fun doSum(x: Int, y: Int): Int {
        return x + y
    }

    override suspend fun getTextViewAd(): TextViewAd {
        return TextViewAdImpl()
    }
}

class TextViewAdImpl : TextViewAd {
    override fun openSession(
        context: Context,
        windowInputToken: IBinder,
        initialWidth: Int,
        initialHeight: Int,
        isZOrderOnTop: Boolean,
        clientExecutor: Executor,
        client: SandboxedUiAdapter.SessionClient
    ) {
        val view = TextView(context)
        view.text = "foo bar baz"
        clientExecutor.execute { client.onSessionOpened(TextViewAdSession(view)) }
    }

    override fun addObserverFactory(sessionObserverFactory: SessionObserverFactory) {}

    override fun removeObserverFactory(sessionObserverFactory: SessionObserverFactory) {}

    inner class TextViewAdSession(override val view: View) : SandboxedUiAdapter.Session {
        override fun close() {}

        override fun notifyConfigurationChanged(configuration: Configuration) {}

        override fun notifyUiChanged(uiContainerInfo: Bundle) {}

        override val signalOptions: Set<String> = setOf()

        override fun notifyResized(width: Int, height: Int) {}

        override fun notifyZOrderChanged(isZOrderOnTop: Boolean) {}
    }
}
