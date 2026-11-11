/*
 * Copyright 2026 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

@file:Suppress("PrimitiveInCollection", "BanThreadSleep")

package androidx.xr.testutils

import android.app.Activity
import android.app.UiAutomation
import android.graphics.Bitmap
import android.graphics.Rect
import android.graphics.RectF
import android.os.Looper
import android.os.ParcelFileDescriptor
import android.os.SystemClock
import android.util.Log
import androidx.annotation.VisibleForTesting
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.screenshot.matchers.MSSIMMatcher
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Quaternion
import androidx.xr.runtime.math.Vector2
import androidx.xr.runtime.math.Vector3
import java.io.InputStream
import java.util.regex.Pattern
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.max
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource

/**
 * Utility for capturing, inspecting, and querying spatial scenes from Android XR devices and
 * emulators.
 *
 * Parses point-in-time scene graph snapshots from `dumpsys spf_cpm` into a clean object structure
 * based strictly on system-guaranteed CPM and SpaceFlinger keywords, properties, and task
 * boundaries.
 *
 * For the complete structural and keyword specification of `dumpsys spf_cpm`, see
 * `CPM_DUMPSYS_ARCHITECTURE.md` located in the `xr:testutils` root directory.
 */
public object SpatialSceneHelper {

    /**
     * Captures a point-in-time snapshot of the spatial scene graph from `dumpsys spf_cpm`.
     *
     * @param uiAutomation The [UiAutomation] instance used to execute the shell command.
     */
    @JvmStatic
    @JvmOverloads
    public fun captureSpatialScene(
        uiAutomation: UiAutomation = InstrumentationRegistry.getInstrumentation().uiAutomation
    ): SpatialScene = captureSceneWithPolling(uiAutomation = uiAutomation)

    /**
     * Repeatedly captures the spatial scene graph until [until] holds for the value returned by
     * [select].
     *
     * Note that each iteration calls [captureSpatialScene], which may itself poll internally for up
     * to 3 seconds while waiting for window transition snapshots to clear.
     *
     * @param description Human-readable description of the condition being awaited, used in the
     *   timeout error message.
     * @param timeout Maximum duration to poll before throwing an [AssertionError].
     * @param pollInterval Delay between successive scene captures.
     * @param uiAutomation The [UiAutomation] instance used to execute the shell command.
     * @param select Selector that extracts a value from each captured [SpatialScene].
     * @param until Predicate that determines whether the selected value satisfies the condition.
     * @return The first selected value for which [until] returns `true`.
     * @throws AssertionError if [until] does not hold within [timeout].
     */
    @JvmStatic
    @JvmOverloads
    public fun <T> waitForSpatialScene(
        description: String,
        timeout: Duration = 5.seconds,
        pollInterval: Duration = 100.milliseconds,
        uiAutomation: UiAutomation = InstrumentationRegistry.getInstrumentation().uiAutomation,
        select: (SpatialScene) -> T,
        until: (T) -> Boolean,
    ): T {
        val start = TimeSource.Monotonic.markNow()
        while (true) {
            val scene = captureSpatialScene(uiAutomation = uiAutomation)
            val selected = select(scene)
            if (until(selected)) return selected
            if (start.elapsedNow() >= timeout) {
                throw AssertionError(
                    "Timed out after $timeout waiting for $description, last value: $selected"
                )
            }
            SystemClock.sleep(pollInterval.inWholeMilliseconds)
        }
    }

    /**
     * Repeatedly captures the spatial scene graph until the target [SpatialTask] (for [activity] if
     * provided, or [SpatialScene.activeTask] otherwise) satisfies [SpatialTask.isSettled].
     *
     * @param activity Optional [Activity] whose spatial task should be awaited. When `null`, awaits
     *   the foreground [SpatialScene.activeTask].
     * @param expectedEmbeddedPanelCount Expected number of visible
     *   [SpatialTask.embeddedWindowLeashes] with valid surface bounds, or `null` to not constrain
     *   the embedded panel count.
     * @param expectedMainPanelVisible Expected visibility of [SpatialTask.taskWindowLeash], or
     *   `null` to not constrain the main panel visibility.
     * @param requireFullSpace Whether the task must have an active Full Space transform
     *   ([SpatialTask.isFullSpace]).
     * @param timeout Maximum duration to poll before throwing an [AssertionError].
     * @param pollInterval Delay between successive scene captures.
     * @param uiAutomation The [UiAutomation] instance used to execute the shell command.
     * @return The settled [SpatialTask].
     * @throws AssertionError if the task does not settle within [timeout].
     */
    @JvmStatic
    @JvmOverloads
    public fun waitForSettledSpatialTask(
        activity: Activity? = null,
        expectedEmbeddedPanelCount: Int? = null,
        expectedMainPanelVisible: Boolean? = null,
        requireFullSpace: Boolean = true,
        timeout: Duration = 5.seconds,
        pollInterval: Duration = 100.milliseconds,
        uiAutomation: UiAutomation = InstrumentationRegistry.getInstrumentation().uiAutomation,
    ): SpatialTask {
        val targetDesc = activity?.let { "task for $it" } ?: "active spatial task"
        val panelDesc =
            if (expectedEmbeddedPanelCount != null) {
                " with $expectedEmbeddedPanelCount visible embedded window leashes"
            } else {
                ""
            }
        val settledTask =
            waitForSpatialScene(
                description = "settled $targetDesc$panelDesc",
                timeout = timeout,
                pollInterval = pollInterval,
                uiAutomation = uiAutomation,
                select = { scene ->
                    if (activity != null) {
                        scene.findTaskByActivity(activity)
                    } else {
                        scene.activeTask
                    }
                },
                until = { task ->
                    task != null &&
                        task.isSettled(
                            expectedEmbeddedPanelCount = expectedEmbeddedPanelCount,
                            expectedMainPanelVisible = expectedMainPanelVisible,
                            requireFullSpace = requireFullSpace,
                        )
                },
            )
        return checkNotNull(settledTask) {
            "waitForSpatialScene returned null for settled $targetDesc$panelDesc"
        }
    }

