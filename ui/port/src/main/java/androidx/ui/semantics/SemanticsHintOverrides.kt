package androidx.ui.semantics

import androidx.ui.foundation.diagnostics.DiagnosticPropertiesBuilder
import androidx.ui.foundation.diagnostics.DiagnosticableTree

// / Provides hint values which override the default hints on supported
// / platforms.
// /
// / On iOS, these values are always ignored.
// @immutable
class SemanticsHintOverrides : DiagnosticableTree {
    // TODO(Migration/ryanmentley): Migrate to data class
//  /// Creates a semantics hint overrides.
//  const SemanticsHintOverrides({
//    this.onTapHint,
//    this.onLongPressHint,
//  }) : assert(onTapHint != ''),
//       assert(onLongPressHint != '');
//
//  /// The hint text for a tap action.
//  ///
//  /// If null, the standard hint is used instead.
//  ///
//  /// The hint should describe what happens when a tap occurs, not the
//  /// manner in which a tap is accomplished.
//  ///
//  /// Bad: 'Double tap to show movies'.
//  /// Good: 'show movies'.
//  final String onTapHint;
//
//  /// The hint text for a long press action.
//  ///
//  /// If null, the standard hint is used instead.
//  ///
//  /// The hint should describe what happens when a long press occurs, not
//  /// the manner in which the long press is accomplished.
//  ///
//  /// Bad: 'Double tap and hold to show tooltip'.
//  /// Good: 'show tooltip'.
//  final String onLongPressHint;
//
//  /// Whether there are any non-null hint values.
//  bool get isNotEmpty => onTapHint != null || onLongPressHint != null;
//

    override fun hashCode(): Int {
        TODO("Not implemented")
        // return ui.hashValues(onTapHint, onLongPressHint)
    }

    override fun equals(other: Any?): Boolean {
        TODO("Not implemented")
//    if (other.runtimeType != runtimeType)
//      return false;
//    final SemanticsHintOverrides typedOther = other;
//    return typedOther.onTapHint == onTapHint
//      && typedOther.onLongPressHint == onLongPressHint;
    }

    override fun debugFillProperties(properties: DiagnosticPropertiesBuilder) {
        TODO("Not implemented")
//    super.debugFillProperties(properties);
//    properties.add(new StringProperty('onTapHint', onTapHint, defaultValue: null));
//    properties.add(new StringProperty('onLongPressHint', onLongPressHint, defaultValue: null));
    }

    override fun toString() = toStringDiagnostic()
}