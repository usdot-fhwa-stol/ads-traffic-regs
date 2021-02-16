/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package adsregs.ws;

import adsregs.bc.BCBase;
import adsregs.bc.BCBaseList;
import adsregs.bc.BCIndex;
import adsregs.util.Text;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 *
 * @author aaron.cherney
 */
public class MatrixReportServlet extends HttpServlet
{
	private String m_sReportFile;
	private final Timer TIMER = new Timer();
	
	@Override
	public void init(ServletConfig oConfig)
	{
		try
		{
			m_sReportFile = oConfig.getInitParameter("filename");
			Path oFile = Paths.get(m_sReportFile);
			Files.deleteIfExists(oFile);
			if (!Files.exists(oFile))
				createFile();
			Calendar oCal = new GregorianCalendar(BCIndex.UTC);
			oCal.set(Calendar.HOUR, 0);
			oCal.set(Calendar.MINUTE, 0);
			oCal.set(Calendar.SECOND, 0);
			oCal.set(Calendar.MILLISECOND, 0);
			oCal.add(Calendar.DAY_OF_WEEK, 1);
			TIMER.scheduleAtFixedRate(new TimerTask() {@Override public void run(){createFile();}}, oCal.getTime(), 86400000); // set regular daily execution starting at 12:00am UTC of the next day
		}
		catch (IOException oEx)
		{
			oEx.printStackTrace();
		}
	}
	
	
	@Override
	public void destroy()
	{
		TIMER.cancel();
	}
	
	
	@Override
	public void doGet(HttpServletRequest oReq, HttpServletResponse oRes)
	   throws ServletException, IOException
	{
		Path oFile = Paths.get(m_sReportFile);
		if (!Files.exists(oFile))
			throw new ServletException("Missing file");
		
		try (BufferedReader oIn = Files.newBufferedReader(oFile, StandardCharsets.UTF_8);
		     PrintWriter oOut = oRes.getWriter())
		{
			int nByte;
			while ((nByte = oIn.read()) >= 0)
				oOut.write(nByte);
		}
		
		oRes.setContentType("application/json");
		oRes.setDateHeader("last-updated", Files.getLastModifiedTime(oFile).toMillis());
	}
	
	
	private void createFile()
	{
		StringBuilder sFileBuf = new StringBuilder();
		StringBuilder sBuf = new StringBuilder();
		ArrayList<BCBaseList> oJurList = new ArrayList();
		BCIndex.getSituationsByJur(oJurList);
		ArrayList<CharSequence> oCols = new ArrayList();
		ArrayList<String> oRows = new ArrayList();
		HashMap<String, String> oJurLabels = new HashMap();
		for (BCBaseList oList : oJurList) // first collect store unique manuever names and map jurisdiction ids to names
		{
			try
			{
				String sLabel = oList.m_sLabel;
				String sId = new String(oList.m_cId);
				oJurLabels.put(sId, sLabel);
				int nIndex = Collections.binarySearch(oRows, sId);
				if (nIndex < 0)
					oRows.add(~nIndex, sId);
				
				for (BCBase oSituation : oList)
				{
					oSituation.getValue("label", sBuf);
					nIndex = Collections.binarySearch(oCols, sBuf, Text.CHAR_SEQ_COMP);
					if (nIndex < 0)
						oCols.add(~nIndex, sBuf.toString());
				}
			}
			catch (Exception oEx)
			{
				oEx.printStackTrace();
			}
		}
		
		sFileBuf.append('{');
		for (BCBaseList oList : oJurList)
		{
			String sJurId = new String(oList.m_cId);
			sFileBuf.append("\"").append(oJurLabels.get(sJurId)).append("\":[");
			int[] nColIndices = new int[oList.m_oIds.size()];
			int nCount = 0;
			for (BCBase oSituation : oList)
			{
				oSituation.getValue("label", sBuf);
				nColIndices[nCount++] = Collections.binarySearch(oCols, sBuf, Text.CHAR_SEQ_COMP);
			}
			Arrays.sort(nColIndices);
			sFileBuf.append(nColIndices[0]);
			for (int nIndex = 1; nIndex < nColIndices.length; nIndex++)
				sFileBuf.append(',').append(nColIndices[nIndex]);
			sFileBuf.append("],");
		}
		sFileBuf.append("\"cols\":[");
		sFileBuf.append("\"").append(oCols.get(0)).append("\"");
		for (int nIndex = 1; nIndex < oCols.size(); nIndex++)
			sFileBuf.append(",\"").append(oCols.get(nIndex)).append("\"");
		sFileBuf.append("],");
		sFileBuf.append("\"rows\":[");
		sFileBuf.append("\"").append(oJurLabels.get(oRows.get(0))).append("\"");
		for (int nIndex = 1; nIndex < oRows.size(); nIndex++)
			sFileBuf.append(",\"").append(oJurLabels.get(oRows.get(nIndex))).append("\"");
		sFileBuf.append("]}");
		
		try (BufferedWriter oOut = Files.newBufferedWriter(Paths.get(m_sReportFile), StandardCharsets.UTF_8))
		{
			oOut.append(sFileBuf);
		}
		catch (Exception oEx)
		{
			oEx.printStackTrace();
		}
	}
}
