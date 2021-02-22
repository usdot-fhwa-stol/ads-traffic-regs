/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package adsregs.ws;

import adsregs.bc.BCIndex;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 *
 * @author aaron.cherney
 */
public class CreateBlock extends HttpServlet
{
	private static final Integer STRING = 0;
	private static final Integer NUMBER = 1;
	private static final Integer ARRAYSTRING = 2;
	private static final Integer ARRAYNUMBER = 3;
	private HashMap<String, Integer> m_nTypes = new HashMap();
	
	@Override
	public void init()
	{
		m_nTypes.put("label", STRING);
		m_nTypes.put("jurisdiction", STRING);
		m_nTypes.put("biblioref", STRING);
		m_nTypes.put("urlref", STRING);
		m_nTypes.put("title",STRING);
		m_nTypes.put("content", STRING);
		m_nTypes.put("situations", ARRAYSTRING);
		m_nTypes.put("descr", STRING);
		m_nTypes.put("instrs", ARRAYSTRING);
		m_nTypes.put("units", STRING);
		m_nTypes.put("svg", STRING);
	}
	
	@Override
	public void doPost(HttpServletRequest oReq, HttpServletResponse oRes)
	   throws IOException, ServletException
	{
		String[] sUriParts = oReq.getRequestURI().split("/");
		Map<String, String[]> oParams = oReq.getParameterMap();
		int nType = Integer.parseInt(sUriParts[sUriParts.length - 1]);
		
		if (nType < 0 || nType > 4)
		{
			oRes.sendError(HttpServletResponse.SC_BAD_REQUEST);
			return;
		}
		
		StringBuilder sBuf = new StringBuilder();
		sBuf.append('{');
		for (Entry<String, String[]> oEntry : oParams.entrySet())
		{
			sBuf.append("\"").append(oEntry.getKey()).append("\":");
			Integer oJsonType = m_nTypes.get(oEntry.getKey());
			if (oJsonType == null)
				oJsonType = STRING;
			
			int nJsonType = oJsonType;
			
			boolean bArray = nJsonType == ARRAYSTRING || nJsonType == ARRAYNUMBER;
			boolean bString = nJsonType ==  STRING || nJsonType == ARRAYSTRING;
			if (bArray)
			{
				sBuf.append('[');
				for (String sVal : oEntry.getValue())
				{
					if (bString)
					{
						sBuf.append("\"");
						escape(oEntry.getValue()[0], sBuf);
						sBuf.append("\"");
					}
					else
						sBuf.append(sVal);
						
					sBuf.append(',');
				}
				sBuf.setLength(sBuf.length() - 1);
				sBuf.append(']');
			}
			else
			{
				if (bString)
				{
					sBuf.append("\"");
					escape(oEntry.getValue()[0], sBuf);
					sBuf.append("\"");
				}
				else
					sBuf.append(oEntry.getValue()[0]);
			}
			sBuf.append(',');
		}
		
		String sNewId = BCIndex.createBlock(sBuf, nType);
		if (sNewId == null)
		{
			oRes.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to create new block");
			return;
		}
			
		System.out.append(sBuf).append('\n');
	}
	
	public static void escape(String sVal, StringBuilder sBuf)
	{
		for (int nIndex = 0; nIndex < sVal.length(); nIndex++)
		{
			char cChar = sVal.charAt(nIndex);
			switch (cChar)
			{
				case '\t':
					sBuf.append("\\t");
					break;
				case '\r':
					break;
				case '\n':
					sBuf.append("\\n");
					break;
				case '\\':
					sBuf.append("\\\\");
					break;
				case '"':
					sBuf.append("\\\"");
					break;
				default:
					sBuf.append(cChar);
					break;
			}
		}
	}
}
