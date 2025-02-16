package Persistence;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import Helper.GoalInfo;

public class Goal_CRUD {

    private static Connection getCon() throws SQLException, ClassNotFoundException {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            String url = "jdbc:mysql://localhost:3306/levelup?zeroDateTimeBehavior=CONVERT_TO_NULL";
            String user = "root";
            String password = "student123";
            
            System.out.println("Attempting to connect to database...");
            Connection con = DriverManager.getConnection(url, user, password);
            System.out.println("Connection established successfully.");
            return con;
        } catch (SQLException e) {
            System.err.println("SQL Connection Error: " + e.getMessage());
            throw e;
        } catch (ClassNotFoundException e) {
            System.err.println("Driver Error: " + e.getMessage());
            throw e;
        }
    }

    public static GoalInfo read(){
        GoalInfo bean = null;


        
        return bean;
    }

    public String create(GoalInfo newGoal){
        String s = "";

        try {
            Connection con = getCon();
            if (con == null) {
                System.err.println("Connection is null");
                return "Database connection failed";
            }
            
            // Use PreparedStatement to prevent SQL injection
            String q = "INSERT INTO GOAL (USER_ID, GOAL_TITLE, TARGET_DATE, METRIC_TYPE, TARGET_VALUE, TARGET_UNIT, FREQUENCY, DESCRIPTION) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
            try (PreparedStatement stmt = con.prepareStatement(q)) {
                stmt.setInt(1, newGoal.getUserID());
                stmt.setString(2,newGoal.getTitle());
                stmt.setString(3, newGoal.getDate());
                stmt.setString(4, newGoal.getMetricType());
                stmt.setString(5, newGoal.getTargetValue());
                stmt.setString(6, newGoal.getTargetUnit());
                stmt.setString(7, newGoal.getFrequency());
                stmt.setString(8, newGoal.getDescription());
                
                System.out.println("Executing query: " + q);
                
                int rowsAffected = stmt.executeUpdate();
                if (rowsAffected > 0) {
                    s = "goal creation success";
                } else {
                    s = "Failed to insert goal";
                }
            }
            
            con.close();
        } catch (SQLException e) {
            System.err.println("SQL Error: " + e.getMessage());
            s = "Error: " + e.getMessage();
        } catch (ClassNotFoundException e) {
            System.err.println("Driver Error: " + e.getMessage());
            s = "Error: Driver not found";
        } catch (Exception e) {
            System.err.println("Unexpected Error: " + e.getMessage());
            s = "Error: Unexpected error occurred";
        }
        return s;
    }

}
