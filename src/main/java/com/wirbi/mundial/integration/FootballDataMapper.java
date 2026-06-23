package com.wirbi.mundial.integration;

import com.wirbi.mundial.model.Stage;

import java.text.Normalizer;
import java.util.Map;

/**
 * Traducción proveedor → dominio (puro, testeable sin HTTP).
 *  - TLA (3 letras FIFA) → nuestro TeamCode, con overrides para el set curado;
 *    por defecto, el TLA en minúsculas.
 *  - Stage y grupo del proveedor → nuestro enum/letra.
 */
public final class FootballDataMapper {

    private FootballDataMapper() {
    }

    /**
     * Mapa TLA (3 letras FIFA) → código ISO alpha-2 (el que usa flag-icons).
     * Cubre las selecciones del Mundial 2026; lo no mapeado cae a TLA en
     * minúsculas (bandera podría no renderizar, pero el nombre es real).
     */
    static final Map<String, String> TLA_OVERRIDES = Map.ofEntries(
            // Anfitriones + UEFA
            Map.entry("MEX", "mx"), Map.entry("USA", "us"), Map.entry("CAN", "ca"),
            Map.entry("FRA", "fr"), Map.entry("ESP", "es"), Map.entry("ENG", "gb-eng"),
            Map.entry("GER", "de"), Map.entry("POR", "pt"), Map.entry("NED", "nl"),
            Map.entry("BEL", "be"), Map.entry("CRO", "hr"), Map.entry("ITA", "it"),
            Map.entry("SUI", "ch"), Map.entry("DEN", "dk"), Map.entry("AUT", "at"),
            Map.entry("SRB", "rs"), Map.entry("POL", "pl"), Map.entry("SWE", "se"),
            Map.entry("NOR", "no"), Map.entry("SCO", "gb-sct"), Map.entry("WAL", "gb-wls"),
            Map.entry("TUR", "tr"), Map.entry("CZE", "cz"), Map.entry("UKR", "ua"),
            Map.entry("HUN", "hu"), Map.entry("GRE", "gr"),
            // CONMEBOL
            Map.entry("BRA", "br"), Map.entry("ARG", "ar"), Map.entry("URU", "uy"),
            Map.entry("COL", "co"), Map.entry("ECU", "ec"), Map.entry("PER", "pe"),
            Map.entry("CHI", "cl"), Map.entry("PAR", "py"), Map.entry("VEN", "ve"),
            Map.entry("BOL", "bo"),
            // CONCACAF
            Map.entry("CRC", "cr"), Map.entry("PAN", "pa"), Map.entry("HON", "hn"),
            Map.entry("JAM", "jm"),
            // CAF
            Map.entry("MAR", "ma"), Map.entry("SEN", "sn"), Map.entry("NGA", "ng"),
            Map.entry("EGY", "eg"), Map.entry("TUN", "tn"), Map.entry("ALG", "dz"),
            Map.entry("CIV", "ci"), Map.entry("GHA", "gh"), Map.entry("CMR", "cm"),
            Map.entry("RSA", "za"), Map.entry("MLI", "ml"),
            // AFC + OFC
            Map.entry("JPN", "jp"), Map.entry("KOR", "kr"), Map.entry("IRN", "ir"),
            Map.entry("KSA", "sa"), Map.entry("AUS", "au"), Map.entry("QAT", "qa"),
            Map.entry("IRQ", "iq"), Map.entry("UAE", "ae"), Map.entry("UZB", "uz"),
            Map.entry("NZL", "nz"), Map.entry("JOR", "jo"),
            // Debutantes / repechaje 2026 (+ alias ISO-3 por si el proveedor los usa)
            Map.entry("BIH", "ba"), Map.entry("HAI", "ht"), Map.entry("HTI", "ht"),
            Map.entry("CUW", "cw"), Map.entry("CPV", "cv"), Map.entry("COD", "cd"),
            Map.entry("URY", "uy"), Map.entry("SUR", "sr"), Map.entry("NCL", "nc")
    );

    /** Nombre en español por código (el proveedor los entrega en inglés). */
    static final Map<String, String> SPANISH_NAMES = Map.ofEntries(
            Map.entry("mx", "México"), Map.entry("us", "Estados Unidos"), Map.entry("ca", "Canadá"),
            Map.entry("ar", "Argentina"), Map.entry("br", "Brasil"), Map.entry("fr", "Francia"),
            Map.entry("es", "España"), Map.entry("gb-eng", "Inglaterra"), Map.entry("de", "Alemania"),
            Map.entry("nl", "Países Bajos"), Map.entry("be", "Bélgica"), Map.entry("hr", "Croacia"),
            Map.entry("it", "Italia"), Map.entry("ch", "Suiza"), Map.entry("dk", "Dinamarca"),
            Map.entry("at", "Austria"), Map.entry("rs", "Serbia"), Map.entry("pl", "Polonia"),
            Map.entry("se", "Suecia"), Map.entry("no", "Noruega"), Map.entry("gb-sct", "Escocia"),
            Map.entry("gb-wls", "Gales"), Map.entry("tr", "Turquía"), Map.entry("cz", "Chequia"),
            Map.entry("ua", "Ucrania"), Map.entry("hu", "Hungría"), Map.entry("gr", "Grecia"),
            Map.entry("co", "Colombia"), Map.entry("ec", "Ecuador"), Map.entry("pe", "Perú"),
            Map.entry("cl", "Chile"), Map.entry("py", "Paraguay"), Map.entry("ve", "Venezuela"),
            Map.entry("bo", "Bolivia"), Map.entry("uy", "Uruguay"), Map.entry("cr", "Costa Rica"),
            Map.entry("pa", "Panamá"), Map.entry("hn", "Honduras"), Map.entry("jm", "Jamaica"),
            Map.entry("ma", "Marruecos"), Map.entry("sn", "Senegal"), Map.entry("ng", "Nigeria"),
            Map.entry("eg", "Egipto"), Map.entry("tn", "Túnez"), Map.entry("dz", "Argelia"),
            Map.entry("ci", "Costa de Marfil"), Map.entry("gh", "Ghana"), Map.entry("cm", "Camerún"),
            Map.entry("za", "Sudáfrica"), Map.entry("ml", "Malí"), Map.entry("cv", "Cabo Verde"),
            Map.entry("jp", "Japón"), Map.entry("kr", "Corea del Sur"), Map.entry("ir", "Irán"),
            Map.entry("sa", "Arabia Saudita"), Map.entry("au", "Australia"), Map.entry("qa", "Catar"),
            Map.entry("iq", "Irak"), Map.entry("ae", "Emiratos Árabes Unidos"), Map.entry("uz", "Uzbekistán"),
            Map.entry("nz", "Nueva Zelanda"), Map.entry("jo", "Jordania"), Map.entry("pt", "Portugal"),
            Map.entry("ba", "Bosnia y Herzegovina"), Map.entry("ht", "Haití"), Map.entry("cw", "Curazao"),
            Map.entry("cd", "RD Congo"), Map.entry("sr", "Surinam"), Map.entry("nc", "Nueva Caledonia")
    );

