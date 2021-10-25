/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.fragment.lint

import com.android.tools.lint.checks.infrastructure.LintDetectorTest.java
import com.android.tools.lint.checks.infrastructure.LintDetectorTest.kotlin
import com.android.tools.lint.checks.infrastructure.TestLintTask
import com.android.tools.lint.checks.infrastructure.TestLintTask.lint
import com.android.tools.lint.checks.infrastructure.TestMode
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

/* ktlint-disable max-line-length */
@RunWith(JUnit4::class)
class UseRequireInsteadOfGetTest {

    private val fragmentStub = java(
        """
        package androidx.fragment.app;
    
        public class Fragment {
          public void getArguments() {

          }
          public void getContext() {

          }
          public void getActivity() {

          }
          public void getFragmentManager() {

          }
          public void getHost() {

          }
          public Fragment getParentFragment() {

          }
          public void getView() {

          }
          public void requireView() {

          }
        }
      """
    ).indented()

    private val preconditionsStub = java(
        """
        package util;

        public final class Preconditions {
          public static <T> T checkNotNull(T value) {

          }

          public static <T> T checkNotNull(T value, String message) {

          }
        }
      """
    ).indented()

    private fun useRequireLint(): TestLintTask {
        return lint()
            .detector(UseRequireInsteadOfGet())
            .issues(UseRequireInsteadOfGet.ISSUE)
    }

    @Test
    fun `simple java checks where the fragment is a variable`() {
        useRequireLint()
            .files(
                fragmentStub,
                preconditionsStub,
                java(
                    """
                  package foo;

                  import androidx.fragment.app.Fragment;
                  import static util.Preconditions.checkNotNull;

                  class Test {
                    void test() {
                      Fragment fragment = new Fragment();

                      checkNotNull(fragment.getArguments());
                      checkNotNull(fragment.getFragmentManager());
                      checkNotNull(fragment.getContext());
                      checkNotNull(fragment.getActivity());
                      checkNotNull(fragment.getHost());
                      checkNotNull(fragment.getParentFragment());
                      checkNotNull(fragment.getView());

                      // These are redundant. Java-only really
                      checkNotNull(fragment.requireArguments());
                      checkNotNull(fragment.requireFragmentManager());
                      checkNotNull(fragment.requireContext());
                      checkNotNull(fragment.requireActivity());
                      checkNotNull(fragment.requireHost());
                      checkNotNull(fragment.requireParentFragment());
                      checkNotNull(fragment.requireView());

                      // These don't have errors
                      fragment.requireArguments();
                      fragment.requireFragmentManager();
                      fragment.requireContext();
                      fragment.requireActivity();
                      fragment.requireHost();
                      fragment.requireParentFragment();
                      fragment.requireView();

                      // These are ignored because they have custom error messages
                      checkNotNull(fragment.getArguments(), "getArguments");
                      checkNotNull(fragment.getFragmentManager(), "getFragmentManager");
                      checkNotNull(fragment.getContext(), "getContext");
                      checkNotNull(fragment.getActivity(), "getActivity");
                      checkNotNull(fragment.getHost(), "getHost");
                      checkNotNull(fragment.getParentFragment(), "getParentFragment");
                      checkNotNull(fragment.getView(), "getView");
                    }
                  }
                """
                ).indented()
            )
            .allowCompilationErrors(false)
            .skipTestModes(TestMode.WHITESPACE) // b/203246909
            .run()
            .expect(
                """
              src/foo/Test.java:10: Error: Use fragment.requireArguments() instead of checkNotNull(fragment.getArguments()) [UseRequireInsteadOfGet]
                  checkNotNull(fragment.getArguments());
                  ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
              src/foo/Test.java:11: Error: Use fragment.requireFragmentManager() instead of checkNotNull(fragment.getFragmentManager()) [UseRequireInsteadOfGet]
                  checkNotNull(fragment.getFragmentManager());
                  ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
              src/foo/Test.java:12: Error: Use fragment.requireContext() instead of checkNotNull(fragment.getContext()) [UseRequireInsteadOfGet]
                  checkNotNull(fragment.getContext());
                  ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
              src/foo/Test.java:13: Error: Use fragment.requireActivity() instead of checkNotNull(fragment.getActivity()) [UseRequireInsteadOfGet]
                  checkNotNull(fragment.getActivity());
                  ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
              src/foo/Test.java:14: Error: Use fragment.requireHost() instead of checkNotNull(fragment.getHost()) [UseRequireInsteadOfGet]
                  checkNotNull(fragment.getHost());
                  ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
              src/foo/Test.java:15: Error: Use fragment.requireParentFragment() instead of checkNotNull(fragment.getParentFragment()) [UseRequireInsteadOfGet]
                  checkNotNull(fragment.getParentFragment());
                  ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
              src/foo/Test.java:16: Error: Use fragment.requireView() instead of checkNotNull(fragment.getView()) [UseRequireInsteadOfGet]
                  checkNotNull(fragment.getView());
                  ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
              7 errors, 0 warnings
                """.trimIndent()
            )
            .expectFixDiffs(
                """
              Fix for src/foo/Test.java line 10: Replace with fragment.requireArguments():
              @@ -10 +10
              -     checkNotNull(fragment.getArguments());
              +     fragment.requireArguments();
              Fix for src/foo/Test.java line 11: Replace with fragment.requireFragmentManager():
              @@ -11 +11
              -     checkNotNull(fragment.getFragmentManager());
              +     fragment.requireFragmentManager();
              Fix for src/foo/Test.java line 12: Replace with fragment.requireContext():
              @@ -12 +12
              -     checkNotNull(fragment.getContext());
              +     fragment.requireContext();
              Fix for src/foo/Test.java line 13: Replace with fragment.requireActivity():
              @@ -13 +13
              -     checkNotNull(fragment.getActivity());
              +     fragment.requireActivity();
              Fix for src/foo/Test.java line 14: Replace with fragment.requireHost():
              @@ -14 +14
              -     checkNotNull(fragment.getHost());
              +     fragment.requireHost();
              Fix for src/foo/Test.java line 15: Replace with fragment.requireParentFragment():
              @@ -15 +15
              -     checkNotNull(fragment.getParentFragment());
              +     fragment.requireParentFragment();
              Fix for src/foo/Test.java line 16: Replace with fragment.requireView():
              @@ -16 +16
              -     checkNotNull(fragment.getView());
              +     fragment.requireView();
                """.trimIndent()
            )
    }

