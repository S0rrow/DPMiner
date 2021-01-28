package edu.handong.csee.isel.metric.collector;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;

import edu.handong.csee.isel.GitFunctions;
import edu.handong.csee.isel.metric.MetricCollector;
import edu.handong.csee.isel.metric.metadata.CommitCollector;
/**
 * 
 * @author 
 *
 */
public class CMetricCollector implements MetricCollector {
	private Git git;
	private Repository repo;
	private String referencePath;
	private String startDate;
	private String endDate;
	private String midDate;
	private boolean developerHistory;
	public static HashMap<String,Integer> tooLongName = new HashMap<>();
	private static int tooLongNameIndex = 0;
	
	List<String> bicList;
	private String outPath;
	private String projectName;
	private String gitURL;
	public GitFunctions gitUtils;
	
	public CMetricCollector(boolean developerHistory, String outPath, String projectName, String gitURL, String startDate, String endDate) throws IOException {
		this.projectName = projectName;
		this.outPath = outPath;
		this.gitURL = gitURL;
		gitUtils = new GitFunctions(projectName, outPath, gitURL);
		
		git = Git.open(gitUtils.getGitDirectory());
		repo = git.getRepository();
		referencePath = outPath + File.separator + projectName +"-reference";
		
		if(startDate == null) this.startDate = "0000-00-00 00:00:00";
		else this.startDate = startDate;
		if(endDate == null) this.endDate = "9999-99-99 99:99:99";
		else this.endDate = endDate;
		
		this.developerHistory = developerHistory;
	}

	/**
	 * @param commitList
	 */
	@Override
	public File collectFrom(List<RevCommit> commitList) {
		File bowArff, cVectorArff;
		
		// 1. collect BOW arff
		BagOfWordsCollector bowCollector = new BagOfWordsCollector();
		bowCollector.setGit(git);
		bowCollector.setRepository(repo);
		bowCollector.setBIC(bicList);
		bowCollector.setCommitList(commitList);
		bowCollector.setReferencePath(referencePath);
		bowCollector.setProjectName(projectName);
		bowCollector.setStartDate(startDate);
		bowCollector.setEndDate(endDate);
		bowCollector.setOutPath(outPath);
		bowCollector.setGitURL(gitURL);
		bowCollector.collect();
//		bowCollector.makeArff(); //TODO: will be removed
		bowArff = bowCollector.getArff();

		// 2. collect Characteristic vector arff
		CharacteristicVectorCollector cVectorCollector = new CharacteristicVectorCollector();
		cVectorCollector.setGit(git);
		cVectorCollector.setRepository(repo);
		cVectorCollector.setBIC(bicList);
		cVectorCollector.setCommitList(commitList);
		cVectorCollector.setReferencePath(referencePath);
		cVectorCollector.setProjectName(projectName);
		cVectorCollector.setStartDate(startDate);
		cVectorCollector.setEndDate(endDate);
		cVectorCollector.collect();
//		cVectorCollector.makeArff();
		cVectorArff = cVectorCollector.getArff();

		// 3. make merged arff between BOW and C-Vector
		File mergedArff = null;

		ArffHelper arffHelper = new ArffHelper();
		arffHelper.setReferencePath(referencePath);
		arffHelper.setProjectName(projectName);
		arffHelper.setOutPath(outPath);
		mergedArff = arffHelper.getMergedBOWArffBetween(bowCollector, cVectorCollector); //arrf 파일이 하나나온다  <<bow-vector arff>>

		// TODO: 4. Meta data, SJ help me
		CommitCollector commitCollector = new CommitCollector(git, referencePath, bicList, projectName, startDate, endDate, developerHistory); //StartDate, strEndDate, test
		if(developerHistory) commitCollector.setMidDate(midDate);
		commitCollector.countCommitMetrics();
		commitCollector.saveResultToCsvFile();
		String arffOutputPath = commitCollector.CSV2ARFF();
		
		File metaArff = new File(arffOutputPath); // TODO: Here your logic: make
																					// metadata arff //reference 안에 있는 arff...!

		ArrayList<String> keyOrder = arffHelper.getKeyOrder();

		// 5. Merge 1,2,3, and return csv

		File resultArff = null;

		try {
			if(!developerHistory)resultArff = arffHelper.makeMergedArff(mergedArff, metaArff, keyOrder);// 여기서 섞는 최종 key-data arff
			else resultArff = arffHelper.makeMergedDeveloperHistoryArff(mergedArff, metaArff, keyOrder, midDate);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return resultArff;
	}

	/**
	 * @param bicList
	 * @return
	 */
	@Override
	public void setBIC(List<String> bicList) {
		this.bicList = bicList;

	}
	
	/**
	 * @param bicList
	 * @return
	 */
	public void setMidDate(String midDate) {
		this.midDate = midDate;
	}
}