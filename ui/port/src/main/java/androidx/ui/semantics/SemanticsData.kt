package androidx.ui.semantics

import androidx.ui.describeEnum
import androidx.ui.engine.geometry.Rect
import androidx.ui.foundation.diagnostics.DiagnosticPropertiesBuilder
import androidx.ui.foundation.diagnostics.Diagnosticable
import androidx.ui.foundation.diagnostics.DiagnosticsProperty
import androidx.ui.foundation.diagnostics.DoubleProperty
import androidx.ui.foundation.diagnostics.EnumProperty
import androidx.ui.foundation.diagnostics.IterableProperty
import androidx.ui.foundation.diagnostics.MessageProperty
import androidx.ui.foundation.diagnostics.StringProperty
import androidx.ui.painting.matrixutils.TransformProperty
import androidx.ui.runtimeType
import androidx.ui.engine.text.TextDirection
import androidx.ui.services.text_editing.TextSelection
import androidx.ui.vectormath64.Matrix4

/**
 * Summary information about a [SemanticsNode] object.
 *
 * A semantics node might [SemanticsNode.mergeAllDescendantsIntoThisNode],
 * which means the individual fields on the semantics node don't fully describe
 * the semantics at that node. This data structure contains the full semantics
 * for the node.
 *
 * Typically obtained from [SemanticsNode.getSemanticsData].
 * The [flags], [actions], [label], and [Rect] arguments must not be null.
 *
 * If [label] is not empty, then [textDirection] must also not be null.
 */
