package com.github.zastai.apiref.formatters;

import com.github.zastai.apiref.internal.ASMUtil;
import com.github.zastai.apiref.internal.Constants;
import com.github.zastai.apiref.internal.WellKnown;
import com.github.zastai.apiref.model.JavaApplication;
import com.github.zastai.apiref.model.JavaClass;
import com.github.zastai.apiref.model.JavaModule;
import com.github.zastai.apiref.model.JavaPackage;
import com.github.zastai.apiref.signatures.ClassSignature;
import com.github.zastai.apiref.signatures.FieldSignature;
import com.github.zastai.apiref.signatures.FormalTypeParameter;
import com.github.zastai.apiref.signatures.MethodSignature;
import com.github.zastai.apiref.signatures.TypeReference;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

/** A class for formatting a Java application's (public) API as Java pseudocode. */
public class JavaFormatter extends CodeFormatter {

  /**
   * Creates a new Java formatter.
   *
   * @param out The stream that should receive the formatted output.
   */
  protected JavaFormatter(@NotNull PrintStream out) {
    super(out);
  }

  /**
   * Formats the public API for a Java application as Java pseudocode and writes it to the specified stream.
   *
   * @param out         The stream to write the public API to.
   * @param application The application whose public API should be formatted.
   */
  public static void formatPublicApi(@NotNull PrintStream out, @NotNull JavaApplication application) {
    final var formatter = new JavaFormatter(out);
    formatter.writePublicApi(application);
  }

  private void maybeWriteParameterName(@NotNull MethodNode mn, int i) {
    if (mn.localVariables == null) {
      return;
    }
    final var parameterStart = (mn.access & Opcodes.ACC_STATIC) != 0 ? 0 : 1;
    if (mn.localVariables.size() < parameterStart + i) {
      return;
    }
    final var parameterVariable = mn.localVariables.get(parameterStart + i);
    this.out.print(' ');
    this.out.print(parameterVariable.name);
  }

  @Override
  protected void writeAnnotation(@NotNull AnnotationNode an) {
    this.out.print('@');
    this.writeTypeName(an.desc);
    if (an.values != null) {
      this.out.print('(');
      this.writeAnnotationValues(an.values);
      this.out.print(')');
    }
  }

  private void writeAnnotationValues(@NotNull List<Object> annotationValues) {
    final var count = annotationValues.size() / 2;
    for (var i = 0; i < count; ++i) {
      if (i > 0) {
        this.out.print(", ");
      }
      final var name = annotationValues.get(2 * i);
      final var values = annotationValues.get(2 * i + 1);
      if (count != 1 || !"value".equals(name)) {
        this.out.print(name);
        this.out.print(" = ");
      }
      if (values instanceof ArrayList<?> valueList) {
        if (valueList.size() == 1) {
          this.writeAnnotationValue(valueList.get(0));
        }
        else if (valueList.isEmpty()) {
          this.out.print("{}");
        }
        else {
          // TODO: Emit an array of annotations as multi-line, but then we'd want to be able to indent more than one level:
          //         @Foo(bar = {
          //                @Bar(...),
          //                @Bar(...),
          //              }, ...)
          //       or
          //         @Foo({
          //           @Bar(...),
          //           @Bar(...),
          //         })
          this.out.print("{ ");
          var first = true;
          for (final var value : valueList) {
            if (first) {
              first = false;
            }
            else {
              this.out.print(", ");
            }
            this.writeAnnotationValue(value);
          }
          this.out.print(" }");
        }
      }
      else {
        // not sure if this is possible - ASM seems to report an ArrayList even for scalar values
        this.writeAnnotationValue(values);
      }
    }
    if (annotationValues.size() % 2 != 0) {
      this.out.printf(" /* excess argument: '%s' */", annotationValues.get(annotationValues.size() - 1));
    }
  }

