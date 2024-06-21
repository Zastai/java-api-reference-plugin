package com.github.zastai.apiref.model;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.tree.ClassNode;

/** A Java module */
public class JavaModule {

  /** The module's name. */
  @NotNull
  public final String name;

  /** The {@code module-info} pseudo-class for the module, if there is one. */
  @Nullable
  public final ClassNode info;

  // FIXME: Maybe have the exported packages as JavaPackage instances?q

  /**
   * Creates a new Java module.
   *
   * @param name The module's name.
   * @param info The {@code module-info} pseudo-class for the module, if there is one.
   */
  public JavaModule(@NotNull String name, @Nullable ClassNode info) {
    this.name = name;
    this.info = info;
  }
}
