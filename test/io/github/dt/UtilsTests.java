package io.github.dt;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public final class UtilsTests {

  @Test
  void arrayOf01() {
    var xs = Utils.arrayOf(1, 2, 3);

    Assertions.assertArrayEquals(new Integer[] {1, 2, 3}, xs);
  }

  @Test
  void newArrayLike01() {
    var xs = new String[] {"a", "b"};

    var ys = Utils.<String, String>newArrayLike(xs, 4);

    Assertions.assertEquals(4, ys.length);
    Assertions.assertEquals(String.class, ys.getClass().getComponentType());
  }

  @Test
  void mkString01() {
    Assertions.assertEquals("1, 2, 3", Utils.mkString(VList.of(1, 2, 3), ", "));
  }

  @Test
  void mkString02() {
    Assertions.assertEquals("[1, 2, 3]", Utils.mkString(VList.of(1, 2, 3), "[", ", ", "]"));
    Assertions.assertEquals("[]", Utils.mkString(VList.empty(), "[", ", ", "]"));
  }
}
