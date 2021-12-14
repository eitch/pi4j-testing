/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ch.eitchnet;

import com.pi4j.Pi4J;
import com.pi4j.context.Context;
import com.pi4j.io.i2c.I2C;
import com.pi4j.io.i2c.I2CConfig;
import com.pi4j.io.i2c.I2CProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author luca
 */
public class I2cLcd {

	private static final Logger logger = LoggerFactory.getLogger(I2cLcd.class);

	private static final byte LCD_CLEARDISPLAY = (byte) 0x01;
	private static final byte LCD_RETURNHOME = (byte) 0x02;
	private static final byte LCD_ENTRYMODESET = (byte) 0x04;
	private static final byte LCD_DISPLAYCONTROL = (byte) 0x08;
	private static final byte LCD_CURSORSHIFT = (byte) 0x10;
	private static final byte LCD_FUNCTIONSET = (byte) 0x20;
	private static final byte LCD_SETCGRAMADDR = (byte) 0x40;
	private static final byte LCD_SETDDRAMADDR = (byte) 0x80;

	// flags for display entry mode
	private static final byte LCD_ENTRYRIGHT = (byte) 0x00;
	private static final byte LCD_ENTRYLEFT = (byte) 0x02;
	private static final byte LCD_ENTRYSHIFTINCREMENT = (byte) 0x01;
	private static final byte LCD_ENTRYSHIFTDECREMENT = (byte) 0x00;

	// flags for display on/off control
	private static final byte LCD_DISPLAYON = (byte) 0x04;
	private static final byte LCD_DISPLAYOFF = (byte) 0x00;
	private static final byte LCD_CURSORON = (byte) 0x02;
	private static final byte LCD_CURSOROFF = (byte) 0x00;
	private static final byte LCD_BLINKON = (byte) 0x01;
	private static final byte LCD_BLINKOFF = (byte) 0x00;

	// flags for display/cursor shift
	private static final byte LCD_DISPLAYMOVE = (byte) 0x08;
	private static final byte LCD_CURSORMOVE = (byte) 0x00;
	private static final byte LCD_MOVERIGHT = (byte) 0x04;
	private static final byte LCD_MOVELEFT = (byte) 0x00;

	// flags for function set
	private static final byte LCD_8BITMODE = (byte) 0x10;
	private static final byte LCD_4BITMODE = (byte) 0x00;
	private static final byte LCD_2LINE = (byte) 0x08;
	private static final byte LCD_1LINE = (byte) 0x00;
	private static final byte LCD_5x10DOTS = (byte) 0x04;
	private static final byte LCD_5x8DOTS = (byte) 0x00;

	// flags for backlight control
	private static final byte LCD_BACKLIGHT = (byte) 0x08;
	private static final byte LCD_NOBACKLIGHT = (byte) 0x00;

	private static final byte En = (byte) 0b00000100; // Enable bit
	private static final byte Rw = (byte) 0b00000010; // Read/Write bit
	private static final byte Rs = (byte) 0b00000001; // Register select bit

	private static final int DEFAULT_BUS = 0x1;
	private static final int DEFAULT_DEVICE = 0x27;

	private final I2C _device;
	private boolean backlight;

	public static void main(String[] args) throws Exception {
		Context pi4j = Pi4J.newAutoContext();
		I2cLcd lcd = new I2cLcd(pi4j);
		logger.info("pi4j configured. Writing to LCD...");
		Thread.sleep(2000L);

		lcd.clear();
		lcd.backlight(true);
		String string = "Hello, world!";
		lcd.displayStringPos(string, 1, 16 - string.length());
		string = "All is well!";
		//lcd.displayStringPos(string, 2, 16 - string.length());
		lcd.displayStringPos(string, 2);

		Thread.sleep(5000L);
		logger.info("Finished with pi4j, shutting down now...");
		lcd.clear();
		lcd.backlight(false);
		pi4j.shutdown();
	}

	public I2cLcd(Context pi4j) {
		this(buildI2CConfig(pi4j));
	}

	private static I2C buildI2CConfig(Context pi4j) {
		I2CProvider i2CProvider = pi4j.provider("linuxfs-i2c");
		I2CConfig i2cConfig = I2C.newConfigBuilder(pi4j).bus(DEFAULT_BUS).device(DEFAULT_DEVICE).build();
		return i2CProvider.create(i2cConfig);
	}

