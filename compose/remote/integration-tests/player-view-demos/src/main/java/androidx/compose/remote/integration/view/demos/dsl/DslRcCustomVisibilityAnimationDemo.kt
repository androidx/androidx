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

package androidx.compose.remote.integration.view.demos.dsl

import androidx.compose.remote.core.CoreDocument
import androidx.compose.remote.core.operations.Header
import androidx.compose.remote.core.operations.layout.Component
import androidx.compose.remote.core.operations.layout.animation.AnimationSpec
import androidx.compose.remote.creation.RemoteComposeWriter.HTag
import androidx.compose.remote.creation.dsl.Modifier
import androidx.compose.remote.creation.dsl.RcColumnVerticalPositioning
import androidx.compose.remote.creation.dsl.RcHorizontalPositioning
import androidx.compose.remote.creation.dsl.RcPaintStyle
import androidx.compose.remote.creation.dsl.RcProfile
import androidx.compose.remote.creation.dsl.animationSpec
import androidx.compose.remote.creation.dsl.background
import androidx.compose.remote.creation.dsl.clip
import androidx.compose.remote.creation.dsl.createRcBuffer
import androidx.compose.remote.creation.dsl.fillMaxSize
import androidx.compose.remote.creation.dsl.height
import androidx.compose.remote.creation.dsl.max
import androidx.compose.remote.creation.dsl.onClick
import androidx.compose.remote.creation.dsl.padding
import androidx.compose.remote.creation.dsl.rf
import androidx.compose.remote.creation.dsl.rsp
import androidx.compose.remote.creation.dsl.sin
import androidx.compose.remote.creation.dsl.size
import androidx.compose.remote.creation.dsl.visibility
import androidx.compose.remote.creation.dsl.width
import androidx.compose.remote.creation.modifiers.RoundedRectShape
import androidx.compose.remote.creation.profile.RcPlatformProfiles
import org.intellij.lang.annotations.Language

@Language("AGSL")
private const val SHADER_LIQUID_CHROME_AGSL =
    """
    uniform shader uTexture;
    uniform float uProgress;
    uniform float2 uResolution;

    half4 sCl(vec2 c, vec2 res) {
        if (c.x < 0.0 || c.x >= res.x || c.y < 0.0 || c.y >= res.y) return half4(0.0);
        return uTexture.eval(c);
    }

    float getField(vec2 p, vec2 c, float w, float h, float pr, float r2) {
        float sum = 0.0;
        for (int i = 0; i < 4; i++) {
            float fi = float(i);
            float a = pr * (4.2 + fi * 1.1) + fi * 1.57;
            vec2 pos = c + vec2(sin(a) * w * (0.36 - fi * 0.06), cos(a * 0.8 + fi) * h * (0.28 - fi * 0.05)) * (1.0 - pr * 0.2);
            vec2 d = p - pos;
            sum += r2 / (dot(d, d) + 16.0);
        }
        return sum;
    }

    vec3 cEnv(vec3 R, float fr) {
        vec3 sky = mix(vec3(0.12, 0.15, 0.22), vec3(0.75, 0.85, 1.0), clamp(R.y * 0.7 + 0.3, 0.0, 1.0));
        vec3 gnd = mix(vec3(0.04), vec3(0.18), clamp(-R.y * 0.8 + 0.2, 0.0, 1.0));
        vec3 hCol = mix(vec3(1.0, 0.9, 0.8), vec3(sin(R.x * 3.0), sin(R.x * 3.0 + 2.09), sin(R.x * 3.0 + 4.18)) * 0.5 + 0.5, 0.45) * exp(-abs(R.y) * 4.5) * 1.6;
        float box = pow(max(0.0, sin(R.x * 1.5 + 0.5) * sin(R.y * 2.5 + 1.2)), 6.0) * 3.5;
        float sun = pow(max(0.0, dot(R, normalize(vec3(0.3, 0.6, 0.74)))), 32.0) * 4.0;
        return mix((R.y >= 0.0 ? sky : gnd) + hCol + vec3(1.0, 0.95, 0.9) * box + vec3(1.0) * sun, vec3(0.9, 0.95, 1.0) * 1.4, fr * 0.7);
    }

    half4 main(vec2 fc) {
        if (uResolution.x <= 0.0 || uResolution.y <= 0.0) return uTexture.eval(fc);
        float p = uProgress;
        if (p <= 0.001) return sCl(fc, uResolution);

        float w = uResolution.x, h = uResolution.y;
        vec2 c = uResolution * 0.5;

        float wPos = (fc.x / max(w, 1.0)) * 0.75 + (fc.y / max(h, 1.0)) * 0.25;
        float mP = clamp(p * 1.6 - wPos * 0.6, 0.0, 1.0);
        if (mP <= 0.001) return sCl(fc, uResolution);

        float r2 = pow(mix(16.0, 2.5, p), 2.0);
        float mb = getField(fc, c, w, h, p, r2);

        vec2 dB = abs(fc - c) - vec2(w * 0.5 - 6.0, h * 0.5 - 6.0);
        float cLiq = clamp(1.0 - (length(max(dB, 0.0)) + min(max(dB.x, dB.y), 0.0)) / 8.0, 0.0, 1.0) * (1.0 - smoothstep(0.1, 0.6, p));

        float rD = length(fc - c);
        float H = mix(cLiq, mb * 0.85, smoothstep(0.05, 0.55, p)) + sin(rD * 0.35 - p * 18.0) * exp(-rD * 0.02) * (1.0 - p) * 0.25;
        float th = mix(0.12, 0.45, p);

        if (H < th) {
            float eD = th - H;
            if (eD < 0.15 && mP > 0.05 && mP < 0.95) {
                float g = (1.0 - eD / 0.15) * (1.0 - p);
                return half4(mix(vec3(1.0, 0.6, 0.2), vec3(0.3, 0.8, 1.0), sin(p * 10.0 + wPos * 6.28) * 0.5 + 0.5) * g * 1.5, g * 0.8);
            }
            return half4(0.0);
        }

        float e = 1.5;
        float dHx = (getField(fc + vec2(e, 0.0), c, w, h, p, r2) - getField(fc - vec2(e, 0.0), c, w, h, p, r2)) / (2.0 * e) + cos(rD * 0.35 - p * 18.0) * 0.15;
        float dHy = (getField(fc + vec2(0.0, e), c, w, h, p, r2) - getField(fc - vec2(0.0, e), c, w, h, p, r2)) / (2.0 * e) + sin(rD * 0.35 - p * 18.0) * 0.15;

        vec3 norm = normalize(vec3(-dHx * 12.0, -dHy * 12.0, sqrt(clamp(H - th, 0.0, 1.0)) * 3.5 + 0.5));
        vec3 R = reflect(vec3(0.0, 0.0, -1.0), norm);
        vec3 chrome = cEnv(R, pow(1.0 - max(0.0, norm.z), 4.0));
        half4 bTex = sCl(fc + norm.xy * 12.0 * (1.0 - p), uResolution);

        vec3 rgb = mix(bTex.rgb * 1.2 + chrome * 0.3, chrome, smoothstep(0.0, 0.35, mP)) + vec3(1.0, 0.7, 0.3) * (smoothstep(0.0, 0.08, mP) * smoothstep(0.25, 0.08, mP)) * 2.0;
        return half4(rgb, clamp(smoothstep(th, th + 0.08, H) * (1.0 - smoothstep(0.85, 1.0, p)), 0.0, 1.0));
    }
    """

@Language("AGSL")
private const val SHADER_SHARD_EXPLOSION_AGSL =
    """
    uniform shader uTexture;
    uniform float uProgress;
    uniform float2 uResolution;

    vec2 hash22(vec2 p) {
        return fract(sin(vec2(dot(p, vec2(127.1, 311.7)), dot(p, vec2(269.5, 183.3)))) * 43758.5453);
    }

    half4 sampleClamped(vec2 c, vec2 res) {
        if (c.x < 0.0 || c.x >= res.x || c.y < 0.0 || c.y >= res.y) return half4(0.0);
        return uTexture.eval(c);
    }

    half4 evalShardLayer(vec2 fragCoord, vec2 center, vec2 bNorm, float shockP, vec2 res, float S, float speed, float rotSpeed, float grav) {
        float flyP = clamp((shockP - 0.06) / 0.94, 0.0, 1.0);
        vec2 disp = -bNorm * (flyP * speed) - vec2(0.0, flyP * flyP * grav);
        vec2 src = fragCoord + disp;
        if (src.x < 0.0 || src.x >= res.x || src.y < 0.0 || src.y >= res.y) return half4(0.0);

        vec2 cell = floor(src / S);
        vec2 h = hash22(cell);
        float sP = clamp((flyP - h.y * 0.15) / 0.85, 0.0, 1.0);

        vec2 frac = (src / S - cell) - 0.5 - (h - 0.5) * 0.35 * sP;
        float rot = (h.x - 0.5) * rotSpeed * sP;
        float cR = cos(-rot), sR = sin(-rot);
        vec2 rFrac = vec2(frac.x * cR - frac.y * sR, frac.x * sR + frac.y * cR);

        float sSize = 0.47 * max(0.0, 1.0 - sP * 0.65);
        float sDist = max(max(abs(rFrac.x), abs(rFrac.y)), (abs(rFrac.x) + abs(rFrac.y)) * 0.707);
        if (sDist > sSize) return half4(0.0);

        vec2 samplePt = (cell + 0.5 + rFrac) * S;
        half4 tex = sampleClamped(samplePt, res);
        if (tex.a <= 0.0) return half4(0.0);

        // 3D facet lighting & specular glint
        float tX = (h.x - 0.5) * 1.5 * sP, tY = (h.y - 0.5) * 1.5 * sP;
        vec3 norm = normalize(vec3(sin(tX + rot), sin(tY - rot), cos(tX) * cos(tY)));
        vec3 light = normalize(vec3(-0.4, -0.6, 0.7));
        float diff = clamp(dot(norm, light) * 0.5 + 0.6, 0.4, 1.3);
        float spec = pow(max(0.0, dot(reflect(-light, norm), vec3(0.0, 0.0, 1.0))), 8.0) * 1.4 * sP;

        float edge = smoothstep(sSize, sSize - 0.12, sDist);
        float a = tex.a * (1.0 - smoothstep(0.85, 1.0, sP));
        vec3 edgeGlow = mix(tex.rgb * 1.4 + vec3(0.2), vec3(1.0), 0.5) * (1.0 - edge) * 0.8;
        vec3 rgb = (tex.rgb * diff + edgeGlow + vec3(1.0, 0.9, 0.8) * spec) * a;
        return half4(rgb, a);
    }

    half4 main(vec2 fragCoord) {
        if (uResolution.x <= 0.0 || uResolution.y <= 0.0) return uTexture.eval(fragCoord);
        float p = uProgress;
        if (p <= 0.001) return sampleClamped(fragCoord, uResolution);

        float w = uResolution.x, h = uResolution.y;
        vec2 center = uResolution * 0.5;
        vec2 fromC = fragCoord - center;
        float distC = length(fromC);
        vec2 bNorm = (distC > 0.001) ? (fromC / distC) : vec2(0.0, 1.0);

        float shockP = clamp(p * 2.2 - (distC / max(w * 0.5, 1.0)) * 0.3, 0.0, 1.0);
        if (shockP <= 0.05 && p < 0.15) return sampleClamped(fragCoord, uResolution);

        // Multi-scale crystal shards scaled proportionally to resolution
        float dScale = max(w / 260.0, 1.0);
        half4 c1 = evalShardLayer(fragCoord, center, bNorm, shockP, uResolution, 3.2 * dScale, 75.0 * dScale, 20.0, 35.0 * dScale);
        half4 c2 = (c1.a < 0.5) ? evalShardLayer(fragCoord, center, bNorm, shockP, uResolution, 1.8 * dScale, 105.0 * dScale, 28.0, 50.0 * dScale) : half4(0.0);
        half4 finalCol = (c1.a >= c2.a) ? c1 : c2;

        // Shockwave refractive flash ring
        float waveDist = abs(distC - p * max(w * 0.7, 1.0));
        float waveInt = smoothstep(8.0 * dScale, 0.0, waveDist) * smoothstep(0.4, 0.0, p) * 0.7;
        vec3 waveGlow = mix(finalCol.rgb * 1.5 + vec3(0.2), vec3(1.0, 0.95, 0.85), 0.6) * waveInt;

        return half4(finalCol.rgb + waveGlow, clamp(finalCol.a + waveInt * 0.5, 0.0, 1.0));
    }
    """

