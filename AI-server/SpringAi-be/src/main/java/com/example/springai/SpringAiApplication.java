package com.example.springai;

import com.cyh.advice.CommonExceptionAdvice;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

@SpringBootApplication(exclude = {
		org.springframework.ai.model.ollama.autoconfigure.OllamaChatAutoConfiguration.class,
},scanBasePackages = {"com.example"})
@Import(CommonExceptionAdvice.class) //注册异常处理器
public class SpringAiApplication {

	public static void main(String[] args) {
		SpringApplication.run(SpringAiApplication.class, args);
	}

}
