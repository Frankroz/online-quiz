package com.quizapp.quiz;

public record StudentQuizResult(
        String studentName,
        long attempts,
        int highestScore,
        int totalQuestions,
        int percentage,
        boolean passed
        ) {}
