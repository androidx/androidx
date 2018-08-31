import androidx.ui.text.TextSelection

// //import 'dart:math' as math;
// //import 'dart:typed_data';
// //import 'dart:ui' as ui;
// //import 'dart:ui' show Offset, Rect, SemanticsAction, SemanticsFlag,
// //       TextDirection;
// //
// //import 'package:flutter/foundation.dart';
// //import 'package:flutter/painting.dart' show MatrixUtils, TransformProperty;
// //import 'package:flutter/services.dart';
// //import 'package:vector_math/vector_math_64.dart';
// //
// //import 'semantics_event.dart';
// //
// //export 'dart:ui' show SemanticsAction;
// //export 'semantics_event.dart';
//
// /// Signature for a function that is called for each [SemanticsNode].
// ///
// /// Return false to stop visiting nodes.
// ///
// /// Used by [SemanticsNode.visitChildren].
// typedef bool SemanticsNodeVisitor(SemanticsNode node);

// / Signature for [SemanticsAction]s that move the cursor.
// /
// / If `extendSelection` is set to true the cursor movement should extend the
// / current selection or (if nothing is currently selected) start a selection.
typealias MoveCursorHandler = (extendSelection: Boolean) -> Unit

// / Signature for the [SemanticsAction.setSelection] handlers to change the
// / text selection (or re-position the cursor) to `selection`.
typealias SetSelectionHandler = (selection: TextSelection) -> Unit

typealias _SemanticsActionHandler = (args: Any?) -> Unit

// String _concatStrings({
//  @required String thisString,
//  @required String otherString,
//  @required TextDirection thisTextDirection,
//  @required TextDirection otherTextDirection
// }) {
//  if (otherString.isEmpty)
//    return thisString;
//  String nestedLabel = otherString;
//  if (thisTextDirection != otherTextDirection && otherTextDirection != null) {
//    switch (otherTextDirection) {
//      case TextDirection.rtl:
//        nestedLabel = '${Unicode.RLE}$nestedLabel${Unicode.PDF}';
//        break;
//      case TextDirection.ltr:
//        nestedLabel = '${Unicode.LRE}$nestedLabel${Unicode.PDF}';
//        break;
//    }
//  }
//  if (thisString.isEmpty)
//    return nestedLabel;
//  return '$thisString\n$nestedLabel';
// }
