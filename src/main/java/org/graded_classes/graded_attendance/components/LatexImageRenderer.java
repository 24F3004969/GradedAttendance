package org.graded_classes.graded_attendance.components;

import org.scilab.forge.jlatexmath.TeXConstants;
import org.scilab.forge.jlatexmath.TeXFormula;
import org.scilab.forge.jlatexmath.TeXIcon;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

public final class LatexImageRenderer {

    public static final double RENDER_QUALITY = 6.0;

    private LatexImageRenderer() {
    }

    public static BufferedImage render(
            String value,
            float logicalSize,
            Color color,
            boolean whiteBackground
    ) {
        String formula =
                prepareFormula(value);

        TeXFormula texFormula =
                new TeXFormula(formula);

        TeXIcon icon =
                texFormula.createTeXIcon(
                        TeXConstants.STYLE_DISPLAY,
                        (float) (
                                logicalSize
                                        * RENDER_QUALITY
                        )
                );

        icon.setForeground(
                color == null
                        ? Color.BLACK
                        : color
        );

        int padding =
                Math.max(
                        3,
                        (int) Math.ceil(
                                RENDER_QUALITY
                        )
                );

        int width = Math.max(
                1,
                icon.getIconWidth() + padding * 2
        );

        int height = Math.max(
                1,
                icon.getIconHeight() + padding * 2
        );

        int imageType =
                whiteBackground
                        ? BufferedImage.TYPE_INT_RGB
                        : BufferedImage.TYPE_INT_ARGB;

        BufferedImage image =
                new BufferedImage(
                        width,
                        height,
                        imageType
                );

        Graphics2D graphics =
                image.createGraphics();

        try {
            if (whiteBackground) {
                graphics.setColor(Color.WHITE);
                graphics.fillRect(
                        0,
                        0,
                        width,
                        height
                );
            }

            graphics.setRenderingHint(
                    RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON
            );

            graphics.setRenderingHint(
                    RenderingHints.KEY_TEXT_ANTIALIASING,
                    RenderingHints.VALUE_TEXT_ANTIALIAS_ON
            );

            graphics.setRenderingHint(
                    RenderingHints.KEY_RENDERING,
                    RenderingHints.VALUE_RENDER_QUALITY
            );

            graphics.setRenderingHint(
                    RenderingHints.KEY_FRACTIONALMETRICS,
                    RenderingHints.VALUE_FRACTIONALMETRICS_ON
            );

            icon.paintIcon(
                    null,
                    graphics,
                    padding,
                    padding
            );

        } finally {
            graphics.dispose();
        }

        return image;
    }

    private static String prepareFormula(
            String value
    ) {
        if (value == null || value.isBlank()) {
            return "\\text{}";
        }

        String formula = value
                .replace('\r', ' ')
                .replace('\n', ' ')
                .replace('\t', ' ')
                .trim()
                .replaceAll("\\s+", " ");

        formula = removeDelimiters(formula);

        formula = formula.replaceAll(
                "\\\\operatorname\\s*\\{([^{}]*)}",
                "\\\\mathrm{$1}"
        );

        formula = formula.replaceAll(
                "\\\\cosec(?![A-Za-z])",
                "\\\\mathrm{cosec}"
        );

        if (!looksLatex(formula)) {
            return "\\text{"
                    + escapeText(formula)
                    + "}";
        }

        return formula;
    }

    private static boolean looksLatex(
            String value
    ) {
        return value.contains("\\")
                || value.contains("^")
                || value.contains("_")
                || value.contains("{")
                || value.contains("}");
    }

    private static String escapeText(
            String value
    ) {
        return value
                .replace("\\", "\\backslash ")
                .replace("{", "\\{")
                .replace("}", "\\}")
                .replace("%", "\\%")
                .replace("#", "\\#")
                .replace("&", "\\&")
                .replace("_", "\\_")
                .replace("$", "\\$");
    }

    private static String removeDelimiters(
            String value
    ) {
        if (value.startsWith("$$")
                && value.endsWith("$$")
                && value.length() >= 4) {

            return value.substring(
                    2,
                    value.length() - 2
            ).trim();
        }

        if (value.startsWith("$")
                && value.endsWith("$")
                && value.length() >= 2) {

            return value.substring(
                    1,
                    value.length() - 1
            ).trim();
        }

        if (value.startsWith("\\(")
                && value.endsWith("\\)")
                && value.length() >= 4) {

            return value.substring(
                    2,
                    value.length() - 2
            ).trim();
        }

        if (value.startsWith("\\[")
                && value.endsWith("\\]")
                && value.length() >= 4) {

            return value.substring(
                    2,
                    value.length() - 2
            ).trim();
        }

        return value;
    }
}