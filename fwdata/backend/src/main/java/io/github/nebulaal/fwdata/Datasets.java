package io.github.nebulaal.fwdata;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
class Datasets {
    enum Kind { SINGLE, MULTIPLE, TEXT }
    record Named(long id, String name) {}
    record Field(long id, String label, Kind kind, boolean required, List<Named> options) {}
    record Dataset(long id, String name, String description) {}
    record Source(long id, String name, String publicKey) {}
    record Detail(Dataset dataset, List<Field> fields, List<Source> sources) {}
    record PublicForm(Dataset dataset, String sourceName, List<Field> fields) {}
    record Destination(long datasetId, long sourceId, String sourceName) {}
    record NewSource(@NotBlank @Size(max = 80) String name) {}
    record NewField(@NotBlank @Size(max = 120) String label, @NotNull Kind kind,
                    boolean required, @NotNull @Size(max = 50) List<@NotBlank @Size(max = 120) String> options) {}
    record NewDataset(@NotBlank @Size(max = 120) String name, @Size(max = 1000) String description,
                      @NotEmpty @Size(max = 50) List<@NotNull @Valid NewField> fields) {}
    private final JdbcClient db;

    Datasets(JdbcClient db) { this.db = db; }

    static long insertedId(JdbcClient.StatementSpec insert) {
        var key = new GeneratedKeyHolder();
        insert.update(key, "id");
        return Objects.requireNonNull(key.getKey()).longValue();
    }

    List<Named> list() {
        return db.sql("SELECT id, name FROM datasets ORDER BY id DESC")
            .query((rs, row) -> new Named(rs.getLong("id"), rs.getString("name"))).list();
    }

    List<Named> sourceNames(long datasetId) {
        return db.sql("SELECT id, name FROM collection_sources WHERE dataset_id = ? ORDER BY id")
            .param(datasetId).query((rs, row) -> new Named(rs.getLong("id"), rs.getString("name"))).list();
    }

    Dataset dataset(long id) {
        return db.sql("SELECT id, name, description FROM datasets WHERE id = ?").param(id)
            .query((rs, row) -> new Dataset(id, rs.getString("name"), rs.getString("description")))
            .optional().orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "数据集不存在"));
    }

    List<Field> fields(long datasetId) {
        return db.sql("""
            SELECT f.id, f.label, f.kind, f.required, o.id AS option_id, o.label AS option_label
            FROM dataset_fields f LEFT JOIN field_options o ON o.field_id = f.id
            WHERE f.dataset_id = ? ORDER BY f.position_no, o.position_no
            """).param(datasetId).query(rs -> {
                Map<Long, Field> fields = new LinkedHashMap<>();
                while (rs.next()) {
                    long id = rs.getLong("id");
                    Field field = fields.get(id);
                    if (field == null) {
                        field = new Field(id, rs.getString("label"), Kind.valueOf(rs.getString("kind")),
                            rs.getBoolean("required"), new ArrayList<>());
                        fields.put(id, field);
                    }
                    Long optionId = rs.getObject("option_id", Long.class);
                    if (optionId != null) field.options().add(new Named(optionId, rs.getString("option_label")));
                }
                return List.copyOf(fields.values());
            });
    }

    Detail detail(long id) {
        Dataset dataset = dataset(id);
        var sources = db.sql("SELECT id, name, public_key FROM collection_sources WHERE dataset_id = ? ORDER BY id")
            .param(id).query((rs, row) -> new Source(rs.getLong("id"), rs.getString("name"), rs.getString("public_key"))).list();
        return new Detail(dataset, fields(id), sources);
    }

    Destination destination(String key) {
        return db.sql("SELECT dataset_id, id, name FROM collection_sources WHERE public_key = ?")
            .param(key).query((rs, row) -> new Destination(rs.getLong("dataset_id"), rs.getLong("id"), rs.getString("name")))
            .optional().orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "填报链接无效"));
    }

    PublicForm form(String key) {
        Destination d = destination(key);
        return new PublicForm(dataset(d.datasetId()), d.sourceName(), fields(d.datasetId()));
    }

    @Transactional
    long create(long adminId, NewDataset input) {
        long id = insertedId(db.sql("INSERT INTO datasets (name, description, created_by) VALUES (?, ?, ?)")
            .params(input.name().strip(), Objects.toString(input.description(), "").strip(), adminId));
        int fieldNo = 0;
        for (NewField field : input.fields()) {
            var options = field.options().stream().map(String::strip).toList();
            if (field.kind() == Kind.TEXT ? !options.isEmpty() : options.size() < 2) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "选择题至少需要两个选项，文本题的选项应为空");
            }
            var unique = new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);
            unique.addAll(options);
            if (unique.size() != options.size()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "同一道题的选项不能重复");
            }
            long fieldId = insertedId(db.sql("""
                INSERT INTO dataset_fields (dataset_id, label, kind, required, position_no) VALUES (?, ?, ?, ?, ?)
                """).params(id, field.label().strip(), field.kind().name(), field.required(), ++fieldNo));
            int optionNo = 0;
            for (String label : options) {
                db.sql("INSERT INTO field_options (field_id, label, position_no) VALUES (?, ?, ?)")
                    .params(fieldId, label, ++optionNo).update();
            }
        }
        addSource(id, "默认来源");
        return id;
    }

    Source addSource(long datasetId, String name) {
        dataset(datasetId);
        String key = UUID.randomUUID().toString().replace("-", "");
        long id = insertedId(db.sql("INSERT INTO collection_sources (dataset_id, name, public_key) VALUES (?, ?, ?)")
            .params(datasetId, name.strip(), key));
        return new Source(id, name.strip(), key);
    }
}
