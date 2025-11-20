/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.wear.protolayout.expression.pipeline;

import android.util.Log;

import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;
import androidx.wear.protolayout.expression.DynamicBuilders;
import androidx.wear.protolayout.expression.proto.DynamicProto.AnimatableDynamicColor;
import androidx.wear.protolayout.expression.proto.DynamicProto.AnimatableDynamicFloat;
import androidx.wear.protolayout.expression.proto.DynamicProto.AnimatableDynamicInt32;
import androidx.wear.protolayout.expression.proto.DynamicProto.ArithmeticFloatOp;
import androidx.wear.protolayout.expression.proto.DynamicProto.ArithmeticInt32Op;
import androidx.wear.protolayout.expression.proto.DynamicProto.BetweenDuration;
import androidx.wear.protolayout.expression.proto.DynamicProto.ComparisonFloatOp;
import androidx.wear.protolayout.expression.proto.DynamicProto.ComparisonInt32Op;
import androidx.wear.protolayout.expression.proto.DynamicProto.ConcatStringOp;
import androidx.wear.protolayout.expression.proto.DynamicProto.ConditionalColorOp;
import androidx.wear.protolayout.expression.proto.DynamicProto.ConditionalDurationOp;
import androidx.wear.protolayout.expression.proto.DynamicProto.ConditionalFloatOp;
import androidx.wear.protolayout.expression.proto.DynamicProto.ConditionalInstantOp;
import androidx.wear.protolayout.expression.proto.DynamicProto.ConditionalInt32Op;
import androidx.wear.protolayout.expression.proto.DynamicProto.ConditionalStringOp;
import androidx.wear.protolayout.expression.proto.DynamicProto.DynamicBool;
import androidx.wear.protolayout.expression.proto.DynamicProto.DynamicColor;
import androidx.wear.protolayout.expression.proto.DynamicProto.DynamicDuration;
import androidx.wear.protolayout.expression.proto.DynamicProto.DynamicFloat;
import androidx.wear.protolayout.expression.proto.DynamicProto.DynamicInstant;
import androidx.wear.protolayout.expression.proto.DynamicProto.DynamicInt32;
import androidx.wear.protolayout.expression.proto.DynamicProto.DynamicString;
import androidx.wear.protolayout.expression.proto.DynamicProto.DynamicZonedDateTime;
import androidx.wear.protolayout.expression.proto.DynamicProto.FloatFormatOp;
import androidx.wear.protolayout.expression.proto.DynamicProto.FloatToInt32Op;
import androidx.wear.protolayout.expression.proto.DynamicProto.GetDurationPartOp;
import androidx.wear.protolayout.expression.proto.DynamicProto.GetZonedDateTimePartOp;
import androidx.wear.protolayout.expression.proto.DynamicProto.InstantToZonedDateTimeOp;
import androidx.wear.protolayout.expression.proto.DynamicProto.Int32FormatOp;
import androidx.wear.protolayout.expression.proto.DynamicProto.Int32ToFloatOp;
import androidx.wear.protolayout.expression.proto.DynamicProto.LogicalBoolOp;
import androidx.wear.protolayout.expression.proto.DynamicProto.NotBoolOp;

import org.jspecify.annotations.Nullable;

import java.util.Objects;

/**
 * A utility class for providing content-based equality for dynamic proto messages. This is needed
 * because the default proto equals method is slow.
 */
@RestrictTo(Scope.LIBRARY_GROUP)
public final class DynamicProtoHashEquals {
    private static final String TAG = "DynamicProtoHashEquals";

    private DynamicProtoHashEquals() {}

    /** Returns a hash code for a proto message that is based on its contents. */
    public static int hashCode(@Nullable Object proto) {
        if (proto == null) {
            return 0;
        }

        if (proto instanceof DynamicBuilders.DynamicInt32) {
            return hashCodeInternal((DynamicBuilders.DynamicInt32) proto);
        }
        if (proto instanceof DynamicBuilders.DynamicString) {
            return hashCodeInternal((DynamicBuilders.DynamicString) proto);
        }
        if (proto instanceof DynamicBuilders.DynamicFloat) {
            return hashCodeInternal((DynamicBuilders.DynamicFloat) proto);
        }
        if (proto instanceof DynamicBuilders.DynamicColor) {
            return hashCodeInternal((DynamicBuilders.DynamicColor) proto);
        }
        if (proto instanceof DynamicBuilders.DynamicDuration) {
            return hashCodeInternal((DynamicBuilders.DynamicDuration) proto);
        }
        if (proto instanceof DynamicBuilders.DynamicInstant) {
            return hashCodeInternal((DynamicBuilders.DynamicInstant) proto);
        }
        if (proto instanceof DynamicBuilders.DynamicZonedDateTime) {
            return hashCodeInternal((DynamicBuilders.DynamicZonedDateTime) proto);
        }
        if (proto instanceof DynamicBuilders.DynamicBool) {
            return hashCodeInternal((DynamicBuilders.DynamicBool) proto);
        }
        return proto.hashCode();
    }

