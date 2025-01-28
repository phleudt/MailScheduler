package com.mailscheduler.service;

import com.mailscheduler.database.DatabaseManager;
import com.mailscheduler.database.dao.FollowUpScheduleDao;
import com.mailscheduler.dto.FollowUpScheduleDto;
import com.mailscheduler.exception.service.FollowUpScheduleServiceException;
import com.mailscheduler.exception.MappingException;
import com.mailscheduler.mapper.EntityMapper;
import com.mailscheduler.model.Schedule;
import com.mailscheduler.model.ScheduleCategory;
import com.mailscheduler.model.ScheduleEntry;

import java.sql.SQLException;
import java.util.*;

public class ScheduleService {
    private final FollowUpScheduleDao followUpScheduleDao;
    private final UserConsoleInteractionService userConsoleInteractionService;

    public ScheduleService() {
        this.followUpScheduleDao = new FollowUpScheduleDao(DatabaseManager.getInstance());
        this.userConsoleInteractionService = new UserConsoleInteractionService();
    }

    public void manageSchedules(int numberOfFollowUps) throws FollowUpScheduleServiceException {
        try {
            int uniqueSchedules = followUpScheduleDao.getUniqueSchedules().size();
            if (uniqueSchedules == 0) {
                UserScheduleSelection userScheduleSelection = userConsoleInteractionService.getUserScheduleSelection(numberOfFollowUps);
                Schedule schedule = createNewSchedule(userScheduleSelection.numberOfFollowUps(), userScheduleSelection.daysBetweenFollowUps());
                insertSchedule(schedule);
            }
        }  catch (SQLException e) {
            throw new FollowUpScheduleServiceException("Failed to get unique schedules: " + e.getMessage());
        }
    }

    private Schedule createNewSchedule(int numberOfFollowUps, List<Integer> daysBetweenFollowUps) {
        Schedule.Builder builder = new Schedule.Builder();
        int scheduleId = 1;
        builder.setScheduleId(scheduleId);
        for (int i = 0; i < numberOfFollowUps; i++) {
            ScheduleEntry entry = new ScheduleEntry(i + 1, daysBetweenFollowUps.get(i), scheduleId, ScheduleCategory.DEFAULT_SCHEDULE);
            builder.addEntry(entry);
        }
        return builder.build();
    }

    private void insertSchedule(Schedule schedule) throws FollowUpScheduleServiceException {
        try {
            List<FollowUpScheduleDto> scheduleDtos = EntityMapper.toFollowUpScheduleDtos(schedule);
            for (FollowUpScheduleDto dto : scheduleDtos) {
                followUpScheduleDao.insertFollowUpSchedule(
                        EntityMapper.toFollowUpScheduleEntity(dto)
                );
            }
        } catch (MappingException | SQLException e) {
            throw new FollowUpScheduleServiceException("Failed to insert schedule into database: " + e.getMessage(), e);

        }
    }

    private void insertSchedule(List<FollowUpScheduleDto> dtos) throws SQLException, MappingException {
        for (FollowUpScheduleDto dto : dtos) {
            followUpScheduleDao.insertFollowUpSchedule(
                    EntityMapper.toFollowUpScheduleEntity(dto)
            );
        }
    }

    record UserScheduleSelection(int numberOfFollowUps, List<Integer> daysBetweenFollowUps) {}
}

class UserConsoleInteractionService extends AbstractUserConsoleInteractionService {

    public ScheduleService.UserScheduleSelection getUserScheduleSelection(int numberOfFollowUps) {
        List<Integer> daysBetweenFollowUps = retrieveDaysBetweenFollowUps(numberOfFollowUps);

        return new ScheduleService.UserScheduleSelection(numberOfFollowUps, daysBetweenFollowUps);
    }

    private List<Integer> retrieveDaysBetweenFollowUps(int numberOfFollowUps) {
        while (true) {
            List<Integer> choices = getValidatedMultipleInputs(
                    "Enter the number of days between each follow-up (comma-separated) (order matters):",
                    Integer.MAX_VALUE
            );

            if (choices.size() == numberOfFollowUps) {
                return choices;
            }
            System.out.println("Number of days between follow-ups must equal: " + numberOfFollowUps);
        }
    }
}
