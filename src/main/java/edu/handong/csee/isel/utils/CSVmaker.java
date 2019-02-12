package edu.handong.csee.isel.utils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

import edu.handong.csee.isel.parser.Data;

public class CSVmaker {
	File file;
	CSVPrinter printer;

	public CSVmaker(File file, String[] headers) throws IOException {
		this.file = file;

		BufferedWriter writer = Files.newBufferedWriter(Paths.get(file.getAbsolutePath()));
		printer = new CSVPrinter(writer, CSVFormat.DEFAULT.withHeader(headers));
	}

	/**
	 * {"Project","fix-commit","fix-shortMessage","fix-date","fix-author","patch"}
	 * @param data
	 * @throws IOException
	 */
	public void write(Data data) throws IOException {
		printer.printRecord(data.project, data.fix_commit, data.fix_shortMessage, convertCalendar(data.fix_date),
				data.fix_author, data.patch);
		printer.flush();
	}
	
	/**
	 * {"BIShal1", "BIpath", "fixPath", "fixShal1", "numLineBI", "numLinePrefix","content"}
	 * @param bi
	 * @throws IOException
	 */
	public void write(BIChange bi) throws IOException {
		printer.printRecord(bi.BIShal1, bi.BIpath, bi.Fixpath, bi.FixShal1,bi.numLineBIC,bi.numLinePreFix,bi.content);
		printer.flush();
	}

	public String convertCalendar(Date date) {
		SimpleDateFormat formatter = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss", Locale.KOREA);
		String dTime = formatter.format(date);
		return dTime;
	}
}