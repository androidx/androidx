/*
 * Copyright (C) 2024 The Android Open Source Project
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
package androidx.compose.remote.core.operations.layout.animation;

import static androidx.compose.remote.core.documentation.DocumentedOperation.FLOAT;
import static androidx.compose.remote.core.documentation.DocumentedOperation.INT;

import androidx.annotation.RestrictTo;
import androidx.compose.remote.core.Operation;
import androidx.compose.remote.core.Operations;
import androidx.compose.remote.core.RemoteContext;
import androidx.compose.remote.core.WireBuffer;
import androidx.compose.remote.core.documentation.DocumentationBuilder;
import androidx.compose.remote.core.operations.layout.modifiers.ModifierOperation;
import androidx.compose.remote.core.operations.utilities.StringSerializer;
import androidx.compose.remote.core.operations.utilities.easing.Easing;
import androidx.compose.remote.core.operations.utilities.easing.GeneralEasing;
import androidx.compose.remote.core.serialize.MapSerializer;

import org.jspecify.annotations.NonNull;

import java.util.List;

/** Basic component animation spec */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class AnimationSpec extends Operation implements ModifierOperation {
    public static final AnimationSpec DEFAULT = new AnimationSpec();
    public static final AnimationSpec DISABLED = new AnimationSpec(0);
    int mAnimationId = -1;
    float mMotionDuration = 300;
    int mMotionEasingType = GeneralEasing.CUBIC_STANDARD;
    float mVisibilityDuration = 300;
    int mVisibilityEasingType = GeneralEasing.CUBIC_STANDARD;
    @NonNull ANIMATION mEnterAnimation = ANIMATION.FADE_IN;
    @NonNull ANIMATION mExitAnimation = ANIMATION.FADE_OUT;
    int mEnterFunctionId = -1;
    int mExitFunctionId = -1;
    @NonNull SEQUENCE mEnterSequence = SEQUENCE.CONCURRENT;
    @NonNull SEQUENCE mExitSequence = SEQUENCE.CONCURRENT;

    private static final int OP_CODE = Operations.ANIMATION_SPEC;
    private static final String CLASS_NAME = "AnimationSpec";

    public AnimationSpec(
            int animationId,
            float motionDuration,
            int motionEasingType,
            float visibilityDuration,
            int visibilityEasingType,
            @NonNull ANIMATION enterAnimation,
            @NonNull ANIMATION exitAnimation,
            int enterFunctionId,
            int exitFunctionId,
            @NonNull SEQUENCE enterSequence,
            @NonNull SEQUENCE exitSequence) {
        this.mAnimationId = animationId;
        this.mMotionDuration = motionDuration;
        this.mMotionEasingType = motionEasingType;
        this.mVisibilityDuration = visibilityDuration;
        this.mVisibilityEasingType = visibilityEasingType;
        this.mEnterAnimation = enterAnimation;
        this.mExitAnimation = exitAnimation;
        this.mEnterFunctionId = enterFunctionId;
        this.mExitFunctionId = exitFunctionId;
        this.mEnterSequence = enterSequence;
        this.mExitSequence = exitSequence;
    }

    public AnimationSpec(
            int animationId,
            float motionDuration,
            int motionEasingType,
            float visibilityDuration,
            int visibilityEasingType,
            @NonNull ANIMATION enterAnimation,
            @NonNull ANIMATION exitAnimation,
            int enterFunctionId,
            int exitFunctionId) {
        this(
                animationId,
                motionDuration,
                motionEasingType,
                visibilityDuration,
                visibilityEasingType,
                enterAnimation,
                exitAnimation,
                enterFunctionId,
                exitFunctionId,
                SEQUENCE.CONCURRENT,
                SEQUENCE.CONCURRENT);
    }

    public AnimationSpec(
            int animationId,
            float motionDuration,
            int motionEasingType,
            float visibilityDuration,
            int visibilityEasingType,
            @NonNull ANIMATION enterAnimation,
            @NonNull ANIMATION exitAnimation) {
        this(
                animationId,
                motionDuration,
                motionEasingType,
                visibilityDuration,
                visibilityEasingType,
                enterAnimation,
                exitAnimation,
                -1,
                -1,
                SEQUENCE.CONCURRENT,
                SEQUENCE.CONCURRENT);
    }

    public AnimationSpec() {
        this(
                -1,
                300,
                GeneralEasing.CUBIC_STANDARD,
                300,
                GeneralEasing.CUBIC_STANDARD,
                ANIMATION.FADE_IN,
                ANIMATION.FADE_OUT);
    }

    public AnimationSpec(int value) {
        this();
        mAnimationId = value;
    }

    public boolean isAnimationEnabled() {
        return mAnimationId != 0;
    }

    public int getAnimationId() {
        return mAnimationId;
    }

    public float getMotionDuration() {
        return mMotionDuration;
    }

    public int getMotionEasingType() {
        return mMotionEasingType;
    }

    public float getVisibilityDuration() {
        return mVisibilityDuration;
    }

    public int getVisibilityEasingType() {
        return mVisibilityEasingType;
    }

    @NonNull
    public ANIMATION getEnterAnimation() {
        return mEnterAnimation;
    }

    @NonNull
    public ANIMATION getExitAnimation() {
        return mExitAnimation;
    }

    public int getEnterFunctionId() {
        return mEnterFunctionId;
    }

    public int getExitFunctionId() {
        return mExitFunctionId;
    }

    @NonNull
    public SEQUENCE getEnterSequence() {
        return mEnterSequence;
    }

    @NonNull
    public SEQUENCE getExitSequence() {
        return mExitSequence;
    }

    @NonNull
    @Override
    public String toString() {
        return "ANIMATION_SPEC (" + mMotionDuration + " ms)";
    }

    @Override
    public void serializeToString(int indent, @NonNull StringSerializer serializer) {
        serializer.append(
                indent,
                "ANIMATION_SPEC = ["
                        + getMotionDuration()
                        + ", "
                        + getMotionEasingType()
                        + ", "
                        + getVisibilityDuration()
                        + ", "
                        + getVisibilityEasingType()
                        + ", "
                        + getEnterAnimation()
                        + ", "
                        + getExitAnimation()
                        + (mEnterFunctionId != -1 ? ", enterFunction=" + mEnterFunctionId : "")
                        + (mExitFunctionId != -1 ? ", exitFunction=" + mExitFunctionId : "")
                        + (mEnterSequence != SEQUENCE.CONCURRENT
                                ? ", enterSequence=" + mEnterSequence
                                : "")
                        + (mExitSequence != SEQUENCE.CONCURRENT
                                ? ", exitSequence=" + mExitSequence
                                : "")
                        + "]");
    }

    @Override
    public void serialize(@NonNull MapSerializer serializer) {
        serializer
                .addType("AnimationSpec")
                .add("animationId", mAnimationId)
                .add("motionDuration", getMotionDuration())
                .add("motionEasingType", Easing.getString(getMotionEasingType()))
                .add("visibilityDuration", getVisibilityDuration())
                .add("visibilityEasingType", Easing.getString(getVisibilityEasingType()))
                .add("enterAnimation", getEnterAnimation())
                .add("exitAnimation", getExitAnimation());
        if (mEnterFunctionId != -1) {
            serializer.add("enterFunctionId", mEnterFunctionId);
        }
        if (mExitFunctionId != -1) {
            serializer.add("exitFunctionId", mExitFunctionId);
        }
        if (mEnterSequence != SEQUENCE.CONCURRENT) {
            serializer.add("enterSequence", mEnterSequence);
        }
        if (mExitSequence != SEQUENCE.CONCURRENT) {
            serializer.add("exitSequence", mExitSequence);
        }
    }

    public enum ANIMATION {
        FADE_IN,
        FADE_OUT,
        SLIDE_LEFT,
        SLIDE_RIGHT,
        SLIDE_TOP,
        SLIDE_BOTTOM,
        ROTATE,
        PARTICLE,
        CUSTOM
    }

    public enum SEQUENCE {
        CONCURRENT,
        BEFORE,
        AFTER
    }

    @Override
    public void write(@NonNull WireBuffer buffer) {
        apply(
                buffer,
                mAnimationId,
                mMotionDuration,
                mMotionEasingType,
                mVisibilityDuration,
                mVisibilityEasingType,
                packAnimation(mEnterAnimation, mEnterSequence),
                packAnimation(mExitAnimation, mExitSequence),
                mEnterFunctionId,
                mExitFunctionId);
    }

    @Override
    public void apply(@NonNull RemoteContext context) {
        // nothing here
    }

    @NonNull
    @Override
    public String deepToString(@NonNull String indent) {
        return (indent != null ? indent : "") + toString();
    }

    /**
     * The name of the class
     *
     * @return the name
     */
    @NonNull
    public static String name() {
        return "AnimationSpec";
    }

    /**
     * The OP_CODE for this command
     *
     * @return the opcode
     */
    public static int id() {
        return Operations.ANIMATION_SPEC;
    }

    /**
     * Returns an int for the given ANIMATION
     *
     * @param animation an ANIMATION enum value
     * @return a corresponding int value
     */
    public static int animationToInt(@NonNull ANIMATION animation) {
        return animation.ordinal();
    }

    /**
     * Packs an ANIMATION and SEQUENCE into a single wire int.
     *
     * @param animation the animation type
     * @param sequence the sequence relative to layout changes
     * @return the packed integer
     */
    public static int packAnimation(@NonNull ANIMATION animation, @NonNull SEQUENCE sequence) {
        return (animation.ordinal() & 0xFF) | ((sequence.ordinal() & 0xFF) << 8);
    }

    /**
     * Maps int value to the corresponding SEQUENCE enum values
     *
     * @param value int value mapped to the enum
     * @return the corresponding SEQUENCE enum value
     */
    @NonNull
    public static SEQUENCE intToSequence(int value) {
        switch (value & 0xFF) {
            case 1:
                return SEQUENCE.BEFORE;
            case 2:
                return SEQUENCE.AFTER;
            case 0:
            default:
                return SEQUENCE.CONCURRENT;
        }
    }

    /**
     * Maps int value to the corresponding ANIMATION enum values
     *
     * @param value int value mapped to the enum
     * @return the corresponding ANIMATION enum value
     */
    @NonNull
    public static ANIMATION intToAnimation(int value) {
        switch (value & 0xFF) {
            case 0:
                return ANIMATION.FADE_IN;
            case 1:
                return ANIMATION.FADE_OUT;
            case 2:
                return ANIMATION.SLIDE_LEFT;
            case 3:
                return ANIMATION.SLIDE_RIGHT;
            case 4:
                return ANIMATION.SLIDE_TOP;
            case 5:
                return ANIMATION.SLIDE_BOTTOM;
            case 6:
                return ANIMATION.ROTATE;
            case 7:
                return ANIMATION.PARTICLE;
            case 8:
                return ANIMATION.CUSTOM;
            default:
                return ANIMATION.FADE_IN;
        }
    }

    /**
     * Write the operation to the buffer
     *
     * @param buffer a WireBuffer
     * @param animationId the animation id
     * @param motionDuration the duration of the motion animation
     * @param motionEasingType the type of easing for the motion animation
     * @param visibilityDuration the duration of the visibility animation
     * @param visibilityEasingType the type of easing for the visibility animation
     * @param enterAnimation the type of animation when "entering" (newly visible)
     * @param exitAnimation the type of animation when "exiting" (newly gone)
     */
    public static void apply(
            @NonNull WireBuffer buffer,
            int animationId,
            float motionDuration,
            int motionEasingType,
            float visibilityDuration,
            int visibilityEasingType,
            int enterAnimation,
            int exitAnimation) {
        apply(
                buffer,
                animationId,
                motionDuration,
                motionEasingType,
                visibilityDuration,
                visibilityEasingType,
                enterAnimation,
                exitAnimation,
                -1,
                -1);
    }

    /**
     * Write the operation to the buffer
     *
     * @param buffer a WireBuffer
     * @param animationId the animation id
     * @param motionDuration the duration of the motion animation
     * @param motionEasingType the type of easing for the motion animation
     * @param visibilityDuration the duration of the visibility animation
     * @param visibilityEasingType the type of easing for the visibility animation
     * @param enterAnimation the type of animation when "entering" (newly visible)
     * @param exitAnimation the type of animation when "exiting" (newly gone)
     * @param enterFunctionId the function id for custom enter animation
     * @param exitFunctionId the function id for custom exit animation
     */
    public static void apply(
            @NonNull WireBuffer buffer,
            int animationId,
            float motionDuration,
            int motionEasingType,
            float visibilityDuration,
            int visibilityEasingType,
            int enterAnimation,
            int exitAnimation,
            int enterFunctionId,
            int exitFunctionId) {
        buffer.start(Operations.ANIMATION_SPEC);
        buffer.writeInt(animationId);
        buffer.writeFloat(motionDuration);
        buffer.writeInt(motionEasingType);
        buffer.writeFloat(visibilityDuration);
        buffer.writeInt(visibilityEasingType);
        buffer.writeInt(enterAnimation);
        buffer.writeInt(exitAnimation);
        if ((enterAnimation & 0xFF) == ANIMATION.CUSTOM.ordinal()
                || (exitAnimation & 0xFF) == ANIMATION.CUSTOM.ordinal()
                || enterFunctionId != -1
                || exitFunctionId != -1) {
            buffer.writeInt(enterFunctionId);
            buffer.writeInt(exitFunctionId);
        }
    }

    /**
     * Read this operation and add it to the list of operations
     *
     * @param buffer the buffer to read
     * @param operations the list of operations that will be added to
     */
    public static void read(@NonNull WireBuffer buffer, @NonNull List<Operation> operations) {
        int animationId = buffer.readId();
        float motionDuration = buffer.readFloat();
        int motionEasingType = buffer.readInt();
        float visibilityDuration = buffer.readFloat();
        int visibilityEasingType = buffer.readInt();
        int rawEnter = buffer.readInt();
        int rawExit = buffer.readInt();
        ANIMATION enterAnimation = intToAnimation(rawEnter);
        ANIMATION exitAnimation = intToAnimation(rawExit);
        SEQUENCE enterSequence = intToSequence(rawEnter >> 8);
        SEQUENCE exitSequence = intToSequence(rawExit >> 8);
        int enterFunctionId = -1;
        int exitFunctionId = -1;
        if (enterAnimation == ANIMATION.CUSTOM || exitAnimation == ANIMATION.CUSTOM) {
            enterFunctionId = buffer.readId();
            exitFunctionId = buffer.readId();
        }
        AnimationSpec op =
                new AnimationSpec(
                        animationId,
                        motionDuration,
                        motionEasingType,
                        visibilityDuration,
                        visibilityEasingType,
                        enterAnimation,
                        exitAnimation,
                        enterFunctionId,
                        exitFunctionId,
                        enterSequence,
                        exitSequence);
        operations.add(op);
    }

    /**
     * Populate the documentation with a description of this operation
     *
     * @param doc to append the description to.
     */
    public static void documentation(@NonNull DocumentationBuilder doc) {
        doc.operation("Animation & Particles Operations", OP_CODE, CLASS_NAME)
                .additionalDocumentation("animation_spec")
                .description("Define the animation specifications for a component")
                .field(INT, "animationId", "The ID of the animation")
                .field(FLOAT, "motionDuration", "Duration of the motion animation in ms")
                .field(INT, "motionEasingType", "The type of easing for motion")
                .field(FLOAT, "visibilityDuration", "Duration of visibility animation in ms")
                .field(INT, "visibilityEasingType", "The type of easing for visibility")
                .field(INT, "enterAnimation", "The entry animation type")
                .field(INT, "exitAnimation", "The exit animation type");
    }
}
