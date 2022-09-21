/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.constraintlayout.compose

import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.semantics.SemanticsPropertyReceiver
import androidx.constraintlayout.core.state.State.PARENT
import androidx.constraintlayout.core.widgets.ConstraintWidget
import androidx.constraintlayout.core.widgets.ConstraintWidgetContainer
import androidx.constraintlayout.core.widgets.HelperWidget
import org.json.JSONArray
import org.json.JSONObject

/**
 * [SemanticsPropertyKey] to test [DesignInfoProvider]
 */
val DesignInfoDataKey = SemanticsPropertyKey<DesignInfoProvider>("DesignInfoProvider")

/**
 * [SemanticsPropertyReceiver] to test [DesignInfoProvider]
 */
@PublishedApi
internal var SemanticsPropertyReceiver.designInfoProvider by DesignInfoDataKey

/**
 * Interface used for Studio tooling.
 *
 * Returns a json string with the constraints and bounding box for each ID in the system.
 */
interface DesignInfoProvider {
    fun getDesignInfo(startX: Int, startY: Int, args: String): String
}

private const val CONSTRAINTS_JSON_VERSION = 1

// These flags represent bit positions starting at 0
private const val CONSTRAINTS = 0
private const val BOUNDS = 1

internal fun parseConstraintsToJson(
    root: ConstraintWidgetContainer,
    state: State,
    startX: Int,
    startY: Int,
    args: String
): String {
    // TODO: Take arguments to filter specific information, eg: "BOUNDS_ONLY" would remove
    //  'constraints' and 'helperReferences' from the json
    // TODO: Add information on the render-time transforms, eg: transforms: { rotationZ: 10 }
    // The root id is not user defined, so we create one
    val rootId = PARENT.toString()
    val idToConstraintsJson = JSONObject()

    // Add bounds and constraints by default
    var withConstraints = true
    var withBounds = true

    args.toIntOrNull()?.let {
        withBounds = it shr BOUNDS == 1
        withConstraints = it shr CONSTRAINTS == 1
    }

    root.children.forEach { constraintWidget ->
        val constraintsInfoArray = JSONArray()
        val helperReferences = mutableListOf<String>()
        val isHelper = constraintWidget is HelperWidget
        val widgetId = constraintWidget.stringId

        if (isHelper) {
            addReferencesIds(constraintWidget as HelperWidget, helperReferences, root, rootId)
        }

        constraintWidget.anchors.forEach { anchor ->
            if (anchor.isConnected) {
                val targetWidget = anchor.target.owner
                val targetIsParent = root == targetWidget
                val targetIsHelper = targetWidget is HelperWidget
                val targetId = when {
                    targetIsParent -> rootId
                    targetIsHelper -> targetWidget.getHelperId(state)
                    else -> targetWidget.getRefId()
                }
                constraintsInfoArray.put(
                    JSONObject()
                        .put("originAnchor", anchor.type)
                        .put("targetAnchor", anchor.target!!.type)
                        .put("target", targetId)
                        .put("margin", anchor.margin)
                )
            }
        }

        idToConstraintsJson.putViewIdToBoundsAndConstraints(
            viewId = widgetId,
            boxJson = constraintWidget.boundsToJson(startX, startY),
            isHelper = constraintWidget is HelperWidget,
            isRoot = false,
            helperReferences = helperReferences,
            constraintsInfoArray = constraintsInfoArray,
            withConstraints = withConstraints,
            withBounds = withBounds
        )
    }
    idToConstraintsJson.putViewIdToBoundsAndConstraints(
        viewId = rootId,
        boxJson = root.boundsToJson(startX, startY),
        isHelper = false,
        isRoot = true,
        helperReferences = emptyList(),
        constraintsInfoArray = JSONArray(),
        withConstraints = withConstraints,
        withBounds = withBounds
    )
    return createDesignInfoJson(idToConstraintsJson)
}

private fun addReferencesIds(
    helperWidget: HelperWidget,
    helperReferences: MutableList<String>,
    root: ConstraintWidgetContainer,
    rootId: String
) {
    for (i in 0 until helperWidget.mWidgetsCount) {
        val referencedWidget = helperWidget.mWidgets[i]
        val referenceId = if (referencedWidget == root) rootId else referencedWidget.getRefId()
        helperReferences.add(referenceId)
    }
}

/**
 * Returns the Id used for HelperWidgets like barriers or guidelines. Blank if there's no Id.
 */
private fun ConstraintWidget.getHelperId(state: State): String =
    state.getKeyId(this as HelperWidget).toString()

/**
 * Returns the Id used for Composables within the layout. Blank if there's no Id.
 */
private fun ConstraintWidget?.getRefId(): String =
    (this?.companionWidget as? Measurable)?.layoutId?.toString() ?: this?.stringId.toString()

private fun createDesignInfoJson(content: JSONObject) = JSONObject()
    .put("type", "CONSTRAINTS")
    .put("version", CONSTRAINTS_JSON_VERSION)
    .put("content", content).toString()

private fun ConstraintWidget.boundsToJson(startX: Int, startY: Int) = JSONObject()
    .put("left", left + startX)
    .put("top", top + startY)
    .put("right", right + startX)
    .put("bottom", bottom + startY)

private fun JSONObject.putViewIdToBoundsAndConstraints(
    viewId: String,
    boxJson: JSONObject,
    isHelper: Boolean,
    isRoot: Boolean,
    helperReferences: List<String>,
    constraintsInfoArray: JSONArray,
    withConstraints: Boolean = true,
    withBounds: Boolean = true
) {
    val viewWithBoundsAndConstraints = JSONObject()
    viewWithBoundsAndConstraints.put("viewId", viewId)

    if (withBounds) {
        viewWithBoundsAndConstraints.put("box", boxJson)
    }
    viewWithBoundsAndConstraints.put("isHelper", isHelper)
    viewWithBoundsAndConstraints.put("isRoot", isRoot)

    val helperReferencesArray = JSONArray()
    helperReferences.forEach(helperReferencesArray::put)
    viewWithBoundsAndConstraints.put("helperReferences", helperReferencesArray)

    if (withConstraints) {
        viewWithBoundsAndConstraints.put("constraints", constraintsInfoArray)
    }
    put(viewId, viewWithBoundsAndConstraints)
}