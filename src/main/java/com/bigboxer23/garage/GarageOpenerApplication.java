package com.bigboxer23.garage;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;

import java.io.IOException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

@SpringBootApplication
@EnableScheduling
public class GarageOpenerApplication implements SchedulingConfigurer
{
	public static void main(String[] args)
	{
		SpringApplication.run(GarageOpenerApplication.class, args);
	}

	public GarageOpenerApplication() throws IOException
	{
	}

	@Override
	public void configureTasks(ScheduledTaskRegistrar theTaskRegistrar)
	{
		theTaskRegistrar.setScheduler(taskExecutor());
	}

	@Bean(destroyMethod="shutdown")
	public Executor taskExecutor()
	{
		return Executors.newScheduledThreadPool(10, new NamedThreadFactory());
	}

	private class NamedThreadFactory implements ThreadFactory
	{
		public Thread newThread(Runnable theRunnable) {
			return new Thread(theRunnable, "Thread-" + System.currentTimeMillis());
		}
	}
}
