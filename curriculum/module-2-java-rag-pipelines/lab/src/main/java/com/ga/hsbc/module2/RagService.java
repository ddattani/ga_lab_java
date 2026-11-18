package com.ga.hsbc.module2;

import java.io.IOException;
import java.util.List;

/**
 * Retrieval-augmented question answering with a relevance guardrail:
 * if nothing in the documents looks relevant, we say so and never call the model.
 *
 * YOUR JOB (You Do): write ask() so that  mvn -q test -Dtest=RagServiceTest  goes green.
 */
public final class RagService {
    public static final String NO_ANSWER = "I don't have information on that in the provided documents.";

    public record RagAnswer(String question, String answer, boolean grounded,
                            List<InMemoryIndex.ScoredChunk> sources, int inputTokens, int outputTokens) {}

    private final InMemoryIndex index;
    private final LlmClient llm;
    private final double minScore;
    private final int topK;

    public RagService(InMemoryIndex index, LlmClient llm, double minScore, int topK) {
        this.index = index;
        this.llm = llm;
        this.minScore = minScore;
        this.topK = topK;
    }

    /**
     * YOUR JOB (You Do): search the index for topK chunks. If the best score is below minScore (or there are no hits),
     * return new RagAnswer(question, NO_ANSWER, false, List.of(), 0, 0) WITHOUT calling the model. Otherwise build the prompt
     * with PromptBuilder, call llm.complete(system, user) exactly once, and return a grounded RagAnswer carrying the
     * sources and the token counts.
     */
    public RagAnswer ask(String question) throws IOException {
        List<InMemoryIndex.ScoredChunk> sources = index.search(question, topK);
        if (sources.isEmpty() || sources.get(0).score() < minScore) {
            return new RagAnswer(question, NO_ANSWER, false, List.of(), 0, 0);
        }

        PromptBuilder.Prompt prompt = PromptBuilder.build(question, sources);
        LlmClient.LlmResponse response = llm.complete(prompt.system(), prompt.user());
        return new RagAnswer(question, response.text(), true, sources,
                response.inputTokens(), response.outputTokens());
    }
}
