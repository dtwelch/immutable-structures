package io.github.dt;

import java.lang.reflect.Array;
import java.util.Arrays;

public final class Utils {

  private Utils() {}

  @SafeVarargs
  public static <T> T[] arrayOf(T... items) {
    return items;
  }

  @SuppressWarnings("unchecked")
  public static <S, T> T[] newArrayLike(S[] example, int length) {
    return (T[]) Array.newInstance(example.getClass().getComponentType(), length);
  }

  public static String mkString(Iterable<?> items, String delimiter) {
    return mkString(items, "", delimiter, "");
  }

  public static String mkString(Iterable<?> items, String left, String delimiter, String right) {
    var sb = new StringBuilder(left);
    var first = true;
    for (var item : items) {
      if (!first) {
        sb.append(delimiter);
      }
      sb.append(item);
      first = false;
    }
    return sb.append(right).toString();
  }
}
