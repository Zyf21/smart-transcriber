package com.transcriber.workerservice.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class QuestionsExtractionResult {

    private List<String> questions;
}
