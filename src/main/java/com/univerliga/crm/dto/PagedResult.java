package com.univerliga.crm.dto;

import java.util.List;

public record PagedResult<T>(
        List<T> items,
        PageMeta page
) {
}
