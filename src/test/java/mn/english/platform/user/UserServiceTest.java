package mn.english.platform.user;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository users;

    @InjectMocks
    private UserService userService;

    @Test
    void createTrimsAndLowercasesTheEmail() {
        when(users.existsByEmail("bat@example.com")).thenReturn(false);
        when(users.saveAndFlush(any(User.class))).thenAnswer(call -> call.getArgument(0));

        User created = userService.create("  Bat@Example.com ", "hash", " Bat ", Role.USER);

        assertEquals("bat@example.com", created.getEmail());
        assertEquals("Bat", created.getDisplayName());
        assertEquals(Role.USER, created.getRole());
    }

    @Test
    void createFailsWhenTheEmailAlreadyExists() {
        when(users.existsByEmail("bat@example.com")).thenReturn(true);

        assertThrows(
                EmailAlreadyUsedException.class,
                () -> userService.create("bat@example.com", "hash", "Bat", Role.USER));
        verify(users, never()).saveAndFlush(any());
    }

    @Test
    void createFailsWhenTheDatabaseRejectsADuplicate() {
        when(users.existsByEmail("bat@example.com")).thenReturn(false);
        when(users.saveAndFlush(any(User.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate key"));

        assertThrows(
                EmailAlreadyUsedException.class,
                () -> userService.create("bat@example.com", "hash", "Bat", Role.USER));
    }

    @Test
    void getByIdFailsWhenTheUserDoesNotExist() {
        UUID id = UUID.randomUUID();
        when(users.findById(id)).thenReturn(Optional.empty());

        assertThrows(AccountNotFoundException.class, () -> userService.getById(id));
    }
}
