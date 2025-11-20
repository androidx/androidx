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

import android.content.Context
import android.os.Build
import android.text.TextUtils
import android.view.textclassifier.TextClassification
import android.view.textclassifier.TextClassificationContext
import android.view.textclassifier.TextClassificationManager
import android.view.textclassifier.TextClassifier
import android.view.textclassifier.TextSelection
import androidx.annotation.RequiresApi
import androidx.annotation.VisibleForTesting
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.text.contextmenu.addProcessedTextContextMenuItems
import androidx.compose.foundation.text.contextmenu.builder.TextContextMenuBuilderScope
import androidx.compose.foundation.text.contextmenu.builder.textClassificationItem
import androidx.compose.foundation.text.selection.TextClassifierHelperMethods.createTextClassificationSession
import androidx.compose.foundation.text.selection.TextClassifierHelperMethods.hasLegacyAssistItem
import androidx.compose.foundation.text.selection.TextClassifierHelperMethods.toAndroidLocaleList
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.intl.LocaleList
import androidx.compose.ui.util.fastForEachIndexed
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

/**
 * The [CoroutineContext] used for launching [TextClassifier] operations within the selection
 * features.
 *
 * This context is expected to be associated with a worker thread to ensure that text
 * classification, which can be a time-consuming operation, does not block the UI thread. Providing
 * a [CoroutineContext] not backed by a worker thread may lead to performance issues or unexpected
 * behavior with [TextClassifier].
 */
val LocalTextClassifierCoroutineContext =
    staticCompositionLocalOf<CoroutineContext> { Dispatchers.IO }

/**
 * Derived from ViewConfiguration.getSmartSelectionInitializingTimeout. This is the timeout for
 * creating a TextClassifier. This is computed from
 * `ViewConfiguration.getSmartSelectionInitializingTimeout` minus
 * `ViewConfiguration.getSmartSelectionInitializedTimeout`. We separate the TextClassifier creation
 * timeout and classification timeout for simplicity.
 */
private const val TEXT_CLASSIFIER_INITIALIZATION_TIMEOUT_MILLIS = 300L

/**
 * Copied from ViewConfiguration.getSmartSelectionInitializedTimeout. This is the timeout for all
 * TextClassifier calls when the TextClassifier object is already created. If TextClassifier doesn't
 * return in this time, we will cancel the call and treat it as if TextClassifier returns null.
 */
private const val TEXT_CLASSIFICATION_TIMEOUT_MILLIS = 200L

@RequiresApi(28)
@VisibleForTesting
internal var PlatformSelectionBehaviorsFactory:
    (CoroutineContext, Context, SelectedTextType, LocaleList?) -> PlatformSelectionBehaviors? =
    { coroutineContext, context, selectionType, localeList ->
        PlatformSelectionBehaviorsImpl(coroutineContext, context, selectionType, localeList)
    }

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal actual fun rememberPlatformSelectionBehaviors(
    selectedTextType: SelectedTextType,
    localeList: LocaleList?,
): PlatformSelectionBehaviors? {
    if (Build.VERSION.SDK_INT < 28) {
        // Smart selection features are not supported under API 28.
        return null
    }
    val context = LocalContext.current
    val coroutineContext = LocalTextClassifierCoroutineContext.current
    return remember(coroutineContext, context, selectedTextType, localeList) {
        PlatformSelectionBehaviorsFactory(coroutineContext, context, selectedTextType, localeList)
    }
}

