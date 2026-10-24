CREATE TABLE recommendation_logs (
    id BIGINT NOT NULL AUTO_INCREMENT,
    requester_user_id BIGINT NOT NULL,
    candidate_user_id BIGINT NOT NULL,
    score DECIMAL(6, 3) NOT NULL,
    explanation JSON NOT NULL,
    algorithm_version VARCHAR(40) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    KEY ix_recommendation_logs_requester_created (requester_user_id, created_at),
    KEY ix_recommendation_logs_candidate_created (candidate_user_id, created_at),
    CONSTRAINT chk_recommendation_score CHECK (score >= 0 AND score <= 100),
    CONSTRAINT fk_recommendation_logs_requester
        FOREIGN KEY (requester_user_id) REFERENCES users (id) ON DELETE CASCADE ON UPDATE RESTRICT,
    CONSTRAINT fk_recommendation_logs_candidate
        FOREIGN KEY (candidate_user_id) REFERENCES users (id) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE=InnoDB;

CREATE TRIGGER bi_recommendation_logs_distinct_users
BEFORE INSERT ON recommendation_logs
FOR EACH ROW
BEGIN
    IF NEW.requester_user_id = NEW.candidate_user_id THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'A requester cannot be recommended to themselves';
    END IF;
END;

CREATE TRIGGER bu_recommendation_logs_distinct_users
BEFORE UPDATE ON recommendation_logs
FOR EACH ROW
BEGIN
    IF NEW.requester_user_id = NEW.candidate_user_id THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'A requester cannot be recommended to themselves';
    END IF;
END;
