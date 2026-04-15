package csulzc.My_Personal_Blogger.controller;

import csulzc.My_Personal_Blogger.api.dto.comment.CommentCreateRequest;
import csulzc.My_Personal_Blogger.api.dto.comment.CommentDTO;
import csulzc.My_Personal_Blogger.api.dto.comment.CommentReplyDTO;
import csulzc.My_Personal_Blogger.api.dto.common.PageResponseDTO;
import csulzc.My_Personal_Blogger.api.response.Result;
import csulzc.My_Personal_Blogger.service.CommentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/comments")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    /**
     * 创建评论（包括回复）
     */
    @PostMapping("/article/{articleId}")
    public ResponseEntity<Result<CommentDTO>> createComment(
            @PathVariable Long articleId,
            @Valid @RequestBody CommentCreateRequest request,
            @RequestParam Long commenterId) {
        if (articleId == null || articleId <= 0) {
            throw new IllegalArgumentException("文章ID无效");
        }
        if (commenterId == null || commenterId <= 0) {
            throw new IllegalArgumentException("评论者ID无效");
        }
        CommentDTO comment = commentService.createComment(articleId, commenterId, request);
        return ResponseEntity.ok(Result.success(comment, "评论发表成功"));
    }

    /**
     * 获取评论详情
     */
    @GetMapping("/{commentId}")
    public ResponseEntity<Result<CommentDTO>> getCommentById(@PathVariable Long commentId) {
        if (commentId == null || commentId <= 0) {
            throw new IllegalArgumentException("评论ID无效");
        }
        CommentDTO comment = commentService.getCommentById(commentId);
        return ResponseEntity.ok(Result.success(comment));
    }

    /**
     * 获取文章的顶级评论列表（分页）
     */
    @GetMapping("/article/{articleId}")
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

    /**
     * 获取某个评论的所有回复（分页）
     */
    @GetMapping("/{commentId}/replies")
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

    /**
     * 获取用户的所有评论（分页）
     */
    @GetMapping("/user/{userId}")
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

    /**
     * 删除评论
     */
    @DeleteMapping("/{commentId}")
    public ResponseEntity<Result<Void>> deleteComment(
            @PathVariable Long commentId,
            @RequestParam Long userId) {
        if (commentId == null || commentId <= 0) {
            throw new IllegalArgumentException("评论ID无效");
        }
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("用户ID无效");
        }
        commentService.deleteComment(commentId, userId);
        return ResponseEntity.ok(Result.success(null, "评论删除成功"));
    }

    /**
     * 统计文章的评论数
     */
    @GetMapping("/article/{articleId}/count")
    public ResponseEntity<Result<Long>> countCommentsByArticle(@PathVariable Long articleId) {
        if (articleId == null || articleId <= 0) {
            throw new IllegalArgumentException("文章ID无效");
        }
        long count = commentService.countCommentsByArticle(articleId);
        return ResponseEntity.ok(Result.success(count));
    }

    /**
     * 统计用户的评论数
     */
    @GetMapping("/user/{userId}/count")
    public ResponseEntity<Result<Long>> countCommentsByUser(@PathVariable Long userId) {
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("用户ID无效");
        }
        long count = commentService.countCommentsByUser(userId);
        return ResponseEntity.ok(Result.success(count));
    }
}
