package com.wirbi.mundial.dto;

import java.util.List;

/** Un grupo del torneo con sus selecciones. */
public record GroupDto(String group, List<TeamDto> teams) {
}
