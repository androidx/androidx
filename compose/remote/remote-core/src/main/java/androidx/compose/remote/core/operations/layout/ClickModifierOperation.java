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
package androidx.compose.remote.core.operations.layout;

import androidx.annotation.RestrictTo;
import androidx.compose.remote.core.CoreDocument;
import androidx.compose.remote.core.Operation;
import androidx.compose.remote.core.Operations;
import androidx.compose.remote.core.PaintContext;
import androidx.compose.remote.core.PaintOperation;
import androidx.compose.remote.core.RemoteContext;
import androidx.compose.remote.core.WireBuffer;
import androidx.compose.remote.core.documentation.DocumentationBuilder;
import androidx.compose.remote.core.operations.TextData;
import androidx.compose.remote.core.operations.Utils;
import androidx.compose.remote.core.operations.layout.modifiers.ModifierOperation;
import androidx.compose.remote.core.operations.paint.PaintBundle;
import androidx.compose.remote.core.operations.utilities.ColorUtils;
import androidx.compose.remote.core.operations.utilities.StringSerializer;
import androidx.compose.remote.core.operations.utilities.easing.Easing;
import androidx.compose.remote.core.operations.utilities.easing.FloatAnimation;
import androidx.compose.remote.core.semantics.AccessibleComponent;
import androidx.compose.remote.core.semantics.CoreSemantics;
import androidx.compose.remote.core.serialize.MapSerializer;
import androidx.compose.remote.core.serialize.SerializeTags;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/** Represents a click modifier + actions */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class ClickModifierOperation extends PaintOperation
        implements Container,
                ModifierOperation,
                DecoratorComponent,
                ClickHandler,
                AccessibleComponent {
    private static final int OP_CODE = Operations.MODIFIER_CLICK;

    long mAnimateRippleStart = 0;
    float mAnimateRippleX = 0f;
    float mAnimateRippleY = 0f;
    int mAnimateRippleDuration = 1000;

    float mWidth = 0;
    float mHeight = 0;

    public float @NonNull [] locationInWindow = new float[2];

    @NonNull PaintBundle mPaint = new PaintBundle();

    @Override
    public boolean isClickable() {
        return true;
    }

    @Nullable
    @Override
    public Role getRole() {
        return Role.BUTTON;
    }

    @Override
    public CoreSemantics.@NonNull Mode getMode() {
        return CoreSemantics.Mode.MERGE;
    }

    /**
     * Animate ripple
     *
     * @param x starting position x of the ripple
     * @param y starting position y of the ripple
     * @param timeStampMillis
     */
    public void animateRipple(float x, float y, long timeStampMillis) {
        mAnimateRippleStart = timeStampMillis;
        mAnimateRippleX = x;
        mAnimateRippleY = y;
    }

    @NonNull public ArrayList<Operation> mList = new ArrayList<>();

    @NonNull
    @Override
    public ArrayList<Operation> getList() {
        return mList;
    }

    @Override
    public void write(@NonNull WireBuffer buffer) {
        apply(buffer);
    }

    @NonNull
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("ClickModifier");
        for (Operation modifierOperation : mList) {
            sb.append("\n        ");
            sb.append(modifierOperation.toString());
        }
        return sb.toString();
    }

    @Override
    public void apply(@NonNull RemoteContext context) {
        RootLayoutComponent root = context.getDocument().getRootLayoutComponent();
        if (root != null) {
            root.setHasTouchListeners(true);
        }
        for (Operation op : mList) {
            if (op instanceof TextData) {
                op.apply(context);
                context.incrementOpCount();
            }
        }
    }

    @NonNull
    @Override
    public String deepToString(@NonNull String indent) {
        return (indent != null ? indent : "") + toString();
    }

    @Override
    public void paint(@NonNull PaintContext context) {
        if (mAnimateRippleStart == 0) {
            return;
        }
        context.needsRepaint();

        float progress = (context.getClock().millis() - mAnimateRippleStart);
        progress /= (float) mAnimateRippleDuration;
        if (progress > 1f) {
            mAnimateRippleStart = 0;
        }
        progress = Math.min(1f, progress);
        context.save();
        context.savePaint();
        mPaint.reset();

        FloatAnimation anim1 =
                new FloatAnimation(Easing.CUBIC_STANDARD, 1f, null, Float.NaN, Float.NaN);
        anim1.setInitialValue(0f);
        anim1.setTargetValue(1f);
        float tween = anim1.get(progress);

        FloatAnimation anim2 =
                new FloatAnimation(Easing.CUBIC_STANDARD, 0.5f, null, Float.NaN, Float.NaN);
        anim2.setInitialValue(0f);
        anim2.setTargetValue(1f);
        float tweenRadius = anim2.get(progress);

        int startColor = ColorUtils.createColor(250, 250, 250, 180);
        int endColor = ColorUtils.createColor(200, 200, 200, 0);
        int paintedColor = Utils.interpolateColor(startColor, endColor, tween);

        float radius = Math.max(mWidth, mHeight) * tweenRadius;
        mPaint.setColor(paintedColor);
        context.applyPaint(mPaint);
        context.clipRect(0f, 0f, mWidth, mHeight);
        context.drawCircle(mAnimateRippleX, mAnimateRippleY, radius);
        context.restorePaint();
        context.restore();
    }

    @Override
    public void layout(
            @NonNull RemoteContext context,
            @NonNull Component component,
            float width,
            float height) {
        mWidth = width;
        mHeight = height;
    }

    @Override
    public void serializeToString(int indent, @NonNull StringSerializer serializer) {
        serializer.append(indent, "CLICK_MODIFIER");
        for (Operation o : mList) {
            if (o instanceof ActionOperation) {
                ((ActionOperation) o).serializeToString(indent + 1, serializer);
            }
        }
    }

    @Override
    public void onClick(
            @NonNull RemoteContext context,
            @NonNull CoreDocument document,
            @NonNull Component component,
            float x,
            float y) {
        if (!component.isVisible()) {
            return;
        }
        locationInWindow[0] = 0f;
        locationInWindow[1] = 0f;
        component.getLocationInWindow(locationInWindow);
        animateRipple(
                x - locationInWindow[0], y - locationInWindow[1], context.getClock().millis());
        for (Operation o : mList) {
            if (o instanceof ActionOperation) {
                ((ActionOperation) o).runAction(context, document, component, x, y);
            }
        }
        context.hapticEffect(3);
    }

    /**
     * The name of the class
     *
     * @return the name
     */
    @NonNull
    public static String name() {
        return "ClickModifier";
    }

    /**
     * Write the operation on the buffer
     *
     * @param buffer
     */
    public static void apply(@NonNull WireBuffer buffer) {
        buffer.start(OP_CODE);
    }

    /**
     * Read this operation and add it to the list of operations
     *
     * @param buffer the buffer to read
     * @param operations the list of operations that will be added to
     */
    public static void read(@NonNull WireBuffer buffer, @NonNull List<Operation> operations) {
        operations.add(new ClickModifierOperation());
    }

    /**
     * Populate the documentation with a description of this operation
     *
     * @param doc to append the description to.
     */
    public static void documentation(@NonNull DocumentationBuilder doc) {
        doc.operation("Layout Operations", OP_CODE, name())
                .description(
                        "Click modifier. This operation contains"
                                + " a list of action executed on click");
    }

    @Override
    public void serialize(@NonNull MapSerializer serializer) {
        serializer.addTags(SerializeTags.MODIFIER).addType("ClickModifierOperation");
    }
}
