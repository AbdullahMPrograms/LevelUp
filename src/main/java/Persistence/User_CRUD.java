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
    
    private static Connection getCon(){
            Connection con = null;
            try
            {
                Class.forName("com.mysql.jdbc.Driver");
                con=DriverManager.getConnection("jdbc:mysql://localhost:3306/LevelUp?autoReconnect=true&useSSL=false","root","student");
                System.out.println("Connection established.");
            }
            catch(Exception e)
            {
                System.out.println(e);
            }
            return con;
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
        
        public String create(UserInfo newUser){
        
            String s = "";
        
            try{
                Connection con = getCon();
                Statement stmt = con.createStatement();
                String q = "INSERT INTO USER (USER_NAME, PASSWORD, EMAIL) VALUES ('" + newUser.getUsername() + "', '" + newUser.getPassword() + "', '" + newUser.getEmail() + "')";
                stmt.executeUpdate(q);
                s = "user creation success";
                con.close();
            }catch (SQLException e)
            {
                System.out.println(e);
            }
            return s;
        }
        
    
}
