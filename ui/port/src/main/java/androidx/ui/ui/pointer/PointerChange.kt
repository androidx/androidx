package androidx.ui.ui.pointer

// /// How the pointer has changed since the last report.
enum class PointerChange {
    // /// The input from the pointer is no longer directed towards this receiver.
    cancel,

    // /// The device has started tracking the pointer.
    // ///
    // /// For example, the pointer might be hovering above the device, having not yet
    // /// made contact with the surface of the device.
    add,

    // /// The device is no longer tracking the pointer.
    // ///
    // /// For example, the pointer might have drifted out of the device's hover
    // /// detection range or might have been disconnected from the system entirely.
    remove,

    // /// The pointer has moved with respect to the device while not in contact with
    // /// the device.
    hover,

    // /// The pointer has made contact with the device.
    down,

    // /// The pointer has moved with respect to the device while in contact with the
    // /// device.
    move,

    // /// The pointer has stopped making contact with the device.
    up
}