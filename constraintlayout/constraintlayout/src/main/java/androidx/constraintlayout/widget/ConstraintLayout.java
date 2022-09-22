/*
 * Copyright (C) 2015 The Android Open Source Project
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

package androidx.constraintlayout.widget;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;
import static androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.MATCH_CONSTRAINT_SPREAD;
import static androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.MATCH_CONSTRAINT_WRAP;
import static androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.PARENT_ID;
import static androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.UNSET;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Log;
import android.util.SparseArray;
import android.util.SparseIntArray;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.core.LinearSystem;
import androidx.constraintlayout.core.Metrics;
import androidx.constraintlayout.core.widgets.ConstraintAnchor;
import androidx.constraintlayout.core.widgets.ConstraintWidget;
import androidx.constraintlayout.core.widgets.ConstraintWidgetContainer;
import androidx.constraintlayout.core.widgets.Guideline;
import androidx.constraintlayout.core.widgets.Optimizer;
import androidx.constraintlayout.core.widgets.analyzer.BasicMeasure;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * A {@code ConstraintLayout} is a {@link android.view.ViewGroup} which allows you
 * to position and size widgets in a flexible way.
 * <p>
 * <b>Note:</b> {@code ConstraintLayout} is available as a support library that you can use
 * on Android systems starting with API level 9 (Gingerbread).
 * As such, we are planning on enriching its API and capabilities over time.
 * This documentation will reflect those changes.
 * </p>
 * <p>
 * There are currently various types of constraints that you can use:
 * <ul>
 * <li>
 * <a href="#RelativePositioning">Relative positioning</a>
 * </li>
 * <li>
 * <a href="#Margins">Margins</a>
 * </li>
 * <li>
 * <a href="#CenteringPositioning">Centering positioning</a>
 * </li>
 * <li>
 * <a href="#CircularPositioning">Circular positioning</a>
 * </li>
 * <li>
 * <a href="#VisibilityBehavior">Visibility behavior</a>
 * </li>
 * <li>
 * <a href="#DimensionConstraints">Dimension constraints</a>
 * </li>
 * <li>
 * <a href="#Chains">Chains</a>
 * </li>
 * <li>
 * <a href="#VirtualHelpers">Virtual Helpers objects</a>
 * </li>
 * <li>
 * <a href="#Optimizer">Optimizer</a>
 * </li>
 * </ul>
 * </p>
 *
 * <p>
 * Note that you cannot have a circular dependency in constraints.
 * </p>
 * <p>
 * Also see {@link ConstraintLayout.LayoutParams
 * ConstraintLayout.LayoutParams} for layout attributes
 * </p>
 *
 * <div class="special reference">
 * <h3>Developer Guide</h3>
 *
 * <h4 id="RelativePositioning"> Relative positioning </h4>
 * <p>
 * Relative positioning is one of the basic building blocks of creating layouts in ConstraintLayout.
 * Those constraints allow you to position a given widget relative to another one. You can constrain
 * a widget on the horizontal and vertical axis:
 * <ul>
 * <li>Horizontal Axis: left, right, start and end sides</li>
 * <li>Vertical Axis: top, bottom sides and text baseline</li>
 * </ul>
 * <p>
 * The general concept is to constrain a given side of a widget to another side of any other widget.
 * <p>
 * For example, in order to position button B to the right of button A (Fig. 1):
 * <br><div align="center">
 * <img width="300px" src="resources/images/relative-positioning.png">
 * <br><b><i>Fig. 1 - Relative Positioning Example</i></b>
 * </div>
 * </p>
 * <p>
 * you would need to do:
 * </p>
 * <pre>{@code
 *         <Button android:id="@+id/buttonA" ... />
 *         <Button android:id="@+id/buttonB" ...
 *                 app:layout_constraintLeft_toRightOf="@+id/buttonA" />
 *         }
 *     </pre>
 * This tells the system that we want the left side of button
 * B to be constrained to the right side of button A.
 * Such a position constraint means that the system will try to have
 * both sides share the same location.
 * <br><div align="center" >
 * <img width="350px" src="resources/images/relative-positioning-constraints.png">
 * <br><b><i>Fig. 2 - Relative Positioning Constraints</i></b>
 * </div>
 *
 * <p>Here is the list of available constraints (Fig. 2):</p>
 * <ul>
 * <li>{@code layout_constraintLeft_toLeftOf}</li>
 * <li>{@code layout_constraintLeft_toRightOf}</li>
 * <li>{@code layout_constraintRight_toLeftOf}</li>
 * <li>{@code layout_constraintRight_toRightOf}</li>
 * <li>{@code layout_constraintTop_toTopOf}</li>
 * <li>{@code layout_constraintTop_toBottomOf}</li>
 * <li>{@code layout_constraintBottom_toTopOf}</li>
 * <li>{@code layout_constraintBottom_toBottomOf}</li>
 * <li>{@code layout_constraintBaseline_toBaselineOf}</li>
 * <li>{@code layout_constraintStart_toEndOf}</li>
 * <li>{@code layout_constraintStart_toStartOf}</li>
 * <li>{@code layout_constraintEnd_toStartOf}</li>
 * <li>{@code layout_constraintEnd_toEndOf}</li>
 * </ul>
 * <p>
 * They all take a reference {@code id} to another widget, or the
 * {@code parent} (which will reference the parent container, i.e. the ConstraintLayout):
 * <pre>{@code
 *         <Button android:id="@+id/buttonB" ...
 *                 app:layout_constraintLeft_toLeftOf="parent" />
 *         }
 *     </pre>
 *
 * </p>
 *
 * <h4 id="Margins"> Margins </h4>
 * <p>
 * <div align="center" >
 * <img width="325px" src="resources/images/relative-positioning-margin.png">
 * <br><b><i>Fig. 3 - Relative Positioning Margins</i></b>
 * </div>
 * <p>If side margins are set, they will be applied to the corresponding constraints
 * (if they exist) (Fig. 3), enforcing the margin as a space between
 * the target and the source side. The usual layout margin attributes can be used to this effect:
 * <ul>
 * <li>{@code android:layout_marginStart}</li>
 * <li>{@code android:layout_marginEnd}</li>
 * <li>{@code android:layout_marginLeft}</li>
 * <li>{@code android:layout_marginTop}</li>
 * <li>{@code android:layout_marginRight}</li>
 * <li>{@code android:layout_marginBottom}</li>
 * <li>{@code layout_marginBaseline}</li>
 * </ul>
 * <p>Note that a margin can only be positive or equal to zero,
 * and takes a {@code Dimension}.</p>
 * <h4 id="GoneMargin"> Margins when connected to a GONE widget</h4>
 * <p>When a position constraint target's visibility is {@code View.GONE},
 * you can also indicate a different
 * margin value to be used using the following attributes:</p>
 * <ul>
 * <li>{@code layout_goneMarginStart}</li>
 * <li>{@code layout_goneMarginEnd}</li>
 * <li>{@code layout_goneMarginLeft}</li>
 * <li>{@code layout_goneMarginTop}</li>
 * <li>{@code layout_goneMarginRight}</li>
 * <li>{@code layout_goneMarginBottom}</li>
 * <li>{@code layout_goneMarginBaseline}</li>
 * </ul>
 * </p>
 *
 * </p>
 * <h4 id="CenteringPositioning"> Centering positioning and bias</h4>
 * <p>
 * A useful aspect of {@code ConstraintLayout} is in how it deals with "impossible" constraints.
 * For example, if
 * we have something like:
 * <pre>{@code
 *         <androidx.constraintlayout.widget.ConstraintLayout ...>
 *             <Button android:id="@+id/button" ...
 *                 app:layout_constraintLeft_toLeftOf="parent"
 *                 app:layout_constraintRight_toRightOf="parent"/>
 *         </>
 *         }
 *     </pre>
 * </p>
 * <p>
 * Unless the {@code ConstraintLayout} happens to have the exact same size as the
 * {@code Button}, both constraints
 * cannot be satisfied at the same time (both sides cannot be where we want them to be).
 * <p><div align="center" >
 * <img width="325px" src="resources/images/centering-positioning.png">
 * <br><b><i>Fig. 4 - Centering Positioning</i></b>
 * </div>
 * <p>
 * What happens in this case is that the constraints act like opposite forces
 * pulling the widget apart equally (Fig. 4); such that the widget will end up being centered
 * in the parent container.
 * This will apply similarly for vertical constraints.
 * </p>
 * <h5 id="Bias">Bias</h5>
 * <p>
 * The default when encountering such opposite constraints is to center the widget;
 * but you can tweak
 * the positioning to favor one side over another using the bias attributes:
 * <ul>
 * <li>{@code layout_constraintHorizontal_bias}</li>
 * <li>{@code layout_constraintVertical_bias}</li>
 * </ul>
 * <p><div align="center" >
 * <img width="325px" src="resources/images/centering-positioning-bias.png">
 * <br><b><i>Fig. 5 - Centering Positioning with Bias</i></b>
 * </div>
 * <p>
 * For example the following will make the left side with a 30% bias instead of the default 50%,
 * such that the left side will be
 * shorter, with the widget leaning more toward the left side (Fig. 5):
 * </p>
 * <pre>{@code
 *         <androidx.constraintlayout.widget.ConstraintLayout ...>
 *             <Button android:id="@+id/button" ...
 *                 app:layout_constraintHorizontal_bias="0.3"
 *                 app:layout_constraintLeft_toLeftOf="parent"
 *                 app:layout_constraintRight_toRightOf="parent"/>
 *         </>
 *         }
 *     </pre>
 * Using bias, you can craft User Interfaces that will better adapt to screen sizes changes.
 * </p>
 * </p>
 *
 * <h4 id="CircularPositioning"> Circular positioning (<b>Added in 1.1</b>)</h4>
 * <p>
 * You can constrain a widget center relative to another widget center,
 * at an angle and a distance. This allows
 * you to position a widget on a circle (see Fig. 6). The following attributes can be used:
 * <ul>
 * <li>{@code layout_constraintCircle} : references another widget id</li>
 * <li>{@code layout_constraintCircleRadius} : the distance to the other widget center</li>
 * <li>{@code layout_constraintCircleAngle} : which angle the widget should be at
 * (in degrees, from 0 to 360)</li>
 * </ul>
 * <p><div align="center" >
 * <img width="325px" src="resources/images/circle1.png">
 * <img width="325px" src="resources/images/circle2.png">
 * <br><b><i>Fig. 6 - Circular Positioning</i></b>
 * </div>
 * <br><br>
 * <pre>{@code
 *  <Button android:id="@+id/buttonA" ... />
 *  <Button android:id="@+id/buttonB" ...
 *      app:layout_constraintCircle="@+id/buttonA"
 *      app:layout_constraintCircleRadius="100dp"
 *      app:layout_constraintCircleAngle="45" />
 *         }
 *     </pre>
 * </p>
 * <h4 id="VisibilityBehavior"> Visibility behavior </h4>
 * <p>
 * {@code ConstraintLayout} has a specific handling of widgets being marked as {@code View.GONE}.
 * <p>{@code GONE} widgets, as usual, are not going to be displayed and
 * are not part of the layout itself (i.e. their actual dimensions
 * will not be changed if marked as {@code GONE}).
 *
 * <p>But in terms of the layout computations, {@code GONE} widgets are still part of it,
 * with an important distinction:
 * <ul>
 * <li> For the layout pass, their dimension will be considered as zero
 * (basically, they will be resolved to a point)</li>
 * <li> If they have constraints to other widgets they will still be respected,
 * but any margins will be as if equals to zero</li>
 * </ul>
 *
 * <p><div align="center" >
 * <img width="350px" src="resources/images/visibility-behavior.png">
 * <br><b><i>Fig. 7 - Visibility Behavior</i></b>
 * </div>
 * <p>This specific behavior allows to build layouts where you can
 * temporarily mark widgets as being {@code GONE},
 * without breaking the layout (Fig. 7), which can be particularly useful
 * when doing simple layout animations.
 * <p><b>Note: </b>The margin used will be the margin that B had
 * defined when connecting to A (see Fig. 7 for an example).
 * In some cases, this might not be the margin you want
 * (e.g. A had a 100dp margin to the side of its container,
 * B only a 16dp to A, marking
 * A as gone, B will have a margin of 16dp to the container).
 * For this reason, you can specify an alternate
 * margin value to be used when the connection is to a widget being marked as gone
 * (see <a href="#GoneMargin">the section above about the gone margin attributes</a>).
 * </p>
 *
 * <h4 id="DimensionConstraints"> Dimensions constraints </h4>
 * <h5>Minimum dimensions on ConstraintLayout</h5>
 * <p>
 * You can define minimum and maximum sizes for the {@code ConstraintLayout} itself:
 * <ul>
 * <li>{@code android:minWidth} set the minimum width for the layout</li>
 * <li>{@code android:minHeight} set the minimum height for the layout</li>
 * <li>{@code android:maxWidth} set the maximum width for the layout</li>
 * <li>{@code android:maxHeight} set the maximum height for the layout</li>
 * </ul>
 * Those minimum and maximum dimensions will be used by
 * {@code ConstraintLayout} when its dimensions are set to {@code WRAP_CONTENT}.
 * </p>
 * <h5>Widgets dimension constraints</h5>
 * <p>
 * The dimension of the widgets can be specified by setting the
 * {@code android:layout_width} and
 * {@code android:layout_height} attributes in 3 different ways:
 * <ul>
 * <li>Using a specific dimension (either a literal value such as
 * {@code 123dp} or a {@code Dimension} reference)</li>
 * <li>Using {@code WRAP_CONTENT}, which will ask the widget to compute its own size</li>
 * <li>Using {@code 0dp}, which is the equivalent of "{@code MATCH_CONSTRAINT}"</li>
 * </ul>
 * <p><div align="center" >
 * <img width="325px" src="resources/images/dimension-match-constraints.png">
 * <br><b><i>Fig. 8 - Dimension Constraints</i></b>
 * </div>
 * The first two works in a similar fashion as other layouts.
 * The last one will resize the widget in such a way as
 * matching the constraints that are set (see Fig. 8, (a) is wrap_content,
 * (b) is 0dp). If margins are set, they will be taken in account
 * in the computation (Fig. 8, (c) with 0dp).
 * <p>
 * <b>Important: </b> {@code MATCH_PARENT} is not recommended for widgets
 * contained in a {@code ConstraintLayout}. Similar behavior can
 * be defined by using {@code MATCH_CONSTRAINT} with the corresponding
 * left/right or top/bottom constraints being set to {@code "parent"}.
 * </p>
 * </p>
 * <h5>WRAP_CONTENT : enforcing constraints (<i><b>Added in 1.1</b></i>)</h5>
 * <p>
 * If a dimension is set to {@code WRAP_CONTENT}, in versions before 1.1
 * they will be treated as a literal dimension -- meaning, constraints will
 * not limit the resulting dimension. While in general this is enough (and faster),
 * in some situations, you might want to use {@code WRAP_CONTENT},
 * yet keep enforcing constraints to limit the resulting dimension. In that case,
 * you can add one of the corresponding attribute:
 * <ul>
 * <li>{@code app:layout_constrainedWidth="true|false"}</li>
 * <li>{@code app:layout_constrainedHeight="true|false"}</li>
 * </ul>
 * </p>
 * <h5>MATCH_CONSTRAINT dimensions (<i><b>Added in 1.1</b></i>)</h5>
 * <p>
 * When a dimension is set to {@code MATCH_CONSTRAINT},
 * the default behavior is to have the resulting size take all the available space.
 * Several additional modifiers are available:
 * <ul>
 * <li>{@code layout_constraintWidth_min} and {@code layout_constraintHeight_min} :
 * will set the minimum size for this dimension</li>
 * <li>{@code layout_constraintWidth_max} and {@code layout_constraintHeight_max} :
 * will set the maximum size for this dimension</li>
 * <li>{@code layout_constraintWidth_percent} and {@code layout_constraintHeight_percent} :
 * will set the size of this dimension as a percentage of the parent</li>
 * </ul>
 * <h6>Min and Max</h6>
 * The value indicated for min and max can be either a dimension in Dp,
 * or "wrap", which will use the same value as what {@code WRAP_CONTENT} would do.
 * <h6>Percent dimension</h6>
 * To use percent, you need to set the following:
 * <ul>
 * <li>The dimension should be set to {@code MATCH_CONSTRAINT} (0dp)</li>
 * <li>The default should be set to percent {@code app:layout_constraintWidth_default="percent"}
 * or {@code app:layout_constraintHeight_default="percent"}</li>
 * <li>Then set the {@code layout_constraintWidth_percent}
 * or {@code layout_constraintHeight_percent} attributes to a value between 0 and 1</li>
 * </ul>
 * </p>
 * <h5>Ratio</h5>
 * <p>
 * You can also define one dimension of a widget as a ratio of the other one.
 * In order to do that, you
 * need to have at least one constrained dimension be set to
 * {@code 0dp} (i.e., {@code MATCH_CONSTRAINT}), and set the
 * attribute {@code layout_constraintDimensionRatio} to a given ratio.
 * For example:
 * <pre>
 *         {@code
 *           <Button android:layout_width="wrap_content"
 *                   android:layout_height="0dp"
 *                   app:layout_constraintDimensionRatio="1:1" />
 *         }
 *     </pre>
 * will set the height of the button to be the same as its width.
 * </p>
 * <p> The ratio can be expressed either as:
 * <ul>
 * <li>a float value, representing a ratio between width and height</li>
 * <li>a ratio in the form "width:height"</li>
 * </ul>
 * </p>
 * <p>
 * You can also use ratio if both dimensions are set to
 * {@code MATCH_CONSTRAINT} (0dp). In this case the system sets the
 * largest dimensions that satisfies all constraints and maintains
 * the aspect ratio specified. To constrain one specific side
 * based on the dimensions of another, you can pre append
 * {@code W,}" or {@code H,} to constrain the width or height
 * respectively.
 * For example,
 * If one dimension is constrained by two targets
 * (e.g. width is 0dp and centered on parent) you can indicate which
 * side should be constrained, by adding the letter
 * {@code W} (for constraining the width) or {@code H}
 * (for constraining the height) in front of the ratio, separated
 * by a comma:
 * <pre>
 *         {@code
 *           <Button android:layout_width="0dp"
 *                   android:layout_height="0dp"
 *                   app:layout_constraintDimensionRatio="H,16:9"
 *                   app:layout_constraintBottom_toBottomOf="parent"
 *                   app:layout_constraintTop_toTopOf="parent"/>
 *         }
 *     </pre>
 * will set the height of the button following a 16:9 ratio,
 * while the width of the button will match the constraints
 * to its parent.
 *
 * </p>
 *
 * <h4 id="Chains">Chains</h4>
 * <p>Chains provide group-like behavior in a single axis (horizontally or vertically).
 * The other axis can be constrained independently.</p>
 * <h5>Creating a chain</h5>
 * <p>
 * A set of widgets are considered a chain if they are linked together via a
 * bi-directional connection (see Fig. 9, showing a minimal chain, with two widgets).
 * </p>
 * <p><div align="center" >
 * <img width="325px" src="resources/images/chains.png">
 * <br><b><i>Fig. 9 - Chain</i></b>
 * </div>
 * <p>
 * <h5>Chain heads</h5>
 * <p>
 * Chains are controlled by attributes set on the first element of the chain
 * (the "head" of the chain):
 * </p>
 * <p><div align="center" >
 * <img width="400px" src="resources/images/chains-head.png">
 * <br><b><i>Fig. 10 - Chain Head</i></b>
 * </div>
 * <p>The head is the left-most widget for horizontal chains,
 * and the top-most widget for vertical chains.</p>
 * <h5>Margins in chains</h5>
 * <p>If margins are specified on connections, they will be taken into account.
 * In the case of spread chains, margins will be deducted from the allocated space.</p>
 * <h5>Chain Style</h5>
 * <p>When setting the attribute {@code layout_constraintHorizontal_chainStyle} or
 * {@code layout_constraintVertical_chainStyle} on the first element of a chain,
 * the behavior of the chain will change according to the specified style
 * (default is {@code CHAIN_SPREAD}).
 * <ul>
 * <li>{@code CHAIN_SPREAD} -- the elements will be spread out (default style)</li>
 * <li>Weighted chain -- in {@code CHAIN_SPREAD} mode,
 * if some widgets are set to {@code MATCH_CONSTRAINT}, they will split the available space</li>
 * <li>{@code CHAIN_SPREAD_INSIDE} -- similar,
 * but the endpoints of the chain will not be spread out</li>
 * <li>{@code CHAIN_PACKED} -- the elements of the chain will be packed together.
 * The horizontal or vertical
 * bias attribute of the child will then affect the positioning of the packed elements</li>
 * </ul>
 * <p><div align="center" >
 * <img width="600px" src="resources/images/chains-styles.png">
 * <br><b><i>Fig. 11 - Chains Styles</i></b>
 * </div>
 * </p>
 * <h5>Weighted chains</h5>
 * <p>The default behavior of a chain is to spread the elements equally in the available space.
 * If one or more elements are using {@code MATCH_CONSTRAINT}, they
 * will use the available empty space (equally divided among themselves).
 * The attribute {@code layout_constraintHorizontal_weight} and
 * {@code layout_constraintVertical_weight}
 * will control how the space will be distributed among the elements using
 * {@code MATCH_CONSTRAINT}. For example,
 * on a chain containing two elements using {@code MATCH_CONSTRAINT},
 * with the first element using a weight of 2 and the second a weight of 1,
 * the space occupied by the first element will be twice that of the second element.</p>
 *
 * <h5>Margins and chains (<i><b>in 1.1</b></i>)</h5>
 * <p>When using margins on elements in a chain, the margins are additive.</p>
 * <p>For example, on a horizontal chain, if one element defines
 * a right margin of 10dp and the next element
 * defines a left margin of 5dp, the resulting margin between those
 * two elements is 15dp.</p>
 * <p>An item plus its margins are considered together when calculating
 * leftover space used by chains
 * to position items. The leftover space does not contain the margins.</p>
 *
 * <h4 id="VirtualHelpers"> Virtual Helper objects </h4>
 * <p>In addition to the intrinsic capabilities detailed previously,
 * you can also use special helper objects
 * in {@code ConstraintLayout} to help you with your layout. Currently, the
 * {@code Guideline}{@see Guideline} object allows you to create
 * Horizontal and Vertical guidelines which are positioned relative to the
 * {@code ConstraintLayout} container. Widgets can
 * then be positioned by constraining them to such guidelines. In <b>1.1</b>,
 * {@code Barrier} and {@code Group} were added too.</p>
 *
 * <h4 id="Optimizer">Optimizer (<i><b>in 1.1</b></i>)</h4>
 * <p>
 * In 1.1 we exposed the constraints optimizer. You can decide which optimizations
 * are applied by adding the tag <i>app:layout_optimizationLevel</i>
 * to the ConstraintLayout element.
 * <ul>
 * <li><b>none</b> : no optimizations are applied</li>
 * <li><b>standard</b> : Default. Optimize direct and barrier constraints only</li>
 * <li><b>direct</b> : optimize direct constraints</li>
 * <li><b>barrier</b> : optimize barrier constraints</li>
 * <li><b>chain</b> : optimize chain constraints (experimental)</li>
 * <li><b>dimensions</b> : optimize dimensions measures (experimental),
 * reducing the number of measures of match constraints elements</li>
 * </ul>
 * </p>
 * <p>This attribute is a mask, so you can decide to turn on or off
 * specific optimizations by listing the ones you want.
 * For example: <i>app:layout_optimizationLevel="direct|barrier|chain"</i> </p>
 * </div>
 */
