import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 *  Very simple yet effective hill climb solver.
 *  Works like this:
 *  1) Remove duplicate characters from the wordlist: (AAP -> AP)
 *  2) Remove newly created duplicate words: (POOP and POP are now both POP, POP has score of 2)
 *  3) Generate random alphabet, check score
 *  4) Perform random swap in alphabet, score increases? Good! Score worse? Swap back!
 *  5) No improvements for a while? Start over with a new random alphabet.
 *  
 *  Words are stored in a weird format, we know each character can only exist once.
 *  So we store the index of the characters, for example the word "THE"
 *  ABCDEFGHIJKLMNOPQRSTUVW
 *  ----2--1-----------0--- 
 *  (T has index 0, H has index 1, E has index 2)
 * 
 *  This helps us with the scoring algorithm:
 *
 *  If the alphabet is ABTEH we would check
 *  PTR = 0             // We now look for the word character WORD[PTR] => T
 *  WORD['A'] != -1?    // Does character A exist in word? No
 *  WORD['B'] != -1?    // Does character B exist in word? No
 *  WORD['T'] != -1?    // Does character T exist in word? Yes
 *    PTR == WORD['T']? // Is the word index T the same as PTR? Yes
 *    PTR++             // We now look for the next word character WORD[PTR] => H
 *  WORD['E'] != -1?    // Does character E exist in word? Yes
 *    PTR == WORD['T']? // Is the word index E the same as PTR? No => NO MATCH!
 * 
 *  This can be done in a single loop over alphabet making is very fast, only integer array lookups.
 *  
 *  Best one found: [B, C, P, F, J, H, L, O, W, A, Q, U, M, X, V, I, N, T, G, K, Z, E, R, D, Y, S] 4046
 */
public class HillClimbSolver {

	public static void main(String[] args) throws Exception {
		HillClimbSolver solver = new HillClimbSolver();
		solver.fillWords("english_words.txt");
		solver.solve();
	}

	private void solve() throws Exception {
		
		Random random = new Random();
		char[] alphabetInput = "ABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray();
		for(int i = 0; i<alphabet.length;i++) {
			alphabet[i] = alphabetInput[i];
		}

		int noImprovementForNRounds = 0;
		while(true) {
			int p1 = random.nextInt(26);
			int p2 = random.nextInt(26);
			
			//Swap:
			swap(p1, p2);
			
			if(!checkTopscore(score())) {
				//Swap back:
				swap(p1, p2);
				noImprovementForNRounds++;
			} else {
				noImprovementForNRounds = 0;
			}
			
			//If we don't have an improvement for a long time, do some swaps and start over again:
			if(noImprovementForNRounds >= 100) {
				noImprovementForNRounds = 0;
				localTopscore = 0;
				for(int i = 0;i<10;i++) {
					swap(random.nextInt(26), random.nextInt(26));
				}
			}
			 
		}
	}

	private char swap(int p1, int p2) {
		char s1 = alphabet[p1];
		alphabet[p1] = alphabet[p2];
		alphabet[p2] = s1;
		return s1;
	}
	
	//Overall topscore:
	private int topscore = 0;
	//Local topscore for this branch (to avoid local optima):
	private int localTopscore = 0;

	private boolean checkTopscore(int score) {
		boolean highscore = false;
		if(score > localTopscore) {
			localTopscore = score;
			highscore = true;
		}
		if(score > topscore) {
			topscore = score;
			System.out .println("New topscore: "+ Arrays.toString(alphabet) +" "+topscore);
		}
		return highscore;
	}
	
	/*
	 * Calculate the score of the current alphabet (against all words)
	 */
	private int score() {
		int wordsMatched = 0;
		for(int i = 0; i < wordIndexes.length; i++) {
			if(check(wordIndexes[i])) {
				wordsMatched += wordScore[i];
			}
		}
		return wordsMatched;
	}
	
	private char[] alphabet = new char[26];
	private int[][] wordIndexes;
	private int[] wordScore;
	
	//Check a single word against the alphabet
	public boolean check(int[] input) {
		int ptr = 0;
		for(int i = 0; i<alphabet.length;i++) {
			int index = input[alphabet[i]-65];
			if(index >= 0) {
				if(index != ptr) {
					return false;
				}
				ptr++;
			}
		}
		return true;
	}
	
	/**
	 * This method reads the words from file. It also does two simple mutations.
	 * - Remove duplicate characters
	 * - Remove newly created duplicate words
	 * 
	 * For example:
	 * APP
	 * AP
	 * AAP
	 * 
	 * All of these are basically "AP", and if the alphabet matches AP you get score+3.
	 */
	private void fillWords(String filename) throws IOException {
		Charset charset = Charset.forName("US-ASCII");
		Path file = FileSystems.getDefault().getPath((filename));
		BufferedReader reader = Files.newBufferedReader(file, charset);
	    String line = null;
	    List<String> words = new LinkedList<String>();
	    while ((line = reader.readLine()) != null) {
		    //Remove double characters for each word:
	    	line = line.replaceAll("(.)(\\1+)", "$1");
	    	words.add(line);
	    }
	    //Needs resorting after removing characters
	    Collections.sort(words);

	    //Remove duplicates
	    Map<String, Integer> unduplicated = new HashMap<String, Integer>();
	    for(String word:words) {
	    	Integer score = unduplicated.get(word);
	    	if(score == null) {
	    		score = 0;
	    	}
	    	score++;
	    	unduplicated.put(word, score);
	    }
	    
	    List<String> unduplicatedWords = new ArrayList<String>(unduplicated.keySet());
	    Collections.sort(unduplicatedWords);
	    
	    //Fill arrays and also fill score (some words are now duplicates, count double)
	    wordIndexes = new int[unduplicated.size()][];
	    wordScore = new int[unduplicated.size()];
	    //Turn into array (for speed):
	    for(int ptr = 0;ptr<unduplicated.size();ptr++) {
	    	String word = unduplicatedWords.get(ptr);
	    	//A word now always has a unique index per character, store this for a speedy lookup:
    		wordIndexes[ptr] = new int[26];
	    	for(int i = 0; i<26;i++) {
	    		wordIndexes[ptr][i] = word.indexOf(i+65);
	    	}
	    	wordScore[ptr] = unduplicated.get(word);
	    }
	}
}
