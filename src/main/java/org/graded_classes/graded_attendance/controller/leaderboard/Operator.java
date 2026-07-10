package org.graded_classes.graded_attendance.controller.leaderboard;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

class Operators {
    private final String k1 = new String(Character.toChars(195));
    private final String k2 = new String(Character.toChars(196));
    private final String k3 = new String(Character.toChars(197));
    private final String k4 = new String(Character.toChars(198));
    private final String k5 = new String(Character.toChars(199));
    private final List<String> memory;

    public Operators(String exp) {
        String value = substitute(exp);
        memory = new ArrayList<>();
        Collections.addAll(memory, value.split("\\+"));
        if (Character.toString(exp.charAt(0)).equals("-")) {
            memory.set(0, "-" + memory.getFirst());
        }
    }

    private String substitute(String va) {
        String val = (Character.toString(va.charAt(0)).equals("-")) ? va.substring(1) : va;
        val = val
                .replaceAll("/-", k1)
                .replaceAll("\\*-", k2)
                .replaceAll("\\^-", k3)
                .replaceAll("E\\+", k4)
                .replaceAll("E-", k5)
                .replaceAll("-", "+-");
        return val;
    }

    private String resubstitute(String va) {
        return va
                .replaceAll(k1, "/-")
                .replaceAll(k2, "*-")
                .replaceAll(k3, "^-")
                .replaceAll(k4, "E+")
                .replaceAll(k5, "E-");
    }

    public double solve() {
        double finalValue = 0.0;
        for (String x : memory) {
            x = resubstitute(x);
            if (x.contains("*")) {
                x = multiply(x.split("\\*"));
            } else if (x.contains("/")) {
                x = divide(x.split("/"));
            } else if (x.contains("^")) {
                x = power(x.split("\\^"));
            }
            x = (x.contains("!")) ? factorial(x) : x;
            finalValue += Double.parseDouble(x);
        }
        return finalValue;
    }

    private String power(String[] split) {
        double finalValue = split[0].contains("!") ? Double.parseDouble(factorial(split[0])) : Double.parseDouble(split[0]);
        for (int i = 1; i < split.length; i++) {
            String x = split[i];
            x = (x.contains("!")) ? factorial(x) : x;
            finalValue = Math.pow(finalValue, Double.parseDouble(x));
        }
        return String.valueOf(finalValue);
    }

    private String divide(String[] split) {
        double finalValue = 1.0;
        for (int i = 0; i < split.length; i++) {
            String x = split[i];
            if (x.contains("^")) {
                x = power(x.split("\\^"));
            }
            x = (x.contains("!")) ? factorial(x) : x;
            finalValue = (i == 0) ? Double.parseDouble(x) : finalValue / Double.parseDouble(x);
        }
        return String.valueOf(finalValue);
    }

    private String multiply(String[] split) {
        double finalValue = 1.0;
        for (String x : split) {
            if (x.contains("/")) {
                x = divide(x.split("/"));
            } else if (x.contains("^")) {
                x = power(x.split("\\^"));
            }
            x = (x.contains("!")) ? factorial(x) : x;
            finalValue *= Double.parseDouble(x);
        }
        return String.valueOf(finalValue);
    }

    private String factorial(String x) {
        while (x.contains("!")) {
            x = x.replace(x.substring(0, x.indexOf('!') + 1), fac(new BigInteger(x.substring(0, x.indexOf('!')))));
        }
        return x;
    }

    private String fac(BigInteger n) {
        return f(n).toString();
    }

    private BigInteger f(BigInteger n) {
        return (n.compareTo(BigInteger.ONE) > 0) ? n.multiply(f(n.subtract(BigInteger.ONE))) : n;
    }
}

