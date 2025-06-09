package com.mailscheduler.infrastructure.persistence.repository;

import com.mailscheduler.domain.model.common.base.EntityId;
import com.mailscheduler.domain.model.common.base.NoMetadata;
import com.mailscheduler.domain.model.schedule.FollowUpPlan;
import com.mailscheduler.domain.model.schedule.FollowUpPlanType;
import com.mailscheduler.domain.repository.FollowUpPlanRepository;
import com.mailscheduler.infrastructure.persistence.database.DatabaseFacade;
import com.mailscheduler.infrastructure.persistence.entity.FollowUpPlanEntity;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

/**
 * SQL implementation of the FollowUpPlanRepository.
 * Handles persistence of FollowUpPlan entities in a relational database.
 */
public class FollowUpPlanSqlRepository extends AbstractSqlRepository<FollowUpPlan, NoMetadata, FollowUpPlanEntity>
        implements FollowUpPlanRepository {

    public FollowUpPlanSqlRepository(DatabaseFacade db) {
        super(db);
    }

    @Override
    protected String tableName() {
        return "followup_plans";
    }

    @Override
    protected FollowUpPlanEntity mapResultSetToEntity(ResultSet rs) throws SQLException {
        return new FollowUpPlanEntity(
                rs.getLong("id"),
                rs.getString("followup_plan_type")
        );
    }

    @Override
    protected FollowUpPlanEntity toTableEntity(FollowUpPlan domainEntity, NoMetadata metadata) {
        return new FollowUpPlanEntity(
                domainEntity.getId() != null ? domainEntity.getId().value() : null,
                domainEntity.getPlanType() != null ? domainEntity.getPlanType().toString() : null
        );
    }

    @Override
    protected FollowUpPlan toDomainEntity(FollowUpPlanEntity tableEntity) {
        return new FollowUpPlan.Builder()
                .setId(EntityId.of(tableEntity.getId()))
                .setFollowUpPlanType(FollowUpPlanType.valueOf(tableEntity.getPlanType()))
                .build();
    }

    @Override
    protected NoMetadata toMetadata(FollowUpPlanEntity tableEntity) {
        return NoMetadata.getInstance();
    }

    @Override
    protected String createInsertSql() {
        return String.format(
                """
                INSERT INTO %s (followup_plan_type) VALUES (?) RETURNING *
                """, tableName());
    }

    @Override
    protected String createUpdateSql() {
        return String.format(
                """
                UPDATE %s SET followup_plan_type = ? WHERE id = ?
                """,
                tableName()
        );
    }

    @Override
    protected void setStatementParameters(PreparedStatement stmt, FollowUpPlanEntity entity) throws SQLException {
        stmt.setString(1, entity.getPlanType());

        // For updates, we need to set the ID as the second parameter
        if (entity.getId() != null) {
            stmt.setLong(2, entity.getId());
        }
    }

    public FollowUpPlan save(FollowUpPlan entity) {
        return super.saveWithMetadata(entity, NoMetadata.getInstance()).entity();
    }

    @Override
    public Optional<FollowUpPlan> findByFollowUpPlanType(FollowUpPlanType followUpPlanType) {
        System.out.println("findByFollowUpPlanType");
        return Optional.empty();
    }

    @Override
    public FollowUpPlan findAllDefaults() {
        System.out.println("findAllDefaults");
        return null;
    }
}
