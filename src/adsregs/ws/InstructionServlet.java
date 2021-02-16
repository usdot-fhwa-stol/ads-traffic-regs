/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package adsregs.ws;

import adsregs.bc.BCBase;
import adsregs.bc.BCBaseList;
import adsregs.bc.BCFactory;
import adsregs.bc.BCIndex;
import adsregs.bc.Instruction;
import adsregs.bc.Situation;
import adsregs.util.Text;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
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
public class InstructionServlet extends HttpServlet
{
	@Override
	public void doPost(HttpServletRequest oReq, HttpServletResponse oRes)
	   throws ServletException, IOException
	{
		String[] sJurs = oReq.getParameterValues("id");
		Arrays.sort(sJurs);
		char[][] cJurIds = new char[sJurs.length][];
		for (int nIndex = 0; nIndex < cJurIds.length; nIndex++)
			cJurIds[nIndex] = Text.getCharArray(sJurs[nIndex]);
		ArrayList<BCBaseList> oTitleList = new ArrayList();
		BCIndex.getInstructionsByJur(oTitleList, cJurIds);
		HashMap<String, String> oSituations = new HashMap();
		oRes.setContentType("application/json");
		
		StringBuilder sBuf = new StringBuilder();
		sBuf.append('{');
		if (oTitleList.isEmpty())
		{
			sBuf.append('}');
			oRes.getWriter().append(sBuf);
			return;
		}
		for (BCBaseList oList : oTitleList)
		{
			sBuf.append('\"').append(oList.m_cId).append("\":{\"titlename\":\"").append(oList.m_sLabel).append("\",");
			sBuf.append("\"instructions\":[");
			for (BCBase oInstruction : oList)
			{
				oInstruction.appendJson(sBuf);
				sBuf.append(',');
				for (char[] cSituation : ((Instruction)oInstruction).m_cSituations)
				{
					try
					{
						Situation oSituation = (Situation)BCFactory.createFromJson(BCIndex.getPath(cSituation));
						oSituations.put(oSituation.getValue("currid"), oSituation.getValue("label"));
					}
					catch (Exception oEx)
					{
						oEx.printStackTrace();
					}
				}
			}
			if (!oList.isEmpty())
				sBuf.setLength(sBuf.length() - 1);
			sBuf.append("],");
			sBuf.append("\"situations\":{");
			for (Entry<String, String> oEntry : oSituations.entrySet())
				sBuf.append("\"").append(oEntry.getKey()).append("\":\"").append(oEntry.getValue()).append("\",");
			if (!oSituations.isEmpty())
				sBuf.setLength(sBuf.length() - 1);
			sBuf.append("}},");
		}
		
		sBuf.setLength(sBuf.length() - 1);
		sBuf.append('}');
		
		oRes.getWriter().append(sBuf);
	}
}
