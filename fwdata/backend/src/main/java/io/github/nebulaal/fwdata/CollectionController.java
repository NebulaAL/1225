package io.github.nebulaal.fwdata;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
class CollectionController {
    record Id(long id) {}
    private final Datasets datasets;
    private final Submissions submissions;

    CollectionController(Datasets datasets, Submissions submissions) {
        this.datasets = datasets;
        this.submissions = submissions;
    }

    @GetMapping("/datasets")
    List<Datasets.Named> datasets() { return datasets.list(); }

    @GetMapping("/datasets/{id}/sources")
    List<Datasets.Named> sources(@PathVariable long id) { return datasets.sourceNames(id); }

    @PostMapping("/manage/datasets")
    @ResponseStatus(HttpStatus.CREATED)
    Id create(@AuthenticationPrincipal Accounts.Identity admin, @Valid @RequestBody Datasets.NewDataset input) {
        return new Id(datasets.create(admin.id, input));
    }

    @GetMapping("/manage/datasets/{id}")
    Datasets.Detail detail(@PathVariable long id) { return datasets.detail(id); }

    @PostMapping("/manage/datasets/{id}/sources")
    @ResponseStatus(HttpStatus.CREATED)
    Datasets.Source addSource(@PathVariable long id, @Valid @RequestBody Datasets.NewSource input) {
        return datasets.addSource(id, input.name());
    }

    @GetMapping("/public/forms/{key}")
    Datasets.PublicForm form(@PathVariable String key) { return datasets.form(key); }

    @PostMapping("/public/forms/{key}/submissions")
    @ResponseStatus(HttpStatus.CREATED)
    Id submit(@PathVariable String key, @Valid @RequestBody Submissions.Input input) {
        return new Id(submissions.submit(key, input));
    }

    @GetMapping("/submissions")
    Page<Submissions.Summary> list(@RequestParam(defaultValue = "1") int page,
        @RequestParam(required = false) Long datasetId, @RequestParam(required = false) Long sourceId) {
        return submissions.list(page, datasetId, sourceId);
    }

    @GetMapping("/submissions/{id}")
    Submissions.Detail submission(@PathVariable long id) { return submissions.detail(id); }
}
