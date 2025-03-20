package Business;

import Helper.GoalInfo;
import Model.GoalModel;
import java.util.List;
import java.util.logging.Logger;

/**
 * Business logic for goal operations
 */
public class GoalBusiness {
    private static final Logger LOGGER = Logger.getLogger(GoalBusiness.class.getName());

    /**
     * Creates a new goal
     * 
     * @param newGoal goal information to create
     * @return Result message of goal creation attempt
     */
    public static String createGoal(GoalInfo newGoal) {
        LOGGER.info("Creating new goal: " + newGoal.getTitle());
        return GoalModel.create(newGoal);
    }
    
    /**
     * Retrieves all goals for a user
     * 
     * @param userID The user ID to get goals for
     * @return List of goals for the user
     */
    public static List<GoalInfo> getUserGoals(int userID) {
        LOGGER.info("Getting goals for user ID: " + userID);
        GoalModel model = new GoalModel();
        return model.getAllUserGoals(userID);
    }
}
