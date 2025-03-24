package Business;

import Helper.GoalInfo;
import Persistence.GoalPersistence;
import java.util.List;

/**
 * Business logic for goal operations
 */
public class GoalBusiness {

    /**
     * Creates a new goal
     * 
     * @param newGoal goal information to create
     * @return Result message of goal creation attempt
     */
    public static String createGoal(GoalInfo newGoal) {
        System.out.println("Creating new goal: " + newGoal.getTitle());
        return GoalPersistence.create(newGoal);
    }
    
    /**
     * Retrieves all goals for a user
     * 
     * @param userID The user ID to get goals for
     * @return List of goals for the user
     */
    public static List<GoalInfo> getUserGoals(int userID) {
        System.out.println("Getting goals for user ID: " + userID);
        GoalPersistence model = new GoalPersistence();
        return model.getAllUserGoals(userID);
    }
}
