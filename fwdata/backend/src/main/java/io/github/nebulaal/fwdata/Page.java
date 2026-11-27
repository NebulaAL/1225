package io.github.nebulaal.fwdata;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

record Page<T>(List<T> items, long total, int page, int pageSize) {
    static final int SIZE = 20;

    static long offset(int page, String query) {
        if (page < 1 || query.length() > 120) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请检查页码或搜索词长度");
        }
        return (long) (page - 1) * SIZE;
    }
}
