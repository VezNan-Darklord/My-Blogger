package csulzc.My_Personal_Blogger;

import csulzc.My_Personal_Blogger.api.dto.category.CategoryDTO;
import csulzc.My_Personal_Blogger.api.dto.category.CategoryRequest;
import csulzc.My_Personal_Blogger.api.dto.category.CategoryStatDTO;
import csulzc.My_Personal_Blogger.api.dto.category.CategoryTreeDTO;
import csulzc.My_Personal_Blogger.domain.entity.Article;
import csulzc.My_Personal_Blogger.domain.entity.Category;
import csulzc.My_Personal_Blogger.domain.entity.User;
import csulzc.My_Personal_Blogger.repository.ArticleRepository;
import csulzc.My_Personal_Blogger.repository.CategoryRepository;
import csulzc.My_Personal_Blogger.repository.UserRepository;
import csulzc.My_Personal_Blogger.service.CategoryService;
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
@Import({CategoryService.class})
@Transactional
@DisplayName("Category 集成测试 - 层间协作")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class CategoryIntegrationTest {

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ArticleRepository articleRepository;

    @Autowired
    private UserRepository userRepository;

    private Category parentCategory;
    private Category childCategory1;
    private Category childCategory2;
    private User testUser;

    @BeforeEach
    void setUp() {
        // 创建测试用户
        testUser = User.builder()
                .username("testuser")
                .email("test@example.com")
                .passwordHash("password123")
                .displayName("测试用户")
                .status(User.UserStatus.ACTIVE)
                .build();
        entityManager.persist(testUser);

        // 创建顶级分类
        parentCategory = Category.builder()
                .name("技术")
                .description("技术相关文章")
                .build();
        entityManager.persist(parentCategory);

        // 创建子分类
        childCategory1 = Category.builder()
                .name("Java")
                .description("Java 编程")
                .parentCategory(parentCategory)
                .build();
        entityManager.persist(childCategory1);

        childCategory2 = Category.builder()
                .name("Spring")
                .description("Spring 框架")
                .parentCategory(parentCategory)
                .build();
        entityManager.persist(childCategory2);

        // 更新父分类的子分类列表
        parentCategory.getSubCategories().add(childCategory1);
        parentCategory.getSubCategories().add(childCategory2);

        entityManager.flush();
    }

    @AfterEach
    void tearDown() {
        entityManager.clear();
    }

    @Test
    @Order(1)
    @DisplayName("测试分类创建 - 验证层级关系持久化")
    void testCategoryCreation_WithHierarchyRelationships() {
        // Given - 准备创建新的子分类
        CategoryRequest request = CategoryRequest.builder()
                .name("Python")
                .description("Python 编程")
                .parentCategoryId(parentCategory.getId())
                .build();

        // When - 通过服务层创建分类
        CategoryDTO result = categoryService.createCategory(request);

        // Then - 验证返回结果
        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("Python");
        assertThat(result.getDescription()).isEqualTo("Python 编程");
        assertThat(result.getParentCategoryId()).isEqualTo(parentCategory.getId());
        assertThat(result.getParentCategoryName()).isEqualTo("技术");

        // 验证数据库中的实体关系
        Optional<Category> savedCategoryOpt = categoryRepository.findById(result.getId());
        assertThat(savedCategoryOpt).isPresent();
        Category savedCategory = savedCategoryOpt.get();

        assertThat(savedCategory.getParentCategory().getId()).isEqualTo(parentCategory.getId());
        assertThat(savedCategory.getName()).isEqualTo("Python");

        // 验证父分类包含新创建的子分类（通过反向查询验证）
        var subCategories = categoryRepository.findByParentCategory(parentCategory);
        assertThat(subCategories).anyMatch(c -> c.getId().equals(savedCategory.getId()));
    }

    @Test
    @Order(2)
    @DisplayName("测试分类更新 - 验证层级关系变更")
    void testCategoryUpdate_WithHierarchyChanges() {
        // Given - 将子分类从 parentCategory 移到另一个父分类
        Category newParent = Category.builder()
                .name("编程语言")
                .description("各种编程语言")
                .build();
        entityManager.persist(newParent);
        entityManager.flush();

        CategoryRequest updateRequest = CategoryRequest.builder()
                .name("Java 编程")
                .description("更新后的 Java 描述")
                .parentCategoryId(newParent.getId())
                .build();

        // When - 执行更新
        CategoryDTO result = categoryService.updateCategory(childCategory1.getId(), updateRequest);

        // Then - 验证更新结果
        assertThat(result.getName()).isEqualTo("Java 编程");
        assertThat(result.getDescription()).isEqualTo("更新后的 Java 描述");
        assertThat(result.getParentCategoryId()).isEqualTo(newParent.getId());
        assertThat(result.getParentCategoryName()).isEqualTo("编程语言");

        // 验证数据库中的变化
        entityManager.flush();
        entityManager.clear();

        Category updatedCategory = entityManager.find(Category.class, childCategory1.getId());
        assertThat(updatedCategory.getName()).isEqualTo("Java 编程");
        assertThat(updatedCategory.getParentCategory().getId()).isEqualTo(newParent.getId());

        // 验证原父分类不再包含该子分类
        var oldParentSubCategories = categoryRepository.findByParentCategory(parentCategory);
        assertThat(oldParentSubCategories).noneMatch(c -> c.getId().equals(childCategory1.getId()));

        // 验证新父分类包含该子分类
        var newParentSubCategories = categoryRepository.findByParentCategory(newParent);
        assertThat(newParentSubCategories).anyMatch(c -> c.getId().equals(childCategory1.getId()));
    }

    @Test
    @Order(3)
    @DisplayName("测试分类树构建 - 验证递归查询和树形结构")
    void testCategoryTreeBuilding_WithRecursiveStructure() {
        // Given - 已有多级分类结构（setUp 中创建）

        // When - 构建分类树
        List<CategoryTreeDTO> tree = categoryService.buildCategoryTree();

        // Then - 验证树形结构
        assertThat(tree).isNotEmpty();

        // 找到"技术"分类节点
        CategoryTreeDTO techNode = tree.stream()
                .filter(node -> "技术".equals(node.getName()))
                .findFirst()
                .orElse(null);

        assertThat(techNode).isNotNull();
        assertThat(techNode.getChildren()).hasSize(2);
        assertThat(techNode.getChildren())
                .extracting("name")
                .containsExactlyInAnyOrder("Java", "Spring");
    }

    @Test
    @Order(4)
    @DisplayName("测试分类路径查询 - 验证从根到叶的路径追踪")
    void testCategoryPathQuery_WithRootToLeafTracking() {
        // Given - 使用 childCategory1（Java），其父分类是 parentCategory（技术）

        // When - 获取分类路径
        List<CategoryDTO> path = categoryService.getCategoryPath(childCategory1.getId());

        // Then - 验证路径正确性
        assertThat(path).hasSize(2);
        assertThat(path.get(0).getName()).isEqualTo("技术"); // 根节点
        assertThat(path.get(1).getName()).isEqualTo("Java"); // 当前节点
    }

    @Test
    @Order(5)
    @DisplayName("测试分类统计 - 验证文章数量计算")
    void testCategoryStatistics_WithArticleCountCalculation() {
        // Given - 为分类添加文章
        for (int i = 0; i < 3; i++) {
            Article article = Article.builder()
                    .title("Java 文章" + i)
                    .content("这是关于 Java 的文章内容，长度足够生成摘要")
                    .author(testUser)
                    .status(Article.ArticleStatus.RELEASE)
                    .build();
            article.addCategory(childCategory1);
            entityManager.persist(article);
        }

        for (int i = 0; i < 2; i++) {
            Article article = Article.builder()
                    .title("Spring 文章" + i)
                    .content("这是关于 Spring 的文章内容，长度足够生成摘要")
                    .author(testUser)
                    .status(Article.ArticleStatus.RELEASE)
                    .build();
            article.addCategory(childCategory2);
            entityManager.persist(article);
        }
        entityManager.flush();
        entityManager.clear();

        // When - 获取分类统计
        List<CategoryStatDTO> stats = categoryService.getCategoryStatistics();

        // Then - 验证统计结果
        assertThat(stats).isNotEmpty();

        CategoryStatDTO javaStat = stats.stream()
                .filter(s -> "Java".equals(s.getCategoryName()))
                .findFirst()
                .orElse(null);

        assertThat(javaStat).isNotNull();
        assertThat(javaStat.getArticleCount()).isEqualTo(3L);

        CategoryStatDTO springStat = stats.stream()
                .filter(s -> "Spring".equals(s.getCategoryName()))
                .findFirst()
                .orElse(null);

        assertThat(springStat).isNotNull();
        assertThat(springStat.getArticleCount()).isEqualTo(2L);
    }

    @Test
    @Order(6)
    @DisplayName("测试顶级分类查询 - 验证过滤逻辑")
    void testGetTopLevelCategories_WithFilteringLogic() {
        // Given - 已有顶级分类和子分类

        // When - 查询所有顶级分类
        List<CategoryDTO> topLevelCategories = categoryService.getAllTopLevelCategories();

        // Then - 验证只返回顶级分类
        assertThat(topLevelCategories).isNotEmpty();
        assertThat(topLevelCategories)
                .extracting("name")
                .contains("技术");

        // 验证不包含子分类
        assertThat(topLevelCategories)
                .extracting("name")
                .doesNotContain("Java", "Spring");
    }

    @Test
    @Order(7)
    @DisplayName("测试子分类查询 - 验证父子关系查询")
    void testGetSubCategories_WithParentChildQuery() {
        // Given - parentCategory 有两个子分类

        // When - 查询子分类
        List<CategoryDTO> subCategories = categoryService.getSubCategories(parentCategory.getId());

        // Then - 验证返回正确的子分类
        assertThat(subCategories).hasSize(2);
        assertThat(subCategories)
                .extracting("name")
                .containsExactlyInAnyOrder("Java", "Spring");
    }

    @Test
    @Order(8)
    @DisplayName("测试分类删除 - 验证约束检查和数据完整性")
    void testCategoryDeletion_WithConstraintChecks() {
        // Given - 创建一个没有文章的空分类
        Category emptyCategory = Category.builder()
                .name("临时分类")
                .description("这是一个临时分类")
                .build();
        entityManager.persist(emptyCategory);
        entityManager.flush();

        Long categoryId = emptyCategory.getId();

        // 验证分类存在
        assertThat(categoryRepository.findById(categoryId)).isPresent();

        // When - 删除空分类
        categoryService.deleteCategory(categoryId);

        // Then - 验证分类已被删除
        assertThat(categoryRepository.findById(categoryId)).isEmpty();
    }

    @Test
    @Order(9)
    @DisplayName("测试分类删除失败 - 验证有文章时的约束")
    void testCategoryDeletion_Failed_WithArticles() {
        // Given - 为分类添加文章
        Article article = Article.builder()
                .title("测试文章")
                .content("这是测试文章的内容，长度足够生成摘要信息")
                .author(testUser)
                .status(Article.ArticleStatus.RELEASE)
                .build();
        article.addCategory(childCategory1);
        entityManager.persist(article);
        entityManager.flush();

        // When & Then - 尝试删除有文章的分类应该失败
        assertThrows(IllegalStateException.class, () -> {
            categoryService.deleteCategory(childCategory1.getId());
        });

        // 验证分类仍然存在
        assertThat(categoryRepository.findById(childCategory1.getId())).isPresent();
    }

    @Test
    @Order(10)
    @DisplayName("测试分类删除失败 - 验证有子分类时的约束")
    void testCategoryDeletion_Failed_WithSubCategories() {
        // Given - parentCategory 有子分类

        // When & Then - 尝试删除有子分类的分类应该失败
        assertThrows(IllegalStateException.class, () -> {
            categoryService.deleteCategory(parentCategory.getId());
        });

        // 验证分类仍然存在
        assertThat(categoryRepository.findById(parentCategory.getId())).isPresent();
    }

    @Test
    @Order(11)
    @DisplayName("测试循环引用检测 - 验证层级关系完整性")
    void testCircularReferenceDetection_WithHierarchyIntegrity() {
        // Given - 尝试将父分类设置为自己的子分类（形成循环）
        CategoryRequest updateRequest = CategoryRequest.builder()
                .name("技术")
                .description("技术相关文章")
                .parentCategoryId(childCategory1.getId()) // 试图让 parentCategory 成为 childCategory1 的子分类
                .build();

        // When & Then - 应该抛出循环引用异常
        assertThrows(IllegalArgumentException.class, () -> {
            categoryService.updateCategory(parentCategory.getId(), updateRequest);
        });

        // 验证关系未被破坏
        Category parent = entityManager.find(Category.class, parentCategory.getId());
        assertThat(parent.getParentCategory()).isNull();
    }

    @Test
    @Order(12)
    @DisplayName("测试分类搜索 - 验证模糊查询功能")
    void testCategorySearch_WithFuzzyQuery() {
        // Given - 已有多个分类

        // When - 搜索包含"Java"的分类
        List<CategoryDTO> searchResults = categoryService.searchCategories("Java");

        // Then - 验证搜索结果
        assertThat(searchResults).isNotEmpty();
        assertThat(searchResults)
                .extracting("name")
                .contains("Java");

        // When - 搜索包含"编程"的分类
        List<CategoryDTO> programmingResults = categoryService.searchCategories("编程");

        // Then - 验证搜索结果（只有 Java 分类的描述包含"编程"）
        assertThat(programmingResults).hasSize(1);
        assertThat(programmingResults)
                .extracting("name")
                .contains("Java");

        // When - 搜索包含"技术"的分类（名称或描述匹配）
        List<CategoryDTO> techResults = categoryService.searchCategories("技术");

        // Then - 验证搜索结果包含顶级分类
        assertThat(techResults).isNotEmpty();
        assertThat(techResults)
                .extracting("name")
                .contains("技术");
    }


    @Test
    @Order(13)
    @DisplayName("测试分页查询 - 验证分页功能")
    void testGetAllCategories_WithPagination() {
        // Given - 创建更多分类
        for (int i = 0; i < 10; i++) {
            Category category = Category.builder()
                    .name("分类" + i)
                    .description("这是分类" + i + "的描述")
                    .build();
            entityManager.persist(category);
        }
        entityManager.flush();

        // When - 分页查询
        var pageResult = categoryService.getAllCategories(0, 5, "createdAt");

        // Then - 验证分页结果
        assertThat(pageResult.getContent()).hasSize(5);
        assertThat(pageResult.getPage()).isEqualTo(0);
        assertThat(pageResult.getSize()).isEqualTo(5);
        assertThat(pageResult.getTotalElements()).isEqualTo(13); // 3 + 10
    }

    @Test
    @Order(14)
    @DisplayName("测试事务回滚 - 验证异常时的数据一致性")
    void testTransactionRollback_OnException() {
        // Given - 准备一个会导致异常的操作（使用重复的名称）
        CategoryRequest invalidRequest = CategoryRequest.builder()
                .name("技术") // 已存在的名称
                .description("重复的分类名称")
                .build();

        // When & Then - 应该抛出异常，且事务回滚
        assertThrows(IllegalArgumentException.class, () -> {
            categoryService.createCategory(invalidRequest);
        });

        // 验证没有创建新的分类
        long count = categoryRepository.count();
        assertThat(count).isEqualTo(3); // 只有 setUp 中创建的 3 个
    }

    @Test
    @Order(15)
    @DisplayName("测试复杂场景 - 多级分类和文章关联的完整流程")
    void testComplexScenario_FullWorkflow() {
        // Given - 模拟真实场景：创建多级分类并关联文章

        // 创建三级分类结构
        CategoryRequest level2Request = CategoryRequest.builder()
                .name("后端开发")
                .description("后端相关技术")
                .parentCategoryId(parentCategory.getId())
                .build();
        CategoryDTO backendCategory = categoryService.createCategory(level2Request);

        CategoryRequest level3Request = CategoryRequest.builder()
                .name("微服务")
                .description("微服务架构")
                .parentCategoryId(backendCategory.getId())
                .build();
        CategoryDTO microserviceCategory = categoryService.createCategory(level3Request);

        // 为不同级别的分类添加文章
        Article article1 = Article.builder()
                .title("Java 基础教程")
                .content("这是关于 Java 基础的详细内容，长度足够生成摘要")
                .author(testUser)
                .status(Article.ArticleStatus.RELEASE)
                .build();
        article1.addCategory(entityManager.find(Category.class, childCategory1.getId()));
        entityManager.persist(article1);

        Article article2 = Article.builder()
                .title("Spring Boot 实战")
                .content("这是关于 Spring Boot 的详细内容，长度足够生成摘要")
                .author(testUser)
                .status(Article.ArticleStatus.RELEASE)
                .build();
        article2.addCategory(entityManager.find(Category.class, childCategory2.getId()));
        entityManager.persist(article2);

        entityManager.flush();
        entityManager.clear();

        // When - 执行各种查询操作
        // 1. 构建分类树
        List<CategoryTreeDTO> tree = categoryService.buildCategoryTree();

        // 2. 获取分类统计
        List<CategoryStatDTO> stats = categoryService.getCategoryStatistics();

        // 3. 获取分类路径
        List<CategoryDTO> path = categoryService.getCategoryPath(microserviceCategory.getId());

        // Then - 验证所有操作的结果
        assertThat(tree).isNotEmpty();
        assertThat(stats).hasSizeGreaterThanOrEqualTo(5);
        assertThat(path).hasSize(3); // 技术 -> 后端开发 -> 微服务
        assertThat(path.get(0).getName()).isEqualTo("技术");
        assertThat(path.get(1).getName()).isEqualTo("后端开发");
        assertThat(path.get(2).getName()).isEqualTo("微服务");

        // 验证数据库状态
        assertThat(categoryRepository.count()).isEqualTo(5); // 3 + 2
        assertThat(articleRepository.count()).isEqualTo(2);
    }
}
