package org.graded_classes.graded_attendance.leaderboard;

public class AnimationDuration {
    double fadeTime,layoutDuration;

    public AnimationDuration( double layoutDuration,double fadeTime) {
        this.fadeTime = fadeTime;
        this.layoutDuration = layoutDuration;
    }

    public double getFadeTime() {
        return fadeTime;
    }

    public void setFadeTime(double fadeTime) {
        this.fadeTime = fadeTime;
    }

    public double getLayoutDuration() {
        return layoutDuration;
    }

    public void setLayoutDuration(double layoutDuration) {
        this.layoutDuration = layoutDuration;
    }

    @Override
    public String toString() {
        return "AnimationDuration{" +
                "fadeTime=" + fadeTime +
                ", layoutDuration=" + layoutDuration +
                '}';
    }
}
