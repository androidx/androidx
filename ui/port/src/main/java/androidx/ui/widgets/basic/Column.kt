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

// / A widget that displays its children in a vertical array.
// /
// / To cause a child to expand to fill the available vertical space, wrap the
// / child in an [Expanded] widget.
// /
// / The [Column] widget does not scroll (and in general it is considered an error
// / to have more children in a [Column] than will fit in the available room). If
// / you have a line of widgets and want them to be able to scroll if there is
// / insufficient room, consider using a [ListView].
// /
// / For a horizontal variant, see [Row].
// /
// / If you only have one child, then consider using [Align] or [Center] to
// / position the child.
// /
// / ## Sample code
// /
// / This example uses a [Column] to arrange three widgets vertically, the last
// / being made to fill all the remaining space.
// /
// / ```dart
// / new Column(
// /   children: <Widget>[
// /     new Text('Deliver features faster'),
// /     new Text('Craft beautiful UIs'),
// /     new Expanded(
// /       child: new FittedBox(
// /         fit: BoxFit.contain, // otherwise the logo will be tiny
// /         child: const FlutterLogo(),
// /       ),
// /     ),
// /   ],
// / )
// / ```
// /
// / In the sample above, the text and the logo are centered on each line. In the
// / following example, the [crossAxisAlignment] is set to
// / [CrossAxisAlignment.START], so that the children are left-aligned. The
// / [mainAxisSize] is set to [MainAxisAlignment.MIN], so that the column shrinks to
// / fit the children.
// /
// / ```dart
// / new Column(
// /   crossAxisAlignment: CrossAxisAlignment.START,
// /   mainAxisSize: MainAxisAlignment.MIN,
// /   children: <Widget>[
// /     new Text('We move under cover and we move as one'),
// /     new Text('Through the night, we have one shot to live another day'),
// /     new Text('We cannot let a stray gunshot give us away'),
// /     new Text('We will fight up close, seize the moment and stay in it'),
// /     new Text('It’s either that or meet the business end of a bayonet'),
// /     new Text('The code word is ‘Rochambeau,’ dig me?'),
// /     new Text('Rochambeau!', style: DefaultTextStyle.of(context).style.apply(fontSizeFactor: 2.0)),
// /   ],
// / )
// / ```
// /
// / ## Troubleshooting
// /
// / ### When the incoming vertical constraints are unbounded
// /
// / When a [Column] has one or more [Expanded] or [Flexible] children, and is
// / placed in another [Column], or in a [ListView], or in some other context
// / that does not provide a maximum height constraint for the [Column], you will
// / get an exception at runtime saying that there are children with non-zero
// / flex but the vertical constraints are unbounded.
// /
// / The problem, as described in the details that accompany that exception, is
// / that using [Flexible] or [Expanded] means that the remaining space after
// / laying out all the other children must be shared equally, but if the
// / incoming vertical constraints are unbounded, there is infinite remaining
// / space.
// /
// / The key to solving this problem is usually to determine why the [Column] is
// / receiving unbounded vertical constraints.
// /
// / One common reason for this to happen is that the [Column] has been placed in
// / another [Column] (without using [Expanded] or [Flexible] around the inner
// / nested [Column]). When a [Column] lays out its non-flex children (those that
// / have neither [Expanded] or [Flexible] around them), it gives them unbounded
// / constraints so that they can determine their own dimensions (passing
// / unbounded constraints usually signals to the child that it should
// / shrink-wrap its contents). The solution in this case is typically to just
// / wrap the inner column in an [Expanded] to indicate that it should take the
// / remaining space of the outer column, rather than being allowed to take any
// / amount of room it desires.
// /
// / Another reason for this message to be displayed is nesting a [Column] inside
// / a [ListView] or other vertical scrollable. In that scenario, there really is
// / infinite vertical space (the whole point of a vertical scrolling list is to
// / allow infinite space vertically). In such scenarios, it is usually worth
// / examining why the inner [Column] should have an [Expanded] or [Flexible]
// / child: what size should the inner children really be? The solution in this
// / case is typically to remove the [Expanded] or [Flexible] widgets from around
// / the inner children.
// /
// / For more discussion about constraints, see [BoxConstraints].
// /
// / ### The yellow and black striped banner
// /
// / When the contents of a [Column] exceed the amount of space available, the
// / [Column] overflows, and the contents are clipped. In debug mode, a yellow
// / and black striped bar is rendered at the overflowing edge to indicate the
// / problem, and a message is printed below the [Column] saying how much
// / overflow was detected.
// /
// / The usual solution is to use a [ListView] rather than a [Column], to enable
// / the contents to scroll when vertical space is limited.
// /
// / ## Layout algorithm
// /
// / _This section describes how a [Column] is rendered by the framework._
// / _See [BoxConstraints] for an introduction to box layout models._
// /
// / Layout for a [Column] proceeds in six steps:
// /
// / 1. Layout each child a null or zero flex factor (e.g., those that are not
// /    [Expanded]) with unbounded vertical constraints and the incoming
// /    horizontal constraints. If the [crossAxisAlignment] is
// /    [CrossAxisAlignment.STRETCH], instead use tight horizontal constraints
// /    that match the incoming max width.
// / 2. Divide the remaining vertical space among the children with non-zero
// /    flex factors (e.g., those that are [Expanded]) according to their flex
// /    factor. For example, a child with a flex factor of 2.0 will receive twice
// /    the amount of vertical space as a child with a flex factor of 1.0.
// / 3. Layout each of the remaining children with the same horizontal
// /    constraints as in step 1, but instead of using unbounded vertical
// /    constraints, use vertical constraints based on the amount of space
// /    allocated in step 2. Children with [Flexible.fit] properties that are
// /    [FlexFit.TIGHT] are given tight constraints (i.e., forced to fill the
// /    allocated space), and children with [Flexible.fit] properties that are
// /    [FlexFit.LOOSE] are given loose constraints (i.e., not forced to fill the
// /    allocated space).
// / 4. The width of the [Column] is the maximum width of the children (which
// /    will always satisfy the incoming horizontal constraints).
// / 5. The height of the [Column] is determined by the [mainAxisSize] property.
// /    If the [mainAxisSize] property is [MainAxisAlignment.MAX], then the height of
// /    the [Column] is the max height of the incoming constraints. If the
// /    [mainAxisSize] property is [MainAxisAlignment.MIN], then the height of the
// /    [Column] is the sum of heights of the children (subject to the incoming
// /    constraints).
// / 6. Determine the position for each child according to the
// /    [mainAxisAlignment] and the [crossAxisAlignment]. For example, if the
// /    [mainAxisAlignment] is [MainAxisAlignment.SPACE_BETWEEN], any vertical
// /    space that has not been allocated to children is divided evenly and
// /    placed between the children.
// /
// / See also:
// /
// /  * [Row], for a horizontal equivalent.
// /  * [Flex], if you don't know in advance if you want a horizontal or vertical
// /    arrangement.
// /  * [Expanded], to indicate children that should take all the remaining room.
// /  * [Flexible], to indicate children that should share the remaining room but
// /    that may size smaller (leaving some remaining room unused).
// /  * [SingleChildScrollView], whose documentation discusses some ways to
// /    use a [Column] inside a scrolling container.
// /  * [Spacer], a widget that takes up space proportional to it's flex value.
// /  * The [catalog of layout widgets](https://flutter.io/widgets/layout/).
class Column(
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
    direction = Axis.VERTICAL,
    mainAxisAlignment = mainAxisAlignment,
    mainAxisSize = mainAxisSize,
    crossAxisAlignment = crossAxisAlignment,
    textDirection = textDirection,
    verticalDirection = verticalDirection,
    textBaseline = textBaseline,
    children = children
)
