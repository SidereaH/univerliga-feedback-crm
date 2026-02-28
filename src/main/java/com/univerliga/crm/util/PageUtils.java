package com.univerliga.crm.util;

import com.univerliga.crm.dto.PageMeta;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

public final class PageUtils {

    private PageUtils() {
    }

    public static Pageable pageable(int page, int size) {
        int validatedPage = Math.max(page, 1);
        int validatedSize = Math.max(size, 1);
        return PageRequest.of(validatedPage - 1, validatedSize);
    }

    public static PageMeta pageMeta(Page<?> page) {
        return new PageMeta(page.getNumber() + 1, page.getSize(), page.getTotalElements(), page.getTotalPages());
    }
}
