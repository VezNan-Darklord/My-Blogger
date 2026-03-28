package csulzc.My_Personal_Blogger.Service;

import csulzc.My_Personal_Blogger.api.dto.category.*;
import csulzc.My_Personal_Blogger.api.dto.common.PageResponseDTO;
import csulzc.My_Personal_Blogger.domain.entity.Category;
import csulzc.My_Personal_Blogger.repository.CategoryRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CategoryService {

    private final CategoryRepository categoryRepository;

    // ==================== 分类创建与更新 ====================

    /**
     * 创建分类
     */
    @Transactional
    public CategoryDTO createCategory(CategoryRequest request) {
        // 检查分类名称是否已存在
        if (categoryRepository.findByName(request.getName()).isPresent()) {
            throw new IllegalArgumentException("分类名称已存在");
        }

        // 构建分类实体
        Category.CategoryBuilder categoryBuilder = Category.builder()
                .name(request.getName())
                .description(request.getDescription());

        // 设置父分类
        if (request.getParentCategoryId() != null) {
            Category parentCategory = categoryRepository.findById(request.getParentCategoryId())
                    .orElseThrow(() -> new EntityNotFoundException("父分类不存在"));
            categoryBuilder.parentCategory(parentCategory);
        }

        Category category = categoryBuilder.build();
        Category savedCategory = categoryRepository.save(category);

        return convertToDTO(savedCategory);
    }

    /**
     * 更新分类信息
     */
    @Transactional
    public CategoryDTO updateCategory(Long categoryId, CategoryRequest request) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new EntityNotFoundException("分类不存在"));

        // 检查新名称是否与其他分类重复
        categoryRepository.findByName(request.getName())
                .filter(c -> !c.getId().equals(categoryId))
                .ifPresent(c -> {
                    throw new IllegalArgumentException("分类名称已存在");
                });

        // 更新字段
        category.setName(request.getName());
        category.setDescription(request.getDescription());

        // 更新父分类
        if (request.getParentCategoryId() != null) {
            if (request.getParentCategoryId().equals(categoryId)) {
                throw new IllegalArgumentException("不能将自己设置为父分类");
            }

            Category parentCategory = categoryRepository.findById(request.getParentCategoryId())
                    .orElseThrow(() -> new EntityNotFoundException("父分类不存在"));

            // 检查是否会形成循环引用
            if (isChildCategory(parentCategory, category)) {
                throw new IllegalArgumentException("不能将子分类设置为父分类，会形成循环引用");
            }

            category.setParentCategory(parentCategory);
        } else {
            category.setParentCategory(null);
        }

        Category updatedCategory = categoryRepository.save(category);
        return convertToDTO(updatedCategory);
    }

    // ==================== 分类查询 ====================

    /**
     * 根据 ID 获取分类详情
     */
    public CategoryDTO getCategoryById(Long categoryId) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new EntityNotFoundException("分类不存在"));
        return convertToDTO(category);
    }

    /**
     * 根据名称获取分类
     */
    public CategoryDTO getCategoryByName(String name) {
        Category category = categoryRepository.findByName(name)
                .orElseThrow(() -> new EntityNotFoundException("分类不存在"));
        return convertToDTO(category);
    }

    /**
     * 获取所有顶级分类（没有父分类的分类）
     */
    public List<CategoryDTO> getAllTopLevelCategories() {
        List<Category> categories = categoryRepository.findByParentCategoryIsNull();
        return categories.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * 获取某个分类的所有子分类
     */
    public List<CategoryDTO> getSubCategories(Long parentCategoryId) {
        Category parent = categoryRepository.findById(parentCategoryId)
                .orElseThrow(() -> new EntityNotFoundException("分类不存在"));

        return categoryRepository.findByParentCategory(parent).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * 分页查询所有分类
     */
    public PageResponseDTO<CategoryDTO> getAllCategories(int page, int size, String sortBy) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortBy).descending());
        Page<Category> categoryPage = categoryRepository.findAll(pageable);

        List<CategoryDTO> content = categoryPage.getContent().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());

        return PageResponseDTO.<CategoryDTO>builder()
                .content(content)
                .page(categoryPage.getNumber())
                .size(categoryPage.getSize())
                .totalElements(categoryPage.getTotalElements())
                .totalPages(categoryPage.getTotalPages())
                .first(categoryPage.isFirst())
                .last(categoryPage.isLast())
                .build();
    }

    // ==================== 分类树管理 ====================

    /**
     * 构建分类树（用于前端下拉树形选择器）
     */
    public List<CategoryTreeDTO> buildCategoryTree() {
        List<Category> allCategories = categoryRepository.findAll();

        // 找到所有顶级分类
        List<Category> topCategories = allCategories.stream()
                .filter(c -> c.getParentCategory() == null)
                .collect(Collectors.toList());

        // 递归构建树形结构
        return topCategories.stream()
                .map(c -> buildCategoryTreeNode(c, allCategories))
                .collect(Collectors.toList());
    }

    /**
     * 递归构建分类树节点
     */
    private CategoryTreeDTO buildCategoryTreeNode(Category category, List<Category> allCategories) {
        CategoryTreeDTO node = CategoryTreeDTO.builder()
                .id(category.getId())
                .name(category.getName())
                .description(category.getDescription())
                .articleCount(countArticlesInCategory(category))
                .children(new ArrayList<>())
                .build();

        // 查找直接子分类
        List<Category> directChildren = allCategories.stream()
                .filter(c -> category.equals(c.getParentCategory()))
                .collect(Collectors.toList());

        // 递归构建子节点
        List<CategoryTreeDTO> children = directChildren.stream()
                .map(child -> buildCategoryTreeNode(child, allCategories))
                .collect(Collectors.toList());

        node.setChildren(children);
        return node;
    }

    /**
     * 获取分类的完整路径（从根到当前分类）
     */
    public List<CategoryDTO> getCategoryPath(Long categoryId) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new EntityNotFoundException("分类不存在"));

        List<CategoryDTO> path = new ArrayList<>();
        Category current = category;

        while (current != null) {
            path.add(0, convertToDTO(current));
            current = current.getParentCategory();
        }

        return path;
    }

    // ==================== 分类统计 ====================

    /**
     * 获取所有分类及其文章数量
     */
    public List<CategoryStatDTO> getCategoryStatistics() {
        List<Object[]> results = categoryRepository.findAllWithArticleCount();

        return results.stream()
                .map(obj -> {
                    Category category = (Category) obj[0];
                    Long articleCount = (Long) obj[1];
                    return CategoryStatDTO.builder()
                            .categoryName(category.getName())
                            .articleCount(articleCount)
                            .build();
                })
                .collect(Collectors.toList());
    }

    /**
     * 计算分类的文章数量（包含子分类的文章）
     */
    public long countArticlesInCategoryIncludingSubCategories(Long categoryId) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new EntityNotFoundException("分类不存在"));

        return countArticlesRecursive(category);
    }

    /**
     * 递归计算分类及其子分类的文章总数
     */
    private long countArticlesRecursive(Category category) {
        long count = category.getArticles().size();

        for (Category subCategory : category.getSubCategories()) {
            count += countArticlesRecursive(subCategory);
        }

        return count;
    }

    /**
     * 计算分类的文章数量（不包含子分类）
     */
    private int countArticlesInCategory(Category category) {
        return category.getArticles().size();
    }

    /**
     * 获取分类的文章占比统计
     */
    public List<CategoryStatDTO> getCategoryPercentageStats() {
        List<CategoryStatDTO> stats = getCategoryStatistics();

        long totalArticles = stats.stream()
                .mapToLong(CategoryStatDTO::getArticleCount)
                .sum();

        if (totalArticles == 0) {
            return stats;
        }

        stats.forEach(stat -> {
            double percentage = (stat.getArticleCount() * 100.0) / totalArticles;
            stat.setPercentage(Math.round(percentage * 100.0) / 100.0);
        });

        return stats;
    }

    // ==================== 分类删除 ====================

    /**
     * 删除分类（如果分类下有文章或子分类，则不允许删除）
     */
    @Transactional
    public void deleteCategory(Long categoryId) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new EntityNotFoundException("分类不存在"));

        // 检查是否有文章关联
        if (!category.getArticles().isEmpty()) {
            throw new IllegalStateException("该分类下还有文章，无法删除");
        }

        // 检查是否有子分类
        if (!category.getSubCategories().isEmpty()) {
            throw new IllegalStateException("该分类还有子分类，无法删除");
        }

        // 如果有父分类，需要从父分类的子分类列表中移除
        Category parentCategory = category.getParentCategory();
        if (parentCategory != null) {
            parentCategory.getSubCategories().remove(category);
            categoryRepository.save(parentCategory);
        }

        categoryRepository.delete(category);
    }

    /**
     * 删除分类并转移文章到指定分类
     */
    @Transactional
    public void deleteCategoryAndTransferArticles(Long categoryId, Long targetCategoryId) {
        Category sourceCategory = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new EntityNotFoundException("源分类不存在"));

        // 检查是否有子分类
        if (!sourceCategory.getSubCategories().isEmpty()) {
            throw new IllegalStateException("该分类还有子分类，请先删除或转移子分类");
        }

        // 如果有目标分类，转移文章；否则直接移除关联
        if (targetCategoryId != null) {
            Category targetCategory = categoryRepository.findById(targetCategoryId)
                    .orElseThrow(() -> new EntityNotFoundException("目标分类不存在"));

            transferArticlesToTargetCategory(sourceCategory, targetCategory);
        } else {
            removeCategoryAssociation(sourceCategory);
        }

        categoryRepository.delete(sourceCategory);
    }

    private void transferArticlesToTargetCategory(Category source, Category target) {
        source.getArticles().forEach(article -> {
            article.removeCategory(source);
            article.addCategory(target);
        });
    }

    private void removeCategoryAssociation(Category source) {
        source.getArticles().forEach(article ->
                article.removeCategory(source)
        );
    }


    // ==================== 分类搜索 ====================

    /**
     * 搜索分类（根据名称或描述）
     */
    public List<CategoryDTO> searchCategories(String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return new ArrayList<>();
        }

        String lowerCaseKeyword = keyword.toLowerCase();

        return categoryRepository.findAll().stream()
                .filter(c -> c.getName().toLowerCase().contains(lowerCaseKeyword) ||
                        (c.getDescription() != null &&
                                c.getDescription().toLowerCase().contains(lowerCaseKeyword)))
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * 获取有文章的分类列表
     */
    public List<CategoryDTO> getCategoriesWithArticles() {
        return categoryRepository.findAll().stream()
                .filter(c -> !c.getArticles().isEmpty())
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    // ==================== 辅助方法 ====================

    /**
     * 检查是否是子分类（避免循环引用）
     */
    private boolean isChildCategory(Category potentialChild, Category potentialParent) {
        Category current = potentialChild;
        while (current != null) {
            if (current.equals(potentialParent)) {
                return true;
            }
            current = current.getParentCategory();
        }
        return false;
    }

    /**
     * 转换为 CategoryDTO
     */
    private CategoryDTO convertToDTO(Category category) {
        CategoryDTO dto = CategoryDTO.builder()
                .id(category.getId())
                .name(category.getName())
                .description(category.getDescription())
                .articleCount(countArticlesInCategory(category))
                .build();

        // 设置父分类信息
        if (category.getParentCategory() != null) {
            dto.setParentCategoryId(category.getParentCategory().getId());
            dto.setParentCategoryName(category.getParentCategory().getName());
        }

        // 设置子分类列表（只转换一层，避免无限递归）
        if (!category.getSubCategories().isEmpty()) {
            List<CategoryDTO> subCategoryDTOs = category.getSubCategories().stream()
                    .map(sub -> CategoryDTO.builder()
                            .id(sub.getId())
                            .name(sub.getName())
                            .description(sub.getDescription())
                            .articleCount(countArticlesInCategory(sub))
                            .build())
                    .collect(Collectors.toList());
            dto.setSubCategories(subCategoryDTOs);
        }

        return dto;
    }

    /**
     * 检查分类名称是否存在（排除指定 ID）
     */
    public boolean existsByName(String name, Long excludeId) {
        return categoryRepository.findByName(name)
                .filter(c -> !c.getId().equals(excludeId))
                .isPresent();
    }

    /**
     * 检查分类名称是否存在
     */
    public boolean existsByName(String name) {
        return categoryRepository.findByName(name).isPresent();
    }

    /**
     * 获取分类总数
     */
    public long getTotalCategoryCount() {
        return categoryRepository.count();
    }

    /**
     * 获取顶级分类数量
     */
    public long getTopLevelCategoryCount() {
        return categoryRepository.findByParentCategoryIsNull().size();
    }
}
