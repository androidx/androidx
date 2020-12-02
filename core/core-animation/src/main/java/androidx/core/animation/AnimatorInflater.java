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
package androidx.core.animation;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.Resources.NotFoundException;
import android.content.res.Resources.Theme;
import android.content.res.TypedArray;
import android.content.res.XmlResourceParser;
import android.graphics.Path;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.util.Xml;
import android.view.InflateException;

import androidx.annotation.AnimatorRes;
import androidx.annotation.InterpolatorRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.graphics.PathParser;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.ArrayList;

/**
 * This class is used to instantiate animator XML files into Animator objects.
 * <p>
 * For performance reasons, inflation relies heavily on pre-processing of
 * XML files that is done at build time. Therefore, it is not currently possible
 * to use this inflater with an XmlPullParser over a plain XML file at runtime;
 * it only works with an XmlPullParser returned from a compiled resource (R.
 * <em>something</em> file.)
 */
public class AnimatorInflater {
    private static final String TAG = "AnimatorInflater";
    /**
     * These flags are used when parsing AnimatorSet objects
     */
    private static final int TOGETHER = 0;
    @SuppressWarnings("unused") // kept around for parity with XML values.
    private static final int SEQUENTIALLY = 1;

    /**
     * Enum values used in XML attributes to indicate the value for mValueType
     */
    private static final int VALUE_TYPE_FLOAT       = 0;
    private static final int VALUE_TYPE_INT         = 1;
    private static final int VALUE_TYPE_PATH        = 2;
    private static final int VALUE_TYPE_COLOR       = 3;
    private static final int VALUE_TYPE_UNDEFINED   = 4;

    private AnimatorInflater() {}

    /**
     * Loads an {@link Animator} object from a resource
     *
     * @param context Application context used to access resources
     * @param id The resource id of the animation to load
     * @return The animator object reference by the specified id
     * @throws NotFoundException when the animation cannot be loaded
     */
    @NonNull
    public static Animator loadAnimator(@NonNull Context context, @AnimatorRes int id)
            throws NotFoundException {
        return loadAnimator(context.getResources(), context.getTheme(), id);
    }

    /**
     * Loads an {@link Animator} object from a resource
     *
     * @param resources The resources
     * @param theme The theme
     * @param id The resource id of the animation to load
     * @return The animator object reference by the specified id
     * @throws NotFoundException when the animation cannot be loaded
     */
    @NonNull
    public static Animator loadAnimator(@NonNull Resources resources, @Nullable Theme theme,
            @AnimatorRes int id) throws NotFoundException {
        return loadAnimator(resources, theme, id, 1);
    }

    static Animator loadAnimator(Resources resources, Theme theme, int id, float pathErrorScale)
            throws NotFoundException {

        Animator animator;
        XmlResourceParser parser = null;
        try {
            parser = resources.getAnimation(id);
            animator = createAnimatorFromXml(resources, theme, parser, pathErrorScale);
            return animator;
        } catch (XmlPullParserException ex) {
            Resources.NotFoundException rnf =
                    new Resources.NotFoundException("Can't load animation resource ID #0x"
                            + Integer.toHexString(id));
            rnf.initCause(ex);
            throw rnf;
        } catch (IOException ex) {
            Resources.NotFoundException rnf =
                    new Resources.NotFoundException("Can't load animation resource ID #0x"
                            + Integer.toHexString(id));
            rnf.initCause(ex);
            throw rnf;
        } finally {
            if (parser != null) parser.close();
        }
    }

    /**
     * PathDataEvaluator is used to interpolate between two paths which are
     * represented in the same format but different control points' values.
     * The path is represented as verbs and points for each of the verbs.
     *
     * An instance of this class cannot be reused for different paths as its
     * buffer array is structured to match the first path pattern.
     */
    static class PathDataEvaluator implements TypeEvaluator<PathParser.PathDataNode[]> {
        private PathParser.PathDataNode[] mPathData;

        @NonNull
        @Override
        public PathParser.PathDataNode[] evaluate(
                float fraction, @NonNull PathParser.PathDataNode[] startPathData,
                @NonNull PathParser.PathDataNode[] endPathData) {
            if (mPathData == null) {
                // This path buffer has to have the same size and structure as the morphing path.
                mPathData = PathParser.deepCopyNodes(endPathData);
            }
            if (!PathParser.interpolatePathDataNodes(
                    mPathData, startPathData, endPathData, fraction)) {
                throw new IllegalArgumentException("Can't interpolate between"
                        + " two incompatible pathData");
            }
            return mPathData;
        }
    }

