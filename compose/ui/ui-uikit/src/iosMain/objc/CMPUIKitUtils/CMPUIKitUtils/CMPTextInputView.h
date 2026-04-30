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

#import <UIKit/UIKit.h>
#import "CMPMacros.h"

@interface CMPTextInputView : UIView <UITextInput>

NS_ASSUME_NONNULL_BEGIN
- (nullable UITextPosition *)positionWithinRangeFarthestInDirection:(UITextRange *)range
                                                farthestInDirection:(UITextLayoutDirection)direction CMP_ABSTRACT_FUNCTION;
- (nullable UITextPosition *)positionWithinRangeAtCharacterOffset:(UITextRange *)range
                                                atCharacterOffset:(NSInteger)offset CMP_ABSTRACT_FUNCTION;
NS_ASSUME_NONNULL_END

- (void)activateTextInputInteractionIfNeeded;

- (void)deactivateTextInputInteractionIfNeeded;

@end
