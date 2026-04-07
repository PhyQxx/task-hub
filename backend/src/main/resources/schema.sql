-- 任务舱 数据库 DDL
-- 数据库名: task_hub
-- 字符集: utf8mb4

CREATE DATABASE IF NOT EXISTS task_hub DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE task_hub;

-- 1. 项目表
CREATE TABLE IF NOT EXISTS projects (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    project_id      VARCHAR(64) NOT NULL UNIQUE COMMENT '项目ID',
    name            VARCHAR(128) NOT NULL COMMENT '项目名称',
    description     TEXT COMMENT '项目描述',
    owner_id        VARCHAR(64) NOT NULL COMMENT '项目负责人ID',
    start_date      DATE COMMENT '项目开始日期',
    end_date        DATE COMMENT '项目截止日期',
    status          VARCHAR(32) DEFAULT 'planning' COMMENT 'planning/active/completed/archived',
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_owner (owner_id),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='项目表';

-- 2. 项目成员关联表
CREATE TABLE IF NOT EXISTS project_members (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    project_id      VARCHAR(64) NOT NULL COMMENT '项目ID',
    member_id       VARCHAR(64) NOT NULL COMMENT '成员ID',
    role            VARCHAR(32) DEFAULT 'member' COMMENT 'owner/member/viewer',
    joined_at       DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_project_member (project_id, member_id),
    INDEX idx_project (project_id),
    INDEX idx_member (member_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='项目成员关联表';

-- 3. 成员表
CREATE TABLE IF NOT EXISTS members (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    member_id       VARCHAR(64) NOT NULL UNIQUE COMMENT '成员ID',
    nickname        VARCHAR(64) NOT NULL COMMENT '成员昵称',
    role            VARCHAR(32) NOT NULL COMMENT '角色',
    skills          JSON COMMENT '技能标签',
    weekly_capacity FLOAT DEFAULT 40.0 COMMENT '周标准工时',
    is_active       TINYINT DEFAULT 1 COMMENT '是否在职',
    avatar          VARCHAR(256) COMMENT '头像URL',
    email           VARCHAR(128) COMMENT '邮箱',
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_role (role),
    INDEX idx_is_active (is_active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='成员表';

-- 4. 任务表
CREATE TABLE IF NOT EXISTS tasks (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    task_id         VARCHAR(64) NOT NULL UNIQUE COMMENT '任务ID',
    title           VARCHAR(256) NOT NULL COMMENT '任务标题',
    description     TEXT COMMENT '任务描述',
    project_id      VARCHAR(64) NOT NULL COMMENT '所属项目ID',
    assignee_id     VARCHAR(64) COMMENT '负责人ID',
    start_date      DATE COMMENT '开始日期',
    end_date        DATE COMMENT '结束日期',
    duration        INT COMMENT '持续天数',
    estimated_hours FLOAT COMMENT '预估工时',
    actual_hours    FLOAT DEFAULT 0 COMMENT '实际工时',
    progress        INT DEFAULT 0 COMMENT '完成百分比',
    status          VARCHAR(32) DEFAULT 'pending' COMMENT 'pending/in_progress/completed/blocked',
    priority        VARCHAR(8) DEFAULT 'P2' COMMENT 'P0/P1/P2/P3',
    blocked_reason  VARCHAR(512) COMMENT '阻塞原因',
    blocked_at      DATETIME COMMENT '阻塞时间',
    is_milestone    TINYINT DEFAULT 0 COMMENT '是否里程碑',
    milestone_date  DATE COMMENT '里程碑日期',
    tags            JSON COMMENT '标签',
    created_by      VARCHAR(64) NOT NULL COMMENT '创建人',
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_project (project_id),
    INDEX idx_assignee (assignee_id),
    INDEX idx_status (status),
    INDEX idx_priority (priority),
    INDEX idx_start_date (start_date),
    INDEX idx_end_date (end_date),
    INDEX idx_is_milestone (is_milestone),
    INDEX idx_project_status_assignee (project_id, status, assignee_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='任务表';

-- 5. 任务依赖关系表
CREATE TABLE IF NOT EXISTS task_dependencies (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    task_id         VARCHAR(64) NOT NULL COMMENT '任务ID（后续任务）',
    depends_on      VARCHAR(64) NOT NULL COMMENT '依赖的任务ID（前置任务）',
    dependency_type VARCHAR(16) DEFAULT 'FS' COMMENT 'FS/SS/FF',
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_dependency (task_id, depends_on),
    INDEX idx_task (task_id),
    INDEX idx_depends_on (depends_on)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='任务依赖关系表';

-- 6. 每日工作日志表
CREATE TABLE IF NOT EXISTS task_work_logs (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    log_id          VARCHAR(64) NOT NULL UNIQUE COMMENT '日志ID',
    task_id         VARCHAR(64) NOT NULL COMMENT '关联任务ID',
    user_id         VARCHAR(64) NOT NULL COMMENT '填报人ID',
    log_date        DATE NOT NULL COMMENT '填报日期',
    today_done      TEXT COMMENT '今日完成',
    tomorrow_plan   TEXT COMMENT '明日计划',
    current_status  VARCHAR(16) DEFAULT '正常' COMMENT '正常/有风险/已阻塞',
    blocked_reason  VARCHAR(512) COMMENT '阻塞原因',
    hours_spent     FLOAT DEFAULT 0 COMMENT '花费工时',
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_task_date (task_id, log_date),
    INDEX idx_task (task_id),
    INDEX idx_user (user_id),
    INDEX idx_log_date (log_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工作日志表';

-- 7. 里程碑表
CREATE TABLE IF NOT EXISTS milestones (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    milestone_id    VARCHAR(64) NOT NULL UNIQUE COMMENT '里程碑ID',
    project_id      VARCHAR(64) NOT NULL COMMENT '所属项目ID',
    name            VARCHAR(128) NOT NULL COMMENT '里程碑名称',
    target_date     DATE NOT NULL COMMENT '目标日期',
    description     TEXT COMMENT '里程碑说明',
    color           VARCHAR(16) DEFAULT '#FFD700' COMMENT '颜色',
    created_by      VARCHAR(64) NOT NULL COMMENT '创建人',
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_project (project_id),
    INDEX idx_target_date (target_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='里程碑表';

-- 8. 任务操作历史表
CREATE TABLE IF NOT EXISTS task_history (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    task_id         VARCHAR(64) NOT NULL COMMENT '任务ID',
    operator        VARCHAR(64) NOT NULL COMMENT '操作人',
    action          VARCHAR(32) NOT NULL COMMENT '操作类型',
    detail          JSON COMMENT '变更详情',
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_task_id (task_id),
    INDEX idx_created_at (created_at),
    INDEX idx_task_time (task_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='任务操作历史表';
