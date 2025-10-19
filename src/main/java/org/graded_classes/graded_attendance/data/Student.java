package org.graded_classes.graded_attendance.data;

import javafx.application.Platform;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Objects;

public class Student {
    public void setEd_no(String ed_no) {
        this.ed_no = ed_no;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setBloodGroup(String bloodGroup) {
        this.bloodGroup = bloodGroup;
    }

    public void setGuardian_phone(String guardian_phone) {
        this.guardian_phone = guardian_phone;
    }

    public void setAadhaar_no(String aadhaar_no) {
        this.aadhaar_no = aadhaar_no;
    }

    public void setFather_name(String father_name) {
        this.father_name = father_name;
    }

    public void setMother_name(String mother_name) {
        this.mother_name = mother_name;
    }

    public void set_class(String _class) {
        this._class = _class;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public void setDob(String dob) {
        this.dob = dob;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public void setFather_occ(String father_occ) {
        this.father_occ = father_occ;
    }

    public void setMother_occ(String mother_occ) {
        this.mother_occ = mother_occ;
    }

    public void setPrevious_ins_name(String previous_ins_name) {
        this.previous_ins_name = previous_ins_name;
    }

    public void setReason_leaving(String reason_leaving) {
        this.reason_leaving = reason_leaving;
    }

    public void setSchool_n(String school_n) {
        this.school_n = school_n;
    }

    public void setSuggestions(String suggestions) {
        this.suggestions = suggestions;
    }

    public void setSubjects(String[] subjects) {
        this.subjects = subjects;
    }

    public void setTelegram_id(String telegram_id) {
        this.telegram_id = telegram_id;
    }

    private String ed_no;
    private String name;
    private String email;
    private String bloodGroup;
    private String guardian_phone;
    private String aadhaar_no;
    private String father_name;
    private String mother_name;
    private String _class;
    private String gender;
    private String dob;
    private String address;
    private String father_occ;
    private String mother_occ;
    private String previous_ins_name;
    private String reason_leaving;
    private String school_n;
    private String suggestions;
    private String[] subjects;
    private String telegram_id;

    public Student(String ed_no,
                   String name,
                   String email,
                   String bloodGroup,
                   String guardian_phone,
                   String aadhaar_no,
                   String father_name,
                   String mother_name,
                   String _class,
                   String gender,
                   String dob,
                   String address,
                   String father_occ,
                   String mother_occ,
                   String previous_ins_name,
                   String reason_leaving,
                   String school_n,
                   String suggestions,
                   String[] subjects,
                   String telegram_id) {
        this.ed_no = ed_no;
        this.name = name;
        this.email = email;
        this.bloodGroup = bloodGroup;
        this.guardian_phone = guardian_phone;
        this.aadhaar_no = aadhaar_no;
        this.father_name = father_name;
        this.mother_name = mother_name;
        this._class = _class;
        this.gender = gender;
        this.dob = dob;
        this.address = address;
        this.father_occ = father_occ;
        this.mother_occ = mother_occ;
        this.previous_ins_name = previous_ins_name;
        this.reason_leaving = reason_leaving;
        this.school_n = school_n;
        this.suggestions = suggestions;
        this.subjects = subjects;
        this.telegram_id = telegram_id;

    }

    public void insertIntoDatabase(Connection connection) throws SQLException {
        System.out.println("inside insertIntoDatabase");
        String sql = "INSERT INTO StudentData(ed_no, name,email, bloodGroup, guardian_phone, aadhaar_no, father_name, mother_name, class, gender, dob, address, father_occ, mother_occ, previous_ins_name, reason_leaving, school_n, suggestions, subjects, telegram_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, ed_no);
            stmt.setString(2, name);
            stmt.setString(3, email);
            stmt.setString(4, bloodGroup);
            stmt.setString(5, guardian_phone);
            stmt.setString(6, aadhaar_no);
            stmt.setString(7, father_name);
            stmt.setString(8, mother_name);
            stmt.setString(9, _class);
            stmt.setString(10, gender);
            stmt.setString(11, dob);
            stmt.setString(12, address);
            stmt.setString(13, father_occ);
            stmt.setString(14, mother_occ);
            stmt.setString(15, previous_ins_name);
            stmt.setString(16, reason_leaving);
            stmt.setString(17, school_n);
            stmt.setString(18, suggestions);
            stmt.setString(19, String.join(", ", subjects)); // Convert list to comma-separated string
            stmt.setString(20, telegram_id);

            stmt.executeUpdate();
        }
    }

    public void update(Connection connection, String columnName, String newValue) {
        String sql = """
                 UPDATE "StudentData" SET %s = ? WHERE ed_no = %s
                """.formatted(columnName, ed_no);
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, newValue);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void updateAll(Connection connection) {

        String sql = "UPDATE StudentData SET "
                + "name = ?, class = ?, email = ?, bloodGroup = ?, guardian_phone = ?, "
                + "aadhaar_no = ?, father_name = ?, mother_name = ?, gender = ?, dob = ?, "
                + "address = ?, father_occ = ?, mother_occ = ?, school_n = ?, subjects = ?, telegram_id = ? "
                + "WHERE ed_no = ?";


        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {

            pstmt.setString(1, name);
            pstmt.setString(2, _class);
            pstmt.setString(3, email);
            pstmt.setString(4, bloodGroup);
            pstmt.setString(5, guardian_phone);
            pstmt.setString(6, aadhaar_no);
            pstmt.setString(7, father_name);
            pstmt.setString(8, mother_name);
            pstmt.setString(9, gender);
            pstmt.setString(10, dob);
            pstmt.setString(11, address);
            pstmt.setString(12, father_occ);
            pstmt.setString(13, mother_occ);
            pstmt.setString(14, school_n);
            pstmt.setString(15, String.join(", ", subjects));
            pstmt.setString(16, telegram_id);
            pstmt.setString(17, ed_no);

            pstmt.executeUpdate();
            System.out.println("Row updated successfully.");

        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }

    }

    public int updateTelegram(Connection connection, String edNo, String name, String _class, String newTelegramId) {
        String sql = """
                 UPDATE StudentData SET telegram_id = ? WHERE ed_no = ? AND TRIM(LOWER(name)) = TRIM(LOWER(?)) AND TRIM(LOWER(class)) = TRIM(LOWER(?))
                """;
        try (PreparedStatement pst = connection.prepareStatement(sql)) {
            pst.setString(1, newTelegramId);
            pst.setString(2, edNo);
            pst.setString(3, name);
            pst.setString(4, _class);
            return pst.executeUpdate();
        } catch (SQLException _) {
        }
        return 0;
    }

    public String ed_no() {
        return ed_no;
    }

    public String name() {
        return name;
    }

    public String email() {
        return email;
    }

    public String bloodGroup() {
        return bloodGroup;
    }

    public String guardian_phone() {
        return guardian_phone;
    }

    public String aadhaar_no() {
        return aadhaar_no;
    }

    public String father_name() {
        return father_name;
    }

    public String mother_name() {
        return mother_name;
    }

    public String _class() {
        return _class;
    }

    public String gender() {
        return gender;
    }

    public String dob() {
        return dob;
    }

    public String address() {
        return address;
    }

    public String father_occ() {
        return father_occ;
    }

    public String mother_occ() {
        return mother_occ;
    }

    public String previous_ins_name() {
        return previous_ins_name;
    }

    public String reason_leaving() {
        return reason_leaving;
    }

    public String school_n() {
        return school_n;
    }

    public String suggestions() {
        return suggestions;
    }

    public String[] subjects() {
        return subjects;
    }

    public String telegram_id() {
        return telegram_id;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (Student) obj;
        return Objects.equals(this.ed_no, that.ed_no) &&
                Objects.equals(this.name, that.name) &&
                Objects.equals(this.email, that.email) &&
                Objects.equals(this.bloodGroup, that.bloodGroup) &&
                Objects.equals(this.guardian_phone, that.guardian_phone) &&
                Objects.equals(this.aadhaar_no, that.aadhaar_no) &&
                Objects.equals(this.father_name, that.father_name) &&
                Objects.equals(this.mother_name, that.mother_name) &&
                Objects.equals(this._class, that._class) &&
                Objects.equals(this.gender, that.gender) &&
                Objects.equals(this.dob, that.dob) &&
                Objects.equals(this.address, that.address) &&
                Objects.equals(this.father_occ, that.father_occ) &&
                Objects.equals(this.mother_occ, that.mother_occ) &&
                Objects.equals(this.previous_ins_name, that.previous_ins_name) &&
                Objects.equals(this.reason_leaving, that.reason_leaving) &&
                Objects.equals(this.school_n, that.school_n) &&
                Objects.equals(this.suggestions, that.suggestions) &&
                Objects.equals(this.subjects, that.subjects) &&
                Objects.equals(this.telegram_id, that.telegram_id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ed_no, name, email, bloodGroup, guardian_phone, aadhaar_no, father_name, mother_name, _class, gender, dob, address, father_occ, mother_occ, previous_ins_name, reason_leaving, school_n, suggestions, subjects, telegram_id);
    }

    @Override
    public String toString() {
        return "Student[" +
                "ed_no=" + ed_no + ", " +
                "name=" + name + ", " +
                "email=" + email + ", " +
                "bloodGroup=" + bloodGroup + ", " +
                "guardian_phone=" + guardian_phone + ", " +
                "aadhaar_no=" + aadhaar_no + ", " +
                "father_name=" + father_name + ", " +
                "mother_name=" + mother_name + ", " +
                "_class=" + _class + ", " +
                "gender=" + gender + ", " +
                "dob=" + dob + ", " +
                "address=" + address + ", " +
                "father_occ=" + father_occ + ", " +
                "mother_occ=" + mother_occ + ", " +
                "previous_ins_name=" + previous_ins_name + ", " +
                "reason_leaving=" + reason_leaving + ", " +
                "school_n=" + school_n + ", " +
                "suggestions=" + suggestions + ", " +
                "subjects=" + subjects + ", " +
                "telegram_id=" + telegram_id + ']';
    }


}
