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

import android.app.PendingIntent
import android.content.Context
import androidx.glance.action.ActionParameters
import androidx.glance.action.ActionRunnable
import androidx.glance.action.actionParametersOf
import androidx.glance.action.mutableActionParametersOf
import androidx.test.core.app.ApplicationProvider
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.Serializable
import kotlin.test.assertNotEquals

@RunWith(RobolectricTestRunner::class)
class ActionRunnableBroadcastReceiverTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Test
    fun createDifferentPendingIntentsWhenDifferentParameters() {
        val key = ActionParameters.Key<Boolean>("test")
        val firstIntent = createPendingIntent(actionParametersOf(key to false))
        val secondIntent = createPendingIntent(actionParametersOf(key to true))

        assertNotEquals(firstIntent, secondIntent)
    }

    @Test
    fun createDifferentPendingIntentsWithSameParameterInstanceButDifferentValue() {
        val key = ActionParameters.Key<Boolean>("test")
        val params = mutableActionParametersOf(key to false)
        val firstIntent = createPendingIntent(params)

        // Changing key value should create a different PI
        params[key] = true
        val secondIntent = createPendingIntent(params)

        assertNotEquals(firstIntent, secondIntent)
    }

    @Test
    fun createDifferentPendingIntentsWithSameMutableClassButDifferentValue() {
        val key = ActionParameters.Key<PlaceholderMutableClass>("test")
        val placeholder = PlaceholderMutableClass(false)
        val firstIntent = createPendingIntent(actionParametersOf(key to placeholder))

        // Changing the value of the class should create a different PI
        placeholder.value = true
        val secondIntent = createPendingIntent(actionParametersOf(key to placeholder))

        assertNotEquals(firstIntent, secondIntent)
    }

    private fun createPendingIntent(parameters: ActionParameters): PendingIntent {
        return ActionRunnableBroadcastReceiver.createPendingIntent(
            context = context,
            runnableClass = ActionRunnable::class.java,
            appWidgetClass = GlanceAppWidget::class.java,
            appWidgetId = 1,
            parameters = parameters
        )
    }

    // Placeholder mutable class that does not implement toString()
    private class PlaceholderMutableClass(var value: Boolean) : Serializable
}