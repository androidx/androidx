package androidx.ui.widgets

import androidx.ui.assert
import androidx.ui.foundation.Key
import androidx.ui.foundation.assertions.FlutterError
import androidx.ui.widgets.framework.Widget

// / Log the dirty widgets that are built each frame.
// /
// / Combined with [debugPrintBuildScope] or [debugPrintBeginFrameBanner], this
// / allows you to distinguish builds triggered by the initial mounting of a
// / widget tree (e.g. in a call to [runApp]) from the regular builds triggered
// / by the pipeline.
// /
// / Combined with [debugPrintScheduleBuildForStacks], this lets you watch a
// / widget's dirty/clean lifecycle.
// /
// / To get similar information but showing it on the timeline available from the
// / Observatory rather than getting it in the console (where it can be
// / overwhelming), consider [debugProfileBuildsEnabled].
// /
// / See also the discussion at [WidgetsBinding.drawFrame].
var debugPrintRebuildDirtyWidgets: Boolean = false

// / Log all calls to [BuildOwner.buildScope].
// /
// / Combined with [debugPrintScheduleBuildForStacks], this allows you to track
// / when a [State.setState] call gets serviced.
// /
// / Combined with [debugPrintRebuildDirtyWidgets] or
// / [debugPrintBeginFrameBanner], this allows you to distinguish builds
// / triggered by the initial mounting of a widget tree (e.g. in a call to
// / [runApp]) from the regular builds triggered by the pipeline.
// /
// / See also the discussion at [WidgetsBinding.drawFrame].
var debugPrintBuildScope: Boolean = false

// / Log the call stacks that mark widgets as needing to be rebuilt.
// /
// / This is called whenever [BuildOwner.scheduleBuildFor] adds an element to the
// / dirty list. Typically this is as a result of [Element.markNeedsBuild] being
// / called, which itself is usually a result of [State.setState] being called.
// /
// / To see when a widget is rebuilt, see [debugPrintRebuildDirtyWidgets].
// /
// / To see when the dirty list is flushed, see [debugPrintBuildScope].
// /
// / To see when a frame is scheduled, see [debugPrintScheduleFrameStacks].
var debugPrintScheduleBuildForStacks: Boolean = false

// / Log when widgets with global keys are deactivated and log when they are
// / reactivated (retaken).
// /
// / This can help track down framework bugs relating to the [GlobalKey] logic.
var debugPrintGlobalKeyedWidgetLifecycle: Boolean = false

// / Adds [Timeline] events for every Widget built.
// /
// / For details on how to use [Timeline] events in the Dart Observatory to
// / optimize your app, see https://fuchsia.googlesource.com/sysui/+/master/docs/performance.md
// /
// / See also [debugProfilePaintsEnabled], which does something similar but for
// / painting, and [debugPrintRebuildDirtyWidgets], which does something similar
// / but reporting the builds to the console.
var debugProfileBuildsEnabled: Boolean = false

// / Show banners for deprecated widgets.
var debugHighlightDeprecatedWidgets: Boolean = false

fun _firstNonUniqueKey(widgets: Iterable<Widget>): Key? {
    val keySet = mutableSetOf<Key>()
    for (widget in widgets) {
        assert(widget != null)
        if (widget.key == null)
            continue
        if (!keySet.add(widget.key))
            return widget.key
    }
    return null
}

// / Asserts if the given child list contains any duplicate non-null keys.
// /
// / To invoke this function, use the following pattern, typically in the
// / relevant Widget's constructor:
// /
// / ```dart
// / assert(!debugChildrenHaveDuplicateKeys(this, children));
// / ```
// /
// / For a version of this function that can be used in contexts where
// / the list of items does not have a particular parent, see
// / [debugItemsHaveDuplicateKeys].
// /
// / Does nothing if asserts are disabled. Always returns true.
fun debugChildrenHaveDuplicateKeys(parent: Widget, children: Iterable<Widget>): Boolean {
    assert {
        val nonUniqueKey = _firstNonUniqueKey(children)
        if (nonUniqueKey != null) {
            throw FlutterError(
                    "Duplicate keys found.\n" +
                    "If multiple keyed nodes exist as children of another node, they must have unique keys.\n" +
                    "$parent has multiple children with key $nonUniqueKey."
            )
        }
        true
    }
    return false
}