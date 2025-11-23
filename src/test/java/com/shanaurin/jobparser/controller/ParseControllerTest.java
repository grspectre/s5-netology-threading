package com.shanaurin.jobparser.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shanaurin.jobparser.model.dto.ParseRequest;
import com.shanaurin.jobparser.service.UrlQueueService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ParseControllerTest {

    private MockMvc mockMvc;
    private UrlQueueService urlQueueService;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        urlQueueService = mock(UrlQueueService.class);
        ParseController controller = new ParseController(urlQueueService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
        objectMapper = new ObjectMapper();
    }

    @Test
    void parse_shouldAcceptUrlsAndReturnAccepted() throws Exception {
        ParseRequest req = new ParseRequest();
        req.setUrls(List.of("u1", "u2"));

        mockMvc.perform(post("/api/parse")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isAccepted())
                .andExpect(content().string("URLs added to queue"));

        ArgumentCaptor<List<String>> captor = ArgumentCaptor.forClass(List.class);
        verify(urlQueueService).addAll(captor.capture());
        assertThat(captor.getValue()).containsExactly("u1", "u2");
    }
}