    /** Nombre del equipo en español por código; si no está, usa el fallback. */
    public static String localizedName(String code, String fallback) {
        if (code == null) return fallback;
        return SPANISH_NAMES.getOrDefault(code, fallback);
    }

    /**
     * Fallback por NOMBRE (normalizado sin acentos) cuando el TLA del proveedor
     * no está mapeado o es raro (p. ej. Curaçao). Garantiza el código ISO correcto.
     */
    static final Map<String, String> NAME_OVERRIDES = Map.ofEntries(
            Map.entry("curacao", "cw"), Map.entry("cape verde", "cv"),
            Map.entry("cape verde islands", "cv"), Map.entry("cabo verde", "cv"),
            Map.entry("dr congo", "cd"), Map.entry("congo dr", "cd"),
            Map.entry("democratic republic of congo", "cd"),
            Map.entry("bosnia and herzegovina", "ba"), Map.entry("bosnia-herzegovina", "ba"),
            Map.entry("bosnia herzegovina", "ba"), Map.entry("haiti", "ht"),
            Map.entry("south africa", "za"), Map.entry("south korea", "kr"),
            Map.entry("korea republic", "kr"), Map.entry("ivory coast", "ci"),
            Map.entry("cote d'ivoire", "ci"), Map.entry("new zealand", "nz"),
            Map.entry("new caledonia", "nc"), Map.entry("saudi arabia", "sa"),
            Map.entry("united arab emirates", "ae"), Map.entry("united states", "us"),
            Map.entry("czechia", "cz"), Map.entry("czech republic", "cz"),
            Map.entry("suriname", "sr")
    );

    private static String normalize(String s) {
        return Normalizer.normalize(s, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase()
                .trim();
    }

    /**
     * Normaliza un nombre para comparaciones tolerantes a acentos y mayúsculas
     * (p. ej. emparejar el goleador del proveedor con nuestro jugador). Vacío si null.
     */
    public static String normalizeName(String s) {
        return s == null || s.isBlank() ? "" : normalize(s);
    }

    /**
     * TeamCode desde un equipo del proveedor: 1) override por TLA, 2) override
     * por nombre normalizado, 3) TLA en minúsculas. null si TBD / sin datos.
     */
    public static String teamCode(ProviderTeam team) {
        if (team == null) return null;
        String tla = team.tla();
        if (tla != null && !tla.isBlank()) {
            String up = tla.toUpperCase();
            if (TLA_OVERRIDES.containsKey(up)) return TLA_OVERRIDES.get(up);
        }
        if (team.name() != null && !team.name().isBlank()) {
            String key = normalize(team.name());
            if (NAME_OVERRIDES.containsKey(key)) return NAME_OVERRIDES.get(key);
        }
        if (tla != null && !tla.isBlank()) return tla.toLowerCase();
        return null;
    }

    public static String teamCodeFromTla(String tla) {
        if (tla == null || tla.isBlank()) return null;
        String up = tla.toUpperCase();
        return TLA_OVERRIDES.getOrDefault(up, up.toLowerCase());
    }

    /** Stage del proveedor → nuestro enum (default GROUP). */
    public static Stage toStage(String providerStage) {
        if (providerStage == null) return Stage.GROUP;
        return switch (providerStage) {
            case "LAST_32" -> Stage.ROUND_OF_32;
            case "LAST_16" -> Stage.ROUND_OF_16;
            case "QUARTER_FINALS" -> Stage.QUARTER_FINAL;
            case "SEMI_FINALS" -> Stage.SEMI_FINAL;
            case "THIRD_PLACE" -> Stage.THIRD_PLACE;
            case "FINAL" -> Stage.FINAL;
            default -> Stage.GROUP; // GROUP_STAGE u otros
        };
    }

    /** "GROUP_A" → "A"; null/otros → null. */
    public static String groupLetter(String providerGroup) {
        if (providerGroup == null || providerGroup.isBlank()) return null;
        return providerGroup.startsWith("GROUP_") ? providerGroup.substring(6) : providerGroup;
    }
}
