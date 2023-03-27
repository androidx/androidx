/*
 * Copyright (C) 2021 The Android Open Source Project
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

package androidx.constraintlayout.core.state;

import androidx.annotation.NonNull;
import androidx.constraintlayout.core.motion.CustomAttribute;
import androidx.constraintlayout.core.motion.CustomVariable;
import androidx.constraintlayout.core.motion.utils.TypedBundle;
import androidx.constraintlayout.core.motion.utils.TypedValues;
import androidx.constraintlayout.core.parser.CLElement;
import androidx.constraintlayout.core.parser.CLKey;
import androidx.constraintlayout.core.parser.CLNumber;
import androidx.constraintlayout.core.parser.CLObject;
import androidx.constraintlayout.core.parser.CLParsingException;
import androidx.constraintlayout.core.widgets.ConstraintAnchor;
import androidx.constraintlayout.core.widgets.ConstraintWidget;

import java.util.HashMap;
import java.util.Set;

/**
 * Utility class to encapsulate layout of a widget
 */
public class WidgetFrame {
    public ConstraintWidget widget = null;
    public int left = 0;
    public int top = 0;
    public int right = 0;
    public int bottom = 0;

    // transforms

    public float pivotX = Float.NaN;
    public float pivotY = Float.NaN;

    public float rotationX = Float.NaN;
    public float rotationY = Float.NaN;
    public float rotationZ = Float.NaN;

    public float translationX = Float.NaN;
    public float translationY = Float.NaN;
    public float translationZ = Float.NaN;
    public static float phone_orientation = Float.NaN;

    public float scaleX = Float.NaN;
    public float scaleY = Float.NaN;

    public float alpha = Float.NaN;
    public float interpolatedPos = Float.NaN;

    public int visibility = ConstraintWidget.VISIBLE;

    private final HashMap<String, CustomVariable> mCustom = new HashMap<>();

    public String name = null;

    TypedBundle mMotionProperties;

    // @TODO: add description
    public int width() {
        return Math.max(0, right - left);
    }

    // @TODO: add description
    public int height() {
        return Math.max(0, bottom - top);
    }

    public WidgetFrame() {
    }

    public WidgetFrame(ConstraintWidget widget) {
        this.widget = widget;
    }

    public WidgetFrame(WidgetFrame frame) {
        widget = frame.widget;
        left = frame.left;
        top = frame.top;
        right = frame.right;
        bottom = frame.bottom;
        updateAttributes(frame);
    }

    // @TODO: add description
    public void updateAttributes(WidgetFrame frame) {
        if (frame == null) {
            return;
        }
        pivotX = frame.pivotX;
        pivotY = frame.pivotY;
        rotationX = frame.rotationX;
        rotationY = frame.rotationY;
        rotationZ = frame.rotationZ;
        translationX = frame.translationX;
        translationY = frame.translationY;
        translationZ = frame.translationZ;
        scaleX = frame.scaleX;
        scaleY = frame.scaleY;
        alpha = frame.alpha;
        visibility = frame.visibility;
        setMotionAttributes(frame.mMotionProperties);
        mCustom.clear();
        for (CustomVariable c : frame.mCustom.values()) {
            mCustom.put(c.getName(), c.copy());
        }
    }

    public boolean isDefaultTransform() {
        return Float.isNaN(rotationX)
                && Float.isNaN(rotationY)
                && Float.isNaN(rotationZ)
                && Float.isNaN(translationX)
                && Float.isNaN(translationY)
                && Float.isNaN(translationZ)
                && Float.isNaN(scaleX)
                && Float.isNaN(scaleY)
                && Float.isNaN(alpha);
    }

