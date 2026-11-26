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
package androidx.compose.remote.core;

import androidx.compose.remote.core.operations.utilities.AnimatedFloatExpression;
import androidx.compose.remote.core.operations.utilities.VectorOpCodes;
import androidx.compose.remote.core.operations.utilities.VectorRpn;

import org.junit.Test;

/**
 * Microbenchmark comparing {@link VectorRpn} to {@link AnimatedFloatExpression}. Not a correctness
 * gate — it prints ns/op so we can quantify per-op overhead. Run with:
 * {@code ./gradlew :compose:remote:remote-core:test --tests "*VectorRpnBenchmarkTest"}.
 */
public class VectorRpnBenchmarkTest {

    private static final int WARMUP = 300_000;
    private static final int ITERS = 5_000_000;

    @Test
    public void benchmark() {
        AnimatedFloatExpression afe = new AnimatedFloatExpression();
        VectorRpn vr = new VectorRpn();
        float[] out = new float[VectorRpn.MAX_DIM];

        // scalar: 2*3 + 4 = 10
        float[] sAfe = {2f, 3f, AnimatedFloatExpression.MUL, 4f, AnimatedFloatExpression.ADD};
        float[] sVec = {2f, 3f, VectorOpCodes.MUL, 4f, VectorOpCodes.ADD};
        // vec3: (1,2,3)*2 + (4,5,6) = (6,9,12)
        float[] v3 = {
                1f, 2f, 3f, VectorOpCodes.VBUILD3,
                2f, VectorOpCodes.MUL,
                4f, 5f, 6f, VectorOpCodes.VBUILD3,
                VectorOpCodes.ADD
        };

        double sink = 0;
        for (int i = 0; i < WARMUP; i++) {
            sink += afe.eval(sAfe, sAfe.length);
            sink += vr.apply(sVec, sVec.length, out);
            sink += vr.apply(v3, v3.length, out);
        }

        long t0 = System.nanoTime();
        for (int i = 0; i < ITERS; i++) sink += afe.eval(sAfe, sAfe.length);
        long t1 = System.nanoTime();
        for (int i = 0; i < ITERS; i++) sink += vr.apply(sVec, sVec.length, out);
        long t2 = System.nanoTime();
        for (int i = 0; i < ITERS; i++) sink += vr.apply(v3, v3.length, out);
        long t3 = System.nanoTime();
        // the real comparison: a vec3 update done the old way = 3 scalar evals (the work VectorRpn
        // replaces with one vec3 eval; scalars in a particle system already use
        // AnimatedFloatExpression)
        for (int i = 0; i < ITERS; i++) {
            sink += afe.eval(sAfe, sAfe.length);
            sink += afe.eval(sAfe, sAfe.length);
            sink += afe.eval(sAfe, sAfe.length);
        }
        long t4 = System.nanoTime();

        double afeNs = (double) (t1 - t0) / ITERS;
        double vrScalarNs = (double) (t2 - t1) / ITERS;
        double vrVec3Ns = (double) (t3 - t2) / ITERS;
        double afeX3Ns = (double) (t4 - t3) / ITERS;
        System.out.println("=== VectorRpn benchmark (ns/op, sink=" + sink + ") ===");
        System.out.printf("scalar  AnimatedFloatExpression  : %6.1f ns/op%n", afeNs);
        System.out.printf("scalar  VectorRpn (not used here) : %6.1f ns/op  (%.1fx)%n",
                vrScalarNs, vrScalarNs / afeNs);
        System.out.printf("vec3    VectorRpn (1 eval, 3 out) : %6.1f ns/op%n", vrVec3Ns);
        System.out.printf(
                "vec3    3x scalar AFE (old way)   : %6.1f ns/op  -> VectorRpn is %.2fx%n",
                afeX3Ns, afeX3Ns / vrVec3Ns);
    }
}
