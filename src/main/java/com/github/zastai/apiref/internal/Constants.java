package com.github.zastai.apiref.internal;

import org.objectweb.asm.Opcodes;

/** Constants used by the implementation. */
public interface Constants {

  /** The access flags for an enum value. */
  int ACC_ENUM_VALUE = Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC | Opcodes.ACC_FINAL | Opcodes.ACC_ENUM;

  /** The access flags for externally visible items (public or protected). */
  int ACC_VISIBLE = Opcodes.ACC_PUBLIC | Opcodes.ACC_PROTECTED;

  /** The ASM API version to use. */
  int API_VERSION = Opcodes.ASM9;

  /** The name of the "module info" pseudo-class. */
  String MODULE_INFO = "module-info";

  /** The name of the "package info" pseudo-class. */
  String PACKAGE_INFO = "package-info";

}