    /**
     * Captures the 3D XR compositor output via [UiAutomation.takeScreenshot], waits for consecutive
     * compositor frames to stabilize, converts hardware-backed bitmaps to
     * [Bitmap.Config.ARGB_8888], optionally crops to [cropBounds] (or automatically crops out the
     * empty background when [autoCrop] is true and [cropBounds] is null), and returns the resulting
     * [Bitmap] for assertion with `androidx.test.screenshot.assertAgainstGolden`.
     *
     * The caller owns the returned [Bitmap] and is responsible for calling [Bitmap.recycle] when
     * finished (or letting higher-level test scopes manage its lifecycle).
     *
     * Standard Android 2D `View.drawToBitmap()` or `SemanticsNodeInteraction.captureToImage()` only
     * captures flat 2D window surfaces and does not include 3D `GltfModelEntity`, `SurfaceEntity`,
     * `SpatialEnvironment` skyboxes, or 3D spatial relationships rendered by SpaceFlinger/Impress.
     * Use this helper after [waitForSpatialScene] for the scene graph to settle.
     *
     * @param cropBounds Optional [Rect] in screen pixel coordinates to crop the compositor
     *   screenshot. If null and [autoCrop] is true, the screenshot is automatically cropped to the
     *   rendered foreground content via [cropForeground].
     * @param padding Extra padding in pixels to preserve around the cropped foreground bounds when
     *   [cropBounds] is null and [autoCrop] is true.
     * @param autoCrop When true and [cropBounds] is null, automatically crops out the background.
     *   Set to false to retrieve the full uncropped compositor frame.
     * @param timeout Maximum duration to wait for consecutive frames to stabilize. If frames have
     *   not stabilized by then, the latest frame is returned and a warning is logged.
     * @param pollInterval Delay between successive compositor captures.
     * @param uiAutomation The [UiAutomation] instance used to capture the compositor frame.
     * @return A software-backed [Bitmap.Config.ARGB_8888] [Bitmap] owned by the caller.
     * @throws IllegalArgumentException if [cropBounds] does not overlap the screenshot.
     * @throws AssertionError if no screenshot could be captured within [timeout].
     */
    @JvmStatic
    @JvmOverloads
    public fun captureSceneToBitmap(
        cropBounds: Rect? = null,
        padding: Int = 0,
        autoCrop: Boolean = true,
        timeout: Duration = 5.seconds,
        pollInterval: Duration = 100.milliseconds,
        uiAutomation: UiAutomation = InstrumentationRegistry.getInstrumentation().uiAutomation,
    ): Bitmap {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        }
        captureSceneWithPolling(
            uiAutomation = uiAutomation,
            maxWaitMs = max(MIN_SCREENSHOT_SCENE_SETTLE_WAIT_MS, timeout.inWholeMilliseconds),
        )
        val settledBitmap = waitForSettledSoftwareScreenshot(uiAutomation, timeout, pollInterval)
        if (cropBounds != null) {
            val cropped =
                try {
                    crop(settledBitmap, cropBounds)
                } catch (t: Throwable) {
                    settledBitmap.recycle()
                    throw t
                }
            // createBitmap returns the source itself when the crop covers the whole bitmap.
            if (cropped !== settledBitmap) {
                settledBitmap.recycle()
            }
            return cropped
        }
        if (autoCrop) {
            val cropped =
                try {
                    cropForeground(settledBitmap, padding = padding)
                } catch (t: Throwable) {
                    settledBitmap.recycle()
                    throw t
                }
            if (cropped !== settledBitmap) {
                settledBitmap.recycle()
            }
            return cropped
        }
        return settledBitmap
    }

    /**
     * Captures the 3D XR compositor output and automatically crops the screenshot to the rendered
     * content of [node], accounting for its position relative to the user's head pose.
     *
     * Delegates to [SpatialNode.captureToBitmap].
     *
     * @param node Target [SpatialNode] (such as [SpatialTask.spatialTransform],
     *   [SpatialTask.activitySpaceRoot], or an individual window leash/panel node) whose rendered
     *   content should be cropped.
     * @param padding Extra padding in pixels to preserve around the cropped content bounds.
     * @param timeout Maximum duration to wait for consecutive frames to stabilize. If frames have
     *   not stabilized by then, the latest frame is returned and a warning is logged.
     * @param pollInterval Delay between successive compositor captures.
     * @param uiAutomation The [UiAutomation] instance used to capture the compositor frame.
     * @return A cropped software-backed [Bitmap.Config.ARGB_8888] [Bitmap] owned by the caller.
     * @throws AssertionError if no screenshot could be captured within [timeout].
     */
    @JvmStatic
    @JvmOverloads
    public fun captureSceneToBitmap(
        node: SpatialNode,
        padding: Int = 0,
        timeout: Duration = 5.seconds,
        pollInterval: Duration = 100.milliseconds,
        uiAutomation: UiAutomation = InstrumentationRegistry.getInstrumentation().uiAutomation,
    ): Bitmap =
        node.captureToBitmap(
            padding = padding,
            timeout = timeout,
            pollInterval = pollInterval,
            uiAutomation = uiAutomation,
        )

    /**
     * Captures the spatial scene and 3D XR compositor output, resolves the target [SpatialNode] via
     * [nodeSelector], and automatically crops the screenshot to the rendered content of the
     * selected node.
     *
     * @param nodeSelector Selector invoked on the settled [SpatialScene] to identify the target
     *   [SpatialNode] to crop.
     * @param padding Extra padding in pixels to preserve around the cropped content bounds.
     * @param timeout Maximum duration to wait for consecutive frames to stabilize.
     * @param pollInterval Delay between successive compositor captures.
     * @param uiAutomation The [UiAutomation] instance used to capture the compositor frame.
     * @return A cropped software-backed [Bitmap.Config.ARGB_8888] [Bitmap] owned by the caller.
     * @throws IllegalArgumentException if [nodeSelector] returns `null` for the captured scene.
     * @throws AssertionError if no screenshot could be captured within [timeout].
     */
    @JvmStatic
    @JvmOverloads
    public fun captureSceneToBitmap(
        nodeSelector: (SpatialScene) -> SpatialNode?,
        padding: Int = 0,
        timeout: Duration = 5.seconds,
        pollInterval: Duration = 100.milliseconds,
        uiAutomation: UiAutomation = InstrumentationRegistry.getInstrumentation().uiAutomation,
    ): Bitmap {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        }
        val scene =
            captureSceneWithPolling(
                uiAutomation = uiAutomation,
                maxWaitMs = max(MIN_SCREENSHOT_SCENE_SETTLE_WAIT_MS, timeout.inWholeMilliseconds),
            )
        val targetNode =
            requireNotNull(nodeSelector(scene)) {
                "nodeSelector returned null for captured SpatialScene (tasks=${scene.tasks})"
            }
        return targetNode.captureToBitmap(
            padding = padding,
            timeout = timeout,
            pollInterval = pollInterval,
            uiAutomation = uiAutomation,
        )
    }

    internal fun captureSceneWithPolling(
        uiAutomation: UiAutomation,
        maxWaitMs: Long = DEFAULT_SCENE_POLL_MAX_WAIT_MS,
    ): SpatialScene {
        var scene: SpatialScene
        val startTime = System.currentTimeMillis()
        while (true) {
            val rawDump = executeShellCommand("dumpsys spf_cpm", uiAutomation)
            val activitiesDump = executeShellCommand("dumpsys activity activities", uiAutomation)
            val taskPackageMap = parseTaskPackageMap(activitiesDump)
            scene = parseSpatialScene(rawDump = rawDump, taskPackageMap = taskPackageMap)
            val hasTransition = scene.hasActiveTransition
            val isReady =
                scene.tasks.any { task ->
                    task.allNodes.any { it.hasSurfaceBounds || it.isEmbeddedWindowLeash }
                }
            if ((!hasTransition && isReady) || System.currentTimeMillis() - startTime > maxWaitMs) {
                break
            }
            try {
                Thread.sleep(100)
            } catch (_: InterruptedException) {}
        }
        return scene
    }

    /**
     * Parses the raw output of `dumpsys spf_cpm` and an optional task-package mapping into a
     * [SpatialScene].
     */
    @VisibleForTesting
    internal fun parseSpatialScene(
        rawDump: String,
        taskPackageMap: Map<Int, String> = emptyMap(),
    ): SpatialScene =
        SpatialScene(rootNodes = parseRootNodes(rawDump), taskPackageMap = taskPackageMap)

    /** Parses the raw output of `dumpsys spf_cpm` into a list of [SpatialNode] root entities. */
    @VisibleForTesting
    internal fun parseRootNodes(dump: String): List<SpatialNode> {
        val rootNodes = mutableListOf<SpatialNode>()
        val nodeStack = ArrayDeque<SpatialNode>()

        var currentNode: SpatialNode? = null
        val currentAttributes = mutableMapOf<String, FloatArray>()
        val currentProperties = mutableMapOf<String, String>()

        fun flushCurrentNode() {
            val node = currentNode ?: return
            val finishedNode =
                SpatialNode(
                    header = node.header,
                    name = node.name,
                    id = node.id,
                    depth = node.depth,
                    attributes = currentAttributes.toMap(),
                    properties = currentProperties.toMap(),
                )
            while (nodeStack.isNotEmpty() && nodeStack.last().depth >= finishedNode.depth) {
                nodeStack.removeLast()
            }

            if (nodeStack.isEmpty()) {
                rootNodes.add(finishedNode)
            } else {
                nodeStack.last().addChild(finishedNode)
            }

            nodeStack.addLast(finishedNode)
            currentNode = null
            currentAttributes.clear()
            currentProperties.clear()
        }

        for (line in dump.lineSequence()) {
            val trimmed = line.trim()
            if (
                trimmed.startsWith("InputTargetManager state:") ||
                    trimmed.startsWith("Stats since reset:") ||
                    trimmed.startsWith("Number of living nodes:") ||
                    trimmed.startsWith("Total size of buffers in scene:")
            ) {
                break
            }
            if (trimmed.isEmpty() || trimmed.startsWith("SPF Nodes:")) {
                continue
            }

            var indent = 0
            while (indent < line.length && line[indent] == ' ') {
                indent++
            }

            if (trimmed.startsWith("Node ")) {
                flushCurrentNode()

                val matcher = NODE_LINE_PATTERN.matcher(line)
                var nodeName = ""
                var nodeId = -1

                if (matcher.find()) {
                    nodeName = matcher.group(1)?.trim() ?: ""
                    nodeId = matcher.group(2)?.toIntOrNull() ?: -1
                } else {
                    val nodeIdx = line.indexOf("Node ")
                    if (nodeIdx >= 0) {
                        val rest = line.substring(nodeIdx + 5).trim()
                        val colonIdx = rest.lastIndexOf(':')
                        nodeName = if (colonIdx > 0) rest.substring(0, colonIdx).trim() else rest
                    }
                }

                currentNode =
                    SpatialNode(header = trimmed, name = nodeName, id = nodeId, depth = indent)
            }

            if (currentNode != null) {
                if (trimmed.contains("visible on both")) {
                    currentProperties["stereoVisibility"] = "visible on both"
                } else if (trimmed.contains("hidden on both")) {
                    currentProperties["stereoVisibility"] = "hidden on both"
                }

                val reformMatcher = REFORM_OPTIONS_PATTERN.matcher(line)
                if (reformMatcher.find()) {
                    currentProperties["reformOptions"] = reformMatcher.group(1) ?: ""
                }

                val sysUiMetadataMatcher = SYS_UI_METADATA_PATTERN.matcher(line)
                if (sysUiMetadataMatcher.find()) {
                    currentProperties["sysUIMetadata"] = sysUiMetadataMatcher.group(1) ?: ""
                }

                val attrMatcher = ATTR_ARRAY_PATTERN.matcher(line)
                while (attrMatcher.find()) {
                    val key = attrMatcher.group(1) ?: continue
                    val valueStr = attrMatcher.group(2) ?: continue
                    val arr = parseArray(valueStr)
                    if (arr.isNotEmpty()) {
                        currentAttributes[key] = arr
                    } else {
                        currentProperties[key] = valueStr
                    }
                }

                val propMatcher = PROP_KEY_VALUE_PATTERN.matcher(line)
                while (propMatcher.find()) {
                    val key = propMatcher.group(1) ?: continue
                    val value = propMatcher.group(2) ?: continue
                    if (!currentProperties.containsKey(key)) {
                        currentProperties[key] = value
                    }
                }
            }
        }

        flushCurrentNode()
        return rootNodes
    }

    private fun parseArray(input: String): FloatArray {
        val cleaned = input.replace("(", "").replace(")", "").replace(",", " ").trim()
        if (cleaned.isEmpty()) return FloatArray(0)
        val parts = cleaned.split(WHITESPACE_PATTERN).filter { it.isNotEmpty() }
        return try {
            FloatArray(parts.size) { i -> parts[i].toFloat() }
        } catch (e: NumberFormatException) {
            FloatArray(0)
        }
    }

    /**
     * Parses the output of `dumpsys activity activities` to build a map of task IDs to their
     * associated Android package names.
     */
    internal fun parseTaskPackageMap(activitiesDump: String): Map<Int, String> {
        if (activitiesDump.isBlank()) return emptyMap()
        val map = mutableMapOf<Int, String>()

        // Pattern 1: ActivityRecord{... <package>/<activity> ... t<taskId> ...}
        val actMatcher = ACTIVITY_RECORD_PATTERN.matcher(activitiesDump)
        while (actMatcher.find()) {
            val pkg = actMatcher.group(1) ?: continue
            val taskId = actMatcher.group(2)?.toIntOrNull() ?: continue
            map[taskId] = pkg
        }

        // Pattern 2: Task{... #<taskId> ... A=(<uid>:)?<package> ...}
        val taskMatcher = TASK_AFFINITY_PATTERN.matcher(activitiesDump)
        while (taskMatcher.find()) {
            val taskId = taskMatcher.group(1)?.toIntOrNull() ?: continue
            val pkg = taskMatcher.group(2) ?: continue
            if (!map.containsKey(taskId)) {
                map[taskId] = pkg
            }
        }

        // Pattern 3: Task{... #<taskId> ... <package>/...}
        val compMatcher = TASK_COMPONENT_PATTERN.matcher(activitiesDump)
        while (compMatcher.find()) {
            val taskId = compMatcher.group(1)?.toIntOrNull() ?: continue
            val pkg = compMatcher.group(2) ?: continue
            if (!map.containsKey(taskId)) {
                map[taskId] = pkg
            }
        }

        return map
    }

    /**
     * Executes a shell command via [uiAutomation] and returns the complete trimmed stdout output.
     */
    internal fun executeShellCommand(
        command: String,
        uiAutomation: UiAutomation? = null,
    ): String {
        return try {
            val automation =
                uiAutomation
                    ?: InstrumentationRegistry.getInstrumentation().uiAutomation
                    ?: return ""
            automation.executeShellCommand(command).use { pfd ->
                ParcelFileDescriptor.AutoCloseInputStream(pfd).use { stream: InputStream ->
                    stream.bufferedReader().readText().trim()
                }
            }
        } catch (e: Exception) {
            Log.e("SpatialSceneHelper", "Error executing shell command: $command", e)
            ""
        }
    }

    private fun isPackageChar(c: Char): Boolean =
        c.isLetterOrDigit() || c == '_' || c == '.' || c == '$'

    private fun containsSegment(
        text: String,
        target: String,
        requireTrailingBoundary: Boolean,
    ): Boolean {
        if (target.isEmpty() || text.isEmpty()) return false
        var index = 0
        while (true) {
            val found = text.indexOf(target, index)
            if (found < 0) return false
            val end = found + target.length
            val validStart = (found == 0 || !isPackageChar(text[found - 1]))
            val validEnd =
                (!requireTrailingBoundary || end == text.length || !isPackageChar(text[end]))
            if (validStart && validEnd) {
                return true
            }
            index = found + 1
        }
    }

    internal fun containsPackage(text: String, pkg: String): Boolean =
        containsSegment(text, pkg, requireTrailingBoundary = true)

    private val ACTIVITY_RECORD_PATTERN =
        Pattern.compile(
            "ActivityRecord\\{[^}]*?\\s+([a-zA-Z0-9_]+(?:\\.[a-zA-Z0-9_]+)+)/[^}\\s]+\\s+t(\\d+)\\b"
        )
    private val TASK_AFFINITY_PATTERN =
        Pattern.compile(
            "Task\\{[^}]*?#(\\d+)[^}]*?\\bA=(?:\\d+:)?([a-zA-Z0-9_]+(?:\\.[a-zA-Z0-9_]+)+)"
        )
    private val TASK_COMPONENT_PATTERN =
        Pattern.compile("Task\\{[^}]*?#(\\d+)[^}]*?\\s+([a-zA-Z0-9_]+(?:\\.[a-zA-Z0-9_]+)+)/")
    private val NODE_LINE_PATTERN =
        Pattern.compile("^\\s*Node\\s+(.+?)(?:\\s*\\([^)]*\\))?(?:\\s+id=(\\d+))?\\s*:?\\s*$")
    private val ATTR_ARRAY_PATTERN = Pattern.compile("([a-zA-Z0-9_#.-]+)=\\(([^)]+)\\)")
    private val REFORM_OPTIONS_PATTERN = Pattern.compile("reformOptions=<([^>]+)>")
    private val SYS_UI_METADATA_PATTERN = Pattern.compile("sysUIMetadata=<([^>]+)>")
    private val PROP_KEY_VALUE_PATTERN = Pattern.compile("([a-zA-Z0-9_#.-]+)=([a-zA-Z0-9_#.-]+)")
    private val WHITESPACE_PATTERN = Pattern.compile("\\s+")
}

