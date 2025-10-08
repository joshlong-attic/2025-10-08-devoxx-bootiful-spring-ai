package com.example.assistant;

import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.spec.McpSchema.CreateMessageRequest;
import io.modelcontextprotocol.spec.McpSchema.CreateMessageResult;
import io.modelcontextprotocol.spec.McpSchema.LoggingMessageNotification;
import io.modelcontextprotocol.spec.McpSchema.ProgressNotification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springaicommunity.mcp.annotation.McpLogging;
import org.springaicommunity.mcp.annotation.McpProgress;
import org.springaicommunity.mcp.annotation.McpSampling;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

// @Service
public class McpClientHandlerProviders {

	private static final Logger logger = LoggerFactory.getLogger(McpClientHandlerProviders.class);

	
	private final ChatClient chatClient;

	public McpClientHandlerProviders(@Lazy ChatClient chatClient) {
		this.chatClient = chatClient;		
	}

	@McpProgress(clients = "conference-assistant")
	public void progressHandler(ProgressNotification progressNotification) {
		System.out.println("MCP PROGRESS: [" 
			+ progressNotification.progressToken() + "] progress: " 
			+ progressNotification.progress()+ " total: "+progressNotification.total()
			+ " message: " + progressNotification.message());
	}

	@McpLogging(clients = "conference-assistant")
	public void loggingHandler(LoggingMessageNotification loggingMessage) {
		System.out.println("MCP LOGGING: [" 
			+ loggingMessage.level() + "] " 
			+ loggingMessage.data());
	}

	@McpSampling(clients = "conference-assistant")
	public CreateMessageResult samplingHandler(CreateMessageRequest llmRequest) {
		logger.info("MCP SAMPLING: {}", llmRequest);

		String userPrompt = ((McpSchema.TextContent) llmRequest.messages().get(0).content()).text();
		// String modelHint = llmRequest.modelPreferences().hints().get(0).name();

		var samplingResponse = chatClient.prompt(userPrompt).call().content();

		return CreateMessageResult.builder()
				.content(new McpSchema.TextContent("Response " + samplingResponse))
				.build();
	}
}
