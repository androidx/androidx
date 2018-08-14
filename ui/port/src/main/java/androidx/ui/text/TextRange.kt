/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.ui.text

// / A range of characters in a string of text.
// @immutable
open class TextRange {
    // TODO(Migration/ryanmentley): Migrate to data class
//  /// Creates a text range.
//  ///
//  /// The [start] and [end] arguments must not be null. Both the [start] and
//  /// [end] must either be greater than or equal to zero or both exactly -1.
//  ///
//  /// Instead of creating an empty text range, consider using the [empty]
//  /// constant.
//  const TextRange({
//    @required this.start,
//    @required this.end
//  }) : assert(start != null && start >= -1),
//       assert(end != null && end >= -1);
//
//  /// A text range that starts and ends at offset.
//  ///
//  /// The [offset] argument must be non-null and greater than or equal to -1.
//  const TextRange.collapsed(int offset)
//    : assert(offset != null && offset >= -1),
//      start = offset,
//      end = offset;
//
//  /// A text range that contains nothing and is not in the text.
//  static const TextRange empty = TextRange(start: -1, end: -1);
//
//  /// The index of the first character in the range.
//  ///
//  /// If [start] and [end] are both -1, the text range is empty.
//  final int start;
//
//  /// The next index after the characters in this range.
//  ///
//  /// If [start] and [end] are both -1, the text range is empty.
//  final int end;
//
//  /// Whether this range represents a valid position in the text.
//  bool get isValid => start >= 0 && end >= 0;
//
//  /// Whether this range is empty (but still potentially placed inside the text).
//  bool get isCollapsed => start == end;
//
//  /// Whether the start of this range precedes the end.
//  bool get isNormalized => end >= start;
//
//  /// The text before this range.
//  String textBefore(String text) {
//    assert(isNormalized);
//    return text.substring(0, start);
//  }
//
//  /// The text after this range.
//  String textAfter(String text) {
//    assert(isNormalized);
//    return text.substring(end);
//  }
//
//  /// The text inside this range.
//  String textInside(String text) {
//    assert(isNormalized);
//    return text.substring(start, end);
//  }
//
//  @override
//  bool operator ==(dynamic other) {
//    if (identical(this, other))
//      return true;
//    if (other is! TextRange)
//      return false;
//    final TextRange typedOther = other;
//    return typedOther.start == start
//        && typedOther.end == end;
//  }
//
//  @override
//  int get hashCode => hashValues(
//    start.hashCode,
//    end.hashCode
//  );
//
//  @override
//  String toString() => 'TextRange(start: $start, end: $end)';
}