    private static PropertyValuesHolder getPVH(TypedArray styledAttributes, int valueType,
            int valueFromId, int valueToId, String propertyName) {

        TypedValue tvFrom = styledAttributes.peekValue(valueFromId);
        boolean hasFrom = (tvFrom != null);
        int fromType = hasFrom ? tvFrom.type : 0;
        TypedValue tvTo = styledAttributes.peekValue(valueToId);
        boolean hasTo = (tvTo != null);
        int toType = hasTo ? tvTo.type : 0;

        if (valueType == VALUE_TYPE_UNDEFINED) {
            // Check whether it's color type. If not, fall back to default type (i.e. float type)
            if ((hasFrom && isColorType(fromType)) || (hasTo && isColorType(toType))) {
                valueType = VALUE_TYPE_COLOR;
            } else {
                valueType = VALUE_TYPE_FLOAT;
            }
        }

        boolean getFloats = (valueType == VALUE_TYPE_FLOAT);

        PropertyValuesHolder returnValue = null;

        if (valueType == VALUE_TYPE_PATH) {
            String fromString = styledAttributes.getString(valueFromId);
            String toString = styledAttributes.getString(valueToId);
            PathParser.PathDataNode[] nodesFrom = fromString == null
                    ? null : PathParser.createNodesFromPathData(fromString);
            PathParser.PathDataNode[] nodesTo = toString == null
                    ? null : PathParser.createNodesFromPathData(toString);

            if (nodesFrom != null || nodesTo != null) {
                if (nodesFrom != null) {
                    TypeEvaluator evaluator = new PathDataEvaluator();
                    if (nodesTo != null) {
                        if (!PathParser.canMorph(nodesFrom, nodesTo)) {
                            throw new InflateException(" Can't morph from " + fromString + " to "
                                    + toString);
                        }
                        returnValue = PropertyValuesHolder.ofObject(propertyName, evaluator,
                                nodesFrom, nodesTo);
                    } else {
                        returnValue = PropertyValuesHolder.ofObject(propertyName, evaluator,
                                (Object) nodesFrom);
                    }
                } else if (nodesTo != null) {
                    TypeEvaluator evaluator = new PathDataEvaluator();
                    returnValue = PropertyValuesHolder.ofObject(propertyName, evaluator,
                            (Object) nodesTo);
                }
            }
        } else {
            TypeEvaluator evaluator = null;
            // Integer and float value types are handled here.
            if (valueType == VALUE_TYPE_COLOR) {
                // special case for colors: ignore valueType and get ints
                evaluator = ArgbEvaluator.getInstance();
            }
            if (getFloats) {
                float valueFrom;
                float valueTo;
                if (hasFrom) {
                    if (fromType == TypedValue.TYPE_DIMENSION) {
                        valueFrom = styledAttributes.getDimension(valueFromId, 0f);
                    } else {
                        valueFrom = styledAttributes.getFloat(valueFromId, 0f);
                    }
                    if (hasTo) {
                        if (toType == TypedValue.TYPE_DIMENSION) {
                            valueTo = styledAttributes.getDimension(valueToId, 0f);
                        } else {
                            valueTo = styledAttributes.getFloat(valueToId, 0f);
                        }
                        returnValue = PropertyValuesHolder.ofFloat(propertyName,
                                valueFrom, valueTo);
                    } else {
                        returnValue = PropertyValuesHolder.ofFloat(propertyName, valueFrom);
                    }
                } else {
                    if (toType == TypedValue.TYPE_DIMENSION) {
                        valueTo = styledAttributes.getDimension(valueToId, 0f);
                    } else {
                        valueTo = styledAttributes.getFloat(valueToId, 0f);
                    }
                    returnValue = PropertyValuesHolder.ofFloat(propertyName, valueTo);
                }
            } else {
                int valueFrom;
                int valueTo;
                if (hasFrom) {
                    if (fromType == TypedValue.TYPE_DIMENSION) {
                        valueFrom = (int) styledAttributes.getDimension(valueFromId, 0f);
                    } else if (isColorType(fromType)) {
                        valueFrom = styledAttributes.getColor(valueFromId, 0);
                    } else {
                        valueFrom = styledAttributes.getInt(valueFromId, 0);
                    }
                    if (hasTo) {
                        if (toType == TypedValue.TYPE_DIMENSION) {
                            valueTo = (int) styledAttributes.getDimension(valueToId, 0f);
                        } else if (isColorType(toType)) {
                            valueTo = styledAttributes.getColor(valueToId, 0);
                        } else {
                            valueTo = styledAttributes.getInt(valueToId, 0);
                        }
                        returnValue = PropertyValuesHolder.ofInt(propertyName, valueFrom, valueTo);
                    } else {
                        returnValue = PropertyValuesHolder.ofInt(propertyName, valueFrom);
                    }
                } else {
                    if (hasTo) {
                        if (toType == TypedValue.TYPE_DIMENSION) {
                            valueTo = (int) styledAttributes.getDimension(valueToId, 0f);
                        } else if (isColorType(toType)) {
                            valueTo = styledAttributes.getColor(valueToId, 0);
                        } else {
                            valueTo = styledAttributes.getInt(valueToId, 0);
                        }
                        returnValue = PropertyValuesHolder.ofInt(propertyName, valueTo);
                    }
                }
            }
            if (returnValue != null && evaluator != null) {
                returnValue.setEvaluator(evaluator);
            }
        }

        return returnValue;
    }

