/*
 * Copyright (C) 2016 The Android Open Source Project
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

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.content.res.XmlResourceParser;
import android.graphics.Color;
import android.os.Build;
import android.os.Build.VERSION_CODES;
import android.util.AttributeSet;
import android.util.Log;
import android.util.SparseArray;
import android.util.SparseIntArray;
import android.util.TypedValue;
import android.util.Xml;
import android.view.LayoutInflater;
import android.view.View;

import androidx.constraintlayout.core.motion.utils.Easing;
import androidx.constraintlayout.core.widgets.ConstraintWidget;
import androidx.constraintlayout.core.widgets.HelperWidget;
import androidx.constraintlayout.motion.widget.Debug;
import androidx.constraintlayout.motion.widget.MotionLayout;
import androidx.constraintlayout.motion.widget.MotionScene;
import androidx.constraintlayout.widget.ConstraintAttribute.AttributeType;
import androidx.constraintlayout.widget.ConstraintLayout.LayoutParams;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * This class allows you to define programmatically a set of constraints to be used with
 *  {@link ConstraintLayout}.
 * <p>
 * For details about Constraint behaviour see {@link ConstraintLayout}.
 * It lets you create and save constraints, and apply them to an existing ConstraintLayout.
 * ConstraintsSet can be created in various ways:
 * <ul>
 * <li>
 * Manually <br> {@code c = new ConstraintSet(); c.connect(....);}
 * </li>
 * <li>
 * from a R.layout.* object <br> {@code c.clone(context, R.layout.layout1);}
 * </li>
 * <li>
 * from a ConstraintLayout <br> {@code c.clone(constraintLayout);}
 * </li>
 * </ul>
 * <p>
 *  Example code:
 *  <pre>
 *      import android.content.Context;
 *      import android.os.Bundle;
 *      import android.support.constraint.ConstraintLayout;
 *      import android.support.constraint.ConstraintSet;
 *      import android.support.transition.TransitionManager;
 *      import android.support.v7.app.AppCompatActivity;
 *      import android.view.View;
 *
 *      public class MainActivity extends AppCompatActivity {
 *          ConstraintSet mConstraintSet1 = new ConstraintSet(); // create a Constraint Set
 *          ConstraintSet mConstraintSet2 = new ConstraintSet(); // create a Constraint Set
 *          ConstraintLayout mConstraintLayout; // cache the ConstraintLayout
 *          boolean mOld = true;
 *
 *
 *          protected void onCreate(Bundle savedInstanceState) {
 *              super.onCreate(savedInstanceState);
 *              Context context = this;
 *              mConstraintSet2.clone(context, R.layout.state2); // get constraints from layout
 *              setContentView(R.layout.state1);
 *              mConstraintLayout = (ConstraintLayout) findViewById(R.id.activity_main);
 *              mConstraintSet1.clone(mConstraintLayout); // get constraints from ConstraintSet
 *          }
 *
 *          public void foo(View view) {
 *              TransitionManager.beginDelayedTransition(mConstraintLayout);
 *              if (mOld = !mOld) {
 *                  mConstraintSet1.applyTo(mConstraintLayout); // set new constraints
 *              }  else {
 *                  mConstraintSet2.applyTo(mConstraintLayout); // set new constraints
 *              }
 *          }
 *      }
 *  <pre/>
 * <p/>
 */
public class ConstraintSet {
    private static final String TAG = "ConstraintSet";
    private static final String ERROR_MESSAGE = "XML parser error must be within a Constraint ";

    private static final int INTERNAL_MATCH_PARENT = -1;
    private static final int INTERNAL_WRAP_CONTENT = -2;
    private static final int INTERNAL_MATCH_CONSTRAINT = -3;
    private static final int INTERNAL_WRAP_CONTENT_CONSTRAINED = -4;

    private boolean mValidate;
    public String mIdString;
    public String derivedState = "";
    private String [] mMatchLabels = new String[0];
    public static final int ROTATE_NONE = 0;
    public static final int ROTATE_PORTRATE_OF_RIGHT = 1;
    public static final int ROTATE_PORTRATE_OF_LEFT = 2;
    public static final int ROTATE_RIGHT_OF_PORTRATE = 3;
    public static final int ROTATE_LEFT_OF_PORTRATE = 4;
    public int mRotate = 0;
    private HashMap<String, ConstraintAttribute> mSavedAttributes = new HashMap<>();

    /**
     * require that all views have IDs to function
     */
    private boolean mForceId = true;
    /**
     * Used to indicate a parameter is cleared or not set
     */
    public static final int UNSET = LayoutParams.UNSET;

    /**
     * Dimension will be controlled by constraints
     */
    public static final int MATCH_CONSTRAINT = ConstraintLayout.LayoutParams.MATCH_CONSTRAINT;

    /**
     * Dimension will set by the view's content
     */
    public static final int WRAP_CONTENT = ConstraintLayout.LayoutParams.WRAP_CONTENT;

    /**
     * How to calculate the size of a view in 0 dp by using its wrap_content size
     */
    public static final int MATCH_CONSTRAINT_WRAP =
            ConstraintLayout.LayoutParams.MATCH_CONSTRAINT_WRAP;

    /**
     * Calculate the size of a view in 0 dp by reducing the constrains gaps as much as possible
     */
    public static final int MATCH_CONSTRAINT_SPREAD =
            ConstraintLayout.LayoutParams.MATCH_CONSTRAINT_SPREAD;

    public static final int MATCH_CONSTRAINT_PERCENT =
            ConstraintLayout.LayoutParams.MATCH_CONSTRAINT_PERCENT;

    /**
     * References the id of the parent.
     * Used in:
     * <ul>
     * <li>{@link #connect(int, int, int, int, int)}</li>
     * <li>{@link #center(int, int, int, int, int, int, int, float)}</li>
     * </ul>
     */
    public static final int PARENT_ID = ConstraintLayout.LayoutParams.PARENT_ID;

    /**
     * The horizontal orientation.
     */
    public static final int HORIZONTAL = ConstraintLayout.LayoutParams.HORIZONTAL;

    /**
     * The vertical orientation.
     */
    public static final int VERTICAL = ConstraintLayout.LayoutParams.VERTICAL;

    /**
     * Used to create a horizontal create guidelines.
     */
    public static final int HORIZONTAL_GUIDELINE = 0;

    /**
     * Used to create a vertical create guidelines.
     * see {@link #create(int, int)}
     */
    public static final int VERTICAL_GUIDELINE = 1;

    /**
     * This view is visible.
     * Use with {@link #setVisibility} and <a href="#attr_android:visibility">{@code
     * android:visibility}.
     */
    public static final int VISIBLE = View.VISIBLE;

    /**
     * This view is invisible, but it still takes up space for layout purposes.
     * Use with {@link #setVisibility} and <a href="#attr_android:visibility">{@code
     * android:visibility}.
     */
    public static final int INVISIBLE = View.INVISIBLE;

    /**
     * This view is gone, and will not take any space for layout
     * purposes. Use with {@link #setVisibility} and <a href="#attr_android:visibility">{@code
     * android:visibility}.
     */
    public static final int GONE = View.GONE;

    /**
     * The left side of a view.
     */
    public static final int LEFT = ConstraintLayout.LayoutParams.LEFT;

    /**
     * The right side of a view.
     */
    public static final int RIGHT = ConstraintLayout.LayoutParams.RIGHT;

    /**
     * The top of a view.
     */
    public static final int TOP = ConstraintLayout.LayoutParams.TOP;

    /**
     * The bottom side of a view.
     */
    public static final int BOTTOM = ConstraintLayout.LayoutParams.BOTTOM;

    /**
     * The baseline of the text in a view.
     */
    public static final int BASELINE = ConstraintLayout.LayoutParams.BASELINE;

    /**
     * The left side of a view in left to right languages.
     * In right to left languages it corresponds to the right side of the view
     */
    public static final int START = ConstraintLayout.LayoutParams.START;

    /**
     * The right side of a view in right to left languages.
     * In right to left languages it corresponds to the left side of the view
     */
    public static final int END = ConstraintLayout.LayoutParams.END;

    /**
     * Circle reference from a view.
     */
    public static final int CIRCLE_REFERENCE = ConstraintLayout.LayoutParams.CIRCLE;

    /**
     * Chain spread style
     */
    public static final int CHAIN_SPREAD = ConstraintLayout.LayoutParams.CHAIN_SPREAD;

    /**
     * Chain spread inside style
     */
    public static final int CHAIN_SPREAD_INSIDE = ConstraintLayout.LayoutParams.CHAIN_SPREAD_INSIDE;

    public static final int VISIBILITY_MODE_NORMAL = 0;
    public static final int VISIBILITY_MODE_IGNORE = 1;
    /**
     * Chain packed style
     */
    public static final int CHAIN_PACKED = ConstraintLayout.LayoutParams.CHAIN_PACKED;

    private static final boolean DEBUG = false;
    private static final int[] VISIBILITY_FLAGS = new int[]{VISIBLE, INVISIBLE, GONE};
    private static final int BARRIER_TYPE = 1;

    private HashMap<Integer, Constraint> mConstraints = new HashMap<Integer, Constraint>();

    private static SparseIntArray sMapToConstant = new SparseIntArray();
    private static SparseIntArray sOverrideMapToConstant = new SparseIntArray();
    private static final int BASELINE_TO_BASELINE = 1;
    private static final int BOTTOM_MARGIN = 2;
    private static final int BOTTOM_TO_BOTTOM = 3;
    private static final int BOTTOM_TO_TOP = 4;
    private static final int DIMENSION_RATIO = 5;
    private static final int EDITOR_ABSOLUTE_X = 6;
    private static final int EDITOR_ABSOLUTE_Y = 7;
    private static final int END_MARGIN = 8;
    private static final int END_TO_END = 9;
    private static final int END_TO_START = 10;
    private static final int GONE_BOTTOM_MARGIN = 11;
    private static final int GONE_END_MARGIN = 12;
    private static final int GONE_LEFT_MARGIN = 13;
    private static final int GONE_RIGHT_MARGIN = 14;
    private static final int GONE_START_MARGIN = 15;
    private static final int GONE_TOP_MARGIN = 16;
    private static final int GUIDE_BEGIN = 17;
    private static final int GUIDE_END = 18;
    private static final int GUIDE_PERCENT = 19;
    private static final int HORIZONTAL_BIAS = 20;
    private static final int LAYOUT_HEIGHT = 21;
    private static final int LAYOUT_VISIBILITY = 22;
    private static final int LAYOUT_WIDTH = 23;
    private static final int LEFT_MARGIN = 24;
    private static final int LEFT_TO_LEFT = 25;
    private static final int LEFT_TO_RIGHT = 26;
    private static final int ORIENTATION = 27;
    private static final int RIGHT_MARGIN = 28;
    private static final int RIGHT_TO_LEFT = 29;
    private static final int RIGHT_TO_RIGHT = 30;
    private static final int START_MARGIN = 31;
    private static final int START_TO_END = 32;
    private static final int START_TO_START = 33;
    private static final int TOP_MARGIN = 34;
    private static final int TOP_TO_BOTTOM = 35;
    private static final int TOP_TO_TOP = 36;
    private static final int VERTICAL_BIAS = 37;
    private static final int VIEW_ID = 38;
    private static final int HORIZONTAL_WEIGHT = 39;
    private static final int VERTICAL_WEIGHT = 40;
    private static final int HORIZONTAL_STYLE = 41;
    private static final int VERTICAL_STYLE = 42;
    private static final int ALPHA = 43;
    private static final int ELEVATION = 44;
    private static final int ROTATION_X = 45;
    private static final int ROTATION_Y = 46;
    private static final int SCALE_X = 47;
    private static final int SCALE_Y = 48;
    private static final int TRANSFORM_PIVOT_X = 49;
    private static final int TRANSFORM_PIVOT_Y = 50;
    private static final int TRANSLATION_X = 51;
    private static final int TRANSLATION_Y = 52;
    private static final int TRANSLATION_Z = 53;
    private static final int WIDTH_DEFAULT = 54;
    private static final int HEIGHT_DEFAULT = 55;
    private static final int WIDTH_MAX = 56;
    private static final int HEIGHT_MAX = 57;
    private static final int WIDTH_MIN = 58;
    private static final int HEIGHT_MIN = 59;
    private static final int ROTATION = 60;
    private static final int CIRCLE = 61;
    private static final int CIRCLE_RADIUS = 62;
    private static final int CIRCLE_ANGLE = 63;
    private static final int ANIMATE_RELATIVE_TO = 64;
    private static final int TRANSITION_EASING = 65;
    private static final int DRAW_PATH = 66;
    private static final int TRANSITION_PATH_ROTATE = 67;
    private static final int PROGRESS = 68;
    private static final int WIDTH_PERCENT = 69;
    private static final int HEIGHT_PERCENT = 70;
    private static final int CHAIN_USE_RTL = 71;
    private static final int BARRIER_DIRECTION = 72;
    private static final int BARRIER_MARGIN = 73;
    private static final int CONSTRAINT_REFERENCED_IDS = 74;
    private static final int BARRIER_ALLOWS_GONE_WIDGETS = 75;
    private static final int PATH_MOTION_ARC = 76;
    private static final int CONSTRAINT_TAG = 77;
    private static final int VISIBILITY_MODE = 78;
    private static final int MOTION_STAGGER = 79;
    private static final int CONSTRAINED_WIDTH = 80;
    private static final int CONSTRAINED_HEIGHT = 81;
    private static final int ANIMATE_CIRCLE_ANGLE_TO = 82;
    private static final int TRANSFORM_PIVOT_TARGET = 83;
    private static final int QUANTIZE_MOTION_STEPS = 84;
    private static final int QUANTIZE_MOTION_PHASE = 85;
    private static final int QUANTIZE_MOTION_INTERPOLATOR = 86;
    private static final int UNUSED = 87;
    private static final int QUANTIZE_MOTION_INTERPOLATOR_TYPE = 88;
    private static final int QUANTIZE_MOTION_INTERPOLATOR_ID = 89;
    private static final int QUANTIZE_MOTION_INTERPOLATOR_STR = 90;
    private static final int BASELINE_TO_TOP = 91;
    private static final int BASELINE_TO_BOTTOM = 92;
    private static final int BASELINE_MARGIN = 93;
    private static final int GONE_BASELINE_MARGIN = 94;
    private static final int LAYOUT_CONSTRAINT_WIDTH = 95;
    private static final int LAYOUT_CONSTRAINT_HEIGHT = 96;
    private static final int LAYOUT_WRAP_BEHAVIOR = 97;
    private static final int MOTION_TARGET = 98;
    private static final int GUIDELINE_USE_RTL = 99;

    private static final String KEY_WEIGHT = "weight";
    private static final String KEY_RATIO = "ratio";
    private static final String KEY_PERCENT_PARENT = "parent";


    static {
        sMapToConstant.append(R.styleable.Constraint_layout_constraintLeft_toLeftOf, LEFT_TO_LEFT);
        sMapToConstant.append(R.styleable.Constraint_layout_constraintLeft_toRightOf,
                LEFT_TO_RIGHT);
        sMapToConstant.append(R.styleable.Constraint_layout_constraintRight_toLeftOf,
                RIGHT_TO_LEFT);
        sMapToConstant.append(
                R.styleable.Constraint_layout_constraintRight_toRightOf, RIGHT_TO_RIGHT);
        sMapToConstant.append(R.styleable.Constraint_layout_constraintTop_toTopOf, TOP_TO_TOP);
        sMapToConstant.append(R.styleable.Constraint_layout_constraintTop_toBottomOf,
                TOP_TO_BOTTOM);
        sMapToConstant.append(R.styleable.Constraint_layout_constraintBottom_toTopOf,
                BOTTOM_TO_TOP);
        sMapToConstant.append(
                R.styleable.Constraint_layout_constraintBottom_toBottomOf, BOTTOM_TO_BOTTOM);
        sMapToConstant.append(
                R.styleable.Constraint_layout_constraintBaseline_toBaselineOf,
                BASELINE_TO_BASELINE);
        sMapToConstant.append(
                R.styleable.Constraint_layout_constraintBaseline_toTopOf, BASELINE_TO_TOP);
        sMapToConstant.append(
                R.styleable.Constraint_layout_constraintBaseline_toBottomOf, BASELINE_TO_BOTTOM);

        sMapToConstant.append(R.styleable.Constraint_layout_editor_absoluteX, EDITOR_ABSOLUTE_X);
        sMapToConstant.append(R.styleable.Constraint_layout_editor_absoluteY, EDITOR_ABSOLUTE_Y);
        sMapToConstant.append(R.styleable.Constraint_layout_constraintGuide_begin, GUIDE_BEGIN);
        sMapToConstant.append(R.styleable.Constraint_layout_constraintGuide_end, GUIDE_END);
        sMapToConstant.append(R.styleable.Constraint_layout_constraintGuide_percent, GUIDE_PERCENT);
        sMapToConstant.append(R.styleable.Constraint_guidelineUseRtl, GUIDELINE_USE_RTL);

        sMapToConstant.append(R.styleable.Constraint_android_orientation, ORIENTATION);
        sMapToConstant.append(R.styleable.Constraint_layout_constraintStart_toEndOf, START_TO_END);
        sMapToConstant.append(
                R.styleable.Constraint_layout_constraintStart_toStartOf, START_TO_START);
        sMapToConstant.append(R.styleable.Constraint_layout_constraintEnd_toStartOf, END_TO_START);
        sMapToConstant.append(R.styleable.Constraint_layout_constraintEnd_toEndOf, END_TO_END);
        sMapToConstant.append(R.styleable.Constraint_layout_goneMarginLeft, GONE_LEFT_MARGIN);
        sMapToConstant.append(R.styleable.Constraint_layout_goneMarginTop, GONE_TOP_MARGIN);
        sMapToConstant.append(R.styleable.Constraint_layout_goneMarginRight, GONE_RIGHT_MARGIN);
        sMapToConstant.append(R.styleable.Constraint_layout_goneMarginBottom, GONE_BOTTOM_MARGIN);
        sMapToConstant.append(R.styleable.Constraint_layout_goneMarginStart, GONE_START_MARGIN);
        sMapToConstant.append(R.styleable.Constraint_layout_goneMarginEnd, GONE_END_MARGIN);
        sMapToConstant.append(
                R.styleable.Constraint_layout_constraintVertical_weight, VERTICAL_WEIGHT);
        sMapToConstant.append(
                R.styleable.Constraint_layout_constraintHorizontal_weight, HORIZONTAL_WEIGHT);
        sMapToConstant.append(
                R.styleable.Constraint_layout_constraintHorizontal_chainStyle, HORIZONTAL_STYLE);
        sMapToConstant.append(
                R.styleable.Constraint_layout_constraintVertical_chainStyle, VERTICAL_STYLE);

        sMapToConstant.append(
                R.styleable.Constraint_layout_constraintHorizontal_bias, HORIZONTAL_BIAS);
        sMapToConstant.append(
                R.styleable.Constraint_layout_constraintVertical_bias, VERTICAL_BIAS);
        sMapToConstant.append(
                R.styleable.Constraint_layout_constraintDimensionRatio, DIMENSION_RATIO);
        sMapToConstant.append(R.styleable.Constraint_layout_constraintLeft_creator, UNUSED);
        sMapToConstant.append(R.styleable.Constraint_layout_constraintTop_creator, UNUSED);
        sMapToConstant.append(R.styleable.Constraint_layout_constraintRight_creator, UNUSED);
        sMapToConstant.append(R.styleable.Constraint_layout_constraintBottom_creator, UNUSED);
        sMapToConstant.append(R.styleable.Constraint_layout_constraintBaseline_creator, UNUSED);
        sMapToConstant.append(R.styleable.Constraint_android_layout_marginLeft, LEFT_MARGIN);
        sMapToConstant.append(R.styleable.Constraint_android_layout_marginRight, RIGHT_MARGIN);
        sMapToConstant.append(R.styleable.Constraint_android_layout_marginStart, START_MARGIN);
        sMapToConstant.append(R.styleable.Constraint_android_layout_marginEnd, END_MARGIN);
        sMapToConstant.append(R.styleable.Constraint_android_layout_marginTop, TOP_MARGIN);
        sMapToConstant.append(R.styleable.Constraint_android_layout_marginBottom, BOTTOM_MARGIN);
        sMapToConstant.append(R.styleable.Constraint_android_layout_width, LAYOUT_WIDTH);
        sMapToConstant.append(
                R.styleable.Constraint_android_layout_height, LAYOUT_HEIGHT);
        sMapToConstant.append(
                R.styleable.Constraint_layout_constraintWidth, LAYOUT_CONSTRAINT_WIDTH);
        sMapToConstant.append(
                R.styleable.Constraint_layout_constraintHeight, LAYOUT_CONSTRAINT_HEIGHT);
        sMapToConstant.append(R.styleable.Constraint_android_visibility, LAYOUT_VISIBILITY);
        sMapToConstant.append(R.styleable.Constraint_android_alpha, ALPHA);
        sMapToConstant.append(R.styleable.Constraint_android_elevation, ELEVATION);
        sMapToConstant.append(R.styleable.Constraint_android_rotationX, ROTATION_X);
        sMapToConstant.append(R.styleable.Constraint_android_rotationY, ROTATION_Y);
        sMapToConstant.append(R.styleable.Constraint_android_rotation, ROTATION);
        sMapToConstant.append(R.styleable.Constraint_android_scaleX, SCALE_X);
        sMapToConstant.append(R.styleable.Constraint_android_scaleY, SCALE_Y);
        sMapToConstant.append(R.styleable.Constraint_android_transformPivotX, TRANSFORM_PIVOT_X);
        sMapToConstant.append(R.styleable.Constraint_android_transformPivotY, TRANSFORM_PIVOT_Y);
        sMapToConstant.append(R.styleable.Constraint_android_translationX, TRANSLATION_X);
        sMapToConstant.append(R.styleable.Constraint_android_translationY, TRANSLATION_Y);
        sMapToConstant.append(R.styleable.Constraint_android_translationZ, TRANSLATION_Z);
        sMapToConstant.append(R.styleable.Constraint_layout_constraintWidth_default, WIDTH_DEFAULT);
        sMapToConstant.append(
                R.styleable.Constraint_layout_constraintHeight_default, HEIGHT_DEFAULT);
        sMapToConstant.append(R.styleable.Constraint_layout_constraintWidth_max, WIDTH_MAX);
        sMapToConstant.append(R.styleable.Constraint_layout_constraintHeight_max, HEIGHT_MAX);
        sMapToConstant.append(R.styleable.Constraint_layout_constraintWidth_min, WIDTH_MIN);
        sMapToConstant.append(R.styleable.Constraint_layout_constraintHeight_min, HEIGHT_MIN);
        sMapToConstant.append(R.styleable.Constraint_layout_constraintCircle, CIRCLE);
        sMapToConstant.append(R.styleable.Constraint_layout_constraintCircleRadius, CIRCLE_RADIUS);
        sMapToConstant.append(R.styleable.Constraint_layout_constraintCircleAngle, CIRCLE_ANGLE);
        sMapToConstant.append(R.styleable.Constraint_animateRelativeTo, ANIMATE_RELATIVE_TO);
        sMapToConstant.append(R.styleable.Constraint_transitionEasing, TRANSITION_EASING);
        sMapToConstant.append(R.styleable.Constraint_drawPath, DRAW_PATH);
        sMapToConstant.append(R.styleable.Constraint_transitionPathRotate, TRANSITION_PATH_ROTATE);
        sMapToConstant.append(R.styleable.Constraint_motionStagger, MOTION_STAGGER);
        sMapToConstant.append(R.styleable.Constraint_android_id, VIEW_ID);
        sMapToConstant.append(R.styleable.Constraint_motionProgress, PROGRESS);
        sMapToConstant.append(
                R.styleable.Constraint_layout_constraintWidth_percent, WIDTH_PERCENT);
        sMapToConstant.append(
                R.styleable.Constraint_layout_constraintHeight_percent, HEIGHT_PERCENT);
        sMapToConstant.append(
                R.styleable.Constraint_layout_wrapBehaviorInParent, LAYOUT_WRAP_BEHAVIOR);

        sMapToConstant.append(R.styleable.Constraint_chainUseRtl, CHAIN_USE_RTL);
        sMapToConstant.append(R.styleable.Constraint_barrierDirection, BARRIER_DIRECTION);
        sMapToConstant.append(R.styleable.Constraint_barrierMargin, BARRIER_MARGIN);
        sMapToConstant.append(
                R.styleable.Constraint_constraint_referenced_ids, CONSTRAINT_REFERENCED_IDS);
        sMapToConstant.append(
                R.styleable.Constraint_barrierAllowsGoneWidgets, BARRIER_ALLOWS_GONE_WIDGETS);
        sMapToConstant.append(R.styleable.Constraint_pathMotionArc, PATH_MOTION_ARC);
        sMapToConstant.append(R.styleable.Constraint_layout_constraintTag, CONSTRAINT_TAG);
        sMapToConstant.append(R.styleable.Constraint_visibilityMode, VISIBILITY_MODE);
        sMapToConstant.append(
                R.styleable.Constraint_layout_constrainedWidth, CONSTRAINED_WIDTH);
        sMapToConstant.append(
                R.styleable.Constraint_layout_constrainedHeight, CONSTRAINED_HEIGHT);
        sMapToConstant.append(
                R.styleable.Constraint_polarRelativeTo, ANIMATE_CIRCLE_ANGLE_TO);
        sMapToConstant.append(
                R.styleable.Constraint_transformPivotTarget, TRANSFORM_PIVOT_TARGET);
        sMapToConstant.append(
                R.styleable.Constraint_quantizeMotionSteps, QUANTIZE_MOTION_STEPS);
        sMapToConstant.append(
                R.styleable.Constraint_quantizeMotionPhase, QUANTIZE_MOTION_PHASE);
        sMapToConstant.append(
                R.styleable.Constraint_quantizeMotionInterpolator, QUANTIZE_MOTION_INTERPOLATOR);


        /*
        The tags not available in constraintOverride
        Left here to help with documentation and understanding
        overrideMapToConstant.append(
        R.styleable.ConstraintOverride_layout_constraintLeft_toLeftOf, LEFT_TO_LEFT);
        overrideMapToConstant.append(
        R.styleable.ConstraintOverride_layout_constraintLeft_toRightOf, LEFT_TO_RIGHT);
        overrideMapToConstant.append(
        R.styleable.ConstraintOverride_layout_constraintRight_toLeftOf, RIGHT_TO_LEFT);
        overrideMapToConstant.append(
        R.styleable.ConstraintOverride_layout_constraintRight_toRightOf, RIGHT_TO_RIGHT);
        overrideMapToConstant.append(
        R.styleable.ConstraintOverride_layout_constraintTop_toTopOf, TOP_TO_TOP);
        overrideMapToConstant.append(
        R.styleable.ConstraintOverride_layout_constraintTop_toBottomOf, TOP_TO_BOTTOM);
        overrideMapToConstant.append(
        R.styleable.ConstraintOverride_layout_constraintBottom_toTopOf, BOTTOM_TO_TOP);
        overrideMapToConstant.append(
        R.styleable.ConstraintOverride_layout_constraintBottom_toBottomOf, BOTTOM_TO_BOTTOM);
        overrideMapToConstant.append(
        R.styleable.ConstraintOverride_layout_constraintBaseline_toBaselineOf,
        BASELINE_TO_BASELINE);
        overrideMapToConstant.append(
        R.styleable.ConstraintOverride_layout_constraintGuide_begin, GUIDE_BEGIN);
        overrideMapToConstant.append(
        R.styleable.ConstraintOverride_layout_constraintGuide_end, GUIDE_END);
        overrideMapToConstant.append(
        R.styleable.ConstraintOverride_layout_constraintGuide_percent, GUIDE_PERCENT);
        overrideMapToConstant.append(
        R.styleable.ConstraintOverride_layout_constraintStart_toEndOf, START_TO_END);
        overrideMapToConstant.append(
        R.styleable.ConstraintOverride_layout_constraintStart_toStartOf, START_TO_START);
        overrideMapToConstant.append(
        R.styleable.ConstraintOverride_layout_constraintEnd_toStartOf, END_TO_START);
        overrideMapToConstant.append(
        R.styleable.ConstraintOverride_layout_constraintEnd_toEndOf, END_TO_END);
        */
        sOverrideMapToConstant.append(
                R.styleable.ConstraintOverride_layout_editor_absoluteY, EDITOR_ABSOLUTE_X);
        sOverrideMapToConstant.append(
                R.styleable.ConstraintOverride_layout_editor_absoluteY, EDITOR_ABSOLUTE_Y);
        sOverrideMapToConstant.append(
                R.styleable.ConstraintOverride_android_orientation, ORIENTATION);
        sOverrideMapToConstant.append(
                R.styleable.ConstraintOverride_layout_goneMarginLeft, GONE_LEFT_MARGIN);
        sOverrideMapToConstant.append(
                R.styleable.ConstraintOverride_layout_goneMarginTop, GONE_TOP_MARGIN);
        sOverrideMapToConstant.append(
                R.styleable.ConstraintOverride_layout_goneMarginRight, GONE_RIGHT_MARGIN);
        sOverrideMapToConstant.append(
                R.styleable.ConstraintOverride_layout_goneMarginBottom, GONE_BOTTOM_MARGIN);
        sOverrideMapToConstant.append(
                R.styleable.ConstraintOverride_layout_goneMarginStart, GONE_START_MARGIN);
        sOverrideMapToConstant.append(
                R.styleable.ConstraintOverride_layout_goneMarginEnd, GONE_END_MARGIN);
        sOverrideMapToConstant.append(
                R.styleable.ConstraintOverride_layout_constraintVertical_weight,
                VERTICAL_WEIGHT);
        sOverrideMapToConstant.append(
                R.styleable.ConstraintOverride_layout_constraintHorizontal_weight,
                HORIZONTAL_WEIGHT);
        sOverrideMapToConstant.append(
                R.styleable.ConstraintOverride_layout_constraintHorizontal_chainStyle,
                HORIZONTAL_STYLE);
        sOverrideMapToConstant.append(
                R.styleable.ConstraintOverride_layout_constraintVertical_chainStyle,
                VERTICAL_STYLE);

        sOverrideMapToConstant.append(
                R.styleable.ConstraintOverride_layout_constraintHorizontal_bias,
                HORIZONTAL_BIAS);
        sOverrideMapToConstant.append(R.styleable.ConstraintOverride_layout_constraintVertical_bias,
                VERTICAL_BIAS);
        sOverrideMapToConstant.append(
                R.styleable.ConstraintOverride_layout_constraintDimensionRatio, DIMENSION_RATIO);
        sOverrideMapToConstant.append(R.styleable.ConstraintOverride_layout_constraintLeft_creator,
                UNUSED);
        sOverrideMapToConstant.append(R.styleable.ConstraintOverride_layout_constraintTop_creator,
                UNUSED);
        sOverrideMapToConstant.append(R.styleable.ConstraintOverride_layout_constraintRight_creator,
                UNUSED);
        sOverrideMapToConstant.append(
                R.styleable.ConstraintOverride_layout_constraintBottom_creator, UNUSED);
        sOverrideMapToConstant.append(
                R.styleable.ConstraintOverride_layout_constraintBaseline_creator,
                UNUSED);
        sOverrideMapToConstant.append(R.styleable.ConstraintOverride_android_layout_marginLeft,
                LEFT_MARGIN);
        sOverrideMapToConstant.append(R.styleable.ConstraintOverride_android_layout_marginRight,
                RIGHT_MARGIN);
        sOverrideMapToConstant.append(R.styleable.ConstraintOverride_android_layout_marginStart,
                START_MARGIN);
        sOverrideMapToConstant.append(R.styleable.ConstraintOverride_android_layout_marginEnd,
                END_MARGIN);
        sOverrideMapToConstant.append(R.styleable.ConstraintOverride_android_layout_marginTop,
                TOP_MARGIN);
        sOverrideMapToConstant.append(R.styleable.ConstraintOverride_android_layout_marginBottom,
                BOTTOM_MARGIN);
        sOverrideMapToConstant.append(R.styleable.ConstraintOverride_android_layout_width,
                LAYOUT_WIDTH);
        sOverrideMapToConstant.append(R.styleable.ConstraintOverride_android_layout_height,
                LAYOUT_HEIGHT);
        sOverrideMapToConstant.append(R.styleable.ConstraintOverride_layout_constraintWidth,
                LAYOUT_CONSTRAINT_WIDTH);
        sOverrideMapToConstant.append(R.styleable.ConstraintOverride_layout_constraintHeight,
                LAYOUT_CONSTRAINT_HEIGHT);
        sOverrideMapToConstant.append(R.styleable.ConstraintOverride_android_visibility,
                LAYOUT_VISIBILITY);
        sOverrideMapToConstant.append(R.styleable.ConstraintOverride_android_alpha, ALPHA);
        sOverrideMapToConstant.append(R.styleable.ConstraintOverride_android_elevation, ELEVATION);
        sOverrideMapToConstant.append(R.styleable.ConstraintOverride_android_rotationX, ROTATION_X);
        sOverrideMapToConstant.append(R.styleable.ConstraintOverride_android_rotationY, ROTATION_Y);
        sOverrideMapToConstant.append(R.styleable.ConstraintOverride_android_rotation, ROTATION);
        sOverrideMapToConstant.append(R.styleable.ConstraintOverride_android_scaleX, SCALE_X);
        sOverrideMapToConstant.append(R.styleable.ConstraintOverride_android_scaleY, SCALE_Y);
        sOverrideMapToConstant.append(R.styleable.ConstraintOverride_android_transformPivotX,
                TRANSFORM_PIVOT_X);
        sOverrideMapToConstant.append(R.styleable.ConstraintOverride_android_transformPivotY,
                TRANSFORM_PIVOT_Y);
        sOverrideMapToConstant.append(R.styleable.ConstraintOverride_android_translationX,
                TRANSLATION_X);
        sOverrideMapToConstant.append(R.styleable.ConstraintOverride_android_translationY,
                TRANSLATION_Y);
        sOverrideMapToConstant.append(R.styleable.ConstraintOverride_android_translationZ,
                TRANSLATION_Z);
        sOverrideMapToConstant.append(R.styleable.ConstraintOverride_layout_constraintWidth_default,
                WIDTH_DEFAULT);
        sOverrideMapToConstant.append(
                R.styleable.ConstraintOverride_layout_constraintHeight_default, HEIGHT_DEFAULT);
        sOverrideMapToConstant.append(R.styleable.ConstraintOverride_layout_constraintWidth_max,
                WIDTH_MAX);
        sOverrideMapToConstant.append(R.styleable.ConstraintOverride_layout_constraintHeight_max,
                HEIGHT_MAX);
        sOverrideMapToConstant.append(R.styleable.ConstraintOverride_layout_constraintWidth_min,
                WIDTH_MIN);
        sOverrideMapToConstant.append(R.styleable.ConstraintOverride_layout_constraintHeight_min,
                HEIGHT_MIN);
        sOverrideMapToConstant.append(R.styleable.ConstraintOverride_layout_constraintCircleRadius,
                CIRCLE_RADIUS);
        sOverrideMapToConstant.append(R.styleable.ConstraintOverride_layout_constraintCircleAngle,
                CIRCLE_ANGLE);
        sOverrideMapToConstant.append(R.styleable.ConstraintOverride_animateRelativeTo,
                ANIMATE_RELATIVE_TO);
        sOverrideMapToConstant.append(R.styleable.ConstraintOverride_transitionEasing,
                TRANSITION_EASING);
        sOverrideMapToConstant.append(R.styleable.ConstraintOverride_drawPath, DRAW_PATH);
        sOverrideMapToConstant.append(R.styleable.ConstraintOverride_transitionPathRotate,
                TRANSITION_PATH_ROTATE);
        sOverrideMapToConstant.append(R.styleable.ConstraintOverride_motionStagger, MOTION_STAGGER);
        sOverrideMapToConstant.append(R.styleable.ConstraintOverride_android_id, VIEW_ID);
        sOverrideMapToConstant.append(R.styleable.ConstraintOverride_motionTarget, MOTION_TARGET);

        sOverrideMapToConstant.append(R.styleable.ConstraintOverride_motionProgress, PROGRESS);
        sOverrideMapToConstant.append(R.styleable.ConstraintOverride_layout_constraintWidth_percent,
                WIDTH_PERCENT);
        sOverrideMapToConstant.append(
                R.styleable.ConstraintOverride_layout_constraintHeight_percent, HEIGHT_PERCENT);

        sOverrideMapToConstant.append(R.styleable.ConstraintOverride_chainUseRtl, CHAIN_USE_RTL);
        sOverrideMapToConstant.append(R.styleable.ConstraintOverride_barrierDirection,
                BARRIER_DIRECTION);
        sOverrideMapToConstant.append(R.styleable.ConstraintOverride_barrierMargin,
                BARRIER_MARGIN);
        sOverrideMapToConstant.append(R.styleable.ConstraintOverride_constraint_referenced_ids,
                CONSTRAINT_REFERENCED_IDS);
        sOverrideMapToConstant.append(R.styleable.ConstraintOverride_barrierAllowsGoneWidgets,
                BARRIER_ALLOWS_GONE_WIDGETS);
        sOverrideMapToConstant.append(R.styleable.ConstraintOverride_pathMotionArc,
                PATH_MOTION_ARC);
        sOverrideMapToConstant.append(R.styleable.ConstraintOverride_layout_constraintTag,
                CONSTRAINT_TAG);
        sOverrideMapToConstant.append(R.styleable.ConstraintOverride_visibilityMode,
                VISIBILITY_MODE);
        sOverrideMapToConstant.append(R.styleable.ConstraintOverride_layout_constrainedWidth,
                CONSTRAINED_WIDTH);
        sOverrideMapToConstant.append(R.styleable.ConstraintOverride_layout_constrainedHeight,
                CONSTRAINED_HEIGHT);
        sOverrideMapToConstant.append(R.styleable.ConstraintOverride_polarRelativeTo,
                ANIMATE_CIRCLE_ANGLE_TO);
        sOverrideMapToConstant.append(R.styleable.ConstraintOverride_transformPivotTarget,
                TRANSFORM_PIVOT_TARGET);
        sOverrideMapToConstant.append(R.styleable.ConstraintOverride_quantizeMotionSteps,
                QUANTIZE_MOTION_STEPS);
        sOverrideMapToConstant.append(R.styleable.ConstraintOverride_quantizeMotionPhase,
                QUANTIZE_MOTION_PHASE);
        sOverrideMapToConstant.append(R.styleable.ConstraintOverride_quantizeMotionInterpolator,
                QUANTIZE_MOTION_INTERPOLATOR);
        sOverrideMapToConstant.append(R.styleable.ConstraintOverride_layout_wrapBehaviorInParent,
                LAYOUT_WRAP_BEHAVIOR);

    }

