package com.nqt.football_management.util;

import com.nqt.football_management.constants.ErrorCode;
import com.nqt.football_management.constants.SortDirection;
import com.nqt.football_management.exception.GlobalException;
import com.nqt.football_management.model.request.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

public final class PageUtil {
    private PageUtil() {
    }

    public static Pageable createPageable(PageRequest request) {
        return createPageable(request, request == null ? null : request.getSortBy());
    }

    public static Pageable createPageable(PageRequest request, String sortBy) {
        if (request == null) {
            throw new GlobalException(ErrorCode.DATA_INVALID, "Page request is required");
        }
        if (request.getPage() < 0) {
            throw new GlobalException(ErrorCode.DATA_INVALID, "Page must be greater than or equal to 0");
        }
        if (request.getSize() <= 0) {
            throw new GlobalException(ErrorCode.DATA_INVALID, "Page size must be greater than 0");
        }
        if (sortBy == null || sortBy.isBlank()) {
            throw new GlobalException(ErrorCode.DATA_INVALID, "Sort field is required");
        }

        Sort sort = request.getDirection() == SortDirection.ASC
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();
        return org.springframework.data.domain.PageRequest.of(
                request.getPage(),
                request.getSize(),
                sort
        );
    }
}
