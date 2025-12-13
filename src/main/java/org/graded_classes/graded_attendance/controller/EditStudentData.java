package org.graded_classes.graded_attendance.controller;

import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import org.graded_classes.graded_attendance.data.Student;

import java.net.URL;
import java.sql.Connection;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.ResourceBundle;

public class EditStudentData implements Initializable {
    @FXML
    VBox formBox;
    @FXML
    private ComboBox<String> _class, blood_group, gender;


    @FXML
    private TextField aadhaar_no, ed_no, email_id,
            father_f_name, father_l_name, father_occ, first_name,
            g_num, last_name, mother_f_name, mother_l_name,
            mother_occ, school_n, telegram_id;
    @FXML
    private TextArea address;
    @FXML
    private DatePicker dob;
    private final String[] blood_groups_list = {"A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-"};
    private final String[] gender_list = {"Male", "Female"};
    private final String[] classes_list = {"I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X", "XI", "XII"};
    private final HashSet<String> list_of_subjects = new HashSet<>();
    LinkedHashMap<String, Student> studentData;
    String edNumber;

    public EditStudentData(LinkedHashMap<String, Student> studentData, String edNumber) {
        this.studentData = studentData;
        this.edNumber = edNumber;

    }


    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        ed_no.setText(edNumber);
        gender.setItems(FXCollections.observableArrayList(gender_list));
        blood_group.setItems(FXCollections.observableArrayList(blood_groups_list));
        _class.setItems(FXCollections.observableArrayList(classes_list));
        if (studentData.get(edNumber).name() != null) {
            first_name.setText(studentData.get(edNumber).name().
                    substring(0, studentData.get(edNumber).name().indexOf(' ')));
            last_name.setText(studentData.get(edNumber).name().
                    substring(studentData.get(edNumber).name().indexOf(' ') + 1));
        }
        if (studentData.get(edNumber).dob() != null)
            dob.getEditor().setText(studentData.get(edNumber).dob());
        if (studentData.get(edNumber)._class() != null)
            _class.getSelectionModel().select(studentData.get(edNumber)._class());
        if (studentData.get(edNumber).gender() != null)
            gender.getSelectionModel().select(studentData.get(edNumber).gender());
        if (studentData.get(edNumber).bloodGroup() != null)
            blood_group.getSelectionModel().select(studentData.get(edNumber).bloodGroup());
        if (studentData.get(edNumber).father_name() != null) {
            father_f_name.setText(studentData.get(edNumber).name().
                    substring(0, studentData.get(edNumber).father_name().indexOf(' ')));
            father_l_name.setText(studentData.get(edNumber).name().
                    substring(studentData.get(edNumber).father_name().indexOf(' ') + 1));
        }
        if (studentData.get(edNumber).mother_name() != null) {
            System.out.println(studentData.get(edNumber).mother_name());
            mother_f_name.setText(studentData.get(edNumber).mother_name().
                    substring(0, studentData.get(edNumber).mother_name().indexOf(' ')));
            mother_l_name.setText(studentData.get(edNumber).mother_name().
                    substring(studentData.get(edNumber).mother_name().indexOf(' ') + 1));
        }
        if (studentData.get(edNumber).father_occ() != null)
            father_occ.setText(studentData.get(edNumber).father_occ());
        if (studentData.get(edNumber).mother_occ() != null)
            mother_occ.setText(studentData.get(edNumber).mother_occ());
        if (studentData.get(edNumber).school_n() != null)
            school_n.setText(studentData.get(edNumber).school_n());
        if (studentData.get(edNumber).telegram_id() != null)
            telegram_id.setText(studentData.get(edNumber).telegram_id());
        if (studentData.get(edNumber).address() != null)
            address.setText(studentData.get(edNumber).address());
        if (studentData.get(edNumber).aadhaar_no() != null)
            aadhaar_no.setText(studentData.get(edNumber).aadhaar_no());
        if (studentData.get(edNumber).email() != null)
            email_id.setText(studentData.get(edNumber).email());
        if (studentData.get(edNumber).guardian_phone() != null)
            g_num.setText(studentData.get(edNumber).guardian_phone());
        var v = studentData.get(edNumber).subjects();
        for (var s : v) {
            var x = (CheckBox) formBox.lookup("#" + s);
            if (x != null)
                x.setSelected(true);
            list_of_subjects.add(s);
        }
    }


    @FXML
    private void onSubjectSelection(ActionEvent event) {
        CheckBox check = (CheckBox) event.getSource();
        String name = check.getText();
        if (check.isSelected())
            list_of_subjects.add(name);
        else
            list_of_subjects.remove(name);
    }

    public void update(Connection connection) {
        Student student = studentData.get(edNumber);
        String fullName = first_name.getText() + " " + last_name.getText();
        String fatherName = father_f_name.getText() + " " + father_l_name.getText();
        String motherName = mother_f_name.getText() + " " + mother_l_name.getText();
        student.setName(fullName);
        student.set_class(_class.getSelectionModel().getSelectedItem());
        student.setEmail(email_id.getText());
        student.setBloodGroup(blood_group.getSelectionModel().getSelectedItem());
        student.setGuardian_phone(g_num.getText());
        student.setAadhaar_no(aadhaar_no.getText());
        student.setFather_name(fatherName);
        student.setMother_name(motherName);
        student.setGender(gender.getSelectionModel().getSelectedItem());
        if (dob.getValue() != null)
            student.setDob(dob.getValue().toString());
        student.setAddress(address.getText());
        student.setFather_occ(father_occ.getText());
        student.setMother_occ(mother_occ.getText());
        student.setSchool_n(school_n.getText());
        student.setSubjects(list_of_subjects.toArray(new String[0]));
        student.setTelegram_id(telegram_id.getText());
        student.updateAll(connection);
    }

}