    public HashMap<String, ConstraintAttribute> getCustomAttributeSet() {
        return mSavedAttributes;
    }

    // @TODO: add description

    /**
     *
     * @param mId
     * @return
     */
    public Constraint getParameters(int mId) {
        return get(mId);
    }

    /**
     * This will copy Constraints from the ConstraintSet
     *
     * @param set
     */
    public void readFallback(ConstraintSet set) {

        for (Integer key : set.mConstraints.keySet()) {
            int id = key;
            Constraint parent = set.mConstraints.get(key);

            if (!mConstraints.containsKey(id)) {
                mConstraints.put(id, new Constraint());
            }
            Constraint constraint = mConstraints.get(id);
            if (constraint == null) {
                continue;
            }
            if (!constraint.layout.mApply) {
                constraint.layout.copyFrom(parent.layout);
            }
            if (!constraint.propertySet.mApply) {
                constraint.propertySet.copyFrom(parent.propertySet);
            }
            if (!constraint.transform.mApply) {
                constraint.transform.copyFrom(parent.transform);
            }
            if (!constraint.motion.mApply) {
                constraint.motion.copyFrom(parent.motion);
            }
            for (String s : parent.mCustomConstraints.keySet()) {
                if (!constraint.mCustomConstraints.containsKey(s)) {
                    constraint.mCustomConstraints.put(s, parent.mCustomConstraints.get(s));
                }
            }
        }
    }

    /**
     * This will copy Constraints from the ConstraintLayout if it does not have parameters
     *
     * @param constraintLayout
     */
    public void readFallback(ConstraintLayout constraintLayout) {
        int count = constraintLayout.getChildCount();
        for (int i = 0; i < count; i++) {
            View view = constraintLayout.getChildAt(i);
            ConstraintLayout.LayoutParams param =
                    (ConstraintLayout.LayoutParams) view.getLayoutParams();

            int id = view.getId();
            if (mForceId && id == -1) {
                throw new RuntimeException("All children of ConstraintLayout "
                        + "must have ids to use ConstraintSet");
            }
            if (!mConstraints.containsKey(id)) {
                mConstraints.put(id, new Constraint());
            }
            Constraint constraint = mConstraints.get(id);
            if (constraint == null) {
                continue;
            }
            if (!constraint.layout.mApply) {
                constraint.fillFrom(id, param);
                if (view instanceof ConstraintHelper) {
                    constraint.layout.mReferenceIds = ((ConstraintHelper) view).getReferencedIds();
                    if (view instanceof Barrier) {
                        Barrier barrier = (Barrier) view;
                        constraint.layout.mBarrierAllowsGoneWidgets = barrier.getAllowsGoneWidget();
                        constraint.layout.mBarrierDirection = barrier.getType();
                        constraint.layout.mBarrierMargin = barrier.getMargin();
                    }
                }
                constraint.layout.mApply = true;
            }
            if (!constraint.propertySet.mApply) {
                constraint.propertySet.visibility = view.getVisibility();
                constraint.propertySet.alpha = view.getAlpha();
                constraint.propertySet.mApply = true;
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {

                if (!constraint.transform.mApply) {
                    constraint.transform.mApply = true;
                    constraint.transform.rotation = view.getRotation();
                    constraint.transform.rotationX = view.getRotationX();
                    constraint.transform.rotationY = view.getRotationY();
                    constraint.transform.scaleX = view.getScaleX();
                    constraint.transform.scaleY = view.getScaleY();

                    float pivotX = view.getPivotX(); // we assume it is not set if set to 0.0
                    float pivotY = view.getPivotY(); // we assume it is not set if set to 0.0

                    if (pivotX != 0.0 || pivotY != 0.0) {
                        constraint.transform.transformPivotX = pivotX;
                        constraint.transform.transformPivotY = pivotY;
                    }

                    constraint.transform.translationX = view.getTranslationX();
                    constraint.transform.translationY = view.getTranslationY();
                    if (Build.VERSION.SDK_INT >= VERSION_CODES.LOLLIPOP) {
                        constraint.transform.translationZ = view.getTranslationZ();
                        if (constraint.transform.applyElevation) {
                            constraint.transform.elevation = view.getElevation();
                        }
                    }
                }
            }
        }

    }

    /**
     * Get the delta form the ConstraintSet and aplly to this
     * @param cs
     */
    public void applyDeltaFrom(ConstraintSet cs) {
        for (Constraint from : cs.mConstraints.values()) {
            if (from.mDelta == null) {
                continue;
            }
            if (from.mTargetString == null) {
                Constraint constraint = getConstraint(from.mViewId);
                from.mDelta.applyDelta(constraint);
                continue;
            }
            for (int key : mConstraints.keySet()) {
                Constraint potential = getConstraint(key);
                if (potential.layout.mConstraintTag == null) {
                    continue;
                }
                if (from.mTargetString.matches(potential.layout.mConstraintTag)) {
                    from.mDelta.applyDelta(potential);

                    @SuppressWarnings("unchecked")
                    HashMap<String, ConstraintAttribute> fromClone =
                            (HashMap<String, ConstraintAttribute>) from.mCustomConstraints.clone();
                    potential.mCustomConstraints.putAll(fromClone);
                }
            }
        }
    }

    /**
     * Parse the constraint dimension attribute
     *
     * @param a
     * @param attr
     * @param orientation
     */
    static void parseDimensionConstraints(Object data, TypedArray a, int attr, int orientation) {
        if (data == null) {
            return;
        }
        // data can be of:
        //
        // ConstraintLayout.LayoutParams
        // ConstraintSet.Layout
        // Constraint.Delta

        TypedValue v = a.peekValue(attr);
        int type = v.type;
        int finalValue = 0;
        boolean finalConstrained = false;
        switch (type) {
            case TypedValue.TYPE_DIMENSION: {
                finalValue = a.getDimensionPixelSize(attr, 0);
            }
            break;
            case TypedValue.TYPE_STRING: {
                String value = a.getString(attr);
                parseDimensionConstraintsString(data, value, orientation);
                return;
            }
            default: {
                int value = a.getInt(attr, 0);
                switch (value) {
                    case INTERNAL_WRAP_CONTENT:
                    case INTERNAL_MATCH_PARENT: {
                        finalValue = value;
                    }
                        break;
                    case INTERNAL_MATCH_CONSTRAINT: {
                        finalValue = MATCH_CONSTRAINT;
                    }
                    break;
                    case INTERNAL_WRAP_CONTENT_CONSTRAINED: {
                        finalValue = WRAP_CONTENT;
                        finalConstrained = true;
                    }
                    break;
                }
            }
        }

        if (data instanceof ConstraintLayout.LayoutParams) {
            ConstraintLayout.LayoutParams params = (ConstraintLayout.LayoutParams) data;
            if (orientation == HORIZONTAL) {
                params.width = finalValue;
                params.constrainedWidth = finalConstrained;
            } else {
                params.height = finalValue;
                params.constrainedHeight = finalConstrained;
            }
        } else if (data instanceof Layout) {
            Layout params = (Layout) data;
            if (orientation == HORIZONTAL) {
                params.mWidth = finalValue;
                params.constrainedWidth = finalConstrained;
            } else {
                params.mHeight = finalValue;
                params.constrainedHeight = finalConstrained;
            }
        } else if (data instanceof Constraint.Delta) {
            Constraint.Delta params = (Constraint.Delta) data;
            if (orientation == HORIZONTAL) {
                params.add(LAYOUT_WIDTH, finalValue);
                params.add(CONSTRAINED_WIDTH, finalConstrained);
            } else {
                params.add(LAYOUT_HEIGHT, finalValue);
                params.add(CONSTRAINED_HEIGHT, finalConstrained);
            }
        }
    }

    /**
     * Parse the dimension ratio string
     *
     * @param value
     */
    static void parseDimensionRatioString(ConstraintLayout.LayoutParams params, String value) {
        String dimensionRatio = value;
        float dimensionRatioValue = Float.NaN;
        int dimensionRatioSide = UNSET;
        if (dimensionRatio != null) {
            int len = dimensionRatio.length();
            int commaIndex = dimensionRatio.indexOf(',');
            if (commaIndex > 0 && commaIndex < len - 1) {
                String dimension = dimensionRatio.substring(0, commaIndex);
                if (dimension.equalsIgnoreCase("W")) {
                    dimensionRatioSide = HORIZONTAL;
                } else if (dimension.equalsIgnoreCase("H")) {
                    dimensionRatioSide = VERTICAL;
                }
                commaIndex++;
            } else {
                commaIndex = 0;
            }
            int colonIndex = dimensionRatio.indexOf(':');
            if (colonIndex >= 0 && colonIndex < len - 1) {
                String nominator = dimensionRatio.substring(commaIndex, colonIndex);
                String denominator = dimensionRatio.substring(colonIndex + 1);
                if (nominator.length() > 0 && denominator.length() > 0) {
                    try {
                        float nominatorValue = Float.parseFloat(nominator);
                        float denominatorValue = Float.parseFloat(denominator);
                        if (nominatorValue > 0 && denominatorValue > 0) {
                            if (dimensionRatioSide == VERTICAL) {
                                dimensionRatioValue = Math.abs(denominatorValue / nominatorValue);
                            } else {
                                dimensionRatioValue = Math.abs(nominatorValue / denominatorValue);
                            }
                        }
                    } catch (NumberFormatException e) {
                        // Ignore
                    }
                }
            } else {
                String r = dimensionRatio.substring(commaIndex);
                if (r.length() > 0) {
                    try {
                        dimensionRatioValue = Float.parseFloat(r);
                    } catch (NumberFormatException e) {
                        // Ignore
                    }
                }
            }
        }
        params.dimensionRatio = dimensionRatio;
        params.mDimensionRatioValue = dimensionRatioValue;
        params.mDimensionRatioSide = dimensionRatioSide;
    }

    /**
     * Parse the constraints string dimension
     *
     * @param value
     * @param orientation
     */
    static void parseDimensionConstraintsString(Object data, String value, int orientation) {
        // data can be of:
        //
        // ConstraintLayout.LayoutParams
        // ConstraintSet.Layout
        // Constraint.Delta

        // String should be of the form
        //
        // "<Key>=<Value>"
        // supported Keys are:
        // "weight=<value>"
        // "ratio=<value>"
        // "parent=<value>"
        if (value == null) {
            return;
        }

        int equalIndex = value.indexOf('=');
        int len = value.length();
        if (equalIndex > 0 && equalIndex < len - 1) {
            String key = value.substring(0, equalIndex);
            String val = value.substring(equalIndex + 1);
            if (val.length() > 0) {
                key = key.trim();
                val = val.trim();
                if (KEY_RATIO.equalsIgnoreCase(key)) {
                    if (data instanceof ConstraintLayout.LayoutParams) {
                        ConstraintLayout.LayoutParams params = (ConstraintLayout.LayoutParams) data;
                        if (orientation == HORIZONTAL) {
                            params.width = MATCH_CONSTRAINT;
                        } else {
                            params.height = MATCH_CONSTRAINT;
                        }
                        parseDimensionRatioString(params, val);
                    } else if (data instanceof Layout) {
                        Layout params = (Layout) data;
                        params.dimensionRatio = val;
                    } else if (data instanceof Constraint.Delta) {
                        Constraint.Delta params = (Constraint.Delta) data;
                        params.add(DIMENSION_RATIO, val);
                    }
                } else if (KEY_WEIGHT.equalsIgnoreCase(key)) {
                    try {
                        float weight = Float.parseFloat(val);
                        if (data instanceof ConstraintLayout.LayoutParams) {
                            ConstraintLayout.LayoutParams params =
                                    (ConstraintLayout.LayoutParams) data;
                            if (orientation == HORIZONTAL) {
                                params.width = MATCH_CONSTRAINT;
                                params.horizontalWeight = weight;
                            } else {
                                params.height = MATCH_CONSTRAINT;
                                params.verticalWeight = weight;
                            }
                        } else if (data instanceof Layout) {
                            Layout params = (Layout) data;
                            if (orientation == HORIZONTAL) {
                                params.mWidth = MATCH_CONSTRAINT;
                                params.horizontalWeight = weight;
                            } else {
                                params.mHeight = MATCH_CONSTRAINT;
                                params.verticalWeight = weight;
                            }
                        } else if (data instanceof Constraint.Delta) {
                            Constraint.Delta params = (Constraint.Delta) data;
                            if (orientation == HORIZONTAL) {
                                params.add(LAYOUT_WIDTH, MATCH_CONSTRAINT);
                                params.add(HORIZONTAL_WEIGHT, weight);
                            } else {
                                params.add(LAYOUT_HEIGHT, MATCH_CONSTRAINT);
                                params.add(VERTICAL_WEIGHT, weight);
                            }
                        }
                    } catch (NumberFormatException e) {
                        // nothing
                    }
                } else if (KEY_PERCENT_PARENT.equalsIgnoreCase(key)) {
                    try {
                        float percent = Math.min(1, Float.parseFloat(val));
                        percent = Math.max(0, percent);
                        if (data instanceof ConstraintLayout.LayoutParams) {
                            ConstraintLayout.LayoutParams params =
                                    (ConstraintLayout.LayoutParams) data;
                            if (orientation == HORIZONTAL) {
                                params.width = MATCH_CONSTRAINT;
                                params.matchConstraintPercentWidth = percent;
                                params.matchConstraintDefaultWidth = MATCH_CONSTRAINT_PERCENT;
                            } else {
                                params.height = MATCH_CONSTRAINT;
                                params.matchConstraintPercentHeight = percent;
                                params.matchConstraintDefaultHeight = MATCH_CONSTRAINT_PERCENT;
                            }
                        } else if (data instanceof Layout) {
                            Layout params = (Layout) data;
                            if (orientation == HORIZONTAL) {
                                params.mWidth = MATCH_CONSTRAINT;
                                params.widthPercent = percent;
                                params.widthDefault = MATCH_CONSTRAINT_PERCENT;
                            } else {
                                params.mHeight = MATCH_CONSTRAINT;
                                params.heightPercent = percent;
                                params.heightDefault = MATCH_CONSTRAINT_PERCENT;
                            }
                        } else if (data instanceof Constraint.Delta) {
                            Constraint.Delta params = (Constraint.Delta) data;
                            if (orientation == HORIZONTAL) {
                                params.add(LAYOUT_WIDTH, MATCH_CONSTRAINT);
                                params.add(WIDTH_DEFAULT, MATCH_CONSTRAINT_PERCENT);
                            } else {
                                params.add(LAYOUT_HEIGHT, MATCH_CONSTRAINT);
                                params.add(HEIGHT_DEFAULT, MATCH_CONSTRAINT_PERCENT);
                            }
                        }
                    } catch (NumberFormatException e) {
                        // nothing
                    }
                }
            }
        }
    }


    /**
     * Get the types associated with this ConstraintSet
     * The types mechanism allows you to tag the constraint set
     * with a series of string to define properties of a ConstraintSet
     *
     * @return an array of type strings
     */
    public String[] getStateLabels() {
        return Arrays.copyOf(mMatchLabels, mMatchLabels.length);
    }

    /**
     * Set the types associated with this ConstraintSet
     * The types mechanism allows you to tag the constraint set
     * with a series of string to define properties of a ConstraintSet
     * @param types a comer separated array of strings.
     */
    public void setStateLabels(String types) {
        mMatchLabels = types.split(",");
        for (int i = 0; i < mMatchLabels.length; i++) {
            mMatchLabels[i] = mMatchLabels[i].trim();
        }
    }
    /**
     * Set the types associated with this ConstraintSet
     * The types mechanism allows you to tag the constraint set
     * with a series of string to define properties of a ConstraintSet
     * @param types a comer separated array of strings.
     */
    public void setStateLabelsList(String... types) {
        mMatchLabels = types;
        for (int i = 0; i < mMatchLabels.length; i++) {
            mMatchLabels[i] = mMatchLabels[i].trim();
        }
    }

    /**
     * Test if the list of strings matches labels defined on this constraintSet
     * @param types list of types
     * @return true if all types are in the labels
     */
    public boolean matchesLabels(String...types) {
        for (String type : types) {
            boolean match = false;
            for (String matchType : mMatchLabels) {
                if (matchType.equals(type)) {
                    match = true;
                    break;
                }
            }
            if (!match) {
                return false;
            }

        }
        return true;
    }

    /**
     *
     */
    public static class Layout {
        public boolean mIsGuideline = false;
        public boolean mApply = false;
        public boolean mOverride = false;
        public int mWidth;
        public int mHeight;
        public static final int UNSET = ConstraintSet.UNSET;
        public static final int UNSET_GONE_MARGIN = Integer.MIN_VALUE;
        public int guideBegin = UNSET;
        public int guideEnd = UNSET;
        public float guidePercent = UNSET;
        public boolean guidelineUseRtl = true;
        public int leftToLeft = UNSET;
        public int leftToRight = UNSET;
        public int rightToLeft = UNSET;
        public int rightToRight = UNSET;
        public int topToTop = UNSET;
        public int topToBottom = UNSET;
        public int bottomToTop = UNSET;
        public int bottomToBottom = UNSET;
        public int baselineToBaseline = UNSET;
        public int baselineToTop = UNSET;
        public int baselineToBottom = UNSET;
        public int startToEnd = UNSET;
        public int startToStart = UNSET;
        public int endToStart = UNSET;
        public int endToEnd = UNSET;
        public float horizontalBias = 0.5f;
        public float verticalBias = 0.5f;
        public String dimensionRatio = null;
        public int circleConstraint = UNSET;
        public int circleRadius = 0;
        public float circleAngle = 0;
        public int editorAbsoluteX = UNSET;
        public int editorAbsoluteY = UNSET;
        public int orientation = UNSET;
        public int leftMargin = 0;
        public int rightMargin = 0;
        public int topMargin = 0;
        public int bottomMargin = 0;
        public int endMargin = 0;
        public int startMargin = 0;
        public int baselineMargin = 0;
        public int goneLeftMargin = UNSET_GONE_MARGIN;
        public int goneTopMargin = UNSET_GONE_MARGIN;
        public int goneRightMargin = UNSET_GONE_MARGIN;
        public int goneBottomMargin = UNSET_GONE_MARGIN;
        public int goneEndMargin = UNSET_GONE_MARGIN;
        public int goneStartMargin = UNSET_GONE_MARGIN;
        public int goneBaselineMargin = UNSET_GONE_MARGIN;
        public float verticalWeight = UNSET;
        public float horizontalWeight = UNSET;
        public int horizontalChainStyle = CHAIN_SPREAD;
        public int verticalChainStyle = CHAIN_SPREAD;
        public int widthDefault = ConstraintWidget.MATCH_CONSTRAINT_SPREAD;
        public int heightDefault = ConstraintWidget.MATCH_CONSTRAINT_SPREAD;
        public int widthMax = 0;
        public int heightMax = 0;
        public int widthMin = 0;
        public int heightMin = 0;
        public float widthPercent = 1;
        public float heightPercent = 1;
        public int mBarrierDirection = UNSET;
        public int mBarrierMargin = 0;
        public int mHelperType = UNSET;
        public int[] mReferenceIds;
        public String mReferenceIdString;
        public String mConstraintTag;
        public boolean constrainedWidth = false;
        public boolean constrainedHeight = false;
        // TODO public boolean mChainUseRtl = false;
        public boolean mBarrierAllowsGoneWidgets = true;
        public int mWrapBehavior = ConstraintWidget.WRAP_BEHAVIOR_INCLUDED;

        /**
         * Copy from Layout
         * @param src
         */
        public void copyFrom(Layout src) {
            mIsGuideline = src.mIsGuideline;
            mWidth = src.mWidth;
            mApply = src.mApply;
            mHeight = src.mHeight;
            guideBegin = src.guideBegin;
            guideEnd = src.guideEnd;
            guidePercent = src.guidePercent;
            guidelineUseRtl = src.guidelineUseRtl;
            leftToLeft = src.leftToLeft;
            leftToRight = src.leftToRight;
            rightToLeft = src.rightToLeft;
            rightToRight = src.rightToRight;
            topToTop = src.topToTop;
            topToBottom = src.topToBottom;
            bottomToTop = src.bottomToTop;
            bottomToBottom = src.bottomToBottom;
            baselineToBaseline = src.baselineToBaseline;
            baselineToTop = src.baselineToTop;
            baselineToBottom = src.baselineToBottom;
            startToEnd = src.startToEnd;
            startToStart = src.startToStart;
            endToStart = src.endToStart;
            endToEnd = src.endToEnd;
            horizontalBias = src.horizontalBias;
            verticalBias = src.verticalBias;
            dimensionRatio = src.dimensionRatio;
            circleConstraint = src.circleConstraint;
            circleRadius = src.circleRadius;
            circleAngle = src.circleAngle;
            editorAbsoluteX = src.editorAbsoluteX;
            editorAbsoluteY = src.editorAbsoluteY;
            orientation = src.orientation;
            leftMargin = src.leftMargin;
            rightMargin = src.rightMargin;
            topMargin = src.topMargin;
            bottomMargin = src.bottomMargin;
            endMargin = src.endMargin;
            startMargin = src.startMargin;
            baselineMargin = src.baselineMargin;
            goneLeftMargin = src.goneLeftMargin;
            goneTopMargin = src.goneTopMargin;
            goneRightMargin = src.goneRightMargin;
            goneBottomMargin = src.goneBottomMargin;
            goneEndMargin = src.goneEndMargin;
            goneStartMargin = src.goneStartMargin;
            goneBaselineMargin = src.goneBaselineMargin;
            verticalWeight = src.verticalWeight;
            horizontalWeight = src.horizontalWeight;
            horizontalChainStyle = src.horizontalChainStyle;
            verticalChainStyle = src.verticalChainStyle;
            widthDefault = src.widthDefault;
            heightDefault = src.heightDefault;
            widthMax = src.widthMax;
            heightMax = src.heightMax;
            widthMin = src.widthMin;
            heightMin = src.heightMin;
            widthPercent = src.widthPercent;
            heightPercent = src.heightPercent;
            mBarrierDirection = src.mBarrierDirection;
            mBarrierMargin = src.mBarrierMargin;
            mHelperType = src.mHelperType;
            mConstraintTag = src.mConstraintTag;

            if (src.mReferenceIds != null && src.mReferenceIdString == null) {
                mReferenceIds = Arrays.copyOf(src.mReferenceIds, src.mReferenceIds.length);
            } else {
                mReferenceIds = null;
            }
            mReferenceIdString = src.mReferenceIdString;
            constrainedWidth = src.constrainedWidth;
            constrainedHeight = src.constrainedHeight;
            // TODO mChainUseRtl = t.mChainUseRtl;
            mBarrierAllowsGoneWidgets = src.mBarrierAllowsGoneWidgets;
            mWrapBehavior = src.mWrapBehavior;
        }

        private static SparseIntArray sMapToConstant = new SparseIntArray();
        private static final int BASELINE_TO_BASELINE = 1;
        private static final int BOTTOM_MARGIN = 2;
        private static final int BOTTOM_TO_BOTTOM = 3;
        private static final int BOTTOM_TO_TOP = 4;
        private static final int DIMENSION_RATIO = 5;
        private static final int EDITOR_ABSOLUTE_X = 6;
        private static final int EDITOR_ABSOLUTE_Y = 7;
        private static final int END_MARGIN = 8;
        private static final int END_TO_END = 9;
        private static final int END_TO_START = 10;
        private static final int GONE_BOTTOM_MARGIN = 11;
        private static final int GONE_END_MARGIN = 12;
        private static final int GONE_LEFT_MARGIN = 13;
        private static final int GONE_RIGHT_MARGIN = 14;
        private static final int GONE_START_MARGIN = 15;
        private static final int GONE_TOP_MARGIN = 16;
        private static final int GUIDE_BEGIN = 17;
        private static final int GUIDE_END = 18;
        private static final int GUIDE_PERCENT = 19;
        private static final int HORIZONTAL_BIAS = 20;
        private static final int LAYOUT_HEIGHT = 21;
        private static final int LAYOUT_WIDTH = 22;
        private static final int LEFT_MARGIN = 23;
        private static final int LEFT_TO_LEFT = 24;
        private static final int LEFT_TO_RIGHT = 25;
        private static final int ORIENTATION = 26;
        private static final int RIGHT_MARGIN = 27;
        private static final int RIGHT_TO_LEFT = 28;
        private static final int RIGHT_TO_RIGHT = 29;
        private static final int START_MARGIN = 30;
        private static final int START_TO_END = 31;
        private static final int START_TO_START = 32;
        private static final int TOP_MARGIN = 33;
        private static final int TOP_TO_BOTTOM = 34;
        private static final int TOP_TO_TOP = 35;
        private static final int VERTICAL_BIAS = 36;
        private static final int HORIZONTAL_WEIGHT = 37;
        private static final int VERTICAL_WEIGHT = 38;
        private static final int HORIZONTAL_STYLE = 39;
        private static final int VERTICAL_STYLE = 40;
        private static final int LAYOUT_CONSTRAINT_WIDTH = 41;
        private static final int LAYOUT_CONSTRAINT_HEIGHT = 42;

        private static final int CIRCLE = 61;
        private static final int CIRCLE_RADIUS = 62;
        private static final int CIRCLE_ANGLE = 63;
        private static final int WIDTH_PERCENT = 69;
        private static final int HEIGHT_PERCENT = 70;
        private static final int CHAIN_USE_RTL = 71;
        private static final int BARRIER_DIRECTION = 72;
        private static final int BARRIER_MARGIN = 73;
        private static final int CONSTRAINT_REFERENCED_IDS = 74;
        private static final int BARRIER_ALLOWS_GONE_WIDGETS = 75;

        private static final int LAYOUT_WRAP_BEHAVIOR = 76;
        private static final int BASELINE_TO_TOP = 77;
        private static final int BASELINE_TO_BOTTOM = 78;
        private static final int GONE_BASELINE_MARGIN = 79;
        private static final int BASELINE_MARGIN = 80;
        private static final int WIDTH_DEFAULT = 81;
        private static final int HEIGHT_DEFAULT = 82;
        private static final int HEIGHT_MAX = 83;
        private static final int WIDTH_MAX = 84;
        private static final int HEIGHT_MIN = 85;
        private static final int WIDTH_MIN = 86;
        private static final int CONSTRAINED_WIDTH = 87;
        private static final int CONSTRAINED_HEIGHT = 88;
        private static final int CONSTRAINT_TAG = 89;
        private static final int GUIDE_USE_RTL = 90;

        private static final int UNUSED = 91;

        static {
            sMapToConstant.append(R.styleable.Layout_layout_constraintLeft_toLeftOf, LEFT_TO_LEFT);
            sMapToConstant.append(R.styleable.Layout_layout_constraintLeft_toRightOf,
                    LEFT_TO_RIGHT);
            sMapToConstant.append(R.styleable.Layout_layout_constraintRight_toLeftOf,
                    RIGHT_TO_LEFT);
            sMapToConstant.append(R.styleable.Layout_layout_constraintRight_toRightOf,
                    RIGHT_TO_RIGHT);
            sMapToConstant.append(R.styleable.Layout_layout_constraintTop_toTopOf, TOP_TO_TOP);
            sMapToConstant.append(R.styleable.Layout_layout_constraintTop_toBottomOf,
                    TOP_TO_BOTTOM);
            sMapToConstant.append(R.styleable.Layout_layout_constraintBottom_toTopOf,
                    BOTTOM_TO_TOP);
            sMapToConstant.append(R.styleable.Layout_layout_constraintBottom_toBottomOf,
                    BOTTOM_TO_BOTTOM);
            sMapToConstant.append(R.styleable.Layout_layout_constraintBaseline_toBaselineOf,
                    BASELINE_TO_BASELINE);

            sMapToConstant.append(R.styleable.Layout_layout_editor_absoluteX, EDITOR_ABSOLUTE_X);
            sMapToConstant.append(R.styleable.Layout_layout_editor_absoluteY, EDITOR_ABSOLUTE_Y);
            sMapToConstant.append(R.styleable.Layout_layout_constraintGuide_begin, GUIDE_BEGIN);
            sMapToConstant.append(R.styleable.Layout_layout_constraintGuide_end, GUIDE_END);
            sMapToConstant.append(R.styleable.Layout_layout_constraintGuide_percent, GUIDE_PERCENT);
            sMapToConstant.append(R.styleable.Layout_guidelineUseRtl, GUIDE_USE_RTL);
            sMapToConstant.append(R.styleable.Layout_android_orientation, ORIENTATION);
            sMapToConstant.append(R.styleable.Layout_layout_constraintStart_toEndOf, START_TO_END);
            sMapToConstant.append(R.styleable.Layout_layout_constraintStart_toStartOf,
                    START_TO_START);
            sMapToConstant.append(R.styleable.Layout_layout_constraintEnd_toStartOf, END_TO_START);
            sMapToConstant.append(R.styleable.Layout_layout_constraintEnd_toEndOf, END_TO_END);
            sMapToConstant.append(R.styleable.Layout_layout_goneMarginLeft, GONE_LEFT_MARGIN);
            sMapToConstant.append(R.styleable.Layout_layout_goneMarginTop, GONE_TOP_MARGIN);
            sMapToConstant.append(R.styleable.Layout_layout_goneMarginRight, GONE_RIGHT_MARGIN);
            sMapToConstant.append(R.styleable.Layout_layout_goneMarginBottom, GONE_BOTTOM_MARGIN);
            sMapToConstant.append(R.styleable.Layout_layout_goneMarginStart, GONE_START_MARGIN);
            sMapToConstant.append(R.styleable.Layout_layout_goneMarginEnd, GONE_END_MARGIN);
            sMapToConstant.append(R.styleable.Layout_layout_constraintVertical_weight,
                    VERTICAL_WEIGHT);
            sMapToConstant.append(R.styleable.Layout_layout_constraintHorizontal_weight,
                    HORIZONTAL_WEIGHT);
            sMapToConstant.append(R.styleable.Layout_layout_constraintHorizontal_chainStyle,
                    HORIZONTAL_STYLE);
            sMapToConstant.append(R.styleable.Layout_layout_constraintVertical_chainStyle,
                    VERTICAL_STYLE);

            sMapToConstant.append(R.styleable.Layout_layout_constraintHorizontal_bias,
                    HORIZONTAL_BIAS);
            sMapToConstant.append(R.styleable.Layout_layout_constraintVertical_bias,
                    VERTICAL_BIAS);
            sMapToConstant.append(R.styleable.Layout_layout_constraintDimensionRatio,
                    DIMENSION_RATIO);
            sMapToConstant.append(R.styleable.Layout_layout_constraintLeft_creator, UNUSED);
            sMapToConstant.append(R.styleable.Layout_layout_constraintTop_creator, UNUSED);
            sMapToConstant.append(R.styleable.Layout_layout_constraintRight_creator, UNUSED);
            sMapToConstant.append(R.styleable.Layout_layout_constraintBottom_creator, UNUSED);
            sMapToConstant.append(R.styleable.Layout_layout_constraintBaseline_creator, UNUSED);
            sMapToConstant.append(R.styleable.Layout_android_layout_marginLeft, LEFT_MARGIN);
            sMapToConstant.append(R.styleable.Layout_android_layout_marginRight, RIGHT_MARGIN);
            sMapToConstant.append(R.styleable.Layout_android_layout_marginStart, START_MARGIN);
            sMapToConstant.append(R.styleable.Layout_android_layout_marginEnd, END_MARGIN);
            sMapToConstant.append(R.styleable.Layout_android_layout_marginTop, TOP_MARGIN);
            sMapToConstant.append(R.styleable.Layout_android_layout_marginBottom, BOTTOM_MARGIN);
            sMapToConstant.append(R.styleable.Layout_android_layout_width, LAYOUT_WIDTH);
            sMapToConstant.append(R.styleable.Layout_android_layout_height, LAYOUT_HEIGHT);
            sMapToConstant.append(R.styleable.Layout_layout_constraintWidth,
                    LAYOUT_CONSTRAINT_WIDTH);
            sMapToConstant.append(R.styleable.Layout_layout_constraintHeight,
                    LAYOUT_CONSTRAINT_HEIGHT);
            sMapToConstant.append(R.styleable.Layout_layout_constrainedWidth,
                    LAYOUT_CONSTRAINT_WIDTH);
            sMapToConstant.append(R.styleable.Layout_layout_constrainedHeight,
                    LAYOUT_CONSTRAINT_HEIGHT);
            sMapToConstant.append(R.styleable.Layout_layout_wrapBehaviorInParent,
                    LAYOUT_WRAP_BEHAVIOR);

            sMapToConstant.append(R.styleable.Layout_layout_constraintCircle, CIRCLE);
            sMapToConstant.append(R.styleable.Layout_layout_constraintCircleRadius, CIRCLE_RADIUS);
            sMapToConstant.append(R.styleable.Layout_layout_constraintCircleAngle, CIRCLE_ANGLE);
            sMapToConstant.append(R.styleable.Layout_layout_constraintWidth_percent, WIDTH_PERCENT);
            sMapToConstant.append(R.styleable.Layout_layout_constraintHeight_percent,
                    HEIGHT_PERCENT);

            sMapToConstant.append(R.styleable.Layout_chainUseRtl, CHAIN_USE_RTL);
            sMapToConstant.append(R.styleable.Layout_barrierDirection, BARRIER_DIRECTION);
            sMapToConstant.append(R.styleable.Layout_barrierMargin, BARRIER_MARGIN);
            sMapToConstant.append(R.styleable.Layout_constraint_referenced_ids,
                    CONSTRAINT_REFERENCED_IDS);
            sMapToConstant.append(R.styleable.Layout_barrierAllowsGoneWidgets,
                    BARRIER_ALLOWS_GONE_WIDGETS);
            sMapToConstant.append(R.styleable.Layout_layout_constraintWidth_max,
                    WIDTH_MAX);
            sMapToConstant.append(R.styleable.Layout_layout_constraintWidth_min,
                    WIDTH_MIN);
            sMapToConstant.append(R.styleable.Layout_layout_constraintWidth_max,
                    HEIGHT_MAX);
            sMapToConstant.append(R.styleable.Layout_layout_constraintHeight_min,
                    HEIGHT_MIN);

            sMapToConstant.append(R.styleable.Layout_layout_constraintWidth, CONSTRAINED_WIDTH);
            sMapToConstant.append(R.styleable.Layout_layout_constraintHeight, CONSTRAINED_HEIGHT);
            sMapToConstant.append(R.styleable.ConstraintLayout_Layout_layout_constraintTag,
                    CONSTRAINT_TAG);
            sMapToConstant.append(R.styleable.Layout_guidelineUseRtl, GUIDE_USE_RTL);

        }

        void fillFromAttributeList(Context context, AttributeSet attrs) {
            TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.Layout);
            mApply = true;
            final int count = a.getIndexCount();
            for (int i = 0; i < count; i++) {
                int attr = a.getIndex(i);

                switch (sMapToConstant.get(attr)) {
                    case LEFT_TO_LEFT:
                        leftToLeft = lookupID(a, attr, leftToLeft);
                        break;
                    case LEFT_TO_RIGHT:
                        leftToRight = lookupID(a, attr, leftToRight);
                        break;
                    case RIGHT_TO_LEFT:
                        rightToLeft = lookupID(a, attr, rightToLeft);
                        break;
                    case RIGHT_TO_RIGHT:
                        rightToRight = lookupID(a, attr, rightToRight);
                        break;
                    case TOP_TO_TOP:
                        topToTop = lookupID(a, attr, topToTop);
                        break;
                    case TOP_TO_BOTTOM:
                        topToBottom = lookupID(a, attr, topToBottom);
                        break;
                    case BOTTOM_TO_TOP:
                        bottomToTop = lookupID(a, attr, bottomToTop);
                        break;
                    case BOTTOM_TO_BOTTOM:
                        bottomToBottom = lookupID(a, attr, bottomToBottom);
                        break;
                    case BASELINE_TO_BASELINE:
                        baselineToBaseline = lookupID(a, attr, baselineToBaseline);
                        break;
                    case BASELINE_TO_TOP:
                        baselineToTop = lookupID(a, attr, baselineToTop);
                        break;
                    case BASELINE_TO_BOTTOM:
                        baselineToBottom = lookupID(a, attr, baselineToBottom);
                        break;
                    case EDITOR_ABSOLUTE_X:
                        editorAbsoluteX = a.getDimensionPixelOffset(attr, editorAbsoluteX);
                        break;
                    case EDITOR_ABSOLUTE_Y:
                        editorAbsoluteY = a.getDimensionPixelOffset(attr, editorAbsoluteY);
                        break;
                    case GUIDE_BEGIN:
                        guideBegin = a.getDimensionPixelOffset(attr, guideBegin);
                        break;
                    case GUIDE_END:
                        guideEnd = a.getDimensionPixelOffset(attr, guideEnd);
                        break;
                    case GUIDE_PERCENT:
                        guidePercent = a.getFloat(attr, guidePercent);
                        break;
                    case GUIDE_USE_RTL:
                        guidelineUseRtl = a.getBoolean(attr, guidelineUseRtl);
                        break;

                    case ORIENTATION:
                        orientation = a.getInt(attr, orientation);
                        break;
                    case START_TO_END:
                        startToEnd = lookupID(a, attr, startToEnd);
                        break;
                    case START_TO_START:
                        startToStart = lookupID(a, attr, startToStart);
                        break;
                    case END_TO_START:
                        endToStart = lookupID(a, attr, endToStart);
                        break;
                    case END_TO_END:
                        endToEnd = lookupID(a, attr, endToEnd);
                        break;
                    case CIRCLE:
                        circleConstraint = lookupID(a, attr, circleConstraint);
                        break;
                    case CIRCLE_RADIUS:
                        circleRadius = a.getDimensionPixelSize(attr, circleRadius);
                        break;
                    case CIRCLE_ANGLE:
                        circleAngle = a.getFloat(attr, circleAngle);
                        break;
                    case GONE_LEFT_MARGIN:
                        goneLeftMargin = a.getDimensionPixelSize(attr, goneLeftMargin);
                        break;
                    case GONE_TOP_MARGIN:
                        goneTopMargin = a.getDimensionPixelSize(attr, goneTopMargin);
                        break;
                    case GONE_RIGHT_MARGIN:
                        goneRightMargin = a.getDimensionPixelSize(attr, goneRightMargin);
                        break;
                    case GONE_BOTTOM_MARGIN:
                        goneBottomMargin = a.getDimensionPixelSize(attr, goneBottomMargin);
                        break;
                    case GONE_START_MARGIN:
                        goneStartMargin = a.getDimensionPixelSize(attr, goneStartMargin);
                        break;
                    case GONE_END_MARGIN:
                        goneEndMargin = a.getDimensionPixelSize(attr, goneEndMargin);
                        break;
                    case GONE_BASELINE_MARGIN:
                        goneBaselineMargin = a.getDimensionPixelSize(attr, goneBaselineMargin);
                        break;
                    case HORIZONTAL_BIAS:
                        horizontalBias = a.getFloat(attr, horizontalBias);
                        break;
                    case VERTICAL_BIAS:
                        verticalBias = a.getFloat(attr, verticalBias);
                        break;
                    case LEFT_MARGIN:
                        leftMargin = a.getDimensionPixelSize(attr, leftMargin);
                        break;
                    case RIGHT_MARGIN:
                        rightMargin = a.getDimensionPixelSize(attr, rightMargin);
                        break;
                    case START_MARGIN:
                        if (Build.VERSION.SDK_INT >= VERSION_CODES.JELLY_BEAN_MR1) {
                            startMargin = a.getDimensionPixelSize(attr, startMargin);
                        }
                        break;
                    case END_MARGIN:
                        if (Build.VERSION.SDK_INT >= VERSION_CODES.JELLY_BEAN_MR1) {
                            endMargin = a.getDimensionPixelSize(attr, endMargin);
                        }
                        break;
                    case TOP_MARGIN:
                        topMargin = a.getDimensionPixelSize(attr, topMargin);
                        break;
                    case BOTTOM_MARGIN:
                        bottomMargin = a.getDimensionPixelSize(attr, bottomMargin);
                        break;
                    case BASELINE_MARGIN:
                        baselineMargin = a.getDimensionPixelSize(attr, baselineMargin);
                        break;
                    case LAYOUT_WIDTH:
                        mWidth = a.getLayoutDimension(attr, mWidth);
                        break;
                    case LAYOUT_HEIGHT:
                        mHeight = a.getLayoutDimension(attr, mHeight);
                        break;
                    case LAYOUT_CONSTRAINT_WIDTH:
                        ConstraintSet.parseDimensionConstraints(this, a, attr, HORIZONTAL);
                        break;
                    case LAYOUT_CONSTRAINT_HEIGHT:
                        ConstraintSet.parseDimensionConstraints(this, a, attr, VERTICAL);
                        break;
                    case WIDTH_DEFAULT:
                        widthDefault = a.getInt(attr, widthDefault);
                        break;
                    case HEIGHT_DEFAULT:
                        heightDefault = a.getInt(attr, heightDefault);
                        break;
                    case VERTICAL_WEIGHT:
                        verticalWeight = a.getFloat(attr, verticalWeight);
                        break;
                    case HORIZONTAL_WEIGHT:
                        horizontalWeight = a.getFloat(attr, horizontalWeight);
                        break;
                    case VERTICAL_STYLE:
                        verticalChainStyle = a.getInt(attr, verticalChainStyle);
                        break;
                    case HORIZONTAL_STYLE:
                        horizontalChainStyle = a.getInt(attr, horizontalChainStyle);
                        break;
                    case DIMENSION_RATIO:
                        dimensionRatio = a.getString(attr);
                        break;
                    case HEIGHT_MAX:
                        heightMax = a.getDimensionPixelSize(attr, heightMax);
                        break;
                    case WIDTH_MAX:
                        widthMax = a.getDimensionPixelSize(attr, widthMax);
                        break;
                    case HEIGHT_MIN:
                        heightMin = a.getDimensionPixelSize(attr, heightMin);
                        break;
                    case WIDTH_MIN:
                        widthMin = a.getDimensionPixelSize(attr, widthMin);
                        break;
                    case WIDTH_PERCENT:
                        widthPercent = a.getFloat(attr, 1);
                        break;
                    case HEIGHT_PERCENT:
                        heightPercent = a.getFloat(attr, 1);
                        break;
                    case CONSTRAINED_WIDTH:
                        constrainedWidth = a.getBoolean(attr, constrainedWidth);
                        break;
                    case CONSTRAINED_HEIGHT:
                        constrainedHeight = a.getBoolean(attr, constrainedHeight);
                        break;
                    case CHAIN_USE_RTL:
                        Log.e(TAG, "CURRENTLY UNSUPPORTED"); // TODO add support or remove
                        //  TODO add support or remove  c.mChainUseRtl
                        //   = a.getBoolean(attr,c.mChainUseRtl);
                        break;
                    case BARRIER_DIRECTION:
                        mBarrierDirection = a.getInt(attr, mBarrierDirection);
                        break;
                    case LAYOUT_WRAP_BEHAVIOR:
                        mWrapBehavior = a.getInt(attr, mWrapBehavior);
                        break;
                    case BARRIER_MARGIN:
                        mBarrierMargin = a.getDimensionPixelSize(attr, mBarrierMargin);
                        break;
                    case CONSTRAINT_REFERENCED_IDS:
                        mReferenceIdString = a.getString(attr);
                        break;
                    case BARRIER_ALLOWS_GONE_WIDGETS:
                        mBarrierAllowsGoneWidgets = a.getBoolean(attr, mBarrierAllowsGoneWidgets);
                        break;
                    case CONSTRAINT_TAG:
                        mConstraintTag = a.getString(attr);
                        break;
                    case UNUSED:
                        Log.w(TAG,
                                "unused attribute 0x" + Integer.toHexString(attr)
                                        + "   " + sMapToConstant.get(attr));
                        break;
                    default:
                        Log.w(TAG,
                                "Unknown attribute 0x" + Integer.toHexString(attr)
                                        + "   " + sMapToConstant.get(attr));

                }
            }
            a.recycle();
        }

        /**
         * Print the content to a string
         * @param scene
         * @param stringBuilder
         */
        public void dump(MotionScene scene, StringBuilder stringBuilder) {
            Field[] fields = this.getClass().getDeclaredFields();
            stringBuilder.append("\n");
            for (int i = 0; i < fields.length; i++) {
                Field field = fields[i];
                String name = field.getName();
                if (Modifier.isStatic(field.getModifiers())) {
                    continue;
                }

                try {
                    Object value = field.get(this);
                    Class<?> type = field.getType();
                    if (type == Integer.TYPE) {
                        Integer iValue = (Integer) value;
                        if (iValue != UNSET) {
                            String stringId = scene.lookUpConstraintName(iValue);
                            stringBuilder.append("    ");
                            stringBuilder.append(name);
                            stringBuilder.append(" = \"");
                            stringBuilder.append((stringId == null) ? iValue : stringId);
                            stringBuilder.append("\"\n");
                        }
                    } else if (type == Float.TYPE) {
                        Float fValue = (Float) value;
                        if (fValue != UNSET) {
                            stringBuilder.append("    ");
                            stringBuilder.append(name);
                            stringBuilder.append(" = \"");
                            stringBuilder.append(fValue);
                            stringBuilder.append("\"\n");
                        }
                    }
                } catch (IllegalAccessException e) {
                    Log.e(TAG, "Error accessing ConstraintSet field", e);
                }
            }
        }
    }

