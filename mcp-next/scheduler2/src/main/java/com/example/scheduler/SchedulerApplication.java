package com.example.scheduler;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import io.modelcontextprotocol.spec.McpSchema;
import org.springaicommunity.mcp.annotation.McpTool;
import org.springaicommunity.mcp.annotation.McpToolParam;
import org.springaicommunity.mcp.context.McpSyncRequestContext;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.stereotype.Component;

@SpringBootApplication
public class SchedulerApplication {

	public static void main(String[] args) {
		SpringApplication.run(SchedulerApplication.class, args);
	}

	@Component
	static class DogAdoptionScheduler {

		@McpTool(description = "schedule an appointment to pick up or adopt a dog from a Pooch Palace location")
		public String schedule(
			McpSyncRequestContext ctx,
			@McpToolParam(description = "the id of the dog") int dogId,
			@McpToolParam(description = "the name of the dog") String dogName) {

			ctx.info("Scheduling dog %s/%s".formatted(dogId, dogName));

			ctx.progress(p -> p.percentage(25).message("Scheduling in progress..."));

			var i = Instant
				.now()
				.plus(3, ChronoUnit.DAYS)
				.toString();

			ctx.progress(p -> p.percentage(50).message("Start weriting haiku..."));

			McpSchema.CreateMessageResult dogPoemResponse = ctx.sample( s-> s
				.systemPrompt("We are haiku poet")
				.message("Please write a haiku about scheduling a dog named " + dogName)).get();

			ctx.progress(p -> p.percentage(100).message("Haiku writing completed."));
			
			ctx.info("Haiku for dog %s/%s: %s".formatted(dogId, dogName, dogPoemResponse.content()));

			System.out.println("scheduling " + dogId + "/" + dogName + " for " + i);
			// System.out.println("haiku: " + dogPoemResponse.content());

			ctx.progress(p -> p.percentage(100).message("Scheduling completed."));

			return i;
		}
	}

}
