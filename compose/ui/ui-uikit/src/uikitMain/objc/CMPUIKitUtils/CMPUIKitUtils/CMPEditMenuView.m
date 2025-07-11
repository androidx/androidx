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

@interface CMPEditMenuViewRegister: NSObject

@property (nonatomic, strong) NSMutableSet<CMPEditMenuView *> *trackedMenus;

@end

@implementation CMPEditMenuViewRegister

+ (instancetype)shared {
    static CMPEditMenuViewRegister *sharedInstance = nil;
    static dispatch_once_t onceToken;
    dispatch_once(&onceToken, ^{
        sharedInstance = [[self alloc] init];
    });
    return sharedInstance;
}

- (instancetype)init {
    self = [super init];
    if (self) {
        _trackedMenus = [NSMutableSet new];
    }
    return self;
}

- (void)addEditMenu:(CMPEditMenuView *)editMenu {
    [self.trackedMenus addObject:editMenu];
}

- (void)removeEditMenu:(CMPEditMenuView *)editMenu {
    [self.trackedMenus removeObject:editMenu];
}

- (void)hideAllMenusSkipping:(CMPEditMenuView *)skipEditMenuView {
    [self.trackedMenus enumerateObjectsUsingBlock:^(CMPEditMenuView * _Nonnull menuView, BOOL * _Nonnull stop) {
        if (menuView != skipEditMenuView) {
            [menuView hideEditMenu];
        }
    }];
}

@end


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

    if (@available(iOS 16, *)) {
        [[CMPEditMenuViewRegister shared] hideAllMenusSkipping:self];
        if (self.editInteraction == nil || contextMenuItemsChanged || !self.isEditMenuShown) {
            BOOL isFirstMenuPresentation = self.presentInteractionBlock == nil;
            [self cancelPresentEditMenuInteraction];
            NSTimeInterval delay = isFirstMenuPresentation ? 0 : [self editMenuDelay];
            [self schedulePresentEditMenuInteractionWithDelay:delay];
        } else if (positionChanged) {
            [self.editInteraction updateVisibleMenuPositionAnimated:NO];
        }
    } else {
        self.isEditMenuShown = YES;
        if (contextMenuItemsChanged || positionChanged) {
            [self hideEditMenu];
            [self scheduleShowMenuController];
        }
    }
}

- (void)didMoveToWindow {
    [super didMoveToWindow];
    
    if (self.window != nil) {
        [[CMPEditMenuViewRegister shared] addEditMenu:self];
    } else {
        [[CMPEditMenuViewRegister shared] removeEditMenu:self];
    }
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

- (void)schedulePresentEditMenuInteractionWithDelay:(NSTimeInterval)delay API_AVAILABLE(ios(16.0)) {
    __weak __auto_type weak_self = self;
    self.presentInteractionBlock = dispatch_block_create(0 ,^{
        __auto_type self = weak_self;
        if (self.editInteraction == nil) {
            self.editInteraction = [[UIEditMenuInteraction alloc] initWithDelegate:self];
            [self addInteraction:self.editInteraction];
        }
        [self presentEditMenuInteraction];
    });
    dispatch_after(dispatch_time(DISPATCH_TIME_NOW, (int64_t)(delay * NSEC_PER_SEC)),
                   dispatch_get_main_queue(),
                   self.presentInteractionBlock);
}

- (void)cancelPresentEditMenuInteraction API_AVAILABLE(ios(16.0)) {
    if (self.presentInteractionBlock != nil) {
        dispatch_block_cancel(self.presentInteractionBlock);
        self.presentInteractionBlock = nil;
    }
}

- (BOOL)canBecomeFirstResponder {
    return YES;
}

- (void)hideEditMenu {
    if (@available(iOS 16, *)) {
        [self cancelPresentEditMenuInteraction];

        if (self.editInteraction != nil) {
            [self.editInteraction dismissMenu];
            [self removeInteraction:self.editInteraction];
            self.editInteraction = nil;
        }
    } else if (@available(iOS 13, *)) {
        self.isEditMenuShown = NO;
        [self cancelShowMenuController];
        [[UIMenuController sharedMenuController] hideMenu];
    } else {
        self.isEditMenuShown = NO;
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

- (void)editMenuInteraction:(UIEditMenuInteraction *)interaction
willDismissMenuForConfiguration:(UIEditMenuConfiguration *)configuration
                   animator:(id<UIEditMenuInteractionAnimating>)animator API_AVAILABLE(ios(16.0)) {
    __weak __auto_type weak_self = self;
    [animator addCompletion:^{
        __auto_type self = weak_self;
        if (self.editInteraction == interaction) {
            self.isEditMenuShown = NO;
        }
    }];
}

- (void)editMenuInteraction:(UIEditMenuInteraction *)interaction
willPresentMenuForConfiguration:(UIEditMenuConfiguration *)configuration
                   animator:(id<UIEditMenuInteractionAnimating>)animator API_AVAILABLE(ios(16.0)) {
    __weak __auto_type weak_self = self;
    [animator addCompletion:^{
        __auto_type self = weak_self;
        if (self.editInteraction == interaction) {
            self.isEditMenuShown = YES;
        }
    }];
}

@end