    /** Checks whether two proto messages are equal based on their contents. */
    public static boolean equals(@Nullable Object dynExpr1, @Nullable Object dynExpr2) {
        if (dynExpr1 == dynExpr2) {
            return true;
        }

        if (dynExpr1 == null || dynExpr2 == null) {
            return false;
        }

        if (dynExpr1.getClass() != dynExpr2.getClass()) {
            return false;
        }

        if (dynExpr1 instanceof DynamicBuilders.DynamicInt32) {
            return equalsInternal(
                    (DynamicBuilders.DynamicInt32) dynExpr1,
                    (DynamicBuilders.DynamicInt32) dynExpr2);
        }
        if (dynExpr1 instanceof DynamicBuilders.DynamicString) {
            return equalsInternal(
                    (DynamicBuilders.DynamicString) dynExpr1,
                    (DynamicBuilders.DynamicString) dynExpr2);
        }
        if (dynExpr1 instanceof DynamicBuilders.DynamicFloat) {
            return equalsInternal(
                    (DynamicBuilders.DynamicFloat) dynExpr1,
                    (DynamicBuilders.DynamicFloat) dynExpr2);
        }
        if (dynExpr1 instanceof DynamicBuilders.DynamicColor) {
            return equalsInternal(
                    (DynamicBuilders.DynamicColor) dynExpr1,
                    (DynamicBuilders.DynamicColor) dynExpr2);
        }
        if (dynExpr1 instanceof DynamicBuilders.DynamicDuration) {
            return equalsInternal(
                    (DynamicBuilders.DynamicDuration) dynExpr1,
                    (DynamicBuilders.DynamicDuration) dynExpr2);
        }
        if (dynExpr1 instanceof DynamicBuilders.DynamicInstant) {
            return equalsInternal(
                    (DynamicBuilders.DynamicInstant) dynExpr1,
                    (DynamicBuilders.DynamicInstant) dynExpr2);
        }
        if (dynExpr1 instanceof DynamicBuilders.DynamicZonedDateTime) {
            return equalsInternal(
                    (DynamicBuilders.DynamicZonedDateTime) dynExpr1,
                    (DynamicBuilders.DynamicZonedDateTime) dynExpr2);
        }
        if (dynExpr1 instanceof DynamicBuilders.DynamicBool) {
            return equalsInternal(
                    (DynamicBuilders.DynamicBool) dynExpr1, (DynamicBuilders.DynamicBool) dynExpr2);
        }

        Log.w(TAG, "Unknown proto message. Falling back to proto equals. This will be slower.");
        return Objects.equals(dynExpr1, dynExpr2);
    }

    private static int hashCodeInternal(GetZonedDateTimePartOp proto) {
        return Objects.hash(proto.getPartType(), hashCode(proto.getInput()));
    }

    private static boolean equalsInternal(
            GetZonedDateTimePartOp proto1, GetZonedDateTimePartOp proto2) {
        return proto1.getPartType() == proto2.getPartType()
                && equals(proto1.getInput(), proto2.getInput());
    }

    private static int hashCodeInternal(GetDurationPartOp proto) {
        return Objects.hash(proto.getDurationPart(), hashCode(proto.getInput()));
    }

    private static boolean equalsInternal(GetDurationPartOp proto1, GetDurationPartOp proto2) {
        return proto1.getDurationPart() == proto2.getDurationPart()
                && equals(proto1.getInput(), proto2.getInput());
    }

    private static int hashCodeInternal(AnimatableDynamicInt32 proto) {
        return Objects.hash(proto.getAnimationSpec().hashCode(), hashCode(proto.getInput()));
    }