    /**
     *
     */
    public static class Transform {
        public boolean mApply = false;
        public float rotation = 0;
        public float rotationX = 0;
        public float rotationY = 0;
        public float scaleX = 1;
        public float scaleY = 1;
        public float transformPivotX = Float.NaN;
        public float transformPivotY = Float.NaN;
        public int transformPivotTarget = UNSET;
        public float translationX = 0;
        public float translationY = 0;
        public float translationZ = 0;
        public boolean applyElevation = false;
        public float elevation = 0;

        /**
         * Copy Transform from src
         * @param src
         */
        public void copyFrom(Transform src) {
            mApply = src.mApply;
            rotation = src.rotation;
            rotationX = src.rotationX;
            rotationY = src.rotationY;
            scaleX = src.scaleX;
            scaleY = src.scaleY;
            transformPivotX = src.transformPivotX;
            transformPivotY = src.transformPivotY;
            transformPivotTarget = src.transformPivotTarget;
            translationX = src.translationX;
            translationY = src.translationY;
            translationZ = src.translationZ;
            applyElevation = src.applyElevation;
            elevation = src.elevation;
        }

        private static SparseIntArray sMapToConstant = new SparseIntArray();
        private static final int ROTATION = 1;
        private static final int ROTATION_X = 2;
        private static final int ROTATION_Y = 3;
        private static final int SCALE_X = 4;
        private static final int SCALE_Y = 5;
        private static final int TRANSFORM_PIVOT_X = 6;
        private static final int TRANSFORM_PIVOT_Y = 7;
        private static final int TRANSLATION_X = 8;
        private static final int TRANSLATION_Y = 9;
        private static final int TRANSLATION_Z = 10;
        private static final int ELEVATION = 11;
        private static final int TRANSFORM_PIVOT_TARGET = 12;


        static {
            sMapToConstant.append(R.styleable.Transform_android_rotation, ROTATION);
            sMapToConstant.append(R.styleable.Transform_android_rotationX, ROTATION_X);
            sMapToConstant.append(R.styleable.Transform_android_rotationY, ROTATION_Y);
            sMapToConstant.append(R.styleable.Transform_android_scaleX, SCALE_X);
            sMapToConstant.append(R.styleable.Transform_android_scaleY, SCALE_Y);
            sMapToConstant.append(R.styleable.Transform_android_transformPivotX, TRANSFORM_PIVOT_X);
            sMapToConstant.append(R.styleable.Transform_android_transformPivotY, TRANSFORM_PIVOT_Y);
            sMapToConstant.append(R.styleable.Transform_android_translationX, TRANSLATION_X);
            sMapToConstant.append(R.styleable.Transform_android_translationY, TRANSLATION_Y);
            sMapToConstant.append(R.styleable.Transform_android_translationZ, TRANSLATION_Z);
            sMapToConstant.append(R.styleable.Transform_android_elevation, ELEVATION);
            sMapToConstant.append(R.styleable.Transform_transformPivotTarget,
                    TRANSFORM_PIVOT_TARGET);

        }