@Language("AGSL")
private const val SHADER_LIQUID_DRAIN_EXIT_AGSL =
    """
    uniform shader uTexture;
    uniform float uProgress;
    uniform float2 uResolution;

    float hash21(vec2 p) {
        p = fract(p * vec2(123.34, 456.21));
        p += dot(p, p + 45.32);
        return fract(p.x * p.y);
    }

    half4 sampleClamped(vec2 coord, vec2 res) {
        if (coord.x < 0.0 || coord.x >= res.x || coord.y < 0.0 || coord.y >= res.y) {
            return half4(0.0);
        }
        return uTexture.eval(coord);
    }

    half4 main(vec2 fragCoord) {
        if (uResolution.x <= 0.0 || uResolution.y <= 0.0) return uTexture.eval(fragCoord);
        vec2 uv = fragCoord / uResolution;
        float p = uProgress;
        
        float rampIn = smoothstep(0.0, 0.14, p);
        float rampOut = 1.0 - smoothstep(0.85, 0.98, p);
        float env = rampIn * rampOut;
        
        // Fluid level drains from top (0.0) down to bottom (uResolution.y)
        float baseLevel = smoothstep(0.04, 0.98, p) * uResolution.y;
        
        float waveDecay = env;
        float wave1 = sin(fragCoord.x * 0.08 + p * 16.0) * 3.5 * waveDecay;
        float wave2 = cos(fragCoord.x * 0.16 - p * 22.0) * 1.8 * waveDecay;
        float wave3 = sin((uv.x - 0.5) * 6.28 + p * 10.0) * 2.5 * waveDecay;
        float surfaceY = baseLevel + wave1 + wave2 + wave3;
        
        if (fragCoord.y < surfaceY) {
            return half4(0.0);
        }
        
        float colIdx = floor(fragCoord.x / 14.0);
        float hBubble = hash21(vec2(colIdx, 5.13));
        float bubbleSpeed = mix(1.0, 2.2, hBubble);
        float bubbleProgress = fract(p * 2.0 * bubbleSpeed + hBubble);
        float bubbleY = uResolution.y - bubbleProgress * (uResolution.y - surfaceY + 10.0);
        vec2 bubblePos = vec2((colIdx + 0.5) * 14.0 + sin(p * 8.0 + hBubble) * 3.0, bubbleY);
        float distToBubble = length(fragCoord - bubblePos);
        float bubbleRadius = mix(1.5, 3.2, hBubble);
        float isBubble = smoothstep(bubbleRadius, bubbleRadius - 1.0, distToBubble) * step(surfaceY, fragCoord.y) * env;
        
        float shimmer = sin(fragCoord.y * 0.3 + p * 12.0) * cos(fragCoord.x * 0.2) * 1.5 * env;
        vec2 sampleCoord = fragCoord + vec2(shimmer, 0.0);
        half4 texCol = sampleClamped(sampleCoord, uResolution);
        if (texCol.a <= 0.0) return half4(0.0);
        
        float distToSurface = abs(fragCoord.y - surfaceY);
        float surfaceGlow = smoothstep(3.5, 0.0, distToSurface) * env;
        float crestHighlight = smoothstep(1.2, 0.0, distToSurface) * 2.2 * env;
        vec3 waveCol = mix(vec3(0.5, 0.9, 1.0), vec3(1.0), 0.8) * (surfaceGlow + crestHighlight);
        
        vec3 bubbleCol = vec3(0.9, 1.0, 1.0) * isBubble * 1.5;
        
        vec3 rgb = texCol.rgb + (waveCol + bubbleCol) * texCol.a;
        float alpha = texCol.a * (1.0 - smoothstep(0.92, 1.0, p));
        
        return half4(rgb, alpha);
    }
    """

@Language("AGSL")
private const val SHADER_LIQUID_FILL_ENTER_AGSL =
    """
    uniform shader uTexture;
    uniform float uProgress;
    uniform float2 uResolution;

    float hash21(vec2 p) {
        p = fract(p * vec2(123.34, 456.21));
        p += dot(p, p + 45.32);
        return fract(p.x * p.y);
    }

    half4 sampleClamped(vec2 coord, vec2 res) {
        if (coord.x < 0.0 || coord.x >= res.x || coord.y < 0.0 || coord.y >= res.y) {
            return half4(0.0);
        }
        return uTexture.eval(coord);
    }

    half4 main(vec2 fragCoord) {
        if (uResolution.x <= 0.0 || uResolution.y <= 0.0) return uTexture.eval(fragCoord);
        vec2 uv = fragCoord / uResolution;
        float p = uProgress;
        
        float settle = 1.0 - smoothstep(0.75, 0.97, p);
        float baseLevel = (1.0 - smoothstep(0.0, 0.95, p)) * uResolution.y;
        
        float waveDecay = settle;
        float wave1 = sin(fragCoord.x * 0.08 - p * 16.0) * 3.5 * waveDecay;
        float wave2 = cos(fragCoord.x * 0.16 + p * 22.0) * 1.8 * waveDecay;
        float wave3 = sin((uv.x - 0.5) * 6.28 - p * 10.0) * 2.5 * waveDecay;
        float surfaceY = baseLevel + wave1 + wave2 + wave3;
        
        if (fragCoord.y < surfaceY) {
            return half4(0.0);
        }
        
        float colIdx = floor(fragCoord.x / 14.0);
        float hBubble = hash21(vec2(colIdx, 3.77));
        float bubbleSpeed = mix(1.0, 2.2, hBubble);
        float bubbleProgress = fract(p * 2.0 * bubbleSpeed + hBubble);
        float bubbleY = uResolution.y - bubbleProgress * (uResolution.y - surfaceY + 10.0);
        vec2 bubblePos = vec2((colIdx + 0.5) * 14.0 + sin(p * 8.0 + hBubble) * 3.0, bubbleY);
        float distToBubble = length(fragCoord - bubblePos);
        float bubbleRadius = mix(1.5, 3.2, hBubble);
        float isBubble = smoothstep(bubbleRadius, bubbleRadius - 1.0, distToBubble) * step(surfaceY, fragCoord.y) * settle;
        
        float shimmer = sin(fragCoord.y * 0.3 - p * 12.0) * cos(fragCoord.x * 0.2) * 1.5 * settle;
        vec2 sampleCoord = fragCoord + vec2(shimmer, 0.0);
        half4 texCol = sampleClamped(sampleCoord, uResolution);
        if (texCol.a <= 0.0) return half4(0.0);
        
        float distToSurface = abs(fragCoord.y - surfaceY);
        float surfaceGlow = smoothstep(3.5, 0.0, distToSurface) * settle;
        float crestHighlight = smoothstep(1.2, 0.0, distToSurface) * 2.2 * settle;
        vec3 waveCol = mix(vec3(0.5, 0.9, 1.0), vec3(1.0), 0.8) * (surfaceGlow + crestHighlight);
        
        vec3 bubbleCol = vec3(0.9, 1.0, 1.0) * isBubble * 1.5;
        
        vec3 rgb = texCol.rgb + (waveCol + bubbleCol) * texCol.a;
        float alpha = texCol.a;
        
        return half4(rgb, alpha);
    }
    """

@Language("AGSL")
private const val SHADER_WARP_SINGULARITY_AGSL =
    """
    uniform shader uTexture;
    uniform float uProgress;
    uniform float2 uResolution;

    half4 sampleClamped(vec2 coord, vec2 res) {
        if (coord.x < 0.0 || coord.x >= res.x || coord.y < 0.0 || coord.y >= res.y) {
            return half4(0.0);
        }
        return uTexture.eval(coord);
    }

    half4 main(vec2 fragCoord) {
        if (uResolution.x <= 0.0 || uResolution.y <= 0.0) return uTexture.eval(fragCoord);
        vec2 center = 0.5 * uResolution;
        vec2 offset = fragCoord - center;
        float r = length(offset);
        float maxR = 0.5 * min(uResolution.x, uResolution.y);
        float normR = r / maxR;
        
        float p = uProgress;
        float swirl = (1.0 - smoothstep(0.0, 1.2, normR)) * p * 8.0;
        float c = cos(swirl), s = sin(swirl);
        mat2 rot = mat2(c, -s, s, c);
        
        float warpFactor = 1.0 + p * 3.5;
        float warpedNormR = pow(normR, 1.0 / warpFactor);
        vec2 warpedOffset = rot * (normalize(offset + vec2(0.0001)) * (warpedNormR * maxR));
        vec2 sampleCoord = center + warpedOffset;
        
        float split = p * 6.0 * (1.0 - normR);
        half4 colR = sampleClamped(sampleCoord + vec2(split, 0.0), uResolution);
        half4 colG = sampleClamped(sampleCoord, uResolution);
        half4 colB = sampleClamped(sampleCoord - vec2(split, 0.0), uResolution);
        half4 baseColor = half4(colR.r, colG.g, colB.b, colG.a);
        
        float horizonRadius = p * maxR * 0.45;
        float inHorizon = smoothstep(horizonRadius, horizonRadius + 2.0, r);
        
        float ringDist = abs(r - horizonRadius);
        float photonGlow = exp(-ringDist * 0.18) * p * 2.5;
        vec3 glowCol = mix(vec3(0.3, 0.7, 1.0), vec3(1.0, 0.4, 0.9), sin(atan(offset.y, offset.x) * 3.0 + p * 10.0) * 0.5 + 0.5);
        
        float shockRadius = p * maxR * 1.8;
        float shockDist = abs(r - shockRadius);
        float shockGlow = exp(-shockDist * 0.25) * (1.0 - p) * 1.8;
        
        float alpha = baseColor.a * inHorizon * (1.0 - p * 0.85);
        vec3 rgb = baseColor.rgb * inHorizon + (glowCol * photonGlow + vec3(0.5, 0.8, 1.0) * shockGlow) * baseColor.a;
        
        float flash = smoothstep(0.85, 0.98, p) * smoothstep(1.0, 0.95, p) * exp(-r * 0.08) * 4.0;
        rgb += vec3(flash);
        alpha = clamp(alpha + flash * 0.8, 0.0, 1.0);
        
        return half4(rgb, alpha);
    }
    """

