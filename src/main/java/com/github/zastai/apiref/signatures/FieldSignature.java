package com.github.zastai.apiref.signatures;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.signature.SignatureReader;

/** A decoded generics signature for a field. */
public final class FieldSignature {

  /** The parent of the class, if it has one (Object is ignored), or the type of the field. */
  public @NotNull TypeReference type;

  private FieldSignature(@NotNull Visitor visitor) {
    if (visitor.baseClass == null) {
      throw new IllegalArgumentException("A field signature must include a base type.");
    }
    if (visitor.baseInterfaces != null) {
      throw new IllegalArgumentException("A field signature should not include base interfaces.");
    }
    if (visitor.parameterTypes != null) {
      throw new IllegalArgumentException("A field signature should not include parameter types.");
    }
    if (visitor.returnType != null) {
      throw new IllegalArgumentException("A field signature should not include a return type.");
    }
    if (visitor.thrownTypes != null) {
      throw new IllegalArgumentException("A field signature should not include thrown exception types.");
    }
    if (visitor.typeParameters != null) {
      throw new IllegalArgumentException("A field signature should not include type parameters.");
    }
    this.type = visitor.baseClass;
  }

  /**
   * Decodes a field signature.
   *
   * @param signature The field signature to decode.
   *
   * @return The decoded field signature.
   */
  @NotNull
  public static FieldSignature decode(@NotNull String signature) {
    final var reader = new SignatureReader(signature);
    final var visitor = new Visitor();
    reader.accept(visitor);
    return new FieldSignature(visitor);
  }

}
