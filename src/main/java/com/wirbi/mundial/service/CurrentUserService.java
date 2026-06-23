package com.wirbi.mundial.service;

import com.wirbi.mundial.model.GlobalPicks;
import com.wirbi.mundial.model.User;
import com.wirbi.mundial.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Usuario actual. Con autenticación activa (Entra ID) el id es el claim
 * {@code oid} del JWT — estable por usuario en el tenant — y el nombre viene
 * de los claims del token. Sin autenticación (desarrollo) se usa el header
 * {@code X-Dev-User-Id} o un default configurable.
 */
@Service
public class CurrentUserService {

    public static final String DEV_HEADER = "X-Dev-User-Id";

    private final UserRepository users;
    private final String defaultId;

    public CurrentUserService(UserRepository users,
                              @Value("${app.current-user.default-id:me}") String defaultId) {
        this.users = users;
        this.defaultId = defaultId;
    }

    public String currentUserId() {
        var jwt = currentJwt();
        if (jwt != null) {
            String oid = jwt.getToken().getClaimAsString("oid");
            if (oid != null && !oid.isBlank()) {
                return oid;
            }
        }
        HttpServletRequest req = currentRequest();
        if (req != null) {
            String header = req.getHeader(DEV_HEADER);
            if (header != null && !header.isBlank()) {
                return header.trim();
            }
        }
        return defaultId;
    }

    /** Carga el usuario actual; lo auto-provisiona en su primer acceso. */
    public User currentUser() {
        String id = currentUserId();
        return users.findById(id).orElseGet(() -> users.save(
                new User(id, displayName(id), hueFor(id), null, false, GlobalPicks.empty(), false)));
    }

    /** Actualiza el avatar elegido del usuario actual. */
    public User updateAvatar(String avatar) {
        User u = currentUser();
        u.setAvatar(avatar);
        return users.save(u);
    }

    /** Nombre para mostrar: claims del token o un fallback legible. */
    private String displayName(String id) {
        var jwt = currentJwt();
        if (jwt != null) {
            String name = jwt.getToken().getClaimAsString("name");
            if (name != null && !name.isBlank()) {
                return name;
            }
            String upn = jwt.getToken().getClaimAsString("preferred_username");
            if (upn != null && !upn.isBlank()) {
                return upn;
            }
        }
        return "Colaborador " + id;
    }

    /** Matiz de avatar determinístico por usuario (estable entre sesiones). */
    private static int hueFor(String id) {
        return Math.floorMod(id.hashCode(), 360);
    }

    private JwtAuthenticationToken currentJwt() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth instanceof JwtAuthenticationToken jwt ? jwt : null;
    }

    private HttpServletRequest currentRequest() {
        var attrs = RequestContextHolder.getRequestAttributes();
        if (attrs instanceof ServletRequestAttributes sra) {
            return sra.getRequest();
        }
        return null;
    }
}
