/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package adsregs.ws;

import adsregs.util.dbf.DbfResultSet;
import adsregs.util.shp.Header;
import adsregs.util.shp.Polyline;
import adsregs.util.shp.PolyshapeIterator;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.zip.ZipFile;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 *
 * @author aaron.cherney
 */
public class USCenPlaceLookup extends HttpServlet
{
	public ArrayList<CenGroup> m_oGroups = new ArrayList();
	public ArrayList<CenPlace> m_oPlacesById = new ArrayList();
	public static Comparator<CenPlace> PLACEBYLABEL = (CenPlace o1, CenPlace o2) -> o1.m_sLabel.compareTo(o2.m_sLabel);
	public static Comparator<CenPlace> PLACEBYGEOID = (CenPlace o1, CenPlace o2) -> o1.m_nGeoId - o2.m_nGeoId;
	public char[] m_cSearch = new char[3];
	public String[] m_sAbbrevs;
	private String m_sCensusDir;
	
	
	@Override
	public void init(ServletConfig oConfig)
	   throws ServletException
	{
		try
		{
			m_sCensusDir = oConfig.getInitParameter("censusdir");
			File oStateFile = new File(oConfig.getInitParameter("statefile"));
			try (ZipFile oZf = new ZipFile(oStateFile))
			{
				DbfResultSet oDbf = new DbfResultSet(new DataInputStream(oZf.getInputStream(oZf.getEntry(oStateFile.getName().replace(".zip", ".dbf")))));
				ArrayList<Integer> oStates = new ArrayList();
				ArrayList<String> oAbbrev = new ArrayList();
				int nMax = Integer.MIN_VALUE;
				while (oDbf.next())
				{
					int nState = oDbf.getInt("STATEFP");
					if (nState > nMax)
						nMax = nState;
					oStates.add(nState);
					oAbbrev.add(oDbf.getString("STUSPS"));
				}

				m_sAbbrevs = new String[nMax + 1];
				for (int nIndex = 0; nIndex < oStates.size(); nIndex++)
					m_sAbbrevs[oStates.get(nIndex)] = oAbbrev.get(nIndex);
			}

			for (File oFile : new File(m_sCensusDir).listFiles(oFile -> oFile.getName().endsWith(".zip")))
			{
				try (ZipFile oZf = new ZipFile(oFile))
				{
					DbfResultSet oDbf = new DbfResultSet(new DataInputStream(oZf.getInputStream(oZf.getEntry(oFile.getName().replace(".zip", ".dbf")))));
					boolean bCounty = oFile.getName().contains("county");
					while (oDbf.next())
					{
						CenPlace oPlace = new CenPlace(oDbf.getString("NAME") + (bCounty ? " County" : ""), oDbf.getInt("GEOID"), oDbf.getInt("STATEFP"), m_sAbbrevs);
						addPlace(oPlace);
					}
				}
			}
		}
		catch (Exception oEx)
		{
			throw new ServletException(oEx);
		}
	}
	
	private void addPlace(CenPlace oPlace)
	{
		int nIndex = m_cSearch.length;
		String sVal = oPlace.m_sName;
		while (nIndex-- > 0) // create search key
		{
			if (nIndex < sVal.length())
				m_cSearch[nIndex] = Character.toUpperCase(sVal.charAt(nIndex));
			else
				m_cSearch[nIndex] = 0;
		}
		
		nIndex = Collections.binarySearch(m_oGroups, m_cSearch);
		if (nIndex < 0)
		{
			nIndex = ~nIndex;
			m_oGroups.add(nIndex, new CenGroup(m_cSearch));
		}
		
		CenGroup oGroup = m_oGroups.get(nIndex);
		nIndex = Collections.binarySearch(oGroup, oPlace, PLACEBYLABEL);
		if (nIndex < 0)
			oGroup.add(~nIndex, oPlace);
		
		nIndex = Collections.binarySearch(m_oPlacesById, oPlace, PLACEBYGEOID);
		if (nIndex < 0)
			m_oPlacesById.add(~nIndex, oPlace);
	}
	
	
	@Override
	public void doPost(HttpServletRequest oReq, HttpServletResponse oRes)
	   throws IOException, ServletException
	{
		String[] sUriParts = oReq.getRequestURI().split("/");
		if (sUriParts[sUriParts.length - 1].compareTo("geo") == 0)
		{
			doGeo(oReq, oRes);
			return;
		}
		String sReq = oReq.getParameter("lookup");
		ArrayList<String> sResults = new ArrayList();
		char[] cSearch = new char[3];
		for (int nIndex = 0; nIndex < cSearch.length; nIndex++)
		{
			if (nIndex < sReq.length())
				cSearch[nIndex] = Character.toUpperCase(sReq.charAt(nIndex));
			else
				cSearch[nIndex] = 0;
		}
		
		int nIndex = Collections.binarySearch(m_oGroups, cSearch);
		if (nIndex >= 0)
		{

			CenGroup oGroup = m_oGroups.get(nIndex);

			int nReqLen = sReq.length();
			nIndex = 0;
			boolean bFound = true;
			int nCmp = 0;
			while(nIndex < oGroup.size() && bFound)
			{
				CenPlace oPlace = oGroup.get(nIndex++);
				String sVal = oPlace.m_sLabel;
				if (sVal.length() < nReqLen)
					continue;
				for (int i = 3; i < sReq.length(); i++)
				{
					if ((nCmp = Character.toUpperCase(sVal.charAt(i)) - Character.toUpperCase(sReq.charAt(i))) != 0)
					{
						if (nCmp > 0 && sVal.charAt(i) != ',')
							bFound = false;
						break;
					}
				}
				if (nCmp == 0)
					sResults.add(oPlace.m_sLabel);
			}
		}
		
		
		oRes.setContentType("application/json");
		StringBuilder sBuf = new StringBuilder();
		sBuf.append('[');
		if (!sResults.isEmpty())
		{
			for (String sRes : sResults)
				sBuf.append('"').append(sRes).append('"').append(',');

			sBuf.setLength(sBuf.length() - 1);
		}
		sBuf.append(']');
		
		try (PrintWriter oOut = oRes.getWriter())
		{
			oOut.append(sBuf);
		}
	}
	
