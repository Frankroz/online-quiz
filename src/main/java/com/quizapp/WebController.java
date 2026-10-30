package com.quizapp;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.quizapp.attempt.AttemptAnswer;
import com.quizapp.attempt.AttemptAnswerRepository;
import com.quizapp.attempt.QuizAttempt;
import com.quizapp.attempt.QuizAttemptRepository;
import com.quizapp.quiz.Question;
import com.quizapp.quiz.Quiz;
import com.quizapp.quiz.QuizRepository;
import com.quizapp.quiz.StudentQuizResult;
import com.quizapp.user.User;
import com.quizapp.user.UserRepository;

import jakarta.transaction.Transactional;

@Controller
public class WebController {

    private final UserRepository users;
    private final QuizRepository quizzes;
    private final QuizAttemptRepository attempts;
    private final AttemptAnswerRepository answers;
    private final PasswordEncoder encoder;

    public WebController(UserRepository users, QuizRepository quizzes, QuizAttemptRepository attempts,
            AttemptAnswerRepository answers, PasswordEncoder encoder) {
        this.users = users;
        this.quizzes = quizzes;
        this.attempts = attempts;
        this.answers = answers;
        this.encoder = encoder;
    }

    @GetMapping("/")
    public String home(Authentication auth, Model model) {
        if (auth == null) {
            return "home";
        }
        if (auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_TEACHER"))) {
            return "redirect:/teacher";
        }
        return "redirect:/student";
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/register")
    public String registerForm() {
        return "register";
    }

    @PostMapping("/register")
    public String register(@RequestParam String username, @RequestParam String password,
            @RequestParam String role, RedirectAttributes ra) {
        username = username.trim();
        if (!username.matches("[A-Za-z0-9_.-]{3,50}") || password.length() < 8
                || !(role.equals("TEACHER") || role.equals("STUDENT"))) {
            ra.addFlashAttribute("error",
                    "Use a username of 3–50 letters/numbers/._- and a password of at least 8 characters.");
            return "redirect:/register";
        }
        if (users.existsByUsername(username)) {
            ra.addFlashAttribute("error", "That username is already registered.");
            return "redirect:/register";
        }
        users.save(new User(username, encoder.encode(password), role));
        ra.addFlashAttribute("message", "Account created. Please log in.");
        return "redirect:/login";
    }

    @GetMapping("/teacher")
    public String teacher(Authentication auth, Model model) {
        model.addAttribute("quizzes", quizzes.findByTeacherUsernameOrderByIdDesc(auth.getName()));
        return "teacher/dashboard";
    }

    @GetMapping("/teacher/quizzes/new")
    public String newQuiz() {
        return "teacher/new-quiz";
    }

    @PostMapping("/teacher/quizzes")
    public String createQuiz(
            Authentication auth,
            @RequestParam String title,
            @RequestParam String subject,
            @RequestParam String description,
            @RequestParam(defaultValue = "1") int questionCount,
            @RequestParam Map<String, String> form,
            RedirectAttributes ra) {

        title = title.trim();
        subject = subject.trim();
        description = description.trim();

        if (title.isBlank() || title.length() > 100
                || subject.isBlank() || subject.length() > 50
                || description.isBlank()) {
            ra.addFlashAttribute("error",
                    "Title (max 100), subject (max 50), and description are required.");
            return "redirect:/teacher/quizzes/new";
        }

        // Validate on the server too; JavaScript can be bypassed.
        if (questionCount < 1 || questionCount > 10) {
            ra.addFlashAttribute("error",
                    "A quiz must contain between 1 and 10 questions.");
            return "redirect:/teacher/quizzes/new";
        }

        User teacher = users.findByUsername(auth.getName()).orElseThrow();
        Quiz quiz = new Quiz(title, subject, description, teacher);

        for (int i = 1; i <= questionCount; i++) {
            String text = form.get("q" + i);
            String a = form.get("a" + i);
            String b = form.get("b" + i);
            String c = form.get("c" + i);
            String d = form.get("d" + i);
            String correct = form.get("correct" + i);
            String explanation = form.get("explanation" + i);

            if (blank(text) || blank(a) || blank(b) || blank(c)
                    || blank(d) || blank(explanation)
                    || correct == null
                    || !List.of("A", "B", "C", "D").contains(correct)) {
                ra.addFlashAttribute("error",
                        "Complete the question, all four options, the correct answer, and explanation for Question "
                        + i + ".");
                return "redirect:/teacher/quizzes/new";
            }

            quiz.addQuestion(new Question(
                    text.trim(),
                    a.trim(),
                    b.trim(),
                    c.trim(),
                    d.trim(),
                    correct,
                    explanation.trim(),
                    i));
        }

        quizzes.save(quiz);

        ra.addFlashAttribute("message",
                "Draft saved with " + questionCount + " question(s).");
        return "redirect:/teacher";
    }

    @PostMapping("/teacher/quizzes/{id}/publish")
    public String publish(@PathVariable Long id, Authentication auth, RedirectAttributes ra) {
        Quiz quiz = quizzes
                .findByIdAndTeacherUsername(id, auth.getName())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Quiz not found"));
        if (quiz == null) {
            ra.addFlashAttribute("error", "Quiz not found or you do not own it.");
            return "redirect:/teacher";
        }
        if (!quiz.getStatus().equals("DRAFT")
                || quiz.getQuestions().isEmpty()
                || quiz.getQuestions().size() > 10) {
            ra.addFlashAttribute("error",
                    "Only a draft containing between 1 and 10 questions can be published.");
            return "redirect:/teacher";
        }
        quiz.publish();
        quizzes.save(quiz);
        ra.addFlashAttribute("message", "Quiz published and visible to students.");
        return "redirect:/teacher";
    }

    @GetMapping("/teacher/quizzes/{quizId}/results")
    @Transactional
    public String quizResults(
            @PathVariable Long quizId,
            Authentication auth,
            Model model) {

        Quiz quiz = quizzes.findByIdAndTeacherUsername(
                quizId,
                auth.getName()
        ).orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Quiz not found"
        ));

