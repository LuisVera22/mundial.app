package com.wirbi.mundial.dto;

import java.util.List;

/** Detalle de una selección con sus partidos (programados + jugados). */
public record TeamDetailDto(String code, String name, String group, List<FanDto> fans,
                            List<MatchDto> matches) {
}
