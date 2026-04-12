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

package androidx.compose.foundation.text.contextmenu.internal

import android.app.ActivityOptions
import android.app.PendingIntent
import android.content.Context
import android.os.Build
import android.util.Log
import android.view.textclassifier.TextClassification
import androidx.annotation.RequiresApi

internal const val TAG = "TextClassification"

@RequiresApi(28)
internal object TextClassificationHelperApi28 {
    fun sendPendingIntent(pendingIntent: PendingIntent) {
        if (Build.VERSION.SDK_INT >= 34) {
            TextClassificationHelper34.sendIntentAllowBackgroundActivityStart(pendingIntent)
        } else {
            pendingIntent.send()
        }
    }

    @Suppress("DEPRECATION")
    fun sendLegacyIntent(context: Context, textClassification: TextClassification) {
        val pendingIntent =
            PendingIntent.getActivity(
                context,
                textClassification.text.hashCode(),
                textClassification.intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        sendPendingIntent(pendingIntent)
    }
}

@RequiresApi(34)
private object TextClassificationHelper34 {
    fun sendIntentAllowBackgroundActivityStart(pendingIntent: PendingIntent) {
        try {
            pendingIntent.send(
                ActivityOptions.makeBasic()
                    .setPendingIntentBackgroundActivityStartMode(
                        ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED
                    )
                    .toBundle()
            )
        } catch (e: PendingIntent.CanceledException) {
            Log.e(TAG, "error sending pendingIntent: $pendingIntent error: $e")
        }
    }
}
