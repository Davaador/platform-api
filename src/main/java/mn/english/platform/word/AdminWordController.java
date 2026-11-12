package mn.english.platform.word;

import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import mn.english.platform.word.dto.WordRequest;
import mn.english.platform.word.dto.WordResponse;

/** Word management. Only ADMIN users can reach /api/admin/** (see SecurityConfig). */
@RestController
@RequestMapping("/api/admin/words")
public class AdminWordController {

    private final WordService service;

    public AdminWordController(WordService service) {
        this.service = service;
    }

    @GetMapping
    public List<WordResponse> list() {
        return service.findAll().stream()
                .map(WordResponse::from)
                .toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public WordResponse create(@Valid @RequestBody WordRequest request) {
        return WordResponse.from(service.create(request));
    }

    @PutMapping("/{id}")
    public WordResponse update(@PathVariable UUID id, @Valid @RequestBody WordRequest request) {
        return WordResponse.from(service.update(id, request));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        service.delete(id);
    }
}
