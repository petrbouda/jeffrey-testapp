package jeffrey.testapp.server;

import jeffrey.testapp.server.leak.NativeMemoryAllocator;
import jeffrey.testapp.server.leak.NativeMemoryAllocatorImpl;
import jeffrey.testapp.server.leak.NoopNativeMemoryAllocator;
import jeffrey.testapp.server.service.EfficientPersonService;
import jeffrey.testapp.server.service.InefficientPersonService;
import jeffrey.testapp.server.service.PersonService;
import org.apache.commons.collections4.queue.CircularFifoQueue;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.sql.DataSource;
import java.util.Queue;

@Configuration
public class ApplicationConfiguration implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new RequestInterceptor())
                .order(Ordered.HIGHEST_PRECEDENCE);
    }

    @Bean
    public RequestInterceptor requestInterceptor() {
        return new RequestInterceptor();
    }

    @Bean
    public NamedParameterJdbcTemplate jdbcTemplate(DataSource dataSource) {
        return new NamedParameterJdbcTemplate(dataSource);
    }

    @Bean
    public NativeMemoryAllocator nativeMemoryLeakingService(
            @Value("${native-memory-leak.mode:false}") boolean nativeMemoryLeakMode) {

        if (nativeMemoryLeakMode) {
            System.out.println("Native memory leak mode: Enabled.");
            return new NativeMemoryAllocatorImpl();
        } else {
            System.out.println("Native memory leak mode: Disabled.");
            return new NoopNativeMemoryAllocator();
        }
    }

    @Bean
    public PersonService personService(
            @Value("${efficient.mode:true}") boolean efficientMode,
            PersonRepository personRepository,
            NativeMemoryAllocator nativeMemoryAllocator) {

        System.out.println("EFFICIENT_MODE=" + efficientMode);
        return efficientMode
                ? new EfficientPersonService(personRepository, nativeMemoryAllocator)
                : new InefficientPersonService(personRepository, nativeMemoryAllocator);
    }

    @Bean
    public PersonRepository personRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        return new PersonRepository(jdbcTemplate);
    }

    @Bean
    public Queue<Person> personQueue() {
        Integer cacheSize = Integer.getInteger("cacheSize");
        // It's not properly synchronized, but I don't care :)
        // I just need to keep some data in Old Generation
        return cacheSize == null
                ? new NoOpQueue<>()
                : new CircularFifoQueue<>(cacheSize);
    }
}
