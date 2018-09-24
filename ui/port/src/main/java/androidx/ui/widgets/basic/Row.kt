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

package androidx.ui.widgets.basic

import androidx.ui.engine.text.TextBaseline
import androidx.ui.engine.text.TextDirection
import androidx.ui.foundation.Key
import androidx.ui.painting.basictypes.Axis
import androidx.ui.painting.basictypes.VerticalDirection
import androidx.ui.rendering.flex.CrossAxisAlignment
import androidx.ui.rendering.flex.MainAxisAlignment
import androidx.ui.rendering.flex.MainAxisSize
import androidx.ui.widgets.framework.Widget

/**
 * A widget that displays its children in a horizontal array.
 *
 * To cause a child to expand to fill the available horizontal space, wrap the
 * child in an [Expanded] widget.
 *
 * The [Row] widget does not scroll (and in general it is considered an error
 * to have more children in a [Row] than will fit in the available room). If
 * you have a line of widgets and want them to be able to scroll if there is
 * insufficient room, consider using a [ListView].
 *
 * For a vertical variant, see [Column].
 *
 * If you only have one child, then consider using [Align] or [Center] to
 * position the child.
 *
 * ## Sample code
 *
 * This example divides the available space into three (horizontally), and
 * places text centered in the first two cells and the Flutter logo centered in
 * the third:
 *
 * ```dart
 * new Row(
 *   children: <Widget>[
 *     new Expanded(
 *       child: new Text('Deliver features faster', textAlign: TextAlign.center),
 *     ),
 *     new Expanded(
 *       child: new Text('Craft beautiful UIs', textAlign: TextAlign.center),
 *     ),
 *     new Expanded(
 *       child: new FittedBox(
 *         fit: BoxFit.contain, // otherwise the logo will be tiny
 *         child: const FlutterLogo(),
 *       ),
 *     ),
 *   ],
 * )
 * ```
 *
 * ## Troubleshooting
 *
 * ### Why does my row have a yellow and black warning stripe?
 *
 * If the non-flexible contents of the row (those that are not wrapped in
 * [Expanded] or [Flexible] widgets) are together wider than the row itself,
 * then the row is said to have overflowed. When a row overflows, the row does
 * not have any remaining space to share between its [Expanded] and [Flexible]
 * children. The row reports this by drawing a yellow and black striped
 * warning box on the edge that is overflowing. If there is room on the outside
 * of the row, the amount of overflow is printed in red lettering.
 *
 * #### Story time
 *
 * Suppose, for instance, that you had this code:
 *
 * ```dart
 * new Row(
 *   children: <Widget>[
 *     const FlutterLogo(),
 *     const Text('Flutter\'s hot reload helps you quickly and easily experiment, build UIs, add features, and fix bug faster. Experience sub-second reload times, without losing state, on emulators, simulators, and hardware for iOS and Android.'),
 *     const Icon(Icons.sentiment_very_satisfied),
 *   ],
 * )
 * ```
 *
 * The row first asks its first child, the [FlutterLogo], to lay out, at
 * whatever size the logo would like. The logo is friendly and happily decides
 * to be 24 pixels to a side. This leaves lots of room for the next child. The
 * row then asks that next child, the text, to lay out, at whatever size it
 * thinks is best.
 *
 * At this point, the text, not knowing how wide is too wide, says "Ok, I will
 * be thiiiiiiiiiiiiiiiiiiiis wide.", and goes well beyond the space that the
 * row has available, not wrapping. The row responds, "That's not fair, now I
 * have no more room available for my other children!", and gets angry and
 * sprouts a yellow and black strip.
 *
 * The fix is to wrap the second child in an [Expanded] widget, which tells the
 * row that the child should be given the remaining room:
 *
 * ```dart
 * new Row(
 *   children: <Widget>[
 *     const FlutterLogo(),
 *     const Expanded(
 *       child: const Text('Flutter\'s hot reload helps you quickly and easily experiment, build UIs, add features, and fix bug faster. Experience sub-second reload times, without losing state, on emulators, simulators, and hardware for iOS and Android.'),
 *     ),
 *     const Icon(Icons.sentiment_very_satisfied),
 *   ],
 * )
 * ```
 *
 * Now, the row first asks the logo to lay out, and then asks the _icon_ to lay
 * out. The [Icon], like the logo, is happy to take on a reasonable size (also
 * 24 pixels, not coincidentally, since both [FlutterLogo] and [Icon] honor the
 * ambient [IconTheme]). This leaves some room left over, and now the row tells
 * the text exactly how wide to be: the exact width of the remaining space. The
 * text, now happy to comply to a reasonable request, wraps the text within
 * that width, and you end up with a paragraph split over several lines.
 *
 * ## Layout algorithm
 *
 * _This section describes how a [Row] is rendered by the framework._
 * _See [BoxConstraints] for an introduction to box layout models._
 *
 * Layout for a [Row] proceeds in six steps:
 *
 * 1. Layout each child a null or zero flex factor (e.g., those that are not
 *    [Expanded]) with unbounded horizontal constraints and the incoming
 *    vertical constraints. If the [crossAxisAlignment] is
 *    [CrossAxisAlignment.STRETCH], instead use tight vertical constraints that
 *    match the incoming max height.
 * 2. Divide the remaining horizontal space among the children with non-zero
 *    flex factors (e.g., those that are [Expanded]) according to their flex
 *    factor. For example, a child with a flex factor of 2.0 will receive twice
 *    the amount of horizontal space as a child with a flex factor of 1.0.
 * 3. Layout each of the remaining children with the same vertical constraints
 *    as in step 1, but instead of using unbounded horizontal constraints, use
 *    horizontal constraints based on the amount of space allocated in step 2.
 *    Children with [Flexible.fit] properties that are [FlexFit.TIGHT] are
 *    given tight constraints (i.e., forced to fill the allocated space), and
 *    children with [Flexible.fit] properties that are [FlexFit.LOOSE] are
 *    given loose constraints (i.e., not forced to fill the allocated space).
 * 4. The height of the [Row] is the maximum height of the children (which will
 *    always satisfy the incoming vertical constraints).
 * 5. The width of the [Row] is determined by the [mainAxisSize] property. If
 *    the [mainAxisSize] property is [MainAxisAlignment.MAX], then the width of the
 *    [Row] is the max width of the incoming constraints. If the [mainAxisSize]
 *    property is [MainAxisAlignment.MIN], then the width of the [Row] is the sum
 *    of widths of the children (subject to the incoming constraints).
 * 6. Determine the position for each child according to the
 *    [mainAxisAlignment] and the [crossAxisAlignment]. For example, if the
 *    [mainAxisAlignment] is [MainAxisAlignment.SPACE_BETWEEN], any horizontal
 *    space that has not been allocated to children is divided evenly and
 *    placed between the children.
 *
 * See also:
 *
 *  * [Column], for a vertical equivalent.
 *  * [Flex], if you don't know in advance if you want a horizontal or vertical
 *    arrangement.
 *  * [Expanded], to indicate children that should take all the remaining room.
 *  * [Flexible], to indicate children that should share the remaining room but
 *    that may by sized smaller (leaving some remaining room unused).
 *  * [Spacer], a widget that takes up space proportional to it's flex value.
 *  * The [catalog of layout widgets](https://flutter.io/widgets/layout/).
 */
class Row(
    key: Key,
    mainAxisAlignment: MainAxisAlignment = MainAxisAlignment.START,
    mainAxisSize: MainAxisSize = MainAxisSize.MAX,
    crossAxisAlignment: CrossAxisAlignment = CrossAxisAlignment.CENTER,
    textDirection: TextDirection? = null,
    verticalDirection: VerticalDirection = VerticalDirection.DOWN,
    textBaseline: TextBaseline? = null,
    children: List<Widget> = listOf()
) : Flex(
    key,
    direction = Axis.HORIZONTAL,
    mainAxisAlignment = mainAxisAlignment,
    mainAxisSize = mainAxisSize,
    crossAxisAlignment = crossAxisAlignment,
    textDirection = textDirection,
    verticalDirection = verticalDirection,
    textBaseline = textBaseline,
    children = children
)
