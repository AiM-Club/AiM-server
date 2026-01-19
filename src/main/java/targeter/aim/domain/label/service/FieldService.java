package targeter.aim.domain.label.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import targeter.aim.domain.label.entity.Field;
import targeter.aim.domain.label.repository.FieldRepository;
import targeter.aim.system.exception.model.ErrorCode;
import targeter.aim.system.exception.model.RestException;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FieldService {

    private final FieldRepository fieldRepository;

    private static final int MAX = 3;

    @Transactional(readOnly = true)
    public Set<Field> findFieldByName(List<String> names) {
        List<String> normalized = normalize(names);
        if(normalized.isEmpty()) return new LinkedHashSet<>();

        List<Field> fields = fieldRepository.findAll();
        List<Field> result = fields.stream()
                .filter(field -> normalized.contains(field.getName()))
                .collect(Collectors.toList());

        if(result.size() != normalized.size()) {
            throw new RestException(ErrorCode.GLOBAL_BAD_REQUEST, "존재하지 않는 분야 이름이 포함되어 있습니다.");
        }

        Map<String, Field> map = result.stream().collect(Collectors.toMap(Field::getName, field -> field));
        Set<Field> resultSet = new LinkedHashSet<>();
        for(String f: normalized) resultSet.add(map.get(f));
        return resultSet;
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
