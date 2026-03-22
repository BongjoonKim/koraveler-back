package server.nadeliv.blog.service;

import server.nadeliv.blog.dto.ViewDTO;
import server.nadeliv.blog.dto.ViewStatsDTO;

public interface ViewService {

    /**
     * 조회수 증가 (24시간 내 동일 IP 중복 방지)
     * @param viewDTO 조회 정보 (documentId, ipAddress, userAgent, referer)
     * @return 조회수가 증가되었으면 true, 중복이면 false
     */
    boolean incrementView(ViewDTO viewDTO);

    /**
     * 특정 문서의 전체 조회수 조회
     * @param documentId 문서 ID
     * @return 전체 조회수
     */
    long getTotalViews(String documentId);

    /**
     * 특정 문서의 유니크 조회수 조회 (IP 기준)
     * @param documentId 문서 ID
     * @return 유니크 조회수
     */
    long getUniqueViews(String documentId);

    /**
     * 특정 문서의 오늘 조회수 조회
     * @param documentId 문서 ID
     * @return 오늘 조회수
     */
    long getTodayViews(String documentId);

    /**
     * 특정 문서의 이번 주 조회수 조회
     * @param documentId 문서 ID
     * @return 이번 주 조회수
     */
    long getWeekViews(String documentId);

    /**
     * 특정 문서의 조회 통계 조회
     * @param documentId 문서 ID
     * @return 조회 통계 정보
     */
    ViewStatsDTO getViewStats(String documentId);
}
