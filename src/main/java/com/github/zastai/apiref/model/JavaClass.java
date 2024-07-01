package com.github.zastai.apiref.model;

import com.github.zastai.apiref.internal.ASMUtil;
import com.github.zastai.apiref.internal.Constants;
import com.github.zastai.apiref.internal.Util;
import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

/** A Java class. */
public class JavaClass {

  /** The loaded class contents. */
  @NotNull
  public final ClassNode contents;

  /** The (externally visible) fields provided by the class. */
  @NotNull
  public final Collection<FieldNode> fields;

  /** The full (internal) name of this class, including a package specification. */
  @NotNull
  public final String fullName;

  /** The (externally visible) methods provided by the class. */
  @NotNull
  public final Collection<MethodNode> methods;

  /** The name of this class. */
  @NotNull
  public final String name;

  /** The Java type to which this class belongs. */
  @NotNull
  public final JavaType parent;

  /** The class file version for this class. */
  public final int version;

  JavaClass(@NotNull ClassNode cn, @NotNull JavaType parent, boolean verbose) {
    this.contents = cn;
    this.fullName = cn.name;
    this.parent = parent;
    this.version = cn.version;
    // Compute the name
    if (parent.parentType != null) {
      final String parentName = parent.parentType.fullName;
      if (!cn.name.startsWith(parentName + '$')) {
        final var msg = String.format("A nested type's name (%s) should start with the parent type's name (%s) followed by '$'.",
                                      cn.name, parentName);
        throw new IllegalArgumentException(msg);
      }
      this.name = cn.name.substring(parentName.length() + 1);
    }
    else {
      this.name = ASMUtil.stripPackage(cn.name);
    }
    if (cn.fields != null) {
      this.fields = cn.fields.stream().filter(fn -> JavaClass.isRelevant(cn, fn, verbose)).toList();
    }
    else {
      this.fields = Collections.emptyList();
    }
    if (cn.methods != null) {
      // FIXME: Maybe we need/want a JavaMethod class, to group overloads together?
      this.methods = cn.methods.stream().filter(mn -> JavaClass.isRelevant(cn, mn, verbose)).toList();
    }
    else {
      this.methods = Collections.emptyList();
    }
  }

  private static boolean isRelevant(@NotNull ClassNode cn, @NotNull FieldNode fn, boolean verbose) {
    if ((fn.access & Constants.ACC_VISIBLE) == 0) {
      if (verbose) {
        System.out.printf("[trace] Skipping %s because it is not externally visible.%n", ASMUtil.describe(cn, fn));
      }
      return false;
    }
    if ((fn.access & Opcodes.ACC_SYNTHETIC) != 0) {
      if (verbose) {
        System.out.printf("[trace] Skipping %s because it is synthetic.%n", ASMUtil.describe(cn, fn));
      }
      return false;
    }
    return true;
  }

  private static boolean isRelevant(@NotNull ClassNode cn, @NotNull MethodNode mn, boolean verbose) {
    if ((mn.access & Constants.ACC_VISIBLE) == 0) {
      if (verbose) {
        System.out.printf("[trace] Skipping %s because it is not externally visible.%n", ASMUtil.describe(cn, mn));
      }
      return false;
    }
    if ((mn.access & Opcodes.ACC_SYNTHETIC) != 0) {
      if (verbose) {
        System.out.printf("[trace] Skipping %s because it is synthetic.%n", ASMUtil.describe(cn, mn));
      }
      return false;
    }
    return true;
  }

  /**
   * Builds a list of the classes nested within this class, if there are any.
   *
   * @return The classes nested within this class, if any.
   */
  @NotNull
  public Collection<JavaClass> nestedClasses() {
    if (this.parent.nestedTypes.isEmpty()) {
      return Collections.emptyList();
    }
    final var nestedClasses = new ArrayList<JavaClass>();
    // FIXME: Should these be sorted in a particular way at this point?
    for (final var nestedType : this.parent.nestedTypes.values()) {
      // Assumption: the version number must match exactly.
      final var nestedClass = nestedType.classes.get(this.version);
      if (nestedClass != null) {
        nestedClasses.add(nestedClass);
      }
    }
    return nestedClasses;
  }

  /**
   * Determines the runtime version corresponding to this class' class file version.
   *
   * @return The runtime version corresponding to this class' class file version.
   */
  @NotNull
  public String runtimeVersion() {
    return Util.runtimeVersion(this.version);
  }

}
