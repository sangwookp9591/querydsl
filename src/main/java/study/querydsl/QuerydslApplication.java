package study.querydsl;

import com.querydsl.jpa.impl.JPAQueryFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import javax.persistence.EntityManager;

@SpringBootApplication
public class QuerydslApplication {

	//first
	public static void main(String[] args) {
		SpringApplication.run(QuerydslApplication.class, args);
	}
	
	@Bean//스프링 빈으로 등록
	JPAQueryFactory JpaQueryFactory(EntityManager em){
		return new JPAQueryFactory(em);
	}

}
