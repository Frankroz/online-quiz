package com.quizapp.attempt;

import com.quizapp.quiz.Question;
import jakarta.persistence.*;

@Entity
@Table(name = "attempt_answers")
public class AttemptAnswer {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "attempt_id", nullable = false) private QuizAttempt attempt;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "question_id", nullable = false) private Question question;
    @Column(name = "selected_option", nullable = false, length = 1) private String selectedOption;
    @Column(name = "is_correct", nullable = false) private boolean correct;
    protected AttemptAnswer() {}
    public Long getId() { return id; }
    public QuizAttempt getAttempt() { return attempt; }
    public Question getQuestion() { return question; }
    public String getSelectedOption() { return selectedOption; }
    public boolean isCorrect() { return correct; }
    public AttemptAnswer(QuizAttempt attempt, Question question, String selectedOption, boolean correct) {
        this.attempt=attempt; this.question=question; this.selectedOption=selectedOption; this.correct=correct;
    }
}