    /**
     * @param anim The animator, must not be null
     * @param arrayAnimator Incoming typed array for Animator's attributes.
     * @param arrayObjectAnimator Incoming typed array for Object Animator's
     *            attributes.
     * @param pixelSize The relative pixel size, used to calculate the
     *                  maximum error for path animations.
     */
    private static void parseAnimatorFromTypeArray(ValueAnimator anim,
            TypedArray arrayAnimator, TypedArray arrayObjectAnimator, float pixelSize) {
        long duration = arrayAnimator.getInt(AndroidResources.STYLEABLE_ANIMATOR_DURATION, 300);

        long startDelay = arrayAnimator.getInt(AndroidResources.STYLEABLE_ANIMATOR_START_OFFSET, 0);

        int valueType = arrayAnimator.getInt(AndroidResources.STYLEABLE_ANIMATOR_VALUE_TYPE,
                VALUE_TYPE_UNDEFINED);

        if (valueType == VALUE_TYPE_UNDEFINED) {
            valueType = inferValueTypeFromValues(arrayAnimator,
                    AndroidResources.STYLEABLE_ANIMATOR_VALUE_FROM,
                    AndroidResources.STYLEABLE_ANIMATOR_VALUE_TO);
        }
        PropertyValuesHolder pvh = getPVH(arrayAnimator, valueType,
                AndroidResources.STYLEABLE_ANIMATOR_VALUE_FROM,
                AndroidResources.STYLEABLE_ANIMATOR_VALUE_TO, "");
        if (pvh != null) {
            anim.setValues(pvh);
        }

        anim.setDuration(duration);
        anim.setStartDelay(startDelay);

        if (arrayAnimator.hasValue(AndroidResources.STYLEABLE_ANIMATOR_REPEAT_COUNT)) {
            anim.setRepeatCount(
                    arrayAnimator.getInt(AndroidResources.STYLEABLE_ANIMATOR_REPEAT_COUNT, 0));
        }
        if (arrayAnimator.hasValue(AndroidResources.STYLEABLE_ANIMATOR_REPEAT_MODE)) {
            anim.setRepeatMode(
                    arrayAnimator.getInt(AndroidResources.STYLEABLE_ANIMATOR_REPEAT_MODE,
                            ValueAnimator.RESTART));
        }

        if (arrayObjectAnimator != null) {
            setupObjectAnimator(anim, arrayObjectAnimator, valueType, pixelSize);
        }
    }

