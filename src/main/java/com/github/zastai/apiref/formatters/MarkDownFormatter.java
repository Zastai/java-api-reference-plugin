package com.github.zastai.apiref.formatters;

import com.github.zastai.apiref.model.JavaApplication;
import org.jetbrains.annotations.NotNull;

import java.io.PrintStream;

/** A class for formatting a Java application's (public) API as MarkDown (with Java pseudocode blocks). */
public class MarkDownFormatter extends JavaFormatter {

  protected MarkDownFormatter(@NotNull PrintStream out) {
    super(out);
  }

  /**
   * Formats the public API for a Java application as MarkDown (with Java pseudocode blocks) and writes it to the specified stream.
   *
   * @param out         The stream to write the public API to.
   * @param application The application whose public API should be formatted.
   */
  public static void formatPublicApi(@NotNull PrintStream out, @NotNull JavaApplication application) {
    final var formatter = new MarkDownFormatter(out);
    formatter.writePublicApi(application);
  }

}
