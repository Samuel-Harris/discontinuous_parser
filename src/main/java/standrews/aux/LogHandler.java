/*
 * Copyright (c) 2018. Lorem ipsum dolor sit amet, consectetur adipiscing elit.
 * Morbi non lorem porttitor neque feugiat blandit. Ut vitae ipsum eget quam lacinia accumsan.
 * Etiam sed turpis ac ipsum condimentum fringilla. Maecenas magna.
 * Proin dapibus sapien vel ante. Aliquam erat volutpat. Pellentesque sagittis ligula eget metus.
 * Vestibulum commodo. Ut rhoncus gravida arcu.
 */

package standrews.aux;

import java.util.logging.ConsoleHandler;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;

/**
 * Custom log handler.
 */
final public class LogHandler extends ConsoleHandler {
    /**
     * Construct a custom log handler.
     */
    public LogHandler() {
        super();
        setLevel(Level.ALL);
        setFormatter(makeFormatter());
    }

    /**
     * Make the formatter that is used by the custom handler.
     *
     * @return A logging formatter.
     */
    private Formatter makeFormatter() {
        return new Formatter() {
            public String format(final LogRecord record) {
                final String className = loggerClassName(record);
                final Throwable error = record.getThrown();
                return (error != null && error.getMessage() != null)
                        ? String.format("[%s] %s (threw: '%s')%n", className, record.getMessage(), error.getMessage())
                        : String.format("[%s] %s%n", className, record.getMessage());
            }
        };
    }

    /**
     * Get the name of the class (stripped of package information)
     * that owns the logger that produced the record.
     *
     * @param record The record produced by the logger.
     * @return The name (stripped of package information) of the class.
     */
    private String loggerClassName(final LogRecord record) {
        final String[] parts = record.getSourceClassName().split("\\.");
        return parts[parts.length - 1];
    }
}
