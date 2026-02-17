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

#import <Foundation/Foundation.h>
#import <Metal/Metal.h>
#import <IOSurface/IOSurfaceRef.h>

NS_ASSUME_NONNULL_BEGIN

/**
 * IOSurface-backed Metal drawable from a managed pool.
 * Managed by CMPMetalLayer with explicit lifecycle control.
 */
@interface CMPDrawable : NSObject

/// Metal texture for rendering.
@property (nonatomic, readonly) id<MTLTexture> texture;

/// Backing IOSurface displayed by the layer.
@property (nonatomic, readonly) IOSurfaceRef surface;

/// Tracks GPU command buffer completion.
@property (nonatomic, assign) BOOL isWaitingForCommandBufferCompletion;

/// Last presentation timestamp for pool management.
@property (nonatomic, assign) CFTimeInterval presentedTime;

/// Reference to the reusable associated skia surface.
@property (nonatomic, weak, nullable) id associatedSkiaSurface;

- (instancetype)initWithTexture:(id<MTLTexture>)texture surface:(IOSurfaceRef)surface;

@end

NS_ASSUME_NONNULL_END