/**
 * Represents a spatial scene graph snapshot consisting of root [SpatialNode] entities.
 *
 * @param rootNodes Top-level root nodes in the spatial scene graph (typically the single `Root`
 *   node).
 * @param taskPackageMap Optional mapping from task IDs to Android package names.
 */
public class SpatialScene
@JvmOverloads
constructor(
    public val rootNodes: List<SpatialNode>,
    public val taskPackageMap: Map<Int, String> = emptyMap(),
) {
    init {
        fun attach(n: SpatialNode) {
            n.scene = this
            for (c in n.children) {
                attach(c)
            }
        }
        for (r in rootNodes) {
            attach(r)
        }
    }

    /**
     * Discovers all structured tasks in the scene graph.
     *
     * In the Android XR CPM (`dumpsys spf_cpm`) architecture, Window Manager creates
     * `space-root-task-<taskId>` containers directly under the compositor `Root` node. Each
     * container defines the root reference space for an independent Android task. Task roots are
     * guaranteed to be direct children of the root node (or the root nodes themselves when
     * evaluating an isolated task subtree) and never nest within other tasks or intermediate
     * wrapper nodes. Consequently, task discovery only inspects top-level roots and their direct
     * children without recursing down the hierarchy.
     */
    public val tasks: List<SpatialTask> by lazy {
        val taskRoots = rootNodes.flatMap { root ->
            // In CPM dumpsys, task roots are always direct children of the compositor Root node
            // (or the root itself if scoped to a task subtree). Intermediate wrapper nodes are not
            // used by Window Manager/CPM for task roots, so deeper recursion is unnecessary.
            if (root.isTaskRoot) listOf(root) else root.children.filter { it.isTaskRoot }
        }
        taskRoots.map { root ->
            val taskId =
                root.name.removePrefix("space-root-task-").substringBefore(' ').toIntOrNull() ?: -1
            val pkg = taskPackageMap[taskId] ?: SpatialTask.extractPackageFromNodes(root)
            SpatialTask(root, pkg)
        }
    }

    /** Finds the first task belonging to [packageName], or null if not found. */
    public fun findTaskByPackageName(packageName: String): SpatialTask? = tasks.firstOrNull {
        it.packageName == packageName
    }

    /** Finds all tasks belonging to [packageName]. */
    public fun findTasksByPackageName(packageName: String): List<SpatialTask> = tasks.filter {
        it.packageName == packageName
    }

    /** Finds a task by its [taskId], or null if not found. */
    public fun findTaskById(taskId: Int): SpatialTask? = tasks.firstOrNull { it.taskId == taskId }

    /** Finds the task associated with the given [activity], or null if not found. */
    public fun findTaskByActivity(activity: Activity): SpatialTask? =
        findTaskById(activity.taskId) ?: findTaskByPackageName(activity.packageName)

    /**
     * The currently active foreground [SpatialTask] in the scene graph, or `null` if no task is
     * active.
     */
    public val activeTask: SpatialTask?
        get() = tasks.firstOrNull { it.isActive }

    /**
     * True if the scene graph currently contains in-flight window transition snapshots, an active
     * system transition scrim, or a visible transient system toast overlay.
     */
    public val hasActiveTransition: Boolean
        get() = rootNodes.any { root ->
            root.findNode { node ->
                node.name.contains("transition snapshot", ignoreCase = true) ||
                    (node.name == "Scrim" && node.isVisible && node.scrimTargetAlpha > 0.01f) ||
                    node.isVisibleToastOverlay
            } != null
        }

    /**
     * The world-space pose of the user's head (`HEAD` coordinate space,
     * `transformUpdateRequest.coordinateSpace=2` in `dumpsys spf_cpm`) if a head-locked transform
     * node is present in the scene graph, or `null` if unavailable.
     */
    public val headPose: Pose? by lazy {
        val headTransformNodes =
            rootNodes
                .flatMap { listOf(it) + it.getDescendants() }
                .filter { it.properties["transformUpdateRequest.coordinateSpace"] == "2" }
        val selected =
            headTransformNodes.firstOrNull {
                it.properties["ImpNodeIsActive"] == "true" ||
                    it.parent?.properties?.get("ImpNodeIsActive") == "true"
            } ?: headTransformNodes.firstOrNull()
        selected?.let { transformNode ->
            val parent = transformNode.parent
            if (parent != null && parent.name != "Root") parent.pose else transformNode.pose
        }
    }

    /**
     * Crops [bitmap] to the foreground content of this scene (plus optional [padding]), cropping
     * out the surrounding black environment background.
     *
     * Delegates to [cropForeground].
     *
     * @param bitmap Source [Bitmap] from the XR compositor.
     * @param padding Extra padding in pixels to preserve around the cropped content bounds.
     * @return A software-backed [Bitmap.Config.ARGB_8888] [Bitmap] containing the cropped
     *   foreground.
     */
    @JvmOverloads
    public fun crop(
        bitmap: Bitmap,
        padding: Int = 0,
    ): Bitmap = cropForeground(bitmap, padding)

    /**
     * Captures the 3D XR compositor output and automatically crops the screenshot to the rendered
     * foreground content of the entire scene, cropping out the surrounding black background.
     *
     * This is particularly useful for scenes containing 3D entities (such as GLTF models) that do
     * not have 2D surface bounds in CPM.
     *
     * @param padding Extra padding in pixels to preserve around the cropped content bounds.
     * @param timeout Maximum duration to wait for consecutive frames to stabilize.
     * @param pollInterval Delay between successive compositor captures.
     * @param uiAutomation The [UiAutomation] instance used to capture the compositor frame.
     * @return A cropped software-backed [Bitmap.Config.ARGB_8888] [Bitmap] owned by the caller.
     * @throws AssertionError if no screenshot could be captured within [timeout].
     */
    @JvmOverloads
    public fun captureToBitmap(
        padding: Int = 0,
        timeout: Duration = 5.seconds,
        pollInterval: Duration = 100.milliseconds,
        uiAutomation: UiAutomation = InstrumentationRegistry.getInstrumentation().uiAutomation,
    ): Bitmap =
        SpatialSceneHelper.captureSceneToBitmap(
            cropBounds = null,
            padding = padding,
            timeout = timeout,
            pollInterval = pollInterval,
            uiAutomation = uiAutomation,
        )

    override fun toString(): String = "SpatialScene(tasks=$tasks, rootNodes=${rootNodes.size})"
}

