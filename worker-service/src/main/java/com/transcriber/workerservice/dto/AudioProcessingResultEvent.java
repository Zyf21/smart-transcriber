package com.transcriber.workerservice.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AudioProcessingResultEvent {

    private UUID uploadId;
    private String status;
    private List<String> questions;
    private String errorMessage;
}
