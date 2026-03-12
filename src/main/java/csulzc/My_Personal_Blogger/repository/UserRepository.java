package csulzc.My_Personal_Blogger.repository;

import csulzc.My_Personal_Blogger.domain.entity.User;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends BaseRepository<User, Long> {

    // 1. 根据用户名查询（派生查询）
    Optional<User> findByUsername(String username);

    // 2. 根据邮箱查询
    Optional<User> findByEmail(String email);

    // 3. 检查用户名是否存在
    boolean existsByUsername(String username);

    // 4. 自定义JPQL查询
    @Query("SELECT u FROM User u LEFT JOIN FETCH u.articles WHERE u.username = :username")
    Optional<User> findByUsernameWithArticles(@Param("username") String username);

    // 5. 统计活跃用户
    @Query("SELECT COUNT(u) FROM User u WHERE u.status = :status")
    long countByStatus(@Param("status") User.UserStatus status);
}

// TODO: 添加其测试类