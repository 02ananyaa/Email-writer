package com.email.writer.service;

import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import com.email.writer.DTO.EmailRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class EmailGeneratorService {

	private final WebClient webClient;

	EmailGeneratorService(WebClient.Builder webClientBuilder) {
		this.webClient = webClientBuilder.build();
	}

	@Value("${gemini.api.url}")
	private String geminiApiUrl;

	@Value("${gemini.api.key}")
	private String geminiApiKey;

	public String generateEmailReply(EmailRequest emailRequest) {
//		build the prompt
		String prompt = buildPrompt(emailRequest);

//		craft a request

		Map<String, Object> requestBody = Map.of("contents",
				new Object[] { Map.of("parts", new Object[] { Map.of("text", prompt) }) }

		);
//		do request and get response
		String response=webClient.post().uri(geminiApiUrl+geminiApiKey)
				.header("Content-Type", "application/json")
				.bodyValue(requestBody)
				.retrieve()
				.bodyToMono(String.class)
				.block();
//		extract response and return response
//	
		return extractResponseContent(response);
	}

	public String extractResponseContent(String response) {
		 try {
			ObjectMapper mapper=new ObjectMapper();
			JsonNode rootNode=mapper.readTree(response);
			return rootNode.path("candidates").get(0).path("content").path("parts").get(0).path("text").asText();
		 }
		 catch(Exception e ) {
			 return "Error processing response: "+e.getMessage();
		 }
	}
	public String buildPrompt(EmailRequest emailRequest) {
		StringBuilder prompt = new StringBuilder();
		prompt.append("Generate a Professional email reply for the following email content.Please don't generate a subject line ");

		if (emailRequest.getTone() != null && !emailRequest.getTone().isEmpty()) {
			prompt.append("Use a").append(emailRequest.getTone()).append("tone");
		}

		prompt.append("\n Original Email \n :").append(emailRequest.getEmailContent());
		return prompt.toString();

	}
}
