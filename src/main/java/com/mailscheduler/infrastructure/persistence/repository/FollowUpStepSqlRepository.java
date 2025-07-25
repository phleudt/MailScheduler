package com.mailscheduler.infrastructure.persistence.repository;

import com.mailscheduler.domain.model.common.base.EntityId;
import com.mailscheduler.domain.model.schedule.FollowUpPlan;
import com.mailscheduler.domain.model.schedule.FollowUpStep;
import com.mailscheduler.domain.model.schedule.FollowUpStepMetadata;
import com.mailscheduler.domain.model.common.base.EntityData;
import com.mailscheduler.domain.repository.FollowUpStepRepository;
import com.mailscheduler.infrastructure.persistence.database.DatabaseFacade;
import com.mailscheduler.infrastructure.persistence.entity.FollowUpStepEntity;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * SQL implementation of the FollowUpStepRepository.
 * Handles persistence of FollowUpStep entities in a relational database.
 */
public class FollowUpStepSqlRepository extends AbstractSqlRepository<FollowUpStep, FollowUpStepMetadata, FollowUpStepEntity>
implements FollowUpStepRepository {

    public FollowUpStepSqlRepository(DatabaseFacade db) {
        super(db);
    }

    @Override
    protected String tableName() {
        return "followup_plan_steps";
    }

    @Override
    protected FollowUpStepEntity mapResultSetToEntity(ResultSet rs) throws SQLException {
        return new FollowUpStepEntity(
                rs.getLong("id"),
                rs.getLong("id"),
                rs.getInt("step_number"),
                rs.getInt("waiting_period"),
                rs.getLong("template_id")
        );
    }

    @Override
    protected FollowUpStepEntity toTableEntity(FollowUpStep domainEntity, FollowUpStepMetadata metadata) {
        return new FollowUpStepEntity(
                domainEntity.getId() != null ? domainEntity.getId().value() : null,
                metadata.planId().value(),
                domainEntity.getStepNumber(),
                domainEntity.getWaitPeriod(),
                metadata.templateId() != null ? metadata.templateId().value() : null
        );
    }

    @Override
    protected FollowUpStep toDomainEntity(FollowUpStepEntity tableEntity) {
        return new FollowUpStep(
                tableEntity.getStepNumber(),
                tableEntity.getWaitingPeriod()
        );
    }

    @Override
    protected FollowUpStepMetadata toMetadata(FollowUpStepEntity tableEntity) {
        return new FollowUpStepMetadata(
                EntityId.of(tableEntity.getPlanId()),
                EntityId.of(tableEntity.getTemplateId())
        );
    }

    @Override
    protected String createInsertSql() {
        return String.format(
                """
                INSERT INTO %s (plan_id, step_number, waiting_period, template_id)
                VALUES (?, ?, ?, ?) RETURNING *
                """, tableName()
        );
    }

    @Override
    protected String createUpdateSql() {
        return String.format(
                """
                UPDATE %s SET
                    plan_id = ?, step_number = ?, waiting_period = ?, template_id = ?
                    WHERE id = ?
                """, tableName()
        );
    }

    @Override
    protected void setStatementParameters(PreparedStatement stmt, FollowUpStepEntity entity) throws SQLException {
        stmt.setLong(1, entity.getPlanId());
        stmt.setInt(2, entity.getStepNumber());
        stmt.setInt(3, entity.getWaitingPeriod());
        stmt.setObject(4, entity.getTemplateId());

        // For updates, we need to set the ID as the 5th parameter
        if (entity.getId() != null) {
            stmt.setLong(5, entity.getId());
        }

    }

    @Override
    public List<EntityData<FollowUpStep, FollowUpStepMetadata>> findByPlanIdOrderByFollowUpNumberAsc(EntityId<FollowUpPlan> planId) {
        String sql = String.format("SELECT * FROM %s WHERE plan_id = ? ORDER BY step_number ASC", tableName());
        List<EntityData<FollowUpStep, FollowUpStepMetadata>> result = new ArrayList<>();

        try (Connection conn = db.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, planId.value());

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    FollowUpStepEntity entity = mapResultSetToEntity(rs);
                    result.add(EntityData.of(
                            toDomainEntity(entity),
                            toMetadata(entity)
                    ));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find follow-up steps by plan ID", e);
        }
        return result;
    }

    @Override
    public Optional<EntityData<FollowUpStep, FollowUpStepMetadata>> findByPlanIdAndFollowupNumber(EntityId<FollowUpPlan> planId, int followupNumber) {
        String sql = String.format("SELECT * FROM %s WHERE plan_id = ? AND step_number = ?", tableName());

        try (Connection conn = db.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, planId.value());
            stmt.setInt(2, followupNumber);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    FollowUpStepEntity entity = mapResultSetToEntity(rs);
                    return Optional.of(EntityData.of(
                            toDomainEntity(entity),
                            toMetadata(entity)
                    ));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find follow-up step by plan ID and step number", e);
        }
        return Optional.empty();
    }

    @Override
    public void deleteByPlanId(EntityId<FollowUpPlan> planId) {
        System.out.println("deleteByPlanId");
    }
}
