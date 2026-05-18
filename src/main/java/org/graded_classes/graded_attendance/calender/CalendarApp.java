package org.graded_classes.graded_attendance.calender;

import com.calendarfx.model.Calendar;
import com.calendarfx.model.CalendarSource;
import com.calendarfx.view.CalendarView;
import javafx.application.Platform;
import org.graded_classes.graded_attendance.GradedResourceLoader;

import java.time.LocalDate;
import java.time.LocalTime;

public class CalendarApp  {

    public CalendarView createCalenderView() {

        CalendarView calendarView = new CalendarView(); // (1)

        Calendar<String> birthdays = new Calendar<>("Birthdays"); // (2)
        Calendar<String> holidays = new Calendar<>("Holidays");


        CalendarSource myCalendarSource = new CalendarSource("My Calendars"); // (4)
        myCalendarSource.getCalendars().addAll(birthdays, holidays);

        calendarView.getCalendarSources().addAll(myCalendarSource); // (5)

        calendarView.setRequestedTime(LocalTime.now());
        calendarView.getStylesheets().clear();
        Thread updateTimeThread = getThread(calendarView);
        updateTimeThread.start();
        return calendarView;
    }

    private static Thread getThread(CalendarView calendarView) {
        Thread updateTimeThread = new Thread("Calendar: Update Time Thread") {
            @Override
            public void run() {
                while (true) {
                    Platform.runLater(() -> {
                        calendarView.setToday(LocalDate.now());
                        calendarView.setTime(LocalTime.now());
                    });

                    try {
                        // update every 10 seconds
                        sleep(10000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                }
            }
        };

        updateTimeThread.setPriority(Thread.MIN_PRIORITY);
        updateTimeThread.setDaemon(true);
        return updateTimeThread;
    }
}