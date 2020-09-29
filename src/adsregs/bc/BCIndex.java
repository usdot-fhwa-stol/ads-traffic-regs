/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package adsregs.bc;

import adsregs.util.Text;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.SimpleTimeZone;
import java.util.TimeZone;
import java.util.stream.Collectors;

/**
 *
 * @author aaron.cherney
 */
public abstract class BCIndex
{
	private static String BASEDIR;
	private static String INDEXFILE;
	private static final ArrayList<char[]> BOUNDARIES = new ArrayList();
	private static final ArrayList<char[]> JURISDICTIONS = new ArrayList();
	private static final ArrayList<char[]> TITLES = new ArrayList();
	private static final ArrayList<char[]> INSTRUCTIONS = new ArrayList();
	private static final ArrayList<char[]> SITUATIONS = new ArrayList();
	private static final ArrayList<char[]> TCDTYPES = new ArrayList();
	private static final ArrayList<IndexRecord> HEADINDEX = new ArrayList();
	private static final ArrayList<IndexRecord> TAILINDEX = new ArrayList();
	private static final Comparator<IndexRecord> CMP_HEAD = (IndexRecord o1, IndexRecord o2) -> {return Text.compare(o1.m_cHeadId, o2.m_cHeadId);};
	private static final Comparator<IndexRecord> CMP_TAIL = (IndexRecord o1, IndexRecord o2) -> {return Text.compare(o1.m_cTailId, o2.m_cTailId);};
	private static final int RECORDLENGTH = 109; // 43 * 2 for ids, 20 for utc timestamp, 3 for 2 commas and new line
	private static final Object LOCK = new Object();
	private static final String DATEFORMAT = "yyyy-MM-dd'T'HH:mm:ssX";
	public static final TimeZone UTC = new SimpleTimeZone(0, "");
	
