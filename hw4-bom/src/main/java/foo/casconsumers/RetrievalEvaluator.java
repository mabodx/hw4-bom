package foo.casconsumers;

import java.text.DecimalFormat;
import java.util.*;
import java.io.*;
import java.security.KeyStore.Entry;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.*;
import java.util.Scanner;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASException;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.collection.CasConsumer_ImplBase;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSList;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.ResourceProcessException;
import org.apache.uima.util.ProcessTrace;

import foo.typesystems.Document;
import foo.typesystems.Token;
import foo.utils.Utils;
/**
 * I will calculate the similarity score in this class and rank the sentece to compute the MRR value
 * @author mabodx
 *
 */
public class RetrievalEvaluator extends CasConsumer_ImplBase {

	/** query id number **/
	public ArrayList<Integer> qIdList;

	/** query and text relevant values **/
	public ArrayList<Integer> relList;
	public ArrayList<Double> scoreList;
	public ArrayList<String> stringansArrayList;
	
	public ArrayList<HashMap<String, Integer>> Termvc;
	public HashMap<Integer, HashMap<String, Integer>> Query;
	HashMap<String, Integer> stopmp = new HashMap<String, Integer>();
	/**
	 * read in the stopwords
	 * @throws FileNotFoundException
	 */
	public void readstopwords() throws FileNotFoundException
	{
	
		Scanner sc = new Scanner(new File("./src/main/resources/stopwords.txt"));
		int flag = 0;
		while(sc.hasNext())
		{
			String now = (String)sc.next();
			if(now.startsWith("a"))
			{
				flag = 1;
			}
			if(flag==1)
			{
				stopmp.put(now, 1);
			}
		}
	}
	/**
	 *  initialize all the Arraylist and I will use them later 
	 */
	public void initialize() throws ResourceInitializationException {

		qIdList = new ArrayList<Integer>();

		relList = new ArrayList<Integer>();
		scoreList = new ArrayList<Double>();
		stringansArrayList = new ArrayList<String>();
		Termvc = new ArrayList();
		
		try {
			readstopwords();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public String stemming(String str)
	{
		int len = str.length();
		char ch = str.charAt(len-1);
		if(ch == 's')
		{
//			System.out.println(str);
			str= str.substring(0,len-1);
		}
		
		return str;
	}

	/**
	 * TODO :: 1. construct the global word dictionary 2. keep the word
	 * frequency for each sentence
	 * I build a Hashmap list in this step and store the term and its frequency in the Hashmap
	 * @param aCas 
	 * 
	 */
	  
	@Override
	public void processCas(CAS aCas) throws ResourceProcessException {

		JCas jcas;
		try {
			jcas =aCas.getJCas();
		} catch (CASException e) {
			throw new ResourceProcessException(e);
		}

		FSIterator it = jcas.getAnnotationIndex(Document.type).iterator();
	
		if (it.hasNext()) {
			Document doc = (Document) it.next();

			//Make sure that your previous annotators have populated this in CAS
			FSList fsTokenList = doc.getTokenList();
			
			qIdList.add(doc.getQueryID());
			relList.add(doc.getRelevanceValue());
			stringansArrayList.add(doc.getText());
			
			ArrayList<Token>tokenList=Utils.fromFSListToCollection(fsTokenList, Token.class);
			Iterator<Token> iter = tokenList.iterator();
			//Do something useful here
			HashMap<String, Integer> mp = new HashMap<String, Integer>();
			
			while(iter.hasNext())
			{
				
				Token now = (Token)iter.next();
				
				if(!stopmp.containsKey(now.getText()))
				{
//					System.out.println(now);
					String string = now.getText();
					string = stemming(string);
					mp.put(string, now.getFrequency());
				}
			}
			Termvc.add(mp);
			
		}
	}

	/**
	 * TODO 1. Compute Cosine Similarity and rank the retrieved sentences 2.
	 * First I use the the Hashmap list to compute the cosine similarity score of each sentence and rank them
	 * Then Compute the MRR metric 
	 */
	@Override
	public void collectionProcessComplete(ProcessTrace arg0)
			throws ResourceProcessException, IOException {

		super.collectionProcessComplete(arg0);
		
		// TODO :: compute the cosine similarity measure
		Iterator<HashMap<String, Integer>> ittermvector = Termvc.iterator();
		Iterator<Integer> itrel = relList.iterator();
		Iterator<Integer> itqid = qIdList.iterator();
		int index, rel;
		HashMap<String,Integer>tvcHashMap;
		HashMap<String, Integer> query = null;
		HashMap<Integer, Double> scores = new HashMap<Integer, Double>();
		HashMap<Integer, Integer> finalrank = new HashMap<Integer, Integer>();
		HashMap<Integer, String> finals = new HashMap<Integer, String>();
		
		double ans = 0;
		int rank =0 ;
		double score= 0;
		Iterator<String> itstr = stringansArrayList.iterator();
		while(itqid.hasNext())
		{
			String string  = itstr.next();
			index = itqid.next();
			rel = itrel.next();
			tvcHashMap = ittermvector.next();
			double temp = 0;
			
			if(rel!=99)
			{
				temp = computeCosineSimilarity(query, tvcHashMap);
//				temp = computeDiceCoefficient(query, tvcHashMap);
//				temp = computeJaccardCoefficient(query, tvcHashMap);
				
//				System.out.println(temp);
				if(rel==1)
				{
					scores.put(index, temp);
					finalrank.put(index, 1);
					finals.put(index,string);
				}
			}
			else {
				query = tvcHashMap;
			}
			DecimalFormat df2 = (DecimalFormat)DecimalFormat.getInstance();
			df2.setMaximumFractionDigits(4);
			scoreList.add(temp);
			String ret = "Score: "+df2.format(temp)+"\trel="+rel+"\tqid="+index+" "+string;
			System.out.println(ret);
		}
		
		
		itrel = relList.iterator();
		itqid = qIdList.iterator();
		Iterator<Double> itscore= scoreList.iterator();
		
		while(itqid.hasNext())
		{
			index = itqid.next();
			rel = itrel.next();
			double temp = itscore.next();
			if(rel==0)
			{
				double now = scores.get(index);
				if(now<temp)
				{
					int ranking = finalrank.get(index)+1;
					finalrank.put(index, ranking);
				}
			}
		}
		
		// TODO :: compute the rank of retrieved sentences
		for (java.util.Map.Entry<Integer, Integer> entry : finalrank.entrySet())
		{
			int key = entry.getKey();
			int value = entry.getValue();
			String ret = "Score: "+scores.get(key)+"\trank="+value+"\trel=1 qid="+key+" "+finals.get(key);
			System.out.println(ret);
		}
		
		
		// TODO :: compute the metric:: mean reciprocal rank
		double metric_mrr = compute_mrr(finalrank);
		System.out.println(" (MRR) Mean Reciprocal Rank ::" + metric_mrr);
	}

	/**
	 * calculate the cosine_similarity of the document
	 * @param queryVector  
	 * @param docVector
	 * @return cosine_similarity
	 */
	private double computeCosineSimilarity(Map<String, Integer> queryVector, Map<String, Integer> docVector) {
		double cosine_similarity=0.0;
		
		// TODO :: compute cosine similarity between two sentences
		
		double numerator =0 ,denominator1 = 0,denominator2=0;
		
		for (java.util.Map.Entry<String, Integer> entry : queryVector.entrySet())
		{
			String key  = entry.getKey();
			int value = entry.getValue();
			
			denominator1 += value*value;
			if(docVector.containsKey(key))
			{
				
				numerator += value*docVector.get(key);
			}
		}
//		System.out.println("***");
		for (java.util.Map.Entry<String, Integer> entry : docVector.entrySet())
		{
			String key  = entry.getKey();
			int value = entry.getValue();
//			System.out.println(key+" "+value);
			denominator2 += value*value;
		}
//		System.out.println("***");
		cosine_similarity = numerator/(Math.sqrt(denominator1)*Math.sqrt(denominator2));
		return cosine_similarity;
	}
	
	/**
	 * calculate the DiceCoefficient of the document
	 * @param queryVector  
	 * @param docVector
	 * @return
	 */
    private double computeDiceCoefficient(Map<String, Integer> queryVector, Map<String, Integer> docVector) {
            double all=0;
            double diceCoefficient = 0;
            
            for (java.util.Map.Entry<String, Integer> entry : queryVector.entrySet())
    		{
    			String key  = entry.getKey();
    			int value = entry.getValue();
    			if(docVector.containsKey(key))
    			{
    				all +=1;
    			}
    			
    		}
            
           
            double len = queryVector.size()+docVector.size();
    		
            diceCoefficient =  2*all / len;
                           
            return diceCoefficient;
    }

    /**
     * calculate the JaccardCoefficient of the document
     * @param queryVector
     * @param docVector
     * @return
     */
    private double computeJaccardCoefficient(Map<String, Integer> queryVector, Map<String, Integer> docVector) {	
    	  double all=0;
          double jaccard = 0;
          Set<String> uToken = new HashSet<String>();
          for (java.util.Map.Entry<String, Integer> entry : queryVector.entrySet())
  		{
  			String key  = entry.getKey();
  			int value = entry.getValue();
  			if(docVector.containsKey(key))
  			{
  				all +=1;
  			}
  			uToken.add(key);
  		}
          
          for (String key : docVector.keySet()) {
          	uToken.add(key);
          }
          double len = uToken.size();
  		
          jaccard =  all / len;
                         
          return jaccard;
    	
    	
    }
	
	/**
	 * compute the MRR value 
	 * @return mrr
	 */
	private double compute_mrr(HashMap<Integer, Integer> now) {
		double metric_mrr=0.0;
		double num =0 ;
		for (java.util.Map.Entry<Integer, Integer> entry : now.entrySet())
		{
			num ++;
			Integer  value = entry.getValue();
			metric_mrr+=(1.0/(double)value);
		}
		
		// TODO :: compute Mean Reciprocal Rank (MRR) of the text collection
		
		return metric_mrr/num;
	}

}
