package com.starter.springboot.utils;

import com.starter.springboot.dto.PaginatedResponse;
import com.starter.springboot.service.IPaginationService;
import org.springframework.data.domain.Page;

public class PaginationConverter {

    private PaginationConverter() {
    }

    /**
     * Converts a Spring Data Page object to PaginatedResponse.
     *
     * @param page the Spring Data Page object
     * @param paginationService the pagination service for metadata extraction
     * @return PaginatedResponse with content and pagination metadata
     */
    public static <T> PaginatedResponse<T> toResponse(Page<T> page, IPaginationService paginationService) {
        return new PaginatedResponse<>(
            page.getContent(),
            paginationService.getCurrentPage(page),
            paginationService.getPageSize(page),
            paginationService.getTotalElements(page),
            paginationService.getTotalPages(page),
            paginationService.isFirst(page),
            paginationService.isLast(page),
            paginationService.hasNext(page),
            paginationService.hasPrevious(page)
        );
    }
}
