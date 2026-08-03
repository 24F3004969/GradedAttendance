package org.graded_classes.graded_attendance.data;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Objects;

public final class OptionData {
    private  int option_index;
    private final LinkedHashMap<Integer,String> options;

    public OptionData(
            int option_index,
            LinkedHashMap<Integer,String> options
    ) {
        this.option_index = option_index;
        this.options = options;
    }

    public int option_index() {
        return option_index;
    }

    public void setOption_index(int option_index) {
        this.option_index = option_index;
    }

    public LinkedHashMap<Integer,String>  options() {
        return options;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (OptionData) obj;
        return this.option_index == that.option_index &&
                Objects.equals(this.options, that.options);
    }

    @Override
    public int hashCode() {
        return Objects.hash(option_index, options);
    }

    @Override
    public String toString() {
        return "OptionData[" +
                "option_index=" + option_index + ", " +
                "options=" + options + ']';
    }

}
