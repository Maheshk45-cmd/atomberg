package com.atomquest.goalportal.service;

import com.atomquest.goalportal.dto.CycleRequest;
import com.atomquest.goalportal.dto.ThrustAreaRequest;
import com.atomquest.goalportal.entity.Cycle;
import com.atomquest.goalportal.entity.ThrustArea;
import com.atomquest.goalportal.repository.CycleRepository;
import com.atomquest.goalportal.repository.ThrustAreaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AdminConfigService {

    private final ThrustAreaRepository thrustAreaRepository;
    private final CycleRepository cycleRepository;

    public List<ThrustArea> getAllThrustAreas() {
        return thrustAreaRepository.findAll();
    }

    @Transactional
    public ThrustArea createThrustArea(ThrustAreaRequest req) {
        ThrustArea ta = ThrustArea.builder()
                .name(req.getName())
                .description(req.getDescription())
                .isActive(req.isActive())
                .build();
        return thrustAreaRepository.save(ta);
    }

    @Transactional
    public ThrustArea updateThrustArea(UUID id, ThrustAreaRequest req) {
        ThrustArea ta = thrustAreaRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Thrust Area not found"));
        ta.setName(req.getName());
        ta.setDescription(req.getDescription());
        ta.setActive(req.isActive());
        return thrustAreaRepository.save(ta);
    }

    public List<Cycle> getAllCycles() {
        return cycleRepository.findAll();
    }

    @Transactional
    public Cycle createCycle(CycleRequest req) {
        // [DATA INTEGRITY] Only one active cycle allowed — deactivate all others first
        if (req.isActive()) {
            cycleRepository.findAll().forEach(c -> { c.setActive(false); cycleRepository.save(c); });
        }
        Cycle cycle = Cycle.builder()
                .year(req.getYear())
                .startDate(req.getStartDate())
                .endDate(req.getEndDate())
                .isActive(req.isActive())
                .build();
        return cycleRepository.save(cycle);
    }

    @Transactional
    public Cycle updateCycle(UUID id, CycleRequest req) {
        Cycle cycle = cycleRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cycle not found"));
        // [DATA INTEGRITY] Only one active cycle allowed — deactivate all others first
        if (req.isActive()) {
            cycleRepository.findAll().forEach(c -> {
                if (!c.getId().equals(id)) { c.setActive(false); cycleRepository.save(c); }
            });
        }
        cycle.setYear(req.getYear());
        cycle.setStartDate(req.getStartDate());
        cycle.setEndDate(req.getEndDate());
        cycle.setActive(req.isActive());
        return cycleRepository.save(cycle);
    }
}