    /**
     * Setup ObjectAnimator's property or values from pathData.
     *
     * @param anim The target Animator which will be updated.
     * @param arrayObjectAnimator TypedArray for the ObjectAnimator.
     * @param valueType the type of value that could be any of VALUE_TYPE_INT, VALUE_TYPE_FLOAT,
     *                 VALUE_TYPE_COLOR, VALUE_TYPE_PATH, VALUE_TYPE_UNDEFINED
     * @param pixelSize The relative pixel size, used to calculate the
     *                  maximum error for path animations.
     */
    private static void setupObjectAnimator(ValueAnimator anim, TypedArray arrayObjectAnimator,
            int valueType, float pixelSize) {
        ObjectAnimator oa = (ObjectAnimator) anim;
        String pathData = null;

        // This works around an issue in API 19 where TypedArray.getString(int) returns a reference
        // wrapped in a string when the attribute at given index isn't defined.
        TypedValue typedValue = new TypedValue();
        arrayObjectAnimator.getValue(
                AndroidResources.STYLEABLE_PROPERTY_ANIMATOR_PATH_DATA, typedValue);
        if (typedValue.type == TypedValue.TYPE_STRING) {
            pathData = typedValue.string.toString();
        }

        // Path can be involved in an ObjectAnimator in the following 3 ways:
        // 1) Path morphing: the property to be animated is pathData, and valueFrom and valueTo
        //    are both of pathType. valueType = pathType needs to be explicitly defined.
        // 2) A property in X or Y dimension can be animated along a path: the property needs to be
        //    defined in propertyXName or propertyYName attribute, the path will be defined in the
        //    pathData attribute. valueFrom and valueTo will not be necessary for this animation.
        // 3) PathInterpolator can also define a path (in pathData) for its interpolation curve.
        // Here we are dealing with case 2:
        if (pathData != null) {
            String propertyXName = arrayObjectAnimator.getString(
                    AndroidResources.STYLEABLE_PROPERTY_ANIMATOR_PROPERTY_X_NAME);
            String propertyYName = arrayObjectAnimator.getString(
                    AndroidResources.STYLEABLE_PROPERTY_ANIMATOR_PROPERTY_Y_NAME);

            if (valueType == VALUE_TYPE_PATH || valueType == VALUE_TYPE_UNDEFINED) {
                // When pathData is defined, we are in case #2 mentioned above. ValueType can only
                // be float type, or int type. Otherwise we fallback to default type.
                valueType = VALUE_TYPE_FLOAT;
            }
            if (propertyXName == null && propertyYName == null) {
                throw new InflateException(arrayObjectAnimator.getPositionDescription()
                        + " propertyXName or propertyYName is needed for PathData");
            } else {
                Path path = PathParser.createPathFromPathData(pathData);
                float error = 0.5f * pixelSize; // max half a pixel error
                PathKeyframes keyframeSet = KeyframeSet.ofPath(path, error);
                Keyframes xKeyframes;
                Keyframes yKeyframes;
                if (valueType == VALUE_TYPE_FLOAT) {
                    xKeyframes = keyframeSet.createXFloatKeyframes();
                    yKeyframes = keyframeSet.createYFloatKeyframes();
                } else {
                    xKeyframes = keyframeSet.createXIntKeyframes();
                    yKeyframes = keyframeSet.createYIntKeyframes();
                }
                PropertyValuesHolder x = null;
                PropertyValuesHolder y = null;
                if (propertyXName != null) {
                    x = PropertyValuesHolder.ofKeyframes(propertyXName, xKeyframes);
                }
                if (propertyYName != null) {
                    y = PropertyValuesHolder.ofKeyframes(propertyYName, yKeyframes);
                }
                if (x == null) {
                    oa.setValues(y);
                } else if (y == null) {
                    oa.setValues(x);
                } else {
                    oa.setValues(x, y);
                }
            }
        } else {
            String propertyName = arrayObjectAnimator.getString(
                    AndroidResources.STYLEABLE_PROPERTY_ANIMATOR_PROPERTY_NAME);
            oa.setPropertyName(propertyName);
        }
    }

    private static Animator createAnimatorFromXml(Resources res, Theme theme, XmlPullParser parser,
            float pixelSize)
            throws XmlPullParserException, IOException {
        return createAnimatorFromXml(res, theme, parser, Xml.asAttributeSet(parser), null, 0,
                pixelSize);
    }

    private static Animator createAnimatorFromXml(Resources res, Theme theme, XmlPullParser parser,
            AttributeSet attrs, AnimatorSet parent, int sequenceOrdering, float pixelSize)
            throws XmlPullParserException, IOException {
        Animator anim = null;
        ArrayList<Animator> childAnims = null;

        // Make sure we are on a start tag.
        int type;
        int depth = parser.getDepth();

        while (((type = parser.next()) != XmlPullParser.END_TAG || parser.getDepth() > depth)
                && type != XmlPullParser.END_DOCUMENT) {

            if (type != XmlPullParser.START_TAG) {
                continue;
            }

            String name = parser.getName();
            boolean gotValues = false;

            if (name.equals("objectAnimator")) {
                anim = loadObjectAnimator(res, theme, attrs, pixelSize);
            } else if (name.equals("animator")) {
                anim = loadAnimator(res, theme, attrs, null, pixelSize);
            } else if (name.equals("set")) {
                anim = new AnimatorSet();
                TypedArray a;
                if (theme != null) {
                    a = theme.obtainStyledAttributes(attrs, AndroidResources.STYLEABLE_ANIMATOR_SET,
                            0, 0);
                } else {
                    a = res.obtainAttributes(attrs, AndroidResources.STYLEABLE_ANIMATOR_SET);
                }
                int ordering = a.getInt(AndroidResources.STYLEABLE_ANIMATOR_SET_ORDERING, TOGETHER);
                createAnimatorFromXml(res, theme, parser, attrs, (AnimatorSet) anim, ordering,
                        pixelSize);
                a.recycle();
            } else if (name.equals("propertyValuesHolder")) {
                PropertyValuesHolder[] values = loadValues(res, theme, parser,
                        Xml.asAttributeSet(parser));
                if (values != null && anim != null && (anim instanceof ValueAnimator)) {
                    ((ValueAnimator) anim).setValues(values);
                }
                gotValues = true;
            } else {
                throw new RuntimeException("Unknown animator name: " + parser.getName());
            }

            if (parent != null && !gotValues) {
                if (childAnims == null) {
                    childAnims = new ArrayList<Animator>();
                }
                childAnims.add(anim);
            }
        }
        if (parent != null && childAnims != null) {
            Animator[] animsArray = new Animator[childAnims.size()];
            int index = 0;
            for (Animator a : childAnims) {
                animsArray[index++] = a;
            }
            if (sequenceOrdering == TOGETHER) {
                parent.playTogether(animsArray);
            } else {
                parent.playSequentially(animsArray);
            }
        }
        return anim;
    }

