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
package androidx.ui.text.style

// A direction in which text flows.
//
// Some languages are written from the left to the right (for example, English,
// Tamil, or Chinese), while others are written from the right to the left (for
// example Aramaic, Hebrew, or Urdu). Some are also written in a mixture, for
// example Arabic is mostly written right-to-left, with numerals written
// left-to-right.
//
// The text direction must be provided to APIs that render text or lay out
// boxes horizontally, so that they can determine which direction to start in:
// either right-to-left, [TextDirection.rtl]; or left-to-right,
// [TextDirection.ltr].
//
// ## Design discussion
//
// Flutter is designed to address the needs of applications written in any of
// the world's currently-used languages, whether they use a right-to-left or
// left-to-right writing direction. Flutter does not support other writing
// modes, such as vertical text or boustrophedon text, as these are rarely used
// in computer programs.
//
// It is common when developing user interface frameworks to pick a default
// text direction — typically left-to-right, the direction most familiar to the
// engineers working on the framework — because this simplifies the development
// of applications on the platform. Unfortunately, this frequently results in
// the platform having unexpected left-to-right biases or assumptions, as
// engineers will typically miss places where they need to support
// right-to-left text. This then results in bugs that only manifest in
// right-to-left environments.
//
// In an effort to minimize the extent to which Flutter experiences this
// category of issues, the lowest levels of the Flutter framework do not have a
// default text reading direction. Any time a reading direction is necessary,
// for example when text is to be displayed, or when a
// writing-direction-dependent value is to be interpreted, the reading
// direction must be explicitly specified. Where possible, such as in `switch`
// statements, the right-to-left case is listed first, to avoid the impression
// that it is an afterthought.
//
// At the higher levels (specifically starting at the widgets library), an
// ambient [Directionality] is introduced, which provides a default. Thus, for
// instance, a [Text] widget in the scope of a [MaterialApp] widget does not
// need to be given an explicit writing direction. The [Directionality.of]
// static method can be used to obtain the ambient text direction for a
// particular [BuildContext].
//
// ### Known left-to-right biases in Flutter
//
// Despite the design intent described above, certain left-to-right biases have
// nonetheless crept into Flutter's design. These include:
//
//  * The [Canvas] origin is at the top left, and the x-axis increases in a
//    left-to-right direction.
//
//  * The default localization in the widgets and material libraries is
//    American English, which is left-to-right.
//
// ### Visual properties vs directional properties
//
// Many classes in the Flutter framework are offered in two versions, a
// visually-oriented variant, and a text-direction-dependent variant. For
// example, [EdgeInsets] is described in terms of top, left, right, and bottom,
// while [EdgeInsetsDirectional] is described in terms of top, start, end, and
// bottom, where start and end correspond to right and left in right-to-left
// text and left and right in left-to-right text.
//
// There are distinct use cases for each of these variants.
//
// Text-direction-dependent variants are useful when developing user interfaces
// that should "flip" with the text direction. For example, a paragraph of text
// in English will typically be left-aligned and a quote will be indented from
// the left, while in Arabic it will be right-aligned and indented from the
// right. Both of these cases are described by the direction-dependent
// [TextAlign.start] and [EdgeInsetsDirectional.start].
//
// In contrast, the visual variants are useful when the text direction is known
// and not affected by the reading direction. For example, an application
// giving driving directions might show a "turn left" arrow on the left and a
// "turn right" arrow on the right — and would do so whether the application
// was localized to French (left-to-right) or Hebrew (right-to-left).
//
// In practice, it is also expected that many developers will only be
// targeting one language, and in that case it may be simpler to think in
// visual terms.
// The order of this enum must match the order of the values in TextDirection.h's TextDirection.
enum class TextDirection {
    // The text flows from right to left (e.g. Arabic, Hebrew).
    Ltr,
    // The text flows from left to right (e.g., English, French).
    Rtl
}