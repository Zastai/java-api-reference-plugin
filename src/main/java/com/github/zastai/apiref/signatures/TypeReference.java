package com.github.zastai.apiref.signatures;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** A type reference that is part of a signature. */
public final class TypeReference {

  /** The number of array dimensions for this type (i.e. the number of '[]' at the end). */
  public int arrayDimensions;

  /** Indicates whether {@link #name} contains the name of a type variable or type descriptor. */
  public boolean isTypeVariable;

  /** The name of the type; this will be either a type descriptor or the name of a type variable. */
  public final @NotNull String name;

  /** The qualifier for this nested type. If this is not {@code null}, {@link #name} will be a plain identifier. */
  public final @Nullable TypeReference qualifier;

  /** Any and all type arguments for the type. */
  public final @Nullable TypeReference @Nullable [] typeArguments;

  /** Indicates a wildcard type reference: '+' for {@code ? extends ...}, '-' for {@code ? super ...}. */
  public final @Nullable Character wildcard;

  TypeReference(@NotNull TypeReferenceVisitor visitor) {
    if (visitor.name == null) {
      throw new IllegalArgumentException("The signature type reference processing did not construct any type name.");
    }
    this.arrayDimensions = visitor.arrayDimensions;
    this.isTypeVariable = visitor.isTypeVariable;
    this.name = visitor.name;
    this.qualifier = visitor.qualifier;
    this.typeArguments = visitor.typeArguments == null ? null : visitor.typeArguments.toArray(TypeReference[]::new);
    this.wildcard = visitor.wildcard;
  }

}