    private static boolean equalsInternal(
            AnimatableDynamicInt32 proto1, AnimatableDynamicInt32 proto2) {
        return Objects.equals(proto1.getAnimationSpec(), proto2.getAnimationSpec())
                && equals(proto1.getInput(), proto2.getInput());
    }

    private static int hashCodeInternal(FloatToInt32Op proto) {
        return Objects.hash(proto.getRoundMode(), hashCode(proto.getInput()));
    }

    private static boolean equalsInternal(FloatToInt32Op proto1, FloatToInt32Op proto2) {
        return proto1.getRoundMode() == proto2.getRoundMode()
                && equals(proto1.getInput(), proto2.getInput());
    }

    private static int hashCodeInternal(ConditionalInt32Op proto) {
        return Objects.hash(
                hashCode(proto.getCondition()),
                hashCode(proto.getValueIfTrue()),
                hashCode(proto.getValueIfFalse()));
    }

    private static boolean equalsInternal(ConditionalInt32Op proto1, ConditionalInt32Op proto2) {
        return equals(proto1.getCondition(), proto2.getCondition())
                && equals(proto1.getValueIfTrue(), proto2.getValueIfTrue())
                && equals(proto1.getValueIfFalse(), proto2.getValueIfFalse());
    }

    private static int hashCodeInternal(ArithmeticInt32Op proto) {
        return Objects.hash(
                proto.getOperationType(),
                hashCode(proto.getInputLhs()),
                hashCode(proto.getInputRhs()));
    }

    private static boolean equalsInternal(ArithmeticInt32Op proto1, ArithmeticInt32Op proto2) {
        return proto1.getOperationType() == proto2.getOperationType()
                && equals(proto1.getInputLhs(), proto2.getInputLhs())
                && equals(proto1.getInputRhs(), proto2.getInputRhs());
    }

    private static int hashCodeInternal(ConcatStringOp proto) {
        return Objects.hash(hashCode(proto.getInputLhs()), hashCode(proto.getInputRhs()));
    }

    private static boolean equalsInternal(ConcatStringOp proto1, ConcatStringOp proto2) {
        return equals(proto1.getInputLhs(), proto2.getInputLhs())
                && equals(proto1.getInputRhs(), proto2.getInputRhs());
    }

    private static int hashCodeInternal(ConditionalStringOp proto) {
        return Objects.hash(
                hashCode(proto.getCondition()),
                hashCode(proto.getValueIfTrue()),
                hashCode(proto.getValueIfFalse()));
    }

    private static boolean equalsInternal(ConditionalStringOp proto1, ConditionalStringOp proto2) {
        return equals(proto1.getCondition(), proto2.getCondition())
                && equals(proto1.getValueIfTrue(), proto2.getValueIfTrue())
                && equals(proto1.getValueIfFalse(), proto2.getValueIfFalse());
    }

    private static int hashCodeInternal(Int32FormatOp proto) {
        return Objects.hash(
                hashCode(proto.getInput()), proto.getMinIntegerDigits(), proto.getGroupingUsed());
    }

    private static boolean equalsInternal(Int32FormatOp proto1, Int32FormatOp proto2) {
        return equals(proto1.getInput(), proto2.getInput())
                && proto1.getMinIntegerDigits() == proto2.getMinIntegerDigits()
                && proto1.getGroupingUsed() == proto2.getGroupingUsed();
    }

    private static int hashCodeInternal(FloatFormatOp proto) {
        return Objects.hash(
                hashCode(proto.getInput()),
                proto.getMaxFractionDigits(),
                proto.getMinFractionDigits(),
                proto.getMinIntegerDigits(),
                proto.getGroupingUsed());
    }

    private static boolean equalsInternal(FloatFormatOp proto1, FloatFormatOp proto2) {
        return equals(proto1.getInput(), proto2.getInput())
                && proto1.getMaxFractionDigits() == proto2.getMaxFractionDigits()
                && proto1.getMinFractionDigits() == proto2.getMinFractionDigits()
                && proto1.getMinIntegerDigits() == proto2.getMinIntegerDigits()
                && proto1.getGroupingUsed() == proto2.getGroupingUsed();
    }

    private static int hashCodeInternal(Int32ToFloatOp proto) {
        return hashCode(proto.getInput());
    }

    private static boolean equalsInternal(Int32ToFloatOp proto1, Int32ToFloatOp proto2) {
        return equals(proto1.getInput(), proto2.getInput());
    }

