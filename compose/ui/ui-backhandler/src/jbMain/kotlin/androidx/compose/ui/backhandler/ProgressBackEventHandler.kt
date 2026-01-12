package androidx.compose.ui.backhandler

import androidx.compose.runtime.CompositeKeyHashCode
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
    compositeKey: CompositeKeyHashCode,
    enabled: Boolean,
    private val onBack: suspend (progress: Flow<BackEventCompat>) -> Unit,
    private val coroutineScope: CoroutineScope
) : NavigationEventHandler<NavigationEventInfo>(
    initialInfo = ProgressBackEventHandlerInfo(compositeKey),
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
            if (progressChannel == null) {
                // it was an instant back event (Esc click)
                // we need to start a new progress event stream
                onBackStarted(NavigationEvent())
            }
            progressChannel?.close()
            progressChannel = null
        }
    }
}

private data class ProgressBackEventHandlerInfo(
    val compositeKey: CompositeKeyHashCode,
) : NavigationEventInfo()