        // Teachers may only view results for their own quizzes.
        if (quiz.getTeacher() == null
                || !quiz.getTeacher().getUsername()
                        .equals(auth.getName())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "You cannot view results for this quiz");
        }

        List<QuizAttempt> allAttempts
                = attempts.findByQuiz_Id(quizId);

        // Group attempts by student username.
        Map<String, List<QuizAttempt>> attemptsByStudent
                = new HashMap<>();

        for (QuizAttempt attempt : allAttempts) {
            String username = attempt.getStudent().getUsername();

            attemptsByStudent
                    .computeIfAbsent(username, key -> new ArrayList<>())
                    .add(attempt);
        }

        final int PASS_PERCENTAGE = 60;

        List<StudentQuizResult> results = new ArrayList<>();

        for (Map.Entry<String, List<QuizAttempt>> entry
                : attemptsByStudent.entrySet()) {

            List<QuizAttempt> studentAttempts = entry.getValue();

            // Select the attempt with the highest percentage.
            QuizAttempt bestAttempt = studentAttempts.stream()
                    .max(Comparator.comparingDouble(a -> {
                        int total = a.getTotalQuestions();

                        return total > 0
                                ? a.getScore() * 100.0 / total
                                : 0.0;
                    }))
                    .orElseThrow();

            int totalQuestions = bestAttempt.getTotalQuestions();
            int highestScore = bestAttempt.getScore();

            int percentage = totalQuestions > 0
                    ? (int) Math.round(
                            highestScore * 100.0 / totalQuestions)
                    : 0;

            results.add(new StudentQuizResult(
                    entry.getKey(),
                    studentAttempts.size(),
                    highestScore,
                    totalQuestions,
                    percentage,
                    percentage >= PASS_PERCENTAGE
            ));
        }

        // Show the highest-performing students first.
        results.sort(
                Comparator.comparingInt(StudentQuizResult::percentage)
                        .reversed()
        );

        model.addAttribute("quiz", quiz);
        model.addAttribute("results", results);
        model.addAttribute("passPercentage", PASS_PERCENTAGE);

        return "teacher/quiz-results";
    }

    @GetMapping("/student")
    public String student(
            @RequestParam(defaultValue = "") String subject,
            Authentication auth,
            Model model) {

        String username = auth.getName();

        List<Quiz> quizList = subject.isBlank()
                ? quizzes.findByStatusOrderByCreatedAtDesc("PUBLISHED")
                : quizzes.findByStatusAndSubjectContainingIgnoreCaseOrderByCreatedAtDesc(
                        "PUBLISHED", subject.trim());

        Map<Long, Long> attemptsUsed = new HashMap<>();

        for (Quiz quiz : quizList) {
            long count = attempts.countByStudent_UsernameAndQuiz_Id(
                    username, quiz.getId());

            attemptsUsed.put(quiz.getId(), count);
        }

        model.addAttribute("subject", subject);
        model.addAttribute("quizzes", quizList);
        model.addAttribute("attemptsUsed", attemptsUsed);

        return "student/dashboard";
    }

    @GetMapping("/student/quizzes/{id}")
    public String takeQuiz(@PathVariable Long id, Model model, RedirectAttributes ra) {
        Quiz quiz = quizzes.findWithQuestionsById(id).orElse(null);
        if (quiz == null || !quiz.getStatus().equals("PUBLISHED")) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Quiz not found");
        }
        model.addAttribute("quiz", quiz);
        return "student/take-quiz";
    }

    @PostMapping("/student/quizzes/{id}/submit")
    @Transactional
    public String submit(@PathVariable Long id, @RequestParam Map<String, String> form, Authentication auth,
            RedirectAttributes ra) {
        Quiz quiz = quizzes.findWithQuestionsById(id).orElse(null);
        if (quiz == null || !quiz.getStatus().equals("PUBLISHED")
                || quiz.getQuestions().isEmpty()
                || quiz.getQuestions().size() > 10) {
            ra.addFlashAttribute("error",
                    "Please answer all " + quiz.getQuestions().size() + " questions.");
            return "redirect:/student";
        }
        Map<Long, String> chosen = new HashMap<>();
        int score = 0;
        for (Question q : quiz.getQuestions()) {
            String value = form.get("q" + q.getId());
            if (value == null || !List.of("A", "B", "C", "D").contains(value)) {
                ra.addFlashAttribute("error", "Please answer all 10 questions.");
                return "redirect:/student/quizzes/" + id;
            }
            chosen.put(q.getId(), value);
            if (q.getCorrectOption().equals(value)) {
                score++;
            }
        }
        User student = users.findByUsername(auth.getName()).orElseThrow();
        int totalQuestions = quiz.getQuestions().size();
        long used = attempts.countByStudent_UsernameAndQuiz_Id(
                auth.getName(), id);

        if (used >= 3) {
            ra.addFlashAttribute(
                    "errorMessage",
                    "You have used all 3 attempts for this quiz.");

            return "redirect:/student/history";
        }
        QuizAttempt attempt = attempts.save(new QuizAttempt(student, quiz, score, totalQuestions));
        for (Question q : quiz.getQuestions()) {
            answers.save(new AttemptAnswer(attempt, q, chosen.get(q.getId()),
                    q.getCorrectOption().equals(chosen.get(q.getId()))));
        }
        ra.addFlashAttribute("attemptId", attempt.getId());
        return "redirect:/attempt/" + attempt.getId();
    }

    @GetMapping("/student/attempts/{attemptId}/review")
    public String reviewAttempt(
            @PathVariable Long attemptId,
            Authentication auth,
            Model model) {

        QuizAttempt attempt = attempts
                .findByIdAndStudentUsername(attemptId, auth.getName())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Attempt not found"));

        Quiz quiz = attempt.getQuiz();

        model.addAttribute("attempt", attempt);
        model.addAttribute("quiz", quiz);

        // Also populate selectedByQuestion using the answers
        // saved for THIS attempt in your database.
        return "student/result";
    }

    @GetMapping("/attempt/{id}")
    @Transactional
    public String result(@PathVariable Long id, Authentication auth, Model model, RedirectAttributes ra) {
        QuizAttempt attempt = attempts.findByIdAndStudentUsername(id, auth.getName()).orElse(null);
        if (attempt == null) {
            ra.addFlashAttribute("error", "Result not found.");
            return "redirect:/student";
        }
        Quiz quiz = quizzes.findWithQuestionsById(attempt.getQuiz().getId()).orElseThrow();
        model.addAttribute("attempt", attempt);
        model.addAttribute("quiz", quiz);
        model.addAttribute("selectedByQuestion", resultSelections(attempt.getId(), quiz));
        return "student/result";
    }

    private Map<Long, String> resultSelections(Long attemptId, Quiz quiz) {
        // Fetch rows using the repository's derived method added below.
        Map<Long, String> map = new HashMap<>();
        for (AttemptAnswer a : answers.findByAttemptId(attemptId)) {
            map.put(a.getQuestion().getId(), a.getSelectedOption());
        }
        return map;
    }

    @GetMapping("/student/history")
    public String studentHistory(Authentication auth, Model model) {
        List<QuizAttempt> history
                = attempts.findByStudentUsernameOrderBySubmittedAtDesc(
                        auth.getName());

        model.addAttribute("history", history);

        return "student/history";
    }

    private boolean blank(String s) {
        return s == null || s.trim().isEmpty();
    }
}
