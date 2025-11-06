package com.mockerview.controller;

import com.mockerview.dto.InterviewerNoteDTO;
import com.mockerview.service.InterviewerNoteService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/interviewer-notes")
@RequiredArgsConstructor
public class InterviewerNoteController {
    private final InterviewerNoteService interviewerNoteService;

    @PostMapping
    public ResponseEntity<InterviewerNoteDTO> saveNote(@RequestBody InterviewerNoteDTO dto) {
        InterviewerNoteDTO saved = interviewerNoteService.saveNote(dto);
        return ResponseEntity.ok(saved);
    }

    @GetMapping("/session/{sessionId}")
    public ResponseEntity<List<InterviewerNoteDTO>> getSessionNotes(@PathVariable Long sessionId) {
        List<InterviewerNoteDTO> notes = interviewerNoteService.getSessionNotes(sessionId);
        return ResponseEntity.ok(notes);
    }

    @GetMapping("/{sessionId}/{intervieweeId}")
    public ResponseEntity<InterviewerNoteDTO> getNote(
            @PathVariable Long sessionId,
            @PathVariable Long intervieweeId,
            @RequestParam Long interviewerId) {
        return interviewerNoteService.getNote(sessionId, intervieweeId, interviewerId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/interviewee/{intervieweeId}")
    public ResponseEntity<List<InterviewerNoteDTO>> getIntervieweeNotes(@PathVariable Long intervieweeId) {
        List<InterviewerNoteDTO> notes = interviewerNoteService.getSubmittedNotesForInterviewee(intervieweeId);
        return ResponseEntity.ok(notes);
    }
}
