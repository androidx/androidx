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

#import "CMPDropInteractionProxy.h"

NS_ASSUME_NONNULL_BEGIN

@implementation CMPDropInteractionProxy

- (BOOL)dropInteraction:(UIDropInteraction *)interaction canHandleSession:(id<UIDropSession>)session {
    return [self canHandleSession:session interaction:interaction];
}

- (BOOL)canHandleSession:(id<UIDropSession>)session interaction:(UIDropInteraction *)interaction {
    CMP_ABSTRACT_FUNCTION_CALLED
}

- (void)dropInteraction:(UIDropInteraction *)interaction performDrop:(id<UIDropSession>)session {
    [self performDropFromSession:session interaction:interaction];
}

- (void)performDropFromSession:(id<UIDropSession>)session interaction:(UIDropInteraction *)interaction {
    CMP_ABSTRACT_FUNCTION_CALLED
}

- (UIDropProposal *)dropInteraction:(UIDropInteraction *)interaction sessionDidUpdate:(id<UIDropSession>)session {
    return [self proposalForSessionUpdate:session interaction:interaction];
}

- (UIDropProposal *)proposalForSessionUpdate:(id<UIDropSession>)session interaction:(UIDropInteraction *)interaction {
    CMP_ABSTRACT_FUNCTION_CALLED
}

- (void)dropInteraction:(UIDropInteraction *)interaction sessionDidEnd:(id<UIDropSession>)session {
    return [self sessionDidEnd:session interaction:interaction];
}

- (void)sessionDidEnd:(id<UIDropSession>)session interaction:(UIDropInteraction *)interaction {
    CMP_ABSTRACT_FUNCTION_CALLED
}

- (void)dropInteraction:(UIDropInteraction *)interaction sessionDidEnter:(id<UIDropSession>)session {
    [self sessionDidEnter:session interaction:interaction];
}

- (void)sessionDidEnter:(id<UIDropSession>)session interaction:(UIDropInteraction *)interaction {
    CMP_ABSTRACT_FUNCTION_CALLED
}

- (void)dropInteraction:(UIDropInteraction *)interaction sessionDidExit:(id<UIDropSession>)session {
    [self sessionDidExit:session interaction:interaction];
}

- (void)sessionDidExit:(id<UIDropSession>)session interaction:(UIDropInteraction *)interaction {
    CMP_ABSTRACT_FUNCTION_CALLED
}

@end

@implementation NSItemProvider (CMPDecoding)

- (void)cmp_loadString:(void (^)(NSString  * _Nullable result, NSError *_Nullable error))completionHandler {
    if ([self canLoadObjectOfClass:NSString.class]) {
        [self loadObjectOfClass:NSString.class completionHandler:completionHandler];
    } else {
        completionHandler(nil, nil);
    }
}

- (void)cmp_loadAny:(Class)objectClass onCompletion:(void (^)(id _Nullable result, NSError *_Nullable error))completionHandler {
    // Check that an object of objectClass can be loaded from NSItemProvider
    if (![objectClass conformsToProtocol:@protocol(NSItemProviderReading)]) {
        NSDictionary *userInfo = @{
            @"description" : [NSString stringWithFormat:@"%@ doesn't conform to protocol NSItemProviderReading and thus can't be loaded", objectClass.description]
        };
        
        NSError *error = [NSError errorWithDomain:NSCocoaErrorDomain
                                             code:0
                                         userInfo:userInfo];
        completionHandler(nil, error);
    } else if ([self canLoadObjectOfClass:objectClass]) {
        // Try loading the object of this class
        [self loadObjectOfClass:objectClass completionHandler:completionHandler];
    } else {
        // This UIDragItem does't contain object of `objectClass`
        completionHandler(nil, nil);
    }
}

@end

NS_ASSUME_NONNULL_END