    @Test
    fun `simple java checks where the code is in a fragment`() {
        useRequireLint()
            .files(
                fragmentStub,
                preconditionsStub,
                java(
                    """
                  package foo;

                  import androidx.fragment.app.Fragment;
                  import static util.Preconditions.checkNotNull;

                  class TestFragment extends Fragment {
                    void test() {
                      checkNotNull(getArguments());
                      checkNotNull(getFragmentManager());
                      checkNotNull(getContext());
                      checkNotNull(getActivity());
                      checkNotNull(getHost());
                      checkNotNull(getParentFragment());
                      checkNotNull(getView());

                      // These are redundant. Java-only really
                      checkNotNull(requireArguments());
                      checkNotNull(requireFragmentManager());
                      checkNotNull(requireContext());
                      checkNotNull(requireActivity());
                      checkNotNull(requireHost());
                      checkNotNull(requireParentFragment());
                      checkNotNull(requireView());

                      // These don't have errors
                      requireArguments();
                      requireFragmentManager();
                      requireContext();
                      requireActivity();
                      requireHost();
                      requireParentFragment();
                      requireView();

                      // These are ignored because they have custom error messages
                      checkNotNull(fragment.getArguments(), "getArguments");
                      checkNotNull(fragment.getFragmentManager(), "getFragmentManager");
                      checkNotNull(fragment.getContext(), "getContext");
                      checkNotNull(fragment.getActivity(), "getActivity");
                      checkNotNull(fragment.getHost(), "getHost");
                      checkNotNull(fragment.getParentFragment(), "getParentFragment");
                      checkNotNull(fragment.getView(), "getView");
                    }
                  }
                """
                ).indented()
            )
            .allowCompilationErrors(false)
            .skipTestModes(TestMode.WHITESPACE) // b/203246909
            .run()
            .expect(
                """
              src/foo/TestFragment.java:8: Error: Use requireArguments() instead of checkNotNull(getArguments()) [UseRequireInsteadOfGet]
                  checkNotNull(getArguments());
                  ~~~~~~~~~~~~~~~~~~~~~~~~~~~~
              src/foo/TestFragment.java:9: Error: Use requireFragmentManager() instead of checkNotNull(getFragmentManager()) [UseRequireInsteadOfGet]
                  checkNotNull(getFragmentManager());
                  ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
              src/foo/TestFragment.java:10: Error: Use requireContext() instead of checkNotNull(getContext()) [UseRequireInsteadOfGet]
                  checkNotNull(getContext());
                  ~~~~~~~~~~~~~~~~~~~~~~~~~~
              src/foo/TestFragment.java:11: Error: Use requireActivity() instead of checkNotNull(getActivity()) [UseRequireInsteadOfGet]
                  checkNotNull(getActivity());
                  ~~~~~~~~~~~~~~~~~~~~~~~~~~~
              src/foo/TestFragment.java:12: Error: Use requireHost() instead of checkNotNull(getHost()) [UseRequireInsteadOfGet]
                  checkNotNull(getHost());
                  ~~~~~~~~~~~~~~~~~~~~~~~
              src/foo/TestFragment.java:13: Error: Use requireParentFragment() instead of checkNotNull(getParentFragment()) [UseRequireInsteadOfGet]
                  checkNotNull(getParentFragment());
                  ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
              src/foo/TestFragment.java:14: Error: Use requireView() instead of checkNotNull(getView()) [UseRequireInsteadOfGet]
                  checkNotNull(getView());
                  ~~~~~~~~~~~~~~~~~~~~~~~
              7 errors, 0 warnings
                """.trimIndent()
            )
            .expectFixDiffs(
                """
              Fix for src/foo/TestFragment.java line 8: Replace with requireArguments():
              @@ -8 +8
              -     checkNotNull(getArguments());
              +     requireArguments();
              Fix for src/foo/TestFragment.java line 9: Replace with requireFragmentManager():
              @@ -9 +9
              -     checkNotNull(getFragmentManager());
              +     requireFragmentManager();
              Fix for src/foo/TestFragment.java line 10: Replace with requireContext():
              @@ -10 +10
              -     checkNotNull(getContext());
              +     requireContext();
              Fix for src/foo/TestFragment.java line 11: Replace with requireActivity():
              @@ -11 +11
              -     checkNotNull(getActivity());
              +     requireActivity();
              Fix for src/foo/TestFragment.java line 12: Replace with requireHost():
              @@ -12 +12
              -     checkNotNull(getHost());
              +     requireHost();
              Fix for src/foo/TestFragment.java line 13: Replace with requireParentFragment():
              @@ -13 +13
              -     checkNotNull(getParentFragment());
              +     requireParentFragment();
              Fix for src/foo/TestFragment.java line 14: Replace with requireView():
              @@ -14 +14
              -     checkNotNull(getView());
              +     requireView();
                """.trimIndent()
            )
    }

