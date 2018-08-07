package androidx.ui.services.raw_keyboard

/**
 * The user has released a key on the keyboard.
 *
 * See also:
 *
 *  * [RawKeyboard], which uses this interface to expose key data.
 */
class RawKeyUpEvent(data: RawKeyEventData) : RawKeyEvent(data)