@Language("AGSL")
private const val SHADER_MATRIX_GLITCH_AGSL =
    """
    uniform shader uTexture;
    uniform float uProgress;
    uniform float2 uResolution;

    float hash21(vec2 p) {
        p = fract(p * vec2(123.34, 456.21));
        p += dot(p, p + 45.32);
        return fract(p.x * p.y);
    }

    half4 sampleClamped(vec2 coord, vec2 res) {
        if (coord.x < 0.0 || coord.x >= res.x || coord.y < 0.0 || coord.y >= res.y) {
            return half4(0.0);
        }
        return uTexture.eval(coord);
    }

    half4 main(vec2 fragCoord) {
        if (uResolution.x <= 0.0 || uResolution.y <= 0.0) return uTexture.eval(fragCoord);
        vec2 uv = fragCoord / uResolution;
        float p = uProgress;
        
        float pixelSize = mix(1.0, 14.0, p * p);
        vec2 blockCoord = floor(fragCoord / pixelSize) * pixelSize;
        
        float sliceY = floor(fragCoord.y / 6.0);
        float sliceHash = hash21(vec2(sliceY, floor(p * 24.0)));
        float jitter = (step(0.75, sliceHash) - 0.5) * 20.0 * p;
        vec2 glitchCoord = blockCoord + vec2(jitter, 0.0);
        
        float colX = floor(fragCoord.x / 8.0);
        float colSpeed = hash21(vec2(colX, 3.14)) * 0.8 + 0.6;
        float rainOffset = p * colSpeed * uResolution.y * 1.5;
        vec2 rainCoord = glitchCoord + vec2(0.0, -rainOffset);
        
        half4 texCol = sampleClamped(glitchCoord, uResolution);
        if (texCol.a <= 0.0) return half4(0.0);
        
        float blockNoise = hash21(floor(blockCoord * 0.15));
        float dissolveSweep = uv.y * 0.4 + blockNoise * 0.6;
        float dissolveCut = p * 1.4;
        
        vec3 matrixGreen = vec3(0.0, 1.0, 0.4);
        vec3 matrixCyan = vec3(0.0, 0.9, 1.0);
        vec3 cyberCol = mix(texCol.rgb, matrixGreen, p * 0.75);
        
        float glyphBit = step(0.4, hash21(floor(fragCoord * 0.3) + vec2(0.0, floor(p * 40.0))));
        
        if (dissolveSweep < dissolveCut) {
            float trailTime = dissolveCut - dissolveSweep;
            float trailFade = smoothstep(0.5, 0.0, trailTime) * (1.0 - p * 0.6);
            float isRaining = step(0.65, hash21(floor(rainCoord * 0.25))) * glyphBit;
            vec3 rainRgb = mix(matrixGreen, matrixCyan, hash21(vec2(colX, 7.7))) * 2.5 * isRaining;
            float rainAlpha = isRaining * trailFade * texCol.a;
            return half4(rainRgb, rainAlpha);
        }
        
        float edgeDist = dissolveSweep - dissolveCut;
        float edgeGlow = smoothstep(0.12, 0.0, edgeDist);
        vec3 glowRgb = mix(matrixCyan, vec3(1.0), 0.5) * 3.0 * edgeGlow;
        
        float scanline = sin(fragCoord.y * 2.0) * 0.15 + 0.85;
        vec3 finalRgb = (cyberCol + glowRgb) * scanline;
        float finalAlpha = texCol.a * (1.0 - p * 0.7);
        
        return half4(finalRgb, finalAlpha);
    }
    """

@Language("AGSL")
private const val SHADER_FIRE_SWEEP_AGSL =
    """
    uniform shader uTexture;
    uniform float uProgress;
    uniform float2 uResolution;

    float hash21(vec2 p) {
        p = fract(p * vec2(234.34, 435.345));
        p += dot(p, p + 34.23);
        return fract(p.x * p.y);
    }

    float noise(vec2 p) {
        vec2 i = floor(p);
        vec2 f = fract(p);
        f = f * f * (3.0 - 2.0 * f);
        return mix(mix(hash21(i), hash21(i + vec2(1.0, 0.0)), f.x),
                   mix(hash21(i + vec2(0.0, 1.0)), hash21(i + vec2(1.0, 1.0)), f.x), f.y);
    }

    float fbm(vec2 p) {
        float v = 0.0;
        float a = 0.5;
        for (int i = 0; i < 3; i++) {
            v += a * noise(p);
            p *= 2.1;
            a *= 0.5;
        }
        return v;
    }

    half4 sampleClamped(vec2 coord, vec2 res) {
        if (coord.x < 0.0 || coord.x >= res.x || coord.y < 0.0 || coord.y >= res.y) {
            return half4(0.0);
        }
        return uTexture.eval(coord);
    }

    half4 main(vec2 fragCoord) {
        if (uResolution.x <= 0.0 || uResolution.y <= 0.0) return uTexture.eval(fragCoord);
        vec2 uv = fragCoord / uResolution;
        float p = uProgress;
        
        float heat = sin(uv.y * 30.0 - p * 15.0) * cos(uv.x * 20.0 + p * 10.0) * 0.02 * p;
        vec2 heatCoord = fragCoord + vec2(heat * uResolution.y, -p * 10.0 * heat);
        half4 texCol = sampleClamped(heatCoord, uResolution);
        
        float n = fbm(uv * 10.0 + vec2(0.0, -p * 4.0));
        float burnThreshold = (uv.x * 0.65 + uv.y * 0.35) + n * 0.35;
        float burnCut = p * 1.5;
        
        if (burnThreshold >= burnCut) {
            if (texCol.a <= 0.0) return half4(0.0);
            float edgeDist = burnThreshold - burnCut;
            float charring = smoothstep(0.18, 0.0, edgeDist);
            float flameGlow = smoothstep(0.08, 0.0, edgeDist);
            float whiteHot = smoothstep(0.025, 0.0, edgeDist);
            
            vec3 charCol = mix(texCol.rgb, vec3(0.08, 0.03, 0.02), charring * 0.8);
            vec3 flameCol = vec3(1.0, 0.45, 0.05) * flameGlow * 3.5;
            vec3 hotCol = vec3(1.0, 0.95, 0.7) * whiteHot * 4.0;
            
            return half4(charCol + flameCol + hotCol, texCol.a);
        }
        
        float timeBurned = burnCut - burnThreshold;
        vec2 flameRise = vec2(sin(uv.y * 15.0 + p * 8.0) * 4.0, timeBurned * 45.0);
        vec2 emberCoord = fragCoord + flameRise;
        
        float emberNoise = hash21(floor(emberCoord * 0.45));
        float isEmber = step(0.82, emberNoise);
        float emberFade = smoothstep(0.45, 0.05, timeBurned) * (1.0 - p * 0.5);
        
        vec3 emberRgb = mix(vec3(1.0, 0.3, 0.02), vec3(1.0, 0.85, 0.2), hash21(floor(emberCoord * 0.2))) * 3.0;
        float emberAlpha = isEmber * emberFade * texCol.a;
        
        float smoke = smoothstep(0.35, 0.0, timeBurned) * (n * 0.4) * (1.0 - p);
        vec3 smokeRgb = vec3(0.15, 0.12, 0.1) * smoke;
        
        float finalAlpha = clamp(emberAlpha + smoke * 0.7, 0.0, 1.0);
        vec3 finalRgb = emberRgb * emberAlpha + smokeRgb;
        
        return half4(finalRgb, finalAlpha);
    }
    """

@Language("AGSL")
private const val SHADER_MELT_AGSL =
    """
    uniform shader uTexture;
    uniform float uProgress;
    uniform float2 uResolution;

    half4 sampleClamped(vec2 coord, vec2 res) {
        if (coord.x < 0.0 || coord.x >= res.x || coord.y < 0.0 || coord.y >= res.y) {
            return half4(0.0);
        }
        return uTexture.eval(coord);
    }

    half4 main(vec2 fragCoord) {
        if (uResolution.x <= 0.0 || uResolution.y <= 0.0) {
            return uTexture.eval(fragCoord);
        }
        vec2 uv = fragCoord / uResolution;
        float env = smoothstep(0.0, 0.16, uProgress);
        
        // Liquid ripple wave distortion based on progress
        float waveDist = length(uv - vec2(0.5, 0.5));
        float wave = sin(waveDist * 20.0 - uProgress * 10.0) * 0.04 * uProgress * env;
        vec2 distortedCoord = fragCoord + vec2(wave * uResolution.y, wave * uResolution.x);
        
        // Chromatic dispersion (RGB split that increases with progress)
        float split = uProgress * 8.0 * env;
        half4 colR = sampleClamped(distortedCoord + vec2(split, 0.0), uResolution);
        half4 colG = sampleClamped(distortedCoord, uResolution);
        half4 colB = sampleClamped(distortedCoord - vec2(split, 0.0), uResolution);
        half4 color = half4(colR.r, colG.g, colB.b, colG.a);
        
        // Plasma energy wave sweep threshold
        float plasma = sin(uv.x * 16.0 + uProgress * 5.0) * cos(uv.y * 16.0 + uProgress * 3.0) * 0.12 * env;
        float threshold = uv.y + plasma;
        
        // Cutoff sweeps across [1.20 .. -0.20] so uProgress=0 is 100% intact and uProgress=1 is 100% dissolved
        float cutoff = mix(1.20, -0.20, uProgress);
        float edgeWidth = 0.08;
        float dissolve = smoothstep(cutoff, cutoff + edgeWidth, threshold);
        
        // Glowing cyan/electric energy border along the dissolve edge
        float borderDist = abs(threshold - cutoff);
        float glow = smoothstep(edgeWidth, 0.0, borderDist) * env * (1.0 - smoothstep(0.85, 1.0, uProgress));
        vec3 glowCol = vec3(0.0, 0.95, 1.0) * glow * 2.5;
        
        // Final composite
        float fadeAlpha = 1.0 - smoothstep(0.15, 1.0, uProgress) * 0.8;
        float finalAlpha = (1.0 - dissolve) * color.a * fadeAlpha;
        vec3 finalRgb = (color.rgb * (1.0 - dissolve)) + glowCol * color.a;
        
        return half4(finalRgb, finalAlpha);
    }
    """