    @Test
    fun `qualified checkNotNulls should remove the qualifier`() {
        useRequireLint()
            .files(
                fragmentStub,
                preconditionsStub,
                java(
                    """
                  package foo;

                  import androidx.fragment.app.Fragment;
                  import util.Preconditions;

                  class TestFragment extends Fragment {
                    void test() {
                      Preconditions.checkNotNull(getArguments());
                    }
                  }
                """
                ).indented()
            )
            .allowCompilationErrors(false)
            .skipTestModes(TestMode.WHITESPACE) // b/203246909
            .run()
            .expect(
                """
              src/foo/TestFragment.java:8: Error: Use requireArguments() instead of Preconditions.checkNotNull(getArguments()) [UseRequireInsteadOfGet]
                  Preconditions.checkNotNull(getArguments());
                  ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
              1 errors, 0 warnings
                """.trimIndent()
            )
            .expectFixDiffs(
                """
              Fix for src/foo/TestFragment.java line 8: Replace with requireArguments():
              @@ -8 +8
              -     Preconditions.checkNotNull(getArguments());
              +     requireArguments();
                """.trimIndent()
            )
    }

    @Test
    fun `simple kotlin checks where the fragment is a variable`() {
        // Note we don't import a preconditions stub here because we use kotlin's built-in
        useRequireLint()
            .files(
                fragmentStub,
                kotlin(
                    """
              package foo

              import androidx.fragment.app.Fragment

              class Test {
                fun test() {
                  val fragment = Fragment()

                  checkNotNull(fragment.getArguments())
                  checkNotNull(fragment.getFragmentManager())
                  checkNotNull(fragment.getContext())
                  checkNotNull(fragment.getActivity())
                  checkNotNull(fragment.getHost())
                  checkNotNull(fragment.getParentFragment())
                  checkNotNull(fragment.getView())

                  checkNotNull(fragment.arguments)
                  checkNotNull(fragment.fragmentManager)
                  checkNotNull(fragment.context)
                  checkNotNull(fragment.activity)
                  checkNotNull(fragment.host)
                  checkNotNull(fragment.parentFragment)
                  checkNotNull(fragment.view)

                  // !! nullchecks
                  fragment.getArguments()!!
                  fragment.getFragmentManager()!!
                  fragment.getContext()!!
                  fragment.getActivity()!!
                  fragment.getHost()!!
                  fragment.getParentFragment()!!
                  fragment.getView()!!
                  fragment.arguments!!
                  fragment.fragmentManager!!
                  fragment.context!!
                  fragment.activity!!
                  fragment.host!!
                  fragment.parentFragment!!
                  fragment.view!!

                  // These don't have errors
                  fragment.requireArguments()
                  fragment.requireFragmentManager()
                  fragment.requireContext()
                  fragment.requireActivity()
                  fragment.requireHost()
                  fragment.requireParentFragment()
                  fragment.requireView()

                  // These are ignored because they have custom error messages
                  checkNotNull(fragment.getArguments()) { "getArguments" }
                  checkNotNull(fragment.getFragmentManager()) { "getFragmentManager" }
                  checkNotNull(fragment.getContext()) { "getContext" }
                  checkNotNull(fragment.getActivity()) { "getActivity" }
                  checkNotNull(fragment.getHost()) { "getHost" }
                  checkNotNull(fragment.getParentFragment()) { "getParentFragment" }
                  checkNotNull(fragment.getView()) { "getView" }
                  requireNonNull(fragment.getArguments()) { "getArguments" }
                  requireNonNull(fragment.getFragmentManager()) { "getFragmentManager" }
                  requireNonNull(fragment.getContext()) { "getContext" }
                  requireNonNull(fragment.getActivity()) { "getActivity" }
                  requireNonNull(fragment.getHost()) { "getHost" }
                  requireNonNull(fragment.getParentFragment()) { "getParentFragment" }
                  requireNonNull(fragment.getView()) { "getView" }
                  checkNotNull(fragment.arguments) { "getArguments" }
                  checkNotNull(fragment.fragmentManager) { "getFragmentManager" }
                  checkNotNull(fragment.context) { "getContext" }
                  checkNotNull(fragment.activity) { "getActivity" }
                  checkNotNull(fragment.host) { "getHost" }
                  checkNotNull(fragment.parentFragment) { "getParentFragment" }
                  checkNotNull(fragment.view) { "getView" }
                  requireNonNull(fragment.arguments) { "getArguments" }
                  requireNonNull(fragment.fragmentManager) { "getFragmentManager" }
                  requireNonNull(fragment.context) { "getContext" }
                  requireNonNull(fragment.activity) { "getActivity" }
                  requireNonNull(fragment.host) { "getHost" }
                  requireNonNull(fragment.parentFragment) { "getParentFragment" }
                  requireNonNull(fragment.view) { "getView" }
                }
              }
            """
                ).indented()
            )
            .allowCompilationErrors(false)
            .skipTestModes(TestMode.WHITESPACE) // b/203246909
            .run()
            .expect(
                """
          src/foo/Test.kt:9: Error: Use fragment.requireArguments() instead of checkNotNull(fragment.getArguments()) [UseRequireInsteadOfGet]
              checkNotNull(fragment.getArguments())
              ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
          src/foo/Test.kt:10: Error: Use fragment.requireFragmentManager() instead of checkNotNull(fragment.getFragmentManager()) [UseRequireInsteadOfGet]
              checkNotNull(fragment.getFragmentManager())
              ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
          src/foo/Test.kt:11: Error: Use fragment.requireContext() instead of checkNotNull(fragment.getContext()) [UseRequireInsteadOfGet]
              checkNotNull(fragment.getContext())
              ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
          src/foo/Test.kt:12: Error: Use fragment.requireActivity() instead of checkNotNull(fragment.getActivity()) [UseRequireInsteadOfGet]
              checkNotNull(fragment.getActivity())
              ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
          src/foo/Test.kt:13: Error: Use fragment.requireHost() instead of checkNotNull(fragment.getHost()) [UseRequireInsteadOfGet]
              checkNotNull(fragment.getHost())
              ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
          src/foo/Test.kt:14: Error: Use fragment.requireParentFragment() instead of checkNotNull(fragment.getParentFragment()) [UseRequireInsteadOfGet]
              checkNotNull(fragment.getParentFragment())
              ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
          src/foo/Test.kt:15: Error: Use fragment.requireView() instead of checkNotNull(fragment.getView()) [UseRequireInsteadOfGet]
              checkNotNull(fragment.getView())
              ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
          src/foo/Test.kt:17: Error: Use fragment.requireArguments() instead of checkNotNull(fragment.arguments) [UseRequireInsteadOfGet]
              checkNotNull(fragment.arguments)
              ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
          src/foo/Test.kt:18: Error: Use fragment.requireFragmentManager() instead of checkNotNull(fragment.fragmentManager) [UseRequireInsteadOfGet]
              checkNotNull(fragment.fragmentManager)
              ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
          src/foo/Test.kt:19: Error: Use fragment.requireContext() instead of checkNotNull(fragment.context) [UseRequireInsteadOfGet]
              checkNotNull(fragment.context)
              ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
          src/foo/Test.kt:20: Error: Use fragment.requireActivity() instead of checkNotNull(fragment.activity) [UseRequireInsteadOfGet]
              checkNotNull(fragment.activity)
              ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
          src/foo/Test.kt:21: Error: Use fragment.requireHost() instead of checkNotNull(fragment.host) [UseRequireInsteadOfGet]
              checkNotNull(fragment.host)
              ~~~~~~~~~~~~~~~~~~~~~~~~~~~
          src/foo/Test.kt:22: Error: Use fragment.requireParentFragment() instead of checkNotNull(fragment.parentFragment) [UseRequireInsteadOfGet]
              checkNotNull(fragment.parentFragment)
              ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
          src/foo/Test.kt:23: Error: Use fragment.requireView() instead of checkNotNull(fragment.view) [UseRequireInsteadOfGet]
              checkNotNull(fragment.view)
              ~~~~~~~~~~~~~~~~~~~~~~~~~~~
          src/foo/Test.kt:26: Error: Use fragment.requireArguments() instead of fragment.getArguments()!! [UseRequireInsteadOfGet]
              fragment.getArguments()!!
              ~~~~~~~~~~~~~~~~~~~~~~~~~
          src/foo/Test.kt:27: Error: Use fragment.requireFragmentManager() instead of fragment.getFragmentManager()!! [UseRequireInsteadOfGet]
              fragment.getFragmentManager()!!
              ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
          src/foo/Test.kt:28: Error: Use fragment.requireContext() instead of fragment.getContext()!! [UseRequireInsteadOfGet]
              fragment.getContext()!!
              ~~~~~~~~~~~~~~~~~~~~~~~
          src/foo/Test.kt:29: Error: Use fragment.requireActivity() instead of fragment.getActivity()!! [UseRequireInsteadOfGet]
              fragment.getActivity()!!
              ~~~~~~~~~~~~~~~~~~~~~~~~
          src/foo/Test.kt:30: Error: Use fragment.requireHost() instead of fragment.getHost()!! [UseRequireInsteadOfGet]
              fragment.getHost()!!
              ~~~~~~~~~~~~~~~~~~~~
          src/foo/Test.kt:31: Error: Use fragment.requireParentFragment() instead of fragment.getParentFragment()!! [UseRequireInsteadOfGet]
              fragment.getParentFragment()!!
              ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
          src/foo/Test.kt:32: Error: Use fragment.requireView() instead of fragment.getView()!! [UseRequireInsteadOfGet]
              fragment.getView()!!
              ~~~~~~~~~~~~~~~~~~~~
          src/foo/Test.kt:33: Error: Use fragment.requireArguments() instead of fragment.arguments!! [UseRequireInsteadOfGet]
              fragment.arguments!!
              ~~~~~~~~~~~~~~~~~~~~
          src/foo/Test.kt:34: Error: Use fragment.requireFragmentManager() instead of fragment.fragmentManager!! [UseRequireInsteadOfGet]
              fragment.fragmentManager!!
              ~~~~~~~~~~~~~~~~~~~~~~~~~~
          src/foo/Test.kt:35: Error: Use fragment.requireContext() instead of fragment.context!! [UseRequireInsteadOfGet]
              fragment.context!!
              ~~~~~~~~~~~~~~~~~~
          src/foo/Test.kt:36: Error: Use fragment.requireActivity() instead of fragment.activity!! [UseRequireInsteadOfGet]
              fragment.activity!!
              ~~~~~~~~~~~~~~~~~~~
          src/foo/Test.kt:37: Error: Use fragment.requireHost() instead of fragment.host!! [UseRequireInsteadOfGet]
              fragment.host!!
              ~~~~~~~~~~~~~~~
          src/foo/Test.kt:38: Error: Use fragment.requireParentFragment() instead of fragment.parentFragment!! [UseRequireInsteadOfGet]
              fragment.parentFragment!!
              ~~~~~~~~~~~~~~~~~~~~~~~~~
          src/foo/Test.kt:39: Error: Use fragment.requireView() instead of fragment.view!! [UseRequireInsteadOfGet]
              fragment.view!!
              ~~~~~~~~~~~~~~~
          28 errors, 0 warnings
                """.trimIndent()
            )
            .expectFixDiffs(
                """
          Fix for src/foo/Test.kt line 9: Replace with fragment.requireArguments():
          @@ -9 +9
          -     checkNotNull(fragment.getArguments())
          +     fragment.requireArguments()
          Fix for src/foo/Test.kt line 10: Replace with fragment.requireFragmentManager():
          @@ -10 +10
          -     checkNotNull(fragment.getFragmentManager())
          +     fragment.requireFragmentManager()
          Fix for src/foo/Test.kt line 11: Replace with fragment.requireContext():
          @@ -11 +11
          -     checkNotNull(fragment.getContext())
          +     fragment.requireContext()
          Fix for src/foo/Test.kt line 12: Replace with fragment.requireActivity():
          @@ -12 +12
          -     checkNotNull(fragment.getActivity())
          +     fragment.requireActivity()
          Fix for src/foo/Test.kt line 13: Replace with fragment.requireHost():
          @@ -13 +13
          -     checkNotNull(fragment.getHost())
          +     fragment.requireHost()
          Fix for src/foo/Test.kt line 14: Replace with fragment.requireParentFragment():
          @@ -14 +14
          -     checkNotNull(fragment.getParentFragment())
          +     fragment.requireParentFragment()
          Fix for src/foo/Test.kt line 15: Replace with fragment.requireView():
          @@ -15 +15
          -     checkNotNull(fragment.getView())
          +     fragment.requireView()
          Fix for src/foo/Test.kt line 17: Replace with fragment.requireArguments():
          @@ -17 +17
          -     checkNotNull(fragment.arguments)
          +     fragment.requireArguments()
          Fix for src/foo/Test.kt line 18: Replace with fragment.requireFragmentManager():
          @@ -18 +18
          -     checkNotNull(fragment.fragmentManager)
          +     fragment.requireFragmentManager()
          Fix for src/foo/Test.kt line 19: Replace with fragment.requireContext():
          @@ -19 +19
          -     checkNotNull(fragment.context)
          +     fragment.requireContext()
          Fix for src/foo/Test.kt line 20: Replace with fragment.requireActivity():
          @@ -20 +20
          -     checkNotNull(fragment.activity)
          +     fragment.requireActivity()
          Fix for src/foo/Test.kt line 21: Replace with fragment.requireHost():
          @@ -21 +21
          -     checkNotNull(fragment.host)
          +     fragment.requireHost()
          Fix for src/foo/Test.kt line 22: Replace with fragment.requireParentFragment():
          @@ -22 +22
          -     checkNotNull(fragment.parentFragment)
          +     fragment.requireParentFragment()
          Fix for src/foo/Test.kt line 23: Replace with fragment.requireView():
          @@ -23 +23
          -     checkNotNull(fragment.view)
          +     fragment.requireView()
          Fix for src/foo/Test.kt line 26: Replace with fragment.requireArguments():
          @@ -26 +26
          -     fragment.getArguments()!!
          +     fragment.requireArguments()
          Fix for src/foo/Test.kt line 27: Replace with fragment.requireFragmentManager():
          @@ -27 +27
          -     fragment.getFragmentManager()!!
          +     fragment.requireFragmentManager()
          Fix for src/foo/Test.kt line 28: Replace with fragment.requireContext():
          @@ -28 +28
          -     fragment.getContext()!!
          +     fragment.requireContext()
          Fix for src/foo/Test.kt line 29: Replace with fragment.requireActivity():
          @@ -29 +29
          -     fragment.getActivity()!!
          +     fragment.requireActivity()
          Fix for src/foo/Test.kt line 30: Replace with fragment.requireHost():
          @@ -30 +30
          -     fragment.getHost()!!
          +     fragment.requireHost()
          Fix for src/foo/Test.kt line 31: Replace with fragment.requireParentFragment():
          @@ -31 +31
          -     fragment.getParentFragment()!!
          +     fragment.requireParentFragment()
          Fix for src/foo/Test.kt line 32: Replace with fragment.requireView():
          @@ -32 +32
          -     fragment.getView()!!
          +     fragment.requireView()
          Fix for src/foo/Test.kt line 33: Replace with fragment.requireArguments():
          @@ -33 +33
          -     fragment.arguments!!
          +     fragment.requireArguments()
          Fix for src/foo/Test.kt line 34: Replace with fragment.requireFragmentManager():
          @@ -34 +34
          -     fragment.fragmentManager!!
          +     fragment.requireFragmentManager()
          Fix for src/foo/Test.kt line 35: Replace with fragment.requireContext():
          @@ -35 +35
          -     fragment.context!!
          +     fragment.requireContext()
          Fix for src/foo/Test.kt line 36: Replace with fragment.requireActivity():
          @@ -36 +36
          -     fragment.activity!!
          +     fragment.requireActivity()
          Fix for src/foo/Test.kt line 37: Replace with fragment.requireHost():
          @@ -37 +37
          -     fragment.host!!
          +     fragment.requireHost()
          Fix for src/foo/Test.kt line 38: Replace with fragment.requireParentFragment():
          @@ -38 +38
          -     fragment.parentFragment!!
          +     fragment.requireParentFragment()
          Fix for src/foo/Test.kt line 39: Replace with fragment.requireView():
          @@ -39 +39
          -     fragment.view!!
          +     fragment.requireView()
                """.trimIndent()
            )
    }

