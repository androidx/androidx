/*
 * Copyright 2019 The Android Open Source Project
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


import { IS_LOG, IS_PROFILE } from './flags';

export function log(message: string, ...args: any[]) {
  if (IS_LOG) {
    let length = args ? args.length : 0;
    if (length > 0) {
      console.log(message, ...args);
    } else {
      console.log(message);
    }
  }
};

export function logError(message: string, ...args: any[]) {
  let length = args ? args.length : 0;
  if (length > 0) {
    console.error(message, ...args);
  } else {
    console.error(message);
  }
};

// check to see if native support for profiling is available.
const NATIVE_PROFILE_SUPPORT =
  typeof window !== 'undefined' && !!window.performance && !!console.profile;

/**
 * A decorator that can profile a function.
 */
export function profile(
  target: any, propertyKey: string, descriptor: PropertyDescriptor) {
  if (IS_PROFILE) {
    return performProfile(target, propertyKey, descriptor);
  } else {
    // return as-is
    return descriptor;
  }
}

function performProfile(
  target: any, propertyKey: string,
  descriptor: PropertyDescriptor): PropertyDescriptor {
  let originalCallable = descriptor.value;
  // name must exist
  let name = originalCallable.name;
  if (!name) {
    name = 'anonymous function';
  }
  if (NATIVE_PROFILE_SUPPORT) {
    descriptor.value = function (...args: any[]) {
      console.profile(name);
      let startTime = window.performance.now();
      let result = originalCallable.call(this || window, ...args);
      let duration = window.performance.now() - startTime;
      console.log(`${name} took ${duration} ms`);
      console.profileEnd();
      return result;
    };
  } else {
    descriptor.value = function (...args: any[]) {
      log(`Profile start ${name}`);
      let start = Date.now();
      let result = originalCallable.call(this || window, ...args);
      let duration = Date.now() - start;
      log(`Profile end ${name} took ${duration} ms.`);
      return result;
    };
  }
  return descriptor;
}
