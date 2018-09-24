package androidx.ui.text

import androidx.ui.engine.text.TextAffinity

// import 'dart:ui' show hashValues, TextAffinity, TextPosition;
//
// import 'package:flutter/foundation.dart';
//
// export 'dart:ui' show TextAffinity, TextPosition;

/** A range of text that represents a selection. */
// @immutable
data class TextSelection(
    /**
     * The offset at which the selection originates.
     *
     * Might be larger than, smaller than, or equal to extent.
     */
    val baseOffset: Int,

    /**
     * The offset at which the selection terminates.
     *
     * When the user uses the arrow keys to adjust the selection, this is the
     * value that changes. Similarly, if the current theme paints a caret on one
     * side of the selection, this is the location at which to paint the caret.
     *
     * Might be larger than, smaller than, or equal to base.
     */
    val extentOffset: Int,

    /**
     * If the text range is collapsed and has more than one visual location
     * (e.g., occurs at a line break), which of the two locations to use when
     * painting the caret.
     */
    val affinity: TextAffinity = TextAffinity.downstream,

    /**
     * Whether this selection has disambiguated its base and extent.
     *
     * On some platforms, the base and extent are not disambiguated until the
     * first time the user adjusts the selection. At that point, either the start
     * or the end of the selection becomes the base and the other one becomes the
     * extent and is adjusted.
     */
    val isDirectional: Boolean = false
) : TextRange(
    start = if (baseOffset < extentOffset) baseOffset else extentOffset,
    end = if (baseOffset < extentOffset) extentOffset else baseOffset
) {
    // TODO(Migration/ryanmentley): Migrate to data class
//
//  /// Creates a collapsed selection at the given offset.
//  ///
//  /// A collapsed selection starts and ends at the same offset, which means it
//  /// contains zero characters but instead serves as an insertion point in the
//  /// text.
//  ///
//  /// The [offset] argument must not be null.
//  const TextSelection.collapsed({
//    @required int offset,
//    this.affinity = TextAffinity.downstream
//  }) : baseOffset = offset, extentOffset = offset, isDirectional = false, super.collapsed(offset);
//
//  /// Creates a collapsed selection at the given text position.
//  ///
//  /// A collapsed selection starts and ends at the same offset, which means it
//  /// contains zero characters but instead serves as an insertion point in the
//  /// text.
//  TextSelection.fromPosition(TextPosition position)
//    : baseOffset = position.offset,
//      extentOffset = position.offset,
//      affinity = position.affinity,
//      isDirectional = false,
//      super.collapsed(position.offset);
//

//
//  /// The position at which the selection originates.
//  ///
//  /// Might be larger than, smaller than, or equal to extent.
//  TextPosition get base => new TextPosition(offset: baseOffset, affinity: affinity);
//
//  /// The position at which the selection terminates.
//  ///
//  /// When the user uses the arrow keys to adjust the selection, this is the
//  /// value that changes. Similarly, if the current theme paints a caret on one
//  /// side of the selection, this is the location at which to paint the caret.
//  ///
//  /// Might be larger than, smaller than, or equal to base.
//  TextPosition get extent => new TextPosition(offset: extentOffset, affinity: affinity);
//
//  @override
//  String toString() {
//    return '$runtimeType(baseOffset: $baseOffset, extentOffset: $extentOffset, affinity: $affinity, isDirectional: $isDirectional)';
//  }
//
//  @override
//  bool operator ==(dynamic other) {
//    if (identical(this, other))
//      return true;
//    if (other is! TextSelection)
//      return false;
//    final TextSelection typedOther = other;
//    return typedOther.baseOffset == baseOffset
//        && typedOther.extentOffset == extentOffset
//        && typedOther.affinity == affinity
//        && typedOther.isDirectional == isDirectional;
//  }
//
//  @override
//  int get hashCode => hashValues(
//    baseOffset.hashCode,
//    extentOffset.hashCode,
//    affinity.hashCode,
//    isDirectional.hashCode
//  );
//
//  /// Creates a new [TextSelection] based on the current selection, with the
//  /// provided parameters overridden.
//  TextSelection copyWith({
//    int baseOffset,
//    int extentOffset,
//    TextAffinity affinity,
//    bool isDirectional,
//  }) {
//    return new TextSelection(
//      baseOffset: baseOffset ?? this.baseOffset,
//      extentOffset: extentOffset ?? this.extentOffset,
//      affinity: affinity ?? this.affinity,
//      isDirectional: isDirectional ?? this.isDirectional,
//    );
//  }
}
