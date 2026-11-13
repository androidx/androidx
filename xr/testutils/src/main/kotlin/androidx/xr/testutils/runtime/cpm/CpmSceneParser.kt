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

package androidx.xr.testutils.runtime.cpm

import android.app.UiAutomation
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.annotation.VisibleForTesting
import androidx.test.platform.app.InstrumentationRegistry
import androidx.xr.testutils.SpatialNode
import androidx.xr.testutils.SpatialScene
import java.io.InputStream
import java.util.regex.Pattern

internal const val DEFAULT_SCENE_POLL_MAX_WAIT_MS = 3000L

internal fun captureSceneWithPolling(
    uiAutomation: UiAutomation,
    maxWaitMs: Long = DEFAULT_SCENE_POLL_MAX_WAIT_MS,
): SpatialScene {
    var scene: SpatialScene
    val startTime = System.currentTimeMillis()
    while (true) {
        val rawDump = executeSceneShellCommand("dumpsys spf_cpm", uiAutomation)
        val activitiesDump = executeSceneShellCommand("dumpsys activity activities", uiAutomation)
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
): SpatialScene = SpatialScene(rootNodes = parseRootNodes(rawDump), taskPackageMap = taskPackageMap)

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
 * Parses the output of `dumpsys activity activities` to build a map of task IDs to their associated
 * Android package names.
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

/** Executes a shell command via [uiAutomation] and returns the complete trimmed stdout output. */
internal fun executeSceneShellCommand(
    command: String,
    uiAutomation: UiAutomation? = null,
): String {
    return try {
        val automation =
            uiAutomation ?: InstrumentationRegistry.getInstrumentation().uiAutomation ?: return ""
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

private val ACTIVITY_RECORD_PATTERN =
    Pattern.compile(
        "ActivityRecord\\{[^}]*?\\s+([a-zA-Z0-9_]+(?:\\.[a-zA-Z0-9_]+)+)/[^}\\s]+\\s+t(\\d+)\\b"
    )
private val TASK_AFFINITY_PATTERN =
    Pattern.compile("Task\\{[^}]*?#(\\d+)[^}]*?\\bA=(?:\\d+:)?([a-zA-Z0-9_]+(?:\\.[a-zA-Z0-9_]+)+)")
private val TASK_COMPONENT_PATTERN =
    Pattern.compile("Task\\{[^}]*?#(\\d+)[^}]*?\\s+([a-zA-Z0-9_]+(?:\\.[a-zA-Z0-9_]+)+)/")
private val NODE_LINE_PATTERN =
    Pattern.compile("^\\s*Node\\s+(.+?)(?:\\s*\\([^)]*\\))?(?:\\s+id=(\\d+))?\\s*:?\\s*$")
private val ATTR_ARRAY_PATTERN = Pattern.compile("([a-zA-Z0-9_#.-]+)=\\(([^)]+)\\)")
private val REFORM_OPTIONS_PATTERN = Pattern.compile("reformOptions=<([^>]+)>")
private val SYS_UI_METADATA_PATTERN = Pattern.compile("sysUIMetadata=<([^>]+)>")
private val PROP_KEY_VALUE_PATTERN = Pattern.compile("([a-zA-Z0-9_#.-]+)=([a-zA-Z0-9_#.-]+)")
private val WHITESPACE_PATTERN = Pattern.compile("\\s+")
