#version 100
// Copyright 2022 The Android Open Source Project
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

// ES2 Vignetting fragment shader
precision highp float;
uniform sampler2D uTexSampler;

uniform int uShouldVignetteColor;
uniform int uShouldVignetteAlpha;
uniform float uInnerRadius;
uniform float uOuterRadius;
uniform float uAspectRatio;
varying vec2 vTexSamplingCoord;

void main() {
    vec4 inputColor = texture2D(uTexSampler, vTexSamplingCoord);

    // Update x and y coords to [-1, 1]
    float recenteredX = 2.0 * (vTexSamplingCoord.x - 0.5);
    float recenteredY = 2.0 * (vTexSamplingCoord.y - 0.5);

    // Un-normalize y coord so that the size of units match on x and y axes, based on an x-axis
    // with data in [-1, 1]
    float aspectCorrectY = recenteredY / uAspectRatio;
    float invAspectRatio = 1.0 / uAspectRatio;
    // Calculate maxRadius, the distance from the center to a corner of the image.
    float maxRadius = sqrt(1.0 + invAspectRatio * invAspectRatio);
    // Calculate radial position of the current texture coordinate.
    float radius = sqrt(recenteredX * recenteredX + aspectCorrectY * aspectCorrectY);
    // Normalize the radius based on the distance to the corner.
    float normalizedRadius = radius / maxRadius;

    // Calculate the vignette amount.  Data outside of the outer radius is set to 0.  Data inside of
    // the inner radius is unchanged.  Data between is interpolated linearly.
    float vignetteAmount = 0.0;
    if (normalizedRadius > uOuterRadius) {
        vignetteAmount = 1.0;
    } else if (normalizedRadius > uInnerRadius) {
        vignetteAmount = (normalizedRadius - uInnerRadius) / (uOuterRadius - uInnerRadius);
    }

    // Apply the vignetting.
    gl_FragColor.rgba = inputColor.rgba;

    bool vignetteAlpha = uShouldVignetteAlpha > 0;
    bool vignetteColor = uShouldVignetteColor > 0;

    if (vignetteColor && vignetteAlpha) {
        gl_FragColor.rgba = inputColor.rgba * (1.0 - vignetteAmount);
    } else if (vignetteColor) {
        gl_FragColor.rgb = inputColor.rgb * (1.0 - vignetteAmount);
    } else if (vignetteAlpha) {
        gl_FragColor.a = inputColor.a * (1.0 - vignetteAmount);
    }
}