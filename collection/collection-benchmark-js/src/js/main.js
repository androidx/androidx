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

let Benchmark = require('benchmark');
let suite = new Benchmark.Suite();

let Collection = require('androidx-collection2')
    .androidx.collection;

let SourceValues = [];
for (let i = 0; i < 10000; i++) {
    SourceValues.push(`value ${i}`);
}

console.log('benchmark suite started');

suite.add('circularArray_addFromHeadAndPopFromTail', function() {
    let circularArray = new Collection.CircularArray();

    console.assert(circularArray.isEmpty());
    for (let value in SourceValues) {
        circularArray.addFirst(value);
    }
    console.assert(!circularArray.isEmpty());
    console.assert(circularArray.size == SourceValues.length);

    var numPopped = 0;
    let numElements = SourceValues.length;

    while (!circularArray.isEmpty()) {
        circularArray.popLast();
        numPopped++;
    }
    console.assert(numPopped === numElements);
})
.add('circularArray_addFromTailAndPopFromHead', function() {
    let circularArray = new Collection.CircularArray();

    console.assert(circularArray.isEmpty());
    for (let value in SourceValues) {
        circularArray.addLast(value);
    }
    console.assert(!circularArray.isEmpty());
    console.assert(circularArray.size == SourceValues.length);

    var numPopped = 0;
    let numElements = SourceValues.length;

    while (!circularArray.isEmpty()) {
        circularArray.popFirst();
        numPopped++;
    }
    console.assert(numPopped === numElements);
})
.add('lruCache_allHits', function() {
    let cache = Collection.LruCache$int(SourceValues.length);
    for (let key in SourceValues) {
        cache.put(key, key);
    }
    for (let key in SourceValues) {
        let value = cache.get(key);
        console.assert(value === key);
    }
    console.assert(cache.hitCount() == SourceValues.length);
    console.assert(cache.missCount() == 0);
})
.add('lruCache_allMisses', function() {
    let cache = Collection.LruCache$int(SourceValues.length);
    for (let key in SourceValues) {
        let value = cache.get(key);
        console.assert(value === null);
    }
    console.assert(cache.hitCount() == 0);
    console.assert(cache.missCount() == SourceValues.length);
})
.add('lruCache_customCreate', function() {
    let cache = Collection.LruCache$int(2);
    cache.create = function(key) { return `value_${key}`; };

    cache.put('foo', '1');
    cache.put('bar', '1');
    let value = cache.get('baz');
    console.assert(value == 'value_baz');
    console.assert(cache.size == 2);
    console.assert(cache.hitCount() == 0);
    console.assert(cache.missCount() == 1);

    let value2 = cache.get('baz');
    console.assert(value2 == 'value_baz');
    console.assert(cache.hitCount() == 1);
    console.assert(cache.missCount() == 1);
})
.on('cycle', function(event) {
  console.log(String(event.target));
})
.on('complete', function() {
  console.log('benchmark suite completed');
})
.run({ 'async': false });