    // @TODO: add description
    public static void interpolate(int parentWidth,
            int parentHeight,
            WidgetFrame frame,
            WidgetFrame start,
            WidgetFrame end,
            Transition transition,
            float progress) {
        int frameNumber = (int) (progress * 100);
        int startX = start.left;
        int startY = start.top;
        int endX = end.left;
        int endY = end.top;
        int startWidth = start.right - start.left;
        int startHeight = start.bottom - start.top;
        int endWidth = end.right - end.left;
        int endHeight = end.bottom - end.top;

        float progressPosition = progress;

        float startAlpha = start.alpha;
        float endAlpha = end.alpha;

        if (start.visibility == ConstraintWidget.GONE) {
            // On visibility gone, keep the same size to do an alpha to zero
            startX -= (int) (endWidth / 2f);
            startY -= (int) (endHeight / 2f);
            startWidth = endWidth;
            startHeight = endHeight;
            if (Float.isNaN(startAlpha)) {
                // override only if not defined...
                startAlpha = 0f;
            }
        }

        if (end.visibility == ConstraintWidget.GONE) {
            // On visibility gone, keep the same size to do an alpha to zero
            endX -= (int) (startWidth / 2f);
            endY -= (int) (startHeight / 2f);
            endWidth = startWidth;
            endHeight = startHeight;
            if (Float.isNaN(endAlpha)) {
                // override only if not defined...
                endAlpha = 0f;
            }
        }

        if (Float.isNaN(startAlpha) && !Float.isNaN(endAlpha)) {
            startAlpha = 1f;
        }
        if (!Float.isNaN(startAlpha) && Float.isNaN(endAlpha)) {
            endAlpha = 1f;
        }

        if (start.visibility == ConstraintWidget.INVISIBLE) {
            startAlpha = 0f;
        }

        if (end.visibility == ConstraintWidget.INVISIBLE) {
            endAlpha = 0f;
        }

        if (frame.widget != null && transition.hasPositionKeyframes()) {
            Transition.KeyPosition firstPosition =
                    transition.findPreviousPosition(frame.widget.stringId, frameNumber);
            Transition.KeyPosition lastPosition =
                    transition.findNextPosition(frame.widget.stringId, frameNumber);

            if (firstPosition == lastPosition) {
                lastPosition = null;
            }
            int interpolateStartFrame = 0;
            int interpolateEndFrame = 100;

            if (firstPosition != null) {
                startX = (int) (firstPosition.mX * parentWidth);
                startY = (int) (firstPosition.mY * parentHeight);
                interpolateStartFrame = firstPosition.mFrame;
            }
            if (lastPosition != null) {
                endX = (int) (lastPosition.mX * parentWidth);
                endY = (int) (lastPosition.mY * parentHeight);
                interpolateEndFrame = lastPosition.mFrame;
            }

            progressPosition = (progress * 100f - interpolateStartFrame)
                    / (float) (interpolateEndFrame - interpolateStartFrame);
        }

        frame.widget = start.widget;

        frame.left = (int) (startX + progressPosition * (endX - startX));
        frame.top = (int) (startY + progressPosition * (endY - startY));
        int width = (int) ((1 - progress) * startWidth + (progress * endWidth));
        int height = (int) ((1 - progress) * startHeight + (progress * endHeight));
        frame.right = frame.left + width;
        frame.bottom = frame.top + height;

        frame.pivotX = interpolate(start.pivotX, end.pivotX, 0.5f, progress);
        frame.pivotY = interpolate(start.pivotY, end.pivotY, 0.5f, progress);

        frame.rotationX = interpolate(start.rotationX, end.rotationX, 0f, progress);
        frame.rotationY = interpolate(start.rotationY, end.rotationY, 0f, progress);
        frame.rotationZ = interpolate(start.rotationZ, end.rotationZ, 0f, progress);

        frame.scaleX = interpolate(start.scaleX, end.scaleX, 1f, progress);
        frame.scaleY = interpolate(start.scaleY, end.scaleY, 1f, progress);

        frame.translationX = interpolate(start.translationX, end.translationX, 0f, progress);
        frame.translationY = interpolate(start.translationY, end.translationY, 0f, progress);
        frame.translationZ = interpolate(start.translationZ, end.translationZ, 0f, progress);

        frame.alpha = interpolate(startAlpha, endAlpha, 1f, progress);

        Set<String> keys = end.mCustom.keySet();
        frame.mCustom.clear();
        for (String key : keys) {
            if (start.mCustom.containsKey(key)) {
                CustomVariable startVariable = start.mCustom.get(key);
                CustomVariable endVariable = end.mCustom.get(key);
                CustomVariable interpolated = new CustomVariable(startVariable);
                frame.mCustom.put(key, interpolated);
                if (startVariable.numberOfInterpolatedValues() == 1) {
                    interpolated.setValue(interpolate(startVariable.getValueToInterpolate(),
                            endVariable.getValueToInterpolate(), 0f, progress));
                } else {
                    int n = startVariable.numberOfInterpolatedValues();
                    float[] startValues = new float[n];
                    float[] endValues = new float[n];
                    startVariable.getValuesToInterpolate(startValues);
                    endVariable.getValuesToInterpolate(endValues);
                    for (int i = 0; i < n; i++) {
                        startValues[i] = interpolate(startValues[i], endValues[i], 0f, progress);
                        interpolated.setValue(startValues);
                    }
                }
            }
        }
    }

