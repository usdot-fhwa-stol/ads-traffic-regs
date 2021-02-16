/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package adsregs.ws;

import adsregs.bc.BCIndex;
import adsregs.bc.Boundary;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 *
 * @author aaron.cherney
 */
public class BoundaryServlet extends HttpServlet
{
	@Override
	public void doPost(HttpServletRequest oReq, HttpServletResponse oRes)
	   throws IOException, ServletException
	{
		String[] sJurs = oReq.getParameterValues("id");
		Arrays.sort(sJurs);
		ArrayList<Boundary> oBoundaries = new ArrayList();
		HashMap<String, ArrayList<Boundary>> oJurMap = new HashMap();
		BCIndex.getCurrentBoundaries(oBoundaries, sJurs);
		for (Boundary oBoundary : oBoundaries)
		{
			String sJur = oBoundary.getValue("jurisdiction");
			if (!oJurMap.containsKey(sJur))
				oJurMap.put(sJur, new ArrayList());
			
			oJurMap.get(sJur).add(oBoundary);
		}
		
		StringBuilder sBuf = new StringBuilder();
		sBuf.append('{');
		for (Map.Entry<String, ArrayList<Boundary>> oEntry : oJurMap.entrySet())
		{
			try
			{
				sBuf.append('\"').append(oEntry.getKey()).append("\":["); // jurisdiction id is the key
				for (Boundary oBoundary : oEntry.getValue())
				{
					oBoundary.appendJson(sBuf);
					sBuf.append(',');
				}
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
		oRes.setContentType("text/json");
	}
}