    private static int hashCodeInternal(ArithmeticFloatOp proto) {
        return Objects.hash(
                proto.getOperationType(),
                hashCode(proto.getInputLhs()),
                hashCode(proto.getInputRhs()));
    }

    private static boolean equalsInternal(ArithmeticFloatOp proto1, ArithmeticFloatOp proto2) {
        return proto1.getOperationType() == proto2.getOperationType()
                && equals(proto1.getInputLhs(), proto2.getInputLhs())
                && equals(proto1.getInputRhs(), proto2.getInputRhs());
    }

    private static int hashCodeInternal(ConditionalFloatOp proto) {
        return Objects.hash(
                hashCode(proto.getCondition()),
                hashCode(proto.getValueIfTrue()),
                hashCode(proto.getValueIfFalse()));
    }

    private static boolean equalsInternal(ConditionalFloatOp proto1, ConditionalFloatOp proto2) {
        return equals(proto1.getCondition(), proto2.getCondition())
                && equals(proto1.getValueIfTrue(), proto2.getValueIfTrue())
                && equals(proto1.getValueIfFalse(), proto2.getValueIfFalse());
    }

    private static int hashCodeInternal(AnimatableDynamicFloat proto) {
        return Objects.hash(hashCode(proto.getInput()), proto.getAnimationSpec().hashCode());
    }

    private static boolean equalsInternal(
            AnimatableDynamicFloat proto1, AnimatableDynamicFloat proto2) {
        return equals(proto1.getInput(), proto2.getInput())
                && Objects.equals(proto1.getAnimationSpec(), proto2.getAnimationSpec());
    }

    private static int hashCodeInternal(ConditionalColorOp proto) {
        return Objects.hash(
                hashCode(proto.getCondition()),
                hashCode(proto.getValueIfTrue()),
                hashCode(proto.getValueIfFalse()));
    }

    private static boolean equalsInternal(ConditionalColorOp proto1, ConditionalColorOp proto2) {
        return equals(proto1.getCondition(), proto2.getCondition())
                && equals(proto1.getValueIfTrue(), proto2.getValueIfTrue())
                && equals(proto1.getValueIfFalse(), proto2.getValueIfFalse());
    }

    private static int hashCodeInternal(AnimatableDynamicColor proto) {
        return Objects.hash(hashCode(proto.getInput()), proto.getAnimationSpec().hashCode());
    }

    private static boolean equalsInternal(
            AnimatableDynamicColor proto1, AnimatableDynamicColor proto2) {
        return equals(proto1.getInput(), proto2.getInput())
                && Objects.equals(proto1.getAnimationSpec(), proto2.getAnimationSpec());
    }

    private static int hashCodeInternal(BetweenDuration proto) {
        return Objects.hash(hashCode(proto.getStartInclusive()), hashCode(proto.getEndExclusive()));
    }

    private static boolean equalsInternal(BetweenDuration proto1, BetweenDuration proto2) {
        return equals(proto1.getStartInclusive(), proto2.getStartInclusive())
                && equals(proto1.getEndExclusive(), proto2.getEndExclusive());
    }

    private static int hashCodeInternal(ConditionalDurationOp proto) {
        return Objects.hash(
                hashCode(proto.getCondition()),
                hashCode(proto.getValueIfTrue()),
                hashCode(proto.getValueIfFalse()));
    }

    private static boolean equalsInternal(
            ConditionalDurationOp proto1, ConditionalDurationOp proto2) {
        return equals(proto1.getCondition(), proto2.getCondition())
                && equals(proto1.getValueIfTrue(), proto2.getValueIfTrue())
                && equals(proto1.getValueIfFalse(), proto2.getValueIfFalse());
    }

    private static int hashCodeInternal(ConditionalInstantOp proto) {
        return Objects.hash(
                hashCode(proto.getCondition()),
                hashCode(proto.getValueIfTrue()),
                hashCode(proto.getValueIfFalse()));
    }

    private static boolean equalsInternal(
            ConditionalInstantOp proto1, ConditionalInstantOp proto2) {
        return equals(proto1.getCondition(), proto2.getCondition())
                && equals(proto1.getValueIfTrue(), proto2.getValueIfTrue())
                && equals(proto1.getValueIfFalse(), proto2.getValueIfFalse());
    }

    private static int hashCodeInternal(InstantToZonedDateTimeOp proto) {
        return Objects.hash(hashCode(proto.getInstant()), proto.getZoneId());
    }

