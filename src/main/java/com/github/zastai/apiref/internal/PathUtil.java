package com.github.zastai.apiref.internal;

import org.jetbrains.annotations.NotNull;

import java.nio.file.Files;
import java.nio.file.Path;

/** Path-related utility methods. */
public interface PathUtil {

  /**
   * Determines whether a given path refers to a class file.
   * <p>
   * This means that it must refer to an existing regular file whose name ends in {@code .class}.
   *
   * @param path The path to check.
   *
   * @return {@code true} when {@code path} refers to a class file; {@code false} otherwise.
   */
  static boolean isClassFile(@NotNull Path path) {
    return PathUtil.isFileWithExtension(path, ".class");
  }

  /**
   * Determines whether a given path refers to an existing directory.
   *
   * @param path The path to check.
   *
   * @return {@code true} when {@code path} refers to an existing directory; {@code false} otherwise.
   */
  static boolean isDirectory(@NotNull Path path) {
    return Files.isDirectory(path);
  }

  /**
   * Determines whether a given path refers to an existing regular file with the specified extension.
   *
   * @param path      The path to check.
   * @param extension The extension to test for.
   *
   * @return {@code true} when {@code path} refers to an existing regular file with the specified extension; {@code false}
   * otherwise.
   */
  static boolean isFileWithExtension(@NotNull Path path, @NotNull String extension) {
    return Files.isRegularFile(path) && path.getFileName().toString().endsWith(extension);
  }

  /**
   * Determines whether a given path refers to a jar file.
   * <p>
   * This means that it must refer to an existing regular file whose name ends in {@code .jar}.
   *
   * @param path The path to check.
   *
   * @return {@code true} when {@code path} refers to a class file; {@code false} otherwise.
   */
  static boolean isJarFile(@NotNull Path path) {
    return PathUtil.isFileWithExtension(path, ".jar");
  }

}
