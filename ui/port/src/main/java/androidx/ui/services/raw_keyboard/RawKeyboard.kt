package androidx.ui.services.raw_keyboard

import android.view.KeyEvent
import androidx.ui.foundation.ValueChanged

/**
 * An interface for listening to raw key events.
 *
 * Raw key events pass through as much information as possible from the
 * underlying platform's key events, which makes them provide a high level of
 * fidelity but a low level of portability.
 *
 * A [RawKeyboard] is useful for listening to raw key events and hardware
 * buttons that are represented as keys. Typically used by games and other apps
 * that use keyboards for purposes other than text entry.
 *
 * See also:
 *
 *  * [RawKeyDownEvent] and [RawKeyUpEvent], the classes used to describe
 *    specific raw key events.
 *  * [RawKeyboardListener], a widget that listens for raw key events.
 *  * [SystemChannels.keyEvent], the low-level channel used for receiving
 *    events from the system.
 */

object RawKeyboard {
    val _listeners = mutableListOf<ValueChanged<RawKeyEvent>>()

    /**
     * Calls the listener every time the user presses or releases a key.
     *
     * Listeners can be removed with [removeListener].
     */
    fun addListener(listener: ValueChanged<RawKeyEvent>) {
        _listeners.add(listener)
    }

    /**
     * Stop calling the listener every time the user presses or releases a key.
     *
     * Listeners can be added with [addListener].
     */
    fun removeListener(listener: ValueChanged<RawKeyEvent>) {
        _listeners.remove(listener)
    }

    // TODO(Migration/xbhatnag): Async & Future
    fun _handleKeyEvent(type: String, event: KeyEvent) {
        val rawKeyEvent = RawKeyEvent.fromKeyEvent(type, event)
        if (_listeners.isEmpty())
            return
        for (listener in _listeners.toList()) {
            if (_listeners.contains(listener))
                listener(rawKeyEvent)
        }
    }
//    Future<dynamic> _handleKeyEvent(dynamic message) async {
//        if (_listeners.isEmpty)
//            return;
//        final RawKeyEvent event = new RawKeyEvent.fromMessage(message);
//        if (event == null)
//            return;
//        for (ValueChanged<RawKeyEvent> listener in new List<ValueChanged<RawKeyEvent>>.from(_listeners))
//        if (_listeners.contains(listener))
//            listener(event);
//    }
}