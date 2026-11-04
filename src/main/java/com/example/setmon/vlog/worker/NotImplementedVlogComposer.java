package com.example.setmon.vlog.worker;

import java.util.List;
import org.springframework.stereotype.Component;

/**
 * 임시 구현. B의 FFmpeg 구현이 들어오면 이 클래스를 지울 것(빈이 2개면 기동 실패).
 * 그 전까지 "지금 만들기"를 누르면 FAILED("VlogComposer 미구현")로 끝난다.
 */
@Component
public class NotImplementedVlogComposer implements VlogComposer {

    @Override
    public VlogResult compose(List<String> clipPaths, List<String> timeLabels) {
        throw new UnsupportedOperationException("VlogComposer 미구현 (B 담당)");
    }
}
