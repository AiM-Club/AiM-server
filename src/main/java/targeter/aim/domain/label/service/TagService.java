package targeter.aim.domain.label.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import targeter.aim.domain.label.entity.Tag;
import targeter.aim.domain.label.repository.TagRepository;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TagService {

    private final TagRepository tagRepository;

    private static final int MAX = 3;

    public Set<Tag> findOrCreateByNames(List<String> names) {
        List<String> normalized = normalize(names);
        if (normalized.isEmpty()) return new LinkedHashSet<>();

        // 1) IN 조회로 기존 태그 먼저 가져오기
        List<Tag> found = tagRepository.findAllByNameIn(normalized);
        Map<String, Tag> foundMap = found.stream()
                .collect(Collectors.toMap(Tag::getName, t -> t));

        // 2) 없는 태그는 생성
        Set<Tag> result = new LinkedHashSet<>();
        for (String n : normalized) {
            Tag tag = foundMap.get(n);
            if (tag == null) {
                tag = tagRepository.save(Tag.builder().name(n).build());
            }
            result.add(tag);
        }
        return result;
    }

    private List<String> normalize(List<String> names) {
        if (names == null) return List.of();
        return names.stream()
                .filter(Objects::nonNull)
                .map(s -> s.trim().replaceAll("\\s+", " "))
                .filter(s -> !s.isEmpty())
                .distinct()
                .limit(MAX)
                .toList();
    }
}
