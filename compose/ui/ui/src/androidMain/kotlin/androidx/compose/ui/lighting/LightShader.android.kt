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

package androidx.compose.ui.lighting

import android.graphics.RuntimeShader
import android.os.Build
import androidx.annotation.ChecksSdkIntAtLeast
import androidx.annotation.RequiresApi
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Shader
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection

@ChecksSdkIntAtLeast(api = Build.VERSION_CODES.TIRAMISU)
internal actual fun LightShader.Companion.isSupported(): Boolean =
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

internal actual fun LightShader(
    lights: List<Light>,
    surfaceMaterial: SurfaceMaterial,
    surfaceGeometrySpec: SurfaceGeometrySpec,
    density: Density,
    layoutDirection: LayoutDirection,
    size: Size,
): LightShader {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
        throw UnsupportedOperationException("LightShader is supported from API 33")
    }
    return AndroidLightShader(
        lights = lights,
        surfaceMaterial = surfaceMaterial,
        surfaceGeometrySpec = surfaceGeometrySpec,
        density = density,
        layoutDirection = layoutDirection,
        size = size,
    )
}

internal actual val LightShader.Companion.MaxLightsAllowed: Int
    get() = 4

/**
 * Android implementation of [LightShader] that evaluates a Blinn-Phong and Schlick Fresnel lighting
 * model for up to [LightShader.MaxLightsAllowed] lights via [RuntimeShader].
 */
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
private class AndroidLightShader(
    lights: List<Light>,
    surfaceMaterial: SurfaceMaterial,
    surfaceGeometrySpec: SurfaceGeometrySpec,
    density: Density,
    layoutDirection: LayoutDirection,
    size: Size,
) :
    BaseLightShader(
        lights = lights,
        surfaceMaterial = surfaceMaterial,
        surfaceGeometrySpec = surfaceGeometrySpec,
        density = density,
        layoutDirection = layoutDirection,
        size = size,
    ) {

    private val runtimeShader = RuntimeShader(BlinnPhongShader)

    override val nativeShader: Shader
        get() = runtimeShader

    init {
        initialize()
    }

    override fun uploadLights(
        lightCount: Int,
        lightTypes: IntArray,
        lightColors: FloatArray,
        lightPositions: FloatArray,
        lightDirections: FloatArray,
        lightFalloffRadii: FloatArray,
        lightCutoffCosines: FloatArray,
        lightIntensities: FloatArray,
    ) {
        runtimeShader.setIntUniform(ULightCount, lightCount)
        runtimeShader.setIntUniform(ULightTypes, lightTypes)
        runtimeShader.setFloatUniform(ULightColors, lightColors)
        runtimeShader.setFloatUniform(ULightPositions, lightPositions)
        runtimeShader.setFloatUniform(ULightDirVectors, lightDirections)
        runtimeShader.setFloatUniform(ULightFalloffRadii, lightFalloffRadii)
        runtimeShader.setFloatUniform(ULightCutoffCosines, lightCutoffCosines)
        runtimeShader.setFloatUniform(ULightIntensities, lightIntensities)
    }

    override fun uploadMaterial(material: SurfaceMaterial) {
        if (material.isDiffuseEnabled) {
            runtimeShader.setIntUniform(UDiffuseEnabled, 1)
            runtimeShader.setFloatUniform(UDiffuseColor, material.packDiffuseColor(materialColor))
        } else {
            runtimeShader.setIntUniform(UDiffuseEnabled, 0)
        }

        if (material.isSpecularEnabled) {
            runtimeShader.setIntUniform(USpecularEnabled, 1)
            runtimeShader.setFloatUniform(USpecularColor, material.packSpecularColor(materialColor))
            runtimeShader.setFloatUniform(USpecularShininess, material.specularShininess)
        } else {
            runtimeShader.setIntUniform(USpecularEnabled, 0)
        }

        if (material.isFresnelEnabled) {
            runtimeShader.setIntUniform(UFresnelEnabled, 1)
            runtimeShader.setFloatUniform(UFresnelReflectance, material.fresnelReflectivity)
            runtimeShader.setFloatUniform(UFresnelIntensity, material.fresnelIntensity)
        } else {
            runtimeShader.setIntUniform(UFresnelEnabled, 0)
        }
    }

    override fun uploadGeometry(geometry: SurfaceGeometry) {
        runtimeShader.setInputShader(UGeometryNormalShader, geometry.getOrBuildNormalShader())
        runtimeShader.setFloatUniform(UGeometryThickness, geometry.thickness)
    }

    companion object {
        // Light related uniforms
        const val ULightCount = "uLightCount"
        const val ULightTypes = "uLightTypes"
        const val ULightColors = "uLightColors"
        const val ULightPositions = "uLightPositions"
        const val ULightDirVectors = "uLightDirVectors"
        const val ULightCutoffCosines = "uLightCutoffCosines"
        const val ULightFalloffRadii = "uLightFalloffRadii"
        const val ULightIntensities = "uLightIntensities"

        // Material related uniforms
        const val UDiffuseEnabled = "uDiffuseEnabled"
        const val USpecularEnabled = "uSpecularEnabled"
        const val UFresnelEnabled = "uFresnelEnabled"
        const val UDiffuseColor = "uDiffuseColor"
        const val USpecularColor = "uSpecularColor"
        const val USpecularShininess = "uSpecularShininess"
        const val UFresnelReflectance = "uFresnelReflectance"
        const val UFresnelIntensity = "uFresnelIntensity"

        // Geometry related uniforms
        const val UGeometryNormalShader = "uGeometryNormalShader"
        const val UGeometryThickness = "uGeometryThickness"

        val BlinnPhongShader =
            """
            const int MAX_LIGHTS = ${LightShader.MaxLightsAllowed};

            const int LIGHT_TYPE_DIRECTIONAL = $LightTypeDirectional;
            const int LIGHT_TYPE_POINT = $LightTypePoint;
            const int LIGHT_TYPE_SPOT = $LightTypeSpot;

            /* Material related props */
            uniform int $UDiffuseEnabled;
            uniform int $USpecularEnabled;
            uniform int $UFresnelEnabled;
            uniform shader $UGeometryNormalShader;
            uniform half4 $UDiffuseColor;
            uniform half4 $USpecularColor;
            uniform float $USpecularShininess;
            uniform float $UFresnelReflectance;
            uniform float $UFresnelIntensity;
            uniform float $UGeometryThickness;

            /* Light related props */
            uniform int $ULightCount;
            uniform int $ULightTypes[MAX_LIGHTS];
            uniform float4 $ULightColors[MAX_LIGHTS];
            uniform float3 $ULightDirVectors[MAX_LIGHTS];
            uniform float3 $ULightPositions[MAX_LIGHTS];
            uniform float $ULightFalloffRadii[MAX_LIGHTS];
            uniform float $ULightCutoffCosines[MAX_LIGHTS];
            uniform float $ULightIntensities[MAX_LIGHTS];

            float3 safeNormalize(float3 value) {
                float lengthSquared = dot(value, value);
                return lengthSquared > 0.0 ? value * inversesqrt(lengthSquared) : float3(0.0);
            }

            half4 main(float2 fragCoord) {
                float4 normalSample = $UGeometryNormalShader.eval(fragCoord);

                // The geometry shader marks fragments outside of the surface with a zero alpha.
                if (normalSample.a == 0.0) {
                    return half4(0.0);
                }
                float3 normal = safeNormalize(normalSample.xyz);

                float3 surfacePos = float3(fragCoord, $UGeometryThickness);
                float3 viewDir = float3(0.0, 0.0, 1.0);

                float3 rgb = float3(0.0);
                float a = 0.0;

                for (int i = 0; i < MAX_LIGHTS; i++) {
                    if (i >= $ULightCount) {
                        break;
                    }

                    float attenuation = $ULightIntensities[i];
                    float3 lightDir = float3(0.0);

                    if ($ULightTypes[i] == LIGHT_TYPE_DIRECTIONAL) {
                        lightDir = safeNormalize($ULightDirVectors[i]);
                    } else {
                        float3 toLight = $ULightPositions[i] - surfacePos;
                        lightDir = safeNormalize(toLight);

                        float radius = $ULightFalloffRadii[i];
                        float falloff =
                            radius > 0.0 ? saturate(1.0 - length(toLight) / radius) : 1.0;
                        attenuation *= falloff * falloff;

                        if ($ULightTypes[i] == LIGHT_TYPE_SPOT) {
                            float3 spotAxis = float3(
                                $ULightDirVectors[i].xy,
                                $ULightDirVectors[i].z + $UGeometryThickness
                            );
                            float cosToFragment =
                                dot(-lightDir, safeNormalize(spotAxis));
                            attenuation *= step($ULightCutoffCosines[i], cosToFragment);
                        }
                    }

                    float nDotL = dot(normal, lightDir);
                    if (nDotL > 0.0 && attenuation > 0.0) {
                        float3 lightRgb = $ULightColors[i].rgb;
                        float lightA = $ULightColors[i].a * attenuation;

                        if ($UDiffuseEnabled == 1) {
                            float w = $UDiffuseColor.a * lightA * nDotL;
                            rgb += $UDiffuseColor.rgb * lightRgb * w;
                            a += w;
                        }

                        if ($USpecularEnabled == 1) {
                            float3 halfDir = safeNormalize(lightDir + viewDir);
                            float specularFactor = pow(max(dot(normal, halfDir), 0.0), $USpecularShininess);
                            float w = $USpecularColor.a * lightA * specularFactor;
                            rgb += $USpecularColor.rgb * lightRgb * w;
                            a += w;
                        }

                        if ($UFresnelEnabled == 1) {
                            float fresnelFactor = pow(1.0 - saturate(dot(normal, viewDir)), 5.0);
                            float f = $UFresnelReflectance + (1.0 - $UFresnelReflectance) * fresnelFactor;
                            float w = lightA * f * $UFresnelIntensity * nDotL;
                            rgb += lightRgb * w;
                            a += w;
                        }
                    }
                }

                float outA = saturate(a);
                if (outA <= 0.0) {
                    return half4(0.0);
                }

                half3 color = fromLinearSrgb(half3(rgb / outA));
                return half4(saturate(color) * outA, outA);
            }
            """
    }
}