/**
 * Encapsulates an Android task's entire spatial scene subtree, rooted at
 * `space-root-task-<taskId>`.
 *
 * In OpenXR, this corresponds to the application's root reference space (`XrSpace`).
 */
public class SpatialTask
@JvmOverloads
constructor(public val activitySpaceRoot: SpatialNode, packageName: String? = null) {

    /** The integer task ID extracted from `space-root-task-<taskId>`. */
    public val taskId: Int =
        activitySpaceRoot.name.removePrefix("space-root-task-").substringBefore(' ').toIntOrNull()
            ?: -1

    /** The package name associated with this task, if resolved or discovered. */
    public val packageName: String? = packageName ?: extractPackageFromNodes(activitySpaceRoot)

    /** The node name of the task root (e.g. `space-root-task-<taskId>`). */
    public val name: String
        get() = activitySpaceRoot.name

    /** System transform node for 3D spatial/Subspace content, or null. */
    public val spatialTransform: SpatialNode? by lazy {
        activitySpaceRoot.children.firstOrNull { it.name.startsWith("space-transform-spatial-") }
    }

    /** Flat list of all nodes belonging to this task hierarchy. */
    public val allNodes: List<SpatialNode> by lazy {
        buildList {
            fun collect(node: SpatialNode) {
                add(node)
                for (child in node.children) {
                    collect(child)
                }
            }
            collect(activitySpaceRoot)
        }
    }

    /** All active raycast and touch input targets within this task. */
    public val inputTargets: List<SpatialNode> by lazy { allNodes.filter { it.isInputTarget } }

    /** The task's primary window leash container ([SpatialNode.isTaskWindowLeash]), if present. */
    public val taskWindowLeash: SpatialNode?
        get() = findNode { it.isTaskWindowLeash }

    /**
     * All embedded Activity task window leash containers ([SpatialNode.isActivityWindowLeash]) in
     * this task.
     */
    public val activityWindowLeashes: List<SpatialNode>
        get() = findNodes { it.isActivityWindowLeash }

    /**
     * All embedded spatial window leash containers ([SpatialNode.isEmbeddedWindowLeash]) in this
     * task.
     */
    public val embeddedWindowLeashes: List<SpatialNode>
        get() = findNodes { it.isEmbeddedWindowLeash }

    /** All spatial window leash containers ([SpatialNode.isWindowLeash]) in this task. */
    public val windowLeashes: List<SpatialNode>
        get() = findNodes { it.isWindowLeash }

    /**
     * All top-level window leash containers in this task (window leashes that are not nested inside
     * another window leash).
     */
    public val topLevelWindowLeashes: List<SpatialNode>
        get() = findNodes { it.isTopLevelWindowLeash }

    /** All 3D subspace nodes (`hasSubspace=true`) in this task. */
    public val subspaceNodes: List<SpatialNode>
        get() = findNodes { it.hasSubspace }

    /**
     * True if this task's root container is visible and active in the scene graph and hosts an
     * active 3D spatial transform or visible window leash.
     */
    public val isActive: Boolean
        get() =
            activitySpaceRoot.isVisible &&
                activitySpaceRoot.properties["ImpNodeIsActive"] != "false" &&
                (isFullSpace ||
                    taskWindowLeash?.isVisible == true ||
                    windowLeashes.any { it.isVisible })

    /**
     * True if this task has a visible 3D spatial transform node (`space-transform-spatial-<id>`) in
     * the scene graph, indicating Full Space Mode is active for the task.
     */
    public val isFullSpace: Boolean
        get() =
            spatialTransform?.let {
                it.isVisible && it.properties["ImpNodeIsActive"] != "false"
            } == true

    /**
     * Effective virtual pixel density in pixels per meter for this task, resolved from
     * [SpatialNode.computedPixelsPerMeter] on the task hierarchy or derived from 2D surface bounds,
     * or `null` if unavailable.
     */
    public val computedPixelsPerMeter: Float?
        get() =
            (spatialTransform ?: activitySpaceRoot).computedPixelsPerMeter
                ?: allNodes.firstNotNullOfOrNull { it.computedPixelsPerMeter }
                ?: allNodes.firstNotNullOfOrNull { node ->
                    val px = node.surfaceBoundsInPixels
                    val m = node.surfaceBoundsInMeters
                    if (px != null && m != null && px.x > 0f && m.x > 0f) px.x / m.x else null
                }

    /**
     * Returns `true` if this task and its spatial scene have settled with no in-flight transitions
     * or alpha animations, and its panels match the expected visibility and count.
     *
     * @param expectedEmbeddedPanelCount Expected number of visible [embeddedWindowLeashes] with
     *   non-null [SpatialNode.effectiveSurfaceBoundsInMeters], or `null` to not constrain the
     *   embedded panel count.
     * @param expectedMainPanelVisible Expected visibility of [taskWindowLeash], or `null` to not
     *   constrain the main panel visibility.
     * @param requireFullSpace Whether the task must be in Full Space Mode ([isFullSpace]).
     */
    @JvmOverloads
    public fun isSettled(
        expectedEmbeddedPanelCount: Int? = null,
        expectedMainPanelVisible: Boolean? = null,
        requireFullSpace: Boolean = true,
    ): Boolean {
        if (!isActive) return false
        if (requireFullSpace && !isFullSpace) return false
        if (activitySpaceRoot.scene?.hasActiveTransition == true) return false
        if (allNodes.any { it.name.contains("transition snapshot", ignoreCase = true) }) {
            return false
        }
        val alphasSettled =
            allNodes
                .filter { it.isVisible }
                .all { node ->
                    val requested = node.properties["requestedAlpha"]?.toFloatOrNull()
                    val computed = node.properties["computedAlpha"]?.toFloatOrNull()
                    requested == null || computed == null || abs(requested - computed) <= 0.01f
                }
        if (!alphasSettled) return false

        if (
            expectedMainPanelVisible != null &&
                (taskWindowLeash?.isVisible == true) != expectedMainPanelVisible
        ) {
            return false
        }

        if (expectedEmbeddedPanelCount != null) {
            val visiblePanels = embeddedWindowLeashes.count {
                it.isVisible && it.effectiveSurfaceBoundsInMeters != null
            }
            if (visiblePanels != expectedEmbeddedPanelCount) return false
        }

        return true
    }

    /** Finds all nodes matching [predicate] within this task. */
    public fun findNodes(predicate: (SpatialNode) -> Boolean): List<SpatialNode> =
        allNodes.filter(predicate)

    /** Finds the first node matching [predicate] within this task, or null. */
    public fun findNode(predicate: (SpatialNode) -> Boolean): SpatialNode? =
        allNodes.firstOrNull(predicate)

    /** Finds all nodes whose name contains [nameSubstring] (case-insensitive) within this task. */
    public fun findNodesByName(nameSubstring: String): List<SpatialNode> = allNodes.filter {
        it.name.contains(nameSubstring, ignoreCase = true)
    }

    /**
     * Finds the first node whose name contains [nameSubstring] (case-insensitive) within this task.
     */
    public fun findNodeByName(nameSubstring: String): SpatialNode? = allNodes.firstOrNull {
        it.name.contains(nameSubstring, ignoreCase = true)
    }

    override fun toString(): String =
        "SpatialTask(taskId=$taskId, packageName=$packageName, nodes=${allNodes.size})"

    internal companion object {
        private val PACKAGE_NAME_PATTERN =
            Pattern.compile("^[a-zA-Z_][a-zA-Z0-9_]*(\\.[a-zA-Z_][a-zA-Z0-9_]*)+$")

        internal fun extractPackageFromNodes(root: SpatialNode): String? {
            fun search(node: SpatialNode): String? {
                val segment = node.name.substringBefore('/').trim()
                if (PACKAGE_NAME_PATTERN.matcher(segment).matches()) {
                    return segment
                }
                for (child in node.children) {
                    val found = search(child)
                    if (found != null) return found
                }
                return null
            }
            return search(root)
        }
    }

    /**
     * Crops [bitmap] to the rendered content bounds of this task (plus optional [padding]),
     * delegating to [spatialTransform] or [activitySpaceRoot].
     *
     * @param bitmap Source [Bitmap] from the XR compositor.
     * @param padding Extra padding in pixels to preserve around the cropped content bounds.
     * @return A software-backed [Bitmap.Config.ARGB_8888] [Bitmap] containing the cropped content.
     */
    @JvmOverloads
    public fun crop(
        bitmap: Bitmap,
        padding: Int = 0,
    ): Bitmap = (spatialTransform ?: activitySpaceRoot).crop(bitmap, padding)

    /**
     * Captures the 3D XR compositor output and automatically crops the screenshot to the rendered
     * bounds of this task's main spatial transform or activity space root.
     *
     * @param padding Extra padding in pixels to preserve around the cropped content bounds.
     * @param timeout Maximum duration to wait for consecutive frames to stabilize.
     * @param pollInterval Delay between successive compositor captures.
     * @param uiAutomation The [UiAutomation] instance used to capture the compositor frame.
     * @return A cropped software-backed [Bitmap.Config.ARGB_8888] [Bitmap] owned by the caller.
     */
    @JvmOverloads
    public fun captureToBitmap(
        padding: Int = 0,
        timeout: Duration = 5.seconds,
        pollInterval: Duration = 100.milliseconds,
        uiAutomation: UiAutomation = InstrumentationRegistry.getInstrumentation().uiAutomation,
    ): Bitmap =
        (spatialTransform ?: activitySpaceRoot).captureToBitmap(
            padding = padding,
            timeout = timeout,
            pollInterval = pollInterval,
            uiAutomation = uiAutomation,
        )
}

