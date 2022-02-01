/*
 * Copyright (C) 2017 The Android Open Source Project
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

#include <sstream>
#include <unordered_map>
#include <vector>

#include <jni.h>
#include <nativehelper/ScopedLocalRef.h>
#include <gtest/gtest.h>

static JavaVM* gVm = nullptr;
JavaVM* GetJavaVM() {
  return gVm;
}
static void RegisterJavaVm(JNIEnv* env) {
  (void)env->GetJavaVM(&gVm);
}

namespace {

    struct {
        jclass clazz;

        /** static methods **/
        jmethodID createTestDescription;

        /** methods **/
        jmethodID addChild;
    } gDescription;

    struct {
        jclass clazz;

        jmethodID fireTestStarted;
        jmethodID fireTestIgnored;
        jmethodID fireTestFailure;
        jmethodID fireTestFinished;

    } gRunNotifier;

    struct {
        jclass clazz;
        jmethodID ctor;
    } gAssertionFailure;

    struct {
        jclass clazz;
        jmethodID ctor;
    } gFailure;

    jobject gEmptyAnnotationsArray;

    struct TestNameInfo {
        std::string nativeName;
        bool run;
    };
// Maps mangled test names to native test names.
    std::unordered_map<std::string, TestNameInfo> gNativeTestNames;

// Return the full native test name as a Java method name, which does not allow
// slashes or dots. Store the original name for later lookup.
    std::string registerAndMangleTestName(const std::string& nativeName) {
      std::string mangledName = nativeName;
      std::replace(mangledName.begin(), mangledName.end(), '.', '_');
      std::replace(mangledName.begin(), mangledName.end(), '/', '_');
      gNativeTestNames.insert(std::make_pair(mangledName, TestNameInfo{nativeName, false}));
      return mangledName;
    }

// Creates org.junit.runner.Description object for a GTest given its name.
    jobject createTestDescription(JNIEnv* env, jstring className, const std::string& mangledName) {
      ScopedLocalRef<jstring> jTestName(env, env->NewStringUTF(mangledName.c_str()));
      return env->CallStaticObjectMethod(gDescription.clazz, gDescription.createTestDescription,
                                         className, jTestName.get(), gEmptyAnnotationsArray);
    }

    jobject createTestDescription(JNIEnv* env, jstring className, const char* testCaseName, const char* testName) {
      std::ostringstream nativeNameStream;
      nativeNameStream << testCaseName << "." << testName;
      std::string mangledName = registerAndMangleTestName(nativeNameStream.str());
      return createTestDescription(env, className, mangledName);
    }

    void addChild(JNIEnv* env, jobject description, jobject childDescription) {
      env->CallVoidMethod(description, gDescription.addChild, childDescription);
    }


    class JUnitNotifyingListener : public ::testing::EmptyTestEventListener {
    public:

        JUnitNotifyingListener(JNIEnv* env, jstring className, jobject runNotifier)
                : mEnv(env)
                , mRunNotifier(runNotifier)
                , mClassName(className)
                , mCurrentTestDescription{env, nullptr}
        {}
        virtual ~JUnitNotifyingListener() {}

        virtual void OnTestStart(const testing::TestInfo &testInfo) override {
          mCurrentTestDescription.reset(
                  createTestDescription(mEnv, mClassName, testInfo.test_case_name(), testInfo.name()));
          notify(gRunNotifier.fireTestStarted);
        }

        virtual void OnTestPartResult(const testing::TestPartResult &testPartResult) override {
          if (!testPartResult.passed()) {
            mCurrentTestError << "\n" << testPartResult.file_name() << ":" << testPartResult.line_number()
                              << "\n" << testPartResult.message() << "\n";
          }
        }

        virtual void OnTestEnd(const testing::TestInfo&) override {
          const std::string error = mCurrentTestError.str();

          if (!error.empty()) {
            ScopedLocalRef<jstring> jmessage(mEnv, mEnv->NewStringUTF(error.c_str()));
            ScopedLocalRef<jobject> jthrowable(mEnv, mEnv->NewObject(gAssertionFailure.clazz,
                                                                     gAssertionFailure.ctor, jmessage.get()));
            ScopedLocalRef<jobject> jfailure(mEnv, mEnv->NewObject(gFailure.clazz,
                                                                   gFailure.ctor, mCurrentTestDescription.get(), jthrowable.get()));
            mEnv->CallVoidMethod(mRunNotifier, gRunNotifier.fireTestFailure, jfailure.get());
          }

          notify(gRunNotifier.fireTestFinished);
          mCurrentTestDescription.reset();
          mCurrentTestError.str("");
          mCurrentTestError.clear();
        }

        void reportDisabledTests(const std::vector<std::string>& mangledNames) {
          for (const std::string& mangledName : mangledNames) {
            mCurrentTestDescription.reset(createTestDescription(mEnv, mClassName, mangledName));
            notify(gRunNotifier.fireTestIgnored);
            mCurrentTestDescription.reset();
          }
        }

    private:
        void notify(jmethodID method) {
          mEnv->CallVoidMethod(mRunNotifier, method, mCurrentTestDescription.get());
        }

        JNIEnv* mEnv;
        jobject mRunNotifier;
        jstring mClassName;
        ScopedLocalRef<jobject> mCurrentTestDescription;
        std::ostringstream mCurrentTestError;
    };

}  // namespace

