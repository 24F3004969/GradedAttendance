package org.graded_classes.graded_attendance.controller.quiz;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ComboBox;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;
import org.graded_classes.graded_attendance.components.FilterComboBox;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class QuizTopic implements Initializable {

    @FXML
    public ComboBox<String> filterBox;
    public FilterComboBox filterComboBox;
    ObservableList<String> observableList = FXCollections.observableArrayList(List.of(
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
    ));

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        AnchorPane parent = (AnchorPane) filterBox.getParent();
        filterComboBox=new FilterComboBox(observableList);
        parent.getChildren().set(parent.getChildren().indexOf(filterBox),filterComboBox);
        filterComboBox.setPromptText("Subject");
    }
}
