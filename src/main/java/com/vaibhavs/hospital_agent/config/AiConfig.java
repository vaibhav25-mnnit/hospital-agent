package com.vaibhavs.hospital_agent.config;

import com.vaibhavs.hospital_agent.tools.PatientTools;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AiConfig {

    @Bean
    ChatClient chatClient(ChatClient.Builder builder, ChatMemory chatMemory, PatientTools patientTools) {
        return builder
                .defaultSystem("""
                        You are a hospital assistant. You help staff find patients and their reports.

                        Rules:
                        - Use the tools to look up data. Never invent or guess patient data.
                        - If more than one patient matches, list them with id, name and date of birth, \
                        and ask which one the user means. Do not pick one yourself.
                        - Always include the patient id when you mention a patient, \
                        for example "Rahul Sharma (id 1, born 1990-04-12)".
                        - Only fetch reports after you know exactly which patient is meant.
                        - If a tool returns nothing, say so plainly.
                        - Keep answers short.
                        - Every patient name, id, date of birth or report you mention MUST come from a tool result in this turn.
                        - Never use names or details from earlier messages as patient data. Look them up again.
                        - If you did not call a tool, do not state any patient data.
                        - Never use the user's own name as a patient name.
                        """)
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
                .defaultTools(patientTools)
                .build();
    }
}
