package csulzc.My_Personal_Blogger.controller;

import csulzc.My_Personal_Blogger.api.dto.article.*;
import csulzc.My_Personal_Blogger.api.dto.common.PageRequestDTO;
import csulzc.My_Personal_Blogger.api.dto.common.PageResponseDTO;
import csulzc.My_Personal_Blogger.api.response.Result;
import csulzc.My_Personal_Blogger.service.ArticleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/articles")
@RequiredArgsConstructor
public class ArticleController {

    private final ArticleService articleService;

    /**
     * 创建文章
     */
    @PostMapping
    public ResponseEntity<Result<ArticleDetailDTO>> createArticle(
            @Valid @RequestBody ArticleCreateRequest request,
            @RequestParam Long authorId) {
        if (authorId == null || authorId <= 0) {
            throw new IllegalArgumentException("作者ID无效");
        }
        ArticleDetailDTO article = articleService.createArticle(request, authorId);
        return ResponseEntity.ok(Result.success(article, "文章创建成功"));
    }

    /**
     * 更新文章
     */
    @PutMapping("/{articleId}")
    public ResponseEntity<Result<ArticleDetailDTO>> updateArticle(
            @PathVariable Long articleId,
            @Valid @RequestBody ArticleUpdateRequest request,
            @RequestParam Long userId) {
        if (articleId == null || articleId <= 0) {
            throw new IllegalArgumentException("文章ID无效");
        }
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("用户ID无效");
        }
        ArticleDetailDTO article = articleService.updateArticle(articleId, request, userId);
        return ResponseEntity.ok(Result.success(article, "文章更新成功"));
    }

    /**
     * 发布文章
     */
    @PostMapping("/{articleId}/publish")
    public ResponseEntity<Result<ArticleDetailDTO>> publishArticle(
            @PathVariable Long articleId,
            @RequestParam Long userId) {
        if (articleId == null || articleId <= 0) {
            throw new IllegalArgumentException("文章ID无效");
        }
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("用户ID无效");
        }
        ArticleDetailDTO article = articleService.publishArticle(articleId, userId);
        return ResponseEntity.ok(Result.success(article, "文章发布成功"));
    }

    /**
     * 归档文章
     */
    @PostMapping("/{articleId}/archive")
    public ResponseEntity<Result<ArticleDetailDTO>> archiveArticle(
            @PathVariable Long articleId,
            @RequestParam Long userId) {
        if (articleId == null || articleId <= 0) {
            throw new IllegalArgumentException("文章ID无效");
        }
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("用户ID无效");
        }
        ArticleDetailDTO article = articleService.archiveArticle(articleId, userId);
        return ResponseEntity.ok(Result.success(article, "文章归档成功"));
    }

    /**
     * 获取文章详情
     */
    @GetMapping("/{articleId}")
    public ResponseEntity<Result<ArticleDetailDTO>> getArticleById(@PathVariable Long articleId) {
        if (articleId == null || articleId <= 0) {
            throw new IllegalArgumentException("文章ID无效");
        }
        ArticleDetailDTO article = articleService.getArticleById(articleId);
        return ResponseEntity.ok(Result.success(article));
    }

    /**
     * 获取文章列表（分页）
     */
    @GetMapping
    public ResponseEntity<Result<PageResponseDTO<ArticleListItemDTO>>> getArticleList(
            @ModelAttribute PageRequestDTO pageRequest) {
        if (pageRequest == null) {
            throw new IllegalArgumentException("分页参数不能为空");
        }
        if (pageRequest.getPage() < 0) {
            throw new IllegalArgumentException("页码不能为负数");
        }
        if (pageRequest.getSize() <= 0 || pageRequest.getSize() > 100) {
            throw new IllegalArgumentException("每页大小必须在1-100之间");
        }
        Pageable pageable = pageRequest.toPageable();
        PageResponseDTO<ArticleListItemDTO> articles = articleService.getArticleList(pageable);
        return ResponseEntity.ok(Result.success(articles));
    }

    /**
     * 根据作者获取文章列表
     */
    @GetMapping("/author/{authorId}")
    public ResponseEntity<Result<PageResponseDTO<ArticleListItemDTO>>> getArticlesByAuthor(
            @PathVariable Long authorId,
            @ModelAttribute PageRequestDTO pageRequest) {
        if (authorId == null || authorId <= 0) {
            throw new IllegalArgumentException("作者ID无效");
        }
        if (pageRequest == null) {
            throw new IllegalArgumentException("分页参数不能为空");
        }
        if (pageRequest.getPage() < 0) {
            throw new IllegalArgumentException("页码不能为负数");
        }
        if (pageRequest.getSize() <= 0 || pageRequest.getSize() > 100) {
            throw new IllegalArgumentException("每页大小必须在1-100之间");
        }
        // TODO: 需要从 UserService 获取 User 对象
        // User author = userService.getUserById(authorId);
        Pageable pageable = pageRequest.toPageable();
        // PageResponseDTO<ArticleListItemDTO> articles = articleService.getArticlesByAuthor(author, pageable);
        // return ResponseEntity.ok(Result.success(articles));
        return ResponseEntity.ok(Result.success(null, "待实现：需要先注入 UserService"));
    }

    /**
     * 搜索文章
     */
    @GetMapping("/search")
    public ResponseEntity<Result<PageResponseDTO<ArticleListItemDTO>>> searchArticles(
            @RequestParam String keyword,
            @ModelAttribute PageRequestDTO pageRequest) {
        if (keyword == null || keyword.trim().isEmpty()) {
            throw new IllegalArgumentException("搜索关键词不能为空");
        }
        if (pageRequest == null) {
            throw new IllegalArgumentException("分页参数不能为空");
        }
        if (pageRequest.getPage() < 0) {
            throw new IllegalArgumentException("页码不能为负数");
        }
        if (pageRequest.getSize() <= 0 || pageRequest.getSize() > 100) {
            throw new IllegalArgumentException("每页大小必须在1-100之间");
        }
        Pageable pageable = pageRequest.toPageable();
        PageResponseDTO<ArticleListItemDTO> articles = articleService.searchArticles(keyword, pageable);
        return ResponseEntity.ok(Result.success(articles));
    }

    /**
     * 删除文章
     */
    @DeleteMapping("/{articleId}")
    public ResponseEntity<Result<Void>> deleteArticle(
            @PathVariable Long articleId,
            @RequestParam Long userId) {
        if (articleId == null || articleId <= 0) {
            throw new IllegalArgumentException("文章ID无效");
        }
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("用户ID无效");
        }
        articleService.deleteArticle(articleId, userId);
        return ResponseEntity.ok(Result.success(null, "文章删除成功"));
    }
}
