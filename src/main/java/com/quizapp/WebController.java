package com.quizapp;

import com.quizapp.user.User;
import com.quizapp.user.UserRepository;
import com.quizapp.quiz.*;
import com.quizapp.attempt.*;
import jakarta.transaction.Transactional;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.util.*;

@Controller
public class WebController {
    private final UserRepository users;
    private final QuizRepository quizzes;
    private final QuizAttemptRepository attempts;
    private final AttemptAnswerRepository answers;
    private final PasswordEncoder encoder;

    public WebController(UserRepository users, QuizRepository quizzes, QuizAttemptRepository attempts,
                         AttemptAnswerRepository answers, PasswordEncoder encoder) {
        this.users=users; this.quizzes=quizzes; this.attempts=attempts; this.answers=answers; this.encoder=encoder;
    }

    @GetMapping("/") public String home(Authentication auth, Model model) {
        if (auth == null) return "home";
        if (auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_TEACHER"))) return "redirect:/teacher";
        return "redirect:/student";
    }
    @GetMapping("/login") public String login() { return "login"; }

    @GetMapping("/register") public String registerForm() { return "register"; }
    @PostMapping("/register") public String register(@RequestParam String username, @RequestParam String password,
            @RequestParam String role, RedirectAttributes ra) {
        username=username.trim();
        if (!username.matches("[A-Za-z0-9_.-]{3,50}") || password.length()<8 ||
            !(role.equals("TEACHER") || role.equals("STUDENT"))) {
            ra.addFlashAttribute("error", "Use a username of 3–50 letters/numbers/._- and a password of at least 8 characters.");
            return "redirect:/register";
        }
        if (users.existsByUsername(username)) {
            ra.addFlashAttribute("error", "That username is already registered."); return "redirect:/register";
        }
        users.save(new User(username, encoder.encode(password), role));
        ra.addFlashAttribute("message", "Account created. Please log in.");
        return "redirect:/login";
    }

    @GetMapping("/teacher") public String teacher(Authentication auth, Model model) {
        model.addAttribute("quizzes", quizzes.findByTeacherUsernameOrderByIdDesc(auth.getName()));
        return "teacher/dashboard";
    }
    @GetMapping("/teacher/quizzes/new") public String newQuiz() { return "teacher/new-quiz"; }

    @PostMapping("/teacher/quizzes") public String createQuiz(Authentication auth, @RequestParam String title,
            @RequestParam String subject, @RequestParam String description,
            @RequestParam Map<String,String> form, RedirectAttributes ra) {
        title=title.trim(); subject=subject.trim(); description=description.trim();
        if (title.isBlank() || title.length()>100 || subject.isBlank() || subject.length()>50 || description.isBlank()) {
            ra.addFlashAttribute("error", "Title (max 100), subject (max 50), and description are required.");
            return "redirect:/teacher/quizzes/new";
        }
        User teacher=users.findByUsername(auth.getName()).orElseThrow();
        Quiz quiz=new Quiz(title, subject, description, teacher);
        for (int i=1;i<=10;i++) {
            String text=form.get("q"+i), a=form.get("a"+i), b=form.get("b"+i), c=form.get("c"+i),
                d=form.get("d"+i), correct=form.get("correct"+i), explanation=form.get("explanation"+i);
            if (blank(text)||blank(a)||blank(b)||blank(c)||blank(d)||blank(explanation)||
                correct==null||!List.of("A","B","C","D").contains(correct)) {
                ra.addFlashAttribute("error", "Complete every question, all four options, the correct option, and explanation.");
                return "redirect:/teacher/quizzes/new";
            }
            quiz.addQuestion(new Question(text.trim(),a.trim(),b.trim(),c.trim(),d.trim(),correct,explanation.trim(),i));
        }
        quizzes.save(quiz);
        ra.addFlashAttribute("message", "Draft saved. Review it and publish when ready.");
        return "redirect:/teacher";
    }

    @PostMapping("/teacher/quizzes/{id}/publish") public String publish(@PathVariable Long id, Authentication auth, RedirectAttributes ra) {
        Quiz quiz=quizzes.findByIdAndTeacherUsername(id,auth.getName()).orElse(null);
        if (quiz==null) { ra.addFlashAttribute("error","Quiz not found or you do not own it."); return "redirect:/teacher"; }
        if (!quiz.getStatus().equals("DRAFT") || quiz.getQuestions().size()!=10) {
            ra.addFlashAttribute("error","Only a draft containing exactly 10 questions can be published."); return "redirect:/teacher";
        }
        quiz.publish(); quizzes.save(quiz);
        ra.addFlashAttribute("message","Quiz published and visible to students.");
        return "redirect:/teacher";
    }

    @GetMapping("/student") public String student(@RequestParam(defaultValue="") String subject, Model model) {
        model.addAttribute("subject", subject);
        model.addAttribute("quizzes", subject.isBlank()
            ? quizzes.findByStatusOrderByCreatedAtDesc("PUBLISHED")
            : quizzes.findByStatusAndSubjectContainingIgnoreCaseOrderByCreatedAtDesc("PUBLISHED",subject.trim()));
        model.addAttribute("attempts", List.of());
        return "student/dashboard";
    }

    @GetMapping("/student/quizzes/{id}") public String takeQuiz(@PathVariable Long id, Model model, RedirectAttributes ra) {
        Quiz quiz=quizzes.findWithQuestionsById(id).orElse(null);
        if (quiz==null || !quiz.getStatus().equals("PUBLISHED")) {
            ra.addFlashAttribute("error","Quiz not found or not published."); return "redirect:/student";
        }
        model.addAttribute("quiz",quiz);
        return "student/take-quiz";
    }

    @PostMapping("/student/quizzes/{id}/submit")
    @Transactional
    public String submit(@PathVariable Long id, @RequestParam Map<String,String> form, Authentication auth,
                         RedirectAttributes ra) {
        Quiz quiz=quizzes.findWithQuestionsById(id).orElse(null);
        if (quiz==null || !quiz.getStatus().equals("PUBLISHED") || quiz.getQuestions().size()!=10) {
            ra.addFlashAttribute("error","This quiz is unavailable."); return "redirect:/student";
        }
        Map<Long,String> chosen=new HashMap<>();
        int score=0;
        for (Question q:quiz.getQuestions()) {
            String value=form.get("q"+q.getId());
            if (value==null || !List.of("A","B","C","D").contains(value)) {
                ra.addFlashAttribute("error","Please answer all 10 questions."); return "redirect:/student/quizzes/"+id;
            }
            chosen.put(q.getId(),value);
            if (q.getCorrectOption().equals(value)) score++;
        }
        User student=users.findByUsername(auth.getName()).orElseThrow();
        QuizAttempt attempt=attempts.save(new QuizAttempt(student,quiz,score));
        for (Question q:quiz.getQuestions())
            answers.save(new AttemptAnswer(attempt,q,chosen.get(q.getId()),q.getCorrectOption().equals(chosen.get(q.getId()))));
        ra.addFlashAttribute("attemptId",attempt.getId());
        return "redirect:/attempt/"+attempt.getId();
    }

    @GetMapping("/attempt/{id}") @Transactional
    public String result(@PathVariable Long id, Authentication auth, Model model, RedirectAttributes ra) {
        QuizAttempt attempt=attempts.findByIdAndStudentUsername(id,auth.getName()).orElse(null);
        if (attempt==null) { ra.addFlashAttribute("error","Result not found."); return "redirect:/student"; }
        Quiz quiz=quizzes.findWithQuestionsById(attempt.getQuiz().getId()).orElseThrow();
        model.addAttribute("attempt",attempt); model.addAttribute("quiz",quiz);
        model.addAttribute("selectedByQuestion", resultSelections(attempt.getId(), quiz));
        return "student/result";
    }

    private Map<Long,String> resultSelections(Long attemptId, Quiz quiz) {
        // Fetch rows using the repository's derived method added below.
        Map<Long,String> map=new HashMap<>();
        for (AttemptAnswer a:answers.findByAttemptId(attemptId)) {
            map.put(a.getQuestion().getId(), a.getSelectedOption());
        }
        return map;
    }

    @GetMapping("/student/history") public String history(Authentication auth, Model model) {
        model.addAttribute("attempts",attempts.findByStudentUsernameOrderBySubmittedAtDesc(auth.getName()));
        return "student/history";
    }

    private boolean blank(String s) { return s==null || s.trim().isEmpty(); }
}
