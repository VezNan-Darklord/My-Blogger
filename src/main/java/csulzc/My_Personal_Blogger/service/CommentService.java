package csulzc.My_Personal_Blogger.service;

import csulzc.My_Personal_Blogger.api.dto.comment.CommentCreateRequest;
import csulzc.My_Personal_Blogger.api.dto.comment.CommentDTO;
import csulzc.My_Personal_Blogger.api.dto.comment.CommentReplyDTO;
import csulzc.My_Personal_Blogger.api.dto.common.PageResponseDTO;
import csulzc.My_Personal_Blogger.api.dto.user.UserProfileDTO;
import csulzc.My_Personal_Blogger.domain.entity.Article;
import csulzc.My_Personal_Blogger.domain.entity.Comment;
import csulzc.My_Personal_Blogger.domain.entity.User;
import csulzc.My_Personal_Blogger.repository.ArticleRepository;
import csulzc.My_Personal_Blogger.repository.CommentRepository;
import csulzc.My_Personal_Blogger.repository.UserRepository;
import csulzc.My_Personal_Blogger.security.SecurityContextUtil;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommentService {

    private final CommentRepository commentRepository;
    private final ArticleRepository articleRepository;
    private final UserRepository userRepository;
    private final SecurityContextUtil securityContextUtil;

    // ==================== 评论创建与删除 ====================

    /**
     * 创建评论（包括回复）
     */
    @Transactional
    public CommentDTO createComment(Long articleId, CommentCreateRequest request) {
        Article article = articleRepository.findById(articleId)
                .orElseThrow(() -> new EntityNotFoundException("文章不存在"));

        User commenter = securityContextUtil.getCurrentUserAndValidateStatus();

        Comment.CommentBuilder commentBuilder = Comment.builder()
                .content(request.getContent())
                .article(article)
                .commenter(commenter);

        if (request.getParentCommentId() != null) {
            Comment parentComment = commentRepository.findById(request.getParentCommentId())
                    .orElseThrow(() -> new EntityNotFoundException("父评论不存在"));

            if (!parentComment.getArticle().getId().equals(articleId)) {
                throw new IllegalArgumentException("父评论不属于此文章");
            }

            commentBuilder.parentComment(parentComment);
        }

        Comment comment = commentBuilder.build();
        Comment savedComment = commentRepository.save(comment);

        return convertToDTO(savedComment);
    }

    /**
     * 删除评论
     */
    @Transactional
    public void deleteComment(Long commentId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new EntityNotFoundException("评论不存在"));

        securityContextUtil.validateOwnershipOrAdmin(comment.getCommenter().getId(), "评论");

        Comment parentComment = comment.getParentComment();
        if (parentComment != null) {
            parentComment.getReplies().remove(comment);
        }

        commentRepository.delete(comment);
    }

    /**
     * 批量删除文章的所有评论
     */
    @Transactional
    public int deleteCommentsByArticle(Long articleId) {
        Article article = articleRepository.findById(articleId)
                .orElseThrow(() -> new EntityNotFoundException("文章不存在"));

        // 先查询所有评论
        List<Comment> allComments = commentRepository.findByArticle(article);

        // 先删除所有回复评论（有父评论的）
        List<Comment> replyComments = allComments.stream()
                .filter(c -> c.getParentComment() != null)
                .collect(Collectors.toList());

        for (Comment reply : replyComments) {
            Comment parent = reply.getParentComment();
            if (parent != null) {
                parent.getReplies().remove(reply);
            }
        }

        commentRepository.deleteAll(replyComments);

        // 再删除顶级评论
        List<Comment> topLevelComments = allComments.stream()
                .filter(c -> c.getParentComment() == null)
                .collect(Collectors.toList());

        commentRepository.deleteAll(topLevelComments);

        return allComments.size();
    }


    // ==================== 评论查询 ====================

    /**
     * 根据 ID 获取评论详情
     */
    public CommentDTO getCommentById(Long commentId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new EntityNotFoundException("评论不存在"));
        return convertToDTO(comment);
    }

    /**
     * 获取文章的顶级评论列表（分页）
     */
    public PageResponseDTO<CommentDTO> getTopLevelComments(Long articleId, int page, int size, String sortBy) {
        Article article = articleRepository.findById(articleId)
                .orElseThrow(() -> new EntityNotFoundException("文章不存在"));

        Pageable pageable = PageRequest.of(page, size, Sort.by(sortBy).descending());
        Page<Comment> commentPage = commentRepository.findByArticleAndParentCommentIsNull(article, pageable);

        List<CommentDTO> content = commentPage.getContent().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());

        return PageResponseDTO.<CommentDTO>builder()
                .content(content)
                .page(commentPage.getNumber())
                .size(commentPage.getSize())
                .totalElements(commentPage.getTotalElements())
                .totalPages(commentPage.getTotalPages())
                .first(commentPage.isFirst())
                .last(commentPage.isLast())
                .build();
    }

    /**
     * 获取某个评论的所有回复（分页）
     */
    public PageResponseDTO<CommentReplyDTO> getCommentReplies(Long commentId, int page, int size, String sortBy) {
        Comment parentComment = commentRepository.findById(commentId)
                .orElseThrow(() -> new EntityNotFoundException("评论不存在"));

        Pageable pageable = PageRequest.of(page, size, Sort.by(sortBy).descending());
        Page<Comment> replyPage = commentRepository.findByParentComment(parentComment, pageable);

        List<CommentReplyDTO> content = replyPage.getContent().stream()
                .map(reply -> convertToReplyDTO(reply, parentComment))
                .collect(Collectors.toList());

        return PageResponseDTO.<CommentReplyDTO>builder()
                .content(content)
                .page(replyPage.getNumber())
                .size(replyPage.getSize())
                .totalElements(replyPage.getTotalElements())
                .totalPages(replyPage.getTotalPages())
                .first(replyPage.isFirst())
                .last(replyPage.isLast())
                .build();
    }

    /**
     * 获取用户的所有评论（分页）
     */
    public PageResponseDTO<CommentDTO> getUserComments(Long userId, int page, int size, String sortBy) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("用户不存在"));

        Pageable pageable = PageRequest.of(page, size, Sort.by(sortBy).descending());
        Page<Comment> commentPage = commentRepository.findByCommenter(user, pageable);

        List<CommentDTO> content = commentPage.getContent().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());

        return PageResponseDTO.<CommentDTO>builder()
                .content(content)
                .page(commentPage.getNumber())
                .size(commentPage.getSize())
                .totalElements(commentPage.getTotalElements())
                .totalPages(commentPage.getTotalPages())
                .first(commentPage.isFirst())
                .last(commentPage.isLast())
                .build();
    }

    // ==================== 评论统计 ====================

    /**
     * 统计文章的评论数
     */
    public long countCommentsByArticle(Long articleId) {
        Article article = articleRepository.findById(articleId)
                .orElseThrow(() -> new EntityNotFoundException("文章不存在"));
        return commentRepository.countByArticle(article);
    }

    /**
     * 统计用户的评论数
     */
    public long countCommentsByUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("用户不存在"));
        return commentRepository.countByCommenter(user);
    }

    // ==================== 辅助方法 ====================

    /**
     * 转换为 CommentDTO
     */
    private CommentDTO convertToDTO(Comment comment) {
        UserProfileDTO commenterDTO = UserProfileDTO.builder()
                .id(comment.getCommenter().getId())
                .username(comment.getCommenter().getUsername())
                .displayName(comment.getCommenter().getDisplayName())
                .avatar(comment.getCommenter().getAvatar())
                .bio(comment.getCommenter().getBio())
                .createdAt(comment.getCommenter().getCreatedAt())
                .build();

        CommentDTO dto = CommentDTO.builder()
                .id(comment.getId())
                .content(comment.getContent())
                .commenter(commenterDTO)
                .articleId(comment.getArticle().getId())
                .parentCommentId(comment.getParentComment() != null ? comment.getParentComment().getId() : null)
                .replyCount(comment.getReplies().size())
                .likeCount(comment.getLikeCount())
                .isLiked(false)  // TODO: 需要根据当前用户判断是否点赞
                .createdAt(comment.getCreatedAt())
                .build();

        // 设置回复列表（只转换第一层回复）
        if (!comment.getReplies().isEmpty()) {
            List<CommentReplyDTO> replyDTOs = comment.getReplies().stream()
                    .limit(5)  // 限制回复显示数量
                    .map(reply -> convertToReplyDTO(reply, comment))
                    .collect(Collectors.toList());
            dto.setReplies(convertToCommentDTOs(replyDTOs));
        }

        return dto;
    }

    /**
     * 转换为 CommentReplyDTO
     */
    private CommentReplyDTO convertToReplyDTO(Comment reply, Comment parentComment) {
        UserProfileDTO commenterDTO = UserProfileDTO.builder()
                .id(reply.getCommenter().getId())
                .username(reply.getCommenter().getUsername())
                .displayName(reply.getCommenter().getDisplayName())
                .avatar(reply.getCommenter().getAvatar())
                .bio(reply.getCommenter().getBio())
                .createdAt(reply.getCommenter().getCreatedAt())
                .build();

        UserProfileDTO replyToUserDTO = null;
        if (reply.getParentComment() != null && reply.getParentComment().getCommenter() != null) {
            replyToUserDTO = UserProfileDTO.builder()
                    .id(reply.getParentComment().getCommenter().getId())
                    .username(reply.getParentComment().getCommenter().getUsername())
                    .displayName(reply.getParentComment().getCommenter().getDisplayName())
                    .avatar(reply.getParentComment().getCommenter().getAvatar())
                    .bio(reply.getParentComment().getCommenter().getBio())
                    .build();
        }

        return CommentReplyDTO.builder()
                .id(reply.getId())
                .content(reply.getContent())
                .commenter(commenterDTO)
                .replyToUser(replyToUserDTO)
                .createdAt(reply.getCreatedAt())
                .build();
    }

    /**
     * 将 CommentReplyDTO 列表转换为 CommentDTO 列表（用于嵌套回复）
     */
    private List<CommentDTO> convertToCommentDTOs(List<CommentReplyDTO> replyDTOs) {
        // 这个方法可以根据需要进一步扩展，目前返回空列表
        // 因为 CommentReplyDTO 和 CommentDTO 结构不同，需要根据业务需求决定如何转换
        return new ArrayList<>();
    }
}
