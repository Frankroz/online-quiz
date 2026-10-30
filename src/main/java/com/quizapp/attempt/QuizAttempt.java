package com.quizapp.attempt;

import com.quizapp.quiz.Quiz;
import com.quizapp.user.User;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "quiz_attempts")
public class QuizAttempt {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "student_id", nullable = false) private User student;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "quiz_id", nullable = false) private Quiz quiz;
    @Column(nullable = false) private int score;
    @Column(name = "total_questions", nullable = false) private int totalQuestions;
    @Column(name = "submitted_at", insertable = false, updatable = false) private LocalDateTime submittedAt;
    protected QuizAttempt() {}
    public QuizAttempt(User student, Quiz quiz, int score, int totalQuestions) { this.student=student; this.quiz=quiz; this.score=score; this.totalQuestions = quiz.getQuestions().size();}
    public Long getId() { return id; }
    public User getStudent() { return student; }
    public Quiz getQuiz() { return quiz; }
    public int getScore() { return score; }
    public int getTotalQuestions() { return totalQuestions; }
    public LocalDateTime getSubmittedAt() { return submittedAt; }
}