	public I2cLcd(I2C device) {
		_device = device;
		init();
	}

	/**
	 * Initializes the LCD with the backlight off
	 */
	public void init() {
		try {
			lcd_write((byte) 0x03);
			lcd_write((byte) 0x03);
			lcd_write((byte) 0x03);
			lcd_write((byte) 0x02);

			lcd_write((byte) (LCD_FUNCTIONSET | LCD_2LINE | LCD_5x8DOTS | LCD_4BITMODE));
			lcd_write((byte) (LCD_DISPLAYCONTROL | LCD_DISPLAYON));
			lcd_write(LCD_CLEARDISPLAY);
			lcd_write((byte) (LCD_ENTRYMODESET | LCD_ENTRYLEFT));

			Thread.sleep(0, 200000);

		} catch (InterruptedException e) {
			throw new IllegalStateException("Interrupted!");
		}
	}

	/**
	 * Turns the backlight on or off
	 */
	public void backlight(boolean state) {
		try {
			this.backlight = state;
			write_cmd(this.backlight ? LCD_BACKLIGHT : LCD_NOBACKLIGHT);
		} catch (InterruptedException e) {
			throw new IllegalStateException("Interrupted!");
		}
	}

	/**
	 * Displays the given string at the given line at the given position
	 */
	public void displayStringPos(String string, int line, int pos) {
		byte pos_new = 0;

		if (line == 1) {
			pos_new = (byte) pos;
		} else if (line == 2) {
			pos_new = (byte) (0x40 + pos);
		} else if (line == 3) {
			pos_new = (byte) (0x14 + pos);
		} else if (line == 4) {
			pos_new = (byte) (0x54 + pos);
		}

		try {
			lcd_write((byte) (0x80 + pos_new));

			for (int i = 0; i < string.length(); i++) {
				lcd_write((byte) string.charAt(i), Rs);
			}
		} catch (InterruptedException e) {
			throw new IllegalStateException("Interrupted!");
		}
	}

	/**
	 * Displays the given string at the given line
	 */
	public void displayStringPos(String string, int line) {
		try {
			switch (line) {
			case 1:
				lcd_write((byte) 0x80);
				break;
			case 2:
				lcd_write((byte) 0xC0);
				break;
			case 3:
				lcd_write((byte) 0x94);
				break;
			case 4:
				lcd_write((byte) 0xD4);
				break;
			}

			for (int i = 0; i < string.length(); i++) {
				lcd_write((byte) string.charAt(i), Rs);
			}
		} catch (InterruptedException e) {
			throw new IllegalStateException("Interrupted!");
		}
	}

	/**
	 * Write a single command
	 */
	private void write_cmd(byte cmd) throws InterruptedException {
		_device.write(cmd);
		Thread.sleep(0, 100000);
	}

	/**
	 * Clocks EN to latch command
	 */
	private void lcd_strobe(byte data) throws InterruptedException {
		_device.write((byte) (data | En | (this.backlight ? LCD_BACKLIGHT : LCD_NOBACKLIGHT)));
		Thread.sleep(0, 500000);
		_device.write((byte) ((data & ~En) | (this.backlight ? LCD_BACKLIGHT : LCD_NOBACKLIGHT)));
		Thread.sleep(0, 100000);
	}

	private void lcd_write_four_bits(byte data) throws InterruptedException {
		_device.write((byte) (data | (this.backlight ? LCD_BACKLIGHT : LCD_NOBACKLIGHT)));
		lcd_strobe(data);
	}

	private void lcd_write(byte cmd, byte mode) throws InterruptedException {
		lcd_write_four_bits((byte) (mode | (cmd & 0xF0)));
		lcd_write_four_bits((byte) (mode | ((cmd << 4) & 0xF0)));
	}

	/**
	 * Write a command to the LCD
	 */
	private void lcd_write(byte cmd) throws InterruptedException {
		lcd_write(cmd, (byte) 0);
	}

	/**
	 * Clear the LCD and set cursor to home
	 */
	private void clear() throws InterruptedException {
		lcd_write(LCD_CLEARDISPLAY);
		lcd_write(LCD_RETURNHOME);
	}
}
