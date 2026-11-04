package com.example.setmon.vlog.dto;

import com.example.setmon.vlog.entity.VlogStatus;
import java.time.LocalDate;

public record VlogStatusResponse(LocalDate date, VlogStatus status) {
}