    private static float interpolate(float start, float end, float defaultValue, float progress) {
        boolean isStartUnset = Float.isNaN(start);
        boolean isEndUnset = Float.isNaN(end);
        if (isStartUnset && isEndUnset) {
            return Float.NaN;
        }
        if (isStartUnset) {
            start = defaultValue;
        }
        if (isEndUnset) {
            end = defaultValue;
        }
        return (start + progress * (end - start));
    }

    // @TODO: add description
    public float centerX() {
        return left + (right - left) / 2f;
    }

    // @TODO: add description
    public float centerY() {
        return top + (bottom - top) / 2f;
    }

    // @TODO: add description
    public WidgetFrame update() {
        if (widget != null) {
            left = widget.getLeft();
            top = widget.getTop();
            right = widget.getRight();
            bottom = widget.getBottom();
            WidgetFrame frame = widget.frame;
            updateAttributes(frame);
        }
        return this;
    }

    // @TODO: add description
    public WidgetFrame update(ConstraintWidget widget) {
        if (widget == null) {
            return this;
        }

        this.widget = widget;
        update();
        return this;
    }

    /**
     * Return whether this WidgetFrame contains a custom property of the given name.
     */
    public boolean containsCustom(@NonNull String name) {
        return mCustom.containsKey(name);
    }

    // @TODO: add description
    public void addCustomColor(String name, int color) {
        setCustomAttribute(name, TypedValues.Custom.TYPE_COLOR, color);
    }

    // @TODO: add description
    public int getCustomColor(String name) {
        if (mCustom.containsKey(name)) {
            return mCustom.get(name).getColorValue();
        }
        return 0xFFFFAA88;
    }

    // @TODO: add description
    public void addCustomFloat(String name, float value) {
        setCustomAttribute(name, TypedValues.Custom.TYPE_FLOAT, value);
    }

    // @TODO: add description
    public float getCustomFloat(String name) {
        if (mCustom.containsKey(name)) {
            return mCustom.get(name).getFloatValue();
        }
        return Float.NaN;
    }

    // @TODO: add description
    public void setCustomAttribute(String name, int type, float value) {
        if (mCustom.containsKey(name)) {
            mCustom.get(name).setFloatValue(value);
        } else {
            mCustom.put(name, new CustomVariable(name, type, value));
        }
    }

    // @TODO: add description
    public void setCustomAttribute(String name, int type, int value) {
        if (mCustom.containsKey(name)) {
            mCustom.get(name).setIntValue(value);
        } else {
            mCustom.put(name, new CustomVariable(name, type, value));
        }
    }

    // @TODO: add description
    public void setCustomAttribute(String name, int type, boolean value) {
        if (mCustom.containsKey(name)) {
            mCustom.get(name).setBooleanValue(value);
        } else {
            mCustom.put(name, new CustomVariable(name, type, value));
        }
    }

    // @TODO: add description
    public void setCustomAttribute(String name, int type, String value) {
        if (mCustom.containsKey(name)) {
            mCustom.get(name).setStringValue(value);
        } else {
            mCustom.put(name, new CustomVariable(name, type, value));
        }
    }

