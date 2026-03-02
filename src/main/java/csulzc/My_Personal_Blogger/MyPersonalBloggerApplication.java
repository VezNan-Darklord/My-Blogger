package csulzc.My_Personal_Blogger;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
@RestController
public class MyPersonalBloggerApplication {

	public static void main(String[] args)
	{
		SpringApplication.run(MyPersonalBloggerApplication.class, args);
	}
	@GetMapping("/")
	public String hello()
	{
		return "Hello, Spring Boot!";
	}
}
