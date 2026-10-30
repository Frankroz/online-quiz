# Online Quiz (Java + Spring Boot + MySQL)

A local web application for teachers to create/publish 10-question multiple-choice quizzes and students to search, take, and review them.

## Requirements
- JDK 21
- Maven 3.9+
- MySQL 8.x running locally

## 1. Create the database and a restricted application user
Run this in MySQL as an administrator (change the password first):

```sql
CREATE DATABASE IF NOT EXISTS quiz_app CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER IF NOT EXISTS 'quiz_app_user'@'localhost' IDENTIFIED BY 'change_me_to_a_strong_password';
GRANT SELECT, INSERT, UPDATE, DELETE, CREATE, ALTER, INDEX, REFERENCES
ON quiz_app.* TO 'quiz_app_user'@'localhost';
FLUSH PRIVILEGES;
```

Flyway creates the tables on first startup. Do not run the old `DROP TABLE` script against any database containing data.

## 2. Configure `.env`
Copy `.env.example` to `.env`, then set the same database username/password as above. The `.env` file is deliberately ignored by Git.

**Windows PowerShell:** run `.\run.ps1` from the project folder.  
**macOS/Linux:** run `chmod +x run.sh && ./run.sh`.

These scripts load `.env` into the process environment before starting Spring Boot. Alternatively, set the environment variables yourself and run `mvn spring-boot:run`.

## 3. Open the app
Visit http://localhost:8080

- Register a teacher or student using the registration page.
- Teacher: create a draft quiz, fill in all 10 questions, then publish.
- Student: search published quizzes, take one, submit all answers, and view the score/explanations.

The application starts with registration open. For a single-PC demo, only use it locally. For a public deployment, add stronger account-provisioning controls, HTTPS, rate limiting, and production secrets management.

## Notes
- Passwords are hashed using BCrypt.
- Quiz answers are not sent to the browser until after submission.
- Score calculation and authorization happen server-side.
- Publishing requires exactly 10 complete questions.
- Published quizzes cannot be edited; create a new quiz instead.
- Multiple attempts are allowed.
- Flyway owns schema changes; Hibernate is configured to validate, not create/drop tables.
