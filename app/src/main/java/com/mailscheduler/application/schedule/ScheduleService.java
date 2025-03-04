package com.mailscheduler.application.schedule;

import com.mailscheduler.infrastructure.persistence.database.DatabaseManager;
import com.mailscheduler.common.exception.service.ScheduleServiceException;
import com.mailscheduler.infrastructure.persistence.exception.RepositoryException;
import com.mailscheduler.domain.schedule.Schedule;
import com.mailscheduler.domain.schedule.ScheduleCategory;
import com.mailscheduler.domain.schedule.ScheduleEntry;
import com.mailscheduler.infrastructure.persistence.repository.sqlite.SQLiteScheduleRepository;
import com.mailscheduler.interfaces.cli.AbstractUserConsoleInteractionService;

import java.util.*;

public class ScheduleService {
    private final SQLiteScheduleRepository scheduleRepository;
    private final UserConsoleInteractionService userConsoleInteractionService;

    public ScheduleService() {
        this.scheduleRepository = new SQLiteScheduleRepository(DatabaseManager.getInstance());
        this.userConsoleInteractionService = new UserConsoleInteractionService();
    }

    public void manageSchedules(int numberOfFollowUps) throws ScheduleServiceException {
        try {
            Optional<List<Schedule>> uniqueSchedulesList = scheduleRepository.getUniqueSchedules();
            if (uniqueSchedulesList.isPresent()) {
                int uniqueSchedules = uniqueSchedulesList.get().size();
                if (uniqueSchedules == 0) {
                    UserScheduleSelection userScheduleSelection = userConsoleInteractionService.getUserScheduleSelection(numberOfFollowUps);
                    Schedule schedule = createNewSchedule(userScheduleSelection.numberOfFollowUps(), userScheduleSelection.daysBetweenFollowUps());
                    scheduleRepository.save(schedule);
                }
            }
        }  catch (RepositoryException e) {
            throw new ScheduleServiceException("Failed to get unique schedules: " + e.getMessage());
        }
    }

    private Schedule createNewSchedule(int numberOfFollowUps, List<Integer> daysBetweenFollowUps) {
        Schedule.Builder builder = new Schedule.Builder();
        int scheduleId = 1;
        builder.setScheduleId(scheduleId);
        for (int i = 0; i < numberOfFollowUps; i++) {
            ScheduleEntry entry = new ScheduleEntry.Builder()
                    .setId(i + 1) // TODO
                    .setNumber(i + 1)
                    .setIntervalDays(daysBetweenFollowUps.get(i))
                    .setScheduleId(scheduleId)
                    .setCategory(ScheduleCategory.DEFAULT_SCHEDULE)
                    .build();

            builder.addEntry(entry);
        }
        return builder.build();
    }

    private void insertSchedule(Schedule schedule) throws ScheduleServiceException {
        try {
            scheduleRepository.save(schedule);
        } catch (RepositoryException e) {
            throw new ScheduleServiceException("Failed to save schedule to database: " + e.getMessage(), e);

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
