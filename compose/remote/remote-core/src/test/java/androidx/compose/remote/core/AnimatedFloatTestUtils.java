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

import static androidx.compose.remote.core.operations.utilities.AnimatedFloatExpression.OFFSET;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Stack;

public class AnimatedFloatTestUtils {

    static float[] expToFloatExp(@NonNull String str) {
        String[] sp = infixToPostfix(str).split(" ");
        // System.out.println(Arrays.toString(sp));
        float[] ret = new float[sp.length];
        for (int i = 0; i < sp.length; i++) {
            ret[i] = toFloatToken(sp[i]);
        }
        return ret;
    }

    static float toFloatToken(@NonNull String token) {
        char c = token.charAt(0);
        if (Character.isDigit(c) || c == '-' || c == '.') return Float.parseFloat(token);
        return sTokenToNan.get(token);
    }

    /**
     * Encode an int value as a float NaN
     *
     * @param v
     * @return
     */
    public static float asNaN(int v) {
        return Float.intBitsToFloat((OFFSET + v) | -0x800000);
    }

    @NonNull static HashMap<String, Float> sTokenToNan = new HashMap<>();

    static {
        int k = 0;
        sTokenToNan.put("NOP", asNaN(k++));
        sTokenToNan.put("+", asNaN(k++));
        sTokenToNan.put("-", asNaN(k++));
        sTokenToNan.put("*", asNaN(k++));
        sTokenToNan.put("/", asNaN(k++));
        sTokenToNan.put("%", asNaN(k++));
        sTokenToNan.put("min", asNaN(k++));
        sTokenToNan.put("max", asNaN(k++));
        sTokenToNan.put("pow", asNaN(k++));
        sTokenToNan.put("sqrt", asNaN(k++));
        sTokenToNan.put("abs", asNaN(k++));
        sTokenToNan.put("sign", asNaN(k++));
        sTokenToNan.put("copySign", asNaN(k++));
        sTokenToNan.put("exp", asNaN(k++));
        sTokenToNan.put("floor", asNaN(k++));
        sTokenToNan.put("log", asNaN(k++));
        sTokenToNan.put("ln", asNaN(k++));
        sTokenToNan.put("round", asNaN(k++));
        sTokenToNan.put("sin", asNaN(k++));
        sTokenToNan.put("cos", asNaN(k++));
        sTokenToNan.put("tan", asNaN(k++));
        sTokenToNan.put("asin", asNaN(k++));
        sTokenToNan.put("acos", asNaN(k++));
        sTokenToNan.put("atan", asNaN(k++));
        sTokenToNan.put("atan2", asNaN(k++));
        sTokenToNan.put("mad", asNaN(k++));
        sTokenToNan.put("ifElse", asNaN(k++));
        sTokenToNan.put("a[0]", asNaN(k++));
        sTokenToNan.put("a[1]", asNaN(k++));
        sTokenToNan.put("a[2]", asNaN(k++));
        sTokenToNan.put("log2", asNaN(k++));
        sTokenToNan.put("inv", asNaN(k++));
        sTokenToNan.put("fract", asNaN(k++));
        sTokenToNan.put("ping_pong", asNaN(k++));
    }

    /**
     * Utility to convert an infix expression to postfix
     *
     * @param exp an infix expression as a string
     * @return a postfix expression as a string
     */
    @NonNull
    public static String infixToPostfix(@NonNull String exp) {
        StringBuilder result = new StringBuilder();
        Stack<String> stack = new Stack<>();
        List<String> tokens = tokenize(exp);

        for (String token : tokens) {
            if (isNumeric(token)) {
                result.append(token).append(" ");
            } else if (isFunction(token)) {
                stack.push(token);
            } else if (token.equals("(")) {
                stack.push(token);
            } else if (token.equals(")")) {
                while (!stack.isEmpty() && !stack.peek().equals("(")) {
                    result.append(stack.pop()).append(" ");
                }
                if (!stack.isEmpty() && stack.peek().equals("(")) {
                    stack.pop(); // popping the '('
                }
                if (!stack.isEmpty() && isFunction(stack.peek())) {
                    result.append(stack.pop()).append(" ");
                }
            } else if (token.equals(",")) {
                // We do nothing at commas, just ensure we've closed off the last argument
            } else { // An operator is encountered
                while (!stack.isEmpty() && precedence(token) <= precedence(stack.peek())) {
                    if (stack.peek().equals("(")) break;
                    result.append(stack.pop()).append(" ");
                }
                stack.push(token);
            }
        }

        // Pop all the operators from the stack
        while (!stack.isEmpty()) {
            result.append(stack.pop()).append(" ");
        }
        return result.toString();
    }

    private static boolean isNumeric(@Nullable String str) {
        if (str == null) {
            return false;
        }
        return str.matches("-?\\d+(\\.\\d+)?"); // Match a number with optional '-' and decimal.
    }

    @NonNull
    static HashSet<String> sMap =
            new HashSet<>(
                    Arrays.asList(
                            "min",
                            "max",
                            "pow",
                            "sqrt",
                            "abs",
                            "sign",
                            "CopySign",
                            "exp",
                            "floor",
                            "log",
                            "ln",
                            "round",
                            "sin",
                            "cos",
                            "tan",
                            "asin",
                            "acos",
                            "atan",
                            "atan2"));

    private static boolean isFunction(String str) {
        return sMap.contains(str);
    }

    private static int precedence(@NonNull String ch) {
        if (isFunction(ch)) {
            return 4; // Function has higher precedence than operators
        }
        switch (ch) {
            case "+":
            case "-":
                return 1;
            case "*":
            case "/":
                return 2;
        }
        return -1;
    }

    // A utility method to tokenize the input expression
    @NonNull
    private static List<String> tokenize(@NonNull String exp) {
        List<String> tokens = new ArrayList<>();
        StringBuilder token = new StringBuilder();
        char[] expArray = exp.toCharArray();

        for (int i = 0; i < expArray.length; i++) {
            char c = expArray[i];
            if (Character.isWhitespace(c)) {
                if (token.length() > 0) {
                    tokens.add(token.toString());
                    token = new StringBuilder();
                }
            } else if (Character.isDigit(c) || c == '.' || (token.length() == 0 && c == '-')) {
                token.append(c);
            } else if (Character.isLetter(c)) {
                if (token.length() > 0 && !Character.isLetter(token.charAt(0))) {
                    tokens.add(token.toString());
                    token = new StringBuilder();
                }
                token.append(c);
            } else if (c == '(' || c == ')' || c == '+' || c == '-' || c == '*' || c == '/'
                    || c == ',') {
                if (token.length() > 0) {
                    tokens.add(token.toString());
                    token = new StringBuilder();
                }
                tokens.add(String.valueOf(c));
            }
        }
        if (token.length() > 0) {
            tokens.add(token.toString());
        }
        return tokens;
    }
}
