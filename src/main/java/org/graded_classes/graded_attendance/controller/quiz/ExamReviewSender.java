package org.graded_classes.graded_attendance.controller.quiz;

import org.graded_classes.graded_attendance.Main;
import org.graded_classes.graded_attendance.controller.home.MainController;
import org.graded_classes.graded_attendance.data.ExamData;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.BiConsumer;

public final class ExamReviewSender {

    private static final long SEND_DELAY_MILLIS = 1000L;
    private static final int MAXIMUM_FAILURE_DETAILS = 15;

    private final MainController mainController;

    public ExamReviewSender(MainController mainController) {
        this.mainController = Objects.requireNonNull(
                mainController,
                "MainController cannot be null."
        );
    }

    public ReviewSendingSummary sendAll(
            ExamData examInfo,
            BiConsumer<Integer, Integer> progressListener
    ) {
        Objects.requireNonNull(
                examInfo,
                "Exam information cannot be null."
        );

        int examId = parseExamId(examInfo);

        File reportDirectory = new File(
                Main.getRootPath(),
                "ExamAnswerReviews/Exam_" + examId
        );

        if (!reportDirectory.isDirectory()) {
            throw new IllegalStateException(
                    "The review PDF directory does not exist: "
                            + reportDirectory.getAbsolutePath()
                            + ". Generate the results before sending reports."
            );
        }

        var eligibleStudents = mainController
                .gradedDataLoader
                .getStudentData()
                .sequencedValues()
                .stream()
                .filter(student ->
                        student._class() != null
                                && Objects.equals(
                                student._class(),
                                examInfo.classes()
                        )
                )
                .filter(student ->
                        student.getBoard() != null
                                && examInfo.board() != null
                                && (
                                student.getBoard().equalsIgnoreCase(examInfo.board())
                                        || examInfo.board().equalsIgnoreCase("Both")
                        )
                )
                .toList();

        int totalStudents = eligibleStudents.size();

        if (totalStudents == 0) {
            return new ReviewSendingSummary(
                    0,
                    0,
                    0,
                    0,
                    0,
                    0,
                    false,
                    List.of("No eligible students matched this exam.")
            );
        }

        int sentCount = 0;
        int missingTelegramIdCount = 0;
        int invalidTelegramIdCount = 0;
        int missingPdfCount = 0;
        int failedCount = 0;
        int processedCount = 0;
        boolean interrupted = false;

        List<String> failureDetails = new ArrayList<>();

        for (var student : eligibleStudents) {
            if (Thread.currentThread().isInterrupted()) {
                interrupted = true;
                break;
            }

            String studentEd = student.ed_no();
            String studentName = student.name() == null || student.name().isBlank()
                    ? studentEd
                    : student.name().trim();

            try {
                String telegramId = student.telegram_id();

                if (telegramId == null || telegramId.isBlank()) {
                    missingTelegramIdCount++;
                    failureDetails.add(
                            studentName + " (" + studentEd + "): No Telegram ID"
                    );
                    continue;
                }

                long telegramChatId;

                try {
                    telegramChatId = Long.parseLong(telegramId.trim());
                } catch (NumberFormatException exception) {
                    invalidTelegramIdCount++;
                    failureDetails.add(
                            studentName
                                    + " ("
                                    + studentEd
                                    + "): Invalid Telegram ID "
                                    + telegramId
                    );
                    continue;
                }

                File reviewPdf = findStudentReviewPdf(
                        reportDirectory,
                        studentEd,
                        studentName
                );

                if (reviewPdf == null || !reviewPdf.isFile()) {
                    missingPdfCount++;
                    failureDetails.add(
                            studentName
                                    + " ("
                                    + studentEd
                                    + "): Review PDF not found"
                    );
                    continue;
                }

                String caption = buildReviewPdfCaption(
                        examInfo,
                        studentName
                );

                mainController.messageSender.sendDocument(
                        reviewPdf,
                        telegramChatId,
                        caption
                );

                sentCount++;

                Thread.sleep(SEND_DELAY_MILLIS);

            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                interrupted = true;

                failureDetails.add(
                        "Sending was interrupted while processing "
                                + studentName
                                + " ("
                                + studentEd
                                + ")."
                );

                break;

            } catch (Exception exception) {
                failedCount++;

                failureDetails.add(
                        studentName
                                + " ("
                                + studentEd
                                + "): "
                                + getThrowableMessage(exception)
                );

                exception.printStackTrace();

            } finally {
                processedCount++;
                notifyProgress(
                        progressListener,
                        processedCount,
                        totalStudents
                );
            }
        }

        return new ReviewSendingSummary(
                totalStudents,
                sentCount,
                missingTelegramIdCount,
                missingPdfCount,
                failedCount,
                invalidTelegramIdCount,
                interrupted,
                List.copyOf(failureDetails)
        );
    }

    private int parseExamId(ExamData examInfo) {
        try {
            return Integer.parseInt(examInfo.id());
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(
                    "Invalid exam ID: " + examInfo.id(),
                    exception
            );
        }
    }

