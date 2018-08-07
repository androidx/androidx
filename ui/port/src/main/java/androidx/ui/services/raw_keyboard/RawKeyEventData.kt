package androidx.ui.services.raw_keyboard

/**
 * Base class for platform specific key event data.
 *
 * This base class exists to have a common type to use for each of the
 * target platform's key event data structures.
 *
 * See also:
 *
 *  * [RawKeyEventDataAndroid], a specialization for Android.
 *  * [RawKeyEventDataFuchsia], a specialization for Fuchsia.
 *  * [RawKeyDownEvent] and [RawKeyUpEvent], the classes that hold the
 *    reference to [RawKeyEventData] subclasses.
 *  * [RawKeyboard], which uses these interfaces to expose key data.
 */
// TODO(Migration/xbhatnag): @immutable
abstract class RawKeyEventData {
    // Abstract const constructor. This constructor enables subclasses to provide
    // const constructors so that they can be used in const expressions.
    // const RawKeyEventData();
}