@Language("AGSL")
private const val SHADER_SNAP_DUST_AGSL =
    """
    uniform shader uTexture;
    uniform float uProgress;
    uniform float2 uResolution;

    float hash21(vec2 p) {
        p = fract(p * vec2(123.34, 456.21));
        p += dot(p, p + 45.32);
        return fract(p.x * p.y);
    }

    float noise(vec2 p) {
        vec2 i = floor(p);
        vec2 f = fract(p);
        f = f * f * (3.0 - 2.0 * f);
        float a = hash21(i);
        float b = hash21(i + vec2(1.0, 0.0));
        float c = hash21(i + vec2(0.0, 1.0));
        float d = hash21(i + vec2(1.0, 1.0));
        return mix(mix(a, b, f.x), mix(c, d, f.x), f.y);
    }

    float fbm(vec2 p) {
        float v = 0.0;
        float a = 0.5;
        for (int i = 0; i < 3; i++) {
            v += a * noise(p);
            p *= 2.0;
            a *= 0.5;
        }
        return v;
    }

    half4 sampleClamped(vec2 coord, vec2 res) {
        if (coord.x < 0.0 || coord.x >= res.x || coord.y < 0.0 || coord.y >= res.y) {
            return half4(0.0);
        }
        return uTexture.eval(coord);
    }

    half4 main(vec2 fragCoord) {
        if (uResolution.x <= 0.0 || uResolution.y <= 0.0) {
            return uTexture.eval(fragCoord);
        }
        vec2 uv = fragCoord / uResolution;
        float n = fbm(uv * 14.0);
        float fineGrain = hash21(floor(fragCoord * 0.7));
        float sweep = uv.x * 0.75 + uv.y * 0.25;
        float dissolveThreshold = sweep + (n * 0.40 + fineGrain * 0.12);
        float currentCut = uProgress * 1.55;
        half4 baseColor = sampleClamped(fragCoord, uResolution);

        if (dissolveThreshold >= currentCut) {
            if (baseColor.a <= 0.0) return half4(0.0);
            float edgeDist = dissolveThreshold - currentCut;
            float emberGlow = smoothstep(0.08, 0.0, edgeDist);
            vec3 emberColor = vec3(1.0, 0.65, 0.15) * 3.5 * emberGlow;
            return half4(baseColor.rgb + emberColor, baseColor.a);
        }

        float timeSinceDissolve = currentCut - dissolveThreshold;
        vec2 wind = vec2(0.85, -0.65);
        vec2 driftOffset = wind * (timeSinceDissolve * 40.0);
        vec2 turbulence = vec2(
            sin(uv.y * 20.0 + uProgress * 7.0) * 5.0,
            cos(uv.x * 20.0 + uProgress * 5.0) * 4.0
        );
        vec2 particleSourceCoord = fragCoord - (driftOffset + turbulence);
        half4 particleColor = sampleClamped(particleSourceCoord, uResolution);
        if (particleColor.a <= 0.0) return half4(0.0);

        float grain1 = hash21(floor(particleSourceCoord * 0.55));
        float grain2 = hash21(floor(particleSourceCoord * 0.30 + vec2(17.0, 43.0)));
        float isDustMote = step(0.78, grain1) * step(0.60, grain2);
        float dustFade = smoothstep(0.60, 0.05, timeSinceDissolve) * (1.0 - uProgress * 0.5);

        vec3 emberSpark = vec3(1.0, 0.72, 0.22) * 2.8 * step(0.92, grain1);
        vec3 ashRgb = mix(particleColor.rgb * 0.55, emberSpark, step(0.86, grain1));
        float dustAlpha = isDustMote * dustFade * particleColor.a;
        vec3 dustRgb = ashRgb * dustAlpha;

        float ashHaze = smoothstep(0.22, 0.0, timeSinceDissolve) * (n * 0.3) * (1.0 - uProgress) * particleColor.a;
        vec3 hazeRgb = vec3(0.22, 0.16, 0.12) * ashHaze;
        float finalAlpha = clamp(dustAlpha + ashHaze * 0.6, 0.0, 1.0);
        vec3 finalRgb = dustRgb + hazeRgb;
        return half4(finalRgb, finalAlpha);
    }
    """

/**
 * Kotlin DSL demo showcasing custom drawing for component visibility animations.
 *
 * Demonstrates:
 * 1. Custom scale + rotation enter/exit animation.
 * 2. Custom slide + expanding ring enter/exit animation.
 * 3. Custom particle explosion exit animation (with converging assemble enter).
 * 4. Custom cosmic vortex spiral exit animation (with singularity emerge enter).
 * 5. Custom cyberpunk hologram glitch & laser scan reveal enter/exit animation.
 * 6. Custom AGSL shader liquid dissolve & plasma melt animation.
 * 7. Custom AGSL shader Snap dust disintegration animation.
 * 8. Custom AGSL shader Black Hole Gravitational Singularity warp animation.
 * 9. Custom AGSL shader Cyber Matrix Digital Teleport / Voxel Glitch animation.
 * 10. Custom AGSL shader Solar Fire Phoenix Incineration sweep animation.
 * 11. Custom AGSL shader Liquid Drip exit & Fluid Fill enter animation.
 */