    private static PropertyValuesHolder[] loadValues(Resources res, Theme theme,
            XmlPullParser parser, AttributeSet attrs) throws XmlPullParserException, IOException {
        ArrayList<PropertyValuesHolder> values = null;

        int type;
        while ((type = parser.getEventType()) != XmlPullParser.END_TAG
                && type != XmlPullParser.END_DOCUMENT) {

            if (type != XmlPullParser.START_TAG) {
                parser.next();
                continue;
            }

            String name = parser.getName();

            if (name.equals("propertyValuesHolder")) {
                TypedArray a;
                if (theme != null) {
                    a = theme.obtainStyledAttributes(attrs,
                            AndroidResources.STYLEABLE_PROPERTY_VALUES_HOLDER, 0, 0);
                } else {
                    a = res.obtainAttributes(attrs,
                            AndroidResources.STYLEABLE_PROPERTY_VALUES_HOLDER);
                }
                String propertyName = a.getString(
                        AndroidResources.STYLEABLE_PROPERTY_VALUES_HOLDER_PROPERTY_NAME);
                int valueType = a.getInt(
                        AndroidResources.STYLEABLE_PROPERTY_VALUES_HOLDER_VALUE_TYPE,
                        VALUE_TYPE_UNDEFINED);

                PropertyValuesHolder pvh = loadPvh(res, theme, parser, propertyName, valueType);
                if (pvh == null) {
                    pvh = getPVH(a, valueType,
                            AndroidResources.STYLEABLE_PROPERTY_VALUES_HOLDER_VALUE_FROM,
                            AndroidResources.STYLEABLE_PROPERTY_VALUES_HOLDER_VALUE_TO,
                            propertyName);
                }
                if (pvh != null) {
                    if (values == null) {
                        values = new ArrayList<PropertyValuesHolder>();
                    }
                    values.add(pvh);
                }
                a.recycle();
            }

            parser.next();
        }

        PropertyValuesHolder[] valuesArray = null;
        if (values != null) {
            int count = values.size();
            valuesArray = new PropertyValuesHolder[count];
            for (int i = 0; i < count; ++i) {
                valuesArray[i] = values.get(i);
            }
        }
        return valuesArray;
    }

    // When no value type is provided in keyframe, we need to infer the type from the value. i.e.
    // if value is defined in the style of a color value, then the color type is returned.
    // Otherwise, default float type is returned.
    private static int inferValueTypeOfKeyframe(Resources res, Theme theme, AttributeSet attrs) {
        int valueType;
        TypedArray a;
        if (theme != null) {
            a = theme.obtainStyledAttributes(attrs, AndroidResources.STYLEABLE_KEYFRAME, 0, 0);
        } else {
            a = res.obtainAttributes(attrs, AndroidResources.STYLEABLE_KEYFRAME);
        }

        TypedValue keyframeValue = a.peekValue(AndroidResources.STYLEABLE_KEYFRAME_VALUE);
        boolean hasValue = (keyframeValue != null);
        // When no value type is provided, check whether it's a color type first.
        // If not, fall back to default value type (i.e. float type).
        if (hasValue && isColorType(keyframeValue.type)) {
            valueType = VALUE_TYPE_COLOR;
        } else {
            valueType = VALUE_TYPE_FLOAT;
        }
        a.recycle();
        return valueType;
    }

    private static int inferValueTypeFromValues(TypedArray styledAttributes, int valueFromId,
            int valueToId) {
        TypedValue tvFrom = styledAttributes.peekValue(valueFromId);
        boolean hasFrom = (tvFrom != null);
        int fromType = hasFrom ? tvFrom.type : 0;
        TypedValue tvTo = styledAttributes.peekValue(valueToId);
        boolean hasTo = (tvTo != null);
        int toType = hasTo ? tvTo.type : 0;

        int valueType;
        // Check whether it's color type. If not, fall back to default type (i.e. float type)
        if ((hasFrom && isColorType(fromType)) || (hasTo && isColorType(toType))) {
            valueType = VALUE_TYPE_COLOR;
        } else {
            valueType = VALUE_TYPE_FLOAT;
        }
        return valueType;
    }

