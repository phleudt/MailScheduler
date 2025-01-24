package com.mailscheduler.database.dao;

import com.mailscheduler.database.DatabaseManager;
import com.mailscheduler.database.entities.FollowUpScheduleEntity;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class FollowUpScheduleDao extends GenericDao<FollowUpScheduleEntity> {
    private static final Logger LOGGER = Logger.getLogger(FollowUpScheduleDao.class.getName());

    public FollowUpScheduleDao(DatabaseManager databaseManager) {
        super(databaseManager);
    }

    @Override
    protected FollowUpScheduleEntity mapResultSetToEntity(ResultSet resultSet) throws SQLException {
        return new FollowUpScheduleEntity(
                resultSet.getInt("id"),
                resultSet.getInt("followup_number"),
                resultSet.getInt("interval_days"),
                resultSet.getInt("schedule_id"),
                resultSet.getString("schedule_category")
        );
    }

    public int insertFollowUpSchedule(FollowUpScheduleEntity followUpSchedule) throws SQLException {
        LOGGER.info("Inserting FollowUpSchedule into database");
        String query = "INSERT INTO FollowUpSchedules (followup_number, interval_days, schedule_id, schedule_category) VALUES (?, ?, ?, ?)";
        return insert(query, followUpSchedule.getFollowup_number(), followUpSchedule.getInterval_days(), followUpSchedule.getSchedule_id(), followUpSchedule.getSchedule_category());
    }

    public boolean updateFollowUpScheduleById(int id, FollowUpScheduleEntity followUpSchedule) throws SQLException {
        LOGGER.info("Updating FollowUpSchedule with ID: " + id);
        String query = "UPDATE FollowUpSchedules SET followup_number = ?, interval_days = ?, schedule_id = ?, schedule_category = ? WHERE id = ?";
        return update(query, followUpSchedule.getFollowup_number(), followUpSchedule.getInterval_days(), followUpSchedule.getSchedule_id(), followUpSchedule.getSchedule_category(), id);
    }

    public boolean deleteFollowUpScheduleById(int id) throws SQLException {
        LOGGER.info("Deleting FollowUpSchedule with ID: " + id);
        String query = "DELETE FROM FollowUpSchedules WHERE id = ?";
        return delete(query, id);
    }

    public FollowUpScheduleEntity findFollowUpScheduleById(int id) throws SQLException {
        LOGGER.info("Finding FollowUpSchedule with ID: " + id);
        String query = "SELECT * FROM FollowUpSchedules WHERE id = ?";
        return findById(query, id);
    }

    public List<Integer> getUniqueSchedules() throws SQLException {
        LOGGER.info("Getting unique schedule IDs");
        String query = "SELECT DISTINCT schedule_id FROM FollowUpSchedules";
        return executeQueryWithListResult(query, resultSet -> resultSet.getInt("schedule_id"));
    }

    public List<FollowUpScheduleEntity> getAllFollowUpSchedules() throws SQLException {
        LOGGER.info("Getting all FollowUpSchedules");
        String query = "SELECT * FROM FollowUpSchedules";
        return findAll(query);
    }
}
