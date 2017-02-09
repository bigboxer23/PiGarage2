package com.bigboxer23.garage;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.io.IOException;
import java.util.Arrays;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

@SpringBootApplication
@EnableScheduling
public class GarageOpenerApplication
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
}
