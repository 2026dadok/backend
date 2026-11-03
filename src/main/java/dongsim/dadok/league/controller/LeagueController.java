package dongsim.dadok.league.controller;

import dongsim.dadok.league.domain.dto.WeekCompetitionResponse;
import dongsim.dadok.league.service.LeagueService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class LeagueController {

    private final LeagueService leagueService;

    // 주간 경쟁 페이지
    @GetMapping("/main/week_competition")
    public ResponseEntity<WeekCompetitionResponse> getWeekCompetition(Authentication authentication) {
        return ResponseEntity.ok(leagueService.getWeekCompetition(authentication.getName()));
    }
}
