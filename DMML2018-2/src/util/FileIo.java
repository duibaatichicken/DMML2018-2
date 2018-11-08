package util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author sahil
 * 
 * Utility class for carrying out input/output
 * operations on files.
 *
 */
public class FileIo {

	/************************* *************************/
	
	/**
	 * @description Read a file line-wise into a list.
	 */
	public static List<String> readLinewiseFromFile(String filepath) {
		List<String> ans = new ArrayList<String>();
		BufferedReader br = null;
		FileReader fr = null;
		try {
			fr = new FileReader(filepath);
			br = new BufferedReader(fr);
			String currentLine = "";
			while((currentLine = br.readLine()) != null) {
				ans.add(currentLine);
			}
			System.out.println("Read data from file " + filepath);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			throw(new RuntimeException("File not found!"));
		} catch (IOException e) {
			e.printStackTrace();
			throw(new RuntimeException("I/O exception!"));
		} finally {
			try {
				br.close();
				fr.close();
			} catch (IOException e) {
				e.printStackTrace();
				throw(new RuntimeException("I/O exception!"));
			}
		}
		return ans;
	}
	
	/************************* *************************/
	
	/**
	 * @description Write specified content to file. The boolean
	 * argument decides whether to append the content or overwrite.
	 */
	public static void writeToFile(String content, String filepath, boolean append) {
		BufferedWriter bw = null;
		FileWriter fw = null;
		try {
			fw = new FileWriter(filepath, append);
			bw = new BufferedWriter(fw);
			bw.write(content);
		} catch (IOException e) {
			e.printStackTrace();
			throw(new RuntimeException("I/O exception!"));
		} finally {
			try {
				bw.close();
				fw.close();
			} catch (IOException e) {
				e.printStackTrace();
				throw(new RuntimeException("I/O exception!"));
			}
		}
	}
}
