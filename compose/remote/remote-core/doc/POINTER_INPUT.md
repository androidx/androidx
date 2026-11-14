# RemoteCompose Pointer Input

How a touch on the screen reaches the operations of a RemoteCompose document.

This document describes the **unified pointer input** system. One router sends
every pointer event to the handlers under the finger, in three passes, like
Jetpack Compose does. It replaces several separate touch paths (clicks, touch
listeners, touch handlers). This document calls those the **legacy path**.

Unified pointer input is:
- **Opt-in**: a document turns it on with a header flag. Without the flag
  nothing changes.
- **Wire compatible**: no new operations and no new fields. The same bytes
  play on either path. Only the header flag differs.
- **Experimental**: read [Known issues](#known-issues-and-limitations) before
  you turn it on.

References:
- Tracking bug: b/567170463
- Design doc: go/rc-pointerinput

## Turning it on

The flag is header property **33**, `Header.FEATURE_UNIFIED_POINTER_INPUT`.
It is an INT: `0` is off (the default) and `1` is on.

| From | How |
| :--- | :--- |
| Java writer | Pass `RemoteComposeWriter.hTag(Header.FEATURE_UNIFIED_POINTER_INPUT, 1)` to the `RemoteComposeWriter` constructor |
| Kotlin DSL | `RcHeaderTag.raw(Header.FEATURE_UNIFIED_POINTER_INPUT, 1)` |
| JSON | `"header": { "unifiedPointerInput": 1 }` |

`remote-creation-compose` has no API for the flag yet. Use a `Profile` whose
writer factory adds the header tag (`createPlayer(experimental = true)` in
`ClickModifierOperationTest` builds one).

The flag changes two things.

**1. Which classes are created when the document is loaded.**
`RemoteComposeBuffer.inflateFromBuffer` reads the header. When the flag is on
it calls `Operations.createMapWithNewPointerInputOperations`, which swaps the
readers of eight opcodes:

| Opcode | Legacy class | Unified class |
| :--- | :--- | :--- |
| `MODIFIER_CLICK` | `ClickModifierOperation` | `ClickModifierOperation2` |
| `MODIFIER_MULTI_CLICK` | `MultiClickModifier` | `MultiClickModifierOperation2` |
| `MODIFIER_TOUCH_DOWN` | `TouchDownModifierOperation` | `TouchDownModifierOperation2` |
| `MODIFIER_TOUCH_UP` | `TouchUpModifierOperation` | `TouchUpModifierOperation2` |
| `MODIFIER_TOUCH_CANCEL` | `TouchCancelModifierOperation` | `TouchCancelModifierOperation2` |
| `MODIFIER_RIPPLE` | `RippleModifierOperation` | `RippleModifierOperation2` |
| `MODIFIER_SCROLL` | `ScrollModifierOperation` | `ScrollModifierOperation2` |
| `TOUCH_EXPRESSION` | `TouchExpression` | `TouchExpressionOperation2` |

The `*2` classes are separate implementations. Each one started as a copy of
the legacy class and was then rewritten. That is why every `*2`
file is wrapped in `// CPD-OFF` and `// CPD-ON` comments: they silence the
duplicate code check until the legacy classes are deleted.

**2. Where the view sends touch events.**
`RemoteComposeView.dispatchTouchEvent` checks the flag. When it is on, the
event goes to `PointerInputManager` and the legacy `onTouchEvent` never runs.
When it is off, the view calls `super.dispatchTouchEvent` and the legacy path
runs as before.

## The life of a pointer event

```
MotionEvent
   |
   v
RemoteComposeView.dispatchTouchEvent              (remote-player-view)
   |   flag off -> super.dispatchTouchEvent -> onTouchEvent -> legacy path
   v   flag on
PointerInputManager.dispatchTouchEvent
   |   follows the first finger only, tracks velocity,
   |   builds a PointerInputEvent
   v
EventManager.dispatchEvent                        (remote-core)
   |   picks the router registered for the event type
   v
PointerInputRouter.routeEvent
   |   DOWN: HitPathTracker finds the components under the pointer
   |   every event: three passes over those components
   |       1. INITIAL   parent -> child
   |       2. MAIN      child  -> parent
   |       3. FINAL     parent -> child
   |   UP / CANCEL: the pointer is removed from the hit path
   v
EventHandler.onEvent                              (the operations)
```

## Where the code lives

All paths are relative to `compose/remote/`.

| Class | Path | Role |
| :--- | :--- | :--- |
| `PointerInputManager` | `remote-player-view/.../player/view/platform/` | Turns a `MotionEvent` into a `PointerInputEvent` |
| `RemoteComposeView` | `remote-player-view/.../player/view/platform/` | Chooses the path, passes the platform gesture values to the context |
| `RemoteComposePlayer` | `remote-player-view/.../player/view/` | Registers the `PointerInputRouter` |
| `PointerInputRouter` | `remote-core/.../core/input/` | Hit testing and the three passes |
| `HitPathTracker` | `remote-core/.../core/input/` | The components under each pointer, as a tree stored in arrays |
| `PointerInputEvent` | `remote-core/.../core/input/` | The event |
| `MetadataUtil` | `remote-core/.../core/input/` | Packs pass, subtype and consumed into the event metadata |
| `Event`, `EventHandler`, `EventRouter`, `EventManager` | `remote-core/.../core/events/` | The generic event system the router plugs into |
| `*Operation2` | `remote-core/.../core/operations/` and below | The handlers |

`EventManager` is generic: it maps an event type to one `EventRouter`.
Pointer input is event type `PointerInputRouter.EVENT_TYPE_POINTER` (`1`).
Unlike other routers, the pointer router ignores `registerHandler`. It finds
handlers by walking the component tree.

## The event

A `PointerInputEvent` has a type, a metadata int and a float array.

**Data** (`getData()`, always 5 floats):

| Index | Value |
| :--- | :--- |
| 0 | Pointer id, a whole number from 0 to 31 |
| 1 | x |
| 2 | y |
| 3 | x velocity, in pixels per second |
| 4 | y velocity, in pixels per second |

The pointer id is a whole number stored in a float. Read it with
`(int) data[0]`. It has to be from 0 to 31, because the tracker keeps the
pointers of a component as the bits of an int. Android pointer ids are in that
range. A player on another platform has to map its own ids into it. The router
throws an `IllegalArgumentException` for an id outside the range, because that
is a bug in the player and not in the document.

The velocity is computed when the pointer goes up. Use it on `UP` only.

The event has no time (b/585824714). Handlers that need time use the clock of
the `RemoteContext`.

**Coordinates.** The event enters the router in the coordinates of the player
view. Before the router gives the event to the handlers of a component, it
changes the x and y of the event to be **local to that component** (it
subtracts `Component.getLocationInWindow`), and changes them back after those
handlers ran. So a handler always sees coordinates relative to its own
component. The router changes the event in place and does not copy it, so a
handler must not keep the event or its data after `onEvent` returns.

**Metadata** (`getMetadata()`), packed by `MetadataUtil`:

| Bits | Field | Values |
| :--- | :--- | :--- |
| 0-1 | Pass | `POINTER_PASS_DISPATCH` 0, `POINTER_PASS_INITIAL` 1, `POINTER_PASS_MAIN` 2, `POINTER_PASS_FINAL` 3 |
| 2-6 | Subtype | `POINTER_SUBTYPE_DOWN` 0, `POINTER_SUBTYPE_UP` 1, `POINTER_SUBTYPE_MOVE` 2, `POINTER_SUBTYPE_CANCEL` 3 |
| 7 | Consumed | 0 or 1 |

`POINTER_PASS_DISPATCH` is the value the event has before the router starts.
Handlers never see it.

## From MotionEvent to PointerInputEvent

`PointerInputManager` follows **the first finger only**. Other fingers are
ignored, and the gesture ends when the first finger goes up, even if other
fingers are still down.

| `MotionEvent` action | Result |
| :--- | :--- |
| `ACTION_DOWN` | `DOWN`. Starts following this pointer |
| `ACTION_MOVE` | `MOVE`, if the first finger is still down |
| `ACTION_POINTER_UP` | `UP`, if the finger that went up is the first finger. Otherwise ignored |
| `ACTION_UP` | `UP`, if the first finger is still down |
| `ACTION_CANCEL` | `CANCEL`, if the first finger is still down |
| `ACTION_POINTER_DOWN` and others | Ignored |

For every event it forwards, it also loads `ID_TOUCH_POS_X`, `ID_TOUCH_POS_Y`
(view coordinates) and `ID_TOUCH_EVENT_TIME` into the `RemoteContext`. Plain
expressions that read these variables keep working, as on the legacy path.

`RemoteComposeView.dispatchTouchEvent` returns `true` when the status has
`POINTER_STATUS_CONSUMED` or `POINTER_STATUS_DISPATCHED` set, and then
invalidates the view. Returning `true` for the `DOWN` is what makes Android
send the rest of the gesture. `DISPATCHED` exists for handlers that watch a
`DOWN` without consuming it, like a scroll container that waits for the touch
slop.

Like the legacy path, it also calls `Limiter.touchBoost()` on `ACTION_DOWN`,
`ACTION_MOVE` and `ACTION_UP`, so the frame rate is not limited during a
touch.

## Hit testing

Hit testing happens once per gesture, on `DOWN`.
`HitPathTracker.addHitPath` starts at the root layout component and goes down
one child at a time. At each level it takes the **last** child (the one drawn
on top) that:
- is not gone and not invisible, and
- contains the pointer (`Component.contains`).

The result is one path from the root to the deepest component under the
pointer. The path stays the same until `UP` or `CANCEL`, even if the pointer
leaves the components or the content scrolls under it. On `UP` and `CANCEL`
the router removes the pointer after the three passes.

The tracker can hold the paths of several pointers. Two pointers share the
components from the root down to the last one that is under both of them, so
the paths form a tree. Today the player only sends one pointer.

**How the tracker stores the tree.** `HitPathTracker` has no node objects. It
keeps the tree in two arrays, so that it allocates nothing during a gesture:

- `mComponents`: the component of each node. It holds references and not
  component ids, because looking up a component by its id boxes the id
  (b/586817696).
- `mNodes`: one `long` for each node. The low 32 bits have one bit for each
  pointer id that is on the node. The high 32 bits are the depth of the node.

The nodes are in tree order: a node comes before its children, and everything
below a node follows it directly. The depth is enough to tell where the nodes
below a node end. For example, with pointer 0 on button A and pointer 1 on
button B, both inside one column:

```
index   component   depth   pointers
0       root        0       0, 1
1       column      1       0, 1
2       button B    2       1
3       button A    2       0
```

- The router goes through the nodes from the first to the last for `INITIAL`
  and `FINAL`, and from the last to the first for `MAIN`. So a parent always
  comes before its children in the first case, and after them in the second.
- The router skips the nodes that the pointer of the event is not on. So an
  event only reaches the components under its own pointer, and an event of a
  pointer that went down outside the root reaches no component.
- With one pointer, the nodes are simply the path from the root to the deepest
  component under the pointer.
- On `DOWN`, a new node is inserted right after its parent. On `UP` and
  `CANCEL`, the bit of the pointer is cleared in every node, and the nodes
  that are left without a pointer are removed.
- The root is always node 0, also when no pointer is down.
- The arrays grow when the paths need more nodes than ever before.

One router serves every document that a player shows. When the root layout
component changes (the player got a new document), the router starts over with
an empty tracker. A document without a root layout component has nothing to
hit: its events are dropped.

## Dispatch

Every event is sent in three passes over the hit path:

| Pass | Direction | Typical use |
| :--- | :--- | :--- |
| `INITIAL` | Parent to child | A parent that wants to act before its children |
| `MAIN` | Child to parent | Gesture detection. All built-in handlers use it |
| `FINAL` | Parent to child | See what was consumed during `MAIN` |

On each component the router calls two kinds of handlers:
- **Modifiers**: the modifiers of a `LayoutComponent` that implement
  `EventHandler`. The list is ordered outer to inner.
- **Content handlers**: operations in the component's `mList` that implement
  `EventHandler` and are not components or modifiers. A
  `TouchExpressionOperation2` in a canvas is one.

A handler is called only if `matchesEvent(EVENT_TYPE_POINTER)` returns `true`.

Order within one component:

| Pass | Order |
| :--- | :--- |
| `INITIAL`, `FINAL` | Modifiers (outer to inner), content handlers, then children |
| `MAIN` | Children, content handlers, then modifiers (inner to outer) |

So in the `MAIN` pass the modifiers of one component run in **reverse** list
order. `MultiClickModifierOperation2` depends on this.

### Consumption

A handler consumes an event by returning a status with
`POINTER_STATUS_CONSUMED` set from `onEvent`. The router then sets the
consumed bit in the event. Every handler that runs later sees it: the rest of
the same component, the rest of the same pass, and the later passes. The bit
is never cleared. The next `MotionEvent` starts unconsumed.

Consumption is advice, not a filter. The router still calls every handler with
a consumed event. Each handler decides what to do with it.

`routeEvent` returns:

| Bit | Meaning |
| :--- | :--- |
| `POINTER_STATUS_CONSUMED` (1) | A handler consumed the event |
| `POINTER_STATUS_DISPATCHED` (2) | At least one pointer handler was on the hit path |

`POINTER_STATUS_NOT_CONSUMED` and `POINTER_STATUS_NONE` are both `0`.

## Rules the built-in handlers follow

These rules come from Compose. Follow them when you add a handler.

1. **Detect gestures in the `MAIN` pass.** Children run before parents, so the
   innermost handler gets the first chance.
2. **A tap needs an unconsumed `DOWN`, and consumes it.** This is how the
   innermost clickable wins over a clickable parent.
3. **A handler that takes over a gesture consumes the `MOVE`.** A scroll
   container does this when the pointer passes the touch slop.
4. **A consumed `MOVE` means the gesture was taken away.** Tap-like handlers
   check for it twice:
   - In `MAIN`: a child consumed it, or an ancestor consumed it in `INITIAL`.
   - In `FINAL`: an ancestor consumed it in its own `MAIN`, which runs *after*
     the child's `MAIN`.
5. **Observers never consume.** Touch down, touch up, touch cancel and ripple
   only watch.
6. **Follow one press at a time.** Most handlers remember the pointer id of
   the `DOWN` and ignore other pointers.

Example: a drag that starts on a button inside a scrolling column. In `MAIN`
the button runs first, then the column.

| Event | Pass | Button (`ClickModifierOperation2`) | Column (`ScrollModifierOperation2`) |
| :--- | :--- | :--- | :--- |
| `DOWN` | `MAIN` | Pressed. Consumes | Follows the pointer. Does not consume |
| Small `MOVE` | `MAIN` | Still pressed | Within the touch slop. Does nothing |
| Large `MOVE` | `MAIN` | Not consumed yet. Still pressed | Past the touch slop. Starts scrolling. Consumes |
| | `FINAL` | Sees the consumed `MOVE`. Cancels the click | |
| `UP` | `MAIN` | Not pressed. Does nothing | Ends the scroll. Consumes |

## The handlers

All of them act in the `MAIN` pass. Some also look at the `FINAL` pass.

| Operation | Consumes | Reacts to consumed events | Summary |
| :--- | :--- | :--- | :--- |
| `ClickModifierOperation2` | `DOWN`, and the `UP` of a click | Yes | Click on `UP` |
| `MultiClickModifierOperation2` | Depends on the click type | Yes | Single click, long press or double click |
| `TouchDownModifierOperation2` | Never | No | Runs actions on `DOWN` |
| `TouchUpModifierOperation2` | Never | Consumed `MOVE` only | Runs actions on `UP` |
| `TouchCancelModifierOperation2` | Never | Consumed `MOVE` only | Runs actions when the press is canceled |
| `RippleModifierOperation2` | Never | No | Press indication |
| `ScrollModifierOperation2` | `MOVE` past the slop, then `UP` / `CANCEL` | Yes | Scrolls its content |
| `TouchExpressionOperation2` | Everything while active | Consumed `DOWN` only | Drives a float from the pointer |

### ClickModifierOperation2

- `DOWN`: if it is not consumed, the press starts and the `DOWN` is consumed.
- `UP`: if the press is still on and the `UP` is not consumed, it runs the
  actions, starts the ripple animation, performs a haptic and consumes the
  `UP`.
- The press is canceled by a consumed `MOVE` (in `MAIN` or `FINAL`), a `MOVE`
  outside the bounds of the component, or `CANCEL`.
- A drag that stays within its bounds still clicks.

### Clicks without a pointer

An accessibility click (and `performClick` with a component id) does not go
through the router. `CoreDocument.performClick` calls `Component.onClick`,
which reaches `ComponentModifiers.onClick`. That method calls
`performClick(context, document)` on `ClickModifierOperation2` and
`MultiClickModifierOperation2`. It runs the actions, the haptic and a ripple
from the center of the component. A multi click modifier only reacts if its
type is `CLICK_TYPE_SINGLE`.

This is planned to change (b/586559163). Accessibility gets an event type of
its own. `ClickModifierOperation2.matchesEvent` returns true for that type
too. When such an event arrives and its subtype is click, the operation calls
its `onClick` without the ripple. `MultiClickModifierOperation2` does the
same. Then the `instanceof` checks in `ComponentModifiers.onClick` and
`performClick` on the two operations go away. Each of them has a TODO.

### MultiClickModifierOperation2

One operation detects one gesture, chosen by `clickType`: `CLICK_TYPE_SINGLE`,
`CLICK_TYPE_LONG` or `CLICK_TYPE_DOUBLE`. A press is canceled the same way as
for a click.

| Type | `DOWN` | Fires | `UP` |
| :--- | :--- | :--- | :--- |
| `SINGLE` | Consumes | On an unconsumed `UP` | Consumes |
| `LONG` | Watches. Does not consume | When the pointer was held for the long press timeout | Consumes if the long press fired |
| `DOUBLE` | First tap: watches. Second tap in time and within the slop: consumes | On the `UP` of the second tap | Consumes if it fired |

**Stacking.** `combinedClickable` writes `SINGLE`, `LONG`, `DOUBLE` on one
component, in that order. In the `MAIN` pass they run in reverse: `DOUBLE`,
`LONG`, `SINGLE`. They work together through consumption:
- `DOUBLE` consumes the `DOWN` of the second tap, so `SINGLE` does not start.
- `LONG` consumes the `UP` after a long press, so `SINGLE` does not click.

This depends on the order in the document. With another order, a double tap
can also run the single click a second time.

The first tap of a double tap still runs the single click. Compose waits to
see if a second tap follows. Three separate operations cannot do that.

**Long press without a timer.** remote-core has no timer. On `DOWN` the
operation asks for a repaint, and `paint()` checks the clock until the timeout
passes. If no frame was drawn in time, the long press is detected on `UP`
instead.

### TouchDownModifierOperation2, TouchUpModifierOperation2, TouchCancelModifierOperation2

They watch a press on their component. They never consume, and they do not
care who consumed the `DOWN`. So they work next to a click on the same
component and on the parent of a clickable child.

Like the legacy classes, they check visibility when the press starts. Hit
testing only puts visible components on the hit path, and touch down only runs
its actions if the component is visible. Touch up and touch cancel do not check
again: they run for a press that started, even if the component was hidden
before the press ended. So actions that undo a touch down (for example a
pressed state) always run. What should happen in this case is not decided yet
(b/588596793).

- **Touch down**: runs on every `DOWN` that reaches the component.
- **Touch up**: runs on the `UP` of a press that started on the component,
  even if the pointer is lifted outside its bounds. It does not need a touch
  down modifier.
- **Touch cancel**: runs once when the press is canceled, by `CANCEL` or by a
  consumed `MOVE` (in `MAIN` or `FINAL`).

After a consumed `MOVE`, touch up ignores the `UP`. So a press ends with
exactly one of up or cancel.

### RippleModifierOperation2

The ripple follows the press. It starts on a `DOWN` within its bounds, grows
for 300 ms, stays while the pointer is held, and fades for 300 ms after `UP`,
`CANCEL` or a `MOVE` outside its bounds. It ignores consumption, so every
ripple on the hit path shows. For the same reason it keeps showing when a
parent starts to scroll, until the pointer leaves its bounds or goes up
(b/585819865).

### ScrollModifierOperation2

- `DOWN`: always followed, never consumed. It also stops a fling that is still
  running.
- `MOVE`: nothing happens until the pointer has moved more than the touch slop
  along the scroll axis. Then the scroll starts and every `MOVE` is consumed.
  The drag is anchored again at that point, so the content does not jump by
  the slop distance.
- A `MOVE` that arrives already consumed cancels the scroll. This is how the
  innermost scroll container wins, and how a slider inside a list works.
- `UP` and `CANCEL` are consumed only if a scroll was in progress.

The scroll position is a float variable driven by a `TouchExpressionOperation2`
nested **inside the modifier**. The router does not reach that nested
expression. The scroll modifier drives it with `onPointerDown`,
`onPointerMove`, `onPointerUp` and `onPointerCancel`. The touch expression
does the fling and the notches.

`scrollByOffset` (used by accessibility) sets the position with
`context.overrideFloat`. The nested touch expression adopts the new value, so
the list can still be dragged afterwards.

### TouchExpressionOperation2

- It lives in the operation list of a component (for example a canvas) and is
  reached as a content handler.
- `DOWN`: if it is not consumed, the gesture starts and the `DOWN` is
  consumed. If a clickable child already took the `DOWN`, the expression does
  not start.
- `MOVE`, `UP`, `CANCEL`: handled and consumed while the gesture is active.
  `CANCEL` behaves like an `UP` with no velocity.
- `TOUCH_POS_X` and `TOUCH_POS_Y` inside the expression are the pointer
  position **local to the component**, taken from the event.
- If something else sets its output (an action, a host override, or
  `scrollByOffset`), `apply()` adopts that value as the new position.

### EventHandlerModifierOperation

This is the generic event handler modifier
(`RecordingModifier.onEvent(type, filter, flags, dataIds, condition, actions)`).
It also receives pointer events when its `type` is `EVENT_TYPE_POINTER`:
- It runs when the event metadata is **exactly** `filter`. Build the filter
  with `MetadataUtil.pack(pass, subtype, consumed)`.
- It returns `flags` as its status. Use `POINTER_STATUS_CONSUMED` to consume.
- It needs the experimental profile.

The tests use it to stand in for "some other handler that consumes".

## Gesture values

The handlers read four values from `RemoteContext`:

| Getter | Default | Set by the view player from |
| :--- | :--- | :--- |
| `getTouchSlop()` | 8 px | `ViewConfiguration.getScaledTouchSlop()` |
| `getDoubleTapSlop()` | 100 px | `ViewConfiguration.getScaledDoubleTapSlop()` |
| `getLongPressTimeoutMillis()` | 400 ms | `ViewConfiguration.getLongPressTimeout()` |
| `getDoubleTapTimeoutMillis()` | 300 ms | `ViewConfiguration.getDoubleTapTimeout()` |

`RemoteComposeView.setClock` sets them when it creates the context. An embedded
document (`RcDocLayout`) forwards the getters to the context of its host.
Combining them into one class is tracked in b/585249842.

## What is different from the legacy path

These differences are on purpose. Each one has a test that only runs with the
flag on.

- **Coordinates.** A touch expression gets the pointer position in its own
  component. The legacy path gives it the position in the document.
- **The innermost handler wins.** A touch expression does not follow a pointer
  that a clickable child handles. With nested scroll containers on the same
  axis, only the inner one scrolls. A touch expression inside a scroll
  container changes its value and the container does not scroll. On the legacy
  path both run in each of these cases.
- **Touch slop.** A scroll starts only after the pointer passes the touch
  slop. The legacy path scrolls from the first move.
- **Click.** A drag that stays within the bounds still clicks. The legacy path
  cancels the click on any drag past the touch slop. A single click modifier
  also clicks after a long hold.
- **Touch up and touch cancel** work without a touch down modifier. When a
  scrolling parent takes over, touch cancel runs (once) and touch up does not.
- **Ripple.** It follows the press and is drawn at the touch position. The
  legacy ripple is a one second animation started by the `DOWN`, with its own
  haptic.
- **Several fingers.** Only the first finger is followed.

## Adding or changing a handler

1. Implement `EventHandler`. Return `eventType == EVENT_TYPE_POINTER` from
   `matchesEvent`.
2. In `onEvent`, read the pass, subtype and consumed bit with `MetadataUtil`.
   Return early for the passes you do not use. Follow the
   [rules above](#rules-the-built-in-handlers-follow).
3. Make sure the router can find it. It must be a modifier of a
   `LayoutComponent`, or an operation in the `mList` of a component. An
   operation nested inside a modifier is **not** found. Its modifier has to
   drive it (see the scroll modifier).
4. If it replaces the reader of an existing opcode, add it to
   `Operations.createMapWithNewPointerInputOperations`.
5. Check the places that test for the legacy class with `instanceof`. The `*2`
   classes do not share a type with the legacy ones, so each place lists both:
   - `ComponentModifiers` (paint, layout, the scroll queries, and `onClick`
     for clicks without a pointer, until b/586559163 is done)
   - `Component` (scroll queries)
   - `LayoutComponent` (`inflate` of the scroll modifier)
   - `ColumnLayout`, `RowLayout` (scroll offset in `getLocationInWindow`)
   - `CoreDocument` (vertical and horizontal scroll queries)
6. Read gesture values from `RemoteContext`. Do not hard code them.
7. Add tests (next section) and run
   `./gradlew :compose:remote:remote-core:lintJvm`.

## Testing

**Operation tests (the main ones).** One file per operation in
`remote-player-view/src/test/java/androidx/compose/remote/core/operations/`
and the packages below it:
`ClickModifierOperationTest`, `MultiClickModifierOperationTest`,
`TouchDownModifierOperationTest`, `TouchUpModifierOperationTest`,
`TouchCancelModifierOperationTest`, `RippleModifierOperationTest`,
`ScrollModifierOperationTest` and `TouchExpressionOperationTest`.

How they are written:
- They are Robolectric tests that build a real document, put it in a
  `RemoteComposePlayer`, send `MotionEvent`s and check what the document did
  (actions that ran, values of named floats). They do not test internal
  classes.
- Each test class runs twice, with `unifiedPointerInput` false and true. The
  document is the same. Only the header flag differs.
- A behavior that only exists with the flag on starts with
  `assumeTrue(unifiedPointerInput)` and a comment that says what the legacy
  path does instead. Do not assert the wrong legacy behavior.
- `TestViewConfiguration` is a Robolectric shadow that lets a test set the
  platform touch slop, double tap slop and timeouts.
- For animations, give the document a clock that follows Robolectric's clock
  (see `robolectricClock` in `TouchExpressionOperationTest`).

**Router tests.**
`remote-core/src/test/java/androidx/compose/remote/core/input/` has
`HitPathTrackerTest`, `HitPathTrackerVisibilityTest`,
`PointerInputRouterDocumentTest` and the other `PointerInputRouter*Test` files.
They drive the real router without a view. Helpers: `PointerInputTestContext`,
`CountingAction` and `TestEvent`.

**Commands.**

```
./gradlew :compose:remote:remote-core:test
./gradlew :compose:remote:remote-player-view:testReleaseUnitTest
./gradlew :compose:remote:remote-player-view:testReleaseUnitTest \
    --tests "*ScrollModifierOperationTest*"
```

**Demos.** Build `:compose:remote:integration-tests:player-view-demos` and open
`ExperimentActivity`. "DSL Click Touch Showcase" and "DSL Nested Interaction
Suite" have a "Unified Pointer Input" checkbox that rebuilds the document with
the flag on or off.

## Known issues and limitations

This list was correct when this document was written. Check it against the
code before you rely on it.

Unless a point says otherwise, these come from reading the code and are not
confirmed with tests.

- **Embedded documents do not get touches.** A tap, a touch down and a scroll
  inside an embedded document (`RcDocLayout`) do nothing when the flag is on
  in the host document, in the embedded document, or in both. This was
  confirmed with a test. `RcDocLayout` forwards touches from the legacy
  methods (`onTouchDown`, `onTouchDrag`, `onTouchUp`, `onClick` and so on) to
  the legacy entry points of the embedded document.
  - Flag on in the host: the router never calls those methods. Its hit path
    stops at the `RcDocLayout`, because the components of the embedded
    document are not in the tree of the host.
  - Flag on only in the embedded document: it is loaded with `*2` operations,
    which do not answer the legacy calls.

  Until this is supported (b/585746862), do not turn the flag on in a document
  that embeds documents or that is embedded.
- **Only one finger is supported.** `PointerInputManager` follows the first
  finger and ignores the others, as the section "From MotionEvent to
  PointerInputEvent" describes. This is on purpose for now, and tests cover
  it. `HitPathTracker` can already hold one path per pointer, and the router
  sends each event only to the components under its pointer. But each handler
  follows one pointer. Gestures with several fingers have to be added
  (b/585747882).
- **Click areas do not work.** A click area (opcode 64) is a child view of
  `RemoteComposeView`. The unified `dispatchTouchEvent` never calls
  `super.dispatchTouchEvent`, so child views get no events. This was confirmed
  with a test. A document without a root layout component gets no pointer
  events at all (b/585818435).
- **An exception in a handler or an action reaches the host.** The legacy
  `onTouchEvent` catches every `Throwable` and disables the document. The
  unified `dispatchTouchEvent` does not catch anything (b/585818057).
- **`View.performClick()` and `View.performLongClick()` are not called.** On
  the legacy path a tap also calls `performClick()` on the player view. That
  runs a click listener set by the host and, with
  `FEATURE_DISALLOW_INTERCEPT_TOUCH`, passes an unhandled click to the parent
  view (b/585818200).
- **Only the view player supports the flag.** Only
  `remote-player-view` registers the router and sends pointer events. The
  operations are swapped for every player, because that happens in
  `RemoteComposeBuffer`. So in another player (for example
  `remote-player-compose`) a document with the flag on gets `*2` operations
  that never receive events.
- **Custom components do not get touches.** A custom component (`Custom`,
  opcode `LAYOUT_CUSTOM`) gets its touches through `onTouchDown`,
  `onTouchDrag` and `onTouchUp`, which only the legacy path calls. `Custom` is
  not an `EventHandler`, so the router never calls it and
  `CustomContext.touchCustom` never runs. Custom component support has to be
  added to the unified path (b/585741367).
- **`requestDisallowInterceptTouchEvent` is never called** on the unified
  path, so `FEATURE_DISALLOW_INTERCEPT_TOUCH` has no effect. A scrolling
  parent view can take a gesture away (b/585818632).
- **A disabled document still gets touches.** The legacy `onTouchEvent`
  returns early when the view is disabled after a load error. The unified
  path does not check (b/585818520).
- **The click draws its own ripple, after the click.**
  `ClickModifierOperation2` and `MultiClickModifierOperation2` keep the
  ripple of the legacy classes: it starts when the click runs, not when the
  pointer goes down. With `rippleEffect()` and `onClick` on one component
  there are two ripples. This is kept on purpose for now (b/585818680).
- **The ripple keeps showing when a parent starts to scroll**, until the
  pointer leaves its bounds or goes up. Views and Compose stop the press
  effect. The ripple ignores consumption on purpose, and to stop here it
  would have to depend on the move being consumed. So we might not want to
  change this (b/585819865).

These are the same on the legacy path:

- **Hit testing ignores padding.** A handler uses the bounds of its component,
  so a tap in the padding of `padding(40).onClick` runs the click. This was
  confirmed with a test on both paths. The design doc asks for handlers to use
  their own bounds. We may decide to keep the current behavior (b/585819404).
- **Hit testing ignores `zIndex`.** It takes the last child in document order.
  Painting sorts the children by `zIndex` (b/585819711).
- **A touch expression has to be inside a component.** Outside of one it gets
  no events. No demo does this, so there is no bug for it.
- **`scrollByOffset` on a scroll container without a position variable**
  writes the override to float id 0 (b/585819411).
- **`ID_TOUCH_VEL_X` and `ID_TOUCH_VEL_Y` are never loaded** (b/585819258).
- **A component that becomes invisible during a press still gets the rest of
  the press.** Hit testing only checks visibility on `DOWN`, and the hit path
  stays the same until `UP` or `CANCEL`. So a component that is hidden after
  the `DOWN` still receives the `UP`, and its touch up and touch cancel actions
  run. This behavior is not defined and has no tests (b/588596793).

**Open follow-ups:**

| Bug | What |
| :--- | :--- |
| b/585249842 | Combine the gesture values on `RemoteContext` into one configuration class |
| b/585250060 | Reduce allocations in pointer input dispatch (a pool of events in `PointerInputEvent.obtain`) |
| b/585250534 | Check how Compose handles a press when the pointer moves outside the bounds |
| b/585716538 | Find a central place for all the event types |
| b/585741367 | Support custom components with unified pointer input |
| b/585746862 | Support embedded documents with unified pointer input |
| b/585747882 | Support multi-finger input with unified pointer input |
| b/585818632 | Call `requestDisallowInterceptTouchEvent` with unified pointer input |
| b/585818520 | Do not send touches to a disabled document |
| b/585818435 | Support click areas with unified pointer input |
| b/585818057 | Catch exceptions from pointer input handlers in the view player |
| b/585818200 | Call `View.performClick` and `performLongClick` with unified pointer input |
| b/585819404 | Hit testing ignores padding |
| b/585818680 | Start the click ripple on pointer down and avoid two ripples |
| b/585819865 | Stop the ripple when a parent starts to scroll |
| b/585819711 | Hit testing ignores `zIndex` |
| b/585819411 | `scrollByOffset` writes float id 0 for a scroll without a position variable |
| b/585819258 | Load `ID_TOUCH_VEL_X` and `ID_TOUCH_VEL_Y` |
| b/585817933 | Support several gestures on one component without stacking operations |
| b/585819117 | Write touch down, up and cancel as event handlers from the creation side |
| b/585818900 | Add a creation API like `Modifier.pointerInput` |
| b/585824714 | Add an event time to pointer input events |
| b/586559163 | Send accessibility clicks as events to `ClickModifierOperation2` |
| b/586817696 | Look up components by ID without boxing the ID |
| b/588596793 | Define what a component that becomes invisible during a press receives |

All of them are children of the tracking bug b/567170463.

## Differences from the design doc

The design doc (go/rc-pointerinput) was written before the code. Where they
differ, the code and this document are right.

| Topic | Design doc | Code |
| :--- | :--- | :--- |
| Passes | Leaves two or three passes open | Three passes, plus consumption |
| Pass values | Initial 0, Main 1, Final 2 | Initial 1, Main 2, Final 3. 0 means not dispatched yet |
| Subtype values | Down 1, Move 2, Up 3, Cancel 4, PointerDown 5, PointerUp 6 | Down 0, Up 1, Move 2, Cancel 3 |
| Bit 7 of the metadata | InBounds | Consumed |
| Data array | localX, localY, time in seconds, pointerId, windowX, windowY | pointerId, x, y, x velocity, y velocity. No time (b/585824714) |
| Event type | `EventManager.TYPE_POINTER` | `PointerInputRouter.EVENT_TYPE_POINTER` |
| How it is turned on | The same operations, a new document version, or a version field in each operation | A header flag that swaps the classes. The wire format does not change |

The metadata values and the data array are more than player internals. The
generic `onEvent` modifier matches the metadata exactly and copies the data by
index, so a document that uses it depends on them.

Proposals of the design doc that are not done:

- **Several gestures in one operation.** `combinedClickable` still writes three
  multi click operations that work together through consumption. How to
  support this is not decided: one operation with a bit per gesture, as the
  design doc proposes, or another way for operations to work together
  (b/585817933).
- **Touch down, touch up and touch cancel as event handlers.** The design doc
  proposes that the creation side writes them as generic event handlers. They
  are still operations of their own (b/585819117).
- **A creation API like `Modifier.pointerInput`** (b/585818900).
- **`setBounds` on `EventHandler`**, for hit testing that knows about padding
  (b/585819404).

## Removing the legacy path

When every document can use unified pointer input:

1. Make `Operations` always use the `*2` readers and delete
   `createMapWithNewPointerInputOperations`.
2. Delete the eight legacy classes and the legacy touch code:
   `RemoteComposeView.onTouchEvent`, `CoreDocument.touchDown` / `touchDrag` /
   `touchUp` / `touchCancel` / `onClick`, `TouchListener` and `TouchHandler`.
   Fix the known issues above first, because some of them are features that
   only the legacy path has today.
   - Keep the path for clicks without a pointer: `CoreDocument.performClick`,
     `Component.onClick` and `ComponentModifiers.onClick`. Accessibility
     clicks use it, until they are sent as events (b/586559163).
     `ClickHandler` can go once that path calls `performClick` on the `*2`
     classes directly.
3. Rename the `*2` classes and remove their `// CPD-OFF` and `// CPD-ON`
   comments.
4. Remove the duplicated `instanceof` checks listed in
   [Adding or changing a handler](#adding-or-changing-a-handler).
5. In the tests, remove the `unifiedPointerInput` parameter and the
   `assumeTrue` calls.
6. Undo the demo changes in `integration-tests/player-view-demos`. The two
   demo documents can stay. Only the switch between the two paths goes away:
   - `DslClickTouchDemo.kt` and `DslNestedInteractionDemo.kt`: remove the
     `unifiedPointerInput` parameter, `@JvmOverloads`, and the
     `RcHeaderTag.raw(Header.FEATURE_UNIFIED_POINTER_INPUT, ...)` header tag.
   - `ExperimentActivity.kt`: delete `getbPointerInput` and register the two
     demos with `getb` instead. Delete the "Unified Pointer Input" checkbox
     and its `unifiedPointerInput` state, and call `func.getDoc()` again.
   - `RemoteComposeFunc` (in `ExperimentActivity.kt`): delete
     `supportsUnifiedPointerInput()` and `getDoc(unifiedPointerInput)`.
   - `DemosCreation.java` needs no change.
7. Remove the flag: `Header.FEATURE_UNIFIED_POINTER_INPUT` (keep the number
   reserved), the check in `RemoteComposeView.dispatchTouchEvent`, the default
   in `CoreDocument` and the `unifiedPointerInput` JSON header tag.
