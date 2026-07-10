# Click Handling and Actions in RemoteCompose

RemoteCompose provides a flexible click system based on modifiers that can trigger both internal state changes and host-side callbacks.

## Click Propagation Process

Click events follow a similar propagation path to touch events but are specifically triggered on the "up" phase of a gesture if the movement threshold hasn't been exceeded.

### 1. Root Dispatch (`CoreDocument`)
When a user clicks, `CoreDocument.onClick(x, y)` is called with root-relative coordinates.

### 2. Backward Search (`Component`)
The document iterates through its component tree in **reverse drawing order** (top-to-bottom). For each component:
- It checks if the point `(x, y)` is within the component's bounds via `contains(x, y)`.
- If contained, it recursively calls `onClick` on its children.
- If no child handles the click, it dispatches to its own `onClick` handlers (usually via `ComponentModifiers`).

### 3. Local Transformation
Before reaching a `ClickModifierOperation`, the coordinates are transformed:
- **Root Space**: Used for initial hit detection.
- **Content Space**: Passed to children and modifiers, accounting for parent scrolling and padding.

## Click Modifiers (`ClickModifierOperation`)

The primary way to make a component interactive is by adding a click modifier.

### Interaction Features
- **Ripple Animation**: Triggers a visual feedback ripple at the exact touch location. The ripple uses `Easing.CUBIC_STANDARD` and animates both color and radius over 1000ms.
- **Haptic Feedback**: Automatically triggers a host haptic effect (type 3) upon a successful click.
- **Role & Semantics**: Identifies the component as a `Role.BUTTON` for accessibility services (e.g., TalkBack).

## Actions

A `ClickModifierOperation` acts as a container for one or more `ActionOperation`s.

| Action Type | Description |
| :--- | :--- |
| **ValueChange** | Updates a RemoteCompose variable (Float, String, or Boolean). Useful for toggling states like `isExpanded` or `clickCount`. |
| **HostAction** | Sends a metadata string back to the host application (Android/iOS). This is used for navigation or triggering native logic. |
| **Custom Actions** | Can be implemented to perform complex sequences, such as multiple variable updates or conditional logic. |

## Interaction with Scrolling

The system ensures that clicks and scrolls don't conflict:
1. **Threshold**: If a touch move exceeds a small distance threshold (e.g., 5-10 pixels), the gesture is "captured" by a scroll parent.
2. **Cancellation**: Once captured by a scroll, any pending click animations or actions on child components are cancelled.
3. **Consumption**: If a click is handled by a child, the event returns `true`, stopping further propagation to parent click handlers.
