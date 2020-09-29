/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package adsregs.ws;

import adsregs.bc.BCFactory;
import adsregs.bc.BCIndex;
import adsregs.bc.Boundary;
import adsregs.bc.Jurisdiction;
import adsregs.util.Geo;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 *
 * @author aaron.cherney
 */
public class JurisdictionServlet extends HttpServlet
{
	@Override
	public void doGet(HttpServletRequest oReq, HttpServletResponse oRes)
	   throws ServletException, IOException
	{
		ArrayList<Jurisdiction> oJurs = new ArrayList();
		BCIndex.getCurrentJurisdictions(oJurs);
		
		StringBuilder sValBuf = new StringBuilder();
		StringBuilder sResBuf = new StringBuilder();
		
		sResBuf.append('{');
		for (Jurisdiction oJur : oJurs)
		{
			oJur.getValue("currid", sValBuf);
			sResBuf.append('\"').append(sValBuf).append("\":\"");
			oJur.getValue("label", sValBuf);
			sResBuf.append(sValBuf).append("\",");
		}
		sResBuf.setLength(sResBuf.length() - 1); // remove trailing comma
		sResBuf.append('}');
		
		oRes.getWriter().append(sResBuf);
		oRes.setContentType("application/json");
	}
	
	
	@Override
	public void doPost(HttpServletRequest oReq, HttpServletResponse oRes)
	   throws ServletException, IOException
	{
		double dLon = Double.parseDouble(oReq.getParameter("lon"));
		double dLat = Double.parseDouble(oReq.getParameter("lat"));
		ArrayList<Boundary> oBoundaries = new ArrayList();
		
		BCIndex.getCurrentBoundaries(oBoundaries);
		
		HashMap<String, ArrayList<Boundary>> oJurMap = new HashMap();
		
		for (Boundary oBoundary : oBoundaries)
		{
			if (!Geo.isInBoundingBox(dLon, dLat, oBoundary.m_dBBox))
				continue;
			
			String sJur = oBoundary.getValue("jurisdiction");
			if (!oJurMap.containsKey(sJur))
				oJurMap.put(sJur, new ArrayList());
			
			oJurMap.get(sJur).add(oBoundary);
		}
		
		StringBuilder sBuf = new StringBuilder();
		sBuf.append('{');
		for (Entry<String, ArrayList<Boundary>> oEntry : oJurMap.entrySet())
		{
			try
			{
				Jurisdiction oJur = (Jurisdiction)BCFactory.createFromJson(BCIndex.getPath(oEntry.getKey()));
				sBuf.append('\"').append(oEntry.getKey()).append("\":{"); // jurisdiction id is the key 
				sBuf.append("\"label\":\"").append(oJur.getValue("label")).append("\",");
				sBuf.append("\"boundaries\":{");
				for (Boundary oBoundary : oEntry.getValue())
				{
					sBuf.append("\"").append(oBoundary.getValue("currid")).append("\":\"").append(oBoundary.getValue("created-dt")).append("\"},");
				}
				sBuf.setLength(sBuf.length() - 1); // remove trailing comma
				sBuf.append("},");
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
