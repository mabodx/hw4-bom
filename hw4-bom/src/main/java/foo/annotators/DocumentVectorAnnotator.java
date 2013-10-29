package foo.annotators;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.axis.types.Entities;
import org.apache.uima.analysis_component.JCasAnnotator_ImplBase;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.internal.util.TextTokenizer;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.EmptyFSList;
import org.apache.uima.jcas.cas.FSArray;
import org.apache.uima.jcas.cas.FSList;
import org.apache.uima.jcas.cas.IntegerArray;
import org.apache.uima.jcas.cas.StringArray;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.cas.FSIterator;

import foo.typesystems.Document;
import foo.typesystems.Token;
import foo.utils.Utils;
/**
 * This file I get the type system information from the instructor. 
 * And the reader is quite helpful. Extract the information from the input and put it in the CAS
 * 
 * @author mabodx
 *	
 */
public class DocumentVectorAnnotator extends JCasAnnotator_ImplBase {

	@Override
	
	public void process(JCas jcas) throws AnalysisEngineProcessException {

		FSIterator<Annotation> iter = jcas.getAnnotationIndex().iterator();
		if (iter.isValid()) {
			iter.moveToNext();
			Document doc = (Document) iter.get();
			createTermFreqVector(jcas, doc);
		}

	}
	/**
	 * 
	 * @param jcas 
	 * @param doc
	 * This file I use the input sentence and tokenize the sentence into terms.
	 */

	private void createTermFreqVector(JCas jcas, Document doc) {
		HashMap<String, Integer>mp = new HashMap<String, Integer>();
		
		String docText = doc.getText();
		TextTokenizer tokenizer= new TextTokenizer(docText);
		 tokenizer.addSeparators(",");
		 tokenizer.addSeparators(" ");
		 tokenizer.addSeparators(".!?");
		 tokenizer.setShowWhitespace(false);
		//TO DO: construct a vector of tokens and update the tokenList in CAS
		String[] tokenStrings = docText.split("\\s+");
		
		 if(tokenStrings.length == 0){
             doc.setTokenList(new EmptyFSList(jcas));
             return;
		 }
		 
//		 System.out.println(docText);
		 while(tokenizer.hasNext())
		 {
			String now = tokenizer.nextToken().toLowerCase();
			if(now.startsWith(" "))
				continue;
//			System.out.println(now+" "+now.length());
			 if(now.length()>0)
			 {
				 int temp = 0;
				 if(mp.containsKey(now))
				 {
					 temp = mp.get(now);
				 }
				 mp.put(now, temp+1);
			 }
		 }
		 ArrayList<Token> tokenList = new ArrayList<Token>();
		 Iterator it = mp.entrySet().iterator();
		 while (it.hasNext()) {
			 
		    Map.Entry pairs = (Map.Entry)it.next();
		    String keyString = (String) pairs.getKey();
		    int value = (Integer) pairs.getValue();
		    Token token = new Token(jcas);
		    token.setText(keyString);
		    token.setFrequency(value);
		     
		    tokenList.add(token);
		    
		 }
		 doc.setTokenList(Utils.fromCollectionToFSList(jcas, tokenList));
	}
}
