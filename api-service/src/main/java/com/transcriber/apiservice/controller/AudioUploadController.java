package com.transcriber.apiservice.controller;

import com.transcriber.apiservice.dto.AudioUploadResponse;
import com.transcriber.apiservice.service.AudioUploadService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/audio-uploads")
@RequiredArgsConstructor
public class AudioUploadController {

    private final AudioUploadService audioUploadService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public AudioUploadResponse uploadAudio(@RequestPart("file") MultipartFile file) {
        return audioUploadService.uploadAudio(file);
    }

    @GetMapping("/{id}")
    public AudioUploadResponse getUpload(@PathVariable UUID id) {
        return audioUploadService.getUpload(id);
    }
}