/**
 * Represents an individual spatial entity in the CPM scene graph.
 *
 * Corresponds conceptually to an OpenXR Scene Entity (`XrSceneEntityKHRX1`).
 */
public class SpatialNode
@JvmOverloads
constructor(
    public val header: String = "",
    public val name: String = "",
    public val id: Int = -1,
    public val depth: Int = 0,
    public val attributes: Map<String, FloatArray> = emptyMap(),
    public val properties: Map<String, String> = emptyMap(),
) {
    public var parent: SpatialNode? = null
        internal set

    /** The [SpatialScene] containing this node, or null if constructed standalone. */
    public var scene: SpatialScene? = null
        internal set

    /** The topmost ancestor node in this spatial node's hierarchy (the root of the tree). */
    public val root: SpatialNode
        get() {
            var curr = this
            while (curr.parent != null) {
                curr = curr.parent!!
            }
            return curr
        }

    /**
     * The world-space pose of the user's head (`HEAD` coordinate space,
     * `transformUpdateRequest.coordinateSpace=2` in `dumpsys spf_cpm`), or `null` if unavailable.
     */
    public val headPose: Pose?
        get() = scene?.headPose ?: findHeadPoseInHierarchy()

    private fun findHeadPoseInHierarchy(): Pose? {
        val rootNode = root
        val headTransformNodes = rootNode.findNodes {
            it.properties["transformUpdateRequest.coordinateSpace"] == "2"
        }
        val selected =
            headTransformNodes.firstOrNull {
                it.properties["ImpNodeIsActive"] == "true" ||
                    it.parent?.properties?.get("ImpNodeIsActive") == "true"
            } ?: headTransformNodes.firstOrNull()
        return selected?.let { transformNode ->
            val parent = transformNode.parent
            if (parent != null && parent.name != "Root") parent.pose else transformNode.pose
        }
    }

    private val _children = mutableListOf<SpatialNode>()

    public val children: List<SpatialNode>
        get() = _children

    internal fun addChild(child: SpatialNode) {
        child.parent = this
        child.scene = this.scene
        _children.add(child)
    }

    public fun getDescendants(): List<SpatialNode> {
        val result = mutableListOf<SpatialNode>()
        fun traverse(node: SpatialNode) {
            for (child in node.children) {
                result.add(child)
                traverse(child)
            }
        }
        traverse(this)
        return result
    }

    /** Finds the first descendant matching [predicate], or null. */
    public fun findDescendant(predicate: (SpatialNode) -> Boolean): SpatialNode? =
        getDescendants().firstOrNull(predicate)

    /**
     * Finds the first descendant whose name contains [nameSubstring] (case-insensitive), or null.
     */
    public fun findDescendantByName(nameSubstring: String): SpatialNode? =
        getDescendants().firstOrNull { it.name.contains(nameSubstring, ignoreCase = true) }

    /** Finds all descendants matching [predicate]. */
    public fun findDescendants(predicate: (SpatialNode) -> Boolean): List<SpatialNode> =
        getDescendants().filter(predicate)

    /** Finds all nodes in this subtree (including `this`) matching [predicate]. */
    public fun findNodes(predicate: (SpatialNode) -> Boolean): List<SpatialNode> =
        (listOf(this) + getDescendants()).filter(predicate)

    /** Finds the first node in this subtree (including `this`) matching [predicate], or null. */
    public fun findNode(predicate: (SpatialNode) -> Boolean): SpatialNode? =
        if (predicate(this)) this else findDescendant(predicate)

    /** Finds all descendants whose name contains [nameSubstring] (case-insensitive). */
    public fun findDescendantsByName(nameSubstring: String): List<SpatialNode> =
        getDescendants().filter { it.name.contains(nameSubstring, ignoreCase = true) }

    public fun isDescendantOf(other: SpatialNode): Boolean {
        var current = parent
        while (current != null) {
            if (current === other) {
                return true
            }
            current = current.parent
        }
        return false
    }

    // --- Transforms & Poses (OpenXR Scene Entity Congruent) ---

    public val position: Vector3
        get() {
            val raw = attributes["ImpNodeWorldPosition"]
            return if (raw != null && raw.size >= 3) {
                Vector3(raw[0], raw[1], raw[2])
            } else {
                Vector3.Zero
            }
        }

    public val rotation: Quaternion
        get() {
            val raw = attributes["ImpNodeWorldRotation"]
            return if (raw != null && raw.size >= 4) {
                Quaternion(x = raw[1], y = raw[2], z = raw[3], w = raw[0])
            } else {
                Quaternion.Identity
            }
        }

    public val scale: Vector3
        get() {
            val raw = attributes["ImpNodeWorldScale"]
            return if (raw != null && raw.size >= 3) {
                Vector3(raw[0], raw[1], raw[2])
            } else {
                Vector3.One
            }
        }

    public val pose: Pose
        get() = Pose(position, rotation)

    /**
     * Transforms [point] from this node's local coordinate space into OpenXR world space, taking
     * into account [position], [rotation], and [scale].
     *
     * @param point 3D coordinates in this node's local space.
     * @return 3D coordinates in OpenXR world space.
     */
    public fun transformPointToWorld(point: Vector3): Vector3 {
        val scaled = point.scale(scale)
        return position + (rotation * scaled)
    }

    /**
     * Transforms [worldPoint] from OpenXR world space into this node's local coordinate space,
     * taking into account [position], [rotation], and [scale].
     *
     * @param worldPoint 3D coordinates in OpenXR world space.
     * @return 3D coordinates in this node's local space.
     */
    public fun transformPointFromWorld(worldPoint: Vector3): Vector3 {
        val unrotated = rotation.inverse * (worldPoint - position)
        val sx = if (abs(scale.x) > 1e-6f) scale.x else 1.0f
        val sy = if (abs(scale.y) > 1e-6f) scale.y else 1.0f
        val sz = if (abs(scale.z) > 1e-6f) scale.z else 1.0f
        return Vector3(unrotated.x / sx, unrotated.y / sy, unrotated.z / sz)
    }

    /**
     * Transforms [vector] from this node's local coordinate space into OpenXR world space, taking
     * into account [rotation] and [scale].
     *
     * @param vector Translation/displacement vector in this node's local space.
     * @return Translation/displacement vector in OpenXR world space.
     */
    public fun transformVectorToWorld(vector: Vector3): Vector3 {
        val scaled = vector.scale(scale)
        return rotation * scaled
    }

    /**
     * Transforms [worldVector] from OpenXR world space into this node's local coordinate space,
     * taking into account [rotation] and [scale].
     *
     * @param worldVector Translation/displacement vector in OpenXR world space.
     * @return Translation/displacement vector in this node's local space.
     */
    public fun transformVectorFromWorld(worldVector: Vector3): Vector3 {
        val unrotated = rotation.inverse * worldVector
        val sx = if (abs(scale.x) > 1e-6f) scale.x else 1.0f
        val sy = if (abs(scale.y) > 1e-6f) scale.y else 1.0f
        val sz = if (abs(scale.z) > 1e-6f) scale.z else 1.0f
        return Vector3(unrotated.x / sx, unrotated.y / sy, unrotated.z / sz)
    }

    // --- Geometry & Metric Bounds ---

    public val surfaceBoundsInMeters: Vector2?
        get() {
            val raw = attributes["root2DSurfaceBoundsInMeters"]
            if (raw != null && raw.size >= 2 && raw[0] > 0f && raw[1] > 0f) {
                return Vector2(raw[0], raw[1])
            }
            if (isSurfaceContainer) {
                val orderedChildren = children.filterNot { it.isWindowLeash || it.isSysUi }
                for (child in orderedChildren) {
                    val childBounds = child.surfaceBoundsInMeters
                    if (childBounds != null) return childBounds
                }
            }
            return null
        }

    public val surfaceBoundsInPixels: Vector2?
        get() {
            val rawPixels = attributes["root2DSurfaceBoundsInPixels"]
            if (
                rawPixels != null && rawPixels.size >= 2 && rawPixels[0] > 0f && rawPixels[1] > 0f
            ) {
                return Vector2(rawPixels[0], rawPixels[1])
            }
            val rawTouch = attributes["touchableRegion"]
            if (rawTouch != null && rawTouch.size >= 4) {
                val w = abs(rawTouch[2] - rawTouch[0])
                val h = abs(rawTouch[3] - rawTouch[1])
                if (w > 0f && h > 0f) {
                    return Vector2(w, h)
                }
            }
            bufferSize?.let {
                return it
            }
            if (isSurfaceContainer) {
                val orderedChildren = children.filterNot { it.isWindowLeash || it.isSysUi }
                for (child in orderedChildren) {
                    val childPixels = child.surfaceBoundsInPixels
                    if (childPixels != null) return childPixels
                }
            }
            return null
        }

    public val hasSurfaceBounds: Boolean
        get() = surfaceBoundsInMeters != null || surfaceBoundsInPixels != null

    public val touchableRegion: Rect?
        get() {
            val raw = attributes["touchableRegion"]
            if (raw != null && raw.size >= 4) {
                return Rect(raw[0].toInt(), raw[1].toInt(), raw[2].toInt(), raw[3].toInt())
            }
            if (isSurfaceContainer) {
                val orderedChildren = children.filterNot { it.isWindowLeash || it.isSysUi }
                for (child in orderedChildren) {
                    val childTouch = child.touchableRegion
                    if (childTouch != null) return childTouch
                }
            }
            return null
        }

    public val bufferSize: Vector2?
        get() {
            val bufferProp = properties["buffer"]
            if (bufferProp != null && bufferProp.contains("x") && bufferProp != "null") {
                val parts = bufferProp.split("x")
                if (parts.size == 2) {
                    val w = parts[0].toFloatOrNull()
                    val h = parts[1].toFloatOrNull()
                    if (w != null && h != null && w > 0f && h > 0f) {
                        return Vector2(w, h)
                    }
                }
            }
            return null
        }

    /**
     * Explicit effective 2D surface bounds in meters (`width, height`), delegating to
     * [primarySurfaceNode] when queried on a window leash container.
     */
    public val effectiveSurfaceBoundsInMeters: Vector2?
        get() = surfaceBoundsInMeters ?: primarySurfaceNode?.surfaceBoundsInMeters

    /**
     * Explicit effective 2D surface bounds in pixels (`width, height`), delegating to
     * [primarySurfaceNode] when queried on a window leash container.
     */
    public val effectiveSurfaceBoundsInPixels: Vector2?
        get() = surfaceBoundsInPixels ?: primarySurfaceNode?.surfaceBoundsInPixels

    /**
     * Explicit effective 2D buffer size in pixels (`width, height`), delegating to
     * [primarySurfaceNode] when queried on a window leash container.
     */
    public val effectiveBufferSize: Vector2?
        get() = bufferSize ?: primarySurfaceNode?.bufferSize

    public val worldExtents: Vector3?
        get() {
            val bounds = surfaceBoundsInMeters
            val parentNode = parent
            val effectiveScale =
                if (isAndroid2d && parentNode != null && parentNode.isSurfaceContainer) {
                    parentNode.scale
                } else {
                    scale
                }
            if (bounds != null && bounds.x > 0f && bounds.y > 0f) {
                return Vector3(
                    bounds.x * effectiveScale.x,
                    bounds.y * effectiveScale.y,
                    if (effectiveScale.z > 0f) effectiveScale.z else 0.01f,
                )
            }
            if (isSurfaceContainer) {
                val orderedChildren = children.filterNot { it.isWindowLeash || it.isSysUi }
                for (child in orderedChildren) {
                    val childExtents = child.worldExtents
                    if (childExtents != null) return childExtents
                }
            }
            if (isInputTarget && effectiveScale.x > 0f && effectiveScale.y > 0f) {
                return effectiveScale
            }
            return null
        }

    public val worldHalfExtents: Vector3?
        get() = worldExtents?.div(2f)

    /**
     * Effective virtual pixel density in pixels per meter (`computedPixelsPerMeter` or positive
     * `pixelsPerMeter` in `dumpsys spf_cpm`), inheriting from [parent] if not explicitly present on
     * this node, or `null` if unavailable.
     */
    public val computedPixelsPerMeter: Float?
        get() =
            properties["computedPixelsPerMeter"]?.toFloatOrNull()?.takeIf { it > 0f }
                ?: properties["pixelsPerMeter"]?.toFloatOrNull()?.takeIf { it > 0f }
                ?: parent?.computedPixelsPerMeter

    // --- System Flags & Capabilities ---

    public val canReceiveInput: Boolean
        get() = properties["canReceiveInput"]?.toBoolean() ?: true

    public val isAndroid2d: Boolean
        get() = properties["isAndroid2d"]?.toBoolean() ?: false

    public val isEmbeddedWindowLeash: Boolean
        get() = properties["isAppSpatialEmbeddedWindowLeash"]?.toBoolean() ?: false

    public val isSysUi: Boolean
        get() = properties["isSysUi"]?.toBoolean() ?: false

    /** True if this node hosts a 3D subspace (`hasSubspace=true` in CPM). */
    public val hasSubspace: Boolean
        get() = properties["hasSubspace"]?.toBoolean() ?: false

    /**
     * True if this node is a 2D SurfaceFlinger buffer quad layer (`isAndroid2d=true` with a buffer
     * or surface bounds).
     */
    public val isAndroid2dSurface: Boolean
        get() = isAndroid2d && (bufferSize != null || hasSurfaceBounds)

    /**
     * True if this node is a CPM/WindowManager task window wrapper node (`window-task-<id>` or
     * `task-<id>-window`).
     */
    public val isTaskWindowWrapper: Boolean
        get() = name.startsWith("window-task-") || TASK_WINDOW_NODE_REGEX.matches(name)

    /**
     * True if this node is the container leash for the task's primary window, identified by
     * directly hosting the WindowManager/CPM `window-task-<taskId>` node.
     */
    public val isTaskWindowLeash: Boolean
        get() = children.any { it.name.startsWith("window-task-") }

    /**
     * True if this node is the container leash for an embedded Activity task window, identified by
     * directly hosting a WindowManager/CPM `task-<id>-window` node.
     */
    public val isActivityWindowLeash: Boolean
        get() = children.any { TASK_WINDOW_NODE_REGEX.matches(it.name) }

    /**
     * True if this node is any spatial window leash container in CPM ([isTaskWindowLeash],
     * [isActivityWindowLeash], or [isEmbeddedWindowLeash]).
     */
    public val isWindowLeash: Boolean
        get() = isTaskWindowLeash || isActivityWindowLeash || isEmbeddedWindowLeash

    /**
     * The closest ancestor node that is a spatial window leash ([isWindowLeash]), or `null` if this
     * node is not nested inside another window leash.
     */
    public val parentWindowLeash: SpatialNode?
        get() {
            var current = parent
            while (current != null) {
                if (current.isWindowLeash) return current
                current = current.parent
            }
            return null
        }

    /**
     * True if this node is a top-level window leash container in the task (i.e. [isWindowLeash] is
     * true and [parentWindowLeash] is `null`).
     */
    public val isTopLevelWindowLeash: Boolean
        get() = isWindowLeash && parentWindowLeash == null

    /**
     * Returns this node's own 2D surface/buffer node ([isAndroid2dSurface]).
     *
     * In CPM, a node's 2D surface is either:
     * - This node itself (if [isAndroid2dSurface] is true),
     * - A direct child of this node (e.g. `#<layerId>` under an embedded window leash), or
     * - A direct child of this node's direct [isTaskWindowWrapper] child (e.g.
     *   `<pkg>/<activity>#<id>` under `window-task-<id>` or `task-<id>-window`).
     */
    public val primarySurfaceNode: SpatialNode?
        get() {
            if (isAndroid2dSurface) return this
            children
                .firstOrNull { it.isAndroid2dSurface && !it.isSysUi }
                ?.let {
                    return it
                }
            children
                .firstOrNull { it.isTaskWindowWrapper }
                ?.children
                ?.firstOrNull { it.isAndroid2dSurface && !it.isSysUi }
                ?.let {
                    return it
                }
            return null
        }

    /**
     * Returns this node's own hit-testable input target node (the [primarySurfaceNode] if it has an
     * `input-id`, or `this` if directly hit-testable).
     */
    public val ownInputTarget: SpatialNode?
        get() = if (isInputTarget) this else primarySurfaceNode?.takeIf { it.isInputTarget }

    /**
     * Embedded window leashes (`isEmbeddedWindowLeash=true`) directly or indirectly attached inside
     * this node whose closest enclosing window leash is `this`.
     */
    public val childWindowLeashes: List<SpatialNode>
        get() = findNodes { it != this && it.isEmbeddedWindowLeash && it.parentWindowLeash == this }

    /** Special node ID from `sysUIMetadata=<specialNode=...>` if present, or `null`. */
    public val sysUiSpecialNode: Int?
        get() {
            val metadata = properties["sysUIMetadata"] ?: return null
            val match = SYS_UI_SPECIAL_NODE_REGEX.find(metadata)
            return match?.groupValues?.get(1)?.toIntOrNull()
        }

    internal val scrimTargetAlpha: Float
        get() {
            val metadata = properties["sysUIMetadata"] ?: return 0f
            val match = SCRIM_TARGET_ALPHA_REGEX.find(metadata) ?: return 0f
            return match.groupValues[1].toFloatOrNull() ?: 0f
        }

    /**
     * All SysUI affordance/decoration nodes (`isSysUi=true` or `sysUiSpecialNode != null`) directly
     * associated with this node (direct children of this node, its [isTaskWindowWrapper], or its
     * [primarySurfaceNode]).
     */
    public val sysUiAffordances: List<SpatialNode>
        get() {
            val candidateParents =
                listOfNotNull(
                        this,
                        children.firstOrNull { it.isTaskWindowWrapper },
                        primarySurfaceNode,
                    )
                    .distinct()
            return candidateParents
                .flatMap { it.children }
                .filter { it.isSysUi || it.sysUiSpecialNode != null }
        }

    /**
     * True if this node represents a system caption bar / task bar decoration (`Caption` or
     * `TaskBar`).
     */
    public val isCaptionBar: Boolean
        get() =
            name.contains("Caption", ignoreCase = true) ||
                name.contains("TaskBar", ignoreCase = true)

    /**
     * True if this node is a system move/drag affordance (`isSysUi=true` and name contains `Move`
     * or `Drag`). Note: Caption bars ([isCaptionBar]) are excluded as they do not react to move
     * actions.
     */
    public val isMoveAffordance: Boolean
        get() =
            isSysUi &&
                (name.contains("Move", ignoreCase = true) ||
                    name.contains("Drag", ignoreCase = true))

    /**
     * True if this node is a system resize handle/corner affordance (`isSysUi=true` and name
     * contains `Resize`).
     */
    public val isResizeAffordance: Boolean
        get() = isSysUi && name.contains("Resize", ignoreCase = true)

    /** Move affordance nodes directly associated with this node. */
    public val moveAffordanceNodes: List<SpatialNode>
        get() = sysUiAffordances.filter { it.isMoveAffordance }

    /** Resize affordance nodes directly associated with this node. */
    public val resizeAffordanceNodes: List<SpatialNode>
        get() = sysUiAffordances.filter { it.isResizeAffordance }

    public val inputId: Int
        get() = properties["input-id"]?.toIntOrNull() ?: -1

    public val isInputTarget: Boolean
        get() = inputId >= 0 && canReceiveInput

    private val reformBitmask: Int
        get() {
            val reform =
                properties["reformOptions"] ?: parent?.properties?.get("reformOptions") ?: return 0
            val enableMatch = REFORM_ENABLE_REGEX.find(reform)
            return enableMatch?.groupValues?.get(1)?.toIntOrNull() ?: 0
        }

    public val isMovable: Boolean
        get() = (reformBitmask and 1) != 0

    public val isResizable: Boolean
        get() = (reformBitmask and 2) != 0

    public val stereoVisibility: String?
        get() = properties["stereoVisibility"]

    public val isVisible: Boolean
        get() {
            if (
                header.contains("hidden") ||
                    properties["requested-vis"] == "hide" ||
                    stereoVisibility == "hidden on both"
            ) {
                return false
            }
            val p = parent
            if (p != null && p.name != "Root") {
                return p.isVisible
            }
            return true
        }

    private val hasOwnRenderedSurface: Boolean
        get() {
            if (bufferSize != null) return true
            val meters = attributes["root2DSurfaceBoundsInMeters"]
            if (meters != null && meters.size >= 2 && meters[0] > 0f && meters[1] > 0f) {
                return true
            }
            val pixels = attributes["root2DSurfaceBoundsInPixels"]
            if (pixels != null && pixels.size >= 2 && pixels[0] > 0f && pixels[1] > 0f) {
                return true
            }
            return false
        }

    /**
     * True if this node represents a toast overlay that can actually render onto the display.
     * Pre-allocated `MIRROR_TOAST_AREA` containers (for example on Moohan where the container node
     * stays `visible on both` with `buffer=null` and `ImpNodeIsActive=false` while empty) are
     * ignored unless they or a visible active descendant have an active toast node or rendered
     * surface.
     */
    internal val isVisibleToastOverlay: Boolean
        get() {
            if (!isVisible) return false
            if (name.startsWith("Toast#")) {
                return properties["ImpNodeIsActive"] != "false"
            }
            if (name == "MIRROR_TOAST_AREA") {
                if (properties["ImpNodeIsActive"] != "false" && hasOwnRenderedSurface) {
                    return true
                }
                return getDescendants().any { child ->
                    child.isVisible &&
                        child.properties["ImpNodeIsActive"] != "false" &&
                        (child.name.startsWith("Toast#") || child.hasOwnRenderedSurface)
                }
            }
            return false
        }

    /**
     * Whether this node is an Android task root reference space (`space-root-task-<taskId>`).
     *
     * In Android XR CPM dumpsys, task roots are created directly under the compositor `Root` node
     * by Window Manager.
     */
    public val isTaskRoot: Boolean
        get() = name.startsWith("space-root-task-")

    public val taskId: Int?
        get() =
            if (isTaskRoot) {
                name.removePrefix("space-root-task-").substringBefore(' ').toIntOrNull()
            } else {
                parent?.taskId
            }

    /**
     * The root reference space container node (`space-root-task-<taskId>`) for the task this node
     * belongs to, or this node itself if it is the task root, or null if not within a task tree.
     */
    public val activitySpaceRoot: SpatialNode?
        get() = if (isTaskRoot) this else parent?.activitySpaceRoot

    internal val isSurfaceContainer: Boolean
        get() =
            isWindowLeash ||
                isTaskWindowWrapper ||
                name.startsWith("window-") ||
                attributes.containsKey("root2DSurfaceBoundsInMeters") ||
                children.any { it.bufferSize != null || it.isAndroid2d }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SpatialNode) return false
        return id == other.id &&
            name == other.name &&
            depth == other.depth &&
            header == other.header
    }

    override fun hashCode(): Int {
        var result = id
        result = 31 * result + name.hashCode()
        result = 31 * result + depth
        result = 31 * result + header.hashCode()
        return result
    }

    override fun toString(): String =
        "SpatialNode(name='$name', id=$id, depth=$depth, pos=$position, children=${children.size})"

    /**
     * Detects the bounds of this [SpatialNode]'s rendered content within [bitmap] (projected
     * relative to the user's head pose) and crops the resulting [Bitmap] to the node's content
     * bounds (plus optional [padding]).
     *
     * @param bitmap Source [Bitmap] from the XR compositor.
     * @param padding Extra padding in pixels to preserve around the cropped content bounds.
     * @return A software-backed [Bitmap.Config.ARGB_8888] [Bitmap] containing the cropped content
     *   of this [SpatialNode].
     */
    @JvmOverloads
    public fun crop(
        bitmap: Bitmap,
        padding: Int = 0,
    ): Bitmap {
        val softwareBitmap = toSoftwareBitmap(bitmap)
        val width = softwareBitmap.width
        val height = softwareBitmap.height
        val pixels = toPixelArray(softwareBitmap)
        val bgColor = detectBackgroundColor(pixels, width, height)
        val fullBounds = Rect(0, 0, width, height)
        val overallBounds =
            findForegroundBounds(pixels, width, height, fullBounds, bgColor)
                ?: return softwareBitmap

        val fraction = computeRelativeNodeBoundsFraction(this)
        val isFullFraction =
            fraction.left <= 0.001f &&
                fraction.top <= 0.001f &&
                fraction.right >= 0.999f &&
                fraction.bottom >= 0.999f
        val tightBounds =
            if (isFullFraction) {
                overallBounds
            } else {
                val w = overallBounds.width()
                val h = overallBounds.height()
                val searchBounds =
                    Rect(
                        (overallBounds.left + (fraction.left * w).toInt()).coerceAtLeast(
                            overallBounds.left
                        ),
                        (overallBounds.top + (fraction.top * h).toInt()).coerceAtLeast(
                            overallBounds.top
                        ),
                        (overallBounds.left + ceil((fraction.right * w).toDouble()).toInt())
                            .coerceAtMost(overallBounds.right),
                        (overallBounds.top + ceil((fraction.bottom * h).toDouble()).toInt())
                            .coerceAtMost(overallBounds.bottom),
                    )
                findForegroundBounds(
                    pixels,
                    width,
                    height,
                    searchBounds,
                    bgColor,
                ) ?: searchBounds
            }

        val safePadding = max(0, padding)
        val finalBounds =
            Rect(
                (tightBounds.left - safePadding).coerceAtLeast(0),
                (tightBounds.top - safePadding).coerceAtLeast(0),
                (tightBounds.right + safePadding).coerceAtMost(width),
                (tightBounds.bottom + safePadding).coerceAtMost(height),
            )

        val cropped =
            try {
                crop(softwareBitmap, finalBounds)
            } catch (t: Throwable) {
                if (softwareBitmap !== bitmap && !softwareBitmap.isRecycled) {
                    softwareBitmap.recycle()
                }
                throw t
            }
        if (cropped !== softwareBitmap && softwareBitmap !== bitmap && !softwareBitmap.isRecycled) {
            softwareBitmap.recycle()
        }
        return cropped
    }

    /**
     * Captures the 3D XR compositor output and automatically crops the screenshot to the rendered
     * bounds of this [SpatialNode], accounting for its position relative to the user's head pose.
     *
     * @param padding Extra padding in pixels to preserve around the cropped content bounds.
     * @param timeout Maximum duration to wait for consecutive frames to stabilize.
     * @param pollInterval Delay between successive compositor captures.
     * @param uiAutomation The [UiAutomation] instance used to capture the compositor frame.
     * @return A cropped software-backed [Bitmap.Config.ARGB_8888] [Bitmap] owned by the caller.
     * @throws AssertionError if no screenshot could be captured within [timeout].
     */
    @JvmOverloads
    public fun captureToBitmap(
        padding: Int = 0,
        timeout: Duration = 5.seconds,
        pollInterval: Duration = 100.milliseconds,
        uiAutomation: UiAutomation = InstrumentationRegistry.getInstrumentation().uiAutomation,
    ): Bitmap {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        }
        SpatialSceneHelper.captureSceneWithPolling(
            uiAutomation = uiAutomation,
            maxWaitMs = max(MIN_SCREENSHOT_SCENE_SETTLE_WAIT_MS, timeout.inWholeMilliseconds),
        )
        val settledBitmap = waitForSettledSoftwareScreenshot(uiAutomation, timeout, pollInterval)
        val processed =
            try {
                crop(
                    bitmap = settledBitmap,
                    padding = padding,
                )
            } catch (t: Throwable) {
                settledBitmap.recycle()
                throw t
            }
        if (processed !== settledBitmap && !settledBitmap.isRecycled) {
            settledBitmap.recycle()
        }
        return processed
    }

    internal companion object {
        private val REFORM_ENABLE_REGEX = Regex("enable=(\\d+)")
        private val SYS_UI_SPECIAL_NODE_REGEX = Regex("specialNode=(\\d+)")
        private val SCRIM_TARGET_ALPHA_REGEX = Regex("targetAlpha=([0-9.+-]+)")
        private val TASK_WINDOW_NODE_REGEX = Regex("^task-\\d+-window$")
    }
}

