package androidx.ui.services.raw_keyboard

import android.view.KeyEvent
import androidx.ui.foundation.assertions.FlutterError

/**
 * Base class for raw key events.
 *
 * Raw key events pass through as much information as possible from the
 * underlying platform's key events, which makes they provide a high level of
 * fidelity but a low level of portability.
 *
 * See also:
 *
 *  * [RawKeyDownEvent], a specialization for events representing the user pressing a key.
 *  * [RawKeyUpEvent], a specialization for events representing the user releasing a key.
 *  * [RawKeyboard], which uses this interface to expose key data.
 *  * [RawKeyboardListener], a widget that listens for raw key events.
 */
// TODO(Migration/xbhatnag): Possibly Private Constructor
abstract class RawKeyEvent(
    val data: RawKeyEventData
) {
    companion object {
        fun fromKeyEvent(type: String, event: KeyEvent): RawKeyEvent {
            val data = RawKeyEventData(
                flags = event.flags,
                codePoint = event.unicodeChar,
                keyCode = event.keyCode,
                scanCode = event.scanCode,
                metaState = event.metaState)

            return when (type) {
                "keydown" -> RawKeyDownEvent(data = data)
                "keyup" -> RawKeyUpEvent(data = data)
                else -> throw FlutterError("Unknown key event type: $type")
            }
        }
    }
}

// @immutable
// abstract class RawKeyEvent {
//     /// Initializes fields for subclasses.
//     const RawKeyEvent({
//         @required this.data,
//     });
//
//     /// Creates a concrete [RawKeyEvent] class from a message in the form received
//     /// on the [SystemChannels.keyEvent] channel.
//     factory RawKeyEvent.fromMessage(Map<String, dynamic> message) {
//         RawKeyEventData data;
//
//         final String keymap = message['keymap'];
//         switch (keymap) {
//             case 'android':
//             data = new RawKeyEventDataAndroid(
//                     flags: message['flags'] ?? 0,
//             codePoint: message['codePoint'] ?? 0,
//             keyCode: message['keyCode'] ?? 0,
//             scanCode: message['scanCode'] ?? 0,
//             metaState: message['metaState'] ?? 0,
//             );
//             break;
//             case 'fuchsia':
//             data = new RawKeyEventDataFuchsia(
//                     hidUsage: message['hidUsage'] ?? 0,
//             codePoint: message['codePoint'] ?? 0,
//             modifiers: message['modifiers'] ?? 0,
//             );
//             break;
//             default:
//             // We don't yet implement raw key events on iOS, but we don't hit this
//             // exception because the engine never sends us these messages.
//             throw new FlutterError('Unknown keymap for key events: $keymap');
//         }
//
//         final String type = message['type'];
//         switch (type) {
//             case 'keydown':
//             return new RawKeyDownEvent(data: data);
//             case 'keyup':
//             return new RawKeyUpEvent(data: data);
//             default:
//             throw new FlutterError('Unknown key event type: $type');
//         }
//     }
//
//     /// Platform-specific information about the key event.
//     final RawKeyEventData data;
// }