        void fillFromAttributeList(Context context, AttributeSet attrs) {
            TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.Transform);
            mApply = true;
            final int count = a.getIndexCount();
            for (int i = 0; i < count; i++) {
                int attr = a.getIndex(i);

                switch (sMapToConstant.get(attr)) {
                    case ROTATION:
                        rotation = a.getFloat(attr, rotation);
                        break;
                    case ROTATION_X:
                        rotationX = a.getFloat(attr, rotationX);
                        break;
                    case ROTATION_Y:
                        rotationY = a.getFloat(attr, rotationY);
                        break;
                    case SCALE_X:
                        scaleX = a.getFloat(attr, scaleX);
                        break;
                    case SCALE_Y:
                        scaleY = a.getFloat(attr, scaleY);
                        break;
                    case TRANSFORM_PIVOT_X:
                        transformPivotX = a.getDimension(attr, transformPivotX);
                        break;
                    case TRANSFORM_PIVOT_Y:
                        transformPivotY = a.getDimension(attr, transformPivotY);
                        break;
                    case TRANSFORM_PIVOT_TARGET:
                        transformPivotTarget = lookupID(a, attr, transformPivotTarget);
                        break;
                    case TRANSLATION_X:
                        translationX = a.getDimension(attr, translationX);
                        break;
                    case TRANSLATION_Y:
                        translationY = a.getDimension(attr, translationY);
                        break;
                    case TRANSLATION_Z:
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                            translationZ = a.getDimension(attr, translationZ);
                        }
                        break;
                    case ELEVATION:
                        if (Build.VERSION.SDK_INT >= VERSION_CODES.LOLLIPOP) {
                            applyElevation = true;
                            elevation = a.getDimension(attr, elevation);
                        }
                        break;
                }
            }
            a.recycle();
        }
    }

    /**
     *
     */
    public static class PropertySet {
        public boolean mApply = false;
        public int visibility = View.VISIBLE;
        public int mVisibilityMode = VISIBILITY_MODE_NORMAL;
        public float alpha = 1;
        public float mProgress = Float.NaN;

        // @TODO: add description

        /**
         *
         * @param src
         */
        public void copyFrom(PropertySet src) {
            mApply = src.mApply;
            visibility = src.visibility;
            alpha = src.alpha;
            mProgress = src.mProgress;
            mVisibilityMode = src.mVisibilityMode;
        }

        void fillFromAttributeList(Context context, AttributeSet attrs) {
            TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.PropertySet);
            mApply = true;
            final int count = a.getIndexCount();
            for (int i = 0; i < count; i++) {
                int attr = a.getIndex(i);

                if (attr == R.styleable.PropertySet_android_alpha) {
                    alpha = a.getFloat(attr, alpha);
                } else if (attr == R.styleable.PropertySet_android_visibility) {
                    visibility = a.getInt(attr, visibility);
                    visibility = VISIBILITY_FLAGS[visibility];
                } else if (attr == R.styleable.PropertySet_visibilityMode) {
                    mVisibilityMode = a.getInt(attr, mVisibilityMode);
                } else if (attr == R.styleable.PropertySet_motionProgress) {
                    mProgress = a.getFloat(attr, mProgress);
                }
            }
            a.recycle();
        }
    }

    /**
     *
     */
    public static class Motion {
        public boolean mApply = false;
        public int mAnimateRelativeTo = Layout.UNSET;
        public int mAnimateCircleAngleTo = 0;
        public String mTransitionEasing = null;
        public int mPathMotionArc = Layout.UNSET;
        public int mDrawPath = 0;
        public float mMotionStagger = Float.NaN;
        public int mPolarRelativeTo = Layout.UNSET;
        public float mPathRotate = Float.NaN;
        public float mQuantizeMotionPhase = Float.NaN;
        public int mQuantizeMotionSteps = Layout.UNSET;
        public String mQuantizeInterpolatorString = null;
        public int mQuantizeInterpolatorType = INTERPOLATOR_UNDEFINED; // undefined
        public int mQuantizeInterpolatorID = -1;
        private static final int INTERPOLATOR_REFERENCE_ID = -2;
        private static final int SPLINE_STRING = -1;
        private static final int INTERPOLATOR_UNDEFINED = -3;

        // @TODO: add description

        /**
         *
         * @param src
         */
        public void copyFrom(Motion src) {
            mApply = src.mApply;
            mAnimateRelativeTo = src.mAnimateRelativeTo;
            mTransitionEasing = src.mTransitionEasing;
            mPathMotionArc = src.mPathMotionArc;
            mDrawPath = src.mDrawPath;
            mPathRotate = src.mPathRotate;
            mMotionStagger = src.mMotionStagger;
            mPolarRelativeTo = src.mPolarRelativeTo;
        }

        private static SparseIntArray sMapToConstant = new SparseIntArray();
        private static final int TRANSITION_PATH_ROTATE = 1;
        private static final int PATH_MOTION_ARC = 2;
        private static final int TRANSITION_EASING = 3;
        private static final int MOTION_DRAW_PATH = 4;
        private static final int ANIMATE_RELATIVE_TO = 5;
        private static final int ANIMATE_CIRCLE_ANGLE_TO = 6;
        private static final int MOTION_STAGGER = 7;
        private static final int QUANTIZE_MOTION_STEPS = 8;
        private static final int QUANTIZE_MOTION_PHASE = 9;
        private static final int QUANTIZE_MOTION_INTERPOLATOR = 10;


        static {
            sMapToConstant.append(R.styleable.Motion_motionPathRotate, TRANSITION_PATH_ROTATE);
            sMapToConstant.append(R.styleable.Motion_pathMotionArc, PATH_MOTION_ARC);
            sMapToConstant.append(R.styleable.Motion_transitionEasing, TRANSITION_EASING);
            sMapToConstant.append(R.styleable.Motion_drawPath, MOTION_DRAW_PATH);
            sMapToConstant.append(R.styleable.Motion_animateRelativeTo, ANIMATE_RELATIVE_TO);
            sMapToConstant.append(R.styleable.Motion_animateCircleAngleTo, ANIMATE_CIRCLE_ANGLE_TO);
            sMapToConstant.append(R.styleable.Motion_motionStagger, MOTION_STAGGER);
            sMapToConstant.append(R.styleable.Motion_quantizeMotionSteps, QUANTIZE_MOTION_STEPS);
            sMapToConstant.append(R.styleable.Motion_quantizeMotionPhase, QUANTIZE_MOTION_PHASE);
            sMapToConstant.append(R.styleable.Motion_quantizeMotionInterpolator,
                    QUANTIZE_MOTION_INTERPOLATOR);
        }

        void fillFromAttributeList(Context context, AttributeSet attrs) {
            TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.Motion);
            mApply = true;
            final int count = a.getIndexCount();
            for (int i = 0; i < count; i++) {
                int attr = a.getIndex(i);

                switch (sMapToConstant.get(attr)) {
                    case TRANSITION_PATH_ROTATE:
                        mPathRotate = a.getFloat(attr, mPathRotate);
                        break;
                    case PATH_MOTION_ARC:
                        mPathMotionArc = a.getInt(attr, mPathMotionArc);
                        break;
                    case TRANSITION_EASING:
                        TypedValue type = a.peekValue(attr);
                        if (type.type == TypedValue.TYPE_STRING) {
                            mTransitionEasing = a.getString(attr);
                        } else {
                            mTransitionEasing = Easing.NAMED_EASING[a.getInteger(attr, 0)];
                        }
                        break;
                    case MOTION_DRAW_PATH:
                        mDrawPath = a.getInt(attr, 0);
                        break;
                    case ANIMATE_RELATIVE_TO:
                        mAnimateRelativeTo = lookupID(a, attr, mAnimateRelativeTo);
                        break;
                    case ANIMATE_CIRCLE_ANGLE_TO:
                        mAnimateCircleAngleTo = a.getInteger(attr, mAnimateCircleAngleTo);
                        break;
                    case MOTION_STAGGER:
                        mMotionStagger = a.getFloat(attr, mMotionStagger);
                        break;
                    case QUANTIZE_MOTION_STEPS:
                        mQuantizeMotionSteps = a.getInteger(attr, mQuantizeMotionSteps);
                        break;
                    case QUANTIZE_MOTION_PHASE:
                        mQuantizeMotionPhase = a.getFloat(attr, mQuantizeMotionPhase);
                        break;
                    case QUANTIZE_MOTION_INTERPOLATOR:
                        type = a.peekValue(attr);

                        if (type.type == TypedValue.TYPE_REFERENCE) {
                            mQuantizeInterpolatorID = a.getResourceId(attr, -1);
                            if (mQuantizeInterpolatorID != -1) {
                                mQuantizeInterpolatorType = INTERPOLATOR_REFERENCE_ID;
                            }
                        } else if (type.type == TypedValue.TYPE_STRING) {
                            mQuantizeInterpolatorString = a.getString(attr);
                            if (mQuantizeInterpolatorString.indexOf("/") > 0) {
                                mQuantizeInterpolatorID = a.getResourceId(attr, -1);
                                mQuantizeInterpolatorType = INTERPOLATOR_REFERENCE_ID;
                            } else {
                                mQuantizeInterpolatorType = SPLINE_STRING;
                            }
                        } else {
                            mQuantizeInterpolatorType = a.getInteger(attr, mQuantizeInterpolatorID);
                        }

                        break;
                }
            }
            a.recycle();
        }
    }

    /**
     *
     */
    public static class Constraint {
        int mViewId;
        String mTargetString;
        public final PropertySet propertySet = new PropertySet();
        public final Motion motion = new Motion();
        public final Layout layout = new Layout();
        public final Transform transform = new Transform();
        public HashMap<String, ConstraintAttribute> mCustomConstraints = new HashMap<>();
        Delta mDelta;

        static class Delta {
            private static final int INITIAL_BOOLEAN = 4;
            private static final int INITIAL_INT = 10;
            private static final int INITIAL_FLOAT = 10;
            private static final int INITIAL_STRING = 5;
            int[] mTypeInt = new int[INITIAL_INT];
            int[] mValueInt = new int[INITIAL_INT];
            int mCountInt = 0;

            void add(int type, int value) {
                if (mCountInt >= mTypeInt.length) {
                    mTypeInt = Arrays.copyOf(mTypeInt, mTypeInt.length * 2);
                    mValueInt = Arrays.copyOf(mValueInt, mValueInt.length * 2);
                }
                mTypeInt[mCountInt] = type;
                mValueInt[mCountInt++] = value;
            }

            int[] mTypeFloat = new int[INITIAL_FLOAT];
            float[] mValueFloat = new float[INITIAL_FLOAT];
            int mCountFloat = 0;

            void add(int type, float value) {
                if (mCountFloat >= mTypeFloat.length) {
                    mTypeFloat = Arrays.copyOf(mTypeFloat, mTypeFloat.length * 2);
                    mValueFloat = Arrays.copyOf(mValueFloat, mValueFloat.length * 2);
                }
                mTypeFloat[mCountFloat] = type;
                mValueFloat[mCountFloat++] = value;
            }

            int[] mTypeString = new int[INITIAL_STRING];
            String[] mValueString = new String[INITIAL_STRING];
            int mCountString = 0;

            void add(int type, String value) {
                if (mCountString >= mTypeString.length) {
                    mTypeString = Arrays.copyOf(mTypeString, mTypeString.length * 2);
                    mValueString = Arrays.copyOf(mValueString, mValueString.length * 2);
                }
                mTypeString[mCountString] = type;
                mValueString[mCountString++] = value;
            }

            int[] mTypeBoolean = new int[INITIAL_BOOLEAN];
            boolean[] mValueBoolean = new boolean[INITIAL_BOOLEAN];
            int mCountBoolean = 0;

            void add(int type, boolean value) {
                if (mCountBoolean >= mTypeBoolean.length) {
                    mTypeBoolean = Arrays.copyOf(mTypeBoolean, mTypeBoolean.length * 2);
                    mValueBoolean = Arrays.copyOf(mValueBoolean, mValueBoolean.length * 2);
                }
                mTypeBoolean[mCountBoolean] = type;
                mValueBoolean[mCountBoolean++] = value;
            }

            void applyDelta(Constraint c) {
                for (int i = 0; i < mCountInt; i++) {
                    setDeltaValue(c, mTypeInt[i], mValueInt[i]);
                }
                for (int i = 0; i < mCountFloat; i++) {
                    setDeltaValue(c, mTypeFloat[i], mValueFloat[i]);
                }
                for (int i = 0; i < mCountString; i++) {
                    setDeltaValue(c, mTypeString[i], mValueString[i]);
                }
                for (int i = 0; i < mCountBoolean; i++) {
                    setDeltaValue(c, mTypeBoolean[i], mValueBoolean[i]);
                }
            }

            @SuppressLint("LogConditional")
            void printDelta(String tag) {
                Log.v(tag, "int");

                for (int i = 0; i < mCountInt; i++) {
                    Log.v(tag, mTypeInt[i] + " = " + mValueInt[i]);
                }
                Log.v(tag, "float");

                for (int i = 0; i < mCountFloat; i++) {
                    Log.v(tag, mTypeFloat[i] + " = " + mValueFloat[i]);
                }
                Log.v(tag, "strings");

                for (int i = 0; i < mCountString; i++) {
                    Log.v(tag, mTypeString[i] + " = " + mValueString[i]);
                }
                Log.v(tag, "boolean");
                for (int i = 0; i < mCountBoolean; i++) {
                    Log.v(tag, mTypeBoolean[i] + " = " + mValueBoolean[i]);
                }
            }
        }

        /**
         * Apply a delta to a constraint
         * @param c
         */
        public void applyDelta(Constraint c) {
            if (mDelta != null) {
                mDelta.applyDelta(c);
            }
        }

        /**
         * Apply a delta file
         * @param tag
         */
        public void printDelta(String tag) {
            if (mDelta != null) {
                mDelta.printDelta(tag);
            } else {
                Log.v(tag, "DELTA IS NULL");
            }
        }

        private ConstraintAttribute get(String attributeName, AttributeType attributeType) {
            ConstraintAttribute ret;
            if (mCustomConstraints.containsKey(attributeName)) {
                ret = mCustomConstraints.get(attributeName);
                if (ret.getType() != attributeType) {
                    throw new IllegalArgumentException(
                            "ConstraintAttribute is already a " + ret.getType().name());
                }
            } else {
                ret = new ConstraintAttribute(attributeName, attributeType);
                mCustomConstraints.put(attributeName, ret);
            }
            return ret;
        }

        private void setStringValue(String attributeName, String value) {
            get(attributeName, AttributeType.STRING_TYPE).setStringValue(value);
        }

        private void setFloatValue(String attributeName, float value) {
            get(attributeName, AttributeType.FLOAT_TYPE).setFloatValue(value);
        }

        private void setIntValue(String attributeName, int value) {
            get(attributeName, AttributeType.INT_TYPE).setIntValue(value);
        }

        private void setColorValue(String attributeName, int value) {
            get(attributeName, AttributeType.COLOR_TYPE).setColorValue(value);
        }

        /**
         * Return a copy of the Constraint
         * @return
         */
        @Override
        public Constraint clone() {
            Constraint clone = new Constraint();
            clone.layout.copyFrom(layout);
            clone.motion.copyFrom(motion);
            clone.propertySet.copyFrom(propertySet);
            clone.transform.copyFrom(transform);
            clone.mViewId = mViewId;
            clone.mDelta = mDelta;
            return clone;
        }

        private void fillFromConstraints(ConstraintHelper helper,
                                         int viewId,
                                         Constraints.LayoutParams param) {
            fillFromConstraints(viewId, param);
            if (helper instanceof Barrier) {
                layout.mHelperType = BARRIER_TYPE;
                Barrier barrier = (Barrier) helper;
                layout.mBarrierDirection = barrier.getType();
                layout.mReferenceIds = barrier.getReferencedIds();
                layout.mBarrierMargin = barrier.getMargin();
            }
        }

        private void fillFromConstraints(int viewId, Constraints.LayoutParams param) {
            fillFrom(viewId, param);
            propertySet.alpha = param.alpha;
            transform.rotation = param.rotation;
            transform.rotationX = param.rotationX;
            transform.rotationY = param.rotationY;
            transform.scaleX = param.scaleX;
            transform.scaleY = param.scaleY;
            transform.transformPivotX = param.transformPivotX;
            transform.transformPivotY = param.transformPivotY;
            transform.translationX = param.translationX;
            transform.translationY = param.translationY;
            transform.translationZ = param.translationZ;
            transform.elevation = param.elevation;
            transform.applyElevation = param.applyElevation;
        }

        private void fillFrom(int viewId, ConstraintLayout.LayoutParams param) {
            mViewId = viewId;
            layout.leftToLeft = param.leftToLeft;
            layout.leftToRight = param.leftToRight;
            layout.rightToLeft = param.rightToLeft;
            layout.rightToRight = param.rightToRight;
            layout.topToTop = param.topToTop;
            layout.topToBottom = param.topToBottom;
            layout.bottomToTop = param.bottomToTop;
            layout.bottomToBottom = param.bottomToBottom;
            layout.baselineToBaseline = param.baselineToBaseline;
            layout.baselineToTop = param.baselineToTop;
            layout.baselineToBottom = param.baselineToBottom;
            layout.startToEnd = param.startToEnd;
            layout.startToStart = param.startToStart;
            layout.endToStart = param.endToStart;
            layout.endToEnd = param.endToEnd;

            layout.horizontalBias = param.horizontalBias;
            layout.verticalBias = param.verticalBias;
            layout.dimensionRatio = param.dimensionRatio;

            layout.circleConstraint = param.circleConstraint;
            layout.circleRadius = param.circleRadius;
            layout.circleAngle = param.circleAngle;

            layout.editorAbsoluteX = param.editorAbsoluteX;
            layout.editorAbsoluteY = param.editorAbsoluteY;
            layout.orientation = param.orientation;
            layout.guidePercent = param.guidePercent;
            layout.guideBegin = param.guideBegin;
            layout.guideEnd = param.guideEnd;
            layout.mWidth = param.width;
            layout.mHeight = param.height;
            layout.leftMargin = param.leftMargin;
            layout.rightMargin = param.rightMargin;
            layout.topMargin = param.topMargin;
            layout.bottomMargin = param.bottomMargin;
            layout.baselineMargin = param.baselineMargin;
            layout.verticalWeight = param.verticalWeight;
            layout.horizontalWeight = param.horizontalWeight;
            layout.verticalChainStyle = param.verticalChainStyle;
            layout.horizontalChainStyle = param.horizontalChainStyle;
            layout.constrainedWidth = param.constrainedWidth;
            layout.constrainedHeight = param.constrainedHeight;
            layout.widthDefault = param.matchConstraintDefaultWidth;
            layout.heightDefault = param.matchConstraintDefaultHeight;
            layout.widthMax = param.matchConstraintMaxWidth;
            layout.heightMax = param.matchConstraintMaxHeight;
            layout.widthMin = param.matchConstraintMinWidth;
            layout.heightMin = param.matchConstraintMinHeight;
            layout.widthPercent = param.matchConstraintPercentWidth;
            layout.heightPercent = param.matchConstraintPercentHeight;
            layout.mConstraintTag = param.constraintTag;
            layout.goneTopMargin = param.goneTopMargin;
            layout.goneBottomMargin = param.goneBottomMargin;
            layout.goneLeftMargin = param.goneLeftMargin;
            layout.goneRightMargin = param.goneRightMargin;
            layout.goneStartMargin = param.goneStartMargin;
            layout.goneEndMargin = param.goneEndMargin;
            layout.goneBaselineMargin = param.goneBaselineMargin;
            layout.mWrapBehavior = param.wrapBehaviorInParent;

            int currentApiVersion = android.os.Build.VERSION.SDK_INT;
            if (currentApiVersion >= android.os.Build.VERSION_CODES.JELLY_BEAN_MR1) {
                layout.endMargin = param.getMarginEnd();
                layout.startMargin = param.getMarginStart();
            }
        }

        /**
         * apply ConstraintSet to the layout params
         * @param param
         */
        public void applyTo(ConstraintLayout.LayoutParams param) {
            param.leftToLeft = layout.leftToLeft;
            param.leftToRight = layout.leftToRight;
            param.rightToLeft = layout.rightToLeft;
            param.rightToRight = layout.rightToRight;

            param.topToTop = layout.topToTop;
            param.topToBottom = layout.topToBottom;
            param.bottomToTop = layout.bottomToTop;
            param.bottomToBottom = layout.bottomToBottom;

            param.baselineToBaseline = layout.baselineToBaseline;
            param.baselineToTop = layout.baselineToTop;
            param.baselineToBottom = layout.baselineToBottom;

            param.startToEnd = layout.startToEnd;
            param.startToStart = layout.startToStart;
            param.endToStart = layout.endToStart;
            param.endToEnd = layout.endToEnd;

            param.leftMargin = layout.leftMargin;
            param.rightMargin = layout.rightMargin;
            param.topMargin = layout.topMargin;
            param.bottomMargin = layout.bottomMargin;
            param.goneStartMargin = layout.goneStartMargin;
            param.goneEndMargin = layout.goneEndMargin;
            param.goneTopMargin = layout.goneTopMargin;
            param.goneBottomMargin = layout.goneBottomMargin;

            param.horizontalBias = layout.horizontalBias;
            param.verticalBias = layout.verticalBias;

            param.circleConstraint = layout.circleConstraint;
            param.circleRadius = layout.circleRadius;
            param.circleAngle = layout.circleAngle;

            param.dimensionRatio = layout.dimensionRatio;
            param.editorAbsoluteX = layout.editorAbsoluteX;
            param.editorAbsoluteY = layout.editorAbsoluteY;
            param.verticalWeight = layout.verticalWeight;
            param.horizontalWeight = layout.horizontalWeight;
            param.verticalChainStyle = layout.verticalChainStyle;
            param.horizontalChainStyle = layout.horizontalChainStyle;
            param.constrainedWidth = layout.constrainedWidth;
            param.constrainedHeight = layout.constrainedHeight;
            param.matchConstraintDefaultWidth = layout.widthDefault;
            param.matchConstraintDefaultHeight = layout.heightDefault;
            param.matchConstraintMaxWidth = layout.widthMax;
            param.matchConstraintMaxHeight = layout.heightMax;
            param.matchConstraintMinWidth = layout.widthMin;
            param.matchConstraintMinHeight = layout.heightMin;
            param.matchConstraintPercentWidth = layout.widthPercent;
            param.matchConstraintPercentHeight = layout.heightPercent;
            param.orientation = layout.orientation;
            param.guidePercent = layout.guidePercent;
            param.guideBegin = layout.guideBegin;
            param.guideEnd = layout.guideEnd;
            param.width = layout.mWidth;
            param.height = layout.mHeight;
            if (layout.mConstraintTag != null) {
                param.constraintTag = layout.mConstraintTag;
            }
            param.wrapBehaviorInParent = layout.mWrapBehavior;

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                param.setMarginStart(layout.startMargin);
                param.setMarginEnd(layout.endMargin);
            }

            param.validate();
        }

    }

    /**
     * Copy the constraints from a layout.
     *
     * @param context            the context for the layout inflation
     * @param constraintLayoutId the id of the layout file
     */
    public void clone(Context context, int constraintLayoutId) {
        clone((ConstraintLayout) LayoutInflater.from(context).inflate(constraintLayoutId, null));
    }

    /**
     * Copy the constraints from a layout.
     *
     * @param set constraint set to copy
     */
    public void clone(ConstraintSet set) {
        mConstraints.clear();
        for (Integer key : set.mConstraints.keySet()) {
            Constraint constraint = set.mConstraints.get(key);
            if (constraint == null) {
                continue;
            }
            mConstraints.put(key, constraint.clone());
        }
    }

    /**
     * Copy the layout parameters of a ConstraintLayout.
     *
     * @param constraintLayout The ConstraintLayout to be copied
     */
    public void clone(ConstraintLayout constraintLayout) {
        int count = constraintLayout.getChildCount();
        mConstraints.clear();
        for (int i = 0; i < count; i++) {
            View view = constraintLayout.getChildAt(i);
            ConstraintLayout.LayoutParams param =
                    (ConstraintLayout.LayoutParams) view.getLayoutParams();

            int id = view.getId();
            if (mForceId && id == -1) {
                throw new RuntimeException("All children of ConstraintLayout must "
                        + "have ids to use ConstraintSet");
            }
            if (!mConstraints.containsKey(id)) {
                mConstraints.put(id, new Constraint());
            }
            Constraint constraint = mConstraints.get(id);
            if (constraint == null) {
                continue;
            }
            constraint.mCustomConstraints =
                    ConstraintAttribute.extractAttributes(mSavedAttributes, view);
            constraint.fillFrom(id, param);
            constraint.propertySet.visibility = view.getVisibility();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                constraint.propertySet.alpha = view.getAlpha();
                constraint.transform.rotation = view.getRotation();
                constraint.transform.rotationX = view.getRotationX();
                constraint.transform.rotationY = view.getRotationY();
                constraint.transform.scaleX = view.getScaleX();
                constraint.transform.scaleY = view.getScaleY();

                float pivotX = view.getPivotX(); // we assume it is not set if set to 0.0
                float pivotY = view.getPivotY(); // we assume it is not set if set to 0.0

                if (pivotX != 0.0 || pivotY != 0.0) {
                    constraint.transform.transformPivotX = pivotX;
                    constraint.transform.transformPivotY = pivotY;
                }

                constraint.transform.translationX = view.getTranslationX();
                constraint.transform.translationY = view.getTranslationY();
                if (Build.VERSION.SDK_INT >= VERSION_CODES.LOLLIPOP) {
                    constraint.transform.translationZ = view.getTranslationZ();
                    if (constraint.transform.applyElevation) {
                        constraint.transform.elevation = view.getElevation();
                    }
                }
            }
            if (view instanceof Barrier) {
                Barrier barrier = ((Barrier) view);
                constraint.layout.mBarrierAllowsGoneWidgets = barrier.getAllowsGoneWidget();
                constraint.layout.mReferenceIds = barrier.getReferencedIds();
                constraint.layout.mBarrierDirection = barrier.getType();
                constraint.layout.mBarrierMargin = barrier.getMargin();
            }
        }
    }

    /**
     * Copy the layout parameters of a ConstraintLayout.
     *
     * @param constraints The ConstraintLayout to be copied
     */
    public void clone(Constraints constraints) {
        int count = constraints.getChildCount();
        mConstraints.clear();
        for (int i = 0; i < count; i++) {
            View view = constraints.getChildAt(i);
            Constraints.LayoutParams param = (Constraints.LayoutParams) view.getLayoutParams();

            int id = view.getId();
            if (mForceId && id == -1) {
                throw new RuntimeException("All children of ConstraintLayout "
                        + "must have ids to use ConstraintSet");
            }
            if (!mConstraints.containsKey(id)) {
                mConstraints.put(id, new Constraint());
            }
            Constraint constraint = mConstraints.get(id);
            if (constraint == null) {
                continue;
            }
            if (view instanceof ConstraintHelper) {
                ConstraintHelper helper = (ConstraintHelper) view;
                constraint.fillFromConstraints(helper, id, param);
            }
            constraint.fillFromConstraints(id, param);
        }
    }

    /**
     * Apply the constraints to a ConstraintLayout.
     *
     * @param constraintLayout to be modified
     */
    public void applyTo(ConstraintLayout constraintLayout) {
        applyToInternal(constraintLayout, true);
        constraintLayout.setConstraintSet(null);
        constraintLayout.requestLayout();
    }


    /**
     * Apply the constraints to a ConstraintLayout.
     *
     * @param constraintLayout to be modified
     */
    public void applyToWithoutCustom(ConstraintLayout constraintLayout) {
        applyToInternal(constraintLayout, false);
        constraintLayout.setConstraintSet(null);
    }

    /**
     * Apply custom attributes alone
     *
     * @param constraintLayout
     */
    public void applyCustomAttributes(ConstraintLayout constraintLayout) {
        int count = constraintLayout.getChildCount();
        for (int i = 0; i < count; i++) {
            View view = constraintLayout.getChildAt(i);
            int id = view.getId();
            if (!mConstraints.containsKey(id)) {
                Log.w(TAG, "id unknown " + Debug.getName(view));
                continue;
            }
            if (mForceId && id == -1) {
                throw new RuntimeException("All children of ConstraintLayout "
                        + "must have ids to use ConstraintSet");
            }

            if (mConstraints.containsKey(id)) {
                Constraint constraint = mConstraints.get(id);
                if (constraint == null) {
                    continue;
                }
                ConstraintAttribute.setAttributes(view, constraint.mCustomConstraints);
            }
        }
    }

    /**
     * Apply Layout to Helper widget
     *
     * @param helper
     * @param child
     * @param layoutParams
     * @param mapIdToWidget
     */
    public void applyToHelper(ConstraintHelper helper, ConstraintWidget child,
                              LayoutParams layoutParams,
                              SparseArray<ConstraintWidget> mapIdToWidget) {
        int id = helper.getId();
        if (mConstraints.containsKey(id)) {
            Constraint constraint = mConstraints.get(id);
            if (constraint != null && child instanceof HelperWidget) {
                HelperWidget helperWidget = (HelperWidget) child;
                helper.loadParameters(constraint, helperWidget, layoutParams, mapIdToWidget);
            }
        }
    }

    /**
     * Fill in a ConstraintLayout LayoutParam based on the id.
     *
     * @param id           Id of the view
     * @param layoutParams LayoutParams to be filled
     */
    public void applyToLayoutParams(int id, ConstraintLayout.LayoutParams layoutParams) {
        if (mConstraints.containsKey(id)) {
            Constraint constraint = mConstraints.get(id);
            if (constraint != null) {
                constraint.applyTo(layoutParams);
            }
        }
    }

    /**
     * Used to set constraints when used by constraint layout
     */
    void applyToInternal(ConstraintLayout constraintLayout, boolean applyPostLayout) {
        int count = constraintLayout.getChildCount();
        HashSet<Integer> used = new HashSet<Integer>(mConstraints.keySet());
        for (int i = 0; i < count; i++) {
            View view = constraintLayout.getChildAt(i);
            int id = view.getId();
            if (!mConstraints.containsKey(id)) {
                Log.w(TAG, "id unknown " + Debug.getName(view));
                continue;
            }

            if (mForceId && id == -1) {
                throw new RuntimeException("All children of ConstraintLayout "
                        + "must have ids to use ConstraintSet");
            }
            if (id == -1) {
                continue;
            }

            if (mConstraints.containsKey(id)) {
                used.remove(id);
                Constraint constraint = mConstraints.get(id);
                if (constraint == null) {
                    continue;
                }
                if (view instanceof Barrier) {
                    constraint.layout.mHelperType = BARRIER_TYPE;
                    Barrier barrier = (Barrier) view;
                    barrier.setId(id);
                    barrier.setType(constraint.layout.mBarrierDirection);
                    barrier.setMargin(constraint.layout.mBarrierMargin);

                    barrier.setAllowsGoneWidget(constraint.layout.mBarrierAllowsGoneWidgets);
                    if (constraint.layout.mReferenceIds != null) {
                        barrier.setReferencedIds(constraint.layout.mReferenceIds);
                    } else if (constraint.layout.mReferenceIdString != null) {
                        constraint.layout.mReferenceIds = convertReferenceString(barrier,
                                constraint.layout.mReferenceIdString);
                        barrier.setReferencedIds(constraint.layout.mReferenceIds);
                    }
                }
                ConstraintLayout.LayoutParams param = (ConstraintLayout.LayoutParams) view
                        .getLayoutParams();
                param.validate();
                constraint.applyTo(param);

                if (applyPostLayout) {
                    ConstraintAttribute.setAttributes(view, constraint.mCustomConstraints);
                }
                view.setLayoutParams(param);
                if (constraint.propertySet.mVisibilityMode == VISIBILITY_MODE_NORMAL) {
                    view.setVisibility(constraint.propertySet.visibility);
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                    view.setAlpha(constraint.propertySet.alpha);
                    view.setRotation(constraint.transform.rotation);
                    view.setRotationX(constraint.transform.rotationX);
                    view.setRotationY(constraint.transform.rotationY);
                    view.setScaleX(constraint.transform.scaleX);
                    view.setScaleY(constraint.transform.scaleY);
                    if (constraint.transform.transformPivotTarget != UNSET) {
                        View layout = (View) view.getParent();
                        View center = layout.findViewById(
                                constraint.transform.transformPivotTarget);
                        if (center != null) {
                            float cy = (center.getTop() + center.getBottom()) / 2.0f;
                            float cx = (center.getLeft() + center.getRight()) / 2.0f;
                            if (view.getRight() - view.getLeft() > 0
                                    && view.getBottom() - view.getTop() > 0) {
                                float px = (cx - view.getLeft());
                                float py = (cy - view.getTop());
                                view.setPivotX(px);
                                view.setPivotY(py);
                            }
                        }
                    } else {
                        if (!Float.isNaN(constraint.transform.transformPivotX)) {
                            view.setPivotX(constraint.transform.transformPivotX);
                        }
                        if (!Float.isNaN(constraint.transform.transformPivotY)) {
                            view.setPivotY(constraint.transform.transformPivotY);
                        }
                    }
                    view.setTranslationX(constraint.transform.translationX);
                    view.setTranslationY(constraint.transform.translationY);
                    if (Build.VERSION.SDK_INT >= VERSION_CODES.LOLLIPOP) {
                        view.setTranslationZ(constraint.transform.translationZ);
                        if (constraint.transform.applyElevation) {
                            view.setElevation(constraint.transform.elevation);
                        }
                    }
                }
            } else {
                Log.v(TAG, "WARNING NO CONSTRAINTS for view " + id);
            }
        }
        for (Integer id : used) {
            Constraint constraint = mConstraints.get(id);
            if (constraint == null) {
                continue;
            }
            if (constraint.layout.mHelperType == BARRIER_TYPE) {
                Barrier barrier = new Barrier(constraintLayout.getContext());
                barrier.setId(id);
                if (constraint.layout.mReferenceIds != null) {
                    barrier.setReferencedIds(constraint.layout.mReferenceIds);
                } else if (constraint.layout.mReferenceIdString != null) {
                    constraint.layout.mReferenceIds = convertReferenceString(barrier,
                            constraint.layout.mReferenceIdString);
                    barrier.setReferencedIds(constraint.layout.mReferenceIds);
                }
                barrier.setType(constraint.layout.mBarrierDirection);
                barrier.setMargin(constraint.layout.mBarrierMargin);
                LayoutParams param = constraintLayout
                        .generateDefaultLayoutParams();
                barrier.validateParams();
                constraint.applyTo(param);
                constraintLayout.addView(barrier, param);
            }
            if (constraint.layout.mIsGuideline) {
                Guideline g = new Guideline(constraintLayout.getContext());
                g.setId(id);
                ConstraintLayout.LayoutParams param =
                        constraintLayout.generateDefaultLayoutParams();
                constraint.applyTo(param);
                constraintLayout.addView(g, param);
            }
        }
        for (int i = 0; i < count; i++) {
            View view = constraintLayout.getChildAt(i);
            if (view instanceof ConstraintHelper) {
                ConstraintHelper constraintHelper = (ConstraintHelper) view;
                constraintHelper.applyLayoutFeaturesInConstraintSet(constraintLayout);
            }
        }
    }

    /**
     * Center widget between the other two widgets.
     * (for sides see: {@link #TOP, {@link #BOTTOM}, {@link #START,
     * {@link #END}, {@link #LEFT, {@link #RIGHT})
     * Note, sides must be all vertical or horizontal sides.
     *
     * @param centerID     ID of the widget to be centered
     * @param firstID      ID of the first widget to connect the left or top of the widget to
     * @param firstSide    the side of the widget to connect to
     * @param firstMargin  the connection margin
     * @param secondId     the ID of the second widget to connect to right or top of the widget to
     * @param secondSide   the side of the widget to connect to
     * @param secondMargin the connection margin
     * @param bias         the ratio between two connections
     */
    public void center(int centerID,
                       int firstID, int firstSide, int firstMargin,
                       int secondId, int secondSide, int secondMargin,
                       float bias) {
        // Error checking

        if (firstMargin < 0) {
            throw new IllegalArgumentException("margin must be > 0");
        }
        if (secondMargin < 0) {
            throw new IllegalArgumentException("margin must be > 0");
        }
        if (bias <= 0 || bias > 1) {
            throw new IllegalArgumentException("bias must be between 0 and 1 inclusive");
        }

        if (firstSide == LEFT || firstSide == RIGHT) {
            connect(centerID, LEFT, firstID, firstSide, firstMargin);
            connect(centerID, RIGHT, secondId, secondSide, secondMargin);
            Constraint constraint = mConstraints.get(centerID);
            if (constraint != null) {
                constraint.layout.horizontalBias = bias;
            }
        } else if (firstSide == START || firstSide == END) {
            connect(centerID, START, firstID, firstSide, firstMargin);
            connect(centerID, END, secondId, secondSide, secondMargin);
            Constraint constraint = mConstraints.get(centerID);
            if (constraint != null) {
                constraint.layout.horizontalBias = bias;
            }
        } else {
            connect(centerID, TOP, firstID, firstSide, firstMargin);
            connect(centerID, BOTTOM, secondId, secondSide, secondMargin);
            Constraint constraint = mConstraints.get(centerID);
            if (constraint != null) {
                constraint.layout.verticalBias = bias;
            }
        }
    }

    /**
     * Centers the widget horizontally to the left and right side on another widgets sides.
     * (for sides see: {@link #START, {@link #END}, {@link #LEFT, {@link #RIGHT})
     *
     * @param centerID    ID of widget to be centered
     * @param leftId      The Id of the widget on the left side
     * @param leftSide    The side of the leftId widget to connect to
     * @param leftMargin  The margin on the left side
     * @param rightId     The Id of the widget on the right side
     * @param rightSide   The side  of the rightId widget to connect to
     * @param rightMargin The margin on the right side
     * @param bias        The ratio of the space on the left vs.
     * right sides 0.5 is centered (default)
     */
    public void centerHorizontally(int centerID,
                                   int leftId,
                                   int leftSide,
                                   int leftMargin,
                                   int rightId,
                                   int rightSide,
                                   int rightMargin,
                                   float bias) {
        connect(centerID, LEFT, leftId, leftSide, leftMargin);
        connect(centerID, RIGHT, rightId, rightSide, rightMargin);
        Constraint constraint = mConstraints.get(centerID);
        if (constraint != null) {
            constraint.layout.horizontalBias = bias;
        }
    }

    /**
     * Centers the widgets horizontally to the left and right side on another widgets sides.
     * (for sides see: {@link #START}, {@link #END},
     * {@link #LEFT}, {@link #RIGHT})
     *
     * @param centerID    ID of widget to be centered
     * @param startId     The Id of the widget on the start side (left in non rtl languages)
     * @param startSide   The side of the startId widget to connect to
     * @param startMargin The margin on the start side
     * @param endId       The Id of the widget on the start side (left in non rtl languages)
     * @param endSide     The side of the endId widget to connect to
     * @param endMargin   The margin on the end side
     * @param bias        The ratio of the space on the start vs end side 0.5 is centered (default)
     */
    public void centerHorizontallyRtl(int centerID, int startId, int startSide, int startMargin,
                                      int endId, int endSide, int endMargin, float bias) {
        connect(centerID, START, startId, startSide, startMargin);
        connect(centerID, END, endId, endSide, endMargin);
        Constraint constraint = mConstraints.get(centerID);
        if (constraint != null) {
            constraint.layout.horizontalBias = bias;
        }
    }

    /**
     * Centers the widgets vertically to the top and bottom side on another widgets sides.
     * (for sides see: {@link #TOP, {@link #BOTTOM})
     *
     * @param centerID     ID of widget to be centered
     * @param topId        The Id of the widget on the top side
     * @param topSide      The side of the leftId widget to connect to
     * @param topMargin    The margin on the top side
     * @param bottomId     The Id of the widget on the bottom side
     * @param bottomSide   The side of the bottomId widget to connect to
     * @param bottomMargin The margin on the bottom side
     * @param bias         The ratio of the space on the top vs.
     * bottom sides 0.5 is centered (default)
     */
    public void centerVertically(int centerID, int topId, int topSide, int topMargin, int bottomId,
                                 int bottomSide, int bottomMargin, float bias) {
        connect(centerID, TOP, topId, topSide, topMargin);
        connect(centerID, BOTTOM, bottomId, bottomSide, bottomMargin);
        Constraint constraint = mConstraints.get(centerID);
        if (constraint != null) {
            constraint.layout.verticalBias = bias;
        }
    }

    /**
     * Spaces a set of widgets vertically between the view topId and bottomId.
     * Widgets can be spaced with weights.
     * This operation sets all the related margins to 0.
     * <p>
     * (for sides see: {@link #TOP, {@link #BOTTOM})
     *
     * @param topId      The id of the widget to connect to or PARENT_ID
     * @param topSide    the side of the start to connect to
     * @param bottomId   The id of the widget to connect to or PARENT_ID
     * @param bottomSide the side of the right to connect to
     * @param chainIds   widgets to use as a chain
     * @param weights    can be null
     * @param style      set the style of the chain
     */
    public void createVerticalChain(int topId,
                                    int topSide,
                                    int bottomId,
                                    int bottomSide,
                                    int[] chainIds,
                                    float[] weights,
                                    int style) {
        if (chainIds.length < 2) {
            throw new IllegalArgumentException("must have 2 or more widgets in a chain");
        }
        if (weights != null && weights.length != chainIds.length) {
            throw new IllegalArgumentException("must have 2 or more widgets in a chain");
        }
        if (weights != null) {
            get(chainIds[0]).layout.verticalWeight = weights[0];
        }
        get(chainIds[0]).layout.verticalChainStyle = style;

        connect(chainIds[0], TOP, topId, topSide, 0);
        for (int i = 1; i < chainIds.length; i++) {
            connect(chainIds[i], TOP, chainIds[i - 1], BOTTOM, 0);
            connect(chainIds[i - 1], BOTTOM, chainIds[i], TOP, 0);
            if (weights != null) {
                get(chainIds[i]).layout.verticalWeight = weights[i];
            }
        }
        connect(chainIds[chainIds.length - 1], BOTTOM, bottomId, bottomSide, 0);
    }

    /**
     * Spaces a set of widgets horizontally between the view startID and endId.
     * Widgets can be spaced with weights.
     * This operation sets all the related margins to 0.
     * <p>
     * (for sides see: {@link #START, {@link #END},
     * {@link #LEFT, {@link #RIGHT}
     *
     * @param leftId    The id of the widget to connect to or PARENT_ID
     * @param leftSide  the side of the start to connect to
     * @param rightId   The id of the widget to connect to or PARENT_ID
     * @param rightSide the side of the right to connect to
     * @param chainIds  The widgets in the chain
     * @param weights   The weight to assign to each element in the chain or null
     * @param style     The type of chain
     */
    public void createHorizontalChain(int leftId,
                                      int leftSide,
                                      int rightId,
                                      int rightSide,
                                      int[] chainIds,
                                      float[] weights,
                                      int style) {
        createHorizontalChain(leftId, leftSide, rightId, rightSide,
                chainIds, weights, style, LEFT, RIGHT);
    }

    /**
     * Spaces a set of widgets horizontal between the view startID and endId.
     * Widgets can be spaced with weights.
     * (for sides see: {@link #START, {@link #END},
     * {@link #LEFT, {@link #RIGHT})
     *
     * @param startId   The id of the widget to connect to or PARENT_ID
     * @param startSide the side of the start to connect to
     * @param endId     The id of the widget to connect to or PARENT_ID
     * @param endSide   the side of the end to connect to
     * @param chainIds  The widgets in the chain
     * @param weights   The weight to assign to each element in the chain or null
     * @param style     The type of chain
     */
    public void createHorizontalChainRtl(int startId,
                                         int startSide,
                                         int endId,
                                         int endSide,
                                         int[] chainIds,
                                         float[] weights,
                                         int style) {
        createHorizontalChain(startId, startSide, endId, endSide,
                chainIds, weights, style, START, END);
    }

    private void createHorizontalChain(int leftId,
                                       int leftSide,
                                       int rightId,
                                       int rightSide,
                                       int[] chainIds,
                                       float[] weights,
                                       int style, int left, int right) {

        if (chainIds.length < 2) {
            throw new IllegalArgumentException("must have 2 or more widgets in a chain");
        }
        if (weights != null && weights.length != chainIds.length) {
            throw new IllegalArgumentException("must have 2 or more widgets in a chain");
        }
        if (weights != null) {
            get(chainIds[0]).layout.horizontalWeight = weights[0];
        }
        get(chainIds[0]).layout.horizontalChainStyle = style;
        connect(chainIds[0], left, leftId, leftSide, UNSET);
        for (int i = 1; i < chainIds.length; i++) {
            connect(chainIds[i], left, chainIds[i - 1], right, UNSET);
            connect(chainIds[i - 1], right, chainIds[i], left, UNSET);
            if (weights != null) {
                get(chainIds[i]).layout.horizontalWeight = weights[i];
            }
        }

        connect(chainIds[chainIds.length - 1], right, rightId, rightSide,
                UNSET);

    }

    /**
     * Create a constraint between two widgets.
     * (for sides see: {@link #TOP, {@link #BOTTOM}, {@link #START, {@link #END},
     * {@link #LEFT, {@link #RIGHT}, {@link #BASELINE})
     *
     * @param startID   the ID of the widget to be constrained
     * @param startSide the side of the widget to constrain
     * @param endID     the id of the widget to constrain to
     * @param endSide   the side of widget to constrain to
     * @param margin    the margin to constrain (margin must be positive)
     */
    public void connect(int startID, int startSide, int endID, int endSide, int margin) {
        if (!mConstraints.containsKey(startID)) {
            mConstraints.put(startID, new Constraint());
        }
        Constraint constraint = mConstraints.get(startID);
        if (constraint == null) {
            return;
        }
        switch (startSide) {
            case LEFT:
                if (endSide == LEFT) {
                    constraint.layout.leftToLeft = endID;
                    constraint.layout.leftToRight = Layout.UNSET;
                } else if (endSide == RIGHT) {
                    constraint.layout.leftToRight = endID;
                    constraint.layout.leftToLeft = Layout.UNSET;

                } else {
                    throw new IllegalArgumentException("Left to "
                            + sideToString(endSide) + " undefined");
                }
                constraint.layout.leftMargin = margin;
                break;
            case RIGHT:
                if (endSide == LEFT) {
                    constraint.layout.rightToLeft = endID;
                    constraint.layout.rightToRight = Layout.UNSET;

                } else if (endSide == RIGHT) {
                    constraint.layout.rightToRight = endID;
                    constraint.layout.rightToLeft = Layout.UNSET;

                } else {
                    throw new IllegalArgumentException("right to "
                            + sideToString(endSide) + " undefined");
                }
                constraint.layout.rightMargin = margin;
                break;
            case TOP:
                if (endSide == TOP) {
                    constraint.layout.topToTop = endID;
                    constraint.layout.topToBottom = Layout.UNSET;
                    constraint.layout.baselineToBaseline = Layout.UNSET;
                    constraint.layout.baselineToTop = Layout.UNSET;
                    constraint.layout.baselineToBottom = Layout.UNSET;
                } else if (endSide == BOTTOM) {
                    constraint.layout.topToBottom = endID;
                    constraint.layout.topToTop = Layout.UNSET;
                    constraint.layout.baselineToBaseline = Layout.UNSET;
                    constraint.layout.baselineToTop = Layout.UNSET;
                    constraint.layout.baselineToBottom = Layout.UNSET;
                } else {
                    throw new IllegalArgumentException("right to "
                            + sideToString(endSide) + " undefined");
                }
                constraint.layout.topMargin = margin;
                break;
            case BOTTOM:
                if (endSide == BOTTOM) {
                    constraint.layout.bottomToBottom = endID;
                    constraint.layout.bottomToTop = Layout.UNSET;
                    constraint.layout.baselineToBaseline = Layout.UNSET;
                    constraint.layout.baselineToTop = Layout.UNSET;
                    constraint.layout.baselineToBottom = Layout.UNSET;
                } else if (endSide == TOP) {
                    constraint.layout.bottomToTop = endID;
                    constraint.layout.bottomToBottom = Layout.UNSET;
                    constraint.layout.baselineToBaseline = Layout.UNSET;
                    constraint.layout.baselineToTop = Layout.UNSET;
                    constraint.layout.baselineToBottom = Layout.UNSET;
                } else {
                    throw new IllegalArgumentException("right to "
                            + sideToString(endSide) + " undefined");
                }
                constraint.layout.bottomMargin = margin;
                break;
            case BASELINE:
                if (endSide == BASELINE) {
                    constraint.layout.baselineToBaseline = endID;
                    constraint.layout.bottomToBottom = Layout.UNSET;
                    constraint.layout.bottomToTop = Layout.UNSET;
                    constraint.layout.topToTop = Layout.UNSET;
                    constraint.layout.topToBottom = Layout.UNSET;
                } else if (endSide == TOP) {
                    constraint.layout.baselineToTop = endID;
                    constraint.layout.bottomToBottom = Layout.UNSET;
                    constraint.layout.bottomToTop = Layout.UNSET;
                    constraint.layout.topToTop = Layout.UNSET;
                    constraint.layout.topToBottom = Layout.UNSET;
                } else if (endSide == BOTTOM) {
                    constraint.layout.baselineToBottom = endID;
                    constraint.layout.bottomToBottom = Layout.UNSET;
                    constraint.layout.bottomToTop = Layout.UNSET;
                    constraint.layout.topToTop = Layout.UNSET;
                    constraint.layout.topToBottom = Layout.UNSET;
                } else {
                    throw new IllegalArgumentException("right to "
                            + sideToString(endSide) + " undefined");
                }
                break;
            case START:
                if (endSide == START) {
                    constraint.layout.startToStart = endID;
                    constraint.layout.startToEnd = Layout.UNSET;
                } else if (endSide == END) {
                    constraint.layout.startToEnd = endID;
                    constraint.layout.startToStart = Layout.UNSET;
                } else {
                    throw new IllegalArgumentException("right to "
                            + sideToString(endSide) + " undefined");
                }
                constraint.layout.startMargin = margin;
                break;
            case END:
                if (endSide == END) {
                    constraint.layout.endToEnd = endID;
                    constraint.layout.endToStart = Layout.UNSET;
                } else if (endSide == START) {
                    constraint.layout.endToStart = endID;
                    constraint.layout.endToEnd = Layout.UNSET;
                } else {
                    throw new IllegalArgumentException("right to "
                            + sideToString(endSide) + " undefined");
                }
                constraint.layout.endMargin = margin;
                break;
            default:
                throw new IllegalArgumentException(
                        sideToString(startSide) + " to " + sideToString(endSide) + " unknown");
        }
    }

    /**
     * Create a constraint between two widgets.
     * (for sides see: {@link #TOP, {@link #BOTTOM}, {@link #START,
     * {@link #END}, {@link #LEFT, {@link #RIGHT}, {@link #BASELINE})
     *
     * @param startID   the ID of the widget to be constrained
     * @param startSide the side of the widget to constrain
     * @param endID     the id of the widget to constrain to
     * @param endSide   the side of widget to constrain to
     */
    public void connect(int startID, int startSide, int endID, int endSide) {
        if (!mConstraints.containsKey(startID)) {
            mConstraints.put(startID, new Constraint());
        }
        Constraint constraint = mConstraints.get(startID);
        if (constraint == null) {
            return;
        }
        switch (startSide) {
            case LEFT:
                if (endSide == LEFT) {
                    constraint.layout.leftToLeft = endID;
                    constraint.layout.leftToRight = Layout.UNSET;
                } else if (endSide == RIGHT) {
                    constraint.layout.leftToRight = endID;
                    constraint.layout.leftToLeft = Layout.UNSET;
                } else {
                    throw new IllegalArgumentException("left to "
                            + sideToString(endSide) + " undefined");
                }
                break;
            case RIGHT:
                if (endSide == LEFT) {
                    constraint.layout.rightToLeft = endID;
                    constraint.layout.rightToRight = Layout.UNSET;

                } else if (endSide == RIGHT) {
                    constraint.layout.rightToRight = endID;
                    constraint.layout.rightToLeft = Layout.UNSET;
                } else {
                    throw new IllegalArgumentException("right to "
                            + sideToString(endSide) + " undefined");
                }
                break;
            case TOP:
                if (endSide == TOP) {
                    constraint.layout.topToTop = endID;
                    constraint.layout.topToBottom = Layout.UNSET;
                    constraint.layout.baselineToBaseline = Layout.UNSET;
                    constraint.layout.baselineToTop = Layout.UNSET;
                    constraint.layout.baselineToBottom = Layout.UNSET;
                } else if (endSide == BOTTOM) {
                    constraint.layout.topToBottom = endID;
                    constraint.layout.topToTop = Layout.UNSET;
                    constraint.layout.baselineToBaseline = Layout.UNSET;
                    constraint.layout.baselineToTop = Layout.UNSET;
                    constraint.layout.baselineToBottom = Layout.UNSET;
                } else {
                    throw new IllegalArgumentException("right to "
                            + sideToString(endSide) + " undefined");
                }
                break;
            case BOTTOM:
                if (endSide == BOTTOM) {
                    constraint.layout.bottomToBottom = endID;
                    constraint.layout.bottomToTop = Layout.UNSET;
                    constraint.layout.baselineToBaseline = Layout.UNSET;
                    constraint.layout.baselineToTop = Layout.UNSET;
                    constraint.layout.baselineToBottom = Layout.UNSET;
                } else if (endSide == TOP) {
                    constraint.layout.bottomToTop = endID;
                    constraint.layout.bottomToBottom = Layout.UNSET;
                    constraint.layout.baselineToBaseline = Layout.UNSET;
                    constraint.layout.baselineToTop = Layout.UNSET;
                    constraint.layout.baselineToBottom = Layout.UNSET;
                } else {
                    throw new IllegalArgumentException("right to "
                            + sideToString(endSide) + " undefined");
                }
                break;
            case BASELINE:
                if (endSide == BASELINE) {
                    constraint.layout.baselineToBaseline = endID;
                    constraint.layout.bottomToBottom = Layout.UNSET;
                    constraint.layout.bottomToTop = Layout.UNSET;
                    constraint.layout.topToTop = Layout.UNSET;
                    constraint.layout.topToBottom = Layout.UNSET;
                } else if (endSide == TOP) {
                    constraint.layout.baselineToTop = endID;
                    constraint.layout.bottomToBottom = Layout.UNSET;
                    constraint.layout.bottomToTop = Layout.UNSET;
                    constraint.layout.topToTop = Layout.UNSET;
                    constraint.layout.topToBottom = Layout.UNSET;
                } else if (endSide == BOTTOM) {
                    constraint.layout.baselineToBottom = endID;
                    constraint.layout.bottomToBottom = Layout.UNSET;
                    constraint.layout.bottomToTop = Layout.UNSET;
                    constraint.layout.topToTop = Layout.UNSET;
                    constraint.layout.topToBottom = Layout.UNSET;
                } else {
                    throw new IllegalArgumentException("right to "
                            + sideToString(endSide) + " undefined");
                }
                break;
            case START:
                if (endSide == START) {
                    constraint.layout.startToStart = endID;
                    constraint.layout.startToEnd = Layout.UNSET;
                } else if (endSide == END) {
                    constraint.layout.startToEnd = endID;
                    constraint.layout.startToStart = Layout.UNSET;
                } else {
                    throw new IllegalArgumentException("right to "
                            + sideToString(endSide) + " undefined");
                }
                break;
            case END:
                if (endSide == END) {
                    constraint.layout.endToEnd = endID;
                    constraint.layout.endToStart = Layout.UNSET;
                } else if (endSide == START) {
                    constraint.layout.endToStart = endID;
                    constraint.layout.endToEnd = Layout.UNSET;
                } else {
                    throw new IllegalArgumentException("right to "
                            + sideToString(endSide) + " undefined");
                }
                break;
            default:
                throw new IllegalArgumentException(
                        sideToString(startSide) + " to " + sideToString(endSide) + " unknown");
        }
    }

    /**
     * Centers the view horizontally relative to toView's position.
     *
     * @param viewId ID of view to center Horizontally
     * @param toView ID of view to center on (or in)
     */
    public void centerHorizontally(int viewId, int toView) {
        if (toView == PARENT_ID) {
            center(viewId, PARENT_ID, ConstraintSet.LEFT, 0, PARENT_ID,
                    ConstraintSet.RIGHT, 0, 0.5f);
        } else {
            center(viewId, toView, ConstraintSet.RIGHT, 0, toView,
                    ConstraintSet.LEFT, 0, 0.5f);
        }
    }

    /**
     * Centers the view horizontally relative to toView's position.
     *
     * @param viewId ID of view to center Horizontally
     * @param toView ID of view to center on (or in)
     */
    public void centerHorizontallyRtl(int viewId, int toView) {
        if (toView == PARENT_ID) {
            center(viewId, PARENT_ID, ConstraintSet.START, 0, PARENT_ID,
                    ConstraintSet.END, 0, 0.5f);
        } else {
            center(viewId, toView, ConstraintSet.END, 0, toView,
                    ConstraintSet.START, 0, 0.5f);
        }
    }

    /**
     * Centers the view vertically relative to toView's position.
     *
     * @param viewId ID of view to center Horizontally
     * @param toView ID of view to center on (or in)
     */
    public void centerVertically(int viewId, int toView) {
        if (toView == PARENT_ID) {
            center(viewId, PARENT_ID, ConstraintSet.TOP, 0, PARENT_ID,
                    ConstraintSet.BOTTOM, 0, 0.5f);
        } else {
            center(viewId, toView, ConstraintSet.BOTTOM, 0, toView, ConstraintSet.TOP,
                    0, 0.5f);
        }
    }

    /**
     * Remove all constraints from this view.
     *
     * @param viewId ID of view to remove all connections to
     */
    public void clear(int viewId) {
        mConstraints.remove(viewId);
    }

    /**
     * Remove a constraint from this view.
     *
     * @param viewId ID of view to center on (or in)
     * @param anchor the Anchor to remove constraint from
     */
    public void clear(int viewId, int anchor) {
        if (mConstraints.containsKey(viewId)) {
            Constraint constraint = mConstraints.get(viewId);
            if (constraint == null) {
                return;
            }
            switch (anchor) {
                case LEFT:
                    constraint.layout.leftToRight = Layout.UNSET;
                    constraint.layout.leftToLeft = Layout.UNSET;
                    constraint.layout.leftMargin = Layout.UNSET;
                    constraint.layout.goneLeftMargin = Layout.UNSET_GONE_MARGIN;
                    break;
                case RIGHT:
                    constraint.layout.rightToRight = Layout.UNSET;
                    constraint.layout.rightToLeft = Layout.UNSET;
                    constraint.layout.rightMargin = Layout.UNSET;
                    constraint.layout.goneRightMargin = Layout.UNSET_GONE_MARGIN;
                    break;
                case TOP:
                    constraint.layout.topToBottom = Layout.UNSET;
                    constraint.layout.topToTop = Layout.UNSET;
                    constraint.layout.topMargin = 0;
                    constraint.layout.goneTopMargin = Layout.UNSET_GONE_MARGIN;
                    break;
                case BOTTOM:
                    constraint.layout.bottomToTop = Layout.UNSET;
                    constraint.layout.bottomToBottom = Layout.UNSET;
                    constraint.layout.bottomMargin = 0;
                    constraint.layout.goneBottomMargin = Layout.UNSET_GONE_MARGIN;
                    break;
                case BASELINE:
                    constraint.layout.baselineToBaseline = Layout.UNSET;
                    constraint.layout.baselineToTop = Layout.UNSET;
                    constraint.layout.baselineToBottom = Layout.UNSET;
                    constraint.layout.baselineMargin = 0;
                    constraint.layout.goneBaselineMargin = Layout.UNSET_GONE_MARGIN;
                    break;
                case START:
                    constraint.layout.startToEnd = Layout.UNSET;
                    constraint.layout.startToStart = Layout.UNSET;
                    constraint.layout.startMargin = 0;
                    constraint.layout.goneStartMargin = Layout.UNSET_GONE_MARGIN;
                    break;
                case END:
                    constraint.layout.endToStart = Layout.UNSET;
                    constraint.layout.endToEnd = Layout.UNSET;
                    constraint.layout.endMargin = 0;
                    constraint.layout.goneEndMargin = Layout.UNSET_GONE_MARGIN;
                    break;
                case CIRCLE_REFERENCE:
                    constraint.layout.circleAngle = Layout.UNSET;
                    constraint.layout.circleRadius = Layout.UNSET;
                    constraint.layout.circleConstraint = Layout.UNSET;
                    break;
                default:
                    throw new IllegalArgumentException("unknown constraint");
            }
        }
    }

    /**
     * Sets the margin.
     *
     * @param viewId ID of view to adjust the margin on
     * @param anchor The side to adjust the margin on
     * @param value  The new value for the margin
     */
    public void setMargin(int viewId, int anchor, int value) {
        Constraint constraint = get(viewId);
        switch (anchor) {
            case LEFT:
                constraint.layout.leftMargin = value;
                break;
            case RIGHT:
                constraint.layout.rightMargin = value;
                break;
            case TOP:
                constraint.layout.topMargin = value;
                break;
            case BOTTOM:
                constraint.layout.bottomMargin = value;
                break;
            case BASELINE:
                constraint.layout.baselineMargin = value;
                break;
            case START:
                constraint.layout.startMargin = value;
                break;
            case END:
                constraint.layout.endMargin = value;
                break;
            default:
                throw new IllegalArgumentException("unknown constraint");
        }
    }

    /**
     * Sets the gone margin.
     *
     * @param viewId ID of view to adjust the margin on
     * @param anchor The side to adjust the margin on
     * @param value  The new value for the margin
     */
    public void setGoneMargin(int viewId, int anchor, int value) {
        Constraint constraint = get(viewId);
        switch (anchor) {
            case LEFT:
                constraint.layout.goneLeftMargin = value;
                break;
            case RIGHT:
                constraint.layout.goneRightMargin = value;
                break;
            case TOP:
                constraint.layout.goneTopMargin = value;
                break;
            case BOTTOM:
                constraint.layout.goneBottomMargin = value;
                break;
            case BASELINE:
                constraint.layout.goneBaselineMargin = value;
                break;
            case START:
                constraint.layout.goneStartMargin = value;
                break;
            case END:
                constraint.layout.goneEndMargin = value;
                break;
            default:
                throw new IllegalArgumentException("unknown constraint");
        }
    }

    /**
     * Adjust the horizontal bias of the view (used with views constrained on left and right).
     *
     * @param viewId ID of view to adjust the horizontal
     * @param bias   the new bias 0.5 is in the middle
     */
    public void setHorizontalBias(int viewId, float bias) {
        get(viewId).layout.horizontalBias = bias;
    }

    /**
     * Adjust the vertical bias of the view (used with views constrained on left and right).
     *
     * @param viewId ID of view to adjust the vertical
     * @param bias   the new bias 0.5 is in the middle
     */
    public void setVerticalBias(int viewId, float bias) {
        get(viewId).layout.verticalBias = bias;
    }

    /**
     * Constrains the views aspect ratio.
     * For Example a HD screen is 16 by 9 = 16/(float)9 = 1.777f.
     *
     * @param viewId ID of view to constrain
     * @param ratio  The ratio of the width to height (width / height)
     */
    public void setDimensionRatio(int viewId, String ratio) {
        get(viewId).layout.dimensionRatio = ratio;
    }

    /**
     * Adjust the visibility of a view.
     *
     * @param viewId     ID of view to adjust the vertical
     * @param visibility the visibility
     */
    public void setVisibility(int viewId, int visibility) {
        get(viewId).propertySet.visibility = visibility;
    }

    /**
     * ConstraintSet will not setVisibility. {@link #VISIBILITY_MODE_IGNORE} or {@link
     * #VISIBILITY_MODE_NORMAL}.
     *
     * @param viewId         ID of view
     * @param visibilityMode
     */
    public void setVisibilityMode(int viewId, int visibilityMode) {
        get(viewId).propertySet.mVisibilityMode = visibilityMode;
    }

    /**
     * ConstraintSet will not setVisibility. {@link #VISIBILITY_MODE_IGNORE} or {@link
     * #VISIBILITY_MODE_NORMAL}.
     *
     * @param viewId ID of view
     */
    public int getVisibilityMode(int viewId) {
        return get(viewId).propertySet.mVisibilityMode;
    }

    /**
     * Get the visibility flag set in this view
     *
     * @param viewId the id of the view
     * @return the visibility constraint for the view
     */
    public int getVisibility(int viewId) {
        return get(viewId).propertySet.visibility;
    }

    /**
     * Get the height set in the view
     *
     * @param viewId the id of the view
     * @return return the height constraint of the view
     */
    public int getHeight(int viewId) {
        return get(viewId).layout.mHeight;
    }

    /**
     * Get the width set in the view
     *
     * @param viewId the id of the view
     * @return return the width constraint of the view
     */
    public int getWidth(int viewId) {
        return get(viewId).layout.mWidth;
    }

    /**
     * Adjust the alpha of a view.
     *
     * @param viewId ID of view to adjust the vertical
     * @param alpha  the alpha
     */
    public void setAlpha(int viewId, float alpha) {
        get(viewId).propertySet.alpha = alpha;
    }

    /**
     * return with the constraint set will apply elevation for the specified view.
     *
     * @return true if the elevation will be set on this view (default is false)
     */
    public boolean getApplyElevation(int viewId) {
        return get(viewId).transform.applyElevation;
    }

    /**
     * set if elevation will be applied to the view.
     * Elevation logic is based on style and animation. By default it is not used because it would
     * lead to unexpected results.
     *
     * @param apply true if this constraint set applies elevation to this view
     */
    public void setApplyElevation(int viewId, boolean apply) {
        if (Build.VERSION.SDK_INT >= VERSION_CODES.LOLLIPOP) {
            get(viewId).transform.applyElevation = apply;
        }
    }

    /**
     * Adjust the elevation of a view.
     *
     * @param viewId    ID of view to adjust the elevation
     * @param elevation the elevation
     */
    public void setElevation(int viewId, float elevation) {
        if (Build.VERSION.SDK_INT >= VERSION_CODES.LOLLIPOP) {
            get(viewId).transform.elevation = elevation;
            get(viewId).transform.applyElevation = true;
        }
    }

    /**
     * Adjust the post-layout rotation about the Z axis of a view.
     *
     * @param viewId   ID of view to adjust th Z rotation
     * @param rotation the rotation about the X axis
     */
    public void setRotation(int viewId, float rotation) {
        get(viewId).transform.rotation = rotation;
    }

    /**
     * Adjust the post-layout rotation about the X axis of a view.
     *
     * @param viewId    ID of view to adjust th X rotation
     * @param rotationX the rotation about the X axis
     */
    public void setRotationX(int viewId, float rotationX) {
        get(viewId).transform.rotationX = rotationX;
    }

    /**
     * Adjust the post-layout rotation about the Y axis of a view.
     *
     * @param viewId    ID of view to adjust the Y rotation
     * @param rotationY the rotationY
     */
    public void setRotationY(int viewId, float rotationY) {
        get(viewId).transform.rotationY = rotationY;
    }

    /**
     * Adjust the post-layout scale in X of a view.
     *
     * @param viewId ID of view to adjust the scale in X
     * @param scaleX the scale in X
     */
    public void setScaleX(int viewId, float scaleX) {
        get(viewId).transform.scaleX = scaleX;
    }

    /**
     * Adjust the post-layout scale in Y of a view.
     *
     * @param viewId ID of view to adjust the scale in Y
     * @param scaleY the scale in Y
     */
    public void setScaleY(int viewId, float scaleY) {
        get(viewId).transform.scaleY = scaleY;
    }

    /**
     * Set X location of the pivot point around which the view will rotate and scale.
     * use Float.NaN to clear the pivot value.
     * Note: once an actual View has had its pivot set it cannot be cleared.
     *
     * @param viewId          ID of view to adjust the transforms pivot point about X
     * @param transformPivotX X location of the pivot point.
     */
    public void setTransformPivotX(int viewId, float transformPivotX) {
        get(viewId).transform.transformPivotX = transformPivotX;
    }

    /**
     * Set Y location of the pivot point around which the view will rotate and scale.
     * use Float.NaN to clear the pivot value.
     * Note: once an actual View has had its pivot set it cannot be cleared.
     *
     * @param viewId          ID of view to adjust the transforms pivot point about Y
     * @param transformPivotY Y location of the pivot point.
     */
    public void setTransformPivotY(int viewId, float transformPivotY) {
        get(viewId).transform.transformPivotY = transformPivotY;
    }

    /**
     * Set X,Y location of the pivot point around which the view will rotate and scale.
     * use Float.NaN to clear the pivot value.
     * Note: once an actual View has had its pivot set it cannot be cleared.
     *
     * @param viewId          ID of view to adjust the transforms pivot point
     * @param transformPivotX X location of the pivot point.
     * @param transformPivotY Y location of the pivot point.
     */
    public void setTransformPivot(int viewId, float transformPivotX, float transformPivotY) {
        Constraint constraint = get(viewId);
        constraint.transform.transformPivotY = transformPivotY;
        constraint.transform.transformPivotX = transformPivotX;
    }

    /**
     * Adjust the post-layout X translation of a view.
     *
     * @param viewId       ID of view to translate in X
     * @param translationX the translation in X
     */
    public void setTranslationX(int viewId, float translationX) {
        get(viewId).transform.translationX = translationX;
    }

    /**
     * Adjust the  post-layout Y translation of a view.
     *
     * @param viewId       ID of view to to translate in Y
     * @param translationY the translation in Y
     */
    public void setTranslationY(int viewId, float translationY) {
        get(viewId).transform.translationY = translationY;
    }

    /**
     * Adjust the post-layout translation of a view.
     *
     * @param viewId       ID of view to adjust its translation in X & Y
     * @param translationX the translation in X
     * @param translationY the translation in Y
     */
    public void setTranslation(int viewId, float translationX, float translationY) {
        Constraint constraint = get(viewId);
        constraint.transform.translationX = translationX;
        constraint.transform.translationY = translationY;
    }

    /**
     * Adjust the translation in Z of a view.
     *
     * @param viewId       ID of view to adjust
     * @param translationZ the translationZ
     */
    public void setTranslationZ(int viewId, float translationZ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            get(viewId).transform.translationZ = translationZ;
        }
    }

    /**
     *
     */
    public void setEditorAbsoluteX(int viewId, int position) {
        get(viewId).layout.editorAbsoluteX = position;
    }

    /**
     *
     */
    public void setEditorAbsoluteY(int viewId, int position) {
        get(viewId).layout.editorAbsoluteY = position;
    }

    /**
     * Sets the wrap behavior of the widget in the parent's wrap computation
     */
    public void setLayoutWrapBehavior(int viewId, int behavior) {
        if (behavior >= 0 && behavior <= ConstraintWidget.WRAP_BEHAVIOR_SKIPPED) {
            get(viewId).layout.mWrapBehavior = behavior;
        }
    }

    /**
     * Sets the height of the view. It can be a dimension, {@link #WRAP_CONTENT} or {@link
     * #MATCH_CONSTRAINT}.
     *
     * @param viewId ID of view to adjust its height
     * @param height the height of the view
     * @since 1.1
     */
    public void constrainHeight(int viewId, int height) {
        get(viewId).layout.mHeight = height;
    }

    /**
     * Sets the width of the view. It can be a dimension, {@link #WRAP_CONTENT} or {@link
     * #MATCH_CONSTRAINT}.
     *
     * @param viewId ID of view to adjust its width
     * @param width  the width of the view
     * @since 1.1
     */
    public void constrainWidth(int viewId, int width) {
        get(viewId).layout.mWidth = width;
    }

    /**
     * Constrain the view on a circle constraint
     *
     * @param viewId ID of the view we constrain
     * @param id     ID of the view we constrain relative to
     * @param radius the radius of the circle in degrees
     * @param angle  the angle
     * @since 1.1
     */
    public void constrainCircle(int viewId, int id, int radius, float angle) {
        Constraint constraint = get(viewId);
        constraint.layout.circleConstraint = id;
        constraint.layout.circleRadius = radius;
        constraint.layout.circleAngle = angle;
    }

    /**
     * Sets the maximum height of the view. It is a dimension, It is only applicable if height is
     * #MATCH_CONSTRAINT}.
     *
     * @param viewId ID of view to adjust it height
     * @param height the maximum height of the constraint
     * @since 1.1
     */
    public void constrainMaxHeight(int viewId, int height) {
        get(viewId).layout.heightMax = height;
    }

    /**
     * Sets the maximum width of the view. It is a dimension, It is only applicable if width is
     * #MATCH_CONSTRAINT}.
     *
     * @param viewId ID of view to adjust its max height
     * @param width  the maximum width of the view
     * @since 1.1
     */
    public void constrainMaxWidth(int viewId, int width) {
        get(viewId).layout.widthMax = width;
    }

    /**
     * Sets the height of the view. It is a dimension, It is only applicable if height is
     * #MATCH_CONSTRAINT}.
     *
     * @param viewId ID of view to adjust its min height
     * @param height the minimum height of the view
     * @since 1.1
     */
    public void constrainMinHeight(int viewId, int height) {
        get(viewId).layout.heightMin = height;
    }

    /**
     * Sets the width of the view.  It is a dimension, It is only applicable if width is
     * #MATCH_CONSTRAINT}.
     *
     * @param viewId ID of view to adjust its min height
     * @param width  the minimum width of the view
     * @since 1.1
     */
    public void constrainMinWidth(int viewId, int width) {
        get(viewId).layout.widthMin = width;
    }

    /**
     * Sets the width of the view as a percentage of the parent.
     *
     * @param viewId
     * @param percent
     * @since 1.1
     */
    public void constrainPercentWidth(int viewId, float percent) {
        get(viewId).layout.widthPercent = percent;
    }

    /**
     * Sets the height of the view as a percentage of the parent.
     *
     * @param viewId
     * @param percent
     * @since 1.1
     */
    public void constrainPercentHeight(int viewId, float percent) {
        get(viewId).layout.heightPercent = percent;
    }

    /**
     * Sets how the height is calculated ether MATCH_CONSTRAINT_WRAP or MATCH_CONSTRAINT_SPREAD.
     * Default is spread.
     *
     * @param viewId ID of view to adjust its matchConstraintDefaultHeight
     * @param height MATCH_CONSTRAINT_WRAP or MATCH_CONSTRAINT_SPREAD
     * @since 1.1
     */
    public void constrainDefaultHeight(int viewId, int height) {
        get(viewId).layout.heightDefault = height;
    }

    /**
     * Sets how the width is calculated ether MATCH_CONSTRAINT_WRAP or MATCH_CONSTRAINT_SPREAD.
     * Default is spread.
     *
     * @param viewId      ID of view to adjust its matchConstraintDefaultWidth
     * @param constrained if true with will be constrained
     * @since 1.1
     */
    public void constrainedWidth(int viewId, boolean constrained) {
        get(viewId).layout.constrainedWidth = constrained;
    }

    /**
     * Sets how the height is calculated ether MATCH_CONSTRAINT_WRAP or MATCH_CONSTRAINT_SPREAD.
     * Default is spread.
     *
     * @param viewId      ID of view to adjust its matchConstraintDefaultHeight
     * @param constrained if true height will be constrained
     * @since 1.1
     */
    public void constrainedHeight(int viewId, boolean constrained) {
        get(viewId).layout.constrainedHeight = constrained;
    }

    /**
     * Sets how the width is calculated ether MATCH_CONSTRAINT_WRAP or MATCH_CONSTRAINT_SPREAD.
     * Default is spread.
     *
     * @param viewId ID of view to adjust its matchConstraintDefaultWidth
     * @param width  SPREAD or WRAP
     * @since 1.1
     */
    public void constrainDefaultWidth(int viewId, int width) {
        get(viewId).layout.widthDefault = width;
    }

    /**
     * The child's weight that we can use to distribute the available horizontal space
     * in a chain, if the dimension behaviour is set to MATCH_CONSTRAINT
     *
     * @param viewId ID of view to adjust its HorizontalWeight
     * @param weight the weight that we can use to distribute the horizontal space
     */
    public void setHorizontalWeight(int viewId, float weight) {
        get(viewId).layout.horizontalWeight = weight;
    }

    /**
     * The child's weight that we can use to distribute the available vertical space
     * in a chain, if the dimension behaviour is set to MATCH_CONSTRAINT
     *
     * @param viewId ID of view to adjust its VerticalWeight
     * @param weight the weight that we can use to distribute the vertical space
     */
    public void setVerticalWeight(int viewId, float weight) {
        get(viewId).layout.verticalWeight = weight;
    }

    /**
     * How the elements of the horizontal chain will be positioned. if the dimension
     * behaviour is set to MATCH_CONSTRAINT. The possible values are:
     *
     * <ul>
     * <li>{@link #CHAIN_SPREAD} -- the elements will be spread out</li>
     * <li>{@link #CHAIN_SPREAD_INSIDE} -- similar, but the endpoints of the chain will not
     * be spread out</li>
     * <li>{@link #CHAIN_PACKED} -- the elements of the chain will be packed together. The
     * horizontal bias attribute of the child will then affect the positioning of the packed
     * elements</li>
     * </ul>
     *
     * @param viewId     ID of view to adjust its HorizontalChainStyle
     * @param chainStyle the weight that we can use to distribute the horizontal space
     */
    public void setHorizontalChainStyle(int viewId, int chainStyle) {
        get(viewId).layout.horizontalChainStyle = chainStyle;
    }

    /**
     * How the elements of the vertical chain will be positioned. in a chain, if the dimension
     * behaviour is set to MATCH_CONSTRAINT
     *
     * <ul>
     * <li>{@link #CHAIN_SPREAD} -- the elements will be spread out</li>
     * <li>{@link #CHAIN_SPREAD_INSIDE} -- similar, but the endpoints of the chain will not
     * be spread out</li>
     * <li>{@link #CHAIN_PACKED} -- the elements of the chain will be packed together. The
     * vertical bias attribute of the child will then affect the positioning of the packed
     * elements</li>
     * </ul>
     *
     * @param viewId     ID of view to adjust its VerticalChainStyle
     * @param chainStyle the weight that we can use to distribute the horizontal space
     */
    public void setVerticalChainStyle(int viewId, int chainStyle) {
        get(viewId).layout.verticalChainStyle = chainStyle;
    }

    /**
     * Adds a view to a horizontal chain.
     *
     * @param viewId  view to add
     * @param leftId  view in chain to the left
     * @param rightId view in chain to the right
     */
    public void addToHorizontalChain(int viewId, int leftId, int rightId) {
        connect(viewId, LEFT, leftId, (leftId == PARENT_ID) ? LEFT : RIGHT, 0);
        connect(viewId, RIGHT, rightId, (rightId == PARENT_ID) ? RIGHT : LEFT, 0);
        if (leftId != PARENT_ID) {
            connect(leftId, RIGHT, viewId, LEFT, 0);
        }
        if (rightId != PARENT_ID) {
            connect(rightId, LEFT, viewId, RIGHT, 0);
        }
    }

    /**
     * Adds a view to a horizontal chain.
     *
     * @param viewId  view to add
     * @param leftId  view to the start side
     * @param rightId view to the end side
     */
    public void addToHorizontalChainRTL(int viewId, int leftId, int rightId) {
        connect(viewId, START, leftId, (leftId == PARENT_ID) ? START : END, 0);
        connect(viewId, END, rightId, (rightId == PARENT_ID) ? END : START, 0);
        if (leftId != PARENT_ID) {
            connect(leftId, END, viewId, START, 0);
        }
        if (rightId != PARENT_ID) {
            connect(rightId, START, viewId, END, 0);
        }
    }

    /**
     * Adds a view to a vertical chain.
     *
     * @param viewId   view to add to a vertical chain
     * @param topId    view above.
     * @param bottomId view below
     */
    public void addToVerticalChain(int viewId, int topId, int bottomId) {
        connect(viewId, TOP, topId, (topId == PARENT_ID) ? TOP : BOTTOM, 0);
        connect(viewId, BOTTOM, bottomId, (bottomId == PARENT_ID) ? BOTTOM : TOP, 0);
        if (topId != PARENT_ID) {
            connect(topId, BOTTOM, viewId, TOP, 0);
        }
        if (bottomId != PARENT_ID) {
            connect(bottomId, TOP, viewId, BOTTOM, 0);
        }
    }

    /**
     * Removes a view from a vertical chain.
     * This assumes the view is connected to a vertical chain.
     * Its behaviour is undefined if not part of a vertical chain.
     *
     * @param viewId the view to be removed
     */
    public void removeFromVerticalChain(int viewId) {
        if (mConstraints.containsKey(viewId)) {
            Constraint constraint = mConstraints.get(viewId);
            if (constraint == null) {
                return;
            }
            int topId = constraint.layout.topToBottom;
            int bottomId = constraint.layout.bottomToTop;
            if (topId != Layout.UNSET || bottomId != Layout.UNSET) {
                if (topId != Layout.UNSET && bottomId != Layout.UNSET) {
                    // top and bottom connected to views
                    connect(topId, BOTTOM, bottomId, TOP, 0);
                    connect(bottomId, TOP, topId, BOTTOM, 0);
                } else if (constraint.layout.bottomToBottom != Layout.UNSET) {
                    // top connected to view. Bottom connected to parent
                    connect(topId, BOTTOM, constraint.layout.bottomToBottom, BOTTOM, 0);
                } else if (constraint.layout.topToTop != Layout.UNSET) {
                    // bottom connected to view. Top connected to parent
                    connect(bottomId, TOP, constraint.layout.topToTop, TOP, 0);
                }
            }
        }
        clear(viewId, TOP);
        clear(viewId, BOTTOM);
    }

    /**
     * Removes a view from a horizontal chain.
     * This assumes the view is connected to a horizontal chain.
     * Its behaviour is undefined if not part of a horizontal chain.
     *
     * @param viewId the view to be removed
     */
    public void removeFromHorizontalChain(int viewId) {
        if (mConstraints.containsKey(viewId)) {
            Constraint constraint = mConstraints.get(viewId);
            if (constraint == null) {
                return;
            }
            int leftId = constraint.layout.leftToRight;
            int rightId = constraint.layout.rightToLeft;
            if (leftId != Layout.UNSET || rightId != Layout.UNSET) {
                if (leftId != Layout.UNSET && rightId != Layout.UNSET) {
                    // left and right connected to views
                    connect(leftId, RIGHT, rightId, LEFT, 0);
                    connect(rightId, LEFT, leftId, RIGHT, 0);
                } else if (constraint.layout.rightToRight != Layout.UNSET) {
                    // left connected to view. right connected to parent
                    connect(leftId, RIGHT, constraint.layout.rightToRight, RIGHT, 0);
                } else if (constraint.layout.leftToLeft != Layout.UNSET) {
                    // right connected to view. left connected to parent
                    connect(rightId, LEFT, constraint.layout.leftToLeft, LEFT, 0);
                }
                clear(viewId, LEFT);
                clear(viewId, RIGHT);
            } else {

                int startId = constraint.layout.startToEnd;
                int endId = constraint.layout.endToStart;
                if (startId != Layout.UNSET || endId != Layout.UNSET) {
                    if (startId != Layout.UNSET && endId != Layout.UNSET) {
                        // start and end connected to views
                        connect(startId, END, endId, START, 0);
                        connect(endId, START, leftId, END, 0);
                    } else if (endId != Layout.UNSET) {
                        if (constraint.layout.rightToRight != Layout.UNSET) {
                            // left connected to view. right connected to parent
                            connect(leftId, END, constraint.layout.rightToRight, END, 0);
                        } else if (constraint.layout.leftToLeft != Layout.UNSET) {
                            // right connected to view. left connected to parent
                            connect(endId, START, constraint.layout.leftToLeft, START, 0);
                        }
                    }
                }
                clear(viewId, START);
                clear(viewId, END);
            }
        }
    }

    /**
     * Creates a ConstraintLayout virtual object. Currently only horizontal or vertical GuideLines.
     *
     * @param guidelineID ID of guideline to create
     * @param orientation the Orientation of the guideline
     */
    public void create(int guidelineID, int orientation) {
        Constraint constraint = get(guidelineID);
        constraint.layout.mIsGuideline = true;
        constraint.layout.orientation = orientation;
    }

    /**
     * Creates a ConstraintLayout Barrier object.
     *
     * @param id
     * @param direction  Barrier.{LEFT,RIGHT,TOP,BOTTOM,START,END}
     * @param referenced
     * @since 1.1
     */
    public void createBarrier(int id, int direction, int margin, int... referenced) {
        Constraint constraint = get(id);
        constraint.layout.mHelperType = BARRIER_TYPE;
        constraint.layout.mBarrierDirection = direction;
        constraint.layout.mBarrierMargin = margin;
        constraint.layout.mIsGuideline = false;
        constraint.layout.mReferenceIds = referenced;
    }

    /**
     * Set the guideline's distance form the top or left edge.
     *
     * @param guidelineID ID of the guideline
     * @param margin      the distance to the top or left edge
     */
    public void setGuidelineBegin(int guidelineID, int margin) {
        get(guidelineID).layout.guideBegin = margin;
        get(guidelineID).layout.guideEnd = Layout.UNSET;
        get(guidelineID).layout.guidePercent = Layout.UNSET;

    }

    /**
     * Set a guideline's distance to end.
     *
     * @param guidelineID ID of the guideline
     * @param margin      the margin to the right or bottom side of container
     */
    public void setGuidelineEnd(int guidelineID, int margin) {
        get(guidelineID).layout.guideEnd = margin;
        get(guidelineID).layout.guideBegin = Layout.UNSET;
        get(guidelineID).layout.guidePercent = Layout.UNSET;
    }

    /**
     * Set a Guideline's percent.
     *
     * @param guidelineID ID of the guideline
     * @param ratio       the ratio between the gap on the left and right
     *                   0.0 is top/left 0.5 is middle
     */
    public void setGuidelinePercent(int guidelineID, float ratio) {
        get(guidelineID).layout.guidePercent = ratio;
        get(guidelineID).layout.guideEnd = Layout.UNSET;
        get(guidelineID).layout.guideBegin = Layout.UNSET;
    }

    /**
     * get the reference id's of a helper.
     *
     * @param id
     * @return array of id's
     */
    public int[] getReferencedIds(int id) {
        Constraint constraint = get(id);
        if (constraint.layout.mReferenceIds == null) {
            return new int[0];
        }
        return Arrays.copyOf(constraint.layout.mReferenceIds,
                constraint.layout.mReferenceIds.length);
    }

    /**
     * sets the reference id's of a barrier.
     *
     * @param id
     * @param referenced
     * @since 2.0
     */
    public void setReferencedIds(int id, int... referenced) {
        Constraint constraint = get(id);
        constraint.layout.mReferenceIds = referenced;
    }

    /**
     * SEt tye type of barier
     * @param id
     * @param type
     */
    public void setBarrierType(int id, int type) {
        Constraint constraint = get(id);
        constraint.layout.mHelperType = type;
    }

    /**
     * Remove the attribute
     * @param attributeName
     */
    public void removeAttribute(String attributeName) {
        mSavedAttributes.remove(attributeName);
    }

    /**
     * Set the value of an attribute of type int
     * @param viewId
     * @param attributeName
     * @param value
     */
    public void setIntValue(int viewId, String attributeName, int value) {
        get(viewId).setIntValue(attributeName, value);
    }

    /**
     * Set the value of an attribute of type color
     * @param viewId
     * @param attributeName
     * @param value
     */
    public void setColorValue(int viewId, String attributeName, int value) {
        get(viewId).setColorValue(attributeName, value);
    }

    /**
     * Set the value of an attribute of type float
     * @param viewId
     * @param attributeName
     * @param value
     */
    public void setFloatValue(int viewId, String attributeName, float value) {
        get(viewId).setFloatValue(attributeName, value);
    }

    /**
     * Set the value of an attribute of type string
     * @param viewId
     * @param attributeName
     * @param value
     */
    public void setStringValue(int viewId, String attributeName, String value) {
        get(viewId).setStringValue(attributeName, value);
    }

    private void addAttributes(AttributeType attributeType, String... attributeName) {
        ConstraintAttribute constraintAttribute = null;
        for (int i = 0; i < attributeName.length; i++) {
            if (mSavedAttributes.containsKey(attributeName[i])) {
                constraintAttribute = mSavedAttributes.get(attributeName[i]);
                if (constraintAttribute == null) {
                    continue;
                }
                if (constraintAttribute.getType() != attributeType) {
                    throw new IllegalArgumentException(
                            "ConstraintAttribute is already a "
                                    + constraintAttribute.getType().name());
                }
            } else {
                constraintAttribute = new ConstraintAttribute(attributeName[i], attributeType);
                mSavedAttributes.put(attributeName[i], constraintAttribute);
            }
        }
    }

    /**
     * Parse int
     * @param set
     * @param attributes
     */
    public void parseIntAttributes(Constraint set, String attributes) {
        String[] sp = attributes.split(",");
        for (int i = 0; i < sp.length; i++) {
            String[] attr = sp[i].split("=");
            if (attr.length != 2) {
                Log.w(TAG, " Unable to parse " + sp[i]);
            } else {
                set.setFloatValue(attr[0], Integer.decode(attr[1]));
            }
        }
    }

    /**
     * Parse color
     * @param set
     * @param attributes
     */
    public void parseColorAttributes(Constraint set, String attributes) {
        String[] sp = attributes.split(",");
        for (int i = 0; i < sp.length; i++) {
            String[] attr = sp[i].split("=");
            if (attr.length != 2) {
                Log.w(TAG, " Unable to parse " + sp[i]);
            } else {
                set.setColorValue(attr[0], Color.parseColor(attr[1]));
            }
        }
    }

    /**
     * Parse floats
     * @param set
     * @param attributes
     */
    public void parseFloatAttributes(Constraint set, String attributes) {
        String[] sp = attributes.split(",");
        for (int i = 0; i < sp.length; i++) {
            String[] attr = sp[i].split("=");
            if (attr.length != 2) {
                Log.w(TAG, " Unable to parse " + sp[i]);
            } else {
                set.setFloatValue(attr[0], Float.parseFloat(attr[1]));
            }
        }
    }

    /**
     * Parse string
     * @param set
     * @param attributes
     */
    public void parseStringAttributes(Constraint set, String attributes) {
        String[] sp = splitString(attributes);
        for (int i = 0; i < sp.length; i++) {
            String[] attr = sp[i].split("=");
            Log.w(TAG, " Unable to parse " + sp[i]);
            set.setStringValue(attr[0], attr[1]);
        }
    }

    private static String[] splitString(String str) {
        char[] chars = str.toCharArray();
        ArrayList<String> list = new ArrayList<>();
        boolean inDouble = false;
        int start = 0;
        for (int i = 0; i < chars.length; i++) {
            if (chars[i] == ',' && !inDouble) {
                list.add(new String(chars, start, i - start));
                start = i + 1;
            } else if (chars[i] == '"') {
                inDouble = !inDouble;
            }
        }
        list.add(new String(chars, start, chars.length - start));
        return list.toArray(new String[list.size()]);
    }

    /**
     * Add attribute of type Int
     * @param attributeName
     */
    public void addIntAttributes(String... attributeName) {
        addAttributes(AttributeType.INT_TYPE, attributeName);
    }

    /**
     * Add attribute of type Color
     * @param attributeName
     */
    public void addColorAttributes(String... attributeName) {
        addAttributes(AttributeType.COLOR_TYPE, attributeName);
    }

    /**
     * Add attribute of type float
     * @param attributeName
     */
    public void addFloatAttributes(String... attributeName) {
        addAttributes(AttributeType.FLOAT_TYPE, attributeName);
    }

    /**
     * Add attribute of type string
     * @param attributeName
     */
    public void addStringAttributes(String... attributeName) {
        addAttributes(AttributeType.STRING_TYPE, attributeName);
    }

    private Constraint get(int id) {
        if (!mConstraints.containsKey(id)) {
            mConstraints.put(id, new Constraint());
        }
        return mConstraints.get(id);
    }

    private String sideToString(int side) {
        switch (side) {
            case LEFT:
                return "left";
            case RIGHT:
                return "right";
            case TOP:
                return "top";
            case BOTTOM:
                return "bottom";
            case BASELINE:
                return "baseline";
            case START:
                return "start";
            case END:
                return "end";
        }
        return "undefined";
    }

    /**
     * Load a constraint set from a constraintSet.xml file.
     * Note. Do NOT use this to load a layout file.
     * It will fail silently as there is no efficient way to differentiate.
     *
     * @param context    the context for the inflation
     * @param resourceId id of xml file in res/xml/
     */
    public void load(Context context, int resourceId) {
        Resources res = context.getResources();
        XmlPullParser parser = res.getXml(resourceId);
        try {
            for (int eventType = parser.getEventType();
                    eventType != XmlResourceParser.END_DOCUMENT;
                    eventType = parser.next()) {
                switch (eventType) {
                    case XmlResourceParser.START_DOCUMENT:
                    case XmlResourceParser.END_TAG:
                    case XmlResourceParser.TEXT:
                        break;
                    case XmlResourceParser.START_TAG:
                        String tagName = parser.getName();
                        Constraint constraint = fillFromAttributeList(context,
                                Xml.asAttributeSet(parser), false);
                        if (tagName.equalsIgnoreCase("Guideline")) {
                            constraint.layout.mIsGuideline = true;
                        }
                        if (DEBUG) {
                            Log.v(TAG, Debug.getLoc()
                                    + " cache " + Debug.getName(context, constraint.mViewId)
                                    + " " + constraint.mViewId);
                        }
                        mConstraints.put(constraint.mViewId, constraint);
                        break;
                }
            }
        } catch (XmlPullParserException e) {
            Log.e(TAG, "Error parsing resource: " + resourceId, e);
        } catch (IOException e) {
            Log.e(TAG, "Error parsing resource: " + resourceId, e);
        }
    }

    /**
     * Load a constraint set from a constraintSet.xml file
     *
     * @param context the context for the inflation
     * @param parser  id of xml file in res/xml/
     */
    public void load(Context context, XmlPullParser parser) {
        String tagName = null;
        try {
            Constraint constraint = null;
            for (int eventType = parser.getEventType();
                    eventType != XmlResourceParser.END_DOCUMENT;
                    eventType = parser.next()) {
                switch (eventType) {
                    case XmlResourceParser.START_DOCUMENT:
                        String document = parser.getName();
                        break;
                    case XmlResourceParser.START_TAG:
                        tagName = parser.getName();
                        if (DEBUG) {
                            Log.v(TAG, Debug.getLoc() + " " + document + " tagName=" + tagName);
                        }
                        switch (tagName) {
                            case "Constraint":
                                constraint = fillFromAttributeList(context,
                                        Xml.asAttributeSet(parser), false);
                                break;
                            case "ConstraintOverride":
                                constraint = fillFromAttributeList(context,
                                        Xml.asAttributeSet(parser), true);
                                break;
                            case "Guideline":
                                constraint = fillFromAttributeList(context,
                                        Xml.asAttributeSet(parser), false);
                                constraint.layout.mIsGuideline = true;
                                constraint.layout.mApply = true;
                                break;
                            case "Barrier":
                                constraint = fillFromAttributeList(context,
                                        Xml.asAttributeSet(parser), false);
                                constraint.layout.mHelperType = BARRIER_TYPE;
                                break;
                            case "PropertySet":
                                if (constraint == null) {
                                    throw new RuntimeException(ERROR_MESSAGE
                                            + parser.getLineNumber());
                                }
                                constraint.propertySet.fillFromAttributeList(context,
                                        Xml.asAttributeSet(parser));
                                break;
                            case "Transform":
                                if (constraint == null) {
                                    throw new RuntimeException(ERROR_MESSAGE
                                            + parser.getLineNumber());
                                }
                                constraint.transform.fillFromAttributeList(context,
                                        Xml.asAttributeSet(parser));
                                break;
                            case "Layout":
                                if (constraint == null) {
                                    throw new RuntimeException(ERROR_MESSAGE
                                            + parser.getLineNumber());
                                }
                                constraint.layout.fillFromAttributeList(context,
                                        Xml.asAttributeSet(parser));
                                break;
                            case "Motion":
                                if (constraint == null) {
                                    throw new RuntimeException(ERROR_MESSAGE
                                            + parser.getLineNumber());
                                }
                                constraint.motion.fillFromAttributeList(context,
                                        Xml.asAttributeSet(parser));
                                break;
                            case "CustomAttribute":
                            case "CustomMethod":
                                if (constraint == null) {
                                    throw new RuntimeException(ERROR_MESSAGE
                                            + parser.getLineNumber());
                                }
                                ConstraintAttribute.parse(context, parser,
                                        constraint.mCustomConstraints);
                                break;
                        }
//                        if (tagName.equalsIgnoreCase("Constraint")) {
//                            constraint = fillFromAttributeList(context,
//                            Xml.asAttributeSet(parser));
//                        } else if (tagName.equalsIgnoreCase("Guideline")) {
//                            constraint = fillFromAttributeList(context,
//                            Xml.asAttributeSet(parser));
//                            constraint.layout.mIsGuideline = true;
//                        } else if (tagName.equalsIgnoreCase("CustomAttribute")) {
//                            ConstraintAttribute.parse(context, parser,
//                            constraint.mCustomConstraints);
//                        }
                        break;
                    case XmlResourceParser.END_TAG:
                        tagName = parser.getName();
                        switch (tagName.toLowerCase(Locale.ROOT)) {
                            case "constraintset":
                                return;
                            case "constraint":
                            case "constraintoverride":
                            case "guideline":
                                mConstraints.put(constraint.mViewId, constraint);
                                constraint = null;
                        }
                        tagName = null;
                        break;
                    case XmlResourceParser.TEXT:
                        break;
                }
            }
        } catch (XmlPullParserException e) {
            Log.e(TAG, "Error parsing XML resource", e);
        } catch (IOException e) {
            Log.e(TAG, "Error parsing XML resource", e);
        }
    }

    private static int lookupID(TypedArray a, int index, int def) {
        int ret = a.getResourceId(index, def);
        if (ret == Layout.UNSET) {
            ret = a.getInt(index, Layout.UNSET);
        }
        return ret;
    }

    private Constraint fillFromAttributeList(Context context,
                                             AttributeSet attrs,
                                             boolean override) {
        Constraint c = new Constraint();
        TypedArray a = context.obtainStyledAttributes(attrs,
                override ? R.styleable.ConstraintOverride : R.styleable.Constraint);
        populateConstraint(c, a, override);
        a.recycle();
        return c;
    }

    /**
     * Used to read a ConstraintDelta
     *
     * @param context
     * @param parser
     * @return
     */
    public static Constraint buildDelta(Context context, XmlPullParser parser) {
        AttributeSet attrs = Xml.asAttributeSet(parser);
        Constraint c = new Constraint();
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.ConstraintOverride);
        populateOverride(c, a);
        a.recycle();
        return c;
    }

    private static void populateOverride(Constraint c, TypedArray a) {

        final int count = a.getIndexCount();
        TypedValue type;
        Constraint.Delta delta = new Constraint.Delta();
        c.mDelta = delta;
        c.motion.mApply = false;
        c.layout.mApply = false;
        c.propertySet.mApply = false;
        c.transform.mApply = false;
        for (int i = 0; i < count; i++) {
            int attr = a.getIndex(i);


            int attrType = sOverrideMapToConstant.get(attr);
            if (DEBUG) {
                Log.v(TAG, Debug.getLoc() + " > " + attrType + " " + getDebugName(attrType));
            }

            switch (attrType) {

                case EDITOR_ABSOLUTE_X:
                    delta.add(EDITOR_ABSOLUTE_X,
                            a.getDimensionPixelOffset(attr, c.layout.editorAbsoluteX));
                    break;
                case EDITOR_ABSOLUTE_Y:
                    delta.add(EDITOR_ABSOLUTE_Y,
                            a.getDimensionPixelOffset(attr, c.layout.editorAbsoluteY));
                    break;
                case GUIDE_BEGIN:
                    delta.add(GUIDE_BEGIN, a.getDimensionPixelOffset(attr, c.layout.guideBegin));
                    break;
                case GUIDE_END:
                    delta.add(GUIDE_END, a.getDimensionPixelOffset(attr, c.layout.guideEnd));
                    break;
                case GUIDE_PERCENT:
                    delta.add(GUIDE_PERCENT, a.getFloat(attr, c.layout.guidePercent));
                    break;
                case GUIDELINE_USE_RTL:
                    delta.add(GUIDELINE_USE_RTL, a.getBoolean(attr, c.layout.guidelineUseRtl));
                    break;
                case ORIENTATION:
                    delta.add(ORIENTATION, a.getInt(attr, c.layout.orientation));
                    break;
                case CIRCLE_RADIUS:
                    delta.add(CIRCLE_RADIUS, a.getDimensionPixelSize(attr, c.layout.circleRadius));
                    break;
                case CIRCLE_ANGLE:
                    delta.add(CIRCLE_ANGLE, a.getFloat(attr, c.layout.circleAngle));
                    break;
                case GONE_LEFT_MARGIN:
                    delta.add(GONE_LEFT_MARGIN,
                            a.getDimensionPixelSize(attr, c.layout.goneLeftMargin));
                    break;
                case GONE_TOP_MARGIN:
                    delta.add(GONE_TOP_MARGIN,
                            a.getDimensionPixelSize(attr, c.layout.goneTopMargin));
                    break;
                case GONE_RIGHT_MARGIN:
                    delta.add(GONE_RIGHT_MARGIN,
                            a.getDimensionPixelSize(attr, c.layout.goneRightMargin));
                    break;
                case GONE_BOTTOM_MARGIN:
                    delta.add(GONE_BOTTOM_MARGIN,
                            a.getDimensionPixelSize(attr, c.layout.goneBottomMargin));
                    break;
                case GONE_START_MARGIN:
                    delta.add(GONE_START_MARGIN,
                            a.getDimensionPixelSize(attr, c.layout.goneStartMargin));
                    break;
                case GONE_END_MARGIN:
                    delta.add(GONE_END_MARGIN,
                            a.getDimensionPixelSize(attr, c.layout.goneEndMargin));
                    break;
                case GONE_BASELINE_MARGIN:
                    delta.add(GONE_BASELINE_MARGIN,
                            a.getDimensionPixelSize(attr, c.layout.goneBaselineMargin));
                    break;
                case HORIZONTAL_BIAS:
                    delta.add(HORIZONTAL_BIAS, a.getFloat(attr, c.layout.horizontalBias));
                    break;
                case VERTICAL_BIAS:
                    delta.add(VERTICAL_BIAS, a.getFloat(attr, c.layout.verticalBias));
                    break;
                case LEFT_MARGIN:
                    delta.add(LEFT_MARGIN, a.getDimensionPixelSize(attr, c.layout.leftMargin));
                    break;
                case RIGHT_MARGIN:
                    delta.add(RIGHT_MARGIN, a.getDimensionPixelSize(attr, c.layout.rightMargin));
                    break;
                case START_MARGIN:
                    if (Build.VERSION.SDK_INT >= VERSION_CODES.JELLY_BEAN_MR1) {
                        delta.add(START_MARGIN,
                                a.getDimensionPixelSize(attr, c.layout.startMargin));
                    }
                    break;
                case END_MARGIN:
                    if (Build.VERSION.SDK_INT >= VERSION_CODES.JELLY_BEAN_MR1) {
                        delta.add(END_MARGIN, a.getDimensionPixelSize(attr, c.layout.endMargin));
                    }
                    break;
                case TOP_MARGIN:
                    delta.add(TOP_MARGIN, a.getDimensionPixelSize(attr, c.layout.topMargin));
                    break;
                case BOTTOM_MARGIN:
                    delta.add(BOTTOM_MARGIN, a.getDimensionPixelSize(attr, c.layout.bottomMargin));
                    break;
                case BASELINE_MARGIN:
                    delta.add(BASELINE_MARGIN,
                            a.getDimensionPixelSize(attr, c.layout.baselineMargin));
                    break;
                case LAYOUT_WIDTH:
                    delta.add(LAYOUT_WIDTH, a.getLayoutDimension(attr, c.layout.mWidth));
                    break;
                case LAYOUT_HEIGHT:
                    delta.add(LAYOUT_HEIGHT, a.getLayoutDimension(attr, c.layout.mHeight));
                    break;
                case LAYOUT_CONSTRAINT_WIDTH:
                    ConstraintSet.parseDimensionConstraints(delta, a, attr, HORIZONTAL);
                    break;
                case LAYOUT_CONSTRAINT_HEIGHT:
                    ConstraintSet.parseDimensionConstraints(delta, a, attr, VERTICAL);
                    break;
                case LAYOUT_WRAP_BEHAVIOR:
                    delta.add(LAYOUT_WRAP_BEHAVIOR, a.getInt(attr, c.layout.mWrapBehavior));
                    break;
                case WIDTH_DEFAULT:
                    delta.add(WIDTH_DEFAULT, a.getInt(attr, c.layout.widthDefault));
                    break;
                case HEIGHT_DEFAULT:
                    delta.add(HEIGHT_DEFAULT, a.getInt(attr, c.layout.heightDefault));
                    break;
                case HEIGHT_MAX:
                    delta.add(HEIGHT_MAX, a.getDimensionPixelSize(attr, c.layout.heightMax));
                    break;
                case WIDTH_MAX:
                    delta.add(WIDTH_MAX, a.getDimensionPixelSize(attr, c.layout.widthMax));
                    break;
                case HEIGHT_MIN:
                    delta.add(HEIGHT_MIN, a.getDimensionPixelSize(attr, c.layout.heightMin));
                    break;
                case WIDTH_MIN:
                    delta.add(WIDTH_MIN, a.getDimensionPixelSize(attr, c.layout.widthMin));
                    break;
                case CONSTRAINED_WIDTH:
                    delta.add(CONSTRAINED_WIDTH, a.getBoolean(attr, c.layout.constrainedWidth));
                    break;
                case CONSTRAINED_HEIGHT:
                    delta.add(CONSTRAINED_HEIGHT, a.getBoolean(attr, c.layout.constrainedHeight));
                    break;
                case LAYOUT_VISIBILITY:
                    delta.add(LAYOUT_VISIBILITY,
                            VISIBILITY_FLAGS[a.getInt(attr, c.propertySet.visibility)]);
                    break;
                case VISIBILITY_MODE:
                    delta.add(VISIBILITY_MODE, a.getInt(attr, c.propertySet.mVisibilityMode));
                    break;
                case ALPHA:
                    delta.add(ALPHA, a.getFloat(attr, c.propertySet.alpha));
                    break;
                case ELEVATION:
                    if (Build.VERSION.SDK_INT >= VERSION_CODES.LOLLIPOP) {
                        delta.add(ELEVATION, true);
                        delta.add(ELEVATION, a.getDimension(attr, c.transform.elevation));
                    }
                    break;
                case ROTATION:
                    delta.add(ROTATION, a.getFloat(attr, c.transform.rotation));
                    break;
                case ROTATION_X:
                    delta.add(ROTATION_X, a.getFloat(attr, c.transform.rotationX));
                    break;
                case ROTATION_Y:
                    delta.add(ROTATION_Y, a.getFloat(attr, c.transform.rotationY));
                    break;
                case SCALE_X:
                    delta.add(SCALE_X, a.getFloat(attr, c.transform.scaleX));
                    break;
                case SCALE_Y:
                    delta.add(SCALE_Y, a.getFloat(attr, c.transform.scaleY));
                    break;
                case TRANSFORM_PIVOT_X:
                    delta.add(TRANSFORM_PIVOT_X, a.getDimension(attr, c.transform.transformPivotX));
                    break;
                case TRANSFORM_PIVOT_Y:
                    delta.add(TRANSFORM_PIVOT_Y, a.getDimension(attr, c.transform.transformPivotY));
                    break;
                case TRANSLATION_X:
                    delta.add(TRANSLATION_X, a.getDimension(attr, c.transform.translationX));
                    break;
                case TRANSLATION_Y:
                    delta.add(TRANSLATION_Y, a.getDimension(attr, c.transform.translationY));
                    break;
                case TRANSLATION_Z:
                    if (Build.VERSION.SDK_INT >= VERSION_CODES.LOLLIPOP) {
                        delta.add(TRANSLATION_Z, a.getDimension(attr, c.transform.translationZ));
                    }
                    break;
                case TRANSFORM_PIVOT_TARGET:
                    delta.add(TRANSFORM_PIVOT_TARGET,
                            lookupID(a, attr, c.transform.transformPivotTarget));
                    break;
                case VERTICAL_WEIGHT:
                    delta.add(VERTICAL_WEIGHT, a.getFloat(attr, c.layout.verticalWeight));
                    break;
                case HORIZONTAL_WEIGHT:
                    delta.add(HORIZONTAL_WEIGHT, a.getFloat(attr, c.layout.horizontalWeight));
                    break;
                case VERTICAL_STYLE:
                    delta.add(VERTICAL_STYLE, a.getInt(attr, c.layout.verticalChainStyle));
                    break;
                case HORIZONTAL_STYLE:
                    delta.add(HORIZONTAL_STYLE, a.getInt(attr, c.layout.horizontalChainStyle));
                    break;
                case VIEW_ID:
                    c.mViewId = a.getResourceId(attr, c.mViewId);
                    delta.add(VIEW_ID, c.mViewId);
                    break;
                case MOTION_TARGET:
                    if (MotionLayout.IS_IN_EDIT_MODE) {
                        c.mViewId = a.getResourceId(attr, c.mViewId);
                        if (c.mViewId == -1) {
                            c.mTargetString = a.getString(attr);
                        }
                    } else {
                        if (a.peekValue(attr).type == TypedValue.TYPE_STRING) {
                            c.mTargetString = a.getString(attr);
                        } else {
                            c.mViewId = a.getResourceId(attr, c.mViewId);
                        }
                    }
                    break;
                case DIMENSION_RATIO:
                    delta.add(DIMENSION_RATIO, a.getString(attr));
                    break;
                case WIDTH_PERCENT:
                    delta.add(WIDTH_PERCENT, a.getFloat(attr, 1));
                    break;
                case HEIGHT_PERCENT:
                    delta.add(HEIGHT_PERCENT, a.getFloat(attr, 1));
                    break;
                case PROGRESS:
                    delta.add(PROGRESS, a.getFloat(attr, c.propertySet.mProgress));
                    break;
                case ANIMATE_RELATIVE_TO:
                    delta.add(ANIMATE_RELATIVE_TO,
                            lookupID(a, attr, c.motion.mAnimateRelativeTo));
                    break;
                case ANIMATE_CIRCLE_ANGLE_TO:
                    delta.add(ANIMATE_CIRCLE_ANGLE_TO,
                            a.getInteger(attr, c.motion.mAnimateCircleAngleTo));
                    break;
                case TRANSITION_EASING:
                    type = a.peekValue(attr);
                    if (type.type == TypedValue.TYPE_STRING) {
                        delta.add(TRANSITION_EASING, a.getString(attr));
                    } else {
                        delta.add(TRANSITION_EASING,
                                Easing.NAMED_EASING[a.getInteger(attr, 0)]);
                    }
                    break;
                case PATH_MOTION_ARC:
                    delta.add(PATH_MOTION_ARC, a.getInt(attr, c.motion.mPathMotionArc));
                    break;
                case TRANSITION_PATH_ROTATE:
                    delta.add(TRANSITION_PATH_ROTATE, a.getFloat(attr, c.motion.mPathRotate));
                    break;
                case MOTION_STAGGER:
                    delta.add(MOTION_STAGGER, a.getFloat(attr, c.motion.mMotionStagger));
                    break;

                case QUANTIZE_MOTION_STEPS:
                    delta.add(QUANTIZE_MOTION_STEPS, a.getInteger(attr,
                            c.motion.mQuantizeMotionSteps));
                    break;
                case QUANTIZE_MOTION_PHASE:
                    delta.add(QUANTIZE_MOTION_PHASE, a.getFloat(attr,
                            c.motion.mQuantizeMotionPhase));
                    break;
                case QUANTIZE_MOTION_INTERPOLATOR:
                    type = a.peekValue(attr);
                    if (type.type == TypedValue.TYPE_REFERENCE) {
                        c.motion.mQuantizeInterpolatorID = a.getResourceId(attr, -1);
                        delta.add(QUANTIZE_MOTION_INTERPOLATOR_ID,
                                c.motion.mQuantizeInterpolatorID);
                        if (c.motion.mQuantizeInterpolatorID != -1) {
                            c.motion.mQuantizeInterpolatorType = Motion.INTERPOLATOR_REFERENCE_ID;
                            delta.add(QUANTIZE_MOTION_INTERPOLATOR_TYPE,
                                    c.motion.mQuantizeInterpolatorType);
                        }
                    } else if (type.type == TypedValue.TYPE_STRING) {
                        c.motion.mQuantizeInterpolatorString = a.getString(attr);
                        delta.add(QUANTIZE_MOTION_INTERPOLATOR_STR,
                                c.motion.mQuantizeInterpolatorString);

                        if (c.motion.mQuantizeInterpolatorString.indexOf("/") > 0) {
                            c.motion.mQuantizeInterpolatorID = a.getResourceId(attr, -1);
                            delta.add(QUANTIZE_MOTION_INTERPOLATOR_ID,
                                    c.motion.mQuantizeInterpolatorID);

                            c.motion.mQuantizeInterpolatorType = Motion.INTERPOLATOR_REFERENCE_ID;
                            delta.add(QUANTIZE_MOTION_INTERPOLATOR_TYPE,
                                    c.motion.mQuantizeInterpolatorType);

                        } else {
                            c.motion.mQuantizeInterpolatorType = Motion.SPLINE_STRING;
                            delta.add(QUANTIZE_MOTION_INTERPOLATOR_TYPE,
                                    c.motion.mQuantizeInterpolatorType);
                        }
                    } else {
                        c.motion.mQuantizeInterpolatorType =
                                a.getInteger(attr, c.motion.mQuantizeInterpolatorID);
                        delta.add(QUANTIZE_MOTION_INTERPOLATOR_TYPE,
                                c.motion.mQuantizeInterpolatorType);
                    }
                    break;
                case DRAW_PATH:
                    delta.add(DRAW_PATH, a.getInt(attr, 0));
                    break;
                case CHAIN_USE_RTL:
                    Log.e(TAG, "CURRENTLY UNSUPPORTED"); // TODO add support or remove
                    //  TODO add support or remove
                    //   c.mChainUseRtl = a.getBoolean(attr,c.mChainUseRtl);
                    break;
                case BARRIER_DIRECTION:
                    delta.add(BARRIER_DIRECTION, a.getInt(attr, c.layout.mBarrierDirection));
                    break;
                case BARRIER_MARGIN:
                    delta.add(BARRIER_MARGIN, a.getDimensionPixelSize(attr,
                            c.layout.mBarrierMargin));
                    break;
                case CONSTRAINT_REFERENCED_IDS:
                    delta.add(CONSTRAINT_REFERENCED_IDS, a.getString(attr));
                    break;
                case CONSTRAINT_TAG:
                    delta.add(CONSTRAINT_TAG, a.getString(attr));
                    break;
                case BARRIER_ALLOWS_GONE_WIDGETS:
                    delta.add(BARRIER_ALLOWS_GONE_WIDGETS, a.getBoolean(attr,
                            c.layout.mBarrierAllowsGoneWidgets));
                    break;
                case UNUSED:
                    Log.w(TAG,
                            "unused attribute 0x" + Integer.toHexString(attr)
                                    + "   " + sMapToConstant.get(attr));
                    break;
                default:
                    Log.w(TAG,
                            "Unknown attribute 0x" + Integer.toHexString(attr)
                                    + "   " + sMapToConstant.get(attr));
            }
        }
    }

    private static void setDeltaValue(Constraint c, int type, float value) {
        switch (type) {
            case GUIDE_PERCENT:
                c.layout.guidePercent = value;
                break;
            case CIRCLE_ANGLE:
                c.layout.circleAngle = value;
                break;
            case HORIZONTAL_BIAS:
                c.layout.horizontalBias = value;
                break;
            case VERTICAL_BIAS:
                c.layout.verticalBias = value;
                break;
            case ALPHA:
                c.propertySet.alpha = value;
                break;
            case ELEVATION:
                c.transform.elevation = value;
                c.transform.applyElevation = true;
                break;
            case ROTATION:
                c.transform.rotation = value;
                break;
            case ROTATION_X:
                c.transform.rotationX = value;
                break;
            case ROTATION_Y:
                c.transform.rotationY = value;
                break;
            case SCALE_X:
                c.transform.scaleX = value;
                break;
            case SCALE_Y:
                c.transform.scaleY = value;
                break;
            case TRANSFORM_PIVOT_X:
                c.transform.transformPivotX = value;
                break;
            case TRANSFORM_PIVOT_Y:
                c.transform.transformPivotY = value;
                break;
            case TRANSLATION_X:
                c.transform.translationX = value;
                break;
            case TRANSLATION_Y:
                c.transform.translationY = value;
                break;
            case TRANSLATION_Z:
                c.transform.translationZ = value;
                break;
            case VERTICAL_WEIGHT:
                c.layout.verticalWeight = value;
                break;
            case HORIZONTAL_WEIGHT:
                c.layout.horizontalWeight = value;
                break;
            case WIDTH_PERCENT:
                c.layout.widthPercent = value;
                break;
            case HEIGHT_PERCENT:
                c.layout.heightPercent = value;
                break;
            case PROGRESS:
                c.propertySet.mProgress = value;
                break;
            case TRANSITION_PATH_ROTATE:
                c.motion.mPathRotate = value;
                break;
            case MOTION_STAGGER:
                c.motion.mMotionStagger = value;
                break;
            case QUANTIZE_MOTION_PHASE:
                c.motion.mQuantizeMotionPhase = value;
                break;
            case UNUSED:
                break;
            default:
                Log.w(TAG,
                        "Unknown attribute 0x");
        }
    }

    private static void setDeltaValue(Constraint c, int type, int value) {
        switch (type) {
            case EDITOR_ABSOLUTE_X:
                c.layout.editorAbsoluteX = value;
                break;
            case EDITOR_ABSOLUTE_Y:
                c.layout.editorAbsoluteY = value;
                break;
            case LAYOUT_WRAP_BEHAVIOR:
                c.layout.mWrapBehavior = value;
                break;
            case GUIDE_BEGIN:
                c.layout.guideBegin = value;
                break;
            case GUIDE_END:
                c.layout.guideEnd = value;
                break;
            case ORIENTATION:
                c.layout.orientation = value;
                break;
            case CIRCLE:
                c.layout.circleConstraint = value;
                break;
            case CIRCLE_RADIUS:
                c.layout.circleRadius = value;
                break;
            case GONE_LEFT_MARGIN:
                c.layout.goneLeftMargin = value;
                break;
            case GONE_TOP_MARGIN:
                c.layout.goneTopMargin = value;
                break;
            case GONE_RIGHT_MARGIN:
                c.layout.goneRightMargin = value;
                break;
            case GONE_BOTTOM_MARGIN:
                c.layout.goneBottomMargin = value;
                break;
            case GONE_START_MARGIN:
                c.layout.goneStartMargin = value;
                break;
            case GONE_END_MARGIN:
                c.layout.goneEndMargin = value;
                break;
            case GONE_BASELINE_MARGIN:
                c.layout.goneBaselineMargin = value;
                break;
            case LEFT_MARGIN:
                c.layout.leftMargin = value;
                break;
            case RIGHT_MARGIN:
                c.layout.rightMargin = value;
                break;
            case START_MARGIN:
                c.layout.startMargin = value;
                break;
            case END_MARGIN:
                c.layout.endMargin = value;
                break;
            case TOP_MARGIN:
                c.layout.topMargin = value;
                break;
            case BOTTOM_MARGIN:
                c.layout.bottomMargin = value;
                break;
            case BASELINE_MARGIN:
                c.layout.baselineMargin = value;
                break;
            case LAYOUT_WIDTH:
                c.layout.mWidth = value;
                break;
            case LAYOUT_HEIGHT:
                c.layout.mHeight = value;
                break;
            case WIDTH_DEFAULT:
                c.layout.widthDefault = value;
                break;
            case HEIGHT_DEFAULT:
                c.layout.heightDefault = value;
                break;
            case HEIGHT_MAX:
                c.layout.heightMax = value;
                break;
            case WIDTH_MAX:
                c.layout.widthMax = value;
                break;
            case HEIGHT_MIN:
                c.layout.heightMin = value;
                break;
            case WIDTH_MIN:
                c.layout.widthMin = value;
                break;
            case LAYOUT_VISIBILITY:
                c.propertySet.visibility = value;
                break;
            case VISIBILITY_MODE:
                c.propertySet.mVisibilityMode = value;
                break;
            case TRANSFORM_PIVOT_TARGET:
                c.transform.transformPivotTarget = value;
                break;
            case VERTICAL_STYLE:
                c.layout.verticalChainStyle = value;
                break;
            case HORIZONTAL_STYLE:
                c.layout.horizontalChainStyle = value;
                break;
            case VIEW_ID:
                c.mViewId = value;
                break;
            case ANIMATE_RELATIVE_TO:
                c.motion.mAnimateRelativeTo = value;
                break;
            case ANIMATE_CIRCLE_ANGLE_TO:
                c.motion.mAnimateCircleAngleTo = value;
                break;
            case PATH_MOTION_ARC:
                c.motion.mPathMotionArc = value;
                break;
            case QUANTIZE_MOTION_STEPS:
                c.motion.mQuantizeMotionSteps = value;
                break;
            case QUANTIZE_MOTION_INTERPOLATOR_TYPE:
                c.motion.mQuantizeInterpolatorType = value;
                break;
            case QUANTIZE_MOTION_INTERPOLATOR_ID:
                c.motion.mQuantizeInterpolatorID = value;
                break;
            case DRAW_PATH:
                c.motion.mDrawPath = value;
                break;
            case BARRIER_DIRECTION:
                c.layout.mBarrierDirection = value;
                break;
            case BARRIER_MARGIN:
                c.layout.mBarrierMargin = value;
                break;
            case UNUSED:
                break;
            default:
                Log.w(TAG,
                        "Unknown attribute 0x");
        }
    }

    private static void setDeltaValue(Constraint c, int type, String value) {
        switch (type) {
            case DIMENSION_RATIO:
                c.layout.dimensionRatio = value;
                break;
            case TRANSITION_EASING:
                c.motion.mTransitionEasing = value;
                break;
            case QUANTIZE_MOTION_INTERPOLATOR_STR:
                c.motion.mQuantizeInterpolatorString = value;
                break;
            case CONSTRAINT_REFERENCED_IDS:
                c.layout.mReferenceIdString = value;
                // If a string is defined, clear up the reference ids array
                c.layout.mReferenceIds = null;
                break;
            case CONSTRAINT_TAG:
                c.layout.mConstraintTag = value;
                break;
            case UNUSED:
                break;
            default:
                Log.w(TAG,
                        "Unknown attribute 0x");
        }
    }

    private static void setDeltaValue(Constraint c, int type, boolean value) {
        switch (type) {
            case CONSTRAINED_WIDTH:
                c.layout.constrainedWidth = value;
                break;
            case CONSTRAINED_HEIGHT:
                c.layout.constrainedHeight = value;
                break;
            case ELEVATION:
                c.transform.applyElevation = value;
                break;
            case BARRIER_ALLOWS_GONE_WIDGETS:
                c.layout.mBarrierAllowsGoneWidgets = value;
                break;
            case UNUSED:
                break;
            default:
                Log.w(TAG, "Unknown attribute 0x");
        }
    }

    private void populateConstraint(Constraint c, TypedArray a, boolean override) {
        if (override) {
            populateOverride(c, a);
            return;
        }
        final int count = a.getIndexCount();
        for (int i = 0; i < count; i++) {
            int attr = a.getIndex(i);
            if (DEBUG) { // USEFUL when adding features to track tags being parsed
                try {
                    Field[] campos = R.styleable.class.getFields();
                    boolean found = false;
                    for (Field f : campos) {
                        try {
                            if (f.getType().isPrimitive()
                                    && attr == f.getInt(null) && f.getName()
                                    .contains("Constraint_")) {
                                found = true;
                                if (DEBUG) {
                                    Log.v(TAG, "L id " + f.getName() + " #" + attr);
                                }
                                break;
                            }
                        } catch (Exception e) {

                        }
                    }
                    if (!found) {
                        campos = android.R.attr.class.getFields();
                        for (Field f : campos) {
                            try {
                                if (f.getType().isPrimitive() && attr == f.getInt(null)
                                        && f.getName()
                                        .contains("Constraint_")) {
                                    found = false;
                                    if (DEBUG) {
                                        Log.v(TAG, "x id " + f.getName());
                                    }
                                    break;
                                }
                            } catch (Exception e) {

                            }
                        }
                    }
                    if (!found) {
                        Log.v(TAG, " ? " + attr);
                    }
                } catch (Exception e) {
                    Log.v(TAG, " " + e.toString());
                }
            }


            if (attr != R.styleable.Constraint_android_id
                    && R.styleable.Constraint_android_layout_marginStart != attr
                    && R.styleable.Constraint_android_layout_marginEnd != attr) {
                c.motion.mApply = true;
                c.layout.mApply = true;
                c.propertySet.mApply = true;
                c.transform.mApply = true;
            }

            switch (sMapToConstant.get(attr)) {
                case LEFT_TO_LEFT:
                    c.layout.leftToLeft = lookupID(a, attr, c.layout.leftToLeft);
                    break;
                case LEFT_TO_RIGHT:
                    c.layout.leftToRight = lookupID(a, attr, c.layout.leftToRight);
                    break;
                case RIGHT_TO_LEFT:
                    c.layout.rightToLeft = lookupID(a, attr, c.layout.rightToLeft);
                    break;
                case RIGHT_TO_RIGHT:
                    c.layout.rightToRight = lookupID(a, attr, c.layout.rightToRight);
                    break;
                case TOP_TO_TOP:
                    c.layout.topToTop = lookupID(a, attr, c.layout.topToTop);
                    break;
                case TOP_TO_BOTTOM:
                    c.layout.topToBottom = lookupID(a, attr, c.layout.topToBottom);
                    break;
                case BOTTOM_TO_TOP:
                    c.layout.bottomToTop = lookupID(a, attr, c.layout.bottomToTop);
                    break;
                case BOTTOM_TO_BOTTOM:
                    c.layout.bottomToBottom = lookupID(a, attr, c.layout.bottomToBottom);
                    break;
                case BASELINE_TO_BASELINE:
                    c.layout.baselineToBaseline = lookupID(a, attr, c.layout.baselineToBaseline);
                    break;
                case BASELINE_TO_TOP:
                    c.layout.baselineToTop = lookupID(a, attr, c.layout.baselineToTop);
                    break;
                case BASELINE_TO_BOTTOM:
                    c.layout.baselineToBottom = lookupID(a, attr, c.layout.baselineToBottom);
                    break;
                case EDITOR_ABSOLUTE_X:
                    c.layout.editorAbsoluteX = a.getDimensionPixelOffset(attr,
                            c.layout.editorAbsoluteX);
                    break;
                case EDITOR_ABSOLUTE_Y:
                    c.layout.editorAbsoluteY = a.getDimensionPixelOffset(attr,
                            c.layout.editorAbsoluteY);
                    break;
                case GUIDE_BEGIN:
                    c.layout.guideBegin = a.getDimensionPixelOffset(attr, c.layout.guideBegin);
                    break;
                case GUIDE_END:
                    c.layout.guideEnd = a.getDimensionPixelOffset(attr, c.layout.guideEnd);
                    break;
                case GUIDE_PERCENT:
                    c.layout.guidePercent = a.getFloat(attr, c.layout.guidePercent);
                    break;
                case ORIENTATION:
                    c.layout.orientation = a.getInt(attr, c.layout.orientation);
                    break;
                case START_TO_END:
                    c.layout.startToEnd = lookupID(a, attr, c.layout.startToEnd);
                    break;
                case START_TO_START:
                    c.layout.startToStart = lookupID(a, attr, c.layout.startToStart);
                    break;
                case END_TO_START:
                    c.layout.endToStart = lookupID(a, attr, c.layout.endToStart);
                    break;
                case END_TO_END:
                    c.layout.endToEnd = lookupID(a, attr, c.layout.endToEnd);
                    break;
                case CIRCLE:
                    c.layout.circleConstraint = lookupID(a, attr, c.layout.circleConstraint);
                    break;
                case CIRCLE_RADIUS:
                    c.layout.circleRadius = a.getDimensionPixelSize(attr, c.layout.circleRadius);
                    break;
                case CIRCLE_ANGLE:
                    c.layout.circleAngle = a.getFloat(attr, c.layout.circleAngle);
                    break;
                case GONE_LEFT_MARGIN:
                    c.layout.goneLeftMargin = a.getDimensionPixelSize(attr,
                            c.layout.goneLeftMargin);
                    break;
                case GONE_TOP_MARGIN:
                    c.layout.goneTopMargin = a.getDimensionPixelSize(attr, c.layout.goneTopMargin);
                    break;
                case GONE_RIGHT_MARGIN:
                    c.layout.goneRightMargin = a.getDimensionPixelSize(attr,
                            c.layout.goneRightMargin);
                    break;
                case GONE_BOTTOM_MARGIN:
                    c.layout.goneBottomMargin = a.getDimensionPixelSize(attr,
                            c.layout.goneBottomMargin);
                    break;
                case GONE_START_MARGIN:
                    c.layout.goneStartMargin =
                            a.getDimensionPixelSize(attr, c.layout.goneStartMargin);
                    break;
                case GONE_END_MARGIN:
                    c.layout.goneEndMargin = a.getDimensionPixelSize(attr, c.layout.goneEndMargin);
                    break;
                case GONE_BASELINE_MARGIN:
                    c.layout.goneBaselineMargin = a.getDimensionPixelSize(attr,
                            c.layout.goneBaselineMargin);
                    break;
                case HORIZONTAL_BIAS:
                    c.layout.horizontalBias = a.getFloat(attr, c.layout.horizontalBias);
                    break;
                case VERTICAL_BIAS:
                    c.layout.verticalBias = a.getFloat(attr, c.layout.verticalBias);
                    break;
                case LEFT_MARGIN:
                    c.layout.leftMargin = a.getDimensionPixelSize(attr, c.layout.leftMargin);
                    break;
                case RIGHT_MARGIN:
                    c.layout.rightMargin = a.getDimensionPixelSize(attr, c.layout.rightMargin);
                    break;
                case START_MARGIN:
                    if (Build.VERSION.SDK_INT >= VERSION_CODES.JELLY_BEAN_MR1) {
                        c.layout.startMargin = a.getDimensionPixelSize(attr, c.layout.startMargin);
                    }
                    break;
                case END_MARGIN:
                    if (Build.VERSION.SDK_INT >= VERSION_CODES.JELLY_BEAN_MR1) {
                        c.layout.endMargin = a.getDimensionPixelSize(attr, c.layout.endMargin);
                    }
                    break;
                case TOP_MARGIN:
                    c.layout.topMargin = a.getDimensionPixelSize(attr, c.layout.topMargin);
                    break;
                case BOTTOM_MARGIN:
                    c.layout.bottomMargin = a.getDimensionPixelSize(attr, c.layout.bottomMargin);
                    break;
                case BASELINE_MARGIN:
                    c.layout.baselineMargin = a.getDimensionPixelSize(attr,
                            c.layout.baselineMargin);
                    break;
                case LAYOUT_WIDTH:
                    c.layout.mWidth = a.getLayoutDimension(attr, c.layout.mWidth);
                    break;
                case LAYOUT_HEIGHT:
                    c.layout.mHeight = a.getLayoutDimension(attr, c.layout.mHeight);
                    break;
                case LAYOUT_CONSTRAINT_WIDTH:
                    ConstraintSet.parseDimensionConstraints(c.layout, a, attr, HORIZONTAL);
                    break;
                case LAYOUT_CONSTRAINT_HEIGHT:
                    ConstraintSet.parseDimensionConstraints(c.layout, a, attr, VERTICAL);
                    break;
                case LAYOUT_WRAP_BEHAVIOR:
                    c.layout.mWrapBehavior = a.getInt(attr, c.layout.mWrapBehavior);
                    break;
                case WIDTH_DEFAULT:
                    c.layout.widthDefault = a.getInt(attr, c.layout.widthDefault);
                    break;
                case HEIGHT_DEFAULT:
                    c.layout.heightDefault = a.getInt(attr, c.layout.heightDefault);
                    break;
                case HEIGHT_MAX:
                    c.layout.heightMax = a.getDimensionPixelSize(attr, c.layout.heightMax);
                    break;
                case WIDTH_MAX:
                    c.layout.widthMax = a.getDimensionPixelSize(attr, c.layout.widthMax);
                    break;
                case HEIGHT_MIN:
                    c.layout.heightMin = a.getDimensionPixelSize(attr, c.layout.heightMin);
                    break;
                case WIDTH_MIN:
                    c.layout.widthMin = a.getDimensionPixelSize(attr, c.layout.widthMin);
                    break;
                case CONSTRAINED_WIDTH:
                    c.layout.constrainedWidth = a.getBoolean(attr, c.layout.constrainedWidth);
                    break;
                case CONSTRAINED_HEIGHT:
                    c.layout.constrainedHeight = a.getBoolean(attr, c.layout.constrainedHeight);
                    break;
                case LAYOUT_VISIBILITY:
                    c.propertySet.visibility = a.getInt(attr, c.propertySet.visibility);
                    c.propertySet.visibility = VISIBILITY_FLAGS[c.propertySet.visibility];
                    break;
                case VISIBILITY_MODE:
                    c.propertySet.mVisibilityMode = a.getInt(attr, c.propertySet.mVisibilityMode);
                    break;
                case ALPHA:
                    c.propertySet.alpha = a.getFloat(attr, c.propertySet.alpha);
                    break;
                case ELEVATION:
                    if (Build.VERSION.SDK_INT >= VERSION_CODES.LOLLIPOP) {
                        c.transform.applyElevation = true;
                        c.transform.elevation = a.getDimension(attr, c.transform.elevation);
                    }
                    break;
                case ROTATION:
                    c.transform.rotation = a.getFloat(attr, c.transform.rotation);
                    break;
                case ROTATION_X:
                    c.transform.rotationX = a.getFloat(attr, c.transform.rotationX);
                    break;
                case ROTATION_Y:
                    c.transform.rotationY = a.getFloat(attr, c.transform.rotationY);
                    break;
                case SCALE_X:
                    c.transform.scaleX = a.getFloat(attr, c.transform.scaleX);
                    break;
                case SCALE_Y:
                    c.transform.scaleY = a.getFloat(attr, c.transform.scaleY);
                    break;
                case TRANSFORM_PIVOT_X:
                    c.transform.transformPivotX = a.getDimension(attr, c.transform.transformPivotX);
                    break;
                case TRANSFORM_PIVOT_Y:
                    c.transform.transformPivotY = a.getDimension(attr, c.transform.transformPivotY);
                    break;
                case TRANSLATION_X:
                    c.transform.translationX = a.getDimension(attr, c.transform.translationX);
                    break;
                case TRANSLATION_Y:
                    c.transform.translationY = a.getDimension(attr, c.transform.translationY);
                    break;
                case TRANSLATION_Z:
                    if (Build.VERSION.SDK_INT >= VERSION_CODES.LOLLIPOP) {
                        c.transform.translationZ = a.getDimension(attr, c.transform.translationZ);
                    }
                    break;
                case TRANSFORM_PIVOT_TARGET:
                    c.transform.transformPivotTarget =
                            lookupID(a, attr, c.transform.transformPivotTarget);
                    break;
                case VERTICAL_WEIGHT:
                    c.layout.verticalWeight = a.getFloat(attr, c.layout.verticalWeight);
                    break;
                case HORIZONTAL_WEIGHT:
                    c.layout.horizontalWeight = a.getFloat(attr, c.layout.horizontalWeight);
                    break;
                case VERTICAL_STYLE:
                    c.layout.verticalChainStyle = a.getInt(attr, c.layout.verticalChainStyle);
                    break;
                case HORIZONTAL_STYLE:
                    c.layout.horizontalChainStyle = a.getInt(attr, c.layout.horizontalChainStyle);
                    break;
                case VIEW_ID:
                    c.mViewId = a.getResourceId(attr, c.mViewId);
                    break;
                case DIMENSION_RATIO:
                    c.layout.dimensionRatio = a.getString(attr);
                    break;
                case WIDTH_PERCENT:
                    c.layout.widthPercent = a.getFloat(attr, 1);
                    break;
                case HEIGHT_PERCENT:
                    c.layout.heightPercent = a.getFloat(attr, 1);
                    break;
                case PROGRESS:
                    c.propertySet.mProgress = a.getFloat(attr, c.propertySet.mProgress);
                    break;
                case ANIMATE_RELATIVE_TO:
                    c.motion.mAnimateRelativeTo = lookupID(a, attr, c.motion.mAnimateRelativeTo);
                    break;
                case ANIMATE_CIRCLE_ANGLE_TO:
                    c.motion.mAnimateCircleAngleTo =
                            a.getInteger(attr, c.motion.mAnimateCircleAngleTo);
                    break;
                case TRANSITION_EASING:
                    TypedValue type = a.peekValue(attr);
                    if (type.type == TypedValue.TYPE_STRING) {
                        c.motion.mTransitionEasing = a.getString(attr);
                    } else {
                        c.motion.mTransitionEasing =
                                Easing.NAMED_EASING[a.getInteger(attr, 0)];
                    }
                    break;
                case PATH_MOTION_ARC:
                    c.motion.mPathMotionArc = a.getInt(attr, c.motion.mPathMotionArc);
                    break;
                case TRANSITION_PATH_ROTATE:
                    c.motion.mPathRotate = a.getFloat(attr, c.motion.mPathRotate);
                    break;
                case MOTION_STAGGER:
                    c.motion.mMotionStagger = a.getFloat(attr, c.motion.mMotionStagger);
                    break;

                case QUANTIZE_MOTION_STEPS:
                    c.motion.mQuantizeMotionSteps = a.getInteger(attr,
                            c.motion.mQuantizeMotionSteps);
                    break;
                case QUANTIZE_MOTION_PHASE:
                    c.motion.mQuantizeMotionPhase = a.getFloat(attr, c.motion.mQuantizeMotionPhase);
                    break;
                case QUANTIZE_MOTION_INTERPOLATOR:
                    type = a.peekValue(attr);

                    if (type.type == TypedValue.TYPE_REFERENCE) {
                        c.motion.mQuantizeInterpolatorID = a.getResourceId(attr, -1);
                        if (c.motion.mQuantizeInterpolatorID != -1) {
                            c.motion.mQuantizeInterpolatorType = Motion.INTERPOLATOR_REFERENCE_ID;
                        }
                    } else if (type.type == TypedValue.TYPE_STRING) {
                        c.motion.mQuantizeInterpolatorString = a.getString(attr);
                        if (c.motion.mQuantizeInterpolatorString.indexOf("/") > 0) {
                            c.motion.mQuantizeInterpolatorID = a.getResourceId(attr, -1);
                            c.motion.mQuantizeInterpolatorType = Motion.INTERPOLATOR_REFERENCE_ID;
                        } else {
                            c.motion.mQuantizeInterpolatorType = Motion.SPLINE_STRING;
                        }
                    } else {
                        c.motion.mQuantizeInterpolatorType = a.getInteger(attr,
                                c.motion.mQuantizeInterpolatorID);
                    }

                    break;


                case DRAW_PATH:
                    c.motion.mDrawPath = a.getInt(attr, 0);
                    break;
                case CHAIN_USE_RTL:
                    Log.e(TAG, "CURRENTLY UNSUPPORTED"); // TODO add support or remove
                    //  TODO add support or remove  c.mChainUseRtl =
                    //   a.getBoolean(attr,c.mChainUseRtl);
                    break;
                case BARRIER_DIRECTION:
                    c.layout.mBarrierDirection = a.getInt(attr, c.layout.mBarrierDirection);
                    break;
                case BARRIER_MARGIN:
                    c.layout.mBarrierMargin = a.getDimensionPixelSize(attr,
                            c.layout.mBarrierMargin);
                    break;
                case CONSTRAINT_REFERENCED_IDS:
                    c.layout.mReferenceIdString = a.getString(attr);
                    break;
                case CONSTRAINT_TAG:
                    c.layout.mConstraintTag = a.getString(attr);
                    break;
                case BARRIER_ALLOWS_GONE_WIDGETS:
                    c.layout.mBarrierAllowsGoneWidgets = a.getBoolean(attr,
                            c.layout.mBarrierAllowsGoneWidgets);
                    break;
                case UNUSED:
                    Log.w(TAG,
                            "unused attribute 0x" + Integer.toHexString(attr)
                                    + "   " + sMapToConstant.get(attr));
                    break;
                default:
                    Log.w(TAG,
                            "Unknown attribute 0x" + Integer.toHexString(attr)
                                    + "   " + sMapToConstant.get(attr));
            }
        }
        if (c.layout.mReferenceIdString != null) {
            // in case the strings are set, make sure to clear up the cached ids
            c.layout.mReferenceIds = null;
        }
    }

    private int[] convertReferenceString(View view, String referenceIdString) {
        String[] split = referenceIdString.split(",");
        Context context = view.getContext();
        int[] tags = new int[split.length];
        int count = 0;
        for (int i = 0; i < split.length; i++) {
            String idString = split[i];
            idString = idString.trim();
            int tag = 0;
            try {
                Class res = R.id.class;
                Field field = res.getField(idString);
                tag = field.getInt(null);
            } catch (Exception e) {
                // Do nothing
            }
            if (tag == 0) {
                tag = context.getResources().getIdentifier(idString, "id",
                        context.getPackageName());
            }

            if (tag == 0 && view.isInEditMode() && view.getParent() instanceof ConstraintLayout) {
                ConstraintLayout constraintLayout = (ConstraintLayout) view.getParent();
                Object value = constraintLayout.getDesignInformation(0, idString);
                if (value != null && value instanceof Integer) {
                    tag = (Integer) value;
                }
            }
            tags[count++] = tag;
        }
        if (count != split.length) {
            tags = Arrays.copyOf(tags, count);
        }
        return tags;
    }

    /**
     *
     */
    public Constraint getConstraint(int id) {
        if (mConstraints.containsKey(id)) {
            return mConstraints.get(id);
        }
        return null;
    }

    /**
     *
     */
    public int[] getKnownIds() {
        Integer[] arr = mConstraints.keySet().toArray(new Integer[0]);
        int[] array = new int[arr.length];
        for (int i = 0; i < array.length; i++) {
            array[i] = arr[i];
        }
        return array;
    }

    /**
     * Enforce id are required for all ConstraintLayout children to use ConstraintSet.
     * default = true;
     */
    public boolean isForceId() {
        return mForceId;
    }

    /**
     * Enforce id are required for all ConstraintLayout children to use ConstraintSet.
     * default = true;
     *
     * @param forceId
     */
    public void setForceId(boolean forceId) {
        this.mForceId = forceId;
    }

    /**
     * If true perform validation checks when parsing ConstraintSets
     * This will slow down parsing and should only be used for debugging
     *
     * @param validate
     */
    public void setValidateOnParse(boolean validate) {
        mValidate = validate;
    }

    /**
     * If true perform validation checks when parsing ConstraintSets
     * This will slow down parsing and should only be used for debugging
     *
     * @return validate
     */
    public boolean isValidateOnParse() {
        return mValidate;
    }

    /**
     * Dump the contents
     *
     * @param scene
     * @param ids
     */
    public void dump(MotionScene scene, int... ids) {
        Set<Integer> keys = mConstraints.keySet();
        HashSet<Integer> set;
        if (ids.length != 0) {
            set = new HashSet<Integer>();
            for (int id : ids) {
                set.add(id);
            }
        } else {
            set = new HashSet<>(keys);
        }
        System.out.println(set.size() + " constraints");
        StringBuilder stringBuilder = new StringBuilder();

        for (Integer id : set.toArray(new Integer[0])) {
            Constraint constraint = mConstraints.get(id);
            if (constraint == null) {
                continue;
            }

            stringBuilder.append("<Constraint id=");
            stringBuilder.append(id);
            stringBuilder.append(" \n");
            constraint.layout.dump(scene, stringBuilder);
            stringBuilder.append("/>\n");
        }
        System.out.println(stringBuilder.toString());

    }

    /**
     * Construct a user friendly error string
     *
     * @param context    the context
     * @param resourceId the xml being parsed
     * @param pullParser the XML parser
     * @return
     */
    static String getLine(Context context, int resourceId, XmlPullParser pullParser) {
        return ".(" + Debug.getName(context, resourceId)
                + ".xml:" + pullParser.getLineNumber()
                + ") \"" + pullParser.getName() + "\"";
    }

    static String getDebugName(int v) {
        for (Field field : ConstraintSet.class.getDeclaredFields()) {
            if (field.getName().contains("_")
                    && field.getType() == int.class
                    && java.lang.reflect.Modifier.isStatic(field.getModifiers())
                    && java.lang.reflect.Modifier.isFinal(field.getModifiers())) {
                int val = 0;
                try {
                    val = field.getInt(null);
                    if (val == v) {
                        return field.getName();
                    }
                } catch (IllegalAccessException e) {
                    Log.e(TAG, "Error accessing ConstraintSet field", e);
                }

            }
        }
        return "UNKNOWN";
    }

    /**
     * Write the state to a Writer
     * @param writer
     * @param layout
     * @param flags
     * @throws IOException
     */
    public void writeState(Writer writer, ConstraintLayout layout, int flags) throws IOException {
        writer.write("\n---------------------------------------------\n");
        if ((flags & 1) == 1) {
            new WriteXmlEngine(writer, layout, flags).writeLayout();
        } else {
            new WriteJsonEngine(writer, layout, flags).writeLayout();
        }
        writer.write("\n---------------------------------------------\n");

    }

    class WriteXmlEngine {
        Writer mWriter;
        ConstraintLayout mLayout;
        Context mContext;
        int mFlags;
        int mUnknownCount = 0;
        final String mLEFT = "'left'";
        final String mRIGHT = "'right'";
        final String mBASELINE = "'baseline'";
        final String mBOTTOM = "'bottom'";
        final String mTOP = "'top'";
        final String mSTART = "'start'";
        final String mEND = "'end'";

        WriteXmlEngine(Writer writer, ConstraintLayout layout, int flags) throws IOException {
            this.mWriter = writer;
            this.mLayout = layout;
            this.mContext = layout.getContext();
            this.mFlags = flags;
        }

        void writeLayout() throws IOException {
            mWriter.write("\n<ConstraintSet>\n");
            for (Integer id : mConstraints.keySet()) {
                Constraint c = mConstraints.get(id);
                String idName = getName(id);
                mWriter.write("  <Constraint");
                mWriter.write(SPACE + "android:id" + "=\"" + idName + "\"");
                Layout l = c.layout;
                writeBaseDimension("android:layout_width", l.mWidth, -5);
                writeBaseDimension("android:layout_height", l.mHeight, -5);

                writeVariable("app:layout_constraintGuide_begin", l.guideBegin, UNSET);
                writeVariable("app:layout_constraintGuide_end", l.guideEnd, UNSET);
                writeVariable("app:layout_constraintGuide_percent", l.guidePercent, UNSET);

                writeVariable("app:layout_constraintHorizontal_bias",
                        l.horizontalBias, 0.5f);
                writeVariable("app:layout_constraintVertical_bias",
                        l.verticalBias, 0.5f);
                writeVariable("app:layout_constraintDimensionRatio",
                        l.dimensionRatio, null);
                writeXmlConstraint("app:layout_constraintCircle", l.circleConstraint);
                writeVariable("app:layout_constraintCircleRadius", l.circleRadius, 0);
                writeVariable("app:layout_constraintCircleAngle", l.circleAngle, 0);

                writeVariable("android:orientation", l.orientation, UNSET);

                writeVariable("app:layout_constraintVertical_weight",
                        l.verticalWeight, UNSET);
                writeVariable("app:layout_constraintHorizontal_weight",
                        l.horizontalWeight, UNSET);
                writeVariable("app:layout_constraintHorizontal_chainStyle",
                        l.horizontalChainStyle, CHAIN_SPREAD);
                writeVariable("app:layout_constraintVertical_chainStyle",
                        l.verticalChainStyle, CHAIN_SPREAD);

                writeVariable("app:barrierDirection", l.mBarrierDirection, UNSET);
                writeVariable("app:barrierMargin", l.mBarrierMargin, 0);

                writeDimension("app:layout_marginLeft", l.leftMargin, 0);
                writeDimension("app:layout_goneMarginLeft",
                        l.goneLeftMargin, Layout.UNSET_GONE_MARGIN);
                writeDimension("app:layout_marginRight", l.rightMargin, 0);
                writeDimension("app:layout_goneMarginRight",
                        l.goneRightMargin, Layout.UNSET_GONE_MARGIN);
                writeDimension("app:layout_marginStart", l.startMargin, 0);
                writeDimension("app:layout_goneMarginStart",
                        l.goneStartMargin, Layout.UNSET_GONE_MARGIN);
                writeDimension("app:layout_marginEnd", l.endMargin, 0);
                writeDimension("app:layout_goneMarginEnd",
                        l.goneEndMargin, Layout.UNSET_GONE_MARGIN);
                writeDimension("app:layout_marginTop", l.topMargin, 0);
                writeDimension("app:layout_goneMarginTop",
                        l.goneTopMargin, Layout.UNSET_GONE_MARGIN);
                writeDimension("app:layout_marginBottom", l.bottomMargin, 0);
                writeDimension("app:layout_goneMarginBottom",
                        l.goneBottomMargin, Layout.UNSET_GONE_MARGIN);
                writeDimension("app:goneBaselineMargin",
                        l.goneBaselineMargin, Layout.UNSET_GONE_MARGIN);
                writeDimension("app:baselineMargin", l.baselineMargin, 0);

                writeBoolen("app:layout_constrainedWidth", l.constrainedWidth, false);
                writeBoolen("app:layout_constrainedHeight",
                        l.constrainedHeight, false);
                writeBoolen("app:barrierAllowsGoneWidgets",
                        l.mBarrierAllowsGoneWidgets, true);
                writeVariable("app:layout_wrapBehaviorInParent", l.mWrapBehavior,
                        ConstraintWidget.WRAP_BEHAVIOR_INCLUDED);

                writeXmlConstraint("app:baselineToBaseline", l.baselineToBaseline);
                writeXmlConstraint("app:baselineToBottom", l.baselineToBottom);
                writeXmlConstraint("app:baselineToTop", l.baselineToTop);
                writeXmlConstraint("app:layout_constraintBottom_toBottomOf", l.bottomToBottom);
                writeXmlConstraint("app:layout_constraintBottom_toTopOf", l.bottomToTop);
                writeXmlConstraint("app:layout_constraintEnd_toEndOf", l.endToEnd);
                writeXmlConstraint("app:layout_constraintEnd_toStartOf", l.endToStart);
                writeXmlConstraint("app:layout_constraintLeft_toLeftOf", l.leftToLeft);
                writeXmlConstraint("app:layout_constraintLeft_toRightOf", l.leftToRight);
                writeXmlConstraint("app:layout_constraintRight_toLeftOf", l.rightToLeft);
                writeXmlConstraint("app:layout_constraintRight_toRightOf", l.rightToRight);
                writeXmlConstraint("app:layout_constraintStart_toEndOf", l.startToEnd);
                writeXmlConstraint("app:layout_constraintStart_toStartOf", l.startToStart);
                writeXmlConstraint("app:layout_constraintTop_toBottomOf", l.topToBottom);
                writeXmlConstraint("app:layout_constraintTop_toTopOf", l.topToTop);

                String[] typesConstraintDefault = {"spread", "wrap", "percent"};
                writeEnum("app:layout_constraintHeight_default", l.heightDefault,
                        typesConstraintDefault, ConstraintWidget.MATCH_CONSTRAINT_SPREAD);
                writeVariable("app:layout_constraintHeight_percent", l.heightPercent, 1);
                writeDimension("app:layout_constraintHeight_min", l.heightMin, 0);
                writeDimension("app:layout_constraintHeight_max", l.heightMax, 0);
                writeBoolen("android:layout_constrainedHeight",
                        l.constrainedHeight, false);

                writeEnum("app:layout_constraintWidth_default",
                        l.widthDefault, typesConstraintDefault,
                        ConstraintWidget.MATCH_CONSTRAINT_SPREAD);
                writeVariable("app:layout_constraintWidth_percent", l.widthPercent, 1);
                writeDimension("app:layout_constraintWidth_min", l.widthMin, 0);
                writeDimension("app:layout_constraintWidth_max", l.widthMax, 0);
                writeBoolen("android:layout_constrainedWidth",
                        l.constrainedWidth, false);

                writeVariable("app:layout_constraintVertical_weight",
                        l.verticalWeight, UNSET);
                writeVariable("app:layout_constraintHorizontal_weight",
                        l.horizontalWeight, UNSET);
                writeVariable("app:layout_constraintHorizontal_chainStyle",
                        l.horizontalChainStyle);
                writeVariable("app:layout_constraintVertical_chainStyle",
                        l.verticalChainStyle);
                String[] barrierDir = {"left", "right", "top", "bottom", "start", "end"};
                writeEnum("app:barrierDirection", l.mBarrierDirection, barrierDir, UNSET);
                writeVariable("app:layout_constraintTag", l.mConstraintTag, null);

                if (l.mReferenceIds != null) {
                    writeVariable("'ReferenceIds'", l.mReferenceIds);
                }
                mWriter.write(" />\n");
            }
            mWriter.write("</ConstraintSet>\n");
        }

        private static final String SPACE = "\n       ";

        private void writeBoolen(String dimString, boolean val, boolean def) throws IOException {
            if (val != def) {
                mWriter.write(SPACE + dimString + "=\"" + val + "dp\"");
            }
        }

        private void writeEnum(String dimString,
                               int val,
                               String[] types,
                               int def) throws IOException {
            if (val != def) {
                mWriter.write(SPACE + dimString + "=\"" + types[val] + "\"");
            }
        }

        private void writeDimension(String dimString, int dim, int def) throws IOException {
            if (dim != def) {
                mWriter.write(SPACE + dimString + "=\"" + dim + "dp\"");
            }
        }

        private void writeBaseDimension(String dimString, int dim, int def) throws IOException {
            if (dim != def) {
                if (dim == -2) {
                    mWriter.write(SPACE + dimString + "=\"wrap_content\"");

                } else if (dim == -1) {
                    mWriter.write(SPACE + dimString + "=\"match_parent\"");

                } else {
                    mWriter.write(SPACE + dimString + "=\"" + dim + "dp\"");
                }
            }
        }

        HashMap<Integer, String> mIdMap = new HashMap<>();

        String getName(int id) {
            if (mIdMap.containsKey(id)) {
                return "@+id/" + mIdMap.get(id) + "";
            }
            if (id == 0) {
                return "parent";
            }
            String name = lookup(id);
            mIdMap.put(id, name);
            return "@+id/" + name + "";
        }

        String lookup(int id) {
            try {
                if (id != -1) {
                    return mContext.getResources().getResourceEntryName(id);
                } else {
                    return "unknown" + ++mUnknownCount;
                }
            } catch (Exception ex) {
                return "unknown" + ++mUnknownCount;
            }
        }

        void writeXmlConstraint(String str, int leftToLeft) throws IOException {
            if (leftToLeft == UNSET) {
                return;
            }
            mWriter.write(SPACE + str);
            mWriter.write("=\"" + getName(leftToLeft) + "\"");

        }

        void writeConstraint(String my, int leftToLeft,
                             String other,
                             int margin,
                             int goneMargin) throws IOException {
            if (leftToLeft == UNSET) {
                return;
            }
            mWriter.write(SPACE + my);
            mWriter.write(":[");
            mWriter.write(getName(leftToLeft));
            mWriter.write(" , ");
            mWriter.write(other);
            if (margin != 0) {
                mWriter.write(" , " + margin);
            }
            mWriter.write("],\n");

        }

        void writeCircle(int circleConstraint,
                         float circleAngle,
                         int circleRadius) throws IOException {
            if (circleConstraint == UNSET) {
                return;
            }
            mWriter.write("circle");
            mWriter.write(":[");
            mWriter.write(getName(circleConstraint));
            mWriter.write(", " + circleAngle);
            mWriter.write(circleRadius + "]");
        }

        void writeVariable(String name, int value) throws IOException {
            if (value == 0 || value == -1) {
                return;
            }
            mWriter.write(SPACE + name + "=\"" + value + "\"\n");
        }

        void writeVariable(String name, float value, float def) throws IOException {
            if (value == def) {
                return;
            }
            mWriter.write(SPACE + name);
            mWriter.write("=\"" + value + "\"");

        }

        void writeVariable(String name, String value, String def) throws IOException {
            if (value == null || value.equals(def)) {
                return;
            }
            mWriter.write(SPACE + name);
            mWriter.write("=\"" + value + "\"");

        }

        void writeVariable(String name, int[] value) throws IOException {
            if (value == null) {
                return;
            }
            mWriter.write(SPACE + name);
            mWriter.write(":");
            for (int i = 0; i < value.length; i++) {
                mWriter.write(((i == 0) ? "[" : ", ") + getName(value[i]));
            }
            mWriter.write("],\n");
        }

        void writeVariable(String name, String value) throws IOException {
            if (value == null) {
                return;
            }
            mWriter.write(name);
            mWriter.write(":");
            mWriter.write(", " + value);
            mWriter.write("\n");

        }
    }

    // ================================== JSON ===============================================
    class WriteJsonEngine {
        Writer mWriter;
        ConstraintLayout mLayout;
        Context mContext;
        int mFlags;
        int mUnknownCount = 0;
        final String mLEFT = "'left'";
        final String mRIGHT = "'right'";
        final String mBASELINE = "'baseline'";
        final String mBOTTOM = "'bottom'";
        final String mTOP = "'top'";
        final String mSTART = "'start'";
        final String mEND = "'end'";
        private static final String SPACE = "       ";

        WriteJsonEngine(Writer writer, ConstraintLayout layout, int flags) throws IOException {
            this.mWriter = writer;
            this.mLayout = layout;
            this.mContext = layout.getContext();
            this.mFlags = flags;
        }

        void writeLayout() throws IOException {
            mWriter.write("\n\'ConstraintSet\':{\n");
            for (Integer id : mConstraints.keySet()) {
                Constraint c = mConstraints.get(id);
                String idName = getName(id);
                mWriter.write(idName + ":{\n");
                Layout l = c.layout;

                writeDimension("height", l.mHeight, l.heightDefault, l.heightPercent,
                        l.heightMin, l.heightMax, l.constrainedHeight);
                writeDimension("width", l.mWidth, l.widthDefault, l.widthPercent,
                        l.widthMin, l.widthMax, l.constrainedWidth);

                writeConstraint(mLEFT, l.leftToLeft, mLEFT, l.leftMargin, l.goneLeftMargin);
                writeConstraint(mLEFT, l.leftToRight, mRIGHT, l.leftMargin, l.goneLeftMargin);
                writeConstraint(mRIGHT, l.rightToLeft, mLEFT, l.rightMargin, l.goneRightMargin);
                writeConstraint(mRIGHT, l.rightToRight, mRIGHT, l.rightMargin, l.goneRightMargin);
                writeConstraint(mBASELINE, l.baselineToBaseline, mBASELINE, UNSET,
                        l.goneBaselineMargin);
                writeConstraint(mBASELINE, l.baselineToTop, mTOP, UNSET, l.goneBaselineMargin);
                writeConstraint(mBASELINE, l.baselineToBottom,
                        mBOTTOM, UNSET, l.goneBaselineMargin);

                writeConstraint(mTOP, l.topToBottom, mBOTTOM, l.topMargin, l.goneTopMargin);
                writeConstraint(mTOP, l.topToTop, mTOP, l.topMargin, l.goneTopMargin);
                writeConstraint(mBOTTOM, l.bottomToBottom, mBOTTOM, l.bottomMargin,
                        l.goneBottomMargin);
                writeConstraint(mBOTTOM, l.bottomToTop, mTOP, l.bottomMargin, l.goneBottomMargin);
                writeConstraint(mSTART, l.startToStart, mSTART, l.startMargin, l.goneStartMargin);
                writeConstraint(mSTART, l.startToEnd, mEND, l.startMargin, l.goneStartMargin);
                writeConstraint(mEND, l.endToStart, mSTART, l.endMargin, l.goneEndMargin);
                writeConstraint(mEND, l.endToEnd, mEND, l.endMargin, l.goneEndMargin);
                writeVariable("'horizontalBias'", l.horizontalBias, 0.5f);
                writeVariable("'verticalBias'", l.verticalBias, 0.5f);

                writeCircle(l.circleConstraint, l.circleAngle, l.circleRadius);

                writeGuideline(l.orientation, l.guideBegin, l.guideEnd, l.guidePercent);
                writeVariable("'dimensionRatio'", l.dimensionRatio);
                writeVariable("'barrierMargin'", l.mBarrierMargin);
                writeVariable("'type'", l.mHelperType);
                writeVariable("'ReferenceId'", l.mReferenceIdString);
                writeVariable("'mBarrierAllowsGoneWidgets'",
                        l.mBarrierAllowsGoneWidgets, true);
                writeVariable("'WrapBehavior'", l.mWrapBehavior);

                writeVariable("'verticalWeight'", l.verticalWeight);
                writeVariable("'horizontalWeight'", l.horizontalWeight);
                writeVariable("'horizontalChainStyle'", l.horizontalChainStyle);
                writeVariable("'verticalChainStyle'", l.verticalChainStyle);
                writeVariable("'barrierDirection'", l.mBarrierDirection);
                if (l.mReferenceIds != null) {
                    writeVariable("'ReferenceIds'", l.mReferenceIds);
                }
                mWriter.write("}\n");
            }
            mWriter.write("}\n");
        }

        private void writeGuideline(int orientation,
                                    int guideBegin,
                                    int guideEnd,
                                    float guidePercent) throws IOException {
            writeVariable("'orientation'", orientation);
            writeVariable("'guideBegin'", guideBegin);
            writeVariable("'guideEnd'", guideEnd);
            writeVariable("'guidePercent'", guidePercent);

        }


        private void writeDimension(String dimString,
                                    int dim,
                                    int dimDefault,
                                    float dimPercent,
                                    int dimMin,
                                    int dimMax,
                                    boolean unusedConstrainedDim) throws IOException {
            if (dim == 0) {
                if (dimMax != UNSET || dimMin != UNSET) {
                    switch (dimDefault) {
                        case 0: // spread
                            mWriter.write(SPACE + dimString
                                    + ": {'spread' ," + dimMin + ", " + dimMax + "}\n");
                            break;
                        case 1: //  wrap
                            mWriter.write(SPACE + dimString
                                    + ": {'wrap' ," + dimMin + ", " + dimMax + "}\n");
                            return;
                        case 2: // percent
                            mWriter.write(SPACE + dimString + ": {'" + dimPercent
                                    + "'% ," + dimMin + ", " + dimMax + "}\n");
                            return;
                    }
                    return;
                }

                switch (dimDefault) {
                    case 0: // spread is the default
                        break;
                    case 1: //  wrap
                        mWriter.write(SPACE + dimString + ": '???????????',\n");
                        return;
                    case 2: // percent
                        mWriter.write(SPACE + dimString + ": '" + dimPercent + "%',\n");
                        return;
                }

            } else if (dim == -2) {
                mWriter.write(SPACE + dimString + ": 'wrap'\n");
            } else if (dim == -1) {
                mWriter.write(SPACE + dimString + ": 'parent'\n");
            } else {
                mWriter.write(SPACE + dimString + ": " + dim + ",\n");
            }
        }

        HashMap<Integer, String> mIdMap = new HashMap<>();

        String getName(int id) {
            if (mIdMap.containsKey(id)) {
                return "\'" + mIdMap.get(id) + "\'";
            }
            if (id == 0) {
                return "'parent'";
            }
            String name = lookup(id);
            mIdMap.put(id, name);
            return "\'" + name + "\'";
        }

        String lookup(int id) {
            try {
                if (id != -1) {
                    return mContext.getResources().getResourceEntryName(id);
                } else {
                    return "unknown" + ++mUnknownCount;
                }
            } catch (Exception ex) {
                return "unknown" + ++mUnknownCount;
            }
        }

        void writeConstraint(String my,
                             int leftToLeft,
                             String other,
                             int margin,
                             int goneMargin) throws IOException {
            if (leftToLeft == UNSET) {
                return;
            }
            mWriter.write(SPACE + my);
            mWriter.write(":[");
            mWriter.write(getName(leftToLeft));
            mWriter.write(" , ");
            mWriter.write(other);
            if (margin != 0) {
                mWriter.write(" , " + margin);
            }
            mWriter.write("],\n");

        }

        void writeCircle(int circleConstraint,
                         float circleAngle,
                         int circleRadius) throws IOException {
            if (circleConstraint == UNSET) {
                return;
            }
            mWriter.write(SPACE + "circle");
            mWriter.write(":[");
            mWriter.write(getName(circleConstraint));
            mWriter.write(", " + circleAngle);
            mWriter.write(circleRadius + "]");
        }

        void writeVariable(String name, int value) throws IOException {
            if (value == 0 || value == -1) {
                return;
            }
            mWriter.write(SPACE + name);
            mWriter.write(":");

            mWriter.write(", " + value);
            mWriter.write("\n");

        }

        void writeVariable(String name, float value) throws IOException {
            if (value == UNSET) {
                return;
            }
            mWriter.write(SPACE + name);

            mWriter.write(": " + value);
            mWriter.write(",\n");

        }

        void writeVariable(String name, float value, float def) throws IOException {
            if (value == def) {
                return;
            }
            mWriter.write(SPACE + name);

            mWriter.write(": " + value);
            mWriter.write(",\n");

        }

        void writeVariable(String name, boolean value) throws IOException {
            if (!value) {
                return;
            }
            mWriter.write(SPACE + name);

            mWriter.write(": " + value);
            mWriter.write(",\n");

        }
        void writeVariable(String name, boolean value , boolean def) throws IOException {
            if (value == def) {
                return;
            }
            mWriter.write(SPACE + name);

            mWriter.write(": " + value);
            mWriter.write(",\n");

        }

        void writeVariable(String name, int[] value) throws IOException {
            if (value == null) {
                return;
            }
            mWriter.write(SPACE + name);
            mWriter.write(": ");
            for (int i = 0; i < value.length; i++) {
                mWriter.write(((i == 0) ? "[" : ", ") + getName(value[i]));
            }
            mWriter.write("],\n");
        }

        void writeVariable(String name, String value) throws IOException {
            if (value == null) {
                return;
            }
            mWriter.write(SPACE + name);
            mWriter.write(":");
            mWriter.write(", " + value);
            mWriter.write("\n");

        }
    }

}