    @Test
    fun `kotlin get then require`() {
        // Note we don't import a preconditions stub here because we use kotlin's built-in
        useRequireLint()
            .files(
                fragmentStub,
                kotlin(
                    """
              package foo

              import androidx.fragment.app.Fragment

              class Test : Fragment() {
                fun test() {
                  parentFragment?.requireView()!!
                }
              }
            """
                ).indented()
            )
            .allowCompilationErrors(false)
            .skipTestModes(TestMode.WHITESPACE) // b/203246909
            .run()
            .expect(
                """
          src/foo/Test.kt:7: Error: Use parentFragment!!.requireView() instead of parentFragment?.requireView()!! [UseRequireInsteadOfGet]
              parentFragment?.requireView()!!
              ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
          1 errors, 0 warnings
                """.trimIndent()
            )
            .expectFixDiffs(
                """
          Fix for src/foo/Test.kt line 7: Replace with parentFragment!!.requireView():
          @@ -7 +7
          -     parentFragment?.requireView()!!
          +     parentFragment!!.requireView()
                """.trimIndent()
            )
    }

    @Test
    fun `simple kotlin get non null with require`() {
        // Note we don't import a preconditions stub here because we use kotlin's built-in
        useRequireLint()
            .files(
                fragmentStub,
                kotlin(
                    """
              package foo

              import androidx.fragment.app.Fragment

              class Test : Fragment() {
                fun test() {
                  parentFragment!!.requireView()
                }
              }
            """
                ).indented()
            )
            .allowCompilationErrors(false)
            .skipTestModes(TestMode.WHITESPACE) // b/203246909
            .run()
            .expect(
                """
          src/foo/Test.kt:7: Error: Use requireParentFragment() instead of parentFragment!! [UseRequireInsteadOfGet]
              parentFragment!!.requireView()
              ~~~~~~~~~~~~~~~~
          1 errors, 0 warnings
                """.trimIndent()
            )
            .expectFixDiffs(
                """
          Fix for src/foo/Test.kt line 7: Replace with requireParentFragment():
          @@ -7 +7
          -     parentFragment!!.requireView()
          +     requireParentFragment().requireView()
                """.trimIndent()
            )
    }

