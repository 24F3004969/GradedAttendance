package org.graded_classes.graded_attendance;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

public class GradedResourceLoader {
    public static URL loadURL(String path) {
        return GradedResourceLoader.class.getResource(path);
    }

    public static String load(String path) {
        return loadURL(path).toExternalForm();
    }

    public static InputStream loadStream(String name) {
        return GradedResourceLoader.class.getResourceAsStream(name);
    }
}
