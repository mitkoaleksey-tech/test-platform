package com.example.test_platform.service;

import com.example.test_platform.domain.entity.KimTaskSetting;
import com.example.test_platform.domain.entity.SystemExamType;
import com.example.test_platform.domain.entity.SystemSubject;
import com.example.test_platform.domain.entity.SystemTaskBank;
import com.example.test_platform.exception.ApiException;
import com.example.test_platform.repository.KimTaskSettingRepository;
import com.example.test_platform.repository.SystemExamTypeRepository;
import com.example.test_platform.repository.SystemSubjectRepository;
import com.example.test_platform.repository.SystemTaskBankRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DictionaryService {

    private final SystemSubjectRepository subjectRepository;
    private final SystemExamTypeRepository examTypeRepository;
    private final SystemTaskBankRepository taskBankRepository;
    private final KimTaskSettingRepository kimTaskSettingRepository;
    private final jakarta.persistence.EntityManager entityManager;

    @PostConstruct
    @Transactional
    public void initDefaultDictionaries() {
        try {
            entityManager.createNativeQuery("ALTER TABLE tasks ADD COLUMN IF NOT EXISTS has_detailed_answer BOOLEAN DEFAULT FALSE").executeUpdate();
        } catch (Exception ignored) {}
        if (!subjectRepository.existsByName("MATHEMATICS")) {
            SystemSubject math = new SystemSubject();
            math.setName("MATHEMATICS");
            math.setDisplayName("Математика (ОГЭ)");
            math.setTotalTasks(25);
            subjectRepository.save(math);
        } else {
            subjectRepository.findByName("MATHEMATICS").ifPresent(s -> {
                s.setTotalTasks(25);
                s.setDisplayName("Математика (ОГЭ)");
                subjectRepository.save(s);
            });
        }

        if (!subjectRepository.existsByName("MATHEMATICS_BASE")) {
            SystemSubject mathBase = new SystemSubject();
            mathBase.setName("MATHEMATICS_BASE");
            mathBase.setDisplayName("Математика (Базовый уровень)");
            mathBase.setTotalTasks(21);
            subjectRepository.save(mathBase);
        } else {
            subjectRepository.findByName("MATHEMATICS_BASE").ifPresent(s -> {
                s.setTotalTasks(21);
                subjectRepository.save(s);
            });
        }

        if (!subjectRepository.existsByName("MATHEMATICS_PROF")) {
            SystemSubject mathProf = new SystemSubject();
            mathProf.setName("MATHEMATICS_PROF");
            mathProf.setDisplayName("Математика (Профильный уровень)");
            mathProf.setTotalTasks(19);
            subjectRepository.save(mathProf);
        } else {
            subjectRepository.findByName("MATHEMATICS_PROF").ifPresent(s -> {
                s.setTotalTasks(19);
                subjectRepository.save(s);
            });
        }

        if (!examTypeRepository.existsByName("EGE")) {
            SystemExamType ege = new SystemExamType();
            ege.setName("EGE");
            ege.setDisplayName("ЕГЭ");
            examTypeRepository.save(ege);
        }

        if (!examTypeRepository.existsByName("OGE")) {
            SystemExamType oge = new SystemExamType();
            oge.setName("OGE");
            oge.setDisplayName("ОГЭ");
            examTypeRepository.save(oge);
        }

        if (!taskBankRepository.existsByName("FIPI")) {
            SystemTaskBank fipi = new SystemTaskBank();
            fipi.setName("FIPI");
            fipi.setDisplayName("ФИПИ");
            taskBankRepository.save(fipi);
        }
    }

    private final com.example.test_platform.repository.TaskRepository taskRepository;

    @Transactional
    public List<SystemSubject> getSubjects() {
        try {
            List<SystemSubject> list = subjectRepository.findAll();
            for (SystemSubject s : list) {
                boolean dirty = false;
                if (s.getName() == null) { s.setName("SUBJECT_" + s.getId()); dirty = true; }
                if (s.getDisplayName() == null || s.getDisplayName().isBlank()) { s.setDisplayName(s.getName()); dirty = true; }
                if ("MATHEMATICS".equalsIgnoreCase(s.getName()) && (s.getTotalTasks() == null || s.getTotalTasks() == 27)) { s.setTotalTasks(25); dirty = true; }
                if ("MATHEMATICS_BASE".equalsIgnoreCase(s.getName()) && (s.getTotalTasks() == null || s.getTotalTasks() == 27)) { s.setTotalTasks(21); dirty = true; }
                if ("MATHEMATICS_PROF".equalsIgnoreCase(s.getName()) && (s.getTotalTasks() == null || s.getTotalTasks() == 27)) { s.setTotalTasks(19); dirty = true; }
                if (dirty) subjectRepository.save(s);
            }
            return list;
        } catch (Exception ex) {
            return java.util.List.of();
        }
    }

    @Transactional
    public SystemSubject addSubject(String name, String displayName, Integer totalTasks) {
        String cleanName = resolveIdentifier(name, displayName);
        if (subjectRepository.existsByName(cleanName)) {
            throw new ApiException(HttpStatus.CONFLICT, "Предмет уже существует");
        }
        SystemSubject s = new SystemSubject();
        s.setName(cleanName);
        s.setDisplayName(displayName.trim());
        if (totalTasks != null && totalTasks > 0) {
            s.setTotalTasks(totalTasks);
        }
        return subjectRepository.save(s);
    }

    @Transactional
    public SystemSubject updateSubject(Long id, String displayName, Integer totalTasks) {
        SystemSubject s = subjectRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Предмет не найден"));
        if (displayName != null && !displayName.isBlank()) {
            s.setDisplayName(displayName.trim());
        }
        if (totalTasks != null && totalTasks > 0) {
            s.setTotalTasks(totalTasks);
        }
        return subjectRepository.save(s);
    }

    @Transactional(readOnly = true)
    public List<SystemExamType> getExamTypes() {
        List<SystemExamType> list = examTypeRepository.findAll();
        for (SystemExamType e : list) {
            if (e.getName() == null) e.setName("EXAM_" + e.getId());
            if (e.getDisplayName() == null) e.setDisplayName(e.getName());
        }
        return list;
    }

    @Transactional
    public SystemExamType addExamType(String name, String displayName) {
        String cleanName = resolveIdentifier(name, displayName);
        if (examTypeRepository.existsByName(cleanName)) {
            throw new ApiException(HttpStatus.CONFLICT, "Тип экзамена уже существует");
        }
        SystemExamType e = new SystemExamType();
        e.setName(cleanName);
        e.setDisplayName(displayName.trim());
        return examTypeRepository.save(e);
    }

    @Transactional(readOnly = true)
    public List<SystemTaskBank> getTaskBanks() {
        List<SystemTaskBank> list = taskBankRepository.findAll();
        for (SystemTaskBank b : list) {
            if (b.getName() == null) b.setName("BANK_" + b.getId());
            if (b.getDisplayName() == null) b.setDisplayName(b.getName());
        }
        return list;
    }

    @Transactional
    public SystemTaskBank addTaskBank(String name, String displayName) {
        String cleanName = resolveIdentifier(name, displayName);
        if (taskBankRepository.existsByName(cleanName)) {
            throw new ApiException(HttpStatus.CONFLICT, "Банк задач уже существует");
        }
        SystemTaskBank b = new SystemTaskBank();
        b.setName(cleanName);
        b.setDisplayName(displayName.trim());
        return taskBankRepository.save(b);
    }

    @Transactional
    public void deleteSubject(Long id) {
        SystemSubject subject = subjectRepository.findById(id).orElse(null);
        if (subject == null) return;

        String name = subject.getName().toUpperCase();
        if (name.equals("MATHEMATICS") || name.equals("MATHEMATICS_BASE") || name.equals("MATHEMATICS_PROF")) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Запрещено удалять системные предметы КИМ");
        }

        try {
            com.example.test_platform.domain.enums.Subject enumVal = com.example.test_platform.domain.enums.Subject.valueOf(name);
            if (taskRepository.countBySubject(enumVal) > 0) {
                throw new ApiException(HttpStatus.CONFLICT, "Нельзя удалить предмет, так как к нему привязаны задачи в банке");
            }
        } catch (IllegalArgumentException ignored) {}

        subjectRepository.delete(subject);
    }

    @Transactional
    public SystemExamType updateExamType(Long id, String displayName) {
        SystemExamType e = examTypeRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Тип экзамена не найден"));
        if (displayName != null && !displayName.isBlank()) {
            e.setDisplayName(displayName.trim());
        }
        return examTypeRepository.save(e);
    }

    @Transactional
    public void deleteExamType(Long id) {
        SystemExamType exam = examTypeRepository.findById(id).orElse(null);
        if (exam == null) return;

        String name = exam.getName().toUpperCase();
        if (name.equals("EGE") || name.equals("OGE")) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Запрещено удалять системные типы экзаменов (ЕГЭ/ОГЭ)");
        }

        try {
            com.example.test_platform.domain.enums.ExamType enumVal = com.example.test_platform.domain.enums.ExamType.valueOf(name);
            if (taskRepository.countByExamType(enumVal) > 0) {
                throw new ApiException(HttpStatus.CONFLICT, "Нельзя удалить тип экзамена, так как к нему привязаны задачи");
            }
        } catch (IllegalArgumentException ignored) {}

        examTypeRepository.delete(exam);
    }

    @Transactional
    public SystemTaskBank updateTaskBank(Long id, String displayName) {
        SystemTaskBank b = taskBankRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Банк задач не найден"));
        if (displayName != null && !displayName.isBlank()) {
            b.setDisplayName(displayName.trim());
        }
        return taskBankRepository.save(b);
    }

    @Transactional
    public void deleteTaskBank(Long id) {
        SystemTaskBank bank = taskBankRepository.findById(id).orElse(null);
        if (bank == null) return;

        String name = bank.getName().toUpperCase();
        if (name.equals("FIPI")) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Запрещено удалять системный банк задач ФИПИ");
        }

        try {
            com.example.test_platform.domain.enums.TaskBank enumVal = com.example.test_platform.domain.enums.TaskBank.valueOf(name);
            if (taskRepository.countByTaskBank(enumVal) > 0) {
                throw new ApiException(HttpStatus.CONFLICT, "Нельзя удалить банк задач, так как к нему привязаны задачи");
            }
        } catch (IllegalArgumentException ignored) {}

        taskBankRepository.delete(bank);
    }

    private String resolveIdentifier(String name, String displayName) {
        if (name != null && !name.isBlank()) {
            return name.trim().toUpperCase();
        }
        if (displayName == null || displayName.isBlank()) {
            return "ITEM_" + System.currentTimeMillis();
        }
        String translit = transliterate(displayName.trim()).toUpperCase().replaceAll("[^A-Z0-9_]", "_");
        if (translit.isBlank() || translit.replaceAll("_", "").isEmpty()) {
            return "ITEM_" + System.currentTimeMillis();
        }
        return translit;
    }

    private String transliterate(String text) {
        char[] abcCyr = {'а','б','в','г','д','е','ё','ж','з','и','й','к','л','м','н','о','п','р','с','т','у','ф','х','ц','ч','ш','щ','ъ','ы','ь','э','ю','я'};
        String[] abcLat = {"a","b","v","g","d","e","e","zh","z","i","y","k","l","m","n","o","p","r","s","t","u","f","h","ts","ch","sh","sch","","y","","e","yu","ya"};
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char c = Character.toLowerCase(text.charAt(i));
            int idx = -1;
            for (int j = 0; j < abcCyr.length; j++) {
                if (abcCyr[j] == c) {
                    idx = j;
                    break;
                }
            }
            if (idx >= 0) {
                sb.append(abcLat[idx]);
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    @Transactional(readOnly = true)
    public List<KimTaskSetting> getKimSettings(String subjectName, String examName) {
        return kimTaskSettingRepository.findBySubjectNameAndExamNameOrderByTaskNumberAsc(subjectName, examName);
    }

    @Transactional
    public KimTaskSetting setTaskMaxScore(String subjectName, String examName, Integer taskNumber, Integer maxScore) {
        KimTaskSetting setting = kimTaskSettingRepository
                .findBySubjectNameAndExamNameAndTaskNumber(subjectName, examName, taskNumber)
                .orElseGet(() -> {
                    KimTaskSetting s = new KimTaskSetting();
                    s.setSubjectName(subjectName);
                    s.setExamName(examName);
                    s.setTaskNumber(taskNumber);
                    return s;
                });
        setting.setMaxScore(maxScore != null && maxScore > 0 ? maxScore : 1);
        return kimTaskSettingRepository.save(setting);
    }
}
