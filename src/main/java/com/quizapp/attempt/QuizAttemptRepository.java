package com.quizapp.attempt;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QuizAttemptRepository extends JpaRepository<QuizAttempt, Long> {
    List<QuizAttempt> findByStudentUsernameOrderBySubmittedAtDesc(String username);
    List<QuizAttempt> findByQuizTeacherUsernameOrderBySubmittedAtDesc(String username);
    java.util.Optional<QuizAttempt> findByIdAndStudentUsername(Long id, String username);
}
