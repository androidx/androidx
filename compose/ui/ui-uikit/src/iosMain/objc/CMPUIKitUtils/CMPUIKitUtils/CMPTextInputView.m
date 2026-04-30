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

#import "CMPTextInputView.h"

@implementation CMPTextInputView

@synthesize beginningOfDocument;
@synthesize hasText;
@synthesize inputDelegate;
@synthesize markedTextRange;
@synthesize selectedTextRange;
@synthesize tokenizer;
@synthesize endOfDocument;
@synthesize markedTextStyle;

- (UITextPosition *)positionWithinRange:(UITextRange *)range atCharacterOffset:(NSInteger)offset {
    return [self positionWithinRangeAtCharacterOffset:range atCharacterOffset:offset];
}

- (UITextPosition *)positionWithinRange:(UITextRange *)range farthestInDirection:(UITextLayoutDirection)direction {
    return [self positionWithinRangeFarthestInDirection:range farthestInDirection:direction];
}

- (NSWritingDirection)baseWritingDirectionForPosition:(nonnull UITextPosition *)position inDirection:(UITextStorageDirection)direction {
    CMP_ABSTRACT_FUNCTION_CALLED
}

- (CGRect)caretRectForPosition:(nonnull UITextPosition *)position {
    CMP_ABSTRACT_FUNCTION_CALLED
}

- (nullable UITextRange *)characterRangeAtPoint:(CGPoint)point {
    CMP_ABSTRACT_FUNCTION_CALLED
}

- (nullable UITextRange *)characterRangeByExtendingPosition:(nonnull UITextPosition *)position inDirection:(UITextLayoutDirection)direction {
    CMP_ABSTRACT_FUNCTION_CALLED
}

- (nullable UITextPosition *)closestPositionToPoint:(CGPoint)point {
    CMP_ABSTRACT_FUNCTION_CALLED
}

- (nullable UITextPosition *)closestPositionToPoint:(CGPoint)point withinRange:(nonnull UITextRange *)range {
    CMP_ABSTRACT_FUNCTION_CALLED
}

- (NSComparisonResult)comparePosition:(nonnull UITextPosition *)position toPosition:(nonnull UITextPosition *)other {
    CMP_ABSTRACT_FUNCTION_CALLED
}

- (CGRect)firstRectForRange:(nonnull UITextRange *)range {
    CMP_ABSTRACT_FUNCTION_CALLED
}

- (NSInteger)offsetFromPosition:(nonnull UITextPosition *)from toPosition:(nonnull UITextPosition *)toPosition {
    CMP_ABSTRACT_FUNCTION_CALLED
}

- (nullable UITextPosition *)positionFromPosition:(nonnull UITextPosition *)position inDirection:(UITextLayoutDirection)direction offset:(NSInteger)offset {
    CMP_ABSTRACT_FUNCTION_CALLED
}

- (nullable UITextPosition *)positionFromPosition:(nonnull UITextPosition *)position offset:(NSInteger)offset {
    CMP_ABSTRACT_FUNCTION_CALLED
}

- (void)replaceRange:(nonnull UITextRange *)range withText:(nonnull NSString *)text {
    CMP_ABSTRACT_FUNCTION_CALLED
}

- (nonnull NSArray<UITextSelectionRect *> *)selectionRectsForRange:(nonnull UITextRange *)range {
    CMP_ABSTRACT_FUNCTION_CALLED
}

- (void)setBaseWritingDirection:(NSWritingDirection)writingDirection forRange:(nonnull UITextRange *)range {
    CMP_ABSTRACT_FUNCTION_CALLED
}

- (void)setMarkedText:(nullable NSString *)markedText selectedRange:(NSRange)selectedRange {
    CMP_ABSTRACT_FUNCTION_CALLED
}

- (nullable NSString *)textInRange:(nonnull UITextRange *)range {
    CMP_ABSTRACT_FUNCTION_CALLED
}

- (nullable UITextRange *)textRangeFromPosition:(nonnull UITextPosition *)fromPosition toPosition:(nonnull UITextPosition *)toPosition {
    CMP_ABSTRACT_FUNCTION_CALLED
}

- (void)unmarkText {
    CMP_ABSTRACT_FUNCTION_CALLED
}

- (nullable UITextPosition *)positionWithinRangeFarthestInDirection:(UITextRange *)range
                                                farthestInDirection:(UITextLayoutDirection)direction {
    CMP_ABSTRACT_FUNCTION_CALLED
}

- (nullable UITextPosition *)positionWithinRangeAtCharacterOffset:(UITextRange *)range
                                                atCharacterOffset:(NSInteger)offset {
    CMP_ABSTRACT_FUNCTION_CALLED
}

- (void)deleteBackward {
    CMP_ABSTRACT_FUNCTION_CALLED
}

- (void)insertText:(nonnull NSString *)text {
    CMP_ABSTRACT_FUNCTION_CALLED
}

- (void)activateTextInputInteractionIfNeeded {
    if (@available(iOS 17, *)) {
        for (id<UIInteraction> interaction in self.interactions) {
            if ([interaction isKindOfClass:[UITextSelectionDisplayInteraction class]]) {
                [((UITextSelectionDisplayInteraction *)interaction) setActivated:YES];
            }
        }
    }
}

- (void)deactivateTextInputInteractionIfNeeded {
    if (@available(iOS 17, *)) {
        for (id<UIInteraction> interaction in self.interactions) {
            if ([interaction isKindOfClass:[UITextSelectionDisplayInteraction class]]) {
                [((UITextSelectionDisplayInteraction *)interaction) setActivated:NO];
            }
        }
    }
}

@end
