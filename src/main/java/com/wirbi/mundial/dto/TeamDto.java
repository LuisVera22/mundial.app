package com.wirbi.mundial.dto;

import java.util.List;

/** Selección del torneo. {@code fans} = usuarios que la pickearon campeona. */
public record TeamDto(String code, String name, String group, List<FanDto> fans) {
}
