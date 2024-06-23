package com.github.zastai.apiref.signatures;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** A formal type parameter in a signature. */
public class FormalTypeParameter {

  /** The name of a type parameter. */
  public final @NotNull String name;

  /** The required base class for the type parameter, if there is one. */
  public final @Nullable TypeReference classBound;

  /** The required implemented interfaces for the type parameter, if there are any. */
  public final @NotNull TypeReference @Nullable [] interfaceBounds;

  FormalTypeParameter(@NotNull String name, @Nullable TypeReference extended, @Nullable List<@NotNull TypeReference> implemented) {
    this.name = name;
    this.classBound = extended;
    this.interfaceBounds = implemented == null ? null : implemented.toArray(TypeReference[]::new);
  }

}
