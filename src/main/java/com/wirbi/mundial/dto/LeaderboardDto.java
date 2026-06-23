package com.wirbi.mundial.dto;

import java.util.List;

public record LeaderboardDto(int round, int participants, List<LeaderboardRowDto> rows) {
}
