package csulzc.My_Personal_Blogger.controller;

import csulzc.My_Personal_Blogger.api.dto.category.*;
import csulzc.My_Personal_Blogger.api.dto.common.PageResponseDTO;
import csulzc.My_Personal_Blogger.api.response.Result;
import csulzc.My_Personal_Blogger.service.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
@Tag(name = "分类管理", description = "分类CRUD及父子类关系相关接口")
public class CategoryController {

    private final CategoryService categoryService;

    /**
     * 创建分类
     */
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "创建分类", description = "创建一个新的分类")
    public ResponseEntity<Result<CategoryDTO>> createCategory(
            @Valid @RequestBody CategoryRequest request) {
        CategoryDTO category = categoryService.createCategory(request);
        return ResponseEntity.ok(Result.success(category, "分类创建成功"));
    }

    /**
     * 更新分类
     */
    @PutMapping("/{categoryId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "更新分类", description = "更新指定ID的分类")
    public ResponseEntity<Result<CategoryDTO>> updateCategory(
            @PathVariable Long categoryId,
            @Valid @RequestBody CategoryRequest request) {
        if (categoryId == null || categoryId <= 0) {
            throw new IllegalArgumentException("分类ID无效");
        }
        CategoryDTO category = categoryService.updateCategory(categoryId, request);
        return ResponseEntity.ok(Result.success(category, "分类更新成功"));
    }

    /**
     * 获取分类详情（通过ID）
     */
    @GetMapping("/{categoryId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "获取分类详情", description = "获取指定ID的分类详情")
    public ResponseEntity<Result<CategoryDTO>> getCategoryById(@PathVariable Long categoryId) {
        if (categoryId == null || categoryId <= 0) {
            throw new IllegalArgumentException("分类ID无效");
        }
        CategoryDTO category = categoryService.getCategoryById(categoryId);
        return ResponseEntity.ok(Result.success(category));
    }

    /**
     * 获取分类详情（通过名称）
     */
    @GetMapping("/name/{name}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "获取分类详情", description = "获取指定名称的分类详情")
    public ResponseEntity<Result<CategoryDTO>> getCategoryByName(@PathVariable String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("分类名称不能为空");
        }
        CategoryDTO category = categoryService.getCategoryByName(name);
        return ResponseEntity.ok(Result.success(category));
    }

    /**
     * 获取所有顶级分类
     */
    @GetMapping("/top-level")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "获取所有顶级分类", description = "获取所有顶级分类列表")
    public ResponseEntity<Result<List<CategoryDTO>>> getAllTopLevelCategories() {
        List<CategoryDTO> categories = categoryService.getAllTopLevelCategories();
        return ResponseEntity.ok(Result.success(categories));
    }

    /**
     * 获取某个分类的所有子分类
     */
    @GetMapping("/{categoryId}/subcategories")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "获取子分类", description = "获取指定分类的所有子分类")
    public ResponseEntity<Result<List<CategoryDTO>>> getSubCategories(@PathVariable Long categoryId) {
        if (categoryId == null || categoryId <= 0) {
            throw new IllegalArgumentException("分类ID无效");
        }
        List<CategoryDTO> subCategories = categoryService.getSubCategories(categoryId);
        return ResponseEntity.ok(Result.success(subCategories));
    }

    /**
     * 获取所有分类（分页）
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "获取所有分类", description = "获取所有分类列表，分页显示")
    public ResponseEntity<Result<PageResponseDTO<CategoryDTO>>> getAllCategories(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy) {
        if (page < 0) {
            throw new IllegalArgumentException("页码不能为负数");
        }
        if (size <= 0 || size > 100) {
            throw new IllegalArgumentException("每页大小必须在1-100之间");
        }
        PageResponseDTO<CategoryDTO> categories = categoryService.getAllCategories(page, size, sortBy);
        return ResponseEntity.ok(Result.success(categories));
    }

    /**
     * 获取分类树（用于前端下拉选择器）
     */
    @GetMapping("/tree")
    @Operation(summary = "获取分类树", description = "获取分类树结构，用于前端下拉选择器")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Result<List<CategoryTreeDTO>>> buildCategoryTree() {
        List<CategoryTreeDTO> tree = categoryService.buildCategoryTree();
        return ResponseEntity.ok(Result.success(tree));
    }

    /**
     * 获取分类的完整路径（从根到当前分类）
     */
    @GetMapping("/{categoryId}/path")
    @Operation(summary = "获取分类路径", description = "获取指定分类的完整路径，从根到当前分类")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Result<List<CategoryDTO>>> getCategoryPath(@PathVariable Long categoryId) {
        if (categoryId == null || categoryId <= 0) {
            throw new IllegalArgumentException("分类ID无效");
        }
        List<CategoryDTO> path = categoryService.getCategoryPath(categoryId);
        return ResponseEntity.ok(Result.success(path));
    }

    /**
     * 获取所有分类及其文章数量统计
     */
    @Operation(summary = "获取分类统计", description = "获取所有分类及其文章数量统计")
    @GetMapping("/statistics")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Result<List<CategoryStatDTO>>> getCategoryStatistics() {
        List<CategoryStatDTO> stats = categoryService.getCategoryStatistics();
        return ResponseEntity.ok(Result.success(stats));
    }

    /**
     * 获取分类的文章占比统计
     */
    @Operation(summary = "获取分类占比统计", description = "获取所有分类的文章占比统计")
    @GetMapping("/statistics/percentage")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Result<List<CategoryStatDTO>>> getCategoryPercentageStats() {
        List<CategoryStatDTO> stats = categoryService.getCategoryPercentageStats();
        return ResponseEntity.ok(Result.success(stats));
    }

    /**
     * 计算分类的文章数量（包含子分类）
     */
    @Operation(summary = "获取分类文章数量", description = "获取指定分类的文章数量，包含子分类")
    @GetMapping("/{categoryId}/article-count")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Result<Long>> countArticlesInCategoryIncludingSubCategories(
            @PathVariable Long categoryId) {
        if (categoryId == null || categoryId <= 0) {
            throw new IllegalArgumentException("分类ID无效");
        }
        long count = categoryService.countArticlesInCategoryIncludingSubCategories(categoryId);
        return ResponseEntity.ok(Result.success(count));
    }

    /**
     * 搜索分类
     */
    @Operation(summary = "搜索分类", description = "根据关键词搜索分类")
    @GetMapping("/search")
    public ResponseEntity<Result<List<CategoryDTO>>> searchCategories(
            @RequestParam String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            throw new IllegalArgumentException("搜索关键词不能为空");
        }
        List<CategoryDTO> categories = categoryService.searchCategories(keyword);
        return ResponseEntity.ok(Result.success(categories));
    }

    /**
     * 获取有文章的分类列表
     */
    @Operation(summary = "获取有文章的分类", description = "获取所有有文章的分类列表")
    @GetMapping("/with-articles")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Result<List<CategoryDTO>>> getCategoriesWithArticles() {
        List<CategoryDTO> categories = categoryService.getCategoriesWithArticles();
        return ResponseEntity.ok(Result.success(categories));
    }

    /**
     * 删除分类
     */
    @Operation(summary = "删除分类", description = "删除指定ID的分类")
    @DeleteMapping("/{categoryId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Result<Void>> deleteCategory(@PathVariable Long categoryId) {
        if (categoryId == null || categoryId <= 0) {
            throw new IllegalArgumentException("分类ID无效");
        }
        categoryService.deleteCategory(categoryId);
        return ResponseEntity.ok(Result.success(null, "分类删除成功"));
    }

    /**
     * 删除分类并转移文章
     */
    @Operation(summary = "删除分类并转移文章", description = "删除指定ID的分类，并将文章转移至目标分类")
    @DeleteMapping("/{categoryId}/transfer")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Result<Void>> deleteCategoryAndTransferArticles(
            @PathVariable Long categoryId,
            @RequestParam(required = false) Long targetCategoryId) {
        if (categoryId == null || categoryId <= 0) {
            throw new IllegalArgumentException("分类ID无效");
        }
        categoryService.deleteCategoryAndTransferArticles(categoryId, targetCategoryId);
        return ResponseEntity.ok(Result.success(null, "分类删除成功，文章已转移"));
    }

    /**
     * 获取分类总数
     */
    @Operation(summary = "获取分类总数", description = "获取分类的总数")
    @GetMapping("/stats/total")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Result<Long>> getTotalCategoryCount() {
        long count = categoryService.getTotalCategoryCount();
        return ResponseEntity.ok(Result.success(count));
    }

    /**
     * 获取顶级分类数量
     */
    @Operation(summary = "获取顶级分类数量", description = "获取顶级分类的数量")
    @GetMapping("/stats/top-level")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Result<Long>> getTopLevelCategoryCount() {
        long count = categoryService.getTopLevelCategoryCount();
        return ResponseEntity.ok(Result.success(count));
    }
}
