package com.nxn.chatbot.controller;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoint de teste para confirmar que o servidor subiu.
 * GET http://localhost:8080/api/health -> "ok"
 */
@RestController
@RequestMapping("/api")
public class HealthController {

	@GetMapping(value = "/health", produces = MediaType.TEXT_PLAIN_VALUE)
	public String health() {
		return "ok";
	}
}