// @immutable
data class SemanticsData(
    /** A bit field of [SemanticsFlag]s that apply to this node. */
    val flags: Int,

    /** A bit field of [SemanticsAction]s that apply to this node. */
    val actions: Int,

    /**
     * A textual description of this node.
     *
     * The reading direction is given by [textDirection].
     */
    val label: String,

    /**
     * The value that [value] will become after performing a
     * [SemanticsAction.increase] action.
     *
     * The reading direction is given by [textDirection].
     */
    val increasedValue: String,

    /**
     * A textual description for the current value of the node.
     *
     * The reading direction is given by [textDirection].
     */
    val value: String,

    /**
     * The value that [value] will become after performing a
     * [SemanticsAction.decrease] action.
     *
     * The reading direction is given by [textDirection].
     */
    val decreasedValue: String,

    /**
     * A brief description of the result of performing an action on this node.
     *
     * The reading direction is given by [textDirection].
     */
    val hint: String,

    /**
     * The reading direction for the text in [label], [value], [hint],
     * [increasedValue], and [decreasedValue].
     */
    val textDirection: TextDirection?,

    /** The bounding box for this node in its coordinate system. */
    val rect: Rect,

    /**
     * The currently selected text (or the position of the cursor) within [value]
     * if this node represents a text field.
     */
    val textSelection: TextSelection?,

    /**
     * Indicates the current scrolling position in logical pixels if the node is
     * scrollable.
     *
     * The properties [scrollExtentMin] and [scrollExtentMax] indicate the valid
     * in-range values for this property. The value for [scrollPosition] may
     * (temporarily) be outside that range, e.g. during an overscroll.
     *
     * See also:
     *
     *  * [ScrollPosition.pixels], from where this value is usually taken.
     */
    val scrollPosition: Double?,

    /**
     * Indicates the maximum in-range value for [scrollPosition] if the node is
     * scrollable.
     *
     * This value may be infinity if the scroll is unbound.
     *
     * See also:
     *
     *  * [ScrollPosition.maxScrollExtent], from where this value is usually taken.
     */
    val scrollExtentMax: Double?,

    /**
     * Indicates the minimum in-range value for [scrollPosition] if the node is
     * scrollable.
     *
     * This value may be infinity if the scroll is unbound.
     *
     * See also:
     *
     *  * [ScrollPosition.minScrollExtent], from where this value is usually taken.
     */
    val scrollExtentMin: Double?,

    /** The set of [SemanticsTag]s associated with this node. */
    val tags: Set<SemanticsTag>,

    /**
     * The transform from this node's coordinate system to its parent's coordinate system.
     *
     * By default, the transform is null, which represents the identity
     * transformation (i.e., that this node has the same coordinate system as its
     * parent).
     */
    val transform: Matrix4?
) : Diagnosticable {

    init {
        assert(label == "" || textDirection != null) {
            "A SemanticsData object with label $label had a null textDirection."
        }
        assert(value == "" || textDirection != null) {
            "A SemanticsData object with value $value had a null textDirection."
        }
        assert(hint == "" || textDirection != null) {
            "A SemanticsData object with hint $hint had a null textDirection."
        }
        assert(decreasedValue == "" || textDirection != null) {
            "A SemanticsData object with decreasedValue $decreasedValue had a null textDirection."
        }
        assert(increasedValue == "" || textDirection != null) {
            "A SemanticsData object with increasedValue $increasedValue had a null textDirection."
        }
    }

    override fun toStringShort(): String {
        return runtimeType().toString()
    }

    override fun toString(): String {
        return toStringDiagnostic()
    }

    override fun debugFillProperties(properties: DiagnosticPropertiesBuilder) {
        super.debugFillProperties(properties)
        properties.add(DiagnosticsProperty.create("rect", rect, showName = false))
        properties.add(
            TransformProperty(
                "transform",
                transform,
                showName = false,
                defaultValue = null
            )
        )
        val actionSummary: MutableList<String> = mutableListOf()
        for (action in SemanticsAction.values.values) {
            if ((actions and action.index) != 0)
                actionSummary.add(describeEnum(action))
        }
        properties.add(IterableProperty<String>("actions", actionSummary, ifEmpty = null))

        val flagSummary: MutableList<String> = mutableListOf()
        for (flag in SemanticsFlag.values.values) {
            if ((flags and flag.index) != 0)
                flagSummary.add(describeEnum(flag))
        }
        properties.add(IterableProperty<String>("flags", flagSummary, ifEmpty = null))
        properties.add(StringProperty("label", label, defaultValue = ""))
        properties.add(StringProperty("value", value, defaultValue = ""))
        properties.add(StringProperty("increasedValue", increasedValue, defaultValue = ""))
        properties.add(StringProperty("decreasedValue", decreasedValue, defaultValue = ""))
        properties.add(StringProperty("hint", hint, defaultValue = ""))
        properties.add(
            EnumProperty<TextDirection>(
                "textDirection",
                textDirection,
                defaultValue = null
            )
        )
        if (textSelection?.isValid == true)
            properties.add(
                MessageProperty(
                    "textSelection",
                    "[${textSelection.start}, ${textSelection.end}]"
                )
            )
        properties.add(
            DoubleProperty.create(
                "scrollExtentMin",
                scrollExtentMin,
                defaultValue = null
            )
        )
        properties.add(
            DoubleProperty.create(
                "scrollPosition",
                scrollPosition,
                defaultValue = null
            )
        )
        properties.add(
            DoubleProperty.create(
                "scrollExtentMax",
                scrollExtentMax,
                defaultValue = null
            )
        )
    }

    //  Not needed for data class
//  @override
//  bool operator ==(dynamic other) {
//    if (other is! SemanticsData)
//      return false;
//    val typedOther: SemanticsData = other;
//    return typedOther.flags == flags
//        && typedOther.actions == actions
//        && typedOther.label == label
//        && typedOther.value == value
//        && typedOther.increasedValue == increasedValue
//        && typedOther.decreasedValue == decreasedValue
//        && typedOther.hint == hint
//        && typedOther.textDirection == textDirection
//        && typedOther.rect == rect
//        && setEquals(typedOther.tags, tags)
//        && typedOther.textSelection == textSelection
//        && typedOther.scrollPosition == scrollPosition
//        && typedOther.scrollExtentMax == scrollExtentMax
//        && typedOther.scrollExtentMin == scrollExtentMin
//        && typedOther.transform == transform
//        && _sortedListsEqual(typedOther.customSemanticsActionIds, customSemanticsActionIds);
//    }

    //  Not needed for data class
//    override fun hashCode(): Int {
//    return ui.hashValues(
//      flags,
//      actions,
//      label,
//      value,
//      increasedValue,
//      decreasedValue,
//      hint,
//      textDirection,
//      rect,
//      tags,
//      textSelection,
//      scrollPosition,
//      scrollExtentMax,
//      scrollExtentMin,
//      transform,
//      ui.hashList(customSemanticsActionIds),
//    );
//    }

//  static Boolean _sortedListsEqual(List<int> left, List<int> right) {
//    if (left == null && right == null)
//      return true;
//    if (left != null && right != null) {
//      if (left.length != right.length)
//        return false;
//      for (int i = 0; i < left.length; i++)
//        if (left[i] != right[i])
//          return false;
//      return true;
//    }
//    return false;
//  }
}