package edu.handong.csee.isel.patch;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.NoHeadException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.AbstractTreeIterator;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.PathFilter;
import org.eclipse.jgit.errors.AmbiguousObjectException;
import org.eclipse.jgit.errors.IncorrectObjectTypeException;
import org.eclipse.jgit.errors.MissingObjectException;
import org.eclipse.jgit.errors.RevisionSyntaxException;

public class Patch {

	final String[] header = new String[] { "Project", "ShortMessage", "CommitHash", "Date", "Author",
			"Diff" };
	ArrayList<Map<String, Object>> commits = new ArrayList<Map<String, Object>>();
	String directoryPath;
	File directory;
	Git git;
	Repository repository;
	HashMap<String, ArrayList<String>> commitHashs; // <branch, commitHash>
	HashMap<String, ArrayList<String>> allPathList = new HashMap<String, ArrayList<String>>(); // <commitHash, pathList>
	ArrayList<String> branchList = new ArrayList<String>();

	public HashMap<String, ArrayList<String>> getCommitHashs() {
		return commitHashs;
	}

	public void setCommitHashs(String branch) throws RevisionSyntaxException, NoHeadException, MissingObjectException,
			IncorrectObjectTypeException, AmbiguousObjectException, GitAPIException, IOException {
		if (branchList.isEmpty() || !branchList.contains(branch)) {
			System.out.println("branch is not!");
			return;
		}
		ArrayList<String> commitHashList = new ArrayList<String>();

		/**/
		Iterable<RevCommit> logs = git.log().call();
		logs = git.log().add(repository.resolve(branch)) // this decide branch in result
				.call();
		for (RevCommit rev : logs) {

			commitHashList.add(rev.getId().getName());

		}
		this.commitHashs.put(branch, commitHashList);

	}

	public void setBranchList() throws GitAPIException, RevisionSyntaxException, MissingObjectException,
			IncorrectObjectTypeException, AmbiguousObjectException, IOException {
		List<Ref> call = git.branchList().call();
		for (Ref ref : call) {
			branchList.add(ref.getName());
			this.setCommitHashs(ref.getName());
		}
	}

	public ArrayList<String> getBranchList() throws GitAPIException {
		return branchList;
	}

	public HashMap<String, ArrayList<String>> getAllPathList() { // parameter: commitHash
		// TODO Auto-generated method stub
		return allPathList;
	}

	public Patch(String directoryPath) throws IOException, GitAPIException {
		this.commitHashs = new HashMap<String, ArrayList<String>>();
		this.directoryPath = directoryPath;
		this.directory = new File(directoryPath);
		this.git = Git.open(new File(directoryPath + "/.git"));
		this.repository = this.git.getRepository();
		this.commitHashs = new HashMap<String, ArrayList<String>>();
		this.setBranchList();
	}

	public void reset() {
		this.directoryPath = null;
		this.directory = null;
		this.git = null;
		this.repository = null;
		this.branchList = null;
		this.commitHashs = null;
	}

	public void set(String directoryPath) throws IOException, GitAPIException {
		this.commitHashs = new HashMap<String, ArrayList<String>>();
		this.directoryPath = directoryPath;
		this.directory = new File(directoryPath);
		this.git = Git.open(new File(directoryPath + "/.git"));
		this.repository = this.git.getRepository();
		this.commitHashs = new HashMap<String, ArrayList<String>>();
		this.setBranchList();
	}

	public ArrayList<String> getPathList(String commitHash) throws IOException {
		ArrayList<String> pathList = new ArrayList<String>();

		RevWalk walk = new RevWalk(repository);
		ObjectId id = repository.resolve(commitHash);
		RevCommit commit = walk.parseCommit(id);

		RevTree tree = commit.getTree();
		TreeWalk treeWalk = new TreeWalk(repository);
		treeWalk.addTree(tree);
		treeWalk.setRecursive(true);
		while (treeWalk.next()) {
			pathList.add(treeWalk.getPathString());
			// System.out.println(" found: " + treeWalk.getPathString());
		}

		walk.dispose();

		return pathList;
	}

	public void showFileDiff(String oldCommitHash, String newCommitHash)
			throws IOException, GitAPIException {

		ArrayList<String> pathsOfOldCommit = this.getPathList(oldCommitHash);
		ArrayList<String> pathsOfNewCommit = this.getPathList(newCommitHash);

		HashSet<String> paths = new HashSet<String>(pathsOfOldCommit);
		paths.addAll(pathsOfNewCommit);

		ArrayList<String> pathList = new ArrayList<String>(paths);

		for (String filePath : pathList) {
			AbstractTreeIterator oldTreeParser = this.prepareTreeParser(repository, oldCommitHash);
			AbstractTreeIterator newTreeParser = this.prepareTreeParser(repository, newCommitHash);

			List<DiffEntry> diff = git.diff().setOldTree(oldTreeParser).setNewTree(newTreeParser)
					.setPathFilter(PathFilter.create(filePath)).
					call();
			for (DiffEntry entry : diff) {
				System.out.println("Entry: " + entry + ", from: " + entry.getOldId() + ", to: " + entry.getNewId());
				try (DiffFormatter formatter = new DiffFormatter(System.out)) {
					formatter.setRepository(repository);
					formatter.format(entry);
				}
			}
		}
	}

