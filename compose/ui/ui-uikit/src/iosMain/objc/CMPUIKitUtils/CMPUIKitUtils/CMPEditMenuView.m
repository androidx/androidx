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
@property (copy, nonatomic, nullable) NSArray<CMPEditMenuCustomAction *> *customActions;

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
    [self showEditMenuAtRect:targetRect
                        copy:copyBlock
                         cut:cutBlock
                       paste:pasteBlock
                   selectAll:selectAllBlock
               customActions:@[]];
}

- (void)showEditMenuAtRect:(CGRect)targetRect
                      copy:(void (^)(void))copyBlock
                       cut:(void (^)(void))cutBlock
                     paste:(void (^)(void))pasteBlock
                 selectAll:(void (^)(void))selectAllBlock
             customActions:(NSArray<CMPEditMenuCustomAction *> *)customActions {
    BOOL contextMenuItemsChanged = [self contextMenuItemsChangedCopy:copyBlock
                                                                 cut:cutBlock
                                                               paste:pasteBlock
                                                           selectAll:selectAllBlock
                                                       customActions:customActions];
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
    self.customActions = customActions;

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
        [self hideEditMenu];
        [[CMPEditMenuViewRegister shared] removeEditMenu:self];
    }
}

- (void)scheduleShowMenuController {
    [self cancelShowMenuController];

    __weak __auto_type weak_self = self;
    self.showContextMenuBlock = dispatch_block_create(0 ,^{
        __auto_type self = weak_self;
        UIMenuController *controller = [UIMenuController sharedMenuController];
        controller.menuItems = [self makeCustomMenuItems];
        [self becomeFirstResponder];
        [controller showMenuFromView:self rect:self.targetRect];

        self.showContextMenuBlock = nil;
    });
    dispatch_after(dispatch_time(DISPATCH_TIME_NOW, (int64_t)([self editMenuDelay] * NSEC_PER_SEC)),
                   dispatch_get_main_queue(),
                   self.showContextMenuBlock);
}

- (NSArray<UIMenuItem *> *)makeCustomMenuItems {
    if (self.customActions.count == 0) {
        return @[];
    }
    
    SEL selectorsArray[] = {
        @selector(customAction0:),
        @selector(customAction1:),
        @selector(customAction2:),
        @selector(customAction3:),
        @selector(customAction4:),
        @selector(customAction5:),
        @selector(customAction6:),
        @selector(customAction7:),
        @selector(customAction8:),
        @selector(customAction9:)
    };
    SEL *selectorsPtr = selectorsArray;
    
    NSMutableArray<UIMenuItem *> *items = [NSMutableArray new];
    [self.customActions enumerateObjectsUsingBlock:^(CMPEditMenuCustomAction *item, NSUInteger index, BOOL * _Nonnull stop) {
        if (index >= customActionsMaxCount) {
            *stop = YES;
            return;
        }
        [items addObject:[[UIMenuItem alloc] initWithTitle:self.customActions[index].title action:selectorsPtr[index]]];
    }];

    return items;
}

- (NSArray<UIMenuElement *> *)makeCustomMenuElements {
    if (self.customActions.count == 0) {
        return @[];
    }
    
    NSMutableArray<UIMenuElement *> *items = [NSMutableArray new];
    [self.customActions enumerateObjectsUsingBlock:^(CMPEditMenuCustomAction *item, NSUInteger index, BOOL * _Nonnull stop) {
        [items addObject:[UIAction actionWithTitle:self.customActions[index].title
                                             image:nil
                                        identifier:nil
                                           handler:^(__kindof UIAction * _Nonnull action) {
            item.actionBlock();
        }]];
    }];
    
    return items;
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
    } else {
        self.isEditMenuShown = NO;
        [self cancelShowMenuController];
        [[UIMenuController sharedMenuController] hideMenu];
    }
}

- (BOOL)contextMenuItemsChangedCopy:(void (^)(void))copyBlock
                                cut:(void (^)(void))cutBlock
                              paste:(void (^)(void))pasteBlock
                          selectAll:(void (^)(void))selectAllBlock
                      customActions:(NSArray<CMPEditMenuCustomAction *> *)customActions {
    return ((self.copyBlock == nil) != (copyBlock == nil) ||
            (self.cutBlock == nil) != (cutBlock == nil) ||
            (self.pasteBlock == nil) != (pasteBlock == nil) ||
            (self.selectAllBlock == nil) != (selectAllBlock == nil) ||
            (![self.customActions isEqualToArray:customActions]));
}

- (BOOL)canPerformAction:(SEL)action withSender:(id)sender {
    if (@selector(copy:) == action) {
        return self.copyBlock != nil;
    }
    if (@selector(paste:) == action) {
        return self.pasteBlock != nil;
    }
    if (@selector(cut:) == action) {
        return self.cutBlock != nil;
    }
    if (@selector(selectAll:) == action) {
        return self.selectAllBlock != nil;
    }

    if (@selector(customAction0:) == action) return self.customActions.count > 0;
    if (@selector(customAction1:) == action) return self.customActions.count > 1;
    if (@selector(customAction2:) == action) return self.customActions.count > 2;
    if (@selector(customAction3:) == action) return self.customActions.count > 3;
    if (@selector(customAction4:) == action) return self.customActions.count > 4;
    if (@selector(customAction5:) == action) return self.customActions.count > 5;
    if (@selector(customAction6:) == action) return self.customActions.count > 6;
    if (@selector(customAction7:) == action) return self.customActions.count > 7;
    if (@selector(customAction8:) == action) return self.customActions.count > 8;
    if (@selector(customAction9:) == action) return self.customActions.count > 9;

    return NO;
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

const NSInteger customActionsMaxCount = 10;

- (void)customAction0:(id)sender {
    [self performCustomActionAtIndex:0];
}

- (void)customAction1:(id)sender {
    [self performCustomActionAtIndex:1];
}

- (void)customAction2:(id)sender {
    [self performCustomActionAtIndex:2];
}

- (void)customAction3:(id)sender {
    [self performCustomActionAtIndex:3];
}

- (void)customAction4:(id)sender {
    [self performCustomActionAtIndex:4];
}

- (void)customAction5:(id)sender {
    [self performCustomActionAtIndex:5];
}

- (void)customAction6:(id)sender {
    [self performCustomActionAtIndex:6];
}

- (void)customAction7:(id)sender {
    [self performCustomActionAtIndex:7];
}

- (void)customAction8:(id)sender {
    [self performCustomActionAtIndex:8];
}

- (void)customAction9:(id)sender {
    [self performCustomActionAtIndex:9];
}

- (void)performCustomActionAtIndex:(NSInteger)index {
    if (index >= self.customActions.count) {
        return;
    }
    self.customActions[index].actionBlock();
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

- (UIMenu *)editMenuInteraction:(UIEditMenuInteraction *)interaction
            menuForConfiguration:(UIEditMenuConfiguration *)configuration
               suggestedActions:(NSArray<UIMenuElement *> *)suggestedActions API_AVAILABLE(ios(16.0)){
    
    NSArray *allActions = [suggestedActions arrayByAddingObjectsFromArray:[self makeCustomMenuElements]];
    
    return [UIMenu menuWithTitle:@"" children:allActions];
}

@end