    private static boolean equalsInternal(
            InstantToZonedDateTimeOp proto1, InstantToZonedDateTimeOp proto2) {
        return equals(proto1.getInstant(), proto2.getInstant())
                && Objects.equals(proto1.getZoneId(), proto2.getZoneId());
    }

    private static int hashCodeInternal(ComparisonInt32Op proto) {
        return Objects.hash(
                proto.getOperationType(),
                hashCode(proto.getInputLhs()),
                hashCode(proto.getInputRhs()));
    }

    private static boolean equalsInternal(ComparisonInt32Op proto1, ComparisonInt32Op proto2) {
        return proto1.getOperationType() == proto2.getOperationType()
                && equals(proto1.getInputLhs(), proto2.getInputLhs())
                && equals(proto1.getInputRhs(), proto2.getInputRhs());
    }

    private static int hashCodeInternal(LogicalBoolOp proto) {
        return Objects.hash(
                proto.getOperationType(),
                hashCode(proto.getInputLhs()),
                hashCode(proto.getInputRhs()));
    }

    private static boolean equalsInternal(LogicalBoolOp proto1, LogicalBoolOp proto2) {
        return proto1.getOperationType() == proto2.getOperationType()
                && equals(proto1.getInputLhs(), proto2.getInputLhs())
                && equals(proto1.getInputRhs(), proto2.getInputRhs());
    }

    private static int hashCodeInternal(NotBoolOp proto) {
        return hashCode(proto.getInput());
    }

    private static boolean equalsInternal(NotBoolOp proto1, NotBoolOp proto2) {
        return equals(proto1.getInput(), proto2.getInput());
    }

    private static int hashCodeInternal(ComparisonFloatOp proto) {
        return Objects.hash(
                proto.getOperationType(),
                hashCode(proto.getInputLhs()),
                hashCode(proto.getInputRhs()));
    }

    private static boolean equalsInternal(ComparisonFloatOp proto1, ComparisonFloatOp proto2) {
        return proto1.getOperationType() == proto2.getOperationType()
                && equals(proto1.getInputLhs(), proto2.getInputLhs())
                && equals(proto1.getInputRhs(), proto2.getInputRhs());
    }

    private static int hashCodeInternal(DynamicBuilders.DynamicFloat expr) {
        DynamicFloat proto = expr.toDynamicFloatProto();
        switch (proto.getInnerCase()) {
            case FIXED:
                return proto.getFixed().hashCode();
            case STATE_SOURCE:
                return proto.getStateSource().hashCode();
            case ARITHMETIC_OPERATION:
                return hashCodeInternal(proto.getArithmeticOperation());
            case INT32_TO_FLOAT_OPERATION:
                return hashCodeInternal(proto.getInt32ToFloatOperation());
            case CONDITIONAL_OP:
                return hashCodeInternal(proto.getConditionalOp());
            case ANIMATABLE_FIXED:
                return proto.getAnimatableFixed().hashCode();
            case ANIMATABLE_DYNAMIC:
                return hashCodeInternal(proto.getAnimatableDynamic());
            case INNER_NOT_SET:
                return 0;
        }
        return 0;
    }

    private static boolean equalsInternal(
            DynamicBuilders.DynamicFloat expr1, DynamicBuilders.DynamicFloat expr2) {
        DynamicFloat proto1 = expr1.toDynamicFloatProto();
        DynamicFloat proto2 = expr2.toDynamicFloatProto();
        if (proto1.getInnerCase() != proto2.getInnerCase()) {
            return false;
        }
        switch (proto1.getInnerCase()) {
            case FIXED:
                return proto1.getFixed().equals(proto2.getFixed());
            case STATE_SOURCE:
                return proto1.getStateSource().equals(proto2.getStateSource());
            case ARITHMETIC_OPERATION:
                return equalsInternal(
                        proto1.getArithmeticOperation(), proto2.getArithmeticOperation());
            case INT32_TO_FLOAT_OPERATION:
                return equalsInternal(
                        proto1.getInt32ToFloatOperation(), proto2.getInt32ToFloatOperation());
            case CONDITIONAL_OP:
                return equalsInternal(proto1.getConditionalOp(), proto2.getConditionalOp());
            case ANIMATABLE_FIXED:
                return proto1.getAnimatableFixed().equals(proto2.getAnimatableFixed());
            case ANIMATABLE_DYNAMIC:
                return equalsInternal(proto1.getAnimatableDynamic(), proto2.getAnimatableDynamic());
            case INNER_NOT_SET:
                return true;
        }
        return false;
    }