    // Load property values holder if there are keyframes defined in it. Otherwise return null.
    private static PropertyValuesHolder loadPvh(Resources res, Theme theme, XmlPullParser parser,
            String propertyName, int valueType)
            throws XmlPullParserException, IOException {

        PropertyValuesHolder value = null;
        ArrayList<Keyframe> keyframes = null;

        int type;
        while ((type = parser.next()) != XmlPullParser.END_TAG
                && type != XmlPullParser.END_DOCUMENT) {
            String name = parser.getName();
            if (name.equals("keyframe")) {
                if (valueType == VALUE_TYPE_UNDEFINED) {
                    valueType = inferValueTypeOfKeyframe(res, theme, Xml.asAttributeSet(parser));
                }
                Keyframe keyframe = loadKeyframe(res, theme, Xml.asAttributeSet(parser), valueType);
                if (keyframe != null) {
                    if (keyframes == null) {
                        keyframes = new ArrayList<Keyframe>();
                    }
                    keyframes.add(keyframe);
                }
                parser.next();
            }
        }

        int count;
        if (keyframes != null && (count = keyframes.size()) > 0) {
            // make sure we have keyframes at 0 and 1
            // If we have keyframes with set fractions, add keyframes at start/end
            // appropriately. If start/end have no set fractions:
            // if there's only one keyframe, set its fraction to 1 and add one at 0
            // if >1 keyframe, set the last fraction to 1, the first fraction to 0
            Keyframe firstKeyframe = keyframes.get(0);
            Keyframe lastKeyframe = keyframes.get(count - 1);
            float endFraction = lastKeyframe.getFraction();
            if (endFraction < 1) {
                if (endFraction < 0) {
                    lastKeyframe.setFraction(1);
                } else {
                    keyframes.add(keyframes.size(), createNewKeyframe(lastKeyframe, 1));
                    ++count;
                }
            }
            float startFraction = firstKeyframe.getFraction();
            if (startFraction != 0) {
                if (startFraction < 0) {
                    firstKeyframe.setFraction(0);
                } else {
                    keyframes.add(0, createNewKeyframe(firstKeyframe, 0));
                    ++count;
                }
            }
            Keyframe[] keyframeArray = new Keyframe[count];
            keyframes.toArray(keyframeArray);
            for (int i = 0; i < count; ++i) {
                Keyframe keyframe = keyframeArray[i];
                if (keyframe.getFraction() < 0) {
                    if (i == 0) {
                        keyframe.setFraction(0);
                    } else if (i == count - 1) {
                        keyframe.setFraction(1);
                    } else {
                        // figure out the start/end parameters of the current gap
                        // in fractions and distribute the gap among those keyframes
                        int startIndex = i;
                        int endIndex = i;
                        for (int j = startIndex + 1; j < count - 1; ++j) {
                            if (keyframeArray[j].getFraction() >= 0) {
                                break;
                            }
                            endIndex = j;
                        }
                        float gap = keyframeArray[endIndex + 1].getFraction()
                                - keyframeArray[startIndex - 1].getFraction();
                        distributeKeyframes(keyframeArray, gap, startIndex, endIndex);
                    }
                }
            }
            value = PropertyValuesHolder.ofKeyframe(propertyName, keyframeArray);
            if (valueType == VALUE_TYPE_COLOR) {
                value.setEvaluator(ArgbEvaluator.getInstance());
            }
        }

        return value;
    }

    private static Keyframe createNewKeyframe(Keyframe sampleKeyframe, float fraction) {
        Class<?> type = sampleKeyframe.getType();
        if (type == float.class) {
            return Keyframe.ofFloat(fraction);
        } else if (type == int.class) {
            return Keyframe.ofInt(fraction);
        } else {
            return Keyframe.ofObject(fraction);
        }
    }

    /**
     * Utility function to set fractions on keyframes to cover a gap in which the
     * fractions are not currently set. Keyframe fractions will be distributed evenly
     * in this gap. For example, a gap of 1 keyframe in the range 0-1 will be at .5, a gap
     * of .6 spread between two keyframes will be at .2 and .4 beyond the fraction at the
     * keyframe before startIndex.
     * Assumptions:
     * - First and last keyframe fractions (bounding this spread) are already set. So,
     * for example, if no fractions are set, we will already set first and last keyframe
     * fraction values to 0 and 1.
     * - startIndex must be >0 (which follows from first assumption).
     * - endIndex must be >= startIndex.
     *
     * @param keyframes the array of keyframes
     * @param gap The total gap we need to distribute
     * @param startIndex The index of the first keyframe whose fraction must be set
     * @param endIndex The index of the last keyframe whose fraction must be set
     */
    private static void distributeKeyframes(Keyframe[] keyframes, float gap,
            int startIndex, int endIndex) {
        int count = endIndex - startIndex + 2;
        float increment = gap / count;
        for (int i = startIndex; i <= endIndex; ++i) {
            keyframes[i].setFraction(keyframes[i - 1].getFraction() + increment);
        }
    }

