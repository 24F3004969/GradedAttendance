package org.graded_classes.graded_attendance.components;

import org.bytedeco.opencv.opencv_core.*;
import org.bytedeco.opencv.opencv_face.LBPHFaceRecognizer;

import java.io.File;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.bytedeco.opencv.global.opencv_core.CV_32SC1;
import static org.bytedeco.opencv.global.opencv_imgcodecs.imread;
import static org.bytedeco.opencv.global.opencv_imgproc.*;

public class Training {

    public static void main(String[] args) {

        String dataPath =
                "C:\\Users\\hey\\GradedAttendance\\data";

        File root = new File(dataPath);

        if (!root.exists()) {
            System.out.println("Data folder not found");
            return;
        }

        List<Mat> imageList = new ArrayList<>();
        List<Integer> labelList = new ArrayList<>();

        File[] studentFolders = root.listFiles(File::isDirectory);

        if (studentFolders == null ||
                studentFolders.length == 0) {

            System.out.println("No student folders found");
            return;
        }

        int totalImages = 0;
        System.out.println(studentFolders.length);
        for (File studentFolder : studentFolders) {

            int label;

            try {
                label = Integer.parseInt(
                        studentFolder.getName()
                );
            } catch (NumberFormatException e) {
                continue;
            }

            File[] images = studentFolder.listFiles();

            if (images == null) {
                continue;
            }

            for (File imageFile : images) {

                Mat image = imread(
                        imageFile.getAbsolutePath(),
                        0
                );

                if (image.empty()) {
                    continue;
                }

                resize(
                        image,
                        image,
                        new Size(200, 200)
                );

                equalizeHist(
                        image,
                        image
                );

                imageList.add(image);
                labelList.add(label);

                totalImages++;
            }
        }

        if (imageList.isEmpty()) {

            System.out.println(
                    "No valid training images found"
            );
            return;
        }

        MatVector images =
                new MatVector(imageList.size());

        for (int i = 0; i < imageList.size(); i++) {

            images.put(
                    i,
                    imageList.get(i)
            );
        }

        Mat labels = new Mat(
                labelList.size(),
                1,
                CV_32SC1
        );

        IntBuffer buffer =
                labels.createBuffer();

        for (int i = 0; i < labelList.size(); i++) {

            buffer.put(
                    i,
                    labelList.get(i)
            );
        }

        try (
                LBPHFaceRecognizer recognizer =
                        LBPHFaceRecognizer.create(
                                1,      // radius
                                8,      // neighbors
                                8,      // gridX
                                8,      // gridY
                                80      // threshold
                        )
        ) {

            recognizer.train(
                    images,
                    labels
            );

            recognizer.save(
                    "attendance_model.yml"
            );

            System.out.println(
                    "Training completed successfully"
            );

            System.out.println(
                    "Students : "
                            + studentFolders.length
            );

            System.out.println(
                    "Images : "
                            + totalImages
            );

            System.out.println(
                    "Model : attendance_model.yml"
            );
        }
    }
}