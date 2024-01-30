/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.wear.protolayout.expression;

import androidx.annotation.NonNull;
import androidx.wear.protolayout.expression.DynamicBuilders.DynamicType;

import java.util.function.Function;

/**
 * Intermediate scopes used inside of {@code onCondition} expressions. This is to enable
 * functionality such as {@code
 * DynamicString.onCondition(...).use(constant("Foo")).elseUse(constant("Bar"))}.
 */
public class ConditionScopes {
    private ConditionScopes() {}

    interface ConditionBuilder<T extends DynamicType> {
        T buildCondition(T trueValue, T falseValue);
    }

    /**
     * Condition scope to allow binding the true value in a onConditional expression. {@code RawT}
     * is the native Java type that can be used when constructing a constant T (e.g. String for
     * DynamicString).
     */
    public static class ConditionScope<T extends DynamicType, RawT> {
        private final ConditionBuilder<T> conditionBuilder;
        private final Function<RawT, T> rawTypeMapper;

        ConditionScope(ConditionBuilder<T> conditionBuilder, Function<RawT, T> rawTypeMapper) {
            this.conditionBuilder = conditionBuilder;
            this.rawTypeMapper = rawTypeMapper;
        }

        /** Sets the value to use as the value when true in a conditional expression. */
        public @NonNull IfTrueScope<T, RawT> use(T valueWhenTrue) {
            return new IfTrueScope<>(valueWhenTrue, conditionBuilder, rawTypeMapper);
        }

        /** Sets the value to use as the value when true in a conditional expression. */
        public @NonNull IfTrueScope<T, RawT> use(RawT valueWhenTrue) {
            return use(rawTypeMapper.apply(valueWhenTrue));
        }
    }

    /**
     * Condition scope to allow binding the true value in a onConditional expression, yielding a
     * resulting Dynamic value.{@code RawT} is the native Java type that can be used when
     * constructing a constant T (e.g. String for DynamicString).
     */
    public static class IfTrueScope<T extends DynamicType, RawT> {
        private final T ifTrueValue;
        private final ConditionBuilder<T> conditionBuilder;
        private final Function<RawT, T> rawTypeMapper;

        IfTrueScope(
                T ifTrueValue,
                ConditionBuilder<T> conditionBuilder,
                Function<RawT, T> rawTypeMapper) {
            this.ifTrueValue = ifTrueValue;
            this.conditionBuilder = conditionBuilder;
            this.rawTypeMapper = rawTypeMapper;
        }

        /** Sets the value to use as the value when false in a conditional expression. */
        public T elseUse(T valueWhenFalse) {
            return conditionBuilder.buildCondition(ifTrueValue, valueWhenFalse);
        }

        /** Sets the value to use as the value when false in a conditional expression. */
        public T elseUse(RawT valueWhenFalse) {
            return elseUse(rawTypeMapper.apply(valueWhenFalse));
        }
    }
}
