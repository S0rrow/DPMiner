package edu.handong.csee.isel.metric.collector;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.eclipse.core.internal.utils.FileUtil;

import weka.core.Instances;
import weka.core.converters.ArffSaver;
import weka.core.converters.TextDirectoryLoader;
import weka.core.stemmers.SnowballStemmer;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.StringToWordVector;

public class ArffHelper {
	private String projectName;
	private String referencePath;

	private final static String attributeNumPatternStr = "(\\{|,)(\\d+)\\s";
	private final static Pattern attributeNumPattern = Pattern.compile(attributeNumPatternStr);

	public void setProjectName(String projectName) {
		this.projectName = projectName;
	}

	public File getArffFromDirectory(String bowDirectoryPath) {
		File arff = null;

		File bowDirectory = new File(bowDirectoryPath);
		TextDirectoryLoader directoryLoader = new TextDirectoryLoader();

		try {
			directoryLoader.setDirectory(bowDirectory);

			Instances dataRaw = directoryLoader.getDataSet();
			SnowballStemmer stemmer = new SnowballStemmer();
			StringToWordVector filter = new StringToWordVector();
			String option = "-R 1,2 -W 70000 -prune-rate -1.0 -N 0 -stemmer weka.core.stemmers.SnowballStemmer -stopwords-handler weka.core.stopwords.Null -M 1 -tokenizer weka.core.tokenizers.WordTokenizer";
			filter.setOptions(option.split(" "));
			filter.setStemmer(stemmer);
			filter.setInputFormat(dataRaw);

			Instances dataFiltered = Filter.useFilter(dataRaw, filter);

			arff = new File(bowDirectoryPath + File.separator + projectName + ".arff");
			ArffSaver arffSaver = new ArffSaver();
			arffSaver.setInstances(dataFiltered);
			arffSaver.setFile(arff);
			arffSaver.writeBatch();

			return arff;

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return arff;
	}

	public File getMergedBOWArffBetween(BagOfWordsCollector bowCollector,
			CharacteristicVectorCollector cVectorCollector) {

		File cleanDirectory = getCleanDirectory();
		File buggyDirectory = getBuggyDirectory();
		if (cleanDirectory.exists()) {
			cleanDirectory.delete();
		}
		if (buggyDirectory.exists()) {
			buggyDirectory.delete();
		}

		// 1. get merged directory
		HashMap<String, File> bowFileCleanMap = new HashMap<>();
		HashMap<String, File> bowFileBuggyMap = new HashMap<>();
		HashMap<String, File> cVectorCleanFileMap = new HashMap<>();
		HashMap<String, File> cVectorBuggyFileMap = new HashMap<>();

		for (File file : bowCollector.getCleanDirectory().listFiles()) {
			bowFileCleanMap.put(file.getName(), file);
		}

		for (File file : bowCollector.getBuggyDirectory().listFiles()) {
			bowFileBuggyMap.put(file.getName(), file);
		}

		for (File file : cVectorCollector.getCleanDirectory().listFiles()) {
			cVectorCleanFileMap.put(file.getName(), file);
		}

		for (File file : cVectorCollector.getBuggyDirectory().listFiles()) {
			cVectorBuggyFileMap.put(file.getName(), file);
		}

		for (String fileName : cVectorCleanFileMap.keySet()) {

			File bowFile = bowFileCleanMap.get(fileName);
			File cVectorFile = cVectorCleanFileMap.get(fileName);
			String bow = null, cVector = null;
			try {
				bow = FileUtils.readFileToString(bowFile, "UTF-8");
				cVector = FileUtils.readFileToString(cVectorFile, "UTF-8");
			} catch (IOException e) {
				e.printStackTrace();
			}
			String mergedContent = bow + "\n" + cVector;

			String mergedPath = getCleanDirectory() + File.separator + fileName;
			File mergedFile = new File(mergedPath);

			mergedFile.getParentFile().mkdirs();
			try {
				FileUtils.write(mergedFile, mergedContent, "UTF-8");
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		for (String fileName : cVectorBuggyFileMap.keySet()) {

			File bowFile = bowFileBuggyMap.get(fileName);
			File cVectorFile = cVectorBuggyFileMap.get(fileName);
			String bow = null, cVector = null;
			try {

				bow = FileUtils.readFileToString(bowFile, "UTF-8");
				cVector = FileUtils.readFileToString(cVectorFile, "UTF-8");
			} catch (IOException e) {
				e.printStackTrace();
			}
			String mergedContent = bow + "\n" + cVector;

			String mergedPath = getBuggyDirectory() + File.separator + fileName;
			File mergedFile = new File(mergedPath);

			mergedFile.getParentFile().mkdirs();
			try {
				FileUtils.write(mergedFile, mergedContent, "UTF-8");
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		// 2. get merged arff
		File arff = this.getArffFromDirectory(getMergedDirectoryPath());

		return arff;
	}

	public void setReferencePath(String referencePath) {
		this.referencePath = referencePath;

	}

	public String getMergedDirectoryPath() {
		return referencePath + File.separator + projectName + "-merged-bow";
	}

	public File getBuggyDirectory() {

		String directoryPath = getMergedDirectoryPath();
		String path = directoryPath + File.separator + "buggy";
		return new File(path);
	}

	public File getCleanDirectory() {
		String directoryPath = getMergedDirectoryPath();
		String path = directoryPath + File.separator + "clean";
		return new File(path);
	}

	/*
	 * main for metric arff parser
	 */
	public static void main(String[] args) throws IOException {

		String arffPath1 = "/Users/imseongbin/Desktop/lottie-android.arff";
		String arffPath2 = "/Users/imseongbin/Desktop/lottie-android.arff";
		File arff1 = new File(arffPath1);
		File arff2 = new File(arffPath2);

		File arff3 = makeMergedArff(arff1, arff2);

	}

	private static String mergeData(String oldStr, String newStr) {

		StringBuffer mergedBuf = new StringBuffer();

		mergedBuf.append(oldStr.substring(0, oldStr.indexOf('}')));
		mergedBuf.append(",");
		mergedBuf.append(newStr.substring(newStr.indexOf('{') + 1, newStr.indexOf('}')));

		return mergedBuf.toString();
	}

	private static List<String> plusAttributeSize(List<String> dataLineList, int plusSize) {

		ArrayList<String> dataPlusLineList = new ArrayList<>();

		for (String line : dataLineList) {

			String newLine = plusAttributeSize(line, plusSize);
			dataPlusLineList.add(newLine);
		}

		return dataPlusLineList;
	}

	private static String plusAttributeSize(String line, int plusSize) {
		StringBuffer newLineBuf = new StringBuffer();

		Matcher m = attributeNumPattern.matcher(line);
		int lastIndex = 0;

		while (m.find()) {

			int num = Integer.parseInt(m.group(2));
			num += plusSize;

			int start = m.start(2);
			int end = m.end(2);

			newLineBuf.append(line.substring(lastIndex, start));
			newLineBuf.append(num);
			lastIndex = end;
		}
		newLineBuf.append(line.substring(lastIndex, line.length()));

		return newLineBuf.toString();
	}

	private static File makeMergedArff(File arff1, File arff2) throws IOException {
		File newFile = new File(arff1.getParentFile().getAbsolutePath() + File.separator + "merged.arff");

		String content1 = FileUtils.readFileToString(arff1, "UTF-8");
		String content2 = FileUtils.readFileToString(arff2, "UTF-8");

		ArrayList<String> attributeLineList1 = getAttributeLinesFrom(content1);
		ArrayList<String> attributeLineList2 = getAttributeLinesFrom(content2);

		ArrayList<String> mergedAttributeLineList = new ArrayList<>();
		mergedAttributeLineList.addAll(attributeLineList1);
		mergedAttributeLineList.addAll(attributeLineList2);

		ArrayList<String> dataLineList1 = getDataLinesFrom(content1);
		ArrayList<String> dataLineList2 = getDataLinesFrom(content2);

		int plusAttributeNum = attributeLineList1.size();
		List<String> dataPlusLineList = plusAttributeSize(dataLineList2, plusAttributeNum);

		List<String> mergedDataLineList = new ArrayList<>();

		for (int i = 0; i < dataLineList1.size(); i++) {
			String data1 = dataLineList1.get(i);
			String data2 = dataPlusLineList.get(i);
			String mergedData = mergeData(data1, data2);
			mergedDataLineList.add(mergedData);
		}

		StringBuffer newContentBuf = new StringBuffer();

		for (String line : mergedAttributeLineList) {
			newContentBuf.append(line + "\n");
		}

		newContentBuf.append("@data\n");

		for (String line : mergedDataLineList) {
			newContentBuf.append(line + "\n");
		}

		FileUtils.write(newFile, newContentBuf.toString(), "UTF-8");

		return newFile;
	}

	private static ArrayList<String> getDataLinesFrom(String content) {
		ArrayList<String> dataLineList = new ArrayList<>();
		String[] lines = content.split("\n");

		boolean dataPart = false;
		for (String line : lines) {
			if (dataPart) {
				dataLineList.add(line);

			} else if (line.startsWith("@data")) {

				dataPart = true;
			}

		}
		return dataLineList;
	}

	private static ArrayList<String> getAttributeLinesFrom(String content) throws IOException {
		ArrayList<String> attributeLineList = new ArrayList<>();

		String[] lines = content.split("\n");

		for (String line : lines) {
			if (line.startsWith("@attribute")) {
				attributeLineList.add(line);
			}
		}

		return attributeLineList;
	}
}