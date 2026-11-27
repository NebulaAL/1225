package io.github.nebulaal.fwdata;

import jakarta.validation.Valid;
import java.security.Principal;
import org.springframework.http.HttpStatus;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
class AccountController {
    private final Accounts accounts;

    AccountController(Accounts accounts) {
        this.accounts = accounts;
    }

    record Csrf(String headerName, String token) {}

    @GetMapping("/auth/csrf")
    Csrf csrf(CsrfToken token) {
        return new Csrf(token.getHeaderName(), token.getToken());
    }

    @GetMapping("/auth/me")
    Accounts.Account me(Principal principal) {
        return accounts.current(principal.getName());
    }

    @GetMapping("/accounts")
    Page<Accounts.Account> list(@RequestParam(defaultValue = "1") int page,
                                @RequestParam(defaultValue = "") String q) {
        return accounts.list(page, q);
    }

    @PostMapping("/accounts")
    @ResponseStatus(HttpStatus.CREATED)
    void create(@Valid @RequestBody Accounts.NewAccount input) {
        accounts.create(input);
    }

    @DeleteMapping("/accounts/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(@PathVariable long id) {
        accounts.deleteUser(id);
    }
}
