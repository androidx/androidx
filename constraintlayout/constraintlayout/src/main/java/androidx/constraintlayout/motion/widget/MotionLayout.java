/*
 * Copyright (C) 2018 The Android Open Source Project
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

package androidx.constraintlayout.motion.widget;

import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;

import static androidx.constraintlayout.motion.widget.MotionScene.Transition.TRANSITION_FLAG_FIRST_DRAW;
import static androidx.constraintlayout.motion.widget.MotionScene.Transition.TRANSITION_FLAG_INTERCEPT_TOUCH;
import static androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.PARENT_ID;
import static androidx.constraintlayout.widget.ConstraintSet.UNSET;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.DashPathEffect;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Bundle;
import android.util.AttributeSet;
import android.util.Log;
import android.util.SparseArray;
import android.util.SparseBooleanArray;
import android.util.SparseIntArray;
import android.view.Display;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Interpolator;
import android.widget.TextView;

import androidx.annotation.IdRes;
import androidx.constraintlayout.core.motion.utils.KeyCache;
import androidx.constraintlayout.core.widgets.ConstraintAnchor;
import androidx.constraintlayout.core.widgets.ConstraintWidget;
import androidx.constraintlayout.core.widgets.ConstraintWidgetContainer;
import androidx.constraintlayout.core.widgets.Flow;
import androidx.constraintlayout.core.widgets.Helper;
import androidx.constraintlayout.core.widgets.Placeholder;
import androidx.constraintlayout.motion.utils.StopLogic;
import androidx.constraintlayout.motion.utils.ViewState;
import androidx.constraintlayout.widget.Barrier;
import androidx.constraintlayout.widget.ConstraintHelper;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.constraintlayout.widget.Constraints;
import androidx.constraintlayout.widget.R;
import androidx.core.view.NestedScrollingParent3;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * A subclass of ConstraintLayout that supports animating between
 * various states <b>Added in 2.0</b>
 * <p>
 * A {@code MotionLayout} is a subclass of {@link ConstraintLayout}
 * which supports transitions between between various states ({@link ConstraintSet})
 * defined in {@link MotionScene}s.
 * <p>
 * <b>Note:</b> {@code MotionLayout} is available as a support library that you can use
 * on Android systems starting with API level 14 (ICS).
 * </p>
 * <p>
 * {@code MotionLayout} links to and requires a {@link MotionScene} file.
 * The file contains one top level tag "MotionScene"
 * <h2>LayoutDescription</h2>
 * <table summary="LayoutDescription">
 * <tr>
 * <th>Tags</th><th>Description</th>
 * </tr>
 * <tr>
 * <td>{@code <StateSet> }</td>
 * <td>Describes states supported by the system (optional)</td>
 * </tr>
 * <tr>
 * <td>{@code <ConstraintSet> }</td>
 * <td>Describes a constraint set</td>
 * </tr>
 * <tr>
 * <td>{@code <Transition> }</td>
 * <td>Describes a transition between two states or ConstraintSets</td>
 * </tr>
 * <tr>
 * <td>{@code <ViewTransition> }</td>
 * <td>Describes a transition of a View within a states or ConstraintSets</td>
 * </tr>
 * </table>
 *
 * <h2>Transition</h2>
 * <table summary="Transition attributes & tags">
 * <tr>
 * <th>Attributes</th><th>Description</th>
 * </tr>
 * <tr>
 * <td>android:id</td>
 * <td>The id of the Transition</td>
 * </tr>
 * <tr>
 * <td>constraintSetStart</td>
 * <td>ConstraintSet to be used as the start constraints or a
 * layout file to get the constraint from</td>
 * </tr>
 * <tr>
 * <td>constraintSetEnd</td>
 * <td>ConstraintSet to be used as the end constraints or a
 * layout file to get the constraint from</td>
 * </tr>
 * <tr>
 * <td>motionInterpolator</td>
 * <td>The ability to set an overall interpolation (easeInOut, linear, etc.)</td>
 * </tr>
 * <tr>
 * <td>duration</td>
 * <td>Length of time to take to perform the transition</td>
 * </tr>
 * <tr>
 * <td>staggered</td>
 * <td>Overrides the Manhattan distance from the top most view in the list of views.
 * <ul>
 *     <li>For any view of stagger value {@code S(Vi)}</li>
 *     <li>With the transition stagger value of {@code TS} (from 0.0 - 1.0)</li>
 *     <li>The duration of the animation is {@code duration}</li>
 *     <li>The views animation duration {@code DS = duration * (1 -TS)}</li>
 *     <li>Call the stagger fraction {@code SFi = (S(Vi) - S(V0)) / (S(Vn) - S(V0))}</li>
 *     <li>The view starts animating at: {@code (duration-DS) * SFi}</li>
 * </ul>
 * </td>
 * </tr>
 * <tr>
 * <td>pathMotionArc</td>
 * <td>The path will move in arc (quarter ellipses)
 * key words {startVertical | startHorizontal | flip | none }</td>
 * </tr>
 * <tr>
 * <td>autoTransition</td>
 * <td>automatically transition from one state to another.
 * key words {none, jumpToStart, jumpToEnd, animateToStart, animateToEnd}</td>
 * </tr>
 * </tr>
 * <tr>
 * <td>transitionFlags</td>
 * <td>flags that adjust the behaviour of Transitions. supports {none, beginOnFirstDraw}
 *      begin on first draw forces the transition's clock to start when it is first
 *      displayed not when the begin is called</td>
 * </tr>
 * </tr>
 * <tr>
 * <td>layoutDuringTransition</td>
 * <td>Configures MotionLayout on how to react to requestLayout calls during transitions.
 * Allowed values are {ignoreRequest, honorRequest}</td>
 * </tr>
 * <tr>
 * <td>{@code <OnSwipe> }</td>
 * <td>Adds support for touch handling (optional)</td>
 * </tr>
 * <tr>
 * <td>{@code <OnClick> }</td>
 * <td>Adds support for triggering transition (optional)</td>
 * </tr>
 * <tr>
 * <td>{@code <KeyFrameSet> }</td>
 * <td>Describes a set of Key object which modify the animation between constraint sets.</td>
 * </tr>
 * </table>
 *
 * <ul>
 * <li>A transition is typically defined by specifying its start and end ConstraintSets.
 * You also have the possibility to not specify them, in which case such transition
 * will become a Default transition.
 * That Default transition will be applied between any state change that isn't
 * explicitly covered by a transition.</li>
 * <li>The starting state of the MotionLayout is defined  to be the constraintSetStart of the first
 * transition.</li>
 * <li>If no transition is specified (or only a default Transition)
 * the MotionLayout tag must contain
 * a app:currentState to define the starting state of the MotionLayout</li>
 * </ul>
 *
 * <h2>ViewTransition</h2>
 * <table summary="Transition attributes & tags">
 * <tr>
 * <th>Attributes</th><th>Description</th>
 * </tr>
 * <tr>
 * <td>android:id</td>
 * <td>The id of the ViewTransition</td>
 * </tr>
 * <tr>
 * <td>viewTransitionMode</td>
 * <td>currentState, allStates, noState transition affect the state of the view
 * in the current constraintSet or all ConstraintSets or non
 *      if noState the ViewTransitions are run asynchronous</td>
 * </tr>
 * <tr>
 * <td>onStateTransition</td>
 * <td>actionDown or actionUp run transition if on touch down or
 * up if view matches motionTarget</td>
 * </tr>
 * <tr>
 * <td>motionInterpolator</td>
 * <td>The ability to set an overall interpolation
 * key words {easeInOut, linear, etc.}</td>
 * </tr>
 * <tr>
 * <td>duration</td>
 * <td>Length of time to take to perform the {@code ViewTransition}</td>
 * </tr>
 * <tr>
 * <td>pathMotionArc</td>
 * <td>The path will move in arc (quarter ellipses)
 * key words {startVertical | startHorizontal | flip | none }</td>
 * </tr>
 * <tr>
 * <td>motionTarget</td>
 * <td>Apply ViewTransition matching this string or id.</td>
 * </tr>
 * </tr>
 * <tr>
 * <td>setsTag</td>
 * <td>set this tag at end of transition</td>
 * </tr>
 * </tr>
 * <tr>
 * <td>clearsTag</td>
 * <td>clears this tag at end of transition</td>
 * </tr>
 * <tr>
 * <td>ifTagSet</td>
 * <td>run transition if this tag is set on view</td>
 * </tr>
 * <tr>
 * <td>ifTagNotSet</td>
 * <td>run transition if this tag is not set on view/td>
 * </tr>
 * <tr>
 * <td>{@code <OnSwipe> }</td>
 * <td>Adds support for touch handling (optional)</td>
 * </tr>
 * <tr>
 * <td>{@code <OnClick> }</td>
 * <td>Adds support for triggering transition (optional)</td>
 * </tr>
 * <tr>
 * <td>{@code <KeyFrameSet> }</td>
 * <td>Describes a set of Key object which modify the animation between constraint sets.</td>
 * </tr>
 * </table>
 *
 * <ul>
 * <li>A Transition is typically defined by specifying its start and end ConstraintSets.
 * You also have the possibility to not specify them, in which case such transition
 * will become a Default transition.
 * That Default transition will be applied between any state change that isn't
 * explicitly covered by a transition.</li>
 * <li>The starting state of the MotionLayout is defined to be the constraintSetStart of the first
 * transition.</li>
 * <li>If no transition is specified (or only a default Transition) the
 * MotionLayout tag must contain
 * a app:currentState to define the starting state of the MotionLayout</li>
 * </ul>
 *
 *
 * <p>
 * <h2>OnSwipe (optional)</h2>
 * <table summary="OnSwipe attributes">
 * <tr>
 * <th>Attributes</th><th>Description</th>
 * </tr>
 * <tr>
 * <td>touchAnchorId</td>
 * <td>Have the drag act as if it is moving the "touchAnchorSide" of this object</td>
 * </tr>
 * <tr>
 * <td>touchRegionId</td>
 * <td>Limits the region that the touch can be start in to the bounds of this view
 * (even if the view is invisible)</td>
 * </tr>
 * <tr>
 * <td>touchAnchorSide</td>
 * <td>The side of the object to move with {top|left|right|bottom}</td>
 * </tr>
 * <tr>
 * <td>maxVelocity</td>
 * <td>limit the maximum velocity (in progress/sec) of the animation will on touch up.
 * Default 4</td>
 * </tr>
 * <tr>
 * <td>dragDirection</td>
 * <td>which side to swipe from {dragUp|dragDown|dragLeft|dragRight}</td>
 * </tr>
 * <tr>
 * <td>maxAcceleration</td>
 * <td>how quickly the animation will accelerate
 * (progress/sec/sec) and decelerate on touch up. Default 1.2</td>
 * </tr>
 * <tr>
 * <td>dragScale</td>
 * <td>scale factor to adjust the swipe by. (e.g. 0.5 would require you to move 2x as much)</td>
 * </tr>
 * <td>dragThreshold</td>
 * <td>How much to drag before swipe gesture runs.
 * Important for mult-direction swipe. Default is 10. 1 is very sensitive.</td>
 * </tr>
 * <tr>
 * <td>moveWhenScrollAtTop</td>
 * <td>If the swipe is scrolling and View (such as RecyclerView or NestedScrollView)
 * do scroll and transition happen at the same time</td>
 * </tr>
 * <tr>
 * <td>onTouchUp</td>
 * <td>Support for various swipe modes
 * autoComplete,autoCompleteToStart,autoCompleteToEnd,stop,decelerate,decelerateAndComplete</td>
 * </tr>
 * </table>
 *
 * <p>
 * <h2>OnClick (optional)</h2>
 * <table summary="OnClick attributes">
 * <tr>
 * <th>Attributes</th><th>Description</th>
 * </tr>
 * <tr>
 * <td>motionTarget</td>
 * <td>What view triggers Transition.</td>
 * </tr>
 * <tr>
 * <td>clickAction</td>
 * <td>Direction for buttons to move the animation.
 * Or (|) combination of:  toggle, transitionToEnd, transitionToStart, jumpToEnd, jumpToStart</td>
 * </tr>
 * </table>
 *
 * <p>
 * <h2>StateSet</h2>
 * <table summary="StateSet tags & attributes">
 * <tr>
 * <td>defaultState</td>
 * <td>The constraint set or layout to use</td>
 * </tr>
 * <tr>
 * <td>{@code <State> }</td>
 * <td>The side of the object to move</td>
 * </tr>
 * </table>
 *
 * <p>
 * <h2>State</h2>
 * <table summary="State attributes">
 * <tr>
 * <td>android:id</td>
 * <td>Id of the State</td>
 * </tr>
 * <tr>
 * <td>constraints</td>
 * <td>Id of the ConstraintSet or the Layout file</td>
 * </tr>
 * <tr>
 * <td>{@code <Variant> }</td>
 * <td>a different constraintSet/layout to choose if the with or height matches</td>
 * </tr>
 * </table>
 *
 * <h2>Variant</h2>
 * <table summary="Variant attributes" >
 * <tr>
 * <td>region_widthLessThan</td>
 * <td>Match if width less than</td>
 * </tr>
 * <tr>
 * <td>region_widthMoreThan</td>
 * <td>Match if width more than</td>
 * </tr>
 * <tr>
 * <td>region_heightLessThan</td>
 * <td>Match if height less than</td>
 * </tr>
 * <tr>
 * <td>region_heightMoreThan</td>
 * <td>Match if height more than</td>
 * </tr>
 * <tr>
 * <td>constraints</td>
 * <td>Id of the ConstraintSet or layout</td>
 * </tr>
 * </table>
 *
 * <p>
 * <h2>ConstraintSet</h2>
 * <table summary="StateSet tags & attributes">
 * <tr>
 * <td>android:id</td>
 * <td>The id of the ConstraintSet</td>
 * </tr>
 * <tr>
 * <td>deriveConstraintsFrom</td>
 * <td>The id of another constraintSet which defines the constraints not define in this set.
 * If not specified the layout defines the undefined constraints.</td>
 * </tr>
 * <tr>
 * <td>{@code <Constraint> }</td>
 * <td>A ConstraintLayout Constraints + other attributes associated with a view</td>
 * </tr>
 * </table>
 *
 * <p>
 * <h2>Constraint</h2>
 * <p> Constraint supports two forms: <p>1: All of ConstraintLayout + the ones listed below +
 * {@code <CustomAttribute> }.</p>
 * <p>Or</p><p>
 * 2: Combination of tags: {@code <Layout> <PropertySet> <Transform> <Motion> <CustomAttribute> }.
 * The advantage of using these is that if not present the attributes are taken from the base
 * layout file. This saves from replicating all the layout tags if only a Motion tag is needed.
 * If <Layout> is used then all layout attributes in the base are ignored. </p>
 * </p>
 * <table summary="Constraint attributes">
 * <tr>
 * <td>android:id</td>
 * <td>Id of the View</td>
 * </tr>
 * <tr>
 * <td>[ConstraintLayout attributes]</td>
 * <td>Any attribute that is part of ConstraintLayout layout is allowed</td>
 * </tr>
 * <tr>
 * <td>[Standard View attributes]</td>
 * <td>A collection of view attributes supported by the system (see below)</td>
 * </tr>
 * <tr>
 * <td>transitionEasing</td>
 * <td>define an easing curve to be used when animating from this point
 * (e.g. {@code curve(1.0,0,0,1.0)})
 * or key words {standard | accelerate | decelerate | linear}</td>
 * </tr>
 * <tr>
 * <td>pathMotionArc</td>
 * <td>the path will move in arc (quarter ellipses)
 * or key words {startVertical | startHorizontal | none }</td>
 * </tr>
 * <tr>
 * <td>transitionPathRotate</td>
 * <td>(float) rotate object relative to path taken</td>
 * </tr>
 * <tr>
 * <td>drawPath</td>
 * <td>draw the path the layout will animate animate</td>
 * </tr>
 * <tr>
 * <td>progress</td>
 * <td>call method setProgress(float) on this  view
 * (used to talk to nested ConstraintLayouts etc.)</td>
 * </tr>
 * <tr>
 * <td>{@code <CustomAttribute> }</td>
 * <td>call a set"name" method via reflection</td>
 * </tr>
 * <tr>
 * <td>{@code <Layout> }</td>
 * <td>Attributes for the ConstraintLayout e.g. layout_constraintTop_toTopOf</td>
 * </tr>
 * <tr>
 * <td>{@code <PropertySet> }</td>
 * <td>currently only visibility, alpha, motionProgress,layout_constraintTag.</td>
 * </tr>
 * <tr>
 * <td>{@code <Transform> }</td>
 * <td>All the view transform API such as android:rotation.</td>
 * </tr>
 * <tr>
 * <td>{@code <Motion> }</td>
 * <td>Motion Layout control commands such as transitionEasing and pathMotionArc</td>
 * </tr>
 * </table>
 *
 * <p>
 * <p>
 * <h2>Layout</h2>
 * <table summary="Variant attributes" >
 * <tr>
 * <td>[ConstraintLayout attributes]</td>
 * <td>Also see {@link ConstraintLayout.LayoutParams
 * ConstraintLayout.LayoutParams} for attributes</td>
 * </tr>
 * </table>
 *
 * <h2>PropertySet</h2>
 * <table summary="Variant attributes" >
 * <tr>
 * <td>visibility</td>
 * <td>set the Visibility of the view. One of Visible, invisible or gone</td>
 * </tr>
 * <tr>
 * <td>alpha</td>
 * <td>setAlpha value</td>
 * </tr>
 * <tr>
 * <td>motionProgress</td>
 * <td>using reflection call setProgress</td>
 * </tr>
 * <tr>
 * <td>layout_constraintTag</td>
 * <td>a tagging string to identify the type of object</td>
 * </tr>
 * </table>
 *
 *
 * <h2>Transform</h2>
 * <table summary="Variant attributes" >
 * <tr>
 * <td>android:elevation</td>
 * <td>base z depth of the view.</td>
 * </tr>
 * <tr>
 * <td>android:rotation</td>
 * <td>rotation of the view, in degrees.</td>
 * </tr>
 * <tr>
 * <td>android:rotationX</td>
 * <td>rotation of the view around the x axis, in degrees.</td>
 * </tr>
 * <tr>
 * <td>android:rotationY</td>
 * <td>rotation of the view around the y axis, in degrees.</td>
 * </tr>
 * <tr>
 * <td>android:scaleX</td>
 * <td>scale of the view in the x direction</td>
 * </tr>
 * <tr>
 * <td>android:scaleY</td>
 * <td>scale of the view in the y direction.</td>
 * </tr>
 * <tr>
 * <td>android:translationX</td>
 * <td>translation in x of the view. This value is added post-layout to the  left
 * property of the view, which is set by its layout.</td>
 * </tr>
 * <tr>
 * <td>android:translationY</td>
 * <td>translation in y of the view. This value is added post-layout to th e top
 * property of the view, which is set by its layout</td>
 * </tr>
 * <tr>
 * <td>android:translationZ</td>
 * <td>translation in z of the view. This value is added to its elevation.</td>
 * </tr>
 * </table>
 *
 *
 * <h2>Motion</h2>
 * <table summary="Variant attributes" >
 * <tr>
 * <td>transitionEasing</td>
 * <td>Defines an acceleration curve.</td>
 * </tr>
 * <tr>
 * <td>pathMotionArc</td>
 * <td>Says the object should move in a quarter ellipse
 * unless the motion is vertical or horizontal</td>
 * </tr>
 * <tr>
 * <td>motionPathRotate</td>
 * <td>set the rotation to the path of the object + this angle.</td>
 * </tr>
 * <tr>
 * <td>drawPath</td>
 * <td>Debugging utility to draw the motion of the path</td>
 * </tr>
 * </table>
 *
 *
 * <h2>CustomAttribute</h2>
 * <table summary="Variant attributes" >
 * <tr>
 * <td>attributeName</td>
 * <td>The name of the attribute. Case sensitive. ( MyAttr will look for method setMyAttr(...)</td>
 * </tr>
 * <tr>
 * <td>customColorValue</td>
 * <td>The value is a color looking setMyAttr(int )</td>
 * </tr>
 * <tr>
 * <td>customIntegerValue</td>
 * <td>The value is an integer looking setMyAttr(int )</td>
 * </tr>
 * <tr>
 * <td>customFloatValue</td>
 * <td>The value is a float looking setMyAttr(float )</td>
 * </tr>
 * <tr>
 * <td>customStringValue</td>
 * <td>The value is a String looking setMyAttr(String )</td>
 * </tr>
 * <tr>
 * <td>customDimension</td>
 * <td>The value is a dimension looking setMyAttr(float )</td>
 * </tr>
 * <tr>
 * <td>customBoolean</td>
 * <td>The value is true or false looking setMyAttr(boolean )</td>
 * </tr>
 * </table>
 *
 * <p>
 * <p>
 * <h2>KeyFrameSet</h2>
 * <p> This is the container for a collection of Key objects (such as KeyPosition) which provide
 * information about how the views should move </p>
 * <table summary="StateSet tags & attributes">
 * <tr>
 * <td>{@code <KeyPosition>}</td>
 * <td>Controls the layout position during animation</td>
 * </tr>
 * <tr>
 * <td>{@code <KeyAttribute>}</td>
 * <td>Controls the post layout properties during animation</td>
 * </tr>
 * <tr>
 * <td>{@code <KeyCycle>}</td>
 * <td>Controls oscillations with respect to position
 * of post layout properties during animation</td>
 * </tr>
 * <tr>
 * <td>{@code <KeyTimeCycle>}</td>
 * <td>Controls oscillations with respect to time of post layout properties during animation</td>
 * </tr>
 * <tr>
 * <td>{@code <KeyTrigger>}</td>
 * <td>trigger callbacks into code at fixed point during the animation</td>
 * </tr>
 * </table>
 *
 * <p>
 * <h2>KeyPosition</h2>
 * <table summary="KeyPosition attributes">
 * <tr>
 * <td>motionTarget</td>
 * <td>Id of the View or a regular expression to match layout_ConstraintTag</td>
 * </tr>
 * <tr>
 * <td>framePosition</td>
 * <td>The point along the interpolation 0 = start 100 = end</td>
 * </tr
 * <tr>
 * <td>transitionEasing</td>
 * <td>define an easing curve to be used when animating from this point (e.g. curve(1.0,0,0, 1.0))
 * or key words {standard | accelerate | decelerate | linear }
 * </td>
 * </tr>
 * <tr>
 * <td>pathMotionArc</td>
 * <td>The path will move in arc (quarter ellipses)
 * key words {startVertical | startHorizontal | flip | none }</td>
 * </tr>
 * <tr>
 * <td>keyPositionType</td>
 * <td>how this keyframe's deviation for linear path is calculated
 * {deltaRelative | pathRelative|parentRelative}</td>
 * </tr>
 * <tr>
 * <td>percentX</td>
 * <td>(float) percent distance from start to end along
 * X axis (deltaRelative) or along the path in pathRelative</td>
 * </tr>
 * <tr>
 * <td>percentY</td>
 * <td>(float) Percent distance from start to end along Y axis
 * (deltaRelative) or perpendicular to path in pathRelative</td>
 * </tr>
 * <tr>
 * <td>percentWidth</td>
 * <td>(float) Percent of change in the width.
 * Note if the width does not change this has no effect.This overrides sizePercent.</td>
 * </tr>
 * <tr>
 * <td>percentHeight</td>
 * <td>(float) Percent of change in the width.
 * Note if the width does not change this has no effect.This overrides sizePercent.</td>
 * </tr>
 * <tr>
 * <td>curveFit</td>
 * <td>path is traced</td>
 * </tr>
 * <tr>
 * <td>drawPath</td>
 * <td>Draw the path of the objects layout takes useful for debugging</td>
 * </tr>
 * <tr>
 * <td>sizePercent</td>
 * <td>If the view changes size this controls how growth of the  size.
 * (for fixed size objects use KeyAttributes scaleX/X)</td>
 * </tr>
 * <tr>
 * <td>curveFit</td>
 * <td>selects a path based on straight lines or a path based on a
 * monotonic spline {linear|spline}</td>
 * </tr>
 * </table>
 *
 * <p>
 * <p>
 * <h2>KeyAttribute</h2>
 * <table summary="KeyAttribute attributes">
 * <tr>
 * <td>motionTarget</td>
 * <td>Id of the View or a regular expression to match layout_ConstraintTag</td>
 * </tr>
 * <tr>
 * <td>framePosition</td>
 * <td>The point along the interpolation 0 = start 100 = end</td>
 * </tr>
 * <tr>
 * <td>curveFit</td>
 * <td>selects a path based on straight lines or a path
 * based on a monotonic spline {linear|spline}</td>
 * </tr>
 * <tr>
 * <td>transitionEasing</td>
 * <td>Define an easing curve to be used when animating from this point (e.g. curve(1.0,0,0, 1.0))
 * or key words {standard | accelerate | decelerate | linear }
 * </td>
 * </tr>
 * <tr>
 * <td>transitionPathRotate</td>
 * <td>(float) rotate object relative to path taken</td>
 * </tr>
 * <tr>
 * <td>drawPath</td>
 * <td>draw the path the layout will animate animate</td>
 * </tr>
 * <tr>
 * <td>motionProgress</td>
 * <td>call method setProgress(float) on this  view
 * (used to talk to nested ConstraintLayouts etc.)</td>
 * </tr>
 * <tr>
 * <td>[standard view attributes](except visibility)</td>
 * <td>A collection of post layout view attributes see below </td>
 * </tr>
 * <tr>
 * <p>
 * <tr>
 * <td>{@code <CustomAttribute> }</td>
 * <td>call a set"name" method via reflection</td>
 * </tr>
 * </table>
 *
 * <h2>CustomAttribute</h2>
 * <table summary="Variant attributes" >
 * <tr>
 * <td>attributeName</td>
 * <td>The name of the attribute. Case sensitive. ( MyAttr will look for method setMyAttr(...)</td>
 * </tr>
 * <tr>
 * <td>customColorValue</td>
 * <td>The value is a color looking setMyAttr(int )</td>
 * </tr>
 * <tr>
 * <td>customIntegerValue</td>
 * <td>The value is an integer looking setMyAttr(int )</td>
 * </tr>
 * <tr>
 * <td>customFloatValue</td>
 * <td>The value is a float looking setMyAttr(float )</td>
 * </tr>
 * <tr>
 * <td>customStringValue</td>
 * <td>The value is a String looking setMyAttr(String )</td>
 * </tr>
 * <tr>
 * <td>customDimension</td>
 * <td>The value is a dimension looking setMyAttr(float )</td>
 * </tr>
 * <tr>
 * <td>customBoolean</td>
 * <td>The value is true or false looking setMyAttr(boolean )</td>
 * </tr>
 * </table>
 *
 * </p>
 * <p>
 * <h2>KeyCycle</h2>
 * <table summary="Constraint attributes">
 * <tr>
 * <td>motionTarget</td>
 * <td>Id of the View or a regular expression to match layout_ConstraintTag</td>
 * </tr>
 * <tr>
 * <td>framePosition</td>
 * <td>The point along the interpolation 0 = start 100 = end</td>
 * </tr>
 * <tr>
 * <td>[Standard View attributes]</td>
 * <td>A collection of view attributes supported by the system (see below)</td>
 * </tr>
 * <tr>
 * <td>waveShape</td>
 * <td>The shape of the wave to generate
 * {sin|square|triangle|sawtooth|reverseSawtooth|cos|bounce}</td>
 * </tr>
 * <tr>
 * <td>wavePeriod</td>
 * <td>The number of cycles to loop near this region</td>
 * </tr>
 * <tr>
 * <td>waveOffset</td>
 * <td>offset value added to the attribute</td>
 * </tr>
 * <tr>
 * <td>transitionPathRotate</td>
 * <td>Cycles applied to rotation relative to the path the view is travelling</td>
 * </tr>
 * <tr>
 * <td>progress</td>
 * <td>call method setProgress(float) on this  view
 * (used to talk to nested ConstraintLayouts etc.)</td>
 * </tr>
 * <tr>
 * <td>{@code <CustomAttribute> }</td>
 * <td>call a set"name" method via reflection (limited to floats)</td>
 * </tr>
 * </table>
 *
 * <h2>CustomAttribute</h2>
 * <table summary="Variant attributes" >
 * <tr>
 * <td>attributeName</td>
 * <td>The name of the attribute. Case sensitive. ( MyAttr will look for method setMyAttr(...)</td>
 * </tr>
 * <tr>
 * <tr>
 * <td>customFloatValue</td>
 * <td>The value is a float looking setMyAttr(float )</td>
 * </tr>
 * <tr>
 * </table>
 *
 * <h2>KeyTimeCycle</h2>
 * <table summary="Constraint attributes">
 * <tr>
 * <td>motionTarget</td>
 * <td>Id of the View or a regular expression to match layout_ConstraintTag</td>
 * </tr>
 * <tr>
 * <td>framePosition</td>
 * <td>The point along the interpolation 0 = start 100 = end</td>
 * </tr>
 * <tr>
 * <td>[Standard View attributes]</td>
 * <td>A collection of view attributes supported by the system (see below)</td>
 * </tr>
 * <tr>
 * <td>waveShape</td>
 * <td>The shape of the wave to generate
 * {sin|square|triangle|sawtooth|reverseSawtooth|cos|bounce}</td>
 * </tr>
 * <tr>
 * <td>wavePeriod</td>
 * <td>The number of cycles per second</td>
 * </tr>
 * <tr>
 * <td>waveOffset</td>
 * <td>offset value added to the attribute</td>
 * </tr>
 * <tr>
 * <td>transitionPathRotate</td>
 * <td>Cycles applied to rotation relative to the path the view is travelling</td>
 * </tr>
 * <tr>
 * <td>progress</td>
 * <td>call method setProgress(float) on this  view
 * (used to talk to nested ConstraintLayouts etc.)</td>
 * </tr>
 * <tr>
 * <td>{@code <CustomAttribute> }</td>
 * <td>call a set"name" method via reflection (limited to floats)</td>
 * </tr>
 * </table>
 *
 * <h2>CustomAttribute</h2>
 * <table summary="Variant attributes" >
 * <tr>
 * <td>attributeName</td>
 * <td>The name of the attribute. Case sensitive. ( MyAttr will look for method setMyAttr(...)</td>
 * </tr>
 * <tr>
 * <tr>
 * <td>customFloatValue</td>
 * <td>The value is a float looking setMyAttr(float )</td>
 * </tr>
 * <tr>
 * </table>
 *
 * <h2>KeyTrigger</h2>
 * <table summary="KeyTrigger attributes">
 * <tr>
 * <td>motionTarget</td>
 * <td>Id of the View or a regular expression to match layout_ConstraintTag</td>
 * </tr>
 * <tr>
 * <td>framePosition</td>
 * <td>The point along the interpolation 0 = start 100 = end</td>
 * </tr
 * <tr>
 * <td>onCross</td>
 * <td>(method name) on crossing this position call this methods on the t arget
 * </td>
 * </tr>
 * <tr>
 * <td>onPositiveCross</td>
 * <td>(method name) on forward crossing of the framePosition call this methods on the target</td>
 * </tr>
 * <tr>
 * <td>onNegativeCross/td>
 * <td>(method name) backward crossing of the framePosition call this methods on the target</td>
 * </tr>
 * <tr>
 * <td>viewTransitionOnCross</td>
 * <td>(ViewTransition Id) start a NoState view transition on crossing or hitting target
 * </td>
 * </tr>
 * <tr>
 * <td>viewTransitionOnPositiveCross</td>
 * <td>(ViewTransition Id) start a NoState view transition forward crossing of the
 * framePosition or entering target</td>
 * </tr>
 * <tr>
 * <td>viewTransitionOnNegativeCross/td>
 * <td>(ViewTransition Id) start a NoState view transition backward crossing of the
 * framePosition or leaving target</td>
 * </tr>
 * <tr>
 * <td>triggerSlack</td>
 * <td>(float) do not call trigger again if the framePosition has not moved this
 * fraction away from the trigger point</td>
 * </tr>
 * <tr>
 * <td>triggerId</td>
 * <td>(id) call the TransitionListener with this trigger id</td>
 * </tr>
 * <tr>
 * <td>motion_postLayoutCollision</td>
 * <td>Define motion pre or post layout. Post layout is more expensive but captures
 * KeyAttributes or KeyCycle motions.</td>
 * </tr>
 * <tr>
 * <td>motion_triggerOnCollision</td>
 * <td>(id) Trigger if the motionTarget collides with the other motionTarget</td>
 * </tr>
 * </table>
 *
 * </p>
 * <p>
 * <h2>Standard attributes</h2>
 * <table summary="Constraint attributes">
 * <tr>
 * <td>android:visibility</td>
 * <td>Android view attribute that</td>
 * </tr>
 * <tr>
 * <td>android:alpha</td>
 * <td>Android view attribute that</td>
 * </tr>
 * <tr>
 * <td>android:elevation</td>
 * <td>base z depth of the view.</td>
 * </tr>
 * <tr>
 * <td>android:rotation</td>
 * <td>rotation of the view, in degrees.</td>
 * </tr>
 * <tr>
 * <td>android:rotationX</td>
 * <td>rotation of the view around the x axis, in degrees.</td>
 * </tr>
 * <tr>
 * <td>android:rotationY</td>
 * <td>rotation of the view around the y axis, in degrees.</td>
 * </tr>
 * <tr>
 * <td>android:scaleX</td>
 * <td>scale of the view in the x direction.</td>
 * </tr>
 * <tr>
 * <td>android:scaleY</td>
 * <td>scale of the view in the y direction.</td>
 * </tr>
 * <tr>
 * <td>android:translationX</td>
 * <td>translation in x of the view.</td>
 * </tr>
 * <tr>
 * <td>android:translationY</td>
 * <td>translation in y of the view.</td>
 * </tr>
 * <tr>
 * <td>android:translationZ</td>
 * <td>translation in z of the view.</td>
 * </tr>
 * <p>
 * </table>
 *
 * </p>
 */
