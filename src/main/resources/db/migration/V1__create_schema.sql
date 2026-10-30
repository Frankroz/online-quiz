CREATE TABLE users (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  username VARCHAR(50) NOT NULL UNIQUE,
  password_hash VARCHAR(255) NOT NULL,
  role VARCHAR(10) NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT chk_user_role CHECK (role IN ('TEACHER','STUDENT'))
) ENGINE=InnoDB;

CREATE TABLE quizzes (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  title VARCHAR(100) NOT NULL,
  subject VARCHAR(50) NOT NULL,
  description TEXT NOT NULL,
  teacher_id BIGINT NOT NULL,
  status VARCHAR(10) NOT NULL DEFAULT 'DRAFT',
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT fk_quiz_teacher FOREIGN KEY (teacher_id) REFERENCES users(id) ON DELETE RESTRICT,
  CONSTRAINT chk_quiz_status CHECK (status IN ('DRAFT','PUBLISHED')),
  INDEX idx_quiz_subject_status (subject, status),
  INDEX idx_quiz_teacher (teacher_id)
) ENGINE=InnoDB;

CREATE TABLE questions (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  quiz_id BIGINT NOT NULL,
  question_text TEXT NOT NULL,
  option_a VARCHAR(255) NOT NULL,
  option_b VARCHAR(255) NOT NULL,
  option_c VARCHAR(255) NOT NULL,
  option_d VARCHAR(255) NOT NULL,
  correct_option CHAR(1) NOT NULL,
  explanation TEXT NOT NULL,
  question_order TINYINT UNSIGNED NOT NULL,
  CONSTRAINT fk_question_quiz FOREIGN KEY (quiz_id) REFERENCES quizzes(id) ON DELETE CASCADE,
  CONSTRAINT chk_correct_option CHECK (correct_option IN ('A','B','C','D')),
  CONSTRAINT chk_question_order CHECK (question_order BETWEEN 1 AND 10),
  CONSTRAINT uq_quiz_question_order UNIQUE (quiz_id, question_order)
) ENGINE=InnoDB;

CREATE TABLE quiz_attempts (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  student_id BIGINT NOT NULL,
  quiz_id BIGINT NOT NULL,
  score INT UNSIGNED NOT NULL DEFAULT 0,
  total_questions INT UNSIGNED NOT NULL DEFAULT 10,
  submitted_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_attempt_student FOREIGN KEY (student_id) REFERENCES users(id) ON DELETE RESTRICT,
  CONSTRAINT fk_attempt_quiz FOREIGN KEY (quiz_id) REFERENCES quizzes(id) ON DELETE RESTRICT,
  CONSTRAINT chk_attempt_score CHECK (score <= total_questions),
  CONSTRAINT chk_attempt_total CHECK (total_questions = 10),
  INDEX idx_attempt_student_date (student_id, submitted_at),
  INDEX idx_attempt_quiz (quiz_id)
) ENGINE=InnoDB;

CREATE TABLE attempt_answers (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  attempt_id BIGINT NOT NULL,
  question_id BIGINT NOT NULL,
  selected_option CHAR(1) NOT NULL,
  is_correct BOOLEAN NOT NULL,
  CONSTRAINT fk_answer_attempt FOREIGN KEY (attempt_id) REFERENCES quiz_attempts(id) ON DELETE CASCADE,
  CONSTRAINT fk_answer_question FOREIGN KEY (question_id) REFERENCES questions(id) ON DELETE RESTRICT,
  CONSTRAINT chk_selected_option CHECK (selected_option IN ('A','B','C','D')),
  CONSTRAINT uq_attempt_question UNIQUE (attempt_id, question_id)
) ENGINE=InnoDB;
