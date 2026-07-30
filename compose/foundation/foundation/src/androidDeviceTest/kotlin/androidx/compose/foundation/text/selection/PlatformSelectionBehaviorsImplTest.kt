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

package androidx.compose.foundation.text.selection

import android.app.PendingIntent
import android.app.RemoteAction
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import android.view.textclassifier.TextClassification
import android.view.textclassifier.TextClassifier
import android.view.textclassifier.TextLinks
import android.view.textclassifier.TextSelection
import androidx.compose.ui.text.TextRange
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import kotlin.coroutines.EmptyCoroutineContext
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = 28)
class PlatformSelectionBehaviorsImplTest {
    @Test
    fun createTextClassificationResult_iconsPreloaded() {
        val context = InstrumentationRegistry.getInstrumentation().context
        val behavior =
            PlatformSelectionBehaviorsImpl(
                coroutineContext = EmptyCoroutineContext,
                context = context,
                selectedTextType = SelectedTextType.EditableText,
                localeList = null,
            )

        val classification =
            createClassificationWithIcon(
                actions = listOf(createRemoteAction(context, shouldShowIcon = true))
            )
        val classifier = TestTextClassifier(classification)

        testWithClassifier(context, classifier) {
            runBlocking {
                behavior.onShowContextMenu(
                    text = "hello world",
                    selection = TextRange(0, 5),
                    secondaryClickLocation = null,
                )
            }

            // Verify that the icon is loaded.
            val result = behavior.textClassificationResult
            assertThat(result).isNotNull()
            assertThat(result!!.icons).hasSize(1)
            assertThat(result.icons[0]).isNotNull()
        }
    }

    @Test
    fun createTextClassificationResult_iconsPreloaded_respectsShouldShowIcon() {
        val context = InstrumentationRegistry.getInstrumentation().context
        val behavior =
            PlatformSelectionBehaviorsImpl(
                coroutineContext = EmptyCoroutineContext,
                context = context,
                selectedTextType = SelectedTextType.EditableText,
                localeList = null,
            )

        // The first action's icon is always loaded (index == 0 in createTextClassificationResult).
        // The second action has shouldShowIcon = false.
        // The third action has shouldShowIcon = true.
        val classification =
            createClassificationWithIcon(
                actions =
                    listOf(
                        createRemoteAction(context, shouldShowIcon = false),
                        createRemoteAction(context, shouldShowIcon = false),
                        createRemoteAction(context, shouldShowIcon = true),
                    )
            )
        val classifier = TestTextClassifier(classification)

        testWithClassifier(context, classifier) {
            runBlocking {
                behavior.onShowContextMenu(
                    text = "hello world",
                    selection = TextRange(0, 5),
                    secondaryClickLocation = null,
                )
            }

            // Verify that the icon is loaded based on shouldShowIcon.
            val result = behavior.textClassificationResult
            assertThat(result).isNotNull()
            assertThat(result!!.icons).hasSize(3)
            assertThat(result.icons[0]).isNotNull() // Always loaded because index == 0 is primary
            assertThat(result.icons[1]).isNull() // Not loaded because shouldShowIcon == false
            assertThat(result.icons[2]).isNotNull() // Loaded because shouldShowIcon == true
        }
    }

    private fun testWithClassifier(
        context: Context,
        classifier: TextClassifier,
        block: () -> Unit,
    ) {
        val textClassificationManager =
            context.getSystemService(
                android.view.textclassifier.TextClassificationManager::class.java
            )!!
        val originalClassifier = textClassificationManager.textClassifier

        try {
            textClassificationManager.setTextClassifier(classifier)
            block()
        } finally {
            textClassificationManager.setTextClassifier(originalClassifier)
        }
    }

    private fun createClassificationWithIcon(actions: List<RemoteAction>): TextClassification {
        val builder = TextClassification.Builder().setText("hello")
        actions.forEach { builder.addAction(it) }
        return builder.build()
    }

    private fun createRemoteAction(context: Context, shouldShowIcon: Boolean): RemoteAction {
        val intent = Intent()
        val pendingIntent =
            PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)
        // Create an icon using a res ID that definitely exists, like android.R.drawable.ic_delete
        val icon = Icon.createWithResource(context, android.R.drawable.ic_delete)

        val remoteAction = RemoteAction(icon, "Title", "Description", pendingIntent)
        remoteAction.setShouldShowIcon(shouldShowIcon)
        return remoteAction
    }

    private class TestTextClassifier(private val classification: TextClassification) :
        TextClassifier {
        override fun classifyText(request: TextClassification.Request): TextClassification {
            return classification
        }

        // Provide dummy implementations for the rest to satisfy the interface
        override fun suggestSelection(request: TextSelection.Request): TextSelection {
            return TextSelection.Builder(request.startIndex, request.endIndex).build()
        }

        override fun generateLinks(request: TextLinks.Request): TextLinks {
            return TextLinks.Builder("").build()
        }
    }
}
