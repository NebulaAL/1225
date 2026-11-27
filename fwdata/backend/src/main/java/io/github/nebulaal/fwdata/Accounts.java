package io.github.nebulaal.fwdata;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Locale;
import java.util.List;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class Accounts implements UserDetailsService {
    enum Role { ADMIN, USER }
    record Account(long id, String username, String displayName, Role role, Instant createdAt) {}
    record NewAccount(
        @NotNull @Pattern(regexp = "[A-Za-z0-9][A-Za-z0-9_.-]{2,39}", message = "账号需为 3 至 40 个字母、数字、点、下划线或短横线，并以字母或数字开头") String username,
        @NotBlank @Size(max = 64) String displayName,
        @NotBlank @Size(min = 8, max = 128, message = "密码长度需为 8 至 128 个字符") String password,
        @NotNull Role role
    ) {}

    private static final String COLUMNS = "id, username, display_name, role, created_at";
    private final JdbcClient db;
    private final PasswordEncoder passwords;

    static class Identity extends User {
        final long id;
        Identity(long id, String username, String hash, String role) {
            super(username, hash, List.of(new SimpleGrantedAuthority("ROLE_" + role)));
            this.id = id;
        }
    }

    Accounts(JdbcClient db, PasswordEncoder passwords) {
        this.db = db;
        this.passwords = passwords;
    }

    private static Account map(ResultSet rs, int row) throws SQLException {
        return new Account(rs.getLong("id"), rs.getString("username"), rs.getString("display_name"),
            Role.valueOf(rs.getString("role")), rs.getTimestamp("created_at").toInstant());
    }

    @Override
    public UserDetails loadUserByUsername(String username) {
        return db.sql("SELECT id, username, password_hash, role FROM accounts WHERE username = ?")
            .param(username.strip().toLowerCase(Locale.ROOT))
            .query((rs, row) -> new Identity(rs.getLong("id"), rs.getString("username"),
                rs.getString("password_hash"), rs.getString("role")))
            .optional().orElseThrow(() -> new UsernameNotFoundException("账号或密码错误"));
    }

    boolean exists(long id) {
        return db.sql("SELECT COUNT(*) FROM accounts WHERE id = ?").param(id).query(Long.class).single() > 0;
    }

    void deleteUser(long id) {
        if (db.sql("DELETE FROM accounts WHERE id = ? AND role = 'USER'").param(id).update() == 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "只能删除现有的普通用户");
        }
    }

    Account current(String username) {
        return db.sql("SELECT " + COLUMNS + " FROM accounts WHERE username = ?")
            .param(username).query(Accounts::map).single();
    }

    void create(NewAccount account) {
        db.sql("INSERT INTO accounts (username, display_name, password_hash, role) VALUES (?, ?, ?, ?)")
            .params(account.username().toLowerCase(Locale.ROOT), account.displayName().strip(),
                passwords.encode(account.password()), account.role().name()).update();
    }

    @Transactional(readOnly = true)
    Page<Account> list(int page, String query) {
        long offset = Page.offset(page, query);
        String filter = " FROM accounts WHERE LOCATE(:q, username) > 0 OR LOCATE(:q, LOWER(display_name)) > 0";
        String q = query.strip().toLowerCase(Locale.ROOT);
        long total = db.sql("SELECT COUNT(*)" + filter).param("q", q).query(Long.class).single();
        var items = db.sql("SELECT " + COLUMNS + filter + " ORDER BY id DESC LIMIT :size OFFSET :offset")
            .param("q", q).param("size", Page.SIZE).param("offset", offset).query(Accounts::map).list();
        return new Page<>(items, total, page, Page.SIZE);
    }
}
