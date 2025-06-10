package com.marsreg.inference.controller;

import com.marsreg.inference.model.InferenceRequest;
import com.marsreg.inference.model.InferenceResponse;
import com.marsreg.inference.service.InferenceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/inference")
@RequiredArgsConstructor
public class InferenceController {

    private final InferenceService inferenceService;

    @PostMapping
    public InferenceResponse infer(@Valid @RequestBody InferenceRequest request) {
        return inferenceService.infer(request);
    }

    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> inferStream(@Valid @RequestBody InferenceRequest request) {
        return Flux.create(sink -> {
            inferenceService.inferStream(request, new InferenceService.StreamCallback() {
                @Override
                public void onToken(String token) {
                    sink.next(token);
                }

                @Override
                public void onComplete() {
                    sink.complete();
                }

                @Override
                public void onError(Throwable error) {
                    sink.error(error);
                }
            });
        });
    }
} 