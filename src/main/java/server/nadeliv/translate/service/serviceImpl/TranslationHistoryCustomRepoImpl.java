package server.nadeliv.translate.service.serviceImpl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.*;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;
import server.nadeliv.translate.dto.LanguageStatistics;
import server.nadeliv.translate.dto.TranslationStatistics;
import server.nadeliv.translate.model.entities.TranslationHistory;
import server.nadeliv.translate.repo.TranslationHistoryCustomRepo;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Repository
@RequiredArgsConstructor
public class TranslationHistoryCustomRepoImpl implements TranslationHistoryCustomRepo {

    private final MongoTemplate mongoTemplate;

    @Override
    public List<LanguageStatistics> getLanguageStatistics(String userId) {
        Aggregation aggregation = Aggregation.newAggregation(
                Aggregation.match(Criteria.where("userId").is(userId)),
                Aggregation.group("sourceLanguage", "targetLanguage")
                        .count().as("count"),
                Aggregation.project()
                        .and("_id.sourceLanguage").as("sourceLanguage")
                        .and("_id.targetLanguage").as("targetLanguage")
                        .and("count").as("count"),
                Aggregation.sort(Sort.Direction.DESC, "count")
        );

        AggregationResults<LanguageStatistics> results = mongoTemplate.aggregate(
                aggregation, "translation_history", LanguageStatistics.class
        );

        return results.getMappedResults();
    }

    @Override
    public TranslationStatistics getTranslationStatistics(String userId, LocalDateTime startDate, LocalDateTime endDate) {
        TranslationStatistics stats = new TranslationStatistics();
        stats.setStartDate(startDate);
        stats.setEndDate(endDate);

        // 총 번역 수
        Query totalQuery = new Query(Criteria.where("userId").is(userId)
                .and("created").gte(startDate).lte(endDate));
        long totalCount = mongoTemplate.count(totalQuery, TranslationHistory.class);
        stats.setTotalTranslations(totalCount);

        // 좋아요한 번역 수
        Query likedQuery = new Query(Criteria.where("userId").is(userId)
                .and("isLiked").is(true)
                .and("created").gte(startDate).lte(endDate));
        long likedCount = mongoTemplate.count(likedQuery, TranslationHistory.class);
        stats.setLikedTranslations(likedCount);

        // 가장 많이 사용한 언어 쌍 찾기
        Aggregation languageAgg = Aggregation.newAggregation(
                Aggregation.match(Criteria.where("userId").is(userId)
                        .and("created").gte(startDate).lte(endDate)),
                Aggregation.group("sourceLanguage").count().as("count"),
                Aggregation.sort(Sort.Direction.DESC, "count"),
                Aggregation.limit(1)
        );

        AggregationResults<Map> sourceResults = mongoTemplate.aggregate(
                languageAgg, "translation_history", Map.class
        );

        if (!sourceResults.getMappedResults().isEmpty()) {
            Map result = sourceResults.getMappedResults().get(0);
            stats.setMostUsedSourceLanguage((String) result.get("_id"));
        }

        // 가장 많이 번역된 타겟 언어
        Aggregation targetAgg = Aggregation.newAggregation(
                Aggregation.match(Criteria.where("userId").is(userId)
                        .and("created").gte(startDate).lte(endDate)),
                Aggregation.group("targetLanguage").count().as("count"),
                Aggregation.sort(Sort.Direction.DESC, "count"),
                Aggregation.limit(1)
        );

        AggregationResults<Map> targetResults = mongoTemplate.aggregate(
                targetAgg, "translation_history", Map.class
        );

        if (!targetResults.getMappedResults().isEmpty()) {
            Map result = targetResults.getMappedResults().get(0);
            stats.setMostUsedTargetLanguage((String) result.get("_id"));
        }

        return stats;
    }

