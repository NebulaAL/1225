package io.github.nebulaal.fwdata;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.*;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
class Submissions {
    record Answer(@Positive long fieldId, @Size(max = 4000) String text,
                  @NotNull @Size(max = 50) List<@NotNull @Positive Long> optionIds) {}
    record Input(@NotNull @Pattern(regexp = "[0-9a-f]{32}") String requestKey,
                 @NotNull @Size(max = 50) List<@NotNull @Valid Answer> answers) {}
    record Summary(long id, String datasetName, String sourceName, Instant submittedAt) {}
    record AnswerView(String label, Datasets.Kind kind, String text, List<String> selected) {}
    record Detail(Summary submission, List<AnswerView> answers) {}
    private static final String JOINS = """
         FROM submissions s JOIN datasets d ON d.id = s.dataset_id
         JOIN collection_sources c ON c.id = s.source_id
        """;
    private static final String SELECT = "SELECT s.id, d.name AS dataset_name, c.name AS source_name, s.submitted_at";
    private final JdbcClient db;
    private final Datasets datasets;

    Submissions(JdbcClient db, Datasets datasets) {
        this.db = db;
        this.datasets = datasets;
    }

    @Transactional
    long submit(String key, Input input) {
        var destination = datasets.destination(key);
        var fields = datasets.fields(destination.datasetId());
        var answers = validate(fields, input.answers());
        long id;
        try {
            id = Datasets.insertedId(db.sql("INSERT INTO submissions (dataset_id, source_id, request_key) VALUES (?, ?, ?)")
                .params(destination.datasetId(), destination.sourceId(), input.requestKey()));
        } catch (DuplicateKeyException retry) {
            return db.sql("SELECT id FROM submissions WHERE source_id = ? AND request_key = ? FOR UPDATE")
                .params(destination.sourceId(), input.requestKey()).query(Long.class).single();
        }
        for (var field : fields) {
            Answer answer = answers.get(field.id());
            if (answer == null) continue;
            var text = field.kind() == Datasets.Kind.TEXT ? answer.text().strip() : null;
            db.sql("INSERT INTO submission_answers (submission_id, dataset_id, field_id, text_value) VALUES (?, ?, ?, ?)")
                .params(id, destination.datasetId(), field.id(), text).update();
            for (long option : answer.optionIds()) {
                db.sql("INSERT INTO answer_choices (submission_id, field_id, option_id) VALUES (?, ?, ?)")
                    .params(id, field.id(), option).update();
            }
        }
        return id;
    }

    private static Map<Long, Answer> validate(List<Datasets.Field> fields, List<Answer> input) {
        Map<Long, Answer> remaining = new HashMap<>();
        for (Answer answer : input) {
            if (remaining.put(answer.fieldId(), answer) != null) throw invalid("同一道题不能重复作答");
        }
        Map<Long, Answer> valid = new HashMap<>();
        for (var field : fields) {
            Answer answer = remaining.remove(field.id());
            boolean hasText = answer != null && answer.text() != null && !answer.text().isBlank();
            boolean hasChoices = answer != null && !answer.optionIds().isEmpty();
            if (!hasText && !hasChoices) {
                if (field.required()) throw invalid("请填写「" + field.label() + "」");
                continue;
            }
            if (field.kind() == Datasets.Kind.TEXT) {
                if (hasChoices) throw invalid("文本题只能填写文字");
            } else {
                if (hasText || (field.kind() == Datasets.Kind.SINGLE && answer.optionIds().size() != 1)) {
                    throw invalid("请检查「" + field.label() + "」的选择");
                }
                Set<Long> choices = new HashSet<>(answer.optionIds());
                var allowed = field.options().stream().map(Datasets.Named::id).toList();
                if (choices.size() != answer.optionIds().size() || !allowed.containsAll(choices)) {
                    throw invalid("选项不属于当前题目或重复");
                }
            }
            valid.put(field.id(), answer);
        }
        if (!remaining.isEmpty()) throw invalid("答案包含当前数据集以外的题目");
        return valid;
    }

    private static ResponseStatusException invalid(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }

    private static Summary summary(ResultSet rs, int row) throws SQLException {
        return new Summary(rs.getLong("id"), rs.getString("dataset_name"), rs.getString("source_name"),
            rs.getTimestamp("submitted_at").toInstant());
    }

    @Transactional(readOnly = true)
    Page<Summary> list(int page, Long datasetId, Long sourceId) {
        long offset = Page.offset(page, "");
        String where = " WHERE 1 = 1";
        Map<String, Object> params = new HashMap<>();
        if (datasetId != null) { where += " AND s.dataset_id = :dataset"; params.put("dataset", datasetId); }
        if (sourceId != null) { where += " AND s.source_id = :source"; params.put("source", sourceId); }
        long total = db.sql("SELECT COUNT(*) FROM submissions s" + where).params(params).query(Long.class).single();
        var items = db.sql(SELECT + JOINS + where + " ORDER BY s.id DESC LIMIT :size OFFSET :offset")
            .params(params).param("size", Page.SIZE).param("offset", offset).query(Submissions::summary).list();
        return new Page<>(items, total, page, Page.SIZE);
    }

    @Transactional(readOnly = true)
    Detail detail(long id) {
        var summary = db.sql(SELECT + JOINS + " WHERE s.id = ?").param(id).query(Submissions::summary)
            .optional().orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "提交记录不存在"));
        var answers = db.sql("""
            SELECT f.id, f.label, f.kind, a.text_value, o.label AS choice
            FROM submissions s JOIN dataset_fields f ON f.dataset_id = s.dataset_id
            LEFT JOIN submission_answers a ON a.submission_id = s.id AND a.field_id = f.id
            LEFT JOIN answer_choices ch ON ch.submission_id = a.submission_id AND ch.field_id = a.field_id
            LEFT JOIN field_options o ON o.id = ch.option_id
            WHERE s.id = ? ORDER BY f.position_no, o.position_no
            """).param(id).query(rs -> {
                Map<Long, AnswerView> result = new LinkedHashMap<>();
                while (rs.next()) {
                    long fieldId = rs.getLong("id");
                    var view = result.get(fieldId);
                    if (view == null) {
                        view = new AnswerView(rs.getString("label"), Datasets.Kind.valueOf(rs.getString("kind")),
                            rs.getString("text_value"), new ArrayList<>());
                        result.put(fieldId, view);
                    }
                    String choice = rs.getString("choice");
                    if (choice != null) view.selected().add(choice);
                }
                return List.copyOf(result.values());
            });
        return new Detail(summary, answers);
    }
}
