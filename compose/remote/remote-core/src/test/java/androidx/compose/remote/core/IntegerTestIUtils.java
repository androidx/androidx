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
package androidx.compose.remote.core;

import static androidx.compose.remote.core.operations.utilities.IntegerExpressionEvaluator.I_ABS;
import static androidx.compose.remote.core.operations.utilities.IntegerExpressionEvaluator.I_ADD;
import static androidx.compose.remote.core.operations.utilities.IntegerExpressionEvaluator.I_AND;
import static androidx.compose.remote.core.operations.utilities.IntegerExpressionEvaluator.I_CLAMP;
import static androidx.compose.remote.core.operations.utilities.IntegerExpressionEvaluator.I_COPY_SIGN;
import static androidx.compose.remote.core.operations.utilities.IntegerExpressionEvaluator.I_DECR;
import static androidx.compose.remote.core.operations.utilities.IntegerExpressionEvaluator.I_DIV;
import static androidx.compose.remote.core.operations.utilities.IntegerExpressionEvaluator.I_IFELSE;
import static androidx.compose.remote.core.operations.utilities.IntegerExpressionEvaluator.I_INCR;
import static androidx.compose.remote.core.operations.utilities.IntegerExpressionEvaluator.I_MAD;
import static androidx.compose.remote.core.operations.utilities.IntegerExpressionEvaluator.I_MAX;
import static androidx.compose.remote.core.operations.utilities.IntegerExpressionEvaluator.I_MIN;
import static androidx.compose.remote.core.operations.utilities.IntegerExpressionEvaluator.I_MOD;
import static androidx.compose.remote.core.operations.utilities.IntegerExpressionEvaluator.I_MUL;
import static androidx.compose.remote.core.operations.utilities.IntegerExpressionEvaluator.I_NEG;
import static androidx.compose.remote.core.operations.utilities.IntegerExpressionEvaluator.I_NOT;
import static androidx.compose.remote.core.operations.utilities.IntegerExpressionEvaluator.I_OR;
import static androidx.compose.remote.core.operations.utilities.IntegerExpressionEvaluator.I_SHL;
import static androidx.compose.remote.core.operations.utilities.IntegerExpressionEvaluator.I_SHR;
import static androidx.compose.remote.core.operations.utilities.IntegerExpressionEvaluator.I_SIGN;
import static androidx.compose.remote.core.operations.utilities.IntegerExpressionEvaluator.I_SUB;
import static androidx.compose.remote.core.operations.utilities.IntegerExpressionEvaluator.I_USHR;
import static androidx.compose.remote.core.operations.utilities.IntegerExpressionEvaluator.I_VAR1;
import static androidx.compose.remote.core.operations.utilities.IntegerExpressionEvaluator.I_VAR2;
import static androidx.compose.remote.core.operations.utilities.IntegerExpressionEvaluator.I_XOR;

public class IntegerTestIUtils {
    public static final long L_ADD = 0x100000000L + I_ADD;
    public static final long L_SUB = 0x100000000L + I_SUB;
    public static final long L_MUL = 0x100000000L + I_MUL;
    public static final long L_DIV = 0x100000000L + I_DIV;
    public static final long L_MOD = 0x100000000L + I_MOD;
    public static final long L_SHL = 0x100000000L + I_SHL;
    public static final long L_SHR = 0x100000000L + I_SHR;
    public static final long L_USHR = 0x100000000L + I_USHR;
    public static final long L_OR = 0x100000000L + I_OR;
    public static final long L_AND = 0x100000000L + I_AND;
    public static final long L_XOR = 0x100000000L + I_XOR;
    public static final long L_COPY_SIGN = 0x100000000L + I_COPY_SIGN;
    public static final long L_MIN = 0x100000000L + I_MIN;
    public static final long L_MAX = 0x100000000L + I_MAX;

    public static final long L_NEG = 0x100000000L + I_NEG;
    public static final long L_ABS = 0x100000000L + I_ABS;
    public static final long L_INCR = 0x100000000L + I_INCR;
    public static final long L_DECR = 0x100000000L + I_DECR;
    public static final long L_NOT = 0x100000000L + I_NOT;
    public static final long L_SIGN = 0x100000000L + I_SIGN;

    public static final long L_CLAMP = 0x100000000L + I_CLAMP;
    public static final long L_IFELSE = 0x100000000L + I_IFELSE;
    public static final long L_MAD = 0x100000000L + I_MAD;

    public static final long L_VAR1 = 0x100000000L + I_VAR1;
    public static final long L_VAR2 = 0x100000000L + I_VAR2;
}
