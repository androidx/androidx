package androidx.ui.services.raw_keyboard

/**
 * Platform-specific key event data for Android.
 *
 * This object contains information about key events obtained from Android's
 * `KeyEvent` interface.
 *
 * See also:
 *
 *  * [RawKeyboard], which uses this interface to expose key data.
 */
// Creates a key event data structure specific for Android.
// TODO(Migration/xbhatnag): Consider merging this class with RawKeyEventData
class RawKeyEventDataAndroid(
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
) : RawKeyEventData()