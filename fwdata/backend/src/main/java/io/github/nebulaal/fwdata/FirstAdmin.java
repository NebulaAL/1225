package io.github.nebulaal.fwdata;

import jakarta.validation.Validator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
class FirstAdmin implements ApplicationRunner {
    private final Accounts accounts;
    private final JdbcClient db;
    private final Validator validator;
    private final String username;
    private final String password;

    FirstAdmin(Accounts accounts, JdbcClient db, Validator validator,
               @Value("${fwdata.admin.username}") String username,
               @Value("${fwdata.admin.password}") String password) {
        this.accounts = accounts;
        this.db = db;
        this.validator = validator;
        this.username = username;
        this.password = password;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        db.sql("SELECT id FROM bootstrap_lock WHERE id = 1 FOR UPDATE").query(Integer.class).single();
        if (db.sql("SELECT COUNT(*) FROM accounts").query(Long.class).single() > 0) {
            return;
        }
        var admin = new Accounts.NewAccount(username, "系统管理员", password, Accounts.Role.ADMIN);
        if (!validator.validate(admin).isEmpty()) {
            throw new IllegalStateException("首次启动请配置有效的 ADMIN_USERNAME 和 8 至 128 个字符的 ADMIN_PASSWORD");
        }
        accounts.create(admin);
    }
}