public class ConstraintLayout extends ViewGroup {
    /**
     *
     */
    public static final String VERSION = "ConstraintLayout-2.2.0-alpha03";
    private static final String TAG = "ConstraintLayout";

    private static final boolean USE_CONSTRAINTS_HELPER = true;
    private static final boolean DEBUG = LinearSystem.FULL_DEBUG;
    private static final boolean DEBUG_DRAW_CONSTRAINTS = false;
    private static final boolean OPTIMIZE_HEIGHT_CHANGE = false;

    SparseArray<View> mChildrenByIds = new SparseArray<>();

    // This array keep a list of helper objects if they are present
    private ArrayList<ConstraintHelper> mConstraintHelpers = new ArrayList<>(4);

    protected ConstraintWidgetContainer mLayoutWidget = new ConstraintWidgetContainer();

    private int mMinWidth = 0;
    private int mMinHeight = 0;
    private int mMaxWidth = Integer.MAX_VALUE;
    private int mMaxHeight = Integer.MAX_VALUE;

    protected boolean mDirtyHierarchy = true;
    private int mOptimizationLevel = Optimizer.OPTIMIZATION_STANDARD;
    private ConstraintSet mConstraintSet = null;
    protected ConstraintLayoutStates mConstraintLayoutSpec = null;

    private int mConstraintSetId = -1;

    private HashMap<String, Integer> mDesignIds = new HashMap<>();

    // Cache last measure
    private int mLastMeasureWidth = -1;
    private int mLastMeasureHeight = -1;
    int mLastMeasureWidthSize = -1;
    int mLastMeasureHeightSize = -1;
    int mLastMeasureWidthMode = MeasureSpec.UNSPECIFIED;
    int mLastMeasureHeightMode = MeasureSpec.UNSPECIFIED;
    private SparseArray<ConstraintWidget> mTempMapIdToWidget = new SparseArray<>();

    /**
     *
     */
    public static final int DESIGN_INFO_ID = 0;
   // private ConstraintsChangedListener mConstraintsChangedListener;
    private Metrics mMetrics;

    private static SharedValues sSharedValues = null;

    /**
     * Returns the SharedValues instance, creating it if it doesn't exist.
     *
     * @return the SharedValues instance
     */
    public static SharedValues getSharedValues() {
        if (sSharedValues == null) {
            sSharedValues = new SharedValues();
        }
        return sSharedValues;
    }

    /**
     *
     */
    public void setDesignInformation(int type, Object value1, Object value2) {
        if (type == DESIGN_INFO_ID
                && value1 instanceof String
                && value2 instanceof Integer) {
            if (mDesignIds == null) {
                mDesignIds = new HashMap<>();
            }
            String name = (String) value1;
            int index = name.indexOf("/");
            if (index != -1) {
                name = name.substring(index + 1);
            }
            int id = (Integer) value2;
            mDesignIds.put(name, id);
        }
    }

    /**
     *
     */
    public Object getDesignInformation(int type, Object value) {
        if (type == DESIGN_INFO_ID && value instanceof String) {
            String name = (String) value;
            if (mDesignIds != null && mDesignIds.containsKey(name)) {
                return mDesignIds.get(name);
            }
        }
        return null;
    }

    public ConstraintLayout(@NonNull Context context) {
        super(context);
        init(null, 0, 0);
    }

    public ConstraintLayout(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(attrs, 0, 0);
    }

    public ConstraintLayout(@NonNull Context context,
                            @Nullable AttributeSet attrs,
                            int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(attrs, defStyleAttr, 0);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public ConstraintLayout(@NonNull Context context,
                            @Nullable AttributeSet attrs,
                            int defStyleAttr,
                            int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(attrs, defStyleAttr, defStyleRes);
    }

    /**
     *
     */
    @Override
    public void setId(int id) {
        mChildrenByIds.remove(getId());
        super.setId(id);
        mChildrenByIds.put(getId(), this);
    }

    // -------------------------------------------------------------------------------------------
    // Measure widgets callbacks
    // -------------------------------------------------------------------------------------------


    // -------------------------------------------------------------------------------------------

    class Measurer implements BasicMeasure.Measurer {
        ConstraintLayout mLayout;
        int mPaddingTop;
        int mPaddingBottom;
        int mPaddingWidth;
        int mPaddingHeight;
        int mLayoutWidthSpec;
        int mLayoutHeightSpec;

        public void captureLayoutInfo(int widthSpec,
                                      int heightSpec,
                                      int top,
                                      int bottom,
                                      int width,
                                      int height) {
            mPaddingTop = top;
            mPaddingBottom = bottom;
            mPaddingWidth = width;
            mPaddingHeight = height;
            mLayoutWidthSpec = widthSpec;
            mLayoutHeightSpec = heightSpec;
        }

        Measurer(ConstraintLayout l) {
            mLayout = l;
        }

        @SuppressLint("WrongCall")
        @Override
        public final void measure(ConstraintWidget widget,
                                  BasicMeasure.Measure measure) {
            if (widget == null) {
                return;
            }
            if (widget.getVisibility() == GONE && !widget.isInPlaceholder()) {
                measure.measuredWidth = 0;
                measure.measuredHeight = 0;
                measure.measuredBaseline = 0;
                return;
            }
            if (widget.getParent() == null) {
                return;
            }

            long startMeasure = 0;
            long endMeasure;

            if (mMetrics != null) {
                mMetrics.mNumberOfMeasures++;
                startMeasure = System.nanoTime();
            }

            ConstraintWidget.DimensionBehaviour horizontalBehavior = measure.horizontalBehavior;
            ConstraintWidget.DimensionBehaviour verticalBehavior = measure.verticalBehavior;

            int horizontalDimension = measure.horizontalDimension;
            int verticalDimension = measure.verticalDimension;

            int horizontalSpec = 0;
            int verticalSpec = 0;

            int heightPadding = mPaddingTop + mPaddingBottom;
            int widthPadding = mPaddingWidth;

            View child = (View) widget.getCompanionWidget();

            switch (horizontalBehavior) {
                case FIXED: {
                    horizontalSpec = MeasureSpec.makeMeasureSpec(horizontalDimension,
                            MeasureSpec.EXACTLY);
                }
                break;
                case WRAP_CONTENT: {
                    horizontalSpec = getChildMeasureSpec(mLayoutWidthSpec,
                            widthPadding, WRAP_CONTENT);
                }
                break;
                case MATCH_PARENT: {
                    // Horizontal spec must account for margin as well as padding here.
                    horizontalSpec = getChildMeasureSpec(mLayoutWidthSpec,
                            widthPadding + widget.getHorizontalMargin(),
                            LayoutParams.MATCH_PARENT);
                }
                break;
                case MATCH_CONSTRAINT: {
                    horizontalSpec = getChildMeasureSpec(mLayoutWidthSpec,
                            widthPadding, WRAP_CONTENT);
                    boolean shouldDoWrap = widget.mMatchConstraintDefaultWidth
                            == MATCH_CONSTRAINT_WRAP;
                    if (measure.measureStrategy == BasicMeasure.Measure.TRY_GIVEN_DIMENSIONS
                            || measure.measureStrategy
                            == BasicMeasure.Measure.USE_GIVEN_DIMENSIONS) {
                        // the solver gives us our new dimension,
                        // but if we previously had it measured with
                        // a wrap, it can be incorrect if the other side was also variable.
                        // So in that case, we have to double-check the
                        // other side is stable (else we can't
                        // just assume the wrap value will be correct).
                        boolean otherDimensionStable = child.getMeasuredHeight()
                                == widget.getHeight();
                        boolean useCurrent = measure.measureStrategy
                                == BasicMeasure.Measure.USE_GIVEN_DIMENSIONS
                                            || !shouldDoWrap
                                            || (shouldDoWrap && otherDimensionStable)
                                            || child instanceof Placeholder
                                            || widget.isResolvedHorizontally();
                        if (useCurrent) {
                            horizontalSpec = MeasureSpec.makeMeasureSpec(widget.getWidth(),
                                    MeasureSpec.EXACTLY);
                        }
                    }
                }
                break;
            }

            switch (verticalBehavior) {
                case FIXED: {
                    verticalSpec = MeasureSpec.makeMeasureSpec(verticalDimension,
                            MeasureSpec.EXACTLY);
                }
                break;
                case WRAP_CONTENT: {
                    verticalSpec = getChildMeasureSpec(mLayoutHeightSpec,
                            heightPadding, WRAP_CONTENT);
                }
                break;
                case MATCH_PARENT: {
                    // Vertical spec must account for margin as well as padding here.
                    verticalSpec = getChildMeasureSpec(mLayoutHeightSpec,
                            heightPadding + widget.getVerticalMargin(),
                            LayoutParams.MATCH_PARENT);
                }
                break;
                case MATCH_CONSTRAINT: {
                    verticalSpec = getChildMeasureSpec(mLayoutHeightSpec,
                            heightPadding, WRAP_CONTENT);
                    boolean shouldDoWrap = widget.mMatchConstraintDefaultHeight
                            == MATCH_CONSTRAINT_WRAP;
                    if (measure.measureStrategy == BasicMeasure.Measure.TRY_GIVEN_DIMENSIONS
                            || measure.measureStrategy
                            == BasicMeasure.Measure.USE_GIVEN_DIMENSIONS) {
                        // the solver gives us our new dimension,
                        // but if we previously had it measured with
                        // a wrap, it can be incorrect if the other side was also variable.
                        // So in that case, we have to double-check
                        // the other side is stable (else we can't
                        // just assume the wrap value will be correct).
                        boolean otherDimensionStable = child.getMeasuredWidth()
                                == widget.getWidth();
                        boolean useCurrent = measure.measureStrategy
                                == BasicMeasure.Measure.USE_GIVEN_DIMENSIONS
                                            || !shouldDoWrap
                                            || (shouldDoWrap && otherDimensionStable)
                                            || (child instanceof Placeholder)
                                            || widget.isResolvedVertically();
                        if (useCurrent) {
                            verticalSpec = MeasureSpec.makeMeasureSpec(widget.getHeight(),
                                    MeasureSpec.EXACTLY);
                        }
                    }
                }
                break;
            }

            ConstraintWidgetContainer container = (ConstraintWidgetContainer) widget.getParent();
            if (container != null && Optimizer.enabled(mOptimizationLevel,
                    Optimizer.OPTIMIZATION_CACHE_MEASURES)) {
                if (child.getMeasuredWidth() == widget.getWidth()
                        // note: the container check replicates legacy behavior, but we might want
                        // to not enforce that in 3.0
                        && child.getMeasuredWidth() < container.getWidth()
                        && child.getMeasuredHeight() == widget.getHeight()
                        && child.getMeasuredHeight() < container.getHeight()
                        && child.getBaseline() == widget.getBaselineDistance()
                        && !widget.isMeasureRequested()
                ) {
                    boolean similar = isSimilarSpec(widget.getLastHorizontalMeasureSpec(),
                            horizontalSpec, widget.getWidth())
                            && isSimilarSpec(widget.getLastVerticalMeasureSpec(),
                            verticalSpec, widget.getHeight());
                    if (similar) {
                        measure.measuredWidth = widget.getWidth();
                        measure.measuredHeight = widget.getHeight();
                        measure.measuredBaseline = widget.getBaselineDistance();
                        // if the dimensions of the solver widget are already the
                        // same as the real view, no need to remeasure.
                        if (DEBUG) {
                            System.out.println("SKIPPED " + widget);
                        }
                        return;
                    }
                }
            }

            boolean horizontalMatchConstraints = (horizontalBehavior
                    == ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);
            boolean verticalMatchConstraints = (verticalBehavior
                    == ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);

            boolean verticalDimensionKnown = (verticalBehavior
                    == ConstraintWidget.DimensionBehaviour.MATCH_PARENT
                    || verticalBehavior == ConstraintWidget.DimensionBehaviour.FIXED);
            boolean horizontalDimensionKnown = (horizontalBehavior
                    == ConstraintWidget.DimensionBehaviour.MATCH_PARENT
                    || horizontalBehavior == ConstraintWidget.DimensionBehaviour.FIXED);
            boolean horizontalUseRatio = horizontalMatchConstraints && widget.mDimensionRatio > 0;
            boolean verticalUseRatio = verticalMatchConstraints && widget.mDimensionRatio > 0;

            if (child == null) {
                return;
            }
            LayoutParams params = (LayoutParams) child.getLayoutParams();

            int width = 0;
            int height = 0;
            int baseline = 0;

            if ((measure.measureStrategy == BasicMeasure.Measure.TRY_GIVEN_DIMENSIONS
                    || measure.measureStrategy == BasicMeasure.Measure.USE_GIVEN_DIMENSIONS)
                    || !(horizontalMatchConstraints
                    && widget.mMatchConstraintDefaultWidth == MATCH_CONSTRAINT_SPREAD
                    && verticalMatchConstraints
                    && widget.mMatchConstraintDefaultHeight == MATCH_CONSTRAINT_SPREAD)) {

                if (child instanceof VirtualLayout
                        && widget instanceof androidx.constraintlayout.core.widgets.VirtualLayout) {
                    androidx.constraintlayout.core.widgets.VirtualLayout layout =
                            (androidx.constraintlayout.core.widgets.VirtualLayout) widget;
                    ((VirtualLayout) child).onMeasure(layout, horizontalSpec, verticalSpec);
                } else {
                    child.measure(horizontalSpec, verticalSpec);
                }
                widget.setLastMeasureSpec(horizontalSpec, verticalSpec);

                int w = child.getMeasuredWidth();
                int h = child.getMeasuredHeight();
                baseline = child.getBaseline();

                width = w;
                height = h;

                if (DEBUG) {
                    String measurement = MeasureSpec.toString(horizontalSpec)
                            + " x " + MeasureSpec.toString(verticalSpec)
                            + " => " + width + " x " + height;
                    System.out.println("    (M) measure "
                            + " (" + widget.getDebugName() + ") : " + measurement);
                }

                if (widget.mMatchConstraintMinWidth > 0) {
                    width = Math.max(widget.mMatchConstraintMinWidth, width);
                }
                if (widget.mMatchConstraintMaxWidth > 0) {
                    width = Math.min(widget.mMatchConstraintMaxWidth, width);
                }
                if (widget.mMatchConstraintMinHeight > 0) {
                    height = Math.max(widget.mMatchConstraintMinHeight, height);
                }
                if (widget.mMatchConstraintMaxHeight > 0) {
                    height = Math.min(widget.mMatchConstraintMaxHeight, height);
                }

                boolean optimizeDirect = Optimizer.enabled(mOptimizationLevel,
                        Optimizer.OPTIMIZATION_DIRECT);
                if (!optimizeDirect) {
                    if (horizontalUseRatio && verticalDimensionKnown) {
                        float ratio = widget.mDimensionRatio;
                        width = (int) (0.5f + height * ratio);
                    } else if (verticalUseRatio && horizontalDimensionKnown) {
                        float ratio = widget.mDimensionRatio;
                        height = (int) (0.5f + width / ratio);
                    }
                }

                if (w != width || h != height) {
                    if (w != width) {
                        horizontalSpec = MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY);
                    }
                    if (h != height) {
                        verticalSpec = MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY);
                    }
                    child.measure(horizontalSpec, verticalSpec);

                    widget.setLastMeasureSpec(horizontalSpec, verticalSpec);
                    width = child.getMeasuredWidth();
                    height = child.getMeasuredHeight();
                    baseline = child.getBaseline();
                    if (DEBUG) {
                        String measurement2 = MeasureSpec.toString(horizontalSpec)
                                + " x " + MeasureSpec.toString(verticalSpec)
                                + " => " + width + " x " + height;
                        System.out.println("measure (b) " + widget.getDebugName()
                                + " : " + measurement2);
                    }
                }

            }

            boolean hasBaseline = baseline != -1;

            measure.measuredNeedsSolverPass = (width != measure.horizontalDimension)
                    || (height != measure.verticalDimension);
            if (params.mNeedsBaseline) {
                hasBaseline = true;
            }
            if (hasBaseline && baseline != -1 && widget.getBaselineDistance() != baseline) {
                measure.measuredNeedsSolverPass = true;
            }
            measure.measuredWidth = width;
            measure.measuredHeight = height;
            measure.measuredHasBaseline = hasBaseline;
            measure.measuredBaseline = baseline;
            if (mMetrics != null) {
                endMeasure = System.nanoTime();
                mMetrics.measuresWidgetsDuration += (endMeasure - startMeasure);
            }
        }

