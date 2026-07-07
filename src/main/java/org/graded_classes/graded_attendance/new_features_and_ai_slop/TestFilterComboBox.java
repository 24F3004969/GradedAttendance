package org.graded_classes.graded_attendance.new_features_and_ai_slop;

import atlantafx.base.theme.PrimerLight;
import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.scene.Scene;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.graded_classes.graded_attendance.components.FilterComboBox;

import java.util.List;

public class TestFilterComboBox extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        var pane = new VBox();
        FilterComboBox filterComboBox = new FilterComboBox(FXCollections.observableArrayList(List.of(
                "English", "Hindi", "Math", "History", "Physics", "Chemistry", "Biology",
                "Geography", "Civics", "Economics", "Computer Science", "Information Technology",
                "Environmental Science", "General Science", "Social Science", "Political Science",
                "Psychology", "Sociology", "Philosophy", "Literature", "Grammar", "Creative Writing",
                "Statistics", "Algebra", "Geometry", "Trigonometry", "Calculus", "Number Theory",
                "Discrete Mathematics", "Applied Mathematics", "Astronomy", "Geology", "Meteorology",
                "Botany", "Zoology", "Biotechnology", "Microbiology", "Genetics", "Human Anatomy",
                "Physiology", "Biochemistry", "Organic Chemistry", "Inorganic Chemistry",
                "Physical Chemistry", "Analytical Chemistry", "Nuclear Physics", "Quantum Physics",
                "Thermodynamics", "Optics", "Electromagnetism", "Mechanics", "Fluid Mechanics",
                "Electronics", "Electrical Engineering Basics", "Robotics", "Artificial Intelligence",
                "Machine Learning", "Data Science", "Database Management", "Networking",
                "Cyber Security", "Web Development", "Mobile App Development", "Cloud Computing",
                "Operating Systems", "Software Engineering", "Data Structures", "Algorithms",
                "Accountancy", "Business Studies", "Commerce", "Entrepreneurship", "Marketing",
                "Finance", "Management", "Human Resource Management", "Law", "Constitutional Law",
                "International Relations", "Public Administration", "Journalism", "Mass Communication",
                "Art", "Drawing", "Painting", "Music", "Dance", "Drama", "Theatre",
                "Physical Education", "Health Education", "Yoga", "Sports Science",
                "Home Science", "Nutrition", "Food Science", "Hospitality Management",
                "Tourism", "Agriculture", "Horticulture"
        )));
        Application.setUserAgentStylesheet(new PrimerLight().getUserAgentStylesheet());
        filterComboBox.setPromptText("Subject");
        pane.getChildren().add(new FilterComboBox(FXCollections.observableArrayList(List.of("jkdfnasjkldn"))));
        pane.getChildren().add(filterComboBox);

        stage.setScene(new Scene(pane, 600, 400));
        stage.show();
    }
}