@RequiresApi(28)
internal class PlatformSelectionBehaviorsImpl(
    private val coroutineContext: CoroutineContext,
    private val context: Context,
    private val selectedTextType: SelectedTextType,
    private val localeList: LocaleList?,
) : PlatformSelectionBehaviors {
    private val mutex = Mutex()
    private var textClassificationSession: TextClassifier? = null

    // The textClassificationResult stores the result of the latest text classification operation.
    // It is a MutableState because changes to this value should trigger a rebuild of the
    // ContextMenuData which depend on this classification data.
    private var textClassificationResult: TextClassificationResult? by mutableStateOf(null)

    private val androidLocalList
        get() =
            localeList?.let { toAndroidLocaleList(it) }
                ?: android.os.LocaleList(Locale.current.platformLocale)

    override suspend fun suggestSelectionForLongPressOrDoubleClick(
        text: CharSequence,
        selection: TextRange,
    ): TextRange? {
        if (text.isEmpty() || selection.collapsed) {
            return null
        }
        return requireTextClassificationSession {
            val builder =
                TextSelection.Request.Builder(text, selection.min, selection.max)
                    .setDefaultLocales(androidLocalList)
            if (Build.VERSION.SDK_INT >= 31) {
                builder.setIncludeTextClassification(true)
            }
            val request = builder.build()
            val suggestedSelection = suggestSelection(request)
            val newSelection =
                TextRange(
                    suggestedSelection.selectionStartIndex,
                    suggestedSelection.selectionEndIndex,
                )
            if (Build.VERSION.SDK_INT >= 31 && suggestedSelection.textClassification != null) {
                mutex.withLock {
                    textClassificationResult =
                        TextClassificationResult(
                            text,
                            newSelection,
                            suggestedSelection.textClassification!!,
                        )
                }
            } else {
                classifyText(text, newSelection, this)
            }
            newSelection
        }
    }

    private suspend fun onShowContextMenuOrSelectionToolbar(
        text: CharSequence,
        selection: TextRange,
    ) {
        if (text.isEmpty() || selection.collapsed) {
            return
        }
        requireTextClassificationSession { classifyText(text, selection, this) }
    }

    override suspend fun onShowContextMenu(
        text: CharSequence,
        selection: TextRange,
        secondaryClickLocation: Offset?,
    ) {
        onShowContextMenuOrSelectionToolbar(text, selection)
    }

    override suspend fun onShowSelectionToolbar(text: CharSequence, selection: TextRange) {
        onShowContextMenuOrSelectionToolbar(text, selection)
    }

    private suspend fun classifyText(
        text: CharSequence,
        selection: TextRange,
        textClassifier: TextClassifier,
    ) {
        mutex.withLock {
            if (textClassificationResult?.canReuse(text, selection) == true) {
                // Do nothing, the text classification result is up to date.
                return
            }
        }
        val request =
            TextClassification.Request.Builder(text, selection.min, selection.max)
                .setDefaultLocales(androidLocalList)
                .build()
        val textClassification = textClassifier.classifyText(request)

        mutex.withLock {
            textClassificationResult = TextClassificationResult(text, selection, textClassification)
        }
    }

    private val AssistantItemKey = Any()

    internal fun TextContextMenuBuilderScope.addSmartSelectionTextContextMenuItems(
        text: CharSequence,
        selection: TextRange,
        child: TextContextMenuBuilderScope.() -> Unit,
    ) {
        val textClassification = tryGetTextClassification(text, selection)
        if (textClassification == null) {
            child()
            return
        }

        if (textClassification.actions.isNotEmpty()) {
            textClassificationItem(AssistantItemKey, textClassification, index = 0)
        } else if (textClassification.hasLegacyAssistItem()) {
            textClassificationItem(AssistantItemKey, textClassification, index = -1)
        }

        child()

        textClassification.actions.fastForEachIndexed { index, remoteAction ->
            if (index > 0) {
                textClassificationItem(AssistantItemKey, textClassification, index = index)
            }
        }
    }

    /**
     * Get the text classification result we created in [onShowContextMenuOrSelectionToolbar]
     * callback. We moved the text classification computation to
     * [onShowContextMenuOrSelectionToolbar] so that
     * 1) building context menu data won't block the UI thread.
     * 2) avoid unnecessary text classification computation. The context menu data is updated
     *    whenever the states that creates it update. This means context menu data might update even
     *    if the context menu is not shown.
     */
    fun tryGetTextClassification(text: CharSequence, selection: TextRange): TextClassification? {
        val acquired = mutex.tryLock()
        if (!acquired) {
            // There is an ongoing text classification operation. This is possible only
            // if the selection has updated since the context menu is shown. Thus, building the
            // TextClassification items is not necessary as the result is already stale.
            // We can avoid blocking the thread and directly return null here.
            return null
        }
        val textClassificationResult = textClassificationResult
        return if (textClassificationResult?.canReuse(text, selection) == true) {
                textClassificationResult.textClassification
            } else {
                null
            }
            .also { mutex.unlock() }
    }

    private suspend fun <T> requireTextClassificationSession(
        block: suspend TextClassifier.() -> T
    ): T? {
        return withContext(coroutineContext) {
            val textClassificationSession =
                mutex.withLock {
                    val session = this@PlatformSelectionBehaviorsImpl.textClassificationSession

                    if (session == null || session.isDestroyed) {
                        withTimeoutOrNull(TEXT_CLASSIFIER_INITIALIZATION_TIMEOUT_MILLIS) {
                            createTextClassificationSession(context, selectedTextType).also {
                                this@PlatformSelectionBehaviorsImpl.textClassificationSession = it
                            }
                        }
                    } else {
                        session
                    }
                }
            withTimeoutOrNull(TEXT_CLASSIFICATION_TIMEOUT_MILLIS) {
                textClassificationSession?.block()
            }
        }
    }
}

