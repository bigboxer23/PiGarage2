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
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

@SpringBootApplication
@EnableScheduling
public class GarageOpenerApplication implements SchedulingConfigurer
{
	private static Logger myLogger = Logger.getLogger("com.bigboxer23");

	public static void main(String[] args)
	{
		SpringApplication.run(GarageOpenerApplication.class, args);
	}

	public GarageOpenerApplication() throws IOException
	{
		setupLogger();
	}

	private void setupLogger() throws IOException
	{
		FileHandler aHandler = new FileHandler(System.getProperty("log.location", "/home/pi/garage/logs/piGarage.log"), true);
		aHandler.setFormatter(new SimpleFormatter());
		myLogger.addHandler(aHandler);
		myLogger.setLevel(Level.parse(System.getProperty("log.level", "WARNING")));
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
			return new Thread(theRunnable, "MyThread");
		}
	}
}
