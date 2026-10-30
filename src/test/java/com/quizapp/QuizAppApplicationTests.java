package com.quizapp;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import static org.junit.jupiter.api.Assertions.*;

class QuizAppApplicationTests {
    @Test void passwordEncoderHashesAndVerifiesPassword() {
        var encoder = new BCryptPasswordEncoder();
        String hash = encoder.encode("example-password");
        assertNotEquals("example-password", hash);
        assertTrue(encoder.matches("example-password", hash));
        assertFalse(encoder.matches("wrong-password", hash));
    }
}