private const val SCENE_CAPTURE_TAG = "SpatialSceneHelper"
private const val DEFAULT_SCENE_POLL_MAX_WAIT_MS = 3000L
private const val MIN_SCREENSHOT_SCENE_SETTLE_WAIT_MS = 5000L
private const val FRAME_STABILITY_MSSIM_THRESHOLD: Double = 0.999

private fun collectGeometryNodes(root: SpatialNode): List<SpatialNode> = root.findNodes { node ->
    node.isVisible &&
        !node.isSysUi &&
        (node.worldExtents != null || node.effectiveSurfaceBoundsInMeters != null)
}

private fun SpatialNode.effectiveWorldExtents(): Vector3? {
    worldExtents?.let {
        return it
    }
    val bounds = effectiveSurfaceBoundsInMeters ?: return null
    return Vector3(
        bounds.x * scale.x,
        bounds.y * scale.y,
        if (scale.z > 0f) scale.z else 0.01f,
    )
}

private fun computeRelativeNodeBoundsFraction(node: SpatialNode): RectF {
    if (node.isTaskRoot || node.name.startsWith("space-transform-spatial-")) {
        return RectF(0f, 0f, 1f, 1f)
    }
    val contextRoot = node.activitySpaceRoot ?: node.root
    if (contextRoot === node) {
        return RectF(0f, 0f, 1f, 1f)
    }

    val targetNodes = collectGeometryNodes(node).ifEmpty { listOf(node) }
    val allNodes = collectGeometryNodes(contextRoot)
    if (allNodes.isEmpty() || allNodes.size <= targetNodes.size) {
        return RectF(0f, 0f, 1f, 1f)
    }

    val head = node.headPose ?: Pose.Identity
    val viewRotInv = head.rotation.inverse

    fun computeBoundsInHead(nodes: List<SpatialNode>): FloatArray? {
        var minX = Float.POSITIVE_INFINITY
        var maxX = Float.NEGATIVE_INFINITY
        var minY = Float.POSITIVE_INFINITY
        var maxY = Float.NEGATIVE_INFINITY
        var count = 0
        for (n in nodes) {
            val ext = n.effectiveWorldExtents() ?: continue
            val center = viewRotInv * (n.position - head.translation)
            val depth = (-center.z).coerceAtLeast(0.01f)
            val projX = center.x / depth
            val projY = center.y / depth

            val localRot = viewRotInv * n.rotation
            val rx = localRot * Vector3(ext.x / 2f, 0f, 0f)
            val ry = localRot * Vector3(0f, ext.y / 2f, 0f)
            val halfW = (abs(rx.x) + abs(ry.x)) / depth
            val halfH = (abs(rx.y) + abs(ry.y)) / depth

            if (projX - halfW < minX) minX = projX - halfW
            if (projX + halfW > maxX) maxX = projX + halfW
            if (projY - halfH < minY) minY = projY - halfH
            if (projY + halfH > maxY) maxY = projY + halfH
            count++
        }
        return if (count > 0) floatArrayOf(minX, minY, maxX, maxY) else null
    }

    val allBounds = computeBoundsInHead(allNodes) ?: return RectF(0f, 0f, 1f, 1f)
    val targetBounds = computeBoundsInHead(targetNodes) ?: return RectF(0f, 0f, 1f, 1f)

    val totalW = allBounds[2] - allBounds[0]
    val totalH = allBounds[3] - allBounds[1]
    if (totalW <= 1e-4f || totalH <= 1e-4f) {
        return RectF(0f, 0f, 1f, 1f)
    }

    val leftFrac = ((targetBounds[0] - allBounds[0]) / totalW).coerceIn(0f, 1f)
    val rightFrac = ((targetBounds[2] - allBounds[0]) / totalW).coerceIn(0f, 1f)
    val topFrac = ((allBounds[3] - targetBounds[3]) / totalH).coerceIn(0f, 1f)
    val bottomFrac = ((allBounds[3] - targetBounds[1]) / totalH).coerceIn(0f, 1f)

    if (rightFrac - leftFrac < 1e-3f || bottomFrac - topFrac < 1e-3f) {
        return RectF(0f, 0f, 1f, 1f)
    }
    return RectF(leftFrac, topFrac, rightFrac, bottomFrac)
}

