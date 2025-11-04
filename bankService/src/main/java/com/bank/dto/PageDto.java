package com.bank.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record PageDto<T>(
        List<T> content,

        @JsonProperty("total_elements")
        long totalElements,

        @JsonProperty("page_number")
        int pageNumber,

        @JsonProperty("page_size")
        int pageSize,

        @JsonProperty("total_pages")
        int totalPages,

        @JsonProperty("has_next")
        boolean hasNext,

        @JsonProperty("has_previous")
        boolean hasPrevious
) {

}