	public static Path getPath(CharSequence sId)
	{
		StringBuilder sBuf = new StringBuilder(BASEDIR.length() + 85);
		sBuf.append(BASEDIR).append('/').append(sId.subSequence(0, 11)).append('/').append(sId.subSequence(11, 22)).append('/').append(sId.subSequence(22, 33)).append('/').append(sId).append(".json");
		return Paths.get(sBuf.toString());
	}
	
	
	public static Path getPath(char[] cId)
	{
		StringBuilder sBuf = new StringBuilder(BASEDIR.length() + 85);
		sBuf.append(BASEDIR).append('/');
		for (int nIndex = 0; nIndex < 11; nIndex++)
			sBuf.append(cId[nIndex]);
		sBuf.append('/');
		for (int nIndex = 11; nIndex < 22; nIndex++)
			sBuf.append(cId[nIndex]);
		sBuf.append('/');
		for (int nIndex = 22; nIndex < 33; nIndex++)
			sBuf.append(cId[nIndex]);
		sBuf.append('/');
		for (int nIndex = 0; nIndex < cId.length; nIndex++)
			sBuf.append(cId[nIndex]);
		sBuf.append(".json");
		
		return Paths.get(sBuf.toString());
	}
	
	
	public static void createIndex(String sFile, String sDir)
	   throws IOException
	{
		if (!sDir.endsWith("/"))
			sDir += "/";
		INDEXFILE = sFile;
		BASEDIR = sDir;
		Path oIndexFile = Paths.get(BASEDIR + INDEXFILE);
		Path oBaseDir = Paths.get(BASEDIR);
		int nPos = 0;
		Files.deleteIfExists(oIndexFile);
//		if (Files.exists(oIndexFile)) // index exists, read into memory
//		{
//			Files.delete(oIndex);
//			try (BufferedReader oIn = Files.newBufferedReader(oIndexFile, StandardCharsets.UTF_8))
//			{
//				while (oIn.ready())
//				{
//					IndexRecord oRecord = new IndexRecord(oIn, nPos);
//					nPos += RECORDLENGTH;
//					HEADINDEX.add(oRecord);
//					TAILINDEX.add(oRecord);
//				}
//			}
//			catch (Exception oEx)
//			{
//				oEx.printStackTrace();
//			}
//		}
//		else // create index from scratch
//		{
			List<Path> oBlockChainFiles = Files.walk(oBaseDir).filter((oPath) -> {return Files.isRegularFile(oPath) && oPath.toString().endsWith(".json");}).collect(Collectors.toList());
			ArrayList<char[]> oPrevs = new ArrayList();
			ArrayList<char[]> oAllIds = new ArrayList();
			ArrayList<char[]> oTails = new ArrayList();
			StringBuilder sBuf = new StringBuilder();
			for (Path oFile : oBlockChainFiles) // first open each file to determine the type for each block chain and keep track of previous ids so we can determine which blocks are the tail of their chain
			{
				try
				{
					BCBase oTemp = BCFactory.createFromJson(oFile);
					oTemp.getValue("specid", sBuf);
					String sClass = BCFactory.getClass(sBuf.toString());
					if (sClass == null)
						throw new Exception(String.format("Invalid spec for file: %s", oFile.toString()));
					
					ArrayList<char[]> oTypeList = null;
					if (sClass.compareTo("adsregs.bc.Boundary") == 0) // determine type
						oTypeList = BOUNDARIES;
					else if (sClass.compareTo("adsregs.bc.Jurisdiction") == 0)
						oTypeList = JURISDICTIONS;
					else if (sClass.compareTo("adsregs.bc.Title") == 0)
						oTypeList = TITLES;
					else if (sClass.compareTo("adsregs.bc.Instruction") == 0)
						oTypeList = INSTRUCTIONS;
					else if (sClass.compareTo("adsregs.bc.Situation") == 0)
						oTypeList = SITUATIONS;
					else if (sClass.compareTo("adsregs.bc.TcdType") == 0)
						oTypeList = TCDTYPES;
					
					oTemp.getValue("currid", sBuf);
					char[] cCurrId = Text.getCharArray(sBuf);
					int nIndex = Collections.binarySearch(oTypeList, cCurrId, Text.CHAR_ARRAY_COMP); // add to the correct list in order
					if (nIndex < 0)
						oTypeList.add(~nIndex, cCurrId);
					
					nIndex = Collections.binarySearch(oAllIds, cCurrId, Text.CHAR_ARRAY_COMP); // keep track of all ids
					if (nIndex < 0)
						oAllIds.add(~nIndex, cCurrId);
					
					if (Integer.parseInt(oTemp.getValue("seq")) != 0) // don't add the previd of genesis blocks
					{
						oTemp.getValue("previd", sBuf);
						char[] cPrevId = Text.getCharArray(sBuf);
						nIndex = Collections.binarySearch(oPrevs, cPrevId, Text.CHAR_ARRAY_COMP);
						if (nIndex < 0)
							oPrevs.add(~nIndex, cPrevId);
					}
				}
				catch (Exception oEx)
				{
					oEx.printStackTrace();
				}
			}
			for (char[] cId : oAllIds) // determine tails of each block by finding ids that are not in the previous id list
			{
				int nIndex = Collections.binarySearch(oPrevs, cId, Text.CHAR_ARRAY_COMP);
				if (nIndex < 0)
					oTails.add(cId);
			}

			for (char[] cTail : oTails)
			{
				try
				{
					BCBase oTemp = BCFactory.createFromJson(getPath(cTail));
					oTemp.getValue("created-dt", sBuf);
					char[] cTimestamp = Text.getCharArray(sBuf);
					oTemp.getValue("previd", sBuf);
					int nSeq = Integer.parseInt(oTemp.getValue("seq"));
					
					char[] cHeadId;
					if (nSeq == 0)
					{
						cHeadId = new char[cTail.length];
						System.arraycopy(cTail, 0, cHeadId, 0, cTail.length);
					}
					else
					{
						cHeadId = Text.getCharArray(sBuf);
					}
					for (int nIndex = 0; nIndex < nSeq - 1; nIndex++) // don't need to open the genesis block because it is the head
					{
						oTemp = BCFactory.createFromJson(getPath(cHeadId));
						oTemp.getValue("previd", sBuf);
						Text.fillCharArray(sBuf, cHeadId);
					}
					
					IndexRecord oRecord = new IndexRecord(cHeadId, cTail, cTimestamp, nPos);
					nPos += RECORDLENGTH;
					HEADINDEX.add(oRecord);
					TAILINDEX.add(oRecord);
				}
				catch (Exception oEx)
				{
					oEx.printStackTrace();
				}
			}
			try (BufferedWriter oOut = Files.newBufferedWriter(Paths.get(BASEDIR + INDEXFILE), StandardCharsets.UTF_8))
			{
				for (IndexRecord oRecord : HEADINDEX)
					oRecord.write(oOut);
			}
			
			try (BufferedWriter oOut = Files.newBufferedWriter(Paths.get(BASEDIR + "types.csv"), StandardCharsets.UTF_8))
			{
				for (char[] cId : BOUNDARIES)
				{
					oOut.write(cId);
					oOut.write(',');
					oOut.write("boundary\n");
				}
				
				for (char[] cId : JURISDICTIONS)
				{
					oOut.write(cId);
					oOut.write(',');
					oOut.write("jurisdiction\n");
				}
				
				for (char[] cId : TITLES)
				{
					oOut.write(cId);
					oOut.write(',');
					oOut.write("title\n");
				}
				
				for (char[] cId : INSTRUCTIONS)
				{
					oOut.write(cId);
					oOut.write(',');
					oOut.write("instruction\n");
				}
				
				for (char[] cId : SITUATIONS)
				{
					oOut.write(cId);
					oOut.write(',');
					oOut.write("situation\n");
				}
				
				for (char[] cId : TCDTYPES)
				{
					oOut.write(cId);
					oOut.write(',');
					oOut.write("tcdtype\n");
				}
			}
//		}
		Collections.sort(HEADINDEX, CMP_HEAD);
		Collections.sort(TAILINDEX, CMP_TAIL);
	}
	
	
	public static boolean updateTail(CharSequence sPrevTail, CharSequence sNewTail, long lTime)
	   throws Exception
	{
		char[] cPrev = Text.getCharArray(sPrevTail);
		char[] cCurr = Text.getCharArray(sNewTail);
		SimpleDateFormat oSdf = new SimpleDateFormat(DATEFORMAT);
		oSdf.setTimeZone(UTC);
		String sTimestamp = oSdf.format(lTime);
		char[] cTimestamp = Text.getCharArray(sTimestamp);
		IndexRecord oSearch = new IndexRecord();
		oSearch.m_cTailId = cPrev;
		synchronized (LOCK)
		{
			int nPrevIndex = Collections.binarySearch(TAILINDEX, oSearch, CMP_TAIL);
			if (nPrevIndex < 0)
				throw new Exception("Previous tail does not exist.");
			
			IndexRecord oRecord = TAILINDEX.remove(nPrevIndex); // remove from the list to reinsert in the correct position
			oSearch.m_cTailId = cCurr;
			int nCurrIndex = Collections.binarySearch(TAILINDEX, oSearch, CMP_TAIL);
			char[] cRecordId = oRecord.m_cTailId;
			if (nCurrIndex >= 0) // cannot have the new tail already be an id in the list
			{
				TAILINDEX.add(nPrevIndex, oRecord); // so reinsert the record removed
				throw new Exception("Current tail already exists.");
			}
			
			System.arraycopy(cCurr, 0, cRecordId, 0, cRecordId.length); // update in memory id
			TAILINDEX.add(~nCurrIndex, oRecord); // reinsert record in correct position
			
			try (RandomAccessFile oRaf = new RandomAccessFile(new File(BASEDIR + INDEXFILE), "rw"))
			{
				oRaf.seek(oRecord.m_nFilePos);
				for (int nIndex = 0; nIndex < cRecordId.length; nIndex++)
					oRaf.write(cRecordId[nIndex]);
				oRaf.write(',');
				for (int nIndex = 0; nIndex < cTimestamp.length; nIndex++)
					oRaf.write(cTimestamp[nIndex]);
			}
		}
		
		return true;
	}
	
	
	public static void getCurrentJurisdictions(ArrayList<Jurisdiction> oJurisdiction)
	{
		for (IndexRecord oRecord : TAILINDEX)
		{
			char[] cId = null;
			synchronized (JURISDICTIONS)
			{
				int nIndex = Collections.binarySearch(JURISDICTIONS, oRecord.m_cTailId, Text.CHAR_ARRAY_COMP);
				if (nIndex >= 0)
					cId = JURISDICTIONS.get(nIndex);
			}
			
			if (cId == null)
				continue;
			
			try
			{
				oJurisdiction.add((Jurisdiction)BCFactory.createFromJson(getPath(cId)));
			}
			catch (Exception oEx)
			{
				oEx.printStackTrace();
			}
		}
	}
	
	
	public static void getCurrentBoundaries(ArrayList<Boundary> oBoundaries)
	{
		for (IndexRecord oRecord : TAILINDEX)
		{
			char[] cId = null;
			synchronized (BOUNDARIES)
			{
				int nIndex = Collections.binarySearch(BOUNDARIES, oRecord.m_cTailId, Text.CHAR_ARRAY_COMP);
				if (nIndex >= 0)
					cId = BOUNDARIES.get(nIndex);
			}
			
			if (cId == null)
				continue;
			
			try
			{
				oBoundaries.add((Boundary)BCFactory.createFromJson(getPath(cId)));
			}
			catch (Exception oEx)
			{
				oEx.printStackTrace();
			}
		}
	}
	
	
		public static void getCurrentBoundaries(ArrayList<Boundary> oBoundaries, String[] sJurs)
	{
		for (IndexRecord oRecord : TAILINDEX)
		{
			char[] cId = null;
			synchronized (BOUNDARIES)
			{
				int nIndex = Collections.binarySearch(BOUNDARIES, oRecord.m_cTailId, Text.CHAR_ARRAY_COMP);
				if (nIndex >= 0)
					cId = BOUNDARIES.get(nIndex);
			}
			
			if (cId == null)
				continue;
			
			try
			{
				Boundary oBoundary = (Boundary)BCFactory.createFromJson(getPath(cId));
				String sJur = oBoundary.getValue("jurisdiction");
				if (Arrays.binarySearch(sJurs, sJur) >= 0)
					oBoundaries.add(oBoundary);
			}
			catch (Exception oEx)
			{
				oEx.printStackTrace();
			}
		}
	}
	
	
	public static void getCurrentSituations(ArrayList<Situation> oSituations)
	{
		for (IndexRecord oRecord : TAILINDEX)
		{
			char[] cId = null;
			synchronized (SITUATIONS)
			{
				int nIndex = Collections.binarySearch(SITUATIONS, oRecord.m_cTailId, Text.CHAR_ARRAY_COMP);
				if (nIndex >= 0)
					cId = SITUATIONS.get(nIndex);
			}
			
			if (cId == null)
				continue;
			try
			{
				oSituations.add((Situation)BCFactory.createFromJson(getPath(cId)));
			}
			catch (Exception oEx)
			{
				oEx.printStackTrace();
			}
		}
	}
	
	
	public static void getCurrentTitles(ArrayList<Title> oTitles)
	{
		for (IndexRecord oRecord : TAILINDEX)
		{
			char[] cId = null;
			synchronized (TITLES)
			{
				int nIndex = Collections.binarySearch(TITLES, oRecord.m_cTailId, Text.CHAR_ARRAY_COMP);
				if (nIndex >= 0)
					cId = TITLES.get(nIndex);
			}
			
			if (cId == null)
				continue;
			try
			{
				oTitles.add((Title)BCFactory.createFromJson(getPath(cId)));
			}
			catch (Exception oEx)
			{
				oEx.printStackTrace();
			}
		}
	}
	
	
	public static void getCurrentTitles(ArrayList<Title> oTitles, char[][] cJurs, ArrayList<char[]> oTitleIds)
	{
		StringBuilder sBuf = BCFactory.getIdBuffer();
		for (IndexRecord oRecord : TAILINDEX)
		{
			char[] cId = null;
			synchronized (TITLES)
			{
				int nIndex = Collections.binarySearch(TITLES, oRecord.m_cTailId, Text.CHAR_ARRAY_COMP);
				if (nIndex >= 0)
					cId = TITLES.get(nIndex);
			}
			
			if (cId == null)
				continue;
			try
			{
				Title oTitle = (Title)BCFactory.createFromJson(getPath(cId));
				oTitle.getValue("jurisdiction", sBuf);
				char[] cJur = Text.getCharArray(sBuf);
				if (Arrays.binarySearch(cJurs, cJur, Text.CHAR_ARRAY_COMP) >= 0)
				{
					oTitles.add(oTitle);
					int nIndex = Collections.binarySearch(oTitleIds, cId, Text.CHAR_ARRAY_COMP);
					if (nIndex < 0)
						oTitleIds.add(~nIndex, cId);
				}
			}
			catch (Exception oEx)
			{
				oEx.printStackTrace();
			}
		}
	}
	