private fun logUnsettledTimeout(timeout: Duration) {
    Log.w(
        SCENE_CAPTURE_TAG,
        "Compositor frames did not stabilize within $timeout; returning the latest frame. " +
            "The screenshot may contain in-flight animation or rendering.",
    )
}

/**
 * Polls [UiAutomation.takeScreenshot] until two consecutive compositor frames match within
 * [FRAME_STABILITY_MSSIM_THRESHOLD] (ensuring newly registered SpaceFlinger/Impress buffers have
 * finished latching), or returns the latest non-null frame (logging a warning) if the timeout
 * elapses. All intermediate bitmaps are recycled before returning.
 */
private fun waitForSettledSoftwareScreenshot(
    uiAutomation: UiAutomation,
    timeout: Duration,
    pollInterval: Duration,
): Bitmap {
    val start = TimeSource.Monotonic.markNow()
    val stabilityMatcher = MSSIMMatcher(threshold = FRAME_STABILITY_MSSIM_THRESHOLD)
    var previousBitmap: Bitmap? = null
    var previousPixels: IntArray? = null
    var currentPixels: IntArray? = null
    var currentBitmap: Bitmap? = null

    try {
        while (true) {
            val raw = uiAutomation.takeScreenshot()
            if (raw != null) {
                val softwareBitmap =
                    try {
                        toSoftwareBitmap(raw)
                    } finally {
                        if (isHardwareBitmap(raw) && !raw.isRecycled) {
                            raw.recycle()
                        }
                    }
                currentBitmap = softwareBitmap
                currentPixels = toPixelArray(softwareBitmap, currentPixels)
                val prev = previousBitmap
                val prevPix = previousPixels

                if (
                    prev != null &&
                        prevPix != null &&
                        prev.width == softwareBitmap.width &&
                        prev.height == softwareBitmap.height
                ) {
                    val matchResult =
                        stabilityMatcher.compareBitmaps(
                            expected = prevPix,
                            given = currentPixels,
                            width = softwareBitmap.width,
                            height = softwareBitmap.height,
                        )
                    prev.recycle()
                    previousBitmap = null
                    matchResult.diff?.recycle()
                    if (matchResult.matches) {
                        currentBitmap = null
                        return softwareBitmap
                    }
                    if (start.elapsedNow() >= timeout) {
                        logUnsettledTimeout(timeout)
                        currentBitmap = null
                        return softwareBitmap
                    }
                } else {
                    prev?.recycle()
                    previousBitmap = null
                    if (start.elapsedNow() >= timeout) {
                        logUnsettledTimeout(timeout)
                        currentBitmap = null
                        return softwareBitmap
                    }
                }

                previousBitmap = softwareBitmap
                val temp = previousPixels
                previousPixels = currentPixels
                currentPixels = temp
                currentBitmap = null
            } else if (start.elapsedNow() >= timeout) {
                previousBitmap?.let {
                    logUnsettledTimeout(timeout)
                    previousBitmap = null
                    return it
                }
                throw AssertionError(
                    "Timed out after $timeout waiting for UiAutomation.takeScreenshot() to " +
                        "return a non-null Bitmap"
                )
            }
            SystemClock.sleep(pollInterval.inWholeMilliseconds)
        }
    } catch (t: Throwable) {
        currentBitmap?.let {
            if (!it.isRecycled) it.recycle()
        }
        previousBitmap?.let {
            if (!it.isRecycled) it.recycle()
        }
        throw t
    }
}
