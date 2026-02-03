-- Add manual scheme for manual treatment sessions
INSERT INTO `scheme` (`id`, `name`, `description`, `status`) VALUES
(1, 'Manual Scheme', 'Scheme for manual treatment sessions', 'ACTIVE')
ON DUPLICATE KEY UPDATE
`name` = VALUES(`name`),
`description` = VALUES(`description`),
`status` = VALUES(`status`);

-- Add default stage for manual scheme
INSERT INTO `scheme_stage` (`id`, `scheme_id`, `stage_no`, `name`, `duration_minutes`, `status`) VALUES
(1, 1, 1, 'Manual Stage', 60, 'ACTIVE')
ON DUPLICATE KEY UPDATE
`scheme_id` = VALUES(`scheme_id`),
`stage_no` = VALUES(`stage_no`),
`name` = VALUES(`name`),
`duration_minutes` = VALUES(`duration_minutes`),
`status` = VALUES(`status`);