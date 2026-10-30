package com.quizapp.attempt;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QuizAttemptRepository
        extends JpaRepository<QuizAttempt, Long> {

    List<QuizAttempt> findByQuizTeacherUsernameOrderBySubmittedAtDesc(
            String username);

    @EntityGraph(attributePaths = {"quiz", "quiz.questions", "student"})
    Optional<QuizAttempt> findByIdAndStudentUsername(
            Long id, String username);

    @EntityGraph(attributePaths = {"quiz", "student"})
    List<QuizAttempt> findByStudentUsernameOrderBySubmittedAtDesc(
            String username);

    @EntityGraph(attributePaths = {"student"})
    List<QuizAttempt> findByQuiz_Id(Long quizId);

    long countByStudent_UsernameAndQuiz_Id(
            String username, Long quizId);
}
