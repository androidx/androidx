package androidx.ui.semantics

import androidx.ui.foundation.diagnostics.DiagnosticPropertiesBuilder
import androidx.ui.foundation.diagnostics.DiagnosticableTree
import androidx.ui.foundation.diagnostics.StringProperty

// / Provides hint values which override the default hints on supported
// / platforms.
// /
// / On iOS, these values are always ignored.
// @immutable
data class SemanticsHintOverrides(
        // / The hint text for a tap action.
        // /
        // / If null, the standard hint is used instead.
        // /
        // / The hint should describe what happens when a tap occurs, not the
        // / manner in which a tap is accomplished.
        // /
        // / Bad: 'Double tap to show movies'.
        // / Good: 'show movies'.
    val onTapHint: String?,
        // / The hint text for a long press action.
        // /
        // / If null, the standard hint is used instead.
        // /
        // / The hint should describe what happens when a long press occurs, not
        // / the manner in which the long press is accomplished.
        // /
        // / Bad: 'Double tap and hold to show tooltip'.
        // / Good: 'show tooltip'.
    val onLongPressHint: String?
) : DiagnosticableTree {

    init {
        assert(onTapHint != "")
        assert(onLongPressHint != "")
    }

    // / Whether there are any non-null hint values.
    val isNotEmpty
        get() = onTapHint != null || onLongPressHint != null

    // TODO(Migration): Not ported because of data class
//    override fun hashCode(): Int {
    // return ui.hashValues(onTapHint, onLongPressHint)
//    }

//    override fun equals(other: Any?): Boolean {
//    if (other.runtimeType != runtimeType)
//      return false;
//    final SemanticsHintOverrides typedOther = other;
//    return typedOther.onTapHint == onTapHint
//      && typedOther.onLongPressHint == onLongPressHint;
//    }

    override fun debugFillProperties(properties: DiagnosticPropertiesBuilder) {
        super.debugFillProperties(properties)
        properties.add(StringProperty("onTapHint", onTapHint, defaultValue = null))
        properties.add(StringProperty("onLongPressHint", onLongPressHint, defaultValue = null))
    }

    override fun toString() = toStringDiagnostic()
}