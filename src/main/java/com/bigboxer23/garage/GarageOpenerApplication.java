package com.bigboxer23.garage;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import java.io.IOException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;

@SpringBootApplication
@EnableScheduling
@OpenAPIDefinition(
		info =
				@Info(
						title = "Garage Opener",
						version = "1",
						description = "Allows rPi to control a garage door, including returning status"
								+ " about open/close state, when last opened, when house door"
								+ " was opened, temp, & humidity",
						contact =
								@Contact(
										name = "bigboxer23@gmail.com",
										url = "https://github.com/bigboxer23/PiGarage2")))
public class GarageOpenerApplication implements SchedulingConfigurer {
	public static void main(String[] args) {
		SpringApplication.run(GarageOpenerApplication.class, args);
	}

	public GarageOpenerApplication() throws IOException {}

	@Override
	public void configureTasks(ScheduledTaskRegistrar theTaskRegistrar) {
		theTaskRegistrar.setScheduler(taskExecutor());
	}

	@Bean(destroyMethod = "shutdown")
	public Executor taskExecutor() {
		return Executors.newScheduledThreadPool(10, new NamedThreadFactory());
	}

	private class NamedThreadFactory implements ThreadFactory {
		public Thread newThread(Runnable theRunnable) {
			return new Thread(theRunnable, "Thread-" + System.currentTimeMillis());
		}
	}
}
