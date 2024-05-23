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

#import "CMPEditMenuView.h"

@interface CMPEditMenuView() <UIEditMenuInteractionDelegate>

@property (weak, nonatomic, nullable) UIView *rootView;

@property (copy, nonatomic, nullable) void (^copyBlock)(void);
@property (copy, nonatomic, nullable) void (^cutBlock)(void);
@property (copy, nonatomic, nullable) void (^pasteBlock)(void);
@property (copy, nonatomic, nullable) void (^selectAllBlock)(void);

@property (strong, nonatomic, nullable) dispatch_block_t showContextMenuBlock;
@property (strong, nonatomic, nullable) dispatch_block_t presentInteractionBlock;

@property (assign, nonatomic) CGRect targetRect;
@property (assign, nonatomic) BOOL isEditMenuShown;
/// Due to the internal implementation of UIEditMenuInteraction, it disappears with animation when a touch is detected.
/// HACK: Keep tracking incoming touches to show UIEditMenuInteraction again after a short delay.
@property (assign, nonatomic) BOOL isPossibleTouchDetected;

@property (readwrite) UIEditMenuInteraction* editInteraction API_AVAILABLE(ios(16.0));

@end

@implementation CMPEditMenuView

id _editInteraction;

- (void)showEditMenuAtRect:(CGRect)targetRect
                      copy:(void (^)(void))copyBlock
                       cut:(void (^)(void))cutBlock
                     paste:(void (^)(void))pasteBlock
                 selectAll:(void (^)(void))selectAllBlock {
    BOOL contextMenuItemsChanged = [self contextMenuItemsChangedCopy:copyBlock
                                                                 cut:cutBlock
                                                               paste:pasteBlock
                                                           selectAll:selectAllBlock];
    BOOL positionChanged = !CGRectEqualToRect(self.targetRect, targetRect);
    BOOL isTargetVisible = CGRectIntersectsRect(self.bounds, targetRect);
    
    if (!isTargetVisible) {
        [self hideEditMenu];
        return;
    }

    self.targetRect = targetRect;
    self.copyBlock = copyBlock;
    self.cutBlock = cutBlock;
    self.pasteBlock = pasteBlock;
    self.selectAllBlock = selectAllBlock;
    self.isEditMenuShown = YES;

    if (@available(iOS 16, *)) {
        if (self.editInteraction == nil) {
            dispatch_async(dispatch_get_main_queue(), ^{
                self.editInteraction = [[UIEditMenuInteraction alloc] initWithDelegate:self];
                [self addInteraction:self.editInteraction];
                [self presentEditMenuInteraction];
                self.isPossibleTouchDetected = NO;
            });
        } else {
            if (self.isPossibleTouchDetected) {
                [self cancelPresentEditMenuInteraction];
                [self schedulePresentEditMenuInteraction];
            } else {
                if (contextMenuItemsChanged) {
                    [self.editInteraction reloadVisibleMenu];
                }
                if (positionChanged) {
                    [self.editInteraction updateVisibleMenuPositionAnimated:NO];
                }
            }
        }
    } else {
        if (contextMenuItemsChanged || positionChanged) {
            [self hideEditMenu];
            [self scheduleShowMenuController];
        }
    }
}

- (UIView *)hitTest:(CGPoint)point withEvent:(UIEvent *)event {
    self.isPossibleTouchDetected = YES;
    return [super hitTest:point withEvent:event];
}

- (void)scheduleShowMenuController {
    [self cancelShowMenuController];

    __weak __auto_type weak_self = self;
    self.showContextMenuBlock = dispatch_block_create(0 ,^{
        __auto_type self = weak_self;
        if (@available(iOS 13, *)) {
            [[UIMenuController sharedMenuController] showMenuFromView:self rect:self.targetRect];
        } else {
            [[UIMenuController sharedMenuController] setTargetRect:self.targetRect inView:self];
            [[UIMenuController sharedMenuController] setMenuVisible:YES];
        }
        self.showContextMenuBlock = nil;
    });
    dispatch_after(dispatch_time(DISPATCH_TIME_NOW, (int64_t)([self editMenuDelay] * NSEC_PER_SEC)),
                   dispatch_get_main_queue(),
                   self.showContextMenuBlock);
}

