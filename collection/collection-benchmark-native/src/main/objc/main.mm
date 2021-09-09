/*
 * Copyright 2021 The Android Open Source Project
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

#import <CollectionKMP/CollectionKMP.h>
#import <Foundation/Foundation.h>
#import <benchmark/benchmark.h>

static NSArray *GenerateOrderedStrings(int size) {
  NSMutableArray *array = [NSMutableArray array];
  for (int i = 0; i < size; i++) {
    [array addObject:[NSString stringWithFormat:@"value %d", i]];
  }
  return array;
}

static NSDictionary *GenerateSourceMap(int size) {
  NSMutableDictionary *dict = [NSMutableDictionary dictionary];
  for (int i = 0; i < size; i++) {
    [dict setObject:[NSString stringWithFormat:@"value %d", i]
             forKey:[NSString stringWithFormat:@"key %d", i]];
  }
  return dict;
}

const int CircularArray_sourceSetSize = 10000;
const int SimpleArrayMap_sourceMapSize = 10000;

static void CircularArray_addFromHeadAndPopFromTail_ObjCCallingKMP(NSArray *source) {
  @autoreleasepool {
    CKMPCircularArray *array = [[CKMPCircularArray alloc] initWithMinCapacity:8];
    for (id element in source) {
      [array addFirstE:element];
    }
    NSCAssert([array size] == [source count], @"");
    for (NSInteger i = 0, s = [source count]; i < s; i++) {
      [array popLast];
    }
    NSCAssert([array isEmpty], @"");
  }
}

static void BM_CircularArray_addFromHeadAndPopFromTail_ObjCCallingKMP(benchmark::State &state) {
  NSArray *source = GenerateOrderedStrings(CircularArray_sourceSetSize);
  for (auto _ : state) {
    CircularArray_addFromHeadAndPopFromTail_ObjCCallingKMP(source);
  }
}
BENCHMARK(BM_CircularArray_addFromHeadAndPopFromTail_ObjCCallingKMP);

static void SimpleArrayMap_addAllThenRemoveIndividually_ObjCCallingKMP(NSDictionary *source) {
  @autoreleasepool {
    CKMPSimpleArrayMap<NSString *, NSString *> *map =
        [[CKMPSimpleArrayMap alloc] initWithCapacity:(int)[source count]];
    for (id key in source) {
      [map putKey:key value:[source objectForKey:key]];
    }
    NSCAssert([map size] == [source count], @"");
    for (id key in source) {
      [map removeKey:key];
    }
    NSCAssert([map isEmpty], @"");
  }
}

static void BM_SimpleArrayMap_addAllThenRemoveIndividually_ObjCCallingKMP(benchmark::State &state) {
  NSDictionary *source = GenerateSourceMap(SimpleArrayMap_sourceMapSize);
  for (auto _ : state) {
    SimpleArrayMap_addAllThenRemoveIndividually_ObjCCallingKMP(source);
  }
}
BENCHMARK(BM_SimpleArrayMap_addAllThenRemoveIndividually_ObjCCallingKMP);

int main(int argc, char **argv) {
  @autoreleasepool {
    ::benchmark::Initialize(&argc, argv);
    if (::benchmark::ReportUnrecognizedArguments(argc, argv)) return 1;
    ::benchmark::RunSpecifiedBenchmarks();
  }
}