	public static void getCurrentInstructions(ArrayList<Instruction> oInstructions)
	{
		for (IndexRecord oRecord : TAILINDEX)
		{
			char[] cId = null;
			synchronized (INSTRUCTIONS)
			{
				int nIndex = Collections.binarySearch(INSTRUCTIONS, oRecord.m_cTailId, Text.CHAR_ARRAY_COMP);
				if (nIndex >= 0)
					cId = INSTRUCTIONS.get(nIndex);
			}
			
			if (cId == null)
				continue;
			try
			{
				oInstructions.add((Instruction)BCFactory.createFromJson(getPath(cId)));
			}
			catch (Exception oEx)
			{
				oEx.printStackTrace();
			}
		}
	}
	
	
	public static void getCurrentTcdTypes(ArrayList<TcdType> oTcdTypes)
	{
		for (IndexRecord oRecord : TAILINDEX)
		{
			char[] cId = null;
			synchronized (TCDTYPES)
			{
				int nIndex = Collections.binarySearch(TCDTYPES, oRecord.m_cTailId, Text.CHAR_ARRAY_COMP);
				if (nIndex >= 0)
					cId = TCDTYPES.get(nIndex);
			}
			
			if (cId == null)
				continue;
			try
			{
				oTcdTypes.add((TcdType)BCFactory.createFromJson(getPath(cId)));
			}
			catch (Exception oEx)
			{
				oEx.printStackTrace();
			}
		}
	}
	
	
	public static void getCurrentTcdTypes(ArrayList<TcdType> oTcdTypes, char[][] cJurs)
	{
		StringBuilder sBuf = BCFactory.getIdBuffer();
		for (IndexRecord oRecord : TAILINDEX)
		{
			char[] cId = null;
			synchronized (TCDTYPES)
			{
				int nIndex = Collections.binarySearch(TCDTYPES, oRecord.m_cTailId, Text.CHAR_ARRAY_COMP);
				if (nIndex >= 0)
					cId = TCDTYPES.get(nIndex);
			}
			
			if (cId == null)
				continue;
			try
			{
				TcdType oTcdType = (TcdType)BCFactory.createFromJson(getPath(cId));
				oTcdType.getValue("jurisdiction", sBuf);
				char[] cJur = Text.getCharArray(sBuf);
				if (Arrays.binarySearch(cJurs, cJur, Text.CHAR_ARRAY_COMP) >= 0)
				{
					oTcdTypes.add(oTcdType);
				}
			}
			catch (Exception oEx)
			{
				oEx.printStackTrace();
			}
		}
	}
	
