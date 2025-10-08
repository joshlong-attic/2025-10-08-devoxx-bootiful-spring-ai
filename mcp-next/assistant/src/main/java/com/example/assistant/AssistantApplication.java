package com.example.assistant;

import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.spec.McpSchema.CreateMessageRequest;
import io.modelcontextprotocol.spec.McpSchema.CreateMessageResult;
import io.modelcontextprotocol.spec.McpSchema.LoggingMessageNotification;
import io.modelcontextprotocol.spec.McpSchema.ProgressNotification;
import org.springaicommunity.mcp.annotation.McpLogging;
import org.springaicommunity.mcp.annotation.McpProgress;
import org.springaicommunity.mcp.annotation.McpSampling;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.PromptChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.memory.repository.jdbc.JdbcChatMemoryRepository;
import org.springframework.ai.document.Document;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.annotation.Id;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@SpringBootApplication
public class AssistantApplication {

    public static void main(String[] args) {
        SpringApplication.run(AssistantApplication.class, args);
    }

    @Bean
    public ChatClient chatClient(
            PromptChatMemoryAdvisor memoryAdvisor,
            QuestionAnswerAdvisor questionAnswerAdvisor,
            ChatClient.Builder chatClientBuilder) {

        return chatClientBuilder
                .defaultAdvisors(memoryAdvisor, questionAnswerAdvisor)
                .defaultSystem("""
                        You are an AI powered assistant to help people adopt a dog from the adoption\s
                        agency named Pooch Palace with locations in Oslo, Crested Butte, Seoul, Tokyo, Singapore, Paris,\s
                        Mumbai, New Delhi, Barcelona, San Francisco, and London. Information about the dogs available\s
                        will be presented below. If there is no information, then return a polite response suggesting we\s
                        don't have any dogs available.
                        """)
                .build();
    }

    @Bean
    QuestionAnswerAdvisor questionAnswerAdvisor(VectorStore vectorStore) {
        return new QuestionAnswerAdvisor(vectorStore);
    }

    @Bean
    PromptChatMemoryAdvisor memory(DataSource dataSource) {
        var jdbc = JdbcChatMemoryRepository
                .builder()
                .dataSource(dataSource)
                .build();
        var mwa = MessageWindowChatMemory
                .builder()
                .chatMemoryRepository(jdbc)
                .build();
        return PromptChatMemoryAdvisor
                .builder(mwa)
                .build();
    }

    //    @Bean
    ApplicationRunner vectorize(VectorStore vectorStore, DogRepository repository) {
        return _ -> repository
                .findAll()
                .forEach(dog -> {
                    var dogument = new Document("id: %s, name: %s, description: %s".formatted(
                            dog.id(), dog.name(), dog.description()
                    ));
                    vectorStore.add(List.of(dogument));
                });
    }
}


interface DogRepository extends ListCrudRepository<Dog, Integer> {
}

record Dog(@Id int id, String name, String owner, String description) {
}



@Controller
@ResponseBody
class AssistantController {

    private final ChatClient ai;    

    private final ToolCallbackProvider schedule;

    AssistantController(@Lazy ChatClient chatClient, @Lazy ToolCallbackProvider scheduler) {
        this.ai = chatClient;
        this.schedule = scheduler;
    }

    @GetMapping("/ask")
    String ask(@RequestParam String question) {
        return this.ai
                .prompt(question)
                .toolCallbacks(this.schedule)
                .toolContext(Map.of("progressToken", "123"))                
                .call()
                .content();
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
		// logger.info("MCP SAMPLING: {}", llmRequest);

		String userPrompt = ((McpSchema.TextContent) llmRequest.messages().get(0).content()).text();

		var samplingResponse = ai.prompt(userPrompt).call().content();

		return CreateMessageResult.builder()
				.content(new McpSchema.TextContent("Response " + samplingResponse))
				.build();
	}


}


record DogAdoptionSuggestion(int id, String name, String description) {
}