/*
 * Copyright 2024 The Android Open Source Project
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

#import <UIKit/UIKit.h>
#import "CMPMacros.h"

NS_ASSUME_NONNULL_BEGIN

/// Class that propery exposes the methods of `UIDropInteractionDelegate` to Kotlin without signature conflicts
@interface CMPDropInteractionProxy : NSObject <UIDropInteractionDelegate>

- (BOOL)canHandleSession:(id<UIDropSession>)session interaction:(UIDropInteraction *)interaction CMP_ABSTRACT_FUNCTION;

- (void)performDropFromSession:(id<UIDropSession>)session interaction:(UIDropInteraction *)interaction CMP_ABSTRACT_FUNCTION;

- (UIDropProposal *)proposalForSessionUpdate:(id<UIDropSession>)session interaction:(UIDropInteraction *)interaction CMP_ABSTRACT_FUNCTION;

- (void)sessionDidEnd:(id<UIDropSession>)session interaction:(UIDropInteraction *) interaction CMP_ABSTRACT_FUNCTION;

- (void)sessionDidEnter:(id<UIDropSession>)session interaction:(UIDropInteraction *)interaction CMP_ABSTRACT_FUNCTION;

- (void)sessionDidExit:(id<UIDropSession>)session interaction:(UIDropInteraction *)interaction CMP_ABSTRACT_FUNCTION;

@end

/// Helper category for decoding `NSItemProvider` into some typed object
@interface NSItemProvider (CMPDecoding)

- (void)cmp_loadString:(void (^)(NSString  * _Nullable result, NSError *error))completionHandler;

- (void)cmp_loadAny:(Class)objectClass onCompletion:(void (^)(id _Nullable result, NSError *_Nullable error))completionHandler;

@end

NS_ASSUME_NONNULL_END
