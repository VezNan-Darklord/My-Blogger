package csulzc.My_Personal_Blogger.api.dto.common;

import lombok.Data;
import lombok.Builder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.List;

@Data
@Builder
public class PageRequestDTO {
    @Builder.Default
    private int page = 0;

    @Builder.Default
    private int size = 10;

    private String sortBy;

    @Builder.Default
    private String sortDirection = "DESC";

    public Pageable toPageable() {
        Sort sort = Sort.by(Sort.Direction.fromString(sortDirection), sortBy);
        return PageRequest.of(page, size, sort);
    }
}