package server.koraveler.blog.service.serviceImpl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import server.koraveler.blog.dto.ViewDTO;
import server.koraveler.blog.dto.ViewStatsDTO;
import server.koraveler.blog.service.ViewService;

@Service
@Slf4j
@RequiredArgsConstructor
public class ViewServiceImpl implements ViewService {
    @Override
    public boolean incrementView(ViewDTO viewDTO) {
        return false;
    }

    @Override
    public long getTotalViews(String documentId) {
        return 0;
    }

    @Override
    public long getUniqueViews(String documentId) {
        return 0;
    }

    @Override
    public long getTodayViews(String documentId) {
        return 0;
    }

    @Override
    public long getWeekViews(String documentId) {
        return 0;
    }

    @Override
    public ViewStatsDTO getViewStats(String documentId) {
        return null;
    }
}
