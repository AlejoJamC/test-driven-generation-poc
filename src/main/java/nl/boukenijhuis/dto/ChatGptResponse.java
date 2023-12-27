package nl.boukenijhuis.dto;

import java.util.List;

public record ChatGptResponse(

        String id,
        String object,
        long created,
        String model,
        List<Choice> choices,
        Usage usage
) {
    public record Choice(
            int index,
            Message message,
            String finish_reason) {
    }

    public record Message(
            String role,
            String content) {
    }

    public record Usage(
            int prompt_tokens,
            int completion_tokens,
            int total_tokens) {
    }
}