    /**
     * Get the custom attribute given Nam
     * @param name Name of the custom attribut
     * @return The customAttribute
     */
    public CustomVariable getCustomAttribute(String name) {
        return mCustom.get(name);
    }

    /**
     * Get the known custom Attributes names
     * @return set of custom attribute names
     */
    public Set<String> getCustomAttributeNames() {
        return mCustom.keySet();
    }

    // @TODO: add description
    public boolean setValue(String key, CLElement value) throws CLParsingException {
        switch (key) {
            case "pivotX":
                pivotX = value.getFloat();
                break;
            case "pivotY":
                pivotY = value.getFloat();
                break;
            case "rotationX":
                rotationX = value.getFloat();
                break;
            case "rotationY":
                rotationY = value.getFloat();
                break;
            case "rotationZ":
                rotationZ = value.getFloat();
                break;
            case "translationX":
                translationX = value.getFloat();
                break;
            case "translationY":
                translationY = value.getFloat();
                break;
            case "translationZ":
                translationZ = value.getFloat();
                break;
            case "scaleX":
                scaleX = value.getFloat();
                break;
            case "scaleY":
                scaleY = value.getFloat();
                break;
            case "alpha":
                alpha = value.getFloat();
                break;
            case "interpolatedPos":
                interpolatedPos = value.getFloat();
                break;
            case "phone_orientation":
                phone_orientation = value.getFloat();
                break;
            case "top":
                top = value.getInt();
                break;
            case "left":
                left = value.getInt();
                break;
            case "right":
                right = value.getInt();
                break;
            case "bottom":
                bottom = value.getInt();
                break;
            case "custom":
                parseCustom(value);
                break;

            default:
                return false;
        }
        return true;
    }

    // @TODO: add description
    public String getId() {
        if (widget == null) {
            return "unknown";
        }
        return widget.stringId;
    }

    void parseCustom(CLElement custom) throws CLParsingException {
        CLObject obj = ((CLObject) custom);
        int n = obj.size();
        for (int i = 0; i < n; i++) {
            CLElement tmp = obj.get(i);
            CLKey k = ((CLKey) tmp);
            CLElement v = k.getValue();
            String vStr = v.content();
            if (vStr.matches("#[0-9a-fA-F]+")) {
                int color = Integer.parseInt(vStr.substring(1), 16);
                setCustomAttribute(name, TypedValues.Custom.TYPE_COLOR, color);
            } else if (v instanceof CLNumber) {
                setCustomAttribute(name, TypedValues.Custom.TYPE_FLOAT, v.getFloat());
            } else {
                setCustomAttribute(name, TypedValues.Custom.TYPE_STRING, vStr);

            }
        }
    }

    // @TODO: add description
    public StringBuilder serialize(StringBuilder ret) {
        return serialize(ret, false);
    }

