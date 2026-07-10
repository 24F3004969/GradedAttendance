package org.graded_classes.graded_attendance.controller.leaderboard;

import java.util.Arrays;

public class Name {
    public static String make_word_name(String word) {

        return countWords(word) > 2 ? word.substring(0, word.lastIndexOf(' ')) : word;
    }

    public static long countWords(String string) {
        return Arrays.stream(string.split("[\\s.,\n\\d]")).
                filter(x -> !x.isEmpty()).count();
    }
}
