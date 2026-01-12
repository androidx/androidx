package androidx.compose.ui.backhandler

import androidx.compose.runtime.CompositeKeyHashCode
import androidx.navigationevent.NavigationEventHandler
import androidx.navigationevent.NavigationEventInfo

internal class BackEventHandler(
    compositeKey: CompositeKeyHashCode,
    enabled: Boolean,
    private val onBack: () -> Unit
) : NavigationEventHandler<NavigationEventInfo>(
    initialInfo = BackEventHandlerInfo(compositeKey),
    isBackEnabled = enabled,
) {
    override fun onBackCompleted() {
        onBack()
    }
}

private data class BackEventHandlerInfo(
    val compositeKey: CompositeKeyHashCode,
) : NavigationEventInfo()
