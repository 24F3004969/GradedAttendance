package org.graded_classes.graded_attendance.components;

import org.bytedeco.opencv.opencv_core.*;
import org.bytedeco.opencv.opencv_face.LBPHFaceRecognizer;

import java.io.File;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;

import static org.bytedeco.opencv.global.opencv_core.CV_32SC1;
import static org.bytedeco.opencv.global.opencv_imgcodecs.imread;
import static org.bytedeco.opencv.global.opencv_imgproc.*;

public class Training {

    public static void main(String[] args) {

        String imageDir = "C:\\Users\\hey\\GradedAttendance\\data";

        File folder = new File(imageDir);
        File[] files = folder.listFiles();

        if (files == null || files.length == 0) {
            System.out.println("No images found!");
            return;
        }

        List<Mat> images = new ArrayList<>();
        Mat labels = new Mat(files.length, 1, CV_32SC1);

        IntBuffer labelBuffer = labels.createBuffer();

        int counter = 0;

        for (File file : files) {

            Mat img = imread(file.getAbsolutePath(), 0); // grayscale

            if (img.empty()) {
                continue;
            }

            // Resize all images to same size
            resize(img, img, new Size(200, 200));

            images.add(img);

            labelBuffer.put(counter, 1); // same person label
            counter++;
        }

        MatVector matVector = new MatVector(images.size());

        for (int i = 0; i < images.size(); i++) {
            matVector.put(i, images.get(i));
        }

        try (LBPHFaceRecognizer recognizer = LBPHFaceRecognizer.create()) {

            recognizer.train(matVector, labels);

            recognizer.save("attendance_model.yml");

            System.out.println("Training completed!");
        }
    }
}