        /**
         * Returns true if the previous measure spec is equivalent to the new one.
         * - if it's the same...
         * - if it's not, but the previous was AT_MOST or UNSPECIFIED and the new one
         *   is EXACTLY with the same size.
         *
         * @param lastMeasureSpec
         * @param spec
         * @param widgetSize
         * @return
         */
        private boolean isSimilarSpec(int lastMeasureSpec, int spec, int widgetSize) {
            if (lastMeasureSpec == spec) {
                return true;
            }
            int lastMode = MeasureSpec.getMode(lastMeasureSpec);
            int mode = MeasureSpec.getMode(spec);
            int size = MeasureSpec.getSize(spec);
            return mode == MeasureSpec.EXACTLY
                    && (lastMode == MeasureSpec.AT_MOST || lastMode == MeasureSpec.UNSPECIFIED)
                    && widgetSize == size;
        }

        @Override
        public final void didMeasures() {
            final int widgetsCount = mLayout.getChildCount();
            for (int i = 0; i < widgetsCount; i++) {
                final View child = mLayout.getChildAt(i);
                if (child instanceof Placeholder) {
                    ((Placeholder) child).updatePostMeasure(mLayout);
                }
            }
            // TODO refactor into an updatePostMeasure interface
            final int helperCount = mLayout.mConstraintHelpers.size();
            if (helperCount > 0) {
                for (int i = 0; i < helperCount; i++) {
                    ConstraintHelper helper = mLayout.mConstraintHelpers.get(i);
                    helper.updatePostMeasure(mLayout);
                }
            }
        }
    }

    Measurer mMeasurer = new Measurer(this);

    private void init(AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        mLayoutWidget.setCompanionWidget(this);
        mLayoutWidget.setMeasurer(mMeasurer);
        mChildrenByIds.put(getId(), this);
        mConstraintSet = null;
        if (attrs != null) {
            TypedArray a = getContext().obtainStyledAttributes(attrs,
                    R.styleable.ConstraintLayout_Layout, defStyleAttr, defStyleRes);
            final int count = a.getIndexCount();
            for (int i = 0; i < count; i++) {
                int attr = a.getIndex(i);
                if (attr == R.styleable.ConstraintLayout_Layout_android_minWidth) {
                    mMinWidth = a.getDimensionPixelOffset(attr, mMinWidth);
                } else if (attr == R.styleable.ConstraintLayout_Layout_android_minHeight) {
                    mMinHeight = a.getDimensionPixelOffset(attr, mMinHeight);
                } else if (attr == R.styleable.ConstraintLayout_Layout_android_maxWidth) {
                    mMaxWidth = a.getDimensionPixelOffset(attr, mMaxWidth);
                } else if (attr == R.styleable.ConstraintLayout_Layout_android_maxHeight) {
                    mMaxHeight = a.getDimensionPixelOffset(attr, mMaxHeight);
                } else if (attr == R.styleable.ConstraintLayout_Layout_layout_optimizationLevel) {
                    mOptimizationLevel = a.getInt(attr, mOptimizationLevel);
                } else if (attr == R.styleable.ConstraintLayout_Layout_layoutDescription) {
                    int id = a.getResourceId(attr, 0);
                    if (id != 0) {
                        try {
                            parseLayoutDescription(id);
                        } catch (Resources.NotFoundException e) {
                            mConstraintLayoutSpec = null;
                        }
                    }
                } else if (attr == R.styleable.ConstraintLayout_Layout_constraintSet) {
                    int id = a.getResourceId(attr, 0);
                    try {
                        mConstraintSet = new ConstraintSet();
                        mConstraintSet.load(getContext(), id);
                    } catch (Resources.NotFoundException e) {
                        mConstraintSet = null;
                    }
                    mConstraintSetId = id;
                }
            }
            a.recycle();
        }
        mLayoutWidget.setOptimizationLevel(mOptimizationLevel);
    }

    /**
     * Subclasses can override the handling of layoutDescription
     *
     * @param id
     */
    protected void parseLayoutDescription(int id) {
        mConstraintLayoutSpec = new ConstraintLayoutStates(getContext(), this, id);
    }

    /**
     *
     */
    @Override
    public void onViewAdded(View view) {
        super.onViewAdded(view);
        ConstraintWidget widget = getViewWidget(view);
        if (view instanceof androidx.constraintlayout.widget.Guideline) {
            if (!(widget instanceof Guideline)) {
                LayoutParams layoutParams = (LayoutParams) view.getLayoutParams();
                layoutParams.mWidget = new Guideline();
                layoutParams.mIsGuideline = true;
                ((Guideline) layoutParams.mWidget).setOrientation(layoutParams.orientation);
            }
        }
        if (view instanceof ConstraintHelper) {
            ConstraintHelper helper = (ConstraintHelper) view;
            helper.validateParams();
            LayoutParams layoutParams = (LayoutParams) view.getLayoutParams();
            layoutParams.mIsHelper = true;
            if (!mConstraintHelpers.contains(helper)) {
                mConstraintHelpers.add(helper);
            }
        }
        mChildrenByIds.put(view.getId(), view);
        mDirtyHierarchy = true;
    }

    /**
     *
     */
    @Override
    public void onViewRemoved(View view) {
        super.onViewRemoved(view);
        mChildrenByIds.remove(view.getId());
        ConstraintWidget widget = getViewWidget(view);
        mLayoutWidget.remove(widget);
        mConstraintHelpers.remove(view);
        mDirtyHierarchy = true;
    }

    /**
     * Set the min width for this view
     *
     * @param value
     */
    public void setMinWidth(int value) {
        if (value == mMinWidth) {
            return;
        }
        mMinWidth = value;
        requestLayout();
    }

    /**
     * Set the min height for this view
     *
     * @param value
     */
    public void setMinHeight(int value) {
        if (value == mMinHeight) {
            return;
        }
        mMinHeight = value;
        requestLayout();
    }

    /**
     * The minimum width of this view.
     *
     * @return The minimum width of this view
     * @see #setMinWidth(int)
     */
    public int getMinWidth() {
        return mMinWidth;
    }

    /**
     * The minimum height of this view.
     *
     * @return The minimum height of this view
     * @see #setMinHeight(int)
     */
    public int getMinHeight() {
        return mMinHeight;
    }

    /**
     * Set the max width for this view
     *
     * @param value
     */
    public void setMaxWidth(int value) {
        if (value == mMaxWidth) {
            return;
        }
        mMaxWidth = value;
        requestLayout();
    }

    /**
     * Set the max height for this view
     *
     * @param value
     */
    public void setMaxHeight(int value) {
        if (value == mMaxHeight) {
            return;
        }
        mMaxHeight = value;
        requestLayout();
    }

    /*
     * The maximum width of this view.
     *
     * @return The maximum width of this view
     *
     * @see #setMaxWidth(int)
     */
    public int getMaxWidth() {
        return mMaxWidth;
    }

    /**
     * The maximum height of this view.
     *
     * @return The maximum height of this view
     * @see #setMaxHeight(int)
     */
    public int getMaxHeight() {
        return mMaxHeight;
    }

    private boolean updateHierarchy() {
        final int count = getChildCount();

        boolean recompute = false;
        for (int i = 0; i < count; i++) {
            final View child = getChildAt(i);
            if (child.isLayoutRequested()) {
                recompute = true;
                break;
            }
        }
        if (recompute) {
            setChildrenConstraints();
        }
        return recompute;
    }

    private void setChildrenConstraints() {
        final boolean isInEditMode = DEBUG || isInEditMode();

        final int count = getChildCount();

        // Make sure everything is fully reset before anything else
        for (int i = 0; i < count; i++) {
            View child = getChildAt(i);
            ConstraintWidget widget = getViewWidget(child);
            if (widget == null) {
                continue;
            }
            widget.reset();
        }

        if (isInEditMode) {
            // In design mode, let's make sure we keep track of the ids; in Studio, a build step
            // might not have been done yet, so asking the system for ids can break. So to be safe,
            // we save the current ids, which helpers can ask for.
            for (int i = 0; i < count; i++) {
                final View view = getChildAt(i);
                try {
                    String IdAsString = getResources().getResourceName(view.getId());
                    setDesignInformation(DESIGN_INFO_ID, IdAsString, view.getId());
                    int slashIndex = IdAsString.indexOf('/');
                    if (slashIndex != -1) {
                        IdAsString = IdAsString.substring(slashIndex + 1);
                    }
                    getTargetWidget(view.getId()).setDebugName(IdAsString);
                } catch (Resources.NotFoundException e) {
                    // nothing
                }
            }
        } else if (DEBUG) {
            mLayoutWidget.setDebugName("root");
            for (int i = 0; i < count; i++) {
                final View view = getChildAt(i);
                try {
                    String IdAsString = getResources().getResourceName(view.getId());
                    setDesignInformation(DESIGN_INFO_ID, IdAsString, view.getId());
                    int slashIndex = IdAsString.indexOf('/');
                    if (slashIndex != -1) {
                        IdAsString = IdAsString.substring(slashIndex + 1);
                    }
                    getTargetWidget(view.getId()).setDebugName(IdAsString);
                } catch (Resources.NotFoundException e) {
                    // nothing
                }
            }
        }

        if (USE_CONSTRAINTS_HELPER && mConstraintSetId != -1) {
            for (int i = 0; i < count; i++) {
                final View child = getChildAt(i);
                if (child.getId() == mConstraintSetId && child instanceof Constraints) {
                    mConstraintSet = ((Constraints) child).getConstraintSet();
                }
            }
        }

        if (mConstraintSet != null) {
            mConstraintSet.applyToInternal(this, true);
        }

        mLayoutWidget.removeAllChildren();

        final int helperCount = mConstraintHelpers.size();
        if (helperCount > 0) {
            for (int i = 0; i < helperCount; i++) {
                ConstraintHelper helper = mConstraintHelpers.get(i);
                helper.updatePreLayout(this);
            }
        }

        // TODO refactor into an updatePreLayout interface
        for (int i = 0; i < count; i++) {
            View child = getChildAt(i);
            if (child instanceof Placeholder) {
                ((Placeholder) child).updatePreLayout(this);
            }
        }

        mTempMapIdToWidget.clear();
        mTempMapIdToWidget.put(PARENT_ID, mLayoutWidget);
        mTempMapIdToWidget.put(getId(), mLayoutWidget);
        for (int i = 0; i < count; i++) {
            final View child = getChildAt(i);
            ConstraintWidget widget = getViewWidget(child);
            mTempMapIdToWidget.put(child.getId(), widget);
        }

        for (int i = 0; i < count; i++) {
            final View child = getChildAt(i);
            ConstraintWidget widget = getViewWidget(child);
            if (widget == null) {
                continue;
            }
            final LayoutParams layoutParams = (LayoutParams) child.getLayoutParams();
            mLayoutWidget.add(widget);
            applyConstraintsFromLayoutParams(isInEditMode, child, widget,
                    layoutParams, mTempMapIdToWidget);
        }
    }


    protected void applyConstraintsFromLayoutParams(boolean isInEditMode,
                                                    View child,
                                                    ConstraintWidget widget,
                                                    LayoutParams layoutParams,
                                                    SparseArray<ConstraintWidget> idToWidget) {

        layoutParams.validate();
        layoutParams.helped = false;

        widget.setVisibility(child.getVisibility());
        if (layoutParams.mIsInPlaceholder) {
            widget.setInPlaceholder(true);
            widget.setVisibility(View.GONE);
        }
        widget.setCompanionWidget(child);

        if (child instanceof ConstraintHelper) {
            ConstraintHelper helper = (ConstraintHelper) child;
            helper.resolveRtl(widget, mLayoutWidget.isRtl());
        }
        if (layoutParams.mIsGuideline) {
            Guideline guideline = (Guideline) widget;
            int resolvedGuideBegin = layoutParams.mResolvedGuideBegin;
            int resolvedGuideEnd = layoutParams.mResolvedGuideEnd;
            float resolvedGuidePercent = layoutParams.mResolvedGuidePercent;
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR1) {
                resolvedGuideBegin = layoutParams.guideBegin;
                resolvedGuideEnd = layoutParams.guideEnd;
                resolvedGuidePercent = layoutParams.guidePercent;
            }
            if (resolvedGuidePercent != UNSET) {
                guideline.setGuidePercent(resolvedGuidePercent);
            } else if (resolvedGuideBegin != UNSET) {
                guideline.setGuideBegin(resolvedGuideBegin);
            } else if (resolvedGuideEnd != UNSET) {
                guideline.setGuideEnd(resolvedGuideEnd);
            }
        } else {
            // Get the left/right constraints resolved for RTL
            int resolvedLeftToLeft = layoutParams.mResolvedLeftToLeft;
            int resolvedLeftToRight = layoutParams.mResolvedLeftToRight;
            int resolvedRightToLeft = layoutParams.mResolvedRightToLeft;
            int resolvedRightToRight = layoutParams.mResolvedRightToRight;
            int resolveGoneLeftMargin = layoutParams.mResolveGoneLeftMargin;
            int resolveGoneRightMargin = layoutParams.mResolveGoneRightMargin;
            float resolvedHorizontalBias = layoutParams.mResolvedHorizontalBias;

            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR1) {
                // Pre JB MR1, left/right should take precedence, unless they are
                // not defined and somehow a corresponding start/end constraint exists
                resolvedLeftToLeft = layoutParams.leftToLeft;
                resolvedLeftToRight = layoutParams.leftToRight;
                resolvedRightToLeft = layoutParams.rightToLeft;
                resolvedRightToRight = layoutParams.rightToRight;
                resolveGoneLeftMargin = layoutParams.goneLeftMargin;
                resolveGoneRightMargin = layoutParams.goneRightMargin;
                resolvedHorizontalBias = layoutParams.horizontalBias;

                if (resolvedLeftToLeft == UNSET && resolvedLeftToRight == UNSET) {
                    if (layoutParams.startToStart != UNSET) {
                        resolvedLeftToLeft = layoutParams.startToStart;
                    } else if (layoutParams.startToEnd != UNSET) {
                        resolvedLeftToRight = layoutParams.startToEnd;
                    }
                }
                if (resolvedRightToLeft == UNSET && resolvedRightToRight == UNSET) {
                    if (layoutParams.endToStart != UNSET) {
                        resolvedRightToLeft = layoutParams.endToStart;
                    } else if (layoutParams.endToEnd != UNSET) {
                        resolvedRightToRight = layoutParams.endToEnd;
                    }
                }
            }

            // Circular constraint
            if (layoutParams.circleConstraint != UNSET) {
                ConstraintWidget target = idToWidget.get(layoutParams.circleConstraint);
                if (target != null) {
                    widget.connectCircularConstraint(target,
                            layoutParams.circleAngle, layoutParams.circleRadius);
                }
            } else {
                // Left constraint
                if (resolvedLeftToLeft != UNSET) {
                    ConstraintWidget target = idToWidget.get(resolvedLeftToLeft);
                    if (target != null) {
                        widget.immediateConnect(ConstraintAnchor.Type.LEFT, target,
                                ConstraintAnchor.Type.LEFT, layoutParams.leftMargin,
                                resolveGoneLeftMargin);
                    }
                } else if (resolvedLeftToRight != UNSET) {
                    ConstraintWidget target = idToWidget.get(resolvedLeftToRight);
                    if (target != null) {
                        widget.immediateConnect(ConstraintAnchor.Type.LEFT, target,
                                ConstraintAnchor.Type.RIGHT, layoutParams.leftMargin,
                                resolveGoneLeftMargin);
                    }
                }

                // Right constraint
                if (resolvedRightToLeft != UNSET) {
                    ConstraintWidget target = idToWidget.get(resolvedRightToLeft);
                    if (target != null) {
                        widget.immediateConnect(ConstraintAnchor.Type.RIGHT, target,
                                ConstraintAnchor.Type.LEFT, layoutParams.rightMargin,
                                resolveGoneRightMargin);
                    }
                } else if (resolvedRightToRight != UNSET) {
                    ConstraintWidget target = idToWidget.get(resolvedRightToRight);
                    if (target != null) {
                        widget.immediateConnect(ConstraintAnchor.Type.RIGHT, target,
                                ConstraintAnchor.Type.RIGHT, layoutParams.rightMargin,
                                resolveGoneRightMargin);
                    }
                }

                // Top constraint
                if (layoutParams.topToTop != UNSET) {
                    ConstraintWidget target = idToWidget.get(layoutParams.topToTop);
                    if (target != null) {
                        widget.immediateConnect(ConstraintAnchor.Type.TOP, target,
                                ConstraintAnchor.Type.TOP, layoutParams.topMargin,
                                layoutParams.goneTopMargin);
                    }
                } else if (layoutParams.topToBottom != UNSET) {
                    ConstraintWidget target = idToWidget.get(layoutParams.topToBottom);
                    if (target != null) {
                        widget.immediateConnect(ConstraintAnchor.Type.TOP, target,
                                ConstraintAnchor.Type.BOTTOM, layoutParams.topMargin,
                                layoutParams.goneTopMargin);
                    }
                }

                // Bottom constraint
                if (layoutParams.bottomToTop != UNSET) {
                    ConstraintWidget target = idToWidget.get(layoutParams.bottomToTop);
                    if (target != null) {
                        widget.immediateConnect(ConstraintAnchor.Type.BOTTOM, target,
                                ConstraintAnchor.Type.TOP, layoutParams.bottomMargin,
                                layoutParams.goneBottomMargin);
                    }
                } else if (layoutParams.bottomToBottom != UNSET) {
                    ConstraintWidget target = idToWidget.get(layoutParams.bottomToBottom);
                    if (target != null) {
                        widget.immediateConnect(ConstraintAnchor.Type.BOTTOM, target,
                                ConstraintAnchor.Type.BOTTOM, layoutParams.bottomMargin,
                                layoutParams.goneBottomMargin);
                    }
                }

                // Baseline constraint
                if (layoutParams.baselineToBaseline != UNSET) {
                    setWidgetBaseline(widget, layoutParams, idToWidget,
                            layoutParams.baselineToBaseline, ConstraintAnchor.Type.BASELINE);
                } else if (layoutParams.baselineToTop != UNSET) {
                    setWidgetBaseline(widget, layoutParams, idToWidget,
                            layoutParams.baselineToTop, ConstraintAnchor.Type.TOP);
                } else if (layoutParams.baselineToBottom != UNSET) {
                    setWidgetBaseline(widget, layoutParams, idToWidget,
                            layoutParams.baselineToBottom, ConstraintAnchor.Type.BOTTOM);
                }

                if (resolvedHorizontalBias >= 0) {
                    widget.setHorizontalBiasPercent(resolvedHorizontalBias);
                }
                if (layoutParams.verticalBias >= 0) {
                    widget.setVerticalBiasPercent(layoutParams.verticalBias);
                }
            }

            if (isInEditMode && ((layoutParams.editorAbsoluteX != UNSET)
                    || (layoutParams.editorAbsoluteY != UNSET))) {
                widget.setOrigin(layoutParams.editorAbsoluteX, layoutParams.editorAbsoluteY);
            }

            // FIXME: need to agree on the correct magic value for this
            //  rather than simply using zero.
            if (!layoutParams.mHorizontalDimensionFixed) {
                if (layoutParams.width == MATCH_PARENT) {
                    if (layoutParams.constrainedWidth) {
                        widget.setHorizontalDimensionBehaviour(ConstraintWidget
                                .DimensionBehaviour.MATCH_CONSTRAINT);
                    } else {
                        widget.setHorizontalDimensionBehaviour(ConstraintWidget
                                .DimensionBehaviour.MATCH_PARENT);
                    }
                    widget.getAnchor(ConstraintAnchor.Type.LEFT).mMargin = layoutParams.leftMargin;
                    widget.getAnchor(ConstraintAnchor.Type.RIGHT).mMargin =
                            layoutParams.rightMargin;
                } else {
                    widget.setHorizontalDimensionBehaviour(ConstraintWidget
                            .DimensionBehaviour.MATCH_CONSTRAINT);
                    widget.setWidth(0);
                }
            } else {
                widget.setHorizontalDimensionBehaviour(ConstraintWidget
                        .DimensionBehaviour.FIXED);
                widget.setWidth(layoutParams.width);
                if (layoutParams.width == WRAP_CONTENT) {
                    widget.setHorizontalDimensionBehaviour(ConstraintWidget
                            .DimensionBehaviour.WRAP_CONTENT);
                }
            }
            if (!layoutParams.mVerticalDimensionFixed) {
                if (layoutParams.height == MATCH_PARENT) {
                    if (layoutParams.constrainedHeight) {
                        widget.setVerticalDimensionBehaviour(ConstraintWidget
                                .DimensionBehaviour.MATCH_CONSTRAINT);
                    } else {
                        widget.setVerticalDimensionBehaviour(ConstraintWidget
                                .DimensionBehaviour.MATCH_PARENT);
                    }
                    widget.getAnchor(ConstraintAnchor.Type.TOP).mMargin = layoutParams.topMargin;
                    widget.getAnchor(ConstraintAnchor.Type.BOTTOM).mMargin =
                            layoutParams.bottomMargin;
                } else {
                    widget.setVerticalDimensionBehaviour(ConstraintWidget
                            .DimensionBehaviour.MATCH_CONSTRAINT);
                    widget.setHeight(0);
                }
            } else {
                widget.setVerticalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.FIXED);
                widget.setHeight(layoutParams.height);
                if (layoutParams.height == WRAP_CONTENT) {
                    widget.setVerticalDimensionBehaviour(ConstraintWidget
                            .DimensionBehaviour.WRAP_CONTENT);
                }
            }

            widget.setDimensionRatio(layoutParams.dimensionRatio);
            widget.setHorizontalWeight(layoutParams.horizontalWeight);
            widget.setVerticalWeight(layoutParams.verticalWeight);
            widget.setHorizontalChainStyle(layoutParams.horizontalChainStyle);
            widget.setVerticalChainStyle(layoutParams.verticalChainStyle);
            widget.setWrapBehaviorInParent(layoutParams.wrapBehaviorInParent);
            widget.setHorizontalMatchStyle(layoutParams.matchConstraintDefaultWidth,
                    layoutParams.matchConstraintMinWidth, layoutParams.matchConstraintMaxWidth,
                    layoutParams.matchConstraintPercentWidth);
            widget.setVerticalMatchStyle(layoutParams.matchConstraintDefaultHeight,
                    layoutParams.matchConstraintMinHeight, layoutParams.matchConstraintMaxHeight,
                    layoutParams.matchConstraintPercentHeight);
        }
    }

    private void setWidgetBaseline(ConstraintWidget widget,
                                   LayoutParams layoutParams,
                                   SparseArray<ConstraintWidget> idToWidget,
                                   int baselineTarget,
                                   ConstraintAnchor.Type type) {
        View view = mChildrenByIds.get(baselineTarget);
        ConstraintWidget target = idToWidget.get(baselineTarget);
        if (target != null && view != null && view.getLayoutParams() instanceof LayoutParams) {
            layoutParams.mNeedsBaseline = true;
            if (type == ConstraintAnchor.Type.BASELINE) { // baseline to baseline
                LayoutParams targetParams = (LayoutParams) view.getLayoutParams();
                targetParams.mNeedsBaseline = true;
                targetParams.mWidget.setHasBaseline(true);
            }
            ConstraintAnchor baseline = widget.getAnchor(ConstraintAnchor.Type.BASELINE);
            ConstraintAnchor targetAnchor = target.getAnchor(type);
            baseline.connect(targetAnchor, layoutParams.baselineMargin,
                    layoutParams.goneBaselineMargin, true);
            widget.setHasBaseline(true);
            widget.getAnchor(ConstraintAnchor.Type.TOP).reset();
            widget.getAnchor(ConstraintAnchor.Type.BOTTOM).reset();
        }
    }

    private ConstraintWidget getTargetWidget(int id) {
        if (id == LayoutParams.PARENT_ID) {
            return mLayoutWidget;
        } else {
            View view = mChildrenByIds.get(id);
            if (view == null) {
                view = findViewById(id);
                if (view != null && view != this && view.getParent() == this) {
                    onViewAdded(view);
                }
            }
            if (view == this) {
                return mLayoutWidget;
            }
            return view == null ? null : ((LayoutParams) view.getLayoutParams()).mWidget;
        }
    }

    /**
     * @param view
     * @return
     *
     */
    public final ConstraintWidget getViewWidget(View view) {
        if (view == this) {
            return mLayoutWidget;
        }
        if (view != null) {
            if (view.getLayoutParams() instanceof LayoutParams) {
                return ((LayoutParams) view.getLayoutParams()).mWidget;
            }
            view.setLayoutParams(generateLayoutParams(view.getLayoutParams()));
            if (view.getLayoutParams() instanceof LayoutParams) {
                return ((LayoutParams) view.getLayoutParams()).mWidget;
            }
        }
        return null;
    }

    /**
     * @param metrics
     * Fills metrics object
     */
    public void fillMetrics(Metrics metrics) {
        mMetrics = metrics;
        mLayoutWidget.fillMetrics(metrics);
    }

    private int mOnMeasureWidthMeasureSpec = 0;
    private int mOnMeasureHeightMeasureSpec = 0;

    /**
     * Handles measuring a layout
     *
     * @param layout
     * @param optimizationLevel
     * @param widthMeasureSpec
     * @param heightMeasureSpec
     */
    protected void resolveSystem(ConstraintWidgetContainer layout,
                                 int optimizationLevel,
                                 int widthMeasureSpec,
                                 int heightMeasureSpec) {
        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);

        int paddingY = Math.max(0, getPaddingTop());
        int paddingBottom = Math.max(0, getPaddingBottom());
        int paddingHeight = paddingY + paddingBottom;
        int paddingWidth = getPaddingWidth();
        int paddingX;
        mMeasurer.captureLayoutInfo(widthMeasureSpec, heightMeasureSpec, paddingY, paddingBottom,
                paddingWidth, paddingHeight);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            int paddingStart = Math.max(0, getPaddingStart());
            int paddingEnd = Math.max(0, getPaddingEnd());
            if (paddingStart > 0 || paddingEnd > 0) {
                if (isRtl()) {
                    paddingX = paddingEnd;
                } else {
                    paddingX = paddingStart;
                }
            } else {
                paddingX = Math.max(0, getPaddingLeft());
            }
        } else {
            paddingX = Math.max(0, getPaddingLeft());
        }

        widthSize -= paddingWidth;
        heightSize -= paddingHeight;

        setSelfDimensionBehaviour(layout, widthMode, widthSize, heightMode, heightSize);

        layout.measure(optimizationLevel, widthMode, widthSize, heightMode, heightSize,
                mLastMeasureWidth, mLastMeasureHeight, paddingX, paddingY);
    }

    /**
     * Handles calling setMeasuredDimension()
     *
     * @param widthMeasureSpec
     * @param heightMeasureSpec
     * @param measuredWidth
     * @param measuredHeight
     * @param isWidthMeasuredTooSmall
     * @param isHeightMeasuredTooSmall
     */
    protected void resolveMeasuredDimension(int widthMeasureSpec,
                                            int heightMeasureSpec,
                                            int measuredWidth,
                                            int measuredHeight,
                                            boolean isWidthMeasuredTooSmall,
                                            boolean isHeightMeasuredTooSmall) {
        int childState = 0;
        int heightPadding = mMeasurer.mPaddingHeight;
        int widthPadding = mMeasurer.mPaddingWidth;

        int androidLayoutWidth = measuredWidth + widthPadding;
        int androidLayoutHeight = measuredHeight + heightPadding;

        int resolvedWidthSize = resolveSizeAndState(androidLayoutWidth,
                widthMeasureSpec, childState);
        int resolvedHeightSize = resolveSizeAndState(androidLayoutHeight, heightMeasureSpec,
                childState << MEASURED_HEIGHT_STATE_SHIFT);
        resolvedWidthSize &= MEASURED_SIZE_MASK;
        resolvedHeightSize &= MEASURED_SIZE_MASK;
        resolvedWidthSize = Math.min(mMaxWidth, resolvedWidthSize);
        resolvedHeightSize = Math.min(mMaxHeight, resolvedHeightSize);
        if (isWidthMeasuredTooSmall) {
            resolvedWidthSize |= MEASURED_STATE_TOO_SMALL;
        }
        if (isHeightMeasuredTooSmall) {
            resolvedHeightSize |= MEASURED_STATE_TOO_SMALL;
        }
        setMeasuredDimension(resolvedWidthSize, resolvedHeightSize);
        mLastMeasureWidth = resolvedWidthSize;
        mLastMeasureHeight = resolvedHeightSize;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        long time = 0;
        if (mMetrics != null) {
            time = System.nanoTime();
            mMetrics.mChildCount = getChildCount();
            mMetrics.mMeasureCalls++;
        }
        mDirtyHierarchy |= dynamicUpdateConstraints(widthMeasureSpec,  heightMeasureSpec);

        @SuppressWarnings({"PointlessBooleanExpression", "ConstantConditions",
                "ComplexBooleanConstant"})  // TODO re-enable
        boolean sameSpecsAsPreviousMeasure =
                false && (mOnMeasureWidthMeasureSpec == widthMeasureSpec
                && mOnMeasureHeightMeasureSpec == heightMeasureSpec);
        //noinspection ConstantConditions
        if (!mDirtyHierarchy && !sameSpecsAsPreviousMeasure) {
            // it's possible that, if we are already marked for a relayout,
            // a view would not call to request a layout;
            // in that case we'd miss updating the hierarchy correctly
            // (window insets change may do that -- we receive
            // a second onMeasure before onLayout).
            // We have to iterate on our children to verify that none set a request layout flag...
            final int count = getChildCount();
            for (int i = 0; i < count; i++) {
                final View child = getChildAt(i);
                if (child.isLayoutRequested()) {
                    if (DEBUG) {
                        System.out.println("### CHILD " + child
                                + " REQUESTED LAYOUT, FORCE DIRTY HIERARCHY");
                    }
                    mDirtyHierarchy = true;
                    break;
                }
            }
        }

        if (!mDirtyHierarchy) {
            //noinspection ConstantConditions
            if (sameSpecsAsPreviousMeasure) {
                resolveMeasuredDimension(widthMeasureSpec, heightMeasureSpec,
                        mLayoutWidget.getWidth(), mLayoutWidget.getHeight(),
                        mLayoutWidget.isWidthMeasuredTooSmall(),
                        mLayoutWidget.isHeightMeasuredTooSmall());
                if (mMetrics != null) {
                    mMetrics.mMeasureDuration +=  System.nanoTime() - time;
                }
                return;
            }
            if (OPTIMIZE_HEIGHT_CHANGE
                    && mOnMeasureWidthMeasureSpec == widthMeasureSpec
                    && MeasureSpec.getMode(widthMeasureSpec) == MeasureSpec.EXACTLY
                    && MeasureSpec.getMode(heightMeasureSpec) == MeasureSpec.AT_MOST
                    && MeasureSpec.getMode(mOnMeasureHeightMeasureSpec) == MeasureSpec.AT_MOST) {
                int newSize = MeasureSpec.getSize(heightMeasureSpec);
                if (DEBUG) {
                    System.out.println("### COMPATIBLE REQ " + newSize
                            + " >= ? " + mLayoutWidget.getHeight());
                }
                if (newSize >= mLayoutWidget.getHeight()
                        && !mLayoutWidget.isHeightMeasuredTooSmall()) {
                    mOnMeasureWidthMeasureSpec = widthMeasureSpec;
                    mOnMeasureHeightMeasureSpec = heightMeasureSpec;
                    resolveMeasuredDimension(widthMeasureSpec, heightMeasureSpec,
                            mLayoutWidget.getWidth(), mLayoutWidget.getHeight(),
                            mLayoutWidget.isWidthMeasuredTooSmall(),
                            mLayoutWidget.isHeightMeasuredTooSmall());
                    if (mMetrics != null) {
                        mMetrics.mMeasureDuration +=  System.nanoTime() - time;
                    }
                    return;
                }
            }
        }
        mOnMeasureWidthMeasureSpec = widthMeasureSpec;
        mOnMeasureHeightMeasureSpec = heightMeasureSpec;

        if (DEBUG) {
            System.out.println("### ON MEASURE " + mDirtyHierarchy
                    + " of " + mLayoutWidget.getDebugName()
                    + " onMeasure width: " + MeasureSpec.toString(widthMeasureSpec)
                    + " height: " + MeasureSpec.toString(heightMeasureSpec) + this);
        }

        mLayoutWidget.setRtl(isRtl());

        if (mDirtyHierarchy) {
            mDirtyHierarchy = false;
            if (updateHierarchy()) {
                mLayoutWidget.updateHierarchy();
            }
        }
        mLayoutWidget.fillMetrics(mMetrics);

        resolveSystem(mLayoutWidget, mOptimizationLevel, widthMeasureSpec, heightMeasureSpec);
        resolveMeasuredDimension(widthMeasureSpec, heightMeasureSpec,
                mLayoutWidget.getWidth(), mLayoutWidget.getHeight(),
                mLayoutWidget.isWidthMeasuredTooSmall(), mLayoutWidget.isHeightMeasuredTooSmall());

        if (mMetrics != null) {
            mMetrics.mMeasureDuration +=  System.nanoTime() - time;
        }
        if (DEBUG) {
            time = System.nanoTime() - time;
            System.out.println(mLayoutWidget.getDebugName() + " (" + getChildCount()
                    + ") DONE onMeasure width: " + MeasureSpec.toString(widthMeasureSpec)
                    + " height: " + MeasureSpec.toString(heightMeasureSpec) + " => "
                    + mLastMeasureWidth + " x " + mLastMeasureHeight
                    + " lasted " + time
            );
        }
    }

    protected boolean isRtl() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            boolean isRtlSupported = (getContext().getApplicationInfo().flags
                    & ApplicationInfo.FLAG_SUPPORTS_RTL) != 0;
            return isRtlSupported && (View.LAYOUT_DIRECTION_RTL == getLayoutDirection());
        }
        return false;
    }

    /**
     * Compute the padding width, taking in account RTL start/end padding if available and present.
     * @return padding width
     */
    private int getPaddingWidth() {
        int widthPadding = Math.max(0, getPaddingLeft()) + Math.max(0, getPaddingRight());
        int rtlPadding = 0;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            rtlPadding = Math.max(0, getPaddingStart()) + Math.max(0, getPaddingEnd());
        }
        if (rtlPadding > 0) {
            widthPadding = rtlPadding;
        }
        return widthPadding;
    }

    protected void setSelfDimensionBehaviour(ConstraintWidgetContainer layout,
                                             int widthMode,
                                             int widthSize,
                                             int heightMode,
                                             int heightSize) {

        int heightPadding = mMeasurer.mPaddingHeight;
        int widthPadding = mMeasurer.mPaddingWidth;

        ConstraintWidget.DimensionBehaviour widthBehaviour =
                ConstraintWidget.DimensionBehaviour.FIXED;
        ConstraintWidget.DimensionBehaviour heightBehaviour =
                ConstraintWidget.DimensionBehaviour.FIXED;

        int desiredWidth = 0;
        int desiredHeight = 0;
        final int childCount = getChildCount();

        switch (widthMode) {
            case MeasureSpec.AT_MOST: {
                widthBehaviour = ConstraintWidget.DimensionBehaviour.WRAP_CONTENT;
                desiredWidth = widthSize;
                if (childCount == 0) {
                    desiredWidth = Math.max(0, mMinWidth);
                }
            }
            break;
            case MeasureSpec.UNSPECIFIED: {
                widthBehaviour = ConstraintWidget.DimensionBehaviour.WRAP_CONTENT;
                if (childCount == 0) {
                    desiredWidth = Math.max(0, mMinWidth);
                }
            }
            break;
            case MeasureSpec.EXACTLY: {
                desiredWidth = Math.min(mMaxWidth - widthPadding, widthSize);
            }
        }
        switch (heightMode) {
            case MeasureSpec.AT_MOST: {
                heightBehaviour = ConstraintWidget.DimensionBehaviour.WRAP_CONTENT;
                desiredHeight = heightSize;
                if (childCount == 0) {
                    desiredHeight = Math.max(0, mMinHeight);
                }
            }
            break;
            case MeasureSpec.UNSPECIFIED: {
                heightBehaviour = ConstraintWidget.DimensionBehaviour.WRAP_CONTENT;
                if (childCount == 0) {
                    desiredHeight = Math.max(0, mMinHeight);
                }
            }
            break;
            case MeasureSpec.EXACTLY: {
                desiredHeight = Math.min(mMaxHeight - heightPadding, heightSize);
            }
        }

        if (desiredWidth != layout.getWidth() || desiredHeight != layout.getHeight()) {
            layout.invalidateMeasures();
        }
        layout.setX(0);
        layout.setY(0);
        layout.setMaxWidth(mMaxWidth - widthPadding);
        layout.setMaxHeight(mMaxHeight - heightPadding);
        layout.setMinWidth(0);
        layout.setMinHeight(0);
        layout.setHorizontalDimensionBehaviour(widthBehaviour);
        layout.setWidth(desiredWidth);
        layout.setVerticalDimensionBehaviour(heightBehaviour);
        layout.setHeight(desiredHeight);
        layout.setMinWidth(mMinWidth - widthPadding);
        layout.setMinHeight(mMinHeight - heightPadding);
    }

    /**
     * Set the State of the ConstraintLayout, causing it to load a particular ConstraintSet.
     * For states with variants the variant with matching width and height
     * constraintSet will be chosen
     *
     * @param id           the constraint set state
     * @param screenWidth  the width of the screen in pixels
     * @param screenHeight the height of the screen in pixels
     */
    public void setState(int id, int screenWidth, int screenHeight) {
        if (mConstraintLayoutSpec != null) {
            mConstraintLayoutSpec.updateConstraints(id, screenWidth, screenHeight);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        if (mMetrics != null) {
            mMetrics.mNumberOfLayouts++;
        }
        if (DEBUG) {
            System.out.println(mLayoutWidget.getDebugName() + " onLayout changed: "
                    + changed + " left: " + left + " top: " + top
                    + " right: " + right + " bottom: " + bottom
                    + " (" + (right - left) + " x " + (bottom - top) + ")");
        }
        final int widgetsCount = getChildCount();
        final boolean isInEditMode = isInEditMode();
        for (int i = 0; i < widgetsCount; i++) {
            final View child = getChildAt(i);
            LayoutParams params = (LayoutParams) child.getLayoutParams();
            ConstraintWidget widget = params.mWidget;

            if (child.getVisibility() == GONE
                    && !params.mIsGuideline
                    && !params.mIsHelper
                    && !params.mIsVirtualGroup
                    && !isInEditMode) {
                // If we are in edit mode, let's layout the widget
                // so that they are at "the right place"
                // visually in the editor (as we get our positions from layoutlib)
                continue;
            }
            if (params.mIsInPlaceholder) {
                continue;
            }
            int l = widget.getX();
            int t = widget.getY();
            int r = l + widget.getWidth();
            int b = t + widget.getHeight();

            if (DEBUG) {
                if (child.getVisibility() != View.GONE
                        && (child.getMeasuredWidth() != widget.getWidth()
                        || child.getMeasuredHeight() != widget.getHeight())) {
                    int deltaX = Math.abs(child.getMeasuredWidth() - widget.getWidth());
                    int deltaY = Math.abs(child.getMeasuredHeight() - widget.getHeight());
                    if (deltaX > 1 || deltaY > 1) {
                        System.out.println("child " + child
                                + " measuredWidth " + child.getMeasuredWidth()
                                + " vs " + widget.getWidth()
                                + " x measureHeight " + child.getMeasuredHeight()
                                + " vs " + widget.getHeight());
                    }
                }
            }

            child.layout(l, t, r, b);
            if (child instanceof Placeholder) {
                Placeholder holder = (Placeholder) child;
                View content = holder.getContent();
                if (content != null) {
                    content.setVisibility(VISIBLE);
                    content.layout(l, t, r, b);
                }
            }
        }
        final int helperCount = mConstraintHelpers.size();
        if (helperCount > 0) {
            for (int i = 0; i < helperCount; i++) {
                ConstraintHelper helper = mConstraintHelpers.get(i);
                helper.updatePostLayout(this);
            }
        }
    }

    /**
     * Set the optimization for the layout resolution.
     * <p>
     * The optimization can be any of the following:
     * <ul>
     * <li>Optimizer.OPTIMIZATION_NONE</li>
     * <li>Optimizer.OPTIMIZATION_STANDARD</li>
     * <li>a mask composed of specific optimizations</li>
     * </ul>
     * The mask can be composed of any combination of the following:
     * <ul>
     * <li>Optimizer.OPTIMIZATION_DIRECT  </li>
     * <li>Optimizer.OPTIMIZATION_BARRIER  </li>
     * <li>Optimizer.OPTIMIZATION_CHAIN  (experimental) </li>
     * <li>Optimizer.OPTIMIZATION_DIMENSIONS  (experimental) </li>
     * </ul>
     * Note that the current implementation of
     * Optimizer.OPTIMIZATION_STANDARD is as a mask of DIRECT and BARRIER.
     * </p>
     *
     * @param level optimization level
     * @since 1.1
     */
    public void setOptimizationLevel(int level) {
        mOptimizationLevel = level;
        mLayoutWidget.setOptimizationLevel(level);
    }

    /**
     * Return the current optimization level for the layout resolution
     *
     * @return the current level
     * @since 1.1
     */
    public int getOptimizationLevel() {
        return mLayoutWidget.getOptimizationLevel();
    }

    /**
     *
     */
    @Override
    public LayoutParams generateLayoutParams(AttributeSet attrs) {
        return new LayoutParams(getContext(), attrs);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected LayoutParams generateDefaultLayoutParams() {
        return new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected ViewGroup.LayoutParams generateLayoutParams(ViewGroup.LayoutParams p) {
        return new LayoutParams(p);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean checkLayoutParams(ViewGroup.LayoutParams p) {
        return p instanceof LayoutParams;
    }

    /**
     * Sets a ConstraintSet object to manage constraints.
     * The ConstraintSet overrides LayoutParams of child views.
     *
     * @param set Layout children using ConstraintSet
     */
    public void setConstraintSet(ConstraintSet set) {
        mConstraintSet = set;
    }

    /**
     * @param id the view id
     * @return the child view, can return null
     * Return a direct child view by its id if it exists
     */
    public View getViewById(int id) {
        return mChildrenByIds.get(id);
    }

    /**
     *
     */
    @Override
    protected void dispatchDraw(Canvas canvas) {
        if (mConstraintHelpers != null) {
            final int helperCount = mConstraintHelpers.size();
            if (helperCount > 0) {
                for (int i = 0; i < helperCount; i++) {
                    ConstraintHelper helper = mConstraintHelpers.get(i);
                    helper.updatePreDraw(this);
                }
            }
        }

        super.dispatchDraw(canvas);

        if (DEBUG || isInEditMode()) {
            float cw = getWidth();
            float ch = getHeight();
            float ow = 1080;
            float oh = 1920;
            final int count = getChildCount();
            for (int i = 0; i < count; i++) {
                View child = getChildAt(i);
                if (child.getVisibility() == GONE) {
                    continue;
                }
                Object tag = child.getTag();
                if (tag != null && tag instanceof String) {
                    String coordinates = (String) tag;
                    @SuppressWarnings("StringSplitter")
                    String[] split = coordinates.split(",");
                    if (split.length == 4) {
                        int x = Integer.parseInt(split[0]);
                        int y = Integer.parseInt(split[1]);
                        int w = Integer.parseInt(split[2]);
                        int h = Integer.parseInt(split[3]);
                        x = (int) ((x / ow) * cw);
                        y = (int) ((y / oh) * ch);
                        w = (int) ((w / ow) * cw);
                        h = (int) ((h / oh) * ch);
                        Paint paint = new Paint();
                        paint.setColor(Color.RED);
                        canvas.drawLine(x, y, x + w, y, paint);
                        canvas.drawLine(x + w, y, x + w, y + h, paint);
                        canvas.drawLine(x + w, y + h, x, y + h, paint);
                        canvas.drawLine(x, y + h, x, y, paint);
                        paint.setColor(Color.GREEN);
                        canvas.drawLine(x, y, x + w, y + h, paint);
                        canvas.drawLine(x, y + h, x + w, y, paint);
                    }
                }
            }
        }
        if (DEBUG_DRAW_CONSTRAINTS) {
            final int count = getChildCount();
            for (int i = 0; i < count; i++) {
                View child = getChildAt(i);
                if (child.getVisibility() == GONE) {
                    continue;
                }
                ConstraintWidget widget = getViewWidget(child);
                if (widget.mTop.isConnected()) {
                    ConstraintWidget target = widget.mTop.mTarget.mOwner;
                    int x1 = widget.getX() + widget.getWidth() / 2;
                    int y1 = widget.getY();
                    int x2 = target.getX() + target.getWidth() / 2;
                    int y2 = 0;
                    if (widget.mTop.mTarget.mType == ConstraintAnchor.Type.TOP) {
                        y2 = target.getY();
                    } else {
                        y2 = target.getY() + target.getHeight();
                    }
                    Paint paint = new Paint();
                    paint.setColor(Color.RED);
                    paint.setStrokeWidth(4);
                    canvas.drawLine(x1, y1, x2, y2, paint);
                }
                if (widget.mBottom.isConnected()) {
                    ConstraintWidget target = widget.mBottom.mTarget.mOwner;
                    int x1 = widget.getX() + widget.getWidth() / 2;
                    int y1 = widget.getY() + widget.getHeight();
                    int x2 = target.getX() + target.getWidth() / 2;
                    int y2 = 0;
                    if (widget.mBottom.mTarget.mType == ConstraintAnchor.Type.TOP) {
                        y2 = target.getY();
                    } else {
                        y2 = target.getY() + target.getHeight();
                    }
                    Paint paint = new Paint();
                    paint.setStrokeWidth(4);
                    paint.setColor(Color.RED);
                    canvas.drawLine(x1, y1, x2, y2, paint);
                }
            }
        }
    }

    /**
     * Notify of constraints changed
     * @param constraintsChangedListener
     */
    public void setOnConstraintsChanged(ConstraintsChangedListener constraintsChangedListener) {
       // this.mConstraintsChangedListener = constraintsChangedListener;
        if (mConstraintLayoutSpec != null) {
            mConstraintLayoutSpec.setOnConstraintsChanged(constraintsChangedListener);
        }
    }

    /**
     * Load a layout description file from the resources.
     *
     * @param layoutDescription The resource id, or 0 to reset the layout description.
     */
    public void loadLayoutDescription(int layoutDescription) {
        if (layoutDescription != 0) {
            try {
                mConstraintLayoutSpec = new ConstraintLayoutStates(getContext(),
                        this, layoutDescription);
            } catch (Resources.NotFoundException e) {
                mConstraintLayoutSpec = null;
            }
        } else {
            mConstraintLayoutSpec = null;
        }
    }

    /**
     * This class contains the different attributes specifying
     * how a view want to be laid out inside
     * a {@link ConstraintLayout}. For building up constraints at run time,
     * using {@link ConstraintSet} is recommended.
     */
    public static class LayoutParams extends ViewGroup.MarginLayoutParams {
        /**
         * Dimension will be controlled by constraints.
         */
        public static final int MATCH_CONSTRAINT = 0;

        /**
         * References the id of the parent.
         */
        public static final int PARENT_ID = 0;

        /**
         * Defines an id that is not set.
         */
        public static final int UNSET = -1;


        /**
         * Defines an id that is not set.
         */
        public static final int GONE_UNSET = Integer.MIN_VALUE;


        /**
         * The horizontal orientation.
         */
        public static final int HORIZONTAL = ConstraintWidget.HORIZONTAL;

        /**
         * The vertical orientation.
         */
        public static final int VERTICAL = ConstraintWidget.VERTICAL;

        /**
         * The left side of a view.
         */
        public static final int LEFT = 1;

        /**
         * The right side of a view.
         */
        public static final int RIGHT = 2;

        /**
         * The top of a view.
         */
        public static final int TOP = 3;

        /**
         * The bottom side of a view.
         */
        public static final int BOTTOM = 4;

        /**
         * The baseline of the text in a view.
         */
        public static final int BASELINE = 5;

        /**
         * The left side of a view in left to right languages.
         * In right to left languages it corresponds to the right side of the view
         */
        public static final int START = 6;

        /**
         * The right side of a view in right to left languages.
         * In right to left languages it corresponds to the left side of the view
         */
        public static final int END = 7;

        /**
         * Circle reference from a view.
         */
        public static final int CIRCLE = 8;

        /**
         * Set matchConstraintDefault* default to the wrap content size.
         * Use to set the matchConstraintDefaultWidth and matchConstraintDefaultHeight
         */
        public static final int MATCH_CONSTRAINT_WRAP = ConstraintWidget.MATCH_CONSTRAINT_WRAP;

        /**
         * Set matchConstraintDefault* spread as much as possible within its constraints.
         * Use to set the matchConstraintDefaultWidth and matchConstraintDefaultHeight
         */
        public static final int MATCH_CONSTRAINT_SPREAD = ConstraintWidget.MATCH_CONSTRAINT_SPREAD;

        /**
         * Set matchConstraintDefault* percent to be based
         * on a percent of another dimension (by default, the parent)
         * Use to set the matchConstraintDefaultWidth and matchConstraintDefaultHeight
         */
        public static final int MATCH_CONSTRAINT_PERCENT =
                ConstraintWidget.MATCH_CONSTRAINT_PERCENT;

        /**
         * Chain spread style
         */
        public static final int CHAIN_SPREAD = ConstraintWidget.CHAIN_SPREAD;

        /**
         * Chain spread inside style
         */
        public static final int CHAIN_SPREAD_INSIDE = ConstraintWidget.CHAIN_SPREAD_INSIDE;

        /**
         * Chain packed style
         */
        public static final int CHAIN_PACKED = ConstraintWidget.CHAIN_PACKED;

        /**
         * The distance of child (guideline) to the top or left edge of its parent.
         */
        public int guideBegin = UNSET;

        /**
         * The distance of child (guideline) to the bottom or right edge of its parent.
         */
        public int guideEnd = UNSET;

        /**
         * The ratio of the distance to the parent's sides
         */
        public float guidePercent = UNSET;

        /**
         * The ratio of the distance to the parent's sides
         */
        public boolean guidelineUseRtl = true;

        /**
         * Constrains the left side of a child to the left side of
         * a target child (contains the target child id).
         */
        public int leftToLeft = UNSET;

        /**
         * Constrains the left side of a child to the right side of
         * a target child (contains the target child id).
         */
        public int leftToRight = UNSET;

        /**
         * Constrains the right side of a child to the left side of
         * a target child (contains the target child id).
         */
        public int rightToLeft = UNSET;

        /**
         * Constrains the right side of a child to the right side of
         * a target child (contains the target child id).
         */
        public int rightToRight = UNSET;

        /**
         * Constrains the top side of a child to the top side of
         * a target child (contains the target child id).
         */
        public int topToTop = UNSET;

        /**
         * Constrains the top side of a child to the bottom side of
         * a target child (contains the target child id).
         */
        public int topToBottom = UNSET;

        /**
         * Constrains the bottom side of a child to the top side of
         * a target child (contains the target child id).
         */
        public int bottomToTop = UNSET;

        /**
         * Constrains the bottom side of a child to the bottom side of
         * a target child (contains the target child id).
         */
        public int bottomToBottom = UNSET;

        /**
         * Constrains the baseline of a child to the baseline of
         * a target child (contains the target child id).
         */
        public int baselineToBaseline = UNSET;

        /**
         * Constrains the baseline of a child to the top of
         * a target child (contains the target child id).
         */
        public int baselineToTop = UNSET;

        /**
         * Constrains the baseline of a child to the bottom of
         * a target child (contains the target child id).
         */
        public int baselineToBottom = UNSET;

        /**
         * Constrains the center of a child to the center of
         * a target child (contains the target child id).
         */
        public int circleConstraint = UNSET;

        /**
         * The radius used for a circular constraint
         */
        public int circleRadius = 0;

        /**
         * The angle used for a circular constraint]
         */
        public float circleAngle = 0;

        /**
         * Constrains the start side of a child to the end side of
         * a target child (contains the target child id).
         */
        public int startToEnd = UNSET;

        /**
         * Constrains the start side of a child to the start side of
         * a target child (contains the target child id).
         */
        public int startToStart = UNSET;

        /**
         * Constrains the end side of a child to the start side of
         * a target child (contains the target child id).
         */
        public int endToStart = UNSET;

        /**
         * Constrains the end side of a child to the end side of
         * a target child (contains the target child id).
         */
        public int endToEnd = UNSET;

        /**
         * The left margin to use when the target is gone.
         */
        public int goneLeftMargin = GONE_UNSET;

        /**
         * The top margin to use when the target is gone.
         */
        public int goneTopMargin = GONE_UNSET;

        /**
         * The right margin to use when the target is gone
         */
        public int goneRightMargin = GONE_UNSET;

        /**
         * The bottom margin to use when the target is gone.
         */
        public int goneBottomMargin = GONE_UNSET;

        /**
         * The start margin to use when the target is gone.
         */
        public int goneStartMargin = GONE_UNSET;

        /**
         * The end margin to use when the target is gone.
         */
        public int goneEndMargin = GONE_UNSET;

        /**
         * The baseline margin to use when the target is gone.
         */
        public int goneBaselineMargin = GONE_UNSET;

        /**
         * The baseline margin.
         */
        public int baselineMargin = 0;

        ///////////////////////////////////////////////////////////////////////////////////////////
        // Layout margins handling TODO: re-activate in 3.0
        ///////////////////////////////////////////////////////////////////////////////////////////

        /**
         * The left margin.
         */
        // public int leftMargin = 0;

        /**
         * The right margin.
         */
        // public int rightMargin = 0;

        // int originalLeftMargin = 0;
        // int originalRightMargin = 0;

        /**
         * The top margin.
         */
        // public int topMargin = 0;

        /**
         * The bottom margin.
         */
        // public int bottomMargin = 0;

        /**
         * The start margin.
         */
        // public int startMargin = UNSET;

        /**
         * The end margin.
         */
        // public int endMargin = UNSET;

        // boolean isRtl = false;
        // int layoutDirection = ViewCompat.LAYOUT_DIRECTION_LTR;

        boolean mWidthSet = true; // need to be set to false when we reactivate this in 3.0
        boolean mHeightSet = true; // need to be set to false when we reactivate this in 3.0

        ///////////////////////////////////////////////////////////////////////////////////////////

        /**
         * The ratio between two connections when
         * the left and right (or start and end) sides are constrained.
         */
        public float horizontalBias = 0.5f;

        /**
         * The ratio between two connections when the top and bottom sides are constrained.
         */
        public float verticalBias = 0.5f;

        /**
         * The ratio information.
         */
        public String dimensionRatio = null;

        /**
         * The ratio between the width and height of the child.
         */
        float mDimensionRatioValue = 0;

        /**
         * The child's side to constrain using dimensRatio.
         */
        int mDimensionRatioSide = VERTICAL;

        /**
         * The child's weight that we can use to distribute the available horizontal space
         * in a chain, if the dimension behaviour is set to MATCH_CONSTRAINT
         */
        public float horizontalWeight = UNSET;

        /**
         * The child's weight that we can use to distribute the available vertical space
         * in a chain, if the dimension behaviour is set to MATCH_CONSTRAINT
         */
        public float verticalWeight = UNSET;

        /**
         * If the child is the start of a horizontal chain, this attribute will drive how
         * the elements of the chain will be positioned. The possible values are:
         * <ul>
         * <li>{@link #CHAIN_SPREAD} -- the elements will be spread out</li>
         * <li>{@link #CHAIN_SPREAD_INSIDE} -- similar, but the endpoints of the chain will not
         * be spread out</li>
         * <li>{@link #CHAIN_PACKED} -- the elements of the chain will be packed together. The
         * horizontal bias attribute of the child will then affect the positioning of the packed
         * elements</li>
         * </ul>
         */
        public int horizontalChainStyle = CHAIN_SPREAD;

        /**
         * If the child is the start of a vertical chain, this attribute will drive how
         * the elements of the chain will be positioned. The possible values are:
         * <ul>
         * <li>{@link #CHAIN_SPREAD} -- the elements will be spread out</li>
         * <li>{@link #CHAIN_SPREAD_INSIDE} -- similar, but the endpoints of the chain will not
         * be spread out</li>
         * <li>{@link #CHAIN_PACKED} -- the elements of the chain will be packed together. The
         * vertical bias attribute of the child will then affect the positioning of the packed
         * elements</li>
         * </ul>
         */
        public int verticalChainStyle = CHAIN_SPREAD;

        /**
         * Define how the widget horizontal dimension is handled when set to MATCH_CONSTRAINT
         * <ul>
         * <li>{@link #MATCH_CONSTRAINT_SPREAD} -- the default. The dimension will expand up to
         * the constraints, minus margins</li>
         * <li>{@link #MATCH_CONSTRAINT_WRAP} -- DEPRECATED -- use instead WRAP_CONTENT and
         * constrainedWidth=true<br>
         * The dimension will be the same as WRAP_CONTENT, unless the size ends
         * up too large for the constraints;
         * in that case the dimension will expand up to the constraints, minus margins
         * This attribute may not be applied if
         * the widget is part of a chain in that dimension.</li>
         * <li>{@link #MATCH_CONSTRAINT_PERCENT} -- The dimension will be a percent of another
         * widget (by default, the parent)</li>
         * </ul>
         */
        public int matchConstraintDefaultWidth = MATCH_CONSTRAINT_SPREAD;

        /**
         * Define how the widget vertical dimension is handled when set to MATCH_CONSTRAINT
         * <ul>
         * <li>{@link #MATCH_CONSTRAINT_SPREAD} -- the default. The dimension will expand up to
         * the constraints, minus margins</li>
         * <li>{@link #MATCH_CONSTRAINT_WRAP} -- DEPRECATED -- use instead WRAP_CONTENT and
         * constrainedWidth=true<br>
         * The dimension will be the same as WRAP_CONTENT, unless the size ends
         * up too large for the constraints;
         * in that case the dimension will expand up to the constraints, minus margins
         * This attribute may not be applied if the widget is
         * part of a chain in that dimension.</li>
         * <li>{@link #MATCH_CONSTRAINT_PERCENT} -- The dimension will be a percent of another
         * widget (by default, the parent)</li>
         * </ul>
         */
        public int matchConstraintDefaultHeight = MATCH_CONSTRAINT_SPREAD;

        /**
         * Specify a minimum width size for the widget.
         * It will only apply if the size of the widget
         * is set to MATCH_CONSTRAINT. Don't apply if the widget is part of a horizontal chain.
         */
        public int matchConstraintMinWidth = 0;

        /**
         * Specify a minimum height size for the widget.
         * It will only apply if the size of the widget
         * is set to MATCH_CONSTRAINT. Don't apply if the widget is part of a vertical chain.
         */
        public int matchConstraintMinHeight = 0;

        /**
         * Specify a maximum width size for the widget.
         * It will only apply if the size of the widget
         * is set to MATCH_CONSTRAINT. Don't apply if the widget is part of a horizontal chain.
         */
        public int matchConstraintMaxWidth = 0;

        /**
         * Specify a maximum height size for the widget.
         * It will only apply if the size of the widget
         * is set to MATCH_CONSTRAINT. Don't apply if the widget is part of a vertical chain.
         */
        public int matchConstraintMaxHeight = 0;

        /**
         * Specify the percentage when using the match constraint percent mode. From 0 to 1.
         */
        public float matchConstraintPercentWidth = 1;

        /**
         * Specify the percentage when using the match constraint percent mode. From 0 to 1.
         */
        public float matchConstraintPercentHeight = 1;

        /**
         * The design time location of the left side of the child.
         * Used at design time for a horizontally unconstrained child.
         */
        public int editorAbsoluteX = UNSET;

        /**
         * The design time location of the right side of the child.
         * Used at design time for a vertically unconstrained child.
         */
        public int editorAbsoluteY = UNSET;

        public int orientation = UNSET;

        /**
         * Specify if the horizontal dimension is constrained in
         * case both left & right constraints are set
         * and the widget dimension is not a fixed dimension. By default,
         * if a widget is set to WRAP_CONTENT,
         * we will treat that dimension as a fixed dimension,
         * meaning the dimension will not change regardless
         * of constraints. Setting this attribute to true allows the dimension to change
         * in order to respect constraints.
         */
        public boolean constrainedWidth = false;

        /**
         * Specify if the vertical dimension is constrained in case both
         * top & bottom constraints are set and the widget dimension is not a fixed dimension.
         * By default, if a widget is set to WRAP_CONTENT,
         * we will treat that dimension as a fixed dimension,
         * meaning the dimension will not change regardless
         * of constraints. Setting this attribute to true allows the
         * dimension to change in order to respect constraints.
         */
        public boolean constrainedHeight = false;

        /**
         * Define a category of view to be used by helpers and motionLayout
         */
        public String constraintTag = null;

        public static final int WRAP_BEHAVIOR_INCLUDED =
                ConstraintWidget.WRAP_BEHAVIOR_INCLUDED;
        public static final int WRAP_BEHAVIOR_HORIZONTAL_ONLY =
                ConstraintWidget.WRAP_BEHAVIOR_HORIZONTAL_ONLY;
        public static final int WRAP_BEHAVIOR_VERTICAL_ONLY =
                ConstraintWidget.WRAP_BEHAVIOR_VERTICAL_ONLY;
        public static final int WRAP_BEHAVIOR_SKIPPED = ConstraintWidget.WRAP_BEHAVIOR_SKIPPED;

        /**
         * Specify how this view is taken in account during the parent's wrap computation
         *
         * Can be either of:
         * WRAP_BEHAVIOR_INCLUDED the widget is taken in account for the wrap (default)
         * WRAP_BEHAVIOR_HORIZONTAL_ONLY the widget will be included in the wrap only horizontally
         * WRAP_BEHAVIOR_VERTICAL_ONLY the widget will be included in the wrap only vertically
         * WRAP_BEHAVIOR_SKIPPED the widget is not part of the wrap computation
         */
        public int wrapBehaviorInParent = WRAP_BEHAVIOR_INCLUDED;

        // Internal use only
        boolean mHorizontalDimensionFixed = true;
        boolean mVerticalDimensionFixed = true;

        boolean mNeedsBaseline = false;
        boolean mIsGuideline = false;
        boolean mIsHelper = false;
        boolean mIsInPlaceholder = false;
        boolean mIsVirtualGroup = false;

        int mResolvedLeftToLeft = UNSET;
        int mResolvedLeftToRight = UNSET;
        int mResolvedRightToLeft = UNSET;
        int mResolvedRightToRight = UNSET;
        int mResolveGoneLeftMargin = GONE_UNSET;
        int mResolveGoneRightMargin = GONE_UNSET;
        float mResolvedHorizontalBias = 0.5f;

        int mResolvedGuideBegin;
        int mResolvedGuideEnd;
        float mResolvedGuidePercent;

        ConstraintWidget mWidget = new ConstraintWidget();

        /**
         *
         */
        public ConstraintWidget getConstraintWidget() {
            return mWidget;
        }

        /**
         * @param text
         *
         */
        public void setWidgetDebugName(String text) {
            mWidget.setDebugName(text);
        }

        /**
         * Reset the ConstraintWidget
         */
        public void reset() {
            if (mWidget != null) {
                mWidget.reset();
            }
        }

        public boolean helped = false;

        /**
         * Create a LayoutParams base on an existing layout Params
         *
         * @param params the Layout Params to be copied
         */
        public LayoutParams(ViewGroup.LayoutParams params) {
            super(params);

            if (!(params instanceof LayoutParams)) {
                return;
            }
            LayoutParams source = (LayoutParams) params;
            ///////////////////////////////////////////////////////////////////////////////////////
            // Layout margins handling TODO: re-activate in 3.0
            ///////////////////////////////////////////////////////////////////////////////////////
            // this.layoutDirection = source.layoutDirection;
            // this.isRtl = source.isRtl;
            // this.originalLeftMargin = source.originalLeftMargin;
            // this.originalRightMargin = source.originalRightMargin;
            // this.startMargin = source.startMargin;
            // this.endMargin = source.endMargin;
            // this.leftMargin = source.leftMargin;
            // this.rightMargin = source.rightMargin;
            // this.topMargin = source.topMargin;
            // this.bottomMargin = source.bottomMargin;
            ///////////////////////////////////////////////////////////////////////////////////////

            this.guideBegin = source.guideBegin;
            this.guideEnd = source.guideEnd;
            this.guidePercent = source.guidePercent;
            this.guidelineUseRtl = source.guidelineUseRtl;
            this.leftToLeft = source.leftToLeft;
            this.leftToRight = source.leftToRight;
            this.rightToLeft = source.rightToLeft;
            this.rightToRight = source.rightToRight;
            this.topToTop = source.topToTop;
            this.topToBottom = source.topToBottom;
            this.bottomToTop = source.bottomToTop;
            this.bottomToBottom = source.bottomToBottom;
            this.baselineToBaseline = source.baselineToBaseline;
            this.baselineToTop = source.baselineToTop;
            this.baselineToBottom = source.baselineToBottom;
            this.circleConstraint = source.circleConstraint;
            this.circleRadius = source.circleRadius;
            this.circleAngle = source.circleAngle;
            this.startToEnd = source.startToEnd;
            this.startToStart = source.startToStart;
            this.endToStart = source.endToStart;
            this.endToEnd = source.endToEnd;
            this.goneLeftMargin = source.goneLeftMargin;
            this.goneTopMargin = source.goneTopMargin;
            this.goneRightMargin = source.goneRightMargin;
            this.goneBottomMargin = source.goneBottomMargin;
            this.goneStartMargin = source.goneStartMargin;
            this.goneEndMargin = source.goneEndMargin;
            this.goneBaselineMargin = source.goneBaselineMargin;
            this.baselineMargin = source.baselineMargin;
            this.horizontalBias = source.horizontalBias;
            this.verticalBias = source.verticalBias;
            this.dimensionRatio = source.dimensionRatio;
            this.mDimensionRatioValue = source.mDimensionRatioValue;
            this.mDimensionRatioSide = source.mDimensionRatioSide;
            this.horizontalWeight = source.horizontalWeight;
            this.verticalWeight = source.verticalWeight;
            this.horizontalChainStyle = source.horizontalChainStyle;
            this.verticalChainStyle = source.verticalChainStyle;
            this.constrainedWidth = source.constrainedWidth;
            this.constrainedHeight = source.constrainedHeight;
            this.matchConstraintDefaultWidth = source.matchConstraintDefaultWidth;
            this.matchConstraintDefaultHeight = source.matchConstraintDefaultHeight;
            this.matchConstraintMinWidth = source.matchConstraintMinWidth;
            this.matchConstraintMaxWidth = source.matchConstraintMaxWidth;
            this.matchConstraintMinHeight = source.matchConstraintMinHeight;
            this.matchConstraintMaxHeight = source.matchConstraintMaxHeight;
            this.matchConstraintPercentWidth = source.matchConstraintPercentWidth;
            this.matchConstraintPercentHeight = source.matchConstraintPercentHeight;
            this.editorAbsoluteX = source.editorAbsoluteX;
            this.editorAbsoluteY = source.editorAbsoluteY;
            this.orientation = source.orientation;
            this.mHorizontalDimensionFixed = source.mHorizontalDimensionFixed;
            this.mVerticalDimensionFixed = source.mVerticalDimensionFixed;
            this.mNeedsBaseline = source.mNeedsBaseline;
            this.mIsGuideline = source.mIsGuideline;
            this.mResolvedLeftToLeft = source.mResolvedLeftToLeft;
            this.mResolvedLeftToRight = source.mResolvedLeftToRight;
            this.mResolvedRightToLeft = source.mResolvedRightToLeft;
            this.mResolvedRightToRight = source.mResolvedRightToRight;
            this.mResolveGoneLeftMargin = source.mResolveGoneLeftMargin;
            this.mResolveGoneRightMargin = source.mResolveGoneRightMargin;
            this.mResolvedHorizontalBias = source.mResolvedHorizontalBias;
            this.constraintTag = source.constraintTag;
            this.wrapBehaviorInParent = source.wrapBehaviorInParent;
            this.mWidget = source.mWidget;
            this.mWidthSet = source.mWidthSet;
            this.mHeightSet = source.mHeightSet;
        }

        private static class Table {
            public static final int UNUSED = 0;
            public static final int ANDROID_ORIENTATION = 1;
            public static final int LAYOUT_CONSTRAINT_CIRCLE = 2;
            public static final int LAYOUT_CONSTRAINT_CIRCLE_RADIUS = 3;
            public static final int LAYOUT_CONSTRAINT_CIRCLE_ANGLE = 4;
            public static final int LAYOUT_CONSTRAINT_GUIDE_BEGIN = 5;
            public static final int LAYOUT_CONSTRAINT_GUIDE_END = 6;
            public static final int LAYOUT_CONSTRAINT_GUIDE_PERCENT = 7;
            public static final int LAYOUT_CONSTRAINT_LEFT_TO_LEFT_OF = 8;
            public static final int LAYOUT_CONSTRAINT_LEFT_TO_RIGHT_OF = 9;
            public static final int LAYOUT_CONSTRAINT_RIGHT_TO_LEFT_OF = 10;
            public static final int LAYOUT_CONSTRAINT_RIGHT_TO_RIGHT_OF = 11;
            public static final int LAYOUT_CONSTRAINT_TOP_TO_TOP_OF = 12;
            public static final int LAYOUT_CONSTRAINT_TOP_TO_BOTTOM_OF = 13;
            public static final int LAYOUT_CONSTRAINT_BOTTOM_TO_TOP_OF = 14;
            public static final int LAYOUT_CONSTRAINT_BOTTOM_TO_BOTTOM_OF = 15;
            public static final int LAYOUT_CONSTRAINT_BASELINE_TO_BASELINE_OF = 16;
            public static final int LAYOUT_CONSTRAINT_START_TO_END_OF = 17;
            public static final int LAYOUT_CONSTRAINT_START_TO_START_OF = 18;
            public static final int LAYOUT_CONSTRAINT_END_TO_START_OF = 19;
            public static final int LAYOUT_CONSTRAINT_END_TO_END_OF = 20;
            public static final int LAYOUT_GONE_MARGIN_LEFT = 21;
            public static final int LAYOUT_GONE_MARGIN_TOP = 22;
            public static final int LAYOUT_GONE_MARGIN_RIGHT = 23;
            public static final int LAYOUT_GONE_MARGIN_BOTTOM = 24;
            public static final int LAYOUT_GONE_MARGIN_START = 25;
            public static final int LAYOUT_GONE_MARGIN_END = 26;
            public static final int LAYOUT_CONSTRAINED_WIDTH = 27;
            public static final int LAYOUT_CONSTRAINED_HEIGHT = 28;
            public static final int LAYOUT_CONSTRAINT_HORIZONTAL_BIAS = 29;
            public static final int LAYOUT_CONSTRAINT_VERTICAL_BIAS = 30;
            public static final int LAYOUT_CONSTRAINT_WIDTH_DEFAULT = 31;
            public static final int LAYOUT_CONSTRAINT_HEIGHT_DEFAULT = 32;
            public static final int LAYOUT_CONSTRAINT_WIDTH_MIN = 33;
            public static final int LAYOUT_CONSTRAINT_WIDTH_MAX = 34;
            public static final int LAYOUT_CONSTRAINT_WIDTH_PERCENT = 35;
            public static final int LAYOUT_CONSTRAINT_HEIGHT_MIN = 36;
            public static final int LAYOUT_CONSTRAINT_HEIGHT_MAX = 37;
            public static final int LAYOUT_CONSTRAINT_HEIGHT_PERCENT = 38;
            public static final int LAYOUT_CONSTRAINT_LEFT_CREATOR = 39;
            public static final int LAYOUT_CONSTRAINT_TOP_CREATOR = 40;
            public static final int LAYOUT_CONSTRAINT_RIGHT_CREATOR = 41;
            public static final int LAYOUT_CONSTRAINT_BOTTOM_CREATOR = 42;
            public static final int LAYOUT_CONSTRAINT_BASELINE_CREATOR = 43;
            public static final int LAYOUT_CONSTRAINT_DIMENSION_RATIO = 44;
            public static final int LAYOUT_CONSTRAINT_HORIZONTAL_WEIGHT = 45;
            public static final int LAYOUT_CONSTRAINT_VERTICAL_WEIGHT = 46;
            public static final int LAYOUT_CONSTRAINT_HORIZONTAL_CHAINSTYLE = 47;
            public static final int LAYOUT_CONSTRAINT_VERTICAL_CHAINSTYLE = 48;
            public static final int LAYOUT_EDITOR_ABSOLUTEX = 49;
            public static final int LAYOUT_EDITOR_ABSOLUTEY = 50;
            public static final int LAYOUT_CONSTRAINT_TAG = 51;
            public static final int LAYOUT_CONSTRAINT_BASELINE_TO_TOP_OF = 52;
            public static final int LAYOUT_CONSTRAINT_BASELINE_TO_BOTTOM_OF = 53;
            public static final int LAYOUT_MARGIN_BASELINE = 54;
            public static final int LAYOUT_GONE_MARGIN_BASELINE = 55;
            ///////////////////////////////////////////////////////////////////////////////////////
            // Layout margins handling TODO: re-activate in 3.0
            ///////////////////////////////////////////////////////////////////////////////////////
            // public static final int LAYOUT_MARGIN_LEFT = 56;
            // public static final int LAYOUT_MARGIN_RIGHT = 57;
            // public static final int LAYOUT_MARGIN_TOP = 58;
            // public static final int LAYOUT_MARGIN_BOTTOM = 59;
            // public static final int LAYOUT_MARGIN_START = 60;
            // public static final int LAYOUT_MARGIN_END = 61;
            // public static final int LAYOUT_WIDTH = 62;
            // public static final int LAYOUT_HEIGHT = 63;
            ///////////////////////////////////////////////////////////////////////////////////////
            public static final int LAYOUT_CONSTRAINT_WIDTH = 64;
            public static final int LAYOUT_CONSTRAINT_HEIGHT = 65;
            public static final int LAYOUT_WRAP_BEHAVIOR_IN_PARENT = 66;
            public static final int GUIDELINE_USE_RTL = 67;

            public static final SparseIntArray sMap = new SparseIntArray();

            static {
                ///////////////////////////////////////////////////////////////////////////////////
                // Layout margins handling TODO: re-activate in 3.0
                ///////////////////////////////////////////////////////////////////////////////////
                // map.append(R.styleable.ConstraintLayout_Layout_android_layout_width,
                // LAYOUT_WIDTH);
                // map.append(R.styleable.ConstraintLayout_Layout_android_layout_height,
                // LAYOUT_HEIGHT);
                // map.append(R.styleable.ConstraintLayout_Layout_android_layout_marginLeft,
                // LAYOUT_MARGIN_LEFT);
                // map.append(R.styleable.ConstraintLayout_Layout_android_layout_marginRight,
                // LAYOUT_MARGIN_RIGHT);
                // map.append(R.styleable.ConstraintLayout_Layout_android_layout_marginTop,
                // LAYOUT_MARGIN_TOP);
                // map.append(R.styleable.ConstraintLayout_Layout_android_layout_marginBottom,
                // LAYOUT_MARGIN_BOTTOM);
                // map.append(R.styleable.ConstraintLayout_Layout_android_layout_marginStart,
                // LAYOUT_MARGIN_START);
                // map.append(R.styleable.ConstraintLayout_Layout_android_layout_marginEnd,
                // LAYOUT_MARGIN_END);
                //////////////////////////////////////////////////////////////////////////////////
                sMap.append(R.styleable.ConstraintLayout_Layout_layout_constraintWidth,
                        LAYOUT_CONSTRAINT_WIDTH);
                sMap.append(R.styleable.ConstraintLayout_Layout_layout_constraintHeight,
                        LAYOUT_CONSTRAINT_HEIGHT);
                sMap.append(R.styleable.ConstraintLayout_Layout_layout_constraintLeft_toLeftOf,
                        LAYOUT_CONSTRAINT_LEFT_TO_LEFT_OF);
                sMap.append(R.styleable.ConstraintLayout_Layout_layout_constraintLeft_toRightOf,
                        LAYOUT_CONSTRAINT_LEFT_TO_RIGHT_OF);
                sMap.append(R.styleable.ConstraintLayout_Layout_layout_constraintRight_toLeftOf,
                        LAYOUT_CONSTRAINT_RIGHT_TO_LEFT_OF);
                sMap.append(R.styleable.ConstraintLayout_Layout_layout_constraintRight_toRightOf,
                        LAYOUT_CONSTRAINT_RIGHT_TO_RIGHT_OF);
                sMap.append(R.styleable.ConstraintLayout_Layout_layout_constraintTop_toTopOf,
                        LAYOUT_CONSTRAINT_TOP_TO_TOP_OF);
                sMap.append(R.styleable.ConstraintLayout_Layout_layout_constraintTop_toBottomOf,
                        LAYOUT_CONSTRAINT_TOP_TO_BOTTOM_OF);
                sMap.append(R.styleable.ConstraintLayout_Layout_layout_constraintBottom_toTopOf,
                        LAYOUT_CONSTRAINT_BOTTOM_TO_TOP_OF);
                sMap.append(R.styleable.ConstraintLayout_Layout_layout_constraintBottom_toBottomOf,
                        LAYOUT_CONSTRAINT_BOTTOM_TO_BOTTOM_OF);
                sMap.append(
                        R.styleable.ConstraintLayout_Layout_layout_constraintBaseline_toBaselineOf,
                        LAYOUT_CONSTRAINT_BASELINE_TO_BASELINE_OF);
                sMap.append(R.styleable.ConstraintLayout_Layout_layout_constraintBaseline_toTopOf,
                        LAYOUT_CONSTRAINT_BASELINE_TO_TOP_OF);
                sMap.append(R.styleable
                                .ConstraintLayout_Layout_layout_constraintBaseline_toBottomOf,
                        LAYOUT_CONSTRAINT_BASELINE_TO_BOTTOM_OF);
                sMap.append(R.styleable.ConstraintLayout_Layout_layout_constraintCircle,
                        LAYOUT_CONSTRAINT_CIRCLE);
                sMap.append(R.styleable.ConstraintLayout_Layout_layout_constraintCircleRadius,
                        LAYOUT_CONSTRAINT_CIRCLE_RADIUS);
                sMap.append(R.styleable.ConstraintLayout_Layout_layout_constraintCircleAngle,
                        LAYOUT_CONSTRAINT_CIRCLE_ANGLE);
                sMap.append(R.styleable.ConstraintLayout_Layout_layout_editor_absoluteX,
                        LAYOUT_EDITOR_ABSOLUTEX);
                sMap.append(R.styleable.ConstraintLayout_Layout_layout_editor_absoluteY,
                        LAYOUT_EDITOR_ABSOLUTEY);
                sMap.append(R.styleable.ConstraintLayout_Layout_layout_constraintGuide_begin,
                        LAYOUT_CONSTRAINT_GUIDE_BEGIN);
                sMap.append(R.styleable.ConstraintLayout_Layout_layout_constraintGuide_end,
                        LAYOUT_CONSTRAINT_GUIDE_END);
                sMap.append(R.styleable.ConstraintLayout_Layout_layout_constraintGuide_percent,
                        LAYOUT_CONSTRAINT_GUIDE_PERCENT);
                sMap.append(R.styleable.ConstraintLayout_Layout_guidelineUseRtl,
                        GUIDELINE_USE_RTL);
                sMap.append(R.styleable.ConstraintLayout_Layout_android_orientation,
                        ANDROID_ORIENTATION);
                sMap.append(R.styleable.ConstraintLayout_Layout_layout_constraintStart_toEndOf,
                        LAYOUT_CONSTRAINT_START_TO_END_OF);
                sMap.append(R.styleable.ConstraintLayout_Layout_layout_constraintStart_toStartOf,
                        LAYOUT_CONSTRAINT_START_TO_START_OF);
                sMap.append(R.styleable.ConstraintLayout_Layout_layout_constraintEnd_toStartOf,
                        LAYOUT_CONSTRAINT_END_TO_START_OF);
                sMap.append(R.styleable.ConstraintLayout_Layout_layout_constraintEnd_toEndOf,
                        LAYOUT_CONSTRAINT_END_TO_END_OF);
                sMap.append(R.styleable.ConstraintLayout_Layout_layout_goneMarginLeft,
                        LAYOUT_GONE_MARGIN_LEFT);
                sMap.append(R.styleable.ConstraintLayout_Layout_layout_goneMarginTop,
                        LAYOUT_GONE_MARGIN_TOP);
                sMap.append(R.styleable.ConstraintLayout_Layout_layout_goneMarginRight,
                        LAYOUT_GONE_MARGIN_RIGHT);
                sMap.append(R.styleable.ConstraintLayout_Layout_layout_goneMarginBottom,
                        LAYOUT_GONE_MARGIN_BOTTOM);
                sMap.append(R.styleable.ConstraintLayout_Layout_layout_goneMarginStart,
                        LAYOUT_GONE_MARGIN_START);
                sMap.append(R.styleable.ConstraintLayout_Layout_layout_goneMarginEnd,
                        LAYOUT_GONE_MARGIN_END);
                sMap.append(R.styleable.ConstraintLayout_Layout_layout_goneMarginBaseline,
                        LAYOUT_GONE_MARGIN_BASELINE);
                sMap.append(R.styleable.ConstraintLayout_Layout_layout_marginBaseline,
                        LAYOUT_MARGIN_BASELINE);
                sMap.append(R.styleable.ConstraintLayout_Layout_layout_constraintHorizontal_bias,
                        LAYOUT_CONSTRAINT_HORIZONTAL_BIAS);
                sMap.append(R.styleable.ConstraintLayout_Layout_layout_constraintVertical_bias,
                        LAYOUT_CONSTRAINT_VERTICAL_BIAS);
                sMap.append(R.styleable.ConstraintLayout_Layout_layout_constraintDimensionRatio,
                        LAYOUT_CONSTRAINT_DIMENSION_RATIO);
                sMap.append(R.styleable.ConstraintLayout_Layout_layout_constraintHorizontal_weight,
                        LAYOUT_CONSTRAINT_HORIZONTAL_WEIGHT);
                sMap.append(R.styleable.ConstraintLayout_Layout_layout_constraintVertical_weight,
                        LAYOUT_CONSTRAINT_VERTICAL_WEIGHT);
                sMap.append(
                        R.styleable.ConstraintLayout_Layout_layout_constraintHorizontal_chainStyle,
                        LAYOUT_CONSTRAINT_HORIZONTAL_CHAINSTYLE);
                sMap.append(R.styleable
                                .ConstraintLayout_Layout_layout_constraintVertical_chainStyle,
                        LAYOUT_CONSTRAINT_VERTICAL_CHAINSTYLE);
                sMap.append(R.styleable.ConstraintLayout_Layout_layout_constrainedWidth,
                        LAYOUT_CONSTRAINED_WIDTH);
                sMap.append(R.styleable.ConstraintLayout_Layout_layout_constrainedHeight,
                        LAYOUT_CONSTRAINED_HEIGHT);
                sMap.append(R.styleable.ConstraintLayout_Layout_layout_constraintWidth_default,
                        LAYOUT_CONSTRAINT_WIDTH_DEFAULT);
                sMap.append(R.styleable.ConstraintLayout_Layout_layout_constraintHeight_default,
                        LAYOUT_CONSTRAINT_HEIGHT_DEFAULT);
                sMap.append(R.styleable.ConstraintLayout_Layout_layout_constraintWidth_min,
                        LAYOUT_CONSTRAINT_WIDTH_MIN);
                sMap.append(R.styleable.ConstraintLayout_Layout_layout_constraintWidth_max,
                        LAYOUT_CONSTRAINT_WIDTH_MAX);
                sMap.append(R.styleable.ConstraintLayout_Layout_layout_constraintWidth_percent,
                        LAYOUT_CONSTRAINT_WIDTH_PERCENT);
                sMap.append(R.styleable.ConstraintLayout_Layout_layout_constraintHeight_min,
                        LAYOUT_CONSTRAINT_HEIGHT_MIN);
                sMap.append(R.styleable.ConstraintLayout_Layout_layout_constraintHeight_max,
                        LAYOUT_CONSTRAINT_HEIGHT_MAX);
                sMap.append(R.styleable.ConstraintLayout_Layout_layout_constraintHeight_percent,
                        LAYOUT_CONSTRAINT_HEIGHT_PERCENT);
                sMap.append(R.styleable.ConstraintLayout_Layout_layout_constraintLeft_creator,
                        LAYOUT_CONSTRAINT_LEFT_CREATOR);
                sMap.append(R.styleable.ConstraintLayout_Layout_layout_constraintTop_creator,
                        LAYOUT_CONSTRAINT_TOP_CREATOR);
                sMap.append(R.styleable.ConstraintLayout_Layout_layout_constraintRight_creator,
                        LAYOUT_CONSTRAINT_RIGHT_CREATOR);
                sMap.append(R.styleable.ConstraintLayout_Layout_layout_constraintBottom_creator,
                        LAYOUT_CONSTRAINT_BOTTOM_CREATOR);
                sMap.append(R.styleable.ConstraintLayout_Layout_layout_constraintBaseline_creator,
                        LAYOUT_CONSTRAINT_BASELINE_CREATOR);
                sMap.append(R.styleable.ConstraintLayout_Layout_layout_constraintTag,
                        LAYOUT_CONSTRAINT_TAG);
                sMap.append(R.styleable.ConstraintLayout_Layout_layout_wrapBehaviorInParent,
                        LAYOUT_WRAP_BEHAVIOR_IN_PARENT);
            }
        }

        ///////////////////////////////////////////////////////////////////////////////////////////
        // Layout margins handling TODO: re-activate in 3.0
        ///////////////////////////////////////////////////////////////////////////////////////////
        /*
        public void setMarginStart(int start) {
            startMargin = start;
        }

        public void setMarginEnd(int end) {
            endMargin = end;
        }

        public int getMarginStart() {
            return startMargin;
        }

        public int getMarginEnd() {
            return endMargin;
        }

        public int getLayoutDirection() {
            return layoutDirection;
        }
        */
        ///////////////////////////////////////////////////////////////////////////////////////

        public LayoutParams(Context c, AttributeSet attrs) {
            super(c, attrs);

            TypedArray a = c.obtainStyledAttributes(attrs, R.styleable.ConstraintLayout_Layout);
            final int n = a.getIndexCount();

            ///////////////////////////////////////////////////////////////////////////////////////
            // Layout margins handling TODO: re-activate in 3.0
            ///////////////////////////////////////////////////////////////////////////////////////
            // super(WRAP_CONTENT, WRAP_CONTENT);
            /*
            if (n == 0) {
               // check if it's an include
               throw new IllegalArgumentException("Invalid LayoutParams supplied to " + this);
            }

            // let's first apply full margins if they are present.
            int margin = a.getDimensionPixelSize(R.styleable
            .ConstraintLayout_Layout_android_layout_margin, -1);
            int horizontalMargin = -1;
            int verticalMargin = -1;
            if (margin >= 0) {
                originalLeftMargin = margin;
                originalRightMargin = margin;
                topMargin = margin;
                bottomMargin = margin;
            } else {
                horizontalMargin = a.getDimensionPixelSize(R.styleable
                .ConstraintLayout_Layout_android_layout_marginHorizontal, -1);
                verticalMargin = a.getDimensionPixelSize(R.styleable
                .ConstraintLayout_Layout_android_layout_marginVertical, -1);
                if (horizontalMargin >= 0) {
                    originalLeftMargin = horizontalMargin;
                    originalRightMargin = horizontalMargin;
                }
                if (verticalMargin >= 0) {
                    topMargin = verticalMargin;
                    bottomMargin = verticalMargin;
                }
            }
            */
            //////////////////////////////////////////////////////////////////////////////////////

            for (int i = 0; i < n; i++) {
                int attr = a.getIndex(i);
                int look = Table.sMap.get(attr);
                switch (look) {
                    case Table.UNUSED: {
                        // Skip
                        break;
                    }
                    case Table.LAYOUT_CONSTRAINT_WIDTH: {
                        ConstraintSet.parseDimensionConstraints(this, a, attr, HORIZONTAL);
                        mWidthSet = true;
                        break;
                    }
                    case Table.LAYOUT_CONSTRAINT_HEIGHT: {
                        ConstraintSet.parseDimensionConstraints(this, a, attr, VERTICAL);
                        mHeightSet = true;
                        break;
                    }
                    ///////////////////////////////////////////////////////////////////////////////
                    // Layout margins handling TODO: re-activate in 3.0
                    ///////////////////////////////////////////////////////////////////////////////
                    /*
                    case Table.LAYOUT_WIDTH: {
                        width = a.getLayoutDimension(R.styleable
                        .ConstraintLayout_Layout_android_layout_width, "layout_width");
                        widthSet = true;
                        break;
                    }
                    case Table.LAYOUT_HEIGHT: {
                        height = a.getLayoutDimension(R.styleable
                        .ConstraintLayout_Layout_android_layout_height, "layout_height");
                        heightSet = true;
                        break;
                    }
                    */
                    ///////////////////////////////////////////////////////////////////////////////
                    case Table.LAYOUT_WRAP_BEHAVIOR_IN_PARENT: {
                        wrapBehaviorInParent = a.getInt(attr, wrapBehaviorInParent);
                        break;
                    }
                    case Table.LAYOUT_CONSTRAINT_LEFT_TO_LEFT_OF: {
                        leftToLeft = a.getResourceId(attr, leftToLeft);
                        if (leftToLeft == UNSET) {
                            leftToLeft = a.getInt(attr, UNSET);
                        }
                        break;
                    }
                    case Table.LAYOUT_CONSTRAINT_LEFT_TO_RIGHT_OF: {
                        leftToRight = a.getResourceId(attr, leftToRight);
                        if (leftToRight == UNSET) {
                            leftToRight = a.getInt(attr, UNSET);
                        }
                        break;
                    }
                    case Table.LAYOUT_CONSTRAINT_RIGHT_TO_LEFT_OF: {
                        rightToLeft = a.getResourceId(attr, rightToLeft);
                        if (rightToLeft == UNSET) {
                            rightToLeft = a.getInt(attr, UNSET);
                        }
                        break;
                    }
                    case Table.LAYOUT_CONSTRAINT_RIGHT_TO_RIGHT_OF: {
                        rightToRight = a.getResourceId(attr, rightToRight);
                        if (rightToRight == UNSET) {
                            rightToRight = a.getInt(attr, UNSET);
                        }
                        break;
                    }
                    case Table.LAYOUT_CONSTRAINT_TOP_TO_TOP_OF: {
                        topToTop = a.getResourceId(attr, topToTop);
                        if (topToTop == UNSET) {
                            topToTop = a.getInt(attr, UNSET);
                        }
                        break;
                    }
                    case Table.LAYOUT_CONSTRAINT_TOP_TO_BOTTOM_OF: {
                        topToBottom = a.getResourceId(attr, topToBottom);
                        if (topToBottom == UNSET) {
                            topToBottom = a.getInt(attr, UNSET);
                        }
                        break;
                    }
                    case Table.LAYOUT_CONSTRAINT_BOTTOM_TO_TOP_OF: {
                        bottomToTop = a.getResourceId(attr, bottomToTop);
                        if (bottomToTop == UNSET) {
                            bottomToTop = a.getInt(attr, UNSET);
                        }
                        break;
                    }
                    case Table.LAYOUT_CONSTRAINT_BOTTOM_TO_BOTTOM_OF: {
                        bottomToBottom = a.getResourceId(attr, bottomToBottom);
                        if (bottomToBottom == UNSET) {
                            bottomToBottom = a.getInt(attr, UNSET);
                        }
                        break;
                    }
                    case Table.LAYOUT_CONSTRAINT_BASELINE_TO_BASELINE_OF: {
                        baselineToBaseline = a.getResourceId(attr, baselineToBaseline);
                        if (baselineToBaseline == UNSET) {
                            baselineToBaseline = a.getInt(attr, UNSET);
                        }
                        break;
                    }
                    case Table.LAYOUT_CONSTRAINT_BASELINE_TO_TOP_OF: {
                        baselineToTop = a.getResourceId(attr, baselineToTop);
                        if (baselineToTop == UNSET) {
                            baselineToTop = a.getInt(attr, UNSET);
                        }
                        break;
                    }
                    case Table.LAYOUT_CONSTRAINT_BASELINE_TO_BOTTOM_OF: {
                        baselineToBottom = a.getResourceId(attr, baselineToBottom);
                        if (baselineToBottom == UNSET) {
                            baselineToBottom = a.getInt(attr, UNSET);
                        }
                        break;
                    }
                    case Table.LAYOUT_CONSTRAINT_CIRCLE: {
                        circleConstraint = a.getResourceId(attr, circleConstraint);
                        if (circleConstraint == UNSET) {
                            circleConstraint = a.getInt(attr, UNSET);
                        }
                        break;
                    }
                    case Table.LAYOUT_CONSTRAINT_CIRCLE_RADIUS: {
                        circleRadius = a.getDimensionPixelSize(attr, circleRadius);
                        break;
                    }
                    case Table.LAYOUT_CONSTRAINT_CIRCLE_ANGLE: {
                        circleAngle = a.getFloat(attr, circleAngle) % 360;
                        if (circleAngle < 0) {
                            circleAngle = (360 - circleAngle) % 360;
                        }
                        break;
                    }
                    case Table.LAYOUT_EDITOR_ABSOLUTEX: {
                        editorAbsoluteX = a.getDimensionPixelOffset(attr, editorAbsoluteX);
                        break;
                    }
                    case Table.LAYOUT_EDITOR_ABSOLUTEY: {
                        editorAbsoluteY = a.getDimensionPixelOffset(attr, editorAbsoluteY);
                        break;
                    }
                    case Table.LAYOUT_CONSTRAINT_GUIDE_BEGIN: {
                        guideBegin = a.getDimensionPixelOffset(attr, guideBegin);
                        break;
                    }

                    case Table.LAYOUT_CONSTRAINT_GUIDE_END: {
                        guideEnd = a.getDimensionPixelOffset(attr, guideEnd);
                        break;
                    }

                    case Table.LAYOUT_CONSTRAINT_GUIDE_PERCENT: {
                        guidePercent = a.getFloat(attr, guidePercent);
                        break;
                    }
                    case Table.GUIDELINE_USE_RTL: {
                        guidelineUseRtl = a.getBoolean(attr, guidelineUseRtl);
                        break;
                    }

                    case Table.ANDROID_ORIENTATION: {
                        orientation = a.getInt(attr, orientation);
                        break;
                    }

                    case Table.LAYOUT_CONSTRAINT_START_TO_END_OF: {
                        startToEnd = a.getResourceId(attr, startToEnd);
                        if (startToEnd == UNSET) {
                            startToEnd = a.getInt(attr, UNSET);
                        }
                        break;
                    }
                    case Table.LAYOUT_CONSTRAINT_START_TO_START_OF: {
                        startToStart = a.getResourceId(attr, startToStart);
                        if (startToStart == UNSET) {
                            startToStart = a.getInt(attr, UNSET);
                        }
                        break;
                    }
                    case Table.LAYOUT_CONSTRAINT_END_TO_START_OF: {
                        endToStart = a.getResourceId(attr, endToStart);
                        if (endToStart == UNSET) {
                            endToStart = a.getInt(attr, UNSET);
                        }
                        break;
                    }
                    case Table.LAYOUT_CONSTRAINT_END_TO_END_OF: {
                        endToEnd = a.getResourceId(attr, endToEnd);
                        if (endToEnd == UNSET) {
                            endToEnd = a.getInt(attr, UNSET);
                        }
                        break;
                    }
                    case Table.LAYOUT_GONE_MARGIN_LEFT: {
                        goneLeftMargin = a.getDimensionPixelSize(attr, goneLeftMargin);
                        break;
                    }
                    case Table.LAYOUT_GONE_MARGIN_TOP: {
                        goneTopMargin = a.getDimensionPixelSize(attr, goneTopMargin);
                        break;
                    }
                    case Table.LAYOUT_GONE_MARGIN_RIGHT: {
                        goneRightMargin = a.getDimensionPixelSize(attr, goneRightMargin);
                        break;
                    }
                    case Table.LAYOUT_GONE_MARGIN_BOTTOM: {
                        goneBottomMargin = a.getDimensionPixelSize(attr, goneBottomMargin);
                        break;
                    }
                    case Table.LAYOUT_GONE_MARGIN_START: {
                        goneStartMargin = a.getDimensionPixelSize(attr, goneStartMargin);
                        break;
                    }
                    case Table.LAYOUT_GONE_MARGIN_END: {
                        goneEndMargin = a.getDimensionPixelSize(attr, goneEndMargin);
                        break;
                    }
                    case Table.LAYOUT_GONE_MARGIN_BASELINE: {
                        goneBaselineMargin = a.getDimensionPixelSize(attr, goneBaselineMargin);
                        break;
                    }
                    case Table.LAYOUT_MARGIN_BASELINE: {
                        baselineMargin = a.getDimensionPixelSize(attr, baselineMargin);
                        break;
                    }
                    //////////////////////////////////////////////////////////////////////////////
                    // Layout margins handling TODO: re-activate in 3.0
                    //////////////////////////////////////////////////////////////////////////////
                    /*
                    case Table.LAYOUT_MARGIN_LEFT: {
                        if (margin == -1 && horizontalMargin == -1) {
                            originalLeftMargin = a.getDimensionPixelSize(attr, originalLeftMargin);
                        }
                        break;
                    }
                    case Table.LAYOUT_MARGIN_RIGHT: {
                        if (margin == -1 && horizontalMargin == -1) {
                            originalRightMargin =
                                a.getDimensionPixelSize(attr, originalRightMargin);
                        }
                        break;
                    }
                    case Table.LAYOUT_MARGIN_TOP: {
                        if (margin == -1 && verticalMargin == -1) {
                            topMargin = a.getDimensionPixelSize(attr, topMargin);
                        }
                        break;
                    }
                    case Table.LAYOUT_MARGIN_BOTTOM: {
                        if (margin == -1 && verticalMargin == -1) {
                            bottomMargin = a.getDimensionPixelSize(attr, bottomMargin);
                        }
                        break;
                    }
                    case Table.LAYOUT_MARGIN_START: {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                            if (margin == -1 && horizontalMargin == -1) {
                                startMargin = a.getDimensionPixelSize(attr, startMargin);
                            }
                        }
                        break;
                    }
                    case Table.LAYOUT_MARGIN_END: {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                            if (margin == -1 && horizontalMargin == -1) {
                                endMargin = a.getDimensionPixelSize(attr, endMargin);
                            }
                        }
                        break;
                    }
                    */
                    ///////////////////////////////////////////////////////////////////////////////
                    case Table.LAYOUT_CONSTRAINT_HORIZONTAL_BIAS: {
                        horizontalBias = a.getFloat(attr, horizontalBias);
                        break;
                    }
                    case Table.LAYOUT_CONSTRAINT_VERTICAL_BIAS: {
                        verticalBias = a.getFloat(attr, verticalBias);
                        break;
                    }
                    case Table.LAYOUT_CONSTRAINT_DIMENSION_RATIO: {
                        ConstraintSet.parseDimensionRatioString(this, a.getString(attr));
                        break;
                    }
                    case Table.LAYOUT_CONSTRAINT_HORIZONTAL_WEIGHT: {
                        horizontalWeight = a.getFloat(attr, horizontalWeight);
                        break;
                    }
                    case Table.LAYOUT_CONSTRAINT_VERTICAL_WEIGHT: {
                        verticalWeight = a.getFloat(attr, verticalWeight);
                        break;
                    }
                    case Table.LAYOUT_CONSTRAINT_HORIZONTAL_CHAINSTYLE: {
                        horizontalChainStyle = a.getInt(attr, CHAIN_SPREAD);
                        break;
                    }
                    case Table.LAYOUT_CONSTRAINT_VERTICAL_CHAINSTYLE: {
                        verticalChainStyle = a.getInt(attr, CHAIN_SPREAD);
                        break;
                    }
                    case Table.LAYOUT_CONSTRAINED_WIDTH: {
                        constrainedWidth = a.getBoolean(attr, constrainedWidth);
                        break;
                    }
                    case Table.LAYOUT_CONSTRAINED_HEIGHT: {
                        constrainedHeight = a.getBoolean(attr, constrainedHeight);
                        break;
                    }
                    case Table.LAYOUT_CONSTRAINT_WIDTH_DEFAULT: {
                        matchConstraintDefaultWidth = a.getInt(attr, MATCH_CONSTRAINT_SPREAD);
                        if (matchConstraintDefaultWidth == MATCH_CONSTRAINT_WRAP) {
                            Log.e(TAG, "layout_constraintWidth_default=\"wrap\" is deprecated."
                                    + "\nUse layout_width=\"WRAP_CONTENT\" and "
                                    + "layout_constrainedWidth=\"true\" instead.");
                        }
                        break;
                    }
                    case Table.LAYOUT_CONSTRAINT_HEIGHT_DEFAULT: {
                        matchConstraintDefaultHeight = a.getInt(attr, MATCH_CONSTRAINT_SPREAD);
                        if (matchConstraintDefaultHeight == MATCH_CONSTRAINT_WRAP) {
                            Log.e(TAG, "layout_constraintHeight_default=\"wrap\" is deprecated."
                                    + "\nUse layout_height=\"WRAP_CONTENT\" and "
                                    + "layout_constrainedHeight=\"true\" instead.");
                        }
                        break;
                    }
                    case Table.LAYOUT_CONSTRAINT_WIDTH_MIN: {
                        try {
                            matchConstraintMinWidth = a.getDimensionPixelSize(attr,
                                    matchConstraintMinWidth);
                        } catch (Exception e) {
                            int value = a.getInt(attr, matchConstraintMinWidth);
                            if (value == WRAP_CONTENT) {
                                matchConstraintMinWidth = WRAP_CONTENT;
                            }
                        }
                        break;
                    }
                    case Table.LAYOUT_CONSTRAINT_WIDTH_MAX: {
                        try {
                            matchConstraintMaxWidth = a.getDimensionPixelSize(attr,
                                    matchConstraintMaxWidth);
                        } catch (Exception e) {
                            int value = a.getInt(attr, matchConstraintMaxWidth);
                            if (value == WRAP_CONTENT) {
                                matchConstraintMaxWidth = WRAP_CONTENT;
                            }
                        }
                        break;
                    }
                    case Table.LAYOUT_CONSTRAINT_WIDTH_PERCENT: {
                        matchConstraintPercentWidth = Math.max(0, a.getFloat(attr,
                                matchConstraintPercentWidth));
                        matchConstraintDefaultWidth = MATCH_CONSTRAINT_PERCENT;
                        break;
                    }
                    case Table.LAYOUT_CONSTRAINT_HEIGHT_MIN: {
                        try {
                            matchConstraintMinHeight = a.getDimensionPixelSize(attr,
                                    matchConstraintMinHeight);
                        } catch (Exception e) {
                            int value = a.getInt(attr, matchConstraintMinHeight);
                            if (value == WRAP_CONTENT) {
                                matchConstraintMinHeight = WRAP_CONTENT;
                            }
                        }
                        break;
                    }
                    case Table.LAYOUT_CONSTRAINT_HEIGHT_MAX: {
                        try {
                            matchConstraintMaxHeight = a.getDimensionPixelSize(attr,
                                    matchConstraintMaxHeight);
                        } catch (Exception e) {
                            int value = a.getInt(attr, matchConstraintMaxHeight);
                            if (value == WRAP_CONTENT) {
                                matchConstraintMaxHeight = WRAP_CONTENT;
                            }
                        }
                        break;
                    }
                    case Table.LAYOUT_CONSTRAINT_HEIGHT_PERCENT: {
                        matchConstraintPercentHeight = Math.max(0, a.getFloat(attr,
                                matchConstraintPercentHeight));
                        matchConstraintDefaultHeight = MATCH_CONSTRAINT_PERCENT;
                        break;
                    }
                    case Table.LAYOUT_CONSTRAINT_TAG:
                        constraintTag = a.getString(attr);
                        break;
                    case Table.LAYOUT_CONSTRAINT_LEFT_CREATOR: {
                        // Skip
                        break;
                    }
                    case Table.LAYOUT_CONSTRAINT_TOP_CREATOR: {
                        // Skip
                        break;
                    }
                    case Table.LAYOUT_CONSTRAINT_RIGHT_CREATOR: {
                        // Skip
                        break;
                    }
                    case Table.LAYOUT_CONSTRAINT_BOTTOM_CREATOR: {
                        // Skip
                        break;
                    }
                    case Table.LAYOUT_CONSTRAINT_BASELINE_CREATOR: {
                        // Skip
                        break;
                    }
                }
            }

            ///////////////////////////////////////////////////////////////////////////////////////
            // Layout margins handling TODO: re-activate in 3.0
            ///////////////////////////////////////////////////////////////////////////////////////
            /*
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR1) {
                leftMargin = originalLeftMargin;
                rightMargin = originalRightMargin;
            }
            */
            ///////////////////////////////////////////////////////////////////////////////////////

            a.recycle();
            validate();
        }

        /**
         * validate the layout
         */
        public void validate() {
            mIsGuideline = false;
            mHorizontalDimensionFixed = true;
            mVerticalDimensionFixed = true;
            ///////////////////////////////////////////////////////////////////////////////////////
            // Layout margins handling TODO: re-activate in 3.0
            ///////////////////////////////////////////////////////////////////////////////////////
            /*
            if (dimensionRatio != null && !widthSet && !heightSet) {
                width = MATCH_CONSTRAINT;
                height = MATCH_CONSTRAINT;
            }
            */
            ///////////////////////////////////////////////////////////////////////////////////////

            if (width == WRAP_CONTENT && constrainedWidth) {
                mHorizontalDimensionFixed = false;
                if (matchConstraintDefaultWidth == MATCH_CONSTRAINT_SPREAD) {
                    matchConstraintDefaultWidth = MATCH_CONSTRAINT_WRAP;
                }
            }
            if (height == WRAP_CONTENT && constrainedHeight) {
                mVerticalDimensionFixed = false;
                if (matchConstraintDefaultHeight == MATCH_CONSTRAINT_SPREAD) {
                    matchConstraintDefaultHeight = MATCH_CONSTRAINT_WRAP;
                }
            }
            if (width == MATCH_CONSTRAINT || width == MATCH_PARENT) {
                mHorizontalDimensionFixed = false;
                // We have to reset LayoutParams width/height to WRAP_CONTENT here,
                // as some widgets like TextView
                // will use the layout params directly as a hint to know
                // if they need to request a layout
                // when their content change (e.g. during setTextView)
                if (width == MATCH_CONSTRAINT
                        && matchConstraintDefaultWidth == MATCH_CONSTRAINT_WRAP) {
                    width = WRAP_CONTENT;
                    constrainedWidth = true;
                }
            }
            if (height == MATCH_CONSTRAINT || height == MATCH_PARENT) {
                mVerticalDimensionFixed = false;
                // We have to reset LayoutParams width/height to WRAP_CONTENT here,
                // as some widgets like TextView
                // will use the layout params directly as a hint to know
                // if they need to request a layout
                // when their content change (e.g. during setTextView)
                if (height == MATCH_CONSTRAINT
                        && matchConstraintDefaultHeight == MATCH_CONSTRAINT_WRAP) {
                    height = WRAP_CONTENT;
                    constrainedHeight = true;
                }
            }
            if (guidePercent != UNSET || guideBegin != UNSET || guideEnd != UNSET) {
                mIsGuideline = true;
                mHorizontalDimensionFixed = true;
                mVerticalDimensionFixed = true;
                if (!(mWidget instanceof Guideline)) {
                    mWidget = new Guideline();
                }
                ((Guideline) mWidget).setOrientation(orientation);
            }
        }

        public LayoutParams(int width, int height) {
            super(width, height);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
        public void resolveLayoutDirection(int layoutDirection) {
            ///////////////////////////////////////////////////////////////////////////////////////
            // Layout margins handling TODO: re-activate in 3.0
            ///////////////////////////////////////////////////////////////////////////////////////
            /*
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                this.layoutDirection = layoutDirection;
                isRtl = (View.LAYOUT_DIRECTION_RTL == layoutDirection);
            }

            // First apply margins.
            leftMargin = originalLeftMargin;
            rightMargin = originalRightMargin;

            if (isRtl) {
                leftMargin = originalRightMargin;
                rightMargin = originalLeftMargin;
                if (startMargin != UNSET) {
                    rightMargin = startMargin;
                }
                if (endMargin != UNSET) {
                    leftMargin = endMargin;
                }
            } else {
                if (startMargin != UNSET) {
                    leftMargin = startMargin;
                }
                if (endMargin != UNSET) {
                    rightMargin = endMargin;
                }
            }
            */
            ///////////////////////////////////////////////////////////////////////////////////////
            int originalLeftMargin = leftMargin;
            int originalRightMargin = rightMargin;

            boolean isRtl = false;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                super.resolveLayoutDirection(layoutDirection);
                isRtl = (View.LAYOUT_DIRECTION_RTL == getLayoutDirection());
            }
            ///////////////////////////////////////////////////////////////////////////////////////

            mResolvedRightToLeft = UNSET;
            mResolvedRightToRight = UNSET;
            mResolvedLeftToLeft = UNSET;
            mResolvedLeftToRight = UNSET;

            mResolveGoneLeftMargin = UNSET;
            mResolveGoneRightMargin = UNSET;
            mResolveGoneLeftMargin = goneLeftMargin;
            mResolveGoneRightMargin = goneRightMargin;
            mResolvedHorizontalBias = horizontalBias;

            mResolvedGuideBegin = guideBegin;
            mResolvedGuideEnd = guideEnd;
            mResolvedGuidePercent = guidePercent;

            // Post JB MR1, if start/end are defined, they take precedence over left/right
            if (isRtl) {
                boolean startEndDefined = false;
                if (startToEnd != UNSET) {
                    mResolvedRightToLeft = startToEnd;
                    startEndDefined = true;
                } else if (startToStart != UNSET) {
                    mResolvedRightToRight = startToStart;
                    startEndDefined = true;
                }
                if (endToStart != UNSET) {
                    mResolvedLeftToRight = endToStart;
                    startEndDefined = true;
                }
                if (endToEnd != UNSET) {
                    mResolvedLeftToLeft = endToEnd;
                    startEndDefined = true;
                }
                if (goneStartMargin != GONE_UNSET) {
                    mResolveGoneRightMargin = goneStartMargin;
                }
                if (goneEndMargin != GONE_UNSET) {
                    mResolveGoneLeftMargin = goneEndMargin;
                }
                if (startEndDefined) {
                    mResolvedHorizontalBias = 1 - horizontalBias;
                }

                // Only apply to vertical guidelines
                if (mIsGuideline && orientation == Guideline.VERTICAL && guidelineUseRtl) {
                    if (guidePercent != UNSET) {
                        mResolvedGuidePercent = 1 - guidePercent;
                        mResolvedGuideBegin = UNSET;
                        mResolvedGuideEnd = UNSET;
                    } else if (guideBegin != UNSET) {
                        mResolvedGuideEnd = guideBegin;
                        mResolvedGuideBegin = UNSET;
                        mResolvedGuidePercent = UNSET;
                    } else if (guideEnd != UNSET) {
                        mResolvedGuideBegin = guideEnd;
                        mResolvedGuideEnd = UNSET;
                        mResolvedGuidePercent = UNSET;
                    }
                }
            } else {
                if (startToEnd != UNSET) {
                    mResolvedLeftToRight = startToEnd;
                }
                if (startToStart != UNSET) {
                    mResolvedLeftToLeft = startToStart;
                }
                if (endToStart != UNSET) {
                    mResolvedRightToLeft = endToStart;
                }
                if (endToEnd != UNSET) {
                    mResolvedRightToRight = endToEnd;
                }
                if (goneStartMargin != GONE_UNSET) {
                    mResolveGoneLeftMargin = goneStartMargin;
                }
                if (goneEndMargin != GONE_UNSET) {
                    mResolveGoneRightMargin = goneEndMargin;
                }
            }
            // if no constraint is defined via RTL attributes, use left/right if present
            if (endToStart == UNSET && endToEnd == UNSET
                    && startToStart == UNSET && startToEnd == UNSET) {
                if (rightToLeft != UNSET) {
                    mResolvedRightToLeft = rightToLeft;
                    if (rightMargin <= 0 && originalRightMargin > 0) {
                        rightMargin = originalRightMargin;
                    }
                } else if (rightToRight != UNSET) {
                    mResolvedRightToRight = rightToRight;
                    if (rightMargin <= 0 && originalRightMargin > 0) {
                        rightMargin = originalRightMargin;
                    }
                }
                if (leftToLeft != UNSET) {
                    mResolvedLeftToLeft = leftToLeft;
                    if (leftMargin <= 0 && originalLeftMargin > 0) {
                        leftMargin = originalLeftMargin;
                    }
                } else if (leftToRight != UNSET) {
                    mResolvedLeftToRight = leftToRight;
                    if (leftMargin <= 0 && originalLeftMargin > 0) {
                        leftMargin = originalLeftMargin;
                    }
                }
            }
        }

        /**
         * Tag that can be used to identify a view as being a member of a group.
         * Which can be used for Helpers or in MotionLayout
         *
         * @return tag string or null if not defined
         */
        public String getConstraintTag() {
            return constraintTag;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void requestLayout() {
        markHierarchyDirty();
        super.requestLayout();
    }

    @Override
    public void forceLayout() {
        markHierarchyDirty();
        super.forceLayout();
    }

    private void markHierarchyDirty() {
        mDirtyHierarchy = true;
        // reset measured cache
        mLastMeasureWidth = -1;
        mLastMeasureHeight = -1;
        mLastMeasureWidthSize = -1;
        mLastMeasureHeightSize = -1;
        mLastMeasureWidthMode = MeasureSpec.UNSPECIFIED;
        mLastMeasureHeightMode = MeasureSpec.UNSPECIFIED;
    }

    /**
     *
     *
     * @return
     */
    @Override
    public boolean shouldDelayChildPressedState() {
        return false;
    }

    /**
     * Returns a JSON5 string useful for debugging the constraints actually applied.
     * In situations where a complex set of code dynamically constructs constraints
     * it is useful to be able to query the layout for what are the constraints.
     * @return json5 string representing the constraint in effect now.
     */
    public String getSceneString() {
        StringBuilder ret = new StringBuilder();

        if (mLayoutWidget.stringId == null) {
            int id = this.getId();
            if (id != -1) {
                String str = getContext().getResources().getResourceEntryName(id);
                mLayoutWidget.stringId = str;
            } else {
                mLayoutWidget.stringId = "parent";
            }
        }
        if (mLayoutWidget.getDebugName() == null) {
            mLayoutWidget.setDebugName(mLayoutWidget.stringId);
            Log.v(TAG, " setDebugName " + mLayoutWidget.getDebugName());
        }

        ArrayList<ConstraintWidget> children = mLayoutWidget.getChildren();
        for (ConstraintWidget child : children) {
            View v = (View) child.getCompanionWidget();
            if (v != null) {
                if (child.stringId == null) {
                    int id = v.getId();
                    if (id != -1) {
                        String str = getContext().getResources().getResourceEntryName(id);
                        child.stringId = str;
                    }
                }
                if (child.getDebugName() == null) {
                    child.setDebugName(child.stringId);
                    Log.v(TAG, " setDebugName " + child.getDebugName());
                }

            }
        }
        mLayoutWidget.getSceneString(ret);
        return ret.toString();
    }

    /**
     * This is the interface to a valued modifier.
     * implement this and add it using addValueModifier
     */
    public interface ValueModifier {
        /**
         *  if needed in the implementation modify params and return true
         * @param width of the ConstraintLayout in pixels
         * @param height of the ConstraintLayout in pixels
         * @param id The id of the view which
         * @param view The View
         * @param params The layout params of the view
         * @return true if you modified the layout params
         */
        boolean update(int width, int height, int id, View view, LayoutParams params);
    }

    private ArrayList<ValueModifier> mModifiers;

    /**
     * a ValueModify to the ConstraintLayout.
     * This can be useful to add custom behavour to the ConstraintLayout or
     * address limitation of the capabilities of Constraint Layout
     * @param modifier
     */
    public void addValueModifier(ValueModifier modifier) {
        if (mModifiers == null) {
            mModifiers = new ArrayList<>();
        }
        mModifiers.add(modifier);
    }

    /**
     * Remove a value modifier this can be useful if the modifier is used during in one state of the
     * system.
     * @param modifier The modifier to remove
     */
    void removeValueModifier(ValueModifier modifier) {
        if (modifier == null) {
            return;
        }
        mModifiers.remove(modifier);
    }

    /**
     * This can be overridden to change the way Modifiers are used.
     * @param widthMeasureSpec
     * @param heightMeasureSpec
     * @return
     */
    protected boolean dynamicUpdateConstraints(int widthMeasureSpec, int heightMeasureSpec) {
        if (mModifiers == null) {
            return false;
        }
        boolean dirty = false;
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = MeasureSpec.getSize(heightMeasureSpec);
        for (ValueModifier m : mModifiers) {
            for (ConstraintWidget widget : mLayoutWidget.getChildren()) {
                View view = (View) widget.getCompanionWidget();
                int id = view.getId();
                LayoutParams layoutParams = (LayoutParams) view.getLayoutParams();
                dirty |= m.update(width, height, id, view, layoutParams);
            }
        }
        return dirty;
    }
}