    /**
     * If true also send the phone orientation
     */
    public StringBuilder serialize(StringBuilder ret, boolean sendPhoneOrientation) {
        WidgetFrame frame = this;
        ret.append("{\n");
        add(ret, "left", frame.left);
        add(ret, "top", frame.top);
        add(ret, "right", frame.right);
        add(ret, "bottom", frame.bottom);
        add(ret, "pivotX", frame.pivotX);
        add(ret, "pivotY", frame.pivotY);
        add(ret, "rotationX", frame.rotationX);
        add(ret, "rotationY", frame.rotationY);
        add(ret, "rotationZ", frame.rotationZ);
        add(ret, "translationX", frame.translationX);
        add(ret, "translationY", frame.translationY);
        add(ret, "translationZ", frame.translationZ);
        add(ret, "scaleX", frame.scaleX);
        add(ret, "scaleY", frame.scaleY);
        add(ret, "alpha", frame.alpha);
        add(ret, "visibility", frame.visibility);
        add(ret, "interpolatedPos", frame.interpolatedPos);
        if (widget != null) {
            for (ConstraintAnchor.Type side : ConstraintAnchor.Type.values()) {
                serializeAnchor(ret, side);
            }
        }
        if (sendPhoneOrientation) {
            add(ret, "phone_orientation", phone_orientation);
        }
        if (sendPhoneOrientation) {
            add(ret, "phone_orientation", phone_orientation);
        }

        if (frame.mCustom.size() != 0) {
            ret.append("custom : {\n");
            for (String s : frame.mCustom.keySet()) {
                CustomVariable value = frame.mCustom.get(s);
                ret.append(s);
                ret.append(": ");
                switch (value.getType()) {
                    case TypedValues.Custom.TYPE_INT:
                        ret.append(value.getIntegerValue());
                        ret.append(",\n");
                        break;
                    case TypedValues.Custom.TYPE_FLOAT:
                    case TypedValues.Custom.TYPE_DIMENSION:
                        ret.append(value.getFloatValue());
                        ret.append(",\n");
                        break;
                    case TypedValues.Custom.TYPE_COLOR:
                        ret.append("'");
                        ret.append(CustomVariable.colorString(value.getIntegerValue()));
                        ret.append("',\n");
                        break;
                    case TypedValues.Custom.TYPE_STRING:
                        ret.append("'");
                        ret.append(value.getStringValue());
                        ret.append("',\n");
                        break;
                    case TypedValues.Custom.TYPE_BOOLEAN:
                        ret.append("'");
                        ret.append(value.getBooleanValue());
                        ret.append("',\n");
                        break;
                }
            }
            ret.append("}\n");
        }

        ret.append("}\n");
        return ret;
    }

    private void serializeAnchor(StringBuilder ret, ConstraintAnchor.Type type) {
        ConstraintAnchor anchor = widget.getAnchor(type);
        if (anchor == null || anchor.mTarget == null) {
            return;
        }
        ret.append("Anchor");
        ret.append(type.name());
        ret.append(": ['");
        String str = anchor.mTarget.getOwner().stringId;
        ret.append(str == null ? "#PARENT" : str);
        ret.append("', '");
        ret.append(anchor.mTarget.getType().name());
        ret.append("', '");
        ret.append(anchor.mMargin);
        ret.append("'],\n");

    }

    private static void add(StringBuilder s, String title, int value) {
        s.append(title);
        s.append(": ");
        s.append(value);
        s.append(",\n");
    }

    private static void add(StringBuilder s, String title, float value) {
        if (Float.isNaN(value)) {
            return;
        }
        s.append(title);
        s.append(": ");
        s.append(value);
        s.append(",\n");
    }

    /**
     * For debugging only
     */
    void printCustomAttributes() {
        StackTraceElement s = new Throwable().getStackTrace()[1];
        String ss = ".(" + s.getFileName() + ":" + s.getLineNumber() + ") " + s.getMethodName();
        ss += " " + (this.hashCode() % 1000);
        if (widget != null) {
            ss += "/" + (widget.hashCode() % 1000) + " ";
        } else {
            ss += "/NULL ";
        }
        if (mCustom != null) {
            for (String key : mCustom.keySet()) {
                System.out.println(ss + mCustom.get(key).toString());
            }
        }
    }

    /**
     * For debugging only
     */
    void logv(String str) {
        StackTraceElement s = new Throwable().getStackTrace()[1];
        String ss = ".(" + s.getFileName() + ":" + s.getLineNumber() + ") " + s.getMethodName();
        ss += " " + (this.hashCode() % 1000);
        if (widget != null) {
            ss += "/" + (widget.hashCode() % 1000);
        } else {
            ss += "/NULL";
        }

        System.out.println(ss + " " + str);
    }

    // @TODO: add description
    public void setCustomValue(CustomAttribute valueAt, float[] mTempValues) {
    }

    void setMotionAttributes(TypedBundle motionProperties) {
        mMotionProperties = motionProperties;
    }

    /**
     * get the property bundle associated with MotionAttributes
     *
     * @return the property bundle associated with MotionAttributes or null
     */
    public TypedBundle getMotionProperties() {
        return mMotionProperties;
    }
}
