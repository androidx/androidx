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

package androidx.constraintlayout.widget;

import android.os.Build;
import android.view.View;
import android.view.ViewGroup;

/**
 *  <b>Added in 2.0</b>
 *  <p>
 *  ConstraintProperties provides an easy to use api to update the layout params
 *  of {@link ConstraintLayout} children
 *  </p>
 */
public class ConstraintProperties {
    ConstraintLayout.LayoutParams mParams;
    View mView;
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
     * The right side of a view in left to right languages.
     * In right to left languages it corresponds to the left side of the view
     */
    public static final int END = ConstraintLayout.LayoutParams.END;
    /**
     * Used to indicate a parameter is cleared or not set
     */
    public static final int UNSET = ConstraintLayout.LayoutParams.UNSET;
    /**
     * References the id of the parent.
     */
    public static final int PARENT_ID = ConstraintLayout.LayoutParams.PARENT_ID;

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

    /**
     * Center view between the other two widgets.
     *
     * @param firstID      ID of the first widget to connect the left or top of the widget to
     * @param firstSide    the side of the widget to connect to
     * @param firstMargin  the connection margin
     * @param secondId     the ID of the second widget to connect to right or top of the widget to
     * @param secondSide   the side of the widget to connect to
     * @param secondMargin the connection margin
     * @param bias         the ratio between two connections
     * @return this
     */

    public ConstraintProperties center(int firstID,
                                       int firstSide,
                                       int firstMargin,
                                       int secondId,
                                       int secondSide,
                                       int secondMargin,
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
            connect(LEFT, firstID, firstSide, firstMargin);
            connect(RIGHT, secondId, secondSide, secondMargin);

            mParams.horizontalBias = bias;
        } else if (firstSide == START || firstSide == END) {
            connect(START, firstID, firstSide, firstMargin);
            connect(END, secondId, secondSide, secondMargin);

            mParams.horizontalBias = bias;
        } else {
            connect(TOP, firstID, firstSide, firstMargin);
            connect(BOTTOM, secondId, secondSide, secondMargin);
            mParams.verticalBias = bias;
        }

