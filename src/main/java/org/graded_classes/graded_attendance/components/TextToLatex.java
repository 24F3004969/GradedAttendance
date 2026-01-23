package org.graded_classes.graded_attendance.components;

import java.util.Arrays;

public class TextToLatex {
    public static String toLatex(String text) {
        String[] lines = text.split("\n");
        System.out.println(Arrays.toString(lines));
        return "";
    }

    static void main() {
        TextToLatex.toLatex("""
                The total number of real solutions of the equation
                `\\theta \\;=\\; \\tan^{-1}\\!\\bigl(2\\tan\\theta\\bigr)\\;-\\;\\dfrac{1}{2}\\,\\sin^{-1}\\!\\left(\\dfrac{6\\tan\\theta}{\\,9+\\tan^{2}\\theta\\,}\\right)`
                (Here, the inverse trigonometric functions `` and `` assume values in `` and `` ,respectively.)
                """);
    }
}