    private void notifyProgress(
            BiConsumer<Integer, Integer> progressListener,
            int processed,
            int total
    ) {
        if (progressListener == null) {
            return;
        }

        try {
            progressListener.accept(processed, total);
        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }

    private File findStudentReviewPdf(
            File reportDirectory,
            String studentEd,
            String studentName
    ) {
        if (reportDirectory == null
                || !reportDirectory.isDirectory()
                || studentEd == null
                || studentEd.isBlank()) {
            return null;
        }

        String trimmedStudentEd = studentEd.trim();
        String effectiveStudentName = studentName == null || studentName.isBlank()
                ? trimmedStudentEd
                : studentName.trim();

        String exactFileName = sanitizePdfFileName(
                trimmedStudentEd
                        + "_"
                        + effectiveStudentName
                        + "_Answer_Review.pdf"
        );

        File exactFile = new File(
                reportDirectory,
                exactFileName
        );

        if (exactFile.isFile()) {
            return exactFile;
        }

        String sanitizedEdPrefix = sanitizeFileNamePart(trimmedStudentEd)
                .toLowerCase(Locale.ROOT) + "_";

        File[] matchingFiles = reportDirectory.listFiles(file -> {
            if (!file.isFile()) {
                return false;
            }

            String fileName = file.getName().toLowerCase(Locale.ROOT);

            return fileName.startsWith(sanitizedEdPrefix)
                    && fileName.endsWith("_answer_review.pdf");
        });

        if (matchingFiles == null || matchingFiles.length == 0) {
            return null;
        }

        return Arrays.stream(matchingFiles)
                .max(Comparator.comparingLong(File::lastModified))
                .orElse(null);
    }

    private String buildReviewPdfCaption(
            ExamData examInfo,
            String studentName
    ) {
        StringBuilder caption = new StringBuilder("Exam answer review");

        appendCaptionLine(caption, "Student", studentName);
        appendCaptionLine(caption, "Subject", examInfo.subject());
        appendCaptionLine(caption, "Class", examInfo.classes());
        appendCaptionLine(caption, "Exam date", examInfo.doe());

        return caption.toString();
    }

    private void appendCaptionLine(
            StringBuilder caption,
            String label,
            String value
    ) {
        if (value == null || value.isBlank()) {
            return;
        }

        caption.append('\n')
                .append(label)
                .append(": ")
                .append(value.trim());
    }

    private String sanitizePdfFileName(String value) {
        String result = value == null || value.isBlank()
                ? "Answer_Review.pdf"
                : sanitizeFileNamePart(value.trim());

        if (!result.toLowerCase(Locale.ROOT).endsWith(".pdf")) {
            result += ".pdf";
        }

        if (result.length() > 150) {
            result = result.substring(0, 146) + ".pdf";
        }

        return result;
    }

    private String sanitizeFileNamePart(String value) {
        if (value == null || value.isBlank()) {
            return "Answer_Review";
        }

        return value.trim()
                .replaceAll("[\\\\/:*?\"<>|]", "_")
                .replaceAll("\\s+", "_")
                .replaceAll("_+", "_");
    }

    private String getThrowableMessage(Throwable throwable) {
        if (throwable == null) {
            return "Unknown error";
        }

        Throwable cause = throwable;

        while (cause.getCause() != null) {
            cause = cause.getCause();
        }

        return cause.getMessage() == null || cause.getMessage().isBlank()
                ? cause.getClass().getSimpleName()
                : cause.getMessage();
    }

    public record ReviewSendingSummary(
            int eligibleStudents,
            int sent,
            int missingTelegramId,
            int missingPdf,
            int failed,
            int invalidTelegramId,
            boolean interrupted,
            List<String> failureDetails
    ) {
        public ReviewSendingSummary {
            failureDetails = failureDetails == null
                    ? List.of()
                    : List.copyOf(failureDetails);
        }

        public String toDisplayText() {
            StringBuilder message = new StringBuilder();

            message.append("Eligible students: ")
                    .append(eligibleStudents)
                    .append("\nSent successfully: ")
                    .append(sent)
                    .append("\nMissing Telegram ID: ")
                    .append(missingTelegramId)
                    .append("\nInvalid Telegram ID: ")
                    .append(invalidTelegramId)
                    .append("\nPDF not found: ")
                    .append(missingPdf)
                    .append("\nFailed to send: ")
                    .append(failed);

            if (interrupted) {
                message.append("\n\nSending was interrupted.");
            }

            if (!failureDetails.isEmpty()) {
                message.append("\n\nDetails:\n");

                int detailsToShow = Math.min(
                        failureDetails.size(),
                        MAXIMUM_FAILURE_DETAILS
                );

                for (int index = 0; index < detailsToShow; index++) {
                    message.append("- ")
                            .append(failureDetails.get(index))
                            .append('\n');
                }

                if (failureDetails.size() > detailsToShow) {
                    message.append("- ...and ")
                            .append(failureDetails.size() - detailsToShow)
                            .append(" more.");
                }
            }

            return message.toString().trim();
        }
    }
}
