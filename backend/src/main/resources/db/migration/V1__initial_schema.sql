CREATE TABLE account_types (
    code VARCHAR(32) NOT NULL,
    display_name VARCHAR(64) NOT NULL,
    PRIMARY KEY (code),
    UNIQUE KEY uq_account_types_display_name (display_name)
) ENGINE=InnoDB;

INSERT INTO account_types (code, display_name) VALUES
    ('STUDENT', 'Student'),
    ('PROFESSIONAL', 'Professional'),
    ('FREELANCER', 'Freelancer'),
    ('MENTOR', 'Mentor'),
    ('ORGANIZATION', 'Organization');

CREATE TABLE users (
    id BIGINT NOT NULL AUTO_INCREMENT,
    email VARCHAR(254) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uq_users_email (email)
) ENGINE=InnoDB;

CREATE TABLE user_account_types (
    user_id BIGINT NOT NULL,
    account_type_code VARCHAR(32) NOT NULL,
    assigned_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (user_id, account_type_code),
    KEY ix_user_account_types_type (account_type_code, user_id),
    CONSTRAINT fk_user_account_types_user
        FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_user_account_types_type
        FOREIGN KEY (account_type_code) REFERENCES account_types (code) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB;

CREATE TABLE profiles (
    user_id BIGINT NOT NULL,
    display_name VARCHAR(120) NOT NULL,
    bio VARCHAR(1000) NULL,
    experience_level VARCHAR(40) NULL,
    goals VARCHAR(1000) NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (user_id),
    CONSTRAINT fk_profiles_user
        FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB;

CREATE TABLE availability_options (
    code VARCHAR(32) NOT NULL,
    display_name VARCHAR(64) NOT NULL,
    PRIMARY KEY (code),
    UNIQUE KEY uq_availability_options_display_name (display_name)
) ENGINE=InnoDB;

INSERT INTO availability_options (code, display_name) VALUES
    ('FLEXIBLE', 'Flexible'),
    ('WEEKDAYS', 'Weekdays'),
    ('EVENINGS', 'Evenings'),
    ('WEEKENDS', 'Weekends');

CREATE TABLE profile_availability (
    user_id BIGINT NOT NULL,
    availability_code VARCHAR(32) NOT NULL,
    PRIMARY KEY (user_id, availability_code),
    KEY ix_profile_availability_option (availability_code, user_id),
    CONSTRAINT fk_profile_availability_profile
        FOREIGN KEY (user_id) REFERENCES profiles (user_id) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_profile_availability_option
        FOREIGN KEY (availability_code) REFERENCES availability_options (code) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB;

CREATE TABLE skills (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uq_skills_name (name)
) ENGINE=InnoDB;

CREATE TABLE user_skills (
    user_id BIGINT NOT NULL,
    skill_id BIGINT NOT NULL,
    proficiency_level VARCHAR(32) NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (user_id, skill_id),
    KEY ix_user_skills_skill (skill_id, user_id),
    CONSTRAINT fk_user_skills_user
        FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_user_skills_skill
        FOREIGN KEY (skill_id) REFERENCES skills (id) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB;

CREATE TABLE interests (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uq_interests_name (name)
) ENGINE=InnoDB;

CREATE TABLE user_interests (
    user_id BIGINT NOT NULL,
    interest_id BIGINT NOT NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (user_id, interest_id),
    KEY ix_user_interests_interest (interest_id, user_id),
    CONSTRAINT fk_user_interests_user
        FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_user_interests_interest
        FOREIGN KEY (interest_id) REFERENCES interests (id) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB;

CREATE TABLE circles (
    id BIGINT NOT NULL AUTO_INCREMENT,
    created_by BIGINT NULL,
    name VARCHAR(120) NOT NULL,
    purpose VARCHAR(40) NOT NULL,
    description VARCHAR(2000) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    KEY ix_circles_creator_created (created_by, created_at),
    CONSTRAINT fk_circles_creator
        FOREIGN KEY (created_by) REFERENCES users (id) ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB;

CREATE TABLE circle_memberships (
    circle_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    membership_role VARCHAR(24) NOT NULL DEFAULT 'MEMBER',
    membership_status VARCHAR(24) NOT NULL DEFAULT 'ACTIVE',
    joined_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (circle_id, user_id),
    KEY ix_circle_memberships_user_status (user_id, membership_status),
    CONSTRAINT chk_circle_membership_status
        CHECK (membership_status IN ('PENDING', 'ACTIVE', 'LEFT')),
    CONSTRAINT fk_circle_memberships_circle
        FOREIGN KEY (circle_id) REFERENCES circles (id) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_circle_memberships_user
        FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB;

CREATE TABLE projects (
    id BIGINT NOT NULL AUTO_INCREMENT,
    circle_id BIGINT NULL,
    created_by BIGINT NULL,
    name VARCHAR(160) NOT NULL,
    description VARCHAR(3000) NOT NULL,
    project_status VARCHAR(24) NOT NULL DEFAULT 'PLANNING',
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    KEY ix_projects_circle_status (circle_id, project_status),
    KEY ix_projects_creator_created (created_by, created_at),
    CONSTRAINT chk_project_status
        CHECK (project_status IN ('PLANNING', 'ACTIVE', 'COMPLETED', 'ARCHIVED')),
    CONSTRAINT fk_projects_circle
        FOREIGN KEY (circle_id) REFERENCES circles (id) ON DELETE SET NULL ON UPDATE CASCADE,
    CONSTRAINT fk_projects_creator
        FOREIGN KEY (created_by) REFERENCES users (id) ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB;

CREATE TABLE project_memberships (
    project_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    membership_role VARCHAR(24) NOT NULL DEFAULT 'MEMBER',
    joined_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (project_id, user_id),
    KEY ix_project_memberships_user (user_id, project_id),
    CONSTRAINT fk_project_memberships_project
        FOREIGN KEY (project_id) REFERENCES projects (id) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_project_memberships_user
        FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB;

CREATE TABLE tasks (
    id BIGINT NOT NULL AUTO_INCREMENT,
    project_id BIGINT NOT NULL,
    created_by BIGINT NULL,
    assignee_user_id BIGINT NULL,
    title VARCHAR(200) NOT NULL,
    description VARCHAR(3000) NULL,
    task_status VARCHAR(24) NOT NULL DEFAULT 'TODO',
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    KEY ix_tasks_project_status (project_id, task_status),
    KEY ix_tasks_assignee_status (assignee_user_id, task_status),
    CONSTRAINT chk_task_status
        CHECK (task_status IN ('TODO', 'IN_PROGRESS', 'DONE')),
    CONSTRAINT fk_tasks_project
        FOREIGN KEY (project_id) REFERENCES projects (id) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_tasks_creator
        FOREIGN KEY (created_by) REFERENCES users (id) ON DELETE SET NULL ON UPDATE CASCADE,
    CONSTRAINT fk_tasks_assignee
        FOREIGN KEY (assignee_user_id) REFERENCES users (id) ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB;