    @Test
    fun `simple kotlin checks where the code is in a fragment`() {
        // Note we don't import a preconditions stub here because we use kotlin's built-in
        useRequireLint()
            .files(
                fragmentStub,
                kotlin(
                    """
              package foo

              import androidx.fragment.app.Fragment

              class Test : Fragment() {
                fun test() {
                  checkNotNull(getArguments())
                  checkNotNull(getFragmentManager())
                  checkNotNull(getContext())
                  checkNotNull(getActivity())
                  checkNotNull(getHost())
                  checkNotNull(getParentFragment())
                  checkNotNull(getView())

                  checkNotNull(arguments)
                  checkNotNull(fragmentManager)
                  checkNotNull(context)
                  checkNotNull(activity)
                  checkNotNull(host)
                  checkNotNull(parentFragment)
                  checkNotNull(view)

                  // !! nullchecks
                  getArguments()!!
                  getFragmentManager()!!
                  getContext()!!
                  getActivity()!!
                  getHost()!!
                  getParentFragment()!!
                  getView()!!
                  arguments!!
                  fragmentManager!!
                  context!!
                  activity!!
                  host!!
                  parentFragment!!
                  view!!

                  // These don't have errors
                  requireArguments()
                  requireFragmentManager()
                  requireContext()
                  requireActivity()
                  requireHost()
                  requireParentFragment()
                  requireView()

                  // These are ignored because they have custom error messages
                  checkNotNull(getArguments()) { "getArguments" }
                  checkNotNull(getFragmentManager()) { "getFragmentManager" }
                  checkNotNull(getContext()) { "getContext" }
                  checkNotNull(getActivity()) { "getActivity" }
                  checkNotNull(getHost()) { "getHost" }
                  checkNotNull(getParentFragment()) { "getParentFragment" }
                  checkNotNull(getView()) { "getView" }
                  requireNonNull(getArguments()) { "getArguments" }
                  requireNonNull(getFragmentManager()) { "getFragmentManager" }
                  requireNonNull(getContext()) { "getContext" }
                  requireNonNull(getActivity()) { "getActivity" }
                  requireNonNull(getHost()) { "getHost" }
                  requireNonNull(getParentFragment()) { "getParentFragment" }
                  requireNonNull(getView()) { "getView" }
                  checkNotNull(arguments) { "getArguments" }
                  checkNotNull(fragmentManager) { "getFragmentManager" }
                  checkNotNull(context) { "getContext" }
                  checkNotNull(activity) { "getActivity" }
                  checkNotNull(host) { "getHost" }
                  checkNotNull(parentFragment) { "getParentFragment" }
                  checkNotNull(view) { "getView" }
                  requireNonNull(arguments) { "getArguments" }
                  requireNonNull(fragmentManager) { "getFragmentManager" }
                  requireNonNull(context) { "getContext" }
                  requireNonNull(activity) { "getActivity" }
                  requireNonNull(host) { "getHost" }
                  requireNonNull(parentFragment) { "getParentFragment" }
                  requireNonNull(view) { "getView" }
                }
              }
            """
                ).indented()
            )
            .allowCompilationErrors(false)
            .skipTestModes(TestMode.WHITESPACE) // b/203246909
            .run()
            .expect(
                """
          src/foo/Test.kt:7: Error: Use requireArguments() instead of checkNotNull(getArguments()) [UseRequireInsteadOfGet]
              checkNotNull(getArguments())
              ~~~~~~~~~~~~~~~~~~~~~~~~~~~~
          src/foo/Test.kt:8: Error: Use requireFragmentManager() instead of checkNotNull(getFragmentManager()) [UseRequireInsteadOfGet]
              checkNotNull(getFragmentManager())
              ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
          src/foo/Test.kt:9: Error: Use requireContext() instead of checkNotNull(getContext()) [UseRequireInsteadOfGet]
              checkNotNull(getContext())
              ~~~~~~~~~~~~~~~~~~~~~~~~~~
          src/foo/Test.kt:10: Error: Use requireActivity() instead of checkNotNull(getActivity()) [UseRequireInsteadOfGet]
              checkNotNull(getActivity())
              ~~~~~~~~~~~~~~~~~~~~~~~~~~~
          src/foo/Test.kt:11: Error: Use requireHost() instead of checkNotNull(getHost()) [UseRequireInsteadOfGet]
              checkNotNull(getHost())
              ~~~~~~~~~~~~~~~~~~~~~~~
          src/foo/Test.kt:12: Error: Use requireParentFragment() instead of checkNotNull(getParentFragment()) [UseRequireInsteadOfGet]
              checkNotNull(getParentFragment())
              ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
          src/foo/Test.kt:13: Error: Use requireView() instead of checkNotNull(getView()) [UseRequireInsteadOfGet]
              checkNotNull(getView())
              ~~~~~~~~~~~~~~~~~~~~~~~
          src/foo/Test.kt:15: Error: Use requireArguments() instead of checkNotNull(arguments) [UseRequireInsteadOfGet]
              checkNotNull(arguments)
              ~~~~~~~~~~~~~~~~~~~~~~~
          src/foo/Test.kt:16: Error: Use requireFragmentManager() instead of checkNotNull(fragmentManager) [UseRequireInsteadOfGet]
              checkNotNull(fragmentManager)
              ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
          src/foo/Test.kt:17: Error: Use requireContext() instead of checkNotNull(context) [UseRequireInsteadOfGet]
              checkNotNull(context)
              ~~~~~~~~~~~~~~~~~~~~~
          src/foo/Test.kt:18: Error: Use requireActivity() instead of checkNotNull(activity) [UseRequireInsteadOfGet]
              checkNotNull(activity)
              ~~~~~~~~~~~~~~~~~~~~~~
          src/foo/Test.kt:19: Error: Use requireHost() instead of checkNotNull(host) [UseRequireInsteadOfGet]
              checkNotNull(host)
              ~~~~~~~~~~~~~~~~~~
          src/foo/Test.kt:20: Error: Use requireParentFragment() instead of checkNotNull(parentFragment) [UseRequireInsteadOfGet]
              checkNotNull(parentFragment)
              ~~~~~~~~~~~~~~~~~~~~~~~~~~~~
          src/foo/Test.kt:21: Error: Use requireView() instead of checkNotNull(view) [UseRequireInsteadOfGet]
              checkNotNull(view)
              ~~~~~~~~~~~~~~~~~~
          src/foo/Test.kt:24: Error: Use requireArguments() instead of getArguments()!! [UseRequireInsteadOfGet]
              getArguments()!!
              ~~~~~~~~~~~~~~~~
          src/foo/Test.kt:25: Error: Use requireFragmentManager() instead of getFragmentManager()!! [UseRequireInsteadOfGet]
              getFragmentManager()!!
              ~~~~~~~~~~~~~~~~~~~~~~
          src/foo/Test.kt:26: Error: Use requireContext() instead of getContext()!! [UseRequireInsteadOfGet]
              getContext()!!
              ~~~~~~~~~~~~~~
          src/foo/Test.kt:27: Error: Use requireActivity() instead of getActivity()!! [UseRequireInsteadOfGet]
              getActivity()!!
              ~~~~~~~~~~~~~~~
          src/foo/Test.kt:28: Error: Use requireHost() instead of getHost()!! [UseRequireInsteadOfGet]
              getHost()!!
              ~~~~~~~~~~~
          src/foo/Test.kt:29: Error: Use requireParentFragment() instead of getParentFragment()!! [UseRequireInsteadOfGet]
              getParentFragment()!!
              ~~~~~~~~~~~~~~~~~~~~~
          src/foo/Test.kt:30: Error: Use requireView() instead of getView()!! [UseRequireInsteadOfGet]
              getView()!!
              ~~~~~~~~~~~
          src/foo/Test.kt:31: Error: Use requireArguments() instead of arguments!! [UseRequireInsteadOfGet]
              arguments!!
              ~~~~~~~~~~~
          src/foo/Test.kt:32: Error: Use requireFragmentManager() instead of fragmentManager!! [UseRequireInsteadOfGet]
              fragmentManager!!
              ~~~~~~~~~~~~~~~~~
          src/foo/Test.kt:33: Error: Use requireContext() instead of context!! [UseRequireInsteadOfGet]
              context!!
              ~~~~~~~~~
          src/foo/Test.kt:34: Error: Use requireActivity() instead of activity!! [UseRequireInsteadOfGet]
              activity!!
              ~~~~~~~~~~
          src/foo/Test.kt:35: Error: Use requireHost() instead of host!! [UseRequireInsteadOfGet]
              host!!
              ~~~~~~
          src/foo/Test.kt:36: Error: Use requireParentFragment() instead of parentFragment!! [UseRequireInsteadOfGet]
              parentFragment!!
              ~~~~~~~~~~~~~~~~
          src/foo/Test.kt:37: Error: Use requireView() instead of view!! [UseRequireInsteadOfGet]
              view!!
              ~~~~~~
          28 errors, 0 warnings
                """.trimIndent()
            )
            .expectFixDiffs(
                """
          Fix for src/foo/Test.kt line 7: Replace with requireArguments():
          @@ -7 +7
          -     checkNotNull(getArguments())
          +     requireArguments()
          Fix for src/foo/Test.kt line 8: Replace with requireFragmentManager():
          @@ -8 +8
          -     checkNotNull(getFragmentManager())
          +     requireFragmentManager()
          Fix for src/foo/Test.kt line 9: Replace with requireContext():
          @@ -9 +9
          -     checkNotNull(getContext())
          +     requireContext()
          Fix for src/foo/Test.kt line 10: Replace with requireActivity():
          @@ -10 +10
          -     checkNotNull(getActivity())
          +     requireActivity()
          Fix for src/foo/Test.kt line 11: Replace with requireHost():
          @@ -11 +11
          -     checkNotNull(getHost())
          +     requireHost()
          Fix for src/foo/Test.kt line 12: Replace with requireParentFragment():
          @@ -12 +12
          -     checkNotNull(getParentFragment())
          +     requireParentFragment()
          Fix for src/foo/Test.kt line 13: Replace with requireView():
          @@ -13 +13
          -     checkNotNull(getView())
          +     requireView()
          Fix for src/foo/Test.kt line 15: Replace with requireArguments():
          @@ -15 +15
          -     checkNotNull(arguments)
          +     requireArguments()
          Fix for src/foo/Test.kt line 16: Replace with requireFragmentManager():
          @@ -16 +16
          -     checkNotNull(fragmentManager)
          +     requireFragmentManager()
          Fix for src/foo/Test.kt line 17: Replace with requireContext():
          @@ -17 +17
          -     checkNotNull(context)
          +     requireContext()
          Fix for src/foo/Test.kt line 18: Replace with requireActivity():
          @@ -18 +18
          -     checkNotNull(activity)
          +     requireActivity()
          Fix for src/foo/Test.kt line 19: Replace with requireHost():
          @@ -19 +19
          -     checkNotNull(host)
          +     requireHost()
          Fix for src/foo/Test.kt line 20: Replace with requireParentFragment():
          @@ -20 +20
          -     checkNotNull(parentFragment)
          +     requireParentFragment()
          Fix for src/foo/Test.kt line 21: Replace with requireView():
          @@ -21 +21
          -     checkNotNull(view)
          +     requireView()
          Fix for src/foo/Test.kt line 24: Replace with requireArguments():
          @@ -24 +24
          -     getArguments()!!
          +     requireArguments()
          Fix for src/foo/Test.kt line 25: Replace with requireFragmentManager():
          @@ -25 +25
          -     getFragmentManager()!!
          +     requireFragmentManager()
          Fix for src/foo/Test.kt line 26: Replace with requireContext():
          @@ -26 +26
          -     getContext()!!
          +     requireContext()
          Fix for src/foo/Test.kt line 27: Replace with requireActivity():
          @@ -27 +27
          -     getActivity()!!
          +     requireActivity()
          Fix for src/foo/Test.kt line 28: Replace with requireHost():
          @@ -28 +28
          -     getHost()!!
          +     requireHost()
          Fix for src/foo/Test.kt line 29: Replace with requireParentFragment():
          @@ -29 +29
          -     getParentFragment()!!
          +     requireParentFragment()
          Fix for src/foo/Test.kt line 30: Replace with requireView():
          @@ -30 +30
          -     getView()!!
          +     requireView()
          Fix for src/foo/Test.kt line 31: Replace with requireArguments():
          @@ -31 +31
          -     arguments!!
          +     requireArguments()
          Fix for src/foo/Test.kt line 32: Replace with requireFragmentManager():
          @@ -32 +32
          -     fragmentManager!!
          +     requireFragmentManager()
          Fix for src/foo/Test.kt line 33: Replace with requireContext():
          @@ -33 +33
          -     context!!
          +     requireContext()
          Fix for src/foo/Test.kt line 34: Replace with requireActivity():
          @@ -34 +34
          -     activity!!
          +     requireActivity()
          Fix for src/foo/Test.kt line 35: Replace with requireHost():
          @@ -35 +35
          -     host!!
          +     requireHost()
          Fix for src/foo/Test.kt line 36: Replace with requireParentFragment():
          @@ -36 +36
          -     parentFragment!!
          +     requireParentFragment()
          Fix for src/foo/Test.kt line 37: Replace with requireView():
          @@ -37 +37
          -     view!!
          +     requireView()
                """.trimIndent()
            )
    }

    @Test
    fun `view local variables should be ignored`() {
        useRequireLint()
            .files(
                fragmentStub,
                preconditionsStub,
                java(
                    """
                  package foo;

                  import androidx.fragment.app.Fragment;
                  import util.Preconditions;

                  class TestFragment extends Fragment {
                    void test() {
                      View view = null;
                      Preconditions.checkNotNull(view);
                    }
                  }
                """
                ).indented()
            )
            .allowCompilationErrors(false)
            .run()
            .expectClean()
    }

    @Test
    fun `activity local variables should be ignored`() {
        useRequireLint()
            .files(
                fragmentStub,
                preconditionsStub,
                java(
                    """
                  package foo;

                  import androidx.fragment.app.Fragment;
                  import util.Preconditions;

                  class TestFragment extends Fragment {
                    void test() {
                      Activity activity = null;
                      Preconditions.checkNotNull(activity);
                    }
                  }
                """
                ).indented()
            )
            .allowCompilationErrors(false)
            .run()
            .expectClean()
    }
}
/* ktlint-enable max-line-length */
