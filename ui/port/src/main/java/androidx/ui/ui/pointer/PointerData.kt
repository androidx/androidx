package androidx.ui.ui.pointer

import androidx.ui.core.Duration

// /// Information about the state of a pointer.
data class PointerData(
    // /// Time of event dispatch, relative to an arbitrary timeline.
    val timeStamp: Duration = Duration.zero,
    // /// How the pointer has changed since the last report.
    val change: PointerChange = PointerChange.cancel,
    // /// The kind of input device for which the event was generated.
    val kind: PointerDeviceKind = PointerDeviceKind.touch,
    // /// Unique identifier for the pointing device, reused across interactions.
    val device: Int = 0,
    // /// X coordinate of the position of the pointer, in physical pixels in the
    // /// global coordinate space.
    val physicalX: Double = 0.0,
    // /// Y coordinate of the position of the pointer, in physical pixels in the
    // /// global coordinate space.
    val physicalY: Double = 0.0,
    // /// Bit field using the *Button constants (PRIMARY_MOUSE_BUTTON,
    // /// SECONDARY_STYLUS_BUTTON, etc). For example, if this has the value 6 and the
    // /// [kind] is [PointerDeviceKind.invertedStylus], then this indicates an
    // /// upside-down stylus with both its primary and secondary buttons pressed.
    val buttons: Int = 0,
    // /// Set if an application from a different security domain is in any way
    // /// obscuring this application's window. (Aspirational; not currently
    // /// implemented.)
    val obscured: Boolean = false,
    // /// The pressure of the touch as a number ranging from 0.0, indicating a touch
    // /// with no discernible pressure, to 1.0, indicating a touch with "normal"
    // /// pressure, and possibly beyond, indicating a stronger touch. For devices
    // /// that do not detect pressure (e.g. mice), returns 1.0.
    val pressure: Double = 0.0,
    // /// The minimum value that [pressure] can return for this pointer. For devices
    // /// that do not detect pressure (e.g. mice), returns 1.0. This will always be
    // /// a number less than or equal to 1.0.
    val pressureMin: Double = 0.0,
    // /// The maximum value that [pressure] can return for this pointer. For devices
    // /// that do not detect pressure (e.g. mice), returns 1.0. This will always be
    // /// a greater than or equal to 1.0.
    val pressureMax: Double = 0.0,
    // /// The distance of the detected object from the input surface (e.g. the
    // /// distance of a stylus or finger from a touch screen), in arbitrary units on
    // /// an arbitrary (not necessarily linear) scale. If the pointer is down, this
    // /// is 0.0 by definition.
    val distance: Double = 0.0,
    // /// The maximum value that a distance can return for this pointer. If this
    // /// input device cannot detect "hover touch" input events, then this will be
    // /// 0.0.
    val distanceMax: Double = 0.0,
    // /// The radius of the contact ellipse along the major axis, in logical pixels.
    val radiusMajor: Double = 0.0,
    // /// The radius of the contact ellipse along the minor axis, in logical pixels.
    val radiusMinor: Double = 0.0,
    // /// The minimum value that could be reported for radiusMajor and radiusMinor
    // /// for this pointer, in logical pixels.
    val radiusMin: Double = 0.0,
    // /// The minimum value that could be reported for radiusMajor and radiusMinor
    // /// for this pointer, in logical pixels.
    val radiusMax: Double = 0.0,
    // /// For PointerDeviceKind.touch events:
    // ///
    // /// The angle of the contact ellipse, in radius in the range:
    // ///
    // ///    -pi/2 < orientation <= pi/2
    // ///
    // /// ...giving the angle of the major axis of the ellipse with the y-axis
    // /// (negative angles indicating an orientation along the top-left /
    // /// bottom-right diagonal, positive angles indicating an orientation along the
    // /// top-right / bottom-left diagonal, and zero indicating an orientation
    // /// parallel with the y-axis).
    // ///
    // /// For PointerDeviceKind.stylus and PointerDeviceKind.invertedStylus events:
    // ///
    // /// The angle of the stylus, in radians in the range:
    // ///
    // ///    -pi < orientation <= pi
    // ///
    // /// ...giving the angle of the axis of the stylus projected onto the input
    // /// surface, relative to the positive y-axis of that surface (thus 0.0
    // /// indicates the stylus, if projected onto that surface, would go from the
    // /// contact point vertically up in the positive y-axis direction, pi would
    // /// indicate that the stylus would go down in the negative y-axis direction;
    // /// pi/4 would indicate that the stylus goes up and to the right, -pi/2 would
    // /// indicate that the stylus goes to the left, etc).
    val orientation: Double = 0.0,
    // /// For PointerDeviceKind.stylus and PointerDeviceKind.invertedStylus events:
    // ///
    // /// The angle of the stylus, in radians in the range:
    // ///
    // ///    0 <= tilt <= pi/2
    // ///
    // /// ...giving the angle of the axis of the stylus, relative to the axis
    // /// perpendicular to the input surface (thus 0.0 indicates the stylus is
    // /// orthogonal to the plane of the input surface, while pi/2 indicates that
    // /// the stylus is flat on that surface).
    val tilt: Double = 0.0
)