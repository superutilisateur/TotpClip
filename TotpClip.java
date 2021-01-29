import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.io.ByteArrayOutputStream;
import java.security.GeneralSecurityException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * Simple "Time-based One-Time Password" generator without dependency (except jvm)
 * I wrote this program to have a unique class that I can deploy quickly and where I want.
 * You just need a java 1.8 vm or higher and your base 32 secret (from google / vip access / ...).
 * 
 * The program takes your secret as a parameter and generates an OPT. 
 * It displays it, gives the remaining validity (2 seconds in advance), 
 * copy it to the clipboard and waits 8 seconds before stopping.
 * 
 * WARNING: in case you use linux, the content of the clipboard is no longer available after the
 * program has stopped (hence the delay of 8s).
 * 
 * I used the work of Gray Watson for the otp generation.
 * (https://github.com/j256/java-two-factor-auth)
 * I rewrote the parsing of the secret (b32 to ByteArray) 
 * to have a shorter (but probably less generic) solution.
 * 
 * *****************************************************************************
 * 
 * Copyright (c) 2021 M.Guillemot
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
 
public class TotpClip {

	private final static String b32alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";

	public static void main(String[] args) throws Exception {

		if (args.length != 1) {
			System.out.println("first and only parameter must be your secret (b32)");
			System.exit(0);
		}
		Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
		String secret = args[0];
		String code = generateCurrentNumber(secret, System.currentTimeMillis() + 2000);
		StringSelection clipselection = new StringSelection(code);
		clipboard.setContents(clipselection, clipselection);

		long diff = 30 - ((System.currentTimeMillis() / 1000) % 30);
		System.out.println("Secret code (clipboard) = " + code + ", change in " + diff + " seconds");
		
		Thread.sleep(8000);
	}

	private static String generateCurrentNumber(String secret, long currentTimeMillis) throws GeneralSecurityException {
		byte[] key = decodeBase32np(secret);
		byte[] data = new byte[8];
		long value = currentTimeMillis / 1000 / 30;
		for (int i = 7; value > 0; i--) {
			data[i] = (byte) (value & 0xFF);
			value >>= 8;
		}
		// encrypt the data with the key and return the SHA1 of it in hex
		SecretKeySpec signKey = new SecretKeySpec(key, "HmacSHA1");
		// if this is expensive, could put in a thread-local
		Mac mac = Mac.getInstance("HmacSHA1");
		mac.init(signKey);
		byte[] hash = mac.doFinal(data);

		// take the 4 least significant bits from the encrypted string as an offset
		int offset = hash[hash.length - 1] & 0xF;

		// We're using a long because Java hasn't got unsigned int.
		long truncatedHash = 0;
		for (int i = offset; i < offset + 4; ++i) {
			truncatedHash <<= 8;
			// get the 4 bytes at the offset
			truncatedHash |= (hash[i] & 0xFF);
		}
		// cut off the top bit
		truncatedHash &= 0x7FFFFFFF;

		// the token is then the last 6 digits in the number
		truncatedHash %= 1000000;

		return String.format("%06d", truncatedHash);
	}

	/**
	 * Decode base 32 string (without padding i.e. with %8 size)to bytes array
	 *
	 * @param inputString
	 * @return the bytes array
	 */
	public static byte[] decodeBase32np(String inputString) {
		char c;
		int value, idx = 0, buffer = 0;
		ByteArrayOutputStream result = new ByteArrayOutputStream();
		inputString = inputString.toUpperCase();
		for (int i = 0; i < inputString.length(); i++) {
			c = inputString.charAt(i);
			value = b32alphabet.indexOf(c);
			if (value == -1)
				throw new IllegalArgumentException("Invalid base-32 character: " + c);
			buffer = buffer << 5;
			buffer = buffer | value;
			idx += 5;
			if (idx >= 8) {
				idx -= 8;
				value = buffer >> idx;
				result.write((byte) value);
			}
		}
		return result.toByteArray();
	}
}
