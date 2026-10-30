package com.quizapp.quiz;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QuizRepository extends JpaRepository<Quiz, Long> {
    List<Quiz> findByStatusOrderByCreatedAtDesc(String status);
    List<Quiz> findByStatusAndSubjectContainingIgnoreCaseOrderByCreatedAtDesc(String status, String subject);
    List<Quiz> findByTeacherUsernameOrderByIdDesc(String username);
    Optional<Quiz> findByIdAndTeacherUsername(Long id, String username);
    Optional<Quiz> findWithQuestionsById(Long id);
}
