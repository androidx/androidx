package androidx.ui.services.raw_keyboard

/**
 * The user has pressed a key on the keyboard.
 *
 * See also:
 *
 *  * [RawKeyboard], which uses this interface to expose key data.
 */
class RawKeyDownEvent(data: RawKeyEventData) : RawKeyEvent(data)