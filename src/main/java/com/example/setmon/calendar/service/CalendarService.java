package com.example.setmon.calendar.service;

import com.example.setmon.calendar.dto.DayDetailResponse;
import com.example.setmon.calendar.dto.DayDetailResponse.ClipItem;
import com.example.setmon.calendar.dto.DayDetailResponse.VlogInfo;
import com.example.setmon.calendar.dto.MonthResponse;
import com.example.setmon.calendar.dto.MonthResponse.DaySummary;
import com.example.setmon.clip.entity.Clip;
import com.example.setmon.clip.service.ClipService;
import com.example.setmon.common.security.AccessGuard;
import com.example.setmon.storage.StorageService;
import com.example.setmon.user.entity.User;
import com.example.setmon.user.service.UserService;
import com.example.setmon.vlog.entity.DailyVlog;
import com.example.setmon.vlog.entity.VlogStatus;
import com.example.setmon.vlog.service.VlogService;
import java.time.Duration;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CalendarService {

    private static final Duration VIEW_URL_TTL = Duration.ofHours(1);

    private final AccessGuard accessGuard;
    private final UserService userService;
    private final ClipService clipService;
    private final VlogService vlogService;
    private final StorageService storageService;

    public MonthResponse getMonth(Long viewerId, Long ownerId, YearMonth month) {
        accessGuard.checkCanView(viewerId, ownerId);
        userService.getUser(ownerId);
        LocalDate from = month.atDay(1);
        LocalDate to = month.atEndOfMonth();

        Map<LocalDate, List<Clip>> clipsByDate = clipService.getClipsBetween(ownerId, from, to).stream()
                .collect(Collectors.groupingBy(Clip::getClipDate, TreeMap::new, Collectors.toList()));
        Map<LocalDate, DailyVlog> vlogByDate = vlogService.findBetween(ownerId, from, to).stream()
                .collect(Collectors.toMap(DailyVlog::getVlogDate, Function.identity()));

        TreeSet<LocalDate> dates = new TreeSet<>(clipsByDate.keySet());
        dates.addAll(vlogByDate.keySet());

        List<DaySummary> days = new ArrayList<>();
        for (LocalDate date : dates) {
            List<Clip> clips = clipsByDate.getOrDefault(date, List.of());
            DailyVlog vlog = vlogByDate.get(date);
            VlogStatus status = vlog == null ? VlogStatus.NONE : vlog.getStatus();
            if (clips.isEmpty() && status == VlogStatus.NONE) {
                continue;
            }
            days.add(new DaySummary(date, url(thumbnailKey(vlog, clips)), clips.size(), status));
        }
        return new MonthResponse(ownerId, month.toString(), days);
    }

    public DayDetailResponse getDay(Long viewerId, Long ownerId, LocalDate date) {
        accessGuard.checkCanView(viewerId, ownerId);
        User owner = userService.getUser(ownerId);

        List<ClipItem> clips = clipService.getClips(ownerId, date).stream()
                .map(c -> new ClipItem(c.getId(), c.getSlotHour(), c.getRecordedAt(), c.getDurationMs(),
                        url(c.getVideoKey()), url(c.getThumbKey())))
                .toList();

        VlogInfo vlogInfo = vlogService.find(ownerId, date)
                .map(v -> new VlogInfo(v.getStatus(), v.isStale(),
                        v.getStatus() == VlogStatus.DONE ? url(v.getVideoKey()) : null,
                        v.getStatus() == VlogStatus.DONE ? url(v.getThumbKey()) : null,
                        v.getStatus() == VlogStatus.FAILED ? v.getFailReason() : null))
                .orElse(new VlogInfo(VlogStatus.NONE, false, null, null, null));

        return new DayDetailResponse(ownerId, date, owner.getNotifyStartHour(), owner.getNotifyEndHour(), vlogInfo, clips);
    }

    /** 브이로그가 완성됐으면 브이로그 썸네일, 아니면 첫 클립 썸네일 */
    private String thumbnailKey(DailyVlog vlog, List<Clip> clips) {
        if (vlog != null && vlog.getStatus() == VlogStatus.DONE && vlog.getThumbKey() != null) {
            return vlog.getThumbKey();
        }
        return clips.isEmpty() ? null : clips.get(0).getThumbKey();
    }

    private String url(String key) {
        return key == null ? null : storageService.presignGet(key, VIEW_URL_TTL);
    }
}
