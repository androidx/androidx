package androidx.compose.ui.backhandler

import androidx.navigationevent.NavigationEventHandler
import androidx.navigationevent.NavigationEventInfo

internal class BackEventHandler(
    enabled: Boolean,
    private val onBack: () -> Unit
) : NavigationEventHandler<NavigationEventInfo.None>(
    initialInfo = NavigationEventInfo.None,
    isBackEnabled = enabled,
) {
    override fun onBackCompleted() {
        onBack()
    }
}
