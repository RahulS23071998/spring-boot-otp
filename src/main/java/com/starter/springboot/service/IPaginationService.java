package com.starter.springboot.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface IPaginationService {

    /**
     * Creates a Pageable object from page and size parameters.
     *
     * @param page the page number (zero-indexed)
     * @param size the page size
     * @return Pageable object
     */
    Pageable createPageable(int page, int size);

    /**
     * Creates a Pageable object with sorting.
     *
     * @param page the page number (zero-indexed)
     * @param size the page size
     * @param sortBy the field to sort by
     * @param sortDirection "asc" or "desc"
     * @return Pageable object
     */
    Pageable createPageableWithSort(int page, int size, String sortBy, String sortDirection);

    /**
     * Gets the total number of pages.
     *
     * @param page the Spring Data Page object
     * @return total number of pages
     */
    int getTotalPages(Page<?> page);

    /**
     * Gets the current page number.
     *
     * @param page the Spring Data Page object
     * @return current page number (zero-indexed)
     */
    int getCurrentPage(Page<?> page);

    /**
     * Gets the page size.
     *
     * @param page the Spring Data Page object
     * @return page size
     */
    int getPageSize(Page<?> page);

    /**
     * Gets the total number of elements.
     *
     * @param page the Spring Data Page object
     * @return total number of elements
     */
    long getTotalElements(Page<?> page);

    /**
     * Checks if there is a next page.
     *
     * @param page the Spring Data Page object
     * @return true if there is a next page
     */
    boolean hasNext(Page<?> page);

    /**
     * Checks if there is a previous page.
     *
     * @param page the Spring Data Page object
     * @return true if there is a previous page
     */
    boolean hasPrevious(Page<?> page);

    /**
     * Checks if this is the first page.
     *
     * @param page the Spring Data Page object
     * @return true if this is the first page
     */
    boolean isFirst(Page<?> page);

    /**
     * Checks if this is the last page.
     *
     * @param page the Spring Data Page object
     * @return true if this is the last page
     */
    boolean isLast(Page<?> page);
}
