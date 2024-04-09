# Android accessibility implementation notes

_aelias@, February 2024_

This document summarizes decisions made during Compose accessibility implementation, primarily about
how Compose communicates with Android accessibility services at the system API level, and why.  From
the point of view of app developers, these are largely implementation details, but contributors to
Compose itself may need to know about these topics when investigating a11y bugs, optimizing
performance, or adding additional use cases for `semantics`.

Compose provides an a11y API defined in terms of `semantics`, but Compose itself is built on top of
an Android View System-oriented API defined in terms of `AccessibilityNodeInfo` and
`AccessibilityEvent`.  Every a11y-relevant property in Compose semantics is internally mapped in
some way to this underlying, preexisting API.  Because Compose apps can run back to API level 21,
during Compose's development we didn't try to change or add features to the system a11y API, but
instead adapted Compose's implementation to it.

Prior to reading this doc, I recommend reading the materials available on developer.android.com, not
just about a11y in Compose but also [a11y in the View
System](https://developer.android.com/reference/android/view/accessibility/package-summary).

This document is divided into three broad categories of topics: tree structure, scheduling, and
interaction of accessibility with testing.

## Tree structure

This section is about the semantics tree structure and the various ways it's transformed to
produce the `AccessibilityNodeInfo` tree.

### Merging

Merging is [covered in Compose's API
documentation](https://developer.android.com/jetpack/compose/semantics#merging-behavior) because
it's controllable using the `mergeDescendants` semantics property.  Here I will cover some notable
implementation details about it.

Merging is not actually a new concept invented by Compose.  It's a preexisting undocumented behavior
dating from the early days of Android accessibility.  TalkBack has its own merging algorithm which
activates implicitly when it sees one of several accessibility properties.  In View System apps, the
most common trigger for TalkBack to merge a subtree is the presence of a click action.  And just
like Compose, TalkBack's algorithm has the rule "children which are themselves merging are not
merged", which becomes intuitive if you imagine the key use case of a Button contained inside a
clickable Card (the button needs to remain focusable independently).

Somewhat counterintuitively, Compose actually converts the _unmerged_ semantics tree to
`AccessibilityNodeInfo`s.  `mergeDescendants` maps to an ordinary AccessibilityNodeInfo property that
activates merging on the a11y service side.  The key property Compose uses for this is called
[`isScreenReaderFocusable`](https://developer.android.com/reference/androidx/core/view/accessibility/AccessibilityNodeInfoCompat#isScreenReaderFocusable())
.  `isScreenReaderFocusable` triggers merging and has no other side effects.

* `isScreenReaderFocusable` is not to be confused with `isFocusable`, which reflects keyboard/D-Pad
focusability.  Keyboard-focusable nodes will always be screen-reader focusable, but not necessarily
the other way around.

* Historical note: in Compose we first wrote our own merging logic at the level of `SemanticsNode`,
but then we realized that if we created the `AccessibilityNodeInfo`s that way, we'd be hiding a lot
of potentially relevant substructural information from a11y services.  We'd also have wound up in a
hard-to-reason-about situation where two merging algorithms get applied one after the other, because
there exists no API to turn off the screenreader-side merging.  So Compose's own merging logic ended
up being used exclusively for unit testing.

### Collapsing

Distinct from merging and more fundamental, Compose's semantics implementation has a concept of
"collapsing" all the semantics modifiers chained on one `LayoutNode`.  Thanks to this, only one
`AccessibilityNodeInfo` will be created per LayoutNode, even if it has multiple semantics Modifiers.

* Collapsing is applied _right to left_ (or _bottom to top_) in code order.  So if several semantics
lambdas in a chain set the same property, the leftmost wins and the others are ignored.
    * The principle here is the left side of the Modifier chain is usually
      higher-level component with more context.  So if a composable from a library sets an undesirable
      semantics property, the app can replace or remove that property by passing a semantics
      Modifier into its Modifier parameter.
* Collapsing raises a question around the bounds of the `AccessibilityNodeInfo` because
  different parts of the Modifier chain can have different positions and sizes.  The policy on this is:
    * If there is one or more `mergeDescendants` semantics block, then the bounds is the leftmost of those.
        * `mergeDescendants` usually indicates a key interactable element such as a Button, so we
          empirically observed that selecting it led to the most sensible bounds.
    * Otherwise, the bounds is the leftmost of all semantics Modifiers.

### isImportantForAccessibility

AccessibilityNodeInfo has a property called `isImportantForAccessibility`.  For the most part,
TalkBack will ignore nodes with this property set to `false`, so it should be set to true if a node
is intended to have an effect on screenreader behavior.

The policy as of Compose 1.6 is that semantics properties are categorized either as
`AccessibilityKey` (most properties) or plain `SemanticsPropertyKey` (`testTag`, `isTraversalGroup`,
and a few rarer properties).  If there exists at least one `AccessibilityKey` on a collapsed semantics node, then
`isImportantForAccessibility = true`.

In View System, this property was `false` by default.  The original idea was it that it should be
`false` for structural Views with little relevance to accessibility like `LinearLayout`, but
`true` for any node that's relevant to screenreader behavior.  In deep View hierarchies, most Views
tend to be marked unimportant.

In contrast, this property is usually `true` in Compose, because most non-accessibility-related
elements like `Column` don't have `semantics`, and therefore no AccessibilityNodeInfo is generated
for them in the first place.  However, Compose does have a smaller subset of nodes that have
semantics but are unimportant to screenreaders, particularly ones that only have semantics relevant
to testing (like `testTag`) and custom developer-defined semantics.

### Pruning

Pruning is an automatic behavior to remove nodes that are automatically detected as invisible.  This
prevents screenreaders from focusing on elements that can't be seen.

There are two main causes of pruning:
1. When a node is invisible due to graphics `alpha = 0f`.
    * This is the only place a graphics property is taken into account in Compose accessibility.  It
      covers for an app-side pattern where an element is marked `alpha = 0f` instead of
      actually removed from the scene.
2. Occlusion: when every pixel of the node bounds is covered by visible nodes with important
semantics that are above it in placement order or z-order.

Both styles of pruning are analogous to a preexisting behavior in View System.

### Fake nodes

Fake nodes are a workaround for some TalkBack behaviors that we wanted to avoid in the context of
Compose.  They are only created for the specific semantics properties `contentDescription` and
`role`, which are normal properties when they're placed on leaf `AccessibilityNodeInfo`, but have
more problematic behaviors when they're placed on an `AccessibilityNodeInfo` that has children.

When one of these two properties is present on a non-leaf SemanticsNode, the Compose a11y
implementation conjures up a new, "fake" rightmost AccessibilityNodeInfo child which was not present
in the original semantics tree, and "moves" the property into the fake node.  Fake nodes don't
correspond to any `LayoutNode` (which is where semantics `id`s are normally generated and held), so
the `id` of the fake node is the `id` of the original SemanticsNode plus a very large constant.

The following explains the distinct motivations for doing this for `contentDescription` and `role`
respectively:

#### contentDescription

In the View System API, `contentDescription` is not only a way of sprinkling in some explanatory
text, but is also a powerful tool which overrides most other screenreader text.  It can thus be used
to replace the entire speech of a complex widget with multiple children.

In Compose's accessibility API, we put the overriding capability into a distinct API
`clearAndSetSemantics` which is harder to use by accident.  We wanted `contentDescription` to be
simply additive like the other string properties.  The way we achieved that is to move it into a
fake node, so that the a11y service sees it as a leaf with nothing to override.

#### Role

Without fake nodes, roles were observed to be spoken in the wrong order when placed on a non-leaf
node.  In View System apps, nodes with both roles and children were very rare, whereas in Compose
they are very common (the classic example is `ButtonView` vs `Button { Text("...") }`).  By spinning
off roles into a fake rightmost child, they are spoken after the contents of the node, as expected.

### AndroidView children

A Compose scene can contain Android View children using the `AndroidView` composable.  In terms of
accessibility, the main interoperability challenge is that the AndroidView's parent is a Compose
leaf `AccessibilityNodeInfo`.  But the `View` class itself only provides an API for specifying a
parent which is another `View`, not a virtual `AccessibilityNodeInfo`.  And if we used that API to
tell screenreaders its parent is the `AndroidComposeView`, the screenreader would get the wrong idea
about its location relative to the rest of the scene (it would perceive it as a sibling to the root
Compose node).

Thus, in order to communicate to screenreaders the detailed location of the AndroidView in the hierarchy, we
[create a special small `AccessibilityDelegate`](https://cs.android.com/androidx/platform/frameworks/support/+/androidx-main:compose/ui/ui/src/androidMain/kotlin/androidx/compose/ui/platform/AndroidComposeView.android.kt;l=989;drc=7234c0fd1e47e7be05ef3235b8d8aa826318433d)
on each Compose `AndroidViewHolder`.  This delegate creates one small `AccessibilityNodeInfo` with
the `parent` and `traversalBefore` properties set to the appropriate Compose semantics id.  It's
only about 50 lines and contained within one function, as compared to the main Compose
AccessibilityDelegate which runs into the thousands of lines: that's because it can rely on the
accessibility properties of the developer-provided View children for everything else.

Unfortunately, we had to use a few workarounds to use a delegate like this:
* The View System will ignore delegates unless `isImportantForAccessibility = true` on the View
containing the delegate, so we had to set it to true.  The problem is that the `AndroidViewHolder`
is precisely the kind of purely structural node that normally ought to set this property to `false`.
As a result of setting this property, TalkBack treated it as so important that it prioritized
focusing it and made it impossible to focus a child `TextView`.
* To workaround that issue, we [set `isVisibleToUser = false`](https://android-review.googlesource.com/c/platform/frameworks/support/+/2735015),
which empirically prevents TalkBack from ever focusing the `AndroidViewHolder`.

## Scheduling events

Accessibility services maintain a model of the AccessibilityNodeInfo currently on the screen within
their process.  So there is a need to keep a11y services informed of changes to the scene over time
using `AccessibilityEvent`s.

`AccessibilityEvent`s have a high CPU cost.  Although the event data structure itself is a small bundle containing a few
[change type enums](https://developer.android.com/reference/android/view/accessibility/AccessibilityEvent#constants_1) and the `id` of the node that changed, the a11y service will typically use them as
a trigger to mark its model dirty and query the app about every node in the subtree of that
`id`.  So in the aftermath of an event you will see a large amount of IPC traffic between the a11y
service process and the app process.

The complexity in Compose's handling of accessibility events is in order to minimize this cost.  There are 3 categories of changes with different processing:

### Semantics property changes: diffing (minimally batched)

When a `semantics` block re-runs, we [post a `semanticsChangeChecker`
task](https://cs.android.com/androidx/platform/frameworks/support/+/androidx-main:compose/ui/ui/src/androidMain/kotlin/androidx/compose/ui/platform/AndroidComposeViewAccessibilityDelegateCompat.android.kt;l=2199;drc=f8f18ee800fcc26361813931f509da19d5abcc86)
on the UI message loop (if one is not already posted).  This task compares the old and new semantics
data structures to see if anything actually changed before sending `AccessibilityEvent`s.

The reason to post a task instead of simply running synchronously is that it provides a minimal
amount of batching.  When an composition happens it's typical that a subtree of `semantics {}`
blocks all need to run in close succession.  By waiting until control returns to the message loop,
we only run the diffing after composition has completed.

### Bounds changes: 100ms throttling

Bounds changes (changes to the x/y/width/height of the semantics nodes) also need to trigger
accessibility events, since many screenreader behaviors depend on positions and sizes.  Such changes
can happen due to layout, animation or scrolling, and not necessarily involve the `semantics {}`
lambdas getting run.  This category of changes is heavily batched and throttled.

When a bounds change occurs, Compose [wakes up a coroutine that runs on 100ms
intervals](https://cs.android.com/androidx/platform/frameworks/support/+/androidx-main:compose/ui/ui/src/androidMain/kotlin/androidx/compose/ui/platform/AndroidComposeViewAccessibilityDelegateCompat.android.kt;l=2208;drc=f8f18ee800fcc26361813931f509da19d5abcc86).
The first bounds change causes events to be sent immediately.  But any follow-up bounds changes
will only have an effect after 100ms has elapsed.  This continues until a full 100ms passes
with zero bounds changes observed, at which point the coroutine hibernates again.

The motivation here is that scrolling and animated navigation typically involve a rapid series of
positional changes before the scene stabilizes.  If we sent these at 60/120fps, there would be a
tremendous amount of event spam, overwhelming the a11y service.  Throttling to 100ms intervals
allows us to batch much of this work.

The 100ms number is copied from a [preexisting number in View
System](https://cs.android.com/android/platform/superproject/main/+/main:frameworks/base/core/java/android/view/View.java;l=8600;drc=8aa453263f85d469ef056680b01ea90ddf300786)
which has a similar function.  Because this 100ms delay has a long history on Android, we can
assume that a11y services have evolved their behavior and performance around it.

### Scrolling changes: fast path to update green focus rect

As mentioned in the above section, in terms of events, scrolling is handled similarly to layout
(there can be a 100ms delay between updates).  As far as screenreader-side behavior is concerned,
this is fast enough, but the biggest problem is that we ideally want the green a11y focus rectangle
to scroll precisely at the same rate as the rest of the screen during two-finger scrolling.  If it
lagged behind at 100ms intervals (or even smaller intervals), that would appear visibly glitchy and
distracting.

Some background context is that the green focus rectangle is [actually drawn by `ViewRootImpl` in
the app
process](https://cs.android.com/android/platform/superproject/main/+/main:frameworks/base/core/java/android/view/ViewRootImpl.java;l=4954;drc=f5dff995c4939ffe9e41d709cca3b198435a55de),
not the a11y service.  Thus, although Android Views have even more aggressive 100ms batching in
terms of a11y events than Compose does, they manage to avoid the "laggy focus rect" problem with a
[fast path in
ViewRootImpl](https://cs.android.com/android/platform/superproject/main/+/main:frameworks/base/core/java/android/view/ViewRootImpl.java;l=4967;drc=f5dff995c4939ffe9e41d709cca3b198435a55de)
where it calls `getBoundsOnScreen` on the focused View at draw time.

Unfortunately, ViewRootImpl didn't intentionally add an analogous fast path API for
AccessibilityNodeInfo.  Fortunately, it still proved to be possible for Compose to create its own
fast path by combining several tricks:

1. The `ScrollAxisRange` [values are held in a lambda](https://cs.android.com/androidx/platform/frameworks/support/+/androidx-main:compose/ui/ui/src/commonMain/kotlin/androidx/compose/ui/semantics/SemanticsProperties.kt;l=697;drc=53478385c1311bec3d3bdf86c8c3162ba667081c) so only that lambda needs to rerun when
the scroll offset changes, instead of the entire semantics block.

2. A ScrollObservationScope is registered to [trigger a11y-side scrolling
logic](https://cs.android.com/androidx/platform/frameworks/support/+/androidx-main:compose/ui/ui/src/androidMain/kotlin/androidx/compose/ui/platform/AndroidComposeViewAccessibilityDelegateCompat.android.kt;l=2764;drc=f8f18ee800fcc26361813931f509da19d5abcc86)
when it changes (an additional form of task scheduling which bypasses the rest of the a11y system
after initial setup).

3. Compose [keeps a
reference](https://cs.android.com/androidx/platform/frameworks/support/+/androidx-main:compose/ui/ui/src/androidMain/kotlin/androidx/compose/ui/platform/AndroidComposeViewAccessibilityDelegateCompat.android.kt;l=3118;drc=f8f18ee800fcc26361813931f509da19d5abcc86)
to the AccessibilityNodeInfo which ViewRootImpl uses as the canonical source of information for the
green rect position.  When a scroll happens, Compose causes the green rect to move by [mutating
the bounds](https://cs.android.com/androidx/platform/frameworks/support/+/androidx-main:compose/ui/ui/src/androidMain/kotlin/androidx/compose/ui/platform/AndroidComposeViewAccessibilityDelegateCompat.android.kt;l=2793;drc=f8f18ee800fcc26361813931f509da19d5abcc86)
on that AccessibilityNodeInfo and triggering a View invalidation, both cheap actions relative to
sending an a11y event.

## Interaction of accessibility and testing

Compose `semantics` API was designed to support testing and accessibility use cases at the same
time.  Not only does this lead to natural code sharing as they both involve programmatic inspection
of the UI, it means that a Compose app's unit tests naturally test it for a baseline level of
accessibility support.

### Compose unit tests

Here is a brief summary of how the tree structure transformations described above apply or don't
apply in the unit test context:

#### Merging
Tests inspect the merged tree by default, in order to test the scene that's relevant to
accessibility, and because it avoids the need for awkward & brittle parent/child traversal code (for
example, on the merged tree, you can find a `Button` by the text inside it, then trigger its click
action, because they are the same node).

To inspect the unmerged tree instead, most test matchers provide an optional parameter
`useUnmergedTree`.  When this flag is true, it shows all nodes that were present in the original
semantics tree, including even the ones replaced by `clearAndSetSemantics`.

As mentioned earlier, Compose's a11y implemention passes the unmerged semantics tree to the
accessibility service and relies on the service-process-side merging algorithm.  The Compose merged
semantics tree can be thought of as a local simulation of the service's merging algorithm.

#### Collapsing
Collapsing transformations fully apply to unit tests as well.  For example, when a test takes a
screenshot of one Compose node, the screenshot's size is the size of its semantic bounds, chosen
according to the policy described above.

#### Pruning
Compose unit tests aren't affected by pruning and can match all nodes that were composed, even if
they are hidden behind another node.  But note that visibility-related assertions may still be
affected by the graphics `alpha = 0f` visibility policy, for example if asserting too early for an
element with a fade-in animation.

#### Fake nodes
Unit tests do not see any fake nodes even when `useUnmergedTree = true`.  This is to avoid confusion
and maintain flexibility to change them in the future.

### UIAutomator

UIAutomator is a Android system instrumentation tool to perform actions like clicking and
scrolling via a PC USB connection.  Originally intended for testing and automation in general, it
has today mostly been superceded by other tools, except for one key use case: performance
benchmarking.  It is uniquely well-suited to performance benchmarking because it works even on very
old builds of Android and requires no debug modes or special privileges.  Thus, it can be used to
benchmark the final release build of the app on an ordinary phone.

What lets UIAutomator work on every app is that it makes use of the accessibility data structures.
It's not exactly an a11y service, as it doesn't appear in the list of services if you query
AccessibilityManager for them.  But it's vaguely analogous to a screenreader in the sense that it
examines the contents of the screen by querying the app for AccessibilityNodeInfo, and triggers
clicks and scrolls via a11y actions.  Thus Compose's accessibility code "automatically" supports
UIAutomator as well, but in practice some adjustments are needed.

Compose [detects that UIAutomator is
the one that enabled
accessibility](https://cs.android.com/androidx/platform/frameworks/support/+/androidx-main:compose/ui/ui/src/androidMain/kotlin/androidx/compose/ui/platform/AndroidComposeViewAccessibilityDelegateCompat.android.kt;l=253)
by observing that system accessibility is currently enabled, yet the AccessibilityManager service
list is empty.  In this mode, Compose will provide AccessibilityNodeInfos when queried as usual, but
it will refrain from sending any AccessibilityEvents events as the scene changes.  The reason is
that sending such events has a performance cost, which would introduce noise in UIAutomator-based
benchmarks.  Furthermore, UIAutomator mostly relies on polling instead of relying on such events anyway.

Also, one Compose semantic property was introduced primarily for use by UIAutomator-based
benchmarks:
[`testTagsAsResourceId`](https://cs.android.com/androidx/platform/frameworks/support/+/androidx-main:compose/ui/ui/src/androidMain/kotlin/androidx/compose/ui/semantics/SemanticsProperties.android.kt;l=37;drc=dc23dcdc4276814e69cc00639c617eac494c7ead).
Although `testTag`s are always provided in AccessibilityNodeInfo `extras`, UIAutomator was written
before `extras` existed so it unfortunately some versions of it have no matcher for this type of
AccessibilityNodeInfo property.  One matcher it does have is for Android resource IDs, and indeed
this is the main one used in UIAutomator-based View System app benchmarks.  So we added a feature to
map one type of test-matching tag to the other.

- Note 1: `testTag`'s use in UIAutomator is also why `testTag`-only nodes aren't fully pruned from
the AccessibilityNodeInfo tree, but marked unimportant instead.

- Note 2: if you are interested in reading the source code of UIAutomator, there are many old
versions of it floating around so be careful you are looking at the right one.  The current version
as of early 2024 is [in the android-support-test branch here](https://android.googlesource.com/platform/frameworks/uiautomator/+/android-support-test/src/main/java/android/support/test/uiautomator/UiObject2.java).