    private static int hashCodeInternal(DynamicBuilders.DynamicColor expr) {
        DynamicColor proto = expr.toDynamicColorProto();
        switch (proto.getInnerCase()) {
            case FIXED:
                return proto.getFixed().hashCode();
            case STATE_SOURCE:
                return proto.getStateSource().hashCode();
            case ANIMATABLE_FIXED:
                return proto.getAnimatableFixed().hashCode();
            case ANIMATABLE_DYNAMIC:
                return hashCodeInternal(proto.getAnimatableDynamic());
            case CONDITIONAL_OP:
                return hashCodeInternal(proto.getConditionalOp());
            case INNER_NOT_SET:
                return 0;
        }
        return 0;
    }

    private static boolean equalsInternal(
            DynamicBuilders.DynamicColor expr1, DynamicBuilders.DynamicColor expr2) {
        DynamicColor proto1 = expr1.toDynamicColorProto();
        DynamicColor proto2 = expr2.toDynamicColorProto();
        if (proto1.getInnerCase() != proto2.getInnerCase()) {
            return false;
        }
        switch (proto1.getInnerCase()) {
            case FIXED:
                return proto1.getFixed().equals(proto2.getFixed());
            case STATE_SOURCE:
                return proto1.getStateSource().equals(proto2.getStateSource());
            case ANIMATABLE_FIXED:
                return proto1.getAnimatableFixed().equals(proto2.getAnimatableFixed());
            case ANIMATABLE_DYNAMIC:
                return equalsInternal(proto1.getAnimatableDynamic(), proto2.getAnimatableDynamic());
            case CONDITIONAL_OP:
                return equalsInternal(proto1.getConditionalOp(), proto2.getConditionalOp());
            case INNER_NOT_SET:
                return true;
        }
        return false;
    }

    private static int hashCodeInternal(DynamicBuilders.DynamicDuration expr) {
        DynamicDuration proto = expr.toDynamicDurationProto();
        switch (proto.getInnerCase()) {
            case BETWEEN:
                return hashCodeInternal(proto.getBetween());
            case FIXED:
                return proto.getFixed().hashCode();
            case CONDITIONAL_OP:
                return hashCodeInternal(proto.getConditionalOp());
            case STATE_SOURCE:
                return proto.getStateSource().hashCode();
            case INNER_NOT_SET:
                return 0;
        }
        return 0;
    }

    private static boolean equalsInternal(
            DynamicBuilders.DynamicDuration expr1, DynamicBuilders.DynamicDuration expr2) {
        DynamicDuration proto1 = expr1.toDynamicDurationProto();
        DynamicDuration proto2 = expr2.toDynamicDurationProto();
        if (proto1.getInnerCase() != proto2.getInnerCase()) {
            return false;
        }
        switch (proto1.getInnerCase()) {
            case BETWEEN:
                return equalsInternal(proto1.getBetween(), proto2.getBetween());
            case FIXED:
                return proto1.getFixed().equals(proto2.getFixed());
            case CONDITIONAL_OP:
                return equalsInternal(proto1.getConditionalOp(), proto2.getConditionalOp());
            case STATE_SOURCE:
                return proto1.getStateSource().equals(proto2.getStateSource());
            case INNER_NOT_SET:
                return true;
        }
        return false;
    }

    private static int hashCodeInternal(DynamicBuilders.DynamicInstant expr) {
        DynamicInstant proto = expr.toDynamicInstantProto();
        switch (proto.getInnerCase()) {
            case FIXED:
                return proto.getFixed().hashCode();
            case PLATFORM_SOURCE:
                return proto.getPlatformSource().hashCode();
            case CONDITIONAL_OP:
                return hashCodeInternal(proto.getConditionalOp());
            case STATE_SOURCE:
                return proto.getStateSource().hashCode();
            case INNER_NOT_SET:
                return 0;
        }
        return 0;
    }

