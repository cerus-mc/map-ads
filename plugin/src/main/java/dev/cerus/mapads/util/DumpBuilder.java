package dev.cerus.mapads.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

public class DumpBuilder {

    private final File outputFile;
    private final List<String> lines;

    private DumpBuilder(final File outputFile) {
        this.outputFile = outputFile;
        this.lines = new ArrayList<>();
    }

    public static DumpBuilder create(final File outputFile) {
        return new DumpBuilder(outputFile);
    }

    public DumpBuilder outlinedText(final String... lines) {
        final int maxLen = Arrays.stream(lines).mapToInt(String::length).max().orElse(0);
        this.line("#".repeat(maxLen + 4));
        for (final String line : lines) {
            final int space = maxLen - line.length();
            this.line("# " + line + " ".repeat(space) + " #");
        }
        this.line("#".repeat(maxLen + 4));
        return this;
    }

    @SafeVarargs
    public final DumpBuilder table(String[] header, Supplier<String[]>... columnSuppliers) {
        if (header.length != columnSuppliers.length) {
            throw new IllegalArgumentException("Number of columns does not match the number of columns provided.");
        }

        String[][] matrix = new String[columnSuppliers.length][];
        int rowNum = 0;
        for (int i = 0; i < columnSuppliers.length; i++) {
            matrix[i] = columnSuppliers[i].get();
            rowNum = matrix[i].length;
        }

        List<String[]> rows = new ArrayList<>();
        rows.add(header);
        outer:
        for (int r = 0; r < rowNum; r++) {
            String[] row = new String[header.length];
            for (int i = 0; i < header.length; i++) {
                if (matrix[i].length == 0) {
                    continue outer;
                }
                row[i] = matrix[i][r];
            }
            rows.add(row);
        }

        return table(rows);
    }

    public DumpBuilder table(final List<String[]> rows) {
        if (rows.isEmpty()) {
            return this;
        }
        if (rows.stream().mapToInt(arr -> arr.length).distinct().count() > 1) {
            throw new IllegalArgumentException("Rows must have the same amount of columns");
        }

        final int columns = rows.get(0).length;
        if (columns < 1) {
            return this;
        }

        final int[] colLens = new int[columns];
        for (final String[] row : rows) {
            for (int i = 0; i < row.length; i++) {
                colLens[i] = Math.max(colLens[i], row[i].length());
            }
        }

        for (int r = 0; r < rows.size(); r++) {
            final String[] row = rows.get(r);

            StringBuilder lineBuilder = new StringBuilder();
            for (int c = 0; c < row.length; c++) {
                final boolean last = c == row.length - 1;
                lineBuilder.append("+-").append("-".repeat(colLens[c])).append("-");
                if (last) {
                    lineBuilder.append("+");
                }
            }
            this.line(lineBuilder.toString());

            lineBuilder = new StringBuilder();
            for (int c = 0; c < row.length; c++) {
                final boolean last = c == row.length - 1;
                final String col = row[c];
                lineBuilder.append("| ").append(col).append(" ".repeat(colLens[c] - col.length())).append(" ");
                if (last) {
                    lineBuilder.append("|");
                }
            }
            this.line(lineBuilder.toString());

            final boolean last = r == rows.size() - 1;
            if (last) {
                lineBuilder = new StringBuilder();
                for (int c = 0; c < row.length; c++) {
                    final boolean l = c == row.length - 1;
                    lineBuilder.append("+-").append("-".repeat(colLens[c])).append("-");
                    if (l) {
                        lineBuilder.append("+");
                    }
                }
                this.line(lineBuilder.toString());
            }
        }
        return this;
    }

    public DumpBuilder line(final String line, final Object... params) {
        this.lines.add(line.formatted(params));
        return this;
    }

    public DumpBuilder line(final String line) {
        this.lines.add(line);
        return this;
    }

    public DumpBuilder blank() {
        this.lines.add("");
        return this;
    }

    public void write() {
        try (final FileOutputStream out = new FileOutputStream(this.outputFile)) {
            for (final String line : this.lines) {
                out.write((line + "\n").getBytes(StandardCharsets.UTF_8));
            }
            out.flush();
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
    }

}