	public void doGeo(HttpServletRequest oReq, HttpServletResponse oRes)
	   throws IOException, ServletException
	{
		String sPlace = oReq.getParameter("place");
		char[] cSearch = new char[3];
		for (int nIndex = 0; nIndex < cSearch.length; nIndex++)
		{
			if (nIndex < sPlace.length())
				cSearch[nIndex] = Character.toUpperCase(sPlace.charAt(nIndex));
			else
				cSearch[nIndex] = 0;
		}
		
		CenPlace oPlace = null;
		int nIndex = Collections.binarySearch(m_oGroups, cSearch);
		if (nIndex >= 0)
		{
			CenGroup oGroup = m_oGroups.get(nIndex);
			CenPlace oSearch = new CenPlace(sPlace);
			nIndex = Collections.binarySearch(oGroup, oSearch, PLACEBYLABEL);
			if (nIndex >= 0)
				oPlace = oGroup.get(nIndex);
		}
		if (oPlace == null)
		{
			oRes.sendError(HttpServletResponse.SC_NOT_FOUND, "Geometry not found.");
			return;
		}
		
		String sContains;
		if (oPlace.m_sLabel.contains(",")) // county or place
		{
			if (oPlace.m_sLabel.contains("county"))
				sContains = "us_county";
			else
				sContains = String.format("%d_place", oPlace.m_nStateFp);
		}
		else
			sContains = "us_state";
		
		StringBuilder sGeoJson = new StringBuilder("[");
		try
		{
			for (File oFile : new File(m_sCensusDir).listFiles(oFile -> oFile.getName().endsWith(".zip")))
			{
				if (!oFile.getName().contains(sContains))
					continue;
				try (ZipFile oZf = new ZipFile(oFile))
				{
					DbfResultSet oDbf = new DbfResultSet(new DataInputStream(oZf.getInputStream(oZf.getEntry(oFile.getName().replace(".zip", ".dbf")))));
					DataInputStream oShp = new DataInputStream(oZf.getInputStream(oZf.getEntry(oFile.getName().replace(".zip", ".shp"))));
					new Header(oShp); // read through shp header
					PolyshapeIterator oIter = null;
					while (oDbf.next())
					{
						Polyline oLine = new Polyline(oShp, true);
						oIter = oLine.iterator(oIter);
						if (oDbf.getInt("GEOID") != oPlace.m_nGeoId)
							continue;
						
						while (oIter.nextPart())
						{
							sGeoJson.append("[");
							oIter.nextPoint();
							int nPrevX = oIter.getX();
							int nPrevY = oIter.getY();
							sGeoJson.append("[").append(nPrevX).append(",").append(nPrevY).append("]");
							while (oIter.nextPoint())
							{
								int nX = oIter.getX();
								int nY = oIter.getY();
								sGeoJson.append(",[").append(nX - nPrevX).append(",").append(nY - nPrevY).append("]");
								nPrevX = nX;
								nPrevY = nY;
							}
							sGeoJson.append("],");
						}
						sGeoJson.setLength(sGeoJson.length() - 1);
						sGeoJson.append("]");
					}
				}
			}
		}
		catch (Exception oEx)
		{
			throw new ServletException(oEx);
		}
		
		oRes.setContentType("application/json");
		try (PrintWriter oOut = oRes.getWriter())
		{
			oOut.append(sGeoJson);
		}
	}
}
