package csulzc.My_Personal_Blogger.service;

import csulzc.My_Personal_Blogger.api.dto.article.*;
import csulzc.My_Personal_Blogger.api.dto.common.PageResponseDTO;
import csulzc.My_Personal_Blogger.api.dto.category.CategoryDTO;
import csulzc.My_Personal_Blogger.api.dto.user.UserProfileDTO;
import csulzc.My_Personal_Blogger.domain.entity.*;
import csulzc.My_Personal_Blogger.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ArticleService {

    private final ArticleRepository articleRepository;
    private final CategoryRepository categoryRepository;

    /**
     * 创建文章
     */
    @Transactional
    public ArticleDetailDTO createArticle(@Valid ArticleCreateRequest request, Long authorId) {
        // 获取作者信息（假设从用户服务获取）
        User author = new User();
        author.setId(authorId);
        
        // 创建文章实体
        Article article = Article.builder()
                .title(request.getTitle())
                .content(request.getContent())
                .summary(generateSummary(request.getContent()))
                .coverImage(request.getCoverImage())
                .status(request.getStatus() != null ? request.getStatus() : Article.ArticleStatus.DRAFT)
                .author(author)
                .likeCount(0)
                .favoriteCount(0)
                .build();
        
        // 设置分类
        if (request.getCategoryIds() != null && !request.getCategoryIds().isEmpty()) {
            Set<Category> categories = new HashSet<>(categoryRepository.findAllById(request.getCategoryIds()));
            
            if (categories.size() != request.getCategoryIds().size()) {
                throw new EntityNotFoundException("部分分类不存在");
            }
            
            categories.forEach(article::addCategory);
        }
        
        // 保存文章
        Article savedArticle = articleRepository.save(article);
        
        return convertToDetailDTO(savedArticle);
    }

    /**
     * 更新文章
     */
    @Transactional
    public ArticleDetailDTO updateArticle(Long articleId, @Valid ArticleUpdateRequest request, Long userId) {
        // 获取文章
        Article article = articleRepository.findById(articleId)
                .orElseThrow(() -> new EntityNotFoundException("文章不存在"));
        
        // 检查权限（只有作者可以修改）
        if (!article.getAuthor().getId().equals(userId)) {
            throw new RuntimeException("无权限修改此文章");
        }
        
        // 更新字段
        if (request.getTitle() != null) {
            article.setTitle(request.getTitle());
        }
        
        if (request.getContent() != null) {
            article.setContent(request.getContent());
            article.setSummary(generateSummary(request.getContent()));
        }
        
        if (request.getSummary() != null) {
            article.setSummary(request.getSummary());
        }
        
        if (request.getCoverImage() != null) {
            article.setCoverImage(request.getCoverImage());
        }
        
        if (request.getStatus() != null) {
            article.setStatus(request.getStatus());
        }
        
        // 更新分类
        if (request.getCategoryIds() != null) {
            // 清空现有分类
            Set<Category> existingCategories = new HashSet<>(article.getCategories());
            existingCategories.forEach(article::removeCategory);
            
            // 添加新分类
            Set<Category> newCategories = new HashSet<>(categoryRepository.findAllById(request.getCategoryIds()));
            
            if (newCategories.size() != request.getCategoryIds().size()) {
                throw new EntityNotFoundException("部分分类不存在");
            }
            
            newCategories.forEach(article::addCategory);
        }
        
        // 保存并返回
        Article updatedArticle = articleRepository.save(article);
        return convertToDetailDTO(updatedArticle);
    }

    /**
     * 发布文章（将状态改为 RELEASE）
     */
    @Transactional
    public ArticleDetailDTO publishArticle(Long articleId, Long userId) {
        Article article = articleRepository.findById(articleId)
                .orElseThrow(() -> new EntityNotFoundException("文章不存在"));
        
        // 检查权限
        if (!article.getAuthor().getId().equals(userId)) {
            throw new RuntimeException("无权限发布此文章");
        }
        
        article.setStatus(Article.ArticleStatus.RELEASE);
        Article publishedArticle = articleRepository.save(article);
        
        return convertToDetailDTO(publishedArticle);
    }

    /**
     * 归档文章
     */
    @Transactional
    public ArticleDetailDTO archiveArticle(Long articleId, Long userId) {
        Article article = articleRepository.findById(articleId)
                .orElseThrow(() -> new EntityNotFoundException("文章不存在"));
        
        if (!article.getAuthor().getId().equals(userId)) {
            throw new RuntimeException("无权限归档此文章");
        }
        
        article.setStatus(Article.ArticleStatus.ARCHIVE);
        Article archivedArticle = articleRepository.save(article);
        
        return convertToDetailDTO(archivedArticle);
    }

    /**
     * 根据 ID 获取文章详情
     */
    public ArticleDetailDTO getArticleById(Long articleId) {
        Article article = articleRepository.findById(articleId)
                .orElseThrow(() -> new EntityNotFoundException("文章不存在"));
        
        return convertToDetailDTO(article);
    }

    /**
     * 获取文章列表（分页）
     */
    public PageResponseDTO<ArticleListItemDTO> getArticleList(Pageable pageable) {
        Page<Article> articlePage = articleRepository.findAll(pageable);
        
        List<ArticleListItemDTO> content = articlePage.getContent().stream()
                .map(this::convertToListItemDTO)
                .collect(Collectors.toList());
        
        return PageResponseDTO.<ArticleListItemDTO>builder()
                .content(content)
                .totalElements(articlePage.getTotalElements())
                .totalPages(articlePage.getTotalPages())
                .page(articlePage.getNumber())
                .size(articlePage.getSize())
                .build();
    }

    /**
     * 根据作者获取文章列表
     */
    public PageResponseDTO<ArticleListItemDTO> getArticlesByAuthor(User author, Pageable pageable) {
        Page<Article> articlePage = articleRepository.findByAuthor(author, pageable);
        
        List<ArticleListItemDTO> content = articlePage.getContent().stream()
                .map(this::convertToListItemDTO)
                .collect(Collectors.toList());
        
        return PageResponseDTO.<ArticleListItemDTO>builder()
                .content(content)
                .totalElements(articlePage.getTotalElements())
                .totalPages(articlePage.getTotalPages())
                .page(articlePage.getNumber())
                .size(articlePage.getSize())
                .build();
    }

    /**
     * 搜索文章（根据标题）
     */
    public PageResponseDTO<ArticleListItemDTO> searchArticles(String keyword, Pageable pageable) {
        Page<Article> articlePage = articleRepository.findByTitleContaining(keyword, pageable);
        
        List<ArticleListItemDTO> content = articlePage.getContent().stream()
                .map(this::convertToListItemDTO)
                .collect(Collectors.toList());
        
        return PageResponseDTO.<ArticleListItemDTO>builder()
                .content(content)
                .totalElements(articlePage.getTotalElements())
                .totalPages(articlePage.getTotalPages())
                .page(articlePage.getNumber())
                .size(articlePage.getSize())
                .build();
    }

    /**
     * 删除文章
     */
    @Transactional
    public void deleteArticle(Long articleId, Long userId) {
        Article article = articleRepository.findById(articleId)
                .orElseThrow(() -> new EntityNotFoundException("文章不存在"));
        
        if (!article.getAuthor().getId().equals(userId)) {
            throw new RuntimeException("无权限删除此文章");
        }
        
        articleRepository.delete(article);
    }

    /**
     * 生成摘要（从内容中提取前 200 个字符）
     */
    private String generateSummary(String content) {
        if (content == null || content.isEmpty()) {
            return "";
        }
        
        int maxLength = 200;
        if (content.length() <= maxLength) {
            return content;
        }
        
        return content.substring(0, maxLength) + "...";
    }

    /**
     * 转换为 ArticleDetailDTO
     */
    private ArticleDetailDTO convertToDetailDTO(Article article) {
        return ArticleDetailDTO.builder()
                .id(article.getId())
                .title(article.getTitle())
                .content(article.getContent())
                .summary(article.getSummary())
                .coverImage(article.getCoverImage())
                .status(article.getStatus())
                .likeCount(article.getLikeCount())
                .favoriteCount(article.getFavoriteCount())
                .commentCount(article.getComments().size())
                .author(convertToUserProfileDTO(article.getAuthor()))
                .categories(convertToCategoryDTOs(article.getCategories()))
                .build();
    }

    private UserProfileDTO convertToUserProfileDTO(User user) {
        if (user == null) {
            return null;
        }

        if (user.getId() == null) {
            return UserProfileDTO.builder()
                    .username(user.getUsername())
                    .displayName(user.getDisplayName())
                    .avatar(user.getAvatar())
                    .bio(user.getBio())
                    .createdAt(user.getCreatedAt())
                    .build();
        }

        return UserProfileDTO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .displayName(user.getDisplayName())
                .avatar(user.getAvatar())
                .bio(user.getBio())
                .createdAt(user.getCreatedAt())
                .build();
    }
    /**
     * 转换为 ArticleListItemDTO
     */
    private ArticleListItemDTO convertToListItemDTO(Article article) {
        return ArticleListItemDTO.builder()
                .id(article.getId())
                .title(article.getTitle())
                .summary(article.getSummary())
                .coverImage(article.getCoverImage())
                .createdAt(article.getCreatedAt())
                .likeCount(article.getLikeCount())
                .commentCount(article.getComments().size())
                .author(convertToUserProfileDTO(article.getAuthor()))
                .categories(convertToCategoryDTOs(article.getCategories()))
                .build();
    }

    /**
     * 转换 Category 列表为 CategoryDTO 列表
     */
    private List<CategoryDTO> convertToCategoryDTOs(Set<Category> categories) {
        if (categories == null) {
            return new ArrayList<>();
        }
        
        return categories.stream()
                .map(category -> CategoryDTO.builder()
                        .name(category.getName())
                        .build())
                .collect(Collectors.toList());
    }
}