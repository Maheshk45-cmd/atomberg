package com.atomquest.goalportal;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;

@SpringBootApplication
public class GoalPortalApplication {

	public static void main(String[] args) {
		SpringApplication.run(GoalPortalApplication.class, args);
	}

    @ControllerAdvice
    public static class GlobalErrorHandler {
        @ExceptionHandler(Exception.class)
        public ResponseEntity<String> handle(Exception ex) {
            ex.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ex.getMessage());
        }
    }
}
