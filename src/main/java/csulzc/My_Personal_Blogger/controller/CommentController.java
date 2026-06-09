package csulzc.My_Personal_Blogger.controller;

import csulzc.My_Personal_Blogger.api.dto.comment.CommentCreateRequest;
import csulzc.My_Personal_Blogger.api.dto.comment.CommentDTO;
import csulzc.My_Personal_Blogger.api.dto.comment.CommentReplyDTO;
import csulzc.My_Personal_Blogger.api.dto.common.PageResponseDTO;
import csulzc.My_Personal_Blogger.api.response.Result;
import csulzc.My_Personal_Blogger.service.CommentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/comments")
@RequiredArgsConstructor
@Tag(name = "评论管理", description = "评论CRUD及回复操作")
public class CommentController {

    private final CommentService commentService;

    @PostMapping("/article/{articleId}")
    @Operation(summary = "创建评论", description = "需要登录，自动使用当前用户作为评论者")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Result<CommentDTO>> createComment(
            @PathVariable Long articleId,
            @Valid @RequestBody CommentCreateRequest request) {
        if (articleId == null || articleId <= 0) {
            throw new IllegalArgumentException("文章ID无效");
        }
        CommentDTO comment = commentService.createComment(articleId, request);
        return ResponseEntity.ok(Result.success(comment, "评论发表成功"));
    }

    @GetMapping("/{commentId}")
    @Operation(summary = "获取评论详情", description = "公开访问")
    public ResponseEntity<Result<CommentDTO>> getCommentById(@PathVariable Long commentId) {
        if (commentId == null || commentId <= 0) {
            throw new IllegalArgumentException("评论ID无效");
        }
        CommentDTO comment = commentService.getCommentById(commentId);
        return ResponseEntity.ok(Result.success(comment));
    }

    @GetMapping("/article/{articleId}")
    @Operation(summary = "获取文章评论列表", description = "公开访问")
    public ResponseEntity<Result<PageResponseDTO<CommentDTO>>> getTopLevelComments(
            @PathVariable Long articleId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy) {
        if (articleId == null || articleId <= 0) {
            throw new IllegalArgumentException("文章ID无效");
        }
        if (page < 0) {
            throw new IllegalArgumentException("页码不能为负数");
        }
        if (size <= 0 || size > 100) {
            throw new IllegalArgumentException("每页大小必须在1-100之间");
        }
        PageResponseDTO<CommentDTO> comments = commentService.getTopLevelComments(articleId, page, size, sortBy);
        return ResponseEntity.ok(Result.success(comments));
    }

    @GetMapping("/{commentId}/replies")
    @Operation(summary = "获取评论回复列表", description = "公开访问")
    public ResponseEntity<Result<PageResponseDTO<CommentReplyDTO>>> getCommentReplies(
            @PathVariable Long commentId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy) {
        if (commentId == null || commentId <= 0) {
            throw new IllegalArgumentException("评论ID无效");
        }
        if (page < 0) {
            throw new IllegalArgumentException("页码不能为负数");
        }
        if (size <= 0 || size > 100) {
            throw new IllegalArgumentException("每页大小必须在1-100之间");
        }
        PageResponseDTO<CommentReplyDTO> replies = commentService.getCommentReplies(commentId, page, size, sortBy);
        return ResponseEntity.ok(Result.success(replies));
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "获取用户评论列表", description = "公开访问")
    public ResponseEntity<Result<PageResponseDTO<CommentDTO>>> getUserComments(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy) {
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("用户ID无效");
        }
        if (page < 0) {
            throw new IllegalArgumentException("页码不能为负数");
        }
        if (size <= 0 || size > 100) {
            throw new IllegalArgumentException("每页大小必须在1-100之间");
        }
        PageResponseDTO<CommentDTO> comments = commentService.getUserComments(userId, page, size, sortBy);
        return ResponseEntity.ok(Result.success(comments));
    }

    @DeleteMapping("/{commentId}")
    @Operation(summary = "删除评论", description = "需要登录且为评论作者或管理员")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Result<Void>> deleteComment(
            @PathVariable Long commentId) {
        if (commentId == null || commentId <= 0) {
            throw new IllegalArgumentException("评论ID无效");
        }
        commentService.deleteComment(commentId);
        return ResponseEntity.ok(Result.success(null, "评论删除成功"));
    }

    @GetMapping("/article/{articleId}/count")
    @Operation(summary = "统计文章评论数", description = "公开访问")
    public ResponseEntity<Result<Long>> countCommentsByArticle(@PathVariable Long articleId) {
        if (articleId == null || articleId <= 0) {
            throw new IllegalArgumentException("文章ID无效");
        }
        long count = commentService.countCommentsByArticle(articleId);
        return ResponseEntity.ok(Result.success(count));
    }

    @GetMapping("/user/{userId}/count")
    @Operation(summary = "统计用户评论数", description = "公开访问")
    public ResponseEntity<Result<Long>> countCommentsByUser(@PathVariable Long userId) {
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("用户ID无效");
        }
        long count = commentService.countCommentsByUser(userId);
        return ResponseEntity.ok(Result.success(count));
    }
}
