/*
 * Copyright (c) 2023 Uber Technologies, Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package com.uber.nullaway;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class NullAwayTypeUseAnnotationsTests extends NullAwayTestsBase {

  @Test
  public void annotationAppliedToTypeParameter() {
    defaultCompilationHelper
        .addSourceLines(
            "Test.java",
            "package com.uber;",
            "import java.util.List;",
            "import java.util.ArrayList;",
            "import org.checkerframework.checker.nullness.qual.Nullable;",
            "class TypeArgumentAnnotation {",
            "  List<@Nullable String> fSafe = new ArrayList<>();",
            "  @Nullable List<String> fUnsafe = new ArrayList<>();",
            "  void useParamSafe(List<@Nullable String> list) {",
            "    list.hashCode();",
            "  }",
            "  void useParamUnsafe(@Nullable List<String> list) {",
            "    // BUG: Diagnostic contains: dereferenced",
            "    list.hashCode();",
            "  }",
            "  void useFieldSafe() {",
            "    fSafe.hashCode();",
            "  }",
            "  void useFieldUnsafe() {",
            "    // BUG: Diagnostic contains: dereferenced",
            "    fUnsafe.hashCode();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void annotationAppliedToInnerTypeImplicitly() {
    defaultCompilationHelper
        .addSourceLines(
            "Test.java",
            "package com.uber;",
            "import org.checkerframework.checker.nullness.qual.Nullable;",
            "class Test {",
            "  @Nullable Foo f;", // i.e. Test.@Nullable Foo
            "  class Foo { }",
            "  public void test() {",
            "    // BUG: Diagnostic contains: dereferenced",
            "    f.hashCode();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void annotationAppliedToInnerTypeImplicitlyWithTypeArgs() {
    defaultCompilationHelper
        .addSourceLines(
            "Test.java",
            "package com.uber;",
            "import org.checkerframework.checker.nullness.qual.Nullable;",
            "class Test {",
            "  @Nullable Foo<String> f1 = null;", // i.e. Test.@Nullable Foo (location [INNER])
            "  // BUG: Diagnostic contains: assigning @Nullable expression to @NonNull field",
            "  Foo<@Nullable String> f2 = null;", // (location [INNER, TYPE_ARG(0)])
            "  @Nullable Foo<@Nullable String> f3 = null;", // two annotations, each with the
            // locations above
            "  class Foo<T> { }",
            "  public void test() {",
            "    // BUG: Diagnostic contains: dereferenced",
            "    f1.hashCode();",
            "    // safe, because nonnull",
            "    f2.hashCode();",
            "    // BUG: Diagnostic contains: dereferenced",
            "    f3.hashCode();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void annotationAppliedToInnerTypeExplicitly() {
    defaultCompilationHelper
        .addSourceLines(
            "Test.java",
            "package com.uber;",
            "import org.checkerframework.checker.nullness.qual.Nullable;",
            "class Test {",
            "  Test.@Nullable Foo f1;",
            "  // BUG: Diagnostic contains: @NonNull field f2 not initialized",
            "  @Nullable Test.Foo f2;",
            "  class Foo { }",
            "  public void test() {",
            "    // BUG: Diagnostic contains: dereferenced",
            "    f1.hashCode();",
            "    f2.hashCode();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void annotationAppliedToInnerTypeExplicitly2() {
    defaultCompilationHelper
        .addSourceLines(
            "Bar.java",
            "package com.uber;",
            "import org.checkerframework.checker.nullness.qual.Nullable;",
            "class Bar {",
            "  public class Foo { }",
            "}")
        .addSourceLines(
            "Test.java",
            "package com.uber;",
            "import org.checkerframework.checker.nullness.qual.Nullable;",
            "class Test {",
            "  Bar.@Nullable Foo f1;",
            "  // BUG: Diagnostic contains: @NonNull field f2 not initialized",
            "  @Nullable Bar.Foo f2;",
            "  public void test() {",
            "    // BUG: Diagnostic contains: dereferenced",
            "    f1.hashCode();",
            "    f2.hashCode();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void annotationAppliedToInnerTypeOfTypeArgument() {
    defaultCompilationHelper
        .addSourceLines(
            "Test.java",
            "package com.uber;",
            "import java.util.Set;",
            "import org.checkerframework.checker.nullness.qual.Nullable;",
            "class Test {",
            "  // BUG: Diagnostic contains: @NonNull field s not initialized",
            "  Set<@Nullable Foo> s;", // i.e. Set<Test.@Nullable Foo>
            "  class Foo { }",
            "  public void test() {",
            "    // safe because field is @NonNull",
            "    s.hashCode();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void typeUseAnnotationOnArray() {
    defaultCompilationHelper
        .addSourceLines(
            "Test.java",
            "package com.uber;",
            "import org.checkerframework.checker.nullness.qual.Nullable;",
            "class Test {",
            "  // BUG: Diagnostic contains: assigning @Nullable expression to @NonNull field",
            "  @Nullable Object[] foo1 = null;",
            "  // ok according to spec",
            "  Object @Nullable[] foo2 = null;",
            "  // BUG: Diagnostic contains: assigning @Nullable expression to @NonNull field",
            "  @Nullable Object [][] foo3 = null;",
            "  // ok according to spec",
            "  Object @Nullable [][] foo4 = null;",
            "  // BUG: Diagnostic contains: assigning @Nullable expression to @NonNull field",
            "  Object [] @Nullable [] foo5 = null;",
            "}")
        .doTest();
  }

  @Test
  public void typeUseAnnotationOnInnerMultiLevel() {
    defaultCompilationHelper
        .addSourceLines(
            "Test.java",
            "package com.uber;",
            "import java.util.Set;",
            "import org.checkerframework.checker.nullness.qual.Nullable;",
            "class A { class B { class C {} } }",
            "class Test {",
            "  // BUG: Diagnostic contains: assigning @Nullable expression to @NonNull field",
            "  @Nullable A.B.C foo1 = null;",
            "  // BUG: Diagnostic contains: assigning @Nullable expression to @NonNull field",
            "  A.@Nullable B.C foo2 = null;",
            "  A.B.@Nullable C foo3 = null;",
            "  // BUG: Diagnostic contains: assigning @Nullable expression to @NonNull field",
            "  A.B.@Nullable C [][] foo4 = null;",
            "}")
        .doTest();
  }
}