    private static boolean equalsInternal(
            DynamicBuilders.DynamicInstant expr1, DynamicBuilders.DynamicInstant expr2) {
        DynamicInstant proto1 = expr1.toDynamicInstantProto();
        DynamicInstant proto2 = expr2.toDynamicInstantProto();
        if (proto1.getInnerCase() != proto2.getInnerCase()) {
            return false;
        }
        switch (proto1.getInnerCase()) {
            case FIXED:
                return proto1.getFixed().equals(proto2.getFixed());
            case PLATFORM_SOURCE:
                return proto1.getPlatformSource().equals(proto2.getPlatformSource());
            case CONDITIONAL_OP:
                return equalsInternal(proto1.getConditionalOp(), proto2.getConditionalOp());
            case STATE_SOURCE:
                return proto1.getStateSource().equals(proto2.getStateSource());
            case INNER_NOT_SET:
                return true;
        }
        return false;
    }

    private static int hashCodeInternal(DynamicBuilders.DynamicZonedDateTime expr) {
        DynamicZonedDateTime proto = expr.toDynamicZonedDateTimeProto();
        if (proto.getInnerCase() == DynamicZonedDateTime.InnerCase.INSTANT_TO_ZONED_DATE_TIME) {
            return hashCodeInternal(proto.getInstantToZonedDateTime());
        }
        return 0;
    }

    private static boolean equalsInternal(
            DynamicBuilders.DynamicZonedDateTime expr1,
            DynamicBuilders.DynamicZonedDateTime expr2) {
        DynamicZonedDateTime proto1 = expr1.toDynamicZonedDateTimeProto();
        DynamicZonedDateTime proto2 = expr2.toDynamicZonedDateTimeProto();
        if (proto1.getInnerCase() != proto2.getInnerCase()) {
            return false;
        }
        if (proto1.getInnerCase() == DynamicZonedDateTime.InnerCase.INSTANT_TO_ZONED_DATE_TIME) {
            return equalsInternal(
                    proto1.getInstantToZonedDateTime(), proto2.getInstantToZonedDateTime());
        }
        return true;
    }

    private static int hashCodeInternal(DynamicBuilders.DynamicBool expr) {
        DynamicBool proto = expr.toDynamicBoolProto();
        switch (proto.getInnerCase()) {
            case FIXED:
                return proto.getFixed().hashCode();
            case STATE_SOURCE:
                return proto.getStateSource().hashCode();
            case INT32_COMPARISON:
                return hashCodeInternal(proto.getInt32Comparison());
            case LOGICAL_OP:
                return hashCodeInternal(proto.getLogicalOp());
            case NOT_OP:
                return hashCodeInternal(proto.getNotOp());
            case FLOAT_COMPARISON:
                return hashCodeInternal(proto.getFloatComparison());
            case INNER_NOT_SET:
                return 0;
        }
        return 0;
    }

    private static boolean equalsInternal(
            DynamicBuilders.DynamicBool expr1, DynamicBuilders.DynamicBool expr2) {
        DynamicBool proto1 = expr1.toDynamicBoolProto();
        DynamicBool proto2 = expr2.toDynamicBoolProto();
        if (proto1.getInnerCase() != proto2.getInnerCase()) {
            return false;
        }
        switch (proto1.getInnerCase()) {
            case FIXED:
                return proto1.getFixed().equals(proto2.getFixed());
            case STATE_SOURCE:
                return proto1.getStateSource().equals(proto2.getStateSource());
            case INT32_COMPARISON:
                return equalsInternal(proto1.getInt32Comparison(), proto2.getInt32Comparison());
            case LOGICAL_OP:
                return equalsInternal(proto1.getLogicalOp(), proto2.getLogicalOp());
            case NOT_OP:
                return equalsInternal(proto1.getNotOp(), proto2.getNotOp());
            case FLOAT_COMPARISON:
                return equalsInternal(proto1.getFloatComparison(), proto2.getFloatComparison());
            case INNER_NOT_SET:
                return true;
        }
        return false;
    }

    private static int hashCodeInternal(DynamicBuilders.DynamicString expr) {
        DynamicString proto = expr.toDynamicStringProto();
        switch (proto.getInnerCase()) {
            case FIXED:
                return proto.getFixed().hashCode();
            case STATE_SOURCE:
                return proto.getStateSource().hashCode();
            case INT32_FORMAT_OP:
                return hashCodeInternal(proto.getInt32FormatOp());
            case FLOAT_FORMAT_OP:
                return hashCodeInternal(proto.getFloatFormatOp());
            case CONDITIONAL_OP:
                return hashCodeInternal(proto.getConditionalOp());
            case CONCAT_OP:
                return hashCodeInternal(proto.getConcatOp());
            case INNER_NOT_SET:
                return 0;
        }
        return 0;
    }

