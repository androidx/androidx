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

#import "CMPDragInteractionProxy.h"

NS_ASSUME_NONNULL_BEGIN

@implementation CMPDragInteractionProxy

- (NSArray<UIDragItem *> *)dragInteraction:(UIDragInteraction *)interaction itemsForBeginningSession:(id<UIDragSession>)session {
    return [self itemsForBeginningSession:session interaction:interaction];
}

- (NSArray<UIDragItem *> *)itemsForBeginningSession:(id<UIDragSession>)session interaction:(UIDragInteraction *)interaction {
    CMP_ABSTRACT_FUNCTION_CALLED
}

- (BOOL)dragInteraction:(UIDragInteraction *)interaction sessionAllowsMoveOperation:(id<UIDragSession>)session {
    return [self doesSessionAllowMoveOperation:session interaction:interaction];
}

- (BOOL)doesSessionAllowMoveOperation:(id<UIDragSession>)session interaction:(UIDragInteraction *)interaction {
    CMP_ABSTRACT_FUNCTION_CALLED
}

- (BOOL)dragInteraction:(UIDragInteraction *)interaction sessionIsRestrictedToDraggingApplication:(id<UIDragSession>)session {
    return [self isSessionRestrictedToDraggingApplication:session interaction:interaction];
}

- (BOOL)isSessionRestrictedToDraggingApplication:(id<UIDragSession>)session interaction:(UIDragInteraction *)interaction {
    CMP_ABSTRACT_FUNCTION_CALLED
}

- (UITargetedDragPreview *_Nullable)dragInteraction:(UIDragInteraction *)interaction previewForLiftingItem:(UIDragItem *)item session:(id<UIDragSession>)session {
    return [self previewForLiftingItemInSession:session item:item interaction:interaction];
}

- (UITargetedDragPreview *_Nullable)previewForLiftingItemInSession:(id<UIDragSession>)session item:(UIDragItem *)item interaction:(UIDragInteraction *)interaction {
    CMP_ABSTRACT_FUNCTION_CALLED
}

- (void)dragInteraction:(UIDragInteraction *)interaction session:(id<UIDragSession>)session didEndWithOperation:(UIDropOperation)operation {
    [self sessionDidEndWithOperation:session interaction:interaction operation:operation];
}

- (void)sessionDidEndWithOperation:(id<UIDragSession>)session interaction:(UIDragInteraction *)interaction operation:(UIDropOperation)operation {
    CMP_ABSTRACT_FUNCTION_CALLED
}


@end

@implementation UIDragItem (CMPEncoding)

+ (instancetype)cmp_itemWithString:(NSString *)string {
    UIDragItem *item = [[UIDragItem alloc] initWithItemProvider:[[NSItemProvider alloc] initWithObject:string]];
    
    item.localObject = string;
    
    return item;
}

+ (instancetype _Nullable)cmp_itemWithAny:(Class)objectClass object:(id)object {
    assert([object isKindOfClass:objectClass] && "`object` is not kind of `objectClass`");
    
    if ([objectClass conformsToProtocol:@protocol(NSItemProviderWriting)]) {
        UIDragItem *item = [[UIDragItem alloc] initWithItemProvider:[[NSItemProvider alloc] initWithObject:object]];
        
        item.localObject = object;
        
        return item;
    } else {
        return nil;
    }
}

@end

NS_ASSUME_NONNULL_END
