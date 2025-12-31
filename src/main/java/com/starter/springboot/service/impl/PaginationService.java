package com.starter.springboot.service.impl;

import com.starter.springboot.exception.InvalidSortFieldException;
import com.starter.springboot.service.IPaginationService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

@Service
public class PaginationService implements IPaginationService {

    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;

    @Override
    public Pageable createPageable(int page, int size) {
        validatePageParameters(page, size);
        return PageRequest.of(page, size);
    }

    @Override
    public Pageable createPageableWithSort(int page, int size, String sortBy, String sortDirection) {
        validatePageParameters(page, size);
        validateSortDirection(sortDirection);
        
        try {
            Sort.Direction direction = Sort.Direction.fromString(sortDirection.toUpperCase());
            Sort sort = Sort.by(direction, sortBy);
            return PageRequest.of(page, size, sort);
        } catch (IllegalArgumentException e) {
            throw new InvalidSortFieldException("Invalid sort direction: " + sortDirection, e);
        }
    }

    @Override
    public int getTotalPages(Page<?> page) {
        return page.getTotalPages();
    }

    @Override
    public int getCurrentPage(Page<?> page) {
        return page.getNumber();
    }

    @Override
    public int getPageSize(Page<?> page) {
        return page.getSize();
    }

    @Override
    public long getTotalElements(Page<?> page) {
        return page.getTotalElements();
    }

    @Override
    public boolean hasNext(Page<?> page) {
        return page.hasNext();
    }

    @Override
    public boolean hasPrevious(Page<?> page) {
        return page.hasPrevious();
    }

    @Override
    public boolean isFirst(Page<?> page) {
        return page.isFirst();
    }

    @Override
    public boolean isLast(Page<?> page) {
        return page.isLast();
    }

    private void validatePageParameters(int page, int size) {
        if (page < 0) {
            throw new IllegalArgumentException("Page number cannot be negative");
        }
        if (size <= 0) {
            throw new IllegalArgumentException("Page size must be greater than 0");
        }
        if (size > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException("Page size cannot exceed " + MAX_PAGE_SIZE);
        }
    }

    private void validateSortDirection(String sortDirection) {
        if (sortDirection == null || sortDirection.trim().isEmpty()) {
            throw new IllegalArgumentException("Sort direction cannot be null or empty");
        }
        
        String direction = sortDirection.toUpperCase().trim();
        if (!direction.equals("ASC") && !direction.equals("DESC")) {
            throw new IllegalArgumentException("Sort direction must be 'asc' or 'desc', got: " + sortDirection);
        }
    }
}
