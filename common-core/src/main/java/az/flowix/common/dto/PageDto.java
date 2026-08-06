package az.flowix.common.dto;

import java.util.List;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PageDto<T> {

    List<T> content;
    int page;
    int size;
    long totalElements;
    int totalPages;
    boolean first;
    boolean last;
    boolean empty;

    public static <T> PageDto<T> of(List<T> content, long totalElements, int page, int size) {
        List<T> items = content != null ? content : List.of();
        int totalPages = size == 0 ? 0 : (int) Math.ceil((double) totalElements / size);
        boolean empty = items.isEmpty();
        return PageDto.<T>builder()
                .content(items)
                .page(page)
                .size(size)
                .totalElements(totalElements)
                .totalPages(totalPages)
                .first(empty || page == 0)
                .last(empty || page >= totalPages - 1)
                .empty(empty)
                .build();
    }

}