private data class TextClassificationResult(
    val text: CharSequence,
    val selection: TextRange,
    val textClassification: TextClassification,
)

private fun TextClassificationResult.canReuse(text: CharSequence, selection: TextRange): Boolean {
    return selection == this.selection && text == this.text
}

/**
 * Add platform specific items to the context menu, including both smart suggested items by
 * [TextClassifier] and `PROCESS_TEXT` item.
 */
internal fun TextContextMenuBuilderScope.addPlatformTextContextMenuItems(
    context: Context,
    editable: Boolean,
    text: CharSequence?,
    selection: TextRange?,
    platformSelectionBehaviors: PlatformSelectionBehaviors?,
    child: TextContextMenuBuilderScope.() -> Unit,
) {
    if (
        Build.VERSION.SDK_INT < 28 ||
            text == null ||
            selection == null ||
            platformSelectionBehaviors == null ||
            platformSelectionBehaviors !is PlatformSelectionBehaviorsImpl
    ) {
        child()
        if (text != null && selection != null) {
            addProcessedTextContextMenuItems(context, editable, text, selection)
        }
        return
    }

    with(platformSelectionBehaviors) {
        addSmartSelectionTextContextMenuItems(text, selection, child)
    }
    addProcessedTextContextMenuItems(context, editable, text, selection)
}

@RequiresApi(28)
internal object TextClassifierHelperMethods {
    fun createTextClassificationSession(
        context: Context,
        selectedTextType: SelectedTextType,
    ): TextClassifier {
        val textClassificationManager =
            context.getSystemService(TextClassificationManager::class.java)

        val widgetType =
            when (selectedTextType) {
                SelectedTextType.EditableText -> TextClassifier.WIDGET_TYPE_EDITTEXT
                SelectedTextType.StaticText -> TextClassifier.WIDGET_TYPE_TEXTVIEW
            }
        val textClassificationContext =
            TextClassificationContext.Builder(context.packageName, widgetType).build()
        return textClassificationManager.createTextClassificationSession(textClassificationContext)
    }

    fun toAndroidLocaleList(localeList: LocaleList): android.os.LocaleList {
        return localeList.let { it ->
            android.os.LocaleList(*it.map { it.platformLocale }.toTypedArray())
        }
    }

    @Suppress("DEPRECATION")
    internal fun TextClassification.hasLegacyAssistItem(): Boolean {
        // Check whether we have the UI data and action.
        return (icon != null || !TextUtils.isEmpty(label)) &&
            (intent != null || onClickListener != null)
    }
}