@Suppress("RestrictedApiAndroidX")
public fun dslRcCustomVisibilityAnimationDemo(): ByteArray {
    return createRcBuffer(
        RcProfile(RcPlatformProfiles.ANDROIDX),
        HTag(Header.DOC_WIDTH, 360),
        HTag(Header.DOC_HEIGHT, 680),
        HTag(Header.DOC_DENSITY_BEHAVIOR, CoreDocument.DENSITY_BEHAVIOR_DP),
        experimental = true,
    ) {
        val visState1 = remoteNamedInteger("vis1", Component.Visibility.VISIBLE)
        val visState2 = remoteNamedInteger("vis2", Component.Visibility.VISIBLE)
        val visState3 = remoteNamedInteger("vis3", Component.Visibility.VISIBLE)
        val visState4 = remoteNamedInteger("vis4", Component.Visibility.VISIBLE)
        val visState5 = remoteNamedInteger("vis5", Component.Visibility.VISIBLE)
        val visState6 = remoteNamedInteger("vis6", Component.Visibility.VISIBLE)
        val visState7 = remoteNamedInteger("vis7", Component.Visibility.VISIBLE)
        val visState8 = remoteNamedInteger("vis8", Component.Visibility.VISIBLE)
        val visState9 = remoteNamedInteger("vis9", Component.Visibility.VISIBLE)
        val visState10 = remoteNamedInteger("vis10", Component.Visibility.VISIBLE)
        val visState11 = remoteNamedInteger("vis11", Component.Visibility.VISIBLE)
        val visState12 = remoteNamedInteger("vis12", Component.Visibility.VISIBLE)
        val visState13 = remoteNamedInteger("vis13", Component.Visibility.VISIBLE)

        // Define Custom Visibility Animation 1: Scale + Rotation + Fade & Expanding Ring
        val customScaleRotateEnter = defineVisibilityAnimation { progress, w, h, x, y ->
            val cx = x + w / 2f.rf
            val cy = y + h / 2f.rf
            val scaleVal = 0.4f.rf + 0.6f.rf * progress
            val angle = (1f.rf - progress) * (-90f.rf)
            val cardAlpha = progress
            val ringRadius = (w / 2f.rf) * progress
            val ringAlpha = 1f.rf - progress
            save {
                paint { alpha(cardAlpha) }
                rotate(angle, cx, cy)
                scale(scaleVal, scaleVal, cx, cy)
                drawComponentContent()
            }
            // Expanding amber accent ring
            save {
                paint {
                    color(0xFFFFB300.toInt())
                    style(RcPaintStyle.Stroke)
                    strokeWidth(2.5f)
                    alpha(ringAlpha)
                }
                drawCircle(cx, cy, ringRadius)
            }
        }

        val customScaleRotateExit = defineVisibilityAnimation { progress, w, h, x, y ->
            val cx = x + w / 2f.rf
            val cy = y + h / 2f.rf
            val scaleVal = 1f.rf - 0.6f.rf * progress
            val angle = progress * 90f.rf
            val cardAlpha = max(0f.rf, 1f.rf - progress)
            save {
                paint { alpha(cardAlpha) }
                rotate(angle, cx, cy)
                scale(scaleVal, scaleVal, cx, cy)
                drawComponentContent()
            }
            // Contracting amber ring fading out
            val ringRadius = (w / 2f.rf) * cardAlpha
            save {
                paint {
                    color(0xFFFFB300.toInt())
                    style(RcPaintStyle.Stroke)
                    strokeWidth(2.5f)
                    alpha(cardAlpha)
                }
                drawCircle(cx, cy, ringRadius)
            }
        }

        // Define Custom Visibility Animation 2: Slide + Fade + Expanding Ring Overlay
        val customSlideRingEnter = defineVisibilityAnimation { progress, w, h, x, y ->
            val offsetY = (1f.rf - progress) * (-80f.rf)
            val cx = x + w / 2f.rf
            val cy = y + h / 2f.rf
            val maxRadius = w / 2f.rf
            val currentRadius = maxRadius * progress
            val ringAlpha = 1f.rf - progress
            val cardAlpha = progress
            save {
                paint { alpha(cardAlpha) }
                translate(0f.rf, offsetY)
                drawComponentContent()
            }
            // Draw accent ring expanding around component
            save {
                paint {
                    color(0xFF00E5FF.toInt())
                    style(RcPaintStyle.Stroke)
                    strokeWidth(3f)
                    alpha(ringAlpha)
                }
                drawCircle(cx, cy, currentRadius)
            }
        }

        val customSlideRingExit = defineVisibilityAnimation { progress, w, h, x, y ->
            val offsetY = progress * 80f.rf
            val cx = x + w / 2f.rf
            val cy = y + h / 2f.rf
            val cardAlpha = max(0f.rf, 1f.rf - progress)
            save {
                paint { alpha(cardAlpha) }
                translate(0f.rf, offsetY)
                drawComponentContent()
            }
            // Contracting accent ring
            val maxRadius = w / 2f.rf
            val currentRadius = maxRadius * cardAlpha
            save {
                paint {
                    color(0xFF00E5FF.toInt())
                    style(RcPaintStyle.Stroke)
                    strokeWidth(2.5f)
                    alpha(cardAlpha)
                }
                drawCircle(cx, cy, currentRadius)
            }
        }

        // Define Custom Visibility Animation 3: Particle Explosion Exit & Converging Assemble Enter
        val customParticleAssembleEnter = defineVisibilityAnimation { progress, w, h, x, y ->
            val cx = x + w / 2f.rf
            val cy = y + h / 2f.rf

            // Card scales up and fades into place
            val cardScale = 0.3f.rf + 0.7f.rf * progress
            val cardAlpha = progress
            save {
                paint { alpha(cardAlpha) }
                scale(cardScale, cardScale, cx, cy)
                drawComponentContent()
            }

            // Converging particles flying inward as the card materializes
            loop(0f.rf, 1f.rf, 16f.rf) { i ->
                val angle = i * 22.5f.rf
                val inDist = (1f.rf - progress) * (140f.rf + (i % 3f.rf) * 30f.rf)
                val pSize = progress * 6f.rf
                val pAlpha = progress
                save {
                    rotate(angle, cx, cy)
                    translate(inDist, 0f.rf)
                    paint {
                        color(0xFFBA68C8.toInt())
                        alpha(pAlpha)
                        style(RcPaintStyle.Fill)
                    }
                    drawCircle(cx, cy, pSize)
                }
            }
        }

        val customParticleExplodeExit = defineVisibilityAnimation { progress, w, h, x, y ->
            val cx = x + w / 2f.rf
            val cy = y + h / 2f.rf

            // Phase 1: Card shrinks and fades rapidly as explosion initiates
            val cardScale = max(0f.rf, 1f.rf - progress * 2.5f.rf)
            val cardAlpha = max(0f.rf, 1f.rf - progress * 2.5f.rf)
            save {
                paint { alpha(cardAlpha) }
                scale(cardScale, cardScale, cx, cy)
                drawComponentContent()
            }

            // Expanding shockwave ring
            val ringRadius = progress * 130f.rf
            val ringAlpha = max(0f.rf, 1f.rf - progress * 1.3f.rf)
            val ringStroke = (1f.rf - progress) * 3f.rf
            save {
                paint {
                    color(0xFFFFD54F.toInt())
                    style(RcPaintStyle.Stroke)
                    strokeWidth(ringStroke)
                    alpha(ringAlpha)
                }
                drawCircle(cx, cy, ringRadius)
            }

            // Exploding particle burst (24 fragments radiating outward with rotation and gravity)
            loop(0f.rf, 1f.rf, 24f.rf) { i ->
                val angle = i * 15f.rf
                val speed = 120f.rf + (i % 4f.rf) * 35f.rf
                val dist = progress * speed
                val gravityY = progress * progress * 50f.rf
                val pSize = max(0f.rf, (1f.rf - progress) * (8f.rf + (i % 3f.rf) * 3f.rf))
                val pAlpha = max(0f.rf, 1f.rf - progress)
                val pRot = i * 30f.rf + progress * 360f.rf
                save {
                    rotate(angle, cx, cy)
                    translate(dist, gravityY)
                    rotate(pRot, cx, cy)
                    paint {
                        color(0xFF9C27B0.toInt())
                        alpha(pAlpha)
                        style(RcPaintStyle.Fill)
                    }
                    drawRoundRect(
                        cx - pSize / 2f.rf,
                        cy - pSize / 2f.rf,
                        cx + pSize / 2f.rf,
                        cy + pSize / 2f.rf,
                        2f.rf,
                        2f.rf,
                    )
                }
            }

            // Secondary outer sparks (12 fast-moving bright golden embers)
            loop(0f.rf, 1f.rf, 12f.rf) { j ->
                val sparkAngle = j * 30f.rf + 15f.rf
                val sparkSpeed = 160f.rf + (j % 3f.rf) * 40f.rf
                val sparkDist = progress * sparkSpeed
                val sparkGravity = progress * progress * 70f.rf
                val sparkSize = max(0f.rf, (1f.rf - progress) * 4f.rf)
                val sparkAlpha = max(0f.rf, 1f.rf - progress)
                save {
                    rotate(sparkAngle, cx, cy)
                    translate(sparkDist, sparkGravity)
                    paint {
                        color(0xFFFFEB3B.toInt())
                        alpha(sparkAlpha)
                        style(RcPaintStyle.Fill)
                    }
                    drawCircle(cx, cy, sparkSize)
                }
            }
        }

        // Define Custom Visibility Animation 4: Cosmic Vortex / Black Hole Swirl
        val customVortexEnter = defineVisibilityAnimation { progress, w, h, x, y ->
            val cx = x + w / 2f.rf
            val cy = y + h / 2f.rf

            // Content emerges from the singularity with an outward spin and smooth fade-in
            val cardScale = progress
            val cardAlpha = progress
            val spinAngle = (1f.rf - progress) * (-540f.rf)
            save {
                paint { alpha(cardAlpha) }
                rotate(spinAngle, cx, cy)
                scale(cardScale, cardScale, cx, cy)
                drawComponentContent()
            }

            // Expanding outward particle spiral
            loop(0f.rf, 1f.rf, 18f.rf) { i ->
                val spiralAngle = i * 20f.rf - (1f.rf - progress) * 360f.rf
                val spiralDist = progress * (130f.rf - (i * 3f.rf))
                val pSize = progress * 5f.rf
                save {
                    rotate(spiralAngle, cx, cy)
                    translate(spiralDist, 0f.rf)
                    paint {
                        color(0xFF00E5FF.toInt())
                        alpha(progress)
                        style(RcPaintStyle.Fill)
                    }
                    drawCircle(cx, cy, pSize)
                }
            }
        }

        val customVortexExit = defineVisibilityAnimation { progress, w, h, x, y ->
            val cx = x + w / 2f.rf
            val cy = y + h / 2f.rf

            // 1. Content spins into singularity while shrinking and fading out
            val cardScale = max(0f.rf, 1f.rf - progress)
            val cardAlpha = max(0f.rf, 1f.rf - progress * 1.2f.rf)
            val spinAngle = progress * 720f.rf
            save {
                paint { alpha(cardAlpha) }
                rotate(spinAngle, cx, cy)
                scale(cardScale, cardScale, cx, cy)
                drawComponentContent()
            }

            // 2. Glowing accretion disk singularity ring
            val ringRadius = max(0f.rf, (1f.rf - progress) * (w / 2.5f.rf) + progress * 8f.rf)
            val ringAlpha = max(0f.rf, 1f.rf - progress * 0.8f.rf)
            val ringStroke = 2f.rf + (1f.rf - progress) * 4f.rf
            save {
                paint {
                    color(0xFF00E5FF.toInt())
                    style(RcPaintStyle.Stroke)
                    strokeWidth(ringStroke)
                    alpha(ringAlpha)
                }
                drawCircle(cx, cy, ringRadius)
            }

            // 3. Dual-arm spiral vortex particles swirling into the black hole
            loop(0f.rf, 1f.rf, 20f.rf) { i ->
                val spiralAngle = i * 18f.rf + progress * 540f.rf
                val spiralDist = (1f.rf - progress) * (150f.rf - (i * 4f.rf))
                val pSize = max(0f.rf, (1f.rf - progress) * 6f.rf)
                val pAlpha = max(0f.rf, 1f.rf - progress)
                save {
                    rotate(spiralAngle, cx, cy)
                    translate(spiralDist, 0f.rf)
                    paint {
                        color(0xFFE040FB.toInt())
                        alpha(pAlpha)
                        style(RcPaintStyle.Fill)
                    }
                    drawCircle(cx, cy, pSize)
                }
            }

            // 4. Inner intense energy core spark
            val coreSize = progress * (1f.rf - progress) * 40f.rf
            val coreAlpha = max(0f.rf, 1f.rf - progress)
            save {
                paint {
                    color(0xFFFFFFFF.toInt())
                    alpha(coreAlpha)
                    style(RcPaintStyle.Fill)
                }
                drawCircle(cx, cy, coreSize)
            }
        }

        // Define Custom Visibility Animation 5: Cyberpunk Hologram / Laser Scan & Glitch
        val customGlitchHologramEnter = defineVisibilityAnimation { progress, w, h, x, y ->
            val sweepY = y + h * progress
            val cardAlpha = min(1f.rf, progress * 1.5f.rf)

            // 1. Content is progressively revealed by a top-to-bottom laser scan clip with smooth
            // fade
            save {
                paint { alpha(cardAlpha) }
                clipRect(x - 5f.rf, y - 5f.rf, x + w + 5f.rf, sweepY)
                drawComponentContent()
            }

            // 2. High-intensity neon laser scanline at the leading edge
            val laserStroke = 3f.rf
            val laserAlpha = max(0f.rf, 1f.rf - max(0f.rf, (progress - 0.85f.rf) * 6.6f.rf))
            save {
                paint {
                    color(0xFF00E5FF.toInt())
                    style(RcPaintStyle.Stroke)
                    strokeWidth(laserStroke)
                    alpha(laserAlpha)
                }
                drawLine(x - 15f.rf, sweepY, x + w + 15f.rf, sweepY)
            }

            // 3. Sparks shooting off the laser scanning edge
            loop(0f.rf, 1f.rf, 10f.rf) { i ->
                val sparkX = x + (i * 28f.rf) % w
                val sparkOffsetY = sin(i * 12f.rf + progress * 20f.rf) * 12f.rf
                val sparkSize = 3f.rf
                save {
                    paint {
                        color(0xFFFFFFFF.toInt())
                        alpha(laserAlpha)
                        style(RcPaintStyle.Fill)
                    }
                    drawCircle(sparkX, sweepY + sparkOffsetY, sparkSize)
                }
            }
        }

        val customGlitchHologramExit = defineVisibilityAnimation { progress, w, h, x, y ->
            val jitterX = sin(progress * 35f.rf) * (progress * 16f.rf)
            val fadeAlpha = max(0f.rf, 1f.rf - progress)

            // 1. Cyan chromatic aberration phantom (offset left)
            val cyanOffset = progress * (-12f.rf)
            val phantomAlpha = max(0f.rf, 0.4f.rf * (1f.rf - progress))
            save {
                paint { alpha(phantomAlpha) }
                translate(cyanOffset + jitterX, 0f.rf)
                drawComponentContent()
            }

            // 2. Magenta chromatic aberration phantom (offset right)
            val magentaOffset = progress * 12f.rf
            save {
                paint { alpha(phantomAlpha) }
                translate(magentaOffset + jitterX, 0f.rf)
                drawComponentContent()
            }

            // 3. Main component with jitter and fade
            save {
                paint { alpha(fadeAlpha) }
                translate(jitterX, 0f.rf)
                drawComponentContent()
            }

            // 4. Horizontal holographic scanlines
            val scanlineAlpha = max(0f.rf, (1f.rf - progress) * 0.7f.rf)
            val scanlineStroke = 1.5f.rf
            loop(0f.rf, 10f.rf, h) { lineOffset ->
                save {
                    paint {
                        color(0xFF00E5FF.toInt())
                        alpha(scanlineAlpha)
                        style(RcPaintStyle.Stroke)
                        strokeWidth(scanlineStroke)
                    }
                    drawLine(x - 10f.rf, y + lineOffset, x + w + 10f.rf, y + lineOffset)
                }
            }

            // 5. Digital data block fragments dissolving vertically
            loop(0f.rf, 1f.rf, 12f.rf) { i ->
                val blockX = x + (i * 22f.rf) % w
                val blockY = y + ((i * 17f.rf) % h) - progress * 50f.rf
                val blockSize = max(0f.rf, (1f.rf - progress) * 8f.rf)
                val blockAlpha = max(0f.rf, 1f.rf - progress)
                save {
                    paint {
                        color(0xFF00E676.toInt())
                        alpha(blockAlpha)
                        style(RcPaintStyle.Fill)
                    }
                    drawRect(blockX, blockY, blockX + blockSize, blockY + blockSize)
                }
            }
        }

        // Define Custom Visibility Animation 6: AGSL Shader Liquid Dissolve & Plasma Melt
        val customShaderMeltEnter = defineVisibilityAnimation { id, progress, w, h, x, y ->
            val offscreenBitmap = createOffscreenBitmap()
            val pMelt = 1f.rf - progress
            val sId =
                shader(SHADER_MELT_AGSL) {
                    uniform("uTexture", offscreenBitmap)
                    uniform("uProgress", pMelt)
                    uniform("uResolution", w, h)
                }
            // 1. Render component to offscreen bitmap
            drawComponentToBitmap(id, offscreenBitmap)
            // 2. Render on main canvas with AGSL shader
            save {
                translate(x, y)
                paint { shader(sId) }
                drawRoundRect(0f.rf, 0f.rf, w, h, h * 0.35f.rf, h * 0.35f.rf)
                paint { clearShader() }
            }
        }

        val customShaderMeltExit = defineVisibilityAnimation { id, progress, w, h, x, y ->
            val offscreenBitmap = createOffscreenBitmap()
            val sId =
                shader(SHADER_MELT_AGSL) {
                    uniform("uTexture", offscreenBitmap)
                    uniform("uProgress", progress)
                    uniform("uResolution", w, h)
                }
            // 1. Render component to offscreen bitmap
            drawComponentToBitmap(id, offscreenBitmap)
            // 2. Render on main canvas with AGSL shader
            save {
                translate(x, y)
                paint { shader(sId) }
                drawRoundRect(0f.rf, 0f.rf, w, h, h * 0.35f.rf, h * 0.35f.rf)
                paint { clearShader() }
            }
        }

        // Define Custom Visibility Animation 7: AGSL Shader Snap Dust Disintegration
        val customSnapDustEnter = defineVisibilityAnimation { id, progress, w, h, x, y ->
            val offscreenBitmap = createOffscreenBitmap()
            val pSnap = 1f.rf - progress
            val sId =
                shader(SHADER_SNAP_DUST_AGSL) {
                    uniform("uTexture", offscreenBitmap)
                    uniform("uProgress", pSnap)
                    uniform("uResolution", w, h)
                }
            // 1. Render component to offscreen bitmap
            drawComponentToBitmap(id, offscreenBitmap)
            // 2. Render on main canvas with AGSL shader (with margin for drifting dust)
            save {
                translate(x, y)
                paint { shader(sId) }
                drawRect(-h * 0.5f.rf, -h * 0.5f.rf, w + h * 0.8f.rf, h + h * 0.5f.rf)
                paint { clearShader() }
            }
        }

        val customSnapDustExit = defineVisibilityAnimation { id, progress, w, h, x, y ->
            val offscreenBitmap = createOffscreenBitmap()
            val sId =
                shader(SHADER_SNAP_DUST_AGSL) {
                    uniform("uTexture", offscreenBitmap)
                    uniform("uProgress", progress)
                    uniform("uResolution", w, h)
                }
            // 1. Render component to offscreen bitmap
            drawComponentToBitmap(id, offscreenBitmap)
            // 2. Render on main canvas with AGSL shader (with margin for drifting dust)
            save {
                translate(x, y)
                paint { shader(sId) }
                drawRect(-h * 0.5f.rf, -h * 0.5f.rf, w + h * 0.8f.rf, h + h * 0.5f.rf)
                paint { clearShader() }
            }
        }

        // Define Custom Visibility Animation 8: AGSL Shader Black Hole Gravitational Singularity
        val customSingularityEnter = defineVisibilityAnimation { id, progress, w, h, x, y ->
            val offscreenBitmap = createOffscreenBitmap()
            val pWarp = 1f.rf - progress
            val sId =
                shader(SHADER_WARP_SINGULARITY_AGSL) {
                    uniform("uTexture", offscreenBitmap)
                    uniform("uProgress", pWarp)
                    uniform("uResolution", w, h)
                }
            drawComponentToBitmap(id, offscreenBitmap)
            save {
                translate(x, y)
                paint { shader(sId) }
                drawRect(-h * 0.7f.rf, -h * 0.7f.rf, w + h * 1.4f.rf, h + h * 1.4f.rf)
                paint { clearShader() }
            }
        }

        val customSingularityExit = defineVisibilityAnimation { id, progress, w, h, x, y ->
            val offscreenBitmap = createOffscreenBitmap()
            val sId =
                shader(SHADER_WARP_SINGULARITY_AGSL) {
                    uniform("uTexture", offscreenBitmap)
                    uniform("uProgress", progress)
                    uniform("uResolution", w, h)
                }
            drawComponentToBitmap(id, offscreenBitmap)
            save {
                translate(x, y)
                paint { shader(sId) }
                drawRect(-h * 0.7f.rf, -h * 0.7f.rf, w + h * 1.4f.rf, h + h * 1.4f.rf)
                paint { clearShader() }
            }
        }

        // Define Custom Visibility Animation 9: AGSL Shader Cyber Matrix Digital Teleport
        val customMatrixGlitchEnter = defineVisibilityAnimation { id, progress, w, h, x, y ->
            val offscreenBitmap = createOffscreenBitmap()
            val pMatrix = 1f.rf - progress
            val sId =
                shader(SHADER_MATRIX_GLITCH_AGSL) {
                    uniform("uTexture", offscreenBitmap)
                    uniform("uProgress", pMatrix)
                    uniform("uResolution", w, h)
                }
            drawComponentToBitmap(id, offscreenBitmap)
            save {
                translate(x, y)
                paint { shader(sId) }
                drawRect(-h * 0.5f.rf, -h * 0.5f.rf, w + h, h + h)
                paint { clearShader() }
            }
        }

        val customMatrixGlitchExit = defineVisibilityAnimation { id, progress, w, h, x, y ->
            val offscreenBitmap = createOffscreenBitmap()
            val sId =
                shader(SHADER_MATRIX_GLITCH_AGSL) {
                    uniform("uTexture", offscreenBitmap)
                    uniform("uProgress", progress)
                    uniform("uResolution", w, h)
                }
            drawComponentToBitmap(id, offscreenBitmap)
            save {
                translate(x, y)
                paint { shader(sId) }
                drawRect(-h * 0.5f.rf, -h * 0.5f.rf, w + h, h + h)
                paint { clearShader() }
            }
        }

        // Define Custom Visibility Animation 10: AGSL Shader Solar Fire Incinerate
        val customFireSweepEnter = defineVisibilityAnimation { id, progress, w, h, x, y ->
            val offscreenBitmap = createOffscreenBitmap()
            val pFire = 1f.rf - progress
            val sId =
                shader(SHADER_FIRE_SWEEP_AGSL) {
                    uniform("uTexture", offscreenBitmap)
                    uniform("uProgress", pFire)
                    uniform("uResolution", w, h)
                }
            drawComponentToBitmap(id, offscreenBitmap)
            save {
                translate(x, y)
                paint { shader(sId) }
                drawRect(-h * 0.5f.rf, -h * 0.8f.rf, w + h, h + h * 1.2f.rf)
                paint { clearShader() }
            }
        }

        val customFireSweepExit = defineVisibilityAnimation { id, progress, w, h, x, y ->
            val offscreenBitmap = createOffscreenBitmap()
            val sId =
                shader(SHADER_FIRE_SWEEP_AGSL) {
                    uniform("uTexture", offscreenBitmap)
                    uniform("uProgress", progress)
                    uniform("uResolution", w, h)
                }
            drawComponentToBitmap(id, offscreenBitmap)
            save {
                translate(x, y)
                paint { shader(sId) }
                drawRect(-h * 0.5f.rf, -h * 0.8f.rf, w + h, h + h * 1.2f.rf)
                paint { clearShader() }
            }
        }

        // Define Custom Visibility Animation 11: AGSL Shader Fluid Drain Exit & Fill Enter
        val customLiquidFillEnter = defineVisibilityAnimation { id, progress, w, h, x, y ->
            val offscreenBitmap = createOffscreenBitmap()
            val sId =
                shader(SHADER_LIQUID_FILL_ENTER_AGSL) {
                    uniform("uTexture", offscreenBitmap)
                    uniform("uProgress", progress)
                    uniform("uResolution", w, h)
                }
            drawComponentToBitmap(id, offscreenBitmap)
            save {
                translate(x, y)
                paint { shader(sId) }
                drawRect(-h * 0.5f.rf, -h * 0.5f.rf, w + h, h + h)
                paint { clearShader() }
            }
        }

        val customLiquidDrainExit = defineVisibilityAnimation { id, progress, w, h, x, y ->
            val offscreenBitmap = createOffscreenBitmap()
            val sId =
                shader(SHADER_LIQUID_DRAIN_EXIT_AGSL) {
                    uniform("uTexture", offscreenBitmap)
                    uniform("uProgress", progress)
                    uniform("uResolution", w, h)
                }
            drawComponentToBitmap(id, offscreenBitmap)
            save {
                translate(x, y)
                paint { shader(sId) }
                drawRect(-h * 0.5f.rf, -h * 0.5f.rf, w + h, h + h)
                paint { clearShader() }
            }
        }

        // Define Custom Visibility Animation 12: AGSL Shader Shard Blast & Reassemble
        val customShardExplodeEnter = defineVisibilityAnimation { id, progress, w, h, x, y ->
            val offscreenBitmap = createOffscreenBitmap()
            val pRev = 1f.rf - progress
            val sId =
                shader(SHADER_SHARD_EXPLOSION_AGSL) {
                    uniform("uTexture", offscreenBitmap)
                    uniform("uProgress", pRev)
                    uniform("uResolution", w, h)
                }
            drawComponentToBitmap(id, offscreenBitmap)
            save {
                translate(x, y)
                paint { shader(sId) }
                drawRect(-h * 2f.rf, -h * 2f.rf, w + h * 4f.rf, h + h * 4f.rf)
                paint { clearShader() }
            }
        }

        val customShardExplodeExit = defineVisibilityAnimation { id, progress, w, h, x, y ->
            val offscreenBitmap = createOffscreenBitmap()
            val sId =
                shader(SHADER_SHARD_EXPLOSION_AGSL) {
                    uniform("uTexture", offscreenBitmap)
                    uniform("uProgress", progress)
                    uniform("uResolution", w, h)
                }
            drawComponentToBitmap(id, offscreenBitmap)
            save {
                translate(x, y)
                paint { shader(sId) }
                drawRect(-h * 2f.rf, -h * 2f.rf, w + h * 4f.rf, h + h * 4f.rf)
                paint { clearShader() }
            }
        }

        // Define Custom Visibility Animation 13: AGSL Shader Liquid Chrome & Ferrofluid Morph
        val customLiquidChromeEnter = defineVisibilityAnimation { id, progress, w, h, x, y ->
            val offscreenBitmap = createOffscreenBitmap()
            val pRev = 1f.rf - progress
            val sId =
                shader(SHADER_LIQUID_CHROME_AGSL) {
                    uniform("uTexture", offscreenBitmap)
                    uniform("uProgress", pRev)
                    uniform("uResolution", w, h)
                }
            drawComponentToBitmap(id, offscreenBitmap)
            save {
                translate(x, y)
                paint { shader(sId) }
                drawRect(-h, -h, w + h, h + h)
                paint { clearShader() }
            }
        }

        val customLiquidChromeExit = defineVisibilityAnimation { id, progress, w, h, x, y ->
            val offscreenBitmap = createOffscreenBitmap()
            val sId =
                shader(SHADER_LIQUID_CHROME_AGSL) {
                    uniform("uTexture", offscreenBitmap)
                    uniform("uProgress", progress)
                    uniform("uResolution", w, h)
                }
            drawComponentToBitmap(id, offscreenBitmap)
            save {
                translate(x, y)
                paint { shader(sId) }
                drawRect(-h, -h, w + h, h + h)
                paint { clearShader() }
            }
        }

        Column(
            modifier = Modifier.fillMaxSize().padding(10f),
            horizontal = RcHorizontalPositioning.Center,
            vertical = RcColumnVerticalPositioning.Top,
        ) {
            Text("Custom Visibility Animations", color = 0xFF222222.toInt(), fontSize = 16.rsp)

            Spacer(Modifier.size(4f))

            // Card 1: Scale & Rotate custom transition
            Box(
                modifier =
                    Modifier.width(260f)
                        .height(30f)
                        .visibility(visState1)
                        .animationSpec(
                            motionDuration = 600f,
                            visibilityDuration = 1000f,
                            enterAnimation = AnimationSpec.ANIMATION.CUSTOM,
                            exitAnimation = AnimationSpec.ANIMATION.CUSTOM,
                            enterFunctionId = customScaleRotateEnter,
                            exitFunctionId = customScaleRotateExit,
                        )
                        .clip(RoundedRectShape(10f, 10f, 10f, 10f))
                        .background(0xFFFF5722.toInt()),
                horizontal = RcHorizontalPositioning.Center,
                vertical = RcColumnVerticalPositioning.Center,
            ) {
                Text("1. Scale & Rotate", color = 0xFFFFFFFF.toInt(), fontSize = 12.rsp)
            }

            Spacer(Modifier.size(2f))

            // Card 2: Slide & Ring custom transition
            Box(
                modifier =
                    Modifier.width(260f)
                        .height(30f)
                        .visibility(visState2)
                        .animationSpec(
                            motionDuration = 600f,
                            visibilityDuration = 1000f,
                            enterAnimation = AnimationSpec.ANIMATION.CUSTOM,
                            exitAnimation = AnimationSpec.ANIMATION.CUSTOM,
                            enterFunctionId = customSlideRingEnter,
                            exitFunctionId = customSlideRingExit,
                        )
                        .clip(RoundedRectShape(10f, 10f, 10f, 10f))
                        .background(0xFF3F51B5.toInt()),
                horizontal = RcHorizontalPositioning.Center,
                vertical = RcColumnVerticalPositioning.Center,
            ) {
                Text("2. Slide & Ring", color = 0xFFFFFFFF.toInt(), fontSize = 12.rsp)
            }

            Spacer(Modifier.size(2f))

            // Card 3: Particle Explosion custom transition
            Box(
                modifier =
                    Modifier.width(260f)
                        .height(30f)
                        .visibility(visState3)
                        .animationSpec(
                            motionDuration = 600f,
                            visibilityDuration = 1000f,
                            enterAnimation = AnimationSpec.ANIMATION.CUSTOM,
                            exitAnimation = AnimationSpec.ANIMATION.CUSTOM,
                            enterFunctionId = customParticleAssembleEnter,
                            exitFunctionId = customParticleExplodeExit,
                        )
                        .clip(RoundedRectShape(10f, 10f, 10f, 10f))
                        .background(0xFF9C27B0.toInt()),
                horizontal = RcHorizontalPositioning.Center,
                vertical = RcColumnVerticalPositioning.Center,
            ) {
                Text("3. Particle Explosion", color = 0xFFFFFFFF.toInt(), fontSize = 12.rsp)
            }

            Spacer(Modifier.size(2f))

            // Card 4: Cosmic Vortex custom transition
            Box(
                modifier =
                    Modifier.width(260f)
                        .height(30f)
                        .visibility(visState4)
                        .animationSpec(
                            motionDuration = 600f,
                            visibilityDuration = 1000f,
                            enterAnimation = AnimationSpec.ANIMATION.CUSTOM,
                            exitAnimation = AnimationSpec.ANIMATION.CUSTOM,
                            enterFunctionId = customVortexEnter,
                            exitFunctionId = customVortexExit,
                        )
                        .clip(RoundedRectShape(10f, 10f, 10f, 10f))
                        .background(0xFF1A237E.toInt()),
                horizontal = RcHorizontalPositioning.Center,
                vertical = RcColumnVerticalPositioning.Center,
            ) {
                Text("4. Cosmic Vortex", color = 0xFFFFFFFF.toInt(), fontSize = 12.rsp)
            }

            Spacer(Modifier.size(2f))

            // Card 5: Cyberpunk Hologram custom transition
            Box(
                modifier =
                    Modifier.width(260f)
                        .height(30f)
                        .visibility(visState5)
                        .animationSpec(
                            motionDuration = 600f,
                            visibilityDuration = 1000f,
                            enterAnimation = AnimationSpec.ANIMATION.CUSTOM,
                            exitAnimation = AnimationSpec.ANIMATION.CUSTOM,
                            enterFunctionId = customGlitchHologramEnter,
                            exitFunctionId = customGlitchHologramExit,
                        )
                        .clip(RoundedRectShape(10f, 10f, 10f, 10f))
                        .background(0xFF004D40.toInt()),
                horizontal = RcHorizontalPositioning.Center,
                vertical = RcColumnVerticalPositioning.Center,
            ) {
                Text("5. Cyberpunk Hologram", color = 0xFF00E5FF.toInt(), fontSize = 12.rsp)
            }

            Spacer(Modifier.size(2f))

            // Card 6: AGSL Shader Liquid Dissolve & Plasma Melt
            Box(
                modifier =
                    Modifier.width(260f)
                        .height(30f)
                        .visibility(visState6)
                        .animationSpec(
                            motionDuration = 600f,
                            visibilityDuration = 1000f,
                            enterAnimation = AnimationSpec.ANIMATION.CUSTOM,
                            exitAnimation = AnimationSpec.ANIMATION.CUSTOM,
                            enterFunctionId = customShaderMeltEnter,
                            exitFunctionId = customShaderMeltExit,
                        )
                        .clip(RoundedRectShape(10f, 10f, 10f, 10f))
                        .background(0xFF006064.toInt()),
                horizontal = RcHorizontalPositioning.Center,
                vertical = RcColumnVerticalPositioning.Center,
            ) {
                Text("6. AGSL Shader Melt", color = 0xFF80DEEA.toInt(), fontSize = 12.rsp)
            }

            Spacer(Modifier.size(2f))

            // Card 7: AGSL Shader Snap Dust Disintegration
            Box(
                modifier =
                    Modifier.width(260f)
                        .height(30f)
                        .visibility(visState7)
                        .animationSpec(
                            motionDuration = 600f,
                            visibilityDuration = 1000f,
                            enterAnimation = AnimationSpec.ANIMATION.CUSTOM,
                            exitAnimation = AnimationSpec.ANIMATION.CUSTOM,
                            enterFunctionId = customSnapDustEnter,
                            exitFunctionId = customSnapDustExit,
                        )
                        .clip(RoundedRectShape(10f, 10f, 10f, 10f))
                        .background(0xFF3E2723.toInt()),
                horizontal = RcHorizontalPositioning.Center,
                vertical = RcColumnVerticalPositioning.Center,
            ) {
                Text("7. Snap Dust", color = 0xFFFFD54F.toInt(), fontSize = 12.rsp)
            }

            Spacer(Modifier.size(2f))

            // Card 8: AGSL Shader Gravitational Singularity
            Box(
                modifier =
                    Modifier.width(260f)
                        .height(30f)
                        .visibility(visState8)
                        .animationSpec(
                            motionDuration = 600f,
                            visibilityDuration = 1000f,
                            enterAnimation = AnimationSpec.ANIMATION.CUSTOM,
                            exitAnimation = AnimationSpec.ANIMATION.CUSTOM,
                            enterFunctionId = customSingularityEnter,
                            exitFunctionId = customSingularityExit,
                        )
                        .clip(RoundedRectShape(10f, 10f, 10f, 10f))
                        .background(0xFF1A0033.toInt()),
                horizontal = RcHorizontalPositioning.Center,
                vertical = RcColumnVerticalPositioning.Center,
            ) {
                Text("8. Gravitational Singularity", color = 0xFFE040FB.toInt(), fontSize = 12.rsp)
            }

            Spacer(Modifier.size(2f))

            // Card 9: AGSL Shader Cyber Matrix Teleport
            Box(
                modifier =
                    Modifier.width(260f)
                        .height(30f)
                        .visibility(visState9)
                        .animationSpec(
                            motionDuration = 600f,
                            visibilityDuration = 1000f,
                            enterAnimation = AnimationSpec.ANIMATION.CUSTOM,
                            exitAnimation = AnimationSpec.ANIMATION.CUSTOM,
                            enterFunctionId = customMatrixGlitchEnter,
                            exitFunctionId = customMatrixGlitchExit,
                        )
                        .clip(RoundedRectShape(10f, 10f, 10f, 10f))
                        .background(0xFF003314.toInt()),
                horizontal = RcHorizontalPositioning.Center,
                vertical = RcColumnVerticalPositioning.Center,
            ) {
                Text("9. Cyber Matrix Teleport", color = 0xFF00E676.toInt(), fontSize = 12.rsp)
            }

            Spacer(Modifier.size(2f))

            // Card 10: AGSL Shader Solar Fire Incinerate
            Box(
                modifier =
                    Modifier.width(260f)
                        .height(30f)
                        .visibility(visState10)
                        .animationSpec(
                            motionDuration = 600f,
                            visibilityDuration = 1000f,
                            enterAnimation = AnimationSpec.ANIMATION.CUSTOM,
                            exitAnimation = AnimationSpec.ANIMATION.CUSTOM,
                            enterFunctionId = customFireSweepEnter,
                            exitFunctionId = customFireSweepExit,
                        )
                        .clip(RoundedRectShape(10f, 10f, 10f, 10f))
                        .background(0xFF4A1000.toInt()),
                horizontal = RcHorizontalPositioning.Center,
                vertical = RcColumnVerticalPositioning.Center,
            ) {
                Text("10. Solar Fire Sweep", color = 0xFFFFAB40.toInt(), fontSize = 12.rsp)
            }

            Spacer(Modifier.size(2f))

            // Card 11: AGSL Shader Fluid Drain & Fill (sequenced: exit BEFORE layout, enter AFTER
            // layout)
            Box(
                modifier =
                    Modifier.width(260f)
                        .height(30f)
                        .visibility(visState11)
                        .animationSpec(
                            motionDuration = 600f,
                            visibilityDuration = 1000f,
                            enterAnimation = AnimationSpec.ANIMATION.CUSTOM,
                            exitAnimation = AnimationSpec.ANIMATION.CUSTOM,
                            enterFunctionId = customLiquidFillEnter,
                            exitFunctionId = customLiquidDrainExit,
                            enterSequence = AnimationSpec.SEQUENCE.AFTER,
                            exitSequence = AnimationSpec.SEQUENCE.BEFORE,
                        )
                        .clip(RoundedRectShape(10f, 10f, 10f, 10f))
                        .background(0xFF004D40.toInt()),
                horizontal = RcHorizontalPositioning.Center,
                vertical = RcColumnVerticalPositioning.Center,
            ) {
                Text("11. Fluid Drain & Fill", color = 0xFF64FFDA.toInt(), fontSize = 12.rsp)
            }

            Spacer(Modifier.size(2f))

            // Card 12: AGSL Shader Shard Blast & Reassemble
            Box(
                modifier =
                    Modifier.width(260f)
                        .height(30f)
                        .visibility(visState12)
                        .animationSpec(
                            motionDuration = 600f,
                            visibilityDuration = 1000f,
                            enterAnimation = AnimationSpec.ANIMATION.CUSTOM,
                            exitAnimation = AnimationSpec.ANIMATION.CUSTOM,
                            enterFunctionId = customShardExplodeEnter,
                            exitFunctionId = customShardExplodeExit,
                        )
                        .clip(RoundedRectShape(10f, 10f, 10f, 10f))
                        .background(0xFF880E4F.toInt()),
                horizontal = RcHorizontalPositioning.Center,
                vertical = RcColumnVerticalPositioning.Center,
            ) {
                Text("12. Shard Blast & Reassemble", color = 0xFFFF80AB.toInt(), fontSize = 12.rsp)
            }

            Spacer(Modifier.size(2f))

            // Card 13: AGSL Shader Liquid Chrome & Mercury Morph
            Box(
                modifier =
                    Modifier.width(260f)
                        .height(30f)
                        .visibility(visState13)
                        .animationSpec(
                            motionDuration = 600f,
                            visibilityDuration = 1000f,
                            enterAnimation = AnimationSpec.ANIMATION.CUSTOM,
                            exitAnimation = AnimationSpec.ANIMATION.CUSTOM,
                            enterFunctionId = customLiquidChromeEnter,
                            exitFunctionId = customLiquidChromeExit,
                        )
                        .clip(RoundedRectShape(10f, 10f, 10f, 10f))
                        .background(0xFF263238.toInt()),
                horizontal = RcHorizontalPositioning.Center,
                vertical = RcColumnVerticalPositioning.Center,
            ) {
                Text("13. Liquid Chrome & Mercury", color = 0xFFECEFF1.toInt(), fontSize = 12.rsp)
            }

            Spacer(Modifier.size(6f))

            // Toggle Buttons: Row 1
            Row(modifier = Modifier.animationSpec(motionDuration = 600f)) {
                Box(
                    modifier =
                        Modifier.width(125f)
                            .height(24f)
                            .clip(RoundedRectShape(6f, 6f, 6f, 6f))
                            .background(0xFFE0E0E0.toInt())
                            .onClick { setValue(visState1, (visState1 + 2) % 4) },
                    horizontal = RcHorizontalPositioning.Center,
                    vertical = RcColumnVerticalPositioning.Center,
                ) {
                    Text("1: Scale/Rotate", color = 0xFF333333.toInt(), fontSize = 10.rsp)
                }
                Spacer(Modifier.size(8f))
                Box(
                    modifier =
                        Modifier.width(125f)
                            .height(24f)
                            .clip(RoundedRectShape(6f, 6f, 6f, 6f))
                            .background(0xFFE0E0E0.toInt())
                            .onClick { setValue(visState2, (visState2 + 2) % 4) },
                    horizontal = RcHorizontalPositioning.Center,
                    vertical = RcColumnVerticalPositioning.Center,
                ) {
                    Text("2: Slide/Ring", color = 0xFF333333.toInt(), fontSize = 10.rsp)
                }
            }

            Spacer(Modifier.size(2f))

            // Toggle Buttons: Row 2
            Row(modifier = Modifier.animationSpec(motionDuration = 600f)) {
                Box(
                    modifier =
                        Modifier.width(125f)
                            .height(24f)
                            .clip(RoundedRectShape(6f, 6f, 6f, 6f))
                            .background(0xFFE0E0E0.toInt())
                            .onClick { setValue(visState3, (visState3 + 2) % 4) },
                    horizontal = RcHorizontalPositioning.Center,
                    vertical = RcColumnVerticalPositioning.Center,
                ) {
                    Text("3: Explosion", color = 0xFF333333.toInt(), fontSize = 10.rsp)
                }
                Spacer(Modifier.size(8f))
                Box(
                    modifier =
                        Modifier.width(125f)
                            .height(24f)
                            .clip(RoundedRectShape(6f, 6f, 6f, 6f))
                            .background(0xFFE0E0E0.toInt())
                            .onClick { setValue(visState4, (visState4 + 2) % 4) },
                    horizontal = RcHorizontalPositioning.Center,
                    vertical = RcColumnVerticalPositioning.Center,
                ) {
                    Text("4: Cosmic Vortex", color = 0xFF333333.toInt(), fontSize = 10.rsp)
                }
            }

            Spacer(Modifier.size(2f))

            // Toggle Buttons: Row 3
            Row(modifier = Modifier.animationSpec(motionDuration = 600f)) {
                Box(
                    modifier =
                        Modifier.width(125f)
                            .height(24f)
                            .clip(RoundedRectShape(6f, 6f, 6f, 6f))
                            .background(0xFFE0E0E0.toInt())
                            .onClick { setValue(visState5, (visState5 + 2) % 4) },
                    horizontal = RcHorizontalPositioning.Center,
                    vertical = RcColumnVerticalPositioning.Center,
                ) {
                    Text("5: Hologram", color = 0xFF333333.toInt(), fontSize = 10.rsp)
                }
                Spacer(Modifier.size(8f))
                Box(
                    modifier =
                        Modifier.width(125f)
                            .height(24f)
                            .clip(RoundedRectShape(6f, 6f, 6f, 6f))
                            .background(0xFFE0E0E0.toInt())
                            .onClick { setValue(visState6, (visState6 + 2) % 4) },
                    horizontal = RcHorizontalPositioning.Center,
                    vertical = RcColumnVerticalPositioning.Center,
                ) {
                    Text("6: Shader Melt", color = 0xFF333333.toInt(), fontSize = 10.rsp)
                }
            }

            Spacer(Modifier.size(2f))

            // Toggle Buttons: Row 4
            Row(modifier = Modifier.animationSpec(motionDuration = 600f)) {
                Box(
                    modifier =
                        Modifier.width(125f)
                            .height(24f)
                            .clip(RoundedRectShape(6f, 6f, 6f, 6f))
                            .background(0xFFE0E0E0.toInt())
                            .onClick { setValue(visState7, (visState7 + 2) % 4) },
                    horizontal = RcHorizontalPositioning.Center,
                    vertical = RcColumnVerticalPositioning.Center,
                ) {
                    Text("7: Snap Dust", color = 0xFF333333.toInt(), fontSize = 10.rsp)
                }
                Spacer(Modifier.size(8f))
                Box(
                    modifier =
                        Modifier.width(125f)
                            .height(24f)
                            .clip(RoundedRectShape(6f, 6f, 6f, 6f))
                            .background(0xFFE0E0E0.toInt())
                            .onClick { setValue(visState8, (visState8 + 2) % 4) },
                    horizontal = RcHorizontalPositioning.Center,
                    vertical = RcColumnVerticalPositioning.Center,
                ) {
                    Text("8: Singularity", color = 0xFF333333.toInt(), fontSize = 10.rsp)
                }
            }

            Spacer(Modifier.size(2f))

            // Toggle Buttons: Row 5
            Row(modifier = Modifier.animationSpec(motionDuration = 600f)) {
                Box(
                    modifier =
                        Modifier.width(125f)
                            .height(24f)
                            .clip(RoundedRectShape(6f, 6f, 6f, 6f))
                            .background(0xFFE0E0E0.toInt())
                            .onClick { setValue(visState9, (visState9 + 2) % 4) },
                    horizontal = RcHorizontalPositioning.Center,
                    vertical = RcColumnVerticalPositioning.Center,
                ) {
                    Text("9: Matrix Glitch", color = 0xFF333333.toInt(), fontSize = 10.rsp)
                }
                Spacer(Modifier.size(8f))
                Box(
                    modifier =
                        Modifier.width(125f)
                            .height(24f)
                            .clip(RoundedRectShape(6f, 6f, 6f, 6f))
                            .background(0xFFE0E0E0.toInt())
                            .onClick { setValue(visState10, (visState10 + 2) % 4) },
                    horizontal = RcHorizontalPositioning.Center,
                    vertical = RcColumnVerticalPositioning.Center,
                ) {
                    Text("10: Solar Fire", color = 0xFF333333.toInt(), fontSize = 10.rsp)
                }
            }

            Spacer(Modifier.size(2f))

            // Toggle Buttons: Row 6
            Row(modifier = Modifier.animationSpec(motionDuration = 600f)) {
                Box(
                    modifier =
                        Modifier.width(125f)
                            .height(24f)
                            .clip(RoundedRectShape(6f, 6f, 6f, 6f))
                            .background(0xFFE0E0E0.toInt())
                            .onClick { setValue(visState11, (visState11 + 2) % 4) },
                    horizontal = RcHorizontalPositioning.Center,
                    vertical = RcColumnVerticalPositioning.Center,
                ) {
                    Text("11: Drain & Fill", color = 0xFF333333.toInt(), fontSize = 10.rsp)
                }
                Spacer(Modifier.size(8f))
                Box(
                    modifier =
                        Modifier.width(125f)
                            .height(24f)
                            .clip(RoundedRectShape(6f, 6f, 6f, 6f))
                            .background(0xFFE0E0E0.toInt())
                            .onClick { setValue(visState12, (visState12 + 2) % 4) },
                    horizontal = RcHorizontalPositioning.Center,
                    vertical = RcColumnVerticalPositioning.Center,
                ) {
                    Text("12: Shard Blast", color = 0xFF333333.toInt(), fontSize = 10.rsp)
                }
            }

            Spacer(Modifier.size(2f))

            // Toggle Buttons: Row 7
            Row(modifier = Modifier.animationSpec(motionDuration = 600f)) {
                Box(
                    modifier =
                        Modifier.width(258f)
                            .height(24f)
                            .clip(RoundedRectShape(6f, 6f, 6f, 6f))
                            .background(0xFFE0E0E0.toInt())
                            .onClick { setValue(visState13, (visState13 + 2) % 4) },
                    horizontal = RcHorizontalPositioning.Center,
                    vertical = RcColumnVerticalPositioning.Center,
                ) {
                    Text("13: Liquid Chrome", color = 0xFF333333.toInt(), fontSize = 10.rsp)
                }
            }
        }
    }
}
