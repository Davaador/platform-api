package mn.english.platform.admin;

import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Placeholder. The real admin features (words, sentences, books) will live in this package.
 * Access is limited to ADMIN in SecurityConfig.
 */
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    @GetMapping("/ping")
    public Map<String, String> ping() {
        return Map.of("status", "ok", "area", "admin");
    }
}