  private void writeClassDeclarator(@NotNull ClassNode cn) {
    var type = "class";
    var access = cn.access;
    //    0x00000001 = ACC_PUBLIC
    //    0x00000002 = ACC_PRIVATE
    //    0x00000004 = ACC_PROTECTED
    //    0x00000008 = ACC_STATIC
    //    0x00000010 = ACC_FINAL
    //    0x00000020 = ACC_SUPER / ACC_SYNCHRONIZED / ACC_OPEN / ACC_TRANSITIVE
    //    0x00000040 = ACC_VOLATILE / ACC_BRIDGE / ACC_STATIC_PHASE
    //    0x00000080 = ACC_VARARGS / ACC_TRANSIENT
    //    0x00000100 = ACC_NATIVE
    //    0x00000200 = ACC_INTERFACE
    //    0x00000400 = ACC_ABSTRACT
    //    0x00000800 = ACC_STRICT
    //    0x00001000 = ACC_SYNTHETIC
    //    0x00002000 = ACC_ANNOTATION
    //    0x00004000 = ACC_ENUM
    //    0x00008000 = ACC_MANDATED / ACC_MODULE
    //    0x00010000 = ACC_RECORD
    //    0x00020000 = ACC_DEPRECATED
    // If this is a nested class, the ACC_STATIC is not found on the class node itself.
    if (cn.nestHostClass != null && cn.innerClasses != null && !cn.innerClasses.isEmpty()) {
      final var innerClass = cn.innerClasses.get(0);
      if (innerClass != null && innerClass.name.equals(cn.name)) {
        final var innerAccess = innerClass.access;
        if ((innerAccess & Opcodes.ACC_STATIC) != 0) {
          access |= Opcodes.ACC_STATIC;
          // FIXME: Should this look at other potential mismatches between the inner access value and the class-level one?
          // FIXME: Or should it switch to using the inner access value entirely?
        }
      }
    }
    // First mask off some that are not written as part of the access modifiers
    if ((access & Opcodes.ACC_INTERFACE) != 0) {
      if ((access & Opcodes.ACC_ANNOTATION) != 0) {
        type = "@interface";
        access &= ~Opcodes.ACC_ANNOTATION;
      }
      else {
        type = "interface";
      }
      access &= ~Opcodes.ACC_INTERFACE;
      access &= ~Opcodes.ACC_ABSTRACT; // implied
      access &= ~Opcodes.ACC_STATIC; // implied
    }
    else if ((access & Opcodes.ACC_ENUM) != 0) {
      access &= ~Opcodes.ACC_ENUM;
      access &= ~Opcodes.ACC_FINAL; // implied
      type = "enum";
    }
    else if ((access & Opcodes.ACC_RECORD) != 0) {
      access &= ~Opcodes.ACC_RECORD;
      access &= ~Opcodes.ACC_FINAL; // implied
      type = "record";
    }
    // This will probably be set on every single type other than Object itself
    access &= ~Opcodes.ACC_SUPER;
    // Now check for access modifiers that apply to a class
    if ((access & Opcodes.ACC_PUBLIC) != 0) {
      this.out.print("public ");
      access &= ~Opcodes.ACC_PUBLIC;
    }
    if ((access & Opcodes.ACC_PRIVATE) != 0) {
      this.out.print("private ");
      access &= ~Opcodes.ACC_PRIVATE;
    }
    if ((access & Opcodes.ACC_PROTECTED) != 0) {
      this.out.print("protected ");
      access &= ~Opcodes.ACC_PROTECTED;
    }
    // The deprecated status is assumed to be marked via @Deprecated, which is emitted separately. Alternatively, we could also
    // adjust the annotation handling to not emit @Deprecated, and instead emit a fake 'deprecated' keyword here.
    access &= ~Opcodes.ACC_DEPRECATED;
    if ((access & Opcodes.ACC_STATIC) != 0) {
      this.out.print("static ");
      access &= ~Opcodes.ACC_STATIC;
    }
    if ((access & Opcodes.ACC_FINAL) != 0) {
      this.out.print("final ");
      access &= ~Opcodes.ACC_FINAL;
    }
    if ((access & Opcodes.ACC_ABSTRACT) != 0) {
      this.out.print("abstract ");
      access &= ~Opcodes.ACC_ABSTRACT;
    }
    if (access != 0) {
      this.out.printf("/* TODO: access flags 0x%08X */ ", access);
    }
    this.out.print(type);
    this.out.print(' ');
  }

  @Override
  protected void writeClassFooter(@NotNull JavaClass jc) {
    this.out.println();
    this.outdent();
    this.writeIndent();
    this.out.print('}');
    this.out.println();
  }

  @Override
  protected void writeClassHeader(@NotNull JavaClass jc) {
    if (jc.parent.classes.size() > 1) {
      // This wording may need to change if a future JDK drops support for older class file formats.
      this.writeLineComment("This version of the class is for use by %s (or later).".formatted(jc.runtimeVersion()));
    }
    final var cn = jc.contents;
    this.writeAnnotations(cn);
    this.writeIndent();
    this.writeClassDeclarator(cn);
    this.out.printf(jc.name);
    if (cn.signature != null) {
      final var signature = ClassSignature.decode(cn.signature);
      this.writeTypeParameters(signature.typeParameters);
      {
        var baseClass = signature.baseClass;
        if (baseClass != null && !baseClass.isTypeVariable) {
          if ((cn.access & Opcodes.ACC_ENUM) != 0 && WellKnown.Descriptors.ENUM.equals(baseClass.name)) {
            baseClass = null;
          }
          else if ((cn.access & Opcodes.ACC_RECORD) != 0 && WellKnown.Descriptors.RECORD.equals(baseClass.name)) {
            baseClass = null;
          }
          else if (WellKnown.Descriptors.OBJECT.equals(baseClass.name)) {
            baseClass = null;
          }
        }
        if (baseClass != null) {
          this.out.print(" extends ");
          this.writeTypeName(baseClass);
        }
      }
      if (signature.baseInterfaces != null) {
        String separator = " implements ";
        for (final var type : signature.baseInterfaces) {
          if (!type.isTypeVariable) {
            if ((cn.access & Opcodes.ACC_ANNOTATION) != 0 && WellKnown.Descriptors.ANNOTATION.equals(type.name)) {
              continue;
            }
          }
          this.out.print(separator);
          this.writeTypeName(type);
          separator = ", ";
        }
      }
    }
    else {
      var superName = cn.superName;
      if ((cn.access & Opcodes.ACC_ENUM) != 0 && WellKnown.Names.ENUM.equals(superName)) {
        superName = null;
      }
      else if ((cn.access & Opcodes.ACC_RECORD) != 0 && WellKnown.Names.RECORD.equals(superName)) {
        superName = null;
      }
      else if (WellKnown.Names.OBJECT.equals(superName)) {
        superName = null;
      }
      if (superName != null) {
        this.out.print(" extends ");
        this.writeTypeName(ASMUtil.descriptorForName(superName));
      }
      if (cn.interfaces != null) {
        String separator = " implements ";
        for (final var name : cn.interfaces) {
          if ((cn.access & Opcodes.ACC_ANNOTATION) != 0 && WellKnown.Names.ANNOTATION.equals(name)) {
            continue;
          }
          this.out.print(separator);
          this.writeTypeName(ASMUtil.descriptorForName(name));
          separator = ", ";
        }
      }
    }
    this.out.println(" {");
    this.indent();
  }

  @Override
  protected void writeClassName(@NotNull Type type) {
    this.writeTypeName(type);
    this.out.print(".class");
  }

  @Override
  protected void writeEndOfEnumValues() {
    this.out.println();
    this.writeIndent();
    this.out.println(';');
  }

  @Override
  protected void writeEnumValue(@NotNull String descriptor, @NotNull String member) {
    this.writeTypeName(descriptor);
    this.out.print('.');
    this.out.print(member);
  }

  @Override
  protected void writeField(@NotNull FieldNode fn) {
    this.out.println();
    this.writeAnnotations(fn);
    this.writeIndent();
    if (fn.access == Constants.ACC_ENUM_VALUE) {
      this.out.print(fn.name);
      this.out.println(",");
      return;
    }
    this.writeMemberAccess(fn.access);
    if (fn.signature != null) {
      final var signature = FieldSignature.decode(fn.signature);
      this.writeTypeName(signature.type);
    }
    else {
      this.writeTypeName(fn.desc);
    }
    this.out.print(' ');
    this.out.print(fn.name);
    this.out.println(";");
  }

  @Override
  protected void writeLiteral(@NotNull Boolean literal) {
    this.out.print(literal);
  }

  @Override
  protected void writeLiteral(@NotNull Byte literal) {
    // FIXME: Should this use an explicit '(byte) ' as prefix?
    this.out.print(literal);
  }

  @Override
  protected void writeLiteral(@NotNull Character literal) {
    this.out.print('\'');
    this.out.print(literal == '\'' ? "\\'" : literal.toString());
    this.out.print('\'');
  }

  @Override
  protected void writeLiteral(@NotNull Double literal) {
    this.out.print(literal);
  }

  @Override
  protected void writeLiteral(@NotNull Float literal) {
    this.out.print(literal);
    this.out.print('F');
  }

  @Override
  protected void writeLiteral(@NotNull Integer literal) {
    this.out.print(literal);
  }

  @Override
  protected void writeLiteral(@NotNull Long literal) {
    this.out.print(literal);
    this.out.print('L');
  }

  @Override
  protected void writeLiteral(@NotNull Short literal) {
    // FIXME: Should this use an explicit '(short) ' as prefix?
    this.out.print(literal);
  }

  @Override
  protected void writeLiteral(@NotNull String literal) {
    this.out.printf("\"%s\"", literal.replace("\"", "\\\""));
  }

  private void writeMemberAccess(int access) {
    //    0x00000001 = ACC_PUBLIC
    //    0x00000002 = ACC_PRIVATE
    //    0x00000004 = ACC_PROTECTED
    //    0x00000008 = ACC_STATIC
    //    0x00000010 = ACC_FINAL
    //    0x00000020 = ACC_SUPER / ACC_SYNCHRONIZED / ACC_OPEN / ACC_TRANSITIVE
    //    0x00000040 = ACC_VOLATILE / ACC_BRIDGE / ACC_STATIC_PHASE
    //    0x00000080 = ACC_VARARGS / ACC_TRANSIENT
    //    0x00000100 = ACC_NATIVE
    //    0x00000200 = ACC_INTERFACE
    //    0x00000400 = ACC_ABSTRACT
    //    0x00000800 = ACC_STRICT
    //    0x00001000 = ACC_SYNTHETIC
    //    0x00002000 = ACC_ANNOTATION
    //    0x00004000 = ACC_ENUM
    //    0x00008000 = ACC_MANDATED / ACC_MODULE
    //    0x00010000 = ACC_RECORD
    //    0x00020000 = ACC_DEPRECATED
    if ((access & Opcodes.ACC_PUBLIC) != 0) {
      this.out.print("public ");
      access &= ~Opcodes.ACC_PUBLIC;
    }
    if ((access & Opcodes.ACC_PRIVATE) != 0) {
      this.out.print("private ");
      access &= ~Opcodes.ACC_PRIVATE;
    }
    if ((access & Opcodes.ACC_PROTECTED) != 0) {
      this.out.print("protected ");
      access &= ~Opcodes.ACC_PROTECTED;
    }
    // For members (constructors/fields/methods), the deprecated status is assumed to be marked via @Deprecated, which is emitted
    // separately. Alternatively, we could also adjust the annotation handling to not emit @Deprecated, and instead emit a fake
    // 'deprecated' keyword here.
    access &= ~Opcodes.ACC_DEPRECATED;
    if ((access & Opcodes.ACC_STATIC) != 0) {
      this.out.print("static ");
      access &= ~Opcodes.ACC_STATIC;
    }
    if ((access & Opcodes.ACC_FINAL) != 0) {
      this.out.print("final ");
      access &= ~Opcodes.ACC_FINAL;
    }
    if ((access & Opcodes.ACC_ABSTRACT) != 0) {
      this.out.print("abstract ");
      access &= ~Opcodes.ACC_ABSTRACT;
    }
    if (access != 0) {
      this.out.printf("/* TODO: access flags 0x%08X */ ", access);
    }
  }

  @Override
  protected void writeMethod(@NotNull MethodNode mn) {
    this.out.println();
    this.writeAnnotations(mn);
    this.writeIndent();
    final boolean varargs;
    {
      int access = mn.access;
      varargs = (mn.access & Opcodes.ACC_VARARGS) != 0;
      access &= ~Opcodes.ACC_VARARGS;
      this.writeMemberAccess(access);
    }
    if (mn.signature != null) {
      final var signature = MethodSignature.decode(mn.signature);
      if (signature.typeParameters != null) {
        this.writeTypeParameters(signature.typeParameters);
        this.out.print(' ');
      }
      // FIXME: For constructors, should we verify that the return type is declared as 'void'? Can it have (relevant) annotations?
      if (!WellKnown.Names.CONSTRUCTOR.equals(mn.name)) {
        // TODO: Handle annotations on the return type.
        this.writeTypeName(signature.returnType);
        this.out.print(' ');
      }
      this.writeMethodName(mn.name);
      this.out.print('(');
      if (signature.parameterTypes != null) {
        for (var i = 0; i < signature.parameterTypes.length; ++i) {
          if (i > 0) {
            this.out.print(", ");
          }
          // TODO: Handle annotations on the parameters.
          this.writeTypeName(signature.parameterTypes[i], varargs && i == signature.parameterTypes.length - 1);
          this.maybeWriteParameterName(mn, i);
        }
      }
      this.out.print(')');
      if (signature.thrownTypes != null) {
        this.out.print(" throws ");
        for (var i = 0; i < signature.thrownTypes.length; ++i) {
          if (i > 0) {
            this.out.print(", ");
          }
          this.writeTypeName(signature.thrownTypes[i]);
        }
      }
    }
    else {
      // FIXME: For constructors, should we verify that the return type is declared as 'void'? Can it have (relevant) annotations?
      if (!WellKnown.Names.CONSTRUCTOR.equals(mn.name)) {
        final var returnType = Type.getReturnType(mn.desc);
        if (returnType != null) {
          // TODO: Handle annotations on the return type.
          this.writeTypeName(returnType);
        }
        else {
          throw new IllegalStateException("Method descriptor did not include a return type.");
        }
        this.out.print(' ');
      }
      this.writeMethodName(mn.name);
      this.out.print('(');
      final var parameterTypes = Type.getArgumentTypes(mn.desc);
      if (parameterTypes.length > 0) {
        // TODO: Handle annotations on the parameters.
        for (var i = 0; i < parameterTypes.length; ++i) {
          if (i > 0) {
            this.out.print(", ");
          }
          this.writeTypeName(parameterTypes[i], varargs && i == parameterTypes.length - 1);
          this.maybeWriteParameterName(mn, i);
        }
      }
      this.out.print(')');
      if (mn.exceptions != null) {
        String separator = " throws ";
        for (final var exception : mn.exceptions) {
          this.out.print(separator);
          this.writeTypeName(ASMUtil.descriptorForName(exception));
          separator = ", ";
        }
      }
    }
    this.out.println(";");
  }

  private void writeMethodName(@NotNull String name) {
    if (WellKnown.Names.CONSTRUCTOR.equals(name) && this.currentClass != null) {
      this.out.print(this.currentClass.name);
    }
    else {
      this.out.print(name);
    }
  }

  @Override
  protected void writeModuleContents(@NotNull JavaModule jm) {
    this.writeIndent();
    this.out.println("TODO");
  }

  @Override
  protected void writePackageFooter(@NotNull JavaPackage jp) {
    this.out.println();
    this.outdent();
    this.writeIndent();
    this.out.print('}');
    this.out.println();
  }

  @Override
  protected void writePackageHeader(@NotNull JavaPackage jp) {
    if (jp.info != null) {
      this.writeAnnotations(jp.info);
    }
    this.writeIndent();
    // The access flags for a package do not matter - they are always 0x1600 (synthetic abstract interface); they don't even get the
    // 'deprecated' bit set when they have @Deprecated on them.
    this.out.print("package ");
    this.writePackageName(jp);
    this.out.print(" {");
    this.out.println();
    this.indent();
  }

  /**
   * Writes out the name of a Java package.
   *
   * @param jp The Java package.
   */
  protected void writePackageName(@NotNull JavaPackage jp) {
    this.out.print(jp.name.replace('/', '.'));
  }

  @Override
  protected void writeTypeName(@NotNull Type type) {
    this.writeTypeName(type, false);
  }

  private void writeTypeName(@NotNull Type type, boolean varargs) {
    String name = type.getClassName();
    if (this.currentPackage != null) {
      final var prefix = this.currentPackage.name.replace('/', '.');
      final int prefixLength = prefix.length();
      if (name.length() > prefixLength + 2 && name.startsWith(prefix) && name.charAt(prefixLength) == '.') {
        name = name.substring(prefixLength + 1);
      }
    }
    if (varargs && name.endsWith("[]")) {
      name = name.substring(0, name.length() - 2) + "...";
    }
    this.out.print(name);
  }

  private void writeTypeArgument(@Nullable TypeReference type) {
    if (type == null) {
      this.out.print('?');
      return;
    }
    if (type.wildcard != null) {
      final char wildcard = type.wildcard;
      this.out.print(switch (wildcard) {
        case '-' -> "? super ";
        case '+' -> "? extends ";
        default -> throw new IllegalArgumentException("Unsupported type argument wildcard (%c) in signature.".formatted(wildcard));
      });
    }
    this.writeTypeName(type);
  }

  private void writeTypeArguments(TypeReference @NotNull ... types) {
    this.out.print('<');
    var first = true;
    for (final var type : types) {
      if (first) {
        first = false;
      }
      else {
        this.out.print(", ");
      }
      this.writeTypeArgument(type);
    }
    this.out.print('>');
  }

  private void writeTypeName(@NotNull TypeReference type) {
    this.writeTypeName(type, false);
  }

  private void writeTypeName(@NotNull TypeReference type, boolean varargs) {
    if (type.qualifier != null) {
      this.writeTypeName(type.qualifier);
      this.out.print('.');
    }
    if (type.isTypeVariable || type.qualifier != null) {
      this.out.print(type.name);
    }
    else {
      this.writeTypeName(type.name);
    }
    if (type.typeArguments != null) {
      this.writeTypeArguments(type.typeArguments);
    }
    for (var i = 0; i < type.arrayDimensions; ++i) {
      this.out.print(varargs && i == type.arrayDimensions - 1 ? "..." : "[]");
    }
  }

  private void writeTypeParameters(@NotNull FormalTypeParameter @Nullable [] typeParameters) {
    if (typeParameters == null) {
      return;
    }
    this.out.print('<');
    var first = true;
    for (final var typeParameter : typeParameters) {
      if (first) {
        first = false;
      }
      else {
        this.out.print(", ");
      }
      this.out.print(typeParameter.name);
      String separator = " extends ";
      {
        var bound = typeParameter.classBound;
        if (bound != null && !bound.isTypeVariable && WellKnown.Descriptors.OBJECT.equals(bound.name)) {
          bound = null;
        }
        if (bound != null) {
          this.out.print(separator);
          this.writeTypeName(bound);
          separator = " & ";
        }
      }
      if (typeParameter.interfaceBounds != null) {
        for (final var bound : typeParameter.interfaceBounds) {
          this.out.print(separator);
          this.writeTypeName(bound);
          separator = " & ";
        }
      }
    }
    this.out.print('>');
  }

}
