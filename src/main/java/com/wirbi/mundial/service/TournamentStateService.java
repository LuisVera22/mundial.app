package com.wirbi.mundial.service;

import com.wirbi.mundial.model.Finals;
import com.wirbi.mundial.model.TournamentState;
import com.wirbi.mundial.repository.TournamentStateRepository;
import org.springframework.stereotype.Service;

/** Acceso al estado dinámico del torneo (finales campeón/goleador). */
@Service
public class TournamentStateService {

    private final TournamentStateRepository repo;

    public TournamentStateService(TournamentStateRepository repo) {
        this.repo = repo;
    }

    public TournamentState get() {
        return repo.findById(TournamentState.SINGLETON_ID)
                .orElseGet(() -> repo.save(new TournamentState(TournamentState.SINGLETON_ID, Finals.empty())));
    }

    public Finals finals() {
        Finals f = get().getFinals();
        return f != null ? f : Finals.empty();
    }

    public void setFinals(Finals finals) {
        TournamentState state = get();
        state.setFinals(finals);
        repo.save(state);
    }
}
