package androidx.ui.widgets.framework

import androidx.ui.VoidCallback
import androidx.ui.assert
import androidx.ui.foundation.assertions.FlutterError
import androidx.ui.foundation.assertions.debugPrintStack
import androidx.ui.foundation.debugPrint
import androidx.ui.widgets.debugPrintBuildScope
import androidx.ui.widgets.debugPrintScheduleBuildForStacks
import androidx.ui.widgets.focusmanager.FocusManager
import androidx.ui.widgets.framework.key.GlobalKey

// / Manager class for the widgets framework.
// /
// / This class tracks which widgets need rebuilding, and handles other tasks
// / that apply to widget trees as a whole, such as managing the inactive element
// / list for the tree and triggering the "reassemble" command when necessary
// / during hot reload when debugging.
// /
// / The main build owner is typically owned by the [WidgetsBinding], and is
// / driven from the operating system along with the rest of the
// / build/layout/paint pipeline.
// /
// / Additional build owners can be built to manage off-screen widget trees.
// /
// / To assign a build owner to a tree, use the
// / [RootRenderObjectElement.assignOwner] method on the root element of the
// / widget tree.
class BuildOwner(
        // / Called on each build pass when the first buildable element is marked
        // / dirty.
    var onBuildScheduled: VoidCallback = {}
) {

    internal val _inactiveElements: _InactiveElements = _InactiveElements()

    internal val _dirtyElements = mutableListOf<Element>()
    private var _scheduledFlushDirtyElements = false
    private var _dirtyElementsNeedsResorting: Boolean? = false; // null if we're not in a buildScope

//    /// The object in charge of the focus tree.
//    ///
//    /// Rarely used directly. Instead, consider using [FocusScope.of] to obtain
//    /// the [FocusScopeNode] for a given [BuildContext].
//    ///
//    /// See [FocusManager] for more details.
    val focusManager = FocusManager()
//
    // / Adds an element to the dirty elements list so that it will be rebuilt
    // / when [WidgetsBinding.drawFrame] calls [buildScope].
    fun scheduleBuildFor(element: Element) {
        assert(element != null)
        assert(element.owner == this)
        assert {
            if (debugPrintScheduleBuildForStacks)
                debugPrintStack(label = "scheduleBuildFor() called for" +
                        "$element" +
                        "${if (_dirtyElements.contains(element)) " (ALREADY IN LIST)" else ""}")
            if (!element.dirty) {
                throw FlutterError(
                        "scheduleBuildFor() called for a widget that is not marked as dirty.\n" +
                        "The method was called for the following element:\n" +
                        "  $element\n" +
                        "This element is not current marked as dirty. Make sure to set the dirty" +
                                "flag before " +
                        "calling scheduleBuildFor().\n" +
                        "If you did not attempt to call scheduleBuildFor() yourself, then this" +
                                "probably " +
                        "indicates a bug in the widgets framework. Please report it: " +
                        "https://github.com/flutter/flutter/issues/new"
                )
            }
            true
        }
        if (element._inDirtyList) {
            assert {
                if (debugPrintScheduleBuildForStacks)
                    debugPrintStack(label = "markNeedsToResortDirtyElements() called;" +
                            "_dirtyElementsNeedsResorting was" +
                            "$_dirtyElementsNeedsResorting (now true);" +
                            "dirty list is: $_dirtyElements")
                if (_dirtyElementsNeedsResorting == null) {
                    throw FlutterError(
                            "markNeedsToResortDirtyElements() called inappropriately.\n" +
                            "The markNeedsToResortDirtyElements() method should only be called" +
                            "while the buildScope() method is actively rebuilding the widget tree."
                    )
                }
                true
            }
            _dirtyElementsNeedsResorting = true
            return
        }
        if (!_scheduledFlushDirtyElements && onBuildScheduled != null) {
            _scheduledFlushDirtyElements = true
            onBuildScheduled()
        }
        _dirtyElements.add(element)
        element._inDirtyList = true
        assert {
            if (debugPrintScheduleBuildForStacks)
                debugPrint("...dirty list is now: $_dirtyElements")
            true
        }
    }

    internal var _debugStateLockLevel = 0
    val _debugStateLocked: Boolean get() = _debugStateLockLevel > 0

    // / Whether this widget tree is in the build phase.
    // /
    // / Only valid when asserts are enabled.
    var debugBuilding: Boolean = false
        private set

    internal var _debugCurrentBuildTarget: Element? = null

    // / Establishes a scope in which calls to [State.setState] are forbidden, and
    // / calls the given `callback`.
    // /
    // / This mechanism is used to ensure that, for instance, [State.dispose] does
    // / not call [State.setState].
    fun lockState(callback: () -> Unit) {
        assert(callback != null)
        assert(_debugStateLockLevel >= 0)
        assert {
            _debugStateLockLevel += 1
            true
        }
        try {
            callback()
        } finally {
            assert {
                _debugStateLockLevel -= 1
                true
            }
        }
        assert(_debugStateLockLevel >= 0)
    }

    // / Establishes a scope for updating the widget tree, and calls the given
    // / `callback`, if any. Then, builds all the elements that were marked as
    // / dirty using [scheduleBuildFor], in depth order.
    // /
    // / This mechanism prevents build methods from transitively requiring other
    // / build methods to run, potentially causing infinite loops.
    // /
    // / The dirty list is processed after `callback` returns, building all the
    // / elements that were marked as dirty using [scheduleBuildFor], in depth
    // / order. If elements are marked as dirty while this method is running, they
    // / must be deeper than the `context` node, and deeper than any
    // / previously-built node in this pass.
    // /
    // / To flush the current dirty list without performing any other work, this
    // / function can be called with no callback. This is what the framework does
    // / each frame, in [WidgetsBinding.drawFrame].
    // /
    // / Only one [buildScope] can be active at a time.
    // /
    // / A [buildScope] implies a [lockState] scope as well.
    // /
    // / To print a console message every time this method is called, set
    // / [debugPrintBuildScope] to true. This is useful when debugging problems
    // / involving widgets not getting marked dirty, or getting marked dirty too
    // / often.
    fun buildScope(context: Element, callback: VoidCallback?) {
        if (callback == null && _dirtyElements.isEmpty())
            return
        assert(context != null)
        assert(_debugStateLockLevel >= 0)
        assert(!debugBuilding)
        assert {
            if (debugPrintBuildScope)
                debugPrint("buildScope called with context $context; " +
                        "dirty list is: $_dirtyElements")
            _debugStateLockLevel += 1
            debugBuilding = true
            true
        }
        // TODO(Migration/Filip): Not need now
        // Timeline.startSync('Build', arguments: timelineWhitelistArguments);
        try {
            _scheduledFlushDirtyElements = true
            if (callback != null) {
                assert(_debugStateLocked)
                var debugPreviousBuildTarget: Element? = null
                assert {
                    context._debugSetAllowIgnoredCallsToMarkNeedsBuild(true)
                    debugPreviousBuildTarget = _debugCurrentBuildTarget
                    _debugCurrentBuildTarget = context
                    true
                }
                _dirtyElementsNeedsResorting = false
                try {
                    callback()
                } finally {
                    assert {
                        context._debugSetAllowIgnoredCallsToMarkNeedsBuild(false)
                        assert(_debugCurrentBuildTarget == context)
                        _debugCurrentBuildTarget = debugPreviousBuildTarget
                        _debugElementWasRebuilt(context)
                        true
                    }
                }
            }

            _dirtyElements.sortWith(Element.ElementComparator())
            _dirtyElementsNeedsResorting = false
            var dirtyCount = _dirtyElements.size
            var index = 0
            while (index < dirtyCount) {
                assert(_dirtyElements[index] != null)
                assert(_dirtyElements[index]._inDirtyList)
                assert(!_dirtyElements[index]._active ||
                        _dirtyElements[index]._debugIsInScope(context))
                try {
                    _dirtyElements[index].rebuild()
                } catch (e: Exception) {
                    _debugReportException(
                            "while rebuilding dirty elements", e, e.stackTrace,
                            informationCollector = {
                                information: StringBuffer ->
                                    information.appendln("The element being rebuilt at the" +
                                            "time was index $index of $dirtyCount:")
                                    information.append("  ${_dirtyElements[index]}")
                            }
                    )
                }
                index += 1
                if (dirtyCount < _dirtyElements.size || _dirtyElementsNeedsResorting!!) {
                    _dirtyElements.sortWith(Element.ElementComparator())
                    _dirtyElementsNeedsResorting = false
                    dirtyCount = _dirtyElements.size
                    while (index > 0 && _dirtyElements[index - 1].dirty) {
                        // It is possible for previously dirty but inactive widgets to move right in the list.
                        // We therefore have to move the index left in the list to account for this.
                        // We don't know how many could have moved. However, we do know that the only possible
                        // change to the list is that nodes that were previously to the left of the index have
                        // now moved to be to the right of the right-most cleaned node, and we do know that
                        // all the clean nodes were to the left of the index. So we move the index left
                        // until just after the right-most clean node.
                        index -= 1
                    }
                }
            }
            assert {
                if (_dirtyElements.any { it._active && it.dirty }) {
                throw FlutterError(
                        "buildScope missed some dirty elements.\n" +
                        "This probably indicates that the dirty list should have been resorted" +
                                "but was not.\n" +
                        "The list of dirty elements at the end of the buildScope call was:\n" +
                        "  $_dirtyElements"
                )
            }
                true
            }
        } finally {
            for (element in _dirtyElements) {
                assert(element._inDirtyList)
                element._inDirtyList = false
            }
            _dirtyElements.clear()
            _scheduledFlushDirtyElements = false
            _dirtyElementsNeedsResorting = null
            // TODO(Migration/Filip): Not need now
            // Timeline.finishSync();
            assert(debugBuilding)
            assert {
                debugBuilding = false
                _debugStateLockLevel -= 1
                if (debugPrintBuildScope)
                    debugPrint("buildScope finished")
                true
            }
        }
        assert(_debugStateLockLevel >= 0)
    }

    var _debugElementsToBeRebuiltDueToGlobalKeyShenanigans:
            MutableMap<Element, MutableSet<GlobalKey<*>>>? = null

    fun _debugTrackElementThatWillNeedToBeRebuiltDueToGlobalKeyShenanigans(
        node: Element,
        key: GlobalKey<*>
    ) {
        _debugElementsToBeRebuiltDueToGlobalKeyShenanigans =
                _debugElementsToBeRebuiltDueToGlobalKeyShenanigans ?: mutableMapOf()
        val keys = _debugElementsToBeRebuiltDueToGlobalKeyShenanigans!!
                .getOrPut(node, { mutableSetOf() })
        keys.add(key)
    }

    fun _debugElementWasRebuilt(node: Element) {
        _debugElementsToBeRebuiltDueToGlobalKeyShenanigans?.remove(node)
    }

    // / Complete the element build pass by unmounting any elements that are no
    // / longer active.
    // /
    // / This is called by [WidgetsBinding.drawFrame].
    // /
    // / In debug mode, this also runs some sanity checks, for example checking for
    // / duplicate global keys.
    // /
    // / After the current call stack unwinds, a microtask that notifies listeners
    // / about changes to global keys will run.
    fun finalizeTree() {
        // TODO(Migration/Filip): Not need now
        // Timeline.startSync('Finalize tree', arguments: timelineWhitelistArguments);
        try {
            lockState {
                _inactiveElements._unmountAll(); // this unregisters the GlobalKeys
            }
            assert {
                try {
                    GlobalKey._debugVerifyIllFatedPopulation()
                    if (_debugElementsToBeRebuiltDueToGlobalKeyShenanigans != null &&
                            _debugElementsToBeRebuiltDueToGlobalKeyShenanigans!!
                                    .isNotEmpty()) {
                        val keys = mutableSetOf<GlobalKey<*>>()
                        for (element in
                        _debugElementsToBeRebuiltDueToGlobalKeyShenanigans!!.keys) {
                            if (element._debugLifecycleState != _ElementLifecycle.defunct)
                                keys.addAll(_debugElementsToBeRebuiltDueToGlobalKeyShenanigans
                                    !![element]!!.asIterable())
                        }
                        if (keys.isNotEmpty()) {
                            val keyStringCount = mutableMapOf<String, Int>()
                            for (key in keys.map { it.toString() }) {
                                if (keyStringCount.containsKey(key)) {
                                    keyStringCount[key] = keyStringCount[key]!! + 1
                                } else {
                                    keyStringCount[key] = 1
                                }
                            }
                            val keyLabels = mutableListOf<String>()
                            keyStringCount.forEach { (key: String, count: Int) ->
                                if (count == 1) {
                                    keyLabels.add(key)
                                } else {
                                    keyLabels.add("$key ($count different affected keys had" +
                                            "this toString representation)")
                                }
                            }

                            val elements = _debugElementsToBeRebuiltDueToGlobalKeyShenanigans!!.keys
                            val elementStringCount = mutableMapOf<String, Int>()
                            for (element in elements.map { it.toString() }) {
                                if (elementStringCount.containsKey(element)) {
                                    elementStringCount[element] = elementStringCount[element]!! + 1
                                } else {
                                    elementStringCount[element] = 1
                                }
                            }
                            val elementLabels = mutableListOf<String>()
                            elementStringCount.forEach { (element: String, count: Int) ->
                                if (count == 1) {
                                    elementLabels.add(element)
                                } else {
                                    elementLabels.add("$element ($count different affected" +
                                            "elements had this toString representation)")
                                }
                            }

                            assert(keyLabels.isNotEmpty())
                            val the = if (keys.size == 1) " the" else ""
                            val s = if (keys.size == 1) "" else "s"
                            val were = if (keys.size == 1) "was" else "were"
                            val their = if (keys.size == 1) "its" else "their"
                            val respective = if (elementLabels.size == 1) "" else " respective"
                            val those = if (keys.size == 1) "that" else "those"
                            val s2 = if (elementLabels.size == 1) "" else "s"
                            val those2 = if (elementLabels.size == 1) "that" else "those"
                            val they = if (elementLabels.size == 1) "it" else "they"
                            val think = if (elementLabels.size == 1) "thinks" else "think"
                            val are = if (elementLabels.size == 1) "is" else "are"
                            throw FlutterError(
                                    "Duplicate GlobalKey$s detected in widget tree.\n" +
                                    "The following GlobalKey$s $were specified multiple times in" +
                                            "the widget tree. This will lead to " +
                                    "parts of the widget tree being truncated unexpectedly," +
                                            "because the second time a key is seen, " +
                                    "the previous instance is moved to the new location. The" +
                                            "key$s $were:\n" +
                                    "- ${keyLabels.joinToString(separator = "\n  ")}\n" +
                                    "This was determined by noticing that after$the widget$s with" +
                                            "the above global key$s $were moved " +
                                    "out of $their$respective previous parent$s2, " +
                                            "$those2 previous" +
                                            "parent$s2 never updated during this frame, meaning " +
                                    "that $they either did not update at all or updated before" +
                                            "the widget$s $were moved, in either case " +
                                    "implying that $they still $think that $they should have a" +
                                            "child with $those global key$s.\n" +
                                    "The specific parent$s2 that did not update after having one" +
                                            "or more children forcibly removed " +
                                    "due to GlobalKey reparenting $are:\n" +
                                    "- ${elementLabels.joinToString(separator = "\n  ")}\n" +
                                    "A GlobalKey can only be specified on one widget at a time" +
                                            "in the widget tree."
                            )
                        }
                    }
                } finally {
                    _debugElementsToBeRebuiltDueToGlobalKeyShenanigans?.clear()
                }
                true
            }
        } catch (e: Throwable) {
            _debugReportException("while finalizing the widget tree", e, e.stackTrace)
        } finally {
            // TODO(Migration/Filip): Not need now
            // Timeline.finishSync();
        }
    }

    // / Cause the entire subtree rooted at the given [Element] to be entirely
    // / rebuilt. This is used by development tools when the application code has
    // / changed and is being hot-reloaded, to cause the widget tree to pick up any
    // / changed implementations.
    // /
    // / This is expensive and should not be called except during development.
    fun reassemble(root: Element) {
        // TODO(Migration/Filip): Not need now
        // Timeline.startSync('Dirty Element Tree');
        try {
            assert(root._parent == null)
            assert(root.owner == this)
            root._reassemble()
        } finally {
            // TODO(Migration/Filip): Not need now
            // Timeline.finishSync();
        }
    }
}