    private static Keyframe loadKeyframe(Resources res, Theme theme, AttributeSet attrs,
            int valueType)
            throws XmlPullParserException, IOException {

        TypedArray a;
        if (theme != null) {
            a = theme.obtainStyledAttributes(attrs, AndroidResources.STYLEABLE_KEYFRAME, 0, 0);
        } else {
            a = res.obtainAttributes(attrs, AndroidResources.STYLEABLE_KEYFRAME);
        }

        Keyframe keyframe = null;

        float fraction = a.getFloat(AndroidResources.STYLEABLE_KEYFRAME_FRACTION, -1);

        TypedValue keyframeValue = a.peekValue(AndroidResources.STYLEABLE_KEYFRAME_VALUE);
        boolean hasValue = (keyframeValue != null);
        if (valueType == VALUE_TYPE_UNDEFINED) {
            // When no value type is provided, check whether it's a color type first.
            // If not, fall back to default value type (i.e. float type).
            if (hasValue && isColorType(keyframeValue.type)) {
                valueType = VALUE_TYPE_COLOR;
            } else {
                valueType = VALUE_TYPE_FLOAT;
            }
        }

        if (hasValue) {
            switch (valueType) {
                case VALUE_TYPE_FLOAT:
                    float value = a.getFloat(AndroidResources.STYLEABLE_KEYFRAME_VALUE, 0);
                    keyframe = Keyframe.ofFloat(fraction, value);
                    break;
                case VALUE_TYPE_COLOR:
                case VALUE_TYPE_INT:
                    int intValue = a.getInt(AndroidResources.STYLEABLE_KEYFRAME_VALUE, 0);
                    keyframe = Keyframe.ofInt(fraction, intValue);
                    break;
            }
        } else {
            keyframe = (valueType == VALUE_TYPE_FLOAT) ? Keyframe.ofFloat(fraction) :
                    Keyframe.ofInt(fraction);
        }

        final int resID = a.getResourceId(AndroidResources.STYLEABLE_KEYFRAME_INTERPOLATOR, 0);
        if (resID > 0) {
            final Interpolator interpolator = loadInterpolator(res, theme, resID);
            keyframe.setInterpolator(interpolator);
        }
        a.recycle();

        return keyframe;
    }

    private static ObjectAnimator loadObjectAnimator(Resources res, Theme theme, AttributeSet attrs,
            float pathErrorScale) throws NotFoundException {
        ObjectAnimator anim = new ObjectAnimator();

        loadAnimator(res, theme, attrs, anim, pathErrorScale);

        return anim;
    }

    /**
     * Creates a new animation whose parameters come from the specified context
     * and attributes set.
     *
     * @param res The resources
     * @param theme The theme that is being used to inflate the animator
     * @param attrs The set of attributes holding the animation parameters
     * @param anim Null if this is a ValueAnimator, otherwise this is an
     *            ObjectAnimator
     * @param pathErrorScale Acceptable error in the unit of pixels
     * @return a ValueAnimator that was inflated from the given resources
     */
    private static ValueAnimator loadAnimator(Resources res, Theme theme, AttributeSet attrs,
            ValueAnimator anim, float pathErrorScale) throws NotFoundException {
        TypedArray arrayAnimator = null;
        TypedArray arrayObjectAnimator = null;

        if (theme != null) {
            arrayAnimator = theme.obtainStyledAttributes(attrs, AndroidResources.STYLEABLE_ANIMATOR,
                    0, 0);
        } else {
            arrayAnimator = res.obtainAttributes(attrs, AndroidResources.STYLEABLE_ANIMATOR);
        }

        // If anim is not null, then it is an object animator.
        if (anim != null) {
            if (theme != null) {
                arrayObjectAnimator = theme.obtainStyledAttributes(attrs,
                        AndroidResources.STYLEABLE_PROPERTY_ANIMATOR, 0, 0);
            } else {
                arrayObjectAnimator = res.obtainAttributes(attrs,
                        AndroidResources.STYLEABLE_PROPERTY_ANIMATOR);
            }
        }

        if (anim == null) {
            anim = new ValueAnimator();
        }

        parseAnimatorFromTypeArray(anim, arrayAnimator, arrayObjectAnimator, pathErrorScale);

        final int resID = arrayAnimator.getResourceId(
                AndroidResources.STYLEABLE_ANIMATOR_INTERPOLATOR, 0);
        if (resID > 0) {
            final Interpolator interpolator = loadInterpolator(res, theme, resID);
            anim.setInterpolator(interpolator);
        }

        arrayAnimator.recycle();
        if (arrayObjectAnimator != null) {
            arrayObjectAnimator.recycle();
        }
        return anim;
    }

