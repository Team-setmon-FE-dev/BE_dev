package com.example.setmon.vlog.worker;

import java.time.LocalDate;

public record VlogJob(Long vlogId, Long userId, LocalDate date) {
}
