package com.github.zastai.apiref.formatters;

import com.github.zastai.apiref.model.JavaApplication;
import org.jetbrains.annotations.NotNull;

import java.io.PrintStream;

/** A class for formatting a Java application's (public) API as Java pseudocode. */
public class JavaFormatter extends CodeFormatter {

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

}