	public static void getCurrentInstructions(ArrayList<Instruction> oInstructions, ArrayList<char[]> oTitleIds)
	{
		StringBuilder sBuf = BCFactory.getIdBuffer();
		for (IndexRecord oRecord : TAILINDEX)
		{
			char[] cId = null;
			synchronized (INSTRUCTIONS)
			{
				int nIndex = Collections.binarySearch(INSTRUCTIONS, oRecord.m_cTailId, Text.CHAR_ARRAY_COMP);
				if (nIndex >= 0)
					cId = INSTRUCTIONS.get(nIndex);
			}
			
			if (cId == null)
				continue;
			try
			{
				Instruction oInstruction = (Instruction)BCFactory.createFromJson(getPath(cId));
				oInstruction.getValue("title", sBuf);
				char[] cTitleId = Text.getCharArray(sBuf);
				if (Collections.binarySearch(oTitleIds, cTitleId, Text.CHAR_ARRAY_COMP) >= 0)
					oInstructions.add(oInstruction);
			}
			catch (Exception oEx)
			{
				oEx.printStackTrace();
			}
		}
	}

	public static void getSituationsByJur(ArrayList<BCBaseList> oJurList)
	{
		ArrayList<Title> oTitles = new ArrayList();
		ArrayList<Instruction> oInstructions = new ArrayList();
		getCurrentTitles(oTitles);
		getCurrentInstructions(oInstructions);
		BCBaseList oSearch = new BCBaseList();
		StringBuilder sBuf = BCFactory.getIdBuffer();
		StringBuilder sTitleBuf = BCFactory.getIdBuffer();
		for (Title oTitle : oTitles)
		{
			oTitle.getValue("jurisdiction", sBuf);
			char[] cJur = Text.getCharArray(sBuf);
			System.arraycopy(cJur, 0, oSearch.m_cId, 0, cJur.length);
			int nIndex = Collections.binarySearch(oJurList, oSearch);
			if (nIndex < 0)
			{
				try
				{
					nIndex = ~nIndex;
					Jurisdiction oJur = (Jurisdiction)BCFactory.createFromJson(BCIndex.getPath(cJur));
					oJurList.add(nIndex, new BCBaseList(cJur, oJur.getValue("label")));
				}
				catch (Exception oEx)
				{
					oEx.printStackTrace();
				}
			}
			
			BCBaseList oList = oJurList.get(nIndex);
			oTitle.getValue("currid", sTitleBuf);
			for (Instruction oInstruction : oInstructions)
			{
				oInstruction.getValue("title", sBuf);
				if (Text.compare(sBuf, sTitleBuf) == 0)
				{
					for (char[] cSituationId : oInstruction.m_cSituations)
					{
						nIndex = Collections.binarySearch(oList.m_oIds, cSituationId, Text.CHAR_ARRAY_COMP);
						if (nIndex < 0)
						{
							try
							{
								Situation oManeuver = (Situation)BCFactory.createFromJson(getPath(cSituationId));
								oList.add(~nIndex, oManeuver);
								oList.m_oIds.add(~nIndex, cSituationId);
							}
							catch (Exception oEx)
							{
								oEx.printStackTrace();
							}
						}
					}
				}
			}
			   
		}
	}
	
	
	public static void getInstructionsByJur(ArrayList<BCBaseList> oTitleList, char[][] cJurIds)
	{
		ArrayList<Title> oTitles = new ArrayList();
		ArrayList<Instruction> oInstructions = new ArrayList();
		ArrayList<char[]> oTitleIds = new ArrayList();
		getCurrentTitles(oTitles, cJurIds, oTitleIds);
		StringBuilder sBuf = BCFactory.getIdBuffer();
		StringBuilder sTitleBuf = BCFactory.getIdBuffer();
		getCurrentInstructions(oInstructions, oTitleIds);
		
		BCBaseList oSearch = new BCBaseList();
		for (Title oTitle : oTitles)
		{
			oTitle.getValue("currid", sTitleBuf);
			char[] cTitle = Text.getCharArray(sTitleBuf);
			System.arraycopy(cTitle, 0, oSearch.m_cId, 0, cTitle.length);
			int nIndex = Collections.binarySearch(oTitleList, oSearch);
			if (nIndex < 0)
			{
				nIndex = ~nIndex;
				oTitleList.add(nIndex, new BCBaseList(cTitle, oTitle.getValue("label")));
			}
			
			BCBaseList oList = oTitleList.get(nIndex);
			
			for (Instruction oInstruction : oInstructions)
			{
				oInstruction.getValue("title", sBuf);
				if (Text.compare(sBuf, sTitleBuf) == 0)
				{
					oInstruction.getValue("currid", sBuf);
					char[] cInstruction = Text.getCharArray(sBuf);
					nIndex = Collections.binarySearch(oList.m_oIds, cInstruction, Text.CHAR_ARRAY_COMP);
					if (nIndex < 0)
					{
						oList.add(~nIndex, oInstruction);
						oList.m_oIds.add(~nIndex, cInstruction);
					}
				}
			}
		}
	}
	
	
	public static void getSituationsByJur(ArrayList<BCBaseList> oJurList, char[][] cJurIds)
	{
		ArrayList<Title> oTitles = new ArrayList();
		ArrayList<Instruction> oInstructions = new ArrayList();
		ArrayList<char[]> oTitleIds = new ArrayList();
		getCurrentTitles(oTitles, cJurIds, oTitleIds);
		StringBuilder sBuf = BCFactory.getIdBuffer();
		StringBuilder sTitleBuf = BCFactory.getIdBuffer();
		getCurrentInstructions(oInstructions, oTitleIds);
		
		BCBaseList oSearch = new BCBaseList();
		for (Title oTitle : oTitles)
		{
			oTitle.getValue("jurisdiction", sBuf);
			char[] cJur = Text.getCharArray(sBuf);
			System.arraycopy(cJur, 0, oSearch.m_cId, 0, cJur.length);
			int nIndex = Collections.binarySearch(oJurList, oSearch);
			try
			{
				nIndex = ~nIndex;
				Jurisdiction oJur = (Jurisdiction)BCFactory.createFromJson(BCIndex.getPath(cJur));
				oJurList.add(nIndex, new BCBaseList(cJur, oJur.getValue("label")));
			}
			catch (Exception oEx)
			{
				oEx.printStackTrace();
			}
			
			BCBaseList oList = oJurList.get(nIndex);
			oTitle.getValue("currid", sTitleBuf);
			for (Instruction oInstruction : oInstructions)
			{
				oInstruction.getValue("title", sBuf);
				if (Text.compare(sBuf, sTitleBuf) == 0)
				{
					for (char[] cSiuationId : oInstruction.m_cSituations)
					{
						nIndex = Collections.binarySearch(oList.m_oIds, cSiuationId, Text.CHAR_ARRAY_COMP);
						if (nIndex < 0)
						{
							try
							{
								Situation oSituation = (Situation)BCFactory.createFromJson(getPath(cSiuationId));
								oList.add(~nIndex, oSituation);
								oList.m_oIds.add(~nIndex, cSiuationId);
							}
							catch (Exception oEx)
							{
								oEx.printStackTrace();
							}
						}
					}
				}
			}  
		}
	}
	
	
	public static void getTcdTypesByJur(ArrayList<BCBaseList> oJurList, char[][] cJurIds)
	{
		ArrayList<TcdType> oTcdTypes = new ArrayList();
		getCurrentTcdTypes(oTcdTypes, cJurIds);
		StringBuilder sBuf = BCFactory.getIdBuffer();
		BCBaseList oSearch = new BCBaseList();
		
		for (TcdType oTcdType : oTcdTypes)
		{
			oTcdType.getValue("jurisdiction", sBuf);
			char[] cJur = Text.getCharArray(sBuf);
			System.arraycopy(cJur, 0, oSearch.m_cId, 0, cJur.length);
			int nIndex = Collections.binarySearch(oJurList, oSearch);
			if (nIndex < 0)
			{
				try
				{
					nIndex = ~nIndex;
					Jurisdiction oJur = (Jurisdiction)BCFactory.createFromJson(BCIndex.getPath(cJur));
					oJurList.add(nIndex, new BCBaseList(cJur, oJur.getValue("label")));

				}
				catch (Exception oEx)
				{
					oEx.printStackTrace();
				}
			}
			
			oJurList.get(nIndex).add(oTcdType);
		}
	}
	
	
	public static BCBase getBlockById(CharSequence sId)
	{
		try
		{
			return BCFactory.createFromJson(BCIndex.getPath(Text.getCharArray(sId)));
		}
		catch (Exception oEx)
		{
			return null;
		}
	}
	
	private static class IndexRecord
	{
		char[] m_cHeadId;
		char[] m_cTailId;
		char[] m_cTimestamp;
		int m_nFilePos;
		
		IndexRecord()
		{
		}
		
		IndexRecord(char[] cHeadId, char[] cTailId, char[] cTimestamp, int nFilePos)
		{
			m_cHeadId = cHeadId;
			m_cTailId = cTailId;
			m_cTimestamp = cTimestamp;
			m_nFilePos = nFilePos;
		}
		
		IndexRecord(Reader oIn, int nFilePos)
		   throws IOException
		{
			m_cHeadId = new char[43];
			m_cTailId = new char[43];
			m_cTimestamp = new char[20];
			oIn.read(m_cTailId);
			oIn.skip(1); // skip comma
			oIn.read(m_cTimestamp);
			oIn.skip(1); // skip comma
			oIn.read(m_cHeadId);
			oIn.skip(1); // skip newline
			m_nFilePos = nFilePos;
		}
		
		void write(Writer oOut)
		   throws IOException
		{
			oOut.write(m_cTailId);
			oOut.append(',');
			oOut.write(m_cTimestamp);
			oOut.append(',');
			oOut.write(m_cHeadId);
			oOut.append('\n');
		}
	}
}
