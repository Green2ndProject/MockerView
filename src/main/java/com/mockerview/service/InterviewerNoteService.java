package com.mockerview.service;

import com.mockerview.dto.InterviewerNoteDTO;
import com.mockerview.entity.InterviewerNote;
import com.mockerview.entity.Session;
import com.mockerview.entity.User;
import com.mockerview.repository.InterviewerNoteRepository;
import com.mockerview.repository.SessionRepository;
import com.mockerview.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InterviewerNoteService {
    private final InterviewerNoteRepository interviewerNoteRepository;
    private final SessionRepository sessionRepository;
    private final UserRepository userRepository;

    @Transactional
    public InterviewerNoteDTO saveNote(InterviewerNoteDTO dto) {
        Session session = sessionRepository.findById(dto.getSessionId())
                .orElseThrow(() -> new RuntimeException("Session not found"));
        
        User interviewer = userRepository.findById(dto.getInterviewerId())
                .orElseThrow(() -> new RuntimeException("Interviewer not found"));
        
        User interviewee = userRepository.findById(dto.getIntervieweeId())
                .orElseThrow(() -> new RuntimeException("Interviewee not found"));

        Optional<InterviewerNote> existingNote = interviewerNoteRepository
                .findBySessionIdAndInterviewerIdAndIntervieweeId(
                        dto.getSessionId(),
                        dto.getInterviewerId(),
                        dto.getIntervieweeId()
                );

        InterviewerNote note;
        if (existingNote.isPresent()) {
            note = existingNote.get();
        } else {
            note = new InterviewerNote();
            note.setSession(session);
            note.setInterviewer(interviewer);
            note.setInterviewee(interviewee);
        }

        note.setRating(dto.getRating());
        note.setStrengths(dto.getStrengths());
        note.setWeaknesses(dto.getWeaknesses());
        note.setImprovements(dto.getImprovements());
        note.setNotes(dto.getNotes());
        note.setSubmitted(dto.getSubmitted());

        InterviewerNote saved = interviewerNoteRepository.save(note);
        return convertToDTO(saved);
    }

    @Transactional(readOnly = true)
    public List<InterviewerNoteDTO> getSessionNotes(Long sessionId) {
        return interviewerNoteRepository.findBySessionId(sessionId).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Optional<InterviewerNoteDTO> getNote(Long sessionId, Long intervieweeId, Long interviewerId) {
        return interviewerNoteRepository
                .findBySessionIdAndInterviewerIdAndIntervieweeId(sessionId, interviewerId, intervieweeId)
                .map(this::convertToDTO);
    }

    @Transactional(readOnly = true)
    public List<InterviewerNoteDTO> getSubmittedNotesForInterviewee(Long intervieweeId) {
        return interviewerNoteRepository.findByIntervieweeIdAndSubmitted(intervieweeId, true).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Double getAverageRatingForSession(Long sessionId) {
        List<InterviewerNote> notes = interviewerNoteRepository.findBySessionId(sessionId);
        if (notes.isEmpty()) {
            return null;
        }
        return notes.stream()
                .filter(n -> n.getRating() != null && n.getSubmitted())
                .mapToInt(InterviewerNote::getRating)
                .average()
                .orElse(0.0);
    }

    private InterviewerNoteDTO convertToDTO(InterviewerNote note) {
        return InterviewerNoteDTO.builder()
                .id(note.getId())
                .sessionId(note.getSession().getId())
                .interviewerId(note.getInterviewer().getId())
                .interviewerName(note.getInterviewer().getName())
                .intervieweeId(note.getInterviewee().getId())
                .intervieweeName(note.getInterviewee().getName())
                .rating(note.getRating())
                .strengths(note.getStrengths())
                .weaknesses(note.getWeaknesses())
                .improvements(note.getImprovements())
                .notes(note.getNotes())
                .submitted(note.getSubmitted())
                .createdAt(note.getCreatedAt())
                .updatedAt(note.getUpdatedAt())
                .build();
    }
}
