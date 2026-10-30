package com.quizapp.quiz;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QuizRepository extends JpaRepository<Quiz, Long> {

    @EntityGraph(attributePaths = {"teacher"})
    List<Quiz> findByStatusOrderByCreatedAtDesc(String status);

    @EntityGraph(attributePaths = {"teacher"})
    List<Quiz> findByStatusAndSubjectContainingIgnoreCaseOrderByCreatedAtDesc(
            String status, String subject);

    @EntityGraph(attributePaths = {"questions"})
    List<Quiz> findByTeacherUsernameOrderByIdDesc(String username);

    @EntityGraph(attributePaths = {"teacher", "questions"})
    Optional<Quiz> findByIdAndTeacherUsername(Long id, String username);

    @EntityGraph(attributePaths = {"questions", "teacher"})
    Optional<Quiz> findWithQuestionsById(Long id);
}