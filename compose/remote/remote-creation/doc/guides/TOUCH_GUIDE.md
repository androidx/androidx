# Touch Interactions Guide in RemoteCompose

RemoteCompose provides a powerful `addTouch` system that allows you to create interactive components (like sliders, knobs, and joysticks) that run entirely on the player side. This ensures low-latency interactions without requiring constant communication with the server.

## Core Concepts

The `addTouch` function creates a **Remote Variable** that changes in response to touch events. You can then use this variable in other drawing or layout operations (e.g., as a rotation angle, an offset, or a scale factor).

### Basic Usage

```kotlin
val touchPos = rc.addTouch(
    defValue = 0f,            // Initial value
    min = 0f,                 // Minimum value
    max = 100f,               // Maximum value
    touchMode = STOP_GENTLY,  // Behavior when touch is released
    velocityId = 0f,          // Reserved for future use
    touchEffects = 4,         // Haptic feedback (e.g., CLOCK_TICK)
    touchSpec = null,         // Extra parameters for stop modes
    easingSpec = null,        // Custom deceleration (maxTime, acceleration, etc.)
    // Mapping expression: how touch movement affects the value
    RemoteContext.FLOAT_TOUCH_POS_X 
)
```

## Parameters Explained

### 1. Limits and Defaults
- `defValue`: The starting value. Can be a constant or a remote ID.
- `min` / `max`: Boundary constraints. If `min` is a NaN encoded ID with value 0, **Wrap Mode** is enabled (value wraps from `max` back to `min`).

### 2. Touch Mode (`stopMode`)
This defines what happens when the user lifts their finger:
- `STOP_INSTANTLY` (1): Movement stops exactly where the finger was released.
- `STOP_GENTLY` (0): The value continues moving with inertia and decelerates.
- `STOP_ENDS` (2): Snaps to either the `min` or `max` boundary.
- `STOP_NOTCHES_EVEN` (3): Snaps to evenly spaced notches. `touchSpec[0]` defines the number of notches.
- `STOP_NOTCHES_PERCENTS` (4): Snaps to specific positions defined as a percentage (0..1) of the range in `touchSpec`.
- `STOP_NOTCHES_ABSOLUTE` (5): Snaps to specific absolute values defined in `touchSpec`.
- `STOP_ABSOLUTE_POS` (6): Jumps directly to the touch position (typical for "jump to click").

### 3. Touch Mapping Expression
The trailing `vararg exp: Float` (or `RFloat` in Kotlin) is a Reverse Polish Notation (RPN) expression that maps touch coordinates to the variable's value.
- **Direct Mapping**: `RemoteContext.FLOAT_TOUCH_POS_X` maps horizontally.
- **Angular Mapping**: Use `ATAN2` to create circular interactions (like knobs).
  ```kotlin
  // Example: Mapping touch to rotation angle around (cx, cy)
  tx, cx, SUB, ty, cy, SUB, ATAN2, RAD_TO_DEG, ADD_OFFSET
  ```

### 4. Touch Effects
Used primarily for haptic feedback. Common values from `Rc.Haptic`:
- `CLOCK_TICK` (4): Subtle tick when crossing notches.
- `VIRTUAL_KEY` (2): Standard press feel.

## Integration with Components

To restrict touch detection to a specific area, place the `addTouch` call inside a `canvas` or `box` component. RemoteCompose automatically scopes touch listeners to the bounding box of the component they are defined in.

### Example: Horizontal Slider

```kotlin
canvas(RecordingModifier().size(200f, 40f)) {
    val w = ComponentWidth()
    val sliderPos = addTouch(
        100f, 0f, w, STOP_GENTLY, 0f, 4, null, null,
        RemoteContext.FLOAT_TOUCH_POS_X
    )
    
    // Draw track
    painter.setColor(Color.GRAY).commit()
    drawLine(0f, 20f, w, 20f)
    
    // Draw thumb
    painter.setColor(Color.RED).commit()
    drawCircle(sliderPos, 20f, 15f)
}
```

## Advanced: Custom Deceleration
You can pass a `FloatArray` to `easingSpec` to control the "feel" of inertia:
```kotlin
// [0f (type), maxTime, maxAcceleration, maxVelocity]
val myEasing = floatArrayOf(0f, 1.5f, 10f, 20f)
```

## Tips for Success
1. **RPN Helpers**: In Kotlin, you can use `RFloat` operators (`+`, `-`, `*`, `/`) to build your mapping expression more naturally before passing it to `addTouch`.
2. **Debug**: Use `addDebugMessage("label", touchVar)` to see the value changing in real-time in the player.
3. **Delta vs Absolute**: By default, `addTouch` calculates the delta from where the touch started. For "jump-to-position" behavior, use `STOP_ABSOLUTE_POS`.