extern "C"
JNIEXPORT void JNICALL
Java_androidx_test_ext_junitgtest_GtestRunner_initialize(JNIEnv *env, jclass, jstring className, jobject suite) {
  RegisterJavaVm(env);

  // Initialize gtest, removing the default result printer
  int argc = 1;
  const char* argv[] = { "gtest_wrapper" };
  ::testing::InitGoogleTest(&argc, (char**) argv);

  auto& listeners = ::testing::UnitTest::GetInstance()->listeners();
  delete listeners.Release(listeners.default_result_printer());

  gDescription.clazz = (jclass) env->NewGlobalRef(env->FindClass("org/junit/runner/Description"));
  gDescription.createTestDescription = env->GetStaticMethodID(gDescription.clazz, "createTestDescription",
                                                              "(Ljava/lang/String;Ljava/lang/String;[Ljava/lang/annotation/Annotation;)Lorg/junit/runner/Description;");
  gDescription.addChild = env->GetMethodID(gDescription.clazz, "addChild",
                                           "(Lorg/junit/runner/Description;)V");

  jclass annotations = env->FindClass("java/lang/annotation/Annotation");
  gEmptyAnnotationsArray = env->NewGlobalRef(env->NewObjectArray(0, annotations, nullptr));
  gNativeTestNames.clear();

  gAssertionFailure.clazz = (jclass) env->NewGlobalRef(env->FindClass("java/lang/AssertionError"));
  gAssertionFailure.ctor = env->GetMethodID(gAssertionFailure.clazz, "<init>", "(Ljava/lang/Object;)V");

  gFailure.clazz = (jclass) env->NewGlobalRef(env->FindClass("org/junit/runner/notification/Failure"));
  gFailure.ctor = env->GetMethodID(gFailure.clazz, "<init>",
                                   "(Lorg/junit/runner/Description;Ljava/lang/Throwable;)V");

  gRunNotifier.clazz = (jclass) env->NewGlobalRef(
          env->FindClass("org/junit/runner/notification/RunNotifier"));
  gRunNotifier.fireTestStarted = env->GetMethodID(gRunNotifier.clazz, "fireTestStarted",
                                                  "(Lorg/junit/runner/Description;)V");
  gRunNotifier.fireTestIgnored = env->GetMethodID(gRunNotifier.clazz, "fireTestIgnored",
                                                  "(Lorg/junit/runner/Description;)V");
  gRunNotifier.fireTestFinished = env->GetMethodID(gRunNotifier.clazz, "fireTestFinished",
                                                   "(Lorg/junit/runner/Description;)V");
  gRunNotifier.fireTestFailure = env->GetMethodID(gRunNotifier.clazz, "fireTestFailure",
                                                  "(Lorg/junit/runner/notification/Failure;)V");

  auto unitTest = ::testing::UnitTest::GetInstance();
  for (int testCaseIndex = 0; testCaseIndex < unitTest->total_test_case_count(); testCaseIndex++) {
    auto testCase = unitTest->GetTestCase(testCaseIndex);
    for (int testIndex = 0; testIndex < testCase->total_test_count(); testIndex++) {
      auto testInfo = testCase->GetTestInfo(testIndex);
      ScopedLocalRef<jobject> testDescription(env,
                                              createTestDescription(env, className, testCase->name(), testInfo->name()));
      addChild(env, suite, testDescription.get());
    }
  }
}

extern "C"
JNIEXPORT void JNICALL
Java_androidx_test_ext_junitgtest_GtestRunner_addTest(JNIEnv *env, jclass, jstring testName) {
  const char* testNameChars = env->GetStringUTFChars(testName, JNI_FALSE);
  auto found = gNativeTestNames.find(testNameChars);
  if (found != gNativeTestNames.end()) {
    found->second.run = true;
  }
  env->ReleaseStringUTFChars(testName, testNameChars);
}

extern "C"
JNIEXPORT jboolean JNICALL
Java_androidx_test_ext_junitgtest_GtestRunner_run(JNIEnv *env, jclass, jstring className, jobject notifier) {
  // Apply the test filter computed in Java-land. The filter is just a list of test names.
  std::ostringstream filterStream;
  std::vector<std::string> mangledNamesOfDisabledTests;
  for (const auto& entry : gNativeTestNames) {
    // If the test was not selected for running by the Java layer, ignore it completely.
    if (!entry.second.run) continue;
    // If the test has DISABLED_ at the beginning of its name, after a slash or after a dot,
    // report it as ignored (disabled) to the Java layer.
    if (entry.second.nativeName.find("DISABLED_") == 0 ||
        entry.second.nativeName.find("/DISABLED_") != std::string::npos ||
        entry.second.nativeName.find(".DISABLED_") != std::string::npos) {
      mangledNamesOfDisabledTests.push_back(entry.first);
      continue;
    }
    filterStream << entry.second.nativeName << ":";
  }
  std::string filter = filterStream.str();
  if (filter.empty()) {
    // If the string we built is empty, we don't want to run any tests, but GTest runs all tests
    // when an empty filter is passed. Replace an empty filter with a filter that matches nothing.
    filter = "-*";
  } else {
    // Removes the trailing colon.
    filter.pop_back();
  }
  ::testing::GTEST_FLAG(filter) = filter;

  auto& listeners = ::testing::UnitTest::GetInstance()->listeners();
  JUnitNotifyingListener junitListener{env, className, notifier};
  listeners.Append(&junitListener);
  int success = RUN_ALL_TESTS();
  listeners.Release(&junitListener);
  junitListener.reportDisabledTests(mangledNamesOfDisabledTests);
  return success == 0;
}