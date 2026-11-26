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

package androidx.compose.remote.creation;

import androidx.annotation.RestrictTo;
import androidx.compose.remote.core.Limits;
import androidx.compose.remote.core.operations.Utils;
import androidx.compose.remote.core.operations.utilities.AnimatedFloatExpression;
import androidx.compose.remote.core.operations.utilities.NanMap;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Rewrites a float-expression (reverse-polish {@code float[]}) so that:
 *
 * <ol>
 *   <li><b>It fits.</b> Every emitted expression stays within {@link Limits#MAX_EXPRESSION_SIZE}
 *       tokens. Float expressions are stored <em>inlined</em> (no sub-expression sharing), so a
 *       deep/often-reused tree easily overflows the 32-token cap and throws at document-build time.
 *       Over-large nodes are spilled into hidden variables (one extra {@code floatExpression} op
 *       per
 *       spill, referenced by its 1-token id), bottom-up, until each piece fits.
 *   <li><b>It shares.</b> Common sub-expressions are computed once. Teams routinely re-type the
 *   same
 *       sub-formula (e.g. {@code width / 2}) across many equations instead of binding it to a
 *       variable; this performs that refactor automatically — within one expression (always) and
 *       across expressions at the document top level (a persistent cache).
 * </ol>
 *
 * <p>It is intentionally conservative: if the expression contains any operator outside a known
 * <b>pure, fixed-arity</b> whitelist (random, noise, array/collection, stack, register, or unknown
 * ops), it <b>bails</b> and returns the input unchanged — so it can never corrupt an expression it
 * doesn't fully understand. The common case (arithmetic, trig, min/max/clamp/lerp, floor/fract, …)
 * is fully covered.
 *
 * <p><b>Loop scoping.</b> A sub-expression that references a loop index variable is only valid
 * inside that loop. The cross-expression cache is therefore disabled while inside a loop
 * ({@link #enterLoop()}/{@link #exitLoop()} bracket it): in-loop spills/CSE still happen (their
 * temp
 * ops are emitted in-region, so always correct), but their ids are never offered to expressions
 * outside the loop. Intra-expression sharing always works.
 *
 * <p>Not thread-safe; intended for single-threaded document authoring. Decoupled from the writer
 * via
 * {@link Emitter} so it can be unit-tested without a player.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public final class FloatExpressionOptimizer {

    /** Emits a temp sub-expression (already within the size limit) and returns its NaN id token. */
    public interface Emitter {
        /**
         * Emit a sub-expression.
         * @param expr expression to emit
         * @return NaN id of the emitted expression
         */
        float emit(float @NonNull [] expr);
    }

    private static final int BUDGET = Limits.MAX_EXPRESSION_SIZE;
    private static final int OFF = AnimatedFloatExpression.OFFSET;

    /** opId - OFFSET -> arity, for the pure fixed-arity ops we can safely split. -1 = not
     * allowed. */
    private static final int[] ARITY = buildArity();

    private static int[] buildArity() {
        int[] a = new int[AnimatedFloatExpression.LAST_OP - OFF + 2];
        Arrays.fill(a, -1);
        // 1-arg: sqrt abs sign exp floor log ln round sin cos tan asin acos atan cbrt deg rad ceil
        //        square log2 inv fract changeSign
        for (int i : new int[]{9, 10, 11, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 28, 29, 30,
                31, 45, 51, 52, 53, 73}) {
            a[i] = 1;
        }
        // 2-arg: add sub mul div mod min max pow copySign atan2 squareSum step hypot pingpong
        for (int i : new int[]{1, 2, 3, 4, 5, 6, 7, 8, 12, 24, 43, 44, 47, 54}) {
            a[i] = 2;
        }
        // 3-arg: mad ifElse clamp lerp smoothStep
        for (int i : new int[]{25, 26, 27, 49, 50}) {
            a[i] = 3;
        }
        return a;
    }

    private static int arity(int opId) {
        int idx = opId - OFF;
        if (idx < 0 || idx >= ARITY.length) {
            return -1;
        }
        return ARITY[idx];
    }

    // Persistent cross-expression cache (document-global sub-expressions); used only at loop
    // depth 0.
    private final Map<String, Float> mGlobalCache = new HashMap<>();
    private final Set<String> mGlobalSeen = new HashSet<>();
    private int mLoopDepth = 0;

    // Per-optimize() working state.
    private @Nullable Emitter mEmitter;
    private @Nullable Map<String, Float> mLocalCache;
    private @Nullable Map<String, Integer> mLocalCount;

    /** Bracket a loop: while inside, the cross-expression cache is suspended (see class doc). */
    public void enterLoop() {
        mLoopDepth++;
    }

    /** End a loop started with {@link #enterLoop()}. */
    public void exitLoop() {
        if (mLoopDepth > 0) {
            mLoopDepth--;
        }
    }

    private boolean global() {
        return mLoopDepth == 0;
    }

    private static final FloatExpressionOptimizer sInstance = new FloatExpressionOptimizer();

    /**
     * Optimize a float expression.
     * @param writer the writer
     * @param value the expression to optimize
     * @param animation the animation for the expression
     * @return the optimized expression
     */
    public static float optimizeAndAdd(
            @NonNull RemoteComposeWriter writer,
            float @NonNull [] value,
            float @Nullable [] animation) {
        float[] optimized = sInstance.optimize(value, expr -> {
            int id = writer.mState.cacheData(expr);
            writer.mBuffer.addAnimatedFloat(id, expr, animation);
            return Utils.asNan(id);
        });
        int finalId = writer.mState.cacheData(optimized);
        writer.mBuffer.addAnimatedFloat(finalId, optimized, animation);
        return Utils.asNan(finalId);
    }

    /**
     * Return an equivalent expression whose top-level array (and every temp it emits via [emitter])
     * is within {@link Limits#MAX_EXPRESSION_SIZE}, sharing common sub-expressions. Returns [value]
     * unchanged if there is nothing to do or the expression isn't safe to rewrite.
     */
    public float @NonNull [] optimize(float @NonNull [] value, @NonNull Emitter emitter) {
        if (value.length <= 1) {
            return value;
        }
        Node root = parse(value);
        if (root == null) {
            return value; // bail: contains something we don't fully understand
        }
        mEmitter = emitter;
        mLocalCache = new HashMap<>();
        mLocalCount = new HashMap<>();
        count(root, value, mLocalCount);
        float[] out = compile(root, value);
        mEmitter = null;
        mLocalCache = null;
        mLocalCount = null;
        return out;
    }

    /** Parse RPN into a tree, or null to bail (unknown/disallowed op, data var, malformed). */
    private @Nullable Node parse(float @NonNull [] a) {
        Node[] stack = new Node[a.length];
        int sp = 0;
        for (int i = 0; i < a.length; i++) {
            float t = a[i];
            if (AnimatedFloatExpression.isMathOperator(t)) {
                int op = AnimatedFloatExpression.fromNaN(t);
                int k = arity(op);
                if (k < 0 || sp < k) {
                    return null;
                }
                Node[] kids = new Node[k];
                for (int j = k - 1; j >= 0; j--) {
                    kids[j] = stack[--sp];
                }
                Node node = new Node();
                node.mLeaf = false;
                node.mToken = t;
                node.mStart = kids[0].mStart;
                node.mEnd = i;
                node.mKids = kids;
                node.mSize = node.mEnd - node.mStart + 1;
                stack[sp++] = node;
            } else {
                // A NaN that isn't an operator is a variable reference; a *data* variable is a
                // collection/array id consumed by an array op we don't split, so bail on it.
                // (isDataVariable does raw bit extraction, so it must be guarded by isNaN — a
                // finite
                // literal like 6.0f must never be mistaken for a data variable.)
                if (Float.isNaN(t) && NanMap.isDataVariable(t)) {
                    return null;
                }
                Node node = new Node();
                node.mLeaf = true;
                node.mToken = t;
                node.mStart = i;
                node.mEnd = i;
                node.mSize = 1;
                stack[sp++] = node;
            }
        }
        return sp == 1 ? stack[0] : null;
    }

    /** Tally how often each (size>=2) sub-expression occurs within this expression. */
    private void count(Node n, float[] src, Map<String, Integer> m) {
        if (n.mLeaf) {
            return;
        }
        for (Node k : n.mKids) {
            count(k, src, m);
        }
        if (n.mSize >= 2) {
            m.merge(canonOf(n, src), 1, Integer::sum);
        }
    }

    /** Produce the (CSE'd, size-bounded) token sequence for [n], emitting temps as needed. */
    private float[] compile(Node n, float[] src) {
        if (n.mLeaf) {
            return new float[]{n.mToken};
        }
        String c = canonOf(n, src);
        Float hit = mLocalCache.get(c);
        if (hit != null) {
            return new float[]{hit};
        }
        if (global()) {
            Float g = mGlobalCache.get(c);
            if (g != null) {
                mLocalCache.put(c, g);
                return new float[]{g};
            }
        }

        float[][] cs = new float[n.mKids.length][];
        int total = 1; // the op token itself
        for (int j = 0; j < n.mKids.length; j++) {
            cs[j] = compile(n.mKids[j], src);
            total += cs[j].length;
        }
        // Spill the largest still-inline child into a temp until the node's own sequence fits.
        while (total > BUDGET) {
            int best = -1;
            int bestLen = 1;
            for (int j = 0; j < cs.length; j++) {
                if (cs[j].length > bestLen) {
                    bestLen = cs[j].length;
                    best = j;
                }
            }
            if (best < 0) {
                break; // all children are single tokens: total = 1 + arity <= 4 (cannot happen)
            }
            float tid = materialize(canonOf(n.mKids[best], src), cs[best]);
            total -= cs[best].length - 1;
            cs[best] = new float[]{tid};
        }

        float[] seq = concat(cs, n.mToken, total);

        boolean recurring =
                mLocalCount.getOrDefault(c, 0) >= 2 || (global() && mGlobalSeen.contains(c));
        if (n.mSize >= 2 && recurring) {
            return new float[]{materialize(c, seq)};
        }
        if (n.mSize >= 2 && global()) {
            mGlobalSeen.add(c);
        }
        return seq;
    }

    /** Emit [seq] as a hidden variable for sub-expression [canon] (deduped), returning its id. */
    private float materialize(String canon, float[] seq) {
        Float ex = mLocalCache.get(canon);
        if (ex != null) {
            return ex;
        }
        if (global()) {
            Float g = mGlobalCache.get(canon);
            if (g != null) {
                mLocalCache.put(canon, g);
                return g;
            }
        }
        float id = mEmitter.emit(seq);
        mLocalCache.put(canon, id);
        if (global()) {
            mGlobalCache.put(canon, id);
        }
        return id;
    }

    private static float[] concat(float[][] parts, float op, int total) {
        float[] out = new float[total];
        int p = 0;
        for (float[] part : parts) {
            System.arraycopy(part, 0, out, p, part.length);
            p += part.length;
        }
        out[p] = op;
        return out;
    }

    private static String canonOf(Node n, float[] src) {
        if (n.mCanon != null) {
            return n.mCanon;
        }
        StringBuilder sb = new StringBuilder();
        for (int i = n.mStart; i <= n.mEnd; i++) {
            sb.append(Float.floatToRawIntBits(src[i])).append(',');
        }
        n.mCanon = sb.toString();
        return n.mCanon;
    }

    private static final class Node {
        boolean mLeaf;
        float mToken; // leaf value, or the operator token
        int mStart;
        int mEnd;
        int mSize;
        Node @Nullable [] mKids;
        @Nullable String mCanon;
    }
}
