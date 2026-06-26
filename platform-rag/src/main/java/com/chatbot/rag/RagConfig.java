package com.chatbot.rag;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.loader.FileSystemDocumentLoader;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import java.nio.file.Files;
import java.nio.file.Path;

@Slf4j
@Configuration
public class RagConfig {

    @Bean
    public DocumentSplitter documentSplitter() {
        return DocumentSplitters.recursive(500, 50);
    }

    @Bean
    public ContentRetriever contentRetriever(EmbeddingStore<TextSegment> embeddingStore,
                                             EmbeddingModel embeddingModel) {
        return EmbeddingStoreContentRetriever.builder()
                .embeddingStore(embeddingStore)
                .embeddingModel(embeddingModel)
                .maxResults(3)
                .minScore(0.6)
                .build();
    }

    @Bean
    public ApplicationRunner ragDocumentLoader(
            @Value("${rag.documents-path:classpath:documents/*}") String documentsPath,
            EmbeddingStore<TextSegment> embeddingStore,
            EmbeddingModel embeddingModel,
            DocumentSplitter documentSplitter) {

        return args -> {
            var ingestor = EmbeddingStoreIngestor.builder()
                    .documentSplitter(documentSplitter)
                    .embeddingModel(embeddingModel)
                    .embeddingStore(embeddingStore)
                    .build();

            Resource[] resources = new PathMatchingResourcePatternResolver()
                    .getResources(documentsPath);

            for (Resource resource : resources) {
                if (!resource.exists() || resource.getFilename() == null) {
                    continue;
                }
                String name = resource.getFilename();
                if (!name.endsWith(".pdf") && !name.endsWith(".txt")) {
                    continue;
                }
                Path temp = Files.createTempFile("rag-", "-" + name);
                resource.getInputStream().transferTo(Files.newOutputStream(temp));
                Document doc = FileSystemDocumentLoader.loadDocument(temp);
                ingestor.ingest(doc);
                Files.deleteIfExists(temp);
                log.info("Ingested document: {}", name);
            }
        };
    }
}
