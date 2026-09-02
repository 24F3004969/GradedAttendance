package org.graded_classes.graded_attendance.components;

import org.graded_classes.graded_attendance.data.OptionData;
import org.graded_classes.graded_attendance.data.QuestionData;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class BasicParser {

    public static TreeMap<Integer, QuestionData> parse(String path, String topicId) {
        ArrayList<QuestionData> listQuestion = new ArrayList<>();
        try {
            // Read entire file
            String text = Files.readString(
                    Path.of(path)
            );
            var list = text.split("#");
            int no = 0;
            var correctOption = list[list.length - 1].replace("\r", "").replace("\n", " ").split(",");
            ;
            for (int i = 0; i < list.length - 1; i++) {
                var t = list[i];
                if (!t.isBlank()) {
                    int firstTilde = t.indexOf('~');
                    int lastTilde = t.lastIndexOf('~');

                    if (firstTilde == -1 || lastTilde == -1 || firstTilde == lastTilde) {
                        continue; // invalid format
                    }
                    String q = t.substring(0, firstTilde);
                    String[] op = t.substring(firstTilde + 1, lastTilde)
                            .split("\\|");
                    var qu = new QuestionData(no + "", topicId, "1", LocalDate.now().toString(), "mcq",
                            "Easy", q.replace("\r", "")
                            .replace("\n", " "), "", new OptionData(
                            Integer.parseInt(correctOption[no].trim())-1, IntStream.range(0, op.length)
                            .boxed()
                            .collect(Collectors.toMap(
                                    k -> k + 1,
                                    k -> op[k],
                                    (a, b) -> a,
                                    LinkedHashMap::new
                            ))
                    ));
                    listQuestion.add(qu);
                    no++;

                   /* IO.println("Question " + no++ + ": " + q.replace("\r", "")
                            .replace("\n", " "));
                    IO.println("Options: " + Arrays.toString(op));*/
                }
            }
            System.out.println(Arrays.toString(correctOption));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return
                IntStream.range(0, listQuestion.size())
                        .boxed()
                        .collect(Collectors.toMap(
                                i -> i + 1,          // Key: transform 0-based index to 1-based
                                listQuestion::get,           // Value: fetch element from list
                                (k1, k2) -> k1,      // Merge rule (not strictly needed here, but required by API)
                                TreeMap::new         // Supplier: forces the output to be a TreeMap
                        ));
    }
}