    private static boolean isColorType(int type) {
        return type >= TypedValue.TYPE_FIRST_COLOR_INT && type <= TypedValue.TYPE_LAST_COLOR_INT;
    }

    /**
     * Loads an {@link Interpolator} object from a resource
     *
     * @param context Application context used to access resources
     * @param id The resource id of the animation to load
     * @return The animation object reference by the specified id
     * @throws NotFoundException when interpolator resources cannot be loaded
     */
    @NonNull
    public static Interpolator loadInterpolator(@NonNull Context context,
            @AnimatorRes @InterpolatorRes int id) throws NotFoundException {
        XmlResourceParser parser = null;
        try {
            parser = context.getResources().getAnimation(id);
            return createInterpolatorFromXml(context.getResources(), context.getTheme(), parser);
        } catch (XmlPullParserException ex) {
            NotFoundException rnf = new NotFoundException("Can't load animation resource ID #0x"
                    + Integer.toHexString(id));
            rnf.initCause(ex);
            throw rnf;
        } catch (IOException ex) {
            NotFoundException rnf = new NotFoundException("Can't load animation resource ID #0x"
                    + Integer.toHexString(id));
            rnf.initCause(ex);
            throw rnf;
        } finally {
            if (parser != null) {
                parser.close();
            }
        }

    }

    /**
     * Loads an {@link Interpolator} object from a resource
     *
     * @param res The resources
     * @param id The resource id of the animation to load
     * @return The interpolator object reference by the specified id
     * @throws NotFoundException when interpolator resources cannot be loaded
     */
    static Interpolator loadInterpolator(Resources res, Theme theme, int id)
            throws NotFoundException {
        // Special treatment for the interpolator introduced at API 21.
        if (id == AndroidResources.FAST_OUT_LINEAR_IN) {
            return new PathInterpolator(0.4f, 0f, 1f, 1f);
        } else if (id == AndroidResources.FAST_OUT_SLOW_IN) {
            return new PathInterpolator(0.4f, 0f, 0.2f, 1f);
        } else if (id == AndroidResources.LINEAR_OUT_SLOW_IN) {
            return new PathInterpolator(0f, 0f, 0.2f, 1f);
        }

        XmlResourceParser parser = null;
        try {
            parser = res.getAnimation(id);
            return createInterpolatorFromXml(res, theme, parser);
        } catch (XmlPullParserException ex) {
            NotFoundException rnf = new NotFoundException("Can't load animation resource ID #0x"
                    + Integer.toHexString(id));
            rnf.initCause(ex);
            throw rnf;
        } catch (IOException ex) {
            NotFoundException rnf = new NotFoundException("Can't load animation resource ID #0x"
                    + Integer.toHexString(id));
            rnf.initCause(ex);
            throw rnf;
        } finally {
            if (parser != null) {
                parser.close();
            }
        }

    }

    private static Interpolator createInterpolatorFromXml(Resources res, Theme theme,
            XmlPullParser parser) throws XmlPullParserException, IOException {

        Interpolator interpolator = null;

        // Make sure we are on a start tag.
        int type;
        int depth = parser.getDepth();

        while (((type = parser.next()) != XmlPullParser.END_TAG || parser.getDepth() > depth)
                && type != XmlPullParser.END_DOCUMENT) {

            if (type != XmlPullParser.START_TAG) {
                continue;
            }

            AttributeSet attrs = Xml.asAttributeSet(parser);

            String name = parser.getName();

            if (name.equals("linearInterpolator")) {
                interpolator = new LinearInterpolator();
            } else if (name.equals("accelerateInterpolator")) {
                interpolator = new AccelerateInterpolator(res, theme, attrs);
            } else if (name.equals("decelerateInterpolator")) {
                interpolator = new DecelerateInterpolator(res, theme, attrs);
            } else if (name.equals("accelerateDecelerateInterpolator")) {
                interpolator = new AccelerateDecelerateInterpolator();
            } else if (name.equals("cycleInterpolator")) {
                interpolator = new CycleInterpolator(res, theme, attrs);
            } else if (name.equals("anticipateInterpolator")) {
                interpolator = new AnticipateInterpolator(res, theme, attrs);
            } else if (name.equals("overshootInterpolator")) {
                interpolator = new OvershootInterpolator(res, theme, attrs);
            } else if (name.equals("anticipateOvershootInterpolator")) {
                interpolator = new AnticipateOvershootInterpolator(res, theme, attrs);
            } else if (name.equals("bounceInterpolator")) {
                interpolator = new BounceInterpolator();
            } else if (name.equals("pathInterpolator")) {
                interpolator = new PathInterpolator(res, theme, attrs, parser);
            } else {
                throw new RuntimeException("Unknown interpolator name: " + name);
            }
        }
        return interpolator;
    }
}
