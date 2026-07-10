package org.graded_classes.graded_attendance.components;


import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public class OpenCVLoader {

    private static boolean loaded = false;

    public static synchronized void loadOpenCV() {

        if (loaded) {
            return;
        }

        try (InputStream is =
                     OpenCVLoader.class.getResourceAsStream(
                             "/org/graded_classes/graded_attendance/native/x64/opencv_java4120.dll")) {

            if (is == null) {
                throw new RuntimeException(
                        "opencv_java4120.dll not found in resources");
            }

            Path tempDll =
                    Files.createTempFile("opencv_", ".dll");

            Files.copy(
                    is,
                    tempDll,
                    StandardCopyOption.REPLACE_EXISTING
            );

            tempDll.toFile().deleteOnExit();

            System.load(tempDll.toAbsolutePath().toString());

            loaded = true;

            System.out.println("OpenCV loaded successfully");

        } catch (Exception e) {
            throw new RuntimeException(
                    "Failed to load OpenCV", e);
        }
    }
}
