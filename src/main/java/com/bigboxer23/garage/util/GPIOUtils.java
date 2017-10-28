package com.bigboxer23.garage.util;

import com.pi4j.io.gpio.Pin;
import com.pi4j.io.gpio.RaspiPin;

/**
 *
 */
public class GPIOUtils
{
	/**
	 * Get a GPIO pin from an integer
	 *
	 * @param theStatusPin integer representation of the pin
	 * @return
	 */
	public static Pin getPin(int theStatusPin)
	{
		Pin aPin = RaspiPin.GPIO_02;
		switch (theStatusPin)
		{
			case 0:
				aPin = RaspiPin.GPIO_00;
				break;
			case 1:
				aPin = RaspiPin.GPIO_01;
				break;
			case 2:
				aPin = RaspiPin.GPIO_02;
				break;
			case 3:
				aPin = RaspiPin.GPIO_03;
				break;
			case 4:
				aPin = RaspiPin.GPIO_04;
				break;
			case 5:
				aPin = RaspiPin.GPIO_05;
				break;
			case 6:
				aPin = RaspiPin.GPIO_06;
				break;
			case 7:
				aPin = RaspiPin.GPIO_07;
				break;
			case 8:
				aPin = RaspiPin.GPIO_08;
				break;
			case 9:
				aPin = RaspiPin.GPIO_09;
				break;
			case 10:
				aPin = RaspiPin.GPIO_10;
				break;
			case 11:
				aPin = RaspiPin.GPIO_11;
				break;
			case 12:
				aPin = RaspiPin.GPIO_12;
				break;
			case 13:
				aPin = RaspiPin.GPIO_13;
				break;
			case 14:
				aPin = RaspiPin.GPIO_14;
				break;
			case 15:
				aPin = RaspiPin.GPIO_15;
				break;
			case 16:
				aPin = RaspiPin.GPIO_16;
				break;
			case 17:
				aPin = RaspiPin.GPIO_17;
				break;
			case 18:
				aPin = RaspiPin.GPIO_18;
				break;
			case 19:
				aPin = RaspiPin.GPIO_19;
				break;
			case 20:
				aPin = RaspiPin.GPIO_20;
				break;
		}
		return aPin;
	}
}
