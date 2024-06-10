/*
 * Copyright 2023 The Android Open Source Project
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

#import <Foundation/Foundation.h>
#import <QuartzCore/QuartzCore.h>
#import <Metal/Metal.h>

#import "CMPMacros.h"

NS_ASSUME_NONNULL_BEGIN

/**
 A handler class for managing Metal drawables explicitly using raw pointers.
 This class encapsulates the lifecycle management of drawable objects,
 facilitating the use in environments where automatic reference counting (ARC)
 mixed with Kotlin/Native memory model that  leads to violation of practices enstated by Apple (namely, not releasing drawables as soon as possible), which lead to inadequate memory spikes during drawable size updates across consequent frames.
 @see https://developer.apple.com/library/archive/documentation/3DDrawing/Conceptual/MTLBestPracticesGuide/Drawables.html

 The class methods handle the acquisition, release, and presentation of
 drawable objects associated with a given CAMetalLayer. Usage of raw pointers
 helps in explicitly controlling the drawable lifecycle, preventing application from keeping drawables and their pools alive longer, than needed, due to awaiting deallocation by GC.
 */
@interface CMPMetalDrawablesHandler : NSObject

/// Initializes the handler with a given Metal layer.
/// @param metalLayer The CAMetalLayer instance to be associated with this handler.
- (instancetype)initWithMetalLayer:(CAMetalLayer *)metalLayer;

/// Retrieves the next drawable object from the associated Metal layer.
/// @return A raw pointer to the next drawable object, ownership is transferred to the caller.
- (void * CMP_OWNED)nextDrawable;

/// Releases a drawable object, indicating that it is no longer in use by the caller.
/// @param drawablePtr A raw pointer to the drawable to be released, indicating transfer of ownership back to the handler.
- (void)releaseDrawable:(void * CMP_CONSUMED)drawablePtr;

/// Retrieves the texture of a drawable without transferring ownership.
/// @param drawablePtr A raw pointer to the drawable from which to get the texture.
/// @return A raw pointer to the texture of the drawable, ownership is not transferred.
- (void * CMP_BORROWED)drawableTexture:(void * CMP_BORROWED)drawablePtr;

/// Presents a drawable to the screen immediately.
/// @param drawablePtr A raw pointer to the drawable to be presented, indicating transfer of ownership.
- (void)presentDrawable:(void * CMP_CONSUMED)drawablePtr;

/// Schedules the presentation of a drawable on a specific command buffer.
/// @param drawablePtr A raw pointer to the drawable to be presented, indicating transfer of ownership.
/// @param commandBuffer The command buffer on which the drawable will be scheduled for presentation.
- (void)scheduleDrawablePresentation:(void * CMP_CONSUMED)drawablePtr onCommandBuffer:(id <MTLCommandBuffer>)commandBuffer;

@end

NS_ASSUME_NONNULL_END
