package csulzc.My_Personal_Blogger.config;

import jakarta.persistence.EntityManagerFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.TransactionTemplate;

@Slf4j
@Configuration
@EnableTransactionManagement
public class TransactionConfig {

    /**
     * 自定义事务管理器，设置默认超时时间为 30 秒
     */
    @Bean
    public PlatformTransactionManager transactionManager(EntityManagerFactory entityManagerFactory) {
        JpaTransactionManager transactionManager = new JpaTransactionManager(entityManagerFactory);
        // 设置默认事务超时时间为 30 秒
        transactionManager.setDefaultTimeout(30);
        log.info("事务管理器已配置，默认超时时间: 30秒");
        return transactionManager;
    }

    /**
     * 提供 TransactionTemplate 方便编程式事务控制
     */
    @Bean
    public TransactionTemplate transactionTemplate(PlatformTransactionManager transactionManager) {
        TransactionTemplate template = new TransactionTemplate(transactionManager);
        template.setTimeout(30);
        return template;
    }
}
