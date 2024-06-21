package com.github.zastai.apiref.internal;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.SortedMap;

/** Various utility methods. */
public interface Util {

  /**
   * Returns an unmodifiable version of the specified map.
   *
   * @param map The map to make unmodifiable; if this is {@code null}, an empty map will be returned.
   * @param <K> The key type for the map.
   * @param <V> The value type for the map.
   *
   * @return An unmodifiable version of {@code map}.
   */
  @NotNull
  static <K, V> SortedMap<K, V> makeUnmodifiable(@Nullable SortedMap<K, V> map) {
    return map == null ? Collections.emptySortedMap() : Collections.unmodifiableSortedMap(map);
  }

  /**
   * Returns a string describing the Java runtime version corresponding to a major version as specified in a class file.
   *
   * @param majorVersion The major version of a class file.
   *
   * @return A string describing the Java runtime version corresponding to a major version as specified in a class file.
   */
  @NotNull
  static String runtimeVersion(int majorVersion) {
    if (majorVersion < 45) {
      throw new IllegalArgumentException("Invalid version (%d) specified.".formatted(majorVersion));
    }
    return "Java SE " + switch (majorVersion) {
      case 45 -> "1.1";
      case 46 -> "1.2";
      case 47 -> "1.3";
      case 48 -> "1.4";
      case 49 -> "5.0";
      case 50 -> "6";
      case 51 -> "7";
      case 52 -> "8";
      case 53 -> "9";
      case 54 -> "10";
      case 55 -> "11";
      case 56 -> "12";
      case 57 -> "13";
      case 58 -> "14";
      case 59 -> "15";
      case 60 -> "16";
      case 61 -> "17";
      case 62 -> "18";
      case 63 -> "19";
      case 64 -> "20";
      default -> "21+";
    };
  }

}
