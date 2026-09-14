package org.graded_classes.graded_attendance.controller.camera;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;

public class TopicDataImporter {

    private final Connection connection;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public TopicDataImporter(Connection connection) {
        this.connection = connection;
    }

    /**
     * Imports topics and subtopics from the exported JSON.
     *
     * @param jsonFilePath path to the JSON export file
     * @param className    class/grade to save, for example "Grade 8"
     * @param subjectName  subject to save, for example "Mathematics"
     */
    public void importTopics(
            Path jsonFilePath,
            String className,
            String subjectName
    ) throws Exception {

        String json = Files.readString(jsonFilePath);
        JsonNode root = objectMapper.readTree(json);
        JsonNode topicsNode = root.path("topics");

        if (!topicsNode.isArray()) {
            throw new IllegalArgumentException(
                    "The JSON does not contain a valid topics array."
            );
        }

        String insertTopicSql = """
                INSERT INTO Topics (class, subject, topic_name)
                VALUES (?, ?, ?)
                """;

        String insertSubtopicSql = """
                INSERT INTO Subtopics (subject, subtopic_name, topic_id)
                VALUES (?, ?, ?)
                """;

        boolean previousAutoCommit = connection.getAutoCommit();

        try (
                PreparedStatement topicStatement = connection.prepareStatement(
                        insertTopicSql,
                        Statement.RETURN_GENERATED_KEYS
                );
                PreparedStatement subtopicStatement =
                        connection.prepareStatement(insertSubtopicSql)
        ) {
            connection.setAutoCommit(false);

            int topicCount = 0;
            int subtopicCount = 0;

            for (JsonNode topicNode : topicsNode) {
                String topicName = topicNode.path("title").asText().trim();

                if (topicName.isEmpty()) {
                    continue;
                }

                topicStatement.setString(1, className);
                topicStatement.setString(2, subjectName);
                topicStatement.setString(3, topicName);
                topicStatement.executeUpdate();

                long topicId;

                try (ResultSet keys = topicStatement.getGeneratedKeys()) {
                    if (!keys.next()) {
                        throw new SQLException(
                                "Could not obtain ID for topic: " + topicName
                        );
                    }

                    topicId = keys.getLong(1);
                }

                topicCount++;

                JsonNode subsNode = topicNode.path("subs");

                if (!subsNode.isArray()) {
                    continue;
                }

                for (JsonNode subNode : subsNode) {
                    String subtopicName = subNode.asText().trim();

                    if (subtopicName.isEmpty()) {
                        continue;
                    }

                    subtopicStatement.setString(1, subjectName);
                    subtopicStatement.setString(2, subtopicName);
                    subtopicStatement.setLong(3, topicId);
                    subtopicStatement.addBatch();

                    subtopicCount++;
                }

                subtopicStatement.executeBatch();
                subtopicStatement.clearBatch();
            }

            connection.commit();

            System.out.println("Import completed successfully.");
            System.out.println("Topics inserted: " + topicCount);
            System.out.println("Subtopics inserted: " + subtopicCount);

        } catch (Exception exception) {
            connection.rollback();
            throw exception;
        } finally {
            connection.setAutoCommit(previousAutoCommit);
        }
    }
}