package androidx.ui.services.raw_keyboard

/**
 * Platform-specific key event data for Android.
 *
 * This object contains information about key events obtained from Android's
 * `KeyEvent` interface.
 *
 * See also:
 *
 *  * [RawKeyDownEvent] and [RawKeyUpEvent], the classes that hold the
 *    reference to [RawKeyEventData] subclasses.
 *  * [RawKeyboard], which uses these interfaces to expose key data.
 */
data class RawKeyEventData(
    // See <https://developer.android.com/reference/android/view/KeyEvent.html#getFlags()>
    val flags: Int = 0,
    // See <https://developer.android.com/reference/android/view/KeyEvent.html#getUnicodeChar()>
    val codePoint: Int = 0,
    // See <https://developer.android.com/reference/android/view/KeyEvent.html#getKeyCode()>
    val keyCode: Int = 0,
    // See <https://developer.android.com/reference/android/view/KeyEvent.html#getScanCode()>
    val scanCode: Int = 0,
    // See <https://developer.android.com/reference/android/view/KeyEvent.html#getMetaState()>
    val metaState: Int = 0
)
