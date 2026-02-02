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
import androidx.glance.action.actionParametersOf
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.ActionCallbackBroadcastReceiver
import androidx.glance.appwidget.action.ActionTrampolineType
import androidx.glance.appwidget.action.createUniqueUri
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import java.io.Serializable
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ActionCallbackBroadcastReceiverTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Test
    fun createDifferentPendingIntentsWhenDifferentViewId() {
        val key = ActionParameters.Key<Boolean>("test")
        val firstIntent = createPendingIntent(actionParametersOf(key to false), 1)
        val secondIntent = createPendingIntent(actionParametersOf(key to true), 2)

        assertThat(firstIntent).isNotEqualTo(secondIntent)
    }

    @Test
    fun createSamePendingIntentsWhenSameViewId() {
        val key = ActionParameters.Key<Boolean>("test")
        val firstIntent = createPendingIntent(actionParametersOf(key to false), 1)
        val secondIntent = createPendingIntent(actionParametersOf(key to true), 1)

        assertThat(firstIntent).isEqualTo(secondIntent)
    }

    private fun createPendingIntent(parameters: ActionParameters, viewId: Int): PendingIntent {
        val translationContext =
            TranslationContext(
                context,
                appWidgetId = 1,
                isRtl = false,
                layoutConfiguration = LayoutConfiguration.create(context, 1),
                itemPosition = -1,
                isLazyCollectionDescendant = false,
                glanceComponents = GlanceComponents.getDefault(context),
            )
        return PendingIntent.getBroadcast(
            context,
            0,
            ActionCallbackBroadcastReceiver.createIntent(
                    translationContext = translationContext,
                    callbackClass = ActionCallback::class.java,
                    parameters = parameters
                )
                .apply {
                    data =
                        createUniqueUri(
                            translationContext,
                            viewId = viewId,
                            type = ActionTrampolineType.CALLBACK,
                        )
                },
            PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }

    // Placeholder mutable class that does not implement toString()
    private class PlaceholderMutableClass(var value: Boolean) : Serializable
}