        return this;
    }

    /**
     * Centers the widget horizontally to the left and right side on another widgets sides.
     *
     * @param leftId      The Id of the widget on the left side
     * @param leftSide    The side of the leftId widget to connect to
     * @param leftMargin  The margin on the left side
     * @param rightId     The Id of the widget on the right side
     * @param rightSide   The side  of the rightId widget to connect to
     * @param rightMargin The margin on the right side
     * @param bias        The ratio of the space on the left vs.
     *                    right sides 0.5 is centered (default)
     * @return this
     */
    public ConstraintProperties centerHorizontally(int leftId,
                                                   int leftSide,
                                                   int leftMargin,
                                                   int rightId,
                                                   int rightSide,
                                                   int rightMargin,
                                                   float bias) {
        connect(LEFT, leftId, leftSide, leftMargin);
        connect(RIGHT, rightId, rightSide, rightMargin);
        mParams.horizontalBias = bias;
        return this;
    }

    /**
     * Centers the widgets horizontally to the left and right side on another widgets sides.
     *
     * @param startId     The Id of the widget on the start side (left in non rtl languages)
     * @param startSide   The side of the startId widget to connect to
     * @param startMargin The margin on the start side
     * @param endId       The Id of the widget on the start side (left in non rtl languages)
     * @param endSide     The side of the endId widget to connect to
     * @param endMargin   The margin on the end side
     * @param bias        The ratio of the space on the start vs end side 0.5 is centered (default)
     * @return this
     */
    public ConstraintProperties centerHorizontallyRtl(int startId,
                                                      int startSide,
                                                      int startMargin,
                                                      int endId,
                                                      int endSide,
                                                      int endMargin,
                                                      float bias) {
        connect(START, startId, startSide, startMargin);
        connect(END, endId, endSide, endMargin);
        mParams.horizontalBias = bias;
        return this;
    }

    /**
     * Centers the widgets Vertically to the top and bottom side on another widgets sides.
     *
     * @param topId        The Id of the widget on the top side
     * @param topSide      The side of the leftId widget to connect to
     * @param topMargin    The margin on the top side
     * @param bottomId     The Id of the widget on the bottom side
     * @param bottomSide   The side of the bottomId widget to connect to
     * @param bottomMargin The margin on the bottom side
     * @param bias         The ratio of the space on the top vs.
     *                     bottom sides 0.5 is centered (default)
     * @return this
     */
    public ConstraintProperties centerVertically(int topId,
                                                 int topSide,
                                                 int topMargin,
                                                 int bottomId,
                                                 int bottomSide,
                                                 int bottomMargin,
                                                 float bias) {
        connect(TOP, topId, topSide, topMargin);
        connect(BOTTOM, bottomId, bottomSide, bottomMargin);
        mParams.verticalBias = bias;
        return this;
    }

    /**
     * Centers the view horizontally relative to toView's position.
     *
     * @param toView ID of view to center on (or in)
     * @return this
     */
    public ConstraintProperties centerHorizontally(int toView) {
        if (toView == PARENT_ID) {
            center(PARENT_ID, ConstraintSet.LEFT, 0, PARENT_ID,
                    ConstraintSet.RIGHT, 0, 0.5f);
        } else {
            center(toView, ConstraintSet.RIGHT, 0, toView,
                    ConstraintSet.LEFT, 0, 0.5f);
        }
        return this;
    }

    /**
     * Centers the view horizontally relative to toView's position.
     *
     * @param toView ID of view to center on (or in)
     * @return this
     */
    public ConstraintProperties centerHorizontallyRtl(int toView) {
        if (toView == PARENT_ID) {
            center(PARENT_ID, ConstraintSet.START, 0, PARENT_ID,
                    ConstraintSet.END, 0, 0.5f);
        } else {
            center(toView, ConstraintSet.END, 0, toView,
                    ConstraintSet.START, 0, 0.5f);
        }
        return this;
    }

    /**
     * Centers the view vertically relative to toView's position.
     *
     * @param toView ID of view to center on (or in)
     * @return this
     */
    public ConstraintProperties centerVertically(int toView) {
        if (toView == PARENT_ID) {
            center(PARENT_ID, ConstraintSet.TOP, 0, PARENT_ID,
                    ConstraintSet.BOTTOM, 0, 0.5f);
        } else {
            center(toView, ConstraintSet.BOTTOM, 0, toView,
                    ConstraintSet.TOP, 0, 0.5f);
        }
        return this;
    }

    /**
     * Remove a constraint from this view.
     *
     * @param anchor the Anchor to remove constraint from
     * @return this
     */
    public ConstraintProperties removeConstraints(int anchor) {
        switch (anchor) {
            case LEFT:
                mParams.leftToRight = mParams.UNSET;
                mParams.leftToLeft = mParams.UNSET;
                mParams.leftMargin = mParams.UNSET;
                mParams.goneLeftMargin = mParams.GONE_UNSET;
                break;
            case RIGHT:
                mParams.rightToRight = mParams.UNSET;
                mParams.rightToLeft = mParams.UNSET;
                mParams.rightMargin = mParams.UNSET;
                mParams.goneRightMargin = mParams.GONE_UNSET;
                break;
            case TOP:
                mParams.topToBottom = mParams.UNSET;
                mParams.topToTop = mParams.UNSET;
                mParams.topMargin = mParams.UNSET;
                mParams.goneTopMargin = mParams.GONE_UNSET;
                break;
            case BOTTOM:
                mParams.bottomToTop = mParams.UNSET;
                mParams.bottomToBottom = mParams.UNSET;
                mParams.bottomMargin = mParams.UNSET;
                mParams.goneBottomMargin = mParams.GONE_UNSET;
                break;
            case BASELINE:
                mParams.baselineToBaseline = mParams.UNSET;
                break;
            case START:
                mParams.startToEnd = mParams.UNSET;
                mParams.startToStart = mParams.UNSET;
                mParams.setMarginStart(mParams.UNSET);
                mParams.goneStartMargin = mParams.GONE_UNSET;
                break;
            case END:
                mParams.endToStart = mParams.UNSET;
                mParams.endToEnd = mParams.UNSET;
                mParams.setMarginEnd(mParams.UNSET);
                mParams.goneEndMargin = mParams.GONE_UNSET;
                break;
            default:
                throw new IllegalArgumentException("unknown constraint");
        }
        return this;
    }

    /**
     * Sets the margin.
     *
     * @param anchor The side to adjust the margin on
     * @param value  The new value for the margin
     * @return this
     */
    public ConstraintProperties margin(int anchor, int value) {
        switch (anchor) {
            case LEFT:
                mParams.leftMargin = value;
                break;
            case RIGHT:
                mParams.rightMargin = value;
                break;
            case TOP:
                mParams.topMargin = value;
                break;
            case BOTTOM:
                mParams.bottomMargin = value;
                break;
            case BASELINE:
                throw new IllegalArgumentException("baseline does not support margins");
            case START:
                mParams.setMarginStart(value);
                break;
            case END:
                mParams.setMarginEnd(value);
                break;
            default:
                throw new IllegalArgumentException("unknown constraint");
        }
        return this;
    }

    /**
     * Sets the gone margin.
     *
     * @param anchor The side to adjust the margin on
     * @param value  The new value for the margin
     * @return this
     */
    public ConstraintProperties goneMargin(int anchor, int value) {
        switch (anchor) {
            case LEFT:
                mParams.goneLeftMargin = value;
                break;
            case RIGHT:
                mParams.goneRightMargin = value;
                break;
            case TOP:
                mParams.goneTopMargin = value;
                break;
            case BOTTOM:
                mParams.goneBottomMargin = value;
                break;
            case BASELINE:
                throw new IllegalArgumentException("baseline does not support margins");
            case START:
                mParams.goneStartMargin = value;
                break;
            case END:
                mParams.goneEndMargin = value;
                break;
            default:
                throw new IllegalArgumentException("unknown constraint");
        }
        return this;
    }

    /**
     * Adjust the horizontal bias of the view (used with views constrained on left and right).
     *
     * @param bias the new bias 0.5 is in the middle
     * @return this
     */
    public ConstraintProperties horizontalBias(float bias) {
        mParams.horizontalBias = bias;
        return this;
    }

    /**
     * Adjust the vertical bias of the view (used with views constrained on left and right).
     *
     * @param bias the new bias 0.5 is in the middle
     * @return this
     */
    public ConstraintProperties verticalBias(float bias) {
        mParams.verticalBias = bias;
        return this;
    }

    /**
     * Constrains the views aspect ratio.
     * For Example a HD screen is 16 by 9 = 16/(float)9 = 1.777f.
     *
     * @param ratio The ratio of the width to height (width / height)
     * @return this
     */
    public ConstraintProperties dimensionRatio(String ratio) {
        mParams.dimensionRatio = ratio;
        return this;
    }

    /**
     * Adjust the visibility of a view.
     *
     * @param visibility the visibility (View.VISIBLE, View.INVISIBLE, View.GONE)
     * @return this
     */
    public ConstraintProperties visibility(int visibility) {
        mView.setVisibility(visibility);
        return this;
    }

    /**
     * Adjust the alpha of a view.
     *
     * @param alpha the alpha
     * @return this
     */
    public ConstraintProperties alpha(float alpha) {
        mView.setAlpha(alpha);
        return this;
    }

    /**
     * Set the elevation of a view.
     *
     * @param elevation the elevation
     * @return this
     */
    public ConstraintProperties elevation(float elevation) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mView.setElevation(elevation);
        }
        return this;
    }

    /**
     * Adjust the post-layout rotation about the Z axis of a view.
     *
     * @param rotation the rotation about the Z axis
     * @return this
     */
    public ConstraintProperties rotation(float rotation) {
        mView.setRotation(rotation);
        return this;
    }

    /**
     * Adjust the post-layout rotation about the X axis of a view.
     *
     * @param rotationX the rotation about the X axis
     * @return this
     */
    public ConstraintProperties rotationX(float rotationX) {
        mView.setRotationX(rotationX);
        return this;
    }

    /**
     * Adjust the post-layout rotation about the Y axis of a view.
     *
     * @param rotationY the rotation about the Y axis
     * @return this
     */
    public ConstraintProperties rotationY(float rotationY) {
        mView.setRotationY(rotationY);
        return this;
    }

    /**
     * Adjust the post-layout scale in X of a view.
     *
     * @param scaleX the scale in X
     * @return this
     */
    public ConstraintProperties scaleX(float scaleX) {
        mView.setScaleY(scaleX);
        return this;
    }

    /**
     * Adjust the post-layout scale in Y of a view.
     *
     * @param scaleY the scale in Y
     * @return this
     */
    public ConstraintProperties scaleY(float scaleY) {
        return this;
    }

    /**
     * Set X location of the pivot point around which the view will rotate and scale.
     *
     * @param transformPivotX X location of the pivot point.
     * @return this
     */
    public ConstraintProperties transformPivotX(float transformPivotX) {
        mView.setPivotX(transformPivotX);
        return this;
    }

    /**
     * Set Y location of the pivot point around which the view will rotate and scale.
     *
     * @param transformPivotY Y location of the pivot point.
     * @return this
     */
    public ConstraintProperties transformPivotY(float transformPivotY) {
        mView.setPivotY(transformPivotY);
        return this;
    }

    /**
     * Set X and Y location of the pivot point around which the view will rotate and scale.
     *
     * @param transformPivotX X location of the pivot point.
     * @param transformPivotY Y location of the pivot point.
     * @return this
     */
    public ConstraintProperties transformPivot(float transformPivotX, float transformPivotY) {
        mView.setPivotX(transformPivotX);
        mView.setPivotY(transformPivotY);
        return this;
    }

    /**
     * Adjust the post-layout X translation of a view.
     *
     * @param translationX the translation in X
     * @return this
     */
    public ConstraintProperties translationX(float translationX) {
        mView.setTranslationX(translationX);
        return this;
    }

    /**
     * Adjust the  post-layout Y translation of a view.
     *
     * @param translationY the translation in Y
     * @return this
     */
    public ConstraintProperties translationY(float translationY) {
        mView.setTranslationY(translationY);
        return this;
    }

    /**
     * Adjust the  post-layout X and Y translation of a view.
     *
     * @param translationX the translation in X
     * @param translationY the translation in Y
     * @return this
     */
    public ConstraintProperties translation(float translationX, float translationY) {
        mView.setTranslationX(translationX);
        mView.setTranslationY(translationY);
        return this;
    }

    /**
     * Adjust the post-layout translation in Z of a view.
     * This is the preferred way to adjust the shadow.
     *
     * @param translationZ the translationZ
     * @return this
     */
    public ConstraintProperties translationZ(float translationZ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mView.setTranslationZ(translationZ);
        }
        return this;
    }

    /**
     * Sets the height of the view.
     *
     * @param height the height of the view
     * @return this
     */
    public ConstraintProperties constrainHeight(int height) {
        mParams.height = height;
        return this;
    }

    /**
     * Sets the width of the view.
     *
     * @param width the width of the view
     * @return this
     */
    public ConstraintProperties constrainWidth(int width) {
        mParams.width = width;
        return this;
    }

    /**
     * Sets the maximum height of the view. It is a dimension, It is only applicable if height is
     * #MATCH_CONSTRAINT}.
     *
     * @param height the maximum height of the view
     * @return this
     */
    public ConstraintProperties constrainMaxHeight(int height) {
        mParams.matchConstraintMaxHeight = height;
        return this;
    }

    /**
     * Sets the maximum width of the view. It is a dimension, It is only applicable if height is
     * #MATCH_CONSTRAINT}.
     *
     * @param width the maximum width of the view
     * @return this
     */
    public ConstraintProperties constrainMaxWidth(int width) {
        mParams.matchConstraintMaxWidth = width;
        return this;
    }

    /**
     * Sets the minimum height of the view. It is a dimension, It is only applicable if height is
     * #MATCH_CONSTRAINT}.
     *
     * @param height the minimum height of the view
     * @return this
     */
    public ConstraintProperties constrainMinHeight(int height) {
        mParams.matchConstraintMinHeight = height;
        return this;
    }

    /**
     * Sets the minimum width of the view. It is a dimension, It is only applicable if height is
     * #MATCH_CONSTRAINT}.
     *
     * @param width the minimum width of the view
     * @return this
     */
    public ConstraintProperties constrainMinWidth(int width) {
        mParams.matchConstraintMinWidth = width;
        return this;
    }

    /**
     * Sets how the height is calculated ether MATCH_CONSTRAINT_WRAP or MATCH_CONSTRAINT_SPREAD.
     * Default is spread.
     *
     * @param height MATCH_CONSTRAINT_WRAP or MATCH_CONSTRAINT_SPREAD
     * @return this
     */
    public ConstraintProperties constrainDefaultHeight(int height) {
        mParams.matchConstraintDefaultHeight = height;
        return this;
    }

    /**
     * Sets how the width is calculated ether MATCH_CONSTRAINT_WRAP or MATCH_CONSTRAINT_SPREAD.
     * Default is spread.
     *
     * @param width MATCH_CONSTRAINT_WRAP or MATCH_CONSTRAINT_SPREAD
     * @return this
     */
    public ConstraintProperties constrainDefaultWidth(int width) {
        mParams.matchConstraintDefaultWidth = width;
        return this;
    }

    /**
     * The child's weight that we can use to distribute the available horizontal space
     * in a chain, if the dimension behaviour is set to MATCH_CONSTRAINT
     *
     * @param weight the weight that we can use to distribute the horizontal space
     * @return this
     */
    public ConstraintProperties horizontalWeight(float weight) {
        mParams.horizontalWeight = weight;
        return this;
    }

    /**
     * The child's weight that we can use to distribute the available vertical space
     * in a chain, if the dimension behaviour is set to MATCH_CONSTRAINT
     *
     * @param weight the weight that we can use to distribute the vertical space
     * @return this
     */
    public ConstraintProperties verticalWeight(float weight) {
        mParams.verticalWeight = weight;
        return this;
    }

    /**
     * How the elements of the horizontal chain will be positioned. If the dimension
     * behaviour is set to MATCH_CONSTRAINT. The possible values are:
     * <p>
     * <ul>
     *   <li>CHAIN_SPREAD -- the elements will be spread out</li>
     *   <li>CHAIN_SPREAD_INSIDE -- similar, but the endpoints of the
     *   chain will not be spread out</li>
     *   <li>CHAIN_PACKED -- the elements of the chain will be packed together. The horizontal
     * bias attribute of the child will then affect the positioning of the packed elements</li>
     * </ul>
     *
     * @param chainStyle the weight that we can use to distribute the horizontal space
     * @return this
     */
    public ConstraintProperties horizontalChainStyle(int chainStyle) {
        mParams.horizontalChainStyle = chainStyle;
        return this;
    }

    /**
     * How the elements of the vertical chain will be positioned. in a chain, if the dimension
     * behaviour is set to MATCH_CONSTRAINT
     * <p>
     * <ul>
     *   <li>CHAIN_SPREAD -- the elements will be spread out</li>
     *   <li>CHAIN_SPREAD_INSIDE -- similar, but the endpoints of the
     *   chain will not be spread out</li>
     *   <li>CHAIN_PACKED -- the elements of the chain will be packed together. The horizontal
     * bias attribute of the child will then affect the positioning of the packed elements</li>
     * </ul>
     *
     * @param chainStyle the weight that we can use to distribute the horizontal space
     * @return this
     */
    public ConstraintProperties verticalChainStyle(int chainStyle) {
        mParams.verticalChainStyle = chainStyle;
        return this;
    }

    /**
     * Adds the view to a horizontal chain.
     *
     * @param leftId  id of the view in chain to the left
     * @param rightId id of the view in chain to the right
     * @return this
     */
    public ConstraintProperties addToHorizontalChain(int leftId, int rightId) {
        connect(LEFT, leftId, (leftId == PARENT_ID) ? LEFT : RIGHT, 0);
        connect(RIGHT, rightId, (rightId == PARENT_ID) ? RIGHT : LEFT, 0);
        if (leftId != PARENT_ID) {
            View leftView = ((ViewGroup) mView.getParent()).findViewById(leftId);
            ConstraintProperties leftProp = new ConstraintProperties(leftView);
            leftProp.connect(RIGHT, mView.getId(), LEFT, 0);
        }
        if (rightId != PARENT_ID) {
            View rightView = ((ViewGroup) mView.getParent()).findViewById(rightId);
            ConstraintProperties rightProp = new ConstraintProperties(rightView);
            rightProp.connect(LEFT, mView.getId(), RIGHT, 0);
        }
        return this;
    }

    /**
     * Adds the view to a horizontal chain using RTL attributes.
     *
     * @param leftId  id of the view in chain to the left
     * @param rightId id of the view in chain to the right
     * @return this
     */
    public ConstraintProperties addToHorizontalChainRTL(int leftId, int rightId) {
        connect(START, leftId, (leftId == PARENT_ID) ? START : END, 0);
        connect(END, rightId, (rightId == PARENT_ID) ? END : START, 0);
        if (leftId != PARENT_ID) {
            View leftView = ((ViewGroup) mView.getParent()).findViewById(leftId);
            ConstraintProperties leftProp = new ConstraintProperties(leftView);
            leftProp.connect(END, mView.getId(), START, 0);
        }
        if (rightId != PARENT_ID) {
            View rightView = ((ViewGroup) mView.getParent()).findViewById(rightId);
            ConstraintProperties rightProp = new ConstraintProperties(rightView);
            rightProp.connect(START, mView.getId(), END, 0);
        }
        return this;
    }

    /**
     * Adds a view to a vertical chain.
     *
     * @param topId    view above.
     * @param bottomId view below
     * @return this
     */
    public ConstraintProperties addToVerticalChain(int topId, int bottomId) {
        connect(TOP, topId, (topId == PARENT_ID) ? TOP : BOTTOM, 0);
        connect(BOTTOM, bottomId, (bottomId == PARENT_ID) ? BOTTOM : TOP, 0);
        if (topId != PARENT_ID) {
            View topView = ((ViewGroup) mView.getParent()).findViewById(topId);
            ConstraintProperties topProp = new ConstraintProperties(topView);
            topProp.connect(BOTTOM, mView.getId(), TOP, 0);
        }
        if (bottomId != PARENT_ID) {
            View bottomView = ((ViewGroup) mView.getParent()).findViewById(bottomId);
            ConstraintProperties bottomProp = new ConstraintProperties(bottomView);
            bottomProp.connect(TOP, mView.getId(), BOTTOM, 0);
        }
        return this;
    }

    /**
     * Removes a view from a vertical chain.
     * This assumes the view is connected to a vertical chain.
     * Its behaviour is undefined if not part of a vertical chain.
     *
     * @return this
     */
    public ConstraintProperties removeFromVerticalChain() {
        int topId = mParams.topToBottom;
        int bottomId = mParams.bottomToTop;
        if (topId != mParams.UNSET || bottomId != mParams.UNSET) {
            View topView = ((ViewGroup) mView.getParent()).findViewById(topId);
            ConstraintProperties topProp = new ConstraintProperties(topView);
            View bottomView = ((ViewGroup) mView.getParent()).findViewById(bottomId);
            ConstraintProperties bottomProp = new ConstraintProperties(bottomView);
            if (topId != mParams.UNSET && bottomId != mParams.UNSET) {
                // top and bottom connected to views
                topProp.connect(BOTTOM, bottomId, TOP, 0);
                bottomProp.connect(TOP, topId, BOTTOM, 0);
            } else if (topId != mParams.UNSET || bottomId != mParams.UNSET) {
                if (mParams.bottomToBottom != mParams.UNSET) {
                    // top connected to view. Bottom connected to parent
                    topProp.connect(BOTTOM, mParams.bottomToBottom, BOTTOM, 0);
                } else if (mParams.topToTop != mParams.UNSET) {
                    // bottom connected to view. Top connected to parent
                    bottomProp.connect(TOP, mParams.topToTop, TOP, 0);
                }
            }
        }

        removeConstraints(TOP);
        removeConstraints(BOTTOM);
        return this;
    }

    /**
     * Removes a view from a vertical chain.
     * This assumes the view is connected to a vertical chain.
     * Its behaviour is undefined if not part of a vertical chain.
     *
     * @return this
     */
    public ConstraintProperties removeFromHorizontalChain() {
        int leftId = mParams.leftToRight;
        int rightId = mParams.rightToLeft;

        if (leftId != mParams.UNSET || rightId != mParams.UNSET) {
            View leftView = ((ViewGroup) mView.getParent()).findViewById(leftId);
            ConstraintProperties leftProp = new ConstraintProperties(leftView);
            View rightView = ((ViewGroup) mView.getParent()).findViewById(rightId);
            ConstraintProperties rightProp = new ConstraintProperties(rightView);
            if (leftId != mParams.UNSET && rightId != mParams.UNSET) {
                // left and right connected to views
                leftProp.connect(RIGHT, rightId, LEFT, 0);
                rightProp.connect(LEFT, leftId, RIGHT, 0);
            } else if (leftId != mParams.UNSET || rightId != mParams.UNSET) {
                if (mParams.rightToRight != mParams.UNSET) {
                    // left connected to view. right connected to parent
                    leftProp.connect(RIGHT, mParams.rightToRight, RIGHT, 0);
                } else if (mParams.leftToLeft != mParams.UNSET) {
                    // right connected to view. left connected to parent
                    rightProp.connect(LEFT, mParams.leftToLeft, LEFT, 0);
                }
            }
            removeConstraints(LEFT);
            removeConstraints(RIGHT);
        } else {

            int startId = mParams.startToEnd;
            int endId = mParams.endToStart;
            if (startId != mParams.UNSET || endId != mParams.UNSET) {
                View startView = ((ViewGroup) mView.getParent()).findViewById(startId);
                ConstraintProperties startProp = new ConstraintProperties(startView);
                View endView = ((ViewGroup) mView.getParent()).findViewById(endId);
                ConstraintProperties endProp = new ConstraintProperties(endView);

                if (startId != mParams.UNSET && endId != mParams.UNSET) {
                    // start and end connected to views
                    startProp.connect(END, endId, START, 0);
                    endProp.connect(START, leftId, END, 0);
                } else if (leftId != mParams.UNSET || endId != mParams.UNSET) {
                    if (mParams.rightToRight != mParams.UNSET) {
                        // left connected to view. right connected to parent
                        startProp.connect(END, mParams.rightToRight, END, 0);
                    } else if (mParams.leftToLeft != mParams.UNSET) {
                        // right connected to view. left connected to parent
                        endProp.connect(START, mParams.leftToLeft, START, 0);
                    }
                }
            }
            removeConstraints(START);
            removeConstraints(END);
        }
        return this;
    }

    /**
     * Create a constraint between two widgets.
     *
     * @param startSide the side of the widget to constrain
     * @param endID     the id of the widget to constrain to
     * @param endSide   the side of widget to constrain to
     * @param margin    the margin to constrain (margin must be positive)
     */
    public ConstraintProperties connect(int startSide, int endID, int endSide, int margin) {

        switch (startSide) {
            case LEFT:
                if (endSide == LEFT) {
                    mParams.leftToLeft = endID;
                    mParams.leftToRight = mParams.UNSET;
                } else if (endSide == RIGHT) {
                    mParams.leftToRight = endID;
                    mParams.leftToLeft = mParams.UNSET;

                } else {
                    throw new IllegalArgumentException("Left to "
                            + sideToString(endSide) + " undefined");
                }
                mParams.leftMargin = margin;
                break;
            case RIGHT:
                if (endSide == LEFT) {
                    mParams.rightToLeft = endID;
                    mParams.rightToRight = mParams.UNSET;

                } else if (endSide == RIGHT) {
                    mParams.rightToRight = endID;
                    mParams.rightToLeft = mParams.UNSET;

                } else {
                    throw new IllegalArgumentException("right to "
                            + sideToString(endSide) + " undefined");
                }
                mParams.rightMargin = margin;
                break;
            case TOP:
                if (endSide == TOP) {
                    mParams.topToTop = endID;
                    mParams.topToBottom = mParams.UNSET;
                    mParams.baselineToBaseline = mParams.UNSET;
                    mParams.baselineToTop = mParams.UNSET;
                    mParams.baselineToBottom = mParams.UNSET;
                } else if (endSide == BOTTOM) {
                    mParams.topToBottom = endID;
                    mParams.topToTop = mParams.UNSET;
                    mParams.baselineToBaseline = mParams.UNSET;
                    mParams.baselineToTop = mParams.UNSET;
                    mParams.baselineToBottom = mParams.UNSET;
                } else {
                    throw new IllegalArgumentException("right to "
                            + sideToString(endSide) + " undefined");
                }
                mParams.topMargin = margin;
                break;
            case BOTTOM:
                if (endSide == BOTTOM) {
                    mParams.bottomToBottom = endID;
                    mParams.bottomToTop = mParams.UNSET;
                    mParams.baselineToBaseline = mParams.UNSET;
                    mParams.baselineToTop = mParams.UNSET;
                    mParams.baselineToBottom = mParams.UNSET;
                } else if (endSide == TOP) {
                    mParams.bottomToTop = endID;
                    mParams.bottomToBottom = mParams.UNSET;
                    mParams.baselineToBaseline = mParams.UNSET;
                    mParams.baselineToTop = mParams.UNSET;
                    mParams.baselineToBottom = mParams.UNSET;
                } else {
                    throw new IllegalArgumentException("right to "
                            + sideToString(endSide) + " undefined");
                }
                mParams.bottomMargin = margin;
                break;
            case BASELINE:
                if (endSide == BASELINE) {
                    mParams.baselineToBaseline = endID;
                    mParams.bottomToBottom = mParams.UNSET;
                    mParams.bottomToTop = mParams.UNSET;
                    mParams.topToTop = mParams.UNSET;
                    mParams.topToBottom = mParams.UNSET;
                } else if (endSide == TOP) {
                    mParams.baselineToTop = endID;
                    mParams.bottomToBottom = mParams.UNSET;
                    mParams.bottomToTop = mParams.UNSET;
                    mParams.topToTop = mParams.UNSET;
                    mParams.topToBottom = mParams.UNSET;
                } else if (endSide == BOTTOM) {
                    mParams.baselineToBottom = endID;
                    mParams.bottomToBottom = mParams.UNSET;
                    mParams.bottomToTop = mParams.UNSET;
                    mParams.topToTop = mParams.UNSET;
                    mParams.topToBottom = mParams.UNSET;
                } else {
                    throw new IllegalArgumentException("right to "
                            + sideToString(endSide) + " undefined");
                }
                mParams.baselineMargin = margin;
                break;
            case START:
                if (endSide == START) {
                    mParams.startToStart = endID;
                    mParams.startToEnd = mParams.UNSET;
                } else if (endSide == END) {
                    mParams.startToEnd = endID;
                    mParams.startToStart = mParams.UNSET;
                } else {
                    throw new IllegalArgumentException("right to "
                            + sideToString(endSide) + " undefined");
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                    mParams.setMarginStart(margin);
                }
                break;
            case END:
                if (endSide == END) {
                    mParams.endToEnd = endID;
                    mParams.endToStart = mParams.UNSET;
                } else if (endSide == START) {
                    mParams.endToStart = endID;
                    mParams.endToEnd = mParams.UNSET;
                } else {
                    throw new IllegalArgumentException("right to "
                            + sideToString(endSide) + " undefined");
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {

                    mParams.setMarginEnd(margin);
                }

                break;
            default:
                throw new IllegalArgumentException(
                        sideToString(startSide) + " to " + sideToString(endSide) + " unknown");
        }
        return this;
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

    public ConstraintProperties(View view) {
        ViewGroup.LayoutParams params = view.getLayoutParams();
        if (params instanceof ConstraintLayout.LayoutParams) {
            mParams = (ConstraintLayout.LayoutParams) params;
            mView = view;
        } else {
            throw new RuntimeException("Only children of ConstraintLayout.LayoutParams supported");
        }
    }

    /**
     * Should be called to apply the changes currently a no op
     * in place for subclasses and future use
     */
    public void apply() {
    }
}
