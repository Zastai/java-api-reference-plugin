package com.github.zastai.apiref.internal;

import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/** Interface providing ASM-related utility methods. */
public interface ASMUtil {

  /**
   * Determines whether a given class has any associated annotations (runtime-visible or otherwise).
   *
   * @param cn The class to check.
   *
   * @return {@code true} when the class has associated annotations (runtime-visible or otherwise); {@code false} when it does not.
   */
  static boolean isAnnotated(@NotNull ClassNode cn) {
    return cn.visibleAnnotations != null
      || cn.invisibleAnnotations != null
      || cn.visibleTypeAnnotations != null
      || cn.invisibleTypeAnnotations != null;
  }

  /**
   * Describes a class, for use in diagnostics.
   *
   * @param cn The class to describe.
   *
   * @return A description of the class.
   */
  @NotNull
  static String describe(@NotNull ClassNode cn) {
    String type = "class";
    if ((cn.access & Opcodes.ACC_ANNOTATION) != 0) {
      type = "annotation";
    }
    else if ((cn.access & Opcodes.ACC_ENUM) != 0) {
      type = "enum";
    }
    else if ((cn.access & Opcodes.ACC_INTERFACE) != 0) {
      type = "interface";
    }
    else if ((cn.access & Opcodes.ACC_RECORD) != 0) {
      type = "record";
    }
    return "%s %s".formatted(type, cn.name);
  }

  /**
   * Describes a field, for use in diagnostics.
   *
   * @param cn The class containing the field.
   * @param fn The field to describe.
   *
   * @return A description of the field.
   */
  @NotNull
  static String describe(@NotNull ClassNode cn, @NotNull FieldNode fn) {
    return "field %s.%s".formatted(cn.name, fn.name);
  }

  /**
   * Describes a method, for use in diagnostics.
   *
   * @param cn The class containing the method.
   * @param mn The method to describe.
   *
   * @return A description of the method.
   */
  @NotNull
  static String describe(@NotNull ClassNode cn, @NotNull MethodNode mn) {
    return "method %s.%s%s".formatted(cn.name, mn.name, mn.desc);
  }

  /**
   * Reads a class file into an ASM class node object.
   *
   * @param file The class file to read.
   *
   * @return The ASM {@link ClassNode} representing the class file's contents.
   *
   * @throws IOException When {@code file} could not be read from.
   */
  @NotNull
  static ClassNode readClassFile(@NotNull Path file) throws IOException {
    final ClassNode cn = new ClassNode(Constants.API_VERSION);
    try (InputStream is = Files.newInputStream(file)) {
      new ClassReader(is).accept(cn, ClassReader.EXPAND_FRAMES);
    }
    return cn;
  }

  /**
   * Strips the package name component from an internal class name.
   *
   * @param name The class name.
   *
   * @return The class name, with any package component removed.
   */
  @NotNull
  static String stripPackage(@NotNull String name) {
    final int slash = name.lastIndexOf('/');
    return slash >= 0 ? name.substring(slash + 1) : name;
  }

}