    @Override
    public List<Map<String, Object>> getFrequentlyTranslatedTexts(String userId, int limit) {
        Aggregation aggregation = Aggregation.newAggregation(
                Aggregation.match(Criteria.where("userId").is(userId)),
                Aggregation.group("sourceText", "targetText", "sourceLanguage", "targetLanguage")
                        .count().as("frequency"),
                Aggregation.sort(Sort.Direction.DESC, "frequency"),
                Aggregation.limit(limit),
                Aggregation.project()
                        .and("_id.sourceText").as("sourceText")
                        .and("_id.targetText").as("targetText")
                        .and("_id.sourceLanguage").as("sourceLanguage")
                        .and("_id.targetLanguage").as("targetLanguage")
                        .and("frequency").as("frequency")
        );

        AggregationResults<Map> results = mongoTemplate.aggregate(
                aggregation, "translation_history", Map.class
        );

        return results.getMappedResults().stream()
                .map(result -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("sourceText", result.get("sourceText"));
                    map.put("targetText", result.get("targetText"));
                    map.put("sourceLanguage", result.get("sourceLanguage"));
                    map.put("targetLanguage", result.get("targetLanguage"));
                    map.put("frequency", result.get("frequency"));
                    return map;
                })
                .collect(Collectors.toList());
    }

    @Override
    public Map<LocalDateTime, Long> getDailyTranslationCount(String userId, LocalDateTime startDate, LocalDateTime endDate) {
        Map<LocalDateTime, Long> dailyCount = new LinkedHashMap<>();

        // 날짜별로 집계
        Aggregation aggregation = Aggregation.newAggregation(
                Aggregation.match(Criteria.where("userId").is(userId)
                        .and("created").gte(startDate).lte(endDate)),
                Aggregation.project()
                        .and("created").extractDayOfYear().as("dayOfYear")
                        .and("created").extractYear().as("year")
                        .and("created").extractMonth().as("month")
                        .and("created").extractDayOfMonth().as("day"),
                Aggregation.group("year", "month", "day")
                        .count().as("count"),
                Aggregation.sort(Sort.Direction.ASC, "_id.year", "_id.month", "_id.day")
        );

        AggregationResults<Map> results = mongoTemplate.aggregate(
                aggregation, "translation_history", Map.class
        );

        for (Map result : results.getMappedResults()) {
            Map<String, Integer> id = (Map<String, Integer>) result.get("_id");
            LocalDateTime date = LocalDateTime.of(
                    id.get("year"),
                    id.get("month"),
                    id.get("day"),
                    0, 0
            );
            Long count = ((Number) result.get("count")).longValue();
            dailyCount.put(date, count);
        }

        return dailyCount;
    }

    @Override
    public List<TranslationHistory> bulkSaveTranslations(List<TranslationHistory> translations) {
        if (translations == null || translations.isEmpty()) {
            return new ArrayList<>();
        }

        try {
            // MongoDB의 insertAll 사용하여 벌크 저장
            return new ArrayList<>(mongoTemplate.insertAll(translations));
        } catch (Exception e) {
            log.error("Error bulk saving translations: ", e);
            // 실패 시 개별 저장 시도
            List<TranslationHistory> savedTranslations = new ArrayList<>();
            for (TranslationHistory translation : translations) {
                try {
                    savedTranslations.add(mongoTemplate.save(translation));
                } catch (Exception ex) {
                    log.error("Failed to save individual translation: ", ex);
                }
            }
            return savedTranslations;
        }
    }

    @Override
    public List<TranslationHistory> exportLikedTranslations(String userId) {
        Query query = new Query(Criteria.where("userId").is(userId)
                .and("isLiked").is(true));
        query.with(Sort.by(Sort.Direction.DESC, "created"));

        return mongoTemplate.find(query, TranslationHistory.class);
    }

    @Override
    public List<TranslationHistory> findSimilarTranslations(String userId, String text, double minSimilarity) {
        // 간단한 유사도 검색 구현 (텍스트 포함 여부로 판단)
        // 실제로는 더 복잡한 텍스트 유사도 알고리즘 필요 (예: Levenshtein distance, Jaccard similarity)

        List<TranslationHistory> allTranslations = mongoTemplate.find(
                Query.query(Criteria.where("userId").is(userId)),
                TranslationHistory.class
        );

        List<TranslationHistory> similarTranslations = new ArrayList<>();

        for (TranslationHistory translation : allTranslations) {
            double similarity = calculateSimilarity(text, translation.getSourceText());
            if (similarity >= minSimilarity) {
                similarTranslations.add(translation);
            }
        }

        // 유사도 높은 순으로 정렬
        similarTranslations.sort((a, b) -> {
            double simA = calculateSimilarity(text, a.getSourceText());
            double simB = calculateSimilarity(text, b.getSourceText());
            return Double.compare(simB, simA);
        });

        return similarTranslations;
    }

    /**
     * 간단한 텍스트 유사도 계산 (Jaccard similarity)
     */
    private double calculateSimilarity(String text1, String text2) {
        if (text1 == null || text2 == null) {
            return 0.0;
        }

        String lower1 = text1.toLowerCase();
        String lower2 = text2.toLowerCase();

        // 완전 일치
        if (lower1.equals(lower2)) {
            return 1.0;
        }

        // 부분 포함
        if (lower1.contains(lower2) || lower2.contains(lower1)) {
            return 0.8;
        }

        // 단어 기반 유사도
        Set<String> words1 = new HashSet<>(Arrays.asList(lower1.split("\\s+")));
        Set<String> words2 = new HashSet<>(Arrays.asList(lower2.split("\\s+")));

        Set<String> intersection = new HashSet<>(words1);
        intersection.retainAll(words2);

        Set<String> union = new HashSet<>(words1);
        union.addAll(words2);

        if (union.isEmpty()) {
            return 0.0;
        }

        return (double) intersection.size() / union.size();
    }
}