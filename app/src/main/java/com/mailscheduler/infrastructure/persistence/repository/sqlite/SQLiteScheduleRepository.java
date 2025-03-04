package com.mailscheduler.infrastructure.persistence.repository.sqlite;

import com.mailscheduler.infrastructure.persistence.database.DatabaseManager;
import com.mailscheduler.infrastructure.persistence.entities.ScheduleEntity;
import com.mailscheduler.domain.schedule.Schedule;
import com.mailscheduler.domain.schedule.ScheduleEntry;
import com.mailscheduler.domain.schedule.ScheduleCategory;
import com.mailscheduler.infrastructure.persistence.exception.RepositoryException;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

import static java.util.stream.Collectors.toList;

public class SQLiteScheduleRepository extends AbstractSQLiteRepository<Schedule, List<ScheduleEntity>> {

    private static final Logger LOGGER = Logger.getLogger(SQLiteScheduleRepository.class.getName());

    private static final String INSERT_SCHEDULE_ENTRY =
            "INSERT INTO Schedules (followup_number, interval_days, schedule_id, schedule_category) " +
                    "VALUES (?, ?, ?, ?)";

    private static final String FIND_BY_SCHEDULE_ID =
            "SELECT * FROM Schedules WHERE schedule_id = ?";

    private static final String DELETE_SCHEDULE =
            "DELETE FROM Schedules WHERE schedule_id = ?";

    private static final String FIND_UNIQUE_SCHEDULES =
            "SELECT DISTINCT schedule_id FROM Schedules";

    public SQLiteScheduleRepository(DatabaseManager databaseManager) {
        super(databaseManager);
    }

    @Override
    protected List<ScheduleEntity> mapResultSetToEntity(ResultSet resultSet) throws SQLException {
        List<ScheduleEntity> entities = new ArrayList<>();
        do {
            entities.add(new ScheduleEntity(
                    resultSet.getInt("id"),
                    resultSet.getInt("followup_number"),
                    resultSet.getInt("interval_days"),
                    resultSet.getInt("schedule_id"),
                    resultSet.getString("schedule_category")
            ));
        } while (resultSet.next());
        return entities;
    }

    @Override
    protected Schedule mapToDomainEntity(List<ScheduleEntity> entities) {
        List<ScheduleEntry> entries = entities.stream()
                .map(entity -> new ScheduleEntry.Builder()
                                .setId(entity.getId())
                                .setNumber(entity.getFollowup_number())
                                .setIntervalDays(entity.getInterval_days())
                                .setScheduleId(entity.getSchedule_id())
                                .setCategory(ScheduleCategory.fromString(entity.getSchedule_category()))
                                .build()
                ).toList();

        return new Schedule.Builder()
                .setScheduleId(entities.get(0).getSchedule_id())
                .addEntries(entries)
                .build();
    }

    @Override
    protected List<ScheduleEntity> mapFromDomainEntity(Schedule domain) {
        return domain.getEntries().stream()
                .map(entry -> new ScheduleEntity(
                        entry.getId().value(),
                        entry.getNumber().value(),
                        entry.getIntervalDays().value(),
                        domain.getId().value(),
                        entry.getCategory().toString()
                )).toList();
    }

    @Override
    protected Object[] extractParameters(List<ScheduleEntity> entities, Object... additionalParams) {
        // TODO
        ScheduleEntity entity = entities.get(0);
        return new Object[] {
                entity.getFollowup_number(),
                entity.getInterval_days(),
                entity.getSchedule_id(),
                entity.getSchedule_category()
        };
    }

    @Override
    public Optional<Schedule> findById(int scheduleId) throws RepositoryException {
        try {
            List<List<ScheduleEntity>> entities = executeQueryForList(FIND_BY_SCHEDULE_ID, scheduleId); // TODO
            return entities.isEmpty() ?
                    Optional.empty() :
                    Optional.of(mapToDomainEntity(entities.get(0)));
        } catch (SQLException e) {
            throw new RepositoryException("Failed to find schedule by id: " + scheduleId, e);
        }
    }

    public Schedule save(Schedule schedule) throws RepositoryException {
        try {
            List<ScheduleEntity> entities = mapFromDomainEntity(schedule);
            for (ScheduleEntity entity : entities) {
                executeUpdate(INSERT_SCHEDULE_ENTRY, extractParameters(List.of(entity)));
            }
            return findById(schedule.getId().value()).orElseThrow();
        } catch (SQLException e) {
            throw new RepositoryException("Failed to save schedule", e);
        }
    }

    public void deleteSchedule(int scheduleId) throws RepositoryException {
        try {
            delete(DELETE_SCHEDULE, scheduleId);
        } catch (RepositoryException e) {
            throw new RepositoryException("Failed to delete schedule: " + scheduleId, e);
        }
    }

    public List<Integer> findAllScheduleIds() throws RepositoryException {
        try {
            return executeQueryForList(FIND_UNIQUE_SCHEDULES)
                    .stream()
                    .map(entities -> entities.get(0).getSchedule_id())
                    .collect(toList());
        } catch (SQLException e) {
            throw new RepositoryException("Failed to find all schedule IDs", e);
        }
    }

    public Optional<List<Schedule>> getUniqueSchedules() throws RepositoryException {
        try {
            String query = "SELECT DISTINCT id, followup_number, interval_days, schedule_id, schedule_category FROM Schedules"; // TODO
            return Optional.of(executeQueryForList(query).stream().map(this::mapToDomainEntity).toList());
    } catch (SQLException e) {
            throw new RepositoryException("Failed to get unique schedules");
        }
    }
}
