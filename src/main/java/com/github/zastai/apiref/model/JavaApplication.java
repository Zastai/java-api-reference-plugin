package com.github.zastai.apiref.model;

import com.github.zastai.apiref.internal.Util;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.SortedMap;

/** Represents an entire Java application. */
public final class JavaApplication {

  /** The modules contained in the application (if any). */
  @NotNull
  @Unmodifiable
  public final SortedMap<String, JavaModule> modules;

  /** The packages contained in the application (if any). */
  @NotNull
  @Unmodifiable
  public final SortedMap<String, JavaPackage> packages;

  /** The top-level classes contained in the application (if any). */
  @NotNull
  @Unmodifiable
  public final SortedMap<String, JavaType> topLevelTypes;

  /**
   * Creates a new Java application.
   *
   * @param modules       Any and all modules that are part of the application.
   * @param packages      Any and all packages that are part of the application.
   * @param topLevelTypes Any and all top-level classes (i.e. those that are not inside a package) that are part of the application.
   */
  public JavaApplication(@Nullable SortedMap<String, JavaModule> modules, @Nullable SortedMap<String, JavaPackage> packages,
                         @Nullable SortedMap<String, JavaType> topLevelTypes) {
    this.modules = Util.makeUnmodifiable(modules);
    this.packages = Util.makeUnmodifiable(packages);
    this.topLevelTypes = Util.makeUnmodifiable(topLevelTypes);
  }

  /**
   * Scans a set of paths for a Java application.
   *
   * @param paths The paths to scan; these can be folders or jar files.
   *
   * @return A Java application spanning the specified paths.
   */
  @NotNull
  public static JavaApplication discover(@NotNull Path @NotNull ... paths) {
    if (paths.length == 0) {
      return new JavaApplication(null, null, null);
    }
    return JavaApplication.discover(Arrays.asList(paths));
  }

  /**
   * Scans a set of paths for a Java application.
   *
   * @param paths The paths to scan; these can be folders or jar files.
   *
   * @return A Java application spanning the specified paths.
   */
  @NotNull
  public static JavaApplication discover(@NotNull Collection<@NotNull Path> paths) {
    if (paths.isEmpty()) {
      return new JavaApplication(null, null, null);
    }
    // TODO
    throw new RuntimeException("Not yet implemented.");
  }

}
