package org.graded_classes.graded_attendance.controller.quiz;

import com.dlsc.gemsfx.SearchField;
import com.dlsc.gemsfx.TimePicker;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import org.graded_classes.graded_attendance.controller.MainController;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class ExamCreator implements Initializable {
    @FXML
    private ComboBox<String> classNum;

    @FXML
    private Button close;

    @FXML
    private TimePicker endTime;

    @FXML
    private DatePicker exam;

    @FXML
    private ComboBox<String> roomNo;

    @FXML
    private TimePicker startTime;

    @FXML
    private SearchField<String> subject;

    @FXML
    private SearchField<String> topicName;
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

    @FXML
    void create(ActionEvent event) {

    }

    MainController mainController;

    public ExamCreator(MainController mainController) {
        this.mainController = mainController;
    }

    @FXML
    void create() {
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        close.setOnMouseClicked(event -> {
            this.mainController.modalPane.hide();
        });
        topicName.setSuggestionProvider(request -> observableList.stream().filter(country ->
                country.toLowerCase().contains(request.getUserText().toLowerCase())).collect(Collectors.toList()));
        subject.setSuggestionProvider(request -> observableList.stream().filter(country ->
                country.toLowerCase().contains(request.getUserText().toLowerCase())).collect(Collectors.toList()));
        classNum.setItems(observableList);
    }
}
