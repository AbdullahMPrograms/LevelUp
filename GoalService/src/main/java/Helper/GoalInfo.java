package Helper;

import java.sql.Date;

public class GoalInfo {
    private int userID;
    private String title, date, metricType, targetValue, targetUnit, frequency, description;

    public GoalInfo(){
        super();
    }

    public GoalInfo(int userID, String title, String date, String metricType, String targetValue, String targetUnit, String frequency, String description){
        this.userID = userID;
        this.title = title;
        this.date = date;
        this.metricType = metricType;
        this.targetValue = targetValue;
        this.targetUnit = targetUnit;
        this.frequency = frequency;
        this.description = description;
    }

    public int getUserID() {
        return userID;
    }

    public void setUserID(int userID) {
        this.userID = userID;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getMetricType() {
        return metricType;
    }

    public void setMetricType(String metricType) {
        this.metricType = metricType;
    }

    public String getTargetValue() {
        return targetValue;
    }

    public void setTargetValue(String targetValue) {
        this.targetValue = targetValue;
    }

    public String getTargetUnit() {
        return targetUnit;
    }

    public void setTargetUnit(String targetUnit) {
        this.targetUnit = targetUnit;
    }

    public String getFrequency() {
        return frequency;
    }

    public void setFrequency(String frequency) {
        this.frequency = frequency;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}