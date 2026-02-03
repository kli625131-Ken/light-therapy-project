-- 创建实验分组与量表模板的多对多连接表
CREATE TABLE experiment_group_survey_templates (
    experiment_group_id BIGINT NOT NULL,
    survey_template_id BIGINT NOT NULL,
    PRIMARY KEY (experiment_group_id, survey_template_id),
    CONSTRAINT fk_experiment_group FOREIGN KEY (experiment_group_id) REFERENCES experiment_group(id) ON DELETE CASCADE,
    CONSTRAINT fk_survey_template FOREIGN KEY (survey_template_id) REFERENCES survey_template(id) ON DELETE CASCADE
);

-- 为现有数据添加默认值（如果需要）
-- 注意：如果之前有使用survey_template_id的记录，需要将其迁移到新的连接表中
-- 这里暂时不执行迁移，因为需要根据实际数据情况处理