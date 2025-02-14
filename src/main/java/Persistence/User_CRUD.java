/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Persistence;

/**
 *
 * @author student
 */

import java.sql.*;
import Helper.UserInfo;

public class User_CRUD {
    
    private static Connection getCon() throws SQLException, ClassNotFoundException {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            String url = "jdbc:mysql://localhost:3306/levelup?zeroDateTimeBehavior=CONVERT_TO_NULL";
            String user = "root";
            String password = "261331";
            
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

    public static UserInfo read(String username, String password){
        UserInfo bean = null;

        try{
            Connection con = getCon();

            String q = "SELECT * FROM USER WHERE USER_NAME LIKE " + username;

            PreparedStatement ps=con.prepareStatement(q);
            ResultSet rs = ps.executeQuery();
            if(rs.next()){
                //bean = new UserInfo();
                int userID = rs.getInt("USER_ID");
                String userName = rs.getString("USER_NAME");
                String passWord = rs.getString("PASSWORD");
                String email = rs.getString("EMAIL");

            }
            con.close();
        }
        catch(Exception e){System.out.println(e);}

        return bean;
    }
    
    public String create(UserInfo newUser) {
        String s = "";
        
        try {
            Connection con = getCon();
            if (con == null) {
                System.err.println("Connection is null");
                return "Database connection failed";
            }
            
            // Use PreparedStatement to prevent SQL injection
            String q = "INSERT INTO USER (USER_NAME, PASSWORD, EMAIL) VALUES (?, ?, ?)";
            try (PreparedStatement stmt = con.prepareStatement(q)) {
                stmt.setString(1, newUser.getUsername());
                stmt.setString(2, newUser.getPassword());
                stmt.setString(3, newUser.getEmail());
                
                System.out.println("Executing query: " + q);
                System.out.println("With values: " + newUser.getUsername() + ", " + newUser.getPassword() + ", " + newUser.getEmail());
                
                int rowsAffected = stmt.executeUpdate();
                if (rowsAffected > 0) {
                    s = "user creation success";
                } else {
                    s = "Failed to insert user";
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