public class MotionLayout extends ConstraintLayout implements
        NestedScrollingParent3 {

    public static final int TOUCH_UP_COMPLETE = 0;
    public static final int TOUCH_UP_COMPLETE_TO_START = 1;
    public static final int TOUCH_UP_COMPLETE_TO_END = 2;
    public static final int TOUCH_UP_STOP = 3;
    public static final int TOUCH_UP_DECELERATE = 4;
    public static final int TOUCH_UP_DECELERATE_AND_COMPLETE = 5;
    public static final int TOUCH_UP_NEVER_TO_START = 6;
    public static final int TOUCH_UP_NEVER_TO_END = 7;

    static final String TAG = "MotionLayout";
    private static final boolean DEBUG = false;

    public static boolean IS_IN_EDIT_MODE;

    MotionScene mScene;
    Interpolator mInterpolator;
    Interpolator mProgressInterpolator = null;
    float mLastVelocity = 0;
    private int mBeginState = UNSET;
    int mCurrentState = UNSET;
    private int mEndState = UNSET;
    private int mLastWidthMeasureSpec = 0;
    private int mLastHeightMeasureSpec = 0;
    private boolean mInteractionEnabled = true;

    HashMap<View, MotionController> mFrameArrayList = new HashMap<>();

    private long mAnimationStartTime = 0;
    private float mTransitionDuration = 1f;
    float mTransitionPosition = 0.0f;
    float mTransitionLastPosition = 0.0f;
    private long mTransitionLastTime;
    float mTransitionGoalPosition = 0.0f;
    private boolean mTransitionInstantly;
    boolean mInTransition = false;
    boolean mIndirectTransition = false;
    private TransitionListener mTransitionListener;
    private float mLastPos;
    private float mLastY;
    public static final int DEBUG_SHOW_NONE = 0;
    public static final int DEBUG_SHOW_PROGRESS = 1;
    public static final int DEBUG_SHOW_PATH = 2;
    int mDebugPath = 0;
    // variable used in painting the debug
    static final int MAX_KEY_FRAMES = 50;
    DevModeDraw mDevModeDraw;
    private boolean mTemporalInterpolator = false;
    private StopLogic mStopLogic = new StopLogic();
    private DecelerateInterpolator mDecelerateLogic = new DecelerateInterpolator();

    private DesignTool mDesignTool;

    boolean mFirstDown = true;

    int mOldWidth;
    int mOldHeight;
    int mLastLayoutWidth;
    int mLastLayoutHeight;

    boolean mUndergoingMotion = false;
    float mScrollTargetDX;
    float mScrollTargetDY;
    long mScrollTargetTime;
    float mScrollTargetDT;
    private boolean mKeepAnimating = false;

    private ArrayList<MotionHelper> mOnShowHelpers = null;
    private ArrayList<MotionHelper> mOnHideHelpers = null;
    private ArrayList<MotionHelper> mDecoratorsHelpers = null;
    private CopyOnWriteArrayList<TransitionListener> mTransitionListeners = null;
    private int mFrames = 0;
    private long mLastDrawTime = -1;
    private float mLastFps = 0;
    private int mListenerState = 0;
    private float mListenerPosition = 0.0f;
    boolean mIsAnimating = false;

    public static final int VELOCITY_POST_LAYOUT = 0;
    public static final int VELOCITY_LAYOUT = 1;
    public static final int VELOCITY_STATIC_POST_LAYOUT = 2;
    public static final int VELOCITY_STATIC_LAYOUT = 3;

    protected boolean mMeasureDuringTransition = false;
    int mStartWrapWidth;
    int mStartWrapHeight;
    int mEndWrapWidth;
    int mEndWrapHeight;
    int mWidthMeasureMode;
    int mHeightMeasureMode;
    float mPostInterpolationPosition;
    private KeyCache mKeyCache = new KeyCache();
    private boolean mInLayout = false;
    private StateCache mStateCache;
    private Runnable mOnComplete = null;
    private int[] mScheduledTransitionTo = null;
    int mScheduledTransitions = 0;
    private boolean mInRotation = false;
    int mRotatMode = 0;
    HashMap<View, ViewState> mPreRotate = new HashMap<>();
    private int mPreRotateWidth;
    private int mPreRotateHeight;
    private int mPreviouseRotation;
    Rect mTempRect = new Rect();
    private boolean mDelayedApply = false;

    MotionController getMotionController(int mTouchAnchorId) {
        return mFrameArrayList.get(findViewById(mTouchAnchorId));
    }

    enum TransitionState {
        UNDEFINED,
        SETUP,
        MOVING,
        FINISHED;
    };

    TransitionState mTransitionState = TransitionState.UNDEFINED;
    private static final float EPSILON = 0.00001f;

    public MotionLayout(@NonNull Context context) {
        super(context);
        init(null);
    }

    public MotionLayout(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(attrs);
    }

    public MotionLayout(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(attrs);
    }

    /**
     * Subclasses can override to define testClasses
     *
     * @return
     */
    protected long getNanoTime() {
        return System.nanoTime();
    }

    /**
     * Subclasses can override to build test frameworks
     *
     * @return
     */
    protected MotionTracker obtainVelocityTracker() {
        return MyTracker.obtain();
    }

    /**
     * Disable the transition based on transitionID
     * @param transitionID
     * @param enable
     */
    public void enableTransition(int transitionID, boolean enable) {
        MotionScene.Transition t = getTransition(transitionID);
        if (enable) {
            t.setEnabled(true);
            return;
        } else {
            if (t == mScene.mCurrentTransition) { // disabling current transition
                List<MotionScene.Transition> transitions =
                        mScene.getTransitionsWithState(mCurrentState);
                for (MotionScene.Transition transition : transitions) {
                    if (transition.isEnabled()) {
                        mScene.mCurrentTransition = transition;
                        break;
                    }
                }
            }
            t.setEnabled(false);
        }
    }

    /**
     * Subclasses can override to build test frameworks
     */
    protected interface MotionTracker {
        void recycle();

        void clear();

        void addMovement(MotionEvent event);

        void computeCurrentVelocity(int units);

        void computeCurrentVelocity(int units, float maxVelocity);

        float getXVelocity();

        float getYVelocity();

        float getXVelocity(int id);

        float getYVelocity(int id);
    }

    void setState(TransitionState newState) {
        if (DEBUG) {
            Debug.logStack(TAG, mTransitionState + " -> " + newState + " "
                    + Debug.getName(getContext(), mCurrentState), 2);
        }
        if (newState == TransitionState.FINISHED && mCurrentState == UNSET) {
            return;
        }
        TransitionState oldState = mTransitionState;
        mTransitionState = newState;

        if (oldState == TransitionState.MOVING && newState == TransitionState.MOVING) {
            fireTransitionChange();
        }
        switch (oldState) {
            case UNDEFINED:
            case SETUP:
                if (newState == TransitionState.MOVING) {
                    fireTransitionChange();
                }
                if (newState == TransitionState.FINISHED) {
                    fireTransitionCompleted();
                }
                break;
            case MOVING:
                if (newState == TransitionState.FINISHED) {
                    fireTransitionCompleted();
                }
                break;
            case FINISHED:
                break;
        }
    }

    private static class MyTracker implements MotionTracker {
        VelocityTracker mTracker;
        private static MyTracker sMe = new MyTracker();

        public static MyTracker obtain() {
            sMe.mTracker = VelocityTracker.obtain();
            return sMe;
        }

        @Override
        public void recycle() {
            if (mTracker != null) {
                mTracker.recycle();
                mTracker = null; // not allowed to call after recycle
            }
        }

        @Override
        public void clear() {
            if (mTracker != null) {
                mTracker.clear();
            }
        }

        @Override
        public void addMovement(MotionEvent event) {
            if (mTracker != null) {
                mTracker.addMovement(event);
            }
        }

        @Override
        public void computeCurrentVelocity(int units) {
            if (mTracker != null) {
                mTracker.computeCurrentVelocity(units);
            }
        }

        @Override
        public void computeCurrentVelocity(int units, float maxVelocity) {
            if (mTracker != null) {
                mTracker.computeCurrentVelocity(units, maxVelocity);
            }
        }

        @Override
        public float getXVelocity() {
            if (mTracker != null) {
                return mTracker.getXVelocity();
            }
            return 0;
        }

        @Override
        public float getYVelocity() {
            if (mTracker != null) {
                return mTracker.getYVelocity();
            }
            return 0;
        }

        @Override
        public float getXVelocity(int id) {
            if (mTracker != null) {
                return mTracker.getXVelocity(id);
            }
            return 0;
        }

        @Override
        public float getYVelocity(int id) {
            if (mTracker != null) {
                return getYVelocity(id);
            }
            return 0;
        }
    }

    /**
     * sets the state to start in. To be used during OnCreate
     *
     * @param beginId the id of the start constraint set
     */
    void setStartState(int beginId) {
        if (!isAttachedToWindow()) {
            if (mStateCache == null) {
                mStateCache = new StateCache();
            }
            mStateCache.setStartState(beginId);
            mStateCache.setEndState(beginId);
            return;
        }
        mCurrentState = beginId;
    }

    /**
     * Set a transition explicitly between two constraint sets
     *
     * @param beginId the id of the start constraint set
     * @param endId   the id of the end constraint set
     */
    public void setTransition(int beginId, int endId) {
        if (!isAttachedToWindow()) {
            if (mStateCache == null) {
                mStateCache = new StateCache();
            }
            mStateCache.setStartState(beginId);
            mStateCache.setEndState(endId);
            return;
        }

        if (mScene != null) {
            mBeginState = beginId;
            mEndState = endId;
            if (DEBUG) {
                Log.v(TAG, Debug.getLocation() + " setTransition "
                        + Debug.getName(getContext(), beginId) + " -> "
                        + Debug.getName(getContext(), endId));
            }
            mScene.setTransition(beginId, endId);
            mModel.initFrom(mLayoutWidget, mScene.getConstraintSet(beginId),
                    mScene.getConstraintSet(endId));
            rebuildScene();
            mTransitionLastPosition = 0;
            transitionToStart();
        }
    }

    /**
     * Set a transition explicitly to a Transition that has an ID
     * The transition must have been named with android:id=...
     *
     * @param transitionId the id to set
     */
    public void setTransition(int transitionId) {
        if (mScene != null) {
            MotionScene.Transition transition = getTransition(transitionId);
            mBeginState = transition.getStartConstraintSetId();
            mEndState = transition.getEndConstraintSetId();

            if (!isAttachedToWindow()) {
                if (mStateCache == null) {
                    mStateCache = new StateCache();
                }
                mStateCache.setStartState(mBeginState);
                mStateCache.setEndState(mEndState);
                return;
            }

            if (DEBUG) {
                Log.v(TAG, Debug.getLocation() + " setTransition "
                        + Debug.getName(getContext(), mBeginState) + " -> "
                        + Debug.getName(getContext(), mEndState)
                        + "   current=" + Debug.getName(getContext(), mCurrentState));
            }

            float pos = Float.NaN;
            if (mCurrentState == mBeginState) {
                pos = 0;
            } else if (mCurrentState == mEndState) {
                pos = 1;
            }
            mScene.setTransition(transition);
            mModel.initFrom(mLayoutWidget,
                    mScene.getConstraintSet(mBeginState),
                    mScene.getConstraintSet(mEndState));
            rebuildScene();

            if (mTransitionLastPosition != pos) {
                // If the last drawn position isn't the same,
                // we might have to make sure we apply the corresponding constraintset.
                if (pos == 0) {
                    endTrigger(true);
                    mScene.getConstraintSet(mBeginState).applyTo(this);
                } else if (pos == 1) {
                    endTrigger(false);
                    mScene.getConstraintSet(mEndState).applyTo(this);
                }
            }

            mTransitionLastPosition = Float.isNaN(pos) ? 0 : pos;

            if (Float.isNaN(pos)) {
                Log.v(TAG, Debug.getLocation() + " transitionToStart ");
                transitionToStart();
            } else {
                setProgress(pos);
            }
        }
    }

    protected void setTransition(MotionScene.Transition transition) {
        mScene.setTransition(transition);
        setState(TransitionState.SETUP);
        if (mCurrentState == mScene.getEndId()) {
            mTransitionLastPosition = 1.0f;
            mTransitionPosition = 1.0f;
            mTransitionGoalPosition = 1;
        } else {
            mTransitionLastPosition = 0;
            mTransitionPosition = 0f;
            mTransitionGoalPosition = 0;
        }
        mTransitionLastTime =
                transition.isTransitionFlag(TRANSITION_FLAG_FIRST_DRAW) ? -1 : getNanoTime();
        if (DEBUG) {
            Log.v(TAG, Debug.getLocation() + "  new mTransitionLastPosition = "
                    + mTransitionLastPosition + "");
            Log.v(TAG, Debug.getLocation() + " setTransition was "
                    + Debug.getName(getContext(), mBeginState)
                    + " -> " + Debug.getName(getContext(), mEndState));
        }
        int newBeginState = mScene.getStartId();
        int newEndState = mScene.getEndId();
        if (newBeginState == mBeginState && newEndState == mEndState) {
            return;
        }
        mBeginState = newBeginState;
        mEndState = newEndState;
        mScene.setTransition(mBeginState, mEndState);

        if (DEBUG) {
            Log.v(TAG, Debug.getLocation() + " setTransition now "
                    + Debug.getName(getContext(), mBeginState) + " -> "
                    + Debug.getName(getContext(), mEndState));
        }

        mModel.initFrom(mLayoutWidget,
                mScene.getConstraintSet(mBeginState),
                mScene.getConstraintSet(mEndState));
        mModel.setMeasuredId(mBeginState, mEndState);
        mModel.reEvaluateState();

        rebuildScene();
    }

    /**
     * This overrides ConstraintLayout and only accepts a MotionScene.
     *
     * @param motionScene The resource id, or 0 to reset the MotionScene.
     */
    @Override
    public void loadLayoutDescription(int motionScene) {
        if (motionScene != 0) {
            try {
                mScene = new MotionScene(getContext(), this, motionScene);
                if (mCurrentState == UNSET && mScene != null) {
                    mCurrentState = mScene.getStartId();
                    mBeginState = mScene.getStartId();
                    mEndState = mScene.getEndId();
                }
                if (isAttachedToWindow()) {
                    try {
                        Display display = getDisplay();
                        mPreviouseRotation = (display == null) ? 0 : display.getRotation();

                        if (mScene != null) {
                            ConstraintSet cSet = mScene.getConstraintSet(mCurrentState);
                            mScene.readFallback(this);
                            if (mDecoratorsHelpers != null) {
                                for (MotionHelper mh : mDecoratorsHelpers) {
                                    mh.onFinishedMotionScene(this);
                                }
                            }
                            if (cSet != null) {
                                cSet.applyTo(this);
                            }
                            mBeginState = mCurrentState;
                        }
                        onNewStateAttachHandlers();
                        if (mStateCache != null) {
                            if (mDelayedApply) {
                                post(new Runnable() {
                                    @Override
                                    public void run() {
                                        mStateCache.apply();
                                    }
                                });
                            } else {
                                mStateCache.apply();
                            }
                        } else {
                            if (mScene != null && mScene.mCurrentTransition != null) {
                                if (mScene.mCurrentTransition.getAutoTransition()
                                        == MotionScene.Transition.AUTO_ANIMATE_TO_END) {
                                    transitionToEnd();
                                    setState(TransitionState.SETUP);
                                    setState(TransitionState.MOVING);
                                }
                            }

                        }
                    } catch (Exception ex) {
                        throw new IllegalArgumentException("unable to parse MotionScene file", ex);
                    }
                } else {
                    mScene = null;
                }

            } catch (Exception ex) {
                throw new IllegalArgumentException("unable to parse MotionScene file", ex);
            }
        } else {
            mScene = null;
        }
    }

    /**
     * Set the State of the Constraint layout. Causing it to load a particular ConstraintSet.
     * for states with variants the variant with matching
     * width and height constraintSet will be chosen
     *
     * @param id           set the state width and height
     * @param screenWidth
     * @param screenHeight
     */
    @Override
    public void setState(int id, int screenWidth, int screenHeight) {
        setState(TransitionState.SETUP);
        mCurrentState = id;
        mBeginState = UNSET;
        mEndState = UNSET;
        if (mConstraintLayoutSpec != null) {
            mConstraintLayoutSpec.updateConstraints(id, screenWidth, screenHeight);
        } else if (mScene != null) {
            mScene.getConstraintSet(id).applyTo(this);
        }
    }

    /**
     * Set the transition position between 0 an 1
     *
     * @param pos
     */
    public void setInterpolatedProgress(float pos) {
        if (mScene != null) {
            setState(TransitionState.MOVING);
            Interpolator interpolator = mScene.getInterpolator();
            if (interpolator != null) {
                setProgress(interpolator.getInterpolation(pos));
                return;
            }
        }
        setProgress(pos);
    }

    /**
     * Set the transition position between 0 an 1
     *
     * @param pos
     * @param velocity
     */
    public void setProgress(float pos, float velocity) {
        if (!isAttachedToWindow()) {
            if (mStateCache == null) {
                mStateCache = new StateCache();
            }
            mStateCache.setProgress(pos);
            mStateCache.setVelocity(velocity);
            return;
        }
        setProgress(pos);
        setState(TransitionState.MOVING);
        mLastVelocity = velocity;
        if (velocity != 0.0f) {
            animateTo(velocity > 0 ? 1 : 0);
        } else if (pos != 0f && pos != 1f) {
            animateTo(pos > 0.5f ? 1 : 0);
        }
    }

    /////////////////////// use to cache the state
    class StateCache {
        float mProgress = Float.NaN;
        float mVelocity = Float.NaN;
        int mStartState = UNSET;
        int mEndState = UNSET;
        final String mKeyProgress = "motion.progress";
        final String mKeyVelocity = "motion.velocity";
        final String mKeyStartState = "motion.StartState";
        final String mKeyEndState = "motion.EndState";

        void apply() {
            if (this.mStartState != UNSET || this.mEndState != UNSET) {
                if (this.mStartState == UNSET) {
                    transitionToState(mEndState);
                } else if (this.mEndState == UNSET) {
                    setState(this.mStartState, -1, -1);
                } else {
                    setTransition(mStartState, mEndState);
                }
                setState(TransitionState.SETUP);
            }
            if (Float.isNaN(this.mVelocity)) {
                if (Float.isNaN(this.mProgress)) {
                    return;
                }
                MotionLayout.this.setProgress(this.mProgress);
                return;
            }
            MotionLayout.this.setProgress(this.mProgress, mVelocity);
            this.mProgress = Float.NaN;
            this.mVelocity = Float.NaN;
            this.mStartState = UNSET;
            this.mEndState = UNSET;
        }

        public Bundle getTransitionState() {
            Bundle bundle = new Bundle();
            bundle.putFloat(mKeyProgress, this.mProgress);
            bundle.putFloat(mKeyVelocity, this.mVelocity);
            bundle.putInt(mKeyStartState, this.mStartState);
            bundle.putInt(mKeyEndState, this.mEndState);
            return bundle;
        }

        public void setTransitionState(Bundle bundle) {
            this.mProgress = bundle.getFloat(mKeyProgress);
            this.mVelocity = bundle.getFloat(mKeyVelocity);
            this.mStartState = bundle.getInt(mKeyStartState);
            this.mEndState = bundle.getInt(mKeyEndState);
        }

        public void setProgress(float progress) {
            this.mProgress = progress;
        }

        public void setEndState(int endState) {
            this.mEndState = endState;
        }

        public void setVelocity(float mVelocity) {
            this.mVelocity = mVelocity;
        }

        public void setStartState(int startState) {
            this.mStartState = startState;
        }

        public void recordState() {
            mEndState = MotionLayout.this.mEndState;
            mStartState = MotionLayout.this.mBeginState;
            mVelocity = MotionLayout.this.getVelocity();
            mProgress = MotionLayout.this.getProgress();
        }
    }

    /**
     * Set the transition state as a bundle
     */
    public void setTransitionState(Bundle bundle) {
        if (mStateCache == null) {
            mStateCache = new StateCache();
        }
        mStateCache.setTransitionState(bundle);
        if (isAttachedToWindow()) {
            mStateCache.apply();
        }
    }

    /**
     * @return bundle containing start and end state
     */
    public Bundle getTransitionState() {
        if (mStateCache == null) {
            mStateCache = new StateCache();
        }
        mStateCache.recordState();
        return mStateCache.getTransitionState();
    }

    /**
     * Set the transition position between 0 an 1
     *
     * @param pos the position in the transition from 0...1
     */
    public void setProgress(float pos) {
        if (pos < 0.0f || pos > 1.0f) {
            Log.w(TAG, "Warning! Progress is defined for values between 0.0 and 1.0 inclusive");
        }
        if (!isAttachedToWindow()) {
            if (mStateCache == null) {
                mStateCache = new StateCache();
            }
            mStateCache.setProgress(pos);
            return;
        }
        if (DEBUG) {
            String str = getContext().getResources().getResourceName(mBeginState) + " -> ";
            str += getContext().getResources().getResourceName(mEndState) + ":" + getProgress();
            Log.v(TAG, Debug.getLocation() + " > " + str);
            Debug.logStack(TAG, " Progress = " + pos, 3);
        }

        if (pos <= 0f) {
            if (mTransitionLastPosition == 1.0f && mCurrentState == mEndState) {
                setState(TransitionState.MOVING); // fire a transient moving as jumping start to end
            }

            mCurrentState = mBeginState;
            if (mTransitionLastPosition == 0.0f) {
                setState(TransitionState.FINISHED);
            }
        } else if (pos >= 1.0f) {
            if (mTransitionLastPosition == 0.0f && mCurrentState == mBeginState) {
                setState(TransitionState.MOVING); // fire a transient moving as jumping end to start
            }

            mCurrentState = mEndState;
            if (mTransitionLastPosition == 1.0f) {
                setState(TransitionState.FINISHED);
            }
        } else {
            mCurrentState = UNSET;
            setState(TransitionState.MOVING);
        }

        if (mScene == null) {
            return;
        }

        mTransitionInstantly = true;
        mTransitionGoalPosition = pos;
        mTransitionPosition = pos;
        mTransitionLastTime = -1;
        mAnimationStartTime = -1;
        mInterpolator = null;

        mInTransition = true;
        invalidate();
    }

    /**
     * Create a transition view for every view
     */
    private void setupMotionViews() {
        int n = getChildCount();

        mModel.build();
        mInTransition = true;
        SparseArray<MotionController> controllers = new SparseArray<>();
        for (int i = 0; i < n; i++) {
            View child = getChildAt(i);
            controllers.put(child.getId(), mFrameArrayList.get(child));
        }
        int layoutWidth = getWidth();
        int layoutHeight = getHeight();
        int arc = mScene.gatPathMotionArc();
        if (arc != UNSET) {
            for (int i = 0; i < n; i++) {
                MotionController motionController = mFrameArrayList.get(getChildAt(i));
                if (motionController != null) {
                    motionController.setPathMotionArc(arc);
                }
            }
        }

        SparseBooleanArray sparseBooleanArray = new SparseBooleanArray();
        int[] depends = new int[mFrameArrayList.size()];
        int count = 0;
        for (int i = 0; i < n; i++) {
            View view = getChildAt(i);
            MotionController motionController = mFrameArrayList.get(view);
            if (motionController.getAnimateRelativeTo() != UNSET) {
                sparseBooleanArray.put(motionController.getAnimateRelativeTo(), true);
                depends[count++] = motionController.getAnimateRelativeTo();
            }
        }
        if (mDecoratorsHelpers != null) {
            for (int i = 0; i < count; i++) {
                MotionController motionController = mFrameArrayList.get(findViewById(depends[i]));
                if (motionController == null) {
                    continue;
                }
                mScene.getKeyFrames(motionController);
            }
            // Allow helpers to access all the motionControllers after
            for (MotionHelper mDecoratorsHelper : mDecoratorsHelpers) {
                mDecoratorsHelper.onPreSetup(this, mFrameArrayList);
            }
            for (int i = 0; i < count; i++) {
                MotionController motionController = mFrameArrayList.get(findViewById(depends[i]));
                if (motionController == null) {
                    continue;
                }
                motionController.setup(layoutWidth, layoutHeight,
                        mTransitionDuration, getNanoTime());
            }
        } else {
            for (int i = 0; i < count; i++) {
                MotionController motionController = mFrameArrayList.get(findViewById(depends[i]));
                if (motionController == null) {
                    continue;
                }
                mScene.getKeyFrames(motionController);
                motionController.setup(layoutWidth, layoutHeight,
                        mTransitionDuration, getNanoTime());
            }
        }
        // getMap the KeyFrames for each view
        for (int i = 0; i < n; i++) {
            View v = getChildAt(i);
            MotionController motionController = mFrameArrayList.get(v);
            if (sparseBooleanArray.get(v.getId())) {
                continue;
            }

            if (motionController != null) {
                mScene.getKeyFrames(motionController);
                motionController.setup(layoutWidth, layoutHeight,
                        mTransitionDuration, getNanoTime());
            }
        }

        float stagger = mScene.getStaggered();
        if (stagger != 0.0f) {
            boolean flip = stagger < 0.0;
            boolean useMotionStagger = false;
            stagger = Math.abs(stagger);
            float min = Float.MAX_VALUE, max = -Float.MAX_VALUE;
            for (int i = 0; i < n; i++) {
                MotionController f = mFrameArrayList.get(getChildAt(i));
                if (!Float.isNaN(f.mMotionStagger)) {
                    useMotionStagger = true;
                    break;
                }
                float x = f.getFinalX();
                float y = f.getFinalY();
                float mdist = flip ? (y - x) : (y + x);
                min = Math.min(min, mdist);
                max = Math.max(max, mdist);
            }
            if (useMotionStagger) {
                min = Float.MAX_VALUE;
                max = -Float.MAX_VALUE;

                for (int i = 0; i < n; i++) {
                    MotionController f = mFrameArrayList.get(getChildAt(i));
                    if (!Float.isNaN(f.mMotionStagger)) {
                        min = Math.min(min, f.mMotionStagger);
                        max = Math.max(max, f.mMotionStagger);
                    }
                }
                for (int i = 0; i < n; i++) {
                    MotionController f = mFrameArrayList.get(getChildAt(i));
                    if (!Float.isNaN(f.mMotionStagger)) {

                        f.mStaggerScale = 1 / (1 - stagger);
                        if (flip) {
                            f.mStaggerOffset = stagger - stagger
                                    * ((max - f.mMotionStagger) / (max - min));
                        } else {
                            f.mStaggerOffset = stagger - stagger
                                    * (f.mMotionStagger - min) / (max - min);
                        }
                    }
                }
            } else {
                for (int i = 0; i < n; i++) {
                    MotionController f = mFrameArrayList.get(getChildAt(i));
                    float x = f.getFinalX();
                    float y = f.getFinalY();
                    float mdist = flip ? (y - x) : (y + x);
                    f.mStaggerScale = 1 / (1 - stagger);
                    f.mStaggerOffset = stagger - stagger * (mdist - min) / (max - min);
                }
            }
        }
    }

    /**
     * @param touchUpMode     behavior on touch up, can be either:
     *                        <ul>
     *                        <li>TOUCH_UP_COMPLETE (default) : will complete the transition,
     *                        picking up
     *                        automatically a correct velocity to do so</li>
     *                        <li>TOUCH_UP_STOP : will allow stopping mid-transition</li>
     *                        <li>TOUCH_UP_DECELERATE : will slowly decay,
     *                        possibly past the transition (i.e.
     *                        it will do a hard stop if unmanaged)</li>
     *                        <li>TOUCH_UP_DECELERATE_AND_COMPLETE :
     *                        will automatically pick between
     *                        TOUCH_UP_COMPLETE and TOUCH_UP_DECELERATE</li>
     *                        </ul>,
     *                        TOUCH_UP_STOP (will allow stopping
     * @param position        animate to given position
     * @param currentVelocity
     */
    public void touchAnimateTo(int touchUpMode, float position, float currentVelocity) {
        if (DEBUG) {
            Log.v(TAG, " " + Debug.getLocation() + " touchAnimateTo "
                    + position + "   " + currentVelocity);
        }
        if (mScene == null) {
            return;
        }
        if (mTransitionLastPosition == position) {
            return;
        }

        mTemporalInterpolator = true;
        mAnimationStartTime = getNanoTime();
        mTransitionDuration = mScene.getDuration() / 1000f;

        mTransitionGoalPosition = position;
        mInTransition = true;

        switch (touchUpMode) {
            case TOUCH_UP_COMPLETE:
            case TOUCH_UP_NEVER_TO_START:
            case TOUCH_UP_NEVER_TO_END:
            case TOUCH_UP_COMPLETE_TO_START:
            case TOUCH_UP_COMPLETE_TO_END: {
                if (touchUpMode == TOUCH_UP_COMPLETE_TO_START
                        || touchUpMode == TOUCH_UP_NEVER_TO_END) {
                    position = 0;
                } else if (touchUpMode == TOUCH_UP_COMPLETE_TO_END
                        || touchUpMode == TOUCH_UP_NEVER_TO_START) {
                    position = 1;
                }

                if (mScene.getAutoCompleteMode()
                        == TouchResponse.COMPLETE_MODE_CONTINUOUS_VELOCITY) {
                    mStopLogic.config(mTransitionLastPosition, position, currentVelocity,
                            mTransitionDuration, mScene.getMaxAcceleration(),
                            mScene.getMaxVelocity());
                } else {
                    mStopLogic.springConfig(mTransitionLastPosition, position, currentVelocity,
                            mScene.getSpringMass(),
                            mScene.getSpringStiffiness(),
                            mScene.getSpringDamping(),
                            mScene.getSpringStopThreshold(), mScene.getSpringBoundary());
                }

                int currentState = mCurrentState; // TODO: remove setProgress(), temporary fix
                mTransitionGoalPosition = position;
                mCurrentState = currentState;
                mInterpolator = mStopLogic;
            }
                break;
            case TOUCH_UP_STOP: {
                // nothing to do
            }
            break;
            case TOUCH_UP_DECELERATE: {
                mDecelerateLogic.config(currentVelocity, mTransitionLastPosition,
                        mScene.getMaxAcceleration());
                mInterpolator = mDecelerateLogic;
            }
            break;
            case TOUCH_UP_DECELERATE_AND_COMPLETE: {
                if (willJump(currentVelocity, mTransitionLastPosition,
                        mScene.getMaxAcceleration())) {
                    mDecelerateLogic.config(currentVelocity,
                            mTransitionLastPosition, mScene.getMaxAcceleration());
                    mInterpolator = mDecelerateLogic;
                } else {
                    mStopLogic.config(mTransitionLastPosition, position, currentVelocity,
                            mTransitionDuration,
                            mScene.getMaxAcceleration(), mScene.getMaxVelocity());
                    mLastVelocity = 0;
                    int currentState = mCurrentState; // TODO: remove setProgress(), (temporary fix)
                    mTransitionGoalPosition = position;
                    mCurrentState = currentState;
                    mInterpolator = mStopLogic;
                }
            }
            break;
        }

        mTransitionInstantly = false;
        mAnimationStartTime = getNanoTime();
        invalidate();
    }

    /**
     * Allows you to use trigger spring motion touch behaviour.
     * You must have configured all the spring parameters in the Transition's OnSwipe
     *
     * @param position the position 0 - 1
     * @param currentVelocity the current velocity rate of change in position per second
     */
    public void touchSpringTo(float position, float currentVelocity) {
        if (DEBUG) {
            Log.v(TAG, " " + Debug.getLocation()
                    + " touchAnimateTo " + position + "   " + currentVelocity);
        }
        if (mScene == null) {
            return;
        }
        if (mTransitionLastPosition == position) {
            return;
        }

        mTemporalInterpolator = true;
        mAnimationStartTime = getNanoTime();
        mTransitionDuration = mScene.getDuration() / 1000f;

        mTransitionGoalPosition = position;
        mInTransition = true;

        mStopLogic.springConfig(mTransitionLastPosition, position, currentVelocity,
                mScene.getSpringMass(), mScene.getSpringStiffiness(), mScene.getSpringDamping(),
                mScene.getSpringStopThreshold(), mScene.getSpringBoundary());

        int currentState = mCurrentState; // TODO: remove setProgress(), this is a temporary fix
        mTransitionGoalPosition = position;
        mCurrentState = currentState;
        mInterpolator = mStopLogic;


        mTransitionInstantly = false;
        mAnimationStartTime = getNanoTime();
        invalidate();
    }

    private static boolean willJump(float velocity,
                                    float position,
                                    float maxAcceleration) {
        if (velocity > 0) {
            float time = velocity / maxAcceleration;
            float pos = velocity * time - (maxAcceleration * time * time) / 2;
            return (position + pos > 1);
        } else {
            float time = -velocity / maxAcceleration;
            float pos = velocity * time + (maxAcceleration * time * time) / 2;
            return (position + pos < 0);
        }
    }

    /**
     * Basic deceleration interpolator
     */
    class DecelerateInterpolator extends MotionInterpolator {
        float mInitialV = 0;
        float mCurrentP = 0;
        float mMaxA;

        public void config(float velocity, float position, float maxAcceleration) {
            mInitialV = velocity;
            mCurrentP = position;
            mMaxA = maxAcceleration;
        }

        @Override
        public float getInterpolation(float time) {
            if (mInitialV > 0) {
                if (mInitialV / mMaxA < time) {
                    time = mInitialV / mMaxA;
                }
                mLastVelocity = mInitialV - mMaxA * time;
                float pos = mInitialV * time - (mMaxA * time * time) / 2;
                return pos + mCurrentP;
            } else {

                if (-mInitialV / mMaxA < time) {
                    time = -mInitialV / mMaxA;
                }
                mLastVelocity = mInitialV + mMaxA * time;
                float pos = mInitialV * time + (mMaxA * time * time) / 2;
                return pos + mCurrentP;
            }
        }

        @Override
        public float getVelocity() {
            return mLastVelocity;
        }
    }

    /**
     * @param position animate to given position
     */
    void animateTo(float position) {
        if (DEBUG) {
            Log.v(TAG, " " + Debug.getLocation() + " ... animateTo(" + position
                    + ") last:" + mTransitionLastPosition);
        }
        if (mScene == null) {
            return;
        }

        if (mTransitionLastPosition != mTransitionPosition && mTransitionInstantly) {
            // if we had a call from setProgress() but evaluate() didn't run,
            // the mTransitionLastPosition might not have been updated
            mTransitionLastPosition = mTransitionPosition;
        }

        if (mTransitionLastPosition == position) {
            return;
        }
        mTemporalInterpolator = false;
        float currentPosition = mTransitionLastPosition;
        mTransitionGoalPosition = position;
        mTransitionDuration = mScene.getDuration() / 1000f;
        setProgress(mTransitionGoalPosition);
        mInterpolator = null;
        mProgressInterpolator = mScene.getInterpolator();
        mTransitionInstantly = false;
        mAnimationStartTime = getNanoTime();
        mInTransition = true;
        mTransitionPosition = currentPosition;
        if (DEBUG) {
            Log.v(TAG, Debug.getLocation() + " mTransitionLastPosition = "
                    + mTransitionLastPosition + " currentPosition =" + currentPosition);
        }
        mTransitionLastPosition = currentPosition;
        invalidate();
    }

    private void computeCurrentPositions() {
        final int n = getChildCount();
        for (int i = 0; i < n; i++) {
            View v = getChildAt(i);
            MotionController frame = mFrameArrayList.get(v);
            if (frame == null) {
                continue;
            }
            frame.setStartCurrentState(v);
        }
    }

    /**
     * Animate to the starting position of the current transition.
     * This will not work during on create as there is no transition
     * Transitions are only set up during onAttach
     */
    public void transitionToStart() {
        animateTo(0.0f);
    }

    /**
     * Animate to the starting position of the current transition.
     * This will not work during on create as there is no transition
     * Transitions are only set up during onAttach
     *
     * @param onComplete callback when task is done
     */
    public void transitionToStart(Runnable onComplete) {
        animateTo(0.0f);
        mOnComplete = onComplete;
    }

    /**
     * Animate to the ending position of the current transition.
     * This will not work during on create as there is no transition
     * Transitions are only set up during onAttach
     */
    public void transitionToEnd() {
        animateTo(1.0f);
        mOnComplete = null;
    }

    /**
     * Animate to the ending position of the current transition.
     * This will not work during on create as there is no transition
     * Transitions are only set up during onAttach
     *
     * @param onComplete callback when task is done
     */
    public void transitionToEnd(Runnable onComplete) {
        animateTo(1.0f);
        mOnComplete = onComplete;
    }

    /**
     * Animate to the state defined by the id.
     * The id is the id of the ConstraintSet or the id of the State.
     *
     * @param id the state to transition to
     */
    public void transitionToState(int id) {
        if (!isAttachedToWindow()) {
            if (mStateCache == null) {
                mStateCache = new StateCache();
            }
            mStateCache.setEndState(id);
            return;
        }
        transitionToState(id, -1, -1);
    }

    /**
     * Animate to the state defined by the id.
     * The id is the id of the ConstraintSet or the id of the State.
     *
     * @param id       the state to transition to
     * @param duration time in ms. if 0 set by default or transition -1 by current
     */

    public void transitionToState(int id, int duration) {
        if (!isAttachedToWindow()) {
            if (mStateCache == null) {
                mStateCache = new StateCache();
            }
            mStateCache.setEndState(id);
            return;
        }
        transitionToState(id, -1, -1, duration);
    }

    /**
     * Animate to the state defined by the id.
     * Width and height may be used in the picking of the id using this StateSet.
     *
     * @param id           the state to transition
     * @param screenWidth  the with of the motionLayout used to select the variant
     * @param screenHeight the height of the motionLayout used to select the variant
     */
    public void transitionToState(int id, int screenWidth, int screenHeight) {
        transitionToState(id, screenWidth, screenHeight, -1);
    }

    /**
     * Rotate the layout based on the angle to a ConstraintSet
     * @param id constraintSet
     * @param duration time to take to rotate
     */
    public void rotateTo(int id, int duration) {
        mInRotation = true;
        mPreRotateWidth = getWidth();
        mPreRotateHeight = getHeight();

        int currentRotation = getDisplay().getRotation();
        mRotatMode = (((currentRotation + 1) % 4) > ((mPreviouseRotation + 1) % 4)) ? 1 : 2;

        mPreviouseRotation = currentRotation;
        final int n = getChildCount();
        for (int i = 0; i < n; i++) {
            View v = getChildAt(i);
            ViewState bounds = mPreRotate.get(v);
            if (bounds == null) {
                bounds = new ViewState();
                mPreRotate.put(v, bounds);
            }
            bounds.getState(v);
        }

        mBeginState = -1;
        mEndState = id;
        mScene.setTransition(-1, mEndState);
        mModel.initFrom(mLayoutWidget, null, mScene.getConstraintSet(mEndState));
        mTransitionPosition = 0;

        mTransitionLastPosition = 0;
        invalidate();
        transitionToEnd(new Runnable() {
            @Override
            public void run() {
                mInRotation = false;
            }
        });
        if (duration > 0) {
            mTransitionDuration = duration / 1000f;
        }

    }

    public boolean isInRotation() {
        return mInRotation;
    }

    /**
     * This jumps to a state
     * It will be at that state after one repaint cycle
     * If the current transition contains that state.
     * It setsProgress 0 or 1 to that state.
     * If not in the current transition itsl
     *
     * @param id state to set
     */
    public void jumpToState(int id) {
        if (!isAttachedToWindow()) {
            mCurrentState = id;
        }
        if (mBeginState == id) {
            setProgress(0);
        } else if (mEndState == id) {
            setProgress(1);
        } else {
            setTransition(id, id);
        }
    }

    /**
     * Animate to the state defined by the id.
     * Width and height may be used in the picking of the id using this StateSet.
     *
     * @param id           the state to transition
     * @param screenWidth  the with of the motionLayout used to select the variant
     * @param screenHeight the height of the motionLayout used to select the variant
     * @param duration     time in ms. if 0 set by default or transition -1 by current
     */

    public void transitionToState(int id, int screenWidth, int screenHeight, int duration) {
        // if id is either end or start state, transition using current setup.
        // if id is not part of end/start, need to setup

        // if id == end state, just animate
        // ... but check if currentState is unknown. if unknown, call computeCurrentPosition
        // if id != end state
        if (DEBUG && mScene.mStateSet == null) {
            Log.v(TAG, Debug.getLocation() + " mStateSet = null");
        }
        if (mScene != null && mScene.mStateSet != null) {
            int tmp_id = mScene.mStateSet.convertToConstraintSet(mCurrentState,
                    id, screenWidth, screenHeight);

            if (tmp_id != -1) {
                if (DEBUG) {
                    Log.v(TAG, " got state  " + Debug.getLocation() + " lookup("
                            + Debug.getName(getContext(), id)
                            + screenWidth + " , " + screenHeight + " ) =  "
                            + Debug.getName(getContext(), tmp_id));
                }
                id = tmp_id;
            }
        }
        if (mCurrentState == id) {
            return;
        }
        if (mBeginState == id) {
            animateTo(0.0f);
            if (duration > 0) {
                mTransitionDuration = duration / 1000f;
            }
            return;
        }
        if (mEndState == id) {
            animateTo(1.0f);
            if (duration > 0) {
                mTransitionDuration = duration / 1000f;
            }
            return;
        }
        mEndState = id;
        if (mCurrentState != UNSET) {
            if (DEBUG) {
                Log.v(TAG, " transitionToState " + Debug.getLocation() + " current  = "
                        + Debug.getName(getContext(), mCurrentState)
                        + " to " + Debug.getName(getContext(), mEndState));
                Debug.logStack(TAG, " transitionToState  ", 4);
                Log.v(TAG, "-------------------------------------------");
            }
            setTransition(mCurrentState, id);

            animateTo(1.0f);

            mTransitionLastPosition = 0;
            transitionToEnd();
            if (duration > 0) {
                mTransitionDuration = duration / 1000f;
            }
            return;
        }
        if (DEBUG) {
            Log.v(TAG, "setTransition  unknown -> "
                    + Debug.getName(getContext(), id));
        }

        // TODO correctly use width & height
        mTemporalInterpolator = false;
        mTransitionGoalPosition = 1;
        mTransitionPosition = 0;
        mTransitionLastPosition = 0;
        mTransitionLastTime = getNanoTime();
        mAnimationStartTime = getNanoTime();
        mTransitionInstantly = false;
        mInterpolator = null;
        if (duration == -1) {
            mTransitionDuration = mScene.getDuration() / 1000f;
        }
        mBeginState = UNSET;
        mScene.setTransition(mBeginState, mEndState);
        SparseArray<MotionController> controllers = new SparseArray<>();
        if (duration == 0) {
            mTransitionDuration = mScene.getDuration() / 1000f;
        } else if (duration > 0) {
            mTransitionDuration = duration / 1000f;
        }

        int n = getChildCount();

        mFrameArrayList.clear();
        for (int i = 0; i < n; i++) {
            View v = getChildAt(i);
            MotionController f = new MotionController(v);
            mFrameArrayList.put(v, f);
            controllers.put(v.getId(), mFrameArrayList.get(v));
        }
        mInTransition = true;

        mModel.initFrom(mLayoutWidget, null, mScene.getConstraintSet(id));
        rebuildScene();
        mModel.build();
        computeCurrentPositions();
        int layoutWidth = getWidth();
        int layoutHeight = getHeight();
        // getMap the KeyFrames for each view

        if (mDecoratorsHelpers != null) {
            for (int i = 0; i < n; i++) {
                MotionController motionController = mFrameArrayList.get(getChildAt(i));
                if (motionController == null) {
                    continue;
                }
                mScene.getKeyFrames(motionController);
            }
            // Allow helpers to access all the motionControllers after
            for (MotionHelper mDecoratorsHelper : mDecoratorsHelpers) {
                mDecoratorsHelper.onPreSetup(this, mFrameArrayList);
            }
            for (int i = 0; i < n; i++) {
                MotionController motionController = mFrameArrayList.get(getChildAt(i));
                if (motionController == null) {
                    continue;
                }
                motionController.setup(layoutWidth, layoutHeight,
                        mTransitionDuration, getNanoTime());
            }
        } else {
            for (int i = 0; i < n; i++) {
                MotionController motionController = mFrameArrayList.get(getChildAt(i));
                if (motionController == null) {
                    continue;
                }
                mScene.getKeyFrames(motionController);
                motionController.setup(layoutWidth, layoutHeight,
                        mTransitionDuration, getNanoTime());
            }
        }

        float stagger = mScene.getStaggered();
        if (stagger != 0.0f) {
            float min = Float.MAX_VALUE, max = -Float.MAX_VALUE;
            for (int i = 0; i < n; i++) {
                MotionController f = mFrameArrayList.get(getChildAt(i));
                float x = f.getFinalX();
                float y = f.getFinalY();
                min = Math.min(min, y + x);
                max = Math.max(max, y + x);
            }

            for (int i = 0; i < n; i++) {
                MotionController f = mFrameArrayList.get(getChildAt(i));
                float x = f.getFinalX();
                float y = f.getFinalY();
                f.mStaggerScale = 1 / (1 - stagger);
                f.mStaggerOffset = stagger - stagger * (x + y - min) / (max - min);
            }
        }

        mTransitionPosition = 0;
        mTransitionLastPosition = 0;
        mInTransition = true;

        invalidate();
    }

    /**
     * Returns the last velocity used in the transition
     *
     * @return
     */
    public float getVelocity() {
        return mLastVelocity;
    }

    /**
     * Returns the last layout velocity used in the transition
     *
     * @param view           The view
     * @param posOnViewX     The x position on the view
     * @param posOnViewY     The y position on the view
     * @param returnVelocity The velocity
     * @param type           Velocity returned 0 = post layout, 1 = layout, 2 = static postlayout
     */
    public void getViewVelocity(View view,
                                float posOnViewX,
                                float posOnViewY,
                                float[] returnVelocity,
                                int type) {
        float v = mLastVelocity;
        float position = mTransitionLastPosition;
        if (mInterpolator != null) {
            float deltaT = EPSILON;
            float dir = Math.signum(mTransitionGoalPosition - mTransitionLastPosition);
            float interpolatedPosition =
                    mInterpolator.getInterpolation(mTransitionLastPosition + deltaT);
            position = mInterpolator.getInterpolation(mTransitionLastPosition);
            interpolatedPosition -= position;
            interpolatedPosition /= deltaT;
            v = dir * interpolatedPosition / mTransitionDuration;
        }

        if (mInterpolator instanceof MotionInterpolator) {
            v = ((MotionInterpolator) mInterpolator).getVelocity();

        }

        MotionController f = mFrameArrayList.get(view);
        if ((type & 1) == 0) {
            f.getPostLayoutDvDp(position,
                    view.getWidth(), view.getHeight(),
                    posOnViewX, posOnViewY, returnVelocity);
        } else {
            f.getDpDt(position, posOnViewX, posOnViewY, returnVelocity);
        }
        if (type < VELOCITY_STATIC_POST_LAYOUT) {
            returnVelocity[0] *= v;
            returnVelocity[1] *= v;
        }

    }

    ////////////////////////////////////////////////////////////////////////////////
    // This contains the logic for interacting with the ConstraintLayout Solver
    class Model {
        ConstraintWidgetContainer mLayoutStart = new ConstraintWidgetContainer();
        ConstraintWidgetContainer mLayoutEnd = new ConstraintWidgetContainer();
        ConstraintSet mStart = null;
        ConstraintSet mEnd = null;
        int mStartId;
        int mEndId;

        void copy(ConstraintWidgetContainer src, ConstraintWidgetContainer dest) {
            ArrayList<ConstraintWidget> children = src.getChildren();
            HashMap<ConstraintWidget, ConstraintWidget> map = new HashMap<>();
            map.put(src, dest);
            dest.getChildren().clear();
            dest.copy(src, map);
            for (ConstraintWidget child_s : children) {
                ConstraintWidget child_d;
                if (child_s instanceof androidx.constraintlayout.core.widgets.Barrier) {
                    child_d = new androidx.constraintlayout.core.widgets.Barrier();
                } else if (child_s instanceof androidx.constraintlayout.core.widgets.Guideline) {
                    child_d = new androidx.constraintlayout.core.widgets.Guideline();
                } else if (child_s instanceof Flow) {
                    child_d = new Flow();
                } else if (child_s instanceof Placeholder) {
                    child_d = new Placeholder();
                } else if (child_s instanceof androidx.constraintlayout.core.widgets.Helper) {
                    child_d = new androidx.constraintlayout.core.widgets.HelperWidget();
                } else {
                    child_d = new ConstraintWidget();
                }
                dest.add(child_d);
                map.put(child_s, child_d);
            }
            for (ConstraintWidget child_s : children) {
                map.get(child_s).copy(child_s, map);
            }
        }

        void initFrom(ConstraintWidgetContainer baseLayout,
                      ConstraintSet start,
                      ConstraintSet end) {
            mStart = start;
            mEnd = end;
            mLayoutStart =  new ConstraintWidgetContainer();
            mLayoutEnd =  new ConstraintWidgetContainer();
            mLayoutStart.setMeasurer(mLayoutWidget.getMeasurer());
            mLayoutEnd.setMeasurer(mLayoutWidget.getMeasurer());
            mLayoutStart.removeAllChildren();
            mLayoutEnd.removeAllChildren();
            copy(mLayoutWidget, mLayoutStart);
            copy(mLayoutWidget, mLayoutEnd);
            if (mTransitionLastPosition > 0.5) {
                if (start != null) {
                    setupConstraintWidget(mLayoutStart, start);
                }
                setupConstraintWidget(mLayoutEnd, end);
            } else {
                setupConstraintWidget(mLayoutEnd, end);
                if (start != null) {
                    setupConstraintWidget(mLayoutStart, start);
                }
            }
            // then init the engine...
            if (DEBUG) {
                Log.v(TAG, "> mLayoutStart.updateHierarchy " + Debug.getLocation());
            }
            mLayoutStart.setRtl(isRtl());
            mLayoutStart.updateHierarchy();

            if (DEBUG) {
                for (ConstraintWidget child : mLayoutStart.getChildren()) {
                    View view = (View) child.getCompanionWidget();
                    debugWidget(">>>>>>>  " + Debug.getName(view), child);
                }
                Log.v(TAG, "> mLayoutEnd.updateHierarchy " + Debug.getLocation());
                Log.v(TAG, "> mLayoutEnd.updateHierarchy  "
                        + Debug.getLocation() + "  == isRtl()=" + isRtl());
            }
            mLayoutEnd.setRtl(isRtl());
            mLayoutEnd.updateHierarchy();

            if (DEBUG) {
                for (ConstraintWidget child : mLayoutEnd.getChildren()) {
                    View view = (View) child.getCompanionWidget();
                    debugWidget(">>>>>>>  " + Debug.getName(view), child);
                }
            }
            ViewGroup.LayoutParams layoutParams = getLayoutParams();
            if (layoutParams != null) {
                if (layoutParams.width == WRAP_CONTENT) {
                    mLayoutStart.setHorizontalDimensionBehaviour(
                            ConstraintWidget.DimensionBehaviour.WRAP_CONTENT);
                    mLayoutEnd.setHorizontalDimensionBehaviour(
                            ConstraintWidget.DimensionBehaviour.WRAP_CONTENT);
                }
                if (layoutParams.height == WRAP_CONTENT) {
                    mLayoutStart.setVerticalDimensionBehaviour(
                            ConstraintWidget.DimensionBehaviour.WRAP_CONTENT);
                    mLayoutEnd.setVerticalDimensionBehaviour(
                            ConstraintWidget.DimensionBehaviour.WRAP_CONTENT);
                }
            }
        }

        private void setupConstraintWidget(ConstraintWidgetContainer base, ConstraintSet cSet) {
            SparseArray<ConstraintWidget> mapIdToWidget = new SparseArray<>();
            Constraints.LayoutParams layoutParams =
                    new Constraints.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT);

            mapIdToWidget.clear();
            mapIdToWidget.put(PARENT_ID, base);
            mapIdToWidget.put(getId(), base);
            if (cSet != null && cSet.mRotate != 0) {
                resolveSystem(mLayoutEnd, getOptimizationLevel(),
                        MeasureSpec.makeMeasureSpec(getHeight(), MeasureSpec.EXACTLY),
                        MeasureSpec.makeMeasureSpec(getWidth(), MeasureSpec.EXACTLY));
            }
            //  build id widget map
            for (ConstraintWidget child : base.getChildren()) {
                child.setAnimated(true);
                View view = (View) child.getCompanionWidget();
                mapIdToWidget.put(view.getId(), child);
            }

            for (ConstraintWidget child : base.getChildren()) {
                View view = (View) child.getCompanionWidget();
                cSet.applyToLayoutParams(view.getId(), layoutParams);

                child.setWidth(cSet.getWidth(view.getId()));
                child.setHeight(cSet.getHeight(view.getId()));
                if (view instanceof ConstraintHelper) {
                    cSet.applyToHelper((ConstraintHelper) view, child, layoutParams, mapIdToWidget);
                    if (view instanceof Barrier) {
                        ((Barrier) view).validateParams();
                        if (DEBUG) {
                            Log.v(TAG, ">>>>>>>>>> Barrier " + Debug.getName(getContext(),
                                    ((Barrier) view).getReferencedIds()));
                        }
                    }
                }
                if (DEBUG) {
                    debugLayoutParam(">>>>>>>  " + Debug.getName(view), layoutParams);
                }
                layoutParams.resolveLayoutDirection(getLayoutDirection());
                applyConstraintsFromLayoutParams(false, view, child, layoutParams, mapIdToWidget);
                if (cSet.getVisibilityMode(view.getId()) == ConstraintSet.VISIBILITY_MODE_IGNORE) {
                    child.setVisibility(view.getVisibility());
                } else {
                    child.setVisibility(cSet.getVisibility(view.getId()));
                }
            }
            for (ConstraintWidget child : base.getChildren()) {
                if (child instanceof androidx.constraintlayout.core.widgets.VirtualLayout) {
                    ConstraintHelper view = (ConstraintHelper) child.getCompanionWidget();
                    Helper helper = (Helper) child;
                    view.updatePreLayout(base, helper, mapIdToWidget);
                    androidx.constraintlayout.core.widgets.VirtualLayout virtualLayout =
                            (androidx.constraintlayout.core.widgets.VirtualLayout) helper;
                    virtualLayout.captureWidgets();
                }
            }
        }

        ConstraintWidget getWidget(ConstraintWidgetContainer container, View view) {
            if (container.getCompanionWidget() == view) {
                return container;
            }
            ArrayList<ConstraintWidget> children = container.getChildren();
            final int count = children.size();
            for (int i = 0; i < count; i++) {
                ConstraintWidget widget = children.get(i);
                if (widget.getCompanionWidget() == view) {
                    return widget;
                }

            }
            return null;
        }

        @SuppressLint("LogConditional")
        private void debugLayoutParam(String str, LayoutParams params) {
            String a = " ";
            a += params.startToStart != UNSET ? "SS" : "__";
            a += params.startToEnd != UNSET ? "|SE" : "|__";
            a += params.endToStart != UNSET ? "|ES" : "|__";
            a += params.endToEnd != UNSET ? "|EE" : "|__";
            a += params.leftToLeft != UNSET ? "|LL" : "|__";
            a += params.leftToRight != UNSET ? "|LR" : "|__";
            a += params.rightToLeft != UNSET ? "|RL" : "|__";
            a += params.rightToRight != UNSET ? "|RR" : "|__";
            a += params.topToTop != UNSET ? "|TT" : "|__";
            a += params.topToBottom != UNSET ? "|TB" : "|__";
            a += params.bottomToTop != UNSET ? "|BT" : "|__";
            a += params.bottomToBottom != UNSET ? "|BB" : "|__";
            Log.v(TAG, str + a);
        }

        @SuppressLint("LogConditional")
        private void debugWidget(String str, ConstraintWidget child) {
            String a = " ";
            a += child.mTop.mTarget != null
                    ? ("T" + (child.mTop.mTarget.mType == ConstraintAnchor.Type.TOP ? "T" : "B"))
                    : "__";
            a += child.mBottom.mTarget != null
                    ? ("B" + (child.mBottom.mTarget.mType == ConstraintAnchor.Type.TOP ? "T" : "B"))
                    : "__";
            a += child.mLeft.mTarget != null
                    ? ("L" + (child.mLeft.mTarget.mType == ConstraintAnchor.Type.LEFT ? "L" : "R"))
                    : "__";
            a += child.mRight.mTarget != null
                    ? ("R" + (child.mRight.mTarget.mType == ConstraintAnchor.Type.LEFT ? "L" : "R"))
                    : "__";
            Log.v(TAG, str + a + " ---  " + child);
        }

        @SuppressLint("LogConditional")
        private void debugLayout(String title, ConstraintWidgetContainer c) {
            View v = (View) c.getCompanionWidget();
            String cName = title + " " + Debug.getName(v);
            Log.v(TAG, cName + "  ========= " + c);
            int count = c.getChildren().size();
            for (int i = 0; i < count; i++) {
                String str = cName + "[" + i + "] ";
                ConstraintWidget child = c.getChildren().get(i);
                String a = "";
                a += child.mTop.mTarget != null ? "T" : "_";
                a += child.mBottom.mTarget != null ? "B" : "_";
                a += child.mLeft.mTarget != null ? "L" : "_";
                a += child.mRight.mTarget != null ? "R" : "_";
                v = (View) child.getCompanionWidget();
                String name = Debug.getName(v);
                if (v instanceof TextView) {
                    name += "(" + ((TextView) v).getText() + ")";
                }
                Log.v(TAG, str + "  " + name + " " + child + " " + a);
            }
            Log.v(TAG, cName + " done. ");
        }

        public void reEvaluateState() {
            measure(mLastWidthMeasureSpec, mLastHeightMeasureSpec);
            setupMotionViews();
        }

        public void measure(int widthMeasureSpec, int heightMeasureSpec) {
            int widthMode = MeasureSpec.getMode(widthMeasureSpec);
            int heightMode = MeasureSpec.getMode(heightMeasureSpec);

            mWidthMeasureMode = widthMode;
            mHeightMeasureMode = heightMode;

            computeStartEndSize(widthMeasureSpec, heightMeasureSpec);

            // This works around the problem that MotionLayout calls its children
            // Wrap content children
            // with measure(AT_MOST,AT_MOST) then measure(EXACTLY, EXACTLY)
            // if a child of MotionLayout is a motionLayout
            // it would not know it could resize during animation
            // other Layouts may have this behaviour but for now this is the only one we support

            boolean recompute_start_end_size = true;
            if (getParent() instanceof MotionLayout
                    && widthMode == MeasureSpec.EXACTLY
                    && heightMode == MeasureSpec.EXACTLY) {
                recompute_start_end_size = false;
            }
            if (recompute_start_end_size) {
                computeStartEndSize(widthMeasureSpec, heightMeasureSpec);

                mStartWrapWidth = mLayoutStart.getWidth();
                mStartWrapHeight = mLayoutStart.getHeight();
                mEndWrapWidth = mLayoutEnd.getWidth();
                mEndWrapHeight = mLayoutEnd.getHeight();
                mMeasureDuringTransition = ((mStartWrapWidth != mEndWrapWidth)
                        || (mStartWrapHeight != mEndWrapHeight));
            }

            int width = mStartWrapWidth;
            int height = mStartWrapHeight;
            if (mWidthMeasureMode == MeasureSpec.AT_MOST
                    || mWidthMeasureMode == MeasureSpec.UNSPECIFIED) {
                width = (int) (mStartWrapWidth + mPostInterpolationPosition
                        * (mEndWrapWidth - mStartWrapWidth));
            }
            if (mHeightMeasureMode == MeasureSpec.AT_MOST
                    || mHeightMeasureMode == MeasureSpec.UNSPECIFIED) {
                height = (int) (mStartWrapHeight + mPostInterpolationPosition
                        * (mEndWrapHeight - mStartWrapHeight));
            }

            boolean isWidthMeasuredTooSmall = mLayoutStart.isWidthMeasuredTooSmall()
                    || mLayoutEnd.isWidthMeasuredTooSmall();
            boolean isHeightMeasuredTooSmall = mLayoutStart.isHeightMeasuredTooSmall()
                    || mLayoutEnd.isHeightMeasuredTooSmall();
            resolveMeasuredDimension(widthMeasureSpec, heightMeasureSpec,
                    width, height, isWidthMeasuredTooSmall, isHeightMeasuredTooSmall);

            if (DEBUG) {
                Debug.logStack(TAG, ">>>>>>>>", 3);
                debugLayout(">>>>>>> measure str ", mLayoutStart);
                debugLayout(">>>>>>> measure end ", mLayoutEnd);
            }
        }

        private void computeStartEndSize(int widthMeasureSpec, int heightMeasureSpec) {
            int optimisationLevel = getOptimizationLevel();

            if (mCurrentState == getStartState()) {
                resolveSystem(mLayoutEnd, optimisationLevel,
                        (mEnd == null || mEnd.mRotate == 0) ? widthMeasureSpec : heightMeasureSpec,
                        (mEnd == null || mEnd.mRotate == 0) ? heightMeasureSpec : widthMeasureSpec);
                if (mStart != null) {
                    resolveSystem(mLayoutStart, optimisationLevel,
                            (mStart.mRotate == 0) ? widthMeasureSpec : heightMeasureSpec,
                            (mStart.mRotate == 0) ? heightMeasureSpec : widthMeasureSpec);
                }
            } else {
                if (mStart != null) {
                    resolveSystem(mLayoutStart, optimisationLevel,
                            (mStart.mRotate == 0) ? widthMeasureSpec : heightMeasureSpec,
                            (mStart.mRotate == 0) ? heightMeasureSpec : widthMeasureSpec);
                }
                resolveSystem(mLayoutEnd, optimisationLevel,
                        (mEnd == null || mEnd.mRotate == 0) ? widthMeasureSpec : heightMeasureSpec,
                        (mEnd == null || mEnd.mRotate == 0) ? heightMeasureSpec : widthMeasureSpec);
            }
        }

        public void build() {
            final int n = getChildCount();
            mFrameArrayList.clear();
            SparseArray<MotionController> controllers = new SparseArray<>();
            int[] ids = new int[n];
            for (int i = 0; i < n; i++) {
                View v = getChildAt(i);
                MotionController motionController = new MotionController(v);
                controllers.put(ids[i] = v.getId(), motionController);
                mFrameArrayList.put(v, motionController);
            }
            for (int i = 0; i < n; i++) {
                View v = getChildAt(i);
                MotionController motionController = mFrameArrayList.get(v);
                if (motionController == null) {
                    continue;
                }
                if (mStart != null) {
                    ConstraintWidget startWidget = getWidget(mLayoutStart, v);
                    if (startWidget != null) {
                        motionController.setStartState(toRect(startWidget), mStart,
                                getWidth(), getHeight());
                    } else {
                        if (mDebugPath != 0) {
                            Log.e(TAG, Debug.getLocation() + "no widget for  "
                                    + Debug.getName(v) + " (" + v.getClass().getName() + ")");
                        }
                    }
                } else {
                    if (mInRotation) {
                        motionController.setStartState(mPreRotate.get(v), v, mRotatMode,
                                mPreRotateWidth, mPreRotateHeight);
                    }
                }
                if (mEnd != null) {
                    ConstraintWidget endWidget = getWidget(mLayoutEnd, v);
                    if (endWidget != null) {
                        motionController.setEndState(toRect(endWidget), mEnd,
                                getWidth(), getHeight());
                    } else {
                        if (mDebugPath != 0) {
                            Log.e(TAG, Debug.getLocation() + "no widget for  "
                                    + Debug.getName(v)
                                    + " (" + v.getClass().getName() + ")");
                        }
                    }
                }
            }

            for (int i = 0; i < n; i++) {
                MotionController controller = controllers.get(ids[i]);
                int relativeToId = controller.getAnimateRelativeTo();
                if (relativeToId != UNSET) {
                    controller.setupRelative(controllers.get(relativeToId));
                }
            }
        }

        public void setMeasuredId(int startId, int endId) {
            mStartId = startId;
            mEndId = endId;
        }

        public boolean isNotConfiguredWith(int startId, int endId) {
            return startId != mStartId || endId != mEndId;
        }
    }

    Model mModel = new Model();

    private Rect toRect(ConstraintWidget cw) {
        mTempRect.top = cw.getY();
        mTempRect.left = cw.getX();
        mTempRect.right = cw.getWidth() + mTempRect.left;
        mTempRect.bottom = cw.getHeight() + mTempRect.top;
        return mTempRect;
    }

    @Override
    public void requestLayout() {
        if (!mMeasureDuringTransition) {
            if (mCurrentState == UNSET && mScene != null
                    && mScene.mCurrentTransition != null) {
                int mode = mScene.mCurrentTransition.getLayoutDuringTransition();
                if (mode == MotionScene.LAYOUT_IGNORE_REQUEST) {
                    return;
                } else if (mode == MotionScene.LAYOUT_CALL_MEASURE) {
                    final int n = getChildCount();
                    for (int i = 0; i < n; i++) {
                        View v = getChildAt(i);
                        MotionController motionController = mFrameArrayList.get(v);
                        motionController.remeasure();
                    }
                    return;
                }
            }
        }
        super.requestLayout();
    }

    @Override
    public String toString() {
        Context context = getContext();
        return Debug.getName(context, mBeginState) + "->"
                + Debug.getName(context, mEndState)
                + " (pos:" + mTransitionLastPosition + " Dpos/Dt:" + mLastVelocity;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (DEBUG) {
            Log.v(TAG, "onMeasure " + Debug.getLocation());
        }
        if (mScene == null) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
            return;
        }
        boolean recalc = (mLastWidthMeasureSpec != widthMeasureSpec
                || mLastHeightMeasureSpec != heightMeasureSpec);
        if (mNeedsFireTransitionCompleted) {
            mNeedsFireTransitionCompleted = false;
            onNewStateAttachHandlers();
            processTransitionCompleted();
            recalc = true;
        }

        if (mDirtyHierarchy) {
            recalc = true;
        }

        mLastWidthMeasureSpec = widthMeasureSpec;
        mLastHeightMeasureSpec = heightMeasureSpec;

        int startId = mScene.getStartId();
        int endId = mScene.getEndId();
        boolean setMeasure = true;
        if ((recalc || mModel.isNotConfiguredWith(startId, endId)) && mBeginState != UNSET) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
            mModel.initFrom(mLayoutWidget, mScene.getConstraintSet(startId),
                    mScene.getConstraintSet(endId));
            mModel.reEvaluateState();
            mModel.setMeasuredId(startId, endId);
            setMeasure = false;
        } else if (recalc) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        }

        if (mMeasureDuringTransition || setMeasure) {
            int heightPadding = getPaddingTop() + getPaddingBottom();
            int widthPadding = getPaddingLeft() + getPaddingRight();
            int androidLayoutWidth = mLayoutWidget.getWidth() + widthPadding;
            int androidLayoutHeight = mLayoutWidget.getHeight() + heightPadding;
            if (mWidthMeasureMode == MeasureSpec.AT_MOST
                    || mWidthMeasureMode == MeasureSpec.UNSPECIFIED) {
                androidLayoutWidth = (int) (mStartWrapWidth + mPostInterpolationPosition
                        * (mEndWrapWidth - mStartWrapWidth));
                requestLayout();
            }
            if (mHeightMeasureMode == MeasureSpec.AT_MOST
                    || mHeightMeasureMode == MeasureSpec.UNSPECIFIED) {
                androidLayoutHeight = (int) (mStartWrapHeight + mPostInterpolationPosition
                        * (mEndWrapHeight - mStartWrapHeight));
                requestLayout();
            }
            setMeasuredDimension(androidLayoutWidth, androidLayoutHeight);
        }
        evaluateLayout();
    }

    @Override
    public boolean onStartNestedScroll(@NonNull View child,
                                       @NonNull View target,
                                       int axes, int type) {
        if (DEBUG) {
            Log.v(TAG, "********** onStartNestedScroll( child:" + Debug.getName(child)
                    + ", target:" + Debug.getName(target) + ", axis:" + axes + ", type:" + type);
        }
        if (mScene == null
                || mScene.mCurrentTransition == null
                || mScene.mCurrentTransition.getTouchResponse() == null
                || (mScene.mCurrentTransition.getTouchResponse().getFlags()
                & TouchResponse.FLAG_DISABLE_SCROLL) != 0) {
            return false;
        }
        return true;
    }

    @Override
    public void onNestedScrollAccepted(@NonNull View child,
                                       @NonNull View target,
                                       int axes,
                                       int type) {
        if (DEBUG) {
            Log.v(TAG, "********** onNestedScrollAccepted( child:" + Debug.getName(child)
                    + ", target:" + Debug.getName(target) + ", axis:" + axes + ", type:" + type);
        }
        mScrollTargetTime = getNanoTime();
        mScrollTargetDT = 0;
        mScrollTargetDX = 0;
        mScrollTargetDY = 0;
    }

    @Override
    public void onStopNestedScroll(@NonNull View target, int type) {
        if (DEBUG) {
            Log.v(TAG, "********** onStopNestedScroll(   target:"
                    + Debug.getName(target) + " , type:" + type + " "
                    + mScrollTargetDX + ", " + mScrollTargetDY);
            Debug.logStack(TAG, "onStopNestedScroll ", 8);

        }
        if (mScene == null || mScrollTargetDT == 0) {
            return;
        }
        mScene.processScrollUp(mScrollTargetDX / mScrollTargetDT,
                mScrollTargetDY / mScrollTargetDT);
    }

    @Override
    public void onNestedScroll(@NonNull View target,
                               int dxConsumed,
                               int dyConsumed,
                               int dxUnconsumed,
                               int dyUnconsumed,
                               int type, int[] consumed) {
        if (mUndergoingMotion || dxConsumed != 0 || dyConsumed != 0) {
            consumed[0] += dxUnconsumed;
            consumed[1] += dyUnconsumed;
        }
        mUndergoingMotion = false;
    }

    @Override
    public void onNestedScroll(@NonNull View target,
                               int dxConsumed,
                               int dyConsumed,
                               int dxUnconsumed,
                               int dyUnconsumed,
                               int type) {
        if (DEBUG) {
            Log.v(TAG, "********** onNestedScroll( target:" + Debug.getName(target)
                    + ", dxConsumed:" + dxConsumed
                    + ", dyConsumed:" + dyConsumed
                    + ", dyConsumed:" + dxUnconsumed
                    + ", dyConsumed:" + dyUnconsumed + ", type:" + type);
        }
    }

    @Override
    public void onNestedPreScroll(@NonNull View target,
                                  int dx,
                                  int dy,
                                  int @NonNull [] consumed,
                                  int type) {

        MotionScene scene = mScene;
        if (scene == null) {
            return;
        }

        MotionScene.Transition currentTransition = scene.mCurrentTransition;
        if (currentTransition == null || !currentTransition.isEnabled()) {
            return;
        }

        if (currentTransition.isEnabled()) {
            TouchResponse touchResponse = currentTransition.getTouchResponse();
            if (touchResponse != null) {
                int regionId = touchResponse.getTouchRegionId();
                if (regionId != MotionScene.UNSET && target.getId() != regionId) {
                    return;
                }
            }
        }

        if (scene.getMoveWhenScrollAtTop()) {
            // This blocks transition during scrolling
            TouchResponse touchResponse = currentTransition.getTouchResponse();
            int vert = -1;
            if (touchResponse != null) {
                if ((touchResponse.getFlags() & TouchResponse.FLAG_SUPPORT_SCROLL_UP) != 0) {
                    vert = dy;
                }
            }
            if ((mTransitionPosition == 1 || mTransitionPosition == 0)
                    && target.canScrollVertically(vert)) {
                return;
            }
        }

        // This should be disabled in androidx
        if (currentTransition.getTouchResponse() != null
                && (currentTransition.getTouchResponse().getFlags()
                & TouchResponse.FLAG_DISABLE_POST_SCROLL) != 0) {
            float dir = scene.getProgressDirection(dx, dy);
            if ((mTransitionLastPosition <= 0.0f && (dir < 0))
                    || (mTransitionLastPosition >= 1.0f && (dir > 0))) {
                target.setNestedScrollingEnabled(false);
                // TODO find a better hack
                target.post(new Runnable() {
                    @Override
                    public void run() {
                        target.setNestedScrollingEnabled(true);
                    }
                });
                return;
            }
        }

        if (DEBUG) {
            Log.v(TAG, "********** onNestedPreScroll(target:"
                    + Debug.getName(target) + ", dx:" + dx + ", dy:" + dy + ", type:" + type);
        }
        float progress = mTransitionPosition;
        long time = getNanoTime();
        mScrollTargetDX = dx;
        mScrollTargetDY = dy;
        mScrollTargetDT = (float) ((time - mScrollTargetTime) * 1E-9);
        mScrollTargetTime = time;
        if (DEBUG) {
            Log.v(TAG, "********** dy = " + dx + " dy = " + dy + " dt = " + mScrollTargetDT);
        }
        scene.processScrollMove(dx, dy);
        if (progress != mTransitionPosition) {
            consumed[0] = dx;
            consumed[1] = dy;
        }
        evaluate(false);
        if (consumed[0] != 0 || consumed[1] != 0) {
            mUndergoingMotion = true;
        }

    }

    @Override
    public boolean onNestedPreFling(@NonNull View target, float velocityX, float velocityY) {
        return false;
    }

    @Override
    public boolean onNestedFling(@NonNull View target,
                                 float velocityX,
                                 float velocityY,
                                 boolean consumed) {
        return false;
    }

    ////////////////////////////////////////////////////////////////////////////////////////
    // Used to draw debugging lines
    ////////////////////////////////////////////////////////////////////////////////////////
    private class DevModeDraw {
        private static final int DEBUG_PATH_TICKS_PER_MS = 16;
        float[] mPoints;
        int[] mPathMode;
        float[] mKeyFramePoints;
        Path mPath;
        Paint mPaint;
        Paint mPaintKeyframes;
        Paint mPaintGraph;
        Paint mTextPaint;
        Paint mFillPaint;
        private float[] mRectangle;
        final int mRedColor = 0xFFFFAA33;
        final int mKeyframeColor = 0xffe0759a;
        final int mGraphColor = 0xFF33AA00;
        final int mShadowColor = 0x77000000;
        final int mDiamondSize = 10;
        DashPathEffect mDashPathEffect;
        int mKeyFrameCount;
        Rect mBounds = new Rect();
        boolean mPresentationMode = false;
        int mShadowTranslate = 1;

        DevModeDraw() {
            mPaint = new Paint();
            mPaint.setAntiAlias(true);
            mPaint.setColor(mRedColor);
            mPaint.setStrokeWidth(2);
            mPaint.setStyle(Paint.Style.STROKE);

            mPaintKeyframes = new Paint();
            mPaintKeyframes.setAntiAlias(true);
            mPaintKeyframes.setColor(mKeyframeColor);
            mPaintKeyframes.setStrokeWidth(2);
            mPaintKeyframes.setStyle(Paint.Style.STROKE);

            mPaintGraph = new Paint();
            mPaintGraph.setAntiAlias(true);
            mPaintGraph.setColor(mGraphColor);
            mPaintGraph.setStrokeWidth(2);
            mPaintGraph.setStyle(Paint.Style.STROKE);

            mTextPaint = new Paint();
            mTextPaint.setAntiAlias(true);
            mTextPaint.setColor(mGraphColor);
            mTextPaint.setTextSize(12 * getContext().getResources().getDisplayMetrics().density);
            mRectangle = new float[8];
            mFillPaint = new Paint();
            mFillPaint.setAntiAlias(true);
            mDashPathEffect = new DashPathEffect(new float[]{4, 8}, 0);
            mPaintGraph.setPathEffect(mDashPathEffect);
            mKeyFramePoints = new float[MAX_KEY_FRAMES * 2];
            mPathMode = new int[MAX_KEY_FRAMES];

            if (mPresentationMode) {
                mPaint.setStrokeWidth(8);
                mFillPaint.setStrokeWidth(8);
                mPaintKeyframes.setStrokeWidth(8);
                mShadowTranslate = 4;
            }
        }

        public void draw(Canvas canvas,
                         HashMap<View, MotionController> frameArrayList,
                         int duration, int debugPath) {
            if (frameArrayList == null || frameArrayList.size() == 0) {
                return;
            }
            canvas.save();
            if (!isInEditMode() && (DEBUG_SHOW_PROGRESS & debugPath) == DEBUG_SHOW_PATH) {
                String str = getContext().getResources().getResourceName(mEndState)
                        + ":" + getProgress();
                canvas.drawText(str, 10, getHeight() - 30, mTextPaint);
                canvas.drawText(str, 11, getHeight() - 29, mPaint);
            }
            for (MotionController motionController : frameArrayList.values()) {
                int mode = motionController.getDrawPath();
                if (debugPath > 0 && mode == MotionController.DRAW_PATH_NONE) {
                    mode = MotionController.DRAW_PATH_BASIC;
                }
                if (mode == MotionController.DRAW_PATH_NONE) { // do not draw path
                    continue;
                }

                mKeyFrameCount = motionController.buildKeyFrames(mKeyFramePoints, mPathMode);

                if (mode >= MotionController.DRAW_PATH_BASIC) {

                    int frames = duration / DEBUG_PATH_TICKS_PER_MS;
                    if (mPoints == null || mPoints.length != frames * 2) {
                        mPoints = new float[frames * 2];
                        mPath = new Path();
                    }

                    canvas.translate(mShadowTranslate, mShadowTranslate);

                    mPaint.setColor(mShadowColor);
                    mFillPaint.setColor(mShadowColor);
                    mPaintKeyframes.setColor(mShadowColor);
                    mPaintGraph.setColor(mShadowColor);
                    motionController.buildPath(mPoints, frames);
                    drawAll(canvas, mode, mKeyFrameCount, motionController);
                    mPaint.setColor(mRedColor);
                    mPaintKeyframes.setColor(mKeyframeColor);
                    mFillPaint.setColor(mKeyframeColor);
                    mPaintGraph.setColor(mGraphColor);

                    canvas.translate(-mShadowTranslate, -mShadowTranslate);
                    drawAll(canvas, mode, mKeyFrameCount, motionController);
                    if (mode == MotionController.DRAW_PATH_RECTANGLE) {
                        drawRectangle(canvas, motionController);
                    }
                }

            }
            canvas.restore();
        }

        public void drawAll(Canvas canvas,
                            int mode,
                            int keyFrames,
                            MotionController motionController) {
            if (mode == MotionController.DRAW_PATH_AS_CONFIGURED) {
                drawPathAsConfigured(canvas);
            }
            if (mode == MotionController.DRAW_PATH_RELATIVE) {
                drawPathRelative(canvas);
            }
            if (mode == MotionController.DRAW_PATH_CARTESIAN) {
                drawPathCartesian(canvas);
            }
            drawBasicPath(canvas);
            drawTicks(canvas, mode, keyFrames, motionController);
        }

        private void drawBasicPath(Canvas canvas) {
            canvas.drawLines(mPoints, mPaint);
        }

        private void drawTicks(Canvas canvas,
                               int mode,
                               int keyFrames,
                               MotionController motionController) {
            int viewWidth = 0;
            int viewHeight = 0;
            if (motionController.mView != null) {
                viewWidth = motionController.mView.getWidth();
                viewHeight = motionController.mView.getHeight();
            }
            for (int i = 1; i < keyFrames - 1; i++) {
                if (mode == MotionController.DRAW_PATH_AS_CONFIGURED
                        && mPathMode[i - 1] == MotionController.DRAW_PATH_NONE) {
                    continue;

                }
                float x = mKeyFramePoints[i * 2];
                float y = mKeyFramePoints[i * 2 + 1];
                mPath.reset();
                mPath.moveTo(x, y + mDiamondSize);
                mPath.lineTo(x + mDiamondSize, y);
                mPath.lineTo(x, y - mDiamondSize);
                mPath.lineTo(x - mDiamondSize, y);
                mPath.close();

                @SuppressWarnings("unused")
                MotionPaths framePoint = motionController.getKeyFrame(i - 1);
                float dx = 0; //framePoint.translationX;
                float dy = 0; //framePoint.translationY;
                if (mode == MotionController.DRAW_PATH_AS_CONFIGURED) {

                    if (mPathMode[i - 1] == MotionPaths.PERPENDICULAR) {
                        drawPathRelativeTicks(canvas, x - dx, y - dy);
                    } else if (mPathMode[i - 1] == MotionPaths.CARTESIAN) {
                        drawPathCartesianTicks(canvas, x - dx, y - dy);
                    } else if (mPathMode[i - 1] == MotionPaths.SCREEN) {
                        drawPathScreenTicks(canvas, x - dx, y - dy, viewWidth, viewHeight);
                    }

                    canvas.drawPath(mPath, mFillPaint);
                }
                if (mode == MotionController.DRAW_PATH_RELATIVE) {
                    drawPathRelativeTicks(canvas, x - dx, y - dy);
                }
                if (mode == MotionController.DRAW_PATH_CARTESIAN) {
                    drawPathCartesianTicks(canvas, x - dx, y - dy);
                }
                if (mode == MotionController.DRAW_PATH_SCREEN) {
                    drawPathScreenTicks(canvas, x - dx, y - dy, viewWidth, viewHeight);
                }
                if (dx != 0 || dy != 0) {
                    drawTranslation(canvas, x - dx, y - dy, x, y);
                } else {
                    canvas.drawPath(mPath, mFillPaint);
                }
            }
            if (mPoints.length > 1) {
                // Draw the starting and ending circle
                canvas.drawCircle(mPoints[0], mPoints[1], 8, mPaintKeyframes);
                canvas.drawCircle(mPoints[mPoints.length - 2],
                        mPoints[mPoints.length - 1], 8, mPaintKeyframes);
            }
        }

        private void drawTranslation(Canvas canvas, float x1, float y1, float x2, float y2) {
            canvas.drawRect(x1, y1, x2, y2, mPaintGraph);
            canvas.drawLine(x1, y1, x2, y2, mPaintGraph);
        }

        private void drawPathRelative(Canvas canvas) {
            canvas.drawLine(mPoints[0], mPoints[1],
                    mPoints[mPoints.length - 2], mPoints[mPoints.length - 1], mPaintGraph);
        }

        private void drawPathAsConfigured(Canvas canvas) {
            boolean path = false;
            boolean cart = false;
            for (int i = 0; i < mKeyFrameCount; i++) {
                if (mPathMode[i] == MotionPaths.PERPENDICULAR) {
                    path = true;
                }
                if (mPathMode[i] == MotionPaths.CARTESIAN) {
                    cart = true;
                }
            }
            if (path) {
                drawPathRelative(canvas);
            }
            if (cart) {
                drawPathCartesian(canvas);
            }
        }

        private void drawPathRelativeTicks(Canvas canvas, float x, float y) {
            float x1 = mPoints[0];
            float y1 = mPoints[1];
            float x2 = mPoints[mPoints.length - 2];
            float y2 = mPoints[mPoints.length - 1];
            float dist = (float) Math.hypot(x1 - x2, y1 - y2);
            float t = ((x - x1) * (x2 - x1) + (y - y1) * (y2 - y1)) / (dist * dist);
            float xp = x1 + t * (x2 - x1);
            float yp = y1 + t * (y2 - y1);

            Path path = new Path();
            path.moveTo(x, y);
            path.lineTo(xp, yp);
            float len = (float) Math.hypot(xp - x, yp - y);
            String text = "" + ((int) (100 * len / dist)) / 100.0f;
            getTextBounds(text, mTextPaint);
            float off = len / 2 - mBounds.width() / 2;
            canvas.drawTextOnPath(text, path, off, -20, mTextPaint);
            canvas.drawLine(x, y, xp, yp, mPaintGraph);
        }

        void getTextBounds(String text, Paint paint) {
            paint.getTextBounds(text, 0, text.length(), mBounds);
        }

        private void drawPathCartesian(Canvas canvas) {
            float x1 = mPoints[0];
            float y1 = mPoints[1];
            float x2 = mPoints[mPoints.length - 2];
            float y2 = mPoints[mPoints.length - 1];

            canvas.drawLine(Math.min(x1, x2), Math.max(y1, y2),
                    Math.max(x1, x2), Math.max(y1, y2), mPaintGraph);
            canvas.drawLine(Math.min(x1, x2), Math.min(y1, y2),
                    Math.min(x1, x2), Math.max(y1, y2), mPaintGraph);
        }

        private void drawPathCartesianTicks(Canvas canvas, float x, float y) {
            float x1 = mPoints[0];
            float y1 = mPoints[1];
            float x2 = mPoints[mPoints.length - 2];
            float y2 = mPoints[mPoints.length - 1];
            float minx = Math.min(x1, x2);
            float maxy = Math.max(y1, y2);
            float xgap = x - Math.min(x1, x2);
            float ygap = Math.max(y1, y2) - y;
            // Horizontal line
            String text = "" + ((int) (0.5 + 100 * xgap / Math.abs(x2 - x1))) / 100.0f;
            getTextBounds(text, mTextPaint);
            float off = xgap / 2 - mBounds.width() / 2;
            canvas.drawText(text, off + minx, y - 20, mTextPaint);
            canvas.drawLine(x, y,
                    Math.min(x1, x2), y, mPaintGraph);

            // Vertical line
            text = "" + ((int) (0.5 + 100 * ygap / Math.abs(y2 - y1))) / 100.0f;
            getTextBounds(text, mTextPaint);
            off = ygap / 2 - mBounds.height() / 2;
            canvas.drawText(text, x + 5, maxy - off, mTextPaint);
            canvas.drawLine(x, y,
                    x, Math.max(y1, y2), mPaintGraph);
        }

        private void drawPathScreenTicks(Canvas canvas,
                                         float x,
                                         float y,
                                         int viewWidth,
                                         int viewHeight) {
            float x1 = 0;
            float y1 = 0;
            float x2 = 1;
            float y2 = 1;
            float minx = 0;
            float maxy = 0;
            float xgap = x;
            float ygap = y;
            // Horizontal line
            String text = "" + ((int) (0.5 + 100 * (xgap - viewWidth / 2)
                    / (getWidth() - viewWidth))) / 100.0f;
            getTextBounds(text, mTextPaint);
            float off = xgap / 2 - mBounds.width() / 2;
            canvas.drawText(text, off + minx, y - 20, mTextPaint);
            canvas.drawLine(x, y,
                    Math.min(x1, x2), y, mPaintGraph);

            // Vertical line
            text = "" + ((int) (0.5 + 100 * (ygap - viewHeight / 2)
                    / (getHeight() - viewHeight))) / 100.0f;
            getTextBounds(text, mTextPaint);
            off = ygap / 2 - mBounds.height() / 2;
            canvas.drawText(text, x + 5, maxy - off, mTextPaint);
            canvas.drawLine(x, y,
                    x, Math.max(y1, y2), mPaintGraph);
        }

        private void drawRectangle(Canvas canvas, MotionController motionController) {
            mPath.reset();
            int rectFrames = 50;
            for (int i = 0; i <= rectFrames; i++) {
                float p = i / (float) rectFrames;
                motionController.buildRect(p, mRectangle, 0);
                mPath.moveTo(mRectangle[0], mRectangle[1]);
                mPath.lineTo(mRectangle[2], mRectangle[3]);
                mPath.lineTo(mRectangle[4], mRectangle[5]);
                mPath.lineTo(mRectangle[6], mRectangle[7]);
                mPath.close();
            }
            mPaint.setColor(0x44000000);
            canvas.translate(2, 2);
            canvas.drawPath(mPath, mPaint);

            canvas.translate(-2, -2);
            mPaint.setColor(0xFFFF0000);
            canvas.drawPath(mPath, mPaint);
        }

    }

    @SuppressLint("LogConditional")
    private void debugPos() {
        for (int i = 0; i < getChildCount(); i++) {
            final View child = getChildAt(i);
            Log.v(TAG, " " + Debug.getLocation() + " " + Debug.getName(this)
                    + " " + Debug.getName(getContext(), mCurrentState) + " " + Debug.getName(child)
                    + child.getLeft() + " "
                    + child.getTop());
        }
    }

    /**
     * Used to draw debugging graphics and to do post layout changes
     *
     * @param canvas
     */
    @Override
    protected void dispatchDraw(Canvas canvas) {
        if (DEBUG) {
            Log.v(TAG, " dispatchDraw " + getProgress() + Debug.getLocation());
        }
        if (mDecoratorsHelpers != null) {
            for (MotionHelper decor : mDecoratorsHelpers) {
                decor.onPreDraw(canvas);
            }
        }
        evaluate(false);
        if (mScene != null && mScene.mViewTransitionController != null) {
            mScene.mViewTransitionController.animate();
        }
        if (DEBUG) {
            Log.v(TAG, " dispatchDraw" + Debug.getLocation() + " " + Debug.getName(this)
                    + " " + Debug.getName(getContext(), mCurrentState));
            debugPos();
        }
        super.dispatchDraw(canvas);
        if (mScene == null) {
            return;
        }
        if (DEBUG) {
            mDebugPath = 0xFF;
        }
        if ((mDebugPath & 1) == 1) {
            if (!isInEditMode()) {
                mFrames++;
                long currentDrawTime = getNanoTime();
                if (mLastDrawTime != -1) {
                    long delay = currentDrawTime - mLastDrawTime;
                    if (delay > 200000000) {
                        float fps = mFrames / (delay * 1E-9f);
                        mLastFps = ((int) (fps * 100)) / 100.0f;
                        mFrames = 0;
                        mLastDrawTime = currentDrawTime;
                    }
                } else {
                    mLastDrawTime = currentDrawTime;
                }
                Paint paint = new Paint();
                paint.setTextSize(42);
                float p = ((int) (getProgress() * 1000)) / 10f;
                String str = mLastFps + " fps " + Debug.getState(this, mBeginState) + " -> ";
                str += Debug.getState(this, mEndState) + " (progress: " + p + " ) state="
                        + ((mCurrentState == UNSET) ? "undefined"
                                : Debug.getState(this, mCurrentState));
                paint.setColor(0xFF000000);
                canvas.drawText(str, 11, getHeight() - 29, paint);
                paint.setColor(0xFF880088);
                canvas.drawText(str, 10, getHeight() - 30, paint);

            }
        }
        if (mDebugPath > 1) {
            if (mDevModeDraw == null) {
                mDevModeDraw = new DevModeDraw();
            }
            mDevModeDraw.draw(canvas, mFrameArrayList, mScene.getDuration(), mDebugPath);
        }
        if (mDecoratorsHelpers != null) {
            for (MotionHelper decor : mDecoratorsHelpers) {
                decor.onPostDraw(canvas);
            }
        }
    }

    /**
     * Direct layout evaluation
     */
    private void evaluateLayout() {
        float dir = Math.signum(mTransitionGoalPosition - mTransitionLastPosition);
        long currentTime = getNanoTime();

        float deltaPos = 0f;
        if (!(mInterpolator instanceof StopLogic)) { // if we are not in a drag
            deltaPos = dir * (currentTime - mTransitionLastTime) * 1E-9f / mTransitionDuration;
        }
        float position = mTransitionLastPosition + deltaPos;

        boolean done = false;
        if (mTransitionInstantly) {
            position = mTransitionGoalPosition;
        }

        if ((dir > 0 && position >= mTransitionGoalPosition)
                || (dir <= 0 && position <= mTransitionGoalPosition)) {
            position = mTransitionGoalPosition;
            done = true;
        }
        if (mInterpolator != null && !done) {
            if (mTemporalInterpolator) {
                float time = (currentTime - mAnimationStartTime) * 1E-9f;
                position = mInterpolator.getInterpolation(time);
            } else {
                position = mInterpolator.getInterpolation(position);
            }
        }
        if ((dir > 0 && position >= mTransitionGoalPosition)
                || (dir <= 0 && position <= mTransitionGoalPosition)) {
            position = mTransitionGoalPosition;
        }
        mPostInterpolationPosition = position;
        int n = getChildCount();
        long time = getNanoTime();
        float interPos = mProgressInterpolator == null ? position
                : mProgressInterpolator.getInterpolation(position);
        for (int i = 0; i < n; i++) {
            final View child = getChildAt(i);
            final MotionController frame = mFrameArrayList.get(child);
            if (frame != null) {
                frame.interpolate(child, interPos, time, mKeyCache);
            }
        }
        if (mMeasureDuringTransition) {
            requestLayout();
        }
    }

    void endTrigger(boolean start) {
        int n = getChildCount();
        for (int i = 0; i < n; i++) {
            final View child = getChildAt(i);
            final MotionController frame = mFrameArrayList.get(child);
            if (frame != null) {
                frame.endTrigger(start);
            }
        }
    }

    void evaluate(boolean force) {

        if (mTransitionLastTime == -1) {
            mTransitionLastTime = getNanoTime();
        }
        if (mTransitionLastPosition > 0.0f && mTransitionLastPosition < 1.0f) {
            mCurrentState = UNSET;
        }

        boolean newState = false;
        if (mKeepAnimating || (mInTransition
                && (force || mTransitionGoalPosition != mTransitionLastPosition))) {
            float dir = Math.signum(mTransitionGoalPosition - mTransitionLastPosition);
            long currentTime = getNanoTime();

            float deltaPos = 0f;
            if (!(mInterpolator instanceof MotionInterpolator)) { // if we are not in a drag
                deltaPos = dir * (currentTime - mTransitionLastTime) * 1E-9f / mTransitionDuration;
            }
            float position = mTransitionLastPosition + deltaPos;

            boolean done = false;
            if (mTransitionInstantly) {
                position = mTransitionGoalPosition;
            }

            if ((dir > 0 && position >= mTransitionGoalPosition)
                    || (dir <= 0 && position <= mTransitionGoalPosition)) {
                position = mTransitionGoalPosition;
                mInTransition = false;
                done = true;
            }
            if (DEBUG) {
                Log.v(TAG, Debug.getLocation() + " mTransitionLastPosition = "
                        + mTransitionLastPosition + " position = " + position);
            }
            mTransitionLastPosition = position;
            mTransitionPosition = position;
            mTransitionLastTime = currentTime;
            int notStopLogic = 0;
            int stopLogicContinue = 1;
            int stopLogicStop = 2;
            int stopLogicDone = notStopLogic;
            if (mInterpolator != null && !done) {
                if (mTemporalInterpolator) {
                    float time = (currentTime - mAnimationStartTime) * 1E-9f;
                    position = mInterpolator.getInterpolation(time);
                    if (mInterpolator == mStopLogic) {
                        boolean dp = mStopLogic.isStopped();
                        stopLogicDone = dp ? stopLogicStop : stopLogicContinue;
                    }

                    if (DEBUG) {
                        Log.v(TAG, Debug.getLocation() + " mTransitionLastPosition = "
                                + mTransitionLastPosition + " position = " + position);
                    }
                    mTransitionLastPosition = position;

                    mTransitionLastTime = currentTime;
                    if (mInterpolator instanceof MotionInterpolator) {
                        float lastVelocity = ((MotionInterpolator) mInterpolator).getVelocity();
                        mLastVelocity = lastVelocity;
                        if (Math.abs(lastVelocity) * mTransitionDuration <= EPSILON
                                && stopLogicDone == stopLogicStop) {
                            mInTransition = false;
                        }
                        if (lastVelocity > 0 && position >= 1.0f) {
                            mTransitionLastPosition = position = 1.0f;
                            mInTransition = false;
                        }
                        if (lastVelocity < 0 && position <= 0) {
                            mTransitionLastPosition = position = 0.0f;
                            mInTransition = false;
                        }
                    }

                } else {

                    float p2 = position;
                    position = mInterpolator.getInterpolation(position);
                    if (mInterpolator instanceof MotionInterpolator) {
                        mLastVelocity = ((MotionInterpolator) mInterpolator).getVelocity();
                    } else {
                        p2 = mInterpolator.getInterpolation(p2 + deltaPos);
                        mLastVelocity = dir * (p2 - position) / deltaPos;
                    }

                }
            } else {
                mLastVelocity = deltaPos;
            }
            if (Math.abs(mLastVelocity) > EPSILON) {
                setState(TransitionState.MOVING);
            }

            if (stopLogicDone != stopLogicContinue) {
                if ((dir > 0 && position >= mTransitionGoalPosition)
                        || (dir <= 0 && position <= mTransitionGoalPosition)) {
                    position = mTransitionGoalPosition;
                    mInTransition = false;
                }

                if (position >= 1.0f || position <= 0.0f) {
                    mInTransition = false;
                    setState(TransitionState.FINISHED);
                }
            }

            int n = getChildCount();
            mKeepAnimating = false;
            long time = getNanoTime();
            if (DEBUG) {
                Log.v(TAG, "LAYOUT frame.interpolate at " + position);
            }
            mPostInterpolationPosition = position;
            float interPos = mProgressInterpolator == null ? position
                    : mProgressInterpolator.getInterpolation(position);
            if (mProgressInterpolator != null) {
                mLastVelocity =
                        mProgressInterpolator
                                .getInterpolation(position + dir / mTransitionDuration);
                mLastVelocity -= mProgressInterpolator.getInterpolation(position);
            }
            for (int i = 0; i < n; i++) {
                final View child = getChildAt(i);
                final MotionController frame = mFrameArrayList.get(child);
                if (frame != null) {
                    mKeepAnimating |= frame.interpolate(child, interPos, time, mKeyCache);
                }
            }
            if (DEBUG) {
                Log.v(TAG, " interpolate " + Debug.getLocation() + " " + Debug.getName(this)
                        + " " + Debug.getName(getContext(), mBeginState) + " " + position);
            }

            boolean end = ((dir > 0 && position >= mTransitionGoalPosition)
                    || (dir <= 0 && position <= mTransitionGoalPosition));
            if (!mKeepAnimating && !mInTransition && end) {
                setState(TransitionState.FINISHED);
            }
            if (mMeasureDuringTransition) {
                requestLayout();
            }

            mKeepAnimating |= !end;

            // If we have hit the begin state begin state could be unset
            if (position <= 0 && mBeginState != UNSET) {
                if (mCurrentState != mBeginState) {
                    newState = true;
                    mCurrentState = mBeginState;
                    ConstraintSet set = mScene.getConstraintSet(mBeginState);
                    set.applyCustomAttributes(this);
                    setState(TransitionState.FINISHED);
                }
            }

            if (position >= 1.0) {
                if (DEBUG) {
                    Log.v(TAG, Debug.getLoc() + " ============= setting  to end "
                            + Debug.getName(getContext(), mEndState) + "  " + position);
                }
                if (mCurrentState != mEndState) {
                    newState = true;
                    mCurrentState = mEndState;
                    ConstraintSet set = mScene.getConstraintSet(mEndState);
                    set.applyCustomAttributes(this);
                    setState(TransitionState.FINISHED);
                }
            }

            if (mKeepAnimating || mInTransition) {
                invalidate();
            } else {
                if ((dir > 0 && position == 1) || (dir < 0 && position == 0)) {
                    setState(TransitionState.FINISHED);
                }
            }
            if (!mKeepAnimating && !mInTransition && ((dir > 0 && position == 1)
                    || (dir < 0 && position == 0))) {
                onNewStateAttachHandlers();
            }
        }
        if (mTransitionLastPosition >= 1.0f) {
            if (mCurrentState != mEndState) {
                newState = true;
            }
            mCurrentState = mEndState;
        } else if (mTransitionLastPosition <= 0.0f) {
            if (mCurrentState != mBeginState) {
                newState = true;
            }
            mCurrentState = mBeginState;
        }

        mNeedsFireTransitionCompleted |= newState;

        if (newState && !mInLayout) {
            requestLayout();
        }

        mTransitionPosition = mTransitionLastPosition;
    }

    private boolean mNeedsFireTransitionCompleted = false;

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        mInLayout = true;
        try {
            if (DEBUG) {
                Log.v(TAG, " onLayout " + getProgress() + "  " + Debug.getLocation());
            }
            if (mScene == null) {
                super.onLayout(changed, left, top, right, bottom);
                return;
            }
            int w = right - left;
            int h = bottom - top;
            if (mLastLayoutWidth != w || mLastLayoutHeight != h) {
                rebuildScene();
                evaluate(true);
                if (DEBUG) {
                    Log.v(TAG, " onLayout  rebuildScene  " + Debug.getLocation());
                }
            }

            mLastLayoutWidth = w;
            mLastLayoutHeight = h;
            mOldWidth = w;
            mOldHeight = h;
        } finally {
            mInLayout = false;
        }
    }

    /**
     * block ConstraintLayout from handling layout description
     *
     * @param id
     */
    @Override
    protected void parseLayoutDescription(int id) {
        mConstraintLayoutSpec = null;
    }

    private void init(AttributeSet attrs) {
        IS_IN_EDIT_MODE = isInEditMode();
        if (attrs != null) {
            TypedArray a = getContext()
                    .obtainStyledAttributes(attrs, R.styleable.MotionLayout);
            final int count = a.getIndexCount();

            boolean apply = true;
            for (int i = 0; i < count; i++) {
                int attr = a.getIndex(i);
                if (attr == R.styleable.MotionLayout_layoutDescription) {
                    int n = a.getResourceId(attr, UNSET);
                    mScene = new MotionScene(getContext(), this, n);
                } else if (attr == R.styleable.MotionLayout_currentState) {
                    mCurrentState = a.getResourceId(attr, UNSET);
                } else if (attr == R.styleable.MotionLayout_motionProgress) {
                    mTransitionGoalPosition = a.getFloat(attr, 0.0f);
                    mInTransition = true;
                } else if (attr == R.styleable.MotionLayout_applyMotionScene) {
                    apply = a.getBoolean(attr, apply);
                } else if (attr == R.styleable.MotionLayout_showPaths) {
                    if (mDebugPath == 0) { // favor motionDebug
                        mDebugPath = a.getBoolean(attr, false) ? DEBUG_SHOW_PATH : 0;
                    }
                } else if (attr == R.styleable.MotionLayout_motionDebug) {
                    mDebugPath = a.getInt(attr, 0);
                }
            }
            a.recycle();
            if (mScene == null) {
                Log.e(TAG, "WARNING NO app:layoutDescription tag");
            }
            if (!apply) {
                mScene = null;
            }
        }
        if (mDebugPath != 0) {
            checkStructure();
        }
        if (mCurrentState == UNSET && mScene != null) {

            mCurrentState = mScene.getStartId();
            mBeginState = mScene.getStartId();
            if (DEBUG) {
                Log.v(TAG, " ============= init   end is "
                        + Debug.getName(getContext(), mEndState));
            }
            mEndState = mScene.getEndId();
            if (DEBUG) {
                Log.v(TAG, " ============= init setting end to "
                        + Debug.getName(getContext(), mEndState));
            }
        }
    }

    /**
     * Sets a motion scene to the layout. Subsequent calls to it will override the previous scene.
     */
    public void setScene(MotionScene scene) {
        mScene = scene;
        mScene.setRtl(isRtl());
        rebuildScene();
    }

    /**
     * Get the motion scene of the layout.
     * Warning! This gives you direct access to the internal
     * state of the MotionLayout making it easy
     * corrupt the state.
     * @return the motion scene
     */
    public MotionScene getScene() {
        return mScene;
    }

    private void checkStructure() {
        if (mScene == null) {
            Log.e(TAG, "CHECK: motion scene not set! set \"app:layoutDescription=\"@xml/file\"");
            return;
        }

        checkStructure(mScene.getStartId(), mScene.getConstraintSet(mScene.getStartId()));
        SparseIntArray startToEnd = new SparseIntArray();
        SparseIntArray endToStart = new SparseIntArray();
        for (MotionScene.Transition definedTransition : mScene.getDefinedTransitions()) {
            if (definedTransition == mScene.mCurrentTransition) {
                Log.v(TAG, "CHECK: CURRENT");
            }
            checkStructure(definedTransition);
            int startId = definedTransition.getStartConstraintSetId();
            int endId = definedTransition.getEndConstraintSetId();
            String startString = Debug.getName(getContext(), startId);
            String endString = Debug.getName(getContext(), endId);
            if (startToEnd.get(startId) == endId) {

                Log.e(TAG, "CHECK: two transitions with the same start and end "
                        + startString + "->" + endString);
            }
            if (endToStart.get(endId) == startId) {

                Log.e(TAG, "CHECK: you can't have reverse transitions"
                        + startString + "->" + endString);
            }
            startToEnd.put(startId, endId);
            endToStart.put(endId, startId);
            if (mScene.getConstraintSet(startId) == null) {
                Log.e(TAG, " no such constraintSetStart " + startString);
            }

            if (mScene.getConstraintSet(endId) == null) {
                Log.e(TAG, " no such constraintSetEnd " + startString);
            }
        }
    }

    private void checkStructure(int csetId, ConstraintSet set) {
        String setName = Debug.getName(getContext(), csetId);
        int size = getChildCount();
        for (int i = 0; i < size; i++) {
            View v = getChildAt(i);
            int id = v.getId();
            if (id == -1) {
                Log.w(TAG, "CHECK: " + setName + " ALL VIEWS SHOULD HAVE ID's "
                        + v.getClass().getName() + " does not!");
            }
            ConstraintSet.Constraint c = set.getConstraint(id);
            if (c == null) {
                Log.w(TAG, "CHECK: " + setName + " NO CONSTRAINTS for " + Debug.getName(v));
            }
        }
        int[] ids = set.getKnownIds();
        for (int i = 0; i < ids.length; i++) {
            int id = ids[i];
            String idString = Debug.getName(getContext(), id);
            if (null == findViewById(ids[i])) {
                Log.w(TAG, "CHECK: " + setName + " NO View matches id " + idString);
            }
            if (set.getHeight(id) == UNSET) {
                Log.w(TAG, "CHECK: " + setName + "(" + idString + ") no LAYOUT_HEIGHT");
            }
            if (set.getWidth(id) == UNSET) {
                Log.w(TAG, "CHECK: " + setName + "(" + idString + ") no LAYOUT_HEIGHT");
            }
        }
    }

    private void checkStructure(MotionScene.Transition transition) {
        if (DEBUG) {
            Log.v(TAG, "CHECK: transition = " + transition.debugString(getContext()));
            Log.v(TAG, "CHECK: transition.setDuration = " + transition.getDuration());
        }
        if (transition.getStartConstraintSetId() == transition.getEndConstraintSetId()) {
            Log.e(TAG, "CHECK: start and end constraint set should not be the same!");
        }
    }

    /**
     * Display the debugging information such as paths information
     *
     * @param debugMode integer representing various debug modes
     *
     */
    public void setDebugMode(int debugMode) {
        mDebugPath = debugMode;
        invalidate();
    }

    private RectF mBoundsCheck = new RectF();
    private View mRegionView = null;
    private Matrix mInverseMatrix = null;

    private boolean callTransformedTouchEvent(View view,
                                              MotionEvent event,
                                              float offsetX,
                                              float offsetY) {
        Matrix viewMatrix = view.getMatrix();

        if (viewMatrix.isIdentity()) {
            event.offsetLocation(offsetX, offsetY);
            boolean handled = view.onTouchEvent(event);
            event.offsetLocation(-offsetX, -offsetY);

            return handled;
        }

        MotionEvent transformedEvent = MotionEvent.obtain(event);

        transformedEvent.offsetLocation(offsetX, offsetY);

        if (mInverseMatrix == null) {
            mInverseMatrix = new Matrix();
        }

        viewMatrix.invert(mInverseMatrix);
        transformedEvent.transform(mInverseMatrix);

        boolean handled = view.onTouchEvent(transformedEvent);

        transformedEvent.recycle();

        return handled;
    }

    /**
     * Walk the view tree to see if a child view handles a touch event.
     *
     * @param x
     * @param y
     * @param view
     * @param event
     * @return
     */
    private boolean handlesTouchEvent(float x, float y, View view, MotionEvent event) {
        boolean handled = false;
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            final int childCount = group.getChildCount();
            for (int i = childCount - 1; i >= 0; i--) {
                View child = group.getChildAt(i);
                if (handlesTouchEvent(x + child.getLeft() - view.getScrollX(),
                        y + child.getTop() - view.getScrollY(),
                        child, event)) {
                    handled = true;
                    break;
                }
            }
        }

        if (!handled) {
            mBoundsCheck.set(x, y,
                    x + view.getRight() - view.getLeft(),
                    y + view.getBottom() - view.getTop());

            if (event.getAction() != MotionEvent.ACTION_DOWN
                    || mBoundsCheck.contains(event.getX(), event.getY())) {
                if (callTransformedTouchEvent(view, event, -x, -y)) {
                    handled = true;
                }
            }
        }

        return handled;
    }

    /**
     * Intercepts the touch event to correctly handle touch region id handover
     *
     * @param event
     * @return
     */
    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        if (mScene == null || !mInteractionEnabled) {
            return false;
        }

        if (mScene.mViewTransitionController != null) {
            mScene.mViewTransitionController.touchEvent(event);
        }
        MotionScene.Transition currentTransition = mScene.mCurrentTransition;
        if (currentTransition != null && currentTransition.isEnabled()) {
            TouchResponse touchResponse = currentTransition.getTouchResponse();
            if (touchResponse != null) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    RectF region = touchResponse.getTouchRegion(this, new RectF());
                    if (region != null
                            && !region.contains(event.getX(), event.getY())) {
                        return false;
                    }
                }
                int regionId = touchResponse.getTouchRegionId();
                if (regionId != MotionScene.UNSET) {
                    if (mRegionView == null || mRegionView.getId() != regionId) {
                        mRegionView = findViewById(regionId);
                    }
                    if (mRegionView != null) {
                        mBoundsCheck.set(mRegionView.getLeft(),
                                mRegionView.getTop(),
                                mRegionView.getRight(),
                                mRegionView.getBottom());
                        if (mBoundsCheck.contains(event.getX(), event.getY())) {
                            // In case of region id, if the view or a child of the view
                            // handles an event we don't need to do anything;
                            if (!handlesTouchEvent(mRegionView.getLeft(), mRegionView.getTop(),
                                    mRegionView, event)) {
                                // but if not, then *we* need to handle the event.
                                return onTouchEvent(event);
                            }
                        }
                    }
                }
            }
        }
        return false;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (DEBUG) {
            Log.v(TAG, Debug.getLocation() + " onTouchEvent = " + mTransitionLastPosition);
        }
        if (mScene != null && mInteractionEnabled && mScene.supportTouch()) {
            MotionScene.Transition currentTransition = mScene.mCurrentTransition;
            if (currentTransition != null && !currentTransition.isEnabled()) {
                return super.onTouchEvent(event);
            }
            mScene.processTouchEvent(event, getCurrentState(), this);
            if (mScene.mCurrentTransition.isTransitionFlag(TRANSITION_FLAG_INTERCEPT_TOUCH)) {
                return mScene.mCurrentTransition.getTouchResponse().isDragStarted();
            }
            return true;
        }
        if (DEBUG) {
            Log.v(TAG, Debug.getLocation() + " mTransitionLastPosition = "
                    + mTransitionLastPosition);
        }
        return super.onTouchEvent(event);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        Display display = getDisplay();
        if (display != null) {
            mPreviouseRotation = display.getRotation();
        }
        if (mScene != null && mCurrentState != UNSET) {
            ConstraintSet cSet = mScene.getConstraintSet(mCurrentState);
            mScene.readFallback(this);
            if (mDecoratorsHelpers != null) {
                for (MotionHelper mh : mDecoratorsHelpers) {
                    mh.onFinishedMotionScene(this);
                }
            }
            if (cSet != null) {
                cSet.applyTo(this);
            }
            mBeginState = mCurrentState;
        }
        onNewStateAttachHandlers();
        if (mStateCache != null) {
            if (mDelayedApply) {
                post(new Runnable() {
                    @Override
                    public void run() {
                        mStateCache.apply();
                    }
                });
            } else {
                mStateCache.apply();
            }
        } else {
            if (mScene != null && mScene.mCurrentTransition != null) {
                if (mScene.mCurrentTransition.getAutoTransition()
                        == MotionScene.Transition.AUTO_ANIMATE_TO_END) {
                    transitionToEnd();
                    setState(TransitionState.SETUP);
                    setState(TransitionState.MOVING);
                }
            }
        }
    }

    @Override
    public void onRtlPropertiesChanged(int layoutDirection) {
        if (mScene != null) {
            mScene.setRtl(isRtl());
        }
    }

    /**
     * This function will set up various handlers (swipe, click...) whenever
     * a new state is reached.
     */
    void onNewStateAttachHandlers() {
        if (mScene == null) {
            return;
        }
        if (mScene.autoTransition(this, mCurrentState)) {
            requestLayout();
            return;
        }
        if (mCurrentState != UNSET) {
            mScene.addOnClickListeners(this, mCurrentState);
        }
        if (mScene.supportTouch()) {
            mScene.setupTouch();
        }
    }

    /**
     * Return the current state id
     *
     * @return current state id
     */
    public int getCurrentState() {
        return mCurrentState;
    }

    /**
     * Get current position during an animation.
     *
     * @return current position from 0.0 to 1.0 inclusive
     */
    public float getProgress() {
        return mTransitionLastPosition;
    }

    /**
     * Provide an estimate of the motion with respect to change in transitionPosition
     * (assume you are currently in a transition)
     *
     * @param mTouchAnchorId id of the anchor view that will be "moved" by touch
     * @param pos            the transition position at which to estimate the position
     * @param locationX      the x location within the view (0.0 = left , 1.0 = right)
     * @param locationY      the y location within the view (0.0 = left , 1.0 = right)
     * @param mAnchorDpDt    returns the dx/dp and dy/dp
     */
    void getAnchorDpDt(int mTouchAnchorId,
                       float pos,
                       float locationX, float locationY,
                       float[] mAnchorDpDt) {
        View v;
        MotionController f = mFrameArrayList.get(v = getViewById(mTouchAnchorId));
        if (DEBUG) {
            Log.v(TAG, " getAnchorDpDt " + Debug.getName(v) + " " + Debug.getLocation());
        }
        if (f != null) {
            f.getDpDt(pos, locationX, locationY, mAnchorDpDt);
            float y = v.getY();
            float deltaPos = pos - mLastPos;
            float deltaY = y - mLastY;
            @SuppressWarnings("unused")
            float dydp = (deltaPos != 0.0f) ? deltaY / deltaPos : Float.NaN;
            if (DEBUG) {
                Log.v(TAG, " getAnchorDpDt " + Debug.getName(v) + " "
                        + Debug.getLocation() + " " + Arrays.toString(mAnchorDpDt));
            }

            mLastPos = pos;
            mLastY = y;
        } else {
            String idName = (v == null) ? "" + mTouchAnchorId :
                    v.getContext().getResources().getResourceName(mTouchAnchorId);
            Log.w(TAG, "WARNING could not find view id " + idName);
        }
    }

    /**
     * Gets the time of the currently set animation.
     *
     * @return time in Milliseconds
     */
    public long getTransitionTimeMs() {
        if (mScene != null) {
            mTransitionDuration = mScene.getDuration() / 1000f;
        }
        return (long) (mTransitionDuration * 1000);
    }

    /**
     * Set a listener to be notified of drawer events.
     *
     * @param listener Listener to notify when drawer events occur
     * @see TransitionListener
     */
    public void setTransitionListener(TransitionListener listener) {
        mTransitionListener = listener;
    }

    /**
     * adds a listener to be notified of drawer events.
     *
     * @param listener Listener to notify when drawer events occur
     * @see TransitionListener
     */
    public void addTransitionListener(TransitionListener listener) {
        if (mTransitionListeners == null) {
            mTransitionListeners = new CopyOnWriteArrayList<>();
        }
        mTransitionListeners.add(listener);
    }

    /**
     * adds a listener to be notified of drawer events.
     *
     * @param listener Listener to notify when drawer events occur
     * @return <tt>true</tt> if it contained the specified listener
     * @see TransitionListener
     */
    public boolean removeTransitionListener(TransitionListener listener) {
        if (mTransitionListeners == null) {
            return false;
        }
        return mTransitionListeners.remove(listener);
    }

    /**
     * Listener for monitoring events about TransitionLayout. <b>Added in 2.0</b>
     */
    public interface TransitionListener {
        /**
         * Called when a drawer is about to start a transition.
         * Note. startId may be -1 if starting from an "undefined state"
         *
         * @param motionLayout The TransitionLayout view that was moved
         * @param startId      the id of the start state (or ConstraintSet). Will be -1 if unknown.
         * @param endId        the id of the end state (or ConstraintSet).
         */
        void onTransitionStarted(MotionLayout motionLayout,
                                        int startId, int endId);

        /**
         * Called when a drawer's position changes.
         *
         * @param motionLayout The TransitionLayout view that was moved
         * @param startId      the id of the start state (or ConstraintSet). Will be -1 if unknown.
         * @param endId        the id of the end state (or ConstraintSet).
         * @param progress     The progress on this transition, from 0 to 1.
         */
        void onTransitionChange(MotionLayout motionLayout,
                                int startId, int endId,
                                float progress);

        /**
         * Called when a drawer has settled completely a state.
         * The TransitionLayout is interactive at this point.
         *
         * @param motionLayout Drawer view that is now open
         * @param currentId    the id it has reached
         */
        void onTransitionCompleted(MotionLayout motionLayout, int currentId);

        /**
         * Call when a trigger is fired
         *
         * @param motionLayout
         * @param triggerId    The id set set with triggerID
         * @param positive     for positive transition edge
         * @param progress
         */
        void onTransitionTrigger(MotionLayout motionLayout, int triggerId, boolean positive,
                                 float progress);
    }

    /**
     * This causes the callback onTransitionTrigger to be called
     *
     * @param triggerId The id set set with triggerID
     * @param positive  for positive transition edge
     * @param progress  the current progress
     */
    public void fireTrigger(int triggerId, boolean positive, float progress) {
        if (mTransitionListener != null) {
            mTransitionListener.onTransitionTrigger(this, triggerId, positive, progress);
        }
        if (mTransitionListeners != null) {
            for (TransitionListener listeners : mTransitionListeners) {
                listeners.onTransitionTrigger(this, triggerId, positive, progress);
            }
        }
    }

    private void fireTransitionChange() {
        if (mTransitionListener != null
                || (mTransitionListeners != null && !mTransitionListeners.isEmpty())) {
            if (mListenerPosition != mTransitionPosition) {
                if (mListenerState != UNSET) {
                    fireTransitionStarted();
                    mIsAnimating = true;
                }
                mListenerState = UNSET;
                mListenerPosition = mTransitionPosition;
                if (mTransitionListener != null) {
                    mTransitionListener.onTransitionChange(this,
                            mBeginState, mEndState, mTransitionPosition);
                }
                if (mTransitionListeners != null) {
                    for (TransitionListener listeners : mTransitionListeners) {
                        listeners.onTransitionChange(this,
                                mBeginState, mEndState, mTransitionPosition);
                    }
                }
                mIsAnimating = true;
            }
        }
    }

    ArrayList<Integer> mTransitionCompleted = new ArrayList<>();

    /**
     * This causes the callback TransitionCompleted to be called
     */
    protected void fireTransitionCompleted() {
        if (mTransitionListener != null
                || (mTransitionListeners != null && !mTransitionListeners.isEmpty())) {
            if (mListenerState == UNSET) {
                mListenerState = mCurrentState;
                int lastState = UNSET;
                if (!mTransitionCompleted.isEmpty()) {
                    lastState = mTransitionCompleted.get(mTransitionCompleted.size() - 1);
                }
                if (lastState != mCurrentState && mCurrentState != -1) {
                    mTransitionCompleted.add(mCurrentState);
                }
            }
        }
        processTransitionCompleted();
        if (mOnComplete != null) {
            mOnComplete.run();
            mOnComplete = null;
        }

        if (mScheduledTransitionTo != null && mScheduledTransitions > 0) {
            transitionToState(mScheduledTransitionTo[0]);
            System.arraycopy(mScheduledTransitionTo,
                    1, mScheduledTransitionTo,
                    0, mScheduledTransitionTo.length - 1);
            mScheduledTransitions--;
        }
    }

    private void processTransitionCompleted() {
        if (mTransitionListener == null
                && (mTransitionListeners == null || mTransitionListeners.isEmpty())) {
            return;
        }
        mIsAnimating = false;
        for (Integer state : mTransitionCompleted) {
            if (mTransitionListener != null) {
                mTransitionListener.onTransitionCompleted(this, state);
            }
            if (mTransitionListeners != null) {
                for (TransitionListener listeners : mTransitionListeners) {
                    listeners.onTransitionCompleted(this, state);
                }
            }
        }
        mTransitionCompleted.clear();
    }

    /**
     *
     */
    public DesignTool getDesignTool() {
        if (mDesignTool == null) {
            mDesignTool = new DesignTool(this);
        }
        return mDesignTool;
    }

    /**
     *
     */
    @Override
    public void onViewAdded(View view) {
        super.onViewAdded(view);
        if (view instanceof MotionHelper) {
            MotionHelper helper = (MotionHelper) view;
            if (mTransitionListeners == null) {
                mTransitionListeners = new CopyOnWriteArrayList<>();
            }
            mTransitionListeners.add(helper);

            if (helper.isUsedOnShow()) {
                if (mOnShowHelpers == null) {
                    mOnShowHelpers = new ArrayList<>();
                }
                mOnShowHelpers.add(helper);
            }
            if (helper.isUseOnHide()) {
                if (mOnHideHelpers == null) {
                    mOnHideHelpers = new ArrayList<>();
                }
                mOnHideHelpers.add(helper);
            }
            if (helper.isDecorator()) {
                if (mDecoratorsHelpers == null) {
                    mDecoratorsHelpers = new ArrayList<>();
                }
                mDecoratorsHelpers.add(helper);
            }
        }
    }

    /**
     *
     */
    @Override
    public void onViewRemoved(View view) {
        super.onViewRemoved(view);
        if (mOnShowHelpers != null) {
            mOnShowHelpers.remove(view);
        }
        if (mOnHideHelpers != null) {
            mOnHideHelpers.remove(view);
        }
    }

    /**
     * Notify OnShow motion helpers
     * @param progress
     */
    public void setOnShow(float progress) {
        if (mOnShowHelpers != null) {
            final int count = mOnShowHelpers.size();
            for (int i = 0; i < count; i++) {
                MotionHelper helper = mOnShowHelpers.get(i);
                helper.setProgress(progress);
            }
        }
    }

    /**
     * Notify OnHide motion helpers
     * @param progress
     */
    public void setOnHide(float progress) {
        if (mOnHideHelpers != null) {
            final int count = mOnHideHelpers.size();
            for (int i = 0; i < count; i++) {
                MotionHelper helper = mOnHideHelpers.get(i);
                helper.setProgress(progress);
            }
        }
    }

    /**
     * Get the id's of all constraintSets used by MotionLayout
     *
     * @return
     */
    public  @IdRes
            int[] getConstraintSetIds() {
        if (mScene == null) {
            return null;
        }
        return mScene.getConstraintSetIds();
    }

    /**
     * Get the id's of all constraintSets with the matching types
     *
     * @return
     */
    public int[] getMatchingConstraintSetIds(String ... types) {
        if (mScene == null) {
            return null;
        }
        return mScene.getMatchingStateLabels(types);
    }

    /**
     * Get the ConstraintSet associated with an id
     * This returns a link to the constraintSet
     * But in most cases can be used.
     * createConstraintSet makes a copy which is more expensive.
     *
     * @param id of the constraintSet
     * @return ConstraintSet of MotionLayout
     * @see #cloneConstraintSet(int)
     */
    public ConstraintSet getConstraintSet(int id) {
        if (mScene == null) {
            return null;
        }
        return mScene.getConstraintSet(id);
    }

    /**
     * Creates a ConstraintSet based on an existing
     * constraintSet.
     * This makes a copy of the ConstraintSet.
     *
     * @param id The ide of the ConstraintSet
     * @return the ConstraintSet
     */
    public ConstraintSet cloneConstraintSet(int id) {
        if (mScene == null) {
            return null;
        }
        ConstraintSet orig = mScene.getConstraintSet(id);
        ConstraintSet ret = new ConstraintSet();
        ret.clone(orig);
        return ret;
    }

    /**
     * rebuild the motion Layouts
     *
     * @deprecated Please call rebuildScene() instead.
     */
    @Deprecated
    public void rebuildMotion() {
        Log.e(TAG, "This method is deprecated. Please call rebuildScene() instead.");
        rebuildScene();
    }

    /**
     * rebuild the motion Layouts
     */
    public void rebuildScene() {
        mModel.reEvaluateState();
        invalidate();
    }

    /**
     * update a ConstraintSet under the id.
     *
     * @param stateId id of the ConstraintSet
     * @param set     The constraintSet
     */
    public void updateState(int stateId, ConstraintSet set) {
        if (mScene != null) {
            mScene.setConstraintSet(stateId, set);
        }
        updateState();
        if (mCurrentState == stateId) {
            set.applyTo(this);
        }
    }

    /**
     * Update a ConstraintSet but animate the change.
     *
     * @param stateId  id of the ConstraintSet
     * @param set      The constraintSet
     * @param duration The length of time to perform the animation
     */
    public void updateStateAnimate(int stateId, ConstraintSet set, int duration) {
        if (mScene == null) {
            return;
        }

        if (mCurrentState == stateId) {
            updateState(R.id.view_transition, getConstraintSet(stateId));
            setState(R.id.view_transition, -1, -1);
            updateState(stateId, set);
            MotionScene.Transition tmpTransition =
                    new MotionScene.Transition(-1, mScene, R.id.view_transition, stateId);
            tmpTransition.setDuration(duration);
            setTransition(tmpTransition);
            transitionToEnd();
        }
    }

    /**
     * on completing the current transition, transition to this state.
     *
     * @param id
     */
    public void scheduleTransitionTo(int id) {
        if (getCurrentState() == -1) {
            transitionToState(id);
        } else {
            if (mScheduledTransitionTo == null) {
                mScheduledTransitionTo = new int[4];
            } else if (mScheduledTransitionTo.length <= mScheduledTransitions) {
                mScheduledTransitionTo =
                        Arrays.copyOf(mScheduledTransitionTo, mScheduledTransitionTo.length * 2);
            }
            mScheduledTransitionTo[mScheduledTransitions++] = id;
        }
    }

    /**
     * Not sure we want this
     *
     *
     */
    public void updateState() {
        mModel.initFrom(mLayoutWidget,
                mScene.getConstraintSet(mBeginState),
                mScene.getConstraintSet(mEndState));
        rebuildScene();
    }

    /**
     * Get all Transitions known to the system.
     *
     * @return
     */
    public ArrayList<MotionScene.Transition> getDefinedTransitions() {
        if (mScene == null) {
            return null;
        }
        return mScene.getDefinedTransitions();
    }

    /**
     * Gets the state you are currently transitioning from.
     * If you are transitioning from an unknown state returns -1
     *
     * @return State you are transitioning from.
     */
    public int getStartState() {
        return mBeginState;
    }

    /**
     * Gets the state you are currently transition to.
     *
     * @return The State you are transitioning to.
     */
    public int getEndState() {
        return mEndState;
    }

    /**
     * Gets the position you are animating to typically 0 or 1.
     * This is useful during animation after touch up
     *
     * @return The target position you are moving to
     */
    public float getTargetPosition() {
        return mTransitionGoalPosition;
    }

    /**
     * Change the current Transition duration.
     *
     * @param milliseconds duration for transition to complete
     */
    public void setTransitionDuration(int milliseconds) {
        if (mScene == null) {
            Log.e(TAG, "MotionScene not defined");
            return;
        }
        mScene.setDuration(milliseconds);
    }

    /**
     * This returns the internal Transition Structure
     *
     * @param id
     * @return
     */
    public MotionScene.Transition getTransition(int id) {
        return mScene.getTransitionById(id);
    }

    /**
     * This looks up the constraintset ID given an id string (
     *
     * @param id String id (without the "@+id/")
     * @return the integer id of the string
     *
     */
    int lookUpConstraintId(String id) {
        if (mScene == null) {
            return 0;
        }
        return mScene.lookUpConstraintId(id);
    }

    /**
     * does a revers look up to find the ConstraintSets Name
     *
     * @param id the integer id of the constraintSet
     * @return
     */
    String getConstraintSetNames(int id) {
        if (mScene == null) {
            return null;
        }
        return mScene.lookUpConstraintName(id);
    }

    /**
     * this allow disabling autoTransitions to prevent design surface from being in undefined states
     *
     * @param disable
     */
    void disableAutoTransition(boolean disable) {
        if (mScene == null) {
            return;
        }
        mScene.disableAutoTransition(disable);
    }

    /**
     * Enables (or disables) MotionLayout's onClick and onSwipe handling.
     *
     * @param enabled If true,  touch & click  is enabled; otherwise it is disabled
     */
    public void setInteractionEnabled(boolean enabled) {
        mInteractionEnabled = enabled;
    }

    /**
     * Determines whether MotionLayout's touch & click handling are enabled.
     * An interaction enabled MotionLayout can respond to user input and initiate and control.
     * MotionLayout interactions are enabled initially by default.
     * MotionLayout touch & click handling may be enabled or disabled by calling its
     * setInteractionEnabled method.
     *
     * @return true if MotionLayout's  touch & click  is enabled, false otherwise
     */
    public boolean isInteractionEnabled() {
        return mInteractionEnabled;
    }

    private void fireTransitionStarted() {
        if (mTransitionListener != null) {
            mTransitionListener.onTransitionStarted(this, mBeginState, mEndState);
        }
        if (mTransitionListeners != null) {
            for (TransitionListener listeners : mTransitionListeners) {
                listeners.onTransitionStarted(this, mBeginState, mEndState);
            }
        }
    }

    /**
     * Execute a ViewTransition.
     * Transition will execute if its conditions are met and it is enabled
     *
     * @param viewTransitionId
     * @param view             The views to apply to
     */
    public void viewTransition(int viewTransitionId, View... view) {
        if (mScene != null) {
            mScene.viewTransition(viewTransitionId, view);
        } else {
            Log.e(TAG, " no motionScene");
        }
    }

    /**
     * Enable a ViewTransition ID.
     *
     * @param viewTransitionId id of ViewTransition
     * @param enable           If false view transition cannot be executed.
     */
    public void enableViewTransition(int viewTransitionId, boolean enable) {
        if (mScene != null) {
            mScene.enableViewTransition(viewTransitionId, enable);
        }
    }

    /**
     * Is transition id enabled or disabled
     *
     * @param viewTransitionId the ide of the transition
     * @return true if enabled
     */
    public boolean isViewTransitionEnabled(int viewTransitionId) {
        if (mScene != null) {
            return mScene.isViewTransitionEnabled(viewTransitionId);
        }
        return false;
    }

    /**
     * Apply the view transitions keyFrames to the MotionController.
     * Note ConstraintOverride is not used
     *
     * @param viewTransitionId the id of the view transition
     * @param motionController the MotionController to apply the keyframes to
     * @return true if it found and applied the viewTransition false otherwise
     */
    public boolean applyViewTransition(int viewTransitionId, MotionController motionController) {
        if (mScene != null) {
            return mScene.applyViewTransition(viewTransitionId, motionController);
        }
        return false;
    }

    /**
     * Is initial state changes are applied during onAttachedToWindow or after.
     * @return
     */
    public boolean isDelayedApplicationOfInitialState() {
        return mDelayedApply;
    }

    /**
     * Initial state changes are applied during onAttachedToWindow unless this is set to true.
     * @param delayedApply
     */
    public void setDelayedApplicationOfInitialState(boolean delayedApply) {
        this.mDelayedApply = delayedApply;
    }

}