- (void)cancelShowMenuController {
    if (self.showContextMenuBlock != nil) {
        dispatch_block_cancel(self.showContextMenuBlock);
        self.showContextMenuBlock = nil;
    }
}

- (NSTimeInterval)editMenuDelay {
    return 0.25;
}

- (UIEditMenuInteraction *)editInteraction API_AVAILABLE(ios(16.0)) {
    return _editInteraction;
}

- (void)setEditInteraction:(UIEditMenuInteraction *)editInteraction API_AVAILABLE(ios(16.0)) {
    _editInteraction = editInteraction;
}

- (void)presentEditMenuInteraction API_AVAILABLE(ios(16.0)) {
    NSAssert(self.editInteraction != nil, @"Edit Interaction must be initialized");

    UIEditMenuConfiguration *config = [UIEditMenuConfiguration configurationWithIdentifier:nil
                                                                               sourcePoint:self.targetRect.origin];
    [self.editInteraction presentEditMenuWithConfiguration:config];
}

- (void)schedulePresentEditMenuInteraction API_AVAILABLE(ios(16.0)) {
    __weak __auto_type weak_self = self;
    self.presentInteractionBlock = dispatch_block_create(0 ,^{
        __auto_type self = weak_self;
        [self presentEditMenuInteraction];
        self.presentInteractionBlock = nil;
        self.isPossibleTouchDetected = NO;
    });
    dispatch_after(dispatch_time(DISPATCH_TIME_NOW, (int64_t)([self editMenuDelay] * NSEC_PER_SEC)),
                   dispatch_get_main_queue(),
                   self.presentInteractionBlock);
}

- (void)cancelPresentEditMenuInteraction API_AVAILABLE(ios(16.0)) {
    if (self.presentInteractionBlock != nil) {
        dispatch_block_cancel(self.presentInteractionBlock);
    }
}

- (BOOL)canBecomeFirstResponder {
    return YES;
}

- (void)hideEditMenu {
    self.isEditMenuShown = NO;
    if (@available(iOS 16, *)) {
        [self cancelPresentEditMenuInteraction];

        if (self.editInteraction != nil) {
            [self.editInteraction dismissMenu];
            [self removeInteraction:self.editInteraction];
            self.editInteraction = nil;
        }
    } else if (@available(iOS 13, *)) {
        [self cancelShowMenuController];
        [[UIMenuController sharedMenuController] hideMenu];
    } else {
        [self cancelShowMenuController];
        [[UIMenuController sharedMenuController] setMenuVisible:NO];
    }
}

- (BOOL)contextMenuItemsChangedCopy:(void (^)(void))copyBlock
                                cut:(void (^)(void))cutBlock
                              paste:(void (^)(void))pasteBlock
                          selectAll:(void (^)(void))selectAllBlock {
    return ((self.copyBlock == nil) != (copyBlock == nil) ||
            (self.cutBlock == nil) != (cutBlock == nil) ||
            (self.pasteBlock == nil) != (pasteBlock == nil) ||
            (self.selectAllBlock == nil) != (selectAllBlock == nil));
}

- (BOOL)canPerformAction:(SEL)action withSender:(id)sender {
    return ((@selector(copy:) == action && self.copyBlock != nil) ||
            (@selector(paste:) == action && self.pasteBlock != nil) ||
            (@selector(cut:) == action && self.cutBlock != nil) ||
            (@selector(selectAll:) == action && self.selectAllBlock != nil));
}

- (void)copy:(id)sender {
    if (self.copyBlock != nil) {
        self.copyBlock();
    }
}

- (void)paste:(id)sender {
    if (self.pasteBlock != nil) {
        self.pasteBlock();
    }
}

- (void)cut:(id)sender {
    if (self.cutBlock != nil) {
        self.cutBlock();
    }
}

- (void)selectAll:(id)sender {
    if (self.selectAllBlock != nil) {
        self.selectAllBlock();
    }
}

- (CGRect)editMenuInteraction:(UIEditMenuInteraction *)interaction
   targetRectForConfiguration:(UIEditMenuConfiguration *)configuration API_AVAILABLE(ios(16.0)) {
    return self.targetRect;
}

@end