    private static boolean equalsInternal(
            DynamicBuilders.DynamicString expr1, DynamicBuilders.DynamicString expr2) {
        DynamicString proto1 = expr1.toDynamicStringProto();
        DynamicString proto2 = expr2.toDynamicStringProto();
        if (proto1.getInnerCase() != proto2.getInnerCase()) {
            return false;
        }
        switch (proto1.getInnerCase()) {
            case FIXED:
                return proto1.getFixed().equals(proto2.getFixed());
            case STATE_SOURCE:
                return proto1.getStateSource().equals(proto2.getStateSource());
            case INT32_FORMAT_OP:
                return equalsInternal(proto1.getInt32FormatOp(), proto2.getInt32FormatOp());
            case FLOAT_FORMAT_OP:
                return equalsInternal(proto1.getFloatFormatOp(), proto2.getFloatFormatOp());
            case CONDITIONAL_OP:
                return equalsInternal(proto1.getConditionalOp(), proto2.getConditionalOp());
            case CONCAT_OP:
                return equalsInternal(proto1.getConcatOp(), proto2.getConcatOp());
            case INNER_NOT_SET:
                return true;
        }
        return false;
    }

    private static int hashCodeInternal(DynamicBuilders.DynamicInt32 expr) {
        DynamicInt32 proto = expr.toDynamicInt32Proto();
        switch (proto.getInnerCase()) {
            case FIXED:
                return proto.getFixed().hashCode();
            case STATE_SOURCE:
                return proto.getStateSource().hashCode();
            case ARITHMETIC_OPERATION:
                return hashCodeInternal(proto.getArithmeticOperation());
            case CONDITIONAL_OP:
                return hashCodeInternal(proto.getConditionalOp());
            case PLATFORM_SOURCE:
                return proto.getPlatformSource().hashCode();
            case FLOAT_TO_INT:
                return hashCodeInternal(proto.getFloatToInt());
            case ANIMATABLE_FIXED:
                return proto.getAnimatableFixed().hashCode();
            case ANIMATABLE_DYNAMIC:
                return hashCodeInternal(proto.getAnimatableDynamic());
            case DURATION_PART:
                return hashCodeInternal(proto.getDurationPart());
            case ZONED_DATE_TIME_PART:
                return hashCodeInternal(proto.getZonedDateTimePart());
            case INNER_NOT_SET:
                return 0;
        }
        return 0;
    }

    private static boolean equalsInternal(
            DynamicBuilders.DynamicInt32 expr1, DynamicBuilders.DynamicInt32 expr2) {
        DynamicInt32 proto1 = expr1.toDynamicInt32Proto();
        DynamicInt32 proto2 = expr2.toDynamicInt32Proto();
        if (proto1.getInnerCase() != proto2.getInnerCase()) {
            return false;
        }
        switch (proto1.getInnerCase()) {
            case FIXED:
                return proto1.getFixed().equals(proto2.getFixed());
            case STATE_SOURCE:
                return proto1.getStateSource().equals(proto2.getStateSource());
            case ARITHMETIC_OPERATION:
                return equalsInternal(
                        proto1.getArithmeticOperation(), proto2.getArithmeticOperation());
            case CONDITIONAL_OP:
                return equalsInternal(proto1.getConditionalOp(), proto2.getConditionalOp());
            case PLATFORM_SOURCE:
                return proto1.getPlatformSource().equals(proto2.getPlatformSource());
            case FLOAT_TO_INT:
                return equalsInternal(proto1.getFloatToInt(), proto2.getFloatToInt());
            case ANIMATABLE_FIXED:
                return proto1.getAnimatableFixed().equals(proto2.getAnimatableFixed());
            case ANIMATABLE_DYNAMIC:
                return equalsInternal(proto1.getAnimatableDynamic(), proto2.getAnimatableDynamic());
            case DURATION_PART:
                return equalsInternal(proto1.getDurationPart(), proto2.getDurationPart());
            case ZONED_DATE_TIME_PART:
                return equalsInternal(proto1.getZonedDateTimePart(), proto2.getZonedDateTimePart());
            case INNER_NOT_SET:
                return true;
        }
        return false;
    }
}