	private AbstractTreeIterator prepareTreeParser(Repository repository, String objectId) throws IOException {
		try (RevWalk walk = new RevWalk(repository)) {
			RevCommit commit = walk.parseCommit(ObjectId.fromString(objectId));
			RevTree tree = walk.parseTree(commit.getTree().getId());

			CanonicalTreeParser treeParser = new CanonicalTreeParser();
			try (ObjectReader reader = repository.newObjectReader()) {
				treeParser.reset(reader, tree.getId());
			}

			walk.dispose();

			return treeParser;
		}
	}

	public String[] makeArrayStringFromArrayListOfString(ArrayList<String> array1) {
		String[] array2 = new String[array1.size()];

		int i = 0;
		for (String content : array1) {
			array2[i++] = content;
		}
		return array2;

	}

	public void analyze(Patch p, String patchesDirectory) throws IOException, GitAPIException {
		
		File folder = new File(patchesDirectory);
		if (!folder.exists()) {
			folder.mkdirs();
		}
		
		Set<Entry<String, ArrayList<String>>> set = this.commitHashs.entrySet();
		Iterator<Entry<String, ArrayList<String>>> it = set.iterator();
		while (it.hasNext()) {
			Map.Entry<String, ArrayList<String>> e = (Map.Entry<String, ArrayList<String>>) it.next();
			String[] hashList = p.makeArrayStringFromArrayListOfString(e.getValue());
//			List<List<DiffEntry>> diffs = null;
			ArrayList<File> diffFiles = null;
			for (int i = 0; i < hashList.length - 1; i++) {
				diffFiles = p.pullDiffs(hashList[i + 1], hashList[i]);
				/* i+1 -> old Hash, i -> new Hash */
				RevWalk walk = new RevWalk(repository);
				ObjectId id = repository.resolve(hashList[i]);
				RevCommit commit = walk.parseCommit(id);
				
				/* 여기에 Csv 작성하는 메소드가 들어와야함. */
				/*"Project", "ShortMessage", "CommitHash", "Date", "Author","Diff" */
				
				String project = "Hbase";
				String shortMessage = commit.getShortMessage();
				String commitHash = hashList[i];
				int date = commit.getCommitTime();
				String Author = commit.getAuthorIdent().getName();
				
				for(DiffEntry entry : diffs) {
					System.out.println();
				}
				
				//diffs;
//				System.out.println(project);
//				System.out.println(shortMessage);
//				System.out.println(commitHash);
//				System.out.println(date);
//				System.out.println(Author);
//				for(List<DiffEntry> diff : diffs) {
//					for(DiffEntry entry : diff) {
//						System.out.println(entry + "** **");
//					}
//					System.out.println("@@@@@@@@@");
//				}
				
			}
		}
		
	}

	public ArrayList<File> pullDiffs(String oldCommitHash, String newCommitHash)
			throws IOException, GitAPIException {
		
		/* fw 부분을 수정해야함 */
		/* temp 폴더를 만들어서 거기에 파일을 만들고, 다시 긁어오는 식으로 해야됨.*/
		OutputStream fw = new FileOutputStream(filename);
		
		RevWalk walk = new RevWalk(repository);
		ObjectId id = repository.resolve(newCommitHash);
		RevCommit commit = walk.parseCommit(id);

		ArrayList<String> pathsOfOldCommit = this.getPathList(oldCommitHash);
		ArrayList<String> pathsOfNewCommit = this.getPathList(newCommitHash);

		HashSet<String> paths = new HashSet<String>(pathsOfOldCommit);
		paths.addAll(pathsOfNewCommit);

		ArrayList<String> pathList = new ArrayList<String>(paths);
//		List<List<DiffEntry>> diffs = null;
		ArrayList<File> diffs = new ArrayList<File>();
		
		for (String filePath : pathList) {
			AbstractTreeIterator oldTreeParser = this.prepareTreeParser(repository, oldCommitHash);
			AbstractTreeIterator newTreeParser = this.prepareTreeParser(repository, newCommitHash);

			List<DiffEntry> diff = git.diff().setOldTree(oldTreeParser).setNewTree(newTreeParser)
					.setPathFilter(PathFilter.create(filePath)).
					call();
			try (DiffFormatter formatter = new DiffFormatter(fw)) {
				formatter.setRepository(repository);
				formatter.format(diff);
			}
			fw.flush();
			fw.close();
		}
		return diffs;
	}
}