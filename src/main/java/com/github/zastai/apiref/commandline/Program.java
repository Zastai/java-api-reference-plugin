package com.github.zastai.apiref.commandline;

import com.github.zastai.apiref.formatters.JavaFormatter;
import com.github.zastai.apiref.formatters.MarkDownFormatter;
import com.github.zastai.apiref.internal.ClassPath;
import com.github.zastai.apiref.internal.PathUtil;
import com.github.zastai.apiref.model.JavaApplication;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.function.BiConsumer;

/** A command-line tool for running Java API extraction. */
public final class Program {

  private static void info(@NotNull String message) {
    System.out.println(message);
  }

  private static void info(@NotNull String message, Object... args) {
    System.out.printf(message, args);
  }

  private static int fail(int rc, @NotNull String message, Object... args) {
    System.err.printf(message, args);
    return rc;
  }

  /**
   * Runs the command-line tool for Java API extraction.
   *
   * @param args The command-line arguments.
   */
  public static void main(String... args) {
    System.exit(Program.run(args));
  }

  private static int run(String... args) {
    if (args == null) {
      return Program.usage(1);
    }
    BiConsumer<PrintStream, JavaApplication> format = JavaFormatter::formatPublicApi;
    boolean verbose = false;
    var idx = 0;
    for (; idx < args.length; ++idx) {
      final String arg = args[idx];
      if (!arg.startsWith("--")) {
        break;
      }
      final String option;
      final String value;
      {
        final int equals = arg.indexOf('=');
        if (equals < 0) {
          option = arg.substring(2);
          value = null;
        }
        else {
          option = arg.substring(2, equals);
          value = arg.substring(equals + 1);
        }
      }
      if ("format".equals(option)) {
        if (value == null || value.isBlank()) {
          return Program.fail(4, "No output format specified (should be 'java' or 'markdown').%n");
        }
        switch (value.toLowerCase(Locale.ROOT)) {
          case "java" -> format = JavaFormatter::formatPublicApi;
          case "markdown" -> format = MarkDownFormatter::formatPublicApi;
          default -> {
            return Program.fail(4, "Unsupported output format '%s' specified (should be 'java' or 'markdown').%n", value);
          }
        }
      }
      else if ("verbose".equals(option) && value == null) {
        verbose = true;
      }
      else if ("help".equals(option) && value == null) {
        return Program.usage(0);
      }
      else {
        return Program.fail(4, "Unsupported option: %s%n", arg);
      }
    }
    if (args.length - idx < 2) {
      return Program.usage(1);
    }
    final JavaApplication application;
    try (final ClassPath classPath = new ClassPath()) {
      // TODO: configure the classpath, especially for things like annotations that mark something as not being part of public API.
      classPath.setVerbose(verbose);
      while (idx + 1 < args.length) {
        try {
          final var jarOrFolder = Path.of(args[idx]).toAbsolutePath().normalize();
          if (PathUtil.isJarFile(jarOrFolder) || PathUtil.isDirectory(jarOrFolder)) {
            classPath.add(jarOrFolder);
          }
          else {
            return Program.fail(2, "Input is neither a folder nor a jar file: %s%n", jarOrFolder);
          }
        }
        catch (IOException e) {
          return Program.fail(2, "Failed to locate class files in %s: %s%n", args[idx], e);
        }
        ++idx;
      }
      application = classPath.buildApplication();
    }
    final Path referencePath;
    if ("-".equals(args[idx])) {
      referencePath = null;
    }
    else {
      referencePath = Path.of(args[idx]).toAbsolutePath();
      final var outputDir = referencePath.getParent();
      if (!Files.isDirectory(outputDir)) {
        return Program.fail(3, "Output folder does not exist: %s%n", outputDir);
      }
    }
    try (final PrintStream reference = Program.openReferenceFile(referencePath)) {
      format.accept(reference, application);
    }
    catch (IOException e) {
      return Program.fail(16, "Failed to generate reference code: %s%n", e);
    }
    return 0;
  }

  @NotNull
  private static PrintStream openReferenceFile(@Nullable Path referencePath) throws IOException {
    if (referencePath == null) {
      return System.out;
    }
    return new PrintStream(Files.newOutputStream(referencePath), false, StandardCharsets.UTF_8);
  }

  private static int usage(int rc) {
    System.out.printf("Usage: java -jar %s.jar [OPTIONS] JAR-OR-FOLDER... OUTPUT-FILE%n", Program.class.getPackageName());
    System.out.println();
    System.out.println("Options:");
    System.out.println("  --format=FORMAT             Specify the output format (java or markdown)");
    return rc;
  }

}
