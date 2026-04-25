package csulzc.My_Personal_Blogger;

import csulzc.My_Personal_Blogger.api.dto.comment.CommentCreateRequest;
import csulzc.My_Personal_Blogger.api.dto.comment.CommentDTO;
import csulzc.My_Personal_Blogger.api.dto.comment.CommentReplyDTO;
import csulzc.My_Personal_Blogger.api.dto.common.PageResponseDTO;
import csulzc.My_Personal_Blogger.domain.entity.Article;
import csulzc.My_Personal_Blogger.domain.entity.Category;
import csulzc.My_Personal_Blogger.domain.entity.Comment;
import csulzc.My_Personal_Blogger.domain.entity.User;
import csulzc.My_Personal_Blogger.repository.ArticleRepository;
import csulzc.My_Personal_Blogger.repository.CommentRepository;
import csulzc.My_Personal_Blogger.repository.UserRepository;
import csulzc.My_Personal_Blogger.service.CommentService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
@Import({CommentService.class})
@Transactional
@DisplayName("Comment 集成测试 - 层间协作")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class CommentIntegrationTest {

    @Autowired
    private CommentService commentService;

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private ArticleRepository articleRepository;

    @Autowired
    private UserRepository userRepository;

    private User commenter1;
    private User commenter2;
    private Article testArticle;
    private Comment topLevelComment1;
    private Comment topLevelComment2;

    @BeforeEach
    void setUp() {
        // 创建测试用户
        commenter1 = User.builder()
                .username("commenter1")
                .email("commenter1@example.com")
                .passwordHash("password123")
                .displayName("评论者1")
                .status(User.UserStatus.ACTIVE)
                .build();
        entityManager.persist(commenter1);

        commenter2 = User.builder()
                .username("commenter2")
                .email("commenter2@example.com")
                .passwordHash("password456")
                .displayName("评论者2")
                .status(User.UserStatus.ACTIVE)
                .build();
        entityManager.persist(commenter2);

        // 创建测试分类
        Category category = Category.builder()
                .name("技术")
                .description("技术文章")
                .build();
        entityManager.persist(category);

        // 创建测试文章
        testArticle = Article.builder()
                .title("测试文章")
                .content("这是测试文章的内容，长度足够生成摘要")
                .author(commenter1)
                .status(Article.ArticleStatus.RELEASE)
                .build();
        testArticle.addCategory(category);
        entityManager.persist(testArticle);

        // 创建顶级评论
        topLevelComment1 = Comment.builder()
                .content("这是一条顶级评论")
                .article(testArticle)
                .commenter(commenter1)
                .likeCount(5)
                .build();
        entityManager.persist(topLevelComment1);

        topLevelComment2 = Comment.builder()
                .content("这是另一条顶级评论")
                .article(testArticle)
                .commenter(commenter2)
                .likeCount(3)
                .build();
        entityManager.persist(topLevelComment2);

        entityManager.flush();
    }

    @AfterEach
    void tearDown() {
        entityManager.clear();
    }

    @Test
    @Order(1)
    @DisplayName("测试创建评论 - 验证实体关系持久化")
    void testCreateComment_WithEntityRelationships() {
        // Given - 准备创建评论请求
        CommentCreateRequest request = CommentCreateRequest.builder()
                .content("这是一条新评论")
                .build();

        // When - 通过服务层创建评论
        CommentDTO result = commentService.createComment(
                testArticle.getId(), commenter2.getId(), request);

        // Then - 验证返回结果
        assertThat(result).isNotNull();
        assertThat(result.getContent()).isEqualTo("这是一条新评论");
        assertThat(result.getCommenter().getId()).isEqualTo(commenter2.getId());

        // 验证数据库中的实体关系
        Optional<Comment> savedCommentOpt = commentRepository.findById(result.getId());
        assertThat(savedCommentOpt).isPresent();
        Comment savedComment = savedCommentOpt.get();

        assertThat(savedComment.getArticle().getId()).isEqualTo(testArticle.getId());
        assertThat(savedComment.getCommenter().getId()).isEqualTo(commenter2.getId());
        assertThat(savedComment.getParentComment()).isNull();

        // 验证文章包含该评论（通过反向查询验证）
        var commentsInArticle = commentRepository.findByArticle(testArticle);
        assertThat(commentsInArticle).anyMatch(c -> c.getId().equals(savedComment.getId()));
    }

    @Test
    @Order(2)
    @DisplayName("测试创建回复 - 验证父子关系")
    void testCreateReply_WithParentChildRelationship() {
        // Given - 准备回复请求
        CommentCreateRequest replyRequest = CommentCreateRequest.builder()
                .content("这是对顶级评论的回复")
                .parentCommentId(topLevelComment1.getId())
                .build();

        // When - 创建回复
        CommentDTO result = commentService.createComment(
                testArticle.getId(), commenter2.getId(), replyRequest);

        // Then - 验证回复创建成功
        assertThat(result).isNotNull();
        assertThat(result.getContent()).isEqualTo("这是对顶级评论的回复");
        assertThat(result.getParentCommentId()).isEqualTo(topLevelComment1.getId());

        // 验证数据库中的父子关系
        entityManager.flush();
        entityManager.clear();
        Comment savedReply = entityManager.find(Comment.class, result.getId());
        assertThat(savedReply.getParentComment().getId()).isEqualTo(topLevelComment1.getId());

        // 验证父评论包含该回复
        Comment parentComment = entityManager.find(Comment.class, topLevelComment1.getId());
        assertThat(parentComment.getReplies()).anyMatch(r -> r.getId().equals(savedReply.getId()));
    }

    @Test
    @Order(3)
    @DisplayName("测试获取顶级评论 - 验证分页查询")
    void testGetTopLevelComments_WithPagination() {
        // Given - 已有两条顶级评论

        // When - 获取顶级评论列表
        PageResponseDTO<CommentDTO> result = commentService.getTopLevelComments(
                testArticle.getId(), 0, 10, "createdAt");

        // Then - 验证分页结果
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getTotalElements()).isEqualTo(2L);
        assertThat(result.getContent())
                .extracting("content")
                .containsExactlyInAnyOrder("这是一条顶级评论", "这是另一条顶级评论");
    }

    @Test
    @Order(4)
    @DisplayName("测试获取评论回复 - 验证回复列表查询")
    void testGetCommentReplies_WithReplyList() {
        // Given - 创建多个回复
        for (int i = 0; i < 3; i++) {
            CommentCreateRequest replyRequest = CommentCreateRequest.builder()
                    .content("回复" + (i + 1))
                    .parentCommentId(topLevelComment1.getId())
                    .build();
            commentService.createComment(testArticle.getId(), commenter2.getId(), replyRequest);
        }
        entityManager.flush();
        entityManager.clear();

        // When - 获取回复列表
        PageResponseDTO<CommentReplyDTO> result = commentService.getCommentReplies(
                topLevelComment1.getId(), 0, 10, "createdAt");

        // Then - 验证回复列表
        assertThat(result.getContent()).hasSize(3);
        assertThat(result.getTotalElements()).isEqualTo(3L);
        assertThat(result.getContent())
                .extracting("content")
                .containsExactlyInAnyOrder("回复1", "回复2", "回复3");
    }

    @Test
    @Order(5)
    @DisplayName("测试删除评论 - 验证级联删除")
    void testDeleteComment_WithCascadeDelete() {
        // Given - 创建一个带回复的评论
        CommentCreateRequest replyRequest = CommentCreateRequest.builder()
                .content("这是一个回复")
                .parentCommentId(topLevelComment1.getId())
                .build();
        CommentDTO reply = commentService.createComment(
                testArticle.getId(), commenter2.getId(), replyRequest);
        entityManager.flush();
        entityManager.clear();

        Long replyId = reply.getId();
        assertThat(commentRepository.findById(replyId)).isPresent();

        // When - 删除回复
        commentService.deleteComment(replyId, commenter2.getId());

        // Then - 验证回复已被删除
        assertThat(commentRepository.findById(replyId)).isEmpty();

        // 验证父评论仍然存在
        assertThat(commentRepository.findById(topLevelComment1.getId())).isPresent();
    }

    @Test
    @Order(6)
    @DisplayName("测试删除评论失败 - 验证权限控制")
    void testDeleteCommentFailed_WithPermissionCheck() {
        // Given - 尝试用非评论者删除评论

        // When & Then - 应该抛出权限异常
        assertThrows(RuntimeException.class, () -> {
            commentService.deleteComment(topLevelComment1.getId(), commenter2.getId());
        });

        // 验证评论仍然存在
        assertThat(commentRepository.findById(topLevelComment1.getId())).isPresent();
    }

    @Test
    @Order(7)
    @DisplayName("测试批量删除文章评论 - 验证清理逻辑")
    void testDeleteCommentsByArticle_WithCleanupLogic() {
        // Given - 为文章创建多条评论和回复
        CommentCreateRequest replyRequest = CommentCreateRequest.builder()
                .content("顶级评论的回复")
                .parentCommentId(topLevelComment1.getId())
                .build();
        commentService.createComment(testArticle.getId(), commenter2.getId(), replyRequest);
        entityManager.flush();

        // When - 批量删除文章的所有评论
        int deletedCount = commentService.deleteCommentsByArticle(testArticle.getId());

        // Then - 验证删除数量
        assertThat(deletedCount).isEqualTo(3); // 2条顶级 + 1条回复

        // 验证所有评论已被删除
        var remainingComments = commentRepository.findByArticle(testArticle);
        assertThat(remainingComments).isEmpty();
    }

    @Test
    @Order(8)
    @DisplayName("测试获取用户评论 - 验证用户评论查询")
    void testGetUserComments_WithUserQuery() {
        // Given - commenter1 有一条评论

        // When - 获取用户的评论
        PageResponseDTO<CommentDTO> result = commentService.getUserComments(
                commenter1.getId(), 0, 10, "createdAt");

        // Then - 验证结果
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().getFirst().getContent())
                .isEqualTo("这是一条顶级评论");
    }

    @Test
    @Order(9)
    @DisplayName("测试创建回复失败 - 验证父评论不属于此文章")
    void testCreateReplyFailed_WithInvalidParentComment() {
        // Given - 创建另一篇文章
        Article anotherArticle = Article.builder()
                .title("另一篇文章")
                .content("这是另一篇文章的内容，长度足够生成摘要")
                .author(commenter1)
                .status(Article.ArticleStatus.RELEASE)
                .build();
        entityManager.persist(anotherArticle);
        entityManager.flush();

        // 在另一篇文章下创建评论
        CommentCreateRequest otherCommentRequest = CommentCreateRequest.builder()
                .content("其他文章的评论")
                .build();
        CommentDTO otherComment = commentService.createComment(
                anotherArticle.getId(), commenter1.getId(), otherCommentRequest);
        entityManager.flush();

        // When & Then - 尝试在当前文章下回复其他文章的评论应该失败
        CommentCreateRequest invalidReplyRequest = CommentCreateRequest.builder()
                .content("无效的回复")
                .parentCommentId(otherComment.getId())
                .build();

        assertThrows(IllegalArgumentException.class, () -> {
            commentService.createComment(testArticle.getId(), commenter2.getId(), invalidReplyRequest);
        });
    }

    @Test
    @Order(10)
    @DisplayName("测试评论层级结构 - 验证多级回复")
    void testCommentHierarchy_WithMultiLevelReplies() {
        // Given - 创建一级回复
        CommentCreateRequest level1ReplyRequest = CommentCreateRequest.builder()
                .content("一级回复")
                .parentCommentId(topLevelComment1.getId())
                .build();
        CommentDTO level1Reply = commentService.createComment(
                testArticle.getId(), commenter2.getId(), level1ReplyRequest);

        // When - 创建二级回复（回复一级回复）
        CommentCreateRequest level2ReplyRequest = CommentCreateRequest.builder()
                .content("二级回复")
                .parentCommentId(level1Reply.getId())
                .build();
        CommentDTO level2Reply = commentService.createComment(
                testArticle.getId(), commenter1.getId(), level2ReplyRequest);

        // Then - 验证层级结构
        entityManager.flush();
        entityManager.clear();

        Comment savedLevel2Reply = entityManager.find(Comment.class, level2Reply.getId());
        assertThat(savedLevel2Reply.getParentComment().getId()).isEqualTo(level1Reply.getId());

        // 验证一级回复包含二级回复
        Comment savedLevel1Reply = entityManager.find(Comment.class, level1Reply.getId());
        assertThat(savedLevel1Reply.getReplies()).anyMatch(r -> r.getId().equals(level2Reply.getId()));
    }

    @Test
    @Order(11)
    @DisplayName("测试事务回滚 - 验证异常时的数据一致性")
    void testTransactionRollback_OnException() {
        // Given - 准备一个会导致异常的操作（父评论不存在）
        CommentCreateRequest invalidRequest = CommentCreateRequest.builder()
                .content("无效的回复")
                .parentCommentId(999L) // 不存在的父评论ID
                .build();

        // When & Then - 应该抛出异常，且事务回滚
        assertThrows(jakarta.persistence.EntityNotFoundException.class, () -> {
            commentService.createComment(testArticle.getId(), commenter2.getId(), invalidRequest);
        });

        // 验证没有创建新的评论
        long count = commentRepository.count();
        assertThat(count).isEqualTo(2); // 只有 setUp 中创建的 2 条
    }

    @Test
    @Order(12)
    @DisplayName("测试复杂场景 - 完整的评论互动流程")
    void testComplexScenario_FullCommentWorkflow() {
        // Given - 模拟真实的评论互动场景

        // 用户1发表文章后，用户2评论
        CommentCreateRequest comment1Request = CommentCreateRequest.builder()
                .content("很好的文章！")
                .build();
        CommentDTO comment1 = commentService.createComment(
                testArticle.getId(), commenter2.getId(), comment1Request);

        // 用户1回复用户2的评论
        CommentCreateRequest reply1Request = CommentCreateRequest.builder()
                .content("谢谢支持！")
                .parentCommentId(comment1.getId())
                .build();
        CommentDTO reply1 = commentService.createComment(
                testArticle.getId(), commenter1.getId(), reply1Request);

        // 用户3（使用commenter2模拟）也来评论
        CommentCreateRequest comment2Request = CommentCreateRequest.builder()
                .content("受益匪浅")
                .build();
        CommentDTO comment2 = commentService.createComment(
                testArticle.getId(), commenter2.getId(), comment2Request);

        entityManager.flush();
        entityManager.clear();

        // When - 获取文章的所有顶级评论
        PageResponseDTO<CommentDTO> topLevelComments = commentService.getTopLevelComments(
                testArticle.getId(), 0, 10, "createdAt");

        // 获取第一条评论的回复
        PageResponseDTO<CommentReplyDTO> replies = commentService.getCommentReplies(
                comment1.getId(), 0, 10, "createdAt");

        // Then - 验证完整的评论结构
        assertThat(topLevelComments.getContent()).hasSize(4); // setUp中的2条 + 新创建的2条
        assertThat(replies.getContent()).hasSize(1);
        assertThat(replies.getContent().getFirst().getContent()).isEqualTo("谢谢支持！");

        // 验证数据库状态
        assertThat(commentRepository.count()).isEqualTo(5); // setUp中的2条 + 新创建的2条 + 1条回复
        assertThat(commentRepository.findByArticle(testArticle)).hasSize(5);

        // 验证新创建的评论确实在列表中
        assertThat(topLevelComments.getContent())
                .extracting("content")
                .containsExactlyInAnyOrder(
                        "这是一条顶级评论",
                        "这是另一条顶级评论",
                        "很好的文章！",
                        "受益匪浅"
                );
    }

}
