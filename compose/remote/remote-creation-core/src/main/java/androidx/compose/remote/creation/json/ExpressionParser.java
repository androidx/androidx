/*
 * Copyright 2026 The Android Open Source Project
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
package androidx.compose.remote.creation.json;

import androidx.annotation.RestrictTo;
import androidx.compose.remote.core.RemoteContext;
import androidx.compose.remote.core.operations.Utils;
import androidx.compose.remote.core.operations.utilities.AnimatedFloatExpression;
import androidx.compose.remote.core.operations.utilities.IntegerExpressionEvaluator;
import androidx.compose.remote.creation.RemoteComposeWriter;

import org.json.JSONException;
import org.json.JSONObject;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

/**
 * Infix-to-RPN Shunting-yard expression compiler and tokenizer.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class ExpressionParser {
    private static final boolean DEBUG = false;
    private final RemoteComposeJsonParser mParser;
    private final RemoteComposeWriter mWriter;

    /**
     * Construct an ExpressionParser instance.
     *
     * @param parser the parent JSON parser instance
     */
    ExpressionParser(@NonNull RemoteComposeJsonParser parser) {
        mParser = parser;
        mWriter = parser.getWriter();
    }

    /**
     * Parse a mathematical expression represented as String or JSONObject.
     *
     * @param value the String expression or the JSONObject describing the animation
     * @return the float identifier of the generated expression
     */
    public float parseExpression(@NonNull Object value) {
        if (value instanceof String) {
            String expression = (String) value;
            List<Object> rpn = infixToRpn(expression);
            if (DEBUG) {
                System.out.println(
                        "### parseExpression: " + expression + " rpn size: " + rpn.size());
            }

            float[] ops = new float[rpn.size()];
            for (int i = 0; i < rpn.size(); i++) {
                Object o = rpn.get(i);
                if (o == null) {
                    ops[i] = 0f;
                    continue;
                }
                if (o instanceof Float) {
                    ops[i] = (Float) o;
                } else if (o instanceof Integer) {
                    ops[i] = AnimatedFloatExpression.asNan((Integer) o);
                }
            }
            return mWriter.floatExpression(ops);
        } else if (value instanceof JSONObject) {
            return parseExpressionWithAnim((JSONObject) value);
        }
        return 0f;
    }

    private float parseExpressionWithAnim(@NonNull JSONObject obj) throws JSONException {
        String expression = obj.get("value").toString();
        float duration = (float) obj.optDouble("anim", 1.0);
        List<Object> rpn = infixToRpn(expression);
        float[] ops = new float[rpn.size()];
        for (int i = 0; i < rpn.size(); i++) {
            Object o = rpn.get(i);
            if (o == null) {
                ops[i] = 0f;
                continue;
            }
            if (o instanceof Float) {
                ops[i] = (Float) o;
            } else if (o instanceof Integer) {
                ops[i] = AnimatedFloatExpression.asNan((Integer) o);
            }
        }
        return mWriter.floatExpression(ops, mWriter.anim(duration));
    }

    public List<Object> infixToRpn(String expression) {
        List<Object> output = new ArrayList<>();
        Stack<String> stack = new Stack<>();
        String[] tokens = tokenize(expression);
        if (DEBUG) {
            System.out.println("### infixToRpn tokens: " + java.util.Arrays.toString(tokens));
        }
        boolean lastWasOperator = true;

        for (String token : tokens) {
            if (isNumber(token)) {
                output.add(Float.parseFloat(token));
                lastWasOperator = false;
            } else if (isVariable(token)) {
                output.add(getVariableNan(token));
                lastWasOperator = false;
            } else if (isFunction(token)) {
                stack.push(token);
                lastWasOperator = true;
            } else if (token.equals(",")) {
                while (!stack.isEmpty() && !stack.peek().equals("(")) {
                    String op = stack.pop();
                    if (isOperator(op)) {
                        output.add(getOperatorOp(op));
                    } else if (isFunction(op)) {
                        output.add(getFunctionOp(op));
                    }
                }
                lastWasOperator = true;
            } else if (isOperator(token)) {
                if (token.equals("-") && lastWasOperator) {
                    stack.push("u-");
                } else {
                    while (!stack.isEmpty() && isOperator(stack.peek())) {
                        String topOp = stack.peek();
                        int p1 = precedence(topOp);
                        int p2 = precedence(token);
                        if (p1 > p2 || (p1 == p2 && !token.equals("u-"))) {
                            output.add(getOperatorOp(stack.pop()));
                        } else {
                            break;
                        }
                    }
                    stack.push(token);
                }
                lastWasOperator = true;
            } else if (token.equals("(")) {
                stack.push(token);
                lastWasOperator = true;
            } else if (token.equals(")")) {
                while (!stack.isEmpty() && !stack.peek().equals("(")) {
                    String op = stack.pop();
                    if (isOperator(op)) {
                        output.add(getOperatorOp(op));
                    } else if (isFunction(op)) {
                        output.add(getFunctionOp(op));
                    }
                }
                if (stack.isEmpty()) throw new JSONException("Mismatched parentheses");
                stack.pop();
                if (!stack.isEmpty() && isFunction(stack.peek())) {
                    output.add(getFunctionOp(stack.pop()));
                }
                lastWasOperator = false;
            } else {
                throw new JSONException("Unknown token in expression: " + token);
            }
        }
        while (!stack.isEmpty()) {
            String op = stack.pop();
            if (isOperator(op)) {
                output.add(getOperatorOp(op));
            } else if (isFunction(op)) {
                output.add(getFunctionOp(op));
            }
        }
        if (DEBUG) {
            System.out.println("### infixToRpn output: " + output);
        }
        return output;
    }

    private String[] tokenize(String expression) {
        List<String> tokens = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < expression.length(); i++) {
            char c = expression.charAt(i);
            if (Character.isWhitespace(c)) continue;
            if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9')
                    || c == '_' || c == '.' || c == '$' || c == '@' || c == '[' || c == ']') {
                sb.append(c);
            } else if (c == '(') {
                if (i + 1 < expression.length() && expression.charAt(i + 1) == ')') {
                    sb.append("()");
                    i++;
                } else {
                    if (sb.length() > 0) {
                        tokens.add(sb.toString());
                        sb.setLength(0);
                    }
                    tokens.add("(");
                }
            } else {
                if (sb.length() > 0) {
                    tokens.add(sb.toString());
                    sb.setLength(0);
                }
                tokens.add(String.valueOf(c));
            }
        }
        if (sb.length() > 0) tokens.add(sb.toString());

        // Merge unary "-" + numeric literal into a single negative literal so the wire
        // representation matches DSL code that passes a negative float directly
        List<String> merged = new ArrayList<>(tokens.size());
        for (int i = 0; i < tokens.size(); i++) {
            String t = tokens.get(i);
            if (t.equals("-") && i + 1 < tokens.size() && isNumber(tokens.get(i + 1))) {
                String prev = merged.isEmpty() ? null : merged.get(merged.size() - 1);
                boolean isUnary = prev == null || prev.equals("(") || prev.equals(",") || (
                        prev.length() == 1 && isOperator(prev));
                if (isUnary) {
                    merged.add("-" + tokens.get(i + 1));
                    i++;
                    continue;
                }
            }
            merged.add(t);
        }
        return merged.toArray(new String[0]);
    }

    private boolean isNumber(String token) {
        try {
            Float.parseFloat(token);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * Check if the token represents a recognized system or user variable.
     *
     * @param token the token string to verify
     * @return true if the token is a valid variable reference
     */
    public boolean isVariable(String token) {
        if (RemoteComposeJsonParser.isVariableRef(token)) {
            String name = RemoteComposeJsonParser.getVariableNameFromRef(token);
            if (name == null || name.isEmpty()) return false;
            for (int i = 0; i < name.length(); i++) {
                char c = name.charAt(i);
                if (!Character.isLetterOrDigit(c) && c != '_' && c != '.') {
                    return false;
                }
            }
            return true;
        }
        if (mParser.mVariables.containsKey(token)) return true;
        if (mParser.mDeferredVariables.containsKey(token)) return true;
        if (mParser.mEmittedVariables.containsKey(token)) return true;
        return isSystemVariable(token);
    }

    /**
     * Check if the token represents a recognized system variable.
     */
    public boolean isSystemVariable(String token) {
        return getSystemVariableNan(token) != null;
    }

    /**
     * Resolve a system variable token to its corresponding NaN encoded floating point ID.
     */
    public Float getSystemVariableNan(String token) {
        switch (token) {
            case "a[0]":
                return Utils.asNan(AnimatedFloatExpression.OFFSET + 70);
            case "a[1]":
                return Utils.asNan(AnimatedFloatExpression.OFFSET + 71);
            case "a[2]":
                return Utils.asNan(AnimatedFloatExpression.OFFSET + 72);
            case "rand":
            case "rand()":
                return Utils.asNan(AnimatedFloatExpression.OFFSET + 39);

            // Animation & Time
            case "delta_time":
            case "deltaTime":
            case "deltaTime()":
            case "animationDeltaTime":
            case "animationDeltaTime()":
            case "dt":
                return RemoteContext.FLOAT_ANIMATION_DELTA_TIME;
            case "animationTime":
            case "animationTime()":
            case "animTime":
                return RemoteContext.FLOAT_ANIMATION_TIME;
            case "time":
            case "continuousSec":
            case "continuousSec()":
                return RemoteContext.FLOAT_CONTINUOUS_SEC;
            case "seconds":
            case "timeInSec":
            case "timeInSec()":
                return RemoteContext.FLOAT_TIME_IN_SEC;
            case "timeInMin":
            case "timeInMin()":
                return RemoteContext.FLOAT_TIME_IN_MIN;
            case "timeInHr":
            case "timeInHr()":
                return RemoteContext.FLOAT_TIME_IN_HR;

            // Calendar & Date
            case "month":
            case "calendarMonth":
            case "calendarMonth()":
                return RemoteContext.FLOAT_CALENDAR_MONTH;
            case "weekDay":
            case "weekDay()":
            case "weekday":
            case "dayOfWeek":
                return RemoteContext.FLOAT_WEEK_DAY;
            case "dayOfMonth":
            case "dayOfMonth()":
            case "day":
                return RemoteContext.FLOAT_DAY_OF_MONTH;
            case "dayOfYear":
            case "dayOfYear()":
                return RemoteContext.FLOAT_DAY_OF_YEAR;
            case "year":
            case "year()":
                return RemoteContext.FLOAT_YEAR;
            case "offsetToUtc":
            case "offsetToUtc()":
            case "utcOffset":
                return RemoteContext.FLOAT_OFFSET_TO_UTC;

            // Dimensions
            case "windowWidth":
            case "windowWidth()":
                return RemoteContext.FLOAT_WINDOW_WIDTH;
            case "windowHeight":
            case "windowHeight()":
                return RemoteContext.FLOAT_WINDOW_HEIGHT;
            case "width":
            case "componentWidth":
            case "componentWidth()":
                return mWriter.addComponentWidthValue();
            case "height":
            case "componentHeight":
            case "componentHeight()":
                return mWriter.addComponentHeightValue();

            // Touch
            case "touchX":
            case "touchPosX":
            case "touchPositionX":
            case "touchX()":
                return RemoteContext.FLOAT_TOUCH_POS_X;
            case "touchY":
            case "touchPosY":
            case "touchPositionY":
            case "touchY()":
                return RemoteContext.FLOAT_TOUCH_POS_Y;
            case "touchVelX":
            case "touchVelocityX":
            case "touchVelX()":
                return RemoteContext.FLOAT_TOUCH_VEL_X;
            case "touchVelY":
            case "touchVelocityY":
            case "touchVelY()":
                return RemoteContext.FLOAT_TOUCH_VEL_Y;
            case "touchTime":
            case "touchTime()":
            case "touchEventTime":
            case "touchEventTime()":
                return RemoteContext.FLOAT_TOUCH_EVENT_TIME;

            // Sensors
            case "accelX":
            case "accelX()":
            case "accelerationX":
            case "accelerationX()":
                return RemoteContext.FLOAT_ACCELERATION_X;
            case "accelY":
            case "accelY()":
            case "accelerationY":
            case "accelerationY()":
                return RemoteContext.FLOAT_ACCELERATION_Y;
            case "accelZ":
            case "accelZ()":
            case "accelerationZ":
            case "accelerationZ()":
                return RemoteContext.FLOAT_ACCELERATION_Z;
            case "gyroX":
            case "gyroX()":
            case "gyroRotX":
            case "gyroRotX()":
            case "gyroRotationX":
                return RemoteContext.FLOAT_GYRO_ROT_X;
            case "gyroY":
            case "gyroY()":
            case "gyroRotY":
            case "gyroRotY()":
            case "gyroRotationY":
                return RemoteContext.FLOAT_GYRO_ROT_Y;
            case "gyroZ":
            case "gyroZ()":
            case "gyroRotZ":
            case "gyroRotZ()":
            case "gyroRotationZ":
                return RemoteContext.FLOAT_GYRO_ROT_Z;
            case "magneticX":
            case "magneticX()":
                return RemoteContext.FLOAT_MAGNETIC_X;
            case "magneticY":
            case "magneticY()":
                return RemoteContext.FLOAT_MAGNETIC_Y;
            case "magneticZ":
            case "magneticZ()":
                return RemoteContext.FLOAT_MAGNETIC_Z;
            case "light":
            case "light()":
            case "lightLevel":
                return RemoteContext.FLOAT_LIGHT;

            // System
            case "density":
            case "density()":
                return RemoteContext.FLOAT_DENSITY;
            case "apiLevel":
            case "apiLevel()":
                return RemoteContext.FLOAT_API_LEVEL;
            case "fontSize":
            case "fontSize()":
                return RemoteContext.FLOAT_FONT_SIZE;
            default:
                return null;
        }
    }

    /**
     * Resolve a variable name to its corresponding NaN encoded floating point ID.
     *
     * @param token the variable reference string
     * @return the NaN encoded variable identifier
     */
    public float getVariableNan(String token) {
        Float systemVar = getSystemVariableNan(token);
        if (systemVar != null) {
            return systemVar;
        }
        if (RemoteComposeJsonParser.isVariableRef(token)) {
            String name = RemoteComposeJsonParser.getVariableNameFromRef(token);
            if (mParser.mIntegerVariables.containsKey(name)) {
                long intId = mParser.mIntegerVariables.get(name);
                return Utils.asNan((int) (intId & 0xFFFFFFFFL));
            }
            Float id = mParser.mVariables.get(name);
            if (id != null) return id;
            if (mParser.mDeferredVariables.containsKey(name)) {
                return mParser.resolveDeferredVariable(name);
            }
            throw new JSONException("Variable not found: " + name);
        }
        if (mParser.mIntegerVariables.containsKey(token)) {
            long intId = mParser.mIntegerVariables.get(token);
            return Utils.asNan((int) (intId & 0xFFFFFFFFL));
        }
        if (mParser.mVariables.containsKey(token)) {
            return mParser.mVariables.get(token);
        }
        if (mParser.mDeferredVariables.containsKey(token)) {
            return mParser.resolveDeferredVariable(token);
        }
        throw new JSONException("Unknown variable: " + token);
    }

    public long parseIntegerExpression(@NonNull String expression) {
        List<Object> rpn = infixToIntegerRpn(expression);
        long[] ops = new long[rpn.size()];
        for (int i = 0; i < rpn.size(); i++) {
            Object o = rpn.get(i);
            if (o instanceof Long) {
                ops[i] = (Long) o;
            } else if (o instanceof Integer) {
                ops[i] = (long) (Integer) o;
            } else if (o instanceof Float) {
                float f = (Float) o;
                if (Float.isNaN(f)) {
                    int rawId = Utils.idFromNan(f);
                    ops[i] = (long) rawId + 0x100000000L;
                } else {
                    ops[i] = (long) f;
                }
            } else if (o instanceof Number) {
                ops[i] = ((Number) o).longValue();
            }
        }
        return mWriter.integerExpression(ops);
    }

    public List<Object> infixToIntegerRpn(String expression) {
        List<Object> output = new ArrayList<>();
        Stack<String> stack = new Stack<>();
        String[] tokens = tokenize(expression);
        boolean lastWasOperator = true;

        for (String token : tokens) {
            if (isNumber(token)) {
                output.add((long) Integer.parseInt(token));
                lastWasOperator = false;
            } else if (isVariable(token)) {
                String name = RemoteComposeJsonParser.isVariableRef(token)
                        ? RemoteComposeJsonParser.getVariableNameFromRef(token) : token;
                if (mParser.mIntegerVariables.containsKey(name)) {
                    output.add(mParser.mIntegerVariables.get(name));
                } else if (mParser.mVariables.containsKey(name)) {
                    Float f = mParser.mVariables.get(name);
                    if (f != null && Float.isNaN(f)) {
                        int rawId = Utils.idFromNan(f);
                        output.add((long) rawId + 0x100000000L);
                    } else {
                        output.add(getVariableNan(token));
                    }
                } else {
                    output.add(mParser.resolveIntegerVariable(token));
                }
                lastWasOperator = false;
            } else if (isFunction(token)) {
                stack.push(token);
                lastWasOperator = true;
            } else if (token.equals(",")) {
                while (!stack.isEmpty() && !stack.peek().equals("(")) {
                    String op = stack.pop();
                    if (isOperator(op)) {
                        output.add(getIntegerOperatorOp(op));
                    } else if (isFunction(op)) {
                        output.add(getIntegerFunctionOp(op));
                    }
                }
                lastWasOperator = true;
            } else if (isOperator(token)) {
                if (token.equals("-") && lastWasOperator) {
                    stack.push("u-");
                } else {
                    while (!stack.isEmpty() && isOperator(stack.peek())) {
                        String topOp = stack.peek();
                        int p1 = precedence(topOp);
                        int p2 = precedence(token);
                        if (p1 > p2 || (p1 == p2 && !token.equals("u-"))) {
                            output.add(getIntegerOperatorOp(stack.pop()));
                        } else {
                            break;
                        }
                    }
                    stack.push(token);
                }
                lastWasOperator = true;
            } else if (token.equals("(")) {
                stack.push(token);
                lastWasOperator = true;
            } else if (token.equals(")")) {
                while (!stack.isEmpty() && !stack.peek().equals("(")) {
                    String op = stack.pop();
                    if (isOperator(op)) {
                        output.add(getIntegerOperatorOp(op));
                    } else if (isFunction(op)) {
                        output.add(getIntegerFunctionOp(op));
                    }
                }
                if (stack.isEmpty()) throw new JSONException("Mismatched parentheses");
                stack.pop();
                if (!stack.isEmpty() && isFunction(stack.peek())) {
                    output.add(getIntegerFunctionOp(stack.pop()));
                }
                lastWasOperator = false;
            } else {
                throw new JSONException("Unknown token in expression: " + token);
            }
        }
        while (!stack.isEmpty()) {
            String op = stack.pop();
            if (isOperator(op)) {
                output.add(getIntegerOperatorOp(op));
            } else if (isFunction(op)) {
                output.add(getIntegerFunctionOp(op));
            }
        }
        return output;
    }

    private long getIntegerOperatorOp(String op) {
        switch (op) {
            case "+":
                return 0x100000000L + IntegerExpressionEvaluator.I_ADD;
            case "-":
                return 0x100000000L + IntegerExpressionEvaluator.I_SUB;
            case "*":
                return 0x100000000L + IntegerExpressionEvaluator.I_MUL;
            case "/":
                return 0x100000000L + IntegerExpressionEvaluator.I_DIV;
            case "%":
                return 0x100000000L + IntegerExpressionEvaluator.I_MOD;
            case "u-":
                return 0x100000000L + IntegerExpressionEvaluator.I_NEG;
            default:
                throw new JSONException("Unsupported integer operator: " + op);
        }
    }

    private long getIntegerFunctionOp(String fn) {
        switch (fn) {
            case "abs":
                return 0x100000000L + IntegerExpressionEvaluator.I_ABS;
            case "min":
                return 0x100000000L + IntegerExpressionEvaluator.I_MIN;
            case "max":
                return 0x100000000L + IntegerExpressionEvaluator.I_MAX;
            case "clamp":
                return 0x100000000L + IntegerExpressionEvaluator.I_CLAMP;
            default:
                throw new JSONException("Unsupported integer function: " + fn);
        }
    }

    private boolean isFunction(String token) {
        return token.equals("sin") || token.equals("cos") || token.equals("sqrt") || token.equals(
                "abs") || token.equals("min") || token.equals("max") || token.equals("pow")
                || token.equals("tan") || token.equals("asin") || token.equals("acos")
                || token.equals("atan") || token.equals("atan2") || token.equals("floor")
                || token.equals("ceil") || token.equals("log") || token.equals("ln")
                || token.equals("round") || token.equals("sign") || token.equals("lerp")
                || token.equals("step") || token.equals("smooth_step") || token.equals("clamp")
                || token.equals("mad") || token.equals("ping_pong") || token.equals("fract")
                || token.equals("exp") || token.equals("hypot") || token.equals("square")
                || token.equals("rand") || token.equals("arrayMin") || token.equals("arrayMax")
                || token.equals("spline") || token.equals("arraySpline") || token.equals(
                "splineLoop") || token.equals("anim") || token.equals("arrayLength")
                || token.equals("arraySum") || token.equals("arraySumSqr") || token.equals(
                "arraySumXY") || token.equals("arrayGet") || token.equals("ifElse");
    }

    private int getFunctionOp(String token) {
        switch (token) {
            case "sin":
                return AnimatedFloatExpression.OFFSET + 18;
            case "cos":
                return AnimatedFloatExpression.OFFSET + 19;
            case "tan":
                return AnimatedFloatExpression.OFFSET + 20;
            case "asin":
                return AnimatedFloatExpression.OFFSET + 21;
            case "acos":
                return AnimatedFloatExpression.OFFSET + 22;
            case "atan":
                return AnimatedFloatExpression.OFFSET + 23;
            case "atan2":
                return AnimatedFloatExpression.OFFSET + 24;
            case "sqrt":
                return AnimatedFloatExpression.OFFSET + 9;
            case "abs":
                return AnimatedFloatExpression.OFFSET + 10;
            case "pow":
                return AnimatedFloatExpression.OFFSET + 8;
            case "min":
                return AnimatedFloatExpression.OFFSET + 6;
            case "max":
                return AnimatedFloatExpression.OFFSET + 7;
            case "floor":
                return AnimatedFloatExpression.OFFSET + 14;
            case "ceil":
                return AnimatedFloatExpression.OFFSET + 31;
            case "log":
                return AnimatedFloatExpression.OFFSET + 15;
            case "ln":
                return AnimatedFloatExpression.OFFSET + 16;
            case "sign":
                return AnimatedFloatExpression.OFFSET + 11;
            case "round":
                return AnimatedFloatExpression.OFFSET + 17;
            case "lerp":
                return AnimatedFloatExpression.OFFSET + 49;
            case "step":
                return AnimatedFloatExpression.OFFSET + 44;
            case "smooth_step":
                return AnimatedFloatExpression.OFFSET + 50;
            case "clamp":
                return AnimatedFloatExpression.OFFSET + 27;
            case "ifElse":
                return AnimatedFloatExpression.OFFSET + 26;
            case "mad":
                return AnimatedFloatExpression.OFFSET + 25;
            case "ping_pong":
                return AnimatedFloatExpression.OFFSET + 54;
            case "fract":
                return AnimatedFloatExpression.OFFSET + 53;
            case "exp":
                return AnimatedFloatExpression.OFFSET + 13;
            case "hypot":
                return AnimatedFloatExpression.OFFSET + 47;
            case "square":
                return AnimatedFloatExpression.OFFSET + 45;
            case "rand":
                return AnimatedFloatExpression.OFFSET + 39;
            case "arrayMin":
                return AnimatedFloatExpression.OFFSET + 34;
            case "arrayMax":
                return AnimatedFloatExpression.OFFSET + 33;
            case "arrayLength":
                return AnimatedFloatExpression.OFFSET + 37;
            case "arraySum":
                return AnimatedFloatExpression.OFFSET + 35;
            case "arraySumSqr":
                return AnimatedFloatExpression.OFFSET + 78;
            case "arraySumXY":
                return AnimatedFloatExpression.OFFSET + 77;
            case "arrayGet":
                return AnimatedFloatExpression.OFFSET + 32;
            case "spline":
            case "arraySpline":
                return AnimatedFloatExpression.OFFSET + 38;
            case "splineLoop":
                return AnimatedFloatExpression.OFFSET + 75;
            case "anim":
                return AnimatedFloatExpression.OFFSET + 256; // PLACEHOLDER
            default:
                return 0;
        }
    }

    private boolean isOperator(String token) {
        return token.equals("+") || token.equals("-") || token.equals("*") || token.equals("/")
                || token.equals("%") || token.equals("u-");
    }

    private int getOperatorOp(String token) {
        switch (token) {
            case "+":
                return AnimatedFloatExpression.OFFSET + 1;
            case "-":
                return AnimatedFloatExpression.OFFSET + 2;
            case "*":
                return AnimatedFloatExpression.OFFSET + 3;
            case "/":
                return AnimatedFloatExpression.OFFSET + 4;
            case "%":
                return AnimatedFloatExpression.OFFSET + 5;
            case "u-":
                return AnimatedFloatExpression.OFFSET + 73; // CHANGE_SIGN
            default:
                return 0;
        }
    }

    private int precedence(String token) {
        switch (token) {
            case "+":
            case "-":
                return 1;
            case "*":
            case "/":
            case "%":
                return 2;
            case "u-":
                return 3;
            default:
                return 0;
        }
    }
}
