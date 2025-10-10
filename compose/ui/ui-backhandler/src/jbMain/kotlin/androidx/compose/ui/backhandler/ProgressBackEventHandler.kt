package androidx.compose.ui.backhandler

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.navigationevent.NavigationEvent
import androidx.navigationevent.NavigationEventHandler
import androidx.navigationevent.NavigationEventInfo
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.launch

@OptIn(ExperimentalComposeUiApi::class)
internal class ProgressBackEventHandler(
    enabled: Boolean,
    private val onBack: suspend (progress: Flow<BackEventCompat>) -> Unit,
    private val coroutineScope: CoroutineScope
) : NavigationEventHandler<NavigationEventInfo.None>(
    initialInfo = NavigationEventInfo.None,
    isBackEnabled = enabled,
) {
    private var progressChannel: Channel<BackEventCompat>? = null

    override fun onBackStarted(event: NavigationEvent) {
        progressChannel?.close(CancellationException("Disposed"))
        progressChannel = Channel<BackEventCompat>().also { channel ->
            coroutineScope.launch {
                onBack(channel.consumeAsFlow())
            }
        }
    }

    override fun onBackProgressed(event: NavigationEvent) {
        progressChannel?.let { channel ->
            val swipeEdge = when (event.swipeEdge) {
                NavigationEvent.EDGE_RIGHT -> BackEventCompat.EDGE_RIGHT
                else -> BackEventCompat.EDGE_LEFT
            }
            val event = BackEventCompat(
                event.touchX, event.touchY, event.progress, swipeEdge
            )
            coroutineScope.launch {
                channel.send(event)
            }
        }
    }

    override fun onBackCancelled() {
        coroutineScope.launch {
            progressChannel?.close(CancellationException("Cancelled"))
            progressChannel = null
        }
    }

    override fun onBackCompleted() {
        coroutineScope.launch {
            progressChannel?.close()
            progressChannel = null
        }
    }
}
