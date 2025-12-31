package com.starter.springboot.service.impl;

import com.starter.springboot.exception.InvalidSortFieldException;
import com.starter.springboot.service.IPaginationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PaginationServiceTest {

    private IPaginationService paginationService;

    @BeforeEach
    void setUp() {
        paginationService = new PaginationService();
    }

    @Test
    void createPageable_ShouldReturnCorrectPageable() {
        Pageable pageable = paginationService.createPageable(0, 10);
        
        assertThat(pageable.getPageNumber()).isZero();
        assertThat(pageable.getPageSize()).isEqualTo(10);
        assertThat(pageable.getSort().isSorted()).isFalse();
    }

    @Test
    void createPageable_ShouldThrowException_WhenPageIsNegative() {
        assertThatThrownBy(() -> paginationService.createPageable(-1, 10))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Page number cannot be negative");
    }

    @Test
    void createPageable_ShouldThrowException_WhenSizeIsZeroOrNegative() {
        assertThatThrownBy(() -> paginationService.createPageable(0, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Page size must be greater than 0");
        
        assertThatThrownBy(() -> paginationService.createPageable(0, -1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Page size must be greater than 0");
    }

    @Test
    void createPageable_ShouldThrowException_WhenSizeExceedsMax() {
        assertThatThrownBy(() -> paginationService.createPageable(0, 101))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Page size cannot exceed 100");
    }

    @Test
    void createPageableWithSort_ShouldReturnCorrectPageable() {
        Pageable pageable = paginationService.createPageableWithSort(0, 10, "id", "asc");
        
        assertThat(pageable.getPageNumber()).isZero();
        assertThat(pageable.getPageSize()).isEqualTo(10);
        assertThat(pageable.getSort().isSorted()).isTrue();
        assertThat(pageable.getSort().getOrderFor("id").getDirection()).isEqualTo(Sort.Direction.ASC);
    }

    @Test
    void createPageableWithSort_ShouldHandleDescSort() {
        Pageable pageable = paginationService.createPageableWithSort(0, 10, "name", "desc");
        
        assertThat(pageable.getSort().getOrderFor("name").getDirection()).isEqualTo(Sort.Direction.DESC);
    }

    @Test
    void createPageableWithSort_ShouldThrowException_WhenSortDirectionIsInvalid() {
        assertThatThrownBy(() -> paginationService.createPageableWithSort(0, 10, "id", "invalid"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Sort direction must be 'asc' or 'desc'");
    }

    @Test
    void paginationMetadataMethods_ShouldReturnCorrectValues() {
        List<String> content = List.of("item1", "item2");
        Page<String> page = new PageImpl<>(content, PageRequest.of(0, 10), 20);

        assertThat(paginationService.getCurrentPage(page)).isZero();
        assertThat(paginationService.getPageSize(page)).isEqualTo(10);
        assertThat(paginationService.getTotalElements(page)).isEqualTo(20);
        assertThat(paginationService.getTotalPages(page)).isEqualTo(2);
        assertThat(paginationService.isFirst(page)).isTrue();
        assertThat(paginationService.isLast(page)).isFalse();
        assertThat(paginationService.hasNext(page)).isTrue();
        assertThat(paginationService.hasPrevious(page)).isFalse();
    }
}
