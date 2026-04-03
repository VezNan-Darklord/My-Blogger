package csulzc.My_Personal_Blogger.api.dto.common;

import lombok.Data;
import lombok.experimental.SuperBuilder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.List;

@Data
@SuperBuilder
public class PageRequestDTO {
    private int page;

    private int size;

    private String sortBy;

    private String sortDirection;

    public Pageable toPageable() {
        Sort sort = Sort.by(Sort.Direction.fromString(sortDirection), sortBy);
        return PageRequest.of(page, size, sort);
    }
}