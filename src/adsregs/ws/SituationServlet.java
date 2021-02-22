/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package adsregs.ws;

import adsregs.bc.BCBase;
import adsregs.bc.BCBaseList;
import adsregs.bc.BCIndex;
import adsregs.bc.Situation;
import adsregs.util.Text;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 *
 * @author aaron.cherney
 */
public class SituationServlet extends HttpServlet
{
	@Override
	public void doGet(HttpServletRequest oReq, HttpServletResponse oRes)
	   throws ServletException, IOException
	{
		ArrayList<Situation> oSits = new ArrayList();
		BCIndex.getCurrentSituations(oSits);
		
		StringBuilder sValBuf = new StringBuilder();
		StringBuilder sResBuf = new StringBuilder();
		
		sResBuf.append('{');
		for (Situation oSit : oSits)
		{
			oSit.getValue("currid", sValBuf);
			sResBuf.append('\"').append(sValBuf).append("\":\"");
			oSit.getValue("label", sValBuf);
			sResBuf.append(sValBuf).append("\",");
		}
		sResBuf.setLength(sResBuf.length() - 1); // remove trailing comma
		sResBuf.append('}');
		
		oRes.getWriter().append(sResBuf);
		oRes.setContentType("application/json");
	}
	
	
	@Override
	public void doPost(HttpServletRequest oReq, HttpServletResponse oRes)
	   throws IOException, ServletException
	{
		String[] sJurs = oReq.getParameterValues("id");
		Arrays.sort(sJurs);
		char[][] cJurIds = new char[sJurs.length][];
		for (int nIndex = 0; nIndex < cJurIds.length; nIndex++)
			cJurIds[nIndex] = Text.getCharArray(sJurs[nIndex]);
		ArrayList<BCBaseList> oJurList = new ArrayList();
		BCIndex.getSituationsByJur(oJurList, cJurIds);
		
		StringBuilder sBuf = new StringBuilder();
		sBuf.append('{');
		for (BCBaseList oList : oJurList)
		{
			try
			{
				sBuf.append('\"').append(oList.m_cId).append("\":["); // jurisdiction id is the key 
				for (BCBase oSituation : oList)
				{
					oSituation.appendJson(sBuf);
					sBuf.append(',');
				}
				if (!oList.isEmpty())
					sBuf.setLength(sBuf.length() - 1);
				sBuf.append("],");
			}
			catch (Exception oEx)
			{
				throw new ServletException(oEx);
			}
		}
		if (sBuf.length() > 1)
			sBuf.setLength(sBuf.length() - 1);
		sBuf.append('}');
		
		oRes.getWriter().append(sBuf);
		oRes.setContentType("application/json");
	}
}
