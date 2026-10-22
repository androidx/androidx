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

package androidx.xr.scenecore.openxr.smoketest

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Typeface
import android.view.Gravity
import android.view.View
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.LinearLayout
import android.widget.TextView
import androidx.xr.runtime.math.Vector3
import java.util.Locale

/**
 * The anchor list of [SceneCoreOpenXrLauncherActivity]: a row for each anchor, with what the
 * glasses report about it and buttons to detach or remove it.
 */
@SuppressLint("SetTextI18n")
internal class SceneCoreAnchorList(
    private val context: Context,
    private val onDetach: (id: Int) -> Unit,
    private val onRemove: (id: Int) -> Unit,
) {
    val view = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }

    /** The rows of [view], so that status updates only change their texts. */
    private val rows = mutableMapOf<Int, Row>()
    private var rowIds: List<Int>? = null

    /** Rebuilds the rows only when the anchors change, otherwise updates their texts. */
    fun bind(anchors: List<SceneCoreAnchorSpec>, statuses: Map<Int, SceneCoreAnchorStatus>) {
        val ids = anchors.map { it.id }
        if (ids != rowIds) {
            rowIds = ids
            view.removeAllViews()
            rows.clear()
            if (anchors.isEmpty()) {
                view.addView(
                    TextView(context).apply {
                        text = "No anchor yet"
                        textSize = TEXT_SIZE_SP
                    }
                )
            }
            anchors.forEach { anchor ->
                val row = Row(anchor.id)
                rows[anchor.id] = row
                view.addView(row.view)
            }
        }
        anchors.forEach { anchor -> rows[anchor.id]?.bind(anchor, statuses[anchor.id]) }
    }

    /** One anchor: what the glasses report about it, and its actions. */
    private inner class Row(id: Int) {
        private val stateSwatch = View(context)
        private val title =
            TextView(context).apply {
                textSize = TEXT_SIZE_SP
                setTypeface(null, Typeface.BOLD)
            }
        private val details = TextView(context).apply { textSize = DETAIL_TEXT_SIZE_SP }
        // Detaching an anchor that an AnchorSpace still uses is a misuse, offered to see what the
        // runtime does then.
        private val detachButton = context.button("Detach") { onDetach(id) }

        val view: View =
            LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(context.dp(8), context.dp(8), 0, context.dp(8))
                addView(
                    LinearLayout(context).apply {
                        orientation = LinearLayout.HORIZONTAL
                        gravity = Gravity.CENTER_VERTICAL
                        addView(
                            stateSwatch,
                            LinearLayout.LayoutParams(context.dp(16), context.dp(16)).apply {
                                marginEnd = context.dp(8)
                            },
                        )
                        addView(title, LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f))
                        addView(detachButton)
                        addView(context.button("Remove") { onRemove(id) })
                    }
                )
                addView(details)
            }

        fun bind(anchor: SceneCoreAnchorSpec, status: SceneCoreAnchorStatus?) {
            val state = status?.state ?: SceneCoreAnchorState.REQUESTED
            stateSwatch.setBackgroundColor(state.color)
            title.text = "Anchor ${anchor.id}: $state"
            details.text = buildString {
                append("Tracking: ${status?.trackingState ?: "-"}")
                appendLine("    XrSpace: ${describeNativeHandle(status?.nativeSpaceHandle)}")
                append("Position: ${status?.activitySpacePosition?.let { format(it) } ?: "-"}")
                append("    Origin updates: ${status?.originUpdates ?: 0}")
                if (status?.isDetached == true) {
                    appendLine()
                    append("The ARCore anchor was detached while the AnchorSpace used it.")
                }
            }
            detachButton.isEnabled = !anchor.isDetachRequested && !state.isFailedOrDisposed
        }

        private fun format(position: Vector3): String =
            String.format(Locale.US, "(%.2f, %.2f, %.2f)", position.x, position.y, position.z)
    }
}
