package org.graded_classes.graded_attendance.components;

import org.apache.pdfbox.multipdf.PDFMergerUtility;

import java.io.File;
import java.util.Arrays;

public class MergeAllPDFs {

    public static void main(String[] args) {
        String inputFolder = "C:\\Users\\hilal\\StudentReport";
        String outputFile = "C:\\Users\\hilal\\Merged.pdf";

        try {
            PDFMergerUtility merger = new PDFMergerUtility();

            File folder = new File(inputFolder);

            File[] pdfFiles = folder.listFiles((dir, name) ->
                    name.toLowerCase().endsWith(".pdf"));

            if (pdfFiles == null || pdfFiles.length == 0) {
                System.out.println("No PDF files found.");
                return;
            }

            // Sort by filename
            Arrays.sort(pdfFiles, (a, b) ->
                    a.getName().compareToIgnoreCase(b.getName()));

            for (File pdf : pdfFiles) {
                if (!pdf.getName().equalsIgnoreCase("Merged.pdf")) {
                    merger.addSource(pdf);
                    System.out.println("Added: " + pdf.getName());
                }
            }

            merger.setDestinationFileName(outputFile);
            merger.mergeDocuments(null);

            System.out.println("Merged PDF created: " + outputFile);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}