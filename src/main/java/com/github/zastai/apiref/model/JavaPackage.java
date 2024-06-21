package com.github.zastai.apiref.model;

import com.github.zastai.apiref.internal.Util;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;
import org.objectweb.asm.tree.ClassNode;

import java.util.SortedMap;

/** A Java package. */
public final class JavaPackage {

  /** The (internal) name of this package. */
  @NotNull
  public final String name;

  /** The {@code package-info} pseudo-class for the package, if there is one. */
  @Nullable
  public final ClassNode info;

  /** The (public) types contained within this package. */
  @NotNull
  @Unmodifiable
  public final SortedMap<String, JavaType> types;

  /**
   * Creates a new Java package.
   *
   * @param name  The (internal) name of the package.
   * @param info  The {@code package-info} pseudo-class for the package, if there is one.
   * @param types The (public) types contained within this package, if there are any.
   */
  public JavaPackage(@NotNull String name, @Nullable ClassNode info, @Nullable SortedMap<String, JavaType> types) {
    this.name = name;
    this.info = info;
    this.types = Util.makeUnmodifiable(types